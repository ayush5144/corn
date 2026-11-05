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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
        if (!c.settings.addressBarBottom) { if (c.findQuery != null) FindBar(c) else AddressBar(c, cur) }
        Box(Modifier.weight(1f).fillMaxWidth().background(C.Surface)) {
            PageArea(c, cur)
            // Suggestions while typing in the address bar (History + unlocked Vault, on the phone only).
            val q = c.addressQuery
            if (!q.isNullOrBlank()) {
                SuggestionList(c, q, Modifier.align(if (c.settings.addressBarBottom) Alignment.BottomCenter else Alignment.TopCenter).padding(horizontal = 10.dp, vertical = 6.dp))
            }
            PasswordFillChip(c) // "Fill as …" when the page has a login form with saved logins
        }
        Column(Modifier.fillMaxWidth().background(C.Bg).onSizeChanged { c.bottomBarHeightPx = it.height }) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
            GroupStrip(c, cur)
            if (c.settings.addressBarBottom) { if (c.findQuery != null) FindBar(c) else AddressBar(c, cur) }
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
                    onValueChange = { if (editing) { field = it; c.addressQuery = it.text } },
                    singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                    cursorBrush = SolidColor(C.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { c.go(field.text); c.addressQuery = null; focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused && !editing) field = TextFieldValue(shown, TextRange(0, shown.length))
                        editing = it.isFocused
                        // Suggestions start once the user types (the selected URL alone shows none).
                        c.addressQuery = if (it.isFocused) "" else null
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
        IconBtn(if (saved) CornIcons.VaultSaved else CornIcons.VaultAdd, "Save page to Vault", { c.openSaveSheet() }, tint = if (saved) C.Accent else C.Text)
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
        val split = c.tabs.split
        val first = split?.let { s -> c.tabs.tabs.firstOrNull { it.id == s.first } }
        val second = split?.let { s -> c.tabs.tabs.firstOrNull { it.id == s.second } }
        if (split != null && first != null && second != null) {
            SplitPanes(c, split, first, second)
        } else {
            PageContent(c, cur, onTouch = null)
        }
    }
}

// ---------------- split screen ----------------

/**
 * Two tabs on screen: top / bottom with the phone upright, side by side when the screen is wider
 * than tall. A draggable divider sets the sizes (25–75 %); letting go of it near an edge closes the
 * pane it was dragged over. The focused pane (last touched) has a thin accent outline.
 * One custom layout for both orientations, so turning the phone or dragging the divider never
 * rebuilds the panes' WebView containers.
 */
@Composable
private fun SplitPanes(c: CornController, split: app.cornbrowser.browser.SplitState, first: TabEntry, second: TabEntry) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val sideBySide = config.screenWidthDp > config.screenHeightDp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dividerPx = with(density) { SPLIT_DIVIDER.roundToPx() }
    var sizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    // While dragging: the tab whose pane would close if the divider were let go now.
    var closing by remember { mutableStateOf<Int?>(null) }
    val latestSideBySide = androidx.compose.runtime.rememberUpdatedState(sideBySide)
    val latestSize = androidx.compose.runtime.rememberUpdatedState(sizePx)

    androidx.compose.ui.layout.Layout(
        modifier = Modifier.fillMaxSize().onSizeChanged { sizePx = it },
        content = {
            key(first.id) { Pane(c, first, focused = split.focused == first.id, closing = closing == first.id) }
            // The divider
            Box(
                Modifier.background(C.Bg).pointerInput(Unit) {
                    var raw = 0f
                    detectDragGestures(
                        onDragStart = { raw = c.tabs.split?.ratio ?: 0.5f },
                        onDragEnd = {
                            val s = c.tabs.split
                            closing = null
                            s?.closedByRelease(raw)?.let { c.closePane(it) }
                        },
                        onDragCancel = { closing = null },
                    ) { change, amount ->
                        change.consume()
                        val sz = latestSize.value
                        val avail = ((if (latestSideBySide.value) sz.width else sz.height) - dividerPx).coerceAtLeast(1)
                        raw += (if (latestSideBySide.value) amount.x else amount.y) / avail
                        c.tabs.resizeSplit(raw)
                        closing = c.tabs.split?.closedByRelease(raw)
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(if (sideBySide) 4.dp else 40.dp, if (sideBySide) 40.dp else 4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(C.Muted),
                )
            }
            key(second.id) { Pane(c, second, focused = split.focused == second.id, closing = closing == second.id) }
        },
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val total = if (sideBySide) w else h
        val avail = (total - dividerPx).coerceAtLeast(0)
        val a = (avail * split.ratio).toInt()
        val b = avail - a
        fun fixed(main: Int) = if (sideBySide) androidx.compose.ui.unit.Constraints.fixed(main, h) else androidx.compose.ui.unit.Constraints.fixed(w, main)
        val p1 = measurables[0].measure(fixed(a))
        val d = measurables[1].measure(fixed(dividerPx.coerceAtMost(total)))
        val p2 = measurables[2].measure(fixed(b))
        layout(w, h) {
            if (sideBySide) {
                p1.place(0, 0); d.place(a, 0); p2.place(a + dividerPx, 0)
            } else {
                p1.place(0, 0); d.place(0, a); p2.place(0, a + dividerPx)
            }
        }
    }
}

/** Thickness of the split-screen divider (its whole width is the drag handle). */
private val SPLIT_DIVIDER = 24.dp // tested: 14 dp was easy to miss with a finger

/** One split-screen pane: the tab's page, a × to close the pane, and the focus outline. */
@Composable
private fun Pane(c: CornController, entry: TabEntry, focused: Boolean, closing: Boolean) {
    val focusThis = androidx.compose.runtime.rememberUpdatedState { c.focusPane(entry.id) }
    Box(
        Modifier.fillMaxSize().clipToBounds()
            // Watch for the first finger down without consuming it, so the page still gets its tap.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    focusThis.value()
                }
            },
    ) {
        PageContent(c, entry, onTouch = { focusThis.value() })
        // This tab's video went fullscreen in split screen: it fills only this pane (QA 2026-11-05, #4).
        if (c.fullscreenInPane && c.fullscreenTabId == entry.id) c.fullscreen?.let { v -> FullscreenView(v, Modifier.matchParentSize()) }
        // Corn yellow, the Flush button's colour and the Tabs screen's "current tab" outline.
        if (focused) Box(Modifier.matchParentSize().border(3.dp, C.Corn))
        if (closing) {
            Box(Modifier.matchParentSize().background(C.Scrim), contentAlignment = Alignment.Center) {
                T("Let go to close", 15.sp, Color.White, FontWeight.SemiBold)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(Color(0xA6000000))
                .clickable { c.closePane(entry.id) },
            contentAlignment = Alignment.Center,
        ) { CornIcon(CornIcons.X, 14.dp, Color.White) }
    }
}

