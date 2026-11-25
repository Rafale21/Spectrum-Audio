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
/* DSP reference copied unchanged from Spectrum Audio Windows 1.1, November 2026. */
#include <stdio.h>
#include "windows-dsp.h"
int main(void){
    int sizes[]={512,1024,2048,4096,8192};int16_t pcm[8192];
    for(int s=0;s<5;s++)for(int method=0;method<3;method++){
        int size=sizes[s];for(int i=0;i<size;i++)pcm[i]=(int16_t)((i*7919LL%26001)-13000);
        int n=spectrum(pcm,size,size/2,size,method);
        for(int k=0;k<=n/2;k++)printf("%d %d %d %.17g\n",size,method,k,amplitude[k]);
    }
    return 0;
}
