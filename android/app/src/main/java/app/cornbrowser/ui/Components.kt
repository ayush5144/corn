/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.browser.TabEntry

// ---------------- text ----------------

@Composable
fun T(
    text: String,
    size: TextUnit = 15.sp,
    color: Color = C.Text,
    weight: FontWeight = FontWeight.Normal,
    display: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) = Text(
    text,
    modifier = modifier,
    color = color,
    fontSize = size,
    fontWeight = weight,
    fontFamily = if (display) Display else Ui,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    lineHeight = (size.value * 1.3f).sp,
)

@Composable
fun Label(text: String, modifier: Modifier = Modifier) =
    T(text.uppercase(), size = 12.sp, color = C.Muted, weight = FontWeight.SemiBold, modifier = modifier.padding(top = 14.dp, bottom = 8.dp))

@Composable
fun CornIcon(icon: ImageVector, size: Dp = 22.dp, tint: Color = C.Text, modifier: Modifier = Modifier) =
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))

// ---------------- buttons ----------------

@Composable
fun IconBtn(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = C.Text, size: Dp = 42.dp, iconSize: Dp = 22.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { CornIcon(icon, iconSize, tint) }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, ghost: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
            .background(if (ghost) C.Surface2 else C.Corn).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (ghost) C.Text else C.CornInk
        if (icon != null) { CornIcon(icon, 20.dp, fg); Spacer(Modifier.width(8.dp)) }
        T(text, 16.sp, fg, FontWeight.Bold)
    }
}

@Composable
fun Pill(text: String, onClick: () -> Unit, icon: ImageVector? = null, corn: Boolean = false) {
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (corn) C.Corn else C.Surface2)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (corn) C.CornInk else C.Text
        if (icon != null) { CornIcon(icon, 16.dp, fg); Spacer(Modifier.width(6.dp)) }
        T(text, 14.sp, fg, FontWeight.SemiBold)
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, dashed: Boolean = false, trailing: ImageVector? = null) {
    Row(
        Modifier.height(34.dp).clip(RoundedCornerShape(17.dp))
            .background(if (on) C.Corn else if (dashed) Color.Transparent else C.Surface2)
            .border(BorderStroke(1.dp, if (on) C.Corn else C.Line), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(text, 14.sp, if (on) C.CornInk else if (dashed) C.Muted else C.Text, if (on) FontWeight.SemiBold else FontWeight.Normal)
        if (trailing != null) { Spacer(Modifier.width(6.dp)); CornIcon(trailing, 12.dp, if (on) C.CornInk else C.Muted) }
    }
}

// ---------------- rows, switches, radios ----------------

@Composable
fun Row50(icon: ImageVector?, text: String, onClick: () -> Unit, hint: String? = null, enabled: Boolean = true, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha = if (enabled) 1f else 0.4f
        if (icon != null) { CornIcon(icon, 22.dp, C.Muted.copy(alpha = alpha)); Spacer(Modifier.width(14.dp)) }
        T(text, 15.sp, C.Text.copy(alpha = alpha), modifier = Modifier.weight(1f))
        if (hint != null) T(hint, 12.sp, C.Muted)
        trailing?.invoke()
    }
}

@Composable
fun CornSwitch(on: Boolean) {
    Box(Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(if (on) C.Leaf else C.Line)) {
        Box(Modifier.offset(x = if (on) 21.dp else 3.dp, y = 3.dp).size(22.dp).clip(CircleShape).background(C.Text))
    }
}

@Composable
fun Radio(on: Boolean) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) C.Corn else C.Muted, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (on) Box(Modifier.size(11.dp).clip(CircleShape).background(C.Corn)) }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(C.Surface2).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(if (i == selected) C.Corn else Color.Transparent)
                    .clickable { onSelect(i) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { T(o, 14.sp, if (i == selected) C.CornInk else C.Muted, FontWeight.SemiBold) }
        }
    }
}

/** A rounded group of settings rows, like the mockup's .sgroup. */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(horizontal = 14.dp, vertical = 4.dp), content = content)
}

@Composable
fun SettingRow(title: String, sub: String? = null, onClick: (() -> Unit)? = null, leading: @Composable (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            T(title, 15.sp)
            if (sub != null) T(sub, 12.5.sp, C.Muted)
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
    }
}

@Composable
fun Caption(text: String) = T(text, 12.5.sp, C.Muted, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 18.dp))

// ---------------- fields ----------------

