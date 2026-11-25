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
public class CursorCheck {
    static void expect(boolean actual,boolean expected){if(actual!=expected)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        CursorVisibility cursor=new CursorVisibility();expect(cursor.visible(0),false);
        cursor.show(100);expect(cursor.visible(100),true);expect(cursor.visible(1599),true);expect(cursor.visible(1600),false);
        // Position/drawing reads while paused must never restart the timeout.
        for(int now=1600;now<5000;now+=300)expect(cursor.visible(now),false);
        cursor.show(5000);expect(cursor.visible(6499),true);expect(cursor.visible(6500),false);
        cursor.touch(true,7000);expect(cursor.visible(12000),true);cursor.touch(false,12000);expect(cursor.visible(13499),true);expect(cursor.visible(13500),false);
        cursor.show(14000);cursor.show(15000);expect(cursor.visible(16499),true);expect(cursor.visible(16500),false);
        cursor.touch(true,17000);cursor.clear();expect(cursor.visible(17001),false);expect(cursor.visible(20000),false);
        // Starting after inactivity restores the cursor for the whole playback.
        cursor.playback(true,21000);expect(cursor.visible(21000),true);expect(cursor.visible(600000),true);
        cursor.show(600000);expect(cursor.visible(900000),true);
        // Repeated paused refreshes must not keep the cursor alive indefinitely.
        cursor.playback(false,900000);expect(cursor.visible(901499),true);
        for(int now=900300;now<=901500;now+=300)cursor.playback(false,now);
        expect(cursor.visible(901500),false);
        cursor.playback(true,902000);cursor.touch(true,902100);cursor.touch(false,902200);expect(cursor.visible(910000),true);
        cursor.playback(false,910000);cursor.playback(true,910100);expect(cursor.visible(999999),true);
        cursor.clear();expect(cursor.playing(),false);expect(cursor.visible(1000000),false);
        System.out.println("PASS: cursor pinned throughout playback; pause/completion starts 1500ms timeout; paused ticks do not extend it; interactions, touch hold, resume and reset.");
    }
}
