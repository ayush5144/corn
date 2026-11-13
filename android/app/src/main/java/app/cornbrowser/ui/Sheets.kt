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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.CornController
import app.cornbrowser.Sheet
import app.cornbrowser.browser.ClearChoice
import app.cornbrowser.data.TabMemory
import app.cornbrowser.data.VaultExport
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
    Sheet(visible = s != null, onDismiss = { c.dismissSheet() }, onHeight = { c.sheetHeightPx = it }) {
        when (val shown = s ?: last) {
            // These three keep their main buttons pinned at the bottom (their content can get long).
            is Sheet.Save -> SaveSheet(c, shown)
            is Sheet.Group -> GroupSheet(c, shown.groupId)
            Sheet.SplitPick -> SplitPickSheet(c)
            else -> SheetScroll { OtherSheet(c, shown) }
        }
    }
}

/** Every sheet whose whole content scrolls (short sheets and lists without a main button). */
@Composable
private fun OtherSheet(c: CornController, shown: Sheet?) {
        when (shown) {
            is Sheet.Link -> LinkSheet(c, shown)
            is Sheet.Image -> ImageSheet(c, shown)
            Sheet.NewCollection -> NewCollectionSheet(c)
            is Sheet.ExternalApp -> ExternalAppSheet(c, shown)
            Sheet.Menu -> MenuSheet(c)
            Sheet.Engine -> EngineSheet(c)
            Sheet.Shield -> ShieldSheet(c)
            Sheet.ConfirmFlush -> ConfirmFlushSheet(c)
            is Sheet.DeleteItem -> DeleteItemSheet(c, shown)
            is Sheet.HistoryEntry -> HistoryEntrySheet(c, shown)
            Sheet.DeleteData -> DeleteDataSheet(c)
            Sheet.ConfirmKeepAllTabs -> ConfirmKeepAllTabsSheet(c)
            Sheet.ConfirmClearHistory -> ConfirmClearHistorySheet(c)
            is Sheet.SitePermissionAsk -> SitePermissionSheet(c, shown)
            is Sheet.Download -> DownloadSheet(c, shown)
            is Sheet.Password -> PasswordSheetContent(c, shown.kind)
            Sheet.VaultMenu -> VaultMenuSheet(c)
            Sheet.ExportLinks -> ExportLinksSheet(c)
            else -> {}
        }
}

@Composable
private fun LinkSheet(c: CornController, s: Sheet.Link) {
    SheetTitle(s.title, s.url)
    LinkRows(c, s.url, s.title)
}

/** Every link action, the same in the link menu and in the menu of a picture that is a link. */
@Composable
private fun LinkRows(c: CornController, url: String, title: String) {
    val group = c.tabs.group(c.tabs.current?.groupId)
    Row50(CornIcons.ChevRight, "Open", { c.sheet = null; c.tabs.load(url) })
    Row50(CornIcons.TabNew, "Open in new tab", { c.openLinkInNewTab(url) })
    // Not in a group yet: this starts one. In a group: adds to it (its name shown on the right).
    if (group == null) Row50(CornIcons.GroupAdd, "Open in new tab group", { c.openLinkInGroup(url) })
    else Row50(CornIcons.GroupAdd, "Open in new tab in group", { c.openLinkInGroup(url) }, hint = group.name)
    Row50(CornIcons.VaultAdd, "Save link to Vault", { c.sheet = Sheet.Save(url, title) })
    Row50(CornIcons.Copy, "Copy link", { c.copyLink(url) })
    Row50(CornIcons.Share, "Share link", { c.share(url) })
}

@Composable
private fun MenuSheet(c: CornController) {
    val cur = c.tabs.current
    Row50(CornIcons.Forward, "Forward", { c.sheet = null; c.tabs.goForward() }, enabled = cur?.canGoForward == true)
    Row50(CornIcons.TabNew, "New tab", { c.newTab() })
    // Not in a group: group this tab with a new tab beside it. In a group: a new tab beside this one.
    val gid = cur?.groupId
    if (gid == null) Row50(CornIcons.GroupAdd, "New tab group", { c.newTabGroup() })
    else Row50(CornIcons.GroupAdd, "New tab in this group", { c.newTabInGroup(gid) }, hint = c.tabs.group(gid)?.name)
    if (c.tabs.split == null) Row50(CornIcons.Split, "Split screen", { c.sheet = Sheet.SplitPick })
    else Row50(CornIcons.Split, "Close split screen", { c.exitSplit() })
    Row50(CornIcons.Search, "Find in page", { c.openFind() }, enabled = cur?.isNewTab == false)
    Row50(CornIcons.Clock, "History", { c.openHistory() })
    Row50(CornIcons.Share, "Share page", { cur?.takeIf { !it.isNewTab }?.let { c.share(it.url) } ?: run { c.sheet = null } }, enabled = cur?.isNewTab == false)
    Row50(CornIcons.Sliders, "Settings", { c.sheet = null; c.screen = app.cornbrowser.Screen.Settings })
}

