# 02 — Checklist

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-13, 13:52 IST

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
| [x] | **Corn password store** in the Vault, survives Flush: Keystore-wrapped AES-256-GCM, own form script, save/update/never prompts, fill on tap (PIN when Vault locked; switchable), Passwords screen. Verified on the emulator (save, PIN fill, no-PIN fill, kept after Flush) | Claude | Saved password fills a login after Flush |
| [x] | Password review #4: clear a copied password when Corn leaves the screen (and on next start if it was closed). Verified by pasting | Claude | Nothing pastes after leaving Corn |
| [x] | Split screen: "New tab in group" adds the tab to the group in the background (both panes stay); toast "Open" puts it in the focused pane. Verified | Claude | Panes unchanged; tab in the group |
| [x] | Password review #3: closing a tab (or a Flush) forgets its login-form note and fill chip, so nothing useless stays in memory | Claude | No leftovers for closed tabs |
| [x] | Password review #2: re-check the page's site right before filling (second lock). Normal fill verified afterwards | Claude | Fill refuses if the tab isn't on the login's site |
| [x] | Main buttons stay visible on long screens: Tabs screen Flush bar pinned at the bottom; group sheet (Save group / New tab in group), Save to Vault (Save) and split picker (New tab) pin their buttons while their content scrolls. Verified with 13 tabs | Claude | Buttons reachable without scrolling |
| [ ] | Passwords on real sites and a real phone (StrongBox), then an independent security review before public launch | Both | Logins save and fill on 3 real sites; review notes committed |
| [x] | **Split screen inside Corn**: two tabs, top/bottom (side by side in landscape), draggable divider, toolbar acts on the focused pane. Verified on the emulator | Claude | Two pages usable at once; divider resizes; closing works |
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
| [x] | Back to home page (↩) from a tab's first page. Verified on the emulator | Claude | Back returns to the home page; Forward reopens the page |
| [x] | Fix blank page when switching tabs (pull-to-refresh container per tab). Verified: group tabs show full size | Claude | Switching tabs always shows the page |
| [x] | Light mode (Settings → Appearance: Dark / Light / Phone), pages follow it. Verified both themes on a plain page | Claude | Plain pages readable in both themes |
| [x] | Menu: New tab group (groups current tab) / New tab in this group. Verified | Claude | Group created with the current tab |
| [x] | Typed addresses fall back to http; Corn error page. Verified with httpforever.com and a `.invalid` address | Claude | http-only sites open; failures show Corn's page |
| [x] | Background tabs load at once; saved picture while reloading; pauseTimers in background. Verified with server timings | Claude | Background tab loaded before tap; no blank reload; 0 timers in background |
| [ ] | **Vault**: save link + title, optional tags, **Collections**, PIN/biometric lock | Claude | Save, find, lock, delete all work |
| [ ] | Privacy: black recents screen, optional App Lock, no notifications while closed | Claude | Verified on a real phone |
| [ ] | Onboarding: privacy explainer, Safe Browsing / Safe Search choices, fun tone; no 18+ gate (Round 24). **Built, awaiting founder approval** | Claude | Founder approves the flow |
| [ ] | Settings screen for every option above | Claude | Every setting in the PRD is reachable |
| [ ] | In-app update check + signed release APK (release build works: 8.6 MB, R8, tested with a debug-key signature) | Both | Founder installs a signed build |
| [x] | Decide Google Safe Browsing default, onboarding mention and setting: on by default, onboarding line, Settings switch (built) | Founder | Recorded in the decision log |
| [x] | Safe Search switch (off by default); search URLs unit-tested for all 5 engines | Claude | Engine gets the right parameter on and off |
| [ ] | Delete a Vault item (long-press). **Built, not yet seen on device** | Claude | A saved link can be removed permanently |
| [x] | HTTPS upgrade + http fallback (unit-tested) and link cleaning (verified: server received `/?id=42` from a link with 4 tracking tags) | Claude | Tracking tags never reach the site |
| [x] | **File uploads** (sites' "choose file / photo"). Verified: test page received the picked 16-byte file | Claude | A test page receives a picked file |
| [x] | **Downloads** (download links save a file, with a confirm sheet). Verified: 20,000-byte file saved intact | Claude | A test file lands in Downloads |
| [x] | **Site permissions**: camera, microphone, location prompts (Allow / Block per site). Verified on https://example.com | Claude | A test page gets access after Allow and is refused after Block |
| [x] | **Address-bar suggestions** from History and the Vault (on the phone only; nothing sent anywhere). Verified | Claude | Typing part of a visited title shows it; tapping opens it |
| [x] | **Find in page** (menu → Find in page; matches count, next/previous). Verified 5 matches, 1/5 → 2/5 | Claude | Finds and steps through matches on a test page |
| [x] | **Text size** setting (verified ×1.30 at XL), and Settings reorganised: Flush, Keep List, Privacy, History and data, Search, Appearance (theme, text size, address bar), Advanced, About | Claude | Text size changes pages; every setting still reachable |
| [ ] | Cookie-banner closing (autoconsent). **Built, not yet seen closing a banner on device** | Founder | A cookie banner is rejected without a tap |

### QA fixes from [04-qa-where-corn-fails](../05-review/04-qa-where-corn-fails-2026-11-05.md) (2026-11-05, in this order)

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | **1. New windows follow the tap rule**: a tapped `target=_blank` link or `window.open` opens a new tab; no tap = blocked; burst guard; a page closing its own window (`onCloseWindow`) closes the tab and returns to the opener | Claude | Test page: tapped link and button open, no-tap blocked, self-closing window returns to opener; ayushpatil.com links open |
| [x] | **2. Links to other apps** (`tel`, `mailto`, `sms`, `geo`, `intent://`, other app schemes): after a tap, a sheet "Open in <app>?"; `intent://` made safe (browsable, no component/selector), web fallback; nothing opens without a tap | Claude | All 5 test links open the right app after a tap; fallback loads; no-tap does nothing |
| [x] | **3. Fullscreen inside its split pane**: video fills only its pane, other pane and system bars stay; Back always leaves fullscreen; closing the tab exits fullscreen | Claude | Two YouTube panes: fullscreen one, the other stays visible; Back and closing work |
| [x] | **4. Pages follow the site's theme**: no forced darkening by default, white default page background, optional "Darken all websites" switch (off) | Claude | ayushpatil.com's light toggle shows white; Hacker News shows its beige |
| [x] | **5. Image long-press menu**: Save image, Open in new tab, Share, Copy link (plain and linked images) | Claude | Long-press on a plain image shows the menu; Save lands in Downloads |
| [x] | **5b. Link long-press title**: the link's label, not the first text inside it (YouTube showed the duration "4:02") | Claude | YouTube video link shows the video title |
| [x] | **6. Ad scriptlets (like Brave)**: engine loads Brave's scriptlet resources and a list with scriptlet rules (downloaded at runtime, never in the repo), Corn injects them at document start; per-site Ads switch still applies | Claude (agent) | YouTube's player data has no ad schedule on 3 videos; no ads seen; other sites unaffected |
| [x] | **7. Repeatable QA**: test pages in the repo, decision functions unit-tested, one script runs the emulator checks and prints pass/fail | Claude (agent) | `scripts/qa-run.sh` passes on the emulator |
| [x] | Verified 2026-11-05 on the emulator: `scripts/qa-run.sh` **17/17** (old build 3/17); ayushpatil.com's Vishlyst link opens a new tab; two YouTube videos in split screen, fullscreen in the bottom pane only (top kept playing, ~0.6 s, one Back exits); YouTube `adPlacements`/`playerAds` gone on 4 videos (were present on 3 before), back with Ads off, gone again with Ads on; video plays; long-press title "Billie Jean…" instead of "4:56"; New collection sheet (Create, duplicate name note) | Claude | Done |
| [ ] | Delete a collection (there's no way to today; found while testing the New collection sheet) | Claude | A collection can be removed; its links stay in All |
| [ ] | **Downloads screen** (list of Corn's downloads: open, share, delete; Flush clears the list, files stay). Recommended, not started | Founder | Founder decides |

## Before launch (production) — added 2026-11-12

The founder has been testing on a real phone and reports it working well. These are what's still needed before a public release; "Must" blocks launch.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | **Must · Real-phone checks that need data:** passwords with the phone's StrongBox chip; 20+ tabs, background tabs and memory; a Keep List site still logged in after Flush (real login); cookie-banner closer on a real banner | Both | Each verified on the founder's phone and noted here |
| [ ] | **Must · Independent security review** of the password store, PIN and data clearing | Founder | Review notes committed; findings fixed or accepted |
| [ ] | **Must · Limit wrong PIN attempts** (password review #1; was "later" in the backlog): growing wait after a few wrong tries, PIN checked with a Keystore key | Claude | Brute-forcing the PIN on the phone is slowed; unit-tested |
| [ ] | **Must · Release key:** the founder creates Corn's release key (`keytool`, password kept private, backed up outside this Mac); key files git-ignored; release builds signed with it (password read from `~/.gradle/gradle.properties`). Today's "test" APK is signed with the debug key. See [05-releasing-and-updates](05-releasing-and-updates.md) | Both | A release build signed with the real key installs over the previous one and keeps its data |
| [x] | Release key created by the founder (`~/corn-release.jks`, 2026-11-13) and release signing verified: `app-release.apk` signed `CN=Corn, O=Corn, C=IN`, SHA-256 `e4b31db3…d3930ad`. Still open in the row above: the founder's backup, and the first signed release installing over the next one | Both | Done |
| [x] | **First signed release: Corn 0.2.1** (versionCode 2), made with `scripts/release.sh 0.2.1`: signed CN=Corn (SHA-256 e4b31db3…), tagged `v0.2.1`, published as a GitHub **pre-release** with install notes: github.com/ayush5144/corn/releases/tag/v0.2.1 (APK file sha256 e8f27e25…, matches the local copy) | Founder | Done |
| [ ] | Founder: uninstall the debug-signed Corn on the phone once (clears its data) and install 0.2.1; from then on updates keep data | Founder | Phone runs the signed 0.2.1 |
| [x] | Release tooling ready (2026-11-13): `scripts/create-release-key.sh` (you only type a password), `~/.gradle/gradle.properties` with the four `CORN_RELEASE_*` values as `CHANGE_ME` placeholders, release signing in `android/app/build.gradle.kts` (from that file or environment variables; unsigned when absent), `scripts/release.sh <version>` (raises `versionCode`, tests, builds, refuses a debug-signed APK, copies to Downloads, then asks before commit, tag, push and GitHub pre-release). Signing verified with a throwaway key | Claude | Done |
| [ ] | **Must · Version numbers:** raise `versionCode` on every release (today 1; `scripts/release.sh` does it) | Claude | Each GitHub/Play release has a higher `versionCode` |
| [ ] | **Must · Testers via GitHub Releases:** each build as a pre-release with the signed APK and install steps; testers told to use Obtainium for update notices | Both | A tester updates from one release to the next without losing data |
| [ ] | **Must · Play App Signing with the same key** at launch ("use my existing key"), so GitHub and Play installs share one seal | Founder | Play Console shows the app signing key matches the release key |
| [ ] | **Must · Play Store requirements:** privacy policy page, Data Safety form, content rating questionnaire (answer honestly: a browser with unrestricted internet access; the listing can say Corn is made for adults; no in-app age gate, Round 24), store listing (text, screenshots, icon) | Founder | Play Console shows no blocking items |
| [ ] | **Must · Licences screen in the app:** credits for uBlock Origin lists and scriptlets (GPL-3.0), Brave's adblock-rust (MPL-2.0), DuckDuckGo autoconsent, UniFFI, fonts (SIL OFL) and Corn's own MPL-2.0 | Claude | Settings → About → Licences lists every component with its licence |
| [ ] | **Must · Name and trademark:** trademark search for the launch name; domain registered | Founder | Search done; domain owned |
| [ ] | **Must · Manage collections:** delete a collection (its links stay in All, or delete them too), rename it, move links between collections, remove a link from a collection (founder, 2026-11-13: "no way to delete, edit or manage a collection today") | Claude | Each action works on the emulator and survives a restart |
| [ ] | **Must · QA suite first-check flake** (setup sometimes fails on the first check after a restart) | Claude | 3 full runs in a row with no setup failures |
| [ ] | Should · Accessibility pass: TalkBack labels, large font sizes, contrast | Claude | TalkBack reads every control; XL font size doesn't break layouts |
| [ ] | Should · Privacy-respecting crash reports (local only or opt-in) | Both | A crash on the phone can be seen and shared by the user |
| [ ] | Should · Downloads screen (recommended: list of Corn's downloads, cleared by Flush, files stay) | Founder | Founder decides |
| [ ] | Should · In-app update check for the GitHub build only: asks GitHub for the latest release, "Update available", downloads and opens Android's installer; explained in Settings (GitHub sees the IP) and switchable off; off in the Play build (Play forbids self-updates) | Claude | An older GitHub build offers the new version; the Play build never checks |
| [ ] | Later · F-Droid listing (Corn is open source; F-Droid builds it from the public code) | Founder | Corn installable and updatable from F-Droid |
| [ ] | Should · Vector redraw of the Corny logo (today an AI draft) | Founder | SVG master in `brand/` |
| [ ] | Should · Founder's check of History and onboarding (marked "awaiting founder" above) | Founder | Approved or changes listed |
| [x] | Long-press on search results: full link menu (with Open in new tab group, Save link to Vault, Share link) even when the link contains a site icon; image actions only for real pictures; titles from the link's heading. Verified on Google (3 results × title and icon) and by the QA check `search-result-link-menu` | Claude | Done |
| [ ] | Search-engine redirect links (Google's `google.com/goto?url=…`) are saved and copied as Google's link, not the site's: the real address is encrypted in the link, so it can only be learned by asking Google. Founder decision needed | Founder | Decided (keep as is / resolve by asking Google when saving or copying) |

## Vault export (Round 25, plan: [13-vault-export](../02-technical/13-vault-export.md))

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Export doc written first (formats, safety, mechanism, tests) | Claude | Committed |
| [x] | Rust: database version 4 (`password_changed_at`, upgrade from 3), `export.rs` (HTML bookmarks, links CSV, passwords CSV, escaping, formula guard on links only), exposed to Kotlin | Claude | Unit tests pass, including the 3 → 4 upgrade |
| [x] | Android: Vault → Export links (HTML / CSV) and Passwords → Export passwords (PIN, warning with checkbox, "Save to…", reminder) | Claude | Emulator: files read back with every item; PIN and checkbox can't be skipped |
| [ ] | The HTML file imports into Chrome on the Mac | Claude | Folders, links and titles appear in Chrome |
| [ ] | Docs (security doc, features, PRD) and release 0.2.2 | Claude | Pushed; signed release published |
| [ ] | Later: Import (same formats) and a full Corn backup (optionally encrypted) | Claude | Founder decides |

## New tab and tab previews (founder, 2026-11-13)

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | **New-tab page:** remove the big "corn" wordmark; a small Corny beside the tagline or the after-Flush one-liner; the search box moved up; stats and Vault shortcuts below | Claude | Seen on the emulator in dark and light, before and after a Flush |
| [x] | **Tab cards without a picture** (e.g. after Corn restarts): a large site badge (first letter on a colour), the site name and title, instead of blank placeholder lines. Nothing saved to disk, nothing fetched | Claude | Restored tabs are recognisable in the tab switcher and group rows |
| [x] | **Clean-up:** closing a tab (or a Flush) drops its picture and icon right away, instead of waiting for the tab list to forget them | Claude | Code path verified; memory not kept for closed tabs |
| [x] | **Group cards review:** several tabs in a group on the emulator; fix anything that reads badly at real size | Claude | Screenshots reviewed; fixes committed |
| [x] | Done 2026-11-13, seen on the emulator: new-tab page without the big logo and wordmark; the tagline in three centred lines ("Browse." / "Save what matters." / "Flush the rest.") with Corny after the last word, sitting right above the search bar, which is about a third of the way down (a trailing 🌽 in an after-Flush line is replaced by Corny); tab cards without a picture show the site icon or letter, name and title (a background-opened tab showed iana.org this way); the new-tab card and the error/crashed pages use Corny instead of the 🌽 emoji; closing a tab or a Flush drops its picture, icon and saved state (`TabEntry.forget`, no `Bitmap.recycle` because a frame may still draw it); group review found a blank white picture of a page captured while loading, so pictures are no longer taken while a page loads | Claude | Done |

## Address bar and Flush button (founder, 2026-11-13)

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | **Address bar empty when tapped** (like Brave), with a "current page" row at the top of the suggestions: the page's address, ✏️ Edit (puts it in the box) and ⧉ Copy | Claude | On a page: tap the bar → empty box, current-page row; Edit fills the box; Copy copies |
| [ ] | **Flush button without the yellow box:** the swirl drawn in corn yellow (the theme's darker yellow in light mode for contrast), same tap area | Claude | Seen in dark and light; Flush still works |

## Later: website and desktop (added 2026-11-13)

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | **Website** for Corn: what it is, privacy promise, download links (Play Store, GitHub releases), privacy policy page (needed for Play). Founder: later | Founder | Site live; privacy policy URL usable in Play Console |
| [ ] | **Desktop version of Corn** (Windows, Mac, Linux): Electron + React, reusing the Rust core (blocking, Vault) via napi-rs, Flush as an in-memory session; plan in [07-desktop](../02-technical/07-desktop.md). Start with a small spike (one window, tabs, blocker, Flush) to measure size and effort; decide its order against the iPhone app | Both | Spike runs on the Mac with blocking and Flush; decision recorded |

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
| Vault import / full backup | Export done (Round 25); import uses the same formats |
| Laptop app (Electron) | After iPhone |
| **Time ranges for deleting history** ("last 15 min / hour / 24 h / 7 days") | Exact for history (and possible for cookies via the cookie database), but site storage and the cache can only be deleted all at once (`clearCache` is per-application). To avoid a half-working option, v1 offers "delete everything" only. Founder decision, Round 11 |
| **Keep List C: keep Keep List sites' local storage** | Android can't delete one site's local storage (`deleteOrigin` doesn't cover it, measured). DuckDuckGo edits WebView's LevelDB files with a LevelDB library to spare Fireproof sites. Until then, Keep List sites keep cookies only, which is what the Keep List promises |
| **Keep List D: offer "Keep this site logged in?" after you log in** | DuckDuckGo watches for a submitted password form and asks on the next page (Ask / Always / Never). Optional, later if needed |
| Picture-in-picture video (video keeps playing in a small floating window when you leave Corn) | Founder: later |
| Print / save page as PDF (Android's print dialog) | Founder: later |
| Limit wrong PIN attempts; check the PIN with a Keystore key (password review #1) | Founder: later. Today: unlimited tries on a 4-digit PIN; passwords stay encrypted either way |
| Two-step logins: remember the username from the previous page (password review #7) | Founder: later. Today only the password is saved when the username is on an earlier page |
| *Info:* a page can trigger "Save password?" for its own site; Corn decrypts a saved password in memory to compare on login (password review #5, #6) | By design; optional later: limit prompts per page |
| Auto-delete history older than 90 days | Idea from Eddy, for people who never Flush. Discuss first |

## Out ❌

| Item | Why |
|---|---|
| Browser extensions | Founder decision, Round 0 |
