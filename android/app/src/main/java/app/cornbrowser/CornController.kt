/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cornbrowser.browser.BlockKind
import app.cornbrowser.browser.ClearChoice
import app.cornbrowser.browser.DataClearing
import app.cornbrowser.browser.Flusher
import app.cornbrowser.browser.NEW_TAB
import app.cornbrowser.browser.TabEntry
import app.cornbrowser.browser.TabHost
import app.cornbrowser.browser.TabManager
import app.cornbrowser.data.CornData
import app.cornbrowser.data.CornSettings
import app.cornbrowser.data.FlushMode
import app.cornbrowser.data.SearchEngines
import app.cornbrowser.data.Shields
import app.cornbrowser.data.TabMemory
import uniffi.corn_ffi.HistoryVisit
import app.cornbrowser.ui.C
import app.cornbrowser.ui.ToastMsg
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class Screen { Onboarding, Browser, Tabs, Vault, Settings, History }

/** Which bottom sheet is open. */
sealed interface Sheet {
    data class Link(val url: String, val title: String) : Sheet
    data object Menu : Sheet
    data object Engine : Sheet
    data object Shield : Sheet
    data class Save(val url: String, val title: String) : Sheet
    data class Group(val groupId: Int) : Sheet
    data object ConfirmFlush : Sheet
    data class DeleteItem(val id: Long, val title: String) : Sheet
    /** Long-press on a history entry. */
    data class HistoryEntry(val visit: HistoryVisit) : Sheet
    /** Settings → Delete browsing data. */
    data object DeleteData : Sheet
    data object ConfirmClearHistory : Sheet
    /** Confirmation before "Keep all tabs in memory". */
    data object ConfirmKeepAllTabs : Sheet
}

/**
 * Corn's app state and actions. Screens read its Compose state and call its methods.
 * Owns the tabs, the Flush flow, Auto-Flush, Kept Session, locks and toasts.
 */
class CornController(private val context: Context) : TabHost {
    val settings = CornSettings(context)
    val data = CornData(context)
    val tabs = TabManager(context, this)
    private val main = Handler(Looper.getMainLooper())

    var screen by mutableStateOf(if (settings.onboarded) Screen.Browser else Screen.Onboarding)
    private var sheetState = mutableStateOf<Sheet?>(null)
    var sheet: Sheet?
        get() = sheetState.value
        set(v) {
            sheetState.value = v
        }
    var toast by mutableStateOf<ToastMsg?>(null)
        private set

    /** Engine used for the next searches; resets to the default on Flush. */
    var engineNow by mutableStateOf(SearchEngines.byId(settings.engine))
    var quip by mutableStateOf<String?>(null)
        private set
    /** True while the drain animation plays. */
    var flushing by mutableStateOf(false)
        private set
    var vaultUnlocked by mutableStateOf(false)
    var appLocked by mutableStateOf(settings.appLock && settings.hasPin)
    var fullscreen by mutableStateOf<View?>(null)
        private set
    private var exitFullscreen: (() -> Unit)? = null

    @Volatile private var siteShields: Map<String, Shields> = emptyMap()
    private val adsSinceSync = AtomicInteger(0)
    private val trackersSinceSync = AtomicInteger(0)
    private val blockSyncPending = AtomicBoolean(false)

    init {
        refreshShields()
        tabs.keepInMemory = ::tabsKeptInMemory
    }

    /** Settings → Advanced → Background tabs. */
    private fun tabsKeptInMemory(): Int = when (settings.tabMemory) {
        TabMemory.AUTO -> autoTabsKeptInMemory
        TabMemory.RECENT -> settings.tabMemoryCount
        TabMemory.ALL -> Int.MAX_VALUE
    }

    /**
     * "Automatic by phone memory": tabs kept in memory besides the open one.
     * Android reports a little less than the advertised size (a 4 GB phone shows about 3.7 GB),
     * hence the in-between thresholds: 4 GB → 1, 6 GB → 2, 8 GB → 3, 12 GB or more → 5.
     */
    val autoTabsKeptInMemory: Int by lazy {
        val am = context.getSystemService(ActivityManager::class.java)
        if (am.isLowRamDevice) return@lazy 0
        val gb = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem / 1_000_000_000.0
        when {
            gb < 5 -> 1
            gb < 7 -> 2
            gb < 10 -> 3
            else -> 5
        }
    }

    // ---------------- startup / lifecycle ----------------

