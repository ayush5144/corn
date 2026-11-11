/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser

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
import com.flushbrowser.browser.BlockKind
import com.flushbrowser.browser.ClearChoice
import com.flushbrowser.browser.DataClearing
import com.flushbrowser.browser.DownloadRequest
import com.flushbrowser.browser.SitePermission
import com.flushbrowser.browser.Flusher
import com.flushbrowser.browser.NEW_TAB
import com.flushbrowser.browser.TabEntry
import com.flushbrowser.browser.TabHost
import com.flushbrowser.browser.TabManager
import com.flushbrowser.data.AppData
import com.flushbrowser.data.AppSettings
import com.flushbrowser.data.FlushMode
import com.flushbrowser.data.SearchEngines
import com.flushbrowser.data.Shields
import com.flushbrowser.data.TabMemory
import uniffi.flush_ffi.HistoryVisit
import com.flushbrowser.ui.C
import com.flushbrowser.ui.ToastMsg
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class Screen { Onboarding, Browser, Tabs, Vault, Settings, History }

/** Which bottom sheet is open. */
sealed interface Sheet {
    data class Link(val url: String, val title: String) : Sheet
    /** Vault → New collection: name it, then Create. */
    data object NewCollection : Sheet
    /** Long-press on an image; [linkUrl] is set when the image is also a link. */
    data class Image(val imageUrl: String, val linkUrl: String?, val title: String?) : Sheet
    /** A tapped link to another app: ask before leaving Corn. [intent] is already made safe. */
    data class ExternalApp(val title: String, val detail: String, val appName: String?, val intent: Intent, val fallbackUrl: String?) : Sheet
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
    /** A site asks for the camera, microphone or location. */
    data class SitePermissionAsk(val site: String, val kinds: Set<SitePermission>) : Sheet
    /** Confirm a download. */
    data class Download(val request: DownloadRequest, val fileName: String) : Sheet
    /** Menu → Split screen: pick the tab for the second pane. */
    data object SplitPick : Sheet
    /** Passwords: save / update, pick an account, PIN before filling, confirmations. */
    data class Password(val kind: com.flushbrowser.passwords.PasswordSheet) : Sheet
}

/** An address-bar suggestion, from the Vault or History (computed on the phone, never sent anywhere). */
data class Suggestion(val title: String, val url: String, val fromVault: Boolean)

/**
 * Corn's app state and actions. Screens read its Compose state and call its methods.
 * Owns the tabs, the Flush flow, Auto-Flush, Kept Session, locks and toasts.
 */
class AppController(private val context: Context) : TabHost {
    val settings = AppSettings(context)
    val data = AppData(context)
    val tabs = TabManager(context, this)
    /** Saved passwords (see passwords/Passwords.kt). */
    val passwords = com.flushbrowser.passwords.Passwords(context, this)
    private val main = Handler(Looper.getMainLooper())

