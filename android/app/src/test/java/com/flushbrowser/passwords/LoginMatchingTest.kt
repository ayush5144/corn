/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.passwords

import com.flushbrowser.passwords.LoginMatching.decideSave
import com.flushbrowser.passwords.LoginMatching.originOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginMatchingTest {
    @Test fun originIsSchemeHostAndNonDefaultPort() {
        assertEquals("https://example.com", originOf("https://Example.COM/login?next=/a#x"))
        assertEquals("https://example.com", originOf("https://example.com:443/"))
        assertEquals("https://example.com:8443", originOf("https://example.com:8443/login"))
        assertEquals("https://www.example.com", originOf("https://www.example.com/"))
        assertEquals("https://example.com", originOf("https://user:pw@example.com/"))
    }

    @Test fun onlyHttps() {
        assertNull(originOf("http://example.com/login"))
        assertNull(originOf("file:///sdcard/a.html"))
        assertNull(originOf("data:text/html,hi"))
        assertNull(originOf("javascript:alert(1)"))
        assertNull(originOf("about:blank"))
        assertNull(originOf(""))
        assertNull(originOf(null))
        assertNull(originOf("not a url"))
    }

    @Test fun localHttpOnlyWhenAllowed() {
        assertNull(originOf("http://10.0.2.2:8000/login.html"))
        assertEquals("http://10.0.2.2:8000", originOf("http://10.0.2.2:8000/login.html", allowLocalHttp = true))
        assertEquals("http://localhost", originOf("http://localhost/", allowLocalHttp = true))
        assertNull(originOf("http://example.com/", allowLocalHttp = true))
    }

    @Test fun displaySite() {
        assertEquals("example.com", LoginMatching.displaySite("https://www.example.com"))
        assertEquals("mail.example.com:8443", LoginMatching.displaySite("https://mail.example.com:8443"))
    }

    private fun login(id: Long, user: String, last: Long = 0, used: Long = 0, origin: String = "https://example.com") =
        SavedLogin(id, origin, user, last, used)

    @Test fun saveDecisions() {
        val saved = listOf(login(1, "ana@example.com"), login(2, "bo"))
        val pw = mapOf(1L to "old", 2L to "bo-pass")
        assertEquals(SaveDecision.New, decideSave(emptyList(), "ana@example.com", "x") { null })
        assertEquals(SaveDecision.New, decideSave(saved, "cy", "x") { pw[it] })
        assertEquals(SaveDecision.AlreadySaved(1), decideSave(saved, "ana@example.com", "old") { pw[it] })
        assertEquals(SaveDecision.Update(1), decideSave(saved, "ana@example.com", "new") { pw[it] })
        // Password-only form: with one login it's that login; with several, Flush can't tell, so it's new.
        assertEquals(SaveDecision.Update(1), decideSave(listOf(login(1, "ana")), "", "new") { "old" })
        assertEquals(SaveDecision.AlreadySaved(1), decideSave(listOf(login(1, "ana")), "", "old") { "old" })
        assertEquals(SaveDecision.New, decideSave(saved, "", "new") { pw[it] })
    }

    @Test fun searchAndGrouping() {
        val a = login(1, "ana", last = 10, origin = "https://b-site.example")
        val b = login(2, "bo", last = 30, origin = "https://b-site.example")
        val c = login(3, "cy", last = 20, origin = "https://www.a-site.example")
        assertTrue(LoginMatching.matchesSearch(c, "A-SITE"))
        assertTrue(LoginMatching.matchesSearch(a, "an"))
        assertFalse(LoginMatching.matchesSearch(a, "zzz"))
        val groups = LoginMatching.groupBySite(listOf(a, b, c))
        assertEquals(listOf("https://www.a-site.example", "https://b-site.example"), groups.map { it.first })
        assertEquals(listOf(2L, 1L), groups[1].second.map { it.id }) // most recently used first
    }
}
