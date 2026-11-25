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
public class ImportCheck {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        ByteArrayOutputStream mono=new ByteArrayOutputStream();ByteBuffer stereo=ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());stereo.putShort((short)10000).putShort((short)30000).putShort((short)-20000).putShort((short)20000).flip();FileAnalysis.writeMono(stereo,mono,2,2);byte[] data=mono.toByteArray();require(data.length==4,"stereo length");require((short)((data[0]&255)|(data[1]<<8))==20000,"stereo average");require(data[2]==0&&data[3]==0,"opposite phase average");
        ByteArrayOutputStream converted=new ByteArrayOutputStream();
        FileAnalysis.writeMono(ByteBuffer.wrap(new byte[]{0,0,(byte)0x80,(byte)0xff,(byte)0xff,0x7f}).order(ByteOrder.LITTLE_ENDIAN),converted,1,21);byte[] packed=converted.toByteArray();require(packed[0]==0&&packed[1]==(byte)0x80&&packed[2]==(byte)0xff&&packed[3]==0x7f,"PCM 24 bit extrema");
        converted.reset();ByteBuffer floats=ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());floats.putFloat(-1).putFloat(1).flip();FileAnalysis.writeMono(floats,converted,1,4);packed=converted.toByteArray();require((short)((packed[0]&255)|(packed[1]<<8))==-32767&&(short)((packed[2]&255)|(packed[3]<<8))==32767,"float extrema");
        converted.reset();ByteBuffer ints=ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());ints.putInt(Integer.MIN_VALUE).putInt(Integer.MAX_VALUE).flip();FileAnalysis.writeMono(ints,converted,1,22);packed=converted.toByteArray();require(packed[0]==0&&packed[1]==(byte)0x80&&packed[2]==(byte)0xff&&packed[3]==0x7f,"PCM 32 bit extrema");
        File raw=File.createTempFile("import-test",".pcm");try{
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(raw))){for(int i=0;i<44100*2+123;i++){int v=(int)(20000*Math.sin(2*Math.PI*3000*i/44100));out.write(v&255);out.write((v>>8)&255);}}
            for(int method=0;method<3;method++){FileAnalysis.Result r=FileAnalysis.analyze(raw,44100,2048,method,m->{});require(Math.abs(r.duration-(2+123.0/44100))<1e-9,"duration native rate");int peak=0;for(int k=1;k<512;k++)if((r.levels[0][k]&255)>(r.levels[0][peak]&255))peak=k;require(Math.abs(peak*22050.0/512-3000)<90,"3000 Hz native-rate peak");}
            try(OutputStream out=new FileOutputStream(raw)){out.write(new byte[2048]);}FileAnalysis.Result silence=FileAnalysis.analyze(raw,48000,1024,0,m->{});for(byte level:silence.levels[0])require(level==0,"silence black");
            try(RandomAccessFile large=new RandomAccessFile(raw,"rw")){large.setLength(8193L*1024*2);}FileAnalysis.Result overview=FileAnalysis.analyze(raw,48000,1024,0,m->{});require(overview.levels.length<=8192,"bounded columns");require(Math.abs(overview.step-2048.0/48000)<1e-9,"overview grouped timing");
            Thread.currentThread().interrupt();try{FileAnalysis.analyze(raw,48000,1024,0,m->{});throw new AssertionError("cancel ignored");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}
        }finally{raw.delete();}System.out.println("Import checks passed: stereo, native rate, all methods, partial frame, silence, cancellation.");
    }
}
