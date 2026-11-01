# 02 — Open Source We Build On

What Corn takes from Brave, DuckDuckGo, Mozilla and others. Licences checked on GitHub on 2026-11-01.

## Ad blocking

| Project | Licence | What Corn uses |
|---|---|---|
| [brave/adblock-rust](https://github.com/brave/adblock-rust) | MPL-2.0 | **The blocking engine** (Rust): request checks, cosmetic hiding, scriptlets |
| [brave/adblock-lists](https://github.com/brave/adblock-lists) | MPL-2.0 | Brave's extra filter lists |
| [brave/adblock-resources](https://github.com/brave/adblock-resources) | MPL-2.0 | Scriptlets and replacement resources (beats anti-adblock walls) |
| [EasyList / EasyPrivacy](https://github.com/easylist/easylist) | GPL-3.0 or CC BY-SA 3.0+ (dual) | Core ad and tracker rules |
| [uBlockOrigin/uAssets](https://github.com/uBlockOrigin/uAssets) | GPL-3.0 | uBlock Origin's filters: annoyances, badware, unbreak fixes |
| [AdguardTeam/AdguardFilters](https://github.com/AdguardTeam/AdguardFilters) | GPL-3.0 | Optional extra lists (pop-ups, mobile ads) |

Filter lists are **downloaded on the device at runtime** as data and credited in About, the same way Brave and uBlock Origin handle them.

## Privacy protections

| Project | Licence | What Corn uses |
|---|---|---|
| [duckduckgo/Android](https://github.com/duckduckgo/Android) | Apache-2.0 | **Corn's closest twin**: a Kotlin + WebView browser. Reference for the Fire Button, WebView setup, request interception, tab handling and privacy settings. |
| [duckduckgo/content-scope-scripts](https://github.com/duckduckgo/content-scope-scripts) | Apache-2.0 | Scripts injected at page start: fingerprinting protection, Global Privacy Control, other in-page protections |
| [duckduckgo/autoconsent](https://github.com/duckduckgo/autoconsent) | MPL-2.0 | Auto-dismisses cookie-consent pop-ups |
| [ClearURLs/Rules](https://github.com/ClearURLs/Rules) | LGPL-3.0 | Tracking-parameter and redirect-wrapper rules for `corn-url` |
| [publicsuffix/list](https://github.com/publicsuffix/list) | MPL-2.0 | Works out which site a URL belongs to (Keep List, third-party detection) |
| [duckduckgo/apple-browsers](https://github.com/duckduckgo/apple-browsers) | Apache-2.0 | iPhone later: WKWebView browser reference |

## Plumbing

| Project | Licence | What Corn uses |
|---|---|---|
| [mozilla/uniffi-rs](https://github.com/mozilla/uniffi-rs) | MPL-2.0 | Generates Kotlin and Swift bindings for the Rust core |
| [sqlcipher/sqlcipher](https://github.com/sqlcipher/sqlcipher) | BSD-3-Clause | Encrypted database for the Vault |

## How each protection is built

| Protection | Built with | Where it runs |
|---|---|---|
| Ad / tracker / pop-up blocking | adblock-rust + filter lists | Rust, from `shouldInterceptRequest` |
| Cookie banners | **autoconsent** clicks "reject"; **EasyList Cookie List** hides leftovers | Injected at page start; cosmetic rules in adblock-rust |
| Fingerprinting + Global Privacy Control | **content-scope-scripts** code with **Corn's own config** | Injected at page start (`addDocumentStartJavaScript`) |
| Link cleaning | **AdGuard URL Tracking filter** (`$removeparam`, supported by adblock-rust) + **ClearURLs** redirect rules | Rust `corn-url`, on top-level navigations |
| Third-party cookies | WebView's own switch (`setAcceptThirdPartyCookies(false)`) | Android setting |
| HTTPS upgrade | Rewrite `http://` → `https://`, fall back if it fails; never allow mixed content | Kotlin |

## Deliberately not used

| Project | Why not |
|---|---|
| DuckDuckGo privacy-configuration | CC BY-NC-SA 4.0 (non-commercial). Corn writes its own config for content-scope-scripts. |
| DuckDuckGo Tracker Blocklists / Tracker Radar | CC BY-NC-SA 4.0 (**non-commercial**). It would block a future paid Cloud Vault. EasyPrivacy covers the same need. |
| Firefox Android (`mozilla-mobile/firefox-android`) | Archived and built on GeckoView; ideas only |
| Brave's Chromium-level privacy code (fingerprint randomisation, debouncing in C++) | Lives inside Chromium; it can't be lifted into WebView. Corn re-creates the ideas (e.g. redirect unwrapping in Rust). |
| DuckDuckGo iOS (`duckduckgo/iOS`) | Archived; moved to `apple-browsers` |

## Licence obligations (summary, not legal advice)

- **Apache-2.0 / BSD:** keep copyright and licence notices; list them in About → Open-source licences.
- **MPL-2.0:** if Corn modifies an MPL file, that file's changes must be published. Using it unmodified only needs the notice.
- **GPL-3.0 / LGPL-3.0 lists and rules:** fetched as data at runtime and credited; not merged into Corn's own code.
- Legal review before the public launch.
