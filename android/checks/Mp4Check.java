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
import java.nio.*;
public class Mp4Check {
    public static void main(String[] args)throws Exception {
        AudioTracks.Selected<String> selected=AudioTracks.select(new AudioTracks.Provider<String>(){public int count(){return 3;}public String format(int i){if(i==0)throw new IllegalArgumentException();return i==1?"video/avc":"audio/mp4a-latm";}public String mime(String f){return f;}});
        if(selected.index!=2)throw new AssertionError("bad metadata/video tracks must be skipped");
        try{AudioTracks.select(new AudioTracks.Provider<String>(){public int count(){return 1;}public String format(int i){throw new IllegalArgumentException();}public String mime(String f){return f;}});throw new AssertionError("missing audio accepted");}catch(IOException expected){if(!expected.getMessage().contains("IllegalArgumentException"))throw new AssertionError("diagnostic lost");}
        try(Mp4Aac aac=Mp4Aac.open(new File(args[0]));OutputStream output=new FileOutputStream(args[1])){
            if(aac.rate!=44100||aac.channels!=1||aac.config.length<2)throw new AssertionError("AAC format");
            // Export packets as ADTS for a reference decoder; verify the resulting PCM externally.
            int objectType=(aac.config[0]&255)>>3;int frequency=((aac.config[0]&7)<<1)|((aac.config[1]&255)>>7),channels=((aac.config[1]&255)>>3)&15;
            if(objectType!=2||frequency!=4||channels!=1)throw new AssertionError("fixture ASC");
            long ticks=0;
            for(int i=0;i<aac.sizes.length;i++){int n=aac.sizes[i],frame=n+7;ByteBuffer buffer=ByteBuffer.allocate(n);aac.read(i,buffer);byte[] header={(byte)255,(byte)241,(byte)(((objectType-1)<<6)|(frequency<<2)|(channels>>2)),(byte)(((channels&3)<<6)|(frame>>11)),(byte)(frame>>3),(byte)(((frame&7)<<5)|31),(byte)252};output.write(header);output.write(buffer.array());System.out.println(aac.offsets[i]+","+n+","+ticks);ticks+=aac.durations[i];}
        }
    }
}
