/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import androidx.compose.ui.graphics.Color
import java.net.URLEncoder

/** A built-in search engine. `template` contains %s for the query; safe search is turned off where the engine allows it. */
data class SearchEngine(val id: String, val name: String, val letter: String, val color: Color, val template: String) {
    fun urlFor(query: String): String = template.replace("%s", URLEncoder.encode(query, "UTF-8"))
}

object SearchEngines {
    val all = listOf(
        SearchEngine("ddg", "DuckDuckGo", "D", Color(0xFFDE5833), "https://duckduckgo.com/?q=%s&kp=-2"),
        SearchEngine("google", "Google", "G", Color(0xFF4285F4), "https://www.google.com/search?q=%s&safe=off"),
        SearchEngine("brave", "Brave Search", "B", Color(0xFFFB542B), "https://search.brave.com/search?q=%s&safesearch=off"),
        SearchEngine("startpage", "Startpage", "S", Color(0xFF6573FF), "https://www.startpage.com/do/search?q=%s"),
        SearchEngine("bing", "Bing", "b", Color(0xFF00809D), "https://www.bing.com/search?q=%s&adlt=off"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    /** Turn what the user typed into a URL: a web address as-is, anything else as a search. */
    fun resolve(input: String, engine: SearchEngine): String {
        val text = input.trim()
        val looksLikeUrl = !text.contains(' ') && (text.contains('.') || text.startsWith("localhost"))
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> text
            looksLikeUrl -> "https://$text"
            else -> engine.urlFor(text)
        }
    }
}
