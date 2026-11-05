#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#
# Run Corn's emulator QA checks against the installed debug build. See qa/README.md.
#
#   scripts/qa-run.sh                 run every check
#   scripts/qa-run.sh NAME [NAME…]    run the named checks (a prefix like "popups-" works too)
#   scripts/qa-run.sh --list          list the checks
#
# Prints one PASS/FAIL line per check with what was measured, and exits non-zero on any FAIL.
# Settings: QA_PORT (default 8770), QA_CDP_PORT (default 9334), QA_NEW_TABS=1 (open every
# page through Android in a new tab), QA_DEBUG=1 (tracebacks), ADB, ANDROID_HOME.
# It installs nothing and changes no settings; it does open a few tabs (Flush clears them).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ADB="${ADB:-$SDK/platform-tools/adb}"
PORT="${QA_PORT:-8770}"
export QA_CDP_PORT="${QA_CDP_PORT:-9334}"
STATE="${TMPDIR:-/tmp}/corn-dev"

if [ "${1:-}" = "--list" ]; then exec python3 -I "$ROOT/scripts/qa/run.py" --list; fi
if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then sed -n '6,15p' "$0"; exit 0; fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT is already in use. Pick another with QA_PORT=…" >&2
  exit 2
fi

cleanup() {
  if [ -f "$STATE/server-$PORT.pid" ]; then
    kill "$(cat "$STATE/server-$PORT.pid")" 2>/dev/null || true
    rm -f "$STATE/server-$PORT.pid"
  fi
  "$ADB" forward --remove "tcp:$QA_CDP_PORT" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Test server for qa/pages (and the emulator, if none is running).
"$ROOT/scripts/dev-start.sh" --serve "$ROOT/qa/pages" --port "$PORT" >/dev/null
for _ in $(seq 1 20); do
  curl -fs "http://127.0.0.1:$PORT/hit.html" >/dev/null 2>&1 && break
  sleep 0.25
done

if ! "$ADB" shell pm path app.cornbrowser >/dev/null 2>&1; then
  echo "Corn (app.cornbrowser) isn't installed. Install a debug build first: scripts/dev-start.sh --install" >&2
  exit 2
fi

export QA_BASE="http://10.0.2.2:$PORT"
export QA_SERVER_LOG="$STATE/server-$PORT.log"
export QA_RUN="$(date +%H%M%S)"
echo "Corn QA run $QA_RUN on $("$ADB" shell getprop ro.product.model | tr -d '\r'), Android $("$ADB" shell getprop ro.build.version.release | tr -d '\r'), Corn $("$ADB" shell dumpsys package app.cornbrowser | sed -n 's/.*versionName=//p' | head -1 | tr -d '\r')"
echo

set +e
python3 -I "$ROOT/scripts/qa/run.py" "$@"
status=$?
set -e

# Leave Corn in front (the app-link checks visit other apps).
"$ADB" shell am start -n app.cornbrowser/.MainActivity >/dev/null 2>&1 || true
exit $status
