# Corn — Rules for AI Coding Agents

Corn is a free, private, mobile-first browser for adults. Start with `README.md`, then `docs/README.md`.

## Where things live

| What | Where |
|---|---|
| What we're building | `docs/01-product/01-prd.md` |
| Every product question + the founder's answer | `docs/01-product/02-decision-log.md` |
| Product vocabulary (use these exact words) | `GLOSSARY.md` |
| Technical plan | `docs/02-technical/` |
| Tasks, owners, "done when" | `docs/03-planning/02-checklist.md` |
| Hard-to-reverse decisions | `docs/adr/` |
| Mockups and prototypes | `mockups/` |
| Logos | `brand/` |
| Security and encryption (passwords, PIN, what's protected) | `docs/02-technical/12-security.md` |
| Logo / brand explorations and the agents that made them | `mockups/brand-compare.html`, `design-agents/`, `.claude/agents/` |

## Rules

- **The code is the source of truth.** Docs explain the code; when a doc and the code disagree, trust the code and fix the doc. Every doc's banner says this (see below).
- **Mockups are HTML files in `mockups/`.** Never publish mockups or prototypes as claude.ai Artifacts or any other hosted page. Plain HTML first; Next.js static export only if necessary. Design at mobile viewport size.
- **Docs are numbered** in reading order inside their folder. Keep `docs/README.md` indexing every doc.
- **Every doc carries the developer banner** (each file in `docs/` and `GLOSSARY.md`, right under the title): the "For developers and AI Agents" note plus **Created** and **Last updated** in IST. When you edit a doc, update its "Last updated" (run `python3 scripts/doc-banner.py` before committing; it reads the dates from git). New docs get the banner too. Not used on `README.md`, `AGENTS.md`, `LICENSE` or `TRADEMARKS.md`.
- **Product decisions go into the decision log first**, then the PRD. Mark each requirement ✅ settled, 🟡 open or ⏭️ later.
- **Never modify upstream code.** Brave's adblock-rust, DuckDuckGo's scripts, UniFFI and other third-party code are used as unmodified, version-pinned dependencies behind a thin Corn wrapper, so updates stay a version bump. Filter lists are downloaded from their authors at runtime, never copied into the repo. DuckDuckGo's and Brave's apps are references for patterns, not code to copy wholesale. If an upstream change is truly needed, propose it upstream first and record the reason in an ADR.
- **Use the glossary's terms** (Session, Flush, Flush Button, Kept Session, Keep List, Vault, Collection, Tab Group, Tab Group Strip…). Add a term the moment it's resolved.
- **Tick a checklist box only when its "Done when" is true.**
- **Privacy is a product requirement:** never add code that sends URLs, searches, titles, tabs or Vault contents off the device.
- **Never log URLs, titles, searches or site names**, not even in debug builds. Blocking stats are plain counts only; Corn keeps no record of what was blocked where.
- Mockups use fake, neutral sites only. No adult content in the repo, mockups, icons or store assets.
