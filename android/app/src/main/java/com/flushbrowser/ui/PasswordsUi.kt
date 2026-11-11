/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flushbrowser.AppController
import com.flushbrowser.Sheet
import com.flushbrowser.passwords.LoginMatching
import com.flushbrowser.passwords.PasswordSheet
import com.flushbrowser.passwords.SavedLogin

// ---------------- browser: the "Fill as …" chip ----------------

/** Above the toolbar when the page has a login form and Corn has logins for its exact site. */
@Composable
fun BoxScope.PasswordFillChip(c: AppController) {
    val tabId = c.tabs.currentId
    val offer = c.passwords.offers[tabId] ?: return
    if (c.sheet != null || c.addressQuery != null || c.findQuery != null) return
    val text = offer.logins.singleOrNull()?.let { "Fill as ${it.username.ifBlank { "your saved login" }}" }
        ?: "Fill password · ${offer.logins.size} accounts"
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            .shadow(8.dp, RoundedCornerShape(21.dp))
            .clip(RoundedCornerShape(21.dp)).background(C.Surface2).border(1.dp, C.Line, RoundedCornerShape(21.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.heightIn(min = 42.dp).widthIn(max = 280.dp).clickable { c.passwords.onChipTap(tabId) }.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(Glyphs.Key, 17.dp, C.Accent)
            Spacer(Modifier.width(8.dp))
            T(text, 14.sp, weight = FontWeight.SemiBold, maxLines = 1)
        }
        IconBtn(Glyphs.X, "Not now", { c.passwords.dismissOffer(tabId) }, tint = C.Muted, size = 38.dp, iconSize = 15.dp)
    }
}

// ---------------- sheets ----------------

@Composable
fun PasswordSheetContent(c: AppController, kind: PasswordSheet) {
    val p = c.passwords
    when (kind) {
        is PasswordSheet.Save -> {
            val site = LoginMatching.displaySite(kind.origin)
            val update = kind.updateId != null
            SheetTitle(
                if (update) "Update password for $site?" else "Save password for $site?",
                if (update) "You logged in with a new password. Want Corn to remember it?" else "Locked in your Vault with your PIN. Never leaves this phone.",
            )
            SettingRow(
                kind.username.ifBlank { "No username" },
                "Password ••••••••",
                leading = { LetterIcon(site, 34.dp) },
            )
            Spacer(Modifier.height(14.dp))
            BigButton(if (update) "Update" else "Save", { p.save(kind) }, icon = Glyphs.Key)
            Spacer(Modifier.height(10.dp))
            BigButton("Not now", { c.sheet = null }, ghost = true)
            if (!update) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).clickable { p.neverSave(kind.origin) }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { T("Never for this site", 15.sp, C.Accent, FontWeight.SemiBold) }
            }
        }
        is PasswordSheet.Choose -> {
            SheetTitle("Fill which account?", LoginMatching.displaySite(kind.origin))
            kind.logins.forEach { login ->
                SettingRow(
                    login.username.ifBlank { "No username" },
                    lastUsedText(login),
                    onClick = { p.requestFill(kind.tabId, login) },
                    leading = { Glyph(Glyphs.Key, tint = C.Accent) },
                )
            }
            if (p.fillNeedsPin) T("Your Vault is locked, so Corn asks for your PIN first.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
        }
        is PasswordSheet.PinToFill -> {
            Box(Modifier.fillMaxWidth().height(500.dp)) {
                PinGate(
                    settings = c.settings,
                    lockedTitle = "PIN to fill",
                    lockedSub = "Your Vault is locked. Same PIN as always.",
                    createSub = "Choose a 4-digit PIN to protect your passwords",
                    onSuccess = { p.onFillPinOk(kind.tabId, kind.login) },
                )
            }
        }
        is PasswordSheet.ConfirmDelete -> {
            SheetTitle(
                "Delete this password?",
                "${kind.login.username.ifBlank { "The login" }} on ${LoginMatching.displaySite(kind.login.origin)} goes for good.",
            )
            BigButton("Delete", { p.delete(kind.login) }, icon = Glyphs.Trash)
            Spacer(Modifier.height(10.dp))
            BigButton("Cancel", { c.sheet = null }, ghost = true)
        }
        PasswordSheet.ConfirmReset -> {
            SheetTitle("Reset passwords?", "Every saved password is deleted. There's no getting them back.")
            BigButton("Reset passwords", { p.resetAll() }, icon = Glyphs.Trash)
            Spacer(Modifier.height(10.dp))
            BigButton("Cancel", { c.sheet = null }, ghost = true)
        }
    }
}

