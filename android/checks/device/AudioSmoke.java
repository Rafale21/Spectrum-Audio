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
import android.app.*;
import android.os.*;
import android.media.*;
import java.io.*;
import java.nio.*;

/** Optional real-device integration test of the production encoder and decoder. */
public final class AudioSmoke extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        if(Build.VERSION.SDK_INT<29){result.putString("stream","SKIP: native OGG/Opus needs Android 10+.\n");finish(Activity.RESULT_OK,result);return;}
        File folder=new File(getContext().getCacheDir(),"audio-smoke-"+System.nanoTime());if(!folder.mkdirs())throw new IOException("No test directory");
        try{
            for(int format:new int[]{RecordingFormat.OPUS,RecordingFormat.AAC})for(int length:new int[]{5937,59377}){
                String name="take-"+format+"-"+length;File draft=new File(folder,name+".draft");short[] block=new short[512];int written=0;
                try(WavFile wav=new WavFile(draft)){while(written<length){int count=Math.min(block.length,length-written);for(int i=0;i<count;i++)block[i]=(short)(12000*Math.sin(2*Math.PI*880*(written+i)/48000.0));wav.write(block,count);written+=count;}}
                File saved=RecordingStore.finish(draft,folder,name,format);if(draft.exists())throw new AssertionError("draft retained after success");
                long samples=decode(saved,format);if(Math.abs(samples-length)>4800)throw new AssertionError("duration mismatch: "+samples+" vs "+length);saved.delete();
            }
        }finally{File[] files=folder.listFiles();if(files!=null)for(File file:files)file.delete();folder.delete();}
        result.putString("stream","PASS: production OGG/Opus and M4A/AAC exports decode, including short/non-aligned takes and finalization.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){result.putString("stream","FAIL: "+error+"\n");finish(Activity.RESULT_CANCELED,result);}}
    private static long decode(File file,int format)throws Exception {
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;
        try{extractor.setDataSource(file.getAbsolutePath());int track=-1;MediaFormat inputFormat=null;
            for(int i=0;i<extractor.getTrackCount();i++){MediaFormat candidate=extractor.getTrackFormat(i);if(candidate.getString(MediaFormat.KEY_MIME).startsWith("audio/")){track=i;inputFormat=candidate;break;}}
            if(track<0)throw new AssertionError("no audio track");String mime=inputFormat.getString(MediaFormat.KEY_MIME);if(format==RecordingFormat.OPUS&&!mime.equals("audio/opus"))throw new AssertionError("not actual Opus: "+mime);
            extractor.selectTrack(track);decoder=MediaCodec.createDecoderByType(mime);decoder.configure(inputFormat,null,null,0);decoder.start();
            boolean inputEnd=false,outputEnd=false;long samples=0,deadline=SystemClock.elapsedRealtime()+20000;int peak=0,channels=1,encoding=AudioFormat.ENCODING_PCM_16BIT;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(!outputEnd){if(SystemClock.elapsedRealtime()>deadline)throw new IOException("decoder timeout");
                if(!inputEnd){int index=decoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer buffer=decoder.getInputBuffer(index);buffer.clear();int n=extractor.readSampleData(buffer,0);if(n<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}else{decoder.queueInputBuffer(index,0,n,extractor.getSampleTime(),0);extractor.advance();}}}
                int index=decoder.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat output=decoder.getOutputFormat();channels=output.getInteger(MediaFormat.KEY_CHANNEL_COUNT);if(output.containsKey(MediaFormat.KEY_PCM_ENCODING))encoding=output.getInteger(MediaFormat.KEY_PCM_ENCODING);}
                else if(index>=0){try{ByteBuffer pcm=AudioBuffers.window(decoder.getOutputBuffer(index),info.offset,info.size).order(ByteOrder.nativeOrder());if(encoding==AudioFormat.ENCODING_PCM_FLOAT){samples+=info.size/4/channels;while(pcm.remaining()>=4)peak=Math.max(peak,(int)(Math.abs(pcm.getFloat())*32768));}else{samples+=info.size/2/channels;while(pcm.remaining()>=2)peak=Math.max(peak,Math.abs((int)pcm.getShort()));}outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;}finally{decoder.releaseOutputBuffer(index,false);}}
            }
            if(peak<1000)throw new AssertionError("decoded audio silent");return samples;
        }finally{extractor.release();if(decoder!=null){try{decoder.stop();}catch(Exception ignored){}decoder.release();}}
    }
}
