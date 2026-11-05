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
    /** Long-press on an image ([linkUrl] set when the image is also a link). */
    fun onImageLongPress(entry: TabEntry, imageUrl: String, linkUrl: String?, title: String?)
    /** The user tapped a link to another app (phone, email, SMS, map, `intent://`…). Ask before leaving Corn. */
    fun onExternalLink(entry: TabEntry, url: String)
    /** A tab opened as a new window closed itself (`window.close()`, e.g. after a sign-in). */
    fun onWindowClosed(entry: TabEntry) {}
    fun onMediaChanged()
    /** [entry]'s page went fullscreen (a video). In split screen it stays inside that tab's pane. */
    fun onEnterFullscreen(entry: TabEntry, view: View, exit: () -> Unit)
    fun onExitFullscreen()
    /** The page process died (crash or the system reclaimed memory). */
    fun onPageCrashed(entry: TabEntry)
    /** Settings → Privacy → Close cookie banners. Read when a tab is created. */
    val cookieBannersEnabled: Boolean
    /** Settings → Privacy → Safe Browsing. */
    val safeBrowsingEnabled: Boolean
    /** Settings → Appearance: pages in light mode (white background, prefers-color-scheme: light). */
    val lightPages: Boolean
    /** Settings → Appearance → Darken all websites (off by default): WebView darkens light-only pages in dark mode. */
    val darkenWebsites: Boolean
    /** A tab's address changed (for history). */
    fun onVisited(entry: TabEntry, url: String, title: String)
    /** A page's title became known (for history). */
    fun onTitle(url: String, title: String)
    /** Settings → Appearance → Text size, in percent (100 = the site's own size). */
    val textZoom: Int
    /** A page asked the user to pick files ("Upload photo"). Return false to cancel. */
    fun onFileChooser(callback: android.webkit.ValueCallback<Array<android.net.Uri>>, params: WebChromeClient.FileChooserParams): Boolean
    /** A page asked for the camera, microphone or location. Call [answer] exactly once. */
    fun onSitePermission(entry: TabEntry, site: String, kinds: Set<SitePermission>, answer: (Boolean) -> Unit)
    /** The page withdrew its request (e.g. it navigated away). */
    fun onSitePermissionCancelled()
    /** The user tapped a link that downloads a file. */
    fun onDownload(request: DownloadRequest)
    /** Find in page: match [active] (0-based) of [total]. */
    fun onFindResult(active: Int, total: Int)
    /** A message from Corn's login-form script ([app.cornbrowser.passwords.LoginScript]). Origin and frame come from WebView. */
    fun onLoginMessage(entry: TabEntry, data: String, sourceOrigin: String, isMainFrame: Boolean, reply: JavaScriptReplyProxy) {}
    /** The tab started loading a new page (its login form, if any, is gone). */
    fun onLoginPageStarted(entry: TabEntry) {}
    /** A tab was closed (or every tab was, by a Flush): drop anything kept for it. */
    fun onTabClosed(entry: TabEntry) {}
}

/** What a site can ask to use. */
enum class SitePermission(val label: String) { CAMERA("camera"), MICROPHONE("microphone"), LOCATION("location") }

/** A file a page wants to download. */
data class DownloadRequest(val url: String, val userAgent: String, val contentDisposition: String?, val mimeType: String?, val length: Long)

