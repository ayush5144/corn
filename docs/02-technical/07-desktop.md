# 07 — Desktop / Laptop (Third Platform)

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-05, 02:00 IST

## Recommendation: Electron

One codebase covers Windows, Mac and Linux, and video plays the same everywhere. The cost is app size and shipping Electron updates yourself. That's a dependency bump, not a fork rebuild.

## Comparison

| | **Electron** ✅ | Tauri | Native per OS (DuckDuckGo's way) |
|---|---|---|---|
| Engine | Chromium on every OS | WebKit on Mac, WebView2 on Windows, WebKitGTK on Linux | WebKit on Mac, WebView2 on Windows |
| Codebases | **1** | 1, but behaves differently per OS | **2+** (Swift and C#) |
| Video on Linux | Good | Weak | n/a |
| Multiple tabs | Mature support (`WebContentsView`) | Still experimental | Good |
| App size | ~100 MB+ | ~10 MB | Small |
| Security updates | You bump Electron (~every 8 weeks) | The OS handles it | The OS handles it |
| Fit for a solo developer | **Best** | Risky for a browser | Too much work |

## Stack

| Layer | Tool |
|---|---|
| Shell | **Electron** (main process in TypeScript) |
| UI | **React + TypeScript** |
| Tabs | One `WebContentsView` per live tab; same freeze policy as mobile |
| Blocking | Rust `corn-block` via **napi-rs**, hooked into `session.webRequest` |
| Flush | Session partition **without** `persist:` → memory only |
| Keys | OS keychain (macOS Keychain, Windows Credential Manager / DPAPI, libsecret) |
| Updates | `electron-updater`, signed builds, a security-first cadence |
| Desktop extras | Hover tab previews, keyboard tab search, optional vertical tabs later |

## Update obligation

Unlike mobile, desktop Corn **ships its own Chromium inside Electron**, so security updates are Corn's job:

```
Electron stable release
        │
        ▼
Bump dependency ──► run test suite ──► sign ──► push via auto-updater
```

The release check must be automated (a bot or CI alert on new Electron releases) before the desktop public launch.

## If only Mac matters

A Swift + WKWebView Mac app can reuse much of the iPhone code. Revisit this only if Windows users turn out to be a small share of demand. Windows dominates desktop overall, so Electron stays the default.
