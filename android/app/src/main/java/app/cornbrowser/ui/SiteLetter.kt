/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

/**
 * The letter shown on a site's letter icon: the first letter of the site's main name, not of its
 * host, so en.wikipedia.org shows "W" and m.youtube.com "Y" (not "E" and "M"). The main name is the
 * label before the ending; endings like co.uk / com.au count as one. Addresses and blanks fall back
 * to the host's first character. No public-suffix list: a few common two-part endings cover the
 * cases people see, and a wrong guess only changes one letter.
 */
object SiteLetter {
    private val secondLevel = setOf("co", "com", "org", "net", "ac", "gov", "edu", "ne", "or", "go")

    fun of(host: String): String {
        val h = host.trim().lowercase().removePrefix("www.").trimEnd('.')
        val labels = h.split('.').filter { it.isNotEmpty() }
        if (labels.isEmpty()) return "?"
        if (labels.all { part -> part.all { it.isDigit() } } || ':' in h) return h.first().uppercase() // an IP address
        val name = when {
            labels.size >= 3 && labels.last().length == 2 && labels[labels.size - 2] in secondLevel -> labels[labels.size - 3]
            labels.size >= 2 -> labels[labels.size - 2]
            else -> labels[0]
        }
        return name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: h.first().uppercase()
    }
}
