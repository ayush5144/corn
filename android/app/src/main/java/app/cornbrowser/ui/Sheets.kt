/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.CornController
import app.cornbrowser.Sheet
import app.cornbrowser.data.SearchEngine
import app.cornbrowser.data.SearchEngines
import app.cornbrowser.data.Shields
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource

/** Every bottom sheet, drawn over whichever screen is showing. */
@Composable
fun BoxScope.Sheets(c: CornController) {
    // Keep the last sheet's content while it slides away.
    var last by remember { mutableStateOf<Sheet?>(null) }
    val s = c.sheet
    if (s != null) last = s
    Sheet(visible = s != null, onDismiss = { c.sheet = null }) {
        when (val shown = s ?: last) {
            is Sheet.Link -> LinkSheet(c, shown)
            Sheet.Menu -> MenuSheet(c)
            Sheet.Engine -> EngineSheet(c)
            Sheet.Shield -> ShieldSheet(c)
            is Sheet.Save -> SaveSheet(c, shown)
            is Sheet.Group -> GroupSheet(c, shown.groupId)
            Sheet.ConfirmFlush -> ConfirmFlushSheet(c)
            null -> {}
        }
    }
}

@Composable
private fun LinkSheet(c: CornController, s: Sheet.Link) {
    val group = c.tabs.group(c.tabs.current?.groupId)
    SheetTitle(s.title, s.url)
    Row50(CornIcons.ChevRight, "Open", { c.sheet = null; c.tabs.load(s.url) })
    Row50(CornIcons.TabNew, "Open in new tab", { c.openLinkInNewTab(s.url) })
    // Not in a group yet: this starts one. In a group: adds to it (its name shown on the right).
    if (group == null) Row50(CornIcons.GroupAdd, "Open in new tab group", { c.openLinkInGroup(s.url) })
    else Row50(CornIcons.GroupAdd, "Open in new tab in group", { c.openLinkInGroup(s.url) }, hint = group.name)
    Row50(CornIcons.VaultAdd, "Save link to Vault", { c.sheet = Sheet.Save(s.url, s.title) })
    Row50(CornIcons.Copy, "Copy link", { c.copyLink(s.url) })
}

@Composable
private fun MenuSheet(c: CornController) {
    val cur = c.tabs.current
    Row50(CornIcons.Forward, "Forward", { c.sheet = null; c.tabs.goForward() }, enabled = cur?.canGoForward == true)
    Row50(CornIcons.TabNew, "New tab", { c.newTab() })
    if (cur?.groupId == null) Row50(CornIcons.GroupAdd, "New tab group", { c.newTabGroup() })
    Row50(CornIcons.Share, "Share page", { cur?.takeIf { !it.isNewTab }?.let { c.share(it.url) } ?: run { c.sheet = null } }, enabled = cur?.isNewTab == false)
    Row50(CornIcons.Sliders, "Settings", { c.sheet = null; c.screen = app.cornbrowser.Screen.Settings })
}

@Composable
private fun EngineSheet(c: CornController) {
    SheetTitle("Search with", "Just for your next searches. Change the default in Settings.")
    SearchEngines.all.forEach { e ->
        SettingRow(
            e.name,
            onClick = { c.engineNow = e; c.sheet = null; c.showToast("Searching with ${e.name}") },
            leading = { EngineBadge(e) },
            trailing = { if (c.engineNow.id == e.id) CornIcon(CornIcons.Check, tint = C.Corn) },
        )
    }
}

