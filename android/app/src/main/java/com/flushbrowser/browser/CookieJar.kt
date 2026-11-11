/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.browser

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.webkit.CookieManager
import java.io.File

/**
 * Precise cookie deletion by editing WebView's own cookie database, the same method
 * DuckDuckGo's Android browser uses (`SQLCookieRemover`).
 *
 * Why: Android's official CookieManager can only delete *every* cookie
 * (`removeAllCookies`); it cannot skip Keep List sites. Flush uses this file edit for one
 * job only: deleting every cookie **except Keep List sites'**, exactly (with all their
 * attributes intact). Per-site or time-range deletion is deliberately not offered.
 *
 * How: WebView (Chromium) keeps cookies in an SQLite file with a `cookies` table
 * keyed by `host_key`. We flush pending
 * cookies to disk, delete matching rows, and VACUUM so deleted values don't linger in
 * the file. WebView also caches cookies in memory, so **after an edit the app process
 * must restart** (see RestartActivity); DuckDuckGo does the same (`killAndRestartProcess`).
 *
 * If the file can't be found or opened (a future WebView could change it), every function
 * returns `false` and callers fall back to the official API.
 */
object CookieJar {
    private const val TAG = "CookieJar"

    /** Where WebView keeps its cookie database (same locations DuckDuckGo checks). */
    private val KNOWN_LOCATIONS = listOf("app_webview/Default/Cookies", "app_webview/Cookies")

    private fun file(context: Context): File? =
        KNOWN_LOCATIONS.map { File(context.applicationInfo.dataDir, it) }.firstOrNull { it.exists() }

    /**
     * Delete every cookie except Keep List sites' (and their subdomains').
     * Returns true if the database was edited: the caller must then restart the process.
     */
    fun deleteAllExcept(context: Context, keep: List<String>): Boolean {
        CookieManager.getInstance().flush() // write any cookies still in memory to the file first
        val db = open(context) ?: return false
        return try {
            if (!hasCookiesTable(db)) return false
            val args = mutableListOf<String>()
            val where = keep.joinToString(" AND ") { h -> "NOT ${hostClause(h, args)}" }.ifEmpty { null }
            val deleted = db.delete("cookies", where, args.toTypedArray())
            db.execSQL("VACUUM") // physically remove deleted values from the file
            Log.i(TAG, "deleted $deleted cookies") // a count only, never hosts or names
            true
        } catch (e: Exception) {
            Log.w(TAG, "cookie database edit failed; caller falls back", e)
            false
        } finally {
            db.close()
        }
    }

    /** `host_key` matches a site and its subdomains: `example.com`, `.example.com`, `x.example.com`. */
    private fun hostClause(host: String, args: MutableList<String>): String {
        val h = host.removePrefix(".").removePrefix("www.")
        args += h; args += ".$h"; args += "%.$h"
        return "(host_key = ? OR host_key = ? OR host_key LIKE ?)"
    }

    private fun open(context: Context): SQLiteDatabase? {
        val f = file(context) ?: return null
        return runCatching {
            SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READWRITE)
        }.onFailure { Log.w(TAG, "cookie database unavailable", it) }.getOrNull()
    }

    private fun hasCookiesTable(db: SQLiteDatabase): Boolean =
        db.rawQuery("PRAGMA table_info('cookies')", null).use { it.count > 0 }
}
