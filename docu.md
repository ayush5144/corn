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

## 2026-11-01 — Fix blank pages when switching tabs; readable tab-count picker

- Code: `ui/BrowserScreen.kt` (one SwipeRefreshLayout per WebView via `key(live)`; it only measures its first child, so swapped-in pages stayed 0×0), `ui/SettingsScreen.kt` (dropdown uses Corn's dark menu colour; picker always visible, dimmed until chosen).
- Updated `docs/01-product/03-features.md` (Pull to refresh, Background tabs evidence).

## 2026-11-01 — QA round: light mode, background tabs, typed http fallback, error page, menu

- Code: `ui/Theme.kt` (dark/light palette, Accent), `MainActivity.kt` (applyTheme, system bars), `data/CornSettings.kt` (`theme`), `browser/CornTab.kt` (page theme, darkening, error state, snapshot, hold), `browser/TabManager.kt` (background loading, snapshot, pause/resume timers, recreateAll; rules comment), `CornController.kt`, `data/SearchEngines.kt` (typed → http), `ui/BrowserScreen.kt` (error page, snapshot overlay), `ui/SettingsScreen.kt` (Appearance), `ui/Sheets.kt` (menu b), `res/values/themes.xml`; unit test for typed addresses.
- Updated `docs/01-product/01-prd.md`, `02-decision-log.md` (Round 12), `03-features.md`, `docs/02-technical/03-android.md`, `docs/03-planning/02-checklist.md`.

## 2026-11-01 — Docs sync after the QA round

- Updated `docs/02-technical/10-storage.md` (settings now include theme and background tabs).
- Updated `docs/03-planning/04-testing-on-a-phone.md` (what to test: background-loaded tabs, light mode).
- Updated `docs/05-review/02-review-2026-11-01.md` (verified rows for this round) and `GLOSSARY.md` (Kept in memory: background-opened tabs).

## 2026-11-01 — Dev start/stop scripts

- Added `scripts/dev-start.sh` (emulator, optional debug install and test server) and `scripts/dev-stop.sh` (stops emulator, test servers, Gradle/Kotlin daemons, adb).
- Updated `docs/02-technical/08-repo-structure.md` (scripts list) and `docs/03-planning/04-testing-on-a-phone.md` (Emulator on the Mac).

## 2026-11-04 — Cookie-banner closer off by default

- Code: `data/CornSettings.kt` (`cookie_banners` default false), `ui/SettingsScreen.kt` (switch description).
- Updated `docs/01-product/01-prd.md` (feature + defaults), `03-features.md` (evidence: Reddit login), `02-decision-log.md` (Round 13).

## 2026-11-05 — Logo and name exploration mockups; missing basics planned

- Added `mockups/brand-compare.html`, `mockups/logos/` (Flush Kernel, Corny, Tab Cob) and `mockups/flush/` (Flush brand board); PNGs reduced to 512 px (originals kept outside the repo).
- Updated `docs/01-product/02-decision-log.md` (Round 14) and `docs/03-planning/02-checklist.md` (six new Phase 3 tasks; PiP and print in the backlog).

## 2026-11-05 — Uploads, downloads, site permissions, suggestions, find, text size; design agents saved

- Code: `browser/CornTab.kt` (file chooser, camera/mic/location requests, download listener, find, text zoom), `CornController.kt` (suggestions, find, uploads, permissions, downloads), `MainActivity.kt` (picker, permission and download-complete hooks), `data/CornSettings.kt` (`text_size`), `ui/BrowserScreen.kt` (find bar, suggestion list), `ui/Sheets.kt` (permission + download sheets, Find in page, shield permissions), `ui/SettingsScreen.kt` (reordered; Text size), `ui/Icons.kt`, `AndroidManifest.xml` (camera, microphone, location).
- Added `design-agents/` (exact agent briefs), `.claude/agents/corn-logo-designer.md` and `flush-brand-designer.md`, Flush concept pages (`mockups/flush/vortex.html`, `button.html`, `wink.html`).
- Updated `docs/01-product/01-prd.md`, `02-decision-log.md` (Round 14 status), `03-features.md`, `docs/03-planning/02-checklist.md`, `docs/02-technical/08-repo-structure.md`, `AGENTS.md`.

## 2026-11-05 — Flush Button glyph: water swirl

- Code: `ui/Icons.kt` (new `CornIcons.Flush`, three-arm vortex + drain, 18-unit visible height; toilet icon removed), used in `BrowserScreen.kt`, `Sheets.kt`, `TabsScreen.kt`, `OnboardingScreen.kt`.
- Updated `docs/01-product/01-prd.md` (Flush Button row), `02-decision-log.md` (Round 15), `03-features.md`.

## 2026-11-05 — Corny becomes the logo

- Assets: `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_foreground.png` (5 densities), new legacy `ic_launcher.png`, `res/values/colors.xml`, `res/drawable-nodpi/corn_logo.png` (transparent Corny); `brand/corny-logo-1024.png`, `brand/corny-logo-transparent-1024.png`.
- Updated `docs/01-product/02-decision-log.md` (Round 16) and `03-features.md` (Brand).

## 2026-11-05 — Password store and split screen decided

- Updated `docs/01-product/02-decision-log.md` (Round 17: password store architecture, fill gate, split screen) and `docs/03-planning/02-checklist.md` (two tasks, building).

## 2026-11-05 — Closing the last tab returns to the home page

- Code: `CornController.kt` (`closeTab`: closing the last tab leaves a fresh home page, returns to the browser, clears an old Flush one-liner), `ui/TabsScreen.kt` ("1 tab" singular; "Tabs" label when there are no groups).
- Verified on the emulator (founder report: closing the only tab seemed to do nothing).

## 2026-11-05 — Password store and split screen merged and verified

- Code (password store, agent branch): `core/crates/corn-vault` (schema v3, `logins`; migration fix so the v1 rebuild only runs below v2), `corn-ffi`, new `android/.../passwords/` (LoginCipher, LoginKeys, LoginMatching, LoginScript, Passwords, PasswordsScreen), hooks in CornTab/CornController/Sheets/BrowserScreen/VaultScreen/SettingsScreen/CornSettings/CornData; fix in `LoginScript.kt` (remember password fields through show-password toggles).
- Code (split screen, agent branch): `browser/SplitState.kt`, `TabManager.kt`, `CornController.kt`, `ui/BrowserScreen.kt` (divider 24 dp), `ui/Sheets.kt`, `ui/TabsScreen.kt`, `ui/Icons.kt`.
- Updated `docs/01-product/01-prd.md`, `02-decision-log.md`, `03-features.md`, `GLOSSARY.md` (Split screen), `docs/02-technical/03-android.md`, `05-flush-model.md`, `10-storage.md`, `docs/03-planning/02-checklist.md`.

## 2026-11-05 — Add passwords by hand; split-screen picker as tab cards

- Code: `passwords/Passwords.kt` (`addManual`), `ui/PasswordsUi.kt` (Add button and form), `ui/Sheets.kt` (split picker: tab cards + New tab), `ui/Components.kt` (`TabPreview` close button optional).
- Updated `docs/01-product/03-features.md` (Add by hand; split picker; address bar in split screen). Verified on the emulator.

## 2026-11-05 — Security doc; split-screen tests

- Added `docs/02-technical/12-security.md` (password encryption: data key + Android Keystore KEK, AES-256-GCM with site/field binding; PIN as gate; key loss and reset; filling rules; other data; known gaps incl. no PIN attempt limit); indexed in `docs/README.md`; linked from `10-storage.md` and `AGENTS.md`.
- Updated `docs/01-product/03-features.md` (split screen tests: bottom address bar, home-page search box in a pane, tap tab already on screen, group strip) and `02-decision-log.md` (Round 18).

## 2026-11-05 — Switch and focus-outline colours

- Code: `ui/Theme.kt` (`SwitchOn` leaf green #86C64A, `SwitchOff`, `SwitchDot` cream #FBF8F1, same in both themes), `ui/Components.kt` (`CornSwitch`), `ui/BrowserScreen.kt` (split focus outline: corn yellow, 3 dp).
- Founder's choice: switches in Corny's leaf green with a cream dot (not dark green with a black dot, not white); split focus outline in the Flush button's yellow. Seen on the emulator.

## 2026-11-05 — Password store code review

- Added `docs/05-review/03-password-review-2026-11-05.md` (what's right, 7 findings with severities, what's not covered); indexed in `docs/README.md`.
- Updated `docs/02-technical/12-security.md` (Known gaps links the review), `docs/03-planning/02-checklist.md` (two follow-up tasks), `docs/01-product/02-decision-log.md` (Round 18 note).

## 2026-11-05 — Clipboard cleared on leaving Corn; split-screen "New tab in group" fix

- Code: `passwords/Passwords.kt` (copy-pending flag; `clearCopiedPassword`, `clearCopyLeftOver`; toast text), `CornController.kt` (clear on background/foreground; `newTabInGroup` in split screen), `browser/TabManager.kt` (`addToGroupInBackground`).
- Updated `docs/05-review/03-password-review-2026-11-05.md` (status per finding), `docs/02-technical/12-security.md`, `docs/03-planning/02-checklist.md`, `docs/01-product/02-decision-log.md` (Round 20), `03-features.md`.

## 2026-11-05 — Pinned main buttons; closed tabs leave no login notes

- Code: `ui/Components.kt` (sheet container no longer scrolls as one block; `SheetScroll`), `ui/Sheets.kt` (group, Save, split picker pin their buttons; other sheets scroll as before), `ui/TabsScreen.kt` (Flush bar pinned at the bottom), `browser/TabManager.kt` + `CornTab.kt` (`onTabClosed`), `CornController.kt`, `passwords/Passwords.kt` (`forgetTab`).
- Updated `docs/01-product/03-features.md`, `02-decision-log.md`, `docs/03-planning/02-checklist.md`, `docs/05-review/03-password-review-2026-11-05.md` (#3 fixed).

## 2026-11-05 — Second site check before filling (review #2)

- Code: `passwords/Passwords.kt` (`fill()` refuses unless the tab's current page is the login's site, main frames too; commented).
- Updated `docs/02-technical/12-security.md` (two locks), `docs/05-review/03-password-review-2026-11-05.md` (#2 fixed), `docs/05-review/01-audit-2026-11-01.md` (password and history storage, password logs), `docs/03-planning/02-checklist.md`, `docs/01-product/02-decision-log.md`.


## 2026-11-05 — QA report: where Corn fails (no code changes)

- Added `docs/05-review/04-qa-where-corn-fails-2026-11-05.md`: 8 findings with emulator data, cause in code and fix (tapped new-tab links blocked as pop-ups, forced page darkening, YouTube ads without scriptlets, fullscreen ignores split panes, tel/mailto/sms/geo/intent links dead, no image long-press menu, blocked pop-up looks opened, one unreproduced fullscreen hang), what passed, fix order.
- Updated `docs/README.md` (Review index row 04).

## 2026-11-05 — QA fixes 1–7, scriptlets, QA suite, collection sheet, nav arrow

- Code: `browser/WebRules.kt` (WindowPolicy, ExternalLinks, LinkTitles) + tests; `CornTab.kt` (tap rule, onCloseWindow, app links, theme, long-press); `CornController.kt` (ExternalApp/Image/NewCollection sheets, fullscreen per pane); `ScriptletInjector.kt`, `CornEngine.kt`, `corn-block` scriptlets/preprocess (agent).
- Added the QA suite: `qa/pages/`, `scripts/qa/`, `scripts/qa-run.sh`, `qa/README.md` (17/17 on the new build).
- Updated `docs/01-product/02-decision-log.md` (Round 21), `03-features.md` (pop-up guard, app links, image menu, light/dark mode).
- Updated `docs/03-planning/02-checklist.md` (QA fixes ticked with evidence; delete a collection added).
- Updated `docs/05-review/04-qa-where-corn-fails-2026-11-05.md` (Status section); `AGENTS.md` (QA suite row).
- Scriptlet agent updated `docs/02-technical/09-filter-lists.md`, `10-storage.md`, `docs/04-reference/02-open-source.md`.

## 2026-11-12 — Back on Corn (branch `corn`); one sentence per line

- Added `ui/Copy.kt` (`oneSentencePerLine`) + `CopyTest` (6 tests); used by the toast (`ui/Components.kt`) and the after-Flush line (`ui/BrowserScreen.kt`, each line centred).
- Updated `docs/01-product/02-decision-log.md` (Round 22: Flush rename tried on `main`, parked; Corn continues on `corn`).

## 2026-11-12 — Long-press menus on search results; launch checklist

- Code: `browser/WebRules.kt` (`ImageRules.isPicture`, tested), `browser/CornTab.kt` (picture check by shown size, heading in link titles), `ui/Sheets.kt` (`LinkRows` shared by link and image menus; Share link added; image actions only for real pictures).
- QA: `qa/pages/links.html` search-result block; check `search-result-link-menu`; `image-long-press-menu` also wants the group action.
- Updated `docs/03-planning/02-checklist.md` (new "Before launch" section) and `docs/01-product/03-features.md` (Long-press menus).

## 2026-11-12 — Kernel Safe Vault icons

- `ui/Icons.kt`: Vault / VaultAdd / VaultSaved are the Kernel Safe (corner + and corner ✓, door outline open around the badge); per-path stroke width ("1.6|M…"); old safe paths removed.
- Updated `docs/01-product/02-decision-log.md` (Round 23).

## 2026-11-13 — No age gate; releasing and updates

- `ui/OnboardingScreen.kt`: button "Start browsing" (no 18+ gate). `.gitignore`: signing key files.
- Added `docs/03-planning/05-releasing-and-updates.md` (signing keys, the debug key, release channels, updates, the plan); indexed in `docs/README.md`.
- Updated decision log (Round 24), PRD (first-launch row), features (onboarding), roadmap (step 6), checklist (onboarding row; release key, version numbers, GitHub testers + Obtainium, Play App Signing, content rating, in-app update check, F-Droid).

## 2026-11-13 — Release tooling; website and desktop on the checklist

- `android/app/build.gradle.kts`: release signing from `~/.gradle/gradle.properties` or environment variables (unsigned when absent). Added `scripts/create-release-key.sh`, `scripts/release.sh`.
- Updated `docs/03-planning/05-releasing-and-updates.md` ("Doing a release") and `docs/03-planning/02-checklist.md` (release tooling done; website and desktop version added).

## 2026-11-13 — Release key created and signing verified

- The founder created `~/corn-release.jks`; a release build is signed `CN=Corn, O=Corn, C=IN` (SHA-256 e4b31db3…d3930ad). No repo code changed.
- Updated `docs/03-planning/02-checklist.md` (new done row; backup and first signed update still open) and `docs/03-planning/05-releasing-and-updates.md` (key created, public fingerprint, how to check an APK).
- Updated `AGENTS.md` (where releasing lives; key never in the repo).

## 2026-11-13 — First signed release 0.2.1; main is Corn again

- `scripts/release.sh 0.2.1` (by the founder): versionCode 2, tag `v0.2.1`, GitHub pre-release with the signed APK.
- Branches: Flush saved as `flush` (ad2891d); `main` now points at Corn (509a18e).
- Updated `docs/03-planning/02-checklist.md` (first signed release done; phone reinstall open), `docs/03-planning/05-releasing-and-updates.md` (Releases so far, branches), decision log Round 22 (branches).

## 2026-11-13 — Vault export (links and passwords)

- Rust `corn-vault`: database version 4 (`logins.password_changed_at`; upgrading from 3 adds it and sets it to `created_at`); `update_login` takes `password_changed`. New `src/export.rs`: links → HTML bookmarks ("Corn Vault" folder, a folder per Collection, `ADD_DATE` in seconds, `TAGS`, HTML escaping), links → CSV (`title,url,collection,tags,saved_at`, formula guard), passwords → Chrome CSV (`name,url,username,password,note`, no formula guard); RFC 4180 quoting, `\r\n`, own UTC date conversion. `Store::export_links_html/csv`.
- `corn-ffi`: `export_links_html`, `export_links_csv`, `export_passwords_csv(rows)`, `Login.password_changed_at`, `update_login(…, password_changed)`.
- Android: Vault → ⋮ → Export links (HTML recommended / CSV); Passwords → Export passwords (PIN every time, plain-text warning with checkbox, then "Save to…"); `ACTION_CREATE_DOCUMENT` launcher in `MainActivity`, `CornController.chooseExportPlace/exportLinks`, `Passwords.export`, `data/VaultExport.kt` (file names) + JVM test. Every `updateLogin` call says whether the password changed.
- Updated `docs/02-technical/13-vault-export.md` (where the code adds detail), `12-security.md` (Exporting), `10-storage.md` (version 4), `docs/01-product/03-features.md`, `01-prd.md`, checklist.

## 2026-11-13 — Export sheet and icons polish

- `ui/Icons.kt`: new `Export` icon (arrow up out of the tray) for Export links and Export passwords; Download stays for downloads.
- `ui/Sheets.kt`: Export links is a single choice (radio options, HTML pre-selected) with Cancel and a corn-yellow Export.
- `ui/Components.kt`: the PIN pad's erase key has no key background.
- Updated `docs/02-technical/13-vault-export.md` (where-in-the-app row).
