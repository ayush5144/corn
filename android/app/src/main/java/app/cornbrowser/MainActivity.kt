/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

private val Bg = Color(0xFF0F0E0B)
private val Surface2 = Color(0xFF25221B)
private val TextC = Color(0xFFF5F0E1)
private val Muted = Color(0xFFA8A08C)
private val CornYellow = Color(0xFFFFC83D)
private val Leaf = Color(0xFF86C64A)

/**
 * Spike UI: the smallest browser that proves WebView + Brave's adblock engine.
 * Address bar, shield toggle with live counts, simple tabs, fullscreen video.
 * The real Corn UI (Tab Group Strip, Vault, Flush…) is built after the spike passes.
 */
class MainActivity : ComponentActivity(), TabListener {
    private val tabs = mutableStateListOf<CornTab>()
    private var current by mutableIntStateOf(0)
    private var engineStatus by mutableStateOf(CornEngine.status)
    private var fullscreenView by mutableStateOf<View?>(null)
    private var exitFullscreen: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG) // chrome://inspect on the Mac
        CornEngine.onChange { runOnUiThread { engineStatus = CornEngine.status } }
        CornEngine.start(applicationContext)
        if (tabs.isEmpty()) newTab(intent?.dataString ?: "https://duckduckgo.com")

        setContent { Browser() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { newTab(it) }
    }

    override fun onDestroy() {
        tabs.forEach { it.destroy() }
        super.onDestroy()
    }

    private fun newTab(url: String) {
        val tab = CornTab(this, this)
        tabs += tab
        current = tabs.lastIndex
        tab.load(url)
    }

    private fun closeTab(index: Int) {
        if (tabs.size == 1) return
        tabs.removeAt(index).destroy()
        current = current.coerceAtMost(tabs.lastIndex)
    }

    private fun toUrl(input: String): String {
        val text = input.trim()
        val looksLikeUrl = !text.contains(' ') && (text.contains('.') || text.startsWith("localhost"))
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> text
            looksLikeUrl -> "https://$text"
            else -> "https://duckduckgo.com/?q=" + URLEncoder.encode(text, "UTF-8")
        }
    }

    // ---------------- TabListener ----------------
    override fun onTabChanged(tab: CornTab) = Unit
    override fun onBlockedPopup(tab: CornTab) { runOnUiThread { toast("Blocked a pop-up") } }
    override fun onBlockedRedirect(tab: CornTab, target: String) { runOnUiThread { toast("Blocked a redirect to $target") } }
    override fun onEnterFullscreen(view: View, exit: () -> Unit) {
        fullscreenView = view
        exitFullscreen = exit
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onExitFullscreen() {
        fullscreenView = null
        exitFullscreen = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---------------- UI ----------------
    @Composable
    private fun Browser() {
        val tab = tabs.getOrNull(current) ?: return

        BackHandler(enabled = fullscreenView != null || tab.webView.canGoBack()) {
            if (fullscreenView != null) exitFullscreen?.invoke() else tab.webView.goBack()
        }

        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                AddressBar(tab)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx -> FrameLayout(ctx) },
                        update = { container ->
                            if (container.getChildAt(0) !== tab.webView) {
                                container.removeAllViews()
                                (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
                                container.addView(tab.webView, FrameLayout.LayoutParams(-1, -1))
                            }
                        },
                    )
                }
                StatusLine(tab)
                TabBar()
            }
            fullscreenView?.let { v ->
                AndroidView(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    factory = { ctx -> FrameLayout(ctx) },
                    update = { c ->
                        if (c.getChildAt(0) !== v) {
                            c.removeAllViews()
                            (v.parent as? ViewGroup)?.removeView(v)
                            c.addView(v, FrameLayout.LayoutParams(-1, -1))
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun AddressBar(tab: CornTab) {
        tab.version // reading the tab's version makes this redraw whenever the tab changes
        val url = tab.url
        var text by remember(tab.id, url) { mutableStateOf(url) }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Surface2).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextC, fontSize = 15.sp),
                    cursorBrush = SolidColor(CornYellow),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { tab.load(toUrl(text)) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val color = if (tab.shieldsUp) Leaf else Muted
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Surface2)
                    .clickable { tab.shieldsUp = !tab.shieldsUp; tab.webView.reload(); tab.changed() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (tab.shieldsUp) "🛡 ${tab.blockedOnPage.get()}" else "🛡 off", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    @Composable
    private fun StatusLine(tab: CornTab) {
        tab.version
        val text = buildString {
            append(engineStatus)
            if (tab.shieldsUp) append(" · blocked ${tab.blockedOnPage.get()} requests · stopped ${tab.guardedOnPage.get()} pop-ups/redirects")
            else append(" · shields OFF on this tab")
        }
        Text(
            text,
            color = Muted,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }

    @Composable
    private fun TabBar() {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { i, t ->
                    t.version // redraw the tab chips when any tab changes
                    val selected = i == current
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (selected) CornYellow else Surface2)
                            .clickable { if (selected) closeTab(i) else current = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (selected && tabs.size > 1) "×" else (t.title.firstOrNull()?.uppercase() ?: "•"),
                            color = if (selected) Color(0xFF2A1E00) else TextC,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Surface2).clickable { newTab("https://duckduckgo.com") },
                contentAlignment = Alignment.Center,
            ) { Text("+", color = TextC, fontSize = 22.sp) }
        }
    }
}
