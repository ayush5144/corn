#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#
# Stop everything Flush's development uses on this Mac, to free memory:
#   - the Android emulator(s)
#   - test servers started by scripts/dev-start.sh
#   - Gradle and Kotlin build daemons (they stay in memory after a build)
#   - Android's adb helper
# Touches only these; your other apps are left alone.
#
#   scripts/dev-stop.sh

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
STATE="${TMPDIR:-/tmp}/flush-dev"

# 1. Emulators: ask each one to shut down, then make sure the process is gone
if [ -x "$ADB" ]; then
  for serial in $("$ADB" devices 2>/dev/null | awk '/^emulator-/{print $1}'); do
    echo "Stopping $serial…"
    "$ADB" -s "$serial" emu kill >/dev/null 2>&1 || true
  done
fi
sleep 2
pkill -f "qemu-system.*-avd" 2>/dev/null && echo "Emulator process ended." || true

# 2. Servers and anything else dev-start.sh recorded
for pidfile in "$STATE"/*.pid; do
  [ -e "$pidfile" ] || continue
  kill "$(cat "$pidfile")" 2>/dev/null && echo "Stopped $(basename "$pidfile" .pid)."
  rm -f "$pidfile"
done

# 3. Build daemons
if [ -x "$ROOT/android/gradlew" ]; then
  export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
  (cd "$ROOT/android" && ./gradlew --stop -q >/dev/null 2>&1) && echo "Gradle daemons stopped."
fi
pkill -f KotlinCompileDaemon 2>/dev/null && echo "Kotlin daemon stopped." || true

# 4. adb helper
[ -x "$ADB" ] && "$ADB" kill-server >/dev/null 2>&1 && echo "adb stopped."

# What's left (should be nothing)
left="$(ps axo pid,command | grep -E "qemu-system|GradleDaemon|KotlinCompileDaemon|adb .*fork-server" | grep -v grep)"
if [ -n "$left" ]; then echo "Still running:"; echo "$left"; else echo "All Flush development processes are stopped."; fi
