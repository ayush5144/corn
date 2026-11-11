/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.ui

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flushbrowser.AppController
import com.flushbrowser.Screen

@Composable
fun AppRoot(
    c: AppController,
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
                Screen.History -> HistoryScreen(c)
            }
            Sheets(c)
            // Toasts sit just above whatever is at the bottom: an open sheet, the browser's bottom bar
            // (measured, so the Tab Group strip and a bottom address bar count), or the screen edge.
            val density = LocalDensity.current
            val toastBottom = with(density) {
                when {
                    c.sheet != null -> c.sheetHeightPx.toDp() + 8.dp
                    c.screen == Screen.Browser -> c.bottomBarHeightPx.toDp() + 8.dp
                    else -> 16.dp
                }
            }
            ToastHost(c.toast, bottomPadding = toastBottom) { c.toastAction() }

            if (pinSetup) {
                Column(Modifier.fillMaxSize().background(C.Bg)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { IconBtn(Glyphs.Back, "Cancel", onPinSetupCancel) }
                    PinGate(c.settings, "", "", "Choose a 4-digit PIN for App Lock and the Vault", onPinSetupDone)
                }
            }
            if (c.appLocked) {
                Box(Modifier.fillMaxSize().background(C.Bg)) {
                    PinGate(c.settings, "Corn is locked", "Enter your PIN", "") { c.appLocked = false }
                }
            }
        }
        // Fullscreen video covers everything, including the system bars. In split screen it stays
        // inside its tab's pane instead (BrowserScreen's Pane draws it there).
        if (!c.fullscreenInPane) c.fullscreen?.let { v -> FullscreenView(v, Modifier.fillMaxSize()) }
        // Fullscreen that started in a pane ends when that tab leaves split screen (pane closed, split ended).
        val split = c.tabs.split
        androidx.compose.runtime.LaunchedEffect(split, c.fullscreenTabId) {
            val id = c.fullscreenTabId
            if (c.fullscreenInPane && id != null && split?.contains(id) != true) c.exitFullscreen()
        }
    }
}

/** A page's fullscreen view (WebView's onShowCustomView), on black. */
@Composable
fun FullscreenView(v: android.view.View, modifier: Modifier) {
    AndroidView(
        modifier = modifier.background(Color.Black),
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
