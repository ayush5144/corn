/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.cornbrowser.CornController
import app.cornbrowser.R
import app.cornbrowser.Sheet
import app.cornbrowser.browser.TabEntry

@Composable
fun BrowserScreen(c: CornController) {
    val cur = c.tabs.current ?: return
    Column(Modifier.fillMaxSize().background(C.Bg)) {
        if (!c.settings.addressBarBottom) AddressBar(c, cur)
        Box(Modifier.weight(1f).fillMaxWidth().background(C.Surface)) { PageArea(c, cur) }
        Column(Modifier.fillMaxWidth().background(C.Bg)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
            GroupStrip(c, cur)
            if (c.settings.addressBarBottom) AddressBar(c, cur)
            Toolbar(c, cur)
        }
    }
}

// ---------------- address bar ----------------

@Composable
private fun AddressBar(c: CornController, cur: TabEntry) {
    val focus = LocalFocusManager.current
    val shown = if (cur.isNewTab) "" else cur.url
    // Not editing: show the page's URL. Editing: the user's own text, starting with the URL selected.
    var editing by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val value = if (editing) field else TextFieldValue(shown)
    val saved = remember(cur.url, c.data.vaultVersion.intValue) { !cur.isNewTab && c.data.isSaved(cur.url) }
    val e = c.engineNow

    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(C.Surface2).padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search Switcher (left)
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).clickable { c.sheet = Sheet.Engine }.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EngineBadge(e)
                CornIcon(CornIcons.ChevDown, 14.dp, C.Muted)
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) T("Search or type a site", 15.sp, C.Muted, maxLines = 1)
                BasicTextField(
                    value = value,
                    onValueChange = { if (editing) field = it },
                    singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                    cursorBrush = SolidColor(C.Corn),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { c.go(field.text); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused && !editing) field = TextFieldValue(shown, TextRange(0, shown.length))
                        editing = it.isFocused
                    },
                )
            }
            // Blocking shield (right)
            val off = !cur.isNewTab && remember(c.data.vaultVersion.intValue, cur.host) { c.shields(cur.host).allOff }
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp))
                    .clickable { if (cur.isNewTab) c.showToast("Nothing to block on this page") else c.sheet = Sheet.Shield }
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CornIcon(if (off) CornIcons.ShieldOff else CornIcons.Shield, 18.dp, if (off) C.Muted else C.Leaf)
                if (!off && !cur.isNewTab) {
                    Spacer(Modifier.width(3.dp))
                    T(cur.blocked.toString(), 12.sp, C.Leaf, FontWeight.Bold)
                }
            }
        }
        // Vault save (far right)
        IconBtn(if (saved) CornIcons.VaultSaved else CornIcons.VaultAdd, "Save page to Vault", { c.openSaveSheet() }, tint = if (saved) C.Corn else C.Text)
    }
}

// ---------------- page ----------------

@Composable
private fun PageArea(c: CornController, cur: TabEntry) {
    val drain by animateFloatAsState(
        targetValue = if (c.flushing) 1f else 0f,
        animationSpec = if (c.flushing) tween(720, easing = CubicBezierEasing(0.55f, 0f, 0.75f, 0.2f)) else snap(),
        label = "drain",
    )
    // The drain layer is applied only while flushing: an always-on graphics layer makes
    // WebView skip painting parts of the page.
    val drainModifier = if (c.flushing || drain > 0f) Modifier.graphicsLayer {
        rotationZ = drain * 560f
        scaleX = 1f - drain
        scaleY = 1f - drain
        alpha = 1f - drain * 0.8f
    } else Modifier
    Box(Modifier.fillMaxSize().then(drainModifier)) {
        val live = cur.live
        if (cur.isNewTab) {
            NewTabPage(c)
        } else if (cur.crashed || live == null) {
            CrashedPage(cur.host) { c.tabs.revive(cur) }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> FrameLayout(ctx) },
                update = { container ->
                    val wv = live.webView
                    if (container.getChildAt(0) !== wv) {
                        container.removeAllViews()
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        container.addView(wv, FrameLayout.LayoutParams(-1, -1))
                    }
                },
            )
        }
    }
}

@Composable
private fun CrashedPage(host: String, onReload: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.Bg).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        T("🌽", 44.sp)
        Spacer(Modifier.height(12.dp))
        T("This page stopped", 22.sp, weight = FontWeight.ExtraBold, display = true)
        Spacer(Modifier.height(6.dp))
        T("$host ran out of memory or crashed. Your other tabs are fine.", 14.sp, C.Muted)
        Spacer(Modifier.height(20.dp))
        BigButton("Reload", onReload)
    }
}

