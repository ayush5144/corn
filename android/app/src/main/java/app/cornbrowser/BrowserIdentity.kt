/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser

/**
 * Builds the user-agent string Corn sends.
 *
 * Android WebView's default user agent says "wv" and "Version/4.0", which marks it as an
 * embedded WebView. Google sign-in and some sites reject that. Like DuckDuckGo's Android
 * browser (UserAgentProvider.kt), Corn sends Chrome's reduced user agent instead:
 *   Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/NNN.0.0.0 Mobile Safari/537.36
 * The reduced form also leaks less about the device (no model, no exact Android version).
 */
object BrowserIdentity {
    private val chromeMajor = Regex("Chrome/(\\d+)")

    fun userAgent(defaultUserAgent: String): String {
        val major = chromeMajor.find(defaultUserAgent)?.groupValues?.get(1) ?: return defaultUserAgent
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$major.0.0.0 Mobile Safari/537.36"
    }
}
