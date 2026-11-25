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
import java.nio.file.*;
public class RecordingCheck {
    private static void require(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        // Regression: a vendor buffer can have a small current limit and a non-zero BufferInfo offset.
        ByteBuffer original=ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());original.putShort(12,(short)1000);original.putShort(14,(short)-1000);original.limit(4);
        ByteBuffer frame=AudioBuffers.window(original,12,4);require(frame.remaining()==4&&frame.getShort()==1000&&frame.getShort()==-1000,"restricted output buffer window");require(original.limit()==4&&original.position()==0,"source not mutated");
        require(AudioBuffers.window(original,32,0).remaining()==0,"empty EOS window");
        try{AudioBuffers.window(original,31,2);throw new AssertionError("invalid range accepted");}catch(IOException expected){}
        try{AudioBuffers.window(null,0,0);throw new AssertionError("null buffer accepted");}catch(IOException expected){}
        require(!AudioBuffers.error(new BufferUnderflowException()).contains("null"),"non-null diagnostic");
        Path directory=Files.createTempDirectory("spectre-save-");
        try{
            File draft=directory.resolve("take.draft").toFile();byte[] data={0, (byte)128, 0,0,(byte)255,127};
            try(OutputStream out=new FileOutputStream(draft)){out.write(new byte[44]);out.write(data);out.write(1);}
            File saved=RecordingStore.finish(draft,directory.toFile(),"take",0);require(saved.isFile()&&!draft.exists(),"automatic finalization");require(saved.length()==50,"odd tail trimmed");
            try(RandomAccessFile in=new RandomAccessFile(saved,"r")){require(in.readInt()==0x52494646,"draft header recovered");in.seek(40);require(Integer.reverseBytes(in.readInt())==6,"recovered PCM length");byte[] actual=new byte[6];in.readFully(actual);require(java.util.Arrays.equals(actual,data),"PCM preserved");}
            require(!directory.resolve("take.wav.part").toFile().exists(),"part removed");
            File reused=directory.resolve("reused.wav").toFile();short[] chunk=new short[100];for(int i=0;i<chunk.length;i++)chunk[i]=(short)(i*311-15000);
            try(WavFile wav=new WavFile(reused)){for(int size:new int[]{100,3,0,77,1})wav.write(chunk,size);try{wav.write(chunk,101);throw new AssertionError("invalid count accepted");}catch(IOException expected){}}
            require(reused.length()==44+(100+3+77+1)*2,"reused buffer must write only valid count");
            try(RandomAccessFile in=new RandomAccessFile(reused,"r")){in.seek(44);for(int size:new int[]{100,3,0,77,1})for(int i=0;i<size;i++)require(Short.reverseBytes(in.readShort())==chunk[i],"varying PCM block contents");}
            File oggCollision=directory.resolve("collision.ogg").toFile();Files.write(oggCollision.toPath(),new byte[]{1,2,3});File oggDraft=directory.resolve("collision.draft").toFile();Files.write(oggDraft.toPath(),new byte[48]);
            try{RecordingStore.finish(oggDraft,directory.toFile(),"collision",RecordingFormat.OPUS);throw new AssertionError("OGG overwrite allowed");}catch(IOException expected){require(oggDraft.exists()&&oggCollision.length()==3,"OGG collision preserves draft and output");}

            File another=directory.resolve("another.draft").toFile();Files.write(another.toPath(),new byte[48]);
            try{RecordingStore.finish(another,directory.toFile(),"take",0);throw new AssertionError("overwrite allowed");}catch(IOException expected){require(another.exists()&&saved.length()==50,"collision preserves original and draft");}
            File empty=directory.resolve("empty.draft").toFile();Files.write(empty.toPath(),new byte[44]);try{RecordingStore.finish(empty,directory.toFile(),"empty",0);throw new AssertionError("empty recording accepted");}catch(IOException expected){require(empty.exists()&&!directory.resolve("empty.wav").toFile().exists(),"failure preserves draft");}
        }finally{try(java.util.stream.Stream<Path> files=Files.walk(directory)){files.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{Files.delete(path);}catch(IOException ignored){}});}}
        System.out.println("PASS: restricted codec buffers, EOS, diagnostics, automatic WAV finalization, draft recovery, PCM preservation, collision and empty-file handling.");
    }
}
