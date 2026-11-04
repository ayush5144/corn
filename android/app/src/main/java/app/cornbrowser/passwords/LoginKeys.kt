/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.passwords

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The key that encrypts saved passwords (the "data key"), modelled on DuckDuckGo Android:
 *
 * - The data key is 256 random bits. It is never written down in clear: only "wrapped", i.e.
 *   encrypted by a key-encryption key (KEK) that lives inside the Android Keystore (StrongBox chip
 *   when the phone has one), can't be exported, and never leaves the secure hardware.
 * - The wrapped data key sits in Corn's private settings file `corn_logins` (not in corn.db).
 *
 * If the KEK is gone (Corn's data copied to a new phone, or the keystore was reset), the wrapped key
 * can't be unwrapped and every saved password is unreadable: [cipher] returns null and the UI offers
 * "Reset passwords" ([reset]). Nothing here ever logs a key, username or password.
 */
class LoginKeys(context: Context) {
    private val prefs = context.getSharedPreferences("corn_logins", Context.MODE_PRIVATE)

    @Volatile private var ready: LoginCipher? = null

    /**
     * The cipher for saved logins, creating the key the first time.
     * Null means passwords exist but can't be decrypted on this phone.
     * Slow the first time (keystore): call off the main thread when possible.
     */
    @Synchronized
    fun cipher(hasSavedLogins: () -> Boolean): LoginCipher? {
        ready?.let { return it }
        val wrapped = prefs.getString(PREF_WRAPPED, null)
        val c = if (wrapped == null) {
            // Logins without a key: the key was lost (e.g. settings file not restored). Don't
            // make a new key over them; the user decides with "Reset passwords".
            if (hasSavedLogins()) null else createKey()
        } else {
            // Unreadable key but nothing saved with it: nothing to lose, start over with a new key.
            unwrap(wrapped) ?: if (hasSavedLogins()) null else { forget(); createKey() }
        }
        ready = c
        return c
    }

    /** Forget the key (and the keystore entry). Call only after deleting every saved login. */
    @Synchronized
    fun reset() = forget()

    private fun forget() {
        ready = null
        prefs.edit().remove(PREF_WRAPPED).commit()
        runCatching { keyStore().deleteEntry(KEK_ALIAS) }
    }

    private fun createKey(): LoginCipher? = runCatching {
        val kek = getOrCreateKek()
        val dataKey = LoginCipher.newKeyBytes()
        val cipher = Cipher.getInstance(KEK_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek) // the keystore picks the IV
        val ct = cipher.doFinal(dataKey)
        val stored = "v1:" + b64(cipher.iv) + ":" + b64(ct)
        prefs.edit().putString(PREF_WRAPPED, stored).commit()
        LoginCipher(LoginCipher.keyFrom(dataKey)).also { dataKey.fill(0) }
    }.onFailure { Log.w(TAG, "couldn't create the password key: ${it.javaClass.simpleName}") }.getOrNull()

    private fun unwrap(stored: String): LoginCipher? = runCatching {
        val parts = stored.split(':')
        require(parts.size == 3 && parts[0] == "v1")
        val kek = keyStore().getKey(KEK_ALIAS, null) as? SecretKey ?: error("no keystore key")
        val cipher = Cipher.getInstance(KEK_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, unb64(parts[1])))
        val dataKey = cipher.doFinal(unb64(parts[2]))
        LoginCipher(LoginCipher.keyFrom(dataKey)).also { dataKey.fill(0) }
    }.onFailure { Log.w(TAG, "saved passwords can't be unlocked on this phone: ${it.javaClass.simpleName}") }.getOrNull()

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKek(): SecretKey {
        (keyStore().getKey(KEK_ALIAS, null) as? SecretKey)?.let { return it }
        // StrongBox (a separate security chip) when the phone has one, else the normal keystore (TEE).
        if (Build.VERSION.SDK_INT >= 28) {
            runCatching { return generateKek(strongBox = true) }
        }
        return generateKek(strongBox = false)
    }

    private fun generateKek(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(KEK_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .apply { if (strongBox && Build.VERSION.SDK_INT >= 28) setIsStrongBoxBacked(true) }
            .build()
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(spec)
        return gen.generateKey()
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val TAG = "CornPasswords"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEK_ALIAS = "corn_logins_kek"
        const val KEK_TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREF_WRAPPED = "wrapped_data_key"
    }
}
