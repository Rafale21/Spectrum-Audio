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
import studio.noads.spectrumaudio.*;
import java.io.*;
public class AudioCheck {
 public static void main(String[] args) throws Exception {
  for(int n:new int[]{512,1024,2048,4096,8192})for(int method=0;method<3;method++){
   short[] x=new short[n];int target=64;
   for(int i=0;i<n;i++)x[i]=(short)(16384*Math.sin(2*Math.PI*target*i/n));
   double[] out=Spectrum.analyze(x,method);int peak=0;for(int k=0;k<out.length;k++)if(out[k]>out[peak])peak=k;
   if(peak!=(method==2?target/2:target))throw new AssertionError("Frequency");
   if(Math.abs(out[peak]-.5)>.002)throw new AssertionError("Amplitude "+out[peak]);
   for(double a:Spectrum.analyze(new short[n],method))if(a!=0)throw new AssertionError("Silence");
  }
  File f=File.createTempFile("spectre-test",".wav");try(WavFile w=new WavFile(f)){w.write(new short[]{-32768,0,32767},3);}
  try(RandomAccessFile r=new RandomAccessFile(f,"r")){if(r.length()!=50)throw new AssertionError("WAV length");r.seek(40);if(Integer.reverseBytes(r.readInt())!=6)throw new AssertionError("WAV size");r.seek(44);if(r.readUnsignedByte()!=0||r.readUnsignedByte()!=128)throw new AssertionError("PCM endianness");}f.delete();
  System.out.println("PASS: 15 sine tests, 15 silence tests, WAV header + PCM.");
 }
}
