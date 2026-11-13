# 13 — Exporting the Vault (links and passwords)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-13 · **Last updated:** 2026-11-13, 12:17 IST

Your Vault is yours: Corn lets you take every saved link and password out, in formats other browsers and password managers import. Decided in the decision log (Round 25). Import and a full Corn backup come later.

## The code is the source of truth

| What | Where |
|---|---|
| Export formats (HTML bookmarks, links CSV, passwords CSV), CSV escaping, dates | `core/crates/corn-vault/src/export.rs` |
| Database version 4 (`password_changed_at`) and its upgrade | `core/crates/corn-vault/src/lib.rs` (`SCHEMA`, `migrate`) |
| What the Kotlin app can call | `core/crates/corn-ffi/src/lib.rs` |
| Export buttons, the warning sheet, the "Save to…" dialog | `android/app/src/main/java/app/cornbrowser/ui/VaultScreen.kt`, `ui/PasswordsUi.kt`, `ui/Sheets.kt`, `CornController.kt` (`chooseExportPlace`, `exportLinks`), `MainActivity.kt` (the launcher) |
| File names and types | `android/app/src/main/java/app/cornbrowser/data/VaultExport.kt` |
| Decrypting passwords for the export | `android/app/src/main/java/app/cornbrowser/passwords/Passwords.kt` |

## What you can export

| What | Formats | Where in the app |
|---|---|---|
| **Saved links** (titles, addresses, Collections, tags, dates) | **HTML bookmarks file** (recommended) or **CSV** | Vault → ⋮ → Export links |
| **Saved passwords** (site, username, password) | **CSV** (Chrome's format) | Vault → Passwords → Export passwords, at the bottom of the list (PIN + warning) |

### Why HTML for links, and why CSV too

- **Browsers import bookmarks only from an HTML bookmarks file** (the "Netscape bookmark file" format). Chrome, Firefox, Safari, Brave and Edge all read it; none of them imports bookmarks from CSV. Collections become folders, tags and dates come along.
- **CSV** is for spreadsheets and apps that take CSV (Notion, Raindrop, scripts). Offered as the second choice.

### Why CSV for passwords

Every password manager imports a CSV, and Chrome's columns are the most widely accepted (Google Password Manager, Bitwarden, 1Password, Proton Pass, Firefox, Safari): `name,url,username,password,note`.

## The files

### Links: HTML bookmarks

```html
<!DOCTYPE NETSCAPE-Bookmark-file-1>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<TITLE>Bookmarks</TITLE>
<H1>Bookmarks</H1>
<DL><p>
    <DT><H3 ADD_DATE="…">Corn Vault</H3>
    <DL><p>
        <DT><H3 ADD_DATE="…">Recipes</H3>          ← one folder per Collection
        <DL><p>
            <DT><A HREF="https://…" ADD_DATE="…" TAGS="pasta,quick">Quiet pasta</A>
        </DL><p>
        <DT><A HREF="https://…" ADD_DATE="…">A link in no Collection</A>
    </DL><p>
</DL><p>
```

- Everything sits in one **Corn Vault** folder, so an import doesn't mix with existing bookmarks.
- `ADD_DATE` is in seconds (Corn stores milliseconds); the Corn Vault folder's is the export time. Titles, addresses and tags are HTML-escaped (`& < > "`); a line break in a title becomes a space; a link with no title shows its address.
- Every Collection becomes a folder, even an empty one; links keep the Vault's order (newest first). The file starts with the same "DO NOT EDIT" comment browsers write.
- File name: `corn-links-YYYY-MM-DD.html`.

### Links: CSV

`title,url,collection,tags,saved_at`. Tags joined with `; ` (a comma would split the column); `saved_at` as UTC ISO 8601 (`2026-11-13T05:40:00Z`); empty collection = not in a Collection. File name: `corn-links-YYYY-MM-DD.csv`.

### Passwords: CSV (Chrome's format)

`name,url,username,password,note`: `name` is the site's host (`example.com`, no port), `url` its origin (`https://example.com`), `note` empty for now. Sorted by site, then username. A login that can't be decrypted is left out, and the toast says how many. File name: `corn-passwords-UNENCRYPTED-YYYY-MM-DD.csv`.

### CSV rules (both CSVs)

- UTF-8; Windows-style line ends (`\r\n`), which every spreadsheet and importer reads.
- A value with a comma, a quote, a line break, or space at either end is wrapped in quotes; quotes inside are doubled (RFC 4180).
- **Links CSV only:** a value starting with `=`, `+`, `-`, `@`, a tab or a carriage return gets a leading `'` so a spreadsheet can't run it as a formula ("CSV injection").
- **Passwords CSV: no formula guard.** It would change any password that starts with `=` or `-` and break the import into a password manager, which is what the file is for. Chrome's own export does the same. The warning says the file is for moving passwords into another password manager.

## Exporting passwords safely

The password CSV is **plain text**: anyone who gets the file can read every password. So:

1. **PIN first**, every time (even if the Vault is unlocked).
2. **A warning sheet:**
   > **Your passwords will be saved as plain text**
   > This file isn't encrypted. Anyone who gets it (another app, a cloud backup, someone with your phone) can read every password in it. Use it to move your passwords into another password manager, then delete the file.
   > ☐ I understand the file isn't encrypted
   > **Export passwords** (disabled until ticked) · Cancel
3. **You choose where it goes:** Android's "Save to…" dialog (Storage Access Framework, `ACTION_CREATE_DOCUMENT`). Corn never writes the file anywhere on its own.
4. **Decrypted only after you've chosen the place**, written straight to it, and dropped from memory; never logged, never cached, never sent anywhere.
5. **After saving:** "Saved. Import it, then delete this file."
6. The Passwords screens keep blocking screenshots (`FLAG_SECURE`).
7. Leaving Corn for the "Save to…" dialog locks the Vault as any other leaving does; the export still finishes, and you come back to the Vault's PIN screen.

Links aren't secret in the same way, so link export has no PIN beyond the Vault's own lock.

## How it works

```
 You tap Export
      │
      ▼
 (passwords only) PIN ──► warning sheet ──► ticked + Export
      │
      ▼
 Android "Save to…" dialog (you pick a folder or app, Corn suggests the file name)
      │  returns a content address (Uri)
      ▼
 Corn builds the file:
   links      Rust core reads items + Collections + tags → export.rs formats HTML or CSV
   passwords  the app decrypts each login (Keystore key) → plain rows → export.rs formats CSV
      │
      ▼
 written to that Uri, then the toast
```

Formatting lives in the Rust core so the iPhone and desktop apps reuse it unchanged. Decryption stays in the Android app because the key lives in Android's Keystore.

## Database version 4

`logins` gains `password_changed_at` (milliseconds). Upgrading from version 3 adds the column and sets it to each login's `created_at`, so nothing is lost; from then on it's set when a password is saved or changed (not when only the username changes). It's what "password last changed" means in exports and, later, in the Passwords screen.

## Not yet

- **Import** (the same formats back in) and a **full Corn backup** (one file with everything, optionally encrypted, that restores into Corn).
- Notes on links or logins (the CSV's `note` column stays empty until then).

## Tests

- Rust unit tests: each format (titles with commas, quotes, line breaks, emoji, `&`), the formula guard on links CSV and its absence on passwords CSV, dates, an empty Vault, the version 3 → 4 upgrade.
- Emulator: export links (both formats) and passwords, read the files back and check every item is there; the PIN and the checkbox can't be skipped.
- The HTML file imports into Chrome on the Mac.
