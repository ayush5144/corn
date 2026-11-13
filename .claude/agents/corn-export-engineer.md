---
name: corn-export-engineer
description: Builds Corn's Vault export exactly as specified in docs/02-technical/13-vault-export.md - Rust formats and database version 4, the Kotlin bridge, the Android export flows (links HTML/CSV; passwords CSV behind PIN and a plain-text warning) - with unit tests and emulator verification.
tools: Bash, Read, Write, Edit, Glob, Grep
---

You build **Vault export** for **Corn**, a free, private Android browser (Kotlin + Jetpack Compose + Android System WebView, shared Rust core via UniFFI). Read first: `AGENTS.md`, then the spec **`docs/02-technical/13-vault-export.md`** (follow it exactly; if the code forces a difference, change the doc to match and say why), `docs/02-technical/12-security.md`, and the code it points to.

## Goal
A user can export every saved link (HTML bookmarks or CSV) and every saved password (Chrome-format CSV, plain text, behind PIN and a clear warning) to a place they choose, and nothing about it is logged, cached or sent anywhere.

## Tasks
1. **Rust, `core/crates/corn-vault`:**
   - Database **version 4**: `logins.password_changed_at INTEGER NOT NULL DEFAULT 0`. New databases get it from `SCHEMA`; upgrading from 3 adds the column (check `PRAGMA table_info`) and sets it to `created_at`. Bump `SCHEMA_VERSION`. Keep the existing upgrade path intact.
   - `Login` gains `password_changed_at`; `add_login` sets it; `update_login` gains a `password_changed: bool` (set the time only when true; the app knows whether the password text changed).
   - New `src/export.rs` with pure functions: links → HTML bookmarks (one "Corn Vault" folder, a folder per Collection, `ADD_DATE` in seconds, `TAGS`, HTML escaping), links → CSV (`title,url,collection,tags,saved_at`, tags joined `; `, UTC ISO 8601), passwords → CSV (`name,url,username,password,note`, name = host). RFC 4180 quoting, `\r\n`, UTF-8; formula guard (`'` prefix for `= + - @ \t \r`) on the **links CSV only**, never on passwords. No new dependencies unless truly needed (write the date conversion yourself).
   - `Store` methods that read items + Collections + tags and return the formatted links export.
   - Unit tests per the spec's "Tests" section, including the 3 → 4 upgrade on a real version-3 database.
2. **FFI, `core/crates/corn-ffi`:** expose the link exports, a passwords-CSV function taking plain rows (`origin, username, password` records), the new `Login` field and the new `update_login` parameter.
3. **Android:**
   - Update every `updateLogin` call (`passwords/Passwords.kt`, `data/CornData.kt`) to pass whether the password changed.
   - **Vault → Export links:** a control in the Vault screen header (match the existing look; e.g. an icon button or a ⋮ menu next to "Lock"), a small sheet to choose **HTML (recommended)** or **CSV**, then Android's "Save to…" (`ACTION_CREATE_DOCUMENT`, suggested name `corn-links-YYYY-MM-DD.html|csv`), write via `contentResolver.openOutputStream`, toast on success or failure.
   - **Passwords → Export passwords:** on the Passwords screen; always the PIN (reuse the existing PIN gate), then the warning sheet with the exact copy and checkbox from the spec (button disabled until ticked), then "Save to…" (`corn-passwords-UNENCRYPTED-YYYY-MM-DD.csv`, `text/csv`), decrypt only after the place is chosen, write, drop the plaintext, toast "Saved. Import it, then delete this file." Disable when there are no passwords. Keep `FLAG_SECURE`.
   - Use an Activity Result launcher appropriate to how the app is structured (read `MainActivity.kt`, `CornController.kt`, `ui/CornApp.kt` first). Never write an export to a temp file, cache or log; never log URLs, titles, usernames or passwords.
4. **Verify (all required):**
   - `cd core && cargo test`; `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug` with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and `ANDROID_HOME=~/Library/Android/sdk`.
   - Emulator (`scripts/dev-start.sh` starts it; app `app.cornbrowser`; Vault PIN on the emulator is 1234): add a few links (with a Collection and tags, a title with a comma and a quote) and a manual password if needed; export links in both formats and passwords into Downloads via the "Save to…" dialog; `adb pull` the files and check every item, the escaping, the columns; confirm the PIN and the checkbox can't be skipped (screenshots, Read them). Run `bash scripts/qa-run.sh` (should still pass).
   - Check the HTML file is well-formed by parsing it with Python's `html.parser` and listing folders and links. (Founder will try the Chrome import on the Mac.)
5. **Docs:** update `docs/02-technical/13-vault-export.md` only where the code differs, `docs/02-technical/12-security.md` (a short "Exporting" section linking to 13), `docs/02-technical/10-storage.md` (database version 4), `docs/01-product/03-features.md` (Vault export), `docs/01-product/01-prd.md` (export requirement ✅), tick the checklist rows under "Vault export" whose "done when" is true, append a `docu.md` entry, run `python3 scripts/doc-banner.py` and revert files whose only change is the banner date.

## Rules
- Never modify upstream code. Privacy is a requirement: nothing leaves the phone except into the place the user picks.
- No unnecessary changes outside this feature.
- Commit in your worktree with a detailed point-by-point message ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Don't push.

**Reply with:** files changed, test results, what you verified on the emulator (with the exported files' contents summarised), any difference from the spec and why, and anything left.
