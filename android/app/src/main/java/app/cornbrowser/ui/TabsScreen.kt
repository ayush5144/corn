/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.CornController
import app.cornbrowser.Sheet

/** All tabs: Tab Groups as rows of previews, then the other tabs in a grid. */
@Composable
fun TabsScreen(c: CornController) {
    val loose = c.tabs.tabs.filter { it.groupId == null }
    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T(c.tabs.tabs.size.let { if (it == 1) "1 tab" else "$it tabs" }, 24.sp, weight = FontWeight.ExtraBold, display = true, modifier = Modifier.weight(1f))
            Pill("New", { c.newTab() }, CornIcons.Plus)
            Pill("Done", { c.screen = app.cornbrowser.Screen.Browser }, corn = true)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
            c.tabs.groups.forEach { g ->
                val members = c.tabs.groupTabs(g.id)
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(C.Surface).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Box(Modifier.size(16.dp).clip(CircleShape).background(g.color))
                        Spacer(Modifier.width(10.dp))
                        T(g.name, 16.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        T(if (members.size == 1) "1 tab" else "${members.size} tabs", 13.sp, C.Muted)
                        IconBtn(CornIcons.ChevRight, "Open group", { c.sheet = Sheet.Group(g.id) }, size = 34.dp, iconSize = 18.dp)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        members.forEach { t ->
                            TabPreview(t, c.tabs.isOnScreen(t.id), { c.switchTab(t.id) }, { c.closeTab(t.id) }, Modifier.width(112.dp))
                        }
                    }
                }
            }
            if (loose.isNotEmpty()) {
                Label(if (c.tabs.groups.isEmpty()) "Tabs" else "Other tabs")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    loose.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { t -> TabPreview(t, c.tabs.isOnScreen(t.id), { c.switchTab(t.id) }, { c.closeTab(t.id) }, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        // Pinned at the bottom: the Flush button sits where it does in the browser toolbar (bottom right),
        // so it's always reachable however many tabs there are.
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
        Row(
            Modifier.fillMaxWidth().background(C.Bg).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                T("Done with all of these?", 14.sp, weight = FontWeight.SemiBold)
                T("Flush wipes every tab and group. Your Vault stays.", 12.5.sp, C.Muted, maxLines = 2)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(56.dp).height(44.dp).clip(RoundedCornerShape(14.dp)).background(C.Corn).clickable { c.requestFlush() },
                contentAlignment = Alignment.Center,
            ) { CornIcon(CornIcons.Flush, 24.dp, C.CornInk) }
        }
    }
}
