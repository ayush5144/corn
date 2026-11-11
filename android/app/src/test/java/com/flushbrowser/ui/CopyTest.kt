package com.flushbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CopyTest {
    @Test fun twoSentencesTwoLines() =
        assertEquals("All gone.\nEven we don't know what it was.", Copy.oneSentencePerLine("All gone. Even we don't know what it was."))

    @Test fun messageWithASiteName() =
        assertEquals("Got it.\nFlush won't ask on example.com again.", Copy.oneSentencePerLine("Got it. Flush won't ask on example.com again."))

    @Test fun emojiStaysOnTheLine() =
        assertEquals("Swirl, swirl, gone. 🌀", Copy.oneSentencePerLine("Swirl, swirl, gone. 🌀"))

    @Test fun oneSentenceUnchanged() =
        assertEquals("Whoosh, clean slate", Copy.oneSentencePerLine("Whoosh, clean slate"))

    @Test fun lowercaseOrNumberAfterDotNoBreak() {
        assertEquals("Version 0.2. 3 tabs", Copy.oneSentencePerLine("Version 0.2. 3 tabs"))
        assertEquals("Saved to example.com today.", Copy.oneSentencePerLine("Saved to example.com today."))
    }

    @Test fun questionAndExclamation() =
        assertEquals("Done!\nFeel free.", Copy.oneSentencePerLine("Done! Feel free."))
}
