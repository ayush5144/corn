/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.cornbrowser.CornController
import app.cornbrowser.Screen

@Composable
fun CornApp(
    c: CornController,
    pinSetup: Boolean,
    onPinSetupDone: () -> Unit,
    onPinSetupCancel: () -> Unit,
    onNeedPin: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            when (c.screen) {
                Screen.Onboarding -> OnboardingScreen(c.settings) { c.finishOnboarding() }
                Screen.Browser -> BrowserScreen(c)
                Screen.Tabs -> TabsScreen(c)
                Screen.Vault -> VaultScreen(c)
                Screen.Settings -> SettingsScreen(c, onNeedPin)
            }
            Sheets(c)
            ToastHost(c.toast, bottomPadding = if (c.screen == Screen.Browser) 132.dp else 24.dp) { c.toastAction() }

            if (pinSetup) {
                Column(Modifier.fillMaxSize().background(C.Bg)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { IconBtn(CornIcons.Back, "Cancel", onPinSetupCancel) }
                    PinGate(c.settings, "", "", "Choose a 4-digit PIN for App Lock and the Vault", onPinSetupDone)
                }
            }
            if (c.appLocked) {
                Box(Modifier.fillMaxSize().background(C.Bg)) {
                    PinGate(c.settings, "Corn is locked", "Enter your PIN", "") { c.appLocked = false }
                }
            }
        }
        // Fullscreen video covers everything, including the system bars.
        c.fullscreen?.let { v ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { ctx -> FrameLayout(ctx) },
                update = { box ->
                    if (box.getChildAt(0) !== v) {
                        box.removeAllViews()
                        (v.parent as? ViewGroup)?.removeView(v)
                        box.addView(v, FrameLayout.LayoutParams(-1, -1))
                    }
                },
            )
        }
    }
}