@Composable
fun Field(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, onDone: (() -> Unit)? = null, height: Dp = 46.dp, leading: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(12.dp)).background(C.Surface2).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) T(placeholder, 15.sp, C.Muted, maxLines = 1)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = C.Text, fontSize = 15.sp, fontFamily = Ui),
                cursorBrush = SolidColor(C.Corn),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------- sheets ----------------

/** Bottom sheet with a dim scrim, like the mockup's .sheet. Tap outside to close. */
@Composable
fun BoxScope.Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(C.Scrim).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss))
    }
    AnimatedVisibility(
        visible, modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(280)) { it }, exit = slideOutVertically(tween(200)) { it },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(C.Surface)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp, bottom = 12.dp).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.Line))
            content()
        }
    }
}

@Composable
fun SheetTitle(title: String, sub: String? = null) {
    T(title, 20.sp, weight = FontWeight.Bold, display = true)
    if (sub != null) T(sub, 13.sp, C.Muted, maxLines = 2, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
    else Spacer(Modifier.height(10.dp))
}

// ---------------- tabs ----------------

/** A site's favicon in a round dot, or its coloured letter when there's no favicon. */
@Composable
fun SiteIcon(entry: TabEntry, size: Dp = 22.dp) {
    val fav = entry.favicon
    if (entry.isNewTab) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) { T("🌽", (size.value * 0.7f).sp) }
    } else if (fav != null) {
        Image(fav.asImageBitmap(), null, Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)), contentScale = ContentScale.Crop)
    } else {
        LetterIcon(entry.host, size)
    }
}

@Composable
fun LetterIcon(host: String, size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(C.forHost(host)), contentAlignment = Alignment.Center) {
        T(host.firstOrNull()?.uppercase() ?: "?", (size.value * 0.55f).sp, Color.White, FontWeight.ExtraBold)
    }
}

/** Tab preview card: screenshot if we have one, otherwise favicon + title. */
@Composable
fun TabPreview(entry: TabEntry, isCurrent: Boolean, onOpen: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(3f / 4f)) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(C.Bg)
                .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) C.Corn else C.Line, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpen),
        ) {
            Row(Modifier.fillMaxWidth().height(26.dp).background(C.Surface2).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SiteIcon(entry, 14.dp)
                Spacer(Modifier.width(6.dp))
                T(if (entry.isNewTab) "New tab" else entry.host, 10.sp, C.Text, FontWeight.Bold, maxLines = 1)
            }
            val shot = entry.preview
            if (shot != null && !entry.isNewTab) {
                Image(shot.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            } else {
                Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(entry.title, 11.5.sp, weight = FontWeight.SemiBold, maxLines = 3)
                    repeat(2) { Box(Modifier.fillMaxWidth(if (it == 1) 0.6f else 1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.Surface2)) }
                }
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(Color(0xA6000000)).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { CornIcon(CornIcons.X, 14.dp, Color.White) }
    }
}

// ---------------- PIN pad ----------------

@Composable
fun PinPad(entered: Int, onDigit: (Char) -> Unit, onBackspace: () -> Unit, onFingerprint: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(if (i < entered) C.Corn else Color.Transparent)
                        .border(2.dp, if (i < entered) C.Corn else C.Muted, CircleShape),
                )
            }
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "fp", "0", "del")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier.width(72.dp).height(60.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (k == "fp" && onFingerprint == null) Color.Transparent else C.Surface)
                                .clickable(enabled = !(k == "fp" && onFingerprint == null)) {
                                    when (k) {
                                        "fp" -> onFingerprint?.invoke()
                                        "del" -> onBackspace()
                                        else -> onDigit(k[0])
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            when (k) {
                                "fp" -> if (onFingerprint != null) CornIcon(CornIcons.Fingerprint, 26.dp)
                                "del" -> CornIcon(CornIcons.Backspace, 24.dp)
                                else -> T(k, 22.sp, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- toast ----------------

data class ToastMsg(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null, val id: Long = System.nanoTime())

@Composable
fun BoxScope.ToastHost(msg: ToastMsg?, bottomPadding: Dp, onAction: () -> Unit) {
    AnimatedVisibility(
        msg != null, modifier = Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = bottomPadding),
        enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut(),
    ) {
        val m = msg ?: return@AnimatedVisibility
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Text).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T(m.text, 14.sp, C.Bg, FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (m.action != null) T(m.action, 14.sp, Color(0xFF8A5B00), FontWeight.ExtraBold, modifier = Modifier.clickable(onClick = onAction).padding(start = 12.dp))
        }
    }
}
