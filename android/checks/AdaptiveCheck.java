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
public class AdaptiveCheck {
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static int at(AdaptiveSpectrum.Tile tile,double time,double hz){AdaptiveSpectrum.Request view=tile.view;int x=Math.max(0,Math.min(view.width-1,(int)((time-view.start)/view.span*view.width)));double f=view.log?Math.log(hz/view.low)/Math.log(view.high/view.low):(hz-view.low)/(view.high-view.low);int y=Math.max(0,Math.min(view.height-1,(int)((1-f)*view.height)));return tile.levels[y*view.width+x]&255;}
    static void write(short[] pcm,File file)throws IOException {try(OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){for(short value:pcm){out.write(value&255);out.write((value>>8)&255);}}}
    public static void main(String[] args)throws Exception {
        for(int size:new int[]{512,1024,2048,8192})for(int method=0;method<3;method++){short[] pcm=new short[size];for(int i=0;i<size;i++)pcm[i]=(short)(10000*Math.sin(2*Math.PI*47*i/size)+2000*Math.cos(2*Math.PI*93*i/size));Spectrum.Analyzer analyzer=new Spectrum.Analyzer(size,method);double[] actual=analyzer.analyze(pcm),expected=Spectrum.analyze(pcm,method);for(int i=0;i<actual.length;i++)require(Math.abs(actual[i]-expected[i])<1e-12,"reusable FFT parity");actual=analyzer.analyze(new short[size]);for(double value:actual)require(value==0,"scratch buffers reset");}
        File source=File.createTempFile("adaptive-check",".pcm");
        try{
            int rate=48000;short[] pcm=new short[(int)(rate*4.8)];
            // A real H encoded in the audio: 24 frequency rows, 12 temporal columns, entirely above 19 kHz.
            for(int sample=0;sample<pcm.length;sample++){double time=sample/(double)rate;int column=(int)(time/.4);double value=0;for(int row=0;row<24;row++)if(((column==2||column==3||column==8||column==9)&&row>=2&&row<=21)||(column>=2&&column<=9&&row>=11&&row<=13))value+=600*Math.sin(2*Math.PI*(19000+row*10)*time);pcm[sample]=(short)Math.max(-32768,Math.min(32767,value));}write(pcm,source);
            AdaptiveSpectrum.Tile tile=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(source,rate,96,512,8192,0,0,4.8,18950,19300,false));
            require(tile.fft==8192,"time detail respects the selected analysis window");require(tile.frequencyStep<6,"fine frequency spacing");
            AdaptiveSpectrum.Tile frequencyDetail=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(source,rate,16,512,8192,0,0,4.8,18950,19300,false));
            require(frequencyDetail.fft==32768&&frequencyDetail.frequencyStep<2,"narrow-band frequency refinement remains available when temporal sampling allows it");
            int left=at(tile,1.0,19050),gap=at(tile,2.2,19050),bar=at(tile,2.2,19120),right=at(tile,3.6,19050);
            require(left>gap+30&&right>gap+30&&bar>gap+30,"hidden H recovered from actual PCM: "+left+","+gap+","+bar+","+right);
            // Moving to a quiet region must not reuse pixels from the visible H.
            AdaptiveSpectrum.Tile quiet=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(source,rate,16,128,2048,0,0,.3,18950,19300,false));for(byte value:quiet.levels)require(value==0,"regional silence");
            AdaptiveSpectrum.Tile log=AdaptiveSpectrum.render(new AdaptiveSpectrum.Request(source,rate,16,256,8192,0,.9,.2,18000,20000,true));require(at(log,1.0,19050)>100,"log frequency mapping");
            require(ColorMap.color(-140,36,120,true)==0xff000000,"silent background black at high gain");require((ColorMap.color(-80,24,100,true)&255)>(ColorMap.color(-80,0,100,true)&255),"display gain brightens real weak signal");
            AdaptiveSpectrum.Request bounded=new AdaptiveSpectrum.Request(source,rate,100000,100000,8192,0,0,4,0,24000,false);require(bounded.width<=4096&&bounded.height<=4096&&(long)bounded.width*bounded.height<=AdaptiveSpectrum.MAX_PIXELS&&AdaptiveSpectrum.fft(bounded)<=32768,"bounded detail resources");
            AdaptiveSpectrum.Request wide=new AdaptiveSpectrum.Request(source,rate,3000,800,1024,0,0,4,0,16000,false);require(wide.width==3000&&wide.height==800,"wide viewport no longer needlessly downscaled at 2048 pixels");
            Thread.currentThread().interrupt();try{AdaptiveSpectrum.render(bounded);throw new AssertionError("cancellation ignored");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}
            if(args.length>0){try(OutputStream out=new FileOutputStream(args[0])){out.write(("P5\n"+tile.view.width+" "+tile.view.height+"\n255\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.write(tile.levels);}}
            System.out.println("PASS: FFT workspace parity/reset; real hidden H recovered at 19 kHz; local PCM seeking; log axis; black silence/gain; memory caps; cancellation.");
        }finally{source.delete();}
    }
}
