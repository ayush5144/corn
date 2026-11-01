#!/usr/bin/env bash
# Build-time assets for the Android app. Run by Gradle before every build; safe to run by hand.
#   1. Offline starter list: today's EasyList + EasyPrivacy compiled into engines, so a fresh
#      install blocks ads even before its first download. Refreshed at most once a day.
#   2. Cookie-banner closer: DuckDuckGo autoconsent (MPL-2.0), unmodified, pinned version.
# Nothing here is committed: outputs go to android/app/build/generated/starter/assets.
# If the network is down, the build continues without the missing piece.
set -uo pipefail

AUTOCONSENT_VERSION="17.2.0"   # bump deliberately; see docs/04-reference/02-open-source.md

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="$HOME/.cargo/bin:$PATH"
OUT="$ROOT/android/app/build/generated/starter/assets"
SRC="$ROOT/android/app/build/starter-src"
mkdir -p "$OUT/starter" "$OUT/autoconsent" "$SRC"

fresh() { [ -f "$1" ] && [ -n "$(find "$1" -mtime -1 2>/dev/null)" ]; }

# 1. Starter engines
if fresh "$OUT/starter/ads.dat" && fresh "$OUT/starter/trackers.dat"; then
  echo "starter lists: up to date"
else
  if curl -sfL --max-time 60 -o "$SRC/easylist.txt" https://easylist.to/easylist/easylist.txt &&
     curl -sfL --max-time 60 -o "$SRC/easyprivacy.txt" https://easylist.to/easylist/easyprivacy.txt; then
    (cd "$ROOT/core" &&
      cargo run -q --release -p corn-block --bin corn-compile -- "$OUT/starter/ads.dat" "$SRC/easylist.txt" &&
      cargo run -q --release -p corn-block --bin corn-compile -- "$OUT/starter/trackers.dat" "$SRC/easyprivacy.txt") ||
      echo "warning: starter lists not compiled" >&2
  else
    echo "warning: could not download filter lists; building without a starter list" >&2
  fi
fi

# 2. autoconsent
if [ -f "$OUT/autoconsent/autoconsent-$AUTOCONSENT_VERSION.js" ]; then
  echo "autoconsent $AUTOCONSENT_VERSION: present"
else
  TMP="$SRC/autoconsent"; rm -rf "$TMP"; mkdir -p "$TMP"
  if curl -sfL --max-time 60 -o "$TMP/ac.tgz" "https://registry.npmjs.org/@duckduckgo/autoconsent/-/autoconsent-$AUTOCONSENT_VERSION.tgz" &&
     tar xzf "$TMP/ac.tgz" -C "$TMP"; then
    rm -f "$OUT/autoconsent/"autoconsent-*.js
    cp "$TMP/package/dist/autoconsent.standalone.js" "$OUT/autoconsent/autoconsent-$AUTOCONSENT_VERSION.js"
    cp "$TMP/package/LICENSE" "$OUT/autoconsent/LICENSE-autoconsent.txt"
    echo "autoconsent $AUTOCONSENT_VERSION: fetched"
  else
    echo "warning: could not fetch autoconsent; building without the cookie-banner closer" >&2
  fi
fi
exit 0
