/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.cornbrowser.R

/**
 * Corn's palette, the same tokens as mockups/prototype.html, in a dark and a light version.
 * [light] is Compose state, so every screen redraws when Settings → Appearance changes.
 */
object C {
    var light by mutableStateOf(false)

    val Bg get() = if (light) Color(0xFFFBF8F1) else Color(0xFF0F0E0B)
    val Surface get() = if (light) Color(0xFFF2EDE1) else Color(0xFF1A1813)
    val Surface2 get() = if (light) Color(0xFFE8E1D0) else Color(0xFF25221B)
    val Line get() = if (light) Color(0xFFD8CFBA) else Color(0xFF36312A)
    val Text get() = if (light) Color(0xFF1E1B14) else Color(0xFFF5F0E1)
    val Muted get() = if (light) Color(0xFF6D6553) else Color(0xFFA8A08C)
    /** Corn yellow for fills (buttons, switches, the Flush button): the same in both themes. */
    val Corn = Color(0xFFFFC83D)
    /** Corn yellow for text and icons: deeper gold in light mode so it stays readable on light backgrounds. */
    val Accent get() = if (light) Color(0xFF9A6500) else Color(0xFFFFC83D)
    val CornInk = Color(0xFF2A1E00)
    val Leaf get() = if (light) Color(0xFF4E8A1E) else Color(0xFF86C64A)
    val Scrim = Color(0x8C000000)

    val GroupColors = listOf(
        Color(0xFFFFC83D), Color(0xFF86C64A), Color(0xFF5B8DEF),
        Color(0xFFE26D9B), Color(0xFF9B6BF2), Color(0xFFFF8A4C),
    )

    /** A stable colour for a site, used behind its letter when there's no favicon. */
    fun forHost(host: String): Color {
        val palette = listOf(0xFF5B8DEF, 0xFFE26D9B, 0xFF9B6BF2, 0xFFFF8A4C, 0xFF3FA796, 0xFFDE5833, 0xFF6573FF)
        return Color(palette[(host.hashCode() and 0x7fffffff) % palette.size])
    }
}

@OptIn(ExperimentalTextApi::class)
private fun bricolage(w: Int) = Font(R.font.bricolage, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@OptIn(ExperimentalTextApi::class)
private fun figtree(w: Int) = Font(R.font.figtree, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Display face: wordmark, headings. */
val Display = FontFamily(bricolage(500), bricolage(700), bricolage(800))

/** UI face: everything else. */
val Ui = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700))
