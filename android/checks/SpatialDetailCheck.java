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
import java.util.*;
/** End-to-end regression: alternating strokes encoded in PCM must not turn into a solid band. */
public class SpatialDetailCheck {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static int at(byte[] levels,int width,int height,double span,double time,double hz){
        int x=Math.max(0,Math.min(width-1,(int)(time/span*width)));
        int y=Math.max(0,Math.min(height-1,(int)((1-hz/16000)*height)));
        return levels[y*width+x]&255;
    }
    static void pgm(Path p,int w,int h,byte[] levels)throws IOException{
        try(OutputStream out=Files.newOutputStream(p)){out.write(("P5\n"+w+" "+h+"\n255\n").getBytes("US-ASCII"));out.write(levels);}
    }
    public static void main(String[] args)throws Exception{
        int rate=48000,w=480,h=720;double duration=2.4;
        File pcm=File.createTempFile("spectral-hatching",".pcm");
        try{
            short[] samples=new short[(int)(rate*duration)];
            for(int i=0;i<samples.length;i++){
                double time=i/(double)rate;
                // A horizontal hatch on a frame, not a picture pasted into the output.
                boolean edge=time<.15||time>duration-.15;
                boolean hatch=((int)(time/.08)&1)==0;
                double value=0;
                for(int row=0;row<17;row++){
                    boolean line=edge||row==0||row==16||(row>=3&&row<=13&&hatch);
                    if(line)value+=850*Math.sin(2*Math.PI*(4800+row*150)*time);
                }
                samples[i]=(short)Math.round(value);
            }
            AdaptiveCheck.write(samples,pcm);
            AdaptiveSpectrum.Request request=new AdaptiveSpectrum.Request(pcm,rate,w,h,1024,0,0,duration,0,16000,false);
            AdaptiveSpectrum.Tile actual=AdaptiveSpectrum.render(request);
            LegacyAdaptiveSpectrum.Tile previous=LegacyAdaptiveSpectrum.render(new LegacyAdaptiveSpectrum.Request(pcm,rate,w,h,1024,0,0,duration,0,16000,false));
            require(actual.fft==1024&&previous.fft==8192,"same 1024 setting: old forced 8192, new preserves 1024");
            double newContrast=0,oldContrast=0;
            for(int period=2;period<12;period++){
                double on=period*.16+.04,off=period*.16+.12;
                newContrast+=at(actual.levels,w,h,duration,on,6000)-at(actual.levels,w,h,duration,off,6000);
                oldContrast+=at(previous.levels,w,h,duration,on,6000)-at(previous.levels,w,h,duration,off,6000);
            }
            newContrast/=10;oldContrast/=10;
            require(newContrast>100,"clear hatching contrast from real PCM: "+newContrast);
            require(newContrast>oldContrast+60,"regression must fail the 1.18 forced-window policy: "+newContrast+" vs "+oldContrast);
            // A zoomed viewport must be recomputed, with the correct audio location.
            AdaptiveSpectrum.Tile zoom=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(pcm,rate,640,720,1024,0,.3,.6,0,16000,false));
            require(zoom.fft==1024,"deep time zoom does not silently enlarge the FFT");
            int x=320,y=(int)((1-6000/16000.0)*720);
            double center=.3+(x+.5)*.6/640;
            int sample=(int)Math.round(center*rate);short[] direct=new short[1024];
            System.arraycopy(samples,sample-512,direct,0,1024);
            double[] spectrum=Spectrum.analyze(direct,0);double peak=0;
            double lo=16000*(1-(y+1.0)/720),hi=16000*(1-y/720.0);
            int first=(int)Math.ceil(lo*1024/rate),last=(int)Math.floor(hi*1024/rate);
            if(last<first)first=last=(int)Math.round((lo+hi)*.5*1024/rate);
            for(int k=first;k<=last;k++)peak=Math.max(peak,spectrum[k]);
            int expected=(int)Math.round(Math.max(0,Math.min(140,20*Math.log10(Math.max(1e-7,peak))+140))*255/140);
            require((zoom.levels[y*640+x]&255)==expected,"zoom reads actual samples at the visible location");
            for(int base:new int[]{512,1024,2048,4096,8192}){
                int lastFft=32768;
                for(double span:new double[]{120,30,6,1,.1,.02}){
                    int n=AdaptiveSpectrum.fft(new AdaptiveSpectrum.Request(pcm,rate,1000,720,base,0,0,span,0,16000,false));
                    require(n>=base&&n<=lastFft,"zoom cannot increase temporal smearing");lastFft=n;
                }
                require(lastFft==base,"maximum zoom respects manual FFT for all sizes");
            }
            // Zero PCM remains truly black, including just outside the file edges.
            AdaptiveSpectrum.Tile outside=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(pcm,rate,16,32,512,0,20,.02,0,16000,false));
            for(byte value:outside.levels)require(value==0,"out-of-file region must be silent");
            if(args.length>0){Path out=Paths.get(args[0]);Files.createDirectories(out);pgm(out.resolve("android-1.18.pgm"),w,h,previous.levels);pgm(out.resolve("android-1.21.pgm"),w,h,actual.levels);Files.write(out.resolve("hatching.pcm"),Files.readAllBytes(pcm.toPath()));
                Files.write(out.resolve("metrics.json"),String.format(Locale.ROOT,"{\"width\":%d,\"height\":%d,\"duration\":%.1f,\"oldFft\":%d,\"newFft\":%d,\"oldContrastDb\":%.2f,\"newContrastDb\":%.2f}\n",w,h,duration,previous.fft,actual.fft,oldContrast*140/255,newContrast*140/255).getBytes("UTF-8"));}
            System.out.printf(Locale.ROOT,"PASS: real audio hatching contrast %.1f -> %.1f dB; selected FFT at deep zoom; regional sample accuracy; outside-file silence.%n",oldContrast*140/255,newContrast*140/255);
        }finally{pcm.delete();}
    }
}
