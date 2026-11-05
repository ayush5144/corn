/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import uniffi.corn_ffi.CornBlocker
import uniffi.corn_ffi.FilterList
import uniffi.corn_ffi.assembleScriptlets
import uniffi.corn_ffi.filterListIncludes
import uniffi.corn_ffi.scriptletModuleImports

/**
 * Owns Corn's blocking engines: Brave's adblock-rust, unmodified, loaded twice:
 * - **ads**: EasyList + uBlock Origin's filters (also supply element hiding and scriptlets)
 * - **trackers**: EasyPrivacy
 * Two engines let each site turn ads and trackers on or off separately without
 * changing the engine itself. (The lists overlap a little.)
 *
 * **Scriptlets** (`+js(...)` rules, e.g. the ones that strip YouTube's ad schedule) need their
 * code, which uBlock Origin publishes as source modules. Corn downloads those unmodified, the Rust
 * core assembles them into adblock-rust resources (as Brave's build does), and they're stored
 * next to the engines in [SCRIPTLETS_CACHE]. Which scriptlet runs on which site is decided only
 * by the downloaded lists; [ScriptletInjector] runs them.
 *
 * Where the rules come from, in order:
 * 1. the cached engines from the last download (`ads-v2.dat`, `trackers-v1.dat`)
 * 2. the previous version's cache (`ads-v1.dat`, EasyList only) until the first download
 * 3. the **offline starter list** built into the app at build time (assets/starter/; EasyList
 *    and EasyPrivacy only: uBO's lists are GPL-only, so they're never shipped inside the app)
 * 4. a fresh download, then cached
 *
 * **Refresh:** every [REFRESH_AFTER_MS] on an unmetered network (Wi-Fi), lists are downloaded
 * again, sanity-checked, built in the background and swapped in. The first download after
 * install (or after the list set changes) uses any network, because the starter list may be weeks
 * old and has no scriptlets. If anything fails, the last good engines and scriptlets stay in use.
 * Downloads are plain file fetches; nothing about browsing is sent.
 */
object CornEngine {
    private const val TAG = "CornEngine"
    private const val REFRESH_AFTER_MS = 4L * 24 * 60 * 60 * 1000 // 4 days
    private const val SCRIPTLETS_CACHE = "scriptlets-v1.json"
    private const val MAX_INCLUDES = 40
    private const val MAX_MODULES = 80

    /** One list to download. [minLines] rejects truncated or wrong files; [includes] follows `!#include`. */
    private class ListUrl(val url: String, val minLines: Int, val trusted: Boolean = false, val includes: Boolean = false)

    private class Source(
        val name: String,
        val cache: String,
        val legacyCache: String?,
        val starter: String,
        val lists: List<ListUrl>,
        val scriptlets: Boolean,
    )

    private const val UBO_LISTS = "https://ublockorigin.github.io/uAssets/filters/"
    private val ADS = Source(
        "ads", "ads-v2.dat", "ads-v1.dat", "starter/ads.dat",
        listOf(
            ListUrl("https://easylist.to/easylist/easylist.txt", 10_000),
            // uBlock Origin's lists carry the scriptlet rules (YouTube's among them). Trusted: they
            // may use uBO's privileged trusted-* scriptlets, as in uBO and Brave.
            ListUrl(UBO_LISTS + "filters.txt", 5_000, trusted = true, includes = true),
            ListUrl(UBO_LISTS + "quick-fixes.txt", 50, trusted = true),
            ListUrl(UBO_LISTS + "unbreak.txt", 500, trusted = true),
        ),
        scriptlets = true,
    )
    private val TRACKERS = Source(
        "trackers", "trackers-v1.dat", null, "starter/trackers.dat",
        listOf(ListUrl("https://easylist.to/easylist/easyprivacy.txt", 10_000)),
        scriptlets = false,
    )

