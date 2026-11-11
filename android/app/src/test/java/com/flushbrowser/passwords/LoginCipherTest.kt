/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.passwords

import com.flushbrowser.passwords.LoginCipher.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LoginCipherTest {
    private val origin = "https://example.com"
    private fun cipher() = LoginCipher(LoginCipher.keyFrom(LoginCipher.newKeyBytes()))

    @Test fun roundTrip() {
        val c = cipher()
        for (text in listOf("hunter2", "", "pässwörd 🌽 with spaces", "x".repeat(2000))) {
            assertEquals(text, c.decrypt(c.encrypt(text, origin, Field.PASSWORD), origin, Field.PASSWORD))
        }
    }

    @Test fun freshIvEveryTime() {
        val c = cipher()
        val a = c.encrypt("same", origin, Field.PASSWORD)
        val b = c.encrypt("same", origin, Field.PASSWORD)
        assertFalse(a.contentEquals(b))
        assertNotEquals(a.copyOfRange(1, 13).toList(), b.copyOfRange(1, 13).toList())
        assertEquals(1 + 12 + 4 + 16, a.size) // version + IV + "same" + GCM tag
    }

    @Test fun ciphertextDoesNotContainThePlaintext() {
        val blob = cipher().encrypt("PLAINMARKER", origin, Field.PASSWORD)
        assertFalse(String(blob, Charsets.ISO_8859_1).contains("PLAINMARKER"))
    }

    @Test fun wrongKeyFails() {
        val blob = cipher().encrypt("secret", origin, Field.PASSWORD)
        assertThrows(LoginDecryptException::class.java) { cipher().decrypt(blob, origin, Field.PASSWORD) }
    }

    @Test fun tamperingFails() {
        val c = cipher()
        val blob = c.encrypt("secret", origin, Field.PASSWORD)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        assertThrows(LoginDecryptException::class.java) { c.decrypt(blob, origin, Field.PASSWORD) }
        assertThrows(LoginDecryptException::class.java) { c.decrypt(byteArrayOf(1, 2, 3), origin, Field.PASSWORD) }
    }

    @Test fun boundToSiteAndField() {
        val c = cipher()
        val blob = c.encrypt("secret", origin, Field.PASSWORD)
        assertThrows(LoginDecryptException::class.java) { c.decrypt(blob, "https://evil.example", Field.PASSWORD) }
        assertThrows(LoginDecryptException::class.java) { c.decrypt(blob, origin, Field.USERNAME) }
    }
}