private fun lastUsedText(login: SavedLogin): String =
    if (login.timesUsed == 0L) "Saved ${relative(login.lastUsedAt)}" else "Last used ${relative(login.lastUsedAt)}"

private fun relative(at: Long): String =
    DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().replaceFirstChar { it.lowercase() }

// ---------------- Vault: entry row ----------------

/** The "Passwords" row at the top of the Vault. */
@Composable
fun PasswordsEntry(c: AppController) {
    val count = remember(c.passwords.version.intValue) { runCatching { c.data.loginCount() }.getOrDefault(0L) }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface)
            .clickable { c.passwords.screenOpen = true }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(C.Surface2), contentAlignment = Alignment.Center) {
            Glyph(Glyphs.Key, 20.dp, C.Accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T("Passwords", 16.sp, weight = FontWeight.SemiBold)
            T(if (count == 1L) "1 saved login" else "$count saved logins", 13.sp, C.Muted)
        }
        Glyph(Glyphs.ChevRight, 18.dp, C.Muted)
    }
}

// ---------------- Vault: Passwords screen ----------------

/**
 * Saved logins, grouped by site. Usernames are visible once the Vault is open; showing, copying or
 * editing a password asks for the PIN (once per visit to this screen). No screenshots or
 * recent-apps preview while it's open (FLAG_SECURE).
 */
@Composable
fun PasswordsScreen(c: AppController) {
    val p = c.passwords
    SecureWindow(c)
    DisposableEffect(Unit) { onDispose { p.screenOpen = false } }

    var detailId by remember { mutableStateOf<Long?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pinOk by remember { mutableStateOf(false) }
    var pinThen by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun withPin(action: () -> Unit) { if (pinOk) action() else pinThen = action }

    BackHandler {
        when {
            c.sheet != null -> c.dismissSheet()
            pinThen != null -> pinThen = null
            adding -> adding = false
            detailId != null -> detailId = null
            else -> p.screenOpen = false
        }
    }

    val version = p.version.intValue
    val unreadable = remember(version) { p.unreadable() }
    val logins = remember(version) { p.all() }
    val detail = detailId?.let { id -> logins.firstOrNull { it.id == id } }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        if (adding) {
            AddPassword(c, onDone = { adding = false })
        } else if (detail != null) {
            PasswordDetail(c, detail, onBack = { detailId = null }, withPin = ::withPin)
        } else {
            PasswordList(c, logins, unreadable, onOpen = { detailId = it.id }, onAdd = { adding = true })
        }
        pinThen?.let { then ->
            Column(Modifier.fillMaxSize().background(C.Bg).clickable(enabled = false) {}) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { IconBtn(Glyphs.Back, "Cancel", { pinThen = null }) }
                PinGate(
                    settings = c.settings,
                    lockedTitle = "Enter your PIN",
                    lockedSub = "To see, copy or edit passwords",
                    createSub = "Choose a 4-digit PIN to protect your passwords",
                    onSuccess = { pinOk = true; pinThen = null; c.vaultUnlocked = true; then() },
                )
            }
        }
    }
}

