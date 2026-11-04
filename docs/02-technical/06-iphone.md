# 06 — iPhone (Second Platform)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 00:41 IST

## Stack

| Layer | Tool | Notes |
|---|---|---|
| Language | **Swift** | |
| UI | **SwiftUI** | Same Corn design as Android |
| Engine | **WKWebView** (WebKit) | Mandatory. Apple doesn't allow other engines outside the EU, so even Chrome on iPhone runs on WebKit. |
| Ad blocking | **WKContentRuleList** + `WKUserScript` | Apple's built-in rule system plus injected scripts. Rules are generated from the same filter lists by the Rust core. Brave iOS converts adblock-rust rules this way too. |
| Rust core | `corn-core` via **UniFFI** → `.xcframework` | Same blocking, URL cleaning, flush policy and Vault as Android |
| Flush | `WKWebsiteDataStore.nonPersistent()` | Everything stays in memory; nothing touches disk |
| Keys | **Keychain + Secure Enclave**, Face ID / Touch ID | |
| Privacy cover | Blur/cover view on `sceneWillResignActive` | App looks blank in the app switcher |

## Blocking difference vs Android

```
Android:  every request ──► Kotlin hook ──► Rust decides live
iPhone:   filter lists ──► Rust compiles ──► WKContentRuleList JSON
                                             └─► WebKit enforces it natively
```

On iPhone, Rust compiles the rules ahead of time and WebKit enforces them. Corn can't inspect requests live the way Android can. Content rule lists have a per-list rule cap, so split rules across several lists.

## App Store

- A general browser with an **18+** age rating is allowed.
- The listing must stay neutral: call it a **private browser**, not an adult-content browser.
- Apple is generally easier on browsers than Google Play is on adult-branded apps, but each submission still goes through review.

## What carries over from Android

| Carries over | Rebuilt |
|---|---|
| Rust core (blocking rules, URL cleaning, flush policy, Vault format) | UI (SwiftUI) |
| Product design and flows | Tab manager (responds to iOS memory warnings) |
| Test matrix | Platform wiring for WKWebView |
