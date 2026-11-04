# Roadmap

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 00:41 IST

## Platform order

```
 Android (Kotlin)  ──►  iPhone (Swift)  ──►  Laptop (Electron)
   build 1st             build 2nd             build 3rd
        └──────── shared Rust core reused by each ────────┘
```

Each new platform gets cheaper because blocking, URL cleaning, flush rules and the Vault are already done.

## Step 0 — The spike (one weekend, decides everything)

Build the smallest possible test:

- One Kotlin activity, one WebView, a URL bar.
- adblock-rust with EasyList + EasyPrivacy via cargo-ndk/UniFFI.
- Pop-ups denied in `onCreateWindow`.

Run it on the 15–20 sites target users actually visit and measure:

| Metric | How | Pass if |
|---|---|---|
| Ad / pop-up block rate | Compare against Brave on the same pages | Close to Brave; no frequent pop-ups or redirects |
| Video | Play, seek, fullscreen, PiP | Works on every test site |
| Memory | 10 tabs open, Android Studio profiler | Stable, no crashes on a mid-range phone |
| Request-check cost | Time the Rust call | Microseconds, not milliseconds |

```
Spike result
├─ Passes            ──► WebView stack confirmed ✅ → Step 1
├─ Blocking fails    ──► Try GeckoView + uBlock Origin
└─ Both fail         ──► Only then reconsider a Brave fork
```

## Android build order

| Step | What | Done when |
|---|---|---|
| 1 | Bare browser: one WebView, URL bar, back/forward | Can browse any site |
| 2 | Rust core (`corn-block`, `corn-lists`, `corn-url`) wired in | Spike metrics pass |
| 3 | Multiple tabs + tab manager (freeze policy) | 20+ tabs without crashes |
| 4 | Flush model (button, background, idle timer, Keep list) | Flush tests in [05](../02-technical/05-flush-model.md) pass |
| 5 | Vault (`corn-vault`, PIN/biometric lock) | Save, search, lock, export, delete work |
| 6 | Polish: onboarding + 18+ gate, privacy screen, update checker, theme | Internal beta |
| 7 | Small adult-only test group | Real-world compatibility list |

## Later

| Phase | Platform | Key work |
|---|---|---|
| iPhone | Swift + WKWebView | Rule-list compiler in Rust, SwiftUI shell, App Store review |
| Laptop | Electron | napi-rs bindings, hover previews, keyboard nav, auto-updater |

## Deferred decisions

- Distribution channel details (website vs GitHub Releases vs Obtainium vs neutral Play listing).
- Licence for Corn's own code (legal review; adblock-rust is MPL-2.0, filter lists have their own licences).
- Any sync, accounts or diagnostics. None are in the MVP.
- Whether a Mac-native app replaces Electron on macOS.

## Repository layout

See [../02-technical/08-repo-structure.md](../02-technical/08-repo-structure.md).
