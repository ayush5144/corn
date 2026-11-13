/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The new tab's Favourites row: 4 fit beside the Vault on a 360 dp phone; beyond that the edge tile is cut about in half. */
class FavoriteTileWidthTest {
    // Content width (20 dp page padding each side) minus the Vault tile and the 7 dp divider.
    private fun rowWidth(phoneDp: Float) = phoneDp - 40f - FAVORITE_TILE_DP - 7f

    @Test fun fourFitOnANarrowPhone() = assertEquals(FAVORITE_TILE_DP, favoriteTileWidth(rowWidth(360f).dp, 4).value, 0.01f)

    @Test fun overflowLeavesHalfATileShowing() {
        for (w in listOf(251f, 302f, 340f, 420f, 600f)) {
            val tile = favoriteTileWidth(w.dp, 20).value
            val shown = w / tile
            assertEquals("width $w", 0.5f, shown - shown.toInt(), 0.01f)
            assertTrue("width $w gave $tile", tile in 60f..80f)
        }
    }

    @Test fun fiveOnATypicalPhoneCutTheFifth() {
        // 411 dp phone: 302 dp for favourites → 4.5 tiles of ~67 dp.
        assertEquals(rowWidth(411f) / 4.5f, favoriteTileWidth(rowWidth(411f).dp, 5).value, 0.01f)
    }
}
