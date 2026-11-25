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
public class OrientationCheck {
    public static void main(String[] args){
        int mode=OrientationMode.AUTO;
        for(int cycle=0;cycle<4;cycle++){mode=OrientationMode.next(mode);if(mode!=OrientationMode.LANDSCAPE)throw new AssertionError("auto to landscape");mode=OrientationMode.next(mode);if(mode!=OrientationMode.PORTRAIT)throw new AssertionError("landscape to portrait");mode=OrientationMode.next(mode);if(mode!=OrientationMode.AUTO)throw new AssertionError("portrait to auto");}
        if(OrientationMode.valid(-1)!=0||OrientationMode.valid(50)!=0||OrientationMode.valid(2)!=2)throw new AssertionError("restored state");
        if(!OrientationMode.label(0).equals("Auto")||!OrientationMode.label(1).equals("Paysage forcé")||!OrientationMode.label(2).equals("Portrait forcé"))throw new AssertionError("labels");
        System.out.println("PASS: orientation cycles auto/landscape/portrait, restored values and labels.");
    }
}
