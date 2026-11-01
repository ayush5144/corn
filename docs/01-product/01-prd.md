# 01 — Product Requirements (PRD)

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
| Blocking of ads, trackers, pop-ups and redirect abuse, on by default | ✅ |
| Per-site blocking toggle when a page breaks | ✅ |
| Cookie-banner auto-dismiss (DuckDuckGo autoconsent) | 🟡 proposed |
| Fingerprinting protection + Global Privacy Control (DuckDuckGo content-scope-scripts) | 🟡 proposed |
| Strip tracking parameters and unwrap redirect links (ClearURLs rules) | 🟡 proposed |
| Third-party cookies blocked; HTTPS upgrade | 🟡 proposed |
| Fullscreen video and Picture-in-Picture | ✅ |
| File downloads | ⏭️ |

### 6.2 Tabs and Tab Groups

| Requirement | Status |
|---|---|
| Open a link in a **new tab** or a **new tab in the current Tab Group** | ✅ |
| **Tab Group Strip** at the bottom (like Chrome Android): one round site icon per tab in the group; tap to switch instantly | ✅ |
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
| Switch engine from **one dropdown inside the search bar** | ✅ |
| Default engine: **DuckDuckGo**, safe search off where the engine allows | ✅ |
| User-added custom engines | ⏭️ |

### 6.4 Flush and session behaviour

| Requirement | Status |
|---|---|
| **Flush Button**: one tap, no confirmation, wipes the Session (tabs, Tab Groups, cookies, cache, history, previews) | ✅ |
| Flush Button is a **toilet/flush** button (not a power button); visual styling is open | ✅ (styling 🟡) |
| Flush Button sits in the **bottom toolbar, at the far right or second from the right** | ✅ |
| Default: **Kept Session**: the Session survives until the user flushes | ✅ |
| Setting: **Auto-Flush after leaving Corn**, user chooses the delay or never | ✅ |
| Delay presets: **when I leave Corn**, **5 min after**, **30 min after** (plus Never) | ✅ |
| Setting: **Auto-Flush after inactivity**: 10, 30 or 60 minutes | ✅ |
| Flush feedback: quick swirl animation, vibration, funny one-liner on the empty page; sound is a setting, off by default | ✅ |
| **A playing video counts as activity** | ✅ |
| "Leaving Corn" = switching apps, going home or locking the phone | ✅ |
| **Keep List**: chosen sites stay logged in across Flushes | ✅ v1 |
| **Corn's own password store** (inside the Vault), survives a Flush | ✅ (tentatively v1.1 🟡) |

### 6.5 Vault

| Requirement | Status |
|---|---|
| **The Vault replaces bookmarks.** Corn has no separate bookmarks or favourites | ✅ |
| Save the current page from the Vault icon in the address bar → sheet to pick a Collection + optional tags | ✅ |
| Save links with their titles | ✅ |
| Optional tags | ✅ |
| **Collections**: one level, each item in exactly one Collection (default "Unsorted"); tags cut across | ✅ |
| Thumbnails (screenshot, placeholder or none) | ⏭️ Not v1 |
| Encrypted on device | ✅ |
| **Vault locked by default** with PIN or biometrics | ✅ |
| Export, import, permanent delete | 🟡 |
| Saving downloaded files into the Vault | ⏭️ |

### 6.6 Privacy and discretion

| Requirement | Status |
|---|---|
| **Black screen in the recent-apps view** | ✅ default on |
| **App Lock** (PIN/biometrics to open Corn): optional, off by default | ✅ |
| **No notifications while Corn is closed**; media controls (if background playback is on) show only "Corn — playing" | ✅ |
| **No backups of any kind**: Corn turns off Android's automatic Google Drive backup | ✅ |
| Corn encrypts the Session data it controls (tab list, Tab Groups, URLs) | ✅ |
| One-time, lightweight 18+ confirmation on first launch | ✅ |
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

## 8. Success measures (draft)

- A new user can browse, open a Tab Group, save to the Vault and flush within a minute of first launch, with no account.
- Switching between tabs and groups feels instant on a mid-range Android phone.
- Most tested sites work with blocking on.
- After a Flush, a test site sees a brand-new visitor, except Keep List sites.
- Corn's own network traffic never contains browsing data.
