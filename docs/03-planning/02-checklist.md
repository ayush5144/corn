# 02 — Checklist

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 14:28 IST

The single list of what's done, what's next and who owns it. Product decisions live in the [decision log](../01-product/02-decision-log.md); this file tracks **work**.

**Owners**
- **Founder**: decisions, design taste, testing on real phones, anything needing accounts or money.
- **Claude**: code, docs, research, prototypes.
- **Both**: Claude drafts, Founder reviews and approves.

**Rule:** tick a box only when its **Done when** is true.

---

## Phase 0 — Product definition 🔄 in progress

**Goal:** Everyone can say exactly what Corn v1 is and isn't.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Pick the architecture (OS web engine + Rust core) | Both | [ADR 0001](../adr/0001-os-web-engine-not-brave-fork.md) written |
| [x] | Organise docs, README, glossary | Claude | `docs/README.md` indexes every doc |
| [x] | Round 0 + Round 1 product Q&A | Both | Answers in the decision log |
| [x] | Round 2: Tab Groups model, Keep List / passwords, thumbnails, Flush placement, prototype tool | Both | Answers in the decision log |
| [x] | Round 3: Session protection, Flush presets, Tab Groups on Flush, toolbar layout | Both | Answers in the decision log |
| [ ] | Round 4: react to the prototype (look and feel, group naming, password store timing) | Founder | Answers in the decision log |
| [ ] | PRD v2 sign-off: no 🟡 left that blocks v1 | Founder | Founder says "PRD approved" |
| [ ] | Research donation platforms that accept an adult-adjacent product | Claude | Shortlist with policy links in `04-reference/` |

## Phase 1 — Setup

**Goal:** The Mac can build and run an Android app with a Rust library.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Git repo pushed to GitHub (`ayush5144/corn`, private) | Claude | `main` on GitHub |
| [x] | Install Android Studio, SDK, NDK, an ARM "Google Play" emulator image | Claude | Emulator boots on the Mac |
| [x] | Install Rust (`rustup`), Android targets, `cargo-ndk` | Claude | `cargo ndk --version` works |
| [ ] | Real Android phone with USB/wireless debugging on | Founder | Phone shows in `adb devices` |
| [ ] | Free disk space to ≥ 40 GB before installs | Founder | `df -h /` shows ≥ 40 GB free |
| [x] | `AGENTS.md` / `CLAUDE.md` with project rules for coding agents | Claude | File committed |

## Phase 2A — Design (runs alongside 2B)

**Goal:** See and feel Corn before writing app code.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Brand voice: cool, funny, modern — sample copy (onboarding, Flush, empty states) | Both | Copy sheet approved |
| [ ] | Low-fi wireframes: browser, tab switcher + Tab Groups, Flush, Vault + Collections, settings, onboarding | Claude | Wireframes reviewed |
| [ ] | Visual style: black + yellow, Flush Button look, icons, type | Both | Style sheet approved |
| [ ] | Clickable prototype (plain HTML, Next.js static only if needed), mobile viewport, in `mockups/prototype.html` (never as a claude.ai artifact). **v0.1 built**, waiting on the founder's tap-through | Claude | Founder has tapped through it on a real phone |
| [ ] | Prototype test with 3–5 target users | Founder | Notes recorded; changes fed back into the PRD |

## Phase 2B — Technical spike (one weekend, throwaway code)

**Goal:** Prove WebView + adblock-rust is good enough before building on it. Built in the real layout (`core/` + `android/`); deleted if it fails.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Minimal Android app: one WebView, URL bar | Claude | Runs on emulator |
| [x] | adblock-rust via cargo-ndk + UniFFI, EasyList + EasyPrivacy | Claude | Requests blocked in logs |
| [x] | Pop-up denial (`onCreateWindow`) + redirect guard. Verified: test page pop-up stopped and counted | Claude | Test pages can't open pop-ups |
| [ ] | Pick 15–20 real test sites | Founder | List saved (privately, not in the repo) |
| [ ] | Measure block rate vs Brave, video, memory with 10 tabs. **Emulator half done:** [03-spike-results.md](03-spike-results.md) | Both | Results table in `03-planning/` |
| [ ] | Go / no-go on WebView | Founder | Recorded as an ADR |

