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

set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?Set ANDROID_HOME}"
BT="$ANDROID_HOME/build-tools/35.0.0"
API="$ANDROID_HOME/platforms/android-35/android.jar"
JDK="${JAVA_HOME:+${JAVA_HOME}/bin/}"
DEST="$PWD/checks/device-build"
mkdir -p "$DEST/classes" "$DEST/dex"
"$BT/aapt2" link -o "$DEST/base.apk" -I "$API" --manifest checks/device/AndroidManifest.xml
"${JDK}javac" -encoding UTF-8 -source 17 -target 17 -cp "$API" -d "$DEST/classes" app/src/main/java/studio/noads/spectrumaudio/{RecordingFormat,RecordingStore,AudioBuffers,WavFile}.java checks/device/AudioSmoke.java
"${JDK}jar" cf "$DEST/classes.jar" -C "$DEST/classes" .
"$BT/d8" --lib "$API" --min-api 26 --output "$DEST/dex" "$DEST/classes.jar"
cp "$DEST/base.apk" "$DEST/with-dex.apk"
(cd "$DEST/dex" && zip -q "$DEST/with-dex.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$DEST/with-dex.apk" "$DEST/aligned.apk"
if [ ! -f "$DEST/checks.keystore" ]; then
 "${JDK}keytool" -genkeypair -keystore "$DEST/checks.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Spectrum Audio Codec Checks' -keyalg RSA -keysize 2048 -validity 10000
fi
"$BT/apksigner" sign --v4-signing-enabled false --ks "$DEST/checks.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$DEST/CodecChecks.apk" "$DEST/aligned.apk"
printf 'Test APK: %s\n' "$DEST/CodecChecks.apk"
