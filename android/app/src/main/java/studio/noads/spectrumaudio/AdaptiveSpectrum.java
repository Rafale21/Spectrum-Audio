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
import java.util.Arrays;
/** Real STFT recalculation of only the visible PCM region. No image interpolation. */
final class AdaptiveSpectrum {
    static final int MAX_SIDE=4096,MAX_PIXELS=2048*1536;
    static final class Request {
        final File pcm;final int rate,width,height,baseFft,method;final double start,span,low,high;final boolean log;
        Request(File pcm,int rate,int width,int height,int baseFft,int method,double start,double span,double low,double high,boolean log){
            this.pcm=pcm;this.rate=rate;
            int w=Math.max(16,Math.min(MAX_SIDE,width)),h=Math.max(16,Math.min(MAX_SIDE,height));
            double scale=Math.min(1,Math.sqrt(MAX_PIXELS/((double)w*h)));
            this.width=(int)(w*scale);this.height=(int)(h*scale);
            this.baseFft=baseFft;this.method=method;this.start=start;this.span=span;this.low=low;this.high=high;this.log=log;
        }
    }
    static final class Tile {
        final Request view;final byte[] levels;final int fft;final double frequencyStep,timeStep;
        Tile(Request request,byte[] levels,int fft){view=request;this.levels=levels;this.fft=fft;frequencyStep=request.rate/(double)(request.method==2?fft/2:fft);timeStep=request.span/request.width;}
        int[] pixels(int gain,int range,boolean gray){int[] pixels=new int[levels.length];for(int i=0;i<pixels.length;i++)pixels[i]=ColorMap.color((levels[i]&255)*140.0/255-140,gain,range,gray);return pixels;}
    }
    /** The selected FFT is the minimum analysis window, not an ignored hint.
     * Extra frequency precision must not smear more than four visible time columns.
     * Zooming in therefore returns to the selected window, like the desktop renderer.
     */
    static int fft(Request view){
        double target=Math.max(view.baseFft,2.0*view.rate*view.height/Math.max(1,view.high-view.low));
        double timeBudget=Math.max(view.baseFft,4.0*view.rate*view.span/view.width);
        int size=view.baseFft;
        while(size<32768&&size<target&&size*2<=timeBudget)size*=2;
        return size;
    }
    private static void check()throws InterruptedIOException {if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Affinage annulé");}
    private static double frequency(Request view,double fraction){return view.log?view.low*Math.pow(view.high/view.low,fraction):view.low+(view.high-view.low)*fraction;}
    static Tile render(Request view)throws IOException {
        if(view.pcm==null||view.rate<=0||!Double.isFinite(view.start)||!Double.isFinite(view.span)||!Double.isFinite(view.low)||!Double.isFinite(view.high)||view.span<=0||view.high<=view.low||view.low<0||(view.log&&view.low<=0)||view.baseFft<512||view.baseFft>32768||(view.baseFft&(view.baseFft-1))!=0)throw new IOException("Zone d’analyse invalide.");
        int fft=fft(view);Spectrum.Analyzer analyzer=new Spectrum.Analyzer(fft,view.method);int effective=view.method==2?fft/2:fft;
        byte[] levels=new byte[view.width*view.height],data=new byte[fft*2];short[] block=new short[fft];int[] low=new int[view.height],high=new int[view.height];
        for(int row=0;row<view.height;row++){double a=frequency(view,1-(row+1.0)/view.height),b=frequency(view,1-row/(double)view.height);int first=(int)Math.ceil(a*effective/view.rate),last=(int)Math.floor(b*effective/view.rate);if(last<first){first=last=(int)Math.round((a+b)*.5*effective/view.rate);}low[row]=Math.max(0,first);high[row]=Math.min(effective/2,last);}
        try(RandomAccessFile input=new RandomAccessFile(view.pcm,"r")){long samples=input.length()/2,previousBegin=Long.MIN_VALUE;double[] spectrum=null;
            for(int column=0;column<view.width;column++){check();double time=view.start+(column+.5)*view.span/view.width;long begin=Math.round(time*view.rate)-fft/2;
                // At deep zoom several pixels can refer to the exact same sample.
                if(spectrum==null||begin!=previousBegin){Arrays.fill(block,(short)0);long first=Math.max(0,begin),last=Math.min(samples,begin+fft);
                if(last>first){input.seek(first*2);int count=(int)(last-first);input.readFully(data,0,count*2);int offset=(int)(first-begin);for(int i=0;i<count;i++)block[offset+i]=(short)((data[i*2]&255)|(data[i*2+1]<<8));}
                spectrum=analyzer.analyze(block);previousBegin=begin;}
                for(int row=0;row<view.height;row++){double amplitude=0;for(int bin=low[row];bin<=high[row];bin++)amplitude=Math.max(amplitude,spectrum[bin]);double db=20*Math.log10(Math.max(1e-7,amplitude));levels[row*view.width+column]=(byte)Math.round(Math.max(0,Math.min(140,db+140))*255/140);}
            }
        }
        return new Tile(view,levels,fft);
    }
}
