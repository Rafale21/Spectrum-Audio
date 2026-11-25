#!/usr/bin/env bash
# This file is part of Spectrum Audio.
# Copyright (C) 2026 No Ads Studio and contributors.
# SPDX-License-Identifier: GPL-3.0-only
#
# Spectrum Audio is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, version 3 only.
# Spectrum Audio is distributed WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License in the repository root LICENSE file.

# Build with installed SDK tools, without resolving the Gradle plugin.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK directory}"
BUILD_TOOLS="${ANDROID_HOME}/build-tools/35.0.0"
PLATFORM="${ANDROID_HOME}/platforms/android-35/android.jar"
JDK_BIN="${JAVA_HOME:+${JAVA_HOME}/bin/}"
OUT="$PWD/manual-build"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/gen"
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/resources.zip"
sed 's/<manifest /<manifest package="studio.noads.spectrumaudio" /' app/src/main/AndroidManifest.xml > "$OUT/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2" link -o "$OUT/base.apk" -I "$PLATFORM" --manifest "$OUT/AndroidManifest.xml" --java "$OUT/gen" -A app/src/main/assets --min-sdk-version 26 --target-sdk-version 35 --version-code 22 --version-name 1.21 "$OUT/resources.zip"
"${JDK_BIN}javac" -encoding UTF-8 -source 17 -target 17 -classpath "$PLATFORM" -d "$OUT/classes" app/src/main/java/studio/noads/spectrumaudio/*.java
"${JDK_BIN}jar" cf "$OUT/classes.jar" -C "$OUT/classes" .
"$BUILD_TOOLS/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" "$OUT/classes.jar"
cp "$OUT/base.apk" "$OUT/with-dex.apk"
(cd "$OUT/dex" && zip -q "$OUT/with-dex.apk" classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/with-dex.apk" "$OUT/aligned.apk"
if [ -n "${SPECTRUM_SIGNING_KEY:-}" ] || [ -n "${SPECTRUM_SIGNING_PASSWORD_FILE:-}" ]; then
 : "${SPECTRUM_SIGNING_KEY:?Provide the private release keystore path}"
 : "${SPECTRUM_SIGNING_PASSWORD_FILE:?Provide the private password file path}"
 "$BUILD_TOOLS/apksigner" sign --v4-signing-enabled false --ks "$SPECTRUM_SIGNING_KEY" --ks-key-alias spectrum-audio --ks-pass "file:$SPECTRUM_SIGNING_PASSWORD_FILE" --out "$OUT/SpectrumAudio.apk" "$OUT/aligned.apk"
else
if [ ! -f development.keystore ]; then
 "${JDK_BIN}keytool" -genkeypair -keystore development.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Spectrum Audio Development' -keyalg RSA -keysize 2048 -validity 10000
fi
"$BUILD_TOOLS/apksigner" sign --v4-signing-enabled false --ks development.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$OUT/SpectrumAudio.apk" "$OUT/aligned.apk"
fi
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT/SpectrumAudio.apk"
printf 'APK: %s\n' "$OUT/SpectrumAudio.apk"
