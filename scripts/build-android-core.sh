#!/usr/bin/env bash
# Build Flush's Rust core for Android and generate its Kotlin bindings.
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
cargo ndk -t arm64-v8a -t x86_64 -P 26 -o "$OUT/jniLibs" build --release -p flush-ffi
# Release .so files are stripped, so read the bindings metadata from an unstripped host build.
cargo build -q -p flush-ffi --lib
HOST_LIB=""
for f in target/debug/libflush_ffi.dylib target/debug/libflush_ffi.so; do [ -f "$f" ] && HOST_LIB="$f" && break; done
[ -n "$HOST_LIB" ] || { echo "host build of flush-ffi not found" >&2; exit 1; }
cargo run -q -p flush-ffi --bin uniffi-bindgen -- generate \
  --library "$HOST_LIB" \
  --language kotlin --out-dir "$OUT/kotlin" --no-format
echo "Rust core built: $OUT"
