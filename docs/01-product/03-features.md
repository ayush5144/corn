# 03 — Features (Built)

Every feature in the Android app today, what it does for the user, and the evidence behind it. Nothing here is planned-only; planned work lives in the [checklist](../03-planning/02-checklist.md).

**Evidence levels**
- ✅ **Verified**: seen working on the emulator (Pixel 8 image, Android 16), with the test described.
- 🧪 **Tested**: covered by automated tests, not yet seen on a device.
- 🔨 **Built**: in the app, not yet verified. Treat as unproven until checked.

Last updated: 2026-11-01 (commit `f6dc779`).

---

## Privacy and blocking

- **Ad and tracker blocking** ✅
  - *What:* Brave's open-source engine (adblock-rust, unmodified) checks every request a page makes against EasyList (ads) and EasyPrivacy (trackers), about 134,000 rules, and blocks matches before they download.
  - *Benefit:* fewer ads, fewer companies following you between sites, and fewer files downloaded.
  - *Evidence:* adblock-tester.com score **100/100**. A test page with exactly 5 ad/tracker requests showed **5 blocked**, and its 3 normal files still loaded. Across 10 heavy sites (BBC, CNN, Reddit, YouTube, NYT and others), **4,360 requests blocked in ~2 minutes**. Average check on an M4 Mac: **1.8 µs per request**.

- **Per-site switches: Ads, Trackers, Pop-ups** 🔨
  - *What:* shield → one main switch, plus **Advanced** with separate switches for each, remembered per site.
  - *Benefit:* if a site breaks, turn off only what's needed, only on that site.
  - *Evidence:* built on two separate engines (EasyList and EasyPrivacy). The switches' effect hasn't been checked on a device. The split is close but not exact, because the two lists overlap slightly.

- **Pop-up and redirect guard** ✅
  - *What:* every pop-up window (tapped or scripted) is stopped, and pages can't send the whole tab to another site unless you tapped.
  - *Benefit:* no surprise pop-unders or redirect chains, which are common on adult sites.
  - *Evidence:* a test page's scripted pop-up was stopped and counted (**1 of 1**).

- **Element hiding** 🔨
  - *What:* leftover empty ad boxes are hidden using EasyList's hiding rules.
  - *Benefit:* cleaner pages with no blank ad slots.
  - *Evidence:* built; not separately measured.

- **Cookie-banner closing** 🔨
  - *What:* DuckDuckGo's open-source autoconsent (v17.2.0, unmodified) recognises "We use cookies" boxes and clicks **Reject** for you. Settings → Privacy, on by default.
  - *Benefit:* no consent boxes covering pages, and no accidental "Accept all".
  - *Evidence:* the same library DuckDuckGo ships in its browsers. Not yet seen closing a banner in Corn; test on a European site.

- **Link cleaning** ✅
  - *What:* removes tracking tags such as `utm_source`, `fbclid`, `gclid`, `msclkid` (about 30 known names) from links before opening them, keeping the real parameters.
  - *Benefit:* the site can't see which ad, post or email you came from.
  - *Evidence:* a link with `?id=42` plus 4 tracking tags reached the server as **`/?id=42`**. 7 unit tests. Brave, Firefox (Strict mode) and Safari (iOS 17+) do the same.

- **HTTPS upgrade** 🧪
  - *What:* `http://` addresses are opened as `https://`. If a site has no secure version, Corn falls back to http for that site for the rest of the session. Home/local addresses are left alone.
  - *Benefit:* your connection to the site is encrypted whenever the site supports it, so others on the network (public Wi-Fi) can't read or change the page.
  - *Evidence:* unit tests for upgrading and for skipping local addresses. Not yet seen on a device.

- **Third-party cookies blocked** ✅ (Android setting)
  - *What:* WebView's `setAcceptThirdPartyCookies(false)` for every tab.
  - *Benefit:* embedded trackers can't set their own cookies on sites you visit.
  - *Evidence:* set in code for every tab; this is a standard Android WebView switch.

- **Insecure content blocked on secure pages** ✅ (Android setting)
  - *What:* `MIXED_CONTENT_NEVER_ALLOW`.
  - *Benefit:* a secure page can't load insecure parts that others could tamper with.

- **Filter lists stay fresh** ✅
  - *What:* lists are downloaded again every 4 days on Wi-Fi, checked (at least 10,000 lines), built in the background and swapped in. If anything fails, the last good lists stay.
  - *Benefit:* new ad and tracker domains keep getting blocked; volunteers update these lists daily.
  - *Evidence:* on a fresh install, fresh lists were swapped in **8 seconds** after launch (log).

- **Offline starter list** ✅
  - *What:* that day's lists are compiled into the app at build time.
  - *Benefit:* blocking works from the first second, even with no internet.
  - *Evidence:* log on a fresh install: "using the built-in starter ads list" before any download.

- **Chrome-style browser identity** 🔨
  - *What:* sends Chrome's reduced user agent instead of WebView's, the same approach DuckDuckGo's Android browser uses.
  - *Benefit:* fewer "browser not supported" blocks (including Google sign-in), and less detail about your phone model.
  - *Evidence:* built; Google sign-in not yet tested.

## Flush and sessions

