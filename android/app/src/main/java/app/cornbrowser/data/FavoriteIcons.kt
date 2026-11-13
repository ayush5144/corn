/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** A favourite site's icon as stored: the page's icon from WebView, scaled to 64×64 PNG (a few KB). */
object FavoriteIcons {
    const val SIZE = 64

    fun png(icon: Bitmap): ByteArray {
        val scaled = if (icon.width == SIZE && icon.height == SIZE) icon else Bitmap.createScaledBitmap(icon, SIZE, SIZE, true)
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }
}
