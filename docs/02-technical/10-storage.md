# 10 — Storage: What Corn Keeps, and Where

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 14:28 IST

Everything stays in Corn's **private app storage** on the phone. Other apps can't read it (Android sandbox), the phone encrypts it while locked (file-based encryption), and Corn opts out of every backup.

## What goes where

| Data | Stored in | Survives Flush? | Why there |
|---|---|---|---|
| **Vault** (Collections, Vault Items, tags) | SQLite database `corn.db`, written by the Rust core (`corn-vault`). Each link saved once; its Collection is optional (All = every link) | ✅ Yes | One format for Android, iPhone and desktop |
| **Keep List** (sites that stay logged in) | `corn.db`, key-value area | ✅ Yes | Reveals sites, so it lives with the Vault, not in plain settings |
| **Per-site shields** (ads / trackers / pop-ups on or off) | `corn.db`, key-value area | ✅ Yes | Same reason |
| **Totals** (trackers & ads blocked, pop-ups stopped) | Settings file: two numbers | ✅ Yes | Counts only: no URLs, sites or times |
| **Kept Session** (open tabs, Tab Groups, their URLs) | `corn.db`, key-value area | ❌ Wiped by Flush | Restores your tabs after a restart when Kept Session is on |
| **History** (page address, title, site, visit times) | `corn.db`: `history_entries` (one per page) + `history_visits` (one per visit) | ❌ Wiped by Flush | Shared format for every platform; "Keep history" off stops recording |
| **Settings** (Flush mode, toggles, default engine, address bar position) | Android app settings file (SharedPreferences) | ✅ Yes | Simple on/off values, no browsing data |
| **Vault PIN** | Settings file, as a salted PBKDF2 hash (never the PIN itself) | ✅ Yes | Lets Corn check the PIN without storing it |
| **Blocking engines** | Cache files `ads-v1.dat` (EasyList) and `trackers-v1.dat` (EasyPrivacy); last refresh time in `corn_engine` settings | ✅ Yes | Rebuilt from downloaded lists every 4 days |
| **Cookies, site data, cache** | WebView's own storage (`app_webview/`, cache folder) | ❌ Wiped (except Keep List cookies and IndexedDB; see [11](11-keep-list.md)) | Owned by the web engine |
| **Tab previews** | Memory only | ❌ Gone on Flush or app close | Never written to disk |

## Why plain SQLite, not SQLCipher

Founder decision (decision log, Round 5). SQLCipher would only add protection when someone gets the raw file: a rooted phone, malware with root access, or forensic extraction tools. In normal use, Android's sandbox and phone encryption already protect the file, the same level as Chrome's bookmarks and history on Android.

**Deleting leaves no trace:** `corn.db` runs with `PRAGMA secure_delete = ON` (deleted rows are overwritten with zeros) and every history deletion empties the write-ahead log (`wal_checkpoint(TRUNCATE)`), so deleted pages can't be read back from `corn.db` or `corn.db-wal`. Unit-tested.

**Size:** history costs about 160 bytes per visit (1,000 visits ≈ 160 KB, measured with Corn's tables).

**Schema upgrades:** `corn.db` carries a version (`PRAGMA user_version`, now 2). Version 2 made a link's Collection optional and moved "Unsorted" links to no Collection (shown in All). Upgrades run once on open and keep all data (unit-tested).

**Switching back later:** in `core/Cargo.toml` change rusqlite's feature to `bundled-sqlcipher-vendored-openssl`, add `PRAGMA key` in `Store::open`, and keep the key in the Android Keystore. Nothing outside `corn-vault` changes.

## What the Vault lock does

The Vault PIN or fingerprint is a gate in the app: nobody sees Vault contents in Corn without it. It does not scramble the file on disk.

## What Corn never stores or logs

- No list of blocked requests, sites or URLs, anywhere (database, files or Android's system log).
- History is kept only while "Keep history" is on, only on the phone, and only until the next Flush.
- Corn's own log lines contain no URLs; WebView usage metrics to Google are opted out.
