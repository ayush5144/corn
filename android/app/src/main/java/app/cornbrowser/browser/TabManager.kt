/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.cornbrowser.ui.C
import org.json.JSONArray
import org.json.JSONObject

const val NEW_TAB = "corn://new"

/** A named set of tabs browsed together. */
class TabGroup(val id: Int, name: String, color: Color, autoNamed: Boolean = false) {
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    /** True while the group still has its placeholder name; it takes the first site's name. */
    var autoNamed by mutableStateOf(autoNamed)
}

/** One tab. Always exists; its WebView (`live`) only exists while the tab is active or paused. */
class TabEntry(val id: Int, url: String, groupId: Int?) {
    var url by mutableStateOf(url)
    var title by mutableStateOf(if (url == NEW_TAB) "New tab" else url)
    var groupId by mutableStateOf(groupId)
    var favicon by mutableStateOf<Bitmap?>(null)
    var preview by mutableStateOf<Bitmap?>(null)
    var blockedAds by mutableIntStateOf(0)
    var blockedTrackers by mutableIntStateOf(0)
    var guarded by mutableIntStateOf(0)
    val blocked: Int get() = blockedAds + blockedTrackers
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    /** True when this tab shows Corn's own new-tab page instead of a website. */
    val isNewTab: Boolean get() = url == NEW_TAB

    val host: String get() = Uri.parse(url).host?.removePrefix("www.") ?: ""

    @Volatile var mediaPlaying = false
    /** True while the page is loading (drives the pull-to-refresh spinner). */
    var loading by mutableStateOf(false)
    /** Set when the page process died; the page shows a reload card. */
    var crashed by mutableStateOf(false)
    /** True when this tab's first page was opened from Corn's home page: Back returns home. */
    var openedFromHome by mutableStateOf(false)
    /** After returning home with Back, Forward reopens this address. */
    internal var homeForwardUrl: String? = null
    /** Last address recorded in history from this tab, and when (to merge address updates into one visit). */
    internal var lastVisitUrl: String? = null
    internal var lastVisitAt: Long = 0
    /** True while an unloaded tab reloads: its saved picture covers the page until the page draws. */
    var showingSnapshot by mutableStateOf(false)
    /** Set when the page couldn't load (no connection, site not found…); shows Corn's error page. */
    var loadError by mutableStateOf<String?>(null)
    /** Scroll position saved when the tab was unloaded, restored after it reloads. */
    internal var savedScrollY: Int = 0
    internal var live: CornTab? = null
    internal var saved: Bundle? = null
}

/**
 * Owns every tab and Tab Group, and the rule for which tabs stay in memory.
 *
 * ## Tab states (the real rules; keep this in sync with docs/02-technical/03-android.md)
 * - **Open tab**: the one on screen. Fully running.
 * - **Kept in memory**: the N most recently used other tabs (N from Settings → Advanced →
 *   Background tabs, via [keepInMemory]). Their WebView stays alive, so returning is instant
 *   and keeps scroll position, video position and typed text. On leaving, Corn pauses every
 *   video/audio element and blocks it from restarting ([CornTab.keepInBackground]), and calls
 *   `WebView.onPause()`. What that does NOT do, per Android's own documentation:
 *   *"Note that this call does not pause JavaScript."* Measured on the emulator: a page kept
 *   in memory learns it is hidden and its timers are slowed about 5× (to about once a second),
 *   but it still runs. `pauseTimers()` would stop scripts, but it is global ("for all WebViews"),
 *   so it would also stop the open tab; Corn never calls it.
 * - **Unloaded**: every other tab. The WebView is destroyed (nothing runs), Corn keeps the
 *   preview picture, the back/forward history and the scroll position. Opening it reloads the
 *   page and scrolls back to where it was.
 * - **Opened in the background** ("Open in new tab", "…in new tab group"): starts loading at once
 *   (like Chrome) as the most recent tab after the open one, so it's ready when tapped. If that goes
 *   over the limit, the oldest kept tab is unloaded. Not when the user keeps 0 tabs in memory.
 * - **Reloading an unloaded tab** shows its saved picture until the page has drawn
 *   (`showingSnapshot`, cleared in `onPageCommitVisible`), so there's no blank screen.
 * - **Corn in the background:** `pauseTimers()` stops every page's timers (it's global, which is
 *   exactly right here); `resumeTimers()` on return. Measured: 20 pings in 20 s on screen, 0 in the
 *   background, back to normal on return.
 * - When Android warns that memory is low, Corn unloads every kept-in-memory tab ([trimMemory]).
 * - A tab on Corn's home page has no WebView at all and costs nothing.
 * - **Split screen** ([split], [SplitState]): two tabs on screen at once. The current tab is the
 *   focused pane's tab (the pane last touched), so everything that acts on "the open tab" acts on
 *   it. Both panes' tabs are fully running and never unloaded while split (not even by
 *   [trimMemory]); the N kept-in-memory tabs above are counted besides those two. Showing another
 *   tab (Tabs screen, group strip, a new tab) replaces the focused pane's tab. Closing either
 *   tab, closing a pane, or a Flush ([reset]) ends split screen. Not saved across restarts.
 */
