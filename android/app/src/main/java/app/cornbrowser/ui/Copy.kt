/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

/** Text layout rules for short messages (toasts, the after-Flush line). Plain Kotlin, unit-tested. */
object Copy {
    // A sentence end (. ! ?) followed by a space and a new sentence starting with a capital letter
    // or an opening quote. Not before an emoji or a number, so "…new cob. 🌽" keeps its emoji.
    private val SENTENCE_BREAK = Regex("([.!?])\\s+(?=[\\p{Lu}\"“‘])")

    /** Each sentence on its own line (founder, 2026-11-12): "All gone.\nFresh as a new cob. 🌽" */
    fun oneSentencePerLine(text: String): String = text.replace(SENTENCE_BREAK, "$1\n")
}
