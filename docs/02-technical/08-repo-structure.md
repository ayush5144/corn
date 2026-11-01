# 08 — Repository Structure

One public repository, `ayush5144/corn`, holds everything: docs, the shared Rust core, and every platform app.

**Legend:** ✅ exists · 🔜 next · ⏭️ later

```
corn/
├── README.md                  ✅ project overview
├── AGENTS.md                  ✅ rules for AI coding agents (CLAUDE.md imports it)
├── CLAUDE.md                  ✅ one line: @AGENTS.md
├── GLOSSARY.md                ✅ product vocabulary
├── LICENSE                    ✅ MPL-2.0
├── TRADEMARKS.md              ✅ name and logo aren't covered by the licence
├── .gitignore                 ✅
├── .github/
│   ├── dependabot.yml         ✅ weekly PRs for new Rust + Android dependency versions
│   └── workflows/             ⏭️ CI: Rust tests, Android build, iOS build
│
├── docs/                      ✅ product, technical, planning, reference, ADRs (see docs/README.md)
├── brand/                     ✅ logo source files
├── mockups/                   ✅ clickable HTML mockups (never hosted as artifacts)
│   └── prototype.html
│
├── core/                      ✅ SHARED RUST CORE: one implementation for every platform
│   ├── Cargo.toml             ✅ workspace: shared versions, release profile
│   └── crates/
│       ├── corn-block/        ✅ wraps Brave's adblock-rust: request checks, element hiding
│       │   └── examples/bench.rs  ✅ benchmark on real filter lists
│       ├── corn-lists/        🔜 download, verify, cache and refresh filter lists
│       ├── corn-url/          🔜 link cleaning, redirect unwrapping, site keys (Public Suffix List)
│       ├── corn-flush/        ⏭️ Flush rules: what to clear, Keep List matching
│       ├── corn-vault/        ✅ SQLite: Vault, Collections, tags, search, Keep List, session
│       ├── corn-ffi/          ✅ UniFFI bindings → Kotlin (Android) and Swift (iOS)
│       └── corn-node/         ⏭️ napi-rs bindings → Node.js (desktop Electron app)
│
├── android/                   ✅ ANDROID APP (Kotlin + Jetpack Compose + System WebView)
│   ├── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew
│   └── app/
│       ├── build.gradle.kts   ✅ runs scripts/build-android-core.sh before every build
│       └── src/main/
│           ├── AndroidManifest.xml          ✅ no backups, internet only
│           ├── java/app/cornbrowser/        ✅ Corn's Kotlin code
│           │   ├── MainActivity.kt          ✅ spike UI
│           │   ├── CornTab.kt               ✅ one tab: WebView + blocking hooks
│           │   ├── CornEngine.kt            ✅ loads/caches the shared blocking engine
│           │   └── BrowserIdentity.kt       ✅ Chrome-style user agent
│           └── res/                         ✅ icons, theme, backup exclusion rules
│
├── ios/                       ⏭️ IPHONE APP (Swift + SwiftUI + WKWebView)
│   ├── Corn.xcodeproj
│   ├── Corn/                  Swift app code
│   └── CornCore/              Swift package wrapping the Rust core (.xcframework + bindings)
│
├── desktop/                   ⏭️ LAPTOP APP (Electron + TypeScript + React)
│   ├── package.json
│   └── src/ (main/, renderer/)
│
└── scripts/                   build glue between the Rust core and each app
    ├── build-android-core.sh  ✅ cargo-ndk → .so files + Kotlin bindings
    ├── build-ios-core.sh      ⏭️ cargo → .xcframework + Swift bindings
    └── build-desktop-core.sh  ⏭️ napi-rs → .node addon
```

## Where new code goes

```
Does every platform need the same behaviour?
├─ Yes (blocking, link cleaning, Flush rules, Vault format) ──► core/crates/<crate>/
└─ No, it touches platform APIs or UI
      ├─ Android ──► android/app/src/main/java/app/cornbrowser/
      ├─ iPhone  ──► ios/Corn/
      └─ Laptop  ──► desktop/src/
```

- **Shared logic lives in Rust once.** Never re-implement blocking, URL cleaning or the Vault format per platform.
- **Platform code stays thin:** UI, web-view wiring, OS security APIs (Keystore, Keychain), memory handling.
- **Only `corn-ffi` and `corn-node` talk to apps.** Other crates are plain Rust libraries with their own tests.
- **Upstream code is never edited** (see `AGENTS.md`): Brave's engine and other dependencies are pinned versions behind thin wrappers.

## Generated files (never committed)

| Generated | Made by | Location |
|---|---|---|
| Rust build output | `cargo` | `core/target/` |
| Android native libraries (`libcorn_ffi.so`) | `scripts/build-android-core.sh` | `android/app/build/generated/uniffi/jniLibs/` |
| Kotlin bindings (`corn_ffi.kt`) | UniFFI bindgen | `android/app/build/generated/uniffi/kotlin/` |
| Android build output | Gradle | `android/app/build/`, `android/.gradle/` |
| Machine-specific SDK path | Android Studio / you | `android/local.properties` |

## Building

| What | Command (from repo root) |
|---|---|
| Rust tests | `cd core && cargo test` |
| Engine benchmark | `cd core && cargo run --release -p corn-block --example bench -- easylist.txt easyprivacy.txt` |
| Android debug app | `cd android && ./gradlew assembleDebug` (also builds the Rust core) |
| Install on phone/emulator | `cd android && ./gradlew installDebug` |
| Open in Android Studio | Open the `android/` folder |

## Naming

| Thing | Name |
|---|---|
| Android application ID / Kotlin package | `app.cornbrowser` (🟡 confirm before first public release; it can't change after) |
| Rust crates | `corn-<area>` (kebab-case), library names `corn_<area>` |
| Generated Kotlin package | `uniffi.corn_ffi` |
