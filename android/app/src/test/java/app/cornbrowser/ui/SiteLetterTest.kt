/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SiteLetterTest {
    @Test fun languageAndMobileSubdomainsUseTheSiteName() {
        assertEquals("W", SiteLetter.of("en.wikipedia.org"))
        assertEquals("W", SiteLetter.of("fr.m.wikipedia.org"))
        assertEquals("Y", SiteLetter.of("m.youtube.com"))
        assertEquals("G", SiteLetter.of("mail.google.com"))
    }

    @Test fun plainSites() {
        assertEquals("W", SiteLetter.of("wikipedia.org"))
        assertEquals("D", SiteLetter.of("www.duckduckgo.com"))
        assertEquals("L", SiteLetter.of("localhost"))
    }

    @Test fun twoPartEndings() {
        assertEquals("B", SiteLetter.of("www.bbc.co.uk"))
        assertEquals("A", SiteLetter.of("abc.net.au"))
        assertEquals("T", SiteLetter.of("timesofindia.co.in"))
    }

    @Test fun addressesAndBlanksFallBack() {
        assertEquals("1", SiteLetter.of("10.0.2.2"))
        assertEquals("?", SiteLetter.of(""))
    }
}
