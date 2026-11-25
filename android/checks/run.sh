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
cd "$(dirname "$0")/.."
mkdir -p checks/classes
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/Spectrum.java app/src/main/java/studio/noads/spectrumaudio/WavFile.java checks/AudioCheck.java
java -cp checks/classes AudioCheck

if [ -n "${ANDROID_HOME:-}" ]; then
 javac -cp "$ANDROID_HOME/platforms/android-35/android.jar" -d checks/classes app/src/main/java/studio/noads/spectrumaudio/{FileAnalysis,Spectrum,AudioBuffers,AudioTracks,Mp4Aac,RecordingStore,RecordingFormat,WavFile}.java checks/{ImportCheck,RecordingCheck}.java
 java -cp "checks/classes:$ANDROID_HOME/platforms/android-35/android.jar" studio.noads.spectrumaudio.ImportCheck
 java -cp "checks/classes:$ANDROID_HOME/platforms/android-35/android.jar" studio.noads.spectrumaudio.RecordingCheck
fi

javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/{Mp4Aac,AudioTracks,AudioBuffers}.java checks/Mp4Check.java
if command -v ffmpeg >/dev/null && command -v ffprobe >/dev/null; then python3 checks/check-mp4.py; fi
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/ViewMath.java checks/ViewCheck.java
java -cp checks/classes studio.noads.spectrumaudio.ViewCheck
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/{AdaptiveSpectrum,Spectrum,ColorMap}.java checks/AdaptiveCheck.java
java -cp checks/classes studio.noads.spectrumaudio.AdaptiveCheck
javac -cp checks/classes -d checks/classes checks/reference/LegacyAdaptiveSpectrum.java checks/SpatialDetailCheck.java checks/DesktopParity.java
java -cp checks/classes studio.noads.spectrumaudio.SpatialDetailCheck "${SPECTRUM_CHECK_OUTPUT:-checks/detail-output}"
if command -v cc >/dev/null; then python3 checks/check-desktop.py; fi
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/OrientationMode.java checks/OrientationCheck.java
java -cp checks/classes studio.noads.spectrumaudio.OrientationCheck
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/LanguageChoice.java checks/LanguageCheck.java
java -cp checks/classes studio.noads.spectrumaudio.LanguageCheck
python3 checks/check-languages.py
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/CursorVisibility.java checks/CursorCheck.java
java -cp checks/classes studio.noads.spectrumaudio.CursorCheck
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/SpectrumPreset.java checks/PresetCheck.java
java -cp checks/classes studio.noads.spectrumaudio.PresetCheck

javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/{LiveColumns,RecordingFormat,StartupDefaults}.java checks/{LiveCheck,FormatCheck}.java
java -cp checks/classes studio.noads.spectrumaudio.LiveCheck
java -cp checks/classes studio.noads.spectrumaudio.FormatCheck
if command -v ffmpeg >/dev/null && command -v ffprobe >/dev/null; then python3 checks/check-opus.py; fi
javac -d checks/classes app/src/main/java/studio/noads/spectrumaudio/Spectrum.java checks/PerformanceCheck.java
java -cp checks/classes studio.noads.spectrumaudio.PerformanceCheck
