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
public class LanguageCheck {
    private static void equal(String a,String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        for(String code:LanguageChoice.CODES){if(!code.equals("auto")){equal(LanguageChoice.resolve(code,Locale.JAPANESE),code);equal(LanguageChoice.CODES[LanguageChoice.index(code)],code);}}
        equal(LanguageChoice.resolve("auto",Locale.FRANCE),"fr");
        equal(LanguageChoice.resolve("auto",Locale.UK),"en");
        equal(LanguageChoice.resolve("auto",Locale.forLanguageTag("pt-BR")),"pt");
        equal(LanguageChoice.resolve("auto",Locale.forLanguageTag("es-MX")),"es");
        equal(LanguageChoice.resolve("auto",Locale.forLanguageTag("zh-CN")),"en");
        equal(LanguageChoice.resolve("missing",Locale.GERMAN),"de");
        equal(LanguageChoice.resolve(null,null),"en");
        equal(LanguageChoice.resolve("tlh",Locale.FRENCH),"tlh");
        System.out.println("PASS: manual language overrides phone, region fallback, unsupported and invalid preferences, Klingon.");
    }
}
