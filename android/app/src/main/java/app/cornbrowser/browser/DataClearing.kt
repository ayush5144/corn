/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import app.cornbrowser.data.CornData

/** What "Delete browsing data" (or Flush) removes. Always everything in each chosen category: no time ranges. */
data class ClearChoice(
    val history: Boolean,
    /** Cookies and IndexedDB (except Keep List sites'), all other site storage, site permissions and HTTP logins. */
    val cookiesAndSiteData: Boolean,
    val cache: Boolean,
    val tabs: Boolean,
) {
    companion object {
        val EVERYTHING = ClearChoice(history = true, cookiesAndSiteData = true, cache = true, tabs = true)
    }
}

/**
 * The single, complete procedure behind Flush, Auto-Flush and "Delete browsing data".
 *
 * Keep List sites keep their cookies and IndexedDB (they stay logged in); everything else in each
 * chosen category is deleted, including Keep List sites' local storage and history.
 *
 * Cookies are deleted with [CookieJar] (DuckDuckGo's method), which requires restarting the app
 * process afterwards; `done(true)` means the caller must restart. If the cookie file can't be
 * edited, the official CookieManager is used instead (`done(false)`, no restart needed).
 */
object DataClearing {

    fun clear(context: Context, tabs: TabManager, data: CornData, what: ClearChoice, done: (needsRestart: Boolean) -> Unit) {
        if (what.tabs) {
            tabs.reset()           // closes every tab and group, leaves one new-tab page
            data.clearSession()
        } else if (what.cookiesAndSiteData) {
            tabs.unloadAll()       // no WebView may keep stale cookies in memory; tabs reload after
        }
        if (what.history) data.clearHistory()
        if (what.cache) {
            // Android: "the cache is per-application, so this will clear the cache for all WebViews".
            WebView(context).apply { clearCache(true); destroy() }
        }
        if (!what.cookiesAndSiteData) {
            done(false)
            return
        }
        val keep = data.keepList()
        WebView(context).apply { clearFormData(); destroy() }
        WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
        GeolocationPermissions.getInstance().clearAll()

        val edited = CookieJar.deleteAllExcept(context, keep)
        if (edited) {
            // A restart follows, so deleting the files directly is complete: nothing in memory
            // writes them back. Keep List sites keep their IndexedDB folders.
            deleteProfileFiles(context, keep)
            done(true)
        } else {
            // No restart possible: use the official API, which deletes every site's storage.
            WebStorage.getInstance().deleteAllData()
            fallbackDeleteCookies(keep) { done(false) }
        }
    }

    /**
     * Delete WebView's site storage files (local storage, IndexedDB, Service Worker caches, Web SQL,
     * session storage, form data…): everything in `app_webview/Default` except
     * - the cookie file, already cleaned exactly by [CookieJar];
     * - Keep List sites' IndexedDB folders. WebView keeps one folder per site, named
     *   `<scheme>_<host>_<port>.indexeddb.leveldb` (and `.indexeddb.blob`); DuckDuckGo spares
     *   Fireproof sites' folders the same way.
     *
     * Why files and not `WebStorage.deleteAllData()`: that call has no per-site option, and it finishes
     * in the background with no "done" signal (measured: local storage was still on disk after the
     * restart). DuckDuckGo also deletes its WebView folder directly before restarting.
     */
    private fun deleteProfileFiles(context: Context, keep: List<String>) {
        val profile = java.io.File(context.applicationInfo.dataDir, "app_webview/Default")
        var removed = 0
        var keptSites = 0
        profile.listFiles()?.forEach { entry ->
            when (entry.name) {
                "Cookies", "Cookies-journal" -> Unit
                "IndexedDB" -> entry.listFiles()?.forEach { site ->
                    if (isKept(site.name.split('_').getOrNull(1), keep)) keptSites++
                    else if (site.deleteRecursively()) removed++
                }
                else -> if (entry.deleteRecursively()) removed++
            }
        }
        android.util.Log.i("DataClearing", "deleted $removed storage entries, kept $keptSites IndexedDB folders") // counts only
    }

    private fun isKept(host: String?, keep: List<String>): Boolean {
        val h = host?.removePrefix("www.") ?: return false
        return keep.any { k -> h == k || h.endsWith(".$k") }
    }

    /**
     * Official-API fallback: delete all cookies, then put Keep List cookies back.
     * Less exact (CookieManager can't read expiry or HttpOnly flags), used only if the file edit fails.
     */
    private fun fallbackDeleteCookies(keep: List<String>, then: () -> Unit) {
        val cm = CookieManager.getInstance()
        val kept = keep.associateWith { host ->
            listOf("https://$host", "https://www.$host").mapNotNull { cm.getCookie(it) }.joinToString("; ")
        }
        cm.removeAllCookies {
            for ((host, cookies) in kept) {
                cookies.split(";").map { it.trim() }.filter { it.contains('=') }.distinct().forEach { c ->
                    cm.setCookie("https://$host", "$c; Domain=$host; Path=/; Secure; Max-Age=34560000; SameSite=Lax")
                }
            }
            cm.flush()
            then()
        }
    }
}
