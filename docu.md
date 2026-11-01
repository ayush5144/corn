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

