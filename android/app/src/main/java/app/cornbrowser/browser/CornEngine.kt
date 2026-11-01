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
 * Owns the single shared blocking engine (Brave's adblock-rust, via Corn's Rust core).
 *
 * First launch: downloads the filter lists, builds the engine, caches it to disk.
 * Later launches: loads the cached engine in a few milliseconds.
 * Filter lists are fetched as data on the device; nothing about browsing is sent.
 */
object CornEngine {
    private const val TAG = "CornEngine"
    private const val CACHE = "engine-v1.dat"

    val LISTS = listOf(
        "https://easylist.to/easylist/easylist.txt",
        "https://easylist.to/easylist/easyprivacy.txt",
    )

    @Volatile var blocker: CornBlocker? = null
        private set

    @Volatile var status: String = "Loading filters…"
        private set

    private var listeners = mutableListOf<() -> Unit>()

    fun onChange(listener: () -> Unit) { synchronized(this) { listeners += listener } }

    private fun publish(newStatus: String) {
        status = newStatus
        val copy = synchronized(this) { listeners.toList() }
        copy.forEach { it() }
    }

    fun start(context: Context) {
        if (blocker != null) return
        val cache = File(context.filesDir, CACHE)
        Thread({
            try {
                val t0 = System.nanoTime()
                if (cache.exists()) {
                    blocker = CornBlocker.fromSerialized(cache.readBytes())
                    publish("Filters ready · cached load ${(System.nanoTime() - t0) / 1_000_000} ms")
                    return@Thread
                }
                publish("Downloading filter lists…")
                val texts = LISTS.map { download(it) }
                val t1 = System.nanoTime()
                val built = CornBlocker.fromLists(texts)
                val buildMs = (System.nanoTime() - t1) / 1_000_000
                cache.writeBytes(built.serialize())
                blocker = built
                val rules = texts.sumOf { it.lines().size }
                publish("Filters ready · $rules rules · built in $buildMs ms")
            } catch (e: Exception) {
                Log.e(TAG, "engine failed", e)
                cache.delete()
                publish("Filters failed: ${e.message}")
            }
        }, "corn-engine").start()
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