class TabManager(private val context: Context, private val host: TabHost) {
    /** How many recent tabs (besides the open one) stay in memory. Set by the controller from Settings. */
    var keepInMemory: () -> Int = { 2 }
    val tabs = mutableStateListOf<TabEntry>()
    val groups = mutableStateListOf<TabGroup>()
    var currentId by mutableIntStateOf(-1)
        private set

    val current: TabEntry? get() = tabs.firstOrNull { it.id == currentId }

    /** Split screen (null when off). Its focused tab is always [currentId]. */
    var split by mutableStateOf<SplitState?>(null)
        private set

    /** True for the open tab, and for both tabs while split. */
    fun isOnScreen(id: Int): Boolean = id == currentId || split?.contains(id) == true

    private fun onScreenIds(): Set<Int> = split?.tabIds ?: setOf(currentId)

    private var nextTabId = 1
    private var nextGroupId = 1
    private val recent = ArrayDeque<Int>() // most recently used first

    fun group(id: Int?): TabGroup? = groups.firstOrNull { it.id == id }
    fun groupTabs(groupId: Int): List<TabEntry> = tabs.filter { it.groupId == groupId }

    /** Open a tab. `inGroup` puts it in the current tab's group, creating the group if needed. */
    fun open(url: String, inGroup: Boolean = false, switchTo: Boolean = true): TabEntry {
        val cur = current
        var gid: Int? = null
        if (inGroup && cur != null) {
            if (cur.groupId == null) {
                // From an empty new tab, the group takes the first site's name later.
                val g = TabGroup(nextGroupId++, siteName(cur), C.GroupColors[groups.size % C.GroupColors.size], autoNamed = cur.isNewTab)
                groups += g
                cur.groupId = g.id
            }
            gid = cur.groupId
        }
        val entry = TabEntry(nextTabId++, url, gid)
        val at = cur?.let { tabs.indexOf(it) + 1 } ?: tabs.size
        tabs.add(at.coerceIn(0, tabs.size), entry)
        if (switchTo) {
            switchTo(entry.id)
        } else if (url != NEW_TAB) {
            entry.title = url
            // Opened in the background: start loading now (like Chrome), as the most recent tab after
            // the open one, so it's ready when tapped. Not when the user keeps 0 tabs in memory.
            if (keepInMemory() > 0) {
                newLive(entry).load(url)
                recent.remove(entry.id)
                recent.add(if (recent.firstOrNull() == currentId) 1.coerceAtMost(recent.size) else 0, entry.id)
                enforceLivePolicy()
            }
        }
        return entry
    }

    fun switchTo(id: Int) {
        val target = tabs.firstOrNull { it.id == id } ?: return
        current?.let { cur -> if (cur.id != id) cur.live?.capturePreview()?.let { cur.preview = it } }
        split?.let { split = it.show(id) } // in split screen: the tab goes into the focused pane
        currentId = id
        recent.remove(id)
        recent.addFirst(id)
        if (!target.isNewTab) ensureLive(target)
        enforceLivePolicy()
    }

    // ---------- split screen ----------

    /** Start split screen: the open tab goes on top (or left), tab [otherId] below (or right), focused. */
    fun startSplit(otherId: Int) {
        val cur = current ?: return
        if (tabs.none { it.id == otherId }) return
        split = SplitState.start(cur.id, otherId) ?: return
        switchTo(otherId)
    }

    /** Start split screen with a new tab (Corn's home page) in the second pane. */
    fun startSplitWithNewTab() {
        val entry = open(NEW_TAB, switchTo = false)
        startSplit(entry.id)
    }

    /** The user touched the pane showing tab [id]: it becomes the current tab. Both stay live. */
    fun focusPane(id: Int) {
        val s = split ?: return
        if (id !in s || id == currentId) return
        split = s.focus(id)
        currentId = id
        recent.remove(id)
        recent.addFirst(id)
    }

    /** Drag the divider ([raw]: the finger's position as a share of the space). */
    fun resizeSplit(raw: Float) {
        split = split?.resized(raw) ?: return
    }

    /** Leave split screen, keeping tab [keep] (default: the focused one) on screen. */
    fun exitSplit(keep: Int? = null) {
        val s = split ?: return
        split = null
        val k = keep?.takeIf { id -> tabs.any { it.id == id } } ?: s.focused
        if (k != currentId) switchTo(k) else enforceLivePolicy()
    }

