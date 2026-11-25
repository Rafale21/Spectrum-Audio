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
import java.util.Arrays;

/** Cached frequency bins and peak-preserving temporal grouping for the fixed bitmap. */
final class LiveColumns {
    private final int[] low=new int[256],high=new int[256];
    private final double[] peaks=new double[256];
    private int frame,bins,hz,group,pending;private boolean log;
    boolean configure(int frame,int bins,int hz,int seconds,boolean log){
        int group=Math.max(1,(int)Math.ceil(seconds*48000.0/(frame*4096.0)));
        if(this.frame==frame&&this.bins==bins&&this.hz==hz&&this.group==group&&this.log==log)return false;
        this.frame=frame;this.bins=bins;this.hz=hz;this.group=group;this.log=log;reset();
        for(int y=0;y<256;y++){
            double a=frequency(1-(y+1)/256.0),b=frequency(1-y/256.0);
            low[y]=Math.max(0,(int)(a/24000*(bins-1)));high[y]=Math.min(bins-1,Math.max(low[y],(int)(b/24000*(bins-1))));
        }
        return true;
    }
    private double frequency(double fraction){return log?20*Math.pow(hz/20.0,fraction):fraction*hz;}
    double step(){return frame*group/48000.0;}
    void reset(){pending=0;Arrays.fill(peaks,0);}
    /** Borrowed output, consumed before the next call. Null until a grouped column is ready. */
    double[] add(double[] spectrum){
        if(pending==0)Arrays.fill(peaks,0);
        for(int y=0;y<256;y++)for(int k=low[y];k<=high[y];k++)peaks[y]=Math.max(peaks[y],spectrum[k]);
        if(++pending<group)return null;pending=0;return peaks;
    }
}
