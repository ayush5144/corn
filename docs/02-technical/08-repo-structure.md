# 08 — Repository Structure

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-11, 23:02 IST

One public repository, `ayush5144/corn` (to be renamed `flush` with the founder's OK, plan step 5), holds everything: docs, the shared Rust core, and every platform app.

**Legend:** ✅ exists · 🔜 next · ⏭️ later

```
./
├── README.md                  ✅ project overview
├── AGENTS.md                  ✅ rules for AI coding agents (CLAUDE.md imports it)
├── CLAUDE.md                  ✅ one line: @AGENTS.md
├── GLOSSARY.md                ✅ product vocabulary
├── LICENSE                    ✅ MPL-2.0
├── TRADEMARKS.md              ✅ the Flush name and logo aren't covered by the licence
├── .gitignore                 ✅
├── .github/
│   ├── dependabot.yml         ✅ weekly PRs for new Rust + Android dependency versions
│   └── workflows/             ⏭️ CI: Rust tests, Android build, iOS build
│
├── docs/                      ✅ product, technical, planning, reference, reviews, ADRs (see docs/README.md)
├── brand/                     ✅ logo and Flush Button masters (see docs/01-product/04-brand.md)
├── mockups/                   ✅ clickable HTML mockups (never hosted as artifacts)
│   └── prototype.html
│
├── core/                      ✅ SHARED RUST CORE: one implementation for every platform
│   ├── Cargo.toml             ✅ workspace: shared versions, release profile
│   └── crates/
│       ├── flush-block/       ✅ wraps Brave's adblock-rust: request checks, element hiding
│       │   ├── examples/bench.rs  ✅ benchmark on real filter lists
│       │   └── src/bin/flush-compile.rs ✅ build-time tool: compiles the offline starter list
│       ├── flush-lists/       🔜 download, verify, cache and refresh filter lists
│       ├── flush-url/         ✅ link cleaning (tracking parameters), HTTPS upgrade
│       ├── flush-policy/      ⏭️ Flush rules: what to clear, Keep List matching
│       ├── flush-store/       ✅ SQLite: Vault, Collections, tags, search, Keep List, session
│       ├── flush-ffi/         ✅ UniFFI bindings → Kotlin (Android) and Swift (iOS)
│       └── flush-node/        ⏭️ napi-rs bindings → Node.js (desktop Electron app)
│
├── android/                   ✅ ANDROID APP (Kotlin + Jetpack Compose + System WebView)
│   ├── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew
│   └── app/
│       ├── build.gradle.kts   ✅ runs scripts/build-android-core.sh before every build; release = R8 + arm64 only
│       ├── proguard-rules.pro ✅ keeps JNA + UniFFI classes through R8
│       └── src/main/
│           ├── AndroidManifest.xml          ✅ no backups, http allowed, WebView metrics opt-out
│           ├── java/com/flushbrowser/       ✅ Flush's Kotlin code
│           │   ├── MainActivity.kt          ✅ lifecycle, back button, recents privacy, fullscreen
│           │   ├── AppController.kt         ✅ app state: screens, sheets, Flush, Auto-Flush, shields, totals
│           │   ├── browser/                 ✅ WebTab (WebView + hooks), TabManager (tabs, groups, live-tab rule),
│           │   │                               BlockEngine (ads + trackers engines), Flusher, BrowserIdentity
│           │   ├── data/                    ✅ AppData (flush.db), AppSettings, SearchEngines
│           │   └── ui/                      ✅ AppRoot (root screen), Compose screens, sheets, components (ToggleSwitch…), theme, icons (Glyphs)
│           ├── assets/licenses/             ✅ font licences (OFL)
│           (app/src/test/)                  ✅ JVM unit tests (SearchEnginesTest)
│           └── res/                         ✅ icons, fonts, engine favicons, logo, theme, backup exclusion rules
│
├── ios/                       ⏭️ IPHONE APP (Swift + SwiftUI + WKWebView)
│   ├── Flush.xcodeproj
│   ├── Flush/                 Swift app code
│   └── FlushCore/             Swift package wrapping the Rust core (.xcframework + bindings)
│
├── desktop/                   ⏭️ LAPTOP APP (Electron + TypeScript + React)
│   ├── package.json
│   └── src/ (main/, renderer/)
│
├── design-agents/             the exact AI agent briefs used for logo/brand work (corn-design/ is the pre-rename record, flush-design/)
├── .claude/agents/            reusable Claude Code agents: qa-engineer, scriptlet-engineer, flush-brand-designer (corn-logo-designer kept as a record)
│
└── scripts/                   build glue between the Rust core and each app
    ├── build-android-core.sh  ✅ cargo-ndk → .so files + Kotlin bindings
    ├── build-starter-assets.sh ✅ starter filter engines + pinned autoconsent → app assets (not committed)
    ├── doc-banner.py          ✅ adds/refreshes the developer banner + Created/Last updated (IST) on every doc
    ├── dev-start.sh           ✅ starts the emulator; --install builds + installs the debug app; --serve DIR runs a test server
    ├── dev-stop.sh            ✅ stops the emulator, test servers, Gradle/Kotlin daemons and adb (frees memory)
    ├── build-ios-core.sh      ⏭️ cargo → .xcframework + Swift bindings
    └── build-desktop-core.sh  ⏭️ napi-rs → .node addon
```

## Where new code goes

```
Does every platform need the same behaviour?
├─ Yes (blocking, link cleaning, Flush rules, Vault format) ──► core/crates/<crate>/
└─ No, it touches platform APIs or UI
      ├─ Android ──► android/app/src/main/java/com/flushbrowser/
      ├─ iPhone  ──► ios/Flush/
      └─ Laptop  ──► desktop/src/
```

- **Shared logic lives in Rust once.** Never re-implement blocking, URL cleaning or the Vault format per platform.
- **Platform code stays thin:** UI, web-view wiring, OS security APIs (Keystore, Keychain), memory handling.
- **Only `flush-ffi` and `flush-node` talk to apps.** Other crates are plain Rust libraries with their own tests.
- **Upstream code is never edited** (see `AGENTS.md`): Brave's engine and other dependencies are pinned versions behind thin wrappers.

## Generated files (never committed)

| Generated | Made by | Location |
|---|---|---|
| Rust build output | `cargo` | `core/target/` |
| Android native libraries (`libflush_ffi.so`) | `scripts/build-android-core.sh` | `android/app/build/generated/uniffi/jniLibs/` |
| Kotlin bindings (`flush_ffi.kt`) | UniFFI bindgen | `android/app/build/generated/uniffi/kotlin/` |
| Android build output | Gradle | `android/app/build/`, `android/.gradle/` |
| Machine-specific SDK path | Android Studio / you | `android/local.properties` |

## Building

| What | Command (from repo root) |
|---|---|
| Rust tests | `cd core && cargo test` |
| Engine benchmark | `cd core && cargo run --release -p flush-block --example bench -- easylist.txt easyprivacy.txt` |
| Android debug app | `cd android && ./gradlew assembleDebug` (also builds the Rust core) |
| Android release app (8.7 MB, unsigned) | `cd android && ./gradlew assembleRelease` |
| Android unit tests | `cd android && ./gradlew testDebugUnitTest` |
| Test APK for a phone (release build signed with the debug key) | see [../03-planning/04-testing-on-a-phone.md](../03-planning/04-testing-on-a-phone.md) |
| Install on phone/emulator | `cd android && ./gradlew installDebug` |
| Open in Android Studio | Open the `android/` folder |

## Naming

| Thing | Name |
|---|---|
| Android application ID | `com.flushbrowser.android` (✅ decided 2026-11-11, Round 22; permanent once uploaded to Google Play) |
| Kotlin package | `com.flushbrowser` |
| Rust crates | `flush-<area>` (kebab-case), library names `flush_<area>` |
| App-wide internal names | Brand-neutral where "Flush" would read as the Flush action (`AppController`, `AppSettings`, `BlockEngine`, Rust `CoreStore`); names pages can see carry no brand (`pageBridge`, `loginBridge`) |
| Generated Kotlin package | `uniffi.flush_ffi` |
