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

import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.nio.ByteBuffer;

final class RecordingStore {
    static String mime(File file){return RecordingFormat.mime(file);}
    static void copy(InputStream in,OutputStream out)throws IOException {byte[] data=new byte[65536];int n;while((n=in.read(data))!=-1)out.write(data,0,n);}
    static File finish(File draft,File directory,String name,int format)throws Exception {
        File output=new File(directory,name+RecordingFormat.extension(format));File temporary=new File(directory,output.getName()+".part");
        try{
            if(output.exists())throw new IOException("Un enregistrement porte déjà ce nom.");
            WavFile.repair(draft);
            if(format!=RecordingFormat.WAV)encode(draft,temporary,format);
            else try(InputStream in=new FileInputStream(draft);OutputStream out=new FileOutputStream(temporary)){copy(in,out);}
            if(temporary.length()==0)throw new IOException("Le fichier final est vide.");
            if(!temporary.renameTo(output))throw new IOException("Impossible de finaliser le fichier enregistré.");
            draft.delete();return output;
        }finally{temporary.delete();}
    }
    private static void encode(File wav,File destination,int selected)throws Exception {
        boolean opus=selected==RecordingFormat.OPUS;
        if(opus&&Build.VERSION.SDK_INT<29)throw new IOException("OGG/Opus nécessite Android 10 ou plus récent.");
        String mime=opus?"audio/opus":"audio/mp4a-latm";
        MediaCodec encoder=null;MediaMuxer muxer=null;boolean started=false,complete=false;
        try(RandomAccessFile in=new RandomAccessFile(wav,"r")){
            in.seek(44);long remaining=in.length()-44;if(remaining<=0)throw new IOException("Enregistrement vide.");
            int chunk=opus?1920:16384; // 20 ms of mono PCM at 48 kHz for Opus.
            MediaFormat format=MediaFormat.createAudioFormat(mime,48000,1);
            if(!opus)format.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE,128000);format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,chunk);
            encoder=MediaCodec.createEncoderByType(mime);encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);encoder.start();
            muxer=new MediaMuxer(destination.getAbsolutePath(),opus?MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG:MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            boolean inputEnd=false,outputEnd=false;long samples=0,lastOutput=System.nanoTime();int track=-1,packets=0;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();MediaCodec.BufferInfo normalized=new MediaCodec.BufferInfo();byte[] data=new byte[chunk];
            while(!outputEnd){if(System.nanoTime()-lastOutput>30_000_000_000L)throw new IOException("L’encodeur audio ne répond plus.");
                if(!inputEnd){int index=encoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer input=encoder.getInputBuffer(index);if(input==null)throw new IOException("Buffer de l’encodeur indisponible.");input.clear();int size=(int)Math.min(remaining,Math.min(data.length,input.remaining()));size-=size%2;
                    if(size==0){if(remaining>0)throw new IOException("Buffer d’entrée trop petit pour le PCM.");encoder.queueInputBuffer(index,0,0,samples*1000000/48000,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                    else{in.readFully(data,0,size);input.put(data,0,size);encoder.queueInputBuffer(index,0,size,samples*1000000/48000,0);samples+=size/2;remaining-=size;}}}
                int index=encoder.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){if(started)throw new IOException("Format modifié pendant l’export.");track=muxer.addTrack(encoder.getOutputFormat());muxer.start();started=true;lastOutput=System.nanoTime();}
                else if(index>=0){lastOutput=System.nanoTime();try{
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){if(!started)throw new IOException("Conteneur audio non initialisé.");ByteBuffer packet=AudioBuffers.window(encoder.getOutputBuffer(index),info.offset,info.size);normalized.set(0,info.size,Math.max(0,info.presentationTimeUs),info.flags);muxer.writeSampleData(track,packet,normalized);packets++;}
                    outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                }finally{encoder.releaseOutputBuffer(index,false);}}
            }
            if(!started||packets==0)throw new IOException("Aucune donnée audio produite.");
            muxer.stop();started=false;if(opus)RecordingFormat.verifyOgg(destination);complete=true;
        }finally{
            if(encoder!=null){try{encoder.stop();}catch(Exception ignored){}try{encoder.release();}catch(Exception ignored){}}
            if(muxer!=null){if(started)try{muxer.stop();}catch(Exception ignored){}try{muxer.release();}catch(Exception ignored){}}
            if(!complete)destination.delete();
        }
    }
    static Uri publish(Context context,File file)throws Exception {
        Uri uri=null;
        if(Build.VERSION.SDK_INT>=29){
            ContentValues values=new ContentValues();values.put(MediaStore.Audio.Media.DISPLAY_NAME,file.getName());values.put(MediaStore.Audio.Media.MIME_TYPE,mime(file));values.put(MediaStore.Audio.Media.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/Spectrum Audio");values.put(MediaStore.Audio.Media.IS_PENDING,1);
            uri=context.getContentResolver().insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),values);if(uri==null)throw new IOException("Impossible de créer le fichier dans Musique.");
            try{try(InputStream in=new FileInputStream(file);OutputStream out=context.getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IOException("Destination inaccessible.");copy(in,out);}values.clear();values.put(MediaStore.Audio.Media.IS_PENDING,0);if(context.getContentResolver().update(uri,values,null,null)<1)throw new IOException("Impossible de publier le fichier audio.");}
            catch(Exception error){try{context.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}throw error;}
        }else{
            File folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),"Spectrum Audio");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Accès au dossier Musique refusé. Autorisez le stockage dans les paramètres Android.");
            File target=new File(folder,file.getName()),pending=new File(folder,file.getName()+".part");
            try{try(InputStream in=new FileInputStream(file);OutputStream out=new FileOutputStream(pending)){copy(in,out);}if(!pending.renameTo(target))throw new IOException("Impossible de finaliser la copie publique.");}finally{pending.delete();}
            uri=Uri.fromFile(target);MediaScannerConnection.scanFile(context,new String[]{target.getAbsolutePath()},new String[]{mime(file)},null);
        }
        context.getSharedPreferences("exports",0).edit().putString(file.getName(),uri.toString()).commit();return uri;
    }
    static void delete(Context context,File file)throws Exception {
        android.content.SharedPreferences prefs=context.getSharedPreferences("exports",0);String reference=prefs.getString(file.getName(),null);
        if(reference!=null){Uri uri=Uri.parse(reference);if("file".equals(uri.getScheme())){File publicFile=new File(uri.getPath());if(publicFile.exists()&&!publicFile.delete())throw new IOException("Impossible de supprimer la copie dans Musique.");MediaScannerConnection.scanFile(context,new String[]{publicFile.getAbsolutePath()},null,null);}else context.getContentResolver().delete(uri,null,null);}
        if(file.exists()&&!file.delete())throw new IOException("Impossible de supprimer la copie dans l’application.");prefs.edit().remove(file.getName()).commit();
    }
}
