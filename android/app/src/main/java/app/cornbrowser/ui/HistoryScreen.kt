/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.cornbrowser.Sheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import uniffi.corn_ffi.HistoryVisit

/**
 * History: every page visited, newest first, grouped by day. Stored only on this phone and
 * cleared by Flush. Tap to open, × to delete one visit, long-press for more.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(c: CornController) {
    var query by remember { mutableStateOf("") }
    val visits = remember(c.data.historyVersion.intValue, query) { c.data.history(query) }
    val days = remember(visits) { visits.groupBy { dayLabel(it.visitedAt) } }
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(CornIcons.Back, "Back to browser", { c.screen = Screen.Browser })
            T("History", 24.sp, weight = FontWeight.ExtraBold, display = true, modifier = Modifier.weight(1f))
            if (visits.isNotEmpty() && query.isBlank()) {
                IconBtn(CornIcons.Trash, "Clear history", { c.sheet = Sheet.ConfirmClearHistory }, tint = C.Muted)
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Field(query, { query = it }, "Search history", leading = { CornIcon(CornIcons.Search, 18.dp, C.Muted) })
        }
        // Same sheet as Settings → Delete browsing data: cookies, cache and tabs live there, not in History.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = { c.sheet = Sheet.DeleteData }).padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CornIcon(CornIcons.Trash, 20.dp, C.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                T("Delete browsing data", 15.sp, C.Accent, FontWeight.SemiBold)
                T("Cookies, cache and tabs too", 12.5.sp, C.Muted)
            }
            CornIcon(CornIcons.ChevRight, 18.dp, C.Muted)
        }
        if (!c.settings.keepHistory) {
            T("History is off, so Corn isn't recording new pages. Turn it on in Settings → History and data.", 13.sp, C.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        if (visits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                T(if (query.isBlank()) "Nothing here. Fresh as a new cob. 🌽" else "No matches", 15.sp, C.Muted)
            }
            return
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            days.forEach { (day, list) ->
                item(key = "day-$day") { Label(day) }
                items(list, key = { it.visitId }) { v ->
                    HistoryRow(
                        v, time.format(Date(v.visitedAt)),
                        onOpen = { c.openFromHistory(v.url) },
                        onMore = { c.sheet = Sheet.HistoryEntry(v) },
                        onDelete = { c.data.deleteVisit(v.visitId); c.showToast("Removed from history") },
                    )
                }
            }
            item { Spacer(Modifier.heightIn(min = 24.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(v: HistoryVisit, time: String, onOpen: () -> Unit, onMore: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onMore).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterIcon(v.host, 28.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T(v.title.ifBlank { v.host }, 15.sp, maxLines = 1)
            T("$time · ${v.host}", 12.5.sp, C.Muted, maxLines = 1)
        }
        IconBtn(CornIcons.X, "Delete from history", onDelete, tint = C.Muted, size = 38.dp, iconSize = 16.dp)
    }
}

private fun dayLabel(ms: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = ms }
    val today = Calendar.getInstance()
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (same(day, today)) return "Today"
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (same(day, today)) return "Yesterday"
    return SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(ms))
}
