# Corn QA on the emulator

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-05 · **Last updated:** 2026-11-05, 10:13 IST

Repeatable checks for the problems found in [the 2026-11-05 QA pass](../docs/05-review/04-qa-where-corn-fails-2026-11-05.md). One command taps through real flows on the emulator and prints PASS or FAIL for each check, with what it measured.

## Run

```sh
scripts/dev-start.sh --install     # once: emulator + a debug build (DevTools must be on)
scripts/qa-run.sh                  # every check, about 4 minutes
scripts/qa-run.sh popups-          # every check starting with "popups-"
scripts/qa-run.sh theme-light-only-page fullscreen-back-exits
scripts/qa-run.sh --list
```

- Exit code: 0 when every check passed, 1 when any failed, 2 when it couldn't start.
- It serves `qa/pages/` on port 8770 (`QA_PORT` to change) through `scripts/dev-start.sh --serve`, and stops that server when it ends.
- It installs nothing, clears nothing and changes no settings. It opens a few tabs (the first page in a new tab, later pages reuse it; pop-ups and split screen add more), visits the dialer, Gmail, Messages and Maps, and leaves Corn in front. Flush afterwards for a clean emulator.
- Don't touch the emulator while it runs. `QA_DEBUG=1` prints tracebacks; `QA_NEW_TABS=1` opens every page through Android in a new tab.

## How a check decides

A check passes only on evidence, never on a wait alone:

| Evidence | How |
|---|---|
| Did a page load? | The test server's request log. Every page is opened with `?r=<run>-<check>`, and `qa.js` gives each target link a unique `hit.html?n=<name>&r=…`, so a log line belongs to exactly one check. |
| Did the tap land? | `qa.js` records the id of every clicked element; a tap that missed fails the check as a setup error, not as the bug. |
| Page state | Chrome DevTools (`Runtime.evaluate`) over the forwarded `webview_devtools_remote_<pid>` socket. Reads only: it never fakes a tap. |
| Which app is in front | `dumpsys activity activities`; the app Android would pick comes from `cmd package resolve-activity`. |
| Corn's sheets and menus | The `uiautomator` tree (text outside the WebView). |
| What's on screen | Raw `screencap` pixels, sampled where the page is plain background. |

Taps are real `adb shell input` taps at element positions read from DevTools.

## Checks

| Check | Proves | Passes when |
|---|---|---|
| `popups-tapped-link-opens` | A tapped `target=_blank` link opens | Its page is requested |
| `popups-tapped-open-opens` | A tapped `window.open` (sign-in, share buttons) opens | Its page is requested |
| `popups-no-tap-blocked` | A `window.open` with no tap stays blocked | The page's timer ran and its page was never requested |
| `popups-self-close-returns` | A pop-up that calls `window.close()` (sign-in windows) closes and returns to its opener | The pop-up loaded, its close timer ran, then the opener is on screen and the pop-up isn't |
| `popups-burst-one-window` | One tap can't open a pile of windows | Exactly 1 of 3 windows from one tap loads (Chrome's rule: one pop-up per tap) |
| `schemes-tel/mailto/sms/geo-ask-then-open` | Links to other apps ask, then open the right app | A sheet with "Open…" appears (Corn still in front), and after Open the app in front is the one `resolve-activity` names |
| `schemes-no-tap-ignored` | A page can't throw you into another app without a tap | After a no-tap `location = 'tel:…'`, Corn is in front and no "Open…" sheet shows |
| `intent-fallback-loads` | `intent://` for an app that isn't installed loads its `S.browser_fallback_url` | The fallback page is requested |
| `theme-site-toggle-light` | A site's own light switch wins over Corn's dark mode | After tapping the site's "light" button, screen pixels are white like the page declares |
| `theme-light-only-page` | A light-only site looks as designed | Screen pixels match the page's beige `rgb(246,246,239)` |
| `image-long-press-menu` | Long-press on images offers Save image | Both the plain and the linked image's sheet contain "Save image" |
| `link-title-not-duration` | The link sheet shows the video's title, not its duration overlay | The sheet shows "Sunrise over a quiet lake" and not "4:02" |
| `fullscreen-back-exits` | Back leaves video fullscreen (QA report #8) | One Back ends `document.fullscreenElement`, the same page is on screen and Corn is in front |
| `fullscreen-split-pane` | Fullscreen in split screen stays in its pane | With pane B fullscreen, pane A's area still shows its green page (pixels) |

The theme checks only exercise forced darkening when Corn's pages are dark (`prefers-color-scheme: dark`); the result line says which it was.

## Files

- `qa/pages/`: the test pages (plain HTML, neutral content, nothing third-party). `qa.js` is the shared helper, `hit.html` the target every link points to, `test.mp4` and `picture.png` were generated with ffmpeg (a test pattern and two colour blocks).
- `scripts/qa-run.sh`: starts the server and runs the checks.
- `scripts/qa/run.py`: the checks. `device.py`: adb, the UI tree, pixels (`python3 -I scripts/qa/device.py tree` prints the tree). `cdp.py`: DevTools (`python3 -I scripts/qa/cdp.py pages`).
