/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.ui

/** Text layout rules for short messages (toasts, the after-Flush line). Plain Kotlin, unit-tested. */
object Copy {
    // A sentence end (. ! ?) followed by a space and a new sentence starting with a capital letter
    // or an opening quote. Not before an emoji ("Swirl, swirl, gone. 🌀" stays on one line) or a number.
    private val SENTENCE_BREAK = Regex("([.!?])\\s+(?=[\\p{Lu}\"“‘])")

    /** Each sentence on its own line (founder, 2026-11-11): "All gone.\nEven we don't know what it was." */
    fun oneSentencePerLine(text: String): String = text.replace(SENTENCE_BREAK, "$1\n")
}
