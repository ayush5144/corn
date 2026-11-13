/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

/**
 * Split screen: two tabs on screen at once, [first] on top (or on the left in landscape) and
 * [second] below it (or on the right). The pane the user last touched is focused; the current
 * tab ([TabManager.currentId]) is always the focused pane's tab, so the address bar, toolbar,
 * find bar and sheets act on it. [ratio] is the first pane's share of the space (25–75 %).
 *
 * Immutable and free of Android code so the rules can be unit-tested; [TabManager] holds the
 * current value (null when split screen is off). Not saved: a restart shows a single tab.
 *
 * **A pair** (founder, 2026-11-13, option B): the two tabs belong together. Opening or switching to
 * any other tab shows it full screen and parks the pair; switching back to either of its tabs brings
 * the split back ([afterSwitch]). Like side-by-side tabs in desktop browsers.
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

        /**
         * Switching to tab [id], given the [active] split (on screen) and the [parked] one (remembered
         * while another tab is shown):
         * - a tab of the active pair: focus its pane;
         * - any other tab while split: shown full screen, the pair is parked;
         * - a tab of the parked pair: the split comes back, focused on that tab;
         * - anything else: no split; a parked pair stays parked.
         */
        fun afterSwitch(active: SplitState?, parked: SplitState?, id: Int): Pair<SplitState?, SplitState?> = when {
            active != null && id in active -> active.focus(id) to null
            active != null -> null to active
            parked != null && id in parked -> parked.focus(id) to null
            else -> null to parked
        }

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
