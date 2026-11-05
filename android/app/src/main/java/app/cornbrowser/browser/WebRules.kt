/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

/**
 * Corn's rules for new windows, links that leave the web, and long-press titles.
 * Plain Kotlin (no Android types) so every rule is unit-tested (`WebRulesTest`).
 * Generic on purpose: no site names here; what's blocked where comes from the filter lists.
 */

/**
 * New windows (`target="_blank"` links, `window.open`): the web's standard rule, used by Chrome,
 * Brave and DuckDuckGo. A window opened right after the user's tap opens as a new tab; one a page
 * opens by itself is a pop-up and is blocked. WebView reports the tap as `isUserGesture`.
 */
object WindowPolicy {
    /** One tap may open one window: more within this time are blocked (pages that open several at once). */
    const val BURST_MS = 1_000L

    enum class Decision { OPEN_TAB, BLOCK }

    /**
     * @param userGesture WebView's `isUserGesture`: the user tapped just before.
     * @param siteBlocksPopups the site's "Pop-ups & redirects" switch (on by default).
     * @param msSinceLastOpened time since this tab last opened a window ([Long.MAX_VALUE] if never).
     */
    fun newWindow(userGesture: Boolean, siteBlocksPopups: Boolean, msSinceLastOpened: Long): Decision = when {
        !siteBlocksPopups -> Decision.OPEN_TAB                        // the user allowed pop-ups on this site
        userGesture && msSinceLastOpened >= BURST_MS -> Decision.OPEN_TAB
        else -> Decision.BLOCK
    }
}

/** Links that aren't web pages: phone numbers, email, SMS, maps and other apps. */
object ExternalLinks {
    enum class Kind { WEB, IGNORE, CALL, EMAIL, TEXT, MAP, APP }

    /** Schemes that never leave Corn: internal, script or local-file addresses. */
    private val IGNORED = setOf("about", "javascript", "data", "blob", "file", "content", "filesystem", "chrome", "ws", "wss")

    fun kind(url: String): Kind {
        val scheme = url.substringBefore(':', "").lowercase()
        return when {
            scheme.isEmpty() || scheme in IGNORED -> Kind.IGNORE
            scheme == "http" || scheme == "https" -> Kind.WEB
            scheme == "tel" -> Kind.CALL
            scheme == "mailto" -> Kind.EMAIL
            scheme in setOf("sms", "smsto", "mms", "mmsto") -> Kind.TEXT
            scheme == "geo" -> Kind.MAP
            // A scheme must be letters, digits, + - . (RFC 3986); anything else is not a real link.
            !scheme.first().isLetter() || !scheme.all { it.isLetterOrDigit() || it in "+-." } -> Kind.IGNORE
            else -> Kind.APP // intent://, android-app://, market://, whatsapp://, …
        }
    }

    /** What the ask sheet says the link will do. [appName] is null when Android can't tell. */
    fun title(kind: Kind, appName: String?): String = when (kind) {
        Kind.CALL -> "Call this number?"
        Kind.EMAIL -> "Write an email?"
        Kind.TEXT -> "Send a text?"
        Kind.MAP -> "Show this place on a map?"
        else -> if (appName != null) "Open in $appName?" else "Open in another app?"
    }

    /** The part worth showing (number, address), without the scheme or parameters. */
    fun detail(kind: Kind, url: String): String {
        val rest = url.substringAfter(':').substringBefore('?').removePrefix("//")
        // Percent-decoding only: URLDecoder would turn the "+" of "+1 555…" into a space.
        val decoded = runCatching { java.net.URLDecoder.decode(rest.replace("+", "%2B"), "UTF-8") }.getOrDefault(rest)
        return when (kind) {
            Kind.CALL, Kind.EMAIL, Kind.TEXT, Kind.MAP -> decoded.take(80)
            else -> "Corn will hand this link to the app."
        }
    }

    /** An `intent://` link's `S.browser_fallback_url`, only if it is a web address. */
    fun fallback(raw: String?): String? =
        raw?.trim()?.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
}

/** The title shown when a link is long-pressed. */
object LinkTitles {
    private val NOT_A_TITLE = Regex("^[\\d\\s:.,/|·•-]*$") // "4:02", "12", "1/3": durations, counters

    /**
     * The first candidate that reads like a title. Candidates come from the page in priority order:
     * for each link to the same address, its `aria-label`, `title`, an image's `alt`, then its text.
     * Video sites put the duration first inside the thumbnail's link, so "4:02" is skipped.
     */
    fun pick(candidates: List<String?>): String? = candidates.asSequence()
        .filterNotNull()
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .firstOrNull { it.isNotEmpty() && !NOT_A_TITLE.matches(it) }
        ?.take(200)
}
