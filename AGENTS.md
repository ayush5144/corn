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

## Rules

- **Mockups are HTML files in `mockups/`.** Never publish mockups or prototypes as claude.ai Artifacts or any other hosted page. Plain HTML first; Next.js static export only if necessary. Design at mobile viewport size.
- **Docs are numbered** in reading order inside their folder. Keep `docs/README.md` indexing every doc.
- **Product decisions go into the decision log first**, then the PRD. Mark each requirement ✅ settled, 🟡 open or ⏭️ later.
- **Use the glossary's terms** (Session, Flush, Flush Button, Kept Session, Keep List, Vault, Collection, Tab Group, Tab Group Strip…). Add a term the moment it's resolved.
- **Tick a checklist box only when its "Done when" is true.**
- **Privacy is a product requirement:** never add code that sends URLs, searches, titles, tabs or Vault contents off the device.
- Mockups use fake, neutral sites only. No adult content in the repo, mockups, icons or store assets.