@Composable
private fun NewTabPage(c: CornController) {
    val focus = LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    val appear = remember(c.quip) { Animatable(0.94f) }
    LaunchedEffect(c.quip) { appear.animateTo(1f, tween(450)) }
    val recent = remember(c.data.vaultVersion.intValue, c.settings.vaultLock) {
        if (c.settings.vaultLock) emptyList() else c.data.recentItems(3)
    }

    Column(
        Modifier.fillMaxSize().background(C.Bg).verticalScroll(rememberScrollState())
            .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
            .padding(horizontal = 20.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.corn_logo), null, Modifier.size(76.dp))
            Spacer(Modifier.height(8.dp))
            T("corn", 44.sp, C.Corn, FontWeight.ExtraBold, display = true)
            Spacer(Modifier.height(4.dp))
            T(c.quip ?: "Browse. Save what matters. Flush the rest.", 19.sp, if (c.quip != null) C.Corn else C.Text, FontWeight.SemiBold, display = true)
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface2).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CornIcon(CornIcons.Search, 20.dp, C.Muted)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) T("Search or type a site", 16.sp, C.Muted, maxLines = 1)
                BasicTextField(
                    q, { q = it }, singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 16.sp, fontFamily = Ui),
                    cursorBrush = SolidColor(C.Corn),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { c.go(q); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // Quick tiles: the newest Vault links (only when the Vault isn't locked) + the Vault itself.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            recent.forEach { item ->
                val host = android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: ""
                QuickTile(host.substringBefore('.').replaceFirstChar { it.uppercase() }, { c.go(item.url) }) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(C.forHost(host)), contentAlignment = Alignment.Center) {
                        T(host.firstOrNull()?.uppercase() ?: "?", 20.sp, Color.White, FontWeight.ExtraBold)
                    }
                }
            }
            QuickTile("Vault", { c.openVault() }) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(C.Surface2), contentAlignment = Alignment.Center) {
                    CornIcon(CornIcons.Vault, 24.dp, C.Corn)
                }
            }
        }
        // All-time totals: real counts of requests Corn blocked, kept across Flushes (numbers only).
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(18.dp)) {
            T("STATS", 12.sp, C.Muted, FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(c.settings.totalBlocked, "Trackers & ads blocked", C.Corn, Modifier.weight(1f))
                Stat(c.settings.totalPopups, "Pop-ups & redirects stopped", C.Leaf, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: Long, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        T(java.text.NumberFormat.getIntegerInstance().format(value), 30.sp, color, FontWeight.ExtraBold, display = true)
        T(label, 13.sp, C.Text)
    }
}

@Composable
private fun QuickTile(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(6.dp))
        T(label, 12.sp, C.Muted, maxLines = 1)
    }
}

// ---------------- Tab Group Strip ----------------

@Composable
private fun GroupStrip(c: CornController, cur: TabEntry) {
    val gid = cur.groupId ?: return
    val g = c.tabs.group(gid) ?: return
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(CornIcons.ChevUp, "Show group ${g.name}", { c.sheet = Sheet.Group(gid) })
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            c.tabs.groupTabs(gid).forEach { t ->
                val isCur = t.id == cur.id
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(if (isCur) 44.dp else 40.dp).clip(CircleShape)
                            .then(if (isCur) Modifier.border(2.dp, g.color, CircleShape) else Modifier)
                            .padding(if (isCur) 4.dp else 0.dp).clip(CircleShape).background(C.Surface2)
                            .clickable { c.switchTab(t.id) },
                        contentAlignment = Alignment.Center,
                    ) { SiteIcon(t, 22.dp) }
                    if (isCur) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).clip(CircleShape).background(C.Text)
                                .clickable { c.tabs.close(t.id) },
                            contentAlignment = Alignment.Center,
                        ) { CornIcon(CornIcons.X, 12.dp, C.Bg) }
                    }
                }
            }
        }
        IconBtn(CornIcons.Plus, "New tab in this group", { c.newTabInGroup(gid) })
    }
}

// ---------------- toolbar ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Toolbar(c: CornController, cur: TabEntry) {
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        // Back (hold for Forward)
        Box(
            Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(14.dp))
                .combinedClickable(enabled = cur.canGoBack || cur.canGoForward, onClick = { c.tabs.goBack() }, onLongClick = { c.tabs.goForward(); c.showToast("Forward") }),
            contentAlignment = Alignment.Center,
        ) { CornIcon(CornIcons.Back, tint = if (cur.canGoBack) C.Text else C.Muted.copy(alpha = 0.45f)) }
        ToolbarButton({ c.openVault() }) { CornIcon(CornIcons.Vault) }
        ToolbarButton({ c.openTabsScreen() }) {
            Box(Modifier.size(23.dp).border(2.dp, C.Text, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                T(c.tabs.tabs.size.toString(), 12.sp, weight = FontWeight.ExtraBold)
            }
        }
        // Flush (second from the right)
        ToolbarButton({ c.requestFlush() }) {
            Box(Modifier.width(46.dp).height(40.dp).clip(RoundedCornerShape(14.dp)).background(C.Corn), contentAlignment = Alignment.Center) {
                CornIcon(CornIcons.Toilet, 24.dp, C.CornInk)
            }
        }
        ToolbarButton({ c.sheet = Sheet.Menu }) { CornIcon(CornIcons.Dots) }
    }
}

@Composable
private fun ToolbarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}