## Phase 3 — Android v1 build

**Goal:** A Corn APK the Founder uses daily.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Project skeleton: `android/` (Kotlin + Compose), `core/` (Rust workspace) | Claude | Builds from a clean checkout |
| [ ] | Browsing basics: address bar, back/forward/reload/share, fullscreen, PiP | Claude | Works on the test sites |
| [x] | Blocking + per-site switches (main + Advanced: ads, trackers, pop-ups). Verified: 5/5 on a known test page | Claude | Spike metrics hold in the real app |
| [x] | Search: built-in engines, DuckDuckGo default, search-bar dropdown switcher | Claude | Switch engine in one tap |
| [ ] | Tabs + **Tab Groups**: Tab Group Strip (icons, `^` grid, `+`, `×`), swipe, previews, instant switching | Claude | 20+ tabs across groups without lag on a mid-range phone |
| [ ] | Background tabs (Settings → Advanced: automatic by memory, recent 0/1/2/3/5, all with warning; unload on low memory; scroll restore; media held paused). **Built, not yet tested on device** | Claude | No crashes with 30 tabs |
| [ ] | **Flush Button** + Kept Session (default) + Auto-Flush (leave delay, inactivity; video counts as activity) | Claude | Flush tests in [05-flush-model](../02-technical/05-flush-model.md) pass |
| [ ] | **Keep List**: chosen sites stay logged in across Flush (cookies + IndexedDB kept, verified with a test site; not yet with a real login) | Claude | Keep List site still logged in after Flush |
| [ ] | **Corn password store** in the Vault, survives Flush (v1 or v1.1, timing open) | Claude | Saved password fills a login after Flush |
| [ ] | Session saved in app-private SQLite + backups disabled | Claude | Kept Session restores after restart; backup tool finds nothing |
| [x] | Filter lists refresh automatically (every 4 days, on Wi-Fi), engine rebuilt in the background. Verified: refreshed 8 s after first launch | Claude | Lists newer than 4 days after a week of use |
| [x] | Starter engine bundled in the app for first launch without internet. Verified: starter loaded before any download | Claude | Fresh install in airplane mode still blocks ads |
| [ ] | Revisit the automatic background-tab numbers (1/2/3/5 by memory) after real-phone testing | Both | Numbers confirmed or changed in the decision log |
| [ ] | **History**: recording, Keep history switch, History screen (search, by day, delete one, delete site, clear all). Verified: visits recorded with titles, one search = one entry, Flush empties it. **Screen not yet checked by the founder** | Claude | Founder uses it on a phone |
| [ ] | **Delete browsing data** (history, cookies + site data, cache, tabs; no time ranges). **Built, not yet tested on device** | Claude | Each category is empty afterwards; Keep List cookies stay |
| [x] | Flush deletes completely: cookies by WebView's cookie database (Keep List cookies untouched), all site storage, then restart. Verified on the emulator (cookie kept only for the Keep List site; local storage and IndexedDB gone for both) | Claude | Test site is a new visitor after Flush, except Keep List cookies |
| [x] | **Keep List A: keep Keep List sites' IndexedDB** on Flush / Delete browsing data (skip their `IndexedDB/<scheme>_<host>_<port>.indexeddb.*` folders; every other site's deleted). Verified: Keep List site kept cookie + IndexedDB (readable after restart), local storage gone; other site kept nothing | Claude | Keep List test site keeps cookie + IndexedDB after Flush; other site keeps nothing |
| [x] | **Keep List B: "Stay logged in after Flush" switch in the shield sheet** (adds/removes the current site, one tap). Verified: tap added the site to the stored Keep List, second tap removed it | Claude | Switch adds and removes the site; Settings list matches |
| [x] | Keep List doc: `docs/02-technical/11-keep-list.md` (what's kept, what's deleted, how, limits) | Claude | Doc indexed in docs/README.md |
| [ ] | Back to home page (↩) from a tab's first page. **Built, not yet seen on device** | Claude | Back returns to the home page; Forward reopens the page |
| [ ] | **Vault**: save link + title, optional tags, **Collections**, PIN/biometric lock | Claude | Save, find, lock, delete all work |
| [ ] | Privacy: black recents screen, optional App Lock, no notifications while closed | Claude | Verified on a real phone |
| [ ] | Onboarding: 18+ confirmation, privacy explainer, fun tone. **Built, awaiting founder approval** | Claude | Founder approves the flow |
| [ ] | Settings screen for every option above | Claude | Every setting in the PRD is reachable |
| [ ] | In-app update check + signed release APK (release build works: 8.6 MB, R8, tested with a debug-key signature) | Both | Founder installs a signed build |
| [x] | Decide Google Safe Browsing default, onboarding mention and setting: on by default, onboarding line, Settings switch (built) | Founder | Recorded in the decision log |
| [x] | Safe Search switch (off by default); search URLs unit-tested for all 5 engines | Claude | Engine gets the right parameter on and off |
| [ ] | Delete a Vault item (long-press). **Built, not yet seen on device** | Claude | A saved link can be removed permanently |
| [x] | HTTPS upgrade + http fallback (unit-tested) and link cleaning (verified: server received `/?id=42` from a link with 4 tracking tags) | Claude | Tracking tags never reach the site |
| [ ] | Cookie-banner closing (autoconsent). **Built, not yet seen closing a banner on device** | Founder | A cookie banner is rejected without a tap |