- **Flush Button** ✅
  - *What:* one tap closes every tab and group and clears cookies (except the Keep List), site storage, cache, form data and the saved session. The Vault, settings and Keep List stay. Drain animation, vibration, a one-liner, optional sound, optional "Ask before flushing".
  - *Benefit:* a clean slate instantly.
  - *Evidence:* animation and fresh page with the one-liner seen. What each category of data looks like after a Flush hasn't been checked file by file yet.

- **Kept Session (default)** 🔨
  - *What:* your tabs and groups are saved when you leave Corn and restored next time, until you flush.
  - *Evidence:* built; restore after closing the app not yet checked.

- **Auto-Flush** 🔨
  - *What:* optional: flush when you leave Corn, 5 or 30 minutes after leaving, or after 10/30/60 minutes of inactivity. A playing video counts as activity.
  - *Evidence:* built; timers not yet checked on a device.

- **Keep List** 🔨
  - *What:* sites whose login cookies survive a Flush, so you stay logged in.
  - *Evidence:* built. Known limit: other saved data for those sites is wiped, so some sites may still log you out.

## Tabs

- **Tab Groups with the Tab Group Strip** 🔨
  - *What:* open a link in a new tab group (or a new tab in the current group); the strip shows each tab's real favicon for one-tap switching; `^` opens the group with previews, rename, colour, "Save group to Vault".
  - *Evidence:* built; not yet seen on a device.

- **Tab previews** ✅
  - *What:* screenshots of tabs in the Tabs screen and group view, kept in memory only (~0.25 MB each).
  - *Evidence:* seen in the Tabs screen.

- **Memory-saving tab rule** 🔨
  - *What:* only the active tab and the 2 most recent stay in memory (paused tabs stop video and audio); others are frozen as a preview and reload when opened.
  - *Benefit:* lower memory use. 10 fully live tabs measured **~1.05 GB** before this rule existed.
  - *Evidence:* built; memory with the rule in place not yet re-measured.

- **Page crash recovery** 🔨
  - *What:* if a page's process dies, the tab shows "This page stopped · Reload" instead of the app breaking.
  - *Evidence:* built after a real crash on a 2 GB emulator; not triggered since.

## Vault

- **Vault with Collections and tags** 🔨
  - *What:* save the current page from the address bar (or a link from its long-press menu) into a Collection, with optional tags; browse Collections, search titles, addresses and tags.
  - *Evidence:* built; save flow fixed today (a typed new Collection is now used when you tap Save). Not yet seen end to end on a device.

- **Delete a saved link** 🔨
  - *What:* long-press a link in a Collection → confirm → removed permanently.
  - *Evidence:* built; not yet seen on a device.

- **Vault PIN** ✅
  - *What:* a 4-digit PIN guards the Vault, on by default. It's stored only as a salted, slowly-hashed value (PBKDF2, 120,000 rounds), never the PIN itself.
  - *Evidence:* "Create a PIN" screen seen.

## Discretion

- **Black screen in recent apps** 🔨: Android 13+ `setRecentsScreenshotEnabled(false)`, on by default. Not yet checked visually.
- **App Lock** 🔨: optional PIN to open Corn (same PIN as the Vault).
- **No notifications while closed** ✅: Corn has no notification code at all.
- **No backups** ✅: `allowBackup="false"` plus rules excluding all data from cloud backup and device transfer.
- **No logs of browsing** ✅: audited. Corn's 5 log messages contain no URLs; page console messages are kept out of Android's log; there's no record of what was blocked where.
- **WebView usage metrics opted out** ✅: `MetricsOptOut` in the manifest.
- **Safe Browsing switch** 🔨: Settings → Privacy, on by default, mentioned in onboarding. Google Safe Browsing warns before malware and phishing pages; Google receives hashed address fragments, not full URLs. Applies to open tabs immediately.

## Search and navigation

- **Safe Search switch** 🧪: Settings → Search, off by default. Off adds each engine's "safe search off" parameter (`kp=-2`, `safe=off`, `safesearch=off`, `adlt=off`); on adds the strict one (`kp=1`, `safe=active`, `safesearch=strict`, `adlt=strict`). Startpage has no URL parameter, so its own setting applies. 3 unit tests (13 address checks).
- **Five search engines with a one-tap switcher** ✅: DuckDuckGo (default), Google, Brave Search, Startpage, Bing, each with its favicon (bundled, never fetched). Seen in the address bar and the engine sheet.
- **Full address in the address bar** ✅: always shows the page's full URL (e.g. `https://duckduckgo.com/?q=corn+browser`), like Brave.
- **Address bar top or bottom** 🔨: a Settings choice.
- **Opens links from other apps** ✅: tested with links sent to Corn.
- **`http://` sites load** ✅: the Android default would block them; the count-test page loaded over http.
- **Fullscreen video** 🔨: built in the spike; not re-checked in the new UI.

## Home page

- **Stats card** ✅
  - *What:* two all-time totals: **trackers & ads blocked** and **pop-ups & redirects stopped**. Plain counts with no sites or URLs, kept across Flushes.
  - *Evidence:* the test page produced exactly **5** and **1**, in both debug and release builds.

## Size

| | Measured |
|---|---|
| Release APK (shrunk, phones only, includes starter lists + cookie-banner script) | **8.6 MB** |
| App data after first run | ~11 MB (block lists 5.9 MB + WebView data); grows with browsing, cleared by Flush |
| App process memory, 1 tab | 139 MB (page content runs in WebView's own process) |
