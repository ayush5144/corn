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

## 2026-11-01 — Docs sync after onboarding switches; Android unit tests documented

- Updated `docs/05-review/02-review-2026-11-01.md` (Testing gaps: 3 Android unit tests).
- Updated `docs/02-technical/08-repo-structure.md` (app/src/test, `./gradlew testDebugUnitTest`).
- Updated `docs/01-product/03-features.md` and `02-decision-log.md` (onboarding switches verified on device).

## 2026-11-01 — Pull to refresh

- Code: `ui/BrowserScreen.kt` (SwipeRefreshLayout around the WebView), `browser/TabManager.kt` (`loading` state), `browser/CornTab.kt` (sets loading on page start/finish), dependency `androidx.swiperefreshlayout:1.2.0`.
- Updated `docs/01-product/03-features.md`, `01-prd.md`, `02-decision-log.md` (Round 10).

## 2026-11-01 — History, complete Flush, "All", background tabs

- Code: `corn-vault` (schema v2: optional Collection, history tables, upgrade from v1; 8 tests), `corn-ffi` (history + All exports), new `browser/CookieJar.kt`, `browser/DataClearing.kt`, `RestartActivity.kt`, `ui/HistoryScreen.kt`; `TabManager.kt` (real tab rules, settings-driven limit, unload, scroll restore, back-to-home), `CornTab.kt` (media hold, history hooks), `CornController.kt`, `CornData.kt`, `CornSettings.kt`, Settings/Sheets/Vault/Components/BrowserScreen UI.
- Updated `GLOSSARY.md` (All, Kept in memory, Unloaded, History, Delete browsing data; Unsorted removed), `docs/01-product/01-prd.md`, `02-decision-log.md` (Round 11), `03-features.md`.
- Updated `docs/02-technical/02-architecture.md`, `03-android.md` (tab rules), `05-flush-model.md` (how Android Flush works), `10-storage.md` (history, schema version).
- Updated `docs/03-planning/02-checklist.md` (new Phase 3 rows; backlog: history time ranges, Keep List local storage, 90-day history idea) and `docs/05-review/02-review-2026-11-01.md`.

## 2026-11-01 — History deletion: clear messages, link, no traces

- Code: `corn-vault` (`secure_delete`, write-ahead log emptied after every history delete; new no-trace test, 9 tests), `ui/HistoryScreen.kt` (Delete browsing data link, × toast), `ui/Sheets.kt` (history-only toasts).
- Updated `docs/01-product/03-features.md`, `02-decision-log.md` (Round 11), `docs/02-technical/10-storage.md`.

## 2026-11-01 — Keep List keeps IndexedDB; one-tap Keep List switch; Keep List doc

- Code: `browser/DataClearing.kt` (storage files deleted directly, Keep List IndexedDB folders kept; `deleteAllData` only in the fallback), `ui/Sheets.kt` (shield "Stay logged in after Flush"), `ui/SettingsScreen.kt` (Keep List caption).
- Added `docs/02-technical/11-keep-list.md` (with the code-is-the-source-of-truth section); indexed in `docs/README.md`.
- Updated `GLOSSARY.md` (Keep List), `docs/01-product/01-prd.md`, `02-decision-log.md`, `03-features.md`, `docs/02-technical/05-flush-model.md`, `10-storage.md`.
- Updated `docs/03-planning/02-checklist.md` (A, B, doc ticked), `docs/05-review/01-audit` and `02-review`.


## 2026-11-01 — Toasts sit just above the bottom bar or open sheet

- Code: `ui/CornApp.kt` (toast offset from measured heights), `ui/BrowserScreen.kt` and `ui/Components.kt` (report bottom-bar and sheet heights), `ui/Sheets.kt`, `CornController.kt` (`bottomBarHeightPx`, `sheetHeightPx`).
- No doc sections affected (UI spacing only). Verified on the emulator: browser (8 dp above the bar), open shield sheet (above the sheet), Settings (16 dp above the edge).

## 2026-11-01 — "Code is the source of truth" stated; sizes and phone test list updated

- Updated `AGENTS.md` (first rule), `README.md` (Docs and code section), `docs/README.md` (intro): the code is the source of truth.
- Updated `docs/03-planning/04-testing-on-a-phone.md` (APK sizes 8.7 / 28.1 MB; what to test: Keep List switch, History, Delete browsing data, background tabs, ↩).
- Updated sizes in `docs/02-technical/08-repo-structure.md`, `docs/05-review/01-audit-2026-11-01.md`, `docs/01-product/03-features.md`; features: toast placement.

