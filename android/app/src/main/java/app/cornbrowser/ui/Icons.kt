/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

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
object CornIcons {
    // Vault body y 3…19 plus feet to y 21 → 18 units tall.
    private val RECT_VAULT = "M5 3h14a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1-2.5 2.5H5a2.5 2.5 0 0 1-2.5-2.5v-11A2.5 2.5 0 0 1 5 3z"
    private val FEET = "M6.5 19v2M17.5 19v2"
    private val SHIELD = "M12 3l7.5 3v5.5c0 4.6-3.2 8.4-7.5 9.5-4.3-1.1-7.5-4.9-7.5-9.5V6z"

    // Arrows are open shapes, so they look bigger than boxes of the same height:
    // drawn smaller on purpose (optical sizing), 15.5 dp visible vs 18 dp for the others.
    val Back = line("M19.2 12H5.2", "M12 18.8L5.2 12 12 5.2")
    val Forward = line("M4.8 12h14", "M12 5.2l6.8 6.8-6.8 6.8")
    /** Back when it returns to Corn's home page: a curved "↩" arrow, same optical size as Back. */
    val BackHome = line("M9.5 13.8L5 9.3l4.5-4.5", "M5 9.3h9a4.75 4.75 0 0 1 0 9.5h-3.5")
    val Vault = toolbar(RECT_VAULT, "M15.5 11a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z", "M12 7.5v1M12 13.5v1M8.5 11h1M14.5 11h1", FEET)
    val VaultAdd = toolbar(RECT_VAULT, "M12 7.4v7.2M8.4 11h7.2", FEET)
    val VaultSaved = toolbar(RECT_VAULT, "M8.3 11.2l2.6 2.6 4.8-5", FEET)
    // Toilet y 3…21.
    val Toilet = toolbar(
        "M4.8 3h4.4a1.3 1.3 0 0 1 1.3 1.3v5.2a1.3 1.3 0 0 1-1.3 1.3H4.8a1.3 1.3 0 0 1-1.3-1.3V4.3A1.3 1.3 0 0 1 4.8 3z",
        "M3.5 10.8h17c0 3.7-3 6.7-6.7 6.7h-4.6c-3.1 0-5.7-2.5-5.7-5.6z",
        "M8.7 17.5l-.5 3.5h7.6l-.5-3.5",
    )
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
    val Clock = line("M20.5 12a8.5 8.5 0 1 1-17 0a8.5 8.5 0 1 1 17 0z", "M12 7.5V12l3 2")
    val Trash = line("M4.5 6.5h15", "M9.5 6.5V4.5h5v2", "M6.5 6.5l1 13h9l1-13", "M10.3 10.5v5.5M13.7 10.5v5.5")
    val Folder = line("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val BellOff = line("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0", "M4 4l16 16")

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
