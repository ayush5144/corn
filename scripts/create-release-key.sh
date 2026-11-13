#!/usr/bin/env bash
# Create Corn's release signing key, once. You only type a password (twice).
# The certificate details are filled in for you; they're public (anyone can read them from the APK).
# Never commit the key or put it in this repo (the repo is public). Back up the file AND the password:
# if either is lost, installs from outside the Play Store can never be updated again.
# Explained in docs/03-planning/05-releasing-and-updates.md.
set -euo pipefail
KEY="${1:-$HOME/corn-release.jks}"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
KEYTOOL="$JAVA_HOME/bin/keytool"
[ -x "$KEYTOOL" ] || KEYTOOL="$(command -v keytool || true)"
[ -n "$KEYTOOL" ] || { echo "keytool not found (it comes with Android Studio's Java)." >&2; exit 1; }
if [ -e "$KEY" ]; then echo "A key already exists at $KEY. Not overwriting it." >&2; exit 1; fi
case "$KEY" in "$(cd "$(dirname "$0")/.." && pwd)"/*) echo "Refusing to create the key inside the repo." >&2; exit 1;; esac
echo "Creating $KEY (alias: corn, RSA 4096, valid ~27 years)."
echo "Choose a long password (20+ characters, or 5-6 random words). Save it in your password manager."
"$KEYTOOL" -genkeypair -v -keystore "$KEY" -storetype PKCS12 -alias corn -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Corn, O=Corn, C=IN"
chmod 600 "$KEY"
echo
echo "Done: $KEY"
echo "Next: put the same password in ~/.gradle/gradle.properties (CORN_RELEASE_STORE_PASSWORD and CORN_RELEASE_KEY_PASSWORD)."
echo "Then back up $KEY somewhere off this Mac (encrypted)."
