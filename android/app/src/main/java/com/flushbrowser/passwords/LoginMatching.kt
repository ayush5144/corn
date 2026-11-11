/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.passwords

import java.net.URI

/** A saved login with its username decrypted (the password stays encrypted until needed). */
data class SavedLogin(
    val id: Long,
    val origin: String,
    val username: String,
    val lastUsedAt: Long,
    val timesUsed: Long,
)

/** What to do after the user submitted a login form. */
sealed interface SaveDecision {
    /** Already saved with this password: nothing to ask. */
    data class AlreadySaved(val id: Long) : SaveDecision
    /** This username is saved with a different password: offer "Update password?". */
    data class Update(val id: Long) : SaveDecision
    /** New login for this site: offer "Save password?". */
    data object New : SaveDecision
}

/**
 * Pure rules for matching logins to pages (no Android classes, so they run in unit tests).
 * Logins match by **exact origin**: scheme, host and port. `www.example.com`, `example.com`,
 * `m.example.com` and `example.com:8443` are four different sites, like in Firefox and Chrome.
 */
object LoginMatching {

    /** Hosts that may use plain http in debug builds only (local test pages; 10.0.2.2 is the emulator's host machine). */
    val LOCAL_TEST_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")

    /**
     * The origin Flush saves logins under: `https://host` or `https://host:port` (port only when not 443).
     * Null for anything else: http pages (except [LOCAL_TEST_HOSTS] when [allowLocalHttp]), files, data: URLs.
     */
    fun originOf(url: String?, allowLocalHttp: Boolean = false): String? {
        if (url.isNullOrBlank()) return null
        val u = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = u.scheme?.lowercase() ?: return null
        val host = u.host?.lowercase()?.trimEnd('.')?.takeIf { it.isNotEmpty() } ?: return null
        val defaultPort = when (scheme) {
            "https" -> 443
            "http" -> if (allowLocalHttp && host in LOCAL_TEST_HOSTS) 80 else return null
            else -> return null
        }
        val port = u.port.takeIf { it != -1 && it != defaultPort }
        return if (port == null) "$scheme://$host" else "$scheme://$host:$port"
    }

    /** What users see for an origin: `example.com`, `mail.example.com:8443`. */
    fun displaySite(origin: String): String = origin.substringAfter("://").removePrefix("www.")

    /**
     * Decide whether to offer saving. [existing] are this origin's logins; [passwordOf] decrypts a
     * login's password (only called for a login with the same username).
     */
    fun decideSave(existing: List<SavedLogin>, username: String, password: String, passwordOf: (Long) -> String?): SaveDecision {
        val same = existing.firstOrNull { it.username == username }
            // No username typed (password-only form) and exactly one login here: it's that one.
            ?: existing.singleOrNull()?.takeIf { username.isEmpty() }
        if (same != null) {
            return if (passwordOf(same.id) == password) SaveDecision.AlreadySaved(same.id) else SaveDecision.Update(same.id)
        }
        return SaveDecision.New
    }

    /** Search on the Passwords screen: site or username contains the text (any case). */
    fun matchesSearch(login: SavedLogin, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return displaySite(login.origin).contains(q, ignoreCase = true) || login.username.contains(q, ignoreCase = true)
    }

    /** The Passwords screen: one group per site, sites A–Z, newest-used first inside a site. */
    fun groupBySite(logins: List<SavedLogin>): List<Pair<String, List<SavedLogin>>> =
        logins.groupBy { it.origin }
            .toList()
            .sortedBy { displaySite(it.first).lowercase() }
            .map { (origin, list) -> origin to list.sortedWith(compareByDescending<SavedLogin> { it.lastUsedAt }.thenByDescending { it.timesUsed }) }
}
