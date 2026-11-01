/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import app.cornbrowser.BuildConfig
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject
import uniffi.corn_ffi.Decision

enum class BlockKind { AD, TRACKER, POPUP }

/** What a live tab needs from the rest of the app. Called on the main thread unless noted. */
interface TabHost {
    /** What to block on a site. Called from WebView's network threads. */
    fun shields(host: String): app.cornbrowser.data.Shields
    /** Called from network threads for ads/trackers. */
    fun onBlocked(kind: BlockKind)
    fun onBlockedPopup()
    /** The site is allowed pop-ups: open this one as a new tab. Returns true if handled. */
    fun onPopupAllowed(opener: TabEntry, resultMsg: Message): Boolean
    fun onBlockedRedirect(target: String)
    fun onLinkLongPress(entry: TabEntry, url: String, title: String)
    fun onMediaChanged()
    fun onEnterFullscreen(view: View, exit: () -> Unit)
    fun onExitFullscreen()
    /** The page process died (crash or the system reclaimed memory). */
    fun onPageCrashed(entry: TabEntry)
    /** Settings → Privacy → Close cookie banners. Read when a tab is created. */
    val cookieBannersEnabled: Boolean
    /** Settings → Privacy → Safe Browsing. */
    val safeBrowsingEnabled: Boolean
}

