# Corn

**A free, private, "use whenever you want" browser.**

Every session can start empty, one tap flushes it, and a locked local Vault keeps only what you choose to save. Ads, trackers, pop-ups and redirect abuse are blocked by default, and nothing about your browsing leaves your device.

## Status

Early build. The Rust core and an Android proof-of-concept exist; the product is still being shaped. The product is being shaped through Q&A rounds, recorded in the [decision log](docs/01-product/02-decision-log.md).

## How it's built (planned)

Native app on each device, using the device's own web engine, plus one shared Rust core:

| Device | Stack | Order |
|---|---|---|
| Android | Kotlin + Jetpack Compose + System WebView | 1st |
| iPhone | Swift + SwiftUI + WKWebView | 2nd |
| Laptop | Electron (TypeScript + React) | 3rd |
| Shared | Rust: blocking (Brave's `adblock-rust`), URL cleaning, flush rules, Vault | All |

Why this and not a Brave fork: [ADR 0001](docs/adr/0001-os-web-engine-not-brave-fork.md).

## Docs and code

The code is always the source of truth. The docs in [`docs/`](docs/README.md) explain how and why things work, but they can fall behind: if a doc and the code disagree, trust the code. Each doc shows when it was created and last updated.

## Repository

```
corn/
├── README.md       this file
├── AGENTS.md       rules for AI coding agents (CLAUDE.md imports it)
├── GLOSSARY.md     product vocabulary (Session, Flush, Vault, …)
├── LICENSE         MPL-2.0
├── TRADEMARKS.md   the name and logo aren't covered by the licence
├── docs/           product, technical, planning, reference, ADRs (start at docs/README.md)
├── core/           shared Rust core: Brave's ad-block engine + Corn logic
├── android/        Android app (Kotlin + Compose + WebView)
├── scripts/        build glue between the Rust core and the apps
├── mockups/        clickable HTML mockups
└── brand/          logo files
```

Planned: `ios/` (Swift) and `desktop/` (Electron). Full layout and rules: [docs/02-technical/08-repo-structure.md](docs/02-technical/08-repo-structure.md).

## Principles

- Nothing about your browsing leaves the device by default.
- No accounts, ads or tracking. Free, funded by donations.
- Flush clears this device only. Corn never claims to erase what websites, networks or the OS recorded.
- Corn is a browser. It does not host, index or recommend content.

## Licence

Code: [MPL-2.0](LICENSE). The Corn name and logo are trademarks and aren't covered by the licence; see [TRADEMARKS.md](TRADEMARKS.md).

