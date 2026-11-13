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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
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
import uniffi.corn_ffi.Favorite
import uniffi.corn_ffi.siteHost
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
            val listAt = Modifier.align(if (c.settings.addressBarBottom) Alignment.BottomCenter else Alignment.TopCenter).padding(horizontal = 10.dp, vertical = 6.dp)
            if (!q.isNullOrBlank()) {
                SuggestionList(c, q, listAt)
            } else if (q != null && !cur.isNewTab) {
                // The bar was just tapped and is empty (founder, 2026-11-13, like Brave): offer the
                // current page here instead of pre-filling the box with it.
                CurrentPageRow(c, cur, listAt)
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
    // Current-page row → Edit: put the address in the box, cursor at the end.
    LaunchedEffect(c.addressPrefill) {
        val p = c.addressPrefill ?: return@LaunchedEffect
        c.addressPrefill = null
        if (editing) { field = TextFieldValue(p, TextRange(p.length)); c.addressQuery = p }
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(Corners.Large)).background(C.Surface2).padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search Switcher (left)
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(Corners.Large)).clickable { c.sheet = Sheet.Engine }.padding(horizontal = 4.dp),
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
                    onValueChange = { v ->
                        // Right after the tap empties the box, the keyboard can echo the old address back
                        // once (a selection update); ignore that echo so the box stays empty.
                        val echo = field.text.isEmpty() && shown.isNotEmpty() && v.text == shown
                        if (editing && !echo) { field = v; c.addressQuery = v.text }
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                    cursorBrush = SolidColor(C.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { c.go(field.text); c.addressQuery = null; focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        // Tapping the bar starts with an empty box (founder, 2026-11-13, like Brave); the
                        // current page is offered below it (CurrentPageRow: Edit, Copy).
                        if (it.isFocused && !editing) field = TextFieldValue("")
                        editing = it.isFocused
                        // Empty but focused: the current-page row shows; suggestions start once the user types.
                        c.addressQuery = if (it.isFocused) "" else null
                    },
                )
            }
            // Blocking shield (right)
            val off = !cur.isNewTab && remember(c.data.vaultVersion.intValue, cur.host) { c.shields(cur.host).allOff }
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(Corners.Large))
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
        Image(painterResource(R.drawable.corn_logo), null, Modifier.size(64.dp))
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
        Image(painterResource(R.drawable.corn_logo), null, Modifier.size(64.dp))
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
/** Shown when the address bar is tapped and still empty: the page you're on, with Edit and Copy. */
@Composable
private fun CurrentPageRow(c: CornController, cur: TabEntry, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Corners.Large)).background(C.Surface2)
            .border(1.dp, C.Line, RoundedCornerShape(Corners.Large)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteIcon(cur, 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T(cur.title.ifBlank { cur.host }, 14.5.sp, maxLines = 1)
            T(cur.url, 12.sp, C.Muted, maxLines = 1)
        }
        // Brave's order: Share, Copy, Edit.
        IconBtn(CornIcons.Share, "Share page", { c.share(cur.url) })
        IconBtn(CornIcons.Copy, "Copy address", { c.copyLink(cur.url) })
        IconBtn(CornIcons.Edit, "Edit address", { c.addressPrefill = cur.url })
    }
}

