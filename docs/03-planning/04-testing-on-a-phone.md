# 04 — Testing on a Phone

Two APKs can be built for a real Android phone. Neither is committed (APKs never go in the repo).

| APK | Size | Use it for | How it's made |
|---|---|---|---|
| **Test build** (`Corn-<version>-test.apk`) | ~8.6 MB | Real-world feel: speed, memory, battery | Release build (R8 shrinking) signed with the Mac's debug key |
| Debug build (`Corn-<version>-debug.apk`) | ~25.6 MB | Debugging: pages can be inspected from the Mac at `chrome://inspect` | `./gradlew assembleDebug` |

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

## What to test first

The 🔨 items in [../01-product/03-features.md](../01-product/03-features.md): Tab Groups and the strip, long-press menus, the Vault (save into a new Collection, delete), Settings, Auto-Flush, Kept Session after closing the app, Keep List, cookie banners on a European site, video fullscreen, and Google sign-in.