    /** Close the pane showing tab [id] (not the tab): the other pane's tab fills the screen. */
    fun closePane(id: Int) {
        val stay = split?.remainingAfterClosing(id) ?: return
        exitSplit(stay)
    }

    /** A pop-up the site is allowed to open: becomes a new tab next to its opener. */
    fun openPopup(opener: TabEntry, resultMsg: android.os.Message): Boolean {
        val entry = TabEntry(nextTabId++, "about:blank", opener.groupId)
        tabs.add((tabs.indexOf(opener) + 1).coerceIn(0, tabs.size), entry)
        val live = newLive(entry)
        val transport = resultMsg.obj as? android.webkit.WebView.WebViewTransport ?: return false
        transport.webView = live.webView
        resultMsg.sendToTarget()
        switchTo(entry.id)
        return true
    }

    /** Load a URL in the current tab (creating its WebView if it's a new-tab page or unloaded). */
    fun load(url: String) {
        val cur = current ?: open(NEW_TAB)
        if (cur.isNewTab) {
            cur.openedFromHome = true   // Back from this page returns to Corn's home page
            cur.homeForwardUrl = null
        }
        cur.url = url
        cur.title = url
        group(cur.groupId)?.takeIf { it.autoNamed }?.let { g ->
            g.name = siteName(cur)
            g.autoNamed = false
        }
        val live = cur.live ?: newLive(cur)
        live.load(url)
        enforceLivePolicy()
    }

    fun close(id: Int) {
        val entry = tabs.firstOrNull { it.id == id } ?: return
        val idx = tabs.indexOf(entry)
        val gid = entry.groupId
        // Closing one of the split-screen tabs ends split screen; the other tab stays on screen.
        val stay = split?.remainingAfterClosing(id)
        if (stay != null) split = null
        entry.live?.destroy()
        entry.live = null
        tabs.remove(entry)
        recent.remove(id)
        if (gid != null) {
            val rest = groupTabs(gid)
            if (rest.size <= 1) {
                rest.forEach { it.groupId = null }
                groups.removeAll { it.id == gid }
            }
        }
        if (tabs.isEmpty()) {
            open(NEW_TAB)
            return
        }
        if (stay != null && tabs.any { it.id == stay }) {
            switchTo(stay)
            return
        }
        if (currentId == id) {
            val next = recent.firstOrNull { r -> tabs.any { it.id == r } }?.let { r -> tabs.first { it.id == r } }
                ?: tabs[idx.coerceAtMost(tabs.lastIndex)]
            switchTo(next.id)
        }
    }

    /** Throw away a tab's dead WebView; it reloads its URL when shown again. */
    fun dropCrashed(entry: TabEntry) {
        entry.live?.let { (it.webView.parent as? android.view.ViewGroup)?.removeView(it.webView); it.webView.destroy() }
        entry.live = null
        entry.saved = null
        entry.crashed = true
        entry.mediaPlaying = false
    }

    /** Reload a crashed tab. */
    fun revive(entry: TabEntry) {
        entry.crashed = false
        if (isOnScreen(entry.id)) ensureLive(entry)
    }

    /** Run something on every tab that currently has a WebView. */
    fun forEachLive(action: (CornTab) -> Unit) = tabs.forEach { t -> t.live?.let(action) }

    fun goBack() = current?.live?.webView?.takeIf { it.canGoBack() }?.goBack()

    fun goForward() {
        val cur = current ?: return
        val forward = cur.homeForwardUrl
        if (cur.isNewTab && forward != null) {
            load(forward)               // Forward from the home page reopens the page you left
            cur.homeForwardUrl = null
        } else {
            cur.live?.webView?.takeIf { it.canGoForward() }?.goForward()
        }
    }

    /** True when Back should return to Corn's home page (no earlier web page in this tab). */
    val backGoesHome: Boolean get() = current?.let { !it.isNewTab && !it.canGoBack && it.openedFromHome } ?: false

    /** Back from a tab's first page when it was opened from the home page: show the home page again. */
    fun goHome() {
        val cur = current ?: return
        cur.homeForwardUrl = cur.url
        cur.live?.destroy()
        cur.live = null
        cur.saved = null
        cur.url = NEW_TAB
        cur.title = "New tab"
        cur.openedFromHome = false
        cur.canGoBack = false
        cur.canGoForward = true
        cur.loading = false
    }
    fun reload() = current?.live?.webView?.reload()

    /** Grab a fresh preview of the current tab (before showing the tab switcher). */
    fun captureCurrentPreview() {
        tabs.filter { isOnScreen(it.id) }.forEach { t -> t.live?.capturePreview()?.let { t.preview = it } }
    }

