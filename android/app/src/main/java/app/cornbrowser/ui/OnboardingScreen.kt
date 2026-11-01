/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cornbrowser.R

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val bob = rememberInfiniteTransition(label = "bob")
    val lift by bob.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "lift")

    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2A2410), C.Bg), radius = 1400f, center = androidx.compose.ui.geometry.Offset(540f, 0f)))
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Image(
            painterResource(R.drawable.corn_logo), "Corn logo",
            Modifier.size(112.dp).offset(x = (-8).dp, y = (-6 * lift).dp).rotate(-4f * lift),
        )
        Spacer(Modifier.height(14.dp))
        T("corn", 64.sp, C.Corn, FontWeight.ExtraBold, display = true)
        Spacer(Modifier.height(10.dp))
        T("The private browser that keeps you organised.", 24.sp, weight = FontWeight.SemiBold, display = true)
        Spacer(Modifier.height(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Bullet(CornIcons.Shield, "Blocks ads, trackers and pop-ups")
            Bullet(CornIcons.Vault, "Saves what matters to a locked Vault")
            Bullet(CornIcons.Toilet, "Flushes everything else in one tap")
        }
        Spacer(Modifier.height(48.dp))
        BigButton("I'm 18 or older · Start browsing", onStart)
        T(
            "Nothing you browse leaves this phone. No account needed.", 13.sp, C.Muted,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface2), contentAlignment = Alignment.Center) {
            CornIcon(icon, 22.dp, C.Corn)
        }
        Spacer(Modifier.width(12.dp))
        T(text, 15.5.sp)
    }
}
