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
// Fenêtres et normalisation de Spectrum Audio 1.18. Le calcul se fait sur les échantillons du fichier à chaque niveau de zoom.
#include <stdint.h>
#include <math.h>
#define MAXFFT 8192
static double re[MAXFFT],im[MAXFFT],windowFn[MAXFFT],ctab[MAXFFT],stab[MAXFFT],amplitude[MAXFFT/2+1],windowSum;
static int rev[MAXFFT],cachedN,cachedMethod;
static void prepareFFT(int n,int method){
    if(cachedN==n && cachedMethod==method)return;
    cachedN=n;cachedMethod=method;windowSum=0;
    for(int i=0,j=0;i<n;i++){
        double a=6.283185307179586*i/(n-1);
        windowFn[i]=method==1?.42-.5*cos(a)+.08*cos(2*a):.5-.5*cos(a);
        windowSum+=windowFn[i];ctab[i]=cos(6.283185307179586*i/n);stab[i]=-sin(6.283185307179586*i/n);
        rev[i]=j;
        if(i+1<n){int bit=n>>1;while(j&bit){j^=bit;bit>>=1;}j^=bit;}
    }
}
static int spectrum(const int16_t *pcm,int count,int center,int size,int method){
    int n=method==2?size/2:size,passes=method==2?3:1;
    prepareFFT(n,method);
    for(int k=0;k<=n/2;k++)amplitude[k]=0;
    for(int p=0;p<passes;p++){
        int first=center-size/2+p*n/2;
        for(int i=0;i<n;i++){
            int ix=first+i,j=rev[i];re[j]=(ix>=0 && ix<count?pcm[ix]/32768.0:0)*windowFn[i];im[j]=0;
        }
        for(int len=2;len<=n;len*=2){
            for(int i=0;i<n;i+=len)for(int j=0;j<len/2;j++){
                int a=i+j,b=a+len/2,k=j*(n/len);
                double r=re[b]*ctab[k]-im[b]*stab[k],v=re[b]*stab[k]+im[b]*ctab[k];
                re[b]=re[a]-r;im[b]=im[a]-v;re[a]+=r;im[a]+=v;
            }
        }
        for(int k=0;k<=n/2;k++){
            double s=(k==0 || k==n/2?1:2)/windowSum;
            amplitude[k]+=(re[k]*re[k]+im[k]*im[k])*s*s/passes;
        }
    }
    return n;
}