@Composable
private fun SuggestionList(c: CornController, query: String, modifier: Modifier) {
    val focus = LocalFocusManager.current
    val items = remember(query, c.data.historyVersion.intValue, c.data.vaultVersion.intValue, c.vaultUnlocked) { c.suggestions(query) }
    if (items.isEmpty()) return
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Corners.Large)).background(C.Surface2)
            .border(1.dp, C.Line, RoundedCornerShape(Corners.Large)).padding(vertical = 4.dp),
    ) {
        items.forEach { s ->
            val host = android.net.Uri.parse(s.url).host?.removePrefix("www.") ?: s.url
            Row(
                Modifier.fillMaxWidth().clickable { focus.clearFocus(); c.openSuggestion(s) }.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The site's letter (Corn keeps no favicons on disk, so this is what it can show for
                // History and Vault links); "Vault ·" marks links from the Vault.
                LetterIcon(host, 24.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    T(s.title, 14.5.sp, maxLines = 1)
                    T(if (s.fromVault) "Vault · $host" else host, 12.sp, C.Muted, maxLines = 1)
                }
                // ↖ puts this address in the box to refine it, without opening it (like Brave).
                IconBtn(CornIcons.FillIn, "Use this address", { c.addressPrefill = s.url })
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
            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(Corners.Large)).background(C.Surface2).padding(horizontal = 14.dp),
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

/** A centred line of the new-tab tagline. A line ending in the corn emoji drops it: Corny is already above. */
@Composable
private fun TaglineText(text: String, color: Color) {
    Text(
        text.removeSuffix("🌽").trimEnd(), color = color, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
        fontFamily = Display, lineHeight = (19 * 1.3).sp, textAlign = TextAlign.Center,
    )
}

@Composable
private fun NewTabPage(c: CornController) {
    val focus = LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    val appear = remember(c.quip) { Animatable(0.94f) }
    LaunchedEffect(c.quip) { appear.animateTo(1f, tween(450)) }
    // Favourites show even while the Vault is locked (founder, Favourites A+; said when adding).
    val favorites = remember(c.data.vaultVersion.intValue) { c.data.favorites() }
    val icons = rememberFavoriteIcons(c)

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val viewportHeight = maxHeight
    Column(
        Modifier.fillMaxSize().background(C.Bg).verticalScroll(rememberScrollState())
            .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // The search bar is the anchor (founder, 2026-11-13): it sits about a third of the way down,
        // like the build with the big logo, and the tagline sits right above it, bottom-aligned, so a
        // short line still hugs the search bar and the spare room goes above it. One sentence per
        // line, centred. Corny sits above the tagline, where the wordmark used to be (founder, 2026-11-13).
        Box(Modifier.fillMaxWidth().height((viewportHeight * 0.34f).coerceAtLeast(150.dp)), contentAlignment = Alignment.BottomCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.corn_logo), "Corn", Modifier.size(56.dp))
                Spacer(Modifier.height(10.dp))
                val quip = c.quip
                val lines = Copy.oneSentencePerLine(quip ?: "Browse. Save what matters. Flush the rest.").split("\n")
                lines.forEach { line -> TaglineText(line, if (quip != null) C.Accent else C.Text) }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(Corners.Large)).background(C.Surface2).padding(horizontal = 14.dp),
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
        // Quick tiles. No favourites: the Vault tile and the "+ Favourite" tile, centred. Otherwise
        // the Favourites row (layout A+): the Vault fixed at the start, a thin divider, favourites
        // scrolling sideways with "+ Favourite" last. No header, no Edit: long-press a tile.
        // "View all" under the row on the right once there are more than fit (5+).
        Column(Modifier.fillMaxWidth()) {
            if (favorites.isEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    VaultTile(c)
                    AddFavoriteTile(c)
                }
            } else {
                FavoritesRow(c, favorites, icons)
            }
            if (favorites.size >= VIEW_ALL_FROM) {
                T(
                    "View all", 13.sp, C.Accent, FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp).clip(RoundedCornerShape(Corners.Small))
                        .clickable { c.openFavorites() }.padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        }
        // All-time totals: real counts of requests Corn blocked, kept across Flushes (numbers only).
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Corners.Large)).background(C.Surface).padding(18.dp)) {
            T("STATS", 12.sp, C.Muted, FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(c.settings.totalBlocked, "Trackers & ads blocked", C.Accent, Modifier.weight(1f))
                Stat(c.settings.totalPopups, "Pop-ups & redirects stopped", C.Leaf, Modifier.weight(1f))
            }
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

/** "View all" shows from this many favourites: up to 4 fit beside the Vault tile on a 360 dp phone. */
private const val VIEW_ALL_FROM = 5

/** One tile column in the Favourites row and drawer: the 56 dp icon plus a little room for the label. */
internal const val FAVORITE_TILE_DP = 62f

/** The last tile of the row: "+ Favourite" opens the Add sheet (address and an optional name). */
@Composable
private fun AddFavoriteTile(c: CornController, width: androidx.compose.ui.unit.Dp = FAVORITE_TILE_DP.dp) {
    val line = C.Line
    QuickTile("Favourite", { c.sheet = Sheet.AddFavorite(fromAll = false) }, width = width) {
        Box(
            Modifier.size(56.dp).drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    line,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(Corners.Large.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
            contentAlignment = Alignment.Center,
        ) { CornIcon(CornIcons.Plus, 24.dp, C.Muted) }
    }
}

@Composable
private fun VaultTile(c: CornController) {
    QuickTile("Vault", { c.openVault() }, width = FAVORITE_TILE_DP.dp) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(Corners.Large)).background(C.Surface2), contentAlignment = Alignment.Center) {
            CornIcon(CornIcons.Vault, 24.dp, C.Accent)
        }
    }
}

