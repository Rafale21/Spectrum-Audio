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

/** One-sided amplitude spectrum, normalized to full-scale sine amplitude. */
public final class Spectrum {
    public static double[] analyze(short[] pcm, int method) {
        int n=pcm.length;
        if(method!=2) return fft(pcm,0,n,method);
        int size=n/2;
        double[] out=new double[size/2+1];
        for(int offset=0;offset<=n-size;offset+=size/2) {
            double[] s=fft(pcm,offset,size,0);
            for(int k=0;k<out.length;k++) out[k]+=s[k]*s[k]/3;
        }
        for(int k=0;k<out.length;k++) out[k]=Math.sqrt(out[k]);
        return out;
    }
    /** Reusable transform for viewport rendering, with cached window and scratch arrays. */
    static final class Analyzer {
        final int n,method;final double[] re,im,window,result;final double sum;
        Analyzer(int frameSize,int method){this.method=method;n=method==2?frameSize/2:frameSize;re=new double[n];im=new double[n];window=new double[n];result=new double[n/2+1];double total=0;for(int i=0;i<n;i++){double a=2*Math.PI*i/(n-1);window[i]=method==1?.42-.5*Math.cos(a)+.08*Math.cos(2*a):.5-.5*Math.cos(a);total+=window[i];}sum=total;}
        double[] analyze(short[] pcm){java.util.Arrays.fill(result,0);int transforms=method==2?3:1;for(int transform=0;transform<transforms;transform++){int offset=transform*n/2;
            for(int i=0;i<n;i++){re[i]=pcm[offset+i]/32768.0*window[i];im[i]=0;}
            for(int i=1,j=0;i<n;i++){int bit=n>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double value=re[i];re[i]=re[j];re[j]=value;}}
            for(int len=2;len<=n;len<<=1){double ar=Math.cos(-2*Math.PI/len),ai=Math.sin(-2*Math.PI/len);for(int i=0;i<n;i+=len){double wr=1,wi=0;for(int j=0;j<len/2;j++){int u=i+j,v=u+len/2;double tr=re[v]*wr-im[v]*wi,ti=re[v]*wi+im[v]*wr;re[v]=re[u]-tr;im[v]=im[u]-ti;re[u]+=tr;im[u]+=ti;double next=wr*ar-wi*ai;wi=wr*ai+wi*ar;wr=next;}}}
            for(int k=0;k<result.length;k++){double value=Math.hypot(re[k],im[k])*(k==0||k==n/2?1:2)/sum;if(method==2)result[k]+=value*value/3;else result[k]=value;}}
            if(method==2)for(int k=0;k<result.length;k++)result[k]=Math.sqrt(result[k]);return result;
        }
    }
    private static double[] fft(short[] pcm,int start,int n,int window) {
        double[] re=new double[n], im=new double[n]; double sum=0;
        for(int i=0;i<n;i++) {
            double a=2*Math.PI*i/(n-1);
            double w=window==1 ? .42-.5*Math.cos(a)+.08*Math.cos(2*a) : .5-.5*Math.cos(a);
            re[i]=pcm[start+i]/32768.0*w; sum+=w;
        }
        for(int i=1,j=0;i<n;i++) {
            int bit=n>>1; for(; (j&bit)!=0;bit>>=1) j^=bit; j^=bit;
            if(i<j){double t=re[i];re[i]=re[j];re[j]=t;}
        }
        for(int len=2;len<=n;len<<=1) {
            double ar=Math.cos(-2*Math.PI/len), ai=Math.sin(-2*Math.PI/len);
            for(int i=0;i<n;i+=len) {
                double wr=1,wi=0;
                for(int j=0;j<len/2;j++) {
                    int u=i+j,v=u+len/2;
                    double tr=re[v]*wr-im[v]*wi, ti=re[v]*wi+im[v]*wr;
                    re[v]=re[u]-tr; im[v]=im[u]-ti; re[u]+=tr; im[u]+=ti;
                    double t=wr*ar-wi*ai;wi=wr*ai+wi*ar;wr=t;
                }
            }
        }
        double[] out=new double[n/2+1];
        for(int k=0;k<out.length;k++) out[k]=Math.hypot(re[k],im[k])*(k==0||k==n/2?1:2)/sum;
        return out;
    }
}
