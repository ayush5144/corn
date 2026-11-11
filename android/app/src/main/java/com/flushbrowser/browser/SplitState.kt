/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.browser

/**
 * Split screen: two tabs on screen at once, [first] on top (or on the left in landscape) and
 * [second] below it (or on the right). The pane the user last touched is focused; the current
 * tab ([TabManager.currentId]) is always the focused pane's tab, so the address bar, toolbar,
 * find bar and sheets act on it. [ratio] is the first pane's share of the space (25–75 %).
 *
 * Immutable and free of Android code so the rules can be unit-tested; [TabManager] holds the
 * current value (null when split screen is off). Not saved: a restart shows a single tab.
 */
data class SplitState(
    val first: Int,
    val second: Int,
    val focusFirst: Boolean = false,
    val ratio: Float = 0.5f,
) {
    /** The focused pane's tab (the current tab). */
    val focused: Int get() = if (focusFirst) first else second
    val unfocused: Int get() = if (focusFirst) second else first
    /** Both tabs on screen: they always stay live (never unloaded) while split. */
    val tabIds: Set<Int> get() = setOf(first, second)

    operator fun contains(id: Int): Boolean = id == first || id == second

    /** Focus the pane showing [id] (no change if it isn't on screen). */
    fun focus(id: Int): SplitState = when (id) {
        first -> copy(focusFirst = true)
        second -> copy(focusFirst = false)
        else -> this
    }

    /**
     * Show tab [id] (Tabs screen, Tab Group Strip, a new tab, a pop-up…): if it's already in a
     * pane, focus that pane; otherwise it replaces the focused pane's tab.
     */
    fun show(id: Int): SplitState = when {
        id in this -> focus(id)
        focusFirst -> copy(first = id)
        else -> copy(second = id)
    }

    /** Drag the divider: [raw] is where the finger is (0 = top/left edge, 1 = bottom/right edge). */
    fun resized(raw: Float): SplitState = copy(ratio = clampRatio(raw))

    /**
     * The tab that stays on screen when the pane showing [id] closes (its × or the divider dragged
     * over it) or when tab [id] itself is closed. Null if [id] isn't on screen.
     */
    fun remainingAfterClosing(id: Int): Int? = when (id) {
        first -> second
        second -> first
        else -> null
    }

    /** The divider was let go at [raw]: the tab whose pane it was dragged over (to close), or null. */
    fun closedByRelease(raw: Float): Int? = when {
        raw <= EDGE -> first
        raw >= 1f - EDGE -> second
        else -> null
    }

    companion object {
        const val MIN_RATIO = 0.25f
        const val MAX_RATIO = 0.75f
        /** Letting go of the divider this close to an edge (as a share of the space) closes that pane. */
        const val EDGE = 0.08f

        /** Start split screen: [current] becomes the top/left pane, [other] the focused bottom/right one. */
        fun start(current: Int, other: Int): SplitState? = if (current == other) null else SplitState(current, other)

        fun clampRatio(raw: Float): Float = if (raw.isNaN()) 0.5f else raw.coerceIn(MIN_RATIO, MAX_RATIO)
    }
}

/**
 * Which tabs, besides those on screen, stay in memory: the [limit] most recently used ([recent],
 * newest first) that still have a WebView ([hasLive]). Tabs on screen ([onScreen]: the current tab,
 * or both split-screen tabs) are always live and don't count towards the limit.
 */
fun tabsKeptInMemory(recent: List<Int>, onScreen: Set<Int>, hasLive: (Int) -> Boolean, limit: Int): Set<Int> {
    val keep = LinkedHashSet<Int>()
    for (id in recent) {
        if (keep.size >= limit.coerceAtLeast(0)) break
        if (id !in onScreen && hasLive(id)) keep += id
    }
    return keep
}