/**
 * Vault → New collection. A sheet with its own Create button (the old tile turned into a text box
 * with no button, and empty or repeated names only said "Pick a different name").
 */
@Composable
private fun NewCollectionSheet(c: CornController) {
    var name by remember { mutableStateOf("") }
    val taken = remember(c.data.vaultVersion.intValue) { c.data.collections().map { it.name.trim().lowercase() }.toSet() + "all" }
    val trimmed = name.trim()
    val duplicate = trimmed.lowercase() in taken
    val ok = trimmed.isNotEmpty() && !duplicate
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    SheetTitle("New collection", "Group saved links, like “Recipes” or “Read later”.")
    Field(name, { name = it.take(40) }, "Name", onDone = { if (ok) c.createCollection(trimmed) }, focus = focus)
    T(
        if (duplicate) "You already have a collection called “$trimmed”." else " ",
        12.5.sp, C.Accent, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
    )
    BigButton("Create", { c.createCollection(trimmed) }, icon = CornIcons.Folder, enabled = ok)
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

/** Long-press on an image: image actions, plus the link's when the image is a link. */
@Composable
private fun ImageSheet(c: CornController, s: Sheet.Image) {
    // Only real pictures reach this sheet (ImageRules.isPicture): every image action works.
    SheetTitle(s.title ?: "Image", s.linkUrl ?: s.imageUrl)
    if (s.linkUrl != null) LinkRows(c, s.linkUrl, s.title ?: s.linkUrl)
    Row50(CornIcons.Download, "Save image", { c.saveImage(s.imageUrl) })
    Row50(CornIcons.TabNew, "Open image in new tab", { c.openLinkInNewTab(s.imageUrl) })
    Row50(CornIcons.Share, "Share image link", { c.share(s.imageUrl) })
    if (s.linkUrl == null) Row50(CornIcons.Copy, "Copy image link", { c.copyLink(s.imageUrl) })
}

/** A tapped link to another app (phone, email, SMS, map, an app): Corn asks before leaving. */
@Composable
private fun ExternalAppSheet(c: CornController, s: Sheet.ExternalApp) {
    SheetTitle(s.title, s.detail)
    BigButton(if (s.appName != null) "Open ${s.appName}" else "Open", { c.openExternalApp(s) }, icon = CornIcons.OpenOutside)
    Spacer(Modifier.height(10.dp))
    if (s.fallbackUrl != null) {
        BigButton("Open web version", { c.openExternalFallback(s) }, ghost = true)
        Spacer(Modifier.height(10.dp))
    }
    BigButton("Cancel", { c.sheet = null }, ghost = true)
    T("The other app sees this link. Nothing else from Corn is shared.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

/** Menu → Split screen: the open tab stays in the first pane; pick what goes in the second. */
@Composable
private fun ColumnScope.SplitPickSheet(c: CornController) {
    val cur = c.tabs.current
    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    val others = c.tabs.tabs.filter { it.id != cur?.id }
    SheetScroll {
    SheetTitle(
        "Split screen",
        if (landscape) "This tab stays on the left. Tap a tab to show it on the right." else "This tab stays on top. Tap a tab to show it below.",
    )
    // The same tab cards as the group sheet, two per row; tap one to put it in the second pane.
    if (others.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            others.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t -> TabPreview(t, false, { c.startSplit(t.id) }, null, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
    }
    // Pinned: "New tab" stays visible with many tabs.
    BigButton("New tab", { c.startSplit(null) }, icon = CornIcons.Plus, ghost = others.isNotEmpty())
}

@Composable
private fun EngineSheet(c: CornController) {
    SheetTitle("Search with", "Just for your next searches. Change the default in Settings.")
    SearchEngines.all.forEach { e ->
        SettingRow(
            e.name,
            onClick = { c.engineNow = e; c.sheet = null; c.showToast("Searching with ${e.name}") },
            leading = { EngineBadge(e) },
            trailing = { if (c.engineNow.id == e.id) CornIcon(CornIcons.Check, tint = C.Accent) },
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Corners.Medium)).clickable { advanced = !advanced }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T("Advanced", 15.sp, C.Accent, FontWeight.SemiBold, modifier = Modifier.weight(1f))
        CornIcon(if (advanced) CornIcons.ChevUp else CornIcons.ChevDown, 18.dp, C.Accent)
    }
    AnimatedVisibility(advanced) {
        Column {
            SettingRow("Ads", "Ad networks and ad scripts (EasyList, uBlock Origin)", onClick = { c.setShields(host, sh.copy(ads = !sh.ads)) }, trailing = { CornSwitch(sh.ads) })
            SettingRow("Trackers", "Analytics and tracking scripts (EasyPrivacy)", onClick = { c.setShields(host, sh.copy(trackers = !sh.trackers)) }, trailing = { CornSwitch(sh.trackers) })
            SettingRow("Pop-ups & redirects", "New windows and pages jumping to other sites", onClick = { c.setShields(host, sh.copy(popups = !sh.popups)) }, trailing = { CornSwitch(sh.popups) })
        }
    }
    T("Only turn things off if the page breaks. Changes apply to this site only and reload the page.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))

    // This session's camera / microphone / location answers for the site (forgotten on Flush or restart).
    val answers = app.cornbrowser.browser.SitePermission.entries.mapNotNull { k -> c.sitePermissions[host to k]?.let { k to it } }
    if (answers.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        answers.forEach { (kind, allowed) ->
            SettingRow(
                kind.label.replaceFirstChar { it.uppercase() },
                if (allowed) "Allowed for this session · tap to ask again" else "Blocked for this session · tap to ask again",
                onClick = { c.forgetSitePermission(host, kind) },
            )
        }
    }

    // Keep List, one tap from the page (also editable in Settings → Keep List).
    val kept = remember(c.data.vaultVersion.intValue, host) { host in c.data.keepList() }
    Spacer(Modifier.height(14.dp))
    SettingRow(
        "Stay logged in after Flush",
        "Keeps this site's login (cookies and saved site data). Adds it to your Keep List.",
        onClick = {
            if (kept) c.data.removeKeep(host) else c.data.addKeep(host)
            c.showToast(if (kept) "$host removed from your Keep List" else "$host stays logged in after Flush")
        },
        trailing = { CornSwitch(kept) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.SaveSheet(c: CornController, s: Sheet.Save) {
    val collections = remember(c.data.vaultVersion.intValue) { c.data.collections() }
    var title by remember(s) { mutableStateOf(s.title) }
    // No Collection by default: the link is saved once and always shows in All.
    var chosen by remember(s) { mutableStateOf<Long?>(null) }
    var naming by remember(s) { mutableStateOf(false) }
    var newName by remember(s) { mutableStateOf("") }
    val tags = remember(s) { mutableStateListOf<String>() }
    var tagInput by remember(s) { mutableStateOf("") }

    SheetScroll {
    SheetTitle("Save to Vault", s.url)
    Label("Title")
    Field(title, { title = it }, "Title")
    Label("Collection (optional)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("None", chosen == null && !naming, { chosen = null; naming = false })
        collections.forEach { col -> Chip(col.name, col.id == chosen, { chosen = if (chosen == col.id) null else col.id }) }
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
    }
    // Pinned: Save stays visible with many Collections or tags.
    Spacer(Modifier.height(14.dp))
    BigButton("Save", {
        // A new Collection typed but not confirmed with the keyboard's Done key still counts.
        val typed = newName.trim()
        val col = (if (naming && typed.isNotEmpty()) c.data.collectionNamed(typed) else null)
            ?: chosen?.let { id -> c.data.collections().firstOrNull { it.id == id } }
        // Same for a tag still in the box.
        val t = tagInput.trim().lowercase()
        if (t.isNotEmpty() && t !in tags) tags += t
        c.save(col?.id, col?.name, title, s.url, tags.toList())
    }, icon = CornIcons.VaultAdd)
    T("Every saved link shows in All. You can save while the Vault is locked; seeing what's inside needs your PIN.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ColumnScope.GroupSheet(c: CornController, groupId: Int) {
    val g = c.tabs.group(groupId) ?: run { c.sheet = null; return }
    val members = c.tabs.groupTabs(groupId)
    SheetScroll {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(g.color).clickable { c.cycleGroupColor(groupId) })
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            g.name, { g.name = it }, singleLine = true,
            textStyle = TextStyle(color = C.Text, fontSize = 20.sp, fontFamily = Display, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(C.Accent),
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
    }
    // Pinned: always visible however many tabs the group has.
    Spacer(Modifier.height(14.dp))
    BigButton("Save group to Vault", { c.saveGroup(groupId) }, icon = CornIcons.VaultAdd)
    Spacer(Modifier.height(10.dp))
    BigButton("New tab in group", { c.newTabInGroup(groupId) }, icon = CornIcons.Plus, ghost = true)
    T("Flush wipes every group. Save it first if you want these links later.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ConfirmFlushSheet(c: CornController) {
    SheetTitle("Flush everything?", "All tabs and groups, cookies, cache and history go. Your Vault and Keep List stay.")
    BigButton("Flush", { c.flushNow(animate = true) }, icon = CornIcons.Flush)
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

/** Vault → ⋮. */
@Composable
private fun VaultMenuSheet(c: CornController) {
    Row50(CornIcons.Export, "Export links", { c.sheet = Sheet.ExportLinks })
}

/**
 * Vault → ⋮ → Export links: pick a format (radio options, HTML pre-selected), then Export opens
 * Android's "Save to…" (docs/02-technical/13-vault-export.md). Tapping an option only selects it.
 */
@Composable
private fun ExportLinksSheet(c: CornController) {
    val total = remember { c.data.itemCount() }
    var format by remember { mutableStateOf(VaultExport.LinksFormat.HTML) }
    SheetTitle("Export links", "Every saved link ($total), with its Collection, tags and date. You choose where the file goes.")
    RadioOption(
        "Bookmarks file (HTML)", "Recommended. Chrome, Firefox, Safari, Edge and Brave import it. Collections become folders.",
        selected = format == VaultExport.LinksFormat.HTML,
    ) { format = VaultExport.LinksFormat.HTML }
    RadioOption("Spreadsheet (CSV)", "For spreadsheets and apps that take CSV.", selected = format == VaultExport.LinksFormat.CSV) {
        format = VaultExport.LinksFormat.CSV
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton("Cancel", { c.sheet = null }, ghost = true, modifier = Modifier.weight(1f))
        BigButton("Export", { c.exportLinks(format) }, icon = CornIcons.Export, modifier = Modifier.weight(1f))
    }
}

/** One choice of a single-choice list: a ring that fills when selected, a title and a line under it. */
@Composable
private fun RadioOption(title: String, sub: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Corners.Medium))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) C.Corn else C.Muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(C.Corn)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            T(title, 16.sp, weight = FontWeight.SemiBold)
            T(sub, 13.sp, C.Muted)
        }
    }
}

@Composable
private fun DeleteItemSheet(c: CornController, s: Sheet.DeleteItem) {
    SheetTitle("Delete this link?", "“${s.title}” will be removed from your Vault for good.")
    BigButton("Delete", { c.deleteVaultItem(s.id) })
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

@Composable
private fun HistoryEntrySheet(c: CornController, s: Sheet.HistoryEntry) {
    val v = s.visit
    SheetTitle(v.title.ifBlank { v.host }, v.url)
    Row50(CornIcons.ChevRight, "Open", { c.sheet = null; c.openFromHistory(v.url) })
    Row50(CornIcons.TabNew, "Open in new tab", { c.sheet = null; c.screen = app.cornbrowser.Screen.Browser; c.tabs.open(v.url) })
    Row50(CornIcons.Copy, "Copy link", { c.copyLink(v.url) })
    Row50(CornIcons.Trash, "Delete from history", { c.data.deleteVisit(v.visitId); c.sheet = null; c.showToast("Deleted from history") })
    Row50(CornIcons.Trash, "Delete all of ${v.host} from history", {
        c.data.deleteHistoryForHost(v.host)
        c.sheet = null
        c.showToast("Deleted ${v.host} from history. Cookies stay.")
    })
    T("This deletes history only. Cookies and site data go with Flush or Delete browsing data.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun DeleteDataSheet(c: CornController) {
    var history by remember { mutableStateOf(true) }
    var cookies by remember { mutableStateOf(true) }
    var cache by remember { mutableStateOf(true) }
    var tabs by remember { mutableStateOf(false) }
    val keep = remember { c.data.keepList() }
    SheetTitle("Delete browsing data", "Everything in each ticked box, from the start. Your Vault and settings stay.")
    SettingRow("History", "Every page in History", onClick = { history = !history }, trailing = { CornSwitch(history) })
    SettingRow(
        "Cookies and site data",
        if (keep.isEmpty()) "Logs you out of sites" else "Logs you out of sites, except the ${keep.size} on your Keep List",
        onClick = { cookies = !cookies }, trailing = { CornSwitch(cookies) },
    )
    SettingRow("Cached images and files", "Pages may load slower the first time", onClick = { cache = !cache }, trailing = { CornSwitch(cache) })
    SettingRow("Open tabs and groups", "Closes every tab", onClick = { tabs = !tabs }, trailing = { CornSwitch(tabs) })
    Spacer(Modifier.height(18.dp))
    val any = history || cookies || cache || tabs
    BigButton("Delete", { if (any) c.deleteBrowsingData(ClearChoice(history, cookies, cache, tabs)) }, icon = CornIcons.Trash)
    if (cookies) {
        T("Corn restarts for a moment after deleting cookies, so nothing is left in memory.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun ConfirmClearHistorySheet(c: CornController) {
    SheetTitle("Clear all history?", "Every page in History goes. Tabs, cookies and your Vault stay.")
    BigButton("Clear history", { c.data.clearHistory(); c.sheet = null; c.showToast("History cleared. Cookies and site data stay.") }, icon = CornIcons.Trash)
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

@Composable
private fun ConfirmKeepAllTabsSheet(c: CornController) {
    SheetTitle("Keep all tabs in memory?", "Not recommended. Every open tab keeps running in the background:")
    T(
        "• More battery drain and heat.\n" +
            "• Android may close Corn to free memory, and you'd lose what wasn't saved.\n" +
            "• All tabs share one page process: if one page crashes, every tab reloads.",
        14.sp, C.Text, modifier = Modifier.padding(bottom = 18.dp),
    )
    BigButton("Keep all tabs", { c.settings.chooseTabMemory(TabMemory.ALL); c.tabs.enforceLivePolicy(); c.sheet = null })
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
}

@Composable
private fun SitePermissionSheet(c: CornController, s: Sheet.SitePermissionAsk) {
    val what = s.kinds.map { it.label }.let { if (it.size > 1) it.dropLast(1).joinToString(", ") + " and " + it.last() else it.first() }
    SheetTitle("${s.site} wants to use your $what", "Corn remembers your answer only until the next Flush.")
    BigButton("Allow", { c.answerSitePermission(s.site, s.kinds, true) })
    Spacer(Modifier.height(10.dp))
    BigButton("Block", { c.answerSitePermission(s.site, s.kinds, false) }, ghost = true)
}

@Composable
private fun DownloadSheet(c: CornController, s: Sheet.Download) {
    val size = s.request.length.takeIf { it > 0 }?.let { android.text.format.Formatter.formatShortFileSize(LocalContext.current, it) }
    val from = android.net.Uri.parse(s.request.url).host?.removePrefix("www.") ?: ""
    SheetTitle("Download this file?", listOfNotNull(s.fileName, size, from.takeIf { it.isNotBlank() }?.let { "from $it" }).joinToString(" · "))
    BigButton("Download", { c.startDownload(s.request, s.fileName) }, icon = CornIcons.Download)
    Spacer(Modifier.height(10.dp))
    BigButton("Cancel", { c.sheet = null }, ghost = true)
    T("Saved to your phone's Downloads folder. Files stay there after a Flush; delete them in your Files app.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 10.dp))
}

/** A search engine's favicon (bundled with the app, never fetched). */
@Composable
fun EngineBadge(engine: SearchEngine, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Image(painterResource(engine.icon), engine.name, Modifier.size(size).clip(RoundedCornerShape(size * 0.25f)))
}
