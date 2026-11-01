# Use the OS web engine, not a Brave fork

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 14:29 IST

Corn runs on each platform's own web engine (Android System WebView, iOS WKWebView, Electron's Chromium on desktop) with a shared Rust core, instead of forking Brave Core. A fork needs a Linux build machine with 100 GB+ of disk, a rebuild for every weekly Chromium security fix, and its own update channel (Corn will likely be sideloaded because of Google Play's adult-content policy). One-person forks like Kiwi Browser died of exactly that work. On Android, Google patches WebView through the Play Store even when Corn itself is sideloaded.

## Considered Options

- **Brave Core fork:** best blocking, but unbuildable on the dev Mac and unmaintainable solo.
- **GeckoView:** full uBlock Origin, but a different engine and Corn ships engine updates itself. Kept as plan B if WebView blocking fails the spike.
- **Tauri / Flutter / React Native:** hide the WebView hooks a browser needs.

## Consequences

- Blocking is roughly 85–95% of Brave Shields (estimate, to be measured), with no extensions.
- Corn must build its own tab freezing to control memory.
- Revisit only if the spike shows WebView blocking clearly falls short. Details: [../02-technical/01-engine-decision.md](../02-technical/01-engine-decision.md).