/** One tab's page: Corn's home page, the WebView, or Corn's crashed / error page. */
@Composable
private fun PageContent(c: CornController, cur: TabEntry, onTouch: (() -> Unit)?) {
    Box(Modifier.fillMaxSize()) {
        val live = cur.live
        if (cur.isNewTab) {
            NewTabPage(c)
        } else if (cur.crashed || live == null) {
            CrashedPage(cur.host) { c.tabs.revive(cur) }
        } else {
            // Pull to refresh: pulling down while the page is already at the top reloads it
            // (Android's standard SwipeRefreshLayout around the WebView).
            // One container per WebView (key): SwipeRefreshLayout picks its child once and only ever
            // measures that one, so swapping another tab's WebView into it left the new page 0×0 (blank).
            // Reloads this tab's own page (in split screen it may not be the focused one).
            val reload = androidx.compose.runtime.rememberUpdatedState { cur.live?.webView?.reload() }
            val touched = androidx.compose.runtime.rememberUpdatedState(onTouch)
            key(live) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        object : androidx.swiperefreshlayout.widget.SwipeRefreshLayout(ctx) {
                            // Split screen: a touch on the page focuses its pane (the touch still reaches the page).
                            override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
                                if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) touched.value?.invoke()
                                return super.dispatchTouchEvent(ev)
                            }
                        }.apply {
                            setColorSchemeColors(0xFFFFC83D.toInt())               // corn yellow spinner
                            setProgressBackgroundColorSchemeColor(C.Surface2.toArgb())
                            setOnRefreshListener {
                                reload.value()
                                postDelayed({ isRefreshing = false }, 15_000)      // never spin forever
                            }
                            (live.webView.parent as? ViewGroup)?.removeView(live.webView)
                            addView(live.webView, ViewGroup.LayoutParams(-1, -1))
                        }
                    },
                    update = { swipe -> if (!cur.loading && swipe.isRefreshing) swipe.isRefreshing = false },
                )
            }
            // An unloaded tab reloading: its saved picture until the page has drawn (no blank flash).
            val shot = cur.preview
            if (cur.showingSnapshot && shot != null) {
                androidx.compose.foundation.Image(
                    shot.asImageBitmap(), null, Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth, alignment = Alignment.TopCenter,
                )
            }
            cur.loadError?.let { reason -> ErrorPage(cur.host, reason) { cur.loadError = null; cur.live?.webView?.reload() } }
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

/** Corn's own page for a site that couldn't load, instead of WebView's plain grey one. */
@Composable
private fun ErrorPage(host: String, reason: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.Bg).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        T("🌽", 44.sp)
        Spacer(Modifier.height(12.dp))
        T("Can't open this page", 22.sp, weight = FontWeight.ExtraBold, display = true)
        Spacer(Modifier.height(6.dp))
        if (host.isNotBlank()) T(host, 14.sp, C.Text, FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        T(reason, 14.sp, C.Muted)
        Spacer(Modifier.height(20.dp))
        BigButton("Try again", onRetry)
    }
}

