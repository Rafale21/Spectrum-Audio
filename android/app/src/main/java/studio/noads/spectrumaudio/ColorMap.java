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
final class ColorMap {
    private static final int[][] STOPS={{0,0,0},{30,28,92},{32,90,163},{29,191,172},{201,231,93},{255,246,201}};
    static int color(double db,int gain,int range,boolean gray){if(db<=-139.99)return 0xff000000;double value=Math.max(0,Math.min(1,(db+gain+range)/range));if(gray){int level=(int)Math.round(value*255);return 0xff000000|level<<16|level<<8|level;}double q=value*5;int i=Math.min(4,(int)q);double f=q-i;int[] a=STOPS[i],b=STOPS[i+1];return 0xff000000|((int)(a[0]*(1-f)+b[0]*f)<<16)|((int)(a[1]*(1-f)+b[1]*f)<<8)|(int)(a[2]*(1-f)+b[2]*f);}
}
