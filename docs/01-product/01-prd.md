# 01 — Product Requirements (PRD)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-13, 18:19 IST

- **Status:** Draft v2, being shaped through Q&A rounds ([02-decision-log.md](02-decision-log.md))
- **Last updated:** 2026-11-01 (after Round 1)
- **Replaces:** the Brave-fork PRD in `~/dev/corn-old/prd-corn-browser.md`
- **Task tracking:** [../03-planning/02-checklist.md](../03-planning/02-checklist.md)

Every requirement below is tagged:

- ✅ **Settled**: decided by the founder; build to it.
- 🟡 **Open**: still under discussion; see the decision log.
- ⏭️ **Later**: agreed, but not in v1. Tracked in the checklist backlog.

## 1. What we're building

**Corn is a free, private, mobile-first browser for adults that keeps you organised.** It blocks ads, trackers, pop-ups and redirect abuse by default. Tab Groups with previews let you browse, swipe and switch instantly. A locked Vault keeps the links you want to keep, organised into Collections. One tap on the Flush Button wipes everything else. Nothing about your browsing leaves the device.

**Personality:** cool, funny, modern. DuckDuckGo is all right; Corn is the fun one that helps you not get lost.

Corn is a browser. It does not host, index, recommend or bypass access controls for any content.

## 2. Who it's for

Adults (18+), where adult content is legal, browsing on their **phones**. ✅ Mobile first: **Android and iPhone** are both target platforms.

## 3. Why someone switches to Corn

| Corn has | DuckDuckGo / Brave private mode |
|---|---|
| **Tab Groups** with previews: open a link into a new group, swipe around, switch instantly | Flat tab lists, no grouping |
| **Vault + Collections**: save and organise links privately so nothing gets lost | No locked, organised library |
| **Flush on your terms**: one-tap button plus your choice of auto-flush | Fire Button with fixed behaviour |
| **Blocking tuned for adult sites**: pop-ups and redirect hijacks | General-purpose blocking |
| A fun personality | Serious, utilitarian |

## 4. Principles

1. ✅ **Nothing leaves the device by default.** URLs, searches, titles, tabs and Vault contents stay local.
2. ✅ **The user is in control.** Every behaviour that matters is a setting with a sensible default.
3. ✅ **No accounts, ads or tracking** in the free browser.
4. ✅ **Honest about limits.** Flush clears this device only, never what websites, networks or the OS recorded.
5. ✅ **The Vault survives every Flush** unless the user separately chooses to delete it.

## 5. Business model

- ✅ **Free.** Funded by donations.
- ⏭️ **Cloud Vault:** optional, paid (~$3–5/month), end-to-end encrypted backup/sync of the Vault. Not v1.

## 6. Features

### 6.1 Browsing

