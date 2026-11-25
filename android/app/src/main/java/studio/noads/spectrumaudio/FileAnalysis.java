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

import android.content.Context;
import android.net.Uri;
import android.media.*;
import java.io.*;
import java.nio.*;

/** Decodes locally to a temporary mono PCM file, then builds a bounded overview. */
final class FileAnalysis {
    interface Progress { void update(String message); }
    static final class Result {
        File pcm;void close(){if(pcm!=null){pcm.delete();pcm=null;}}
        final byte[][] levels; final int rate; final double duration,step;
        Result(byte[][] l,int r,double d,double s){levels=l;rate=r;duration=d;step=s;}
    }
    static void check() throws InterruptedIOException {if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Annulé");}
    static Result load(Context context,Uri uri,int fft,int method,Progress progress)throws Exception {
        try{return loadNative(context,uri,fft,method,progress);}catch(InterruptedIOException e){throw e;}catch(Exception nativeError){
            if(!"file".equals(uri.getScheme()))throw nativeError;
            File raw=File.createTempFile("spectre-aac-",".pcm",context.getCacheDir());boolean keep=false;
            try{progress.update("Lecture M4A/AAC de secours…");int rate=decodeMp4(new File(uri.getPath()),raw);Result result=analyze(raw,rate,fft,method,progress);result.pcm=raw;keep=true;return result;}
            catch(InterruptedIOException e){throw e;}catch(Exception fallback){throw new IOException(AudioBuffers.error(nativeError)+"\nSecours M4A/AAC : "+AudioBuffers.error(fallback),fallback);}finally{if(!keep)raw.delete();}
        }
    }
    private static Result loadNative(Context context,Uri uri,int fft,int method,Progress progress)throws Exception {
        File raw=File.createTempFile("spectre-import-",".pcm",context.getCacheDir());boolean keep=false;
        try {
            String mime;MediaExtractor probe=new MediaExtractor();
            try{source(probe,context,uri);AudioTracks.Selected<MediaFormat> track=audioTrack(probe);mime=track.format.getString(MediaFormat.KEY_MIME);}
            catch(Exception e){throw new IOException("Ouverture du conteneur : "+AudioBuffers.error(e),e);}finally{probe.release();}
            java.util.ArrayList<String> candidates=new java.util.ArrayList<>();
            if(!"audio/raw".equals(mime)){
                for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos())if(!info.isEncoder())for(String type:info.getSupportedTypes())if(type.equalsIgnoreCase(mime)){String name=info.getName();if(name.startsWith("c2.android.")||name.startsWith("OMX.google."))candidates.add(0,name);else candidates.add(name);break;}
            }
            candidates.add(null);Exception failure=null;int rate=0;
            for(int attempt=0;attempt<Math.min(4,candidates.size());attempt++){
                check();String decoder=candidates.get(attempt);progress.update("Décodage "+mime+" · essai "+(attempt+1)+"…");
                try{rate=decode(context,uri,raw,decoder);failure=null;break;}
                catch(InterruptedIOException e){throw e;}
                catch(Exception e){failure=new IOException("Décodage "+mime+" ("+(decoder==null?"défaut":decoder)+") : "+AudioBuffers.error(e),e);}
            }
            if(failure!=null)throw failure;
            Result result=analyze(raw,rate,fft,method,progress);result.pcm=raw;keep=true;return result;
        }finally{if(!keep)raw.delete();}
    }
    private static void source(MediaExtractor extractor,Context context,Uri uri)throws IOException {
        if("file".equals(uri.getScheme())){try(FileInputStream input=new FileInputStream(new File(uri.getPath()))){extractor.setDataSource(input.getFD());}}
        else extractor.setDataSource(context,uri,null);
    }
    private static AudioTracks.Selected<MediaFormat> audioTrack(MediaExtractor extractor)throws IOException {
        return AudioTracks.select(new AudioTracks.Provider<MediaFormat>(){public int count(){return extractor.getTrackCount();}public MediaFormat format(int index){return extractor.getTrackFormat(index);}public String mime(MediaFormat format){return format.getString(MediaFormat.KEY_MIME);}});
    }
    private static int integer(MediaFormat format,String key,int fallback){return format.containsKey(key)?format.getInteger(key):fallback;}
    private static int decode(Context context,Uri uri,File raw,String decoder)throws Exception {
        MediaExtractor extractor=new MediaExtractor();MediaCodec codec=null;
        try {
            source(extractor,context,uri);AudioTracks.Selected<MediaFormat> selected=audioTrack(extractor);extractor.selectTrack(selected.index);MediaFormat format=selected.format;
            int rate=integer(format,MediaFormat.KEY_SAMPLE_RATE,0),channels=integer(format,MediaFormat.KEY_CHANNEL_COUNT,0),encoding=integer(format,MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
            if(rate<=0||channels<=0)throw new IOException("Fréquence ou nombre de canaux manquant dans la piste audio.");
            String mime=format.getString(MediaFormat.KEY_MIME);boolean pcm="audio/raw".equals(mime);
            if(!pcm){codec=decoder==null?MediaCodec.createDecoderByType(mime):MediaCodec.createByCodecName(decoder);codec.configure(format,null,null,0);codec.start();}
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(raw))){
                if(pcm){ByteBuffer buffer=ByteBuffer.allocateDirect(1024*1024);while(true){check();buffer.clear();int n=extractor.readSampleData(buffer,0);if(n<0)break;writeMono(AudioBuffers.window(buffer,0,n),out,channels,encoding);extractor.advance();limit(raw);}}
                else {
                    boolean inputEnd=false,outputEnd=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long lastOutput=System.nanoTime();
                    while(!outputEnd){check();if(System.nanoTime()-lastOutput>30_000_000_000L)throw new IOException("Le décodeur ne répond plus.");
                        if(!inputEnd){int index=codec.dequeueInputBuffer(10000);if(index>=0){ByteBuffer input=codec.getInputBuffer(index);if(input==null)throw new IOException("Buffer d’entrée indisponible.");input.clear();int n=extractor.readSampleData(input,0);
                            if(n<0){codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                            else {if((extractor.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Piste audio protégée/chiffrée non prise en charge.");codec.queueInputBuffer(index,0,n,Math.max(0,extractor.getSampleTime()),0);extractor.advance();}}}
                        int index=codec.dequeueOutputBuffer(info,10000);
                        if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat output=codec.getOutputFormat();rate=integer(output,MediaFormat.KEY_SAMPLE_RATE,rate);channels=integer(output,MediaFormat.KEY_CHANNEL_COUNT,channels);encoding=integer(output,MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);lastOutput=System.nanoTime();}
                        else if(index>=0){lastOutput=System.nanoTime();try{if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0)writeMono(AudioBuffers.window(codec.getOutputBuffer(index),info.offset,info.size),out,channels,encoding);outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;}finally{codec.releaseOutputBuffer(index,false);}}
                        limit(raw);
                    }
                }
            }
            if(raw.length()==0)throw new IOException("Le décodeur n’a produit aucun échantillon audio.");
            return rate;
        }finally{if(codec!=null){try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}}try{extractor.release();}catch(Exception ignored){}}
    }
    private static int decodeMp4(File source,File raw)throws Exception {
        MediaCodec codec=null;
        try(Mp4Aac input=Mp4Aac.open(source);OutputStream out=new BufferedOutputStream(new FileOutputStream(raw))){
            MediaFormat format=MediaFormat.createAudioFormat("audio/mp4a-latm",input.rate,input.channels);format.setByteBuffer("csd-0",ByteBuffer.wrap(input.config));format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,input.maxSize);
            codec=MediaCodec.createDecoderByType("audio/mp4a-latm");codec.configure(format,null,null,0);codec.start();
            int sample=0,rate=input.rate,channels=input.channels,encoding=AudioFormat.ENCODING_PCM_16BIT;long ticks=0,lastOutput=System.nanoTime();boolean inputEnd=false,outputEnd=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(!outputEnd){check();if(System.nanoTime()-lastOutput>30_000_000_000L)throw new IOException("Le décodeur AAC de secours ne répond plus.");
                if(!inputEnd){int index=codec.dequeueInputBuffer(10000);if(index>=0){ByteBuffer buffer=codec.getInputBuffer(index);if(buffer==null)throw new IOException("Buffer AAC absent.");buffer.clear();long time=ticks*1000000/input.timescale;
                    if(sample==input.sizes.length){codec.queueInputBuffer(index,0,0,time,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                    else{int size=input.read(sample,buffer);codec.queueInputBuffer(index,0,size,time,0);ticks+=input.durations[sample++];}}}
                int index=codec.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat output=codec.getOutputFormat();rate=integer(output,MediaFormat.KEY_SAMPLE_RATE,rate);channels=integer(output,MediaFormat.KEY_CHANNEL_COUNT,channels);encoding=integer(output,MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);lastOutput=System.nanoTime();}
                else if(index>=0){lastOutput=System.nanoTime();try{if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0)writeMono(AudioBuffers.window(codec.getOutputBuffer(index),info.offset,info.size),out,channels,encoding);outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;}finally{codec.releaseOutputBuffer(index,false);}}limit(raw);
            }
            out.flush();if(raw.length()==0)throw new IOException("Aucun échantillon AAC décodé.");return rate;
        }finally{if(codec!=null){try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}}}
    }
    private static void limit(File raw)throws IOException {if(raw.length()>1024L*1024*1024)throw new IOException("Limite de 1 Go de PCM décodé atteinte.");}
    static void writeMono(ByteBuffer b,OutputStream out,int channels,int encoding)throws IOException {
        int bytes=encoding==AudioFormat.ENCODING_PCM_FLOAT||encoding==22?4:encoding==21?3:encoding==AudioFormat.ENCODING_PCM_8BIT?1:encoding==AudioFormat.ENCODING_PCM_16BIT?2:0;
        if(bytes==0||channels<1)throw new IOException("Encodage PCM non pris en charge ("+encoding+").");
        while(b.remaining()>=bytes*channels){double sum=0;for(int c=0;c<channels;c++){
            if(encoding==AudioFormat.ENCODING_PCM_FLOAT)sum+=Math.max(-1,Math.min(1,b.getFloat()))*32767;
            else if(encoding==22)sum+=b.getInt()/65536.0;
            else if(encoding==21){int a=b.get()&255,d=b.get()&255,e=b.get()&255;int v=b.order()==ByteOrder.LITTLE_ENDIAN?a|(d<<8)|(e<<16):e|(d<<8)|(a<<16);sum+=((v<<8)>>8)/256.0;}
            else sum+=bytes==1?((b.get()&255)-128)*256:b.getShort();
        }int v=Math.max(-32768,Math.min(32767,(int)(sum/channels)));out.write(v&255);out.write((v>>8)&255);}
    }

