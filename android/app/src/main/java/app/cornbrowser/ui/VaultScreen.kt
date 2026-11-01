/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.CornController
import app.cornbrowser.Screen
import app.cornbrowser.data.CornSettings
import uniffi.corn_ffi.Collection
import uniffi.corn_ffi.VaultItem

@Composable
fun VaultScreen(c: CornController) {
    var openCollection by remember { mutableStateOf<Long?>(null) }
    val locked = c.settings.vaultLock && !c.vaultUnlocked

    if (locked) {
        Column(Modifier.fillMaxSize().background(C.Bg)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { IconBtn(CornIcons.Back, "Back", { c.screen = Screen.Browser }) }
            PinGate(
                settings = c.settings,
                lockedTitle = "Vault is locked",
                lockedSub = "Enter your PIN",
                createSub = "Choose a 4-digit PIN for your Vault",
                onSuccess = { c.vaultUnlocked = true },
            )
        }
        return
    }

    val col = openCollection
    if (col != null) {
        CollectionView(c, col, onBack = { openCollection = null })
        return
    }

    var query by remember { mutableStateOf("") }
    var naming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val version = c.data.vaultVersion.intValue
    val collections = remember(version) { c.data.collections() }
    val total = remember(version) { c.data.itemCount() }
    val hits = remember(query, version) { if (query.isBlank()) emptyList() else c.data.search(query) }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(CornIcons.Back, "Back to browser", { c.screen = Screen.Browser })
            T("Vault", 24.sp, weight = FontWeight.ExtraBold, display = true, modifier = Modifier.weight(1f))
            if (c.settings.vaultLock) Pill("Lock", { c.vaultUnlocked = false }, CornIcons.Lock)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            Field(query, { query = it }, "Search $total saved links", leading = { CornIcon(CornIcons.Search, 18.dp, C.Muted) })
            Spacer(Modifier.height(16.dp))
            if (query.isNotBlank()) {
                if (hits.isEmpty()) Empty("No saved links match “$query”.") else hits.forEach { ItemRow(c, it) }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // "All" (every saved link, not a stored Collection), the Collections, then the
                    // "New collection" tile (null), two per row. Tiles in a row share one height.
                    val all = Collection(ALL, "All", total)
                    val tiles: List<Collection?> = listOf(all) + collections + listOf(null)
                    tiles.chunked(2).forEach { row ->
                        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { col ->
                                if (col != null) {
                                    CollectionTile(col, Modifier.weight(1f).fillMaxHeight()) { openCollection = col.id }
                                } else {
                                    NewCollectionTile(naming, newName, { newName = it }, { naming = true }, Modifier.weight(1f).fillMaxHeight()) {
                                        c.data.createCollection(newName)?.let { created -> openCollection = created.id }
                                            ?: c.showToast("Pick a different name")
                                        naming = false
                                        newName = ""
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                T("Everything in here stays on this phone and survives every Flush.", 12.5.sp, C.Muted, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

/** Stands for "All" in the grid: every saved link. Never a real Collection id (SQLite ids start at 1). */
private const val ALL = -1L

@Composable
private fun CollectionTile(col: Collection, modifier: Modifier, onOpen: () -> Unit) {
    Column(
        modifier.heightIn(min = 108.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface).clickable(onClick = onOpen).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(C.Surface2), contentAlignment = Alignment.Center) {
            CornIcon(if (col.id == ALL) CornIcons.Vault else CornIcons.Folder, 20.dp, C.Accent)
        }
        Column {
            T(col.name, 16.sp, weight = FontWeight.SemiBold, maxLines = 1)
            T("${col.itemCount} ${if (col.itemCount == 1L) "link" else "links"}", 13.sp, C.Muted)
        }
    }
}

@Composable
private fun NewCollectionTile(naming: Boolean, name: String, onName: (String) -> Unit, onStart: () -> Unit, modifier: Modifier, onDone: () -> Unit) {
    Column(
        modifier.heightIn(min = 108.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, C.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onStart).padding(14.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (naming) {
            Field(name, onName, "Name it", height = 40.dp, onDone = onDone)
        } else {
            CornIcon(CornIcons.Plus, 22.dp, C.Muted)
            Spacer(Modifier.height(6.dp))
            T("New collection", 14.sp, C.Muted)
        }
    }
}

@Composable
private fun CollectionView(c: CornController, id: Long, onBack: () -> Unit) {
    val version = c.data.vaultVersion.intValue
    val isAll = id == ALL
    val name = remember(version) { if (isAll) "All" else c.data.collections().firstOrNull { it.id == id }?.name ?: "" }
    val items = remember(version) { c.data.items(if (isAll) null else id) }
    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(CornIcons.Back, "Back to collections", onBack)
            T(name, 24.sp, weight = FontWeight.ExtraBold, display = true, maxLines = 1, modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (items.isEmpty()) Empty("Nothing here yet. Tap the Vault icon in the address bar on any page to save it here.")
            else {
                items.forEach { ItemRow(c, it) }
                T("Long-press a link to delete it.", 12.5.sp, C.Muted, modifier = Modifier.padding(vertical = 14.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(c: CornController, it: VaultItem) {
    val host = Uri.parse(it.url).host?.removePrefix("www.") ?: it.url
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = { c.openInNewTab(it.url) }, onLongClick = { c.sheet = app.cornbrowser.Sheet.DeleteItem(it.id, it.title) })
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LetterIcon(host, 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            T(it.title, 15.sp, weight = FontWeight.SemiBold, maxLines = 2)
            T(host, 12.5.sp, C.Muted, maxLines = 1)
            if (it.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    it.tags.forEach { tag ->
                        Row(
                            Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface2).padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CornIcon(CornIcons.Tag, 11.dp, C.Muted)
                            Spacer(Modifier.width(4.dp))
                            T(tag, 12.sp, C.Muted)
                        }
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

@Composable
private fun Empty(text: String) = T(text, 15.sp, C.Muted, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 10.dp))

/**
 * PIN screen used by the Vault and App Lock. With no PIN yet, it walks through
 * choosing one (enter, confirm); afterwards it checks the PIN. (Fingerprint unlock is in the backlog.)
 */
@Composable
fun PinGate(
    settings: CornSettings,
    lockedTitle: String,
    lockedSub: String,
    createSub: String,
    onSuccess: () -> Unit,
) {
    val creating = !settings.hasPin
    var digits by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when {
        !creating -> lockedTitle
        first == null -> "Create a PIN"
        else -> "Confirm your PIN"
    }
    val sub = error ?: when {
        !creating -> lockedSub
        first == null -> createSub
        else -> "Enter the same 4 digits again"
    }

    fun complete(pin: String) {
        if (!creating) {
            if (settings.checkPin(pin)) onSuccess() else { error = "Wrong PIN. Try again."; digits = "" }
            return
        }
        val f = first
        if (f == null) { first = pin; digits = ""; error = null }
        else if (f == pin) { settings.setPin(pin); onSuccess() }
        else { first = null; digits = ""; error = "PINs didn't match. Choose a PIN again." }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(C.Surface2), contentAlignment = Alignment.Center) {
            CornIcon(CornIcons.Lock, 34.dp, C.Accent)
        }
        Spacer(Modifier.height(18.dp))
        T(title, 24.sp, weight = FontWeight.ExtraBold, display = true)
        Spacer(Modifier.height(6.dp))
        T(sub, 14.sp, if (error != null) C.Accent else C.Muted)
        Spacer(Modifier.height(22.dp))
        PinPad(
            entered = digits.length,
            onDigit = { d -> if (digits.length < 4) { digits += d; error = null; if (digits.length == 4) complete(digits) } },
            onBackspace = { digits = digits.dropLast(1) },
            onFingerprint = null,
        )
    }
}
