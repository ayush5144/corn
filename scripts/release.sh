#!/usr/bin/env bash
# Build a signed Corn release and (optionally) publish it as a GitHub pre-release.
#
#   scripts/release.sh 0.2.1        new visible version 0.2.1; versionCode goes up by 1
#
# Needs the release key set up once (scripts/create-release-key.sh, then the password in
# ~/.gradle/gradle.properties). Every step that changes GitHub asks first.
# Explained in docs/03-planning/05-releasing-and-updates.md.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
NAME="${1:-}"; [ -n "$NAME" ] || { echo "Usage: scripts/release.sh <version, e.g. 0.2.1>" >&2; exit 1; }
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH" ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
PROPS="$HOME/.gradle/gradle.properties"
ask() { read -r -p "$1 [y/N] " a; [ "$a" = "y" ] || [ "$a" = "Y" ]; }

# 1. The key is set up (values present, not placeholders, file exists).
get() { grep -E "^$1=" "$PROPS" 2>/dev/null | head -1 | cut -d= -f2-; }
KEYFILE="$(get CORN_RELEASE_STORE_FILE)"
for v in CORN_RELEASE_STORE_FILE CORN_RELEASE_STORE_PASSWORD CORN_RELEASE_KEY_ALIAS CORN_RELEASE_KEY_PASSWORD; do
  val="$(get $v)"; [ -n "$val" ] && [ "$val" != "CHANGE_ME" ] || { echo "Set $v in $PROPS first." >&2; exit 1; }
done
[ -f "$KEYFILE" ] || { echo "Key file not found: $KEYFILE (run scripts/create-release-key.sh)." >&2; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "Commit or stash your changes first." >&2; exit 1; }

# 2. Version: versionCode +1, versionName as given.
GRADLE=android/app/build.gradle.kts
OLD_CODE="$(sed -n 's/^ *versionCode = \([0-9]*\).*/\1/p' "$GRADLE")"; NEW_CODE=$((OLD_CODE + 1))
sed -i '' "s/^\( *\)versionCode = $OLD_CODE/\1versionCode = $NEW_CODE/; s/^\( *\)versionName = \".*\"/\1versionName = \"$NAME\"/" "$GRADLE"
echo "Version: $NAME (versionCode $OLD_CODE -> $NEW_CODE)"

# 3. Test and build.
(cd android && ./gradlew -q :app:testDebugUnitTest :app:assembleRelease)
APK=android/app/build/outputs/apk/release/app-release.apk
[ -f "$APK" ] || { echo "No signed APK at $APK: is the release key configured?" >&2; git checkout -- "$GRADLE"; exit 1; }

# 4. Verify the signature is Corn's key, not the debug key.
CERT="$("$BT/apksigner" verify --print-certs "$APK" 2>/dev/null)"
echo "$CERT" | grep -E "certificate DN|SHA-256" | head -2
echo "$CERT" | grep -q "CN=Android Debug" && { echo "Signed with the DEBUG key: stopping." >&2; git checkout -- "$GRADLE"; exit 1; }
OUT="$HOME/Downloads/Corn-$NAME.apk"; cp "$APK" "$OUT"; echo "APK: $OUT ($(du -h "$OUT" | cut -f1))"

# 5. Record and publish (each step asks).
if ask "Commit the version bump and tag v$NAME?"; then
  git commit -q -am "Release $NAME (versionCode $NEW_CODE)"; git tag -a "v$NAME" -m "Corn $NAME"
  if ask "Push the commit and tag to GitHub?"; then git push -q && git push -q origin "v$NAME"; fi
  if ask "Create a GitHub pre-release v$NAME with the APK attached (public)?"; then
    gh release create "v$NAME" "$OUT#Corn-$NAME.apk" --prerelease --title "Corn $NAME (beta)" --notes "Beta build of Corn, the private browser. Website: https://cornbrowser.xyz/

**Install on Android:** open this page on your phone, tap Corn-$NAME.apk, allow installs from your browser when asked, then tap Install.
**Updating:** install a newer release the same way; your Vault and settings are kept. To be told about new versions, use Obtainium (add github.com/ayush5144/corn).
More: docs/03-planning/05-releasing-and-updates.md"
  fi
else
  git checkout -- "$GRADLE"; echo "Version change undone (nothing committed)."
fi
