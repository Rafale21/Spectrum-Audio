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
import java.nio.file.*;
public class FormatCheck {
    static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        require(RecordingFormat.DEFAULT==RecordingFormat.OPUS,"default format is Opus");
        require(RecordingFormat.extension(2).equals(".ogg")&&RecordingFormat.extension(1).equals(".m4a")&&RecordingFormat.extension(0).equals(".wav"),"stable format identifiers");
        require(RecordingFormat.mime(new File("take.OGG")).equals("audio/ogg")&&RecordingFormat.mime(new File("take.opus")).equals("audio/ogg"),"correct OGG MIME type");
        for(String name:new String[]{"take.ogg","old.wav","old.m4a","take.opus","take.draft"})require(RecordingFormat.saved(name),"recording listing includes "+name);
        require(!RecordingFormat.saved("take.ogg.part"),"unfinished export not listed");
        StartupDefaults old=new StartupDefaults(2048,12000,0,false);require(old.fft==1024&&old.hz==16000&&old.format==2,"one-time upgrade of old defaults");
        StartupDefaults custom=new StartupDefaults(8192,24000,1,false);require(custom.fft==8192&&custom.hz==24000&&custom.format==2,"custom spectrum retained while selecting requested Opus");
        for(int format=0;format<=2;format++){StartupDefaults saved=new StartupDefaults(512,8000,format,true);require(saved.fft==512&&saved.hz==8000&&saved.format==format,"subsequent user settings preserved");}
        File invalid=File.createTempFile("invalid-opus",".ogg");try{Files.write(invalid.toPath(),new byte[256]);try{RecordingFormat.verifyOgg(invalid);throw new AssertionError("fake ogg accepted");}catch(IOException expected){}byte[] fake=new byte[256];System.arraycopy("OggS".getBytes("US-ASCII"),0,fake,0,4);Files.write(invalid.toPath(),fake);try{RecordingFormat.verifyOgg(invalid);throw new AssertionError("missing OpusHead accepted");}catch(IOException expected){}}finally{invalid.delete();}
        for(String file:args)RecordingFormat.verifyOgg(new File(file));
        System.out.println("PASS: real OGG/Opus header guard, recording MIME/listing, defaults migration and preserved later choices.");
    }
}
