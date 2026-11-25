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
final class OrientationMode {
    static final int AUTO=0,LANDSCAPE=1,PORTRAIT=2;
    static int valid(int mode){return mode>=AUTO&&mode<=PORTRAIT?mode:AUTO;}
    static int next(int mode){return (valid(mode)+1)%3;}
    static String label(int mode){return mode==LANDSCAPE?"Paysage forcé":mode==PORTRAIT?"Portrait forcé":"Auto";}
}
