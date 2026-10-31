# 03 — Android Stack (First Platform)

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
| Vault storage | **Rust core: rusqlite + SQLCipher** (see [04](04-rust-core.md)) | Encrypted local database for saved items, tags and notes. One format on every platform. |
| Vault files | **Rust core: XChaCha20-Poly1305** | Encrypts downloaded files and thumbnails |
| Keys and lock | **Android Keystore + BiometricPrompt** | Holds the Vault key in secure hardware; unlocks with fingerprint, face or PIN |
| Settings | **DataStore** | Corn preferences |
| Downloads | **OkHttp + WorkManager** | Saves files into private app storage, not the public Downloads folder |
| Video extras | Built into WebView + Android **Picture-in-Picture** | Fullscreen, PiP, background playback |
| Privacy screen | `FLAG_SECURE` | App looks blank in recent apps; blocks screenshots of the Vault |
| Build | Android Studio, Gradle, Rust toolchain | All of it runs on a Mac |
| Testing | JUnit, Compose UI tests, **Macrobenchmark** | Correctness, plus memory and speed with many tabs |
| Distribution | Signed APK via website, GitHub Releases or Obtainium, plus an in-app update check | Works around the Play Store's adult-content policy |

> **Refinement vs. the first draft:** the first draft used Room + SQLCipher + Google Tink in Kotlin for the Vault. It moved into the Rust core so a Vault exported from the phone opens on iPhone and laptop with the same code. If iOS and desktop are ever dropped, Room + SQLCipher in Kotlin is a fine fallback.

## The blocker hooks

| Threat | WebView hook | Corn's action |
|---|---|---|
| Ads and trackers | `shouldInterceptRequest` (+ `ServiceWorkerControllerCompat` for service workers) | Ask Rust; return an empty response if blocked |
| Pop-up windows | `onCreateWindow` | Deny by default; allow only on a real user tap, if the user opts in |
| Redirect hijacks | `shouldOverrideUrlLoading` + user-gesture check | Stop top-level navigations the user didn't trigger |
| Hidden ads, anti-adblock walls | `addDocumentStartJavaScript` (androidx.webkit 1.9+) | Inject cosmetic CSS and scriptlets before page scripts run |
| Tracking parameters | `shouldOverrideUrlLoading` | Rust strips `utm_*`, `fbclid` etc. and unwraps `out.php?url=` style redirects |

## What Corn builds itself

1. **Tab manager.** Keep a few tabs live and freeze the rest to save memory. This is the hardest part.
2. **Blocker glue.** Connect the Rust core to WebView's hooks.
3. **Vault.** UI and locking in Kotlin; storage and encryption in Rust.
4. **Corn UI.** Tab switcher, onboarding, flush controls and theme.

## Tab manager memory policy

```
New tab opened
      │
      ▼
Live tabs > limit (3–5, tuned per device RAM)?
      │
  ┌───┴───┐
  No     Yes ──► Freeze the least recently used tab:
  │             • take a preview screenshot (memory/disk, session only)
  │             • save WebView state bundle
  │             • destroy the WebView (frees the renderer)
  ▼
Keep going

System sends onTrimMemory(...) ──► freeze more tabs immediately
User taps a frozen tab        ──► create a WebView, restore state, reload
```

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
