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

/** Spectral settings only: export, input, language and orientation stay independent. */
final class SpectrumPreset {
    final int fft,method,hz,duration,gain,range;final boolean adaptive,log,gray;
    SpectrumPreset(int fft,int method,int hz,int duration,boolean adaptive,boolean log,int gain,int range,boolean gray){this.fft=fft;this.method=method;this.hz=hz;this.duration=duration;this.adaptive=adaptive;this.log=log;this.gain=gain;this.range=range;this.gray=gray;}
    static SpectrumPreset defaults(){return new SpectrumPreset(1024,0,16000,15,true,false,0,100,false);}
    static SpectrumPreset hiddenImages(){return new SpectrumPreset(1024,0,16000,15,true,false,12,80,true);}
    String encode(){return "1|"+fft+"|"+method+"|"+hz+"|"+duration+"|"+(adaptive?1:0)+"|"+(log?1:0)+"|"+gain+"|"+range+"|"+(gray?1:0);}
    private static boolean oneOf(int n,int... allowed){for(int a:allowed)if(n==a)return true;return false;}
    static SpectrumPreset decode(String encoded){
        if(encoded==null)return null;
        try{String[] parts=encoded.split("\\|",-1);if(parts.length!=10)return null;int[] v=new int[10];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(parts[i]);
            if(v[0]!=1||!oneOf(v[1],512,1024,2048,4096,8192)||!oneOf(v[2],0,1,2)||!oneOf(v[3],2000,4000,8000,12000,16000,24000)||!oneOf(v[4],5,10,15,30,60)||!oneOf(v[5],0,1)||!oneOf(v[6],0,1)||!oneOf(v[7],0,6,12,24,36)||!oneOf(v[8],40,60,80,100,120)||!oneOf(v[9],0,1))return null;
            return new SpectrumPreset(v[1],v[2],v[3],v[4],v[5]==1,v[6]==1,v[7],v[8],v[9]==1);
        }catch(NumberFormatException invalid){return null;}
    }
}
