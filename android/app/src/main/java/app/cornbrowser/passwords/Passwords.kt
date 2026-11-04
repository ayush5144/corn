/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.passwords

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.JavaScriptReplyProxy
import app.cornbrowser.BuildConfig
import app.cornbrowser.CornController
import app.cornbrowser.Sheet
import app.cornbrowser.browser.TabEntry
import org.json.JSONObject

/** Password sheets (shown through [Sheet.Password]). */
sealed interface PasswordSheet {
    /** "Save password for example.com?" or, with [updateId], "Update password?". */
    data class Save(val origin: String, val username: String, val password: String, val updateId: Long?) : PasswordSheet
    /** Several accounts for this page: pick one to fill. */
    data class Choose(val tabId: Int, val origin: String, val logins: List<SavedLogin>) : PasswordSheet
    /** PIN before filling while the Vault is locked. */
    data class PinToFill(val tabId: Int, val login: SavedLogin) : PasswordSheet
    data class ConfirmDelete(val login: SavedLogin) : PasswordSheet
    data object ConfirmReset : PasswordSheet
}

/** The chip above the toolbar: saved logins for the page's site. */
data class FillOffer(val origin: String, val logins: List<SavedLogin>)

/**
 * Corn's password store: saving, filling and managing logins.
 *
 * - Storage: corn.db `logins` table via the Rust core; usernames and passwords encrypted with
 *   [LoginCipher] (AES-256-GCM), whose key is protected by the Android Keystore ([LoginKeys]).
 * - Pages talk to Corn through [LoginScript]. Corn only listens to https pages, to the main frame or
 *   frames of the same origin, matches logins by exact origin, and sends a saved password to a page
 *   only after the user taps Fill.
 * - Viewing, copying or editing a password needs the PIN (Passwords screen). Filling needs it too
 *   while the Vault is locked, unless "Ask for PIN before filling" is off.
 *
 * Never logs usernames, passwords or sites.
 */
class Passwords(private val context: Context, private val c: CornController) {
    private val keys = LoginKeys(context)
    private val main = Handler(Looper.getMainLooper())
    private val allowLocalHttp = BuildConfig.DEBUG

    /** Bumped when saved logins change, so screens redraw. */
    val version = mutableIntStateOf(0)

    /** The Passwords screen is open inside the Vault. */
    var screenOpen by mutableStateOf(false)

    /** Fill offers per tab id (the browser shows the current tab's). */
    val offers = mutableStateMapOf<Int, FillOffer>()

    /** The frame in each tab that reported a login form, with its reply channel. */
    private class FormFrame(val origin: String, val isMainFrame: Boolean, val reply: JavaScriptReplyProxy)
    private val frames = mutableMapOf<Int, FormFrame>()

    init {
        // Unwrapping the key talks to the keystore (slow on StrongBox): do it once, early, off the main thread.
        Thread({ runCatching { cipher() } }, "corn-passwords-key").start()
    }

    private fun cipher(): LoginCipher? = keys.cipher { runCatching { c.data.loginCount() > 0 }.getOrDefault(false) }

    /** Saved passwords exist but can't be decrypted on this phone (key lost, e.g. data moved to a new phone). */
    fun unreadable(): Boolean = cipher() == null && runCatching { c.data.loginCount() > 0 }.getOrDefault(false)

    // ---------------- reading ----------------

    /** Every login with its username decrypted, most recently used first. Rows that fail to decrypt are skipped. */
    fun all(): List<SavedLogin> {
        val ci = cipher() ?: return emptyList()
        return decryptUsernames(ci, runCatching { c.data.allLogins() }.getOrDefault(emptyList()))
    }

    fun forOrigin(origin: String): List<SavedLogin> {
        val ci = cipher() ?: return emptyList()
        return decryptUsernames(ci, runCatching { c.data.loginsForOrigin(origin) }.getOrDefault(emptyList()))
    }

    private fun decryptUsernames(ci: LoginCipher, rows: List<uniffi.corn_ffi.Login>): List<SavedLogin> {
        var failed = 0
        val out = rows.mapNotNull { r ->
            runCatching { SavedLogin(r.id, r.origin, ci.decrypt(r.usernameEnc, r.origin, LoginCipher.Field.USERNAME), r.lastUsedAt, r.timesUsed) }
                .onFailure { failed++ }.getOrNull()
        }
        if (failed > 0) Log.w(TAG, "$failed saved logins couldn't be decrypted") // a count only
        return out
    }