    private fun ensureLive(entry: TabEntry): CornTab {
        entry.live?.let { return it }
        // Reloading an unloaded tab: show its saved picture until the page has drawn.
        entry.showingSnapshot = entry.preview != null
        val live = newLive(entry)
        val state = entry.saved
        if (state != null) {
            live.restore(state)
            entry.saved = null
        } else {
            live.load(entry.url)
        }
        live.restoreScrollAfterLoad(entry.savedScrollY)
        entry.savedScrollY = 0
        return live
    }

    private fun newLive(entry: TabEntry): CornTab = CornTab(context, entry, host).also { entry.live = it }

    /** Apply the rules above: the open tab (both in split screen) runs, the N most recent stay in memory, the rest are unloaded. */
    fun enforceLivePolicy() {
        val onScreen = onScreenIds()
        val keep = tabsKeptInMemory(recent.toList(), onScreen, { id -> tabs.any { it.id == id && it.live != null } }, keepInMemory())
        for (t in tabs) {
            val live = t.live ?: continue
            when {
                t.id in onScreen -> live.bringToForeground()
                t.id in keep -> live.keepInBackground()
                else -> unload(t)
            }
        }
    }

    /** Android says memory is low: unload every tab except the open one (both in split screen). */
    fun trimMemory() {
        tabs.filter { !isOnScreen(it.id) }.forEach { unload(it) }
    }

    /** Corn left the screen: stop every page's timers (Android's global pause), resumed on return. */
    fun pauseAllTimers() = tabs.firstNotNullOfOrNull { it.live }?.webView?.pauseTimers()

    fun resumeAllTimers() = tabs.firstNotNullOfOrNull { it.live }?.webView?.resumeTimers()

    /** Rebuild every page (theme change): unload all, then reload the open tab right away. */
    fun recreateAll() {
        tabs.forEach { unload(it) }
        tabs.filter { isOnScreen(it.id) && !it.isNewTab }.forEach { ensureLive(it) }
    }

    /** Unload every tab, including the open one (before deleting cookies). The open tab reloads when shown. */
    fun unloadAll() {
        tabs.forEach { unload(it) }
    }

    private fun unload(t: TabEntry) {
        val live = t.live ?: return
        if (t.preview == null) live.capturePreview()?.let { t.preview = it }
        t.savedScrollY = live.webView.scrollY
        t.saved = live.saveState()
        live.destroy()
        t.live = null
        t.mediaPlaying = false
    }

    val anyMediaPlaying: Boolean get() = tabs.any { it.mediaPlaying }

    /** Destroy every tab and group (used by Flush). Leaves one fresh new-tab page. */
    fun reset() {
        split = null
        tabs.forEach { it.live?.destroy(); it.live = null }
        tabs.clear()
        groups.clear()
        recent.clear()
        open(NEW_TAB)
    }

    fun destroyAll() {
        tabs.forEach { it.live?.destroy(); it.live = null }
    }

    // ---------- Kept Session ----------

    fun toJson(): String {
        val gs = JSONArray()
        groups.forEach { g -> gs.put(JSONObject().put("id", g.id).put("name", g.name).put("color", C.GroupColors.indexOf(g.color))) }
        val ts = JSONArray()
        tabs.forEach { t -> ts.put(JSONObject().put("url", t.url).put("title", t.title).put("group", t.groupId ?: -1)) }
        return JSONObject().put("groups", gs).put("tabs", ts).put("current", tabs.indexOfFirst { it.id == currentId }).toString()
    }

    /** Restore saved tabs as unloaded entries; only the current one loads. Returns false if nothing was restored. */
    fun restore(json: String): Boolean {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return false
        val ts = o.optJSONArray("tabs") ?: return false
        if (ts.length() == 0) return false
        val gs = o.optJSONArray("groups") ?: JSONArray()
        val groupMap = mutableMapOf<Int, Int>()
        for (i in 0 until gs.length()) {
            val g = gs.getJSONObject(i)
            val color = C.GroupColors.getOrElse(g.optInt("color")) { C.GroupColors[0] }
            val ng = TabGroup(nextGroupId++, g.optString("name", "Group"), color)
            groups += ng
            groupMap[g.optInt("id")] = ng.id
        }
        for (i in 0 until ts.length()) {
            val t = ts.getJSONObject(i)
            val e = TabEntry(nextTabId++, t.optString("url", NEW_TAB), groupMap[t.optInt("group", -1)])
            e.title = t.optString("title", e.url)
            tabs += e
        }
        val cur = o.optInt("current", 0).coerceIn(0, tabs.lastIndex)
        switchTo(tabs[cur].id)
        return true
    }

    private fun siteName(t: TabEntry): String =
        if (t.isNewTab) "Group ${groups.size + 1}"
        else t.host.substringBeforeLast('.').substringAfterLast('.').replaceFirstChar { it.uppercase() }.ifBlank { t.host }
}