    /** Restore the Kept Session if allowed, else start with a fresh new-tab page. */
    fun start(openUrl: String?, restartQuip: String? = null, restartToast: String? = null) {
        isStarting = true
        if (shouldAutoFlushOnReturn()) {
            // Corn was away long enough for Auto-Flush: wipe, then restart clean (opening nothing).
            clearData(ClearChoice.EVERYTHING, quipAfter = Flusher.QUIPS.random()) {}
        } else {
            data.loadSession()?.let { tabs.restore(it) }
        }
        isStarting = false
        restartQuip?.let { quip = it }
        restartToast?.let { showToast(it) }
        when {
            tabs.tabs.isEmpty() -> tabs.open(openUrl ?: NEW_TAB) // a link opened Corn: no extra empty tab
            openUrl != null -> openInNewTab(openUrl)
        }
        settings.leftAt = 0L
        settings.lastActivityAt = System.currentTimeMillis()
    }

    fun onAppForeground() {
        inForeground = true
        app.cornbrowser.browser.CornEngine.refreshIfStale(context)
        if (shouldAutoFlushOnReturn()) flushNow(animate = false)
        main.removeCallbacks(leaveFlush)
        settings.leftAt = 0L
        if (!(settings.appLock && settings.hasPin)) appLocked = false
        startInactivityTicker()
    }

    fun onAppBackground() {
        inForeground = false
        main.removeCallbacks(inactivityTick)
        val now = System.currentTimeMillis()
        settings.leftAt = now
        settings.persist()
        data.saveSession(tabs.toJson()) // a later Flush (manual or automatic) deletes it
        vaultUnlocked = false
        if (settings.appLock && settings.hasPin) appLocked = true
        settings.flushMode.delayMs?.let { delay -> main.postDelayed(leaveFlush, delay) }
    }

    private var inForeground = false

    /** Auto-Flush timer after leaving Corn. Runs in the background, so it can't relaunch Corn. */
    private val leaveFlush = Runnable { flushNow(animate = false) }

    /** Android says memory is running low (only acted on in "Automatic by phone memory"). */
    fun onTrimMemory(level: Int) {
        @Suppress("DEPRECATION")
        val low = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && level != ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        if (low && settings.tabMemory == TabMemory.AUTO) tabs.trimMemory()
    }

    private fun shouldAutoFlushOnReturn(): Boolean {
        val now = System.currentTimeMillis()
        val left = settings.leftAt
        val byLeave = settings.flushMode.delayMs?.let { left > 0 && now - left >= it } ?: false
        val byIdle = settings.inactivity && now - settings.lastActivityAt >= settings.inactivityMinutes * 60_000L
        return byLeave || byIdle
    }

    /** Any touch counts as activity. */
    fun onUserActivity() {
        settings.lastActivityAt = System.currentTimeMillis()
    }

    private val inactivityTick = object : Runnable {
        override fun run() {
            if (tabs.anyMediaPlaying) onUserActivity() // a playing video counts as activity
            if (settings.inactivity &&
                System.currentTimeMillis() - settings.lastActivityAt >= settings.inactivityMinutes * 60_000L
            ) {
                flushNow(animate = false)
                onUserActivity()
            }
            if (settings.inactivity) main.postDelayed(this, 30_000L)
        }
    }

    /** Only runs while "Flush after inactivity" is on: no wake-ups otherwise. */
    private fun startInactivityTicker() {
        main.removeCallbacks(inactivityTick)
        if (settings.inactivity) main.postDelayed(inactivityTick, 30_000L)
    }

    /** Settings calls this when the inactivity switch changes. */
    fun onInactivitySettingChanged() = startInactivityTicker()

    // ---------------- navigation ----------------

    fun finishOnboarding() {
        settings.onboarded = true
        screen = Screen.Browser
    }

    fun go(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        quip = null
        tabs.load(SearchEngines.resolve(text, engineNow, settings.safeSearch))
    }

    fun openInNewTab(url: String) {
        tabs.open(url)
        screen = Screen.Browser
    }

    fun newTab() {
        tabs.open(NEW_TAB)
        sheet = null
        screen = Screen.Browser
    }

    fun newTabGroup() {
        tabs.openNewGroup()
        sheet = null
        screen = Screen.Browser
    }

    fun openTabsScreen() {
        tabs.captureCurrentPreview()
        screen = Screen.Tabs
    }

    fun switchTab(id: Int) {
        tabs.switchTo(id)
        sheet = null
        screen = Screen.Browser
    }

    fun back(): Boolean {
        when {
            fullscreen != null -> exitFullscreen?.invoke()
            sheet != null -> sheet = null
            screen != Screen.Browser && screen != Screen.Onboarding -> screen = Screen.Browser
            tabs.current?.canGoBack == true -> tabs.goBack()
            tabs.backGoesHome -> tabs.goHome()
            else -> return false
        }
        return true
    }

