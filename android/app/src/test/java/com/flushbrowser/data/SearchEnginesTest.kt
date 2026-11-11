/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEnginesTest {
    private fun url(id: String, safe: Boolean) = SearchEngines.byId(id).urlFor("corn browser", safe)

    @Test fun safeSearchOff() {
        assertEquals("https://duckduckgo.com/?q=corn+browser&kp=-2", url("ddg", false))
        assertEquals("https://www.google.com/search?q=corn+browser&safe=off", url("google", false))
        assertEquals("https://search.brave.com/search?q=corn+browser&safesearch=off", url("brave", false))
        assertEquals("https://www.bing.com/search?q=corn+browser&adlt=off", url("bing", false))
        assertEquals("https://www.startpage.com/do/search?q=corn+browser", url("startpage", false))
    }

    @Test fun safeSearchOn() {
        assertEquals("https://duckduckgo.com/?q=corn+browser&kp=1", url("ddg", true))
        assertEquals("https://www.google.com/search?q=corn+browser&safe=active", url("google", true))
        assertEquals("https://search.brave.com/search?q=corn+browser&safesearch=strict", url("brave", true))
        assertEquals("https://www.bing.com/search?q=corn+browser&adlt=strict", url("bing", true))
    }

    @Test
    fun typedAddressesStartFromHttpSoTheUpgradeCanFallBack() {
        val ddg = SearchEngines.byId("ddg")
        assertEquals("http://example.com/page", SearchEngines.resolve("example.com/page", ddg, false))
        assertEquals("https://example.com/", SearchEngines.resolve("https://example.com/", ddg, false))
        assertEquals("http://example.com/", SearchEngines.resolve("http://example.com/", ddg, false))
        assertEquals("https://www.startpage.com/do/search?q=corn+browser", url("startpage", true))
    }

    @Test fun webAddressesAreNotSearched() {
        val ddg = SearchEngines.byId("ddg")
        // Typed without a scheme: http, which Corn's HTTPS upgrade turns into https (with http fallback).
        assertEquals("http://example.com", SearchEngines.resolve("example.com", ddg, false))
        assertEquals("http://example.com/a", SearchEngines.resolve("http://example.com/a", ddg, true))
        assertEquals("https://duckduckgo.com/?q=hello+world&kp=1", SearchEngines.resolve("hello world", ddg, true))
    }
}
