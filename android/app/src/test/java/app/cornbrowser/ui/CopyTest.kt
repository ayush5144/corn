package app.cornbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CopyTest {
    @Test fun twoSentencesTwoLines() =
        assertEquals("Flushed.\nNobody saw nothing.", Copy.oneSentencePerLine("Flushed. Nobody saw nothing."))

    @Test fun emojiStaysWithItsSentence() =
        assertEquals("All gone.\nFresh as a new cob. 🌽", Copy.oneSentencePerLine("All gone. Fresh as a new cob. 🌽"))

    @Test fun messageWithASiteName() =
        assertEquals("Got it.\nCorn won't ask on example.com again.", Copy.oneSentencePerLine("Got it. Corn won't ask on example.com again."))

    @Test fun oneSentenceUnchanged() =
        assertEquals("Opened in a new tab", Copy.oneSentencePerLine("Opened in a new tab"))

    @Test fun lowercaseOrNumberAfterDotNoBreak() {
        assertEquals("Version 0.2. 3 tabs", Copy.oneSentencePerLine("Version 0.2. 3 tabs"))
        assertEquals("Saved to example.com today.", Copy.oneSentencePerLine("Saved to example.com today."))
    }

    @Test fun questionAndExclamation() =
        assertEquals("Done!\nFeel free.", Copy.oneSentencePerLine("Done! Feel free."))
}
