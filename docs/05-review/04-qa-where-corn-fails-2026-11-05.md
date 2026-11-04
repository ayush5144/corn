# 04 — QA Report: Where Corn Fails (2026-11-05)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-05 · **Last updated:** 2026-11-05, 03:00 IST

A hands-on QA pass on the emulator (`corn_pixel`, WebView DevTools via `adb forward`), looking for places where Corn breaks a normal user flow. **No app code was changed.** The one setting changed for a test (pop-up blocking off for ayushpatil.com) was turned back on, and the site settings were checked afterwards (`site_shields = {}`).

**How things were measured:**
- **Taps** were real screen taps (`adb input`).
- **Page state** (classes, colours, fullscreen state, YouTube's player data) was read from inside the page through Chrome DevTools (`Runtime.evaluate`).
- **"Did a request happen?"** was answered from the local test server's access log, so there was no guessing from the screen.
- **Screenshots** were taken with `adb screencap`; on-screen pixels were sampled with Python.

## Summary

| # | Problem | Severity | Confirmed? | Fix size |
|---|---|---|---|---|
| 1 | Tapped links that open in a new tab are blocked as "pop-ups" | **High** (breaks links, Google sign-in, share buttons) | ✅ Yes, on 3 sites + a test page | Small |
| 2 | Pages forced dark in dark mode; a site's own "light" switch can't win | **High** (pages look wrong, site toggles broken) | ✅ Yes, read from the page + pixels | Small |
| 3 | YouTube ads aren't removed | Medium | ✅ The cause is confirmed (no scriptlets); an ad playing wasn't captured | Medium–large |
| 4 | Fullscreen video in split screen covers the whole phone, not its pane | Medium | ✅ Yes | Medium |
| 5 | Links to other apps (phone, email, SMS, maps, "open in app") do nothing | **High** | ✅ Yes, all 5 types | Small |
| 6 | Long-pressing a plain image shows no menu (no save/open/share) | Medium | ✅ Yes | Small |
| 7 | A blocked pop-up still looks "opened" to the page | Low | ✅ Yes | Comes with #1 |
| 8 | Fullscreen once got stuck (Back didn't exit) | Unknown | ❌ Seen once, not reproduced in 3 tries | — |

**What passed:**
- **PDF links:** a "Download this file?" sheet with name, size and site appeared, and the file was saved to Downloads.
- **Session restore after Corn is killed:** 7 tabs before, 7 tabs and the same page after.
- **Back from fullscreen**, both in a single tab and in split screen.
- **Scripted pop-ups with no tap** are blocked: 0 requests reached the server.

---

## 1. Tapped links are blocked as pop-ups

**What the user sees:** on ayushpatil.com, tapping "link" next to Vishlyst shows the toast "Blocked a pop-up", and nothing opens.

**Cause:**
- `browser/CornTab.kt`, `onCreateWindow(view, isDialog, isUserGesture, resultMsg)`. WebView passes `isUserGesture = true` when the user tapped. Corn **ignores it** and blocks every new-window request while the site's "Pop-ups & redirects" blocking is on, which is the default.
- With `setSupportMultipleWindows(true)`, every `target="_blank"` link ("open in a new tab") goes through `onCreateWindow`, so all of them get blocked.

**Evidence:**

| Test | Result |
|---|---|
| ayushpatil.com, tap Vishlyst "link" | Toast "Blocked a pop-up"; tab count unchanged |
| Same, with pop-up blocking turned off for that site only | vishlyst.com opened in a new tab (6 → 7 tabs) |
| tailwindcss.com, tap a sponsor link (mux.com) | Toast "Blocked a pop-up" (screenshot) |
| Test page: tapped `<a target=_blank>` | Blocked; **0** requests for its page in the server log |
| Test page: tapped button running `window.open()` | Blocked; **0** requests |
| Test page: `window.open()` with **no** tap (after 3 s) | Blocked (correct); **0** requests |

**How common this is** (anchor tags in each site's HTML, counted with curl and a mobile Chrome user agent):

| Site | Links | Open in new tab |
|---|---|---|
| ayushpatil.com | 25 | **24** |
| tailwindcss.com | 80 | **48** |
| github.com (a profile) | 103 | 12 |
| scubasearch.io | 4 | 2 |
| news.ycombinator.com, en.wikipedia.org, bbc.com/news | 227 / 1683 / 236 | 0 |

Link-in-bio pages, portfolios, docs sites and GitHub are hit hardest. News sites and Wikipedia aren't hit at all.

**Same root cause, wider damage (from the `window.open` test):** these features all open their window from a tap, so Corn blocks them today:
- "Continue with Google / Apple / Microsoft" sign-in windows
- PayPal-style checkout windows
- "Share on X / Facebook" buttons

A live Google sign-in wasn't tested.

**Fix (the browser standard; Chrome, Firefox, Brave and DuckDuckGo all do this):**
- In `onCreateWindow`, **if `isUserGesture` is true, open the request as a new tab** (the existing `host.onPopupAllowed` path), even when pop-up blocking is on.
- Block only when there was no tap.
- Keep the per-site switch for the rare site that abuses taps.
- Optional: like Chrome, when the site's "Pop-ups & redirects" blocking is on, block tapped pop-ups that a page opens repeatedly, for example more than one within a second.

## 2. Forced darkening: the site's "light" mode stays black

**What the user sees:**
- On ayushpatil.com in Corn's dark mode, switching the site to light keeps the page black.
- Sites without a dark mode (for example Hacker News) are shown dark even though they were designed light.

**Cause:**
- `browser/CornTab.kt`: when Corn's theme is dark, Corn calls `WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)` and gives the page a dark default background (`0xFF0F0E0B`).
- With darkening allowed, **WebView recolours any page that presents itself as light**, so a site's own switch to light gets darkened straight back.
- Corn also tells pages `prefers-color-scheme: dark` (through the WebView's theme), which is correct and should stay.

**Evidence (read from inside the page after clicking ayushpatil.com's theme button):**

| | Before click | After click |
|---|---|---|
| `<html class>` | `dark` | `light` |
| `localStorage.theme` | `dark` | `light` |
| `color-scheme` | dark | light |
| Body background / text | `rgb(0,0,0)` / `rgb(255,255,255)` | **`rgb(255,255,255)` / `rgb(0,0,0)`** |
| **On screen (screenshot)** | Black page, white text | **Still black page, white text** |

So the site switched correctly; WebView painted over it.

**Hacker News:**
- Its declared background is `rgb(246,246,239)` (beige).
- Pixels sampled on screen were `(15,15,13)` and `(26,24,19)`, which is near-black.
- That is Corn's darkening, not the site's choice.

**Tailwind:** inconclusive. After switching to its light theme, the page reported `color-scheme: light`, but its text stayed white. The click also opened the site's menu, so this isn't counted.

**Why darkening was turned on:** in light-mode work, some pages showed black text on Corn's dark default background. Darkening hid that problem; it didn't fix it.

**Fix (recommended):**
- **Pages follow the site:** turn algorithmic darkening off by default.
  - Sites with a dark mode still go dark automatically, because Corn keeps telling them `prefers-color-scheme: dark`.
  - Site toggles then work, and light-only sites look as designed. This is Chrome's default on Android.
- **White default page background** (the web's default) instead of `0xFF0F0E0B`. This removes the black-text-on-black problem at its source.
- **Optional:** a Settings switch "**Darken all websites**", off by default, for people who want everything dark. Explain on the switch that it overrides sites' own themes.

## 3. YouTube ads aren't removed

**What the user expects:** with Brave's engine (adblock-rust), YouTube ads should be skipped or never play.

**Cause: Corn uses Brave's engine, but not the part that handles YouTube.**
- **Network blocking can't remove YouTube video ads.** The ad video comes from the same servers (`googlevideo.com`) as the real video. Brave and uBlock Origin remove YouTube ads with **scriptlets**: small scripts injected into the page before YouTube's own code runs. They edit YouTube's player data, for example deleting `adPlacements` from `ytInitialPlayerResponse`.
- **Corn's lists contain no YouTube scriptlets.** Corn downloads only EasyList (ads) and EasyPrivacy (trackers) (`browser/CornEngine.kt`, lines 40–41). Counted on 2026-11-05:

  | List | Rules mentioning YouTube | Of which scriptlets (`+js(...)`) |
  |---|---|---|
  | EasyList | 43 | **0** |
  | EasyPrivacy | 22 | **0** |
  | uBlock Origin `filters.txt` (for comparison) | 37 | **15** |

- **Corn's engine has no scriptlet code loaded.** `core/crates/corn-block` builds the engine from the filter lists only. It never loads the "resources" file that holds the scriptlet code (adblock-rust's `use_resources`), so even a `+js(...)` rule would have nothing to run.
- **Even if it did, Corn never runs it.** The engine already returns `injected_script` (exposed through UniFFI in `corn-ffi`), but no Kotlin code reads it (`grep injectedScript` finds nothing on the Android side).

**Evidence on the device:** on three popular videos, YouTube's own player data in Corn still contained an ad schedule.

| Video | `adPlacements` | `playerAds` |
|---|---|---|
| kJQP7kiw5Fk | 1 | 1 |
| JGwWNGJdvx8 | 1 | 1 |
| fJ9rUzIMcZQ | 1 | 1 |

- **This means YouTube still had an ad lined up for each of these videos.** A browser using uBlock Origin's YouTube rules deletes these fields before YouTube reads them.
- **An ad wasn't seen playing** at the moment of sampling (about 14 s after load, `ad-showing` false). That's partly luck: YouTube doesn't play an ad on every view. The cause above doesn't depend on catching one.

**Fix:**
1. **Download the scriptlet resources at runtime** (Brave publishes them, built from uBlock Origin's scriptlets) and load them with adblock-rust's `use_resources`. Don't copy them into the repo; this follows the "lists are downloaded, never copied" rule.
2. **Add a list that has the YouTube rules,** for example uBlock Origin `filters.txt`, or Brave's own list. Check each list's licence before shipping.
3. **Run the scriptlets:** inject `injected_script` at **document start**, before YouTube's code runs. Use `addDocumentStartJavaScript` (already used for the password script), scoped to the page's site, and keep the existing per-site "Ads" switch.

**Honest caveats:**
- YouTube changes its code often to break ad blockers. Brave and uBlock ship rule updates every few days, so Corn would need to refresh lists often (it already downloads lists at runtime).
- YouTube sometimes shows "Ad blockers are not allowed" warnings, or slows playback for blocked ads.
- This is the most work of the fixes in this report, and it's an ongoing cat-and-mouse.

## 4. Fullscreen in split screen covers the whole phone

**What the user expects:** with two YouTube tabs in split screen, tapping fullscreen on one should fill **only that pane**, and the other pane stays visible. That's how Android's own split screen treats Chrome: fullscreen fills the app's half of the screen.

**What happens:** the video takes the whole phone, the other pane disappears, the system bars hide, and Android shows its "Viewing full screen" tip.

**Evidence:**
- Split screen was set up with Bohemian Rhapsody on top and Despacito below (the WebView heights in the view tree were 917/918 px each).
- Fullscreen was requested on one pane. The screenshot shows one video in the middle of an all-black phone.

**Cause:**
- `browser/CornTab.kt`: `onShowCustomView` hands the video's fullscreen view to `CornController.onEnterFullscreen`, which stores it in `c.fullscreen`.
- `ui/CornApp.kt` (around line 70): the stored view is drawn over **everything**, with `Modifier.fillMaxSize()`.
- `MainActivity.kt`, line 74: the system bars are hidden whenever `c.fullscreen` is set.
- Nothing checks whether the tab is in split screen or which pane it's in.

**Fix:**
- Remember **which tab** went fullscreen (`onEnterFullscreen(entry, view, exit)`).
- If that tab is in split screen, draw the fullscreen view **inside that tab's pane**, the same box that holds its WebView in `ui/BrowserScreen.kt`, and **don't hide the system bars**. The other pane keeps playing.
- Outside split screen, keep today's behaviour.
- Optional: a "maximise" button on the in-pane video for when the user does want the whole screen.
- Watch out for exiting fullscreen when the pane is closed, swapped or the split ends: call the stored `exit()` so WebView isn't left believing it's still fullscreen.

## 5. Links to other apps do nothing

**What the user sees:** tapping a phone number, email address, "text us", a map link or "Open in app" on a website does nothing.

**Cause:**
- `browser/CornTab.kt`, `shouldOverrideUrlLoading`: `if (scheme != "http" && scheme != "https") return true // never jump out to other apps silently`.
- Returning `true` means "Corn handled it", but Corn does nothing with it. The comment's goal (no *silent* jumps) is right, but there's no non-silent path.

**Evidence:** test page with five links. After each tap, the foreground app was still Corn. Android itself has an app ready for each one (`cmd package resolve-activity`), so the emulator wasn't missing them.

| Link | Corn | App Android would open |
|---|---|---|
| `tel:` | Nothing | Phone (`com.google.android.dialer`) |
| `mailto:` | Nothing | Gmail (`com.google.android.gm`) |
| `sms:` | Nothing | Messages (`com.google.android.apps.messaging`) |
| `geo:` | Nothing | Maps (`com.google.android.apps.maps`) |
| `intent://…` | Nothing | (the named app, or its `browser_fallback_url`) |

**Fix:**
- **Only after a tap, ask:** "Open in Phone?" with Open / Cancel (a small sheet), then start the app with an `Intent`. That keeps the "never silently" rule.
- **`intent://` links:**
  - Parse them with `Intent.parseUri(url, Intent.URI_INTENT_SCHEME)`.
  - Strip the component and selector, and add `CATEGORY_BROWSABLE` (the standard safety steps from Android's docs, to stop pages opening internal parts of other apps).
  - If no app can open it, load its `S.browser_fallback_url` in the tab.
- **Never open anything without a tap,** so a page can't throw the user into another app.
- **Privacy:** opening another app hands it the link, but only when the user chooses it. That's expected and should be stated on the sheet.

## 6. Long-pressing an image shows no menu

**What the user sees:** long-pressing a plain picture (one that isn't a link) shows nothing. There's no way to save, share or open the image.

**Cause:** `browser/CornTab.kt`, `onLongPress()`, handles only `SRC_ANCHOR_TYPE` (a link) and `SRC_IMAGE_ANCHOR_TYPE` (an image that is a link). For `IMAGE_TYPE` (a plain image) it returns `false`, so nothing happens.

**Evidence:** a test page with one full-width image; after a 1.2 s long-press on it, the screen had **0** menu items.

**Fix:** handle `IMAGE_TYPE` and show an image sheet with:
- **Save image**, through the existing `DownloadManager` path (so it shows the same "Download this file?" sheet and the "stays after a Flush" note)
- **Open image in new tab**
- **Share**
- **Copy image link**

Keep the existing link sheet for linked images, with "Save image" added.

## 7. A blocked pop-up still looks "opened" to the page

**Evidence:** on the test page, the scripted `window.open()` with no tap was blocked (0 requests reached the server), yet the page's own check `w ? 'opened' : 'blocked'` printed **"opened"**. WebView returns a window object even though Corn refused the window.

**Why it matters:** some sites check whether `window.open` returned null, and fall back to opening the page in the same tab or showing "please allow pop-ups". With Corn, they think it worked, so the user is left with nothing.

**Fix:** mostly fixed by #1 (tapped pop-ups will open). For the rest, this is WebView behaviour, so there's no clean fix inside Corn; the "Blocked a pop-up" toast is the user's cue. A possible later improvement: give the toast an "Open anyway" action.

## 8. Fullscreen got stuck once (not reproduced)

- **During the split-screen test, Back seemed not to leave fullscreen** several times. In between, a PDF link had been opened from outside Corn, and Android's "Viewing full screen" tip was still showing.
- **Retried 3 ways, and each time one Back left fullscreen:**
  - a single tab
  - split screen
  - fullscreen, then a link opened from outside
- **The likely explanation** is that the first Back went to Android's tip, and later Backs raced with the new tab opening. That isn't proven.
- **Code note:** `CornController.back()` calls the stored `exitFullscreen` but doesn't clear `c.fullscreen` itself; it waits for WebView's `onHideCustomView`. If a fullscreen tab were ever closed or unloaded mid-fullscreen, nothing would clear it.
- **Cheap safeguard (worth adding with #4):** clear `fullscreen` in `back()` too, and exit fullscreen whenever its tab is closed or unloaded.

---

## What passed

| Flow | Result |
|---|---|
| PDF link (w3.org dummy.pdf) | "Download this file? dummy.pdf · 13 kB · from w3.org" sheet with Download, and the Downloads note |
| Corn killed in the background, then reopened | 7 tabs before, 7 after; the same page was showing |
| Back from fullscreen (single tab, split screen) | One Back exits, and Corn's bars come back |
| Pop-up with no tap | Blocked; 0 requests |
| Two live YouTube videos in split screen | Both panes load and play (two WebViews of 917/918 px) |

## Suggested order

| Order | Fix | Why first |
|---|---|---|
| 1 | #1 Tapped new-tab links + sign-in windows | Breaks basic links and logins on many sites; a few lines |
| 2 | #5 Links to other apps | Phone, email, maps and "open in app" are dead today; small |
| 3 | #2 Follow the site's theme | Pages look wrong in dark mode; small |
| 4 | #6 Image long-press menu | Common action, small |
| 5 | #4 Fullscreen inside its split pane (+ #8 safeguard) | Split-screen polish |
| 6 | #3 YouTube scriptlets | Biggest piece and ongoing upkeep; worth doing well |

## Not covered

- A real Google / Apple sign-in flow (#1 says why it likely fails).
- A real phone (all tests were on the emulator).
- Linktree: its links are built by JavaScript, so curl couldn't count them. They likely open in new tabs (#1), but that wasn't checked in Corn.
- Video ads other than YouTube's, other sites' theme switches beyond those listed, and printing or PiP (not built yet).
