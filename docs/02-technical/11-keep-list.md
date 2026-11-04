# 11 — Keep List and Data Clearing

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 01:33 IST

## The code is the source of truth

This doc explains the Keep List and how Corn deletes browsing data. The behaviour lives in the code:

| What | Where |
|---|---|
| The whole deletion procedure (Flush, Auto-Flush, Delete browsing data) | `android/app/src/main/java/app/cornbrowser/browser/DataClearing.kt` |
| Cookie deletion that spares Keep List sites | `browser/CookieJar.kt` |
| Restart after deleting | `RestartActivity.kt` |
| Keep List storage (`keep_list` in corn.db) | `data/CornData.kt` |
| History deletion (no traces left in the file) | `core/crates/corn-vault/src/lib.rs` |

If this doc and the code disagree, the code is right, and this doc should be fixed.

## Goal

Keep List sites **stay logged in** after Flush and Delete browsing data. Everything else is deleted **completely**: nothing half-deleted, no option that only partly works.

## What the Keep List keeps

| Data | Keep List site | Every other site | How |
|---|---|---|---|
| Cookies | ✅ Kept, untouched (expiry and security flags as the site set them) | Deleted | Rows deleted from WebView's cookie database, except Keep List hosts and their subdomains (`CookieJar`) |
| IndexedDB (site databases, often used for logins) | ✅ Kept | Deleted | Every `app_webview/Default/IndexedDB/<scheme>_<host>_<port>.indexeddb.*` folder deleted except Keep List sites' |
| Local storage | ❌ Deleted | Deleted | Android can't delete one site's local storage (see Limits) |
| Other site storage (Service Worker caches, Web SQL, session storage, form data) | ❌ Deleted | Deleted | Files in `app_webview/Default` deleted |
| History | ❌ Deleted | Deleted | The Keep List is about logins, not history |
| Cache (images, scripts) | ❌ Deleted | Deleted | `WebView.clearCache(true)`: one cache for the whole app |

Verified on the emulator (two local test sites, `10.0.2.2` on the Keep List): after Flush the Keep List site still had its cookie and its IndexedDB (readable after the restart); its local storage was gone; the other site had nothing left.

## Adding and removing sites

- **From the page:** shield → **Stay logged in after Flush** switch. One tap adds the site; another removes it. Verified.
- **In Settings → Keep List:** type a site to add it, × to remove.
- Stored in corn.db (key `keep_list`), which survives every Flush. A site and its subdomains count (`example.com` covers `mail.example.com`).

## How a deletion runs

```
Flush / Auto-Flush / Delete browsing data
        │
        ▼
1. Tabs: closed (Flush, or "Open tabs" ticked) or unloaded (so no page holds data in memory)
2. History: deleted in corn.db (secure_delete + write-ahead log emptied)
3. Cache: WebView.clearCache(true)                        (if chosen)
4. Form data, HTTP logins, location permissions: cleared  (with cookies)
5. Cookies: WebView's cookie database edited, Keep List rows kept, VACUUM
        │
        ├── edit worked ──► delete site-storage files (keep Keep List IndexedDB folders)
        │                   ──► restart Corn's process (about 0.3 s)
        │
        └── edit failed ──► WebStorage.deleteAllData() (every site, Keep List included)
                            + CookieManager.removeAllCookies, then re-create Keep List cookies
                            (no restart; less exact: expiry and HttpOnly can't be read back)
```

**Why the restart:** WebView keeps cookies and storage in memory too. After editing its files, only a new process is sure to start from the cleaned files. DuckDuckGo does the same after its Fire Button. In the background (Auto-Flush after leaving), Android doesn't let an app relaunch itself, so the process just ends and the next launch starts clean.

**Why files, not `WebStorage.deleteAllData()`:** it has no per-site option, and it finishes in the background with no "done" signal. Measured: local storage was still on disk after the restart that followed.

## What each option uses: Android's own tools or a workaround

| Data | Method | Android built-in? |
|---|---|---|
| Cache | `WebView.clearCache(true)` | ✅ Built-in. All sites at once; not by site or by time ("the cache is per-application", Android docs) |
| Cookies | Edit WebView's cookie database | 🔧 Workaround (DuckDuckGo's `SQLCookieRemover`). Android's `CookieManager` can only delete every cookie |
| Site storage | Delete WebView's storage files before the restart | 🔧 Workaround (DuckDuckGo deletes its WebView folder the same way). Built-in `deleteAllData()` is the fallback |
| History | Corn's own database | Corn's own: exact |

Verified on the emulator: with "Cached images and files" unticked, all 113 cache files survived a delete; ticked, none of the old files were left (only the 24-byte `index`).

## How DuckDuckGo does it (reference)

DuckDuckGo calls these sites **Fireproof**. From their Android code:
- **Cookies:** the same database edit Corn uses.
- **IndexedDB:** deletes every site folder except Fireproof sites'. Corn does the same (Keep List A).
- **Local storage:** opens WebView's LevelDB with a Java LevelDB library and deletes every key not belonging to a Fireproof site. Falls back to deleting everything.
- **Choosing sites:** a menu item, a Settings list, and an automatic "Would you like to Fireproof this site?" after it detects a login (Ask / Always / Never).

## Limits

- **Local storage isn't kept** for Keep List sites. `WebStorage.deleteOrigin()` covers only Application Cache and Web SQL (Android docs); measured, local storage survived it. A site that keeps its login only in local storage will log you out.
- **No time ranges, no per-site deletion** in Delete browsing data: the cache and site storage can only be deleted all at once, so those options would be incomplete.
- **Depends on WebView's file layout** (cookie database location and columns, IndexedDB folder names). If a WebView update changes it, Corn falls back to deleting everything: still complete, just without the Keep List.

## Later (on the checklist)

- **C: keep Keep List sites' local storage**, using DuckDuckGo's LevelDB method. Needs a new library and must match Chromium's key format.
- **D: offer "Keep this site logged in?" after you log in**, like DuckDuckGo's login prompt.
- Time ranges for deleting history (exact for history only).
