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
public final class WavFile implements Closeable {
    private final RandomAccessFile out; private long bytes;private byte[] buffer=new byte[0];
    public WavFile(File file)throws IOException {out=new RandomAccessFile(file,"rw");out.setLength(0);out.write(new byte[44]);}
    public synchronized void write(short[] samples,int count)throws IOException {
        if(count<0||count>samples.length)throw new IOException("Nombre d’échantillons invalide");
        if(bytes+count*2L>0xffffffffL-36)throw new IOException("Limite WAV de 4 Go atteinte");
        int size=count*2;if(buffer.length<size)buffer=new byte[size];for(int i=0;i<count;i++){buffer[2*i]=(byte)samples[i];buffer[2*i+1]=(byte)(samples[i]>>8);}out.write(buffer,0,size);bytes+=size;
    }
    private static void le(RandomAccessFile out,long n,int size)throws IOException {for(int i=0;i<size;i++)out.write((int)(n>>(8*i))&255);}
    private static void header(RandomAccessFile out,long bytes)throws IOException {out.seek(0);out.writeBytes("RIFF");le(out,bytes+36,4);out.writeBytes("WAVEfmt ");le(out,16,4);le(out,1,2);le(out,1,2);le(out,48000,4);le(out,96000,4);le(out,2,2);le(out,16,2);out.writeBytes("data");le(out,bytes,4);}
    /** Repair an owned unfinished mono 48 kHz draft using its actual PCM length. */
    static void repair(File file)throws IOException {try(RandomAccessFile out=new RandomAccessFile(file,"rw")){long bytes=out.length()-44;bytes-=Math.floorMod(bytes,2);if(bytes<=0||bytes>0xffffffffL-36)throw new IOException("Prise vide ou taille WAV invalide.");out.setLength(44+bytes);header(out,bytes);}}
    public synchronized void close()throws IOException {try{header(out,bytes);}finally{out.close();}}
}
