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
    /** Cookies (except Keep List sites'), all site storage, saved site permissions and HTTP logins. */
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
 * Keep List sites keep their cookies (they stay logged in); everything else in each chosen
 * category is deleted, including Keep List sites' site storage and history.
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
        deleteSiteStorage {
            if (edited) {
                deleteProfileFiles(context) // the process restarts right after, so nothing writes them back
                done(true)
            } else {
                fallbackDeleteCookies(keep) { done(false) }
            }
        }
    }

    /**
     * `deleteAllData()` works in the background with no "done" signal, and measured on the emulator,
     * local storage was still on disk after the restart that follows. So before restarting, also
     * delete WebView's site-storage files directly (DuckDuckGo deletes its WebView folder the same
     * way, keeping the cookie file). Kept: the cookie file, already cleaned exactly by [CookieJar].
     */
    private fun deleteProfileFiles(context: Context) {
        val profile = java.io.File(context.applicationInfo.dataDir, "app_webview/Default")
        val keep = setOf("Cookies", "Cookies-journal")
        var removed = 0
        profile.listFiles()?.filter { it.name !in keep }?.forEach { if (it.deleteRecursively()) removed++ }
        android.util.Log.i("DataClearing", "deleted $removed WebView storage entries") // a count only
    }

    /**
     * Site storage (local storage, IndexedDB, Cache Storage…): always **every** site, Keep List
     * included. Android can't delete one site's local storage (`WebStorage.deleteOrigin` covers
     * only Application Cache and Web SQL; measured: local storage survived it), so a per-site
     * delete would be incomplete. Keep List sites stay logged in through their cookies.
     */
    private fun deleteSiteStorage(then: () -> Unit) {
        WebStorage.getInstance().deleteAllData()
        then()
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
