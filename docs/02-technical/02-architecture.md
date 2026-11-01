# 02 — Architecture

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 14:38 IST

## Shape of the system

Corn is **three thin native shells plus one shared Rust core.**

- Each shell is written in its platform's native language and uses that platform's web engine.
- The Rust core holds the logic that must behave the same everywhere: blocking, filter lists, URL cleaning, flush rules and the Vault.
- Bindings are generated: **UniFFI** for Kotlin and Swift, **napi-rs** for Electron (Node.js).

Mozilla shares code across its apps this way (application-services), and so does Brave (adblock-rust across every platform).

```
                  ┌────────────────────────────────────────┐
                  │   SHARED CORN CORE (Rust)              │
                  │   • adblock-rust (blocking)            │
                  │   • filter list updates                │
                  │   • URL cleaning                       │
                  │   • flush rules                        │
                  │   • Vault storage (SQLite)             │
                  └──────┬──────────────┬─────────────┬────┘
                 UniFFI  │      UniFFI  │      napi-rs│
                         ▼              ▼             ▼
            ┌────────────────┐ ┌───────────────┐ ┌──────────────────┐
            │ ANDROID        │ │ iPHONE        │ │ LAPTOP           │
            │ Kotlin +       │ │ Swift +       │ │ Electron         │
            │ Jetpack Compose│ │ SwiftUI       │ │ (TypeScript +    │
            │                │ │               │ │  React UI)       │
            ├────────────────┤ ├───────────────┤ ├──────────────────┤
            │ System WebView │ │ WKWebView     │ │ Chromium inside  │
            │ (Blink + V8)   │ │ (WebKit)      │ │ Electron         │
            │ Google patches │ │ Apple patches │ │ You ship updates │
            └────────────────┘ └───────────────┘ └──────────────────┘
               Build 1st          Build 2nd          Build 3rd
```

## What each device uses

| | **Brave** (reference) | **DuckDuckGo** (closest to Corn) | **Corn** |
|---|---|---|---|
| Android | Chromium fork: C++ core, UI mostly **Java** with some Kotlin | **Kotlin** + System WebView | **Kotlin** + System WebView |
| iPhone | **Swift** + WKWebView | **Swift** + WKWebView | **Swift** + WKWebView |
| Mac | Chromium fork (C++, UI pages in TypeScript/React) | **Swift** + WKWebView | Electron |
| Windows | Chromium fork (C++) | WebView2 (Microsoft's Chromium-based WebView) | Electron |
| Ad blocking | **adblock-rust** (Rust) | Their own tracker blocker | **adblock-rust** (Rust) |

Brave writes Java on Android only because it inherited Chromium's Android code. A new Android app uses Kotlin; Google has been "Kotlin-first" for Android since 2019.

## Layer diagram (Android shown; iPhone and desktop mirror it)

```
                         ┌───────────────────────────┐
                         │          USER             │
                         └─────────────┬─────────────┘
                                       │ taps / types
                                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                CORN APP  (Kotlin + Jetpack Compose)              │
│                                                                  │
│   Toolbar & URL bar   Tab switcher (grid, swipe, search)   Vault │
│          │                    │                              │   │
│          ▼                    ▼                              ▼   │
│   ┌─────────────┐     ┌──────────────┐           ┌─────────────┐ │
│   │ Tab Manager │     │ Tab previews │           │ Vault UI    │ │
│   │ N in memory,│     │ (local only) │           │ 🔑 Keystore │ │
│   │ rest unload.│     └──────────────┘           └──────┬──────┘ │
│   └──────┬──────┘                                       │        │
└──────────┼──────────────────────────────────────────────┼────────┘
           │ one WebView per live tab                     │
           ▼                                              ▼
┌──────────────────────────────────────┐   ┌───────────────────────┐
│ androidx.webkit                      │   │  RUST CORE (UniFFI)   │
│ profiles · page-start scripts        │   │  corn-vault           │
│ Normal profile │ Private profile     │   │  corn-flush           │
└──────────────────┬───────────────────┘   └───────────────────────┘
                   │ every page request
                   ▼
┌──────────────────────────────────────────────────────────────────┐
│     CORN BLOCKER  (Kotlin hooks → UniFFI → Rust corn-block)      │
│   two engines: ads (EasyList) + trackers (EasyPrivacy), per site │
│   ads / trackers ......... shouldInterceptRequest  → BLOCK       │
│   pop-up windows ......... onCreateWindow          → DENY        │
│   redirect hijacks ....... shouldOverrideUrlLoading→ STOP        │
│   hidden ads + anti-adblock  page-start scripts    → HIDE/PATCH  │
└──────────────────────────────┬───────────────────────────────────┘
                               │ only allowed requests
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│   ANDROID SYSTEM WEBVIEW  (Blink + V8 + hardware video decode)   │
│               ← security updates pushed by Google                │
└──────────────────────────────┬───────────────────────────────────┘
                               ▼
                           🌐 Website
```

## Request flow

```
Page asks for a resource
        │
        ▼
Platform hook catches it  (shouldInterceptRequest / WKContentRuleList / Electron webRequest)
        │
        ▼
Rust corn-block: "block, allow, or redirect to a stub?"   ~5 µs per check
        │
   ┌────┴─────┐
   ▼          ▼
 BLOCK      ALLOW ──► Engine's own network stack ──► Website
(empty
 response)
```

Corn servers are never in this path. The only network calls Corn itself makes are filter-list downloads and an update check, and neither carries browsing data.

## Ownership boundaries

| Layer | Owner | Corn's rule |
|---|---|---|
| Web engine (Blink/V8, WebKit) | Google / Apple / Electron | Never patched. Updated by the OS (mobile) or an Electron bump (desktop). |
| Platform shell | Corn (Kotlin / Swift / TypeScript) | UI, tabs, WebView wiring, platform security APIs. |
| Rust core | Corn | Shared logic that must behave identically everywhere. |
| Filter lists | EasyList, EasyPrivacy, uBlock filters, etc. | Downloaded on device; respect each list's licence. |

## Local data

```
Corn on device
├── Browsing session      # cookies, cache, site data — wiped on Flush
├── Tab state + previews  # session only — wiped on Flush
├── Keep list             # sites allowed to stay logged in (optional)
├── Corn settings         # preferences
└── Vault                 # PIN-locked, survives every Flush
```

See [05-flush-model.md](05-flush-model.md) for the lifecycle and [04-rust-core.md](04-rust-core.md) for which parts are Rust.
