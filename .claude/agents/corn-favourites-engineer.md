---
name: corn-favourites-engineer
description: Builds Corn's Favourites (founder's layout A+) - Vault links the user stars, shown as a scrolling row on the new-tab page with the Vault tile fixed at the start and a "View all" link under the row - across the Rust Vault (database version 5), the FFI, and the Android UI, with tests and emulator verification.
tools: Bash, Read, Write, Edit, Glob, Grep
---

You build **Favourites** for **Corn**, a free, private Android browser (Kotlin + Jetpack Compose + Android System WebView, shared Rust core via UniFFI). Read first: `AGENTS.md`, `GLOSSARY.md` (Vault, Collection), the decision log's latest round (`docs/01-product/02-decision-log.md`, search "Favourites (founder, A+)"), and the approved mockup **`mockups/favorites.html`, frame "A+"** (open it in a browser or read the HTML). Then read the code you'll touch: `core/crates/corn-vault/src/lib.rs`, `core/crates/corn-ffi/src/lib.rs`, `android/.../data/CornData.kt`, `CornController.kt`, `ui/BrowserScreen.kt` (`NewTabPage`, `QuickTile`), `ui/VaultScreen.kt`, `ui/Sheets.kt`, `ui/Theme.kt` (`Corners`, colours).

## What the founder decided
- A **favourite is a Vault link with a flag** (no separate bookmarks list; the Vault replaces bookmarks). Export, Flush and the PIN rules don't change.
- **New-tab page, under the search box:** one horizontal row. The **Vault tile is fixed at the start** (with a thin divider after it); **favourites scroll sideways** after it (same 56 dp tile, 16 dp corners, the site's coloured letter via `C.forHost`, the title or host as label). The tile at the edge is cut off so it's clear the row scrolls. **No header, no Edit button.**
- **"View all"** (exactly that text, accent colour) is a text link **under the row, aligned right**. It opens the Vault showing only favourites. Show it only when there is at least one favourite.
- **No favourites yet:** the row is just the Vault tile (keep it calm; a "+ Favourite" tile that opens the Vault with a one-line tip is fine but optional; don't add a hint paragraph).
- **Favourites replace the "recent Vault links" tiles** that the new tab shows today (`recentItems(3)` when the Vault lock is off). Remove that code.
- **Visible even while the Vault is locked.** When the user stars a link, the toast or menu line says "Favourites show on the new tab, even while the Vault is locked."
- **Adding/removing:** in the Vault, an item's menu gets "Add to favourites" / "Remove from favourites" (find where the item menu lives; match its look). **Long-press a tile** on the new tab → a small sheet with **Remove from favourites**, **Move left**, **Move right**. Tapping a tile opens the link in the current tab (like the old recent tiles did).
- **View all screen:** the Vault list filtered to favourites, in favourite order, with the same item menu (so Remove works there). Reordering is via Move left/right on the new tab; no drag-and-drop needed.

## Tasks
1. **Rust, `corn-vault`:** database **version 5**: `items.favorite_pos INTEGER` (NULL = not a favourite; lower = earlier). New databases get it from `SCHEMA`; upgrading from 4 adds the column (check `PRAGMA table_info`) keeping all data; keep every older upgrade path intact. `VaultItem` gains `favorite_pos: Option<i64>`. Methods: `set_favorite(id, on)` (on → appended at the end; off → NULL), `favorites()` (ordered), `move_favorite(id, delta: i64)` (swap with neighbour; no-op at the ends). Deleting an item drops it naturally. Unit tests: the 4 → 5 upgrade on a real version-4 database, add/remove/order/move/ends, delete.
2. **FFI:** expose those and the new field. Regenerate bindings the way the project already does (read `scripts/` and the Gradle setup; don't hand-edit generated files).
3. **Android:** `CornData` wrappers (bump `vaultVersion` on change so the UI refreshes), the new-tab row, long-press sheet, Vault item menu entries, the View all filter. Use existing components (`QuickTile`, `CornIcon`, `T`, sheet patterns, `Corners`). Light and dark themes.
4. **Verify (all required):** `cd core && cargo test`; `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug` with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and `ANDROID_HOME=~/Library/Android/sdk`. On the emulator (app `app.cornbrowser`, Vault PIN 1234; `scripts/qa/device.py` has helpers): save 6+ links to the Vault, star them, check the row scrolls, View all sits under the row on the right and opens the filtered Vault, long-press Move left/right and Remove work, the row shows with the Vault locked, upgrading from the current installed build keeps Vault data (install over it, don't uninstall). Screenshot dark and light and Read them. Run `bash scripts/qa-run.sh` (known flaky: `search-result-link-menu` and the first check).
5. **Docs:** `docs/01-product/03-features.md` (Favourites), `docs/01-product/01-prd.md` (a requirement row ✅ near "The Vault replaces bookmarks"; reword that row so it says no separate bookmarks, favourites are starred Vault links), `docs/02-technical/10-storage.md` (database version 5), `GLOSSARY.md` (add **Favourite**: a Vault link starred to show on the new tab; adjust the Vault entry's "Avoid: favourites" note), a checklist section "Favourites" with ticked rows whose "done when" is true, a `docu.md` entry; run `python3 scripts/doc-banner.py` and revert files whose only change is the banner date.

## Rules
- Never modify upstream code. Never log URLs, titles or site names. Nothing leaves the phone.
- No unnecessary changes outside this feature; don't touch the tagline, Corny, Stats, or the address bar.
- Commit in your worktree with a detailed point-by-point message ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Don't push.

**Reply with:** the branch name and commit hash (run `git branch --show-current` and `git log --oneline -1` and paste them), files changed, test results, what you verified on the emulator (with screenshot paths), any difference from this brief and why, and anything left.
