/*
This file is part of Spectrum Audio.
Copyright (C) 2026 No Ads Studio and contributors.
SPDX-License-Identifier: GPL-3.0-only

Spectrum Audio is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, version 3 only.
Spectrum Audio is distributed WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License in the repository root LICENSE file.
*/
package studio.noads.spectrumaudio;

/** One-time upgrade: retain custom spectral settings and all saved personal presets. */
final class StartupDefaults {
    final int fft,hz,format;
    StartupDefaults(int previousFft,int previousHz,int previousFormat,boolean migrated){
        boolean oldSpectrum=!migrated&&previousFft==2048&&previousHz==12000;
        fft=oldSpectrum?1024:previousFft;hz=oldSpectrum?16000:previousHz;
        format=migrated?RecordingFormat.valid(previousFormat):RecordingFormat.DEFAULT;
    }
}
