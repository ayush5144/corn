/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.flushbrowser.R
import java.net.URLEncoder

/**
 * A built-in search engine. `template` contains %s for the query. `safeOff` / `safeOn` are the
 * engine's own URL parameters for safe search (empty when the engine has none, e.g. Startpage).
 */
data class SearchEngine(
    val id: String,
    val name: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val template: String,
    val safeOff: String,
    val safeOn: String,
) {
    fun urlFor(query: String, safeSearch: Boolean): String {
        val base = template.replace("%s", URLEncoder.encode(query, "UTF-8"))
        val param = if (safeSearch) safeOn else safeOff
        return if (param.isEmpty()) base else "$base&$param"
    }
}

object SearchEngines {
    val all = listOf(
        SearchEngine("ddg", "DuckDuckGo", R.drawable.engine_ddg, Color(0xFFDE5833), "https://duckduckgo.com/?q=%s", "kp=-2", "kp=1"),
        SearchEngine("google", "Google", R.drawable.engine_google, Color(0xFF4285F4), "https://www.google.com/search?q=%s", "safe=off", "safe=active"),
        SearchEngine("brave", "Brave Search", R.drawable.engine_brave, Color(0xFFFB542B), "https://search.brave.com/search?q=%s", "safesearch=off", "safesearch=strict"),
        SearchEngine("startpage", "Startpage", R.drawable.engine_startpage, Color(0xFF6573FF), "https://www.startpage.com/do/search?q=%s", "", ""),
        SearchEngine("bing", "Bing", R.drawable.engine_bing, Color(0xFF00809D), "https://www.bing.com/search?q=%s", "adlt=off", "adlt=strict"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()


    /** Turn what the user typed into a URL: a web address as-is, anything else as a search. */
    fun resolve(input: String, engine: SearchEngine, safeSearch: Boolean): String {
        val text = input.trim()
        val looksLikeUrl = !text.contains(' ') && (text.contains('.') || text.startsWith("localhost"))
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> text
            // Typed without a scheme: start from http and let Flush's HTTPS upgrade try https first,
            // falling back to http if the site has no https (as Chrome does for typed addresses).
            looksLikeUrl -> "http://$text"
            else -> engine.urlFor(text, safeSearch)
        }
    }
}
