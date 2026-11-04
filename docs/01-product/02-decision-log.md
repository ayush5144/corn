# 02 — Decision Log

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 01:43 IST

Every product question asked and the founder's answer, in order. Settled answers flow into [01-prd.md](01-prd.md). New rounds are appended at the bottom.

**Format:** question → recommendation given → founder's answer → status.

---

## Round 0 — 2026-11-01

### Q0.1 Who and where?
- **Recommended:** Android-heavy markets, short-to-medium sessions.
- **Answer:** Not answered yet.
- **Status:** 🟡 Open → carried to Round 1.

### Q0.2 What happens when the app is closed?
- **Recommended:** Flush after a short timer the user can change, with the Flush button always one tap away.
- **Answer:** The user decides. All options exist:
  - Flush immediately via a button (a power or toilet/flush button, like DuckDuckGo's Fire Button).
  - "Flush after X minutes" / "flush after X minutes of inactivity" as a setting.
  - "Keep the session" as a setting.
- **Interpretation:** "Keep the session until the user encrypts" was read as *keep the session until the user flushes*. Confirm in Round 1.
- **Status:** ✅ Settled (options). 🟡 Default and button design open.

### Q0.3 What goes in the Vault for v1?
- **Recommended:** Links, titles, tags and optional thumbnails; files later.
- **Answer:** Links and titles; tags if needed; thumbnails to be discussed. Users must be able to group items.
- **Interpretation:** "walls" in the voice note = **Vault**.
- **Status:** ✅ Links + titles + grouping. 🟡 Tags detail, thumbnails, files.

### Q0.4 How discreet?
- **Recommended:** PIN plus blank recents in v1; disguised icon later.
- **Answer:** PIN to open, as a setting. Black screen in recent apps (liked). Notifications only appear while the app is open; defaults plus settings.
- **Status:** ✅ Settled. 🟡 Disguised icon not addressed; notification detail open.

### Q0.5 Default search engine?
- **Recommended:** DuckDuckGo with safe search off; user can change.
- **Answer:** DuckDuckGo, Google, etc. Multiple engines, switchable from a single dropdown in the search bar.
- **Status:** ✅ Settled. 🟡 Out-of-box default and safe-search handling open.

### Q0.6 How does Corn make money?
- **Answer:** It doesn't, for now. Free browser, funded by donations. Idea: a paid Cloud Vault at $3–5/month.
- **Status:** ✅ Free + donations. 🟡 Cloud Vault is an idea, not v1.

### Q0.7 What's out of v1?
- **Recommended:** Desktop, iPhone, sync, file downloads and extensions all out.
- **Answer:** Extensions are out. "Other things should be in."
- **Status:** ✅ Extensions out. 🟡 Conflicts with Android-first; needs splitting in Round 1.

### Q0.8 "What do you mean by sync?"
- **Answer given:** Sync means the same Vault (and possibly settings) on multiple devices, which requires storing it on a server. The founder's Cloud Vault idea is the same thing.
- **Status:** 🟡 Folded into the Cloud Vault question.

---

## Round 1 — 2026-11-01

### Q1 Where are your users?
- **Recommended:** Android-heavy markets; Android first.
- **Answer:** Mobile first: Android **and** iPhone, not only Android. Build on Android first, but other platforms are definite.
- **Status:** ✅ Android built first; iPhone committed next.

### Q2 Split "everything else is in" into versions
- **Recommended:** v1 = Android only with every PRD feature.
- **Answer:** Android only while building; other platforms definitely later.
- **Status:** ✅ v1 = Android.

### Q3 Why would someone switch to Corn?
- **Recommended:** Flush as the centre, a private Vault, blocking tuned for adult sites.
- **Answer:** Corn is cool, funny and modern. It helps you save links, organise your stuff and not get lost. DuckDuckGo has no tab grouping: in Corn you open a link into a new Tab Group, browse, swipe or click around, see previews and switch instantly.
- **Status:** ✅ Differentiators: Tab Groups + Vault/Collections + Flush on your terms + blocking + personality.

### Q4 What does "closed" mean?
- **Recommended:** Leaving Corn, with a 30-second grace period.
- **Answer:** When the app is closed, the user chooses how long until it flushes, or never.
- **Status:** ✅ "Leaving Corn" trigger with a user-chosen delay or never.

### Q5 Inactivity while a video plays
- **Recommended:** A playing video counts as activity.
- **Answer:** Yes. If a video is playing, the session stays active.
- **Status:** ✅

### Q6 Out-of-box default
- **Recommended:** Flush when you leave Corn (30-second grace).
- **Answer:** **Rejected.** Default is keeping the session until the user flushes. Keep three preset choices.
- **Status:** ✅ Default = Kept Session. 🟡 What the three presets are (Round 2).

### Q7 Kept Session protection
- **Recommended:** Android sandbox + App Lock; Corn encrypts only the Vault.
- **Answer:** Asked whether to switch engines if Corn can't encrypt cookies and site data, and what to do instead.
- **Response given:** Don't switch engines; no engine solves this (see "Q7 follow-up" below).
- **Status:** 🟡 Recommendation pending acceptance (Round 2).

### Q8 Flush Button design
- **Recommended:** Flush icon in the toolbar, one tap, no confirmation.
- **Answer:** A toilet/flush button, not a power button. Styling is flexible.
- **Status:** ✅ Toilet/flush button; one tap. 🟡 Visual styling.

### Q9 Vault groups
- **Recommended:** "Collections", one level, one Collection per item, tags cut across.
- **Answer:** "Collections" or "Vault groups"; the recommendation was right.
- **Status:** ✅ **Collections** (glossary picks one term).

### Q10 Vault thumbnails
- **Recommended:** Screenshot at save time, encrypted, can be turned off.
- **Answer:** Optional: screenshot, no thumbnails, or a default placeholder.
- **Status:** ✅ Optional. 🟡 Which is the default.

### Q11 Vault lock vs App Lock
- **Answer:** Vault offers PIN or biometrics by default; App Lock is optional.
- **Status:** ✅

### Q12 Notifications
- **Answer:** Fine with no notifications while closed: "actually good."
- **Status:** ✅

### Q13 Default search engine
- **Status:** ✅ Accepted (DuckDuckGo default, dropdown switcher). Carried from Round 0.

### Q14 File downloads in v1
- **Answer:** Not a priority. Skip for now, add to the checklist.
- **Status:** ⏭️ Backlog.

### Q15 Keep List
- **Recommended:** v1.1.
- **Answer:** **Rejected.** Needed in v1: a list of sites that stay logged in across Flushes, or saved passwords that survive a Flush.
- **Status:** ✅ v1. 🟡 Design (Round 2).

### Q16 Cloud Vault
- **Answer:** Not v1; add to the checklist.
- **Status:** ⏭️ Backlog.

### Q17 18+ gate and disguised icon
- **Status:** ✅ Accepted by silence; flag if wrong.

### New: Tab Groups
- **Answer (from Q3):** Open links in a new Tab Group, swipe, previews, instant switching.
- **Status:** ✅ Core v1 feature. 🟡 Details (Round 2).

### New: Checklist
- **Answer:** Create a checklist with tasks and goals assigned properly.
- **Status:** ✅ Created: [../03-planning/02-checklist.md](../03-planning/02-checklist.md).

### Q7 follow-up — "Should we switch engines so Corn can encrypt cookies?"

**No.** GeckoView (Firefox) and a Brave fork store cookies the same way: in the app's private folder, without an extra app-level key. Chrome, Brave and DuckDuckGo on Android all rely on the same protection. What matters is *who* could read the data:

| Threat | What protects Kept Session data |
|---|---|
| Someone picks up your unlocked phone and opens Corn | **App Lock** (optional), Privacy Screen |
| Another app on the phone | Android **app sandbox**: apps can't read each other's files |
| Phone stolen or seized while locked | Android **file-based encryption** (mandatory on modern phones), tied to your phone's lock |
| Phone / Google cloud backup | **Corn opts out of backups** entirely, so Session data is never copied off the phone |
| Rooted or hacked phone | Out of scope; no browser can protect against this |

**Proposed:** (1) don't switch engines; (2) Corn encrypts everything *it* stores about the Session (tab list, Tab Groups, URLs, previews) with its own key, the way the Vault is encrypted; (3) exclude all app data from backups; (4) when the user turns on Kept Session, suggest turning on App Lock.

---

## Round 2 — 2026-11-01

### Q1 Protecting the Kept Session
- **Recommended:** Keep the engine; Corn encrypts its own Session records; backups off; suggest App Lock.
- **Answer:** Not answered.
- **Status:** 🟡 Carried to Round 3.

### Q2 Auto-Flush presets
- **Recommended:** When I leave / 5 min after / 30 min after; separate inactivity switch (10/30/60 min).
- **Answer:** Not answered.
- **Status:** 🟡 Carried to Round 3.

### Q3 Keep List vs saved passwords
- **Recommended:** Keep List + Android autofill in v1; Corn's own password store later.
- **Answer:** Both (a) Keep List and (b) Corn's own password store, in v1 or v1.1.
- **Status:** ✅ Both are in. 🟡 Whether the password store lands in v1 or v1.1. Android autofill support not chosen.

### Q4 Tab Groups and Flush
- **Recommended:** Flush wipes every Tab Group; "Save group to Vault" turns a group into a Collection.
- **Answer:** "I don't know."
- **Status:** 🟡 Carried to Round 3, with the clarified Tab Group model.

### Q5 How Tab Groups work
- **Recommended:** Long-press menu, auto-named groups, rows of previews, swipe on the toolbar.
- **Answer:** Shared a Chrome Android screenshot instead. A Tab Group is a **strip at the bottom of the screen** with one round site icon per tab in the group. Tap an icon to switch instantly. `^` expands the group, `+` adds a tab, `×` closes the current tab.
- **Status:** ✅ Chrome-style Tab Group Strip. 🟡 Naming/colours (Round 3).

### Q6 Default thumbnail in the Vault
- **Answer:** Thumbnails are a later feature, not v1. v1 Vault = links, titles, Collections, optional tags.
- **Status:** ⏭️ Thumbnails to backlog.

### Q7 Flush Button placement and feedback
- **Answer:** Bottom toolbar (or search bar), ideally **bottom-right or second from the right** of the bottom toolbar.
- **Status:** ✅ Placement. 🟡 Animation/haptics/sound not addressed.

### Q8 Mockup tool
- **Answer:** Clickable web prototype. Plain HTML preferred, Next.js static export if necessary. Mobile viewport size.
- **Status:** ✅

---

## Round 3 — 2026-11-01

### Q1 Protecting the Kept Session
- **Answer:** Didn't follow the question; "there won't be any cloud backups."
- **Clarified:** Android backs up app data to Google Drive automatically unless the app opts out. Corn opts out. Corn also encrypts its own Session records (tabs, Tab Groups, URLs); this is an engineering default with no visible product change.
- **Status:** ✅ No backups of any kind. ✅ Corn-encrypted Session records.

### Q2 Auto-Flush presets
- **Status:** ✅ Accepted: when I leave Corn / 5 min after / 30 min after; separate inactivity switch (10/30/60 min); a playing video counts as activity.

### Q3 Tab Groups on Flush
- **Answer:** Yes, Flush wipes all groups. Asked where "Save to Vault" sits; it should be visible on pages.
- **Status:** ✅ Flush wipes all Tab Groups. ✅ Save to Vault is visible on every page (see Q7). "Save group to Vault" lives in the group's `^` grid.

### Q4 Password store timing
- **Answer:** Not answered.
- **Status:** 🟡 Tentatively v1.1 (recommendation, unopposed). Revisit before the v1 build.

### Q5 Address bar position
- **Answer:** Top by default; bottom as an adjustable setting.
- **Status:** ✅ Top default; "address bar at bottom" is a v1 setting.

### Q6 Bottom toolbar
- **Answer:** Right for now. The Vault icon could sit in the search bar.
- **Status:** ✅ Back (hold for Forward) · Vault · Tabs · Flush · Menu. ✅ A Vault save icon also sits in the address bar.

### Q7 Saving to the Vault
- **Answer:** No star in the address bar. A star means bookmarks/favourites, and the Vault does the same job.
- **Status:** ✅ **Corn has no separate bookmarks; the Vault replaces them.** The address bar shows a Vault save icon (not a star) on every page; tapping it opens the save sheet (Collection + optional tags).

### Q8 Flush feedback
- **Status:** ✅ Quick swirl animation, vibration, funny one-liner on the empty page. Sound setting off by default.

### Q9 Look and feel
- **Answer:** Unsure; decide after seeing the prototype.
- **Status:** 🟡 Prototype starts dark + corn yellow + leaf green with the existing logo; founder reacts.

### Q10 Prototype scope
- **Answer:** 18+ onboarding probably not needed, "but sure."
- **Status:** ✅ All screens, with a single lightweight 18+ confirmation.

### Process
- Founder asked for no more questions for now. Remaining 🟡 items get decided by reacting to the prototype.

---

## Round 4 — after the prototype

### Address bar order
- **Founder asked:** Search engine on the left and shield on the right, or keep the shield left because right is easier for right-handers?
- **Recommended:** Swap them. The address bar sits at the top, which is hard to reach with either side of the thumb, so handedness barely matters there. What matters is that the most-used control sits at the far right: the Vault save icon. The shield is rarely touched, so it can sit next to it.
- **Status:** ✅ Search Switcher left, shield right, Vault icon far right. Applied to the prototype.

### Licence for Corn's own code
- **Founder asked:** What licence should Corn be published under?
- **Recommended:** **MPL-2.0** for the app and Rust core (same as Brave, Firefox and adblock-rust; App Store compatible). "Corn" name and logo stay trademarks, not covered by the licence. Server code for a future Cloud Vault decided separately.
- **Answer:** Keep it MPL-2.0.
- **Status:** ✅ MPL-2.0. `LICENSE` and `TRADEMARKS.md` added. Repository made public by the founder on 2026-11-01. Legal review before public launch.

### Google sign-in inside WebView
- **Issue:** Google blocks account sign-in from embedded WebViews ("disallowed_useragent").
- **How DuckDuckGo handles it (read from duckduckgo/Android source):** it rebuilds the user agent to look like regular Chrome (reduced Chrome UA, no WebView markers, Chrome version shortened to `NNN.0.0.0`), keeps per-site exceptions, and allows third-party cookies only on `accounts.google.com` flows.
- **Status:** ✅ Corn does the same. Tested in the spike.

### WebView vs GeckoView (final)
- **Founder asked:** Keep WebView or switch to GeckoView?
- **Answer given:** Keep WebView. GeckoView stays plan B with one trigger: the spike shows WebView blocking clearly worse than Brave on the founder's test sites. No more engine debate before the spike results.
- **Status:** ✅ WebView. Re-confirmed.

### How many tabs stay live
- **Founder asked:** How many background tabs do Chrome and Brave keep live? Suggested one live tab (the current one) is enough.
- **Facts found:** Chrome on Android (and Brave, built on Chromium) has no fixed count. Background tabs stay in memory until the phone runs low, then Chrome kills them "aggressively" and reloads them when tapped. The spike measured ~1 GB with 10 live tabs.
- **Recommended:** 1 active tab + up to 2 recently used tabs paused (scripts and media stopped, still in memory) + every other tab frozen (preview screenshot + saved state, reloads on tap). Under memory pressure, drop to 1 active. One live tab alone would make every Tab Group Strip switch a 1–3 s reload, which conflicts with "instant switching".
- **Answer:** Go with 1 active + 2 paused for now (paused tabs don't play media). Come back to the number later.
- **Status:** ✅ 1 active + 2 paused. Revisit task added to the checklist.

### Spike scope vs mockup
- **Founder asked:** Where are settings, tab previews and the other mockup features?
- **Answer given:** Only the engine proof is built (blocking, basic tabs, address bar, fullscreen video). Every mockup feature is still to build, after planning. Founder wants to plan before building anything more.
- **Status:** ✅ No new building until the plan is agreed.

## Round 5 — 2026-11-01

### Flush confirmation
- **Answer:** Add a Settings toggle for a Flush confirmation.
- **Status:** ✅ Toggle in Settings, **off by default** (one tap stays the default).

### What the Keep List is
- **Founder asked:** Is the Keep List history or saved passwords?
- **Answer given:** Neither. It's the list of sites whose login cookies survive a Flush, so the user stays logged in to them. Passwords are the separate password store (v1.1).
- **Status:** ✅ Definition confirmed in the glossary and PRD.

### Favicons in the Tab Group Strip
- **Founder asked:** Shouldn't the strip show favicons instead of letters?
- **Answer given:** Yes. Letters in the mockup were stand-ins; the app shows each site's real favicon, with a coloured letter only as a fallback.
- **Status:** ✅

### Where data is stored, and SQLCipher
- **Founder asked:** What does "Unsorted" mean, and what database do we use?
- **Answer given:** "Unsorted" is the default Collection, not a location. Proposed SQLCipher (encrypted SQLite) for the Vault, Keep List and session, and plain app settings for toggles.
- **Founder decision:** Use **plain SQLite** instead of SQLCipher, after weighing it: SQLCipher only adds protection against rooted phones and forensic tools; Android's sandbox and phone encryption already protect app-private files in normal use, the same level as Chrome's own bookmarks.
- **Consequence:** The Vault and Kept Session are no longer separately encrypted by Corn (this reverses "Corn-encrypted Session records" from Round 3). The Vault PIN gates access in the app. Switching back later is a one-line change in `core/Cargo.toml` plus a key.
- **Status:** ✅ Plain SQLite. Details: [../02-technical/10-storage.md](../02-technical/10-storage.md).

### Build the mockup
- **Answer:** Build every feature from the mockup into the Android app, matching its UI and UX. Don't assume things; brief the founder on the changes.
- **Status:** ✅ In progress.

### Fingerprint unlock
- **Answer:** Skip fingerprint unlock for now.
- **Status:** ⏭️ Backlog. The Vault and App Lock use a 4-digit PIN (stored as a salted PBKDF2 hash).

### Rendering differences on DuckDuckGo
- **Founder asked:** Is WebView rendering correctly? DuckDuckGo looked different from Chrome.
- **Findings:** (1) "Get it on Google Play" is DuckDuckGo's mobile page; Chrome on the same emulator shows it too. (2) Dark vs light is the app's dark theme vs the emulator's light mode. (3) Black and blank areas came from a Corn bug (an always-on animation layer around the page made WebView skip painting) plus a page-process crash on a 2 GB emulator.
- **Founder decision:** Don't make site-specific fixes; if filter rules hide something on a site, leave it for now.
- **Status:** ✅ General fixes only: animation layer applied only during Flush; Corn now recovers from page crashes with a reload card.

## Round 6 — 2026-11-01

### Menu: new Tab Group
- **Answer:** When the current tab isn't in a group, the menu offers "New tab group".
- **Status:** ✅ Built. The group starts as "Group N" and takes the first site's name.

### Search engine icons
- **Answer:** Use favicons (or emoji) instead of letters.
- **Status:** ✅ Favicons, bundled in the app (never fetched at runtime).

### Shield sheet: Advanced
- **Answer:** Add "Advanced" with separate toggles for ads, trackers and pop-ups, but only if possible without changing the blocker code.
- **How:** Possible without touching adblock-rust: Corn runs two unmodified engines, one on EasyList (ads), one on EasyPrivacy (trackers); pop-ups and redirects are Corn's own guard. Limitation: the lists overlap slightly.
- **Status:** ✅ Built, per site.

### "new group" shown twice
- **Cause:** the grey hint meant "this creates a new group" but read as a duplicate.
- **Status:** ✅ Fixed: "Open in new tab group" (no group yet) vs "Open in new tab in group · <name>".

### Toilet button
- **Answer:** Founder likes it as is.
- **Status:** ✅ No change.

### Home page stats
- **Answer:** Show totals in a Brave-like card, but only two numbers: trackers & ads blocked, pop-ups stopped. No who-blocked-what, no logs. Numbers must be real, not estimated.
- **Status:** ✅ Built. Two counters only (no URLs, sites or timestamps). Verified with a test page containing exactly 5 ad/tracker requests and 1 pop-up: Corn showed 5 and 1, in both debug and release builds. Bandwidth/time-saved estimates deliberately left out.

### Efficiency and privacy audit
- **Findings and fixes:** crash log contained the page URL (removed); `http://` sites couldn't load at all (fixed); scripted pop-ups were blocked but not counted (fixed); last-activity time and totals were written to disk on every tap/batch (now in memory, written when leaving Corn); inactivity timer woke the app every 30 s even when off (now only when on); unused session counter and engine status removed; tab previews halved in memory (~0.25 MB each); WebView usage metrics to Google opted out (`MetricsOptOut`).
- **Release build:** R8 shrinking + phones-only native library: 6.0 MB (debug 21.3 MB).
- **Open:** Google Safe Browsing in WebView is on by default (it checks hashed URL prefixes with Google to warn about malware/phishing). Keep it or turn it off is a founder decision.

## Round 7 — 2026-11-01

### Privacy features to build
- **Answer:** Build filter-list refresh, HTTPS upgrade, Vault item delete, cookie-banner closing, offline starter list, and link cleaning (easy). Fingerprinting protection: explanation only for now. Password store v1.1; fingerprint unlock later.
- **Status:** ✅ All six built. Verified on device: starter list + refresh, link cleaning. Unit-tested: HTTPS upgrade, link cleaning. Not yet seen on device: cookie-banner closing, Vault delete.

### Search engine name in the search box
- **Answer:** Don't name the engine in the search box; the favicon already shows it.
- **Status:** ✅ New-tab placeholder is "Search or type a site".

### Do other browsers have password stores and download vaults?
- **Findings:** Chrome, Brave and DuckDuckGo all have built-in password managers. Download "vaults" are rarer: Aloha Browser offers passcode-locked folders for downloads (listed as a Premium feature on its site).
- **Status:** ✅ Informational. Password store stays v1.1; downloads stay in the backlog.

## Round 8 — 2026-11-01

### Saving to a Collection went to Unsorted
- **Cause:** a new Collection typed in the save sheet was only created when the keyboard's Done key was pressed; tapping Save without it fell back to Unsorted.
- **Status:** ✅ Fixed: Save creates (or reuses) the typed Collection; a tag still in the box is also kept.

### Search engine address in the address bar
- **Founder asked:** On a results page, why show `https://duckduckgo.com/?q=…`?
- **Findings:** Firefox has "Show search terms in the address bar on results pages instead of URL"; Safari shows only the site name.
- **Status:** ✅ On a built-in engine's results page the bar shows just the search words; tapping lets you edit them.
- **Reversed (same day):** Founder: keep showing the full URL, as Brave does. The search-words display was removed.

### Link cleaning in other browsers
- **Findings:** Brave (on by default, query string filter), Firefox 102+ (Strict mode), Safari iOS 17+ (Link Tracking Protection, ~25 click IDs).
- **Status:** ✅ Informational.

### Features file
- **Answer:** A features file listing every built feature, its benefit and data-backed evidence, no assumptions.
- **Status:** ✅ [03-features.md](03-features.md), with evidence levels (verified / tested / built).

## Round 9 — 2026-11-01

### Safe Browsing and Safe Search switches
- **Answer:** Add both switches to Settings. Safe Search keeps a switch even though engines are asked for "off".
- **Status:** ✅ Built. Safe Browsing: on by default, Settings → Privacy, one onboarding line. Safe Search: off by default, Settings → Search; flips each engine's URL parameter between off and strict (Startpage has none). Search URLs unit-tested for all 5 engines.
- **Follow-up:** Onboarding shows both settings with their current state (On/Off) and a switch each, instead of a text line. ✅ Built and verified on a fresh install (switch flips the label and saves the setting).

## Round 10 — 2026-11-01

### Pull to refresh
- **Answer:** Add pull-to-refresh (drag down at the top of a page to reload), as other mobile browsers do. Only this for now.
- **Status:** ✅ Built and verified with the test server. Chrome has had it since v41 (2015), Safari since iOS 15, Firefox for Android has it as an option (off by default since 2023).

## Round 11 — 2026-11-01

### History
- **Answer:** Chrome/Brave-style history: recorded on the phone only, cleared by Flush, "Keep history" switch (on by default). History screen: search, grouped by day, tap to open, delete one entry, delete a site from history (history only), clear all.
- **Research:** DuckDuckGo's Android app has **no History screen and no time-range clearing**; it records history only for address-bar suggestions. Eddy (Kotlin + WebView) records each finished page, skips the same address within 5 s, offers time ranges for history only, and deletes all cookies at once.
- **Status:** ✅ Built. Verified: a search is recorded once with its title (DuckDuckGo changes its address several times while loading; same page within 5 s in one tab = one visit; before the fix one search made 6 entries).

### Deleting browsing data: only what is fully possible
- **Answer:** "Delete browsing data" deletes everything in each chosen category. **No time ranges and no per-site cookie or storage deletion for now**: only controls that work completely. Time ranges for history go on the checklist for later.
- **Data:** `WebView.clearCache()` "is per-application, so this will clear the cache for all WebViews" (Android docs): the cache can't be deleted by time. `WebStorage.deleteOrigin()` covers Application Cache and Web SQL only; measured: a site's local storage survived it.

### Flush deletes completely, Keep List cookies exactly
- **Answer:** Flush must never be an incomplete deletion, and Keep List sites stay logged in.
- **How:** Corn deletes cookies by editing WebView's cookie database directly, as DuckDuckGo does (`SQLCookieRemover`): every cookie except Keep List sites' goes, with Keep List cookies untouched (before, they were re-created and lost their expiry and HttpOnly flags). Site storage of **every** site is deleted (`deleteAllData` plus WebView's storage files), then Corn restarts its process (about 0.3 s) so nothing stays in memory. In the background (Auto-Flush after leaving), Android doesn't allow a relaunch, so the process just ends.
- **Status:** ✅ Verified on the emulator: Keep List site kept its cookie; the other site lost its cookie; local storage and IndexedDB gone for both; history 13 visits → 0.
- **Later:** keep Keep List sites' local storage too (DuckDuckGo edits WebView's LevelDB files with a LevelDB library). See checklist.

### History deletion: say what it does, link to the rest
- **Answer:** History deletions say they're history only (× → "Removed from history"; site → "… Cookies stay."; clear all → "History cleared. Cookies and site data stay."). The History screen links to Delete browsing data (as Chrome and Brave do). With Keep history off, nothing is stored.
- **Added:** deleted history leaves no trace in the database files (`secure_delete` + emptying the write-ahead log); without it, a cleared page title was still readable in `corn.db-wal` (unit test).
- **Open:** a Keep history switch on the History screen too (it's in Settings now).

### Keep List stays; A + B now, C + D later
- **Answer:** Keep the Keep List (allowing a site's cookies is how you stay signed in; the password store is separate and stays on the checklist). Build **A** (keep Keep List sites' IndexedDB) and **B** ("Stay logged in after Flush" switch in the shield sheet); **C** (local storage via LevelDB) and **D** (prompt after login) later if needed.
- **Status:** ✅ A and B built and verified on the emulator. Doc: [11-keep-list](../02-technical/11-keep-list.md).

### Round 12 — QA fixes, light mode, background tabs (2026-11-01)
- **Blank group tabs** (founder report): not slow loading. The server got the page in the same second; the page view was 0×0. Cause: SwipeRefreshLayout (pull to refresh) only measures the first child it saw. Fixed with one container per tab.
- **Tab-count dropdown**: worked but invisible (light menu, light text). Fixed; the picker is always visible.
- **Menu (founder chose b):** "New tab group" groups the current tab with a new tab beside it; inside a group, "New tab in this group".
- **Light mode** (founder): Settings → Appearance, Dark default; pages follow the theme. Fixes black-on-black plain pages.
- **Typed http-only sites** and **Corn's error page** (founder approved).
- **Background tabs** (founder agreed): load at once like Chrome; show the saved picture while an unloaded tab reloads; `pauseTimers()` while Corn is in the background. Per-tab freezing isn't possible in WebView (`onPause` doesn't pause JavaScript; `pauseTimers` is global).
- **Rename to "Flush"**: raised by the founder; discussion only, nothing renamed.

### Round 15 — Flush Button glyph (2026-11-05)
- **Question:** a better Flush Button icon while the app is still Corn (not renamed to Flush).
- **Answer:** option A, a **water swirl**: three arms of water spiralling into a drain, on the yellow pill. It says "flush" without a toilet and works under either name. Replaces the toilet icon everywhere (toolbar, Flush confirmation, Tabs screen, onboarding). A single-line spiral was tried first and read as a snail/hypnosis swirl, so the three-arm vortex was used.
- **Also answered:** Titanium Browser (jqssun) is a full Chromium fork (Vanadium-based, GPL-2.0), not WebView; its time-range "Delete browsing data" is Chrome's own. Using the Corny mascot in the app proposed.

### Round 19 — Colours: switches and split focus (2026-11-05)
- **Answer:** switches use Corny's leaf green (#86C64A) with a cream dot (#FBF8F1, the Settings background) in both themes; the split-screen focus outline is corn yellow (#FFC83D, the Flush button's yellow), 3 dp. Yellow = actions and selection; green = "on".

### Round 18 — Split-screen picker, manual passwords, security doc (2026-11-05)
- **Split picker:** tab cards like the group sheet (founder's request); a grid/list switch everywhere was judged overkill for now (Claude's view: cards are better for a handful of tabs, a list for many).
- **Tapping a tab already in the other pane:** moves the focus there (no fullscreen, no rearranging). Tested.
- **Add passwords by hand:** built. **Security doc:** `docs/02-technical/12-security.md`.
- **Open:** limit wrong PIN attempts (found while writing the doc; not changed).

### Round 17 — Password store and split screen (2026-11-05)
- **Password store architecture** (researched: Firefox's Rust `logins` component keeps the site in clear and encrypts username/password; DuckDuckGo Android protects its keys with the Android Keystore; Chrome uses OS encryption + device check): `logins` table in corn.db via the Rust core (site readable for lookup; username + password AES-256-GCM encrypted in Kotlin); a random data key wrapped by a non-exportable Android Keystore key (StrongBox when available); viewing/copying/editing needs the Corn PIN; Corn's **own small form script** (not DuckDuckGo's library); fill only on the user's tap, https only, exact site match; passwords survive every Flush; nothing synced or sent.
- **Fill gate:** PIN only when the Vault is locked; a Settings switch **"Ask for PIN before filling"** (on by default) can turn it off.
- **Split screen inside Corn:** two tabs at once, **top/bottom with a draggable divider** in portrait, side by side in landscape; the toolbar acts on the last-touched pane; both stay in memory. Android's own split screen (two Corn windows) later.
- **Order:** both built in parallel by two agents in separate git worktrees, then merged and tested.
- **Status:** ✅ both merged and verified on the emulator (see features doc). Fixed during testing: the show-password toggle hid the password field from the save offer; the split divider's touch area was too thin (14 → 24 dp). Agent choices accepted: the picked tab goes in the bottom pane and gets focus; sites and usernames are visible once the Vault is open, only passwords always need the PIN.

### Round 16 — Corny is Corn's logo (2026-11-05)
- **Answer:** the founder chose **Corny** (the winking "shh" cob) as the logo.
- **Done:** adaptive app icon (Corny on #0C0B09, inside Android's 66 dp safe zone so circle/squircle launchers don't clip it; legacy icons too) and the in-app logo on the new-tab page and onboarding (background removed so it sits cleanly in light and dark mode). Masters: `brand/corny-logo-1024.png`, `brand/corny-logo-transparent-1024.png`. Seen on the emulator's home screen and new-tab page.
- **Still to do:** a clean vector redraw (the art is an AI draft); Corny in empty states and the error page (still 🌽) if wanted; the name stays Corn for now.

### Round 14 — Logo and name exploration; missing browser basics (2026-11-05)
- **Logo:** the current logo is "too real". Three Corn concepts (Flush Kernel, Corny, Tab Cob) and a full "Flush" rename exploration (3 logos, navy/electric-blue palette, Fredoka, copy kit) were generated with Codex image generation. Compare: `mockups/brand-compare.html`. Full-size originals are kept outside the repo (`~/Documents/corn-logo-originals`); the repo has 512 px copies. **Open:** name (Corn or Flush) and logo.
- **Missing basics found by a code check:** file uploads, downloads, site camera/mic/location prompts, find in page, address-bar suggestions, text size: none were in the code. **Answer:** build the first five plus text size, and reorganise Settings. Picture-in-picture and print/save as PDF later.
- **Status:** ✅ All built and verified on the emulator (see features doc for the data). Site permissions are remembered for the session only; Vault suggestions only while the Vault is unlocked; downloads go to the phone's Downloads folder and survive a Flush.
- **Agents saved:** the exact logo/brand agent briefs are in `design-agents/` (`corn-design/`, `flush-design/`), with reusable definitions in `.claude/agents/`. Flush concepts now have their own pages (Vortex, Button, Wink).

### Round 13 — Cookie-banner closer off by default (2026-11-04)
- **Data:** Reddit sign-in on the emulator. Closer on: the login box closed within ~7 s and "Continue with Google" was missing. Closer off: the box stayed open and the Google button showed. One run each; which autoconsent rule matched isn't known yet.
- **Answer:** Keep it **off by default**; users decide on each site's cookie banner themselves and can switch the closer on in Settings → Privacy.
- **Also discussed:** renaming Corn to "Flush" (opinion given, nothing decided); engine alternatives (WebView, GeckoView, a Chromium fork; Servo is early on Android and has no official iOS support).

### "Unsorted" becomes "All"
- **Answer:** Each link is saved once. Picking a Collection is optional. **All** is a view of every saved link, not a stored Collection, so nothing is ever saved twice. Existing Unsorted links moved to "no Collection" (they show in All); the Unsorted Collection was removed (database upgrade, unit-tested).

### Background tabs
- **Answer:** Settings → Advanced → Background tabs: **Automatic by phone memory** (default: 1 tab kept in memory on 4 GB, 2 on 6 GB, 3 on 8 GB, 5 on 12 GB+; all unloaded when Android warns memory is low), **Keep recent tabs in memory** (0/1/2/3/5), **Keep all tabs in memory** (not recommended; confirmation about battery, heat, Android closing Corn, and one shared page process). Names: "Kept in memory" and "Unloaded" replace "paused" and "frozen".
- **Facts written into the code** (top of `TabManager.kt`): `WebView.onPause()` "does not pause JavaScript" (Android docs); measured, a hidden tab's timers slow about 5×; `pauseTimers()` is global, so Corn never calls it. Videos in kept tabs are paused and blocked from restarting; unloaded tabs restore their scroll position.

### Back to the home page
- **Answer:** On a tab's first page opened from Corn's home page, Back shows a curved ↩ arrow and returns to the home page; Forward reopens the page. Not a heart.

### Smaller fixes
- Vault tiles in a row share one height. Tab card header 36 dp with the × centred in it; long titles end with "…" before the ×.

