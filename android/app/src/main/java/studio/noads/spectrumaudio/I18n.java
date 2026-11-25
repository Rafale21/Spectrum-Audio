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
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

/** Offline UI catalog. Atomic snapshots also serve import/save worker threads. */
final class I18n {
    static final String[] CODES=LanguageChoice.CODES;
    static final String[] NAMES={"Auto","Français","English","Español","Deutsch","Italiano","Português","Русский","日本語","tlhIngan Hol"};
    private static final class Snapshot {
        final Map<String,String> strings;final Locale locale;
        Snapshot(Map<String,String> strings,Locale locale){this.strings=Collections.unmodifiableMap(strings);this.locale=locale;}
    }
    private static volatile Snapshot current=new Snapshot(Collections.emptyMap(),Locale.FRENCH);
    private static JSONObject catalog;
    static int index(String code){return LanguageChoice.index(code);}
    static synchronized void load(Context context,String preference){
        String code=LanguageChoice.resolve(preference,Locale.getDefault());
        Map<String,String> strings=new HashMap<>();
        try {
            if(!code.equals("fr")){
                if(catalog==null){try(InputStream input=context.getAssets().open("languages.json");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
                    catalog=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                }}
                JSONObject language=catalog.getJSONObject(code);Iterator<String> keys=language.keys();while(keys.hasNext()){String key=keys.next();strings.put(key,language.getString(key));}
            }
        }catch(Exception error){android.util.Log.e("SpectreI18n","Unable to load packaged language catalog",error);}
        current=new Snapshot(strings,Locale.forLanguageTag(code.equals("tlh")?"en":code));
    }
    static String t(String french){Snapshot snapshot=current;String translated=snapshot.strings.get(french);return translated==null?french:translated;}
    static Locale locale(){return current.locale;}
    /** Localize progress only. Decoder selection and technical diagnostics stay intact. */
    static String progress(String message){
        if(message.startsWith("Décodage "))return t("Décodage ")+message.substring("Décodage ".length()).replace(" · essai ",t(" · essai "));
        if(message.startsWith("Analyse : "))return t("Analyse : ")+message.substring("Analyse : ".length());
        return t(message);
    }
}