@Composable
private fun ShieldSheet(c: CornController) {
    val cur = c.tabs.current ?: return
    val host = cur.host
    val sh = remember(c.data.vaultVersion.intValue, host) { c.shields(host) }
    var advanced by remember { mutableStateOf(!sh.allOn && !sh.allOff) }
    SheetTitle(
        "Blocking on $host",
        when {
            sh.allOff -> "Blocking is off for this site."
            else -> "Blocked on this page: ${cur.blocked} ads & trackers, ${cur.guarded} pop-ups & redirects."
        },
    )
    SettingRow(
        "Block ads, trackers and pop-ups here",
        if (!sh.allOn && !sh.allOff) "Some blocking is off. See Advanced." else null,
        onClick = { c.setShields(host, if (sh.allOff) Shields.ALL_ON else Shields.ALL_OFF) },
        trailing = { CornSwitch(!sh.allOff) },
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { advanced = !advanced }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T("Advanced", 15.sp, C.Corn, FontWeight.SemiBold, modifier = Modifier.weight(1f))
        CornIcon(if (advanced) CornIcons.ChevUp else CornIcons.ChevDown, 18.dp, C.Corn)
    }
    AnimatedVisibility(advanced) {
        Column {
            SettingRow("Ads", "Ad networks and ad scripts (EasyList)", onClick = { c.setShields(host, sh.copy(ads = !sh.ads)) }, trailing = { CornSwitch(sh.ads) })
            SettingRow("Trackers", "Analytics and tracking scripts (EasyPrivacy)", onClick = { c.setShields(host, sh.copy(trackers = !sh.trackers)) }, trailing = { CornSwitch(sh.trackers) })
            SettingRow("Pop-ups & redirects", "New windows and pages jumping to other sites", onClick = { c.setShields(host, sh.copy(popups = !sh.popups)) }, trailing = { CornSwitch(sh.popups) })
        }
    }
    T("Only turn things off if the page breaks. Changes apply to this site only and reload the page.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveSheet(c: CornController, s: Sheet.Save) {
    val collections = remember(c.data.vaultVersion.intValue) { c.data.collections() }
    var title by remember(s) { mutableStateOf(s.title) }
    var chosen by remember(s) { mutableStateOf(c.data.unsortedId()) }
    var naming by remember(s) { mutableStateOf(false) }
    var newName by remember(s) { mutableStateOf("") }
    val tags = remember(s) { mutableStateListOf<String>() }
    var tagInput by remember(s) { mutableStateOf("") }

    SheetTitle("Save to Vault", s.url)
    Label("Title")
    Field(title, { title = it }, "Title")
    Label("Collection")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        collections.forEach { col -> Chip(col.name, col.id == chosen, { chosen = col.id }) }
        if (naming) {
            Field(newName, { newName = it }, "Name it", Modifier.width(150.dp), height = 34.dp, onDone = {
                c.data.createCollection(newName)?.let { chosen = it.id }
                naming = false
                newName = ""
            })
        } else {
            Chip("+ New", false, { naming = true }, dashed = true)
        }
    }
    Label("Tags (optional)")
    if (tags.isNotEmpty()) {
        FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { t -> Chip(t, false, { tags.remove(t) }, trailing = CornIcons.X) }
        }
    }
    Field(tagInput, { tagInput = it }, "Type a tag and press done", onDone = {
        val t = tagInput.trim().lowercase()
        if (t.isNotEmpty() && t !in tags) tags += t
        tagInput = ""
    })
    Spacer(Modifier.height(18.dp))
    BigButton("Save", {
        val col = collections.firstOrNull { it.id == chosen } ?: c.data.collections().first { it.id == chosen }
        c.save(col.id, col.name, title, s.url, tags.toList())
    }, icon = CornIcons.VaultAdd)
    T("You can save while the Vault is locked. Seeing what's inside needs your PIN.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun GroupSheet(c: CornController, groupId: Int) {
    val g = c.tabs.group(groupId) ?: run { c.sheet = null; return }
    val members = c.tabs.groupTabs(groupId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(g.color).clickable { c.cycleGroupColor(groupId) })
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            g.name, { g.name = it }, singleLine = true,
            textStyle = TextStyle(color = C.Text, fontSize = 20.sp, fontFamily = Display, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(C.Corn),
            modifier = Modifier.weight(1f),
        )
    }
    T("${members.size} tabs · tap a tab to jump to it", 13.sp, C.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        members.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    TabPreview(t, t.id == c.tabs.currentId, { c.switchTab(t.id) }, { c.tabs.close(t.id) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    BigButton("Save group to Vault", { c.saveGroup(groupId) }, icon = CornIcons.VaultAdd)
    Spacer(Modifier.height(10.dp))
    BigButton("New tab in group", { c.newTabInGroup(groupId) }, icon = CornIcons.Plus, ghost = true)
    T("Flush wipes every group. Save it first if you want these links later.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ConfirmFlushSheet(c: CornController) {
    SheetTitle("Flush everything?", "All tabs and groups, cookies, cache and history go. Your Vault and Keep List stay.")
    BigButton("Flush", { c.flushNow(animate = true) }, icon = CornIcons.Toilet)
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

/** A search engine's favicon (bundled with the app, never fetched). */
@Composable
fun EngineBadge(engine: SearchEngine, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Image(painterResource(engine.icon), engine.name, Modifier.size(size).clip(RoundedCornerShape(size * 0.25f)))
}