    static Result analyze(File raw,int rate,int fft,int method,Progress progress)throws IOException {
        long samples=raw.length()/2;if(samples==0)throw new IOException("Le fichier audio est vide.");
        long frames=(samples+fft-1)/fft;int group=(int)Math.max(1,(frames+8191)/8192);int columns=(int)((frames+group-1)/group);
        byte[][] levels=new byte[columns][512];short[] block=new short[fft];byte[] bytes=new byte[fft*2];Spectrum.Analyzer analyzer=new Spectrum.Analyzer(fft,method);
        try(InputStream in=new BufferedInputStream(new FileInputStream(raw))){
            for(long frame=0;frame<frames;frame++){check();int read=0,n;while(read<bytes.length&&(n=in.read(bytes,read,bytes.length-read))>0)read+=n;
                for(int i=0;i<fft;i++)block[i]=i*2+1<read?(short)((bytes[i*2]&255)|(bytes[i*2+1]<<8)):0;
                double[] a=analyzer.analyze(block);byte[] column=levels[(int)(frame/group)];
                for(int bin=0;bin<a.length;bin++){int row=Math.min(511,bin*512/a.length);int value=(int)Math.round(Math.max(0,Math.min(100,100+20*Math.log10(Math.max(1e-6,a[bin]))))*2.55);if(value>(column[row]&255))column[row]=(byte)value;}
                if(frame%128==0)progress.update("Analyse : "+(frame*100/frames)+" %");
            }
        }
        return new Result(levels,rate,samples/(double)rate,group*fft/(double)rate);
    }
}