    /** A login's password in clear. Callers must have checked the PIN (or the fill rule). */
    fun passwordOf(login: SavedLogin): String? {
        val ci = cipher() ?: return null
        val row = runCatching { c.data.loginsForOrigin(login.origin) }.getOrNull()?.firstOrNull { it.id == login.id } ?: return null
        return runCatching { ci.decrypt(row.passwordEnc, row.origin, LoginCipher.Field.PASSWORD) }.getOrNull()
    }

    // ---------------- writing ----------------

    fun save(s: PasswordSheet.Save) {
        c.sheet = null
        val ci = cipher() ?: run { c.showToast("Corn couldn't save this password"); return }
        val u = ci.encrypt(s.username, s.origin, LoginCipher.Field.USERNAME)
        val p = ci.encrypt(s.password, s.origin, LoginCipher.Field.PASSWORD)
        val ok = runCatching {
            if (s.updateId != null) c.data.updateLogin(s.updateId, u, p) else c.data.addLogin(s.origin, u, p)
        }.onFailure { Log.w(TAG, "save failed: ${it.javaClass.simpleName}") }.isSuccess
        changed()
        c.showToast(
            when {
                !ok -> "Corn couldn't save this password"
                s.updateId != null -> "Password updated. Safe in your Vault."
                else -> "Password saved. Safe in your Vault."
            },
        )
    }

    /** Edit from the Passwords screen. */
    fun update(login: SavedLogin, username: String, password: String): Boolean {
        val ci = cipher() ?: return false
        val ok = runCatching {
            c.data.updateLogin(
                login.id,
                ci.encrypt(username, login.origin, LoginCipher.Field.USERNAME),
                ci.encrypt(password, login.origin, LoginCipher.Field.PASSWORD),
            )
        }.isSuccess
        changed()
        return ok
    }

    fun delete(login: SavedLogin) {
        c.sheet = null
        runCatching { c.data.deleteLogin(login.id) }
        changed()
        c.showToast("Password deleted")
    }

    /** "Reset passwords": delete every saved login and start over with a new key. */
    fun resetAll() {
        c.sheet = null
        runCatching { c.data.deleteAllLogins() }
        keys.reset()
        offers.clear()
        changed()
        c.showToast("Passwords reset. Fresh start.")
    }

    fun neverSave(origin: String) {
        c.sheet = null
        c.data.addNeverSave(origin)
        c.showToast("Got it. Corn won't ask on ${LoginMatching.displaySite(origin)} again.")
    }

    private fun changed() {
        version.intValue++
        // Refresh the chips: a save or delete changes what can be filled.
        for (id in offers.keys.toList()) {
            val o = offers[id] ?: continue
            val now = forOrigin(o.origin)
            if (now.isEmpty()) offers.remove(id) else offers[id] = o.copy(logins = now)
        }
    }

    // ---------------- page messages ----------------

    /** A tab's main frame started a new page: forget its form and chip. */
    fun onPageStarted(entry: TabEntry) {
        frames.remove(entry.id)
        offers.remove(entry.id)
    }

    /**
     * A message from [LoginScript]. [sourceOrigin] and [isMainFrame] come from WebView, not the page.
     * Accepted only from https (or local test pages in debug builds), and only from the main frame
     * or a frame with the main page's exact origin.
     */
    fun onMessage(entry: TabEntry, data: String, sourceOrigin: String, isMainFrame: Boolean, reply: JavaScriptReplyProxy) {
        val origin = LoginMatching.originOf(sourceOrigin, allowLocalHttp) ?: return
        if (!isMainFrame && origin != LoginMatching.originOf(entry.url, allowLocalHttp)) return
        val msg = runCatching { JSONObject(data) }.getOrNull() ?: return
        when (msg.optString("t")) {
            "form" -> {
                val known = frames[entry.id]
                if (known == null || !known.isMainFrame || isMainFrame) frames[entry.id] = FormFrame(origin, isMainFrame, reply)
                val logins = forOrigin(origin)
                if (logins.isEmpty()) offers.remove(entry.id) else offers[entry.id] = FillOffer(origin, logins)
            }
            "login" -> {
                val password = msg.optString("p")
                if (password.isEmpty() || password.length > 4096) return
                onSubmitted(origin, msg.optString("u").trim().take(1024), password)
            }
        }
    }