/** Each favourite site's saved icon, decoded once per change. */
@Composable
private fun rememberFavoriteIcons(c: CornController): Map<String, FavIcon> =
    remember(c.data.vaultVersion.intValue) {
        c.data.favoriteIcons().mapNotNull { (host, png) ->
            runCatching { android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size) }.getOrNull()?.let { host to FavIcon.of(it) }
        }.toMap()
    }

/**
 * A decoded favourite icon plus what's needed to keep it visible: its average brightness and whether
 * it is see-through (many sites serve a white or black mark on a transparent background).
 */
private class FavIcon(val image: ImageBitmap, val luminance: Float, val seeThrough: Boolean) {
    companion object {
        fun of(b: android.graphics.Bitmap): FavIcon {
            val px = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
            var sum = 0f; var opaque = 0
            for (p in px) {
                if (android.graphics.Color.alpha(p) < 128) continue
                opaque++
                sum += (0.2126f * android.graphics.Color.red(p) + 0.7152f * android.graphics.Color.green(p) + 0.0722f * android.graphics.Color.blue(p)) / 255f
            }
            val lum = if (opaque == 0) 0.5f else sum / opaque
            return FavIcon(b.asImageBitmap(), lum, opaque < px.size * 3 / 4)
        }
    }
}

/**
 * The Vault tile, a divider, then the Favourites scrolling sideways, "+ Favourite" last. The Vault
 * tile and four favourites fit on a 360 dp phone (5 × 62 dp + 7 dp divider). With 5 or more, the
 * tile width is adjusted (close to 62 dp) so the tile at the right edge is cut about in half; the
 * edge fades out while there is more to scroll, so it's clear the row scrolls.
 */
