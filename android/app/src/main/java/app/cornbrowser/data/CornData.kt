/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import uniffi.corn_ffi.Collection
import uniffi.corn_ffi.CornStore
import uniffi.corn_ffi.Favorite
import uniffi.corn_ffi.HistoryVisit
import uniffi.corn_ffi.Login
import uniffi.corn_ffi.VaultItem

/** What Corn blocks on a site. Default: everything on. */
data class Shields(val ads: Boolean = true, val trackers: Boolean = true, val popups: Boolean = true) {
    val allOn: Boolean get() = ads && trackers && popups
    val allOff: Boolean get() = !ads && !trackers && !popups

    companion object {
        val ALL_ON = Shields()
        val ALL_OFF = Shields(ads = false, trackers = false, popups = false)
    }
}

/**
 * Corn's local store (corn.db, plain SQLite in app-private storage, via the Rust core).
 * Holds the Vault, history, the Keep List, per-site blocking choices and the Kept Session.
 * See docs/02-technical/10-storage.md.
 */
class CornData(context: Context) {
    private val store: CornStore = CornStore.open(File(context.filesDir, "corn.db").absolutePath)

    /** Bumped on every Vault change so screens showing Vault data redraw. */
    val vaultVersion = mutableIntStateOf(0)

    /** Bumped on every history change so the History screen redraws. */
    val historyVersion = mutableIntStateOf(0)

    private fun changed() { vaultVersion.intValue++ }

    // ---------- Vault ----------
    fun collections(): List<Collection> = store.collections()
    /** Number of saved links: what "All" shows. */
    fun itemCount(): Long = store.itemCount()
    /** Links in one Collection, or every saved link ("All") when `collectionId` is null. */
    fun items(collectionId: Long?): List<VaultItem> = store.items(collectionId)
    fun search(query: String): List<VaultItem> = store.search(query)
    fun isSaved(url: String): Boolean = url.isNotBlank() && store.containsUrl(url)

    /** The Collection with this name, created if it doesn't exist yet. */
    fun collectionNamed(name: String): Collection? = runCatching { store.collectionNamed(name) }
        .onSuccess { changed() }
        .getOrNull()

    fun createCollection(name: String): Collection? = runCatching { store.createCollection(name) }
        .onSuccess { changed() }
        .onFailure { Log.w(TAG, "create collection", it) }
        .getOrNull()

    /** Save a link once. `collectionId` null = in no Collection (it still shows in All). */
    fun save(collectionId: Long?, title: String, url: String, tags: List<String>) {
        store.addItem(collectionId, title, url, tags)
        changed()
    }

    /** "Save group to Vault": put every URL into the Collection named after the group. Returns the count added. */
    fun saveGroup(name: String, links: List<Pair<String, String>>): Int {
        val c = store.collectionNamed(name)
        val existing = store.items(c.id).map { it.url }.toSet()
        var added = 0
        for ((title, url) in links) {
            if (url !in existing) {
                store.addItem(c.id, title, url, emptyList())
                added++
            }
        }
        changed()
        return added
    }

    fun deleteItem(id: Long) {
        store.deleteItem(id)
        changed()
    }

    // ---------- Favourites (the new tab's tiles: their own list, not Vault links) ----------
    // Shown without the PIN and kept by every Flush. Each favourite site may have one small icon.
    /** Every Favourite in new-tab order. */
    fun favorites(): List<Favorite> = runCatching { store.favorites() }.getOrDefault(emptyList())

    /** Add at the end of the row; an address that's already a favourite keeps its place. */
    fun addFavorite(url: String, title: String): Favorite? = runCatching { store.addFavorite(url, title) }
        .onSuccess { changed() }
        .onFailure { Log.w(TAG, "add favourite failed: ${it.javaClass.simpleName}") } // never the address
        .getOrNull()

    fun removeFavorite(id: Long) {
        store.removeFavorite(id)
        changed()
    }

    /** Move a Favourite one place left (-1) or right (+1); nothing happens at the ends. */
    fun moveFavorite(id: Long, delta: Long) {
        store.moveFavorite(id, delta)
        changed()
    }

    /** Each favourite site's icon (64×64 PNG), by host (lower case, no "www."). */
    fun favoriteIcons(): Map<String, ByteArray> =
        runCatching { store.favoriteIcons().associate { it.host to it.png } }.getOrDefault(emptyMap())

    /** True when a favourite on [host] has no icon yet, or one older than [maxAgeMs]. */
    fun favoriteIconWanted(host: String, maxAgeMs: Long): Boolean =
        runCatching { store.favoriteIconWanted(host, maxAgeMs) }.getOrDefault(false)

    /** Keep [png] as [host]'s icon; the Rust core ignores it unless a favourite is on that site. */
    fun setFavoriteIcon(host: String, png: ByteArray) {
        if (runCatching { store.setFavoriteIcon(host, png) }.getOrDefault(false)) changed()
    }

    // ---------- History (on this phone only; cleared by Flush) ----------
    fun recordVisit(url: String, title: String, host: String) = runCatching {
        store.recordVisit(url, title, host, System.currentTimeMillis())
        historyVersion.intValue++
    }.onFailure { Log.w(TAG, "history write failed: ${it.javaClass.simpleName}") } // never the address

