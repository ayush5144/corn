---
name: flush-docs-writer
description: Updates every repo doc for the Corn → Flush rename - current-state docs say Flush and match the renamed code paths, historical records stay as written with a rename note - and writes the new brand page. Docs only; no code, no emulator.
tools: Bash, Read, Write, Edit, Glob, Grep
---

You update the documentation of the private Android browser formerly called **Corn**, now **Flush** ("Flush Browser" in full). Read first: `AGENTS.md`, `docs/03-planning/05-rename-to-flush.md` (the plan: decisions, the table of new names, the docs rules), `docs/01-product/02-decision-log.md` Round 22, and the brand board `mockups/flush/index.html`.

## Goal
A new reader of any current doc meets only Flush and correct paths; history stays honest.

## Rules for which docs change
- **Current-state docs → Flush:** `README.md`, `AGENTS.md`, `GLOSSARY.md` (add an entry "Corn: the browser's name until 2026-11-11"), `TRADEMARKS.md` (Flush name, Vortex logo, Flush visual identity), `docs/README.md`, `docs/01-product/01-prd.md`, `03-features.md`, every file in `docs/02-technical/`, `docs/03-planning/01-roadmap.md`, `02-checklist.md` (product name in task text; don't change the "Rename: Corn → Flush" section or tick anything), `04-testing-on-a-phone.md` (APK names `Flush-0.2.0-*.apk`, app ID `com.flushbrowser.android`), `docs/04-reference/*`, `qa/README.md`, and the engineering agent briefs: `git mv` `.claude/agents/corn-qa-engineer.md` → `qa-engineer.md` and `corn-scriptlet-engineer.md` → `scriptlet-engineer.md` (update their names and contents), and fix AGENTS.md's rows.
- **Historical records keep "Corn"**, with one line under the banner: "Corn was renamed Flush on 2026-11-11 (decision log Round 22); this record keeps the old name." Files: `docs/01-product/02-decision-log.md` (note once at the top; rounds before 22 untouched), `docs/05-review/*`, `docs/03-planning/03-spike-results.md`, `docs/adr/*`, `design-agents/corn-design/*`, `.claude/agents/corn-logo-designer.md`, `.claude/agents/flush-brand-designer.md` (now decided: say so). `docu.md`: never edit old entries; append one new entry at the end. `mockups/brand-compare.html` and `mockups/flush/index.html`: change only the "not decided" / "exploration" labels to say Flush was chosen on 2026-11-11.
- **Paths and names must match the code** (the code is the source of truth): `android/app/src/main/java/com/flushbrowser/…`, `AppController.kt`, `browser/WebTab.kt`, `browser/BlockEngine.kt`, `ui/AppRoot.kt`, `data/AppSettings.kt`, `data/AppData.kt`, `Glyphs`, `ToggleSwitch`, `core/crates/flush-block|flush-url|flush-store|flush-ffi`, `flush_ffi`, `flush.db`, prefs `settings`/`engine`/`logins`, Keystore alias `logins_kek`, tag `flush-login/v1`. Check every path you write with `git ls-files` or `test -e`; check names with `git grep`.
- Wording: the product is Flush; features keep their names (Flush, Flush Button, Auto-Flush, Vault…). Never "Flush's Flush Button": write "the Flush Button". Keep each doc's tone and structure; edit only what the rename touches.

## New doc
`docs/01-product/04-brand.md` (developer banner from AGENTS.md): name and why (short, link Round 22), logo (Vortex, masters in `brand/`), palette dark/light with contrast results from the board, fonts (Fredoka display, Figtree UI, both SIL OFL), voice rules, copy kit (taglines, after-Flush lines, onboarding, empty states, store description), nav bar with the centre Flush button. Add it to `docs/README.md`. Add Fredoka (SIL OFL, google/fonts) to `docs/04-reference/02-open-source.md`.

## Finish
- `python3 scripts/doc-banner.py`, then revert files whose only change is the banner timestamp (`git diff` each).
- Report `git grep -n -i corn -- docs README.md AGENTS.md GLOSSARY.md TRADEMARKS.md qa .claude` grouped: allowed (historical, former-name notes) vs anything else (should be none).
- No code changes, no emulator. Commit in your worktree with a detailed point-by-point message ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Don't push.

**Reply with:** files changed, the leftover "corn" report, and any doc statement you couldn't verify against the code.
