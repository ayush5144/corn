# 09 — Filter Lists

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 18:38 IST

## What they are

Filter lists are plain-text files of rules that tell the blocker what to block or hide. Brave's adblock-rust is the engine; the lists are its instructions. Volunteers maintain them and update them several times a day.

```
||doubleclick.net^                 ← block every request to doubleclick.net
||ads.example.com^$third-party     ← block it only when another site loads it
@@||cdn.example.com/player.js      ← exception: never block this one
example.org##.ad-banner            ← on example.org, hide elements with class "ad-banner"
##.sponsored-box                   ← hide "sponsored-box" on every site
```

## Lists Corn uses

| List | Purpose | Size (Nov 2026) | Licence | Status |
|---|---|---|---|---|
| EasyList | Ads (+ element hiding) → the **ads** engine | ~77,000 lines | GPL-3.0 or CC BY-SA 3.0+ | ✅ in app |
| EasyPrivacy | Trackers → the **trackers** engine | ~57,000 lines | GPL-3.0 or CC BY-SA 3.0+ | ✅ in app |
| EasyList Cookie List | Cookie banners (hiding) | | GPL-3.0 or CC BY-SA 3.0+ | 🔜 |
| uBlock Origin filters | Annoyances, malware, unbreak fixes | | GPL-3.0 | 🔜 |
| Brave lists + resources | Extra rules; scriptlets against anti-adblock | | MPL-2.0 | 🔜 |
| AdGuard URL Tracking | Link cleaning (`$removeparam`) | | GPL-3.0 | 🔜 |
| AdGuard pop-ups / mobile | Optional extras | | GPL-3.0 | ⏭️ |

## Why they're not in the repo

1. **They go stale in days.** Fetching from the authors means users get yesterday's fixes, not last month's.
2. **They aren't ours.** The phone fetches them like any web page, so their GPL licence stays separate from Corn's MPL licence.
3. **Volunteers maintain them for free.** Fetching from the source keeps that work flowing to Corn without us maintaining copies.
4. **Smaller app.** About 6 MB is fetched once instead of being shipped in every update.

## Lifecycle on the phone

```
First launch:  download lists → build two engines (ads = EasyList, trackers = EasyPrivacy) → save ads-v1.dat, trackers-v1.dat
Every launch:  load the saved engines (~1 ms on an M4 Mac; a few ms on a phone)
Every 4 days, on Wi-Fi: download fresh lists → sanity-check → rebuild in the background → swap engines   (✅ built)
Fresh install:  load the built-in starter list instantly → first download on any network → swap   (✅ built)
```

**Why two engines:** each site can switch ads and trackers off separately (shield → Advanced) without changing adblock-rust. Both are the same unmodified engine loaded with a different list. The lists overlap slightly, so the split is close but not exact.

Corn's own servers are never involved. Downloading a list sends no browsing data.

## Measured (Mac M4, `core/crates/corn-block/examples/bench.rs`)

| Measure | EasyList + EasyPrivacy (134,076 lines) |
|---|---|
| Build engine from lists | 46 ms |
| Saved engine size | 5.7 MB |
| Load saved engine | 1.2 ms |
| Average request check | 1.8 µs |

Phone figures go in the spike results.

## Gaps to close before release

| Gap | Plan | Tracked in |
|---|---|---|
| Lists refresh | ✅ Every 4 days on Wi-Fi (`CornEngine.refreshIfStale`, checked on start and when Corn comes to the foreground) | Done |
| Fresh install with no internet | ✅ Starter engines compiled at build time from that day's lists (`scripts/build-starter-assets.sh`, `corn-compile`), never committed | Done |
| A corrupted download could break blocking | ✅ Last good engine kept; new one swapped in only after it builds and is saved | Done |
| A list could be tampered with in transit | ✅ HTTPS only; rejected if under 10,000 lines | Done |

## Credits

Every list Corn uses is credited by name, author and licence in **About → Open-source licences**.
