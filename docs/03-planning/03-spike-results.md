# 03 — Spike Results

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 00:41 IST

**Question:** Is Android WebView + Brave's adblock-rust good enough to build Corn on?
**Status:** 🟡 Promising on the emulator. Real-phone tests and the founder's own test sites are still to do.
**Build tested:** commit after `cf7b247`, debug APK, 2026-11-01.

## Setup

| | |
|---|---|
| Emulator | Pixel 8, Android 16 (API 36), Google Play image, arm64, on an Apple M4 Mac |
| WebView | 133.0.6943.137 (the image's built-in version; real phones update it through Play) |
| Filter lists | EasyList + EasyPrivacy, downloaded on first launch (134,084 rules) |
| App | `android/` spike: one WebView per tab, Brave's engine via `core/` (Rust → UniFFI → Kotlin) |

## Results so far

| Test | Result | Verdict |
|---|---|---|
| Engine build on first launch | 2.5 s (emulator), off the main thread | ✅ Acceptable once; cached afterwards |
| Engine load from cache | 76–479 ms on emulator (cold start slowest) | ✅ |
| Request check (Mac benchmark) | 1.8 µs average | ✅ |
| **adblock-tester.com** | **100 / 100** (11 services, 22 checks) | ✅ |
| Blocked on load: The Verge | 10 requests | ✅ |
| Blocked on load: Amazon | 29 requests | ✅ |
| 10 heavy sites in 10 tabs (BBC, CNN, Reddit, YouTube, Wikipedia, NYT, IMDb, ESPN, Amazon, The Verge) | 4,360 requests blocked in ~2 min; no crashes | ✅ |
| **Memory, 10 live tabs** | App 174 MB PSS + WebView renderer 878 MB ≈ **1.05 GB** | ⚠️ **Tab freezing is required.** Keep 3–5 tabs live and freeze the rest, as planned. |
| Cookie banners | Still shown (e.g. The Verge) | ⏳ Expected at spike time: autoconsent was built later (see the features doc) |

Seen in the blocked list: Google AdSense, Google Tag Manager / Analytics, DoubleClick, Facebook pixel, Hotjar, Yandex Metrica, Sentry, Bugsnag, Cloudflare Insights.

## Still to test

| Test | Owner | How |
|---|---|---|
| Founder's 15–20 sites, side by side with Brave | Founder | Same pages in Corn and Brave on the phone; note leftover ads, pop-ups, redirects |
| Real phone: speed, memory, battery | Founder + Claude | USB debugging on, `./gradlew installDebug`, Android Studio profiler |
| Video: play, seek, fullscreen, rotate | Founder | Video sites from the test list |
| Pop-up and redirect guard | Claude | Test pages that call `window.open` and script redirects |
| Google sign-in with the Chrome-style user agent | Founder | Sign in to a Google account inside Corn |
| Element hiding (cosmetic filters) | Claude | Check that leftover ad boxes collapse |

## Decision gate

```
Founder's sites close to Brave + video works + no crashes on the phone
├─ Yes ──► WebView confirmed (ADR 0002) → build Android v1 on this code
└─ No, blocking clearly worse ──► try GeckoView (plan B)
```

## Bugs found and fixed during the spike

| Bug | Cause | Fix |
|---|---|---|
| Engine not thread-safe | adblock-rust's default `single-thread` feature; WebView checks requests on several threads | Disable default features at the workspace level |
| Kotlin bindings missing | Release `.so` files are stripped of UniFFI metadata | Generate bindings from an unstripped host build |
| Compose libraries needed a newer SDK | Compose 2026.09 requires compileSdk 37 | Compile against API 37; still runs on Android 8+ |
| Address bar and counters didn't refresh | Compose skipped the composables (tab state wasn't observable) | Each tab exposes a Compose state `version`, bumped on the main thread |
| Blocked count reset too late | Counters cleared in `onPageStarted`, after early sub-requests | Reset when the main-frame request is intercepted |
