# Sources

Facts in these docs and where they come from. Anything marked *estimate* in the docs is reasoning, not a measurement.

## Build requirements and release cadence

- Chromium Android build: Linux x86-64 only, ≥100 GB disk, ≥8 GB RAM (>16 GB recommended) — [Chromium Android build instructions](https://chromium.googlesource.com/chromium/src/+/main/docs/android_build_instructions.md)
- New Chrome milestone every 4 weeks, weekly security refreshes — [Chrome release cycle](https://chromium.googlesource.com/chromium/src/+/HEAD/docs/process/release_cycle.md)
- Brave Android build environment — [Brave wiki](https://github.com/brave/brave-browser/wiki/Android-Development-Environment)

## Fork maintenance

- Kiwi Browser archived January 2025 — [Android Authority](https://androidauthority.com/kiwi-browser-shutting-down-3519510), [AlternativeTo](https://www.alternativeto.net/news/2025/1/kiwi-browser-discontinued-explore-alternatives-for-extension-support-and-security/)

## Distribution

- Google Play sexual content policy — [Play Console Help](https://support.google.com/googleplay/android-developer/answer/9878810)

## WebView capabilities

- androidx.webkit 1.9.0: multi-profile (`ProfileStore`) and `addDocumentStartJavaScript` — [androidx.webkit releases](https://developer.android.com/jetpack/androidx/releases/webkit), [WebViewCompat reference](https://developer.android.com/reference/androidx/webkit/WebViewCompat.html)
- DuckDuckGo Android runs on System WebView — [Wikipedia](https://www.Wikipedia.org/wiki/DuckDuckGo_browser), [Discourse thread](https://meta.discourse.org/t/discourse-not-supported-on-duckduckgo-browser-because-of-androidwebview/252751?tl=en)

## Ad blocking

- adblock-rust: ~5.7 µs average per request (EasyList + EasyPrivacy), 69× faster than Brave's previous engine — [Brave blog](https://brave.com/blog/improved-ad-blocker-performance/)
- adblock-rust memory cut ~75% (~45 MB) via FlatBuffers — [Shivan Kaul's post](https://shivankaul.com/blog/adblock-memory-reduction), [CyberInsider](https://cyberinsider.com/brave-boosts-adblock-engine-performance-via-75-memory-usage-cut/)
- uBlock Origin on GeckoView — [Mozilla Add-ons blog](https://blog.mozilla.org/addons/2020/02/06/ublock-origin-for-firefox-android-nightly/), [Mozilla Discourse](https://discourse.mozilla.org/t/how-to-load-ublock-origin-into-firefox-geckoview/130449)

## V8

- V8 Lite mode — [v8.dev blog](https://v8.dev/blog/v8-lite)

## From general knowledge (not re-verified in this session)

- Google declared Android "Kotlin-first" in 2019.
- Brave Android UI is mostly Java inherited from Chromium; Brave iOS uses WKWebView.
- DuckDuckGo iOS/macOS use WKWebView; DuckDuckGo Windows uses WebView2.
- Apple requires WebKit for iOS browsers outside the EU.
- Electron sessions without the `persist:` prefix are in-memory.
- Bromite stopped releasing in 2022.
