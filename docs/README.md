# Corn Docs

Read in folder order. Start with the product, then the technical plan.

## 01 — Product: *what* we're building

| # | Doc | What it covers |
|---|---|---|
| 01 | [PRD](01-product/01-prd.md) | What Corn is, who it's for, every feature with ✅ settled / 🟡 open status |
| 02 | [Decision log](01-product/02-decision-log.md) | Every question asked and the founder's answer, round by round |

## 02 — Technical: *how* we build it

| # | Doc | What it covers |
|---|---|---|
| 01 | [Engine decision](02-technical/01-engine-decision.md) | Why WebView + native shell instead of a Brave fork, with data |
| 02 | [Architecture](02-technical/02-architecture.md) | The whole system across devices, layers and request flow |
| 03 | [Android](02-technical/03-android.md) | The Android stack in detail (first platform) |
| 04 | [Rust core](02-technical/04-rust-core.md) | Where Rust is used, where it isn't, and what it does for memory |
| 05 | [Flush model](02-technical/05-flush-model.md) | How Flush works on each platform |
| 06 | [iPhone](02-technical/06-iphone.md) | iOS stack and App Store notes |
| 07 | [Desktop](02-technical/07-desktop.md) | Laptop stack: Electron vs Tauri vs native |

## 03 — Planning: *when* we build it

| # | Doc | What it covers |
|---|---|---|
| 01 | [Roadmap](03-planning/01-roadmap.md) | Platform order, the first spike test, build steps, decision gates |
| 02 | [Checklist](03-planning/02-checklist.md) | Every task by phase, with owner and "done when"; backlog |

## 04 — Reference

| # | Doc | What it covers |
|---|---|---|
| 01 | [Sources](04-reference/01-sources.md) | Every external fact and where it came from |

## ADRs — Architecture decisions

Short records of hard-to-reverse choices and why they were made.

| # | Decision |
|---|---|
| 0001 | [Use the OS web engine, not a Brave fork](adr/0001-os-web-engine-not-brave-fork.md) |

## Vocabulary

Product terms (Session, Flush, Vault, Keep List…) are defined in [../GLOSSARY.md](../GLOSSARY.md). Use those words in docs, UI copy and code.

## Conventions

- Folders and files are numbered in reading order.
- New product decisions go into the decision log first, then the PRD.
- A hard-to-reverse technical choice gets an ADR in `adr/` (numbered `0002-…`, `0003-…`).
