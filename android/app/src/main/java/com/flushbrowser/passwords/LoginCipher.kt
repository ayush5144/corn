/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.passwords

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts saved usernames and passwords with AES-256-GCM.
 *
 * Plain JVM crypto with the key passed in, so it runs in unit tests (the Android Keystore that
 * protects the key lives in [LoginKeys]). Each value gets a fresh random 12-byte IV.
 *
 * Blob layout: `[version 0x01][12-byte IV][ciphertext + 16-byte GCM tag]`.
 *
 * The site (origin) and the field name are bound in as associated data, so a blob copied into
 * another site's row, or a username blob swapped with a password blob, fails to decrypt.
 */
class LoginCipher(private val key: SecretKey) {
    init {
        require(key.encoded == null || key.encoded.size == KEY_BYTES) { "AES-256 key required" }
    }

    fun encrypt(plain: String, origin: String, field: Field): ByteArray {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(origin, field))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return byteArrayOf(VERSION) + iv + ct
    }

    /** Throws [LoginDecryptException] if the blob is damaged, was made with another key, or belongs elsewhere. */
    fun decrypt(blob: ByteArray, origin: String, field: Field): String {
        if (blob.size < 1 + IV_BYTES + TAG_BITS / 8 || blob[0] != VERSION) throw LoginDecryptException("unknown format")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 1, IV_BYTES))
            cipher.updateAAD(aad(origin, field))
            String(cipher.doFinal(blob, 1 + IV_BYTES, blob.size - 1 - IV_BYTES), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw LoginDecryptException("authentication failed")
        } catch (e: java.security.GeneralSecurityException) {
            throw LoginDecryptException(e.javaClass.simpleName)
        }
    }

    enum class Field(val tag: String) { USERNAME("u"), PASSWORD("p") }

    companion object {
        const val KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val VERSION: Byte = 1
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val random = SecureRandom()

        private fun aad(origin: String, field: Field) = "flush-login/v1|$origin|${field.tag}".toByteArray(Charsets.UTF_8)

        /** A new random 256-bit data key. */
        fun newKeyBytes(): ByteArray = ByteArray(KEY_BYTES).also { random.nextBytes(it) }

        fun keyFrom(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")
    }
}

/** A saved value couldn't be decrypted. Carries no data from the value itself. */
class LoginDecryptException(reason: String) : Exception(reason)