| Requirement | Status |
|---|---|
| Address bar: URL and search | ✅ |
| Back, forward, reload, share | ✅ |
| Pull down at the top of a page to reload | ✅ |
| Blocking of ads, trackers, pop-ups and redirect abuse, on by default | ✅ |
| Per-site blocking: one main switch, plus **Advanced** switches for Ads, Trackers and Pop-ups & redirects | ✅ |
| Home page **stats**: two all-time totals (trackers & ads blocked, pop-ups & redirects stopped), counts only | ✅ |
| Google Safe Browsing: Settings switch, **on by default**; shown on onboarding with its on/off state and a switch | ✅ |
| Cookie-banner closing: clicks "Reject" (DuckDuckGo autoconsent); Settings switch, **off by default** (it closed Reddit's login box in testing) | ✅ |
| Fingerprinting protection + Global Privacy Control (DuckDuckGo content-scope-scripts) | 🟡 proposed |
| Link cleaning: strip tracking parameters (`utm_*`, `fbclid`, `gclid`…) | ✅ (redirect unwrapping later) |
| Third-party cookies blocked | ✅ |
| HTTPS upgrade, with http fallback if the secure version fails | ✅ |
| Filter lists refresh every 4 days on Wi-Fi; offline starter list built in | ✅ |
| Fullscreen video and Picture-in-Picture | ✅ |
| File downloads | ⏭️ |

### 6.2 Tabs and Tab Groups

| Requirement | Status |
|---|---|
| Open a link in a **new tab** or a **new tab in the current Tab Group** (or a **new tab group** when not in one) | ✅ |
| Menu: **New tab group** when the current tab isn't in a group | ✅ |
| **Tab Group Strip** at the bottom (like Chrome Android): one round icon per tab showing the site's **real favicon**; tap to switch instantly | ✅ |
| Strip controls: `^` opens the group's tab grid with previews, `+` adds a tab to the group, `×` closes the current tab | ✅ |
| Swipe between tabs; click around freely | ✅ |
| Tab previews in the switcher grid | ✅ |
| Flush wipes every Tab Group | ✅ |
| "Save group to Vault" (turns a group into a Collection) in the group's `^` grid | ✅ |
| Group naming and colours | 🟡 decide from prototype |

### 6.2b Layout

| Requirement | Status |
|---|---|
| Address bar **at the top** by default | ✅ |
| Setting: move the address bar to the bottom | ✅ v1 |
| Address bar, left to right: **Search Switcher** · address · **blocking shield** · then the **Vault save icon** (no star) | ✅ |
| Bottom toolbar: **Back** (hold for Forward) · **Vault** · **Tabs** · **Flush** · **Menu** | ✅ |
| Forward, New tab, Settings live in the Menu | ✅ |
| Look and feel (colours, type, icon style) | 🟡 decide from prototype |

### 6.3 Search

| Requirement | Status |
|---|---|
| Several built-in engines: DuckDuckGo, Google, Brave Search, Startpage, Bing | ✅ |
| Switch engine from **one dropdown inside the search bar**, each engine shown with its favicon | ✅ |
| Default engine: **DuckDuckGo**, safe search off where the engine allows | ✅ |
| **Safe Search** switch (filter explicit results), **off by default**; also shown on onboarding with a switch | ✅ |
| User-added custom engines | ⏭️ |

### 6.4 Flush and session behaviour

| Requirement | Status |
|---|---|
| **Flush Button**: one tap wipes the Session (tabs, Tab Groups, cookies, cache, history, previews) | ✅ |
| Setting: **Flush confirmation** ("Ask before flushing"), **on by default**; the sheet's "Don't ask again" (counts only when you Flush) turns it off | ✅ |
| Flush Button is a **flush** button (not a power button). Glyph: a water swirl going down a drain (three arms + drain dot) on the corn-yellow pill, replacing the toilet; same glyph in the confirmation, Tabs screen and onboarding | ✅ |
| Flush Button sits in the **bottom toolbar, at the far right or second from the right** | ✅ |
| Default: **Kept Session**: the Session survives until the user flushes | ✅ |
| Setting: **Auto-Flush after leaving Corn**, user chooses the delay or never | ✅ |
| Delay presets: **when I leave Corn**, **5 min after**, **30 min after** (plus Never) | ✅ |
| Setting: **Auto-Flush after inactivity**: 10, 30 or 60 minutes | ✅ |
| Flush feedback: quick swirl animation, vibration, funny one-liner on the empty page; sound is a setting, off by default | ✅ |
| **A playing video counts as activity** | ✅ |
| "Leaving Corn" = switching apps, going home or locking the phone | ✅ |
| **Keep List**: sites that stay logged in after a Flush: their cookies and IndexedDB are kept; their local storage, history and everything else is deleted. Not history, not passwords. Add from the shield ("Stay logged in after Flush") or Settings. See [11-keep-list](../02-technical/11-keep-list.md) | ✅ v1 |
| **History**: pages visited, on the phone only, cleared by Flush; "Keep history" switch (on by default); History screen with search, grouped by day, delete one entry, delete a site from history, clear all | ✅ |
| **Delete browsing data** (Settings): history, cookies and site data (Keep List cookies stay), cache, open tabs; everything in each ticked category, **no time ranges** | ✅ |
| Time ranges for deleting history ("last hour" etc.) | ⏭️ Later (see checklist) |
| **Background tabs** (Settings → Advanced): Automatic by phone memory (default), Keep recent tabs (0/1/2/3/5), Keep all (with a warning) | ✅ |
| Back from a tab's first page opened from the home page returns to the home page (curved ↩ arrow) | ✅ |
| **Appearance**: Dark (default), Light, or match the phone; pages follow it (light: white pages; dark: pages without a dark style are darkened) | ✅ |
| Menu: "New tab group" groups the current tab with a new tab beside it; inside a group, "New tab in this group" | ✅ |
| Tabs opened in the background start loading at once; an unloaded tab shows its saved picture while it reloads | ✅ |
| Corn's own error page when a site can't load; typed addresses fall back to http when a site has no https | ✅ |
| File uploads (Android file picker) and downloads (confirm, then the phone's Downloads folder; files survive Flush) | ✅ |
| Sites' camera, microphone and location: Allow / Block per site, remembered for the session only | ✅ |
| Address-bar suggestions from History and the unlocked Vault, on the phone only; tapping the bar shows an empty box with the current page (Share, Copy, Edit) | ✅ |
| Find in page; Text size (S/M/L/XL) in Settings → Appearance | ✅ |
| Picture-in-picture video; print / save as PDF | ⏭️ Later |
| **Corn's own password store** (inside the Vault), survives a Flush: save/update/never prompts, fill on tap with PIN when the Vault is locked (switchable), Passwords screen | ✅ v1 (built) |
| **Split screen** inside Corn: two tabs, top/bottom (side by side in landscape), draggable divider | ✅ (built) |

### 6.5 Vault

| Requirement | Status |
|---|---|
| **The Vault replaces bookmarks.** No separate bookmarks folder: the Vault is for saving links; Favourites are quick-access tiles on the new tab | ✅ |
| **Favourites** (layout A+): a scrolling row under the new tab's search box, the Vault tile fixed at the start and a "+ Favourite" tile last; "View all" under the row (5+ favourites) opens a bottom drawer with every favourite (4 across, "+ Add"). Their own list (not Vault links), added from a Vault link's long-press or by typing an address; long-press a tile to Remove / Move left / Move right. Shown without the PIN, kept by Flush. Site icons (one per site) from pages Corn showed, letters until then | ✅ |
| Save the current page from the Vault icon in the address bar (a safe with a + in the corner; a ✓ once saved) → sheet to pick a Collection + optional tags | ✅ |
| Save links with their titles | ✅ |
| Optional tags | ✅ |
| **Collections**: one level, optional: each link is saved once, in one Collection or none; **All** shows every saved link; tags cut across | ✅ |
| Thumbnails (screenshot, placeholder or none) | ⏭️ Not v1 |
| Stored only on the device (app-private SQLite, protected by Android's sandbox and phone encryption) | ✅ (SQLCipher dropped, see decision log) |
| **Vault locked by default** with a 4-digit PIN | ✅ |
| Fingerprint / face unlock | ⏭️ Later |
| Permanent delete (long-press a saved link) | ✅ |
| Export: links (HTML bookmarks, recommended, or CSV) and passwords (Chrome-format CSV, plain text, behind the PIN and a warning); see [13-vault-export](../02-technical/13-vault-export.md) | ✅ |
| Import, full Corn backup | ⏭️ |
| Saving downloaded files into the Vault | ⏭️ |

### 6.6 Privacy and discretion

| Requirement | Status |
|---|---|
| **Black screen in the recent-apps view** | ✅ default on |
| **App Lock** (PIN to open Corn): optional, off by default | ✅ |
| **No notifications while Corn is closed**; media controls (if background playback is on) show only "Corn — playing" | ✅ |
| **No backups of any kind**: Corn turns off Android's automatic Google Drive backup | ✅ |
| Kept Session data stored in app-private SQLite (not separately encrypted) | ✅ |
| No age gate on first launch (Round 24: a self-declared 18+ tap protects no one); Safe Search is offered in onboarding instead | ✅ |
| Disguised app icon | ⏭️ |

### 6.7 Out of scope

| Item | Status |
|---|---|
| Browser extensions | ❌ Out |

## 7. Platforms

| Platform | Status |
|---|---|
| **Android** | ✅ Built first; v1 ships here |
| **iPhone** | ✅ Committed next; mobile-first means both |
| Laptop | ⏭️ Later |

## 8. Defaults

What a new user gets before changing anything.

| Setting | Default | Status |
|---|---|---|
| Session | Keep my session until I flush (Kept Session) | ✅ |
| Auto-Flush after Leaving Corn | Off (options: when I leave / 5 min / 30 min) | ✅ |
| Flush after inactivity | Off (10 / 30 / 60 min; a playing video counts as activity) | ✅ |
| Flush sound | Off | ✅ |
| Flush confirmation | On: "Flush everything?" with "Don't ask again"; ticking it and flushing makes Flush one tap (switch back in Settings) | ✅ |
| Keep List | Empty | ✅ |
| Vault lock | On (4-digit PIN) | ✅ |
| App Lock | Off | ✅ |
| Black screen in recent apps | On | ✅ |
| Notifications while closed | None | ✅ |
| Backups | Off, always | ✅ |
| Search engine | DuckDuckGo, safe search off where the engine allows | ✅ |
| Safe Search | Off | ✅ |
| Safe Browsing | On | ✅ |
| Address bar position | Top | ✅ |
| Theme (Settings → Appearance) | Dark (also Light, or match the phone); pages follow it | ✅ |
| Ad / tracker / pop-up blocking | On for every site (each can be switched off per site) | ✅ |
| Redirect guard | On | ✅ |
| Third-party cookies | Blocked | ✅ |
| Where Vault saves land | No Collection (shown in All) | ✅ |
| Cookie banners | Off: the user handles each site's banner (closer can be switched on) | ✅ |
| Fingerprinting protection + Global Privacy Control | On | 🟡 proposed |
| Link cleaning | On | 🟡 proposed |
| HTTPS upgrade | On | 🟡 proposed |
| Background tabs | Automatic by phone memory: 1 kept in memory on 4 GB, 2 on 6 GB, 3 on 8 GB, 5 on 12 GB+; the rest unloaded | ✅ |
| Keep history | On (cleared by every Flush) | ✅ |

## 9. Success measures (draft)

- A new user can browse, open a Tab Group, save to the Vault and flush within a minute of first launch, with no account.
- Switching between tabs and groups feels instant on a mid-range Android phone.
- Most tested sites work with blocking on.
- After a Flush, a test site sees a brand-new visitor, except Keep List sites.
- Corn's own network traffic never contains browsing data.
