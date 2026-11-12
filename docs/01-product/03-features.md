# 03 — Features (Built)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-12, 11:28 IST

Every feature in the Android app today, what it does for the user, and the evidence behind it. Nothing here is planned-only; planned work lives in the [checklist](../03-planning/02-checklist.md).

**Evidence levels**
- ✅ **Verified**: seen working on the emulator (Pixel 8 image, Android 16), with the test described.
- 🧪 **Tested**: covered by automated tests, not yet seen on a device.
- 🔨 **Built**: in the app, not yet verified. Treat as unproven until checked.

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
  - *What:* windows a page opens by itself are stopped; a link or button you tap that opens a new window opens as a new tab (one per tap), like Chrome. A sign-in window that closes itself brings back the page that opened it. Pages can't send the whole tab to another site unless you tapped.
  - *Benefit:* no surprise pop-unders or redirect chains, which are common on adult sites, while "open in new tab" links, "Sign in with Google" and share buttons still work.
  - *Evidence (2026-11-05, `scripts/qa-run.sh`):* tapped `target=_blank` link and tapped `window.open` each opened 1 page; no-tap pop-up 0 requests; one tap asking for 3 windows opened 1; self-closing window returned to its opener. ayushpatil.com's links open (they were all blocked before).

- **Links to other apps** ✅
  - *What:* phone, email, SMS, map and "open in app" links: after a tap, Flush asks ("Call this number?", "Open in <app>?"): Open, Open web version (when the link has one), Cancel. Never without a tap; `intent://` links are made safe like Chrome and DuckDuckGo do.
  - *Evidence:* tel/mailto/sms/geo opened Phone, Gmail, Messages and Maps after Open; a no-tap `tel:` did nothing; an app that isn't installed fell back to the web version.

- **Image long-press menu** ✅: Save image (through the download sheet), Open image in new tab, Share image link, Copy. Linked images also get the link's actions. Link titles come from the page's labels, so YouTube shows the video title instead of its duration.

- **Element hiding** 🔨
  - *What:* leftover empty ad boxes are hidden using EasyList's hiding rules.
  - *Benefit:* cleaner pages with no blank ad slots.
  - *Evidence:* built; not separately measured.

