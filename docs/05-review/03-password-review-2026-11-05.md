# 03 — Password Store Code Review (2026-11-05)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-05 · **Last updated:** 2026-11-05, 02:00 IST

A read-through of every file of the password store by Claude (the main session), after it was built by an agent and tested on the emulator. Files read: `passwords/LoginCipher.kt`, `LoginKeys.kt`, `LoginMatching.kt`, `LoginScript.kt`, `Passwords.kt`, `ui/PasswordsUi.kt`, and the `logins` table in `core/crates/corn-vault`. How the encryption works: [12-security](../02-technical/12-security.md).

**Verdict: sound for testing and daily use.** No bug found that leaks a password. A few defence-in-depth gaps are listed below.

**Status (2026-11-05, founder's decisions):** #4 **fixed**; #1 kept as is for now (later); #5, #6 recorded as info; #7 later; #2 and #3 waiting for the founder.

## What's done right (checked in the code)

| Area | What the code does |
|---|---|
| Encryption | AES-256-GCM, a fresh random 12-byte IV per value, 128-bit tag; tampering makes decryption fail instead of returning wrong data |
| Binding | Site + field (`corn-login/v1|<site>|u` or `|p`) as associated data: a blob can't be moved to another site or swapped with the username |
| Key | Random 256-bit data key, stored only wrapped by a non-exportable Android Keystore key (StrongBox first, then the normal keystore); the temporary copy of the key bytes is wiped after use |
| Lost key | Never makes a new key over existing passwords; offers "Reset passwords" instead |
| Which site | The site is taken from WebView's record of where a message came from (`sourceOrigin`), never from the page's own message, so a page can only trigger a save for itself |
| Frames | Only the main frame, or a frame with the main page's exact site, is listened to |
| https | Only https pages in release builds (local test addresses only in debug builds) |
| Filling | Only after the user's tap; sent only to the frame that showed the form, only if its site equals the login's |
| Navigation | A tab starting a new page forgets its form connection and fill chip (`onPageStarted`) |
| PIN | Reveal, copy and edit of a password always go through the PIN; filling needs it when the Vault is locked (switchable) |
| Screen | `FLAG_SECURE` while the Passwords screens are open (verified: screenshots are black) |
| Clipboard | Copies marked sensitive (Android 13+), cleared after 45 s (only if it's still Corn's copy) |
| Logs | Only error types and counts; never sites, usernames or passwords |
| Disk | `secure_delete` + write-ahead log emptied after changes; verified no username or password text in the raw database files |

## Findings

| # | Severity | Finding | Suggested fix |
|---|---|---|---|
| 1 | Medium | **No limit on wrong PIN attempts**, and the 4-digit PIN's salted hash in the settings file could be brute-forced by someone who extracts it (needs root). Passwords stay encrypted either way (their key is in the Keystore), but the Vault screens open | Growing wait after a few wrong tries; check the PIN with a Keystore-held key (HMAC) so the hash can't be cracked off the phone |
| 2 | Low | **Fill doesn't re-check the tab's current site for the main frame.** It relies on `onPageStarted` having cleared the old form connection when the tab navigated. That works today, but one extra check (current page's site = login's site) would make it safe even if a navigation event were ever missed | Add the check in `Passwords.fill()` |
| 3 | Low | **Closed tabs' form connections and chips are never removed** (cleared only on that tab's next page load). Small memory leak; no security effect (the reply channel of a closed tab goes nowhere) | Remove them when a tab closes |
| 4 | Low | **Clipboard isn't cleared if Corn is closed within the 45 s** (the timer lives in the app). Android 13+ still hides it from previews | ✅ **Fixed:** Corn also clears its copy the moment it leaves the screen, and on its next start if it was closed first (a saved "copy pending" flag). Verified: pasting inside Corn gave the password; after leaving and returning, pasting gave nothing |
| 5 | Info | Any https page can send Corn fake "login" messages, but only for **its own site**: worst case, repeated "Save password?" prompts on that site | Optional: limit prompts per page |
| 6 | Info | On submit, Corn decrypts the saved password in memory (no PIN) to decide between "Save", "Update" and "already saved". Normal for password managers; never shown or logged | None |
| 7 | Gap | Two-step logins (username and password on separate pages): only the password is saved | Remember the username from the previous page |

## Not covered by this review

- Behaviour on a real phone with a StrongBox chip (the emulator uses the fallback keystore).
- Real-world sites (Google, Reddit, GitHub logins).
- An independent human security review before a public launch.
