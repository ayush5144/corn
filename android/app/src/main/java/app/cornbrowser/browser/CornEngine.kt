/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.content.Context
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
 * changing the engine itself. (The lists overlap a little: EasyList has a few
 * tracker rules and EasyPrivacy a few ad rules.)
 *
 * First launch downloads the lists and caches the built engines; later launches load
 * the cache in milliseconds. Lists are fetched as data; nothing about browsing is sent.
 */
object CornEngine {
    private const val TAG = "CornEngine"

    private class Source(val name: String, val cache: String, val lists: List<String>)

    private val ADS = Source("ads", "ads-v1.dat", listOf("https://easylist.to/easylist/easylist.txt"))
    private val TRACKERS = Source("trackers", "trackers-v1.dat", listOf("https://easylist.to/easylist/easyprivacy.txt"))

    @Volatile var ads: CornBlocker? = null
        private set
    @Volatile var trackers: CornBlocker? = null
        private set

    @Volatile private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        File(context.filesDir, "engine-v1.dat").delete() // the spike's combined engine, no longer used
        Thread({
            try {
                ads = load(context, ADS)
                trackers = load(context, TRACKERS)
            } catch (e: Exception) {
                Log.e(TAG, "engine failed", e)
                started = false
            }
        }, "corn-engine").start()
    }

    private fun load(context: Context, src: Source): CornBlocker {
        val cache = File(context.filesDir, src.cache)
        if (cache.exists()) {
            runCatching { return CornBlocker.fromSerialized(cache.readBytes()) }
                .onFailure { Log.w(TAG, "cached ${src.name} engine unreadable, rebuilding", it); cache.delete() }
        }
        val texts = src.lists.map { download(it) }
        val built = CornBlocker.fromLists(texts)
        cache.writeBytes(built.serialize())
        Log.i(TAG, "built ${src.name} engine from ${texts.sumOf { it.lines().size }} lines")
        return built
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
