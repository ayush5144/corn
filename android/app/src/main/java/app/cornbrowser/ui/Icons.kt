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
 * Corn's icon set: the same 24×24 line icons drawn in mockups/prototype.html.
 * Tint them with Icon(tint = …).
 */
object CornIcons {
    private val RECT_VAULT = "M5.5 4h13a2.5 2.5 0 0 1 2.5 2.5v10a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 16.5v-10A2.5 2.5 0 0 1 5.5 4z"
    private val FEET = "M6.5 19v1.6M17.5 19v1.6"
    private val SHIELD = "M12 3l7.5 3v5.5c0 4.6-3.2 8.4-7.5 9.5-4.3-1.1-7.5-4.9-7.5-9.5V6z"

    val Back = line("M19 12H5", "M12 19l-7-7 7-7")
    val Forward = line("M5 12h14", "M12 5l7 7-7 7")
    val Vault = line(RECT_VAULT, "M15.2 11.5a3.2 3.2 0 1 1-6.4 0a3.2 3.2 0 1 1 6.4 0z", "M12 8.3v.9M12 13.8v.9M8.8 11.5h.9M14.3 11.5h.9", FEET)
    val VaultAdd = line(RECT_VAULT, "M12 8.2v6.6M8.7 11.5h6.6", FEET)
    val VaultSaved = line(RECT_VAULT, "M8.5 11.7l2.4 2.4 4.6-4.8", FEET)
    val Toilet = line(
        "M4.8 2.5h4.4a1.3 1.3 0 0 1 1.3 1.3v5.4a1.3 1.3 0 0 1-1.3 1.3H4.8a1.3 1.3 0 0 1-1.3-1.3V3.8a1.3 1.3 0 0 1 1.3-1.3z",
        "M3.5 10.5h17c0 3.9-3.1 7-7 7h-4.3c-3.2 0-5.7-2.6-5.7-5.7z",
        "M8.7 17.5l-.6 4h7l-.6-4",
    )
    val Dots = filled(
        "M12 3.5a1.5 1.5 0 1 1 0 3a1.5 1.5 0 1 1 0-3z",
        "M12 10.5a1.5 1.5 0 1 1 0 3a1.5 1.5 0 1 1 0-3z",
        "M12 17.5a1.5 1.5 0 1 1 0 3a1.5 1.5 0 1 1 0-3z",
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
    val Folder = line("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val BellOff = line("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0", "M4 4l16 16")

    private fun line(vararg paths: String): ImageVector = build(paths, filled = false)
    private fun filled(vararg paths: String): ImageVector = build(paths, filled = true)

    private fun build(paths: Array<out String>, filled: Boolean): ImageVector {
        val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (d in paths) {
            val nodes = PathParser().parsePathString(d).toNodes()
            if (filled) {
                b.addPath(nodes, fill = SolidColor(Color.Black))
            } else {
                b.addPath(
                    nodes,
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        return b.build()
    }
}
