# 03 — Android Stack (First Platform)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-04, 23:58 IST

## Summary

- **No Tauri.**
- **Yes to Brave's ad-block engine (`adblock-rust`)**, but not Brave Shields or any Brave browser code.
- Everything else is standard native Android.

## The stack

| Layer | Tool | What it does |
|---|---|---|
| Language | **Kotlin** | Google's main language for Android apps |
| UI | **Jetpack Compose** | Corn's interface: toolbar, tab switcher, Vault screens, black-and-yellow theme |
| Browser engine | **Android System WebView** + **androidx.webkit** (≥ 1.9) | Blink and V8 render pages and play video. Google keeps it updated. |
| Ad blocking | **adblock-rust** (Brave's engine, MPL-2.0), wrapped in Corn's Rust core | Decides which requests and page elements to block |
| Rust ↔ Kotlin bridge | **cargo-ndk + UniFFI** | Builds Rust for Android and generates the Kotlin bindings automatically |
| Filter lists | EasyList, EasyPrivacy, uBlock filters, Peter Lowe's list | The block rules. They update on device; the download sends no browsing data. |
| Vault storage | **Rust core: rusqlite (plain SQLite)** (see [04](04-rust-core.md), [10](10-storage.md)) | Local database for saved items, tags, Keep List and session. One format on every platform. |
| Vault files | ⏭️ Later (downloads aren't in v1) | |
| Lock | **4-digit PIN** (salted PBKDF2 hash in app settings) | Gates the Vault and App Lock. Fingerprint unlock is in the backlog. |
| Settings | **DataStore** | Corn preferences |
| Downloads | **OkHttp + WorkManager** | Saves files into private app storage, not the public Downloads folder |
| Video extras | Built into WebView + Android **Picture-in-Picture** | Fullscreen, PiP, background playback |
| Privacy screen | `setRecentsScreenshotEnabled(false)` (Android 13+), `FLAG_SECURE` on older versions | App looks blank in recent apps |
| Build | Android Studio, Gradle, Rust toolchain | All of it runs on a Mac |
| Testing | JUnit, Compose UI tests, **Macrobenchmark** | Correctness, plus memory and speed with many tabs |
| Distribution | Signed APK via website, GitHub Releases or Obtainium, plus an in-app update check | Works around the Play Store's adult-content policy |

> **Refinement vs. the first draft:** the first draft used Room + SQLCipher + Google Tink in Kotlin for the Vault. It moved into the Rust core so a Vault exported from the phone opens on iPhone and laptop with the same code. If iOS and desktop are ever dropped, Room + SQLCipher in Kotlin is a fine fallback.

## The blocker hooks

| Threat | WebView hook | Corn's action |
|---|---|---|
| Ads and trackers | `shouldInterceptRequest` | Ask the trackers engine, then the ads engine (each only if switched on for this site); return an empty response if blocked |
| Pop-up windows | `onCreateWindow` (every pop-up, tapped or scripted, reaches it) | Deny and count by default; if the site allows pop-ups, open it as a new tab |
| Redirect hijacks | `shouldOverrideUrlLoading` + user-gesture check | Stop top-level navigations the user didn't trigger |
| Hidden ads, anti-adblock walls | `addDocumentStartJavaScript` (androidx.webkit 1.9+) | Inject cosmetic CSS and scriptlets before page scripts run |
| Tracking parameters | `shouldOverrideUrlLoading` + `load()` | ✅ Rust `corn-url` strips `utm_*`, `fbclid`, `gclid` and other click IDs (redirect unwrapping not built) |
| http pages | `shouldOverrideUrlLoading` + `load()`; `onReceivedError` / `onReceivedSslError` | ✅ HTTPS upgrade; falls back to http for the session if the secure version fails; local addresses exempt |
| Cookie banners | `addDocumentStartJavaScript` (all frames) | ✅ DuckDuckGo autoconsent 17.2.0, unmodified, clicks "Reject"; Settings switch, on by default |
| Page console messages | `onConsoleMessage` returns true | ✅ Not copied into Android's system log (they can contain URLs) |
| Page process crash | `onRenderProcessGone` | Drop the dead WebView and show a "This page stopped · Reload" card instead of crashing the app |

## What Corn builds itself

1. **Tab manager.** Keep a few tabs live and freeze the rest to save memory. This is the hardest part.
2. **Blocker glue.** Connect the Rust core to WebView's hooks.
3. **Vault.** UI and PIN lock in Kotlin; storage in Rust.
4. **Corn UI.** Tab switcher, onboarding, flush controls and theme.

## Tab manager memory policy

The real rules are written at the top of `TabManager.kt`; keep this section in sync with it.

```
Tab switched or opened
      │
      ▼
Keep the open tab + the N most recently used tabs "kept in memory"
  N = Settings → Advanced → Background tabs:
      Automatic by phone memory (default): 4 GB → 1, 6 GB → 2, 8 GB → 3, 12 GB+ → 5
      Keep recent tabs: 0 / 1 / 2 / 3 / 5
      Keep all (not recommended, asks first)
      │
      ▼
Every other tab is "unloaded":
  • keep its preview picture (memory only), back/forward state and scroll position
  • destroy the WebView (nothing runs)

Android warns memory is low (onTrimMemory) ──► unload every tab except the open one (Automatic mode)
User opens an unloaded tab                 ──► new WebView, restore state, reload, scroll back
```

What "kept in memory" really means (Android's documentation and our measurements):
- Corn pauses every video/audio element and stops the page restarting them, then calls `WebView.onPause()`.
- `onPause()` "does not pause JavaScript" (Android docs). Measured: a hidden tab's timers slow about 5× but keep running.
- `pauseTimers()` would stop scripts but is global ("for all WebViews"), so it would stop the open tab too. Corn never calls it.
- All tabs share one page process (measured): one page crashing reloads every tab.
- `pauseTimers()` is used where its global effect is right: while Corn is in the background (measured: page timers 20/20 s → 0/20 s).
- Tabs opened in the background start loading at once and take a memory slot; an unloaded tab shows its saved picture until the page draws (`onPageCommitVisible`).

The tab limit and the freeze policy make the biggest memory difference in the whole app. This is Kotlin's job, not Rust's.

## Why not Tauri, Flutter or React Native

- Tauri is designed for apps whose interface is one web page. A browser is the reverse: a native shell managing many web pages.
- All the important hooks are Android APIs. With Tauri, Corn would write Kotlin plugins anyway and juggle Rust, JavaScript and Kotlin instead of Kotlin plus a small Rust core.
- Tauri's desktop code sharing doesn't help a browser, because it uses a different engine on each OS.
- Flutter and React Native WebView plugins hide the hooks a browser needs.

| Option | Verdict |
|---|---|
| **Kotlin + Compose** | ✅ The standard for Android. Full access to every WebView hook. Used by DuckDuckGo. |
| Java | Older; Brave uses it only because of Chromium's history. |
| Kotlin Multiplatform | Can share logic with iOS. Not needed, because the Rust core does the sharing. |
| Flutter / React Native | ❌ WebView plugins hide the hooks. |
| Tauri | ❌ Built for single-page apps, not browsers. |

## Known WebView limits (and the plan for each)

| Limit | Plan |
|---|---|
| No WebSocket interception | Accept for MVP; measure in the spike how many ads rely on it |
| `MULTI_PROFILE` missing on old WebViews | Feature-check at runtime; fall back to clearing cookies/storage/cache manually |
| Outdated WebView on devices without Google services | Show a warning screen on very old WebView versions |
| Heavy per-tab memory | Tab manager freeze policy above |
