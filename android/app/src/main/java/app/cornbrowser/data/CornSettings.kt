/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** When Corn flushes on its own. Default: KEEP (Kept Session). */
enum class FlushMode(val key: String, val delayMs: Long?) {
    KEEP("keep", null),
    LEAVE_NOW("leave0", 0L),
    LEAVE_5("leave5", 5 * 60_000L),
    LEAVE_30("leave30", 30 * 60_000L);

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: KEEP
    }
}

/**
 * Corn's plain settings: on/off values only, no browsing data
 * (the Keep List and per-site choices live in the SQLite store).
 * Every field is Compose state, so screens redraw when it changes.
 * Defaults match docs/01-product/01-prd.md §8.
 */
class CornSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("corn_settings", Context.MODE_PRIVATE)

    var onboarded by pref("onboarded", false)
    var flushMode by mutableStateOf(FlushMode.of(prefs.getString("flush_mode", null)))
        private set
    var inactivity by pref("inactivity", false)
    var inactivityMinutes by intPref("inactivity_minutes", 30)
    var flushSound by pref("flush_sound", false)
    var flushConfirm by pref("flush_confirm", false)
    var appLock by pref("app_lock", false)
    var vaultLock by pref("vault_lock", true)
    var blackRecents by pref("black_recents", true)
    var engine by stringPref("engine", "ddg")
    var addressBarBottom by pref("address_bar_bottom", false)

    fun chooseFlushMode(mode: FlushMode) {
        flushMode = mode
        prefs.edit().putString("flush_mode", mode.key).apply()
    }

    // ---------- PIN (shared by the Vault lock and App Lock) ----------

    val hasPin: Boolean get() = prefs.contains("pin_hash")

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("pin_salt", salt.hex()).putString("pin_hash", hash(pin, salt).hex()).apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString("pin_salt", null)?.unhex() ?: return false
        val expected = prefs.getString("pin_hash", null) ?: return false
        return hash(pin, salt).hex() == expected
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    // ---------- helpers: Compose state backed by SharedPreferences ----------

    private fun pref(key: String, default: Boolean) = object : kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        val s = mutableStateOf(prefs.getBoolean(key, default))
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = s.value
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Boolean) {
            s.value = value
            prefs.edit().putBoolean(key, value).apply()
        }
    }

    private fun intPref(key: String, default: Int) = object : kotlin.properties.ReadWriteProperty<Any?, Int> {
        val s = mutableStateOf(prefs.getInt(key, default))
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = s.value
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int) {
            s.value = value
            prefs.edit().putInt(key, value).apply()
        }
    }

    private fun stringPref(key: String, default: String) = object : kotlin.properties.ReadWriteProperty<Any?, String> {
        val s = mutableStateOf(prefs.getString(key, default) ?: default)
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = s.value
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) {
            s.value = value
            prefs.edit().putString(key, value).apply()
        }
    }

    // ---------- all-time totals: two plain numbers, nothing about which site or what was blocked ----------

    // Kept in memory while browsing and written to disk only when leaving Corn (see persist()).
    /** Requests blocked by the ad + tracker filters (each blocked request counts once). */
    var totalBlocked by mutableStateOf(prefs.getLong("total_blocked", 0L))
    /** Pop-ups denied + redirects stopped. */
    var totalPopups by mutableStateOf(prefs.getLong("total_popups", 0L))

    /** Last touch or playing video. In memory; written on persist(). */
    var lastActivityAt: Long = prefs.getLong("last_activity_at", System.currentTimeMillis())

    /** Write the in-memory counters and timestamps in one go (called when leaving Corn). */
    fun persist() {
        prefs.edit()
            .putLong("total_blocked", totalBlocked)
            .putLong("total_popups", totalPopups)
            .putLong("last_activity_at", lastActivityAt)
            .apply()
    }

    // ---------- timestamps used by Auto-Flush (not user settings) ----------

    var leftAt: Long
        get() = prefs.getLong("left_at", 0L)
        set(v) { prefs.edit().putLong("left_at", v).apply() }
}

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
private fun String.unhex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
