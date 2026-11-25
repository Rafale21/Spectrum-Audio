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
import java.io.*;
import java.util.*;
/** Writes the same transforms used by the Android renderer for the C desktop reference. */
public class DesktopParity {
    public static void main(String[] args){
        Locale.setDefault(Locale.ROOT);
        for(int size:new int[]{512,1024,2048,4096,8192})for(int method=0;method<3;method++){
            short[] pcm=new short[size];for(int i=0;i<size;i++)pcm[i]=(short)((i*7919L%26001)-13000);
            double[] values=new Spectrum.Analyzer(size,method).analyze(pcm);
            for(int bin=0;bin<values.length;bin++)System.out.printf("%d %d %d %.17g%n",size,method,bin,values[bin]*values[bin]);
        }
    }
}