/** Address-bar suggestions: unlocked Vault links first, then History. Tap to open. */
@Composable
private fun SuggestionList(c: CornController, query: String, modifier: Modifier) {
    val focus = LocalFocusManager.current
    val items = remember(query, c.data.historyVersion.intValue, c.data.vaultVersion.intValue, c.vaultUnlocked) { c.suggestions(query) }
    if (items.isEmpty()) return
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface2)
            .border(1.dp, C.Line, RoundedCornerShape(16.dp)).padding(vertical = 4.dp),
    ) {
        items.forEach { s ->
            Row(
                Modifier.fillMaxWidth().clickable { focus.clearFocus(); c.openSuggestion(s) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CornIcon(if (s.fromVault) CornIcons.Vault else CornIcons.Clock, 18.dp, C.Muted)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    T(s.title, 14.5.sp, maxLines = 1)
                    T(android.net.Uri.parse(s.url).host?.removePrefix("www.") ?: s.url, 12.sp, C.Muted, maxLines = 1)
                }
            }
        }
    }
}

/** Find in page: replaces the address bar while open. */
@Composable
private fun FindBar(c: CornController) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val q = c.findQuery ?: ""
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(C.Surface2).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CornIcon(CornIcons.Search, 18.dp, C.Muted)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) T("Find in page", 15.sp, C.Muted, maxLines = 1)
                BasicTextField(
                    q, { c.find(it) }, singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                    cursorBrush = SolidColor(C.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { c.findNext(true) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            if (q.isNotEmpty()) T(if (c.findTotal == 0) "0/0" else "${c.findActive + 1}/${c.findTotal}", 13.sp, C.Muted)
        }
        IconBtn(CornIcons.ChevUp, "Previous match", { c.findNext(false) }, size = 38.dp, iconSize = 18.dp)
        IconBtn(CornIcons.ChevDown, "Next match", { c.findNext(true) }, size = 38.dp, iconSize = 18.dp)
        IconBtn(CornIcons.X, "Close find", { c.closeFind() }, size = 38.dp, iconSize = 18.dp)
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
            T("corn", 44.sp, C.Accent, FontWeight.ExtraBold, display = true)
            Spacer(Modifier.height(4.dp))
            T(c.quip ?: "Browse. Save what matters. Flush the rest.", 19.sp, if (c.quip != null) C.Accent else C.Text, FontWeight.SemiBold, display = true)
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
                    cursorBrush = SolidColor(C.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { c.go(q); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (q.isNotBlank()) SuggestionList(c, q, Modifier.fillMaxWidth())
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
                    CornIcon(CornIcons.Vault, 24.dp, C.Accent)
                }
            }
        }
        // All-time totals: real counts of requests Corn blocked, kept across Flushes (numbers only).
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(18.dp)) {
            T("STATS", 12.sp, C.Muted, FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(c.settings.totalBlocked, "Trackers & ads blocked", C.Accent, Modifier.weight(1f))
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
        // Back (hold for Forward). On a tab's first page opened from Corn's home page, Back shows
        // a curved ↩ and returns to the home page.
        val home = !cur.isNewTab && !cur.canGoBack && cur.openedFromHome
        Box(
            Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    enabled = cur.canGoBack || cur.canGoForward || home,
                    onClick = { if (home) c.tabs.goHome() else c.tabs.goBack() },
                    onLongClick = { c.tabs.goForward(); c.showToast("Forward") },
                ),
            contentAlignment = Alignment.Center,
        ) {
            CornIcon(
                CornIcons.Back, TOOLBAR_ICON,
                tint = if (cur.canGoBack || home) C.Text else C.Muted.copy(alpha = 0.45f),
            )
        }
        ToolbarButton({ c.openVault() }) { CornIcon(CornIcons.Vault, TOOLBAR_ICON) }
        ToolbarButton({ c.openTabsScreen() }) {
            // 18 dp: the same visible height as the other toolbar icons.
            Box(Modifier.size(18.dp).border(1.9.dp, C.Text, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                T(c.tabs.tabs.size.toString(), 11.sp, weight = FontWeight.ExtraBold)
            }
        }
        // Flush (second from the right)
        ToolbarButton({ c.requestFlush() }) {
            Box(Modifier.width(46.dp).height(40.dp).clip(RoundedCornerShape(14.dp)).background(C.Corn), contentAlignment = Alignment.Center) {
                CornIcon(CornIcons.Flush, TOOLBAR_ICON, C.CornInk)
            }
        }
        ToolbarButton({ c.sheet = Sheet.Menu }) { CornIcon(CornIcons.Dots, TOOLBAR_ICON) }
    }
}

/** Every toolbar icon is drawn on a 24 dp canvas whose visible shape is 18 dp tall. */
private val TOOLBAR_ICON = 24.dp

@Composable
private fun ToolbarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}
