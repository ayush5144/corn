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

/**
 * Owns Corn's blocking engines: Brave's adblock-rust, unmodified, loaded twice:
 * - **ads**: EasyList (also supplies element hiding)
 * - **trackers**: EasyPrivacy
 * Two engines let each site turn ads and trackers on or off separately without
 * changing the engine itself. (The lists overlap a little.)
 *
 * Where the rules come from, in order:
 * 1. the cached engines from the last download (`ads-v1.dat`, `trackers-v1.dat`)
 * 2. the **offline starter list** built into the app at build time (assets/starter/)
 * 3. a fresh download, then cached
 *
 * **Refresh:** every [REFRESH_AFTER_MS] on an unmetered network (Wi-Fi), lists are downloaded
 * again, sanity-checked, built in the background and swapped in. The first download after
 * install uses any network, because the starter list may be weeks old. If anything fails, the
 * last good engines stay in use. Downloads are plain file fetches; nothing about browsing is sent.
 */
object CornEngine {
    private const val TAG = "CornEngine"
    private const val REFRESH_AFTER_MS = 4L * 24 * 60 * 60 * 1000 // 4 days
    private const val MIN_LINES = 10_000                         // a real list has tens of thousands

    private class Source(val name: String, val cache: String, val starter: String, val lists: List<String>)

    private val ADS = Source("ads", "ads-v1.dat", "starter/ads.dat", listOf("https://easylist.to/easylist/easylist.txt"))
    private val TRACKERS = Source("trackers", "trackers-v1.dat", "starter/trackers.dat", listOf("https://easylist.to/easylist/easyprivacy.txt"))

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
            ads = loadLocal(app, ADS)
            trackers = loadLocal(app, TRACKERS)
            refreshIfStale(app)
        }, "corn-engine").start()
    }

    /** Called when Corn comes to the foreground; cheap when nothing is due. */
    fun refreshIfStale(context: Context) {
        val app = context.applicationContext
        val last = lastUpdated(app)
        val due = last == 0L || System.currentTimeMillis() - last >= REFRESH_AFTER_MS
        if (!due || refreshing) return
        val net = network(app) ?: return               // offline: try again next time
        if (last != 0L && net.metered) return          // routine refreshes wait for Wi-Fi
        refreshing = true
        Thread({
            try {
                val newAds = download(app, ADS)
                val newTrackers = download(app, TRACKERS)
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

    /** The cached engine from the last download, or the starter list built into the app. */
    private fun loadLocal(context: Context, src: Source): CornBlocker? {
        val cache = File(context.filesDir, src.cache)
        if (cache.exists()) {
            runCatching { return CornBlocker.fromSerialized(cache.readBytes()) }
                .onFailure { Log.w(TAG, "cached ${src.name} engine unreadable", it); cache.delete(); setLastUpdated(context, 0L) }
        }
        return runCatching { context.assets.open(src.starter).use { CornBlocker.fromSerialized(it.readBytes()) } }
            .onSuccess { Log.i(TAG, "using the built-in starter ${src.name} list") }
            .onFailure { Log.w(TAG, "no starter ${src.name} list in this build") }
            .getOrNull()
    }

    /** Download, sanity-check, build and cache one engine. Throws on any problem (caller keeps the old one). */
    private fun download(context: Context, src: Source): CornBlocker {
        val texts = src.lists.map { fetch(it) }
        val lines = texts.sumOf { it.lines().size }
        check(lines >= MIN_LINES) { "${src.name} list too small ($lines lines)" }
        val built = CornBlocker.fromLists(texts)
        val tmp = File(context.filesDir, src.cache + ".tmp")
        tmp.writeBytes(built.serialize())
        check(tmp.renameTo(File(context.filesDir, src.cache))) { "could not save ${src.name} engine" }
        return built
    }

    private fun fetch(url: String): String {
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
