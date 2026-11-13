/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import java.net.URI

/** The "Add a favourite" sheet's address field. Plain Kotlin (unit-tested). */
object FavoriteInput {
    /**
     * What the user typed as a web address, the way the address bar reads it: "example.com" becomes
     * "http://example.com" (Corn then upgrades it to https with corn-url), http(s) addresses are kept.
     * Null when it isn't an address (blank, spaces, no dot, another scheme, no host).
     */
    fun address(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val lower = text.lowercase()
        val url = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> text
            "://" in text -> return null
            text.contains('.') || lower.startsWith("localhost") -> "http://$text"
            else -> return null
        }
        val host = runCatching { URI(url).host }.getOrNull() ?: return null
        if (host.isEmpty() || host.startsWith('.') || host.endsWith('.') || (!host.contains('.') && host != "localhost")) return null
        return url
    }

    /** The site's name as the default favourite name: "https://www.Example.com/a" → "example.com". */
    fun site(url: String): String =
        (runCatching { URI(url).host }.getOrNull() ?: "").lowercase().removePrefix("www.")
}
