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
import java.util.*;

/** Bounded ISO BMFF AAC sample reader, used when Android cannot expose track formats. */
final class Mp4Aac implements Closeable {
    final RandomAccessFile file;final int rate,channels,timescale;final byte[] config;
    final int[] sizes,durations;final long[] offsets;final int maxSize;
    private static final int MAX_SAMPLES=1_000_000;
    private static final class Box {long data,end;String type;Box(String t,long d,long e){type=t;data=d;end=e;}}
    private static final class Track {int rate,channels,timescale;byte[] config;int[] sizes,durations;long[] offsets;}
    static Mp4Aac open(File source)throws IOException {RandomAccessFile file=new RandomAccessFile(source,"r");try{return new Mp4Aac(file);}catch(IOException|RuntimeException e){file.close();throw e;}}
    private Mp4Aac(RandomAccessFile input)throws IOException {
        file=input;Box moov=find(children(0,file.length()),"moov");Track selected=null;String failure="";
        for(Box track:children(moov.data,moov.end))if(track.type.equals("trak"))try{Track candidate=track(track);if(candidate!=null){selected=candidate;break;}}catch(IOException|RuntimeException e){failure=AudioBuffers.error(e);}
        if(selected==null)throw new IOException("Secours M4A : aucune piste AAC non chiffrée compatible. "+failure);
        rate=selected.rate;channels=selected.channels;timescale=selected.timescale;config=selected.config;sizes=selected.sizes;durations=selected.durations;offsets=selected.offsets;int max=0;for(int size:sizes)max=Math.max(max,size);maxSize=max;
    }
    private List<Box> children(long start,long end)throws IOException {
        List<Box> result=new ArrayList<>();long position=start;
        while(position+8<=end){if(result.size()>10000)throw new IOException("Trop de boîtes MP4.");file.seek(position);long length=u32();String type=four();long header=8;if(length==1){length=file.readLong();header=16;}else if(length==0)length=end-position;
            if(length<header||length>end-position)throw new IOException("Boîte MP4 tronquée : "+type);result.add(new Box(type,position+header,position+length));position+=length;}
        return result;
    }
    private static Box find(List<Box> boxes,String type)throws IOException {for(Box b:boxes)if(b.type.equals(type))return b;throw new IOException("Table MP4 absente : "+type);}
    private String four()throws IOException {byte[] bytes=new byte[4];file.readFully(bytes);return new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1);}
    private long u32()throws IOException {return file.readInt()&0xffffffffL;}
    private int number(long value,int max,String what)throws IOException {if(value<=0||value>max)throw new IOException(what+" hors limites : "+value);return (int)value;}
    private int count(Box box,int stride)throws IOException {file.seek(box.data+4);long count=u32();if(count>MAX_SAMPLES||count*stride>box.end-box.data-8)throw new IOException("Table MP4 invalide : "+box.type);return (int)count;}
    private Track track(Box track)throws IOException {
        Box mdia=find(children(track.data,track.end),"mdia");List<Box> media=children(mdia.data,mdia.end);Box hdlr=find(media,"hdlr");file.seek(hdlr.data+8);if(!four().equals("soun"))return null;
        Track result=new Track();Box mdhd=find(media,"mdhd");file.seek(mdhd.data);int version=file.readUnsignedByte();if(version>1)throw new IOException("Version mdhd inconnue.");file.seek(mdhd.data+(version==1?20:12));result.timescale=number(u32(),Integer.MAX_VALUE,"Temps MP4");
        Box minf=find(media,"minf"),stbl=find(children(minf.data,minf.end),"stbl");List<Box> tables=children(stbl.data,stbl.end);
        Box stsd=find(tables,"stsd");file.seek(stsd.data+4);if(u32()!=1)throw new IOException("Plusieurs descriptions audio non prises en charge en secours.");List<Box> entries=children(stsd.data+8,stsd.end);Box entry=find(entries,"mp4a");long origin=entry.data-8;
        file.seek(origin+16);int soundVersion=file.readUnsignedShort();if(soundVersion>1)throw new IOException("Version mp4a inconnue.");file.seek(origin+24);result.channels=file.readUnsignedShort();file.seek(origin+32);result.rate=(int)(u32()>>>16);
        Box esds=findEsds(children(origin+(soundVersion==1?52:36),entry.end));long configLength=esds.end-esds.data-4;if(configLength<=0||configLength>65536)throw new IOException("Configuration AAC invalide.");byte[] descriptors=new byte[(int)configLength];file.seek(esds.data+4);file.readFully(descriptors);result.config=descriptor(descriptors,0,descriptors.length,0);if(result.config==null||result.config.length<2)throw new IOException("Configuration AAC absente.");audioConfig(result);
        Box stsz=find(tables,"stsz");file.seek(stsz.data+4);long uniform=u32();int samples=number(u32(),MAX_SAMPLES,"Nombre de paquets AAC");result.sizes=new int[samples];
        if(uniform==0&&samples*4L>stsz.end-stsz.data-12)throw new IOException("Table stsz tronquée.");for(int i=0;i<samples;i++)result.sizes[i]=number(uniform==0?u32():uniform,16*1024*1024,"Taille paquet AAC");
        Box stts=find(tables,"stts");int timingCount=count(stts,8);result.durations=new int[samples];int sample=0;
        for(int i=0;i<timingCount;i++){int amount=number(u32(),MAX_SAMPLES,"Durée paquets"),delta=number(u32(),Integer.MAX_VALUE,"Pas temporel");if(amount>samples-sample)throw new IOException("Table stts incohérente.");Arrays.fill(result.durations,sample,sample+amount,delta);sample+=amount;}if(sample!=samples)throw new IOException("Durées AAC incomplètes.");
        Box stsc=find(tables,"stsc");int mappings=count(stsc,12);int[] first=new int[mappings],perChunk=new int[mappings];
        for(int i=0;i<mappings;i++){first[i]=number(u32(),MAX_SAMPLES,"Premier chunk");perChunk[i]=number(u32(),MAX_SAMPLES,"Paquets par chunk");if(u32()!=1||(i==0&&first[i]!=1)||(i>0&&first[i]<=first[i-1]))throw new IOException("Table stsc non prise en charge.");}if(mappings==0)throw new IOException("Table stsc vide.");
        Box chunks=null;for(Box b:tables)if(b.type.equals("stco")||b.type.equals("co64")){chunks=b;break;}if(chunks==null)throw new IOException("Offsets des chunks absents.");boolean wide=chunks.type.equals("co64");int chunkCount=count(chunks,wide?8:4);long[] locations=new long[chunkCount];for(int i=0;i<chunkCount;i++)locations[i]=wide?file.readLong():u32();
        result.offsets=new long[samples];sample=0;int mapping=0;
        for(int chunk=0;chunk<chunkCount;chunk++){while(mapping+1<mappings&&first[mapping+1]<=chunk+1)mapping++;long offset=locations[chunk];for(int i=0;i<perChunk[mapping];i++){if(sample>=samples)throw new IOException("Nombre de paquets/chunks incohérent.");int size=result.sizes[sample];if(offset<0||offset>file.length()-size)throw new IOException("Paquet AAC hors du fichier.");result.offsets[sample++]=offset;offset+=size;}}
        if(sample!=samples)throw new IOException("Paquets AAC manquants.");return result;
    }
    private static final class Bits {final byte[] bytes;int bit;Bits(byte[] b){bytes=b;}int read(int count)throws IOException {if(bit+count>bytes.length*8)throw new IOException("Configuration AAC tronquée.");int value=0;while(count-->0){value=(value<<1)|((bytes[bit/8]>>(7-bit%8))&1);bit++;}return value;}}
    private static int aacRate(Bits bits)throws IOException {int index=bits.read(4);int[] rates={96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};if(index==15)return bits.read(24);if(index>=rates.length)throw new IOException("Fréquence AAC réservée.");return rates[index];}
    private static void audioConfig(Track track)throws IOException {Bits bits=new Bits(track.config);int type=bits.read(5);if(type==31)type=32+bits.read(6);int rate=aacRate(bits),configuration=bits.read(4);if(type==5||type==29)rate=aacRate(bits);track.rate=rate;if(configuration>=1&&configuration<=7)track.channels=configuration==7?8:configuration;if(track.rate<=0||track.rate>192000||track.channels<1||track.channels>8)throw new IOException("Configuration de fréquence/canaux AAC non prise en charge.");}
    private Box findEsds(List<Box> boxes)throws IOException {for(Box b:boxes){if(b.type.equals("esds"))return b;if(b.type.equals("wave"))return findEsds(children(b.data,b.end));}throw new IOException("Boîte esds absente.");}
    private static byte[] descriptor(byte[] data,int start,int end,int depth)throws IOException {
        if(depth>5)throw new IOException("Descripteurs AAC trop imbriqués.");int position=start;
        while(position<end){int tag=data[position++]&255,length=0,bytes=0,value;do{if(position>=end||bytes++>=4)throw new IOException("Longueur esds invalide.");value=data[position++]&255;length=(length<<7)|(value&127);}while((value&128)!=0);if(length>end-position)throw new IOException("Descripteur esds tronqué.");int next=position+length;
            if(tag==5)return Arrays.copyOfRange(data,position,next);
            int nested=-1;if(tag==3){if(length<3)throw new IOException("Descripteur ES invalide.");int flags=data[position+2]&255;nested=position+3;if((flags&128)!=0)nested+=2;if((flags&64)!=0){if(nested>=next)throw new IOException("URL ES invalide.");nested+=1+(data[nested]&255);}if((flags&32)!=0)nested+=2;}else if(tag==4)nested=position+13;
            if(nested>=0){if(nested>next)throw new IOException("Descripteur AAC invalide.");byte[] config=descriptor(data,nested,next,depth+1);if(config!=null)return config;}position=next;}
        return null;
    }
    int read(int index,java.nio.ByteBuffer destination)throws IOException {int size=sizes[index];if(size>destination.remaining())throw new IOException("Buffer AAC trop petit : "+size);byte[] packet=new byte[size];file.seek(offsets[index]);file.readFully(packet);destination.put(packet);return size;}
    public void close()throws IOException {file.close();}
}
