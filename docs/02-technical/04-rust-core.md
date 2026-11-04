# 04 — Rust Core: Where Rust Is Used and Why

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 01:33 IST

## The honest headline

**Rust will not make the web engine use less memory.** The biggest memory consumer in Corn is the web engine itself (Blink/V8 or WebKit renderers, video buffers, page JavaScript), and Google, Apple or Electron own that code.

Rust earns its place in three ways:

1. **Blocking**, the biggest memory lever Corn controls inside a page. Every ad script, iframe and video ad that never loads is memory and CPU never spent.
2. **One implementation on every platform.** Blocking, URL cleaning, flush rules and the Vault format behave identically on Android, iPhone and laptop.
3. **Control and safety** for security-critical code (encryption, parsing untrusted filter lists) without garbage-collector pauses or memory-safety bugs.

## Memory levers, ranked by impact

| Rank | Lever | Who owns it | Impact |
|---|---|---|---|
| 1 | Limit live tabs; freeze the rest ([03](03-android.md)) | Platform shell (Kotlin/Swift/TS) | **Largest.** Each live web view carries a renderer. |
| 2 | Block ads, trackers and pop-ups before they load | **Rust** (`corn-block`) | **Large.** Ad scripts and video ads are heavy. |
| 3 | Small blocking-engine footprint | **Rust** (adblock-rust + FlatBuffers) | **Medium.** Brave cut adblock-rust memory ~75% (~45 MB) by moving filters into FlatBuffers. One engine instance is shared by all tabs. |
| 4 | Writing app logic in Rust instead of Kotlin/Swift | Rust | **Small.** Not a reason on its own. |

## What goes in Rust ✅

| Crate | Job | Why Rust | Phase |
|---|---|---|---|
| `corn-block` | Wraps adblock-rust: request checks, cosmetic CSS, scriptlets | Same engine as Brave Shields; ~5 µs per request check | 1 |
| `corn-lists` | Download, verify, compile and cache filter lists as a compact binary engine | Off-main-thread, memory-compact, identical everywhere | 1 |
| `corn-url` | ✅ Strip tracking parameters, HTTPS upgrade. ⏳ Redirect unwrapping, site keys (Public Suffix List) | Parses untrusted input; shared rules | 1 |
| `corn-flush` | Flush **policy**: what to clear, when, and which Keep-list sites to spare | Same rules on every platform | 2 |
| `corn-vault` | SQLite database (rusqlite): Collections, Vault Items, tags, search, key-value area for Keep List / session | One Vault format everywhere; security-critical | 2 |
| `corn-ffi` | UniFFI bindings (Kotlin, Swift) | Generated glue; keeps the API small | 1 |
| `corn-node` | napi-rs bindings for the Electron desktop app | UniFFI doesn't target Node.js | Desktop |

## What stays out of Rust ❌

| Thing | Stays in | Why |
|---|---|---|
| UI | Compose / SwiftUI / React | Native UI is faster to build and feels right |
| WebView / WKWebView control | Kotlin / Swift | Platform APIs; wrapping them in Rust adds layers for nothing |
| Tab lifecycle and memory policy | Kotlin / Swift / TS | Must react to OS memory signals (`onTrimMemory`, iOS memory warnings) |
| Video playback | Web engine | Hardware decoding is already native |
| Downloads networking | OkHttp / URLSession / Electron | Platform handles background work, battery and permissions |
| Key storage | Android Keystore / Secure Enclave / OS keychain | Keys belong in secure hardware. Rust receives the key; it never stores it. |
| Executing a flush | Platform APIs | Only the platform can delete a WebView profile or data store. Rust decides; the platform executes. |

## How it's wired

```
corn-core/                      (Rust workspace, separate from the app shells)
├── crates/
│   ├── corn-block/             adblock-rust wrapper
│   ├── corn-lists/             filter list manager
│   ├── corn-url/               URL cleaning
│   ├── corn-flush/             flush policy
│   ├── corn-vault/             SQLite storage
│   ├── corn-ffi/               UniFFI bindings (Kotlin, Swift)
│   └── corn-node/              napi-rs bindings (Electron)
└── Cargo.toml

        corn-ffi
           │
   ┌───────┼──────────────┐
   ▼       ▼              ▼
 Kotlin   Swift        Node.js
 (.so via (.xcframework (.node via
 cargo-ndk) via cargo)   napi-rs)
```

## Hot-path rule

`shouldInterceptRequest` calls Rust for **every** sub-request, often hundreds per page. Keep that call boring:

- Pass plain strings (URL, source URL, request type); return a small enum (`Allow`, `Block`, `Redirect(stub)`).
- Load the engine once at startup from the precompiled binary; never parse filter lists on the request path.
- Two engine instances (ads, trackers), each shared by all tabs and read-only during browsing; swap them atomically when lists update.

## Decision: Vault in Rust or in Kotlin?

| | Vault in Rust ✅ | Vault in Kotlin (Room) |
|---|---|---|
| Same format on iPhone and laptop | Yes, one codebase | No, rewrite per platform |
| Export from phone → open on laptop | Works by design | Needs a hand-written compatible format |
| Android-only speed | Slightly slower to start | Faster to start |
| Pick if… | iPhone or desktop is planned | Android is the only platform ever |

Recommendation: **Rust**, since iPhone and laptop are on the roadmap.
