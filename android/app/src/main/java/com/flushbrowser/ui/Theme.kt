/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.flushbrowser.R

/**
 * Flush's palette, the same tokens as the brand board (mockups/flush/index.html), in a dark and a light version.
 * [light] is Compose state, so every screen redraws when Settings → Appearance changes.
 */
object C {
    var light by mutableStateOf(false)

    val Bg get() = if (light) Color(0xFFF5FAFF) else Color(0xFF071226)
    val Surface get() = if (light) Color(0xFFE8F1FC) else Color(0xFF0D1C36)
    val Surface2 get() = if (light) Color(0xFFDAE7F7) else Color(0xFF142749)
    val Line get() = if (light) Color(0xFFC3D5EC) else Color(0xFF24395F)
    val Text get() = if (light) Color(0xFF0A1A30) else Color(0xFFEAF4FF)
    val Muted get() = if (light) Color(0xFF4D6280) else Color(0xFF93A8C6)
    /** Flush blue for fills (buttons, switches, the Flush Button): the same in both themes. */
    val Fill = Color(0xFF1EA7FF)
    /** Accent for text and icons: cyan in dark mode, a deeper blue in light mode so it stays readable on light backgrounds. */
    val Accent get() = if (light) Color(0xFF005FCC) else Color(0xFF3FD8FF)
    /** Text and icons on [Fill]: navy ink (white on the blue would only reach about 2.4 : 1). */
    val Ink = Color(0xFF03142B)
    val Success get() = if (light) Color(0xFF11804E) else Color(0xFF3DDC97)
    /**
     * Success for text on Surface / Surface2. In light mode the board's #11804E reaches only
     * 4.36 : 1 on Surface and 3.97 : 1 on Surface2, so a slightly deeper green (≥ 4.5 : 1 on all three).
     */
    val SuccessOnSurface get() = if (light) Color(0xFF0E7647) else Success
    /** Switches when on: the Fill blue, the same in both themes. */
    val SwitchOn = Fill
    /** The switch dot: the light theme's near-white background, in both themes. */
    val SwitchDot = Color(0xFFF5FAFF)
    /** Switches when off: a neutral blue-grey track from the palette (the other theme's Muted), so the dot shows. */
    val SwitchOff get() = if (light) Color(0xFF93A8C6) else Color(0xFF4D6280)
    /** The logo's navy (the Vortex art's own background): behind the logo and the Flush Button in both themes. */
    val LogoBg = Color(0xFF071226)
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
private fun fredoka(w: Int) = Font(R.font.fredoka, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@OptIn(ExperimentalTextApi::class)
private fun figtree(w: Int) = Font(R.font.figtree, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Display face (Fredoka, weights 300–700): wordmark, headings. ExtraBold requests get 700, its boldest. */
val Display = FontFamily(fredoka(500), fredoka(600), fredoka(700))

/** UI face: everything else. */
val Ui = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700))
