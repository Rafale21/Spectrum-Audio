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

/** Playback pins the cursor; pausing starts a fresh user inactivity deadline. */
final class CursorVisibility {
    static final long DELAY_MS=1500;
    private long lastActivity;private boolean active,touching,playing;
    void show(long now){lastActivity=now;active=true;}
    void touch(boolean down,long now){touching=down;show(now);}
    boolean playback(boolean value,long now){if(playing==value)return false;playing=value;show(now);return true;}
    boolean playing(){return playing;}
    boolean visible(long now){return active&&(playing||touching||now-lastActivity<DELAY_MS);}
    void clear(){active=false;touching=false;playing=false;}
}
