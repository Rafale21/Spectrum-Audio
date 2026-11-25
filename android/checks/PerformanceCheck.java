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
import java.lang.management.ManagementFactory;
public class PerformanceCheck {
    public static void main(String[] args){
        int size=1024,rounds=2000;short[] pcm=new short[size];for(int i=0;i<size;i++)pcm[i]=(short)(12000*Math.sin(i*.11));Spectrum.Analyzer reused=new Spectrum.Analyzer(size,0);
        for(int i=0;i<300;i++){Spectrum.analyze(pcm,0);reused.analyze(pcm);}
        java.lang.management.ThreadMXBean metrics=ManagementFactory.getThreadMXBean();if(!(metrics instanceof com.sun.management.ThreadMXBean bean)||!bean.isThreadAllocatedMemorySupported()){System.out.println("SKIP: allocation counter unavailable on this JVM.");return;}if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().getId();
        long bytes=bean.getThreadAllocatedBytes(id),start=System.nanoTime();double sum=0;for(int i=0;i<rounds;i++)sum+=Spectrum.analyze(pcm,0)[5];long referenceTime=System.nanoTime()-start,referenceBytes=bean.getThreadAllocatedBytes(id)-bytes;
        bytes=bean.getThreadAllocatedBytes(id);start=System.nanoTime();for(int i=0;i<rounds;i++)sum+=reused.analyze(pcm)[5];long reusedTime=System.nanoTime()-start,reusedBytes=bean.getThreadAllocatedBytes(id)-bytes;
        if(!(sum>0)||reusedBytes>=referenceBytes)throw new AssertionError("allocation optimization not effective");
        System.out.printf(java.util.Locale.ROOT,"Host JVM benchmark (%d x FFT %d): reference %.1f ms / %d bytes; reused %.1f ms / %d bytes. Android timing not measured.%n",rounds,size,referenceTime/1e6,referenceBytes,reusedTime/1e6,reusedBytes);
    }
}
