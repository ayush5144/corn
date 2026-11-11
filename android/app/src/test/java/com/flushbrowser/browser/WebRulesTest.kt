package com.flushbrowser.browser

import com.flushbrowser.browser.ExternalLinks.Kind
import com.flushbrowser.browser.WindowPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebRulesTest {
    private val never = Long.MAX_VALUE

    @Test fun tappedWindowOpensAsTab() =
        assertEquals(Decision.OPEN_TAB, WindowPolicy.newWindow(userGesture = true, siteBlocksPopups = true, msSinceLastOpened = never))

    @Test fun windowWithoutTapIsBlocked() =
        assertEquals(Decision.BLOCK, WindowPolicy.newWindow(userGesture = false, siteBlocksPopups = true, msSinceLastOpened = never))

    @Test fun secondWindowFromOneTapIsBlocked() =
        assertEquals(Decision.BLOCK, WindowPolicy.newWindow(userGesture = true, siteBlocksPopups = true, msSinceLastOpened = 200))

    @Test fun nextTapAfterBurstWindowOpens() =
        assertEquals(Decision.OPEN_TAB, WindowPolicy.newWindow(userGesture = true, siteBlocksPopups = true, msSinceLastOpened = WindowPolicy.BURST_MS))

    @Test fun siteAllowedPopupsOpensEverything() =
        assertEquals(Decision.OPEN_TAB, WindowPolicy.newWindow(userGesture = false, siteBlocksPopups = false, msSinceLastOpened = 0))

    @Test fun linkKinds() {
        assertEquals(Kind.WEB, ExternalLinks.kind("https://example.com"))
        assertEquals(Kind.WEB, ExternalLinks.kind("HTTP://example.com"))
        assertEquals(Kind.CALL, ExternalLinks.kind("tel:+15551234567"))
        assertEquals(Kind.EMAIL, ExternalLinks.kind("mailto:a@example.com"))
        assertEquals(Kind.TEXT, ExternalLinks.kind("sms:+1555"))
        assertEquals(Kind.TEXT, ExternalLinks.kind("smsto:+1555"))
        assertEquals(Kind.MAP, ExternalLinks.kind("geo:37.4,-122.0"))
        assertEquals(Kind.APP, ExternalLinks.kind("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals(Kind.APP, ExternalLinks.kind("market://details?id=x"))
        assertEquals(Kind.APP, ExternalLinks.kind("whatsapp://send?text=hi"))
        assertEquals(Kind.IGNORE, ExternalLinks.kind("javascript:alert(1)"))
        assertEquals(Kind.IGNORE, ExternalLinks.kind("file:///sdcard/x"))
        assertEquals(Kind.IGNORE, ExternalLinks.kind("content://x/y"))
        assertEquals(Kind.IGNORE, ExternalLinks.kind("about:blank"))
        assertEquals(Kind.IGNORE, ExternalLinks.kind("no scheme here"))
    }

    @Test fun sheetText() {
        assertEquals("Call this number?", ExternalLinks.title(Kind.CALL, "Phone"))
        assertEquals("Open in WhatsApp?", ExternalLinks.title(Kind.APP, "WhatsApp"))
        assertEquals("Open in another app?", ExternalLinks.title(Kind.APP, null))
        assertEquals("+1 555 123", ExternalLinks.detail(Kind.CALL, "tel:+1%20555%20123"))
        assertEquals("hello@example.com", ExternalLinks.detail(Kind.EMAIL, "mailto:hello@example.com?subject=Hi"))
    }

    @Test fun fallbackOnlyWeb() {
        assertEquals("https://example.com/app", ExternalLinks.fallback("https://example.com/app"))
        assertNull(ExternalLinks.fallback("javascript:alert(1)"))
        assertNull(ExternalLinks.fallback("intent://x"))
        assertNull(ExternalLinks.fallback(null))
    }

    @Test fun linkTitleSkipsDurationsAndCounters() {
        assertEquals("Luis Fonsi - Despacito", LinkTitles.pick(listOf(null, "", "4:02", "  1:02:33 ", "Luis Fonsi -\n Despacito")))
        assertEquals("Watch later", LinkTitles.pick(listOf("Watch later", "Something else")))
        assertNull(LinkTitles.pick(listOf("12", "3/5", " ")))
        assertEquals(200, LinkTitles.pick(listOf("x".repeat(500)))!!.length)
    }
}
