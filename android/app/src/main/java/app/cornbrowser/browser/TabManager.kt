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
    /** Set when the page process died; the page shows a reload card. */
    var crashed by mutableStateOf(false)
    internal var live: CornTab? = null
    internal var saved: Bundle? = null
}

/**
 * Owns every tab and Tab Group, and the live-tab rule:
 * **1 active + up to 2 paused** (in memory, media stopped); every other tab is
 * **frozen** (preview + saved state, no WebView) and reloads when opened.
 * See docs/01-product/02-decision-log.md, "How many tabs stay live".
 */
class TabManager(private val context: Context, private val host: TabHost) {
    val tabs = mutableStateListOf<TabEntry>()
    val groups = mutableStateListOf<TabGroup>()
    var currentId by mutableIntStateOf(-1)
        private set

    val current: TabEntry? get() = tabs.firstOrNull { it.id == currentId }

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
                val g = TabGroup(nextGroupId++, siteName(cur), C.GroupColors[groups.size % C.GroupColors.size])
                groups += g
                cur.groupId = g.id
            }
            gid = cur.groupId
        }
        val entry = TabEntry(nextTabId++, url, gid)
        val at = cur?.let { tabs.indexOf(it) + 1 } ?: tabs.size
        tabs.add(at.coerceIn(0, tabs.size), entry)
        if (switchTo) switchTo(entry.id) else if (url != NEW_TAB) entry.title = url
        return entry
    }

    fun switchTo(id: Int) {
        val target = tabs.firstOrNull { it.id == id } ?: return
        current?.let { cur -> if (cur.id != id) cur.live?.capturePreview()?.let { cur.preview = it } }
        currentId = id
        recent.remove(id)
        recent.addFirst(id)
        if (!target.isNewTab) ensureLive(target)
        enforceLivePolicy()
    }

    /** Start a new Tab Group holding one new tab. The group takes the first site's name. */
    fun openNewGroup(): TabEntry {
        val g = TabGroup(nextGroupId++, "Group ${groups.size + 1}", C.GroupColors[groups.size % C.GroupColors.size], autoNamed = true)
        groups += g
        val entry = TabEntry(nextTabId++, NEW_TAB, g.id)
        val at = current?.let { tabs.indexOf(it) + 1 } ?: tabs.size
        tabs.add(at.coerceIn(0, tabs.size), entry)
        switchTo(entry.id)
        return entry
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

    /** Load a URL in the current tab (creating its WebView if it's a new-tab page or frozen). */
    fun load(url: String) {
        val cur = current ?: open(NEW_TAB)
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
        if (entry.id == currentId) ensureLive(entry)
    }

    fun goBack() = current?.live?.webView?.takeIf { it.canGoBack() }?.goBack()
    fun goForward() = current?.live?.webView?.takeIf { it.canGoForward() }?.goForward()
    fun reload() = current?.live?.webView?.reload()

    /** Grab a fresh preview of the current tab (before showing the tab switcher). */
    fun captureCurrentPreview() {
        current?.let { cur -> cur.live?.capturePreview()?.let { cur.preview = it } }
    }

    private fun ensureLive(entry: TabEntry): CornTab {
        entry.live?.let { return it }
        val live = newLive(entry)
        val state = entry.saved
        if (state != null) {
            live.restore(state)
            entry.saved = null
        } else {
            live.load(entry.url)
        }
        return live
    }

    private fun newLive(entry: TabEntry): CornTab = CornTab(context, entry, host).also { entry.live = it }

    /** Active tab runs; the next two most recent stay in memory paused; everything else is frozen. */
    fun enforceLivePolicy() {
        val keep = mutableListOf<Int>()
        keep += currentId
        for (id in recent) {
            if (keep.size >= 1 + PAUSED_TABS) break
            if (id != currentId && tabs.any { it.id == id && it.live != null }) keep += id
        }
        for (t in tabs) {
            val live = t.live ?: continue
            when {
                t.id == currentId -> live.resume()
                t.id in keep -> live.pause()
                else -> freeze(t)
            }
        }
    }

    private fun freeze(t: TabEntry) {
        val live = t.live ?: return
        if (t.preview == null) live.capturePreview()?.let { t.preview = it }
        t.saved = live.saveState()
        live.destroy()
        t.live = null
        t.mediaPlaying = false
    }

    val anyMediaPlaying: Boolean get() = tabs.any { it.mediaPlaying }

    /** Destroy every tab and group (used by Flush). Leaves one fresh new-tab page. */
    fun reset() {
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

    /** Restore saved tabs as frozen entries; only the current one loads. Returns false if nothing was restored. */
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

    companion object {
        const val PAUSED_TABS = 2
    }
}
