/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.cornbrowser.browser.CornEngine
import app.cornbrowser.ui.CornApp

class MainActivity : ComponentActivity() {
    private lateinit var c: CornController
    private var pinSetup by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG) // chrome://inspect from the Mac while developing
        CornEngine.start(applicationContext)

        c = CornController(this)
        c.start(
            intent?.dataString,
            restartQuip = intent?.getStringExtra(RestartActivity.EXTRA_QUIP),
            restartToast = intent?.getStringExtra(RestartActivity.EXTRA_TOAST),
        )

        onBackPressedDispatcher.addCallback(this) {
            if (pinSetup) pinSetup = false else if (!c.back()) moveTaskToBack(true)
        }

        setContent {
            LaunchedEffect(c.settings.blackRecents) { applyRecentsPrivacy(c.settings.blackRecents) }
            LaunchedEffect(c.fullscreen) { setSystemBarsHidden(c.fullscreen != null) }
            CornApp(
                c, pinSetup,
                onPinSetupDone = { pinSetup = false; c.settings.appLock = true; c.showToast("App Lock is on") },
                onPinSetupCancel = { pinSetup = false },
                onNeedPin = { pinSetup = true },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        c.onAppForeground()
    }

    override fun onStop() {
        c.onAppBackground()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { c.openInNewTab(it) }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        c.onTrimMemory(level)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) c.onUserActivity()
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        c.tabs.destroyAll()
        super.onDestroy()
    }

    /** Black screen in the recent-apps view. Android 13+ can do this without blocking screenshots. */
    private fun applyRecentsPrivacy(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            setRecentsScreenshotEnabled(!on)
        } else if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun setSystemBarsHidden(hidden: Boolean) {
        val ctl = WindowCompat.getInsetsController(window, window.decorView)
        if (hidden) {
            ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            ctl.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