    private fun onSubmitted(origin: String, username: String, password: String) {
        if (!c.settings.offerSavePasswords || origin in c.data.neverSaveList()) return
        if (cipher() == null) return // passwords locked out on this phone: the Passwords screen explains
        val existing = forOrigin(origin)
        val decision = LoginMatching.decideSave(existing, username, password) { id -> existing.firstOrNull { it.id == id }?.let(::passwordOf) }
        val sheet = when (decision) {
            is SaveDecision.AlreadySaved -> return
            is SaveDecision.Update -> PasswordSheet.Save(origin, username, password, decision.id)
            SaveDecision.New -> PasswordSheet.Save(origin, username, password, null)
        }
        // Don't push away something the user is answering (a permission or download question).
        val open = c.sheet
        if (open != null && open !is Sheet.Password) return
        c.sheet = Sheet.Password(sheet)
    }

    // ---------------- filling ----------------

    /** Tap on the chip. One account: fill it. Several: let the user pick. */
    fun onChipTap(tabId: Int) {
        val offer = offers[tabId] ?: return
        val only = offer.logins.singleOrNull()
        if (only != null) requestFill(tabId, only) else c.sheet = Sheet.Password(PasswordSheet.Choose(tabId, offer.origin, offer.logins))
    }

    fun dismissOffer(tabId: Int) {
        offers.remove(tabId)
    }

    /** The PIN is needed while the Vault is locked, unless the user switched that off in Settings. */
    val fillNeedsPin: Boolean get() = c.settings.askPinBeforeFill && !c.vaultUnlocked

    /** The user tapped an account to fill. */
    fun requestFill(tabId: Int, login: SavedLogin) {
        if (fillNeedsPin) c.sheet = Sheet.Password(PasswordSheet.PinToFill(tabId, login)) else fill(tabId, login)
    }

    /** PIN accepted on the fill sheet: that also unlocks the Vault for this session (same PIN). */
    fun onFillPinOk(tabId: Int, login: SavedLogin) {
        c.vaultUnlocked = true
        fill(tabId, login)
    }

    private fun fill(tabId: Int, login: SavedLogin) {
        c.sheet = null
        val frame = frames[tabId]
        val entry = c.tabs.tabs.firstOrNull { it.id == tabId }
        // The form must still be on the login's exact site, in a tab still showing that site.
        if (frame == null || entry == null || frame.origin != login.origin ||
            (frame.isMainFrame.not() && LoginMatching.originOf(entry.url, allowLocalHttp) != login.origin)
        ) {
            c.showToast("The login form is gone. Reload the page and try again.")
            return
        }
        val password = passwordOf(login) ?: run { c.showToast("Corn couldn't read this password"); return }
        val msg = JSONObject().put("t", "fill").put("u", login.username).put("p", password).toString()
        runCatching { frame.reply.postMessage(msg) }.onFailure { c.showToast("The login form is gone. Reload the page and try again.") }
        runCatching { c.data.touchLogin(login.id) }
        offers.remove(tabId)
        version.intValue++
    }

    // ---------------- clipboard ----------------

    private val clearClipboard = Runnable {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return@Runnable
        // Clear only if it's still Corn's copy (when Android lets us check; otherwise clear to be safe).
        val label = runCatching { cm.primaryClipDescription?.label?.toString() }.getOrNull()
        if (label != null && label != CLIP_LABEL) return@Runnable
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip() else cm.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    /**
     * Copy a username or password. Marked sensitive (Android 13+ hides it from the clipboard preview
     * and keyboard suggestions) and cleared after 45 seconds.
     */
    fun copy(text: String, what: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE", true)
        }
        cm.setPrimaryClip(clip)
        main.removeCallbacks(clearClipboard)
        main.postDelayed(clearClipboard, 45_000L)
        // Android 13+ shows its own "Copied" confirmation.
        if (Build.VERSION.SDK_INT < 33) c.showToast("$what copied. Corn clears it in 45 seconds.") else c.showToast("Corn clears the clipboard in 45 seconds")
    }

    private companion object {
        const val TAG = "CornPasswords"
        const val CLIP_LABEL = "Corn password"
    }
}
