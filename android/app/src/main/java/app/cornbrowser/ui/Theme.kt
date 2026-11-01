/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.cornbrowser.R

/** Corn's palette, the same tokens as mockups/prototype.html. */
object C {
    val Bg = Color(0xFF0F0E0B)
    val Surface = Color(0xFF1A1813)
    val Surface2 = Color(0xFF25221B)
    val Line = Color(0xFF36312A)
    val Text = Color(0xFFF5F0E1)
    val Muted = Color(0xFFA8A08C)
    val Corn = Color(0xFFFFC83D)
    val CornInk = Color(0xFF2A1E00)
    val Leaf = Color(0xFF86C64A)
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