## Phase 4 — Beta

**Goal:** Real users, real feedback, no privacy surprises.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Privacy policy + terms (plain language) | Both | Published |
| [ ] | Distribution: website / GitHub Releases / Obtainium | Founder | Download link works |
| [ ] | Donation link live | Founder | First donation possible |
| [ ] | Small adult-only beta group | Founder | 10+ testers, feedback logged |
| [ ] | Network audit: Corn's own traffic contains no browsing data | Claude | Audit notes committed |

---

## Backlog ⏭️ (agreed, not v1)

| Item | Notes |
|---|---|
| **iPhone app** | Committed next platform after Android v1 (Swift + WKWebView) |
| **File downloads** | Private storage, not the phone gallery |
| **Cloud Vault** (~$3–5/month) | Opt-in, end-to-end encrypted; needs a payment provider that accepts it |
| Save downloaded files into the Vault | After downloads ship |
| Vault thumbnails (screenshot / placeholder / none) | Founder decision, Round 2 |
| Custom search engines | |
| Disguised app icon | |
| Vault export / import | Needs a format decision first |
| Laptop app (Electron) | After iPhone |
| **Time ranges for deleting history** ("last 15 min / hour / 24 h / 7 days") | Exact for history (and possible for cookies via the cookie database), but site storage and the cache can only be deleted all at once (`clearCache` is per-application). To avoid a half-working option, v1 offers "delete everything" only. Founder decision, Round 11 |
| **Keep List C: keep Keep List sites' local storage** | Android can't delete one site's local storage (`deleteOrigin` doesn't cover it, measured). DuckDuckGo edits WebView's LevelDB files with a LevelDB library to spare Fireproof sites. Until then, Keep List sites keep cookies only, which is what the Keep List promises |
| **Keep List D: offer "Keep this site logged in?" after you log in** | DuckDuckGo watches for a submitted password form and asks on the next page (Ask / Always / Never). Optional, later if needed |
| Auto-delete history older than 90 days | Idea from Eddy, for people who never Flush. Discuss first |

## Out ❌

| Item | Why |
|---|---|
| Browser extensions | Founder decision, Round 0 |
