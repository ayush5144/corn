# 04 — Testing on a Phone

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-01 · **Last updated:** 2026-11-01, 23:16 IST

Two APKs can be built for a real Android phone. Neither is committed (APKs never go in the repo).

| APK | Size | Use it for | How it's made |
|---|---|---|---|
| **Test build** (`Corn-<version>-test.apk`) | ~8.7 MB | Real-world feel: speed, memory, battery | Release build (R8 shrinking) signed with the Mac's debug key |
| Debug build (`Corn-<version>-debug.apk`) | ~28.1 MB | Debugging: pages can be inspected from the Mac at `chrome://inspect` | `./gradlew assembleDebug` |

## Build the test APK

```bash
cd android
./gradlew assembleRelease
BT=$HOME/Library/Android/sdk/build-tools/36.1.0
$BT/zipalign -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/corn-aligned.apk
$BT/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out ~/Downloads/Corn-test.apk /tmp/corn-aligned.apk
```

The debug key is only for testing. A public release needs a real signing key (see the checklist: "signed release APK"). Losing that key would make future updates impossible, so it has to be created and stored deliberately.

## Install it

**With a cable (recommended):**
1. Phone: Settings → About phone → tap **Build number** 7 times → Developer options → **USB debugging** on.
2. Plug into the Mac and accept "Allow USB debugging?".
3. `adb install -r ~/Downloads/Corn-test.apk` (`adb` is in `~/Library/Android/sdk/platform-tools/`).

**Without a cable:** send the APK to the phone (Google Drive, Telegram, email to yourself; AirDrop doesn't reach Android), open it, and allow "Install unknown apps" for the app you opened it from.

**Switching between the two builds:** both use the same app ID and key, so one replaces the other and keeps data.

## Emulator on the Mac

`scripts/dev-start.sh --install` starts the emulator and installs the debug app (`--serve DIR` also serves test pages at `http://10.0.2.2:8765`). When you're done, `scripts/dev-stop.sh` stops the emulator, test servers and build daemons, which together use several GB of memory.

## What to test first

The 🔨 items in [../01-product/03-features.md](../01-product/03-features.md): Tab Groups and the strip, long-press menus, the Vault (save into a new Collection, delete), Settings, Auto-Flush, Kept Session after closing the app, Keep List (shield → "Stay logged in after Flush", then Flush and check you're still signed in), History (search, delete, clear) and Delete browsing data, background tabs (Settings → Advanced; a link opened in a new tab should already be loaded when you tap it), light mode (Settings → Appearance), Back from a page opened from the home page (↩), cookie banners on a European site, video fullscreen, and Google sign-in.
