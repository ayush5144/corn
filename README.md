# Corn 🌽

**A free, private, "use whenever you want" browser for adults.**

Every session can start empty, one tap flushes it, and an encrypted local Vault keeps only what you choose to save. Ads, trackers, pop-ups and redirect abuse are blocked by default, and nothing about your browsing leaves your device.

## Status

Planning. No app code yet. The product is being shaped through Q&A rounds, recorded in the [decision log](docs/01-product/02-decision-log.md).

## How it's built (planned)

Native app on each device, using the device's own web engine, plus one shared Rust core:

| Device | Stack | Order |
|---|---|---|
| Android | Kotlin + Jetpack Compose + System WebView | 1st |
| iPhone | Swift + SwiftUI + WKWebView | 2nd |
| Laptop | Electron (TypeScript + React) | 3rd |
| Shared | Rust: blocking (Brave's `adblock-rust`), URL cleaning, flush rules, Vault | All |

Why this and not a Brave fork: [ADR 0001](docs/adr/0001-os-web-engine-not-brave-fork.md).

## Repository

```
corn/
├── README.md      this file
├── AGENTS.md      rules for AI coding agents (CLAUDE.md imports it)
├── LICENSE        MPL-2.0
├── TRADEMARKS.md  the name and logo aren't covered by the licence
├── GLOSSARY.md    product vocabulary (Session, Flush, Vault, …)
├── brand/         logo files
├── mockups/       clickable phone-size HTML mockups (open in a browser)
└── docs/          product, technical, planning, reference, ADRs
    └── README.md  docs index — start here
```

Planned later: `core/` (Rust), `android/`, `ios/`, `desktop/`.

## Principles

- Nothing about your browsing leaves the device by default.
- No accounts, ads or tracking. Free, funded by donations.
- Flush clears this device only. Corn never claims to erase what websites, networks or the OS recorded.
- Corn is a browser. It does not host, index or recommend content.

## Licence

Code: [MPL-2.0](LICENSE). The Corn name and logo are trademarks and aren't covered by the licence; see [TRADEMARKS.md](TRADEMARKS.md).

