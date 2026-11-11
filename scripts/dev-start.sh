#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#
# Start Corn's local development setup. Stop everything with scripts/dev-stop.sh.
#
#   scripts/dev-start.sh                      start the Android emulator only
#   scripts/dev-start.sh --install            ...and build + install the debug app
#   scripts/dev-start.sh --serve DIR          ...and serve DIR at http://10.0.2.2:8765 (test pages)
#   scripts/dev-start.sh --install --serve DIR --port 8080
#
# Settings (environment variables): FLUSH_AVD (default corn_pixel), ANDROID_HOME, JAVA_HOME.
# Test pages are never kept in this repo; point --serve at a folder outside it.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
AVD="${FLUSH_AVD:-corn_pixel}"
STATE="${TMPDIR:-/tmp}/flush-dev"   # pid files and logs, so dev-stop.sh knows what to stop
mkdir -p "$STATE"

INSTALL=0
SERVE_DIR=""
PORT=8765
while [ $# -gt 0 ]; do
  case "$1" in
    --install) INSTALL=1 ;;
    --serve) SERVE_DIR="${2:?--serve needs a folder}"; shift ;;
    --port) PORT="${2:?--port needs a number}"; shift ;;
    -h|--help) sed -n '6,15p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1 (try --help)" >&2; exit 1 ;;
  esac
  shift
done

# 1. Emulator (skipped if one is already running)
if "$ADB" devices 2>/dev/null | grep -q '^emulator-'; then
  echo "Emulator already running."
else
  echo "Starting emulator '$AVD' (4 GB)…"
  nohup "$SDK/emulator/emulator" -avd "$AVD" -no-snapshot-save -no-boot-anim -memory 4096 \
    > "$STATE/emulator.log" 2>&1 &
  echo $! > "$STATE/emulator.pid"
  "$ADB" wait-for-device
  for _ in $(seq 1 90); do   # up to 3 minutes
    [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 2
  done
  echo "Emulator ready."
fi

# 2. Build and install the debug app
if [ "$INSTALL" = 1 ]; then
  export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
  export ANDROID_HOME="$SDK"
  echo "Building and installing the debug app…"
  (cd "$ROOT/android" && ./gradlew -q :app:assembleDebug)
  "$ADB" install -r "$ROOT/android/app/build/outputs/apk/debug/app-debug.apk" | tail -1
fi

# 3. Local test server (pages reachable from the emulator at http://10.0.2.2:PORT)
if [ -n "$SERVE_DIR" ]; then
  [ -d "$SERVE_DIR" ] || { echo "No such folder: $SERVE_DIR" >&2; exit 1; }
  nohup python3 -I -m http.server "$PORT" --bind 0.0.0.0 --directory "$SERVE_DIR" \
    > "$STATE/server-$PORT.log" 2>&1 &
  echo $! > "$STATE/server-$PORT.pid"
  echo "Serving $SERVE_DIR at http://10.0.2.2:$PORT (from the emulator). Log: $STATE/server-$PORT.log"
fi

echo "Done. Stop everything with: scripts/dev-stop.sh"