@Composable
private fun PasswordList(c: AppController, logins: List<SavedLogin>, unreadable: Boolean, onOpen: (SavedLogin) -> Unit, onAdd: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Glyphs.Back, "Back to the Vault", { c.passwords.screenOpen = false })
            T("Passwords", 24.sp, weight = FontWeight.ExtraBold, display = true, modifier = Modifier.weight(1f))
            if (!unreadable) Pill("Add", onAdd, Glyphs.Plus)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            if (unreadable) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(16.dp)) {
                    T("Corn can't open your saved passwords", 17.sp, weight = FontWeight.Bold, display = true)
                    Spacer(Modifier.height(6.dp))
                    T(
                        "They're locked with a key that lives in this phone's security chip, and that key is gone. " +
                            "This happens when Corn's data moves to a new phone or the phone's security was reset. " +
                            "Nobody can read them, Corn included. Reset to start saving passwords again.",
                        14.sp, C.Muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    BigButton("Reset passwords", { c.sheet = Sheet.Password(PasswordSheet.ConfirmReset) }, icon = Glyphs.Trash)
                }
                return@Column
            }
            Field(query, { query = it }, "Search ${logins.size} saved logins", leading = { Glyph(Glyphs.Search, 18.dp, C.Muted) })
            Spacer(Modifier.height(8.dp))
            val shown = logins.filter { LoginMatching.matchesSearch(it, query) }
            if (logins.isEmpty()) {
                T("No passwords yet. Log in to a site and Corn offers to save it here, locked with your PIN.", 15.sp, C.Muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 10.dp))
            } else if (shown.isEmpty()) {
                T("No saved logins match “$query”.", 15.sp, C.Muted, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 10.dp))
            }
            LoginMatching.groupBySite(shown).forEach { (origin, group) ->
                val site = LoginMatching.displaySite(origin)
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    LetterIcon(site, 26.dp)
                    Spacer(Modifier.width(10.dp))
                    T(site, 15.sp, weight = FontWeight.SemiBold, maxLines = 1)
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(horizontal = 14.dp)) {
                    group.forEach { login ->
                        SettingRow(
                            login.username.ifBlank { "No username" },
                            lastUsedText(login),
                            onClick = { onOpen(login) },
                            trailing = { Glyph(Glyphs.ChevRight, 18.dp, C.Muted) },
                        )
                    }
                }
            }
            if (logins.isNotEmpty()) {
                T("Passwords stay on this phone, encrypted, and survive every Flush. Seeing one needs your PIN.", 12.5.sp, C.Muted,
                    modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

/** Passwords → Add: save a login by hand (site, username or email, password). */
@Composable
private fun AddPassword(c: AppController, onDone: () -> Unit) {
    var site by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Glyphs.Back, "Back to passwords", onDone)
            T("Add password", 24.sp, weight = FontWeight.ExtraBold, display = true)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            Label("Site")
            Field(site, { site = it; error = null }, "example.com")
            Label("Username or email")
            Field(user, { user = it; error = null }, "you@example.com")
            Label("Password")
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface2).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (pass.isEmpty()) T("Password", 15.sp, C.Muted, maxLines = 1)
                    androidx.compose.foundation.text.BasicTextField(
                        pass, { pass = it; error = null }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(C.Accent),
                        visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                T(if (show) "Hide" else "Show", 13.sp, C.Accent, FontWeight.SemiBold, modifier = Modifier.clickable { show = !show }.padding(10.dp))
            }
            error?.let { T(it, 13.sp, C.Accent, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(20.dp))
            BigButton("Save", {
                error = c.passwords.addManual(site, user, pass)
                if (error == null) onDone()
            }, icon = Glyphs.Lock)
            T("Encrypted and locked in your Vault with your PIN. Corn fills it on this site's login page when you tap. Never leaves this phone.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun PasswordDetail(c: AppController, login: SavedLogin, onBack: () -> Unit, withPin: (() -> Unit) -> Unit) {
    val p = c.passwords
    val site = LoginMatching.displaySite(login.origin)
    var revealed by remember(login.id) { mutableStateOf<String?>(null) }
    var editing by remember(login.id) { mutableStateOf(false) }
    var editUser by remember(login.id) { mutableStateOf("") }
    var editPass by remember(login.id) { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Glyphs.Back, "Back to passwords", onBack)
            T(site, 22.sp, weight = FontWeight.ExtraBold, display = true, maxLines = 1, modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            if (editing) {
                Label("Username")
                Field(editUser, { editUser = it }, "Username")
                Label("Password")
                Field(editPass, { editPass = it }, "Password")
                Spacer(Modifier.height(18.dp))
                BigButton("Save changes", {
                    if (editPass.isEmpty()) { c.showToast("A password can't be empty"); return@BigButton }
                    if (p.update(login, editUser.trim(), editPass)) {
                        editing = false
                        revealed = null
                        c.showToast("Saved")
                    } else {
                        c.showToast("Corn couldn't save the changes")
                    }
                }, icon = Glyphs.Check)
                Spacer(Modifier.height(10.dp))
                BigButton("Cancel", { editing = false; editPass = ""; editUser = "" }, ghost = true)
                return@Column
            }
            SettingsGroup {
                SettingRow("Site", login.origin, leading = { LetterIcon(site, 30.dp) })
                SettingRow(
                    "Username", login.username.ifBlank { "No username" },
                    trailing = { if (login.username.isNotEmpty()) IconBtn(Glyphs.Copy, "Copy username", { p.copy(login.username, "Username") }, size = 38.dp, iconSize = 18.dp) },
                )
                SettingRow(
                    "Password", revealed ?: "••••••••",
                    trailing = {
                        Row {
                            IconBtn(if (revealed == null) Glyphs.Eye else Glyphs.EyeOff, if (revealed == null) "Show password" else "Hide password", {
                                if (revealed != null) revealed = null
                                else withPin { revealed = p.passwordOf(login) ?: run { c.showToast("Corn couldn't read this password"); null } }
                            }, size = 38.dp, iconSize = 18.dp)
                            IconBtn(Glyphs.Copy, "Copy password", {
                                withPin { p.passwordOf(login)?.let { p.copy(it, "Password") } ?: c.showToast("Corn couldn't read this password") }
                            }, size = 38.dp, iconSize = 18.dp)
                        }
                    },
                )
                SettingRow("Used", if (login.timesUsed == 0L) "Not filled yet" else "${login.timesUsed} ${if (login.timesUsed == 1L) "time" else "times"} · ${lastUsedText(login)}")
            }
            Spacer(Modifier.height(14.dp))
            BigButton("Edit", {
                withPin {
                    val pw = p.passwordOf(login)
                    if (pw == null) { c.showToast("Corn couldn't read this password"); return@withPin }
                    editUser = login.username
                    editPass = pw
                    editing = true
                }
            }, icon = Glyphs.Edit, ghost = true)
            Spacer(Modifier.height(10.dp))
            BigButton("Delete", { c.sheet = Sheet.Password(PasswordSheet.ConfirmDelete(login)) }, icon = Glyphs.Trash, ghost = true)
        }
    }
}

/** No screenshots, screen recording or recent-apps preview while passwords are on screen. */
@Composable
private fun SecureWindow(c: AppController) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val w = activity?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            // Below Android 13, "Black screen in recent apps" uses FLAG_SECURE too: leave it on for that.
            if (!(Build.VERSION.SDK_INT < 33 && c.settings.blackRecents)) w?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------- Settings ----------------

/** Settings → Privacy: the password switches, the Passwords screen and the "Never save" sites. */
@Composable
fun PasswordSettings(c: AppController) {
    val s = c.settings
    SettingRow("Offer to save passwords", "After you log in, Corn asks. Saved in your Vault, on this phone only.",
        onClick = { s.offerSavePasswords = !s.offerSavePasswords }, trailing = { ToggleSwitch(s.offerSavePasswords) })
    SettingRow("Ask for PIN before filling", "Only when the Vault is locked.",
        onClick = { s.askPinBeforeFill = !s.askPinBeforeFill }, trailing = { ToggleSwitch(s.askPinBeforeFill) })
    SettingRow("Saved passwords", onClick = {
        c.passwords.screenOpen = true
        c.screen = com.flushbrowser.Screen.Vault
    }, leading = { Glyph(Glyphs.Key, tint = C.Muted) }, trailing = { Glyph(Glyphs.ChevRight, 18.dp, C.Muted) })
    val never = remember(c.data.vaultVersion.intValue) { c.data.neverSaveList() }
    if (never.isNotEmpty()) {
        T("Never save on", 13.sp, C.Muted, FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        never.forEach { origin ->
            val site = LoginMatching.displaySite(origin)
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                T(site, 15.sp, modifier = Modifier.weight(1f))
                IconBtn(Glyphs.X, "Remove $site", { c.data.removeNeverSave(origin) }, size = 34.dp, iconSize = 16.dp)
            }
        }
    }
}
