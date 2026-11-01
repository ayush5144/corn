#!/usr/bin/env bash
# Build Corn's Rust core for Android and generate its Kotlin bindings.
# Called automatically by Gradle (preBuild) and safe to run by hand from the repo root.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="$HOME/.cargo/bin:$PATH"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  ANDROID_NDK_HOME="$(ls -d "$SDK"/ndk/* 2>/dev/null | sort -V | tail -1)"
  export ANDROID_NDK_HOME
fi
[ -d "$ANDROID_NDK_HOME" ] || { echo "Android NDK not found under $SDK/ndk" >&2; exit 1; }

OUT="$ROOT/android/app/build/generated/uniffi"
mkdir -p "$OUT/jniLibs" "$OUT/kotlin"

cd "$ROOT/core"
cargo ndk -t arm64-v8a -t x86_64 -P 26 -o "$OUT/jniLibs" build --release -p corn-ffi
cargo run -q --release -p corn-ffi --bin uniffi-bindgen -- generate \
  --library "$OUT/jniLibs/arm64-v8a/libcorn_ffi.so" \
  --language kotlin --out-dir "$OUT/kotlin" --no-format
echo "Rust core built: $OUT"