    fun updateTitle(url: String, title: String) = runCatching { store.updateTitle(url, title) }

    fun history(query: String?, limit: Long = 500): List<HistoryVisit> =
        runCatching { store.history(query?.takeIf { it.isNotBlank() }, limit) }.getOrDefault(emptyList())

    fun deleteVisit(visitId: Long) { store.deleteVisit(visitId); historyVersion.intValue++ }

    /** Delete every history entry for a site and its subdomains (history only, not cookies). */
    fun deleteHistoryForHost(host: String) { store.deleteHistoryForHost(host); historyVersion.intValue++ }

    fun clearHistory() { store.clearHistory(); historyVersion.intValue++ }

    // ---------- Keep List (sites that stay logged in after a Flush) ----------
    fun keepList(): List<String> = readList(KEEP)

    fun addKeep(site: String) {
        val host = normalizeHost(site) ?: return
        writeList(KEEP, (keepList() + host).distinct())
    }

    fun removeKeep(host: String) = writeList(KEEP, keepList() - host)

    // ---------- Per-site shields (what's blocked on each site) ----------
    /** Every site whose shields differ from the default (all on). */
    fun allSiteShields(): Map<String, Shields> {
        val raw = store.kvGet(SITE_SHIELDS) ?: return emptyMap()
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        return o.keys().asSequence().associateWith { host ->
            val s = o.getJSONObject(host)
            Shields(s.optBoolean("ads", true), s.optBoolean("trackers", true), s.optBoolean("popups", true))
        }
    }

    fun setSiteShields(host: String, shields: Shields) {
        val all = allSiteShields().toMutableMap()
        if (shields == Shields.ALL_ON) all.remove(host) else all[host] = shields
        val o = JSONObject()
        all.forEach { (h, s) -> o.put(h, JSONObject().put("ads", s.ads).put("trackers", s.trackers).put("popups", s.popups)) }
        store.kvSet(SITE_SHIELDS, o.toString())
        changed()
    }

    // ---------- Passwords (see passwords/Passwords.kt) ----------
    // Usernames and passwords arrive here already encrypted (passwords/LoginCipher); the site is in clear.
    // Like the Vault, they survive every Flush and "Delete browsing data".
    fun addLogin(origin: String, usernameEnc: ByteArray, passwordEnc: ByteArray): Login = store.addLogin(origin, usernameEnc, passwordEnc)
    /** [passwordChanged]: the password text changed (not only the username), so its "last changed" date moves. */
    fun updateLogin(id: Long, usernameEnc: ByteArray, passwordEnc: ByteArray, passwordChanged: Boolean) =
        store.updateLogin(id, usernameEnc, passwordEnc, passwordChanged)
    fun deleteLogin(id: Long) = store.deleteLogin(id)
    fun deleteAllLogins() = store.deleteAllLogins()
    fun loginsForOrigin(origin: String): List<Login> = store.loginsForOrigin(origin)
    fun allLogins(): List<Login> = store.allLogins()
    fun loginCount(): Long = store.loginCount()
    fun touchLogin(id: Long) = store.touchLogin(id)

    /** Sites (origins) where Corn never offers to save a password. */
    fun neverSaveList(): List<String> = readList(NEVER_SAVE)
    fun addNeverSave(origin: String) = writeList(NEVER_SAVE, (neverSaveList() + origin).distinct())
    fun removeNeverSave(origin: String) = writeList(NEVER_SAVE, neverSaveList() - origin)

    // ---------- Export (docs/02-technical/13-vault-export.md) ----------
    /** Every saved link as an HTML bookmarks file, formatted by the Rust core. */
    fun exportLinksHtml(): String = store.exportLinksHtml()
    /** Every saved link as CSV. */
    fun exportLinksCsv(): String = store.exportLinksCsv()

    // ---------- Kept Session ----------
    fun saveSession(json: String) = store.kvSet(SESSION, json)
    fun loadSession(): String? = store.kvGet(SESSION)
    fun clearSession() = store.kvDelete(SESSION)

    private fun readList(key: String): List<String> {
        val raw = store.kvGet(key) ?: return emptyList()
        return runCatching { JSONArray(raw).let { a -> List(a.length()) { a.getString(it) } } }.getOrDefault(emptyList())
    }

    private fun writeList(key: String, list: List<String>) {
        store.kvSet(key, JSONArray(list).toString())
        changed()
    }

    companion object {
        private const val TAG = "CornData"
        private const val KEEP = "keep_list"
        private const val SITE_SHIELDS = "site_shields"
        private const val SESSION = "kept_session"
        private const val NEVER_SAVE = "login_never_save"

        /** "https://Mail.Example.com/inbox" or "mail.example.com" → "mail.example.com". */
        fun normalizeHost(input: String): String? {
            val t = input.trim().lowercase()
            if (t.isEmpty()) return null
            val withScheme = if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
            return Uri.parse(withScheme).host?.removePrefix("www.")?.takeIf { it.contains('.') }
        }
    }
}
