# 05 — Flush Model: "Use Whenever You Want"

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 20:14 IST

## The idea

Corn's Flush Button works like DuckDuckGo's Fire Button: one tap wipes the Session, and only the Vault, settings and Keep List survive.

**Default (Round 1 decision): Kept Session.** The Session survives until the user flushes. Auto-Flush after Leaving Corn (user-chosen delay) and Auto-Flush after inactivity are opt-in settings. A playing video counts as activity. The lifecycle below shows the Auto-Flush path.

## Lifecycle

```
   Open Corn
       │
       ▼
 ┌──────────────────────┐
 │ Fresh, empty session │  no cookies, no history, no cache
 └──────────┬───────────┘
            ▼
        Browse  ──── save something? ──► VAULT (PIN-locked)
            │                              └─ survives every flush
            ▼
 ┌─────────────────────────────────────┐
 │ FLUSH triggers:                     │
 │  🔥 Flush button                     │
 │  📱 App closed or sent to background │
 │  ⏱  X minutes idle (user's choice)   │
 └──────────┬──────────────────────────┘
            ▼
 Close all tabs → delete cookies, cache, history,
 tab previews → back to a fresh session
            │
            ▼
   "Keep list" sites (optional) stay logged in
```

## How each platform does it

| Platform | Tool that makes Flush work | How clean it is |
|---|---|---|
| Android | Built: edit WebView's cookie database (DuckDuckGo's method) + delete WebView's storage files (Keep List IndexedDB kept) + `clearCache`, then restart the process | Complete. Verified on the emulator. |
| iPhone | `WKWebsiteDataStore.nonPersistent()` | Cleanest. Nothing is written to disk. |
| Laptop | Electron session without the `persist:` prefix | Cleanest. Memory only. |

**How Android Flush works (built, `DataClearing.kt`):**
1. Close every tab (destroys their WebViews) and delete the Kept Session.
2. Delete history (corn.db), the cache (`clearCache`, all of it), form data, HTTP logins and location permissions.
3. Cookies: open WebView's cookie database (`app_webview/Default/Cookies`) and delete every row except Keep List sites (and their subdomains), then `VACUUM`. Keep List cookies stay exactly as they were.
4. Site storage: WebView's storage files are deleted directly (everything in `app_webview/Default` except the cookie file and Keep List sites' IndexedDB folders). Not `WebStorage.deleteAllData()`: it has no per-site option and finishes in the background (measured: local storage was still on disk after the restart). Details: [11-keep-list](11-keep-list.md).
5. Restart Corn's process (about 0.3 s) so WebView drops everything it held in memory. In the background (Auto-Flush after leaving), Android doesn't allow a relaunch, so the process just ends.

**Fallback:** if the cookie database can't be opened, Corn uses `WebStorage.deleteAllData()` (every site's storage) and `CookieManager.removeAllCookies`, then re-creates Keep List cookies (less exact: their expiry and HttpOnly flags can't be read), with no restart. `ProfileStore` (androidx.webkit) was considered but isn't needed.

## Who decides, who executes

```
Trigger (button / background / timer)
        │
        ▼
Rust corn-flush: build the flush plan
  • which categories to clear
  • which Keep-list sites to spare
        │
        ▼
Platform shell executes the plan
  • close tabs, destroy web views
  • delete profile / data store
  • delete tab previews
        │
        ▼
Fresh session ready
```

## What Flush clears vs keeps

| Data | Flush | Notes |
|---|---|---|
| Open tabs | ✅ Cleared | |
| Cookies, IndexedDB | ✅ Cleared | Except Keep List sites' |
| Other site storage (local storage…), cache | ✅ Cleared | All sites, Keep List included |
| History | ✅ Cleared | Corn's own History (corn.db) |
| Tab previews | ✅ Cleared | Never written outside the session |
| Downloads list | ✅ Cleared | Files saved to the Vault stay |
| Vault | ❌ Kept | Deleted only by a separate, explicit user action |
| Corn settings | ❌ Kept | |
| Keep list | ❌ Kept | |

## Supporting features

- **Privacy screen:** the app looks blank in recent apps (`FLAG_SECURE` on Android, a privacy cover on iOS, window hiding on desktop).
- **App lock:** optional fingerprint, face or PIN on open.
- **Smaller scope:** history, the downloads list and tab restore shrink or disappear. Less to build.

## The promise Corn must not break

Flush clears **this device only**. It cannot erase what websites, internet providers, the operating system, keyboards or other apps recorded. Every Flush screen and the onboarding copy must say so plainly.

## Tests required before claiming a guarantee

- After Flush, a cookie-setting test site sees a new visitor.
- After Flush, no tab previews or cache files remain in app storage (checked with a file-system dump on a debug build).
- Vault items are present and still locked after Flush.
- Keep-list sites stay logged in; non-listed sites don't.
- Background and idle triggers fire on real devices, including after the OS kills the app.
