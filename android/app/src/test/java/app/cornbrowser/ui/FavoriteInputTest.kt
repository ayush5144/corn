/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The "Add a favourite" sheet's address field. */
class FavoriteInputTest {
    @Test fun bareSiteIsAnAddress() = assertEquals("http://example.com", FavoriteInput.address("  example.com "))
    @Test fun pathKept() = assertEquals("http://example.com/news?a=1", FavoriteInput.address("example.com/news?a=1"))
    @Test fun httpsKept() = assertEquals("https://www.example.org/", FavoriteInput.address("https://www.example.org/"))
    @Test fun localhost() = assertEquals("http://localhost:8080", FavoriteInput.address("localhost:8080"))
    @Test fun ipAddress() = assertEquals("http://10.0.2.2:8765/", FavoriteInput.address("10.0.2.2:8765/"))

    @Test fun notAddresses() {
        for (bad in listOf("", "   ", "hello", "two words.com", "ftp://example.com", "javascript:alert(1)", "http://", ".com", "example."))
            assertNull(bad, FavoriteInput.address(bad))
    }

    @Test fun siteName() {
        assertEquals("example.com", FavoriteInput.site("https://WWW.Example.com/a"))
        assertEquals("news.example.org", FavoriteInput.site("http://news.example.org"))
    }
}
