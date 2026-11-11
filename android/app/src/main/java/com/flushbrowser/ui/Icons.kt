/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Corn's icon set: 24×24 line icons, tint with Icon(tint = …).
 * Toolbar icons share one visible height: their drawing spans y = 3…21 (18 of 24 units),
 * so at 24 dp every one of them is 18 dp tall without stretching.
 */
object Glyphs {
    // Vault body y 3…19 plus feet to y 21 → 18 units tall.
    private val RECT_VAULT = "M5 3h14a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1-2.5 2.5H5a2.5 2.5 0 0 1-2.5-2.5v-11A2.5 2.5 0 0 1 5 3z"
    private val FEET = "M6.5 19v2M17.5 19v2"
    private val SHIELD = "M12 3l7.5 3v5.5c0 4.6-3.2 8.4-7.5 9.5-4.3-1.1-7.5-4.9-7.5-9.5V6z"

    // Arrows are open shapes, so they look bigger than boxes of the same height:
    // drawn smaller on purpose (optical sizing), 15.5 dp visible vs 18 dp for the others.
    val Back = line("M19.2 12H5.2", "M12 18.8L5.2 12 12 5.2")
    val Forward = line("M4.8 12h14", "M12 5.2l6.8 6.8-6.8 6.8")
    /** The bottom nav bar's Back button only: a curved "↩" arrow (founder's choice, 2026-11-05). */
    val NavBack = line("M9.5 13.8L5 9.3l4.5-4.5", "M5 9.3h9a4.75 4.75 0 0 1 0 9.5h-3.5")
    val Vault = toolbar(RECT_VAULT, "M15.5 11a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z", "M12 7.5v1M12 13.5v1M8.5 11h1M14.5 11h1", FEET)
    val VaultAdd = toolbar(RECT_VAULT, "M12 7.4v7.2M8.4 11h7.2", FEET)
    val VaultSaved = toolbar(RECT_VAULT, "M8.3 11.2l2.6 2.6 4.8-5", FEET)
    // The Flush Button's glyph: three arms of water swirling into a drain (founder's choice, option A).
    // Spans y = 3.95…20.05, so with the 1.9 stroke it's 18 units tall like the other toolbar icons.
    val Flush: ImageVector = run {
        val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        listOf(
            "M12.00 3.95 L13.27 4.26 L14.43 4.77 L15.47 5.44 L16.36 6.26 L17.08 7.18 L17.61 8.18 L17.97 9.23 L18.13 10.29 L18.12 11.32 L17.94 12.31 L17.60 13.22 L17.14 14.03 L16.56 14.73 L15.90 15.29 L15.18 15.72 L14.43 16.01 L13.67 16.15 L12.93 16.16 L12.23 16.05 L11.60 15.82 L11.04 15.50 L10.58 15.11 L10.21 14.66 L9.95 14.19",
            "M18.97 16.02 L18.07 16.97 L17.05 17.72 L15.94 18.28 L14.79 18.65 L13.63 18.80 L12.50 18.77 L11.42 18.55 L10.42 18.17 L9.53 17.64 L8.76 16.99 L8.14 16.24 L7.67 15.43 L7.36 14.58 L7.20 13.73 L7.19 12.89 L7.32 12.10 L7.57 11.37 L7.93 10.73 L8.38 10.18 L8.89 9.74 L9.45 9.42 L10.02 9.21 L10.59 9.12 L11.13 9.13",
            "M5.03 16.03 L4.67 14.77 L4.52 13.51 L4.59 12.27 L4.85 11.10 L5.29 10.01 L5.89 9.05 L6.62 8.22 L7.45 7.55 L8.36 7.04 L9.30 6.70 L10.26 6.54 L11.19 6.53 L12.08 6.69 L12.90 6.98 L13.63 7.39 L14.26 7.90 L14.76 8.48 L15.14 9.11 L15.39 9.78 L15.51 10.44 L15.51 11.08 L15.41 11.68 L15.20 12.22 L14.92 12.69",
        ).forEach { d ->
            b.addPath(
                PathParser().parsePathString(d).toNodes(), fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        b.addPath(PathParser().parsePathString("M13.3 12a1.3 1.3 0 1 1-2.6 0a1.3 1.3 0 1 1 2.6 0z").toNodes(), fill = SolidColor(Color.Black)) // the drain
        b.build()
    }
    // Dots y 3…21.
    val Dots = filled(
        "M12 3a1.7 1.7 0 1 1 0 3.4a1.7 1.7 0 1 1 0-3.4z",
        "M12 10.3a1.7 1.7 0 1 1 0 3.4a1.7 1.7 0 1 1 0-3.4z",
        "M12 17.6a1.7 1.7 0 1 1 0 3.4a1.7 1.7 0 1 1 0-3.4z",
    )
    val Shield = line(SHIELD, "M8.8 12.2l2.2 2.2 4.3-4.6")
    val ShieldOff = line(SHIELD, "M9 9l6 6M15 9l-6 6")
    val ChevDown = line("M7 10l5 5 5-5")
    val ChevUp = line("M6 15l6-6 6 6")
    val ChevRight = line("M9 6l6 6-6 6")
    val Plus = line("M12 5v14M5 12h14")
    val X = line("M6 6l12 12M18 6L6 18")
    val Search = line("M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0z", "M20 20l-3.5-3.5")
    val Lock = line("M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Sliders = line("M4 7h9M17 7h3M4 17h3M11 17h9", "M17 7a2 2 0 1 1-4 0a2 2 0 1 1 4 0z", "M11 17a2 2 0 1 1-4 0a2 2 0 1 1 4 0z")
    val Share = line("M12 15V3M7 8l5-5 5 5", "M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5")
    val Fingerprint = line(
        "M7.5 19c1-1.8 1.5-3.9 1.5-6.5a3 3 0 0 1 6 0c0 1.2-.1 2.3-.3 3.4",
        "M5 15.5c.4-1 .5-2 .5-3a6.5 6.5 0 0 1 13 0c0 2.4-.4 4.6-1.1 6.6",
        "M12 12.5c0 3.2-.7 6-2 8.5",
    )
    val Tag = line("M3.5 3.5h7.6l9.4 9.4-7.6 7.6-9.4-9.4z", "M9.4 8a1.4 1.4 0 1 1-2.8 0a1.4 1.4 0 1 1 2.8 0z")
    val GroupAdd = line(
        "M5 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M15 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M5 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z",
        "M17 14v6M14 17h6",
    )
    val TabNew = line(
        "M5.5 5h13A2.5 2.5 0 0 1 21 7.5v10a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5v-10A2.5 2.5 0 0 1 5.5 5z",
        "M12 9.5v6M9 12.5h6",
    )
    val Copy = line(
        "M10 8h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z",
        "M16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3",
    )
    val Backspace = line("M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9l-6-7z", "M12.5 9.5l5 5M17.5 9.5l-5 5")
    val Heart = line("M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z")
    val Check = line("M5 12.5l4.5 4.5L19 7")
    val Download = line("M12 4v11", "M7 10.5l5 5 5-5", "M5 20h14")
    val Clock = line("M20.5 12a8.5 8.5 0 1 1-17 0a8.5 8.5 0 1 1 17 0z", "M12 7.5V12l3 2")
    val Trash = line("M4.5 6.5h15", "M9.5 6.5V4.5h5v2", "M6.5 6.5l1 13h9l1-13", "M10.3 10.5v5.5M13.7 10.5v5.5")
    val Folder = line("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    /** Split screen: a page divided into two panes. */
    val Split = line("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M3 12h18")
    val BellOff = line("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0", "M4 4l16 16")
    // Passwords
    val Key = line("M11.2 12.8a4.6 4.6 0 1 1-6.5 6.5 4.6 4.6 0 0 1 6.5-6.5z", "M11.2 12.8L20 4", "M16.8 7.2l2.6 2.6", "M14.4 9.6l1.9 1.9")
    val Eye = line("M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z")
    val EyeOff = line("M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", "M4 4l16 16")
    val Edit = line("M4.5 19.5h4l10.2-10.2a2.1 2.1 0 0 0-3-3L5.5 16.5z", "M14 7.5l3 3")

    private const val STROKE = 1.9f

    private fun line(vararg paths: String): ImageVector = build(paths, filled = false, scale = 1f)
    private fun filled(vararg paths: String): ImageVector = build(paths, filled = true, scale = 1f)

    /**
     * Toolbar icons are drawn on the y = 3…21 grid. A stroke adds half its width above and below,
     * so the drawing is scaled about the centre to make the *visible* height exactly 18 units,
     * with the stroke widened to compensate so it still renders at 1.9.
     */
    private fun toolbar(vararg paths: String): ImageVector = build(paths, filled = false, scale = (18f - STROKE) / 18f)

    private fun build(paths: Array<out String>, filled: Boolean, scale: Float): ImageVector {
        val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        if (scale != 1f) b.addGroup(scaleX = scale, scaleY = scale, pivotX = 12f, pivotY = 12f)
        for (d in paths) {
            val nodes = PathParser().parsePathString(d).toNodes()
            if (filled) {
                b.addPath(nodes, fill = SolidColor(Color.Black))
            } else {
                b.addPath(
                    nodes,
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE / scale,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        if (scale != 1f) b.clearGroup()
        return b.build()
    }
}
