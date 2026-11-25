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
import java.util.Locale;

/** Stable saved preference IDs; never reorder when adding a format to the menu. */
final class RecordingFormat {
    static final int WAV=0,AAC=1,OPUS=2,DEFAULT=OPUS;
    static int valid(int format){return format>=WAV&&format<=OPUS?format:DEFAULT;}
    static String extension(int format){switch(format){case WAV:return ".wav";case AAC:return ".m4a";case OPUS:return ".ogg";default:throw new IllegalArgumentException("Format audio invalide");}}
    static String mime(File file){String name=file.getName().toLowerCase(Locale.ROOT);return name.endsWith(".ogg")||name.endsWith(".opus")?"audio/ogg":name.endsWith(".m4a")?"audio/mp4":"audio/wav";}
    static boolean saved(String name){name=name.toLowerCase(Locale.ROOT);return name.endsWith(".wav")||name.endsWith(".m4a")||name.endsWith(".ogg")||name.endsWith(".opus")||name.endsWith(".draft");}
    static void verifyOgg(File file)throws IOException {
        byte[] header=new byte[256];int read;try(InputStream in=new FileInputStream(file)){read=in.read(header);}
        if(read<36||header[0]!='O'||header[1]!='g'||header[2]!='g'||header[3]!='S')throw new IOException("Conteneur OGG invalide.");
        byte[] magic={'O','p','u','s','H','e','a','d'};
        for(int offset=27;offset+magic.length<=read;offset++){boolean found=true;for(int i=0;i<magic.length;i++)if(header[offset+i]!=magic[i]){found=false;break;}if(found)return;}
        throw new IOException("En-tête Opus absent du conteneur OGG.");
    }
}
