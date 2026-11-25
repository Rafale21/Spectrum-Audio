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
import java.util.Locale;

/** Stable preference codes, independent of display names and array positions. */
final class LanguageChoice {
    static final String[] CODES={"auto","fr","en","es","de","it","pt","ru","ja","tlh"};
    static int index(String code){for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;return 0;}
    static String resolve(String preference,Locale phone){
        String code=CODES[index(preference)];
        if(code.equals("auto"))code=phone==null?"en":phone.getLanguage();
        return index(code)>0?code:"en";
    }
}