- **Cookie-banner closing** 🔨
  - *What:* DuckDuckGo's open-source autoconsent (v17.2.0, unmodified) recognises "We use cookies" boxes and clicks **Reject** for you. Settings → Privacy, **off by default**.
  - *Benefit:* no consent boxes covering pages, and no accidental "Accept all".
  - *Evidence:* the same library DuckDuckGo ships in its browsers. Not yet seen closing a banner in Flush. Known problem (why it's off by default): on Reddit, with it on, the login box closed within ~7 s and "Continue with Google" was missing; with it off, the login box stayed open and the Google button showed (one run each, on the emulator).

- **Link cleaning** ✅
  - *What:* removes tracking tags such as `utm_source`, `fbclid`, `gclid`, `msclkid` (about 30 known names) from links before opening them, keeping the real parameters.
  - *Benefit:* the site can't see which ad, post or email you came from.
  - *Evidence:* a link with `?id=42` plus 4 tracking tags reached the server as **`/?id=42`**. 7 unit tests. Brave, Firefox (Strict mode) and Safari (iOS 17+) do the same.

- **HTTPS upgrade** 🧪
  - *What:* `http://` addresses are opened as `https://`. If a site has no secure version, Flush falls back to http for that site for the rest of the session. Home/local addresses are left alone.
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
  - *What:* one tap closes every tab and group and deletes history, cookies and IndexedDB (except Keep List sites'), all other site storage, cache, form data and the saved session, then the app restarts itself in about 0.3 s so nothing stays in memory. The Vault, settings and Keep List stay. Drain animation, vibration, a one-liner, optional sound, optional "Ask before flushing". The button's glyph is a water swirl going down a drain (seen in the toolbar on the emulator).
  - *Benefit:* a clean slate instantly, with nothing left behind.
  - *Evidence:* on the emulator, two test sites each set a cookie, local storage and IndexedDB. After Flush: the Keep List site still had its cookie; the other site's cookie was gone; local storage and IndexedDB were gone for both; history went from 13 visits to 0. Log: cookies deleted from WebView's cookie database (no fallback needed), process restarted.

- **Kept Session (default)** 🔨
  - *What:* your tabs and groups are saved when you leave the app and restored next time, until you flush.
  - *Evidence:* built; restore after closing the app not yet checked.

- **Auto-Flush** 🔨
  - *What:* optional: flush when you leave the app, 5 or 30 minutes after leaving, or after 10/30/60 minutes of inactivity. A playing video counts as activity.
  - *Evidence:* built; timers not yet checked on a device.

- **Keep List** ✅
  - *What:* sites that stay logged in after a Flush. Their cookies are left untouched in WebView's cookie database (DuckDuckGo's method), so expiry and security flags stay as the site set them, and their IndexedDB (site databases) is kept. Add or remove the site you're on with one tap: shield → **Stay logged in after Flush**; or in Settings → Keep List. Details: [11-keep-list](../02-technical/11-keep-list.md).
  - *Evidence:* after Flush, a Keep List test site still had its cookie and its IndexedDB (readable after the restart); a non-Keep List site had neither. The shield switch added the site to the stored list and removed it on a second tap. Known limit: their local storage is deleted (Android can't delete one site's local storage), so a site that keeps its login only there may log you out.

- **History** ✅
  - *What:* pages you visit, on this phone only, cleared by every Flush. Menu → History: search, grouped by day, tap to open, × to delete one visit, long-press to delete a whole site from history, trash icon to clear all. "Keep history" switch in Settings (on by default); when off, nothing is written. A "Delete browsing data" link at the top of History covers cookies, cache and tabs. Every history deletion says it's history only ("Cookies stay").
  - *Evidence:* visits recorded with page titles (e.g. a test search's "… at DuckDuckGo" title). One search = one entry (pages that change their own address while loading are merged within 5 s; before the fix one DuckDuckGo search made 6 entries). Flush emptied it. Deletion leaves no trace in the database files: SQLite `secure_delete` plus emptying the write-ahead log (a unit test searches the files for the deleted page; without the fix the title was still readable in the `-wal` file). Size: about 160 bytes per visit (1,000 visits ≈ 160 KB, measured). The screen itself hasn't been reviewed by the founder yet.

- **Delete browsing data** 🔨
  - *What:* Settings → History and data: tick History, Cookies and site data (Keep List cookies stay), Cached files, Open tabs; everything in each ticked category goes. No time ranges, because the cache and site storage can only be deleted all at once.
  - *Evidence:* uses the same deletion as Flush (verified above); this screen not yet tested on a device.

## Tabs

- **Tab Groups with the Tab Group Strip** 🔨
  - *What:* open a link in a new tab group (or a new tab in the current group); the strip shows each tab's real favicon for one-tap switching; `^` opens the group with previews, rename, colour, "Save group to Vault".
  - *Evidence:* built; not yet seen on a device.

- **Main buttons stay visible** ✅: on the Tabs screen, a slim "Done with all of these?" bar with the Flush button (bottom right, as in the toolbar) is pinned at the bottom; the group sheet, Save to Vault and the split-screen picker keep their buttons pinned while their content scrolls. Verified with 13 tabs: the Flush bar, "Save group to Vault"/"New tab in group" and the picker's "New tab" stayed in place while the lists scrolled. Not pinned on purpose: Settings, History, Passwords, Vault (actions already in fixed headers) and short sheets.

- **Tab previews** ✅
  - *What:* screenshots of tabs in the Tabs screen and group view, kept in memory only (~0.25 MB each).
  - *Evidence:* seen in the Tabs screen.

- **Toasts sit next to what you tapped** ✅: just above the bottom bar (measured, so the Tab Group strip and a bottom address bar count), above an open sheet, or 16 dp above the bottom edge on screens without a bar. Seen in all three cases on the emulator.

- **Background tabs** 🔨
  - *What:* Settings → Advanced → Background tabs. **Automatic by phone memory** (default: 1 recent tab kept in memory on 4 GB, 2 on 6 GB, 3 on 8 GB, 5 on 12 GB+, all unloaded when Android warns memory is low), **Keep recent tabs** (0/1/2/3/5) or **Keep all** (asks first: battery, heat, Android may close Flush, one crash reloads every tab). Kept tabs reopen instantly; their videos stay paused and can't restart. Unloaded tabs run nothing and reload with their scroll position.
  - *Benefit:* lower memory and battery use. 10 tabs: **~644 MB** with 2 kept vs **~1.05 GB** all loaded (measured earlier).
  - *Evidence:* tab counts checked through WebView's DevTools list: with 4 tabs open on the 4 GB emulator, 2 were in memory (the open tab + 1 recent), as designed. The tab-count picker is always visible (dimmed until "Keep recent tabs" is chosen; picking a count chooses it) and its menu is readable (it was light-on-light before). Scroll restore and video hold not yet tested on a device.

- **Menu: New tab group / New tab in this group** ✅: outside a group, "New tab group" puts the current tab in a new group with a new tab beside it; inside a group, the menu shows "New tab in this group". Verified on the emulator (group strip went from the page alone to page + new tab, then 3 tabs).

- **Tabs opened in the background load at once** ✅: "Open in new tab" and "…in new tab group" start loading straight away (like Chrome), taking a memory slot as the most recent tab. Verified: the server got the page in the same second it was opened in the background, before any tap; opening it caused no new request and showed the time it loaded in the background.

- **Saved picture while an unloaded tab reloads** ✅: no blank screen. Verified with a test site that takes 4 s to answer: 0.8 s after opening the unloaded tab, the screen showed its saved picture ("served 18:34:12"); the live page ("served 18:35:10") replaced it when it arrived.

- **Pages pause when Flush is in the background** ✅: `pauseTimers()` (Android's global pause) while Flush is off screen, `resumeTimers()` on return. Measured with a page that pings every second: 20 pings in 20 s on screen, 0 in 20 s in the background, 9 in 10 s after returning.

- **Back to the home page** ✅
  - *What:* on a tab's first page opened from Flush's home page, Back shows a curved ↩ and returns to the home page; Forward reopens the page.
  - *Evidence:* seen on the emulator: ↩ shown on the first page, tapping it returned to the home page, Forward reopened the page.

- **Page crash recovery** 🔨
  - *What:* if a page's process dies, the tab shows "This page stopped · Reload" instead of the app breaking.
  - *Evidence:* built after a real crash on a 2 GB emulator; not triggered since.

## Vault

- **Vault with Collections and tags** 🔨
  - *What:* save the current page from the address bar (or a link from its long-press menu), optionally into a Collection, with optional tags. **All** shows every saved link; each link is stored once. Browse Collections, search titles, addresses and tags. Tiles in a row share one height.
  - *Upgrade:* links that were in the old "Unsorted" Collection moved to no Collection and show in All (verified: the emulator's database upgraded to version 2 and "Unsorted" was removed; unit-tested with tags kept).
  - *Evidence:* built; save flow fixed today (a typed new Collection is now used when you tap Save). Not yet seen end to end on a device.

- **Delete a saved link** 🔨
  - *What:* long-press a link in a Collection → confirm → removed permanently.
  - *Evidence:* built; not yet seen on a device.

- **Vault PIN** ✅
  - *What:* a 4-digit PIN guards the Vault, on by default. It's stored only as a salted, slowly-hashed value (PBKDF2, 120,000 rounds), never the PIN itself.
  - *Evidence:* "Create a PIN" screen seen.

## Discretion

- **Black screen in recent apps** 🔨: Android 13+ `setRecentsScreenshotEnabled(false)`, on by default. Not yet checked visually.
- **App Lock** 🔨: optional PIN to open Flush (same PIN as the Vault).
- **No notifications while closed** ✅: Flush has no notification code at all.
- **No backups** ✅: `allowBackup="false"` plus rules excluding all data from cloud backup and device transfer.
- **No logs of browsing** ✅: audited. Flush's 5 log messages contain no URLs; page console messages are kept out of Android's log; there's no record of what was blocked where.
- **WebView usage metrics opted out** ✅: `MetricsOptOut` in the manifest.
- **Safe Browsing switch** 🔨: Settings → Privacy, on by default.
- **Onboarding privacy choices** ✅: onboarding shows Safe Browsing (On) and Safe Search (Off) with a switch each. Verified on a fresh install: tapping Safe Search turned the label to "On" and saved `safe_search = true`. Google Safe Browsing warns before malware and phishing pages; Google receives hashed address fragments, not full URLs. Applies to open tabs immediately.

## Search and navigation

- **Safe Search switch** 🧪: Settings → Search, off by default. Off adds each engine's "safe search off" parameter (`kp=-2`, `safe=off`, `safesearch=off`, `adlt=off`); on adds the strict one (`kp=1`, `safe=active`, `safesearch=strict`, `adlt=strict`). Startpage has no URL parameter, so its own setting applies. 3 unit tests (13 address checks).
- **Five search engines with a one-tap switcher** ✅: DuckDuckGo (default), Google, Brave Search, Startpage, Bing, each with its favicon (bundled, never fetched). Seen in the address bar and the engine sheet.
- **Full address in the address bar** ✅: always shows the page's full URL (e.g. `https://duckduckgo.com/?q=private+browser`), like Brave.
- **Address bar top or bottom** 🔨: a Settings choice.
- **Light and dark mode** ✅: Settings → Appearance: Dark (default), Light, or Phone (follows the phone's dark mode). Sites see the user's preference (`prefers-color-scheme`), so sites with a dark mode follow it; pages are otherwise shown as the site designed them, on the web's white default background, and a site's own light/dark switch works. **Darken all websites** (off by default) makes WebView also darken light-only pages in dark mode (`setAlgorithmicDarkeningAllowed`). Verified 2026-11-05: a site's own switch to light shows white on screen (was black); a beige light-only page shows its beige (was near-black). Switching rebuilds open pages so they change immediately.
- **Typed addresses fall back to http** ✅: typing `example.com` starts from `http://` and Flush's HTTPS upgrade tries https first. Verified: `httpforever.com` (https fails with a handshake error, checked from the Mac) opened at `http://httpforever.com/`; before, it showed a connection error. Unit-tested.
- **Flush's error page** ✅: a site that can't load shows "Can't open this page" with a plain-language reason and Try again, instead of WebView's grey page. Verified with an address that can't exist (`.invalid`): "Flush couldn't find this site. Check the address, or your connection."
- **Pull to refresh** ✅: pulling down while the page is at the top reloads it (Android's SwipeRefreshLayout, spinner in the theme's action colour). Each tab's page has its own pull container: SwipeRefreshLayout only ever measures the first child it saw, so swapping another tab's page into a shared one left it 0×0 and blank (bug from the first version, fixed; verified: switching between group tabs shows each page at full size, and pull still reloads after switching). Verified: the test server saw the page requested again after a pull (4 → 5); an upward scroll did not reload it. Like Chrome (since v41, 2015), Safari (iOS 15) and Firefox for Android (opt-in). Known limit: on pages that scroll inside their own box rather than the whole page, a pull at the top of the page can reload even if the inner box isn't at its top.
- **File uploads** ✅: a site's "Choose file" opens Android's file picker (several files when the site allows it). Verified: a test page received the test text file (16 B), the exact file picked.
- **Downloads** ✅: tapping a download asks first ("Download this file? name · size · from site"), then Android's download manager saves it to the phone's Downloads folder (so it finishes even if Flush closes; a notification shows only while downloading). "Downloaded … · Open" when done. Files stay after a Flush. Verified: a 20,000-byte test zip saved, identical to the original. Links that aren't http/https (e.g. `blob:`) say "Flush can't download this kind of file yet".
- **Camera, microphone and location for sites** ✅: a site asking shows "example.com wants to use your camera" with Allow / Block; Android's own permission is asked only after Allow. Answers last for the session (a Flush or restart forgets them) and show in the shield sheet, where a tap resets them. Browsers only allow these on https pages (seen: an http test page was refused by Chromium itself). Verified on https://example.com: location returned the emulator's GPS position (37.422, −122.084); the camera gave 1 live video track; Block on the microphone returned `NotAllowedError` without asking Android. One early location attempt failed with no Android dialog and didn't happen again in 3 later runs; cause unknown.
- **Address-bar suggestions** ✅: while typing, up to 6 matches from History and the Vault (Vault links only while the Vault is unlocked or the lock is off), computed on the phone; nothing is sent anywhere. Also under the new-tab page's search box. Verified: typing "feat" showed "Features test · 10.0.2.2"; tapping it opened the page.
- **Find in page** ✅: Menu → Find in page; the address bar becomes a find bar with a match count and previous/next. Verified: "kernel" found 5 matches; Next moved to 2/5 with the current match highlighted.
- **Text size** ✅: Settings → Appearance → Text size: S 85%, M 100%, L 115%, XL 130%, for every page, applied immediately. Verified: a heading went from 38.5 px to 49.9 px tall at XL (×1.30).
- **Opens links from other apps** ✅: tested with links sent to Flush.
- **`http://` sites load** ✅: the Android default would block them; the count-test page loaded over http.
- **Fullscreen video** 🔨: built in the spike; not re-checked in the new UI.

## Passwords and split screen

- **Password store** ✅: after you log in, "Save password for example.com?" (Save / Not now / Never for this site; "Update password?" for a changed one). Next time a "Fill as you@…" chip appears; Flush fills only when you tap, only on https (local test addresses allowed in debug builds) and only on the exact site. If the Vault is locked, filling asks your PIN first (Settings → "Ask for PIN before filling" can turn that off). Vault → Passwords: search, grouped by site, reveal/copy/edit with the PIN, copies cleared from the clipboard after 45 s or when you leave the app (verified by pasting), screenshots blocked. Encrypted AES-256-GCM with a key protected by the Android Keystore; survives every Flush; never synced or sent.
  - *Evidence (emulator):* a real login on a test page brought the save offer; a fill after the PIN put the exact saved password in the field; a second fill in the same session didn't ask the PIN; after a Flush the login was still offered. A bug found in testing and fixed: after tapping a site's "Show password" button the password field becomes a text field and no save offer appeared; the script now remembers password fields. 6 crypto + 6 matching JVM tests, 3 Rust tests (incl. no trace left after update/delete and the v2→v3 upgrade).
  - **Add by hand** ✅: Passwords → Add: site (e.g. `example.com`, saved as https), username or email, password with Show/Hide. Same encryption as automatic saves; the same site + username updates instead of duplicating. Verified: the login appeared in the list, and neither the username nor the password appears anywhere in `flush.db` or `flush.db-wal` (searched the raw files). Screenshots of the Passwords screens come out black (the screenshot block works).
  - *Not yet:* two-step logins (username and password on separate pages: only the password is saved), export/import.
- **Split screen** ✅: Menu → Split screen → the same tab cards as the group sheet (previews, two per row; grouped tabs included) or New tab. The address bar works in split screen and loads into the focused pane. Verified: typing an address loaded it in the bottom pane only; with the address bar set to Bottom, suggestions appeared above it and the address loaded in the focused pane; a New tab's own search box inside the bottom pane loaded there; tapping a tab that's already in the other pane (Tabs screen) just moves the focus to that pane (no fullscreen); tapping another tab in the group strip replaces only the focused pane. "New tab in group" during split screen adds the tab to the group without changing either pane, with a toast "Open" (verified: panes unchanged, tab count 2 → 3, Open put it in the focused pane). Two tabs at once, top/bottom (side by side in landscape), a draggable divider (25–75%; drag to an edge to close a pane), the toolbar and address bar act on the pane you last touched (outlined), × on a pane closes the pane, not the tab.
  - *Evidence (emulator):* both pages full size (1080×930 each); tapping a pane moved the address bar to it and links still opened in that pane; dragging resized (548/1313 px, stopped at about 74/26%); landscape showed them side by side with content intact; "Try again" on an error page reloaded only that pane; × closed the pane and kept both tabs. The divider was widened from 14 to 24 dp after a drag 40 px off-centre missed it. 10 JVM tests.

## Brand

- **Flush brand** ✅: the Vortex logo as the app icon (adaptive, inside the 66 dp safe zone) and on the new-tab page and onboarding, the navy/electric-blue palette in dark and light, Fredoka for display text, and the Flush Button in Fill blue (second from the right; the raised centre button was parked on 2026-11-12). Details: [04-brand](04-brand.md). Verified on the emulator 2026-11-11: screenshots of launcher, onboarding, new-tab page, nav bar, a Flush toast, Settings, Vault, History, sheets in dark and light; every text pair ≥ 4.5:1 (light-mode green on surfaces uses #0E7647 because the board's #11804E measured 4.36:1); QA suite 17/17.

## Home page

- **Stats card** ✅
  - *What:* two all-time totals: **trackers & ads blocked** and **pop-ups & redirects stopped**. Plain counts with no sites or URLs, kept across Flushes.
  - *Evidence:* the test page produced exactly **5** and **1**, in both debug and release builds.

## Size

| | Measured |
|---|---|
| Release APK (shrunk, phones only, includes starter lists + cookie-banner script) | **8.7 MB** |
| App data after first run | ~11 MB (block lists 5.9 MB + WebView data); grows with browsing, cleared by Flush |
| App process memory, 1 tab | 139 MB (page content runs in WebView's own process) |
