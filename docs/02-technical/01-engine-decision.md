# 01 — Decision: WebView + Native Shell, Not a Brave Fork

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 01:43 IST

## Verdict

**Build Corn as a native app on each platform using the OS-provided web engine (Android System WebView, iOS WKWebView, Electron's Chromium on desktop), with a shared Rust core.**

A Brave fork is the right architecture for a funded browser team. For a solo developer, it's the wrong place to start.

Corn's differentiators (the tab experience, the Vault, the flush-first design, the black-and-yellow look and blocking by default) all sit *above* the browser engine. None of them require owning one.

## In simple words

- **You can build it today.** WebView runs in Android Studio on a Mac. A Brave fork needs a Linux machine with 100 GB+ of disk.
- **Google handles the dangerous part.** Security fixes for the engine reach users through the Play Store automatically. With a fork, you rebuild and ship every week, forever.
- **What makes Corn different doesn't need a fork.**
- **You find out fast whether people want it.** A working app takes days, not months.

**What you give up:** slightly weaker ad blocking than Brave (still very good), no browser extensions, and you manage tab memory yourself.

**When to revisit a fork:** only if real-site testing shows WebView blocking clearly falls short, *and* there's money and time to maintain one. Kotlin Vault and tab code can largely move into Brave's Android codebase, which is Java/Kotlin.

---

## Problem 1: The Brave fork can't be built on the dev machine

| | Chromium Android build needs | Dev Mac (Apple M4) |
|---|---|---|
| Host OS | **Linux x86-64 only.** "Building the Android client on Windows or Mac is not supported and doesn't work." | macOS ❌ |
| Disk | ≥100 GB free | 57 GB free ❌ |
| RAM | ≥8 GB, ">16GB highly recommended" | 16 GB ⚠️ |

Step one of the Brave-fork plan would be renting a powerful Linux server, and every edit, build and test loop would run remotely.

## Problem 2: Keeping a fork updated never ends

```
Chromium ships a new version every 4 weeks
        + weekly security patches
                │
                ▼
Brave rebases onto it ──► you rebase your fork onto Brave
                │
                ▼
Rebuild on Linux ──► test ──► sign ──► ship to users
                │
                ▼
Miss a cycle and users run a browser with publicly known security holes
```

This is how solo forks die. **Kiwi Browser**, a one-person Chromium fork with millions of installs, was archived in January 2025 because its founder lacked the time to keep maintaining it. The old plan's `upstream-sync.md` was 22 KB of process for this one problem.

## Problem 3: Distribution changes the picture

Google Play's policy: *"We don't allow apps that contain or promote sexual content… or any content or services intended to be sexually gratifying."* An adult-branded browser will likely be rejected, so Corn may be distributed as a self-hosted APK.

> If Corn is sideloaded **and** carries its own Chromium (Brave fork), nobody patches users unless Corn builds its own auto-updater and never misses a release.
>
> If Corn runs on **Android System WebView**, Google keeps updating the engine through the Play Store even though Corn itself is sideloaded.

This is the single most important reason for the decision.

---

## Option comparison (Android first)

| | **Brave fork** (old plan) | **Raw Chromium fork** | **System WebView + native shell** ✅ | **GeckoView** (Firefox engine) | Flutter/RN + webview plugin |
|---|---|---|---|---|---|
| Engine match with Chrome | Exact | Exact | Exact (Blink/V8) | Different engine (Gecko) | Exact (wraps WebView) |
| Ad-blocking ceiling | Excellent (Brave Shields) | Build it yourself | High: `adblock-rust` + page-start scripts | Excellent (full **uBlock Origin**) | Low: plugins hide the hooks |
| Who patches security holes | **You, weekly** | **You, weekly** | **Google, via Play** | You bump a Gradle dependency ~every 4 weeks | Google |
| Builds on a Mac | ❌ Linux only, 100 GB+ | ❌ | ✅ Android Studio, minutes | ✅ | ✅ |
| Time to first usable APK | Weeks to months | Months | Days | 1–2 weeks | Days |
| APK size | ~100 MB+ | ~100 MB+ | ~5–15 MB | ~60–80 MB | ~20 MB |
| Survivable for one developer | Low (see Kiwi) | Very low | **High** | Medium-high | Medium |
| Real-world proof | Brave | Bromite/Kiwi (both dead) | **DuckDuckGo Android** | Firefox, Iceraven | Few serious browsers |

APK sizes and timelines are estimates, not measurements.

## Decision flowchart

```
Do you need browser extensions or engine-level changes for the MVP?
├─ No (the PRD lists none) ──► Can WebView + adblock-rust block well enough?
│                               ├─ Yes ──► WebView shell ✅
│                               └─ No  ──► GeckoView + uBlock Origin (plan B)
└─ Yes ──► Funded, with a Linux build machine and a release engineer?
           ├─ Yes ──► Brave fork
           └─ No  ──► Don't build it yet
```

## Honest weaknesses of WebView

- **Tab memory.** Every WebView is heavy. Corn must build tab freezing itself; Chromium gives it for free.
- **Blocking gaps.** Request interception doesn't see WebSocket traffic, and there are no extensions. Expect roughly 85–95% of Brave Shields quality. That's an estimate, to be measured in the spike ([08-roadmap.md](../03-planning/01-roadmap.md)).
- **Private profiles.** Separate profiles need the `MULTI_PROFILE` WebView feature, so Corn checks for it at runtime and falls back on older WebViews.
- **Old WebViews.** Devices without Google services, such as some Huawei phones, ship outdated WebViews.

None of these force a fork.

---

## Corrections to earlier research (Google AI Mode chat)

| Claim | Reality |
|---|---|
| "Most websites are built on Chromium V8" | Websites are written to web standards, not to V8. Video works through MSE/EME plus hls.js/dash.js, decoded by Android's hardware decoders (MediaCodec). Any modern engine plays them. |
| V8 Lite mode helps | It saves ~1.5 MB of heap by **turning off the TurboFan optimizing compiler**, which makes JavaScript-heavy players slower. Video sites spend memory on video buffers and ad scripts; blocking ads saves far more. |
| "Tauri uses WebView2 on Android" | Wrong. Tauri uses Android System WebView on Android and WebKitGTK on Linux, which has weak codec support for these sites. |
| Electron `--lite-mode` flags | Desktop only, and Lite mode hurts video-player JavaScript for the reason above. |

Sources: [sources.md](../04-reference/01-sources.md).
