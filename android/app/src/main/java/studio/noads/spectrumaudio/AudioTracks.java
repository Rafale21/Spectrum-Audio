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
import java.io.IOException;
final class AudioTracks {
    interface Provider<T>{int count();T format(int index);String mime(T format);}
    static final class Selected<T>{final int index;final T format;Selected(int i,T f){index=i;format=f;}}
    static <T> Selected<T> select(Provider<T> source)throws IOException {
        StringBuilder failures=new StringBuilder();
        for(int i=0;i<source.count();i++)try{T format=source.format(i);String mime=source.mime(format);if(mime!=null&&mime.startsWith("audio/"))return new Selected<T>(i,format);}catch(RuntimeException error){if(failures.length()<500)failures.append("Piste ").append(i).append(" : ").append(AudioBuffers.error(error)).append(". ");}
        throw new IOException("Aucune piste audio lisible. "+failures);
    }
}
