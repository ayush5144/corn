# 05 — Flush Model: "Use Whenever You Want"

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
| Android | `ProfileStore.deleteProfile()` (androidx.webkit) | Very clean. Data is written to disk, then deleted. |
| iPhone | `WKWebsiteDataStore.nonPersistent()` | Cleanest. Nothing is written to disk. |
| Laptop | Electron session without the `persist:` prefix | Cleanest. Memory only. |

**Android fallback:** where `MULTI_PROFILE` isn't supported, clear `CookieManager`, `WebStorage`, the WebView cache and form data manually, then recreate all WebViews.

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
| Cookies, site storage, cache | ✅ Cleared | Except Keep-list sites, if enabled |
| History | ✅ Cleared | Corn keeps session history only |
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
