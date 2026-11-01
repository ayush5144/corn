/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.BuildConfig
import app.cornbrowser.CornController
import app.cornbrowser.Screen
import app.cornbrowser.data.FlushMode
import app.cornbrowser.data.SearchEngines

/**
 * Settings, in the mockup's order. `onNeedPin` runs when App Lock is turned on before a PIN exists.
 */
@Composable
fun SettingsScreen(c: CornController, onNeedPin: () -> Unit) {
    val s = c.settings
    val keep = remember(c.data.vaultVersion.intValue) { c.data.keepList() }
    var keepInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(CornIcons.Back, "Back to browser", { c.screen = Screen.Browser })
            T("Settings", 24.sp, weight = FontWeight.ExtraBold, display = true)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Label("When should Corn flush?")
            SettingsGroup {
                FlushRadio(c, FlushMode.KEEP, "Keep my session until I flush", "Default. Nothing goes until you hit the toilet.")
                FlushRadio(c, FlushMode.LEAVE_NOW, "Flush when I leave Corn")
                FlushRadio(c, FlushMode.LEAVE_5, "Flush 5 minutes after I leave")
                FlushRadio(c, FlushMode.LEAVE_30, "Flush 30 minutes after I leave")
            }
            Caption("Leaving Corn means switching apps, going home or locking your phone.")
            SettingsGroup {
                ToggleRow("Flush after inactivity", "A playing video counts as activity.", s.inactivity) { s.inactivity = it; c.onInactivitySettingChanged() }
                if (s.inactivity) {
                    SettingRow("After", trailing = {
                        val opts = listOf(10, 30, 60)
                        Segmented(opts.map { "$it min" }, opts.indexOf(s.inactivityMinutes).coerceAtLeast(0)) { s.inactivityMinutes = opts[it] }
                    })
                }
                ToggleRow("Flush sound", "Off by default, so it never goes off in public.", s.flushSound) { s.flushSound = it }
                ToggleRow("Ask before flushing", "Off by default. When on, Corn asks before wiping.", s.flushConfirm) { s.flushConfirm = it }
            }

            Label("Keep List")
            SettingsGroup {
                keep.forEach { host ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        T(host, 15.sp, modifier = Modifier.weight(1f))
                        IconBtn(CornIcons.X, "Remove $host", { c.data.removeKeep(host) }, size = 34.dp, iconSize = 16.dp)
                    }
                }
                Box(Modifier.padding(vertical = 6.dp)) {
                    Field(keepInput, { keepInput = it }, "Add a site, e.g. mail.example.com", height = 40.dp, onDone = {
                        c.data.addKeep(keepInput)
                        keepInput = ""
                    })
                }
            }
            Caption("These sites stay logged in after a Flush. It's not history or passwords: Corn keeps only their login cookies.")

            Label("Privacy")
            SettingsGroup {
                ToggleRow("App Lock", "Ask for your PIN to open Corn.", s.appLock) { on ->
                    if (on && !s.hasPin) onNeedPin() else s.appLock = on
                }
                ToggleRow("Lock the Vault", "Your PIN to see saved links.", s.vaultLock) { s.vaultLock = it }
                ToggleRow("Black screen in recent apps", "Hides what's on screen in the app switcher.", s.blackRecents) { s.blackRecents = it }
                SettingRow("Notifications", "Corn never notifies you while it's closed.", leading = { CornIcon(CornIcons.BellOff, tint = C.Muted) })
            }

            Label("Search")
            SettingsGroup {
                SearchEngines.all.forEach { e ->
                    SettingRow(e.name, onClick = { s.engine = e.id; c.engineNow = e }, leading = { EngineBadge(e) }, trailing = { Radio(s.engine == e.id) })
                }
            }
            Caption("Default engine. You can switch for one search from the dropdown in the address bar.")

            Label("Layout")
            SettingsGroup {
                SettingRow("Address bar", trailing = {
                    Segmented(listOf("Top", "Bottom"), if (s.addressBarBottom) 1 else 0) { i ->
                        s.addressBarBottom = i == 1
                        c.showToast(if (i == 1) "Address bar moved to the bottom" else "Address bar moved to the top")
                    }
                })
            }

            Label("About")
            SettingsGroup {
                SettingRow("Donate", "Corn is free. Donations keep it that way.", onClick = { c.showToast("Donations open at launch. Thank you! 🌽") },
                    leading = { CornIcon(CornIcons.Heart, tint = C.Corn) }, trailing = { CornIcon(CornIcons.ChevRight, 18.dp, C.Muted) })
                SettingRow("Version", trailing = { T(BuildConfig.VERSION_NAME, 12.sp, C.Muted) })
            }
            Box(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FlushRadio(c: CornController, mode: FlushMode, title: String, sub: String? = null) {
    SettingRow(title, sub, onClick = { c.settings.chooseFlushMode(mode) }, leading = { Radio(c.settings.flushMode == mode) })
}

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, sub, onClick = { onChange(!on) }, trailing = { CornSwitch(on) })
}
