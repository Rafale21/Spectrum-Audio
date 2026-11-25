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
public class LiveCheck {
    static void require(boolean v,String reason){if(!v)throw new AssertionError(reason);}
    public static void main(String[] args){
        for(int frame:new int[]{512,1024,2048,4096,8192})for(int method=0;method<3;method++)for(int hz:new int[]{2000,16000,24000})for(boolean log:new boolean[]{false,true}){
            int bins=(method==2?frame/4:frame/2)+1;double[] values=new double[bins];for(int i=0;i<bins;i++)values[i]=Math.abs(Math.sin(i*.19));
            LiveColumns columns=new LiveColumns();require(columns.configure(frame,bins,hz,15,log),"initial configuration");double[] actual=columns.add(values);
            for(int y=0;y<256;y++){double low=log?20*Math.pow(hz/20.0,1-(y+1)/256.0):hz*(1-(y+1)/256.0),high=log?20*Math.pow(hz/20.0,1-y/256.0):hz*(1-y/256.0);int a=Math.max(0,(int)(low/24000*(bins-1))),b=Math.min(bins-1,Math.max(a,(int)(high/24000*(bins-1))));double reference=0;for(int k=a;k<=b;k++)reference=Math.max(reference,values[k]);require(actual[y]==reference,"cached bin mapping must match previous renderer");}
            require(!columns.configure(frame,bins,hz,15,log),"no redundant bin rebuild");double[] output=columns.add(new double[bins]);for(double value:output)require(value==0,"next frame must not retain old peaks");
        }
        LiveColumns longView=new LiveColumns();longView.configure(512,257,16000,60,false);double[] loud=new double[257];java.util.Arrays.fill(loud,.8);
        require(longView.add(loud)==null,"512/60s groups two frames");double[] peak=longView.add(new double[257]);for(double value:peak)require(value==.8,"transient peaks preserved while grouping");require(Math.ceil(60/longView.step())<=4096,"full 60 seconds fit fixed history bitmap");
        longView.add(loud);longView.reset();require(longView.add(new double[257])==null,"clear discards pending transient");for(double value:longView.add(new double[257]))require(value==0,"clear resets peak buffer");
        System.out.println("PASS: live-bin parity at all FFT sizes/methods and frequency scales, buffer reset, 60s at 512 within 4096 columns, grouped transients preserved.");
    }
}
