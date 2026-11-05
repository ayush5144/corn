/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import uniffi.corn_ffi.CornBlocker

/**
 * Runs the ads engine's scriptlets (`+js(...)` rules from the downloaded lists) in one tab, at
 * document start, before any of the page's own scripts. Nothing here knows about any site: the
 * lists decide which scriptlets run where.
 *
 * How: a page's scriptlets depend only on its host, so each host gets its own
 * `addDocumentStartJavaScript` registration, limited to that origin. It must exist before the
 * page's document is created, so [prepare] is called as each main-frame navigation starts:
 * - from `shouldInterceptRequest` (WebView's network thread) for every main-frame request, which
 *   covers typed addresses, links, reloads, back/forward and restored tabs. That thread waits
 *   (at most [WAIT_MS]) while the main thread registers, so the script is in place before the
 *   response arrives;
 * - from `shouldOverrideUrlLoading` for server redirects (e.g. youtube.com → m.youtube.com), which
 *   don't pass through `shouldInterceptRequest`.
 * Why not other ways: one static script that asks Corn for its scriptlets would have to `eval`
 * them, which sites with Trusted Types (YouTube sends `require-trusted-types-for 'script'`)
 * block; `evaluateJavascript` at page start runs too late to catch the page's first scripts.
 *
 * The per-site Ads switch is respected: with Ads off, the host's registration is removed before
 * the next page load (including a reload). A list refresh replaces registrations as hosts are
 * visited again. Registrations live as long as the tab's WebView.
 */
internal class ScriptletInjector(
    private val webView: WebView,
    private val adsOn: (host: String) -> Boolean,
) {
    /** What's registered for one origin. [engine] = the ads engine it was computed from. */
    private class Registered(val engine: CornBlocker?, val adsOn: Boolean, val handler: ScriptHandler?)

    private val byOrigin = ConcurrentHashMap<String, Registered>()
    private val main = Handler(Looper.getMainLooper())
    private val supported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    /** Any thread. Make sure the page at [url] will start with its scriptlets (or none, with Ads off). */
    fun prepare(url: String) {
        if (!supported) return
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return
        if (scheme != "https" && scheme != "http") return
        val host = uri.host ?: return
        val origin = if (uri.port != -1) "$scheme://$host:${uri.port}" else "$scheme://$host"
        val engine = CornEngine.ads
        val on = adsOn(host.removePrefix("www."))
        if (upToDate(origin, engine, on)) return

        // Work out the script here (off the main thread when called from the network thread).
        val script = if (on && engine != null) engine.scriptlets(url) else ""
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply(origin, engine, on, script)
        } else {
            val done = CountDownLatch(1)
            main.post { try { apply(origin, engine, on, script) } finally { done.countDown() } }
            done.await(WAIT_MS, TimeUnit.MILLISECONDS) // if the main thread is busy, the page may start without them
        }
    }

    private fun upToDate(origin: String, engine: CornBlocker?, on: Boolean): Boolean {
        val r = byOrigin[origin] ?: return false
        return r.adsOn == on && (!on || r.engine === engine)
    }

    /** Main thread only. */
    private fun apply(origin: String, engine: CornBlocker?, on: Boolean, script: String) {
        if (upToDate(origin, engine, on)) return // another call got here first
        byOrigin.remove(origin)?.handler?.remove()
        val handler = if (script.isEmpty()) null else runCatching {
            WebViewCompat.addDocumentStartJavaScript(webView, script, setOf(origin))
        }.getOrNull()
        byOrigin[origin] = Registered(engine, on, handler)
    }

    private companion object {
        const val WAIT_MS = 300L
    }
}