    // ---------------- link menu ----------------

    fun openLinkInNewTab(url: String) {
        sheet = null
        val entry = tabs.open(url, switchTo = false)
        showToast("Opened in a new tab", "Switch") { switchTab(entry.id) }
    }

    fun openLinkInGroup(url: String) {
        sheet = null
        tabs.open(url, inGroup = true, switchTo = false)
        val g = tabs.group(tabs.current?.groupId)
        showToast("Added to “${g?.name ?: "group"}”. Tap it in the strip.")
    }

    fun copyLink(url: String) {
        sheet = null
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Link", url))
        showToast("Link copied")
    }

    fun share(url: String) {
        sheet = null
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---------------- Vault ----------------

    fun openSaveSheet() {
        val cur = tabs.current ?: return
        if (cur.isNewTab) { showToast("Open a page first, then save it"); return }
        sheet = Sheet.Save(cur.url, cur.title)
    }

    /** `collectionId` null = saved without a Collection (it shows in All). */
    fun save(collectionId: Long?, collectionName: String?, title: String, url: String, tags: List<String>) {
        data.save(collectionId, title.ifBlank { url }, url, tags)
        sheet = null
        showToast(if (collectionName != null) "Saved to $collectionName" else "Saved to your Vault")
    }

    fun saveGroup(groupId: Int) {
        val g = tabs.group(groupId) ?: return
        val links = tabs.groupTabs(groupId).filterNot { it.isNewTab }.map { it.title to it.url }
        val added = data.saveGroup(g.name, links)
        sheet = null
        showToast("Saved $added links to “${g.name}”")
    }

    fun deleteVaultItem(id: Long) {
        data.deleteItem(id)
        sheet = null
        showToast("Deleted from the Vault")
    }

    fun openVault() {
        sheet = null
        screen = Screen.Vault
    }

    // ---------------- Flush ----------------

    /** The Flush Button. Asks first only if the user turned on Flush confirmation. */
    fun requestFlush() {
        if (settings.flushConfirm && sheet != Sheet.ConfirmFlush) sheet = Sheet.ConfirmFlush else flushNow(animate = true)
    }

    fun flushNow(animate: Boolean) {
        sheet = null
        val wipe = {
            val line = Flusher.QUIPS.random()
            clearData(ClearChoice.EVERYTHING, quipAfter = line) {
                vaultUnlocked = false
                engineNow = SearchEngines.byId(settings.engine)
                quip = line
            }
        }
        if (!animate) { wipe(); return }
        screen = Screen.Browser
        Flusher.vibrate(context)
        if (settings.flushSound) Flusher.playSound()
        flushing = true
        main.postDelayed({ wipe(); flushing = false }, 720)
    }

    /**
     * Run [DataClearing]. When cookies were deleted from WebView's cookie file, Corn must restart
     * its process so WebView forgets the cookies it holds in memory:
     * - in the foreground, Corn relaunches itself and shows [quipAfter] / [toastAfter];
     * - in the background (Auto-Flush after leaving), Android doesn't allow a relaunch, so the
     *   process just ends and the next launch starts clean.
     * Without a restart (cookie file not editable, or cookies not chosen), [done] runs instead.
     */
    private fun clearData(what: ClearChoice, quipAfter: String? = null, toastAfter: String? = null, done: () -> Unit) {
        DataClearing.clear(context, tabs, data, what) { needsRestart ->
            if (!needsRestart) {
                done()
                return@clear
            }
            settings.markFlushedAndSave()
            // Tabs not chosen for deletion come back after the restart (they reload).
            if (!what.tabs) data.saveSession(tabs.toJson())
            if (inForeground || isStarting) {
                RestartActivity.restart(context, quipAfter, toastAfter)
            } else {
                RestartActivity.exitProcess()
            }
        }
    }

    /** True while MainActivity.onCreate runs start() (the app is visible: a relaunch is allowed). */
    private var isStarting = false

    // ---------------- history ----------------

    /**
     * Record a visit. Pages often change their own address while loading (a search page adds
     * `&ia=web`, redirects, `history.replaceState`), and WebView reports each change. Within 5 s in
     * the same tab, the same page (same site and path, any `?…`) counts as one visit.
     */
    override fun onVisited(entry: TabEntry, url: String, title: String) {
        if (!settings.keepHistory || !(url.startsWith("https://") || url.startsWith("http://"))) return
        val uri = android.net.Uri.parse(url)
        val host = uri.host?.removePrefix("www.") ?: return
        val now = System.currentTimeMillis()
        val last = entry.lastVisitUrl?.let { android.net.Uri.parse(it) }
        val samePage = last != null && last.host?.removePrefix("www.") == host && (last.path ?: "") == (uri.path ?: "")
        if (samePage && now - entry.lastVisitAt < 5_000) return
        entry.lastVisitUrl = url
        entry.lastVisitAt = now
        data.recordVisit(url, title, host)
    }

    override fun onTitle(url: String, title: String) {
        if (settings.keepHistory) data.updateTitle(url, title)
    }

    fun openHistory() {
        sheet = null
        screen = Screen.History
    }

    fun openFromHistory(url: String) {
        screen = Screen.Browser
        if (tabs.current?.isNewTab == true) tabs.load(url) else tabs.open(url)
    }

    /** Settings → Delete browsing data. Everything in each ticked category (no time ranges). */
    fun deleteBrowsingData(what: ClearChoice) {
        sheet = null
        clearData(what, toastAfter = "Browsing data deleted") { showToast("Browsing data deleted") }
    }

    // ---------------- per-site blocking ----------------

    /** corn.db holds per-site choices; cache them so WebView's network threads never touch the database. */
    private fun refreshShields() {
        siteShields = data.allSiteShields()
    }

    /** Change what's blocked on one site, then reload the page so it takes effect. */
    fun setShields(host: String, shields: Shields) {
        data.setSiteShields(host, shields)
        refreshShields()
        tabs.reload()
    }

    override fun shields(host: String): Shields = siteShields[host] ?: Shields.ALL_ON

    override fun onBlocked(kind: BlockKind) {
        when (kind) {
            BlockKind.AD -> adsSinceSync.incrementAndGet()
            BlockKind.TRACKER -> trackersSinceSync.incrementAndGet()
            BlockKind.POPUP -> {}
        }
        if (blockSyncPending.compareAndSet(false, true)) {
            main.post {
                blockSyncPending.set(false)
                val a = adsSinceSync.getAndSet(0)
                val t = trackersSinceSync.getAndSet(0)
                settings.totalBlocked += a + t
            }
        }
    }

    override fun onBlockedPopup() = main.post {
        settings.totalPopups += 1
        showToast("Blocked a pop-up")
    }.let { }

    override fun onPopupAllowed(opener: TabEntry, resultMsg: android.os.Message): Boolean = tabs.openPopup(opener, resultMsg)
    override fun onBlockedRedirect(target: String) = main.post {
        settings.totalPopups += 1
        showToast("Blocked a redirect to $target")
    }.let { }

    override fun onLinkLongPress(entry: TabEntry, url: String, title: String) {
        sheet = Sheet.Link(url, title)
    }

    override fun onMediaChanged() {
        if (tabs.anyMediaPlaying) onUserActivity()
    }

    override fun onEnterFullscreen(view: View, exit: () -> Unit) {
        fullscreen = view
        exitFullscreen = exit
    }

    override val cookieBannersEnabled: Boolean get() = settings.cookieBanners
    override val safeBrowsingEnabled: Boolean get() = settings.safeBrowsing

    /** Settings calls this so open tabs follow the Safe Browsing switch right away. */
    fun applySafeBrowsing() = tabs.forEachLive { it.setSafeBrowsing(settings.safeBrowsing) }

    override fun onPageCrashed(entry: TabEntry) {
        if (fullscreen != null) onExitFullscreen()
        tabs.dropCrashed(entry)
    }

    override fun onExitFullscreen() {
        fullscreen = null
        exitFullscreen = null
    }

    // ---------------- toast ----------------

    private val hideToast = Runnable { toast = null }

    fun showToast(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
        toast = ToastMsg(text, action, onAction)
        main.removeCallbacks(hideToast)
        main.postDelayed(hideToast, 2800)
    }

    fun toastAction() {
        val a = toast?.onAction
        toast = null
        a?.invoke()
    }

    // ---------------- helpers for screens ----------------

    fun cycleGroupColor(groupId: Int) {
        val g = tabs.group(groupId) ?: return
        g.color = C.GroupColors[(C.GroupColors.indexOf(g.color) + 1) % C.GroupColors.size]
    }

    fun newTabInGroup(groupId: Int) {
        val first = tabs.groupTabs(groupId).firstOrNull() ?: return
        if (tabs.current?.groupId != groupId) tabs.switchTo(first.id)
        tabs.open(NEW_TAB, inGroup = true)
        sheet = null
        screen = Screen.Browser
    }
}