    /** uBlock Origin's scriptlet sources: latest release, files under src/js/. */
    private const val UBO_LATEST = "https://github.com/gorhill/uBlock/releases/latest"
    private const val UBO_SRC = "https://raw.githubusercontent.com/gorhill/uBlock/%s/src/js/"
    private const val UBO_ENTRY = "resources/scriptlets.js"

    @Volatile var ads: CornBlocker? = null
        private set
    @Volatile var trackers: CornBlocker? = null
        private set

    @Volatile private var started = false
    @Volatile private var refreshing = false

    /** Load engines (cache, else starter), then refresh in the background if they're stale. */
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        File(app.filesDir, "engine-v1.dat").delete() // the spike's combined engine, no longer used
        Thread({
            val scriptlets = readScriptlets(app)
            ads = loadLocal(app, ADS, scriptlets)
            trackers = loadLocal(app, TRACKERS, null)
            refreshIfStale(app)
        }, "corn-engine").start()
    }

    /** Called when Corn comes to the foreground; cheap when nothing is due. */
    fun refreshIfStale(context: Context) {
        val app = context.applicationContext
        val last = lastUpdated(app)
        // A missing cache (new install, or a new list set like ads-v2) counts as a first download.
        val first = last == 0L || listOf(ADS, TRACKERS).any { !File(app.filesDir, it.cache).exists() }
        val due = first || System.currentTimeMillis() - last >= REFRESH_AFTER_MS
        if (!due || refreshing) return
        val net = network(app) ?: return               // offline: try again next time
        if (!first && net.metered) return              // routine refreshes wait for Wi-Fi
        refreshing = true
        Thread({
            try {
                // Scriptlets first; if they fail, the last good set is used with the new lists.
                val scriptlets = runCatching { downloadScriptlets(app) }
                    .onFailure { Log.w(TAG, "scriptlets refresh failed; keeping the last good set", it) }
                    .getOrNull() ?: readScriptlets(app)
                val newAds = download(app, ADS, scriptlets)
                val newTrackers = download(app, TRACKERS, null)
                ads = newAds
                trackers = newTrackers
                setLastUpdated(app, System.currentTimeMillis())
                Log.i(TAG, "filter lists refreshed")
            } catch (e: Exception) {
                Log.w(TAG, "refresh failed; keeping the current lists", e)
            } finally {
                refreshing = false
            }
        }, "corn-engine-refresh").start()
    }

    /** The cached engine from the last download, else the previous version's cache, else the starter list. */
    private fun loadLocal(context: Context, src: Source, scriptlets: String?): CornBlocker? {
        for (name in listOfNotNull(src.cache, src.legacyCache)) {
            val cache = File(context.filesDir, name)
            if (!cache.exists()) continue
            runCatching { return CornBlocker.fromSerializedWithScriptlets(cache.readBytes(), scriptlets) }
                .onFailure { Log.w(TAG, "cached ${src.name} engine unreadable", it); cache.delete(); setLastUpdated(context, 0L) }
        }
        return runCatching { context.assets.open(src.starter).use { CornBlocker.fromSerializedWithScriptlets(it.readBytes(), scriptlets) } }
            .onSuccess { Log.i(TAG, "using the built-in starter ${src.name} list") }
            .onFailure { Log.w(TAG, "no starter ${src.name} list in this build") }
            .getOrNull()
    }

    /** Download, sanity-check, build and cache one engine. Throws on any problem (caller keeps the old one). */
    private fun download(context: Context, src: Source, scriptlets: String?): CornBlocker {
        val lists = mutableListOf<FilterList>()
        for (l in src.lists) {
            val text = fetch(l.url)
            val lines = text.lines().size
            check(lines >= l.minLines) { "${src.name} list too small ($lines lines)" }
            lists += FilterList(text, l.trusted)
            if (l.includes) lists += fetchIncludes(l, text)
        }
        val built = CornBlocker.fromFilterLists(lists, if (src.scriptlets) scriptlets else null)
        val tmp = File(context.filesDir, src.cache + ".tmp")
        tmp.writeBytes(built.serialize())
        check(tmp.renameTo(File(context.filesDir, src.cache))) { "could not save ${src.name} engine" }
        src.legacyCache?.let { File(context.filesDir, it).delete() }
        return built
    }

    /** Files a list `!#include`s (plain names next to it; the Rust core filters anything else), and theirs. */
    private fun fetchIncludes(list: ListUrl, text: String): List<FilterList> {
        val out = mutableListOf<FilterList>()
        val seen = mutableSetOf<String>()
        var pending = filterListIncludes(text)
        while (pending.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (name in pending) {
                if (!seen.add(name)) continue
                check(seen.size <= MAX_INCLUDES) { "too many included lists" }
                val inc = fetch(URL(URL(list.url), name).toString())
                out += FilterList(inc, list.trusted)
                next += filterListIncludes(inc)
            }
            pending = next
        }
        return out
    }

    // ---------------- Scriptlets ----------------

    /** The stored scriptlet set, or null if there's none yet. */
    private fun readScriptlets(context: Context): String? =
        File(context.filesDir, SCRIPTLETS_CACHE).takeIf { it.exists() }?.readText()

    /**
     * Download uBlock Origin's scriptlet modules (its latest release, unmodified), assemble them in
     * the Rust core and store the result. Throws on any problem; the stored set stays as it was.
     */
    private fun downloadScriptlets(context: Context): String {
        val base = UBO_SRC.format(latestUboRelease())
        val sources = mutableListOf<String>()
        val seen = mutableSetOf(UBO_ENTRY)
        val queue = ArrayDeque(listOf(UBO_ENTRY))
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val text = fetch(base + path)
            sources += text
            for (spec in scriptletModuleImports(text)) {
                val dep = resolveModule(path, spec) ?: continue
                if (seen.add(dep)) {
                    check(seen.size <= MAX_MODULES) { "too many scriptlet modules" }
                    queue.addLast(dep)
                }
            }
        }
        val set = assembleScriptlets(sources) // checks it looks like a complete uBO release
        val tmp = File(context.filesDir, "$SCRIPTLETS_CACHE.tmp")
        tmp.writeText(set.json)
        check(tmp.renameTo(File(context.filesDir, SCRIPTLETS_CACHE))) { "could not save scriptlets" }
        Log.i(TAG, "scriptlets refreshed: ${set.count}")
        return set.json
    }

    /** `./x.js` / `../y.js` relative to [from], kept inside uBO's src/js folder; null if it would leave it. */
    private fun resolveModule(from: String, spec: String): String? {
        val parts = from.split('/').dropLast(1).toMutableList()
        for (seg in spec.split('/')) {
            when (seg) {
                ".", "" -> {}
                ".." -> if (parts.isEmpty()) return null else parts.removeAt(parts.size - 1)
                else -> {
                    if (!seg.matches(Regex("[A-Za-z0-9_.-]+"))) return null
                    parts += seg
                }
            }
        }
        return parts.joinToString("/")
    }

    /** The tag of uBlock Origin's latest release, read from GitHub's redirect (no API call). */
    private fun latestUboRelease(): String {
        val conn = URL(UBO_LATEST).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            val location = conn.getHeaderField("Location") ?: error("no release redirect (HTTP ${conn.responseCode})")
            location.substringAfterLast("/tag/").also {
                check(it.matches(Regex("[0-9A-Za-z._-]{1,40}"))) { "unexpected release tag" }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun fetch(url: String): String {
        check(url.startsWith("https://")) { "HTTPS only" }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        return try {
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private class Net(val metered: Boolean)

    private fun network(context: Context): Net? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return null
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null
        return Net(metered = cm.isActiveNetworkMetered)
    }

    private fun prefs(context: Context) = context.getSharedPreferences("corn_engine", Context.MODE_PRIVATE)
    private fun lastUpdated(context: Context) = prefs(context).getLong("last_updated", 0L)
    private fun setLastUpdated(context: Context, t: Long) = prefs(context).edit().putLong("last_updated", t).apply()
}
