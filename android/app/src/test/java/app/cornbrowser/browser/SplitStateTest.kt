/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitStateTest {
    @Test fun startPutsCurrentFirstAndFocusesTheNewPane() {
        val s = SplitState.start(current = 1, other = 2)!!
        assertEquals(1, s.first)
        assertEquals(2, s.second)
        assertEquals(2, s.focused)
        assertEquals(1, s.unfocused)
        assertEquals(0.5f, s.ratio)
        assertEquals(setOf(1, 2), s.tabIds)
    }

    @Test fun cannotSplitATabWithItself() = assertNull(SplitState.start(3, 3))

    @Test fun focusSwitchesPanesAndIgnoresOtherTabs() {
        val s = SplitState.start(1, 2)!!
        assertEquals(1, s.focus(1).focused)
        assertEquals(2, s.focus(1).focus(2).focused)
        assertEquals(s, s.focus(9))
    }

    @Test fun showingAnotherTabReplacesTheFocusedPane() {
        val s = SplitState.start(1, 2)!!          // focus on 2 (second pane)
        val t = s.show(5)
        assertEquals(1, t.first)
        assertEquals(5, t.second)
        assertEquals(5, t.focused)
        val u = s.focus(1).show(7)                 // focus on 1 (first pane)
        assertEquals(7, u.first)
        assertEquals(2, u.second)
        assertEquals(7, u.focused)
    }

    @Test fun showingATabAlreadyOnScreenFocusesIt() {
        val s = SplitState.start(1, 2)!!.show(1)
        assertEquals(1, s.first)
        assertEquals(2, s.second)
        assertEquals(1, s.focused)
    }

    @Test fun dividerIsClampedTo25To75Percent() {
        val s = SplitState.start(1, 2)!!
        assertEquals(0.25f, s.resized(0.02f).ratio)
        assertEquals(0.75f, s.resized(0.99f).ratio)
        assertEquals(0.4f, s.resized(0.4f).ratio)
        assertEquals(0.5f, s.resized(Float.NaN).ratio)
    }

    @Test fun lettingGoAtAnEdgeClosesThatPane() {
        val s = SplitState.start(1, 2)!!
        assertEquals(1, s.closedByRelease(0.03f))   // dragged up/left over the first pane
        assertEquals(2, s.closedByRelease(0.97f))   // dragged down/right over the second pane
        assertNull(s.closedByRelease(0.2f))
        assertNull(s.closedByRelease(0.8f))
    }

    @Test fun closingAPaneOrItsTabKeepsTheOtherTab() {
        val s = SplitState.start(1, 2)!!
        assertEquals(2, s.remainingAfterClosing(1))
        assertEquals(1, s.remainingAfterClosing(2))
        assertNull(s.remainingAfterClosing(3))       // a tab not on screen: split stays
        assertTrue(1 in s)
        assertFalse(3 in s)
    }

    @Test fun bothSplitTabsStayLiveAndDoNotCountTowardsTheLimit() {
        val live = setOf(1, 2, 3, 4, 5)
        val recent = listOf(2, 1, 3, 4, 5)
        assertEquals(setOf(3, 4), tabsKeptInMemory(recent, setOf(1, 2), { it in live }, 2))
        // Single tab: only the open one is excluded.
        assertEquals(setOf(1, 3), tabsKeptInMemory(recent, setOf(2), { it in live }, 2))
    }

    @Test fun keptTabsSkipUnloadedOnesAndRespectZero() {
        val recent = listOf(1, 2, 3, 4)
        assertEquals(setOf(4), tabsKeptInMemory(recent, setOf(1, 2), { it == 4 }, 2))
        assertEquals(emptySet<Int>(), tabsKeptInMemory(recent, setOf(1, 2), { true }, 0))
        assertEquals(emptySet<Int>(), tabsKeptInMemory(recent, setOf(1, 2), { true }, -1))
    }
}