    var screen by mutableStateOf(if (settings.onboarded) Screen.Browser else Screen.Onboarding)
    private var sheetState = mutableStateOf<Sheet?>(null)
    var sheet: Sheet?
        get() = sheetState.value
        set(v) {
            sheetState.value = v
        }
    /** Measured heights (px) of the browser's bottom bar and the open sheet, so toasts sit just above them. */
    var bottomBarHeightPx by mutableIntStateOf(0)
    var sheetHeightPx by mutableIntStateOf(0)
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
    /** The tab whose page is fullscreen. */
    var fullscreenTabId by mutableStateOf<Int?>(null)
        private set
    /** Fullscreen started while that tab was in split screen: the video fills only its pane. */
    var fullscreenInPane by mutableStateOf(false)
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
        passwords.clearCopyLeftOver() // in case Corn was closed before it could clear a copied password
        tabs.resumeAllTimers()
        com.flushbrowser.browser.BlockEngine.refreshIfStale(context)
        if (shouldAutoFlushOnReturn()) flushNow(animate = false)
        main.removeCallbacks(leaveFlush)
        settings.leftAt = 0L
        if (!(settings.appLock && settings.hasPin)) appLocked = false
        startInactivityTicker()
    }

    fun onAppBackground() {
        inForeground = false
        passwords.clearCopiedPassword() // a copied password never stays on the clipboard after Corn leaves the screen
        tabs.pauseAllTimers()
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

    /** Put the current tab in a new group with a new tab beside it (like "Open in new tab group" for links). */
    fun newTabGroup() {
        tabs.open(NEW_TAB, inGroup = true)
        sheet = null
        screen = Screen.Browser
    }

    fun openTabsScreen() {
        tabs.captureCurrentPreview()
        screen = Screen.Tabs
    }

    /** Close a tab. Closing the last one leaves a fresh home page and returns to the browser. */
    fun closeTab(id: Int) {
        val wasLast = tabs.tabs.size == 1
        tabs.close(id)
        if (wasLast) {
            sheet = null
            quip = null // an old Flush one-liner would be misleading here
            screen = Screen.Browser
        }
    }

    fun switchTab(id: Int) {
        if (findQuery != null) closeFind()
        tabs.switchTo(id)
        sheet = null
        screen = Screen.Browser
    }

    // ---------------- split screen ----------------

    /** Start split screen with tab [otherId] in the second pane, or a new tab when null. */
    fun startSplit(otherId: Int?) {
        if (findQuery != null) closeFind()
        sheet = null
        if (otherId == null) tabs.startSplitWithNewTab() else tabs.startSplit(otherId)
        screen = Screen.Browser
    }

    /** Menu → Close split screen: keeps the focused tab. */
    fun exitSplit() {
        sheet = null
        tabs.exitSplit()
    }

    /** The × on a pane, or the divider dragged over it: closes the pane, not the tab. */
    fun closePane(tabId: Int) {
        if (findQuery != null) closeFind()
        tabs.closePane(tabId)
    }

    /** A touch inside a pane focuses it: the address bar, toolbar and sheets now act on its tab. */
    fun focusPane(tabId: Int) {
        if (tabId == tabs.currentId || tabs.split?.contains(tabId) != true) return
        if (findQuery != null) closeFind()
        tabs.focusPane(tabId)
    }

    fun back(): Boolean {
        when {
            fullscreen != null -> exitFullscreen()
            sheet != null -> dismissSheet()
            findQuery != null -> closeFind()
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

    /** New collection sheet → Create. The sheet already checked the name is new and not empty. */
    fun createCollection(name: String) {
        val created = data.createCollection(name)
        if (created == null) { showToast("Couldn't create that collection"); return }
        sheet = null
        showToast("Collection “${created.name}” created")
    }

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

    // ---------------- address-bar suggestions ----------------

    /** The address bar's text while it's being edited (null when not editing): drives suggestions. */
    var addressQuery by mutableStateOf<String?>(null)

    /** Up to 6 matches: saved Vault links first, then History. Local only. */
    fun suggestions(query: String): List<Suggestion> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val out = LinkedHashMap<String, Suggestion>()
        // A locked Vault never shows its links here: only when it's unlocked (or the lock is off).
        val vaultOpen = !settings.vaultLock || vaultUnlocked
        if (vaultOpen) runCatching { data.search(q) }.getOrDefault(emptyList()).take(3)
            .forEach { out.putIfAbsent(it.url, Suggestion(it.title.ifBlank { it.url }, it.url, true)) }
        data.history(q, 30).forEach { v -> if (out.size < 6) out.putIfAbsent(v.url, Suggestion(v.title.ifBlank { v.host }, v.url, false)) }
        return out.values.take(6)
    }

    fun openSuggestion(s: Suggestion) {
        addressQuery = null
        quip = null
        tabs.load(s.url)
    }

    // ---------------- find in page ----------------

    /** Non-null while the find bar is open. */
    var findQuery by mutableStateOf<String?>(null)
        private set
    var findActive by mutableIntStateOf(0)
        private set
    var findTotal by mutableIntStateOf(0)
        private set

    fun openFind() {
        sheet = null
        if (tabs.current?.live == null) { showToast("Open a page first"); return }
        findQuery = ""
        findTotal = 0
    }

    fun find(query: String) {
        findQuery = query
        if (query.isEmpty()) findTotal = 0
        tabs.current?.live?.find(query)
    }

    fun findNext(forward: Boolean) = tabs.current?.live?.findNext(forward)

    fun closeFind() {
        tabs.current?.live?.clearFind()
        findQuery = null
        findTotal = 0
    }

    // ---------------- passwords ----------------

    override fun onLoginMessage(entry: TabEntry, data: String, sourceOrigin: String, isMainFrame: Boolean, reply: androidx.webkit.JavaScriptReplyProxy) =
        passwords.onMessage(entry, data, sourceOrigin, isMainFrame, reply)

    override fun onLoginPageStarted(entry: TabEntry) = passwords.onPageStarted(entry)
    override fun onTabClosed(entry: TabEntry) {
        passwords.forgetTab(entry)
        // Its WebView is gone: nothing would ever report the end of its fullscreen.
        if (fullscreenTabId == entry.id) onExitFullscreen()
    }

    /** A tab opened as a new window closed itself: close it and go back to the page that opened it. */
    override fun onWindowClosed(entry: TabEntry) {
        main.post { tabs.closeOpenedWindow(entry) }
    }

    override fun onFindResult(active: Int, total: Int) {
        findActive = active
        findTotal = total
    }

    // ---------------- text size ----------------

    override val textZoom: Int get() = settings.textSize

    fun applyTextSize() = tabs.forEachLive { it.setTextZoom(settings.textSize) }

    // ---------------- file uploads ----------------

    /** Set by MainActivity: opens Android's file picker. */
    var fileChooserLauncher: ((Intent) -> Unit)? = null
    private var pendingFiles: android.webkit.ValueCallback<Array<android.net.Uri>>? = null

    override fun onFileChooser(callback: android.webkit.ValueCallback<Array<android.net.Uri>>, params: android.webkit.WebChromeClient.FileChooserParams): Boolean {
        val launch = fileChooserLauncher ?: return false
        val intent = runCatching { params.createIntent() }.getOrNull() ?: return false
        if (params.mode == android.webkit.WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        pendingFiles?.onReceiveValue(null) // a page may only have one picker open
        pendingFiles = callback
        return runCatching { launch(intent) }.map { true }.getOrElse {
            pendingFiles = null
            showToast("No app on this phone can pick files")
            false
        }
    }

    /** The picker closed: hand the chosen files (or nothing) back to the page. Always answers. */
    fun onFilesChosen(resultCode: Int, data: Intent?) {
        val callback = pendingFiles ?: return
        pendingFiles = null
        val ok = resultCode == android.app.Activity.RESULT_OK
        val uris = data?.clipData?.let { clip -> Array(clip.itemCount) { clip.getItemAt(it).uri } }
            ?: android.webkit.WebChromeClient.FileChooserParams.parseResult(resultCode, data)
        callback.onReceiveValue(if (ok) uris else null)
    }

    // ---------------- site permissions (camera, microphone, location) ----------------

    /** Set by MainActivity: asks Android for runtime permissions, then reports each result. */
    var androidPermissionRequester: ((Array<String>, (Map<String, Boolean>) -> Unit) -> Unit)? = null

    /** This session's answers per site. Not saved: a restart or Flush forgets them. */
    val sitePermissions = androidx.compose.runtime.mutableStateMapOf<Pair<String, SitePermission>, Boolean>()
    private var pendingPermission: ((Boolean) -> Unit)? = null

    override fun onSitePermission(entry: TabEntry, site: String, kinds: Set<SitePermission>, answer: (Boolean) -> Unit) {
        val known = kinds.map { sitePermissions[site to it] }
        when {
            known.any { it == false } -> answer(false)
            known.all { it == true } -> ensureAndroidPermissions(kinds, answer)
            else -> {
                pendingPermission?.invoke(false)
                pendingPermission = answer
                sheet = Sheet.SitePermissionAsk(site, kinds)
            }
        }
    }

    fun answerSitePermission(site: String, kinds: Set<SitePermission>, allow: Boolean) {
        val answer = pendingPermission ?: return
        pendingPermission = null
        sheet = null
        kinds.forEach { sitePermissions[site to it] = allow }
        if (allow) ensureAndroidPermissions(kinds, answer) else answer(false)
    }

    override fun onSitePermissionCancelled() {
        pendingPermission = null
        if (sheet is Sheet.SitePermissionAsk) sheet = null
    }

    /** Forget this session's answer for a site (shield sheet). */
    fun forgetSitePermission(site: String, kind: SitePermission) = sitePermissions.remove(site to kind)

    private fun ensureAndroidPermissions(kinds: Set<SitePermission>, done: (Boolean) -> Unit) {
        val needed = kinds.flatMap {
            when (it) {
                SitePermission.CAMERA -> listOf(android.Manifest.permission.CAMERA)
                SitePermission.MICROPHONE -> listOf(android.Manifest.permission.RECORD_AUDIO)
                SitePermission.LOCATION -> listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        val granted = { p: String -> androidx.core.content.ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        fun satisfied(has: (String) -> Boolean) = kinds.all {
            when (it) {
                SitePermission.CAMERA -> has(android.Manifest.permission.CAMERA)
                SitePermission.MICROPHONE -> has(android.Manifest.permission.RECORD_AUDIO)
                SitePermission.LOCATION -> has(android.Manifest.permission.ACCESS_FINE_LOCATION) || has(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (satisfied(granted)) { done(true); return }
        val ask = androidPermissionRequester ?: run { done(false); return }
        ask(needed.toTypedArray()) { results ->
            val ok = satisfied { p -> results[p] == true || granted(p) }
            if (!ok) showToast("Android didn't allow it. You can change this in the phone's Settings → Apps → Corn.")
            done(ok)
        }
    }

    // ---------------- downloads ----------------

    private val downloads = mutableMapOf<Long, String>()

    override fun onDownload(request: DownloadRequest) {
        main.post {
            if (!request.url.startsWith("https://") && !request.url.startsWith("http://")) {
                showToast("Corn can't download this kind of file yet")
                return@post
            }
            val name = android.webkit.URLUtil.guessFileName(request.url, request.contentDisposition, request.mimeType)
            sheet = Sheet.Download(request, name)
        }
    }

    /** Hand the file to Android's download manager (it downloads even if Corn is closed). */
    fun startDownload(request: DownloadRequest, name: String) {
        sheet = null
        val dm = context.getSystemService(android.app.DownloadManager::class.java)
        val req = android.app.DownloadManager.Request(android.net.Uri.parse(request.url))
            .setTitle(name)
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE) // only while downloading
            .addRequestHeader("User-Agent", request.userAgent)
        request.mimeType?.takeIf { it.isNotBlank() }?.let { req.setMimeType(it) }
        android.webkit.CookieManager.getInstance().getCookie(request.url)?.let { req.addRequestHeader("Cookie", it) }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
        } else {
            req.setDestinationInExternalFilesDir(context, android.os.Environment.DIRECTORY_DOWNLOADS, name)
        }
        val id = runCatching { dm.enqueue(req) }.getOrElse { showToast("The download couldn't start"); return }
        downloads[id] = name
        showToast("Downloading $name…")
    }

    /** Android says a download finished (MainActivity listens while Corn is open). */
    fun onDownloadComplete(id: Long) {
        val name = downloads.remove(id) ?: return
        val dm = context.getSystemService(android.app.DownloadManager::class.java)
        val ok = dm.query(android.app.DownloadManager.Query().setFilterById(id)).use { c ->
            c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS)) == android.app.DownloadManager.STATUS_SUCCESSFUL
        }
        if (!ok) { showToast("$name didn't download"); return }
        showToast("Downloaded $name", "Open") {
            val uri = dm.getUriForDownloadedFile(id)
            val open = Intent(Intent.ACTION_VIEW).setDataAndType(uri, dm.getMimeTypeForDownloadedFile(id))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(open) }.onFailure { showToast("No app on this phone can open this file") }
        }
    }

    /** Closing any sheet: a site permission left unanswered counts as Block (not remembered). */
    fun dismissSheet() {
        if (sheet is Sheet.SitePermissionAsk) { pendingPermission?.invoke(false); pendingPermission = null }
        sheet = null
    }

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

    override fun onImageLongPress(entry: TabEntry, imageUrl: String, linkUrl: String?, title: String?) {
        sheet = Sheet.Image(imageUrl, linkUrl, title)
    }

    /** Image sheet → Save image: the same confirm sheet and Downloads folder as any download. */
    fun saveImage(imageUrl: String) {
        if (!imageUrl.startsWith("https://") && !imageUrl.startsWith("http://")) {
            sheet = null
            showToast("Corn can't save this image")
            return
        }
        val ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(imageUrl)
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        val ua = tabs.current?.live?.webView?.settings?.userAgentString ?: ""
        onDownload(DownloadRequest(imageUrl, ua, null, mime, -1))
    }

    // ---------------- links to other apps ----------------

    /**
     * A tapped link to another app. Builds a safe intent (browsable only, no component or selector,
     * no permission grants, never Corn itself), the way Chrome and DuckDuckGo do, then asks.
     */
    override fun onExternalLink(entry: TabEntry, url: String) {
        main.post {
            val kind = com.flushbrowser.browser.ExternalLinks.kind(url)
            val scheme = url.substringBefore(':').lowercase()
            val intent = runCatching {
                when (scheme) {
                    "intent" -> Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    "android-app" -> Intent.parseUri(url, Intent.URI_ANDROID_APP_SCHEME)
                    else -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                }
            }.getOrNull()
            if (intent == null) { showToast("Corn can't open this link"); return@post }
            val fallback = com.flushbrowser.browser.ExternalLinks.fallback(intent.getStringExtra("browser_fallback_url"))
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // drops any URI permission grants the page asked for
            if (intent.`package` == context.packageName) return@post
            // The app's name, when Android lets Corn see it. Since Android 11 most installed apps are
            // hidden from other apps, so "unknown" doesn't mean "not installed": Open still tries, and
            // falls back to the link's web version (if it has one) only when no app takes it.
            val pm = context.packageManager
            val target = runCatching { pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) }.getOrNull()
            // "android" = Android's own app chooser (several apps can open it): no single name to show.
            val appName = target?.activityInfo?.takeIf { it.packageName != "android" }?.loadLabel(pm)?.toString()
            sheet = Sheet.ExternalApp(
                title = com.flushbrowser.browser.ExternalLinks.title(kind, appName),
                detail = com.flushbrowser.browser.ExternalLinks.detail(kind, url),
                appName = appName,
                intent = intent,
                fallbackUrl = fallback,
            )
        }
    }

    /** Ask sheet → Open web version (only offered when the link has one). */
    fun openExternalFallback(s: Sheet.ExternalApp) {
        sheet = null
        s.fallbackUrl?.let { tabs.load(it) }
    }

    /** Ask sheet → Open. */
    fun openExternalApp(s: Sheet.ExternalApp) {
        sheet = null
        val opened = runCatching { context.startActivity(s.intent) }.isSuccess
        if (opened) return
        if (s.fallbackUrl != null) tabs.load(s.fallbackUrl) else showToast("No app on this phone can open this link")
    }

    override fun onMediaChanged() {
        if (tabs.anyMediaPlaying) onUserActivity()
    }

    override fun onEnterFullscreen(entry: TabEntry, view: View, exit: () -> Unit) {
        fullscreen = view
        fullscreenTabId = entry.id
        fullscreenInPane = tabs.split?.contains(entry.id) == true
        exitFullscreen = exit
    }

    /** Leave fullscreen: tell the page, and clear Corn's state even if the page never answers. */
    fun exitFullscreen() {
        val exit = exitFullscreen
        onExitFullscreen()
        runCatching { exit?.invoke() }
    }

    override val darkenWebsites: Boolean get() = settings.darkenWebsites

    /** Pages use the same theme as Corn (Settings → Appearance). */
    override val lightPages: Boolean get() = com.flushbrowser.ui.C.light

    /** Appearance changed: rebuild open pages so sites see the new light/dark preference. */
    fun onThemeChanged() = tabs.recreateAll()

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
        fullscreenTabId = null
        fullscreenInPane = false
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
        // Split screen: don't touch the panes being viewed; add the tab to the group and offer to open it.
        if (tabs.split != null) {
            val added = tabs.addToGroupInBackground(groupId) ?: return
            sheet = null
            screen = Screen.Browser
            showToast("New tab added to “${tabs.group(groupId)?.name ?: "the group"}”", "Open") { switchTab(added.id) }
            return
        }
        val first = tabs.groupTabs(groupId).firstOrNull() ?: return
        if (tabs.current?.groupId != groupId) tabs.switchTo(first.id)
        tabs.open(NEW_TAB, inGroup = true)
        sheet = null
        screen = Screen.Browser
    }
}
