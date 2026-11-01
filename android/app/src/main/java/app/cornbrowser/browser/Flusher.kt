/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import app.cornbrowser.data.CornData
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wipes the Session: tabs, Tab Groups, cookies (except Keep List sites), site storage,
 * cache, form data and the saved Kept Session. The Vault, settings and Keep List survive.
 * Clears this device only; see docs/02-technical/05-flush-model.md.
 */
object Flusher {
    fun flush(context: Context, tabs: TabManager, data: CornData, done: () -> Unit) {
        val cm = CookieManager.getInstance()

        // 1. Remember Keep List sites' cookies (name=value pairs) so they stay logged in.
        val kept = data.keepList().associateWith { host ->
            listOf("https://$host", "https://www.$host").mapNotNull { cm.getCookie(it) }.joinToString("; ")
        }

        // 2. Close every tab.
        tabs.reset()

        // 3. Clear web data.
        WebStorage.getInstance().deleteAllData()
        GeolocationPermissions.getInstance().clearAll()
        WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
        WebView(context).apply { clearCache(true); clearFormData(); destroy() }
        data.clearSession()

        cm.removeAllCookies {
            // 4. Put Keep List cookies back. Attributes (expiry, HttpOnly) aren't readable through
            //    CookieManager, so they come back as secure, long-lived cookies for that site.
            for ((host, cookies) in kept) {
                cookies.split(";").map { it.trim() }.filter { it.contains('=') }.distinct().forEach { c ->
                    cm.setCookie("https://$host", "$c; Domain=$host; Path=/; Secure; Max-Age=34560000; SameSite=Lax")
                }
            }
            cm.flush()
            done()
        }
    }

    /** A short buzz: tap, pause, longer swirl. */
    fun vibrate(context: Context) {
        val v: Vibrator? = if (android.os.Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching { v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 70), -1)) }
    }

    /** A synthesised whoosh that falls in pitch, like water draining (only if the user turned the sound on). */
    fun playSound() {
        Thread {
            runCatching {
                val rate = 22050
                val n = (rate * 0.9).toInt()
                val pcm = ShortArray(n)
                var lp = 0.0
                for (i in 0 until n) {
                    val t = i.toDouble() / n
                    val cutoff = 0.25 * (1 - t) + 0.02            // low-pass sweep: bright → dull
                    lp += cutoff * ((Random.nextDouble() * 2 - 1) - lp)
                    val wobble = 1 + 0.3 * sin(2 * PI * 9 * t)  // swirl
                    val env = sin(PI * t)                        // fade in and out
                    pcm[i] = (lp * env * wobble * 26000).toInt().coerceIn(-32767, 32767).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(n * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, n)
                track.play()
                Thread.sleep(1000)
                track.release()
            }
        }.start()
    }

    val QUIPS = listOf(
        "All gone. Fresh as a new cob. 🌽",
        "Flushed. Nobody saw nothing.",
        "Squeaky clean. Like it never happened.",
        "Session down the drain. Your Vault's safe.",
        "Poof. Clean slate, same great corn.",
    )
}
