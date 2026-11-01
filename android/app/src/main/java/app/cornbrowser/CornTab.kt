/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject
import uniffi.corn_ffi.Decision

/** Events a tab reports to the UI. Called on the main thread. */
interface TabListener {
    fun onTabChanged(tab: CornTab)
    fun onBlockedPopup(tab: CornTab)
    fun onBlockedRedirect(tab: CornTab, target: String)
    fun onEnterFullscreen(view: View, exit: () -> Unit)
    fun onExitFullscreen()
}

/** One browser tab: a WebView wired to Corn's blocker. */
@SuppressLint("SetJavaScriptEnabled")
class CornTab(context: Context, private val listener: TabListener) {
    val id: Int = nextId.getAndIncrement()
    val webView = WebView(context)

    @Volatile var url: String = ""
        private set
    @Volatile var title: String = "New tab"
        private set
    /** Requests blocked on the current page. */
    val blockedOnPage = AtomicInteger(0)
    /** Pop-ups and redirects stopped on the current page. */
    val guardedOnPage = AtomicInteger(0)
    /** When false, this tab loads everything (used to compare with blocking off). */
    @Volatile var shieldsUp: Boolean = true

    /** The page that is currently loading: the "source" for third-party checks. */
    @Volatile private var pageUrl: String = ""
    @Volatile private var exceptions: List<String> = emptyList()
    @Volatile private var generichide: Boolean = false
    private var lastUserGestureAt = 0L

    init {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = true
        s.setSupportMultipleWindows(true) // so window.open() reaches onCreateWindow, where Corn denies it
        s.javaScriptCanOpenWindowsAutomatically = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.setGeolocationEnabled(false)
        s.userAgentString = BrowserIdentity.userAgent(s.userAgentString)

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            // Don't tell every site which app is browsing (X-Requested-With header).
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, emptySet())
        }

        installCosmeticBridge()
        webView.webViewClient = Client()
        webView.webChromeClient = Chrome()
        webView.setOnTouchListener { v, _ -> lastUserGestureAt = System.currentTimeMillis(); v.performClick(); false }
    }

    fun load(input: String) = webView.loadUrl(input)

    fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }

    // ---------------- Network blocking ----------------

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val reqUrl = request.url.toString()
            if (request.isForMainFrame) {
                pageUrl = reqUrl
                return null
            }
            if (!shieldsUp) return null
            val blocker = CornEngine.blocker ?: return null
            if (!reqUrl.startsWith("http")) return null
            val decision = blocker.check(reqUrl, pageUrl, request.requestHeaders["Accept"], false)
            if (decision == Decision.BLOCK) {
                blockedOnPage.incrementAndGet()
                return EMPTY()
            }
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url
            val scheme = target.scheme ?: return true
            if (scheme != "http" && scheme != "https") {
                Log.i(TAG, "blocked non-web navigation: $scheme")
                return true // intent://, market://, etc. Never jump out to other apps silently.
            }
            if (!shieldsUp || !request.isForMainFrame || request.isRedirect) return false
            // Redirect guard: a page navigating the whole tab to another site with no recent tap.
            val recentTap = System.currentTimeMillis() - lastUserGestureAt < 1500
            if (!request.hasGesture() && !recentTap && !sameSite(target, Uri.parse(url))) {
                guardedOnPage.incrementAndGet()
                listener.onBlockedRedirect(this@CornTab, target.host ?: target.toString())
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, startUrl: String, favicon: Bitmap?) {
            url = startUrl
            pageUrl = startUrl
            blockedOnPage.set(0)
            guardedOnPage.set(0)
            exceptions = emptyList()
            generichide = false
            listener.onTabChanged(this@CornTab)
        }

        override fun onPageFinished(view: WebView, finishedUrl: String) {
            url = finishedUrl
            title = view.title?.takeIf { it.isNotBlank() } ?: finishedUrl
            listener.onTabChanged(this@CornTab)
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            // Pop-ups and pop-unders: denied. (A later version opens user-tapped ones as a new tab.)
            guardedOnPage.incrementAndGet()
            listener.onBlockedPopup(this@CornTab)
            return false
        }

        override fun onReceivedTitle(view: WebView, newTitle: String?) {
            if (!newTitle.isNullOrBlank()) title = newTitle
            listener.onTabChanged(this@CornTab)
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            listener.onEnterFullscreen(view) { callback.onCustomViewHidden() }
        }

        override fun onHideCustomView() = listener.onExitFullscreen()
    }

    // ---------------- Cosmetic filtering ----------------
    // A script injected before page scripts asks Corn for element-hiding CSS for this URL,
    // then sends the page's classes and ids so Corn can return generic hiding rules.

    private fun installCosmeticBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            Log.w(TAG, "WebView too old for cosmetic filtering")
            return
        }
        WebViewCompat.addWebMessageListener(webView, "cornBridge", setOf("*")) { _, message, _, isMainFrame, reply ->
            if (isMainFrame) handleBridgeMessage(message, reply)
        }
        WebViewCompat.addDocumentStartJavaScript(webView, COSMETIC_JS, setOf("*"))
    }

    private fun handleBridgeMessage(message: WebMessageCompat, reply: JavaScriptReplyProxy) {
        if (!shieldsUp) return
        val blocker = CornEngine.blocker ?: return
        val msg = runCatching { JSONObject(message.data ?: return) }.getOrNull() ?: return
        when (msg.optString("t")) {
            "url" -> {
                val c = blocker.cosmetic(msg.optString("url"))
                exceptions = c.exceptions
                generichide = c.generichide
                if (c.hideCss.isNotEmpty()) reply.postMessage(JSONObject().put("css", c.hideCss).toString())
            }
            "gen" -> {
                if (generichide) return
                val css = blocker.genericCss(msg.optJSONArray("classes").toList(), msg.optJSONArray("ids").toList(), exceptions)
                if (css.isNotEmpty()) reply.postMessage(JSONObject().put("css", css).toString())
            }
        }
    }

    companion object {
        private const val TAG = "CornTab"
        private val nextId = AtomicInteger(1)
        private fun EMPTY() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

        private fun sameSite(a: Uri, b: Uri): Boolean {
            val ha = a.host ?: return false
            val hb = b.host ?: return false
            return siteKey(ha) == siteKey(hb)
        }

        /** Rough site key (last two labels). The real version uses the Public Suffix List in Rust. */
        private fun siteKey(host: String): String = host.split('.').takeLast(2).joinToString(".")

        private fun JSONArray?.toList(): List<String> =
            if (this == null) emptyList() else List(length()) { optString(it) }

        private val COSMETIC_JS = """
            (function () {
              if (window.__cornCosmetic || !window.cornBridge) return;
              window.__cornCosmetic = true;
              var bridge = window.cornBridge;
              var style = document.createElement('style');
              style.setAttribute('data-corn', '');
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
