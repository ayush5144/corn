## 2026-11-01 — Per-site ad/tracker/pop-up switches, stats, new tab group, efficiency and privacy fixes

- Code: two blocking engines (EasyList ads, EasyPrivacy trackers) in `browser/CornEngine.kt`; per-site `Shields` in `data/CornData.kt`; two totals in `data/CornSettings.kt`; menu "New tab group"; engine favicons; R8 release build; http allowed; WebView metrics opt-out.
- Updated `docs/02-technical/09-filter-lists.md` (Lists Corn uses, Lifecycle: two engines).
- Updated `docs/02-technical/10-storage.md` (engine cache files).
- Updated `docs/02-technical/03-android.md` (stack: PIN lock, privacy screen; blocker hooks: two engines, pop-ups, crash recovery, link cleaning not built).
- Updated `docs/02-technical/04-rust-core.md` (hot-path rule: two engine instances).
- Updated `docs/02-technical/02-architecture.md` (blocker layer: two engines).
- Updated `docs/02-technical/08-repo-structure.md` (Android tree, release command, reviews folder).
- Updated `docs/01-product/01-prd.md` (per-site switches, stats, Safe Browsing open, favicons, new tab group).
- Updated `docs/03-planning/02-checklist.md` (pop-up guard and blocking verified; Safe Browsing decision and Vault delete tasks).
- Updated `AGENTS.md` (never log URLs rule; CLAUDE.md imports it).
- Added `docs/05-review/01-audit-2026-11-01.md` and `docs/05-review/02-review-2026-11-01.md`; indexed in `docs/README.md`.

## 2026-11-01 — Filter refresh, offline starter list, HTTPS upgrade, link cleaning, cookie banners, Vault delete

- Code: `browser/CornEngine.kt` (refresh + starter), `core/crates/corn-url` (clean, https_upgrade), `corn-block/src/bin/corn-compile.rs`, `scripts/build-starter-assets.sh`, `browser/CornTab.kt` (upgrade, cleaning, autoconsent, console silenced), Vault delete sheet, Settings switch for cookie banners.
- Updated `docs/02-technical/09-filter-lists.md` (lifecycle, gaps now closed).
- Updated `docs/02-technical/03-android.md`, `04-rust-core.md`, `08-repo-structure.md`, `10-storage.md`.
- Updated `docs/04-reference/02-open-source.md` (autoconsent in use, pinned version).
- Updated `docs/01-product/01-prd.md`, `02-decision-log.md` (Round 7), `docs/03-planning/02-checklist.md`.

## 2026-11-01 — Collection save fix, search words in the address bar, features file

- Code: `ui/Sheets.kt` (Save uses a typed new Collection), `data/CornData.kt` (`collectionNamed`), `data/SearchEngines.kt` (`searchTermsIn`), `ui/BrowserScreen.kt` (address bar shows search words).
- Added `docs/01-product/03-features.md`; indexed in `docs/README.md`.
- Updated `docs/01-product/02-decision-log.md` (Round 8).

## 2026-11-01 — Address bar shows the full URL again

- Code: reverted search-words display in `ui/BrowserScreen.kt`; removed `searchTermsIn` from `data/SearchEngines.kt`.
- Updated `docs/01-product/03-features.md` (Search and navigation) and `docs/01-product/02-decision-log.md` (Round 8, reversed).

## 2026-11-01 — Toolbar icons share one visible height

- Code: `ui/Icons.kt` toolbar icons drawn on a y 3–21 grid and stroke-compensated to an 18 dp visible height; arrows optically smaller (~15 dp); `ui/BrowserScreen.kt` tabs box 18 dp.
- Measured on the emulator: Vault 18.3, Tabs 17.9, Menu 18.3, Toilet 17.9, Back 15.5 dp (enlarged from 14.9 at founder request).

## 2026-11-01 — Checklist release size corrected

- Updated `docs/03-planning/02-checklist.md` (Phase 3: release APK 8.6 MB, was 6.0 before the starter lists and cookie-banner script).

## 2026-11-01 — Audit, review and features brought up to date; phone-testing guide

- Updated `docs/05-review/01-audit-2026-11-01.md` (release 8.6 MB, refresh traffic, console messages kept out of logs, open items closed).
- Updated `docs/05-review/02-review-2026-11-01.md` (new verified rows, Collection and icon bugs, privacy items status, 22 Rust tests).
- Updated `docs/02-technical/08-repo-structure.md` (release size, test APK link) and `docs/03-planning/03-spike-results.md` (cookie banners note).
- Added `docs/03-planning/04-testing-on-a-phone.md`; indexed in `docs/README.md`.

## 2026-11-01 — Safe Browsing and Safe Search switches

- Code: `data/SearchEngines.kt` (per-engine safe-search parameters), `data/CornSettings.kt` (`safeBrowsing`, `safeSearch`), `browser/CornTab.kt` (`setSafeBrowsing`), Settings switches, onboarding line, first JVM unit tests (`SearchEnginesTest`).
- Updated `docs/01-product/01-prd.md` (features + defaults), `03-features.md`, `02-decision-log.md` (Round 9), `docs/03-planning/02-checklist.md`, `docs/05-review/01-audit` and `02-review` (decision closed).

## 2026-11-01 — Onboarding shows Safe Browsing and Safe Search switches

- Code: `ui/OnboardingScreen.kt` (two switches with On/Off state), `ui/CornApp.kt` (passes settings).
- Updated `docs/01-product/01-prd.md`, `03-features.md`, `02-decision-log.md` (Round 9 follow-up).