/**
 * The live part of a tab: a WebView wired to Corn's blocker.
 * A tab that isn't live (frozen) has no CornTab, only its TabEntry with a preview and saved state.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class CornTab(context: Context, private val entry: TabEntry, private val host: TabHost) {
    // The WebView's context theme tells pages the user's preference (prefers-color-scheme).
    val webView = WebView(
        android.view.ContextThemeWrapper(context, if (host.lightPages) app.cornbrowser.R.style.Theme_Corn_LightPages else app.cornbrowser.R.style.Theme_Corn),
    )

    private val adsOnPage = AtomicInteger(0)
    private val trackersOnPage = AtomicInteger(0)
    private val guardedOnPage = AtomicInteger(0)
    private val syncPending = AtomicBoolean(false)
    @Volatile private var pageUrl: String = entry.url
    @Volatile private var exceptions: List<String> = emptyList()
    @Volatile private var generichide: Boolean = false
    private var lastUserGestureAt = 0L
    /** When this tab last opened a new window (WindowPolicy's one-window-per-tap rule). */
    private var lastWindowAt = 0L
    private val main = Handler(Looper.getMainLooper())

    init {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = true
        // Every new-window request (tapped target=_blank link, window.open, scripted pop-up) reaches
        // onCreateWindow, where Corn applies WindowPolicy: tapped = new tab, untapped = blocked.
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // Location only after the user allows it for the site (onGeolocationPermissionsShowPrompt).
        s.setGeolocationEnabled(true)
        s.textZoom = host.textZoom
        s.userAgentString = BrowserIdentity.userAgent(s.userAgentString)
        setSafeBrowsing(host.safeBrowsingEnabled)
        // Pages follow the site (like Chrome): sites with a dark mode go dark because the theme above
        // says the user prefers dark; light-only sites stay as designed, and a site's own light/dark
        // switch works. White is the web's default page background, so text without a colour stays
        // readable. Forced darkening only with Settings → Darken all websites (QA 2026-11-05, #2: it
        // turned ayushpatil.com's own light mode back to black and Hacker News's beige to near-black).
        val darken = host.darkenWebsites && !host.lightPages
        webView.setBackgroundColor(if (darken) 0xFF0F0E0B.toInt() else android.graphics.Color.WHITE)
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
            androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, darken)
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        installBridge()
        installLoginBridge()
        installCookieBannerCloser(context)
        webView.webViewClient = Client()
        webView.webChromeClient = Chrome()
        webView.setOnTouchListener { _, _ -> lastUserGestureAt = System.currentTimeMillis(); false }
        webView.setOnLongClickListener { onLongPress() }
        webView.setDownloadListener { url, userAgent, disposition, mime, length ->
            host.onDownload(DownloadRequest(url, userAgent, disposition, mime, length))
        }
        webView.setFindListener { active, total, done -> if (done) host.onFindResult(active, total) }
    }

    // ---------- find in page ----------
    fun find(query: String) = if (query.isEmpty()) webView.clearMatches() else webView.findAllAsync(query)
    fun findNext(forward: Boolean) = webView.findNext(forward)
    fun clearFind() = webView.clearMatches()

    fun setTextZoom(percent: Int) { webView.settings.textZoom = percent }

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

    /** Plain-language reason for Corn's error page (WebViewClient error codes). */
    private fun errorText(code: Int): String = when (code) {
        WebViewClient.ERROR_HOST_LOOKUP -> "Corn couldn't find this site. Check the address, or your connection."
        WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_TIMEOUT -> "The site didn't respond. It may be down, or your connection is slow."
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "A secure connection to this site failed."
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "Corn can't open this kind of link."
        WebViewClient.ERROR_TOO_MANY_REQUESTS -> "The site is getting too many requests. Try again in a moment."
        else -> "This page couldn't be loaded."
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

    /**
     * The tab stays in memory behind the open tab. Pauses every video/audio element and stops the
     * page from restarting them until the tab is opened again, then calls `WebView.onPause()`
     * (animations, geolocation). Scripts keep running, slowed down by Chromium for hidden pages;
     * see the rules at the top of [TabManager].
     */
    /** True while the tab is kept in memory behind the open tab (media held paused). */
    private var held = false

    fun keepInBackground() {
        held = true
        webView.evaluateJavascript(
            "window.__cornHold=true;document.querySelectorAll('video,audio').forEach(function(m){m.pause()})", null,
        )
        webView.onPause()
        entry.mediaPlaying = false
    }

    /** The tab is the open one again: allow media and resume. */
    fun bringToForeground() {
        held = false
        webView.evaluateJavascript("window.__cornHold=false", null)
        webView.onResume()
    }

    private var pendingScrollY = 0

    /** After an unloaded tab reloads, scroll back to where the user was. */
    fun restoreScrollAfterLoad(y: Int) {
        pendingScrollY = y
    }

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
                requestLinkHref { _, title -> linkTitle(url, title) { best -> host.onLinkLongPress(entry, url, best ?: url) } }
                return true
            }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                val image = hit.extra
                requestLinkHref { url, title ->
                    when {
                        url == null && image != null -> host.onImageLongPress(entry, image, null, null)
                        url == null -> Unit
                        image != null -> linkTitle(url, title) { best -> host.onImageLongPress(entry, image, url, best) }
                        else -> linkTitle(url, title) { best -> host.onLinkLongPress(entry, url, best ?: url) }
                    }
                }
                return true
            }
            WebView.HitTestResult.IMAGE_TYPE -> {
                val image = hit.extra ?: return false
                host.onImageLongPress(entry, image, null, null)
                return true
            }
        }
        return false
    }

    /**
     * The link's title for the long-press sheet. WebView only gives the first text inside the link,
     * which on video sites is the duration ("4:02"). Ask the page for every link to the same address
     * (labels first, then text) and let [LinkTitles.pick] skip durations and counters.
     */
    private fun linkTitle(url: String, fallback: String?, done: (String?) -> Unit) {
        val js = "(function(u){var out=[];var as=document.querySelectorAll('a[href]');" +
            "for(var i=0;i<as.length&&out.length<40;i++){var a=as[i];if(a.href!==u)continue;" +
            "out.push(a.getAttribute('aria-label'),a.getAttribute('title'));" +
            "var im=a.querySelector('img[alt]');if(im)out.push(im.getAttribute('alt'));" +
            "out.push((a.innerText||'').slice(0,300));}return out;})(" + JSONObject.quote(url) + ")"
        webView.evaluateJavascript(js) { result ->
            val fromPage = runCatching {
                val arr = JSONArray(result)
                List(arr.length()) { i -> if (arr.isNull(i)) null else arr.optString(i) }
            }.getOrDefault(emptyList())
            done(LinkTitles.pick(fromPage + fallback))
        }
    }

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
            if (scheme != "http" && scheme != "https") {
                // Links to other apps (phone, email, SMS, maps, intent://): only after a tap, and Corn
                // asks first. A page can never send the user to another app by itself.
                val tapped = request.hasGesture() || System.currentTimeMillis() - lastUserGestureAt < 1500
                if (tapped && ExternalLinks.kind(target.toString()) != ExternalLinks.Kind.IGNORE) {
                    host.onExternalLink(entry, target.toString())
                }
                return true
            }
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

        override fun onPageCommitVisible(view: WebView, url: String) {
            entry.showingSnapshot = false // the page has drawn: drop the saved picture
            if (held) view.evaluateJavascript("window.__cornHold=true", null) // still in the background
        }

        override fun onPageStarted(view: WebView, startUrl: String, favicon: Bitmap?) {
            entry.loadError = null
            entry.loading = true
            entry.url = startUrl
            entry.favicon = favicon
            exceptions = emptyList()
            generichide = false
            host.onLoginPageStarted(entry)
            sync()
        }

        override fun onPageFinished(view: WebView, finishedUrl: String) {
            entry.loading = false
            entry.showingSnapshot = false
            entry.url = finishedUrl
            view.title?.takeIf { it.isNotBlank() }?.let { entry.title = it }
            if (pendingScrollY > 0) {
                val y = pendingScrollY
                pendingScrollY = 0
                view.postDelayed({ view.scrollTo(0, y) }, 250) // after the page lays out
            }
            sync()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
            if (!request.isForMainFrame || fallBackToHttp(view, request.url.toString())) return
            entry.loadError = errorText(error.errorCode)
            entry.showingSnapshot = false
            entry.loading = false
        }

        override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
            handler.cancel() // never load a page with a broken certificate
            if (fallBackToHttp(view, error.url)) return
            if (error.url == view.url || entry.url == error.url) {
                entry.loadError = "This site's security certificate isn't valid, so Corn didn't open it."
                entry.showingSnapshot = false
                entry.loading = false
            }
        }

        override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
            // Without this, Android kills the whole app when a page's process dies.
            Log.w(TAG, "page process gone (crashed=${detail.didCrash()})") // never log URLs
            host.onPageCrashed(entry)
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            entry.url = url
            if (!isReload) host.onVisited(entry, url, view.title ?: "")
            sync()
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onShowFileChooser(view: WebView, callback: android.webkit.ValueCallback<Array<android.net.Uri>>, params: FileChooserParams): Boolean =
            host.onFileChooser(callback, params)

        /** Camera / microphone (WebRTC). Anything else (e.g. protected media IDs) is refused. */
        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
            val kinds = buildSet {
                if (android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(SitePermission.CAMERA)
                if (android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(SitePermission.MICROPHONE)
            }
            val site = request.origin.host?.removePrefix("www.") ?: ""
            main.post {
                if (kinds.isEmpty() || site.isEmpty()) { request.deny(); return@post }
                host.onSitePermission(entry, site, kinds) { allow ->
                    if (allow) {
                        request.grant(request.resources.filter {
                            it == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE || it == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        }.toTypedArray())
                    } else {
                        request.deny()
                    }
                }
            }
        }

        override fun onPermissionRequestCanceled(request: android.webkit.PermissionRequest) {
            main.post { host.onSitePermissionCancelled() }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
            val site = Uri.parse(origin).host?.removePrefix("www.") ?: ""
            if (site.isEmpty()) { callback.invoke(origin, false, false); return }
            // retain = false: WebView doesn't store the answer; Corn remembers it for this session only.
            host.onSitePermission(entry, site, setOf(SitePermission.LOCATION)) { allow -> callback.invoke(origin, allow, false) }
        }

        override fun onGeolocationPermissionsHidePrompt() = host.onSitePermissionCancelled()

        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            // Tapped links and buttons ("open in new tab", "Sign in with Google", "Share") open as a
            // new tab; windows a page opens by itself are pop-ups. See WindowPolicy.
            val now = System.currentTimeMillis()
            val decision = WindowPolicy.newWindow(
                userGesture = isUserGesture,
                siteBlocksPopups = shieldsHere().popups,
                msSinceLastOpened = if (lastWindowAt == 0L) Long.MAX_VALUE else now - lastWindowAt,
            )
            if (decision == WindowPolicy.Decision.OPEN_TAB && resultMsg != null && host.onPopupAllowed(entry, resultMsg)) {
                lastWindowAt = now
                return true
            }
            guardedOnPage.incrementAndGet()
            sync()
            host.onBlockedPopup()
            return false
        }

        /** A window this tab was opened as closed itself (sign-in pop-ups do this when done). */
        override fun onCloseWindow(window: WebView) = host.onWindowClosed(entry)

        override fun onReceivedTitle(view: WebView, title: String?) {
            if (!title.isNullOrBlank()) {
                entry.title = title
                host.onTitle(entry.url, title)
            }
        }

        override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
            if (icon != null) entry.favicon = icon
        }

        /** Page console messages would otherwise be copied into Android's system log (with URLs). */
        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean = true

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            host.onEnterFullscreen(entry, view) { callback.onCustomViewHidden() }
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

    /**
     * Passwords: Corn's own login-form script in every frame, and a separate `cornLogins` channel.
     * The script runs on https pages only; [TabHost.onLoginMessage] checks origin and frame again.
     */
    private fun installLoginBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) return
        WebViewCompat.addWebMessageListener(webView, "cornLogins", setOf("*")) { _, message, sourceOrigin, isMainFrame, reply ->
            val data = message.data ?: return@addWebMessageListener
            host.onLoginMessage(entry, data, sourceOrigin.toString(), isMainFrame, reply)
        }
        WebViewCompat.addDocumentStartJavaScript(webView, app.cornbrowser.passwords.LoginScript.source(BuildConfig.DEBUG), setOf("*"))
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
              // A tab kept in memory behind the open tab may not restart media (window.__cornHold).
              document.addEventListener('play', function (e) {
                if (window.__cornHold && e.target && e.target.pause) e.target.pause();
              }, true);
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