@Composable
private fun FavoritesRow(c: CornController, favorites: List<Favorite>, icons: Map<String, FavIcon>) {
    val scroll = rememberScrollState()
    val line = C.Line
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        VaultTile(c)
        Box(Modifier.padding(start = 2.dp, end = 4.dp).width(1.dp).height(56.dp).background(line))
        BoxWithConstraints(Modifier.weight(1f)) {
        // Up to 4 favourites keep the usual width (they fit; "+ Favourite" may scroll). From 5, the
        // width is tuned so a tile is cut in half at the edge.
        val tile = if (favorites.size < VIEW_ALL_FROM) FAVORITE_TILE_DP.dp else favoriteTileWidth(maxWidth, favorites.size + 1)
        Row(
            Modifier.fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (scroll.canScrollForward) {
                        val fade = 36.dp.toPx()
                        drawRect(
                            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
                .horizontalScroll(scroll),
        ) {
            favorites.forEach { fav -> FavoriteTile(c, fav, icons, fromAll = false, width = tile) }
            AddFavoriteTile(c, width = tile)
        }
        }
    }
}

/** [FAVORITE_TILE_DP] when every tile fits; otherwise the width nearest it (at least 60) that leaves half a tile showing at the edge. */
internal fun favoriteTileWidth(available: androidx.compose.ui.unit.Dp, count: Int): androidx.compose.ui.unit.Dp {
    val usual = FAVORITE_TILE_DP
    val w = available.value
    if (count * usual <= w) return usual.dp
    val whole = (w / usual - 0.5f).toInt().coerceAtLeast(1)
    val options = listOf(whole, whole + 1).map { w / (it + 0.5f) }.filter { it >= 60f }
    return (options.minByOrNull { kotlin.math.abs(it - usual) } ?: (w / (whole + 0.5f))).dp
}

/**
 * One Favourite: the site's icon (kept from a page Corn showed) centred on the tile, or its coloured
 * letter; the name (or host) under it. Tap opens it here; long-press: Remove / Move.
 */
@Composable
private fun FavoriteTile(c: CornController, fav: Favorite, icons: Map<String, FavIcon>, fromAll: Boolean, width: androidx.compose.ui.unit.Dp = FAVORITE_TILE_DP.dp) {
    val host = android.net.Uri.parse(fav.url).host?.removePrefix("www.") ?: ""
    val icon = icons[siteHost(fav.url)]
    QuickTile(fav.title.ifBlank { host }, { c.openFavorite(fav.url) }, onLongClick = { c.sheet = Sheet.FavoriteTile(fav, fromAll) }, width = width) {
        if (icon != null) {
            val tile = C.Surface2
            val tileLight = tile.luminance() > 0.5f
            // A see-through icon too close to the tile's brightness (GitHub's white mark on a light
            // tile) gets a small plate of the opposite tone behind it.
            val plate = icon.seeThrough && kotlin.math.abs(icon.luminance - tile.luminance()) < 0.35f
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(Corners.Large)).background(tile), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(Corners.Medium))
                        .background(if (!plate) Color.Transparent else if (tileLight) Color(0xFF2E2A24) else Color(0xFFF2EEE6)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(icon.image, null, Modifier.size(30.dp).clip(RoundedCornerShape(Corners.Small)), filterQuality = FilterQuality.Medium)
                }
            }
        } else {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(Corners.Large)).background(C.forHost(host)), contentAlignment = Alignment.Center) {
                T(host.firstOrNull()?.uppercase() ?: "?", 20.sp, Color.White, FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * "View all": every Favourite as tiles, 4 across, in favourite order, under "Favourites (n)" with
 * "+ Add" on the right. Shown in the bottom drawer (Sheets.kt).
 */
@Composable
fun FavoritesGrid(c: CornController) {
    val favorites = remember(c.data.vaultVersion.intValue) { c.data.favorites() }
    val icons = rememberFavoriteIcons(c)
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        // The count in brackets in the lighter muted colour, on the title's baseline.
        val muted = C.Muted
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("Favourites")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = muted, fontWeight = FontWeight.SemiBold))
                append(" (${favorites.size})")
                pop()
            },
            Modifier.weight(1f), color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Display, maxLines = 1,
        )
        T(
            "+ Add", 15.sp, C.Accent, FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(Corners.Small)).clickable { c.sheet = Sheet.AddFavorite(fromAll = true) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        favorites.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { fav -> Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { FavoriteTile(c, fav, icons, fromAll = true) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    T("Long-press a tile to remove or move it.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 16.dp, start = 4.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickTile(
    label: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, width: androidx.compose.ui.unit.Dp = 72.dp, icon: @Composable () -> Unit,
) {
    Column(
        Modifier.width(width).clip(RoundedCornerShape(Corners.Medium)).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        T(label, 12.sp, C.Muted, maxLines = 1, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 2.dp))
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
            Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(Corners.Large))
                .combinedClickable(
                    enabled = cur.canGoBack || cur.canGoForward || home,
                    onClick = { if (home) c.tabs.goHome() else c.tabs.goBack() },
                    onLongClick = { c.tabs.goForward(); c.showToast("Forward") },
                ),
            contentAlignment = Alignment.Center,
        ) {
            CornIcon(
                CornIcons.NavBack, TOOLBAR_ICON,
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
            // No yellow box (founder, 2026-11-13: it stood out too much): the swirl itself in corn yellow;
            // C.Accent is the darker yellow in light mode, so the lines stay visible on cream.
            CornIcon(CornIcons.Flush, TOOLBAR_ICON, C.Accent)
        }
        ToolbarButton({ c.sheet = Sheet.Menu }) { CornIcon(CornIcons.Dots, TOOLBAR_ICON) }
    }
}

/** Every toolbar icon is drawn on a 24 dp canvas whose visible shape is 18 dp tall. */
private val TOOLBAR_ICON = 24.dp

@Composable
private fun ToolbarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.width(56.dp).height(48.dp).clip(RoundedCornerShape(Corners.Large)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}
