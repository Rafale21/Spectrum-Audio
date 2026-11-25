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
import java.nio.*;
final class AudioBuffers {
    static ByteBuffer window(ByteBuffer source,int offset,int size)throws IOException {
        if(source==null)throw new IOException("Le décodeur n’a pas fourni de buffer audio.");
        if(offset<0||size<0||(long)offset+size>source.capacity())throw new IOException("Limites du buffer audio invalides : "+offset+" + "+size+" / "+source.capacity());
        // Codec buffers may arrive with a restricted position/limit. BufferInfo uses capacity offsets.
        ByteBuffer data=source.duplicate();data.clear();data.position(offset);data.limit(offset+size);
        return data.slice().order(ByteOrder.nativeOrder());
    }
    static String error(Throwable error){
        String message=error.getMessage();String result=error.getClass().getSimpleName();
        if(message!=null&&!message.trim().isEmpty()&&!"null".equals(message))result+=" : "+message;
        for(StackTraceElement frame:error.getStackTrace())if(frame.getClassName().startsWith("studio.noads.spectrumaudio.")){result+=" ["+frame.getFileName()+":"+frame.getLineNumber()+"]";break;}
        return result;
    }
}
