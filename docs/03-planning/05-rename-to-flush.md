# 05 — Rename: Corn → Flush

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-11 · **Last updated:** 2026-11-11, 22:40 IST

The founder renamed the browser from **Corn** to **Flush** before its first release (decision log, Round 22). This plan lists every change, in order, so nothing is left out. Brand source: `mockups/flush/index.html` (the brand board).

## What's decided

| Item | Decision |
|---|---|
| Public name | **Flush**. Full name "Flush Browser" where context is needed (store title, Product Hunt, press). Home-screen label: "Flush". Wordmark: lowercase **flush** in Fredoka 600 |
| Logo | The brand board's **A · The Vortex** (original: four arms, two bubbles). Master: `~/Documents/corn-logo-originals/flush/flush-a-vortex.png` (1024 px) |
| Colours | The brand board's palette, dark (default) and light, every text pair ≥ 4.5:1 (computed by `mockups/flush/contrast.py`) |
| Fonts | **Fredoka** (display: wordmark, headings) replaces Bricolage Grotesque; **Figtree** stays for UI text |
| Voice | Same as Corn: cool, quick, cheeky, tasteful. Jokes about your tracks going down the drain, never about what's in the bowl. Corn-specific jokes (cob, kernels, 🌽) are replaced by the board's lines |
| Copy | Board's taglines (primary: "Browse whatever. Flush whenever."), after-Flush one-liners, onboarding, empty states, store description |
| Nav bar | Founder's layout (2026-11-11): Back · Vault · **Flush (centre, raised round button)** · Tabs · Menu. The button's face is the "Riding the swirl" drop from `mockups/flush/wink.html` (`wink-swirl.png`), not the swirl glyph |
| Feature names | Unchanged: Flush, Flush Button, Auto-Flush, Kept Session, Keep List, Vault, Collection… The app is named after its main action |

## Names in code and data

Rule: user-facing text says Flush; internal names that are app-wide become **brand-neutral** when "Flush" would read as the Flush action (`FlushController` would sound like it controls flushing). Names that pages can see are neutral too, so sites can't spot the browser by name.

| Today | After |
|---|---|
| applicationId / namespace `app.cornbrowser` | applicationId **`com.flushbrowser.android`**, Kotlin package **`com.flushbrowser`** (permanent once uploaded to Google Play) |
| Gradle `rootProject.name = "Corn"` | `"Flush"` |
| `CornController`, `CornApp`, `CornTab`, `CornEngine` | `AppController`, `AppRoot`, `WebTab`, `BlockEngine` |
| `CornSettings`, `CornData` | `AppSettings`, `AppData` |
| `CornIcons`, `CornIcon()`, `CornSwitch` | `Glyphs`, `Glyph()`, `ToggleSwitch` |
| Theme tokens `C.Corn`, `C.CornInk`, `C.Leaf` | `C.Fill`, `C.Ink`, `C.Success` (the board's token names) |
| Styles `Theme.Corn`, `Theme.Corn.LightPages` | `Theme.Flush`, `Theme.Flush.LightPages` |
| Rust crates `corn-block`, `corn-url`, `corn-vault`, `corn-ffi`; bin `corn-compile` | `flush-block`, `flush-url`, `flush-store`, `flush-ffi`; `flush-compile` |
| UniFFI namespace `corn_ffi`; Rust types `CornBlocker`, `CornStore`, `CornError` | `flush_ffi`; `CoreBlocker`, `CoreStore`, `CoreError` |
| Files and keys: `corn.db`, prefs `corn_settings` / `corn_engine` / `corn_logins`, Keystore alias `corn_logins_kek`, encryption tag `corn-login/v1`, intent extras `corn.*`, thread names `corn-*` | `flush.db`, `settings` / `engine` / `logins`, `logins_kek`, `flush-login/v1`, `flush.*`, neutral thread names |
| Seen by pages: `cornBridge`, `cornLogins`, `window.__cornHold` | Neutral names (no brand) |
| `res/drawable-nodpi/corn_logo.png`, launcher icons | `flush_logo.png`, Vortex launcher icons (adaptive: art on #071226, inside the 66 dp safe zone; legacy sizes; splash background) |
| APKs `Corn-0.2.0-debug.apk`, `Corn-0.2.0-test.apk` | `Flush-0.2.0-debug.apk`, `Flush-0.2.0-test.apk` |
| Agent briefs `corn-qa-engineer`, `corn-scriptlet-engineer` | `qa-engineer`, `scriptlet-engineer` |

**No data migration:** the new applicationId installs as a new app with empty storage (Android ties data to the app ID). Nobody outside the founder has Corn installed. The old Corn app stays on devices until it's uninstalled.

**Not renamed:** the emulator's name `corn_pixel` (local only); historical files kept as a record (below).

## Docs

- **Current-state docs** say Flush: README, AGENTS, GLOSSARY (adds "Corn: former name"), TRADEMARKS, PRD, features, all of `docs/02-technical/`, roadmap, checklist, testing on a phone, `qa/README.md`, agent briefs that are still used.
- **Historical records stay as written**, with a one-line note at the top that Corn was renamed Flush on 2026-11-11: decision-log rounds before 22, `docs/05-review/*`, spike results, `docu.md` entries, `design-agents/corn-design/`, `mockups/logos/`, `brand/` Corn logos (kept as the record; no longer used).
- **New:** a brand page `docs/01-product/04-brand.md` (name, logo, palette, fonts, voice, copy kit; source: the board).

## Order of work

| Step | What | Who | Done when |
|---|---|---|---|
| 0 | This plan + decision log Round 22, committed before any code | Claude | Committed |
| 1 | **Names in code and data** (table above), no visual change | Claude | Rust + Kotlin tests pass; QA suite 17/17 under the new app ID; `git grep -i corn` in code shows only the allowed list |
| 2a | **Brand in the app**: palette, Fredoka, Vortex logo and launcher icons, centre Flush button, onboarding, new-tab lockup, toasts, empty states, every user-facing "Corn" string, Settings → About | Agent (worktree) | Screenshots in dark and light of: launcher, onboarding, new-tab page, nav bar, a Flush toast, Settings, Vault, a sheet; contrast as on the board |
| 2b | **Docs** (rules above) + new brand page | Agent (worktree), in parallel with 2a | `git grep -i corn docs` shows only historical records and "former name" notes |
| 3 | Merge, full verification: tests, QA suite, real sites (YouTube, ayushpatil.com), screenshots, leftover-name report, release size | Claude | All pass; report to the founder |
| 4 | APKs in Downloads under the new names, push | Claude | APKs install; pushed |
| 5 | **Founder's OK needed:** rename the GitHub repo `ayush5144/corn` → `flush` (GitHub redirects the old URL); rename the local folder `~/dev/corn` → `~/dev/flush` (done last: it changes every path this session uses); uninstall the old Corn app from the emulator; register flushbrowser.com | Founder | Done or declined |

## Risks

- **Centre Flush button and misfires.** A big raised button where thumbs rest makes accidental Flushes more likely, and "Ask before Flush" is off by default. Watch it in daily use; if it misfires, turn the confirmation on by default or add an Undo window (later, separate decision).
- **The logo at small sizes.** The board noted the original Vortex's bubbles and highlights blur at 48 px. Check the launcher icon on a real phone; a vector redraw is already on the backlog.
- **Trademarks** for "Flush" not searched yet (USPTO / EUIPO) and social handles not checked: before public launch.
