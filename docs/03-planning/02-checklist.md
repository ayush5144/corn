# 02 — Checklist

The single list of what's done, what's next and who owns it. Product decisions live in the [decision log](../01-product/02-decision-log.md); this file tracks **work**.

**Owners**
- **Founder**: decisions, design taste, testing on real phones, anything needing accounts or money.
- **Claude**: code, docs, research, prototypes.
- **Both**: Claude drafts, Founder reviews and approves.

**Rule:** tick a box only when its **Done when** is true.

---

## Phase 0 — Product definition 🔄 in progress

**Goal:** Everyone can say exactly what Corn v1 is and isn't.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Pick the architecture (OS web engine + Rust core) | Both | [ADR 0001](../adr/0001-os-web-engine-not-brave-fork.md) written |
| [x] | Organise docs, README, glossary | Claude | `docs/README.md` indexes every doc |
| [x] | Round 0 + Round 1 product Q&A | Both | Answers in the decision log |
| [x] | Round 2: Tab Groups model, Keep List / passwords, thumbnails, Flush placement, prototype tool | Both | Answers in the decision log |
| [x] | Round 3: Session protection, Flush presets, Tab Groups on Flush, toolbar layout | Both | Answers in the decision log |
| [ ] | Round 4: react to the prototype (look and feel, group naming, password store timing) | Founder | Answers in the decision log |
| [ ] | PRD v2 sign-off: no 🟡 left that blocks v1 | Founder | Founder says "PRD approved" |
| [ ] | Research donation platforms that accept an adult-adjacent product | Claude | Shortlist with policy links in `04-reference/` |

## Phase 1 — Setup

**Goal:** The Mac can build and run an Android app with a Rust library.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [x] | Git repo pushed to GitHub (`ayush5144/corn`, private) | Claude | `main` on GitHub |
| [ ] | Install Android Studio, SDK, NDK, an ARM "Google Play" emulator image | Founder | Emulator boots on the Mac |
| [ ] | Install Rust (`rustup`), Android targets, `cargo-ndk` | Founder | `cargo ndk --version` works |
| [ ] | Real Android phone with USB/wireless debugging on | Founder | Phone shows in `adb devices` |
| [ ] | Free disk space to ≥ 40 GB before installs | Founder | `df -h /` shows ≥ 40 GB free |
| [x] | `AGENTS.md` / `CLAUDE.md` with project rules for coding agents | Claude | File committed |

## Phase 2A — Design (runs alongside 2B)

**Goal:** See and feel Corn before writing app code.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Brand voice: cool, funny, modern — sample copy (onboarding, Flush, empty states) | Both | Copy sheet approved |
| [ ] | Low-fi wireframes: browser, tab switcher + Tab Groups, Flush, Vault + Collections, settings, onboarding | Claude | Wireframes reviewed |
| [ ] | Visual style: black + yellow, Flush Button look, icons, type | Both | Style sheet approved |
| [ ] | Clickable prototype (plain HTML, Next.js static only if needed), mobile viewport, in `mockups/prototype.html` (never as a claude.ai artifact). **v0.1 built**, waiting on the founder's tap-through | Claude | Founder has tapped through it on a real phone |
| [ ] | Prototype test with 3–5 target users | Founder | Notes recorded; changes fed back into the PRD |

## Phase 2B — Technical spike (one weekend, throwaway code)

**Goal:** Prove WebView + adblock-rust is good enough before building on it.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Minimal Android app: one WebView, URL bar | Claude | Runs on emulator |
| [ ] | adblock-rust via cargo-ndk + UniFFI, EasyList + EasyPrivacy | Claude | Requests blocked in logs |
| [ ] | Pop-up denial (`onCreateWindow`) + redirect guard | Claude | Test pages can't open pop-ups |
| [ ] | Pick 15–20 real test sites | Founder | List saved (privately, not in the repo) |
| [ ] | Measure block rate vs Brave, video, memory with 10 tabs | Both | Results table in `03-planning/` |
| [ ] | Go / no-go on WebView | Founder | Recorded as an ADR |

## Phase 3 — Android v1 build

**Goal:** A Corn APK the Founder uses daily.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Project skeleton: `android/` (Kotlin + Compose), `core/` (Rust workspace) | Claude | Builds from a clean checkout |
| [ ] | Browsing basics: address bar, back/forward/reload/share, fullscreen, PiP | Claude | Works on the test sites |
| [ ] | Blocking + per-site toggle | Claude | Spike metrics hold in the real app |
| [ ] | Search: built-in engines, DuckDuckGo default, search-bar dropdown switcher | Claude | Switch engine in one tap |
| [ ] | Tabs + **Tab Groups**: Tab Group Strip (icons, `^` grid, `+`, `×`), swipe, previews, instant switching | Claude | 20+ tabs across groups without lag on a mid-range phone |
| [ ] | Tab freezing (memory policy) | Claude | No crashes with 30 tabs |
| [ ] | **Flush Button** + Kept Session (default) + Auto-Flush (leave delay, inactivity; video counts as activity) | Claude | Flush tests in [05-flush-model](../02-technical/05-flush-model.md) pass |
| [ ] | **Keep List**: chosen sites stay logged in across Flush | Claude | Keep List site still logged in after Flush |
| [ ] | **Corn password store** in the Vault, survives Flush (v1 or v1.1, timing open) | Claude | Saved password fills a login after Flush |
| [ ] | Corn-encrypted Session data + backups disabled | Claude | No readable URLs in app files on a debug dump |
| [ ] | **Vault**: save link + title, optional tags, **Collections**, PIN/biometric lock | Claude | Save, find, lock, delete all work |
| [ ] | Privacy: black recents screen, optional App Lock, no notifications while closed | Claude | Verified on a real phone |
| [ ] | Onboarding: 18+ confirmation, privacy explainer, fun tone | Claude | Founder approves the flow |
| [ ] | Settings screen for every option above | Claude | Every setting in the PRD is reachable |
| [ ] | In-app update check + signed release APK | Both | Founder installs a signed build |

## Phase 4 — Beta

**Goal:** Real users, real feedback, no privacy surprises.

| ✓ | Task | Owner | Done when |
|---|---|---|---|
| [ ] | Privacy policy + terms (plain language) | Both | Published |
| [ ] | Distribution: website / GitHub Releases / Obtainium | Founder | Download link works |
| [ ] | Donation link live | Founder | First donation possible |
| [ ] | Small adult-only beta group | Founder | 10+ testers, feedback logged |
| [ ] | Network audit: Corn's own traffic contains no browsing data | Claude | Audit notes committed |

---

## Backlog ⏭️ (agreed, not v1)

| Item | Notes |
|---|---|
| **iPhone app** | Committed next platform after Android v1 (Swift + WKWebView) |
| **File downloads** | Private storage, not the phone gallery |
| **Cloud Vault** (~$3–5/month) | Opt-in, end-to-end encrypted; needs a payment provider that accepts it |
| Save downloaded files into the Vault | After downloads ship |
| Vault thumbnails (screenshot / placeholder / none) | Founder decision, Round 2 |
| Custom search engines | |
| Disguised app icon | |
| Vault export / import | Needs a format decision first |
| Laptop app (Electron) | After iPhone |

## Out ❌

| Item | Why |
|---|---|
| Browser extensions | Founder decision, Round 0 |
