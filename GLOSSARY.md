# Flush

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-11, 23:02 IST

Flush is a free, private, "use whenever you want" browser for adults. This glossary fixes the words we use for its product concepts so docs, UI copy and code all mean the same thing.

## The app

**Flush (the app)**:
The browser's name; "Flush Browser" in full where context is needed (store title, press). The app is named after its main action, so write "the Flush Button", never "Flush's Flush Button". When "Flush" could mean either the app or the action, say "the app".
_Avoid_: FlushBrowser, Flush app (in UI copy)

**Corn**:
The browser's name until 2026-11-11 (decision log Round 22). Historical records keep it.
_Avoid_: Corn in new docs, UI copy or code

## Session and Flush

**Session**:
Everything the browser holds from current browsing: open tabs, cookies, site data, cache, history and tab previews.
_Avoid_: Profile, browsing data (when meaning the whole session)

**Flush**:
Wiping the current Session from the device, leaving the Vault, settings and Keep List untouched.
_Avoid_: Clear, burn, fire, reset, wipe

**Flush Button**:
The one-tap control that triggers a Flush immediately.
_Avoid_: Fire button, panic button, power button

**Auto-Flush**:
A Flush that the app triggers on the user's behalf, according to their chosen setting (on app close or after inactivity).
_Avoid_: Auto-clear, timer flush

**Kept Session**:
The default setting, under which the Session survives until the user presses the Flush Button.
_Avoid_: Persistent mode, normal mode

**Leaving the app**:
The user switching to another app, going to the home screen or locking the phone while Flush is open. (Called "Leaving Corn" before the rename.)
_Avoid_: Closing, exiting, backgrounding, "leaving Flush" (reads like the action)

**Keep List**:
Sites the user has chosen to stay logged in to across Flushes. Flush keeps their cookies and IndexedDB; everything else about them is still deleted.
_Avoid_: Fireproof, whitelist, allowlist

## Tabs

**Tab**:
One open web page in the Session.

**Tab Group**:
A set of Tabs the user browses together, opened from one another, switchable in one tap from the Tab Group Strip.
_Avoid_: Tab stack, tab folder, workspace

**Kept in memory**:
A tab other than the open one whose page stays loaded, so it reopens instantly where it was. Its videos stay paused; its scripts keep running, slowed down. A tab opened in the background starts loading at once and counts as kept. How many: Settings → Advanced → Background tabs.
_Avoid_: Paused, sleeping

**Unloaded**:
A tab whose page has been closed to save memory. Flush keeps its preview, back/forward history and scroll position; opening it reloads the page.
_Avoid_: Frozen, discarded, suspended

**Split screen**:
Two tabs shown at once inside Flush, top/bottom (side by side in landscape), with a draggable divider. The pane you last touched is the one the toolbar acts on.
_Avoid_: Dual view, multi-window

**Tab Group Strip**:
The bar at the bottom of the screen showing one round site icon per Tab in the current Tab Group.
_Avoid_: Tab bar, group bar, bubbles

## Vault

**Vault**:
The user's private, local, PIN-locked collection of things they explicitly chose to save; Flush's replacement for bookmarks. It survives every Flush.
_Avoid_: Walls, bookmarks, favourites, library

**Vault Item**:
One saved thing in the Vault: a link with its title, plus optional tags and thumbnail.
_Avoid_: Bookmark, save, entry

**Collection**:
A named, single-level grouping of Vault Items; each Vault Item belongs to at most one Collection (picking one is optional).
_Avoid_: Vault group, folder, album

**All**:
The Vault view showing every saved Vault Item, in a Collection or not. Not a stored Collection: each link is saved once.
_Avoid_: Unsorted, Inbox, default folder

## History

**History**:
Flush's list of pages visited, stored only on the phone, cleared by every Flush. "Keep history" switches recording off.
_Avoid_: Browsing log, activity

**Delete browsing data**:
The Settings action that deletes everything in the chosen categories (history, cookies and site data, cache, tabs), without a full Flush. No time ranges.
_Avoid_: Clear data, wipe

## Privacy

**Privacy Screen**:
The black screen Flush shows in the operating system's recent-apps view instead of page content.
_Avoid_: Blur, recents cover

**App Lock**:
The optional PIN or biometric check required to open Flush.
_Avoid_: Passcode, app PIN

**Blocking**:
Flush stopping ads, trackers, pop-ups and redirect abuse before they load.
_Avoid_: Shields, ad block (as a product name)

## Search

**Search Switcher**:
The dropdown inside the search bar that changes the search engine for the current search.
_Avoid_: Engine picker, search selector