/**
 * The live part of a tab: a WebView wired to Corn's blocker.
 * A tab that isn't live (frozen) has no CornTab, only its TabEntry with a preview and saved state.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class CornTab(context: Context, private val entry: TabEntry, private val host: TabHost) {
    val webView = WebView(context)

    private val adsOnPage = AtomicInteger(0)
    private val trackersOnPage = AtomicInteger(0)
    private val guardedOnPage = AtomicInteger(0)
    private val syncPending = AtomicBoolean(false)
    @Volatile private var pageUrl: String = entry.url
    @Volatile private var exceptions: List<String> = emptyList()
    @Volatile private var generichide: Boolean = false
    private var lastUserGestureAt = 0L
    private val main = Handler(Looper.getMainLooper())

    init {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = true
        // Every pop-up attempt (tapped or scripted) reaches onCreateWindow, where Corn decides
        // per site and counts it. Left off, WebView would drop scripted pop-ups silently.
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.setGeolocationEnabled(false)
        s.userAgentString = BrowserIdentity.userAgent(s.userAgentString)
        setSafeBrowsing(host.safeBrowsingEnabled)
        webView.setBackgroundColor(0xFF0F0E0B.toInt())

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        installBridge()
        installCookieBannerCloser(context)
        webView.webViewClient = Client()
        webView.webChromeClient = Chrome()
        webView.setOnTouchListener { _, _ -> lastUserGestureAt = System.currentTimeMillis(); false }
        webView.setOnLongClickListener { onLongPress() }
    }

    fun load(url: String) = webView.loadUrl(prepare(url))

    /** Google Safe Browsing: warns before malware and phishing pages (hashed URL prefixes, not full URLs). */
    fun setSafeBrowsing(on: Boolean) {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.SAFE_BROWSING_ENABLE)) {
            androidx.webkit.WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, on)
        }
    }

    /** https versions Corn tried, mapped to the http original to fall back to if they fail. */
    private val upgraded = mutableMapOf<String, String>()

    /** Link cleaning (tracking parameters removed), then HTTPS upgrade unless this site is known to lack https. */
    private fun prepare(url: String): String {
        var u = uniffi.corn_ffi.cleanUrl(url)
        val h = Uri.parse(u).host
        if (h != null && h !in httpOnlyHosts) {
            uniffi.corn_ffi.httpsUpgrade(u)?.let { secure -> upgraded[secure] = u; u = secure }
        }
        return u
    }

    /** The secure version failed: remember the site has no https (for this session) and load http. */
    private fun fallBackToHttp(view: WebView, failedUrl: String): Boolean {
        val http = upgraded.remove(failedUrl) ?: return false
        Uri.parse(http).host?.let { httpOnlyHosts += it }
        view.loadUrl(http)
        return true
    }

    fun restore(state: Bundle) {
        webView.restoreState(state)
    }

    fun saveState(): Bundle = Bundle().also { webView.saveState(it) }

    /** Paused tabs stay in memory but stop scripts' heavy work and all media. */
    fun pause() {
        webView.evaluateJavascript("document.querySelectorAll('video,audio').forEach(function(m){m.pause()})", null)
        webView.onPause()
        entry.mediaPlaying = false
    }

    fun resume() = webView.onResume()

    /** A small screenshot for tab previews (memory only, never written to disk). */
    fun capturePreview(): Bitmap? {
        val w = webView.width
        val h = webView.height
        if (w <= 0 || h <= 0) return null
        val scale = 0.25f // ~0.25 MB per preview; memory only
        return runCatching {
            val bmp = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.RGB_565)
            val c = Canvas(bmp)
            c.scale(scale, scale)
            webView.draw(c)
            bmp
        }.getOrNull()
    }

    fun destroy() {
        webView.stopLoading()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    /** Copy counters and navigation state into the observable TabEntry, at most once per frame. */
    private fun sync() {
        if (syncPending.compareAndSet(false, true)) {
            main.post {
                syncPending.set(false)
                entry.blockedAds = adsOnPage.get()
                entry.blockedTrackers = trackersOnPage.get()
                entry.guarded = guardedOnPage.get()
                entry.canGoBack = webView.canGoBack()
                entry.canGoForward = webView.canGoForward()
            }
        }
    }

    private fun onLongPress(): Boolean {
        val hit = webView.hitTestResult
        when (hit.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                val url = hit.extra ?: return false
                requestLinkTitle { title -> host.onLinkLongPress(entry, url, title ?: url) }
                return true
            }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                requestLinkHref { url, title -> if (url != null) host.onLinkLongPress(entry, url, title ?: url) }
                return true
            }
        }
        return false
    }

    private fun requestLinkTitle(done: (String?) -> Unit) = requestLinkHref { _, title -> done(title) }

    private fun requestLinkHref(done: (url: String?, title: String?) -> Unit) {
        val handler = Handler(Looper.getMainLooper()) { msg ->
            val data = msg.data
            done(data.getString("url"), data.getString("title")?.takeIf { it.isNotBlank() })
            true
        }
        val msg: Message = handler.obtainMessage()
        webView.requestFocusNodeHref(msg)
    }

    // ---------------- Network blocking ----------------

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val reqUrl = request.url.toString()
            if (request.isForMainFrame) {
                // A new page is starting: reset per-page state before its sub-requests arrive.
                pageUrl = reqUrl
                adsOnPage.set(0)
                trackersOnPage.set(0)
                guardedOnPage.set(0)
                sync()
                return null
            }
            if (!reqUrl.startsWith("http")) return null
            val pageHost = Uri.parse(pageUrl).host?.removePrefix("www.") ?: return null
            val sh = host.shields(pageHost)
            val accept = request.requestHeaders["Accept"]
            // Trackers (EasyPrivacy) first, then ads (EasyList): each only if this site has it switched on.
            if (sh.trackers && CornEngine.trackers?.check(reqUrl, pageUrl, accept, false) == Decision.BLOCK) {
                trackersOnPage.incrementAndGet()
                host.onBlocked(BlockKind.TRACKER)
                sync()
                return empty()
            }
            if (sh.ads && CornEngine.ads?.check(reqUrl, pageUrl, accept, false) == Decision.BLOCK) {
                adsOnPage.incrementAndGet()
                host.onBlocked(BlockKind.AD)
                sync()
                return empty()
            }
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url
            val scheme = target.scheme ?: return true
            if (scheme != "http" && scheme != "https") return true // never jump out to other apps silently
            if (!request.isForMainFrame) return false
            if (!request.isRedirect) {
                val currentHost = Uri.parse(entry.url).host?.removePrefix("www.")
                // Redirect guard: the page navigating the whole tab to another site with no recent tap.
                val recentTap = System.currentTimeMillis() - lastUserGestureAt < 1500
                if (currentHost != null && host.shields(currentHost).popups &&
                    !request.hasGesture() && !recentTap && !sameSite(target, Uri.parse(entry.url))
                ) {
                    guardedOnPage.incrementAndGet()
                    sync()
                    host.onBlockedRedirect(target.host ?: target.toString())
                    return true
                }
            }
            // Link cleaning + HTTPS upgrade: load the tidied address instead.
            val original = target.toString()
            val prepared = prepare(original)
            if (prepared != original) {
                view.loadUrl(prepared)
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, startUrl: String, favicon: Bitmap?) {
            entry.url = startUrl
            entry.favicon = favicon
            exceptions = emptyList()
            generichide = false
            sync()
        }

        override fun onPageFinished(view: WebView, finishedUrl: String) {
            entry.url = finishedUrl
            view.title?.takeIf { it.isNotBlank() }?.let { entry.title = it }
            sync()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
            if (request.isForMainFrame) fallBackToHttp(view, request.url.toString())
        }

        override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
            handler.cancel() // never load a page with a broken certificate
            fallBackToHttp(view, error.url)
        }

        override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
            // Without this, Android kills the whole app when a page's process dies.
            Log.w(TAG, "page process gone (crashed=${detail.didCrash()})") // never log URLs
            host.onPageCrashed(entry)
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            entry.url = url
            sync()
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            if (!shieldsHere().popups && resultMsg != null) return host.onPopupAllowed(entry, resultMsg)
            guardedOnPage.incrementAndGet()
            sync()
            host.onBlockedPopup()
            return false
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            if (!title.isNullOrBlank()) entry.title = title
        }

        override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
            if (icon != null) entry.favicon = icon
        }

        /** Page console messages would otherwise be copied into Android's system log (with URLs). */
        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean = true

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            host.onEnterFullscreen(view) { callback.onCustomViewHidden() }
        }

        override fun onHideCustomView() = host.onExitFullscreen()
    }

    // ---------------- Page bridge: element hiding + media activity ----------------

    private fun installBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            Log.w(TAG, "WebView too old for element hiding")
            return
        }
        WebViewCompat.addWebMessageListener(webView, "cornBridge", setOf("*")) { _, message, _, isMainFrame, reply ->
            handleBridgeMessage(message, isMainFrame, reply)
        }
        WebViewCompat.addDocumentStartJavaScript(webView, PAGE_JS, setOf("*"))
    }

    /** DuckDuckGo autoconsent (unmodified, built into the app): clicks "Reject" on cookie banners. */
    private fun installCookieBannerCloser(context: Context) {
        if (!host.cookieBannersEnabled || !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val js = autoconsentScript(context) ?: return
        WebViewCompat.addDocumentStartJavaScript(webView, js, setOf("*")) // all frames: many banners live in iframes
    }

    private fun handleBridgeMessage(message: WebMessageCompat, isMainFrame: Boolean, reply: JavaScriptReplyProxy) {
        val msg = runCatching { JSONObject(message.data ?: return) }.getOrNull() ?: return
        when (msg.optString("t")) {
            "media" -> {
                entry.mediaPlaying = msg.optBoolean("playing")
                host.onMediaChanged()
            }
            "url" -> {
                if (!isMainFrame || !shieldsHere().ads) return
                val blocker = CornEngine.ads ?: return
                val c = blocker.cosmetic(msg.optString("url"))
                exceptions = c.exceptions
                generichide = c.generichide
                if (c.hideCss.isNotEmpty()) reply.postMessage(JSONObject().put("css", c.hideCss).toString())
            }
            "gen" -> {
                if (!isMainFrame || generichide || !shieldsHere().ads) return
                val blocker = CornEngine.ads ?: return
                val css = blocker.genericCss(msg.optJSONArray("classes").toList(), msg.optJSONArray("ids").toList(), exceptions)
                if (css.isNotEmpty()) reply.postMessage(JSONObject().put("css", css).toString())
            }
        }
    }

    private fun shieldsHere(): app.cornbrowser.data.Shields =
        Uri.parse(entry.url).host?.let { host.shields(it.removePrefix("www.")) } ?: app.cornbrowser.data.Shields.ALL_ON

    private fun empty() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    companion object {
        private const val TAG = "CornTab"

        /** Sites whose https version failed this session; loaded over http without retrying. */
        private val httpOnlyHosts: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())

        @Volatile private var autoconsent: String? = null

        private fun autoconsentScript(context: Context): String? {
            autoconsent?.let { return it }
            val name = context.assets.list("autoconsent")?.firstOrNull { it.startsWith("autoconsent-") && it.endsWith(".js") } ?: return null
            return context.assets.open("autoconsent/$name").bufferedReader().use { it.readText() }.also { autoconsent = it }
        }

        private fun sameSite(a: Uri, b: Uri): Boolean {
            val ha = a.host ?: return false
            val hb = b.host ?: return false
            return siteKey(ha) == siteKey(hb)
        }

        /** Rough site key (last two labels). The real version moves to the Rust core with the Public Suffix List. */
        private fun siteKey(host: String): String = host.split('.').takeLast(2).joinToString(".")

        private fun JSONArray?.toList(): List<String> =
            if (this == null) emptyList() else List(length()) { optString(it) }

        /** Injected before page scripts: element hiding and "is a video playing" reports. */
        private val PAGE_JS = """
            (function () {
              if (window.__corn || !window.cornBridge) return;
              window.__corn = true;
              var bridge = window.cornBridge;
              function playing() {
                var m = document.querySelectorAll('video,audio');
                for (var i = 0; i < m.length; i++) if (!m[i].paused && !m[i].ended) return true;
                return false;
              }
              ['play', 'playing', 'pause', 'ended'].forEach(function (ev) {
                document.addEventListener(ev, function () {
                  bridge.postMessage(JSON.stringify({ t: 'media', playing: playing() }));
                }, true);
              });
              if (window.top !== window) return; // element hiding runs in the main page only
              var style = document.createElement('style');
              function apply(css) {
                if (!css) return;
                style.textContent += css + '\n';
                var root = document.head || document.documentElement;
                if (root && style.parentNode !== root) root.appendChild(style);
              }
              bridge.onmessage = function (e) {
                try { var m = JSON.parse(e.data); if (m.css) apply(m.css); } catch (_) {}
              };
              bridge.postMessage(JSON.stringify({ t: 'url', url: location.href }));
              var seen = new Set(), pc = [], pi = [], timer = null;
              function note(el) {
                if (el.id && !seen.has('#' + el.id)) { seen.add('#' + el.id); pi.push(el.id); }
                if (el.classList) for (var i = 0; i < el.classList.length; i++) {
                  var c = el.classList[i];
                  if (!seen.has('.' + c)) { seen.add('.' + c); pc.push(c); }
                }
              }
              function send() {
                timer = null;
                if (!pc.length && !pi.length) return;
                bridge.postMessage(JSON.stringify({ t: 'gen', classes: pc, ids: pi }));
                pc = []; pi = [];
              }
              function scan(node) {
                if (!node || node.nodeType !== 1) return;
                note(node);
                var all = node.querySelectorAll('[id],[class]');
                for (var i = 0; i < all.length; i++) note(all[i]);
                if (!timer) timer = setTimeout(send, 60);
              }
              function boot() {
                scan(document.documentElement);
                new MutationObserver(function (ms) {
                  for (var i = 0; i < ms.length; i++) ms[i].addedNodes.forEach(scan);
                }).observe(document.documentElement, { childList: true, subtree: true });
              }
              if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
            })();
        """.trimIndent()
    }
}
