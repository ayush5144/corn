# 12 — Security and Encryption

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-05 · **Last updated:** 2026-11-13, 12:17 IST

## The code is the source of truth

| What | Where |
|---|---|
| Password encryption (AES-256-GCM) | `android/app/src/main/java/app/cornbrowser/passwords/LoginCipher.kt` |
| The password key and the Android Keystore | `passwords/LoginKeys.kt` |
| When passwords are decrypted, saved and filled | `passwords/Passwords.kt`, `passwords/LoginScript.kt` |
| Password table (`logins`) | `core/crates/corn-vault/src/lib.rs` |
| PIN hashing | `data/CornSettings.kt` (`setPin`, `checkPin`) |
| Backups off | `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml` |

If this doc and the code disagree, the code is right and this doc should be fixed.

## Saved passwords: how they're encrypted

Two keys, like DuckDuckGo's Android browser; the table design is like Firefox's `logins`.

```
 username / password ──AES-256-GCM──► encrypted bytes stored in corn.db (table "logins")
                         ▲
              DATA KEY (random 256-bit, made once, the first time a password is saved)
                         │ stored only in locked ("wrapped") form, in Corn's private
                         │ settings file corn_logins (not in corn.db)
                         ▲ unlocked by
              KEY-ENCRYPTION KEY "corn_logins_kek"
                inside the phone's Android Keystore: StrongBox security chip when the
                phone has one, otherwise the phone's secure area (TEE).
                It can't be read or copied out of the phone, not even by Corn.
```

1. **Data key.** A random 256-bit key, created the first time a password is saved. Every username and password is encrypted with it.
2. **Key-encryption key.** An AES-256 key that lives inside the Android Keystore (StrongBox when available). Corn asks the Keystore to unlock ("unwrap") the data key; the key-encryption key itself never leaves the security hardware.
3. **Each value** (one username, one password) is encrypted separately with **AES-256-GCM** and a fresh random 12-byte IV. Stored layout: `[version 1][12-byte IV][ciphertext + 16-byte tag]`. GCM's tag means any tampering is detected (decryption fails instead of returning wrong data).
4. **Bound to its site and field.** The site and the field name (`corn-login/v1|<site>|<field>`) are mixed in as "associated data", so an encrypted password can't be moved to another site, or swapped with the username, without failing to decrypt.
5. **What's readable in corn.db:** only the site (e.g. `https://example.com`), times and use counts, so Corn can find the right logins without decrypting anything. Usernames and passwords are never stored readable. Verified: after saving `manual@test.example` / a test password, neither string appears anywhere in `corn.db` or `corn.db-wal` (raw file search).
6. **Deleting leaves no trace:** corn.db runs with `secure_delete` (deleted rows are overwritten with zeros) and the write-ahead log is emptied after every password change or delete (unit-tested).

## The PIN is a gate, not the encryption key

| | What it does |
|---|---|
| **Encryption** (above) | Protects passwords **on disk**: someone who copies Corn's files (e.g. from a rooted phone or a backup tool) gets only encrypted bytes, and the key to them is locked in this phone's security hardware |
| **PIN** | Protects passwords **inside the app**: viewing, copying and editing always need it; filling needs it when the Vault is locked (Settings → "Ask for PIN before filling" can turn that off) |

The PIN itself is never stored: Corn keeps a salted hash (PBKDF2-HMAC-SHA256, 120,000 rounds, 16-byte random salt) and compares hashes.

## When the key is lost

If the Keystore key disappears (Corn's data moved to a different phone, the phone's security reset, a factory reset of the security chip), the data key can't be unlocked and **nobody can decrypt the saved passwords, Corn included**. The Passwords screen then says "Corn can't open your saved passwords" and offers **Reset passwords**, which deletes them so saving can start again. This is the price of keeping the key in hardware; it's the same with DuckDuckGo's and Chrome's on-device password stores.

## Filling rules (so pages can't steal passwords)

- Corn fills **only when you tap** "Fill as …": never silently (silent autofill lets hidden form fields on a malicious page receive passwords).
- **https only** (local test addresses allowed in debug builds) and **only the exact site** the password was saved for.
- The password is sent only to the page frame that showed the login form, and only if that frame's site matches.
- **Two locks against the wrong site:** when a tab starts loading a new page, Corn drops its connection to the old login form; and at the moment you tap Fill, Corn checks again that the tab is showing the login's exact site right now, and refuses otherwise (`Passwords.fill()`).
- Copies are marked sensitive (Android 13+). The clipboard is cleared after 45 s, **or as soon as Corn leaves the screen**, whichever is first; if Corn was closed before it could clear, it clears on its next start (verified). It only clears when the clipboard still holds Corn's copy (when Android doesn't let Corn check, it clears to be safe). The Passwords screens block screenshots (`FLAG_SECURE`, verified: screenshots come out black).

## Other data on the phone

| Data | Protection |
|---|---|
| Vault links, History, Keep List (corn.db) | Plain SQLite in Corn's private storage: Android's app sandbox + the phone's own file encryption. Not encrypted by Corn (founder decision: no SQLCipher, see [10-storage](10-storage.md)) |
| Cookies, site data, cache | WebView's own storage; deleted by Flush (see [11-keep-list](11-keep-list.md)) |
| Backups | Off for all of Corn's data (`allowBackup=false`, empty extraction rules) |
| Logs | Never contain URLs, titles, searches, site names, usernames or passwords (counts and error types only) |
| Network | Nothing about browsing, the Vault or passwords is ever sent anywhere |

## Known gaps

Full list with severities: [password store code review](../05-review/03-password-review-2026-11-05.md).


- **No limit on wrong PIN attempts.** The PIN screen allows unlimited tries, and a 4-digit PIN has 10,000 combinations. Someone with your unlocked phone could eventually guess it. Founder decision: keep for now, address later (checklist).
- **Info, by design:** any https page can make Corn ask "Save password?" for **its own site** (never another site's); Corn decrypts a saved password in memory, without the PIN, to decide between Save / Update / already saved (never shown or logged).
- **4-digit PIN hash:** the salted hash in Corn's settings file could be brute-forced quickly by someone who extracts that file (needs root access). The passwords themselves stay safe because their key is in the Keystore, not derived from the PIN.
- **Two-step logins** (username and password on separate pages): only the password is saved.
- No import yet (export: see below).

## Exporting

Links and passwords can be exported ([13-vault-export](13-vault-export.md)). The passwords file is **plain text** (Chrome's CSV format, what password managers import), so: the PIN is asked every time, even with the Vault open; a warning says the file isn't encrypted and Export stays disabled until "I understand the file isn't encrypted" is ticked; the user picks the place in Android's "Save to…" dialog; passwords are decrypted only after that, written straight there, and never cached, logged or sent; the screen keeps blocking screenshots. Corn can't protect the file once it's saved: the toast says "Saved. Import it, then delete this file."
