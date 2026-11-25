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
public class PresetCheck {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        SpectrumPreset defaults=SpectrumPreset.defaults(),images=SpectrumPreset.hiddenImages();
        check(defaults.fft==1024&&defaults.method==0&&defaults.hz==16000&&defaults.duration==15&&defaults.adaptive&&!defaults.log&&defaults.gain==0&&defaults.range==100&&!defaults.gray);
        check(images.fft==1024&&images.hz==16000&&images.method==0&&images.adaptive&&!images.log&&images.gray&&images.gain==12&&images.range==80);
        SpectrumPreset custom=new SpectrumPreset(512,2,24000,60,false,true,36,120,true);
        String[] stored={defaults.encode(),images.encode(),custom.encode()};
        for(String saved:stored){SpectrumPreset recalled=SpectrumPreset.decode(saved);check(recalled!=null&&recalled.encode().equals(saved));}
        // Replacing one slot does not affect the other saved configurations.
        String first=stored[0],third=stored[2];stored[1]=new SpectrumPreset(4096,1,4000,5,true,true,6,40,false).encode();
        check(stored[0].equals(first)&&stored[2].equals(third));check(SpectrumPreset.decode(stored[1]).hz==4000);
        for(String invalid:new String[]{null,"","1|1024","2|1024|0|16000|15|1|0|12|80|1","1|1234|0|16000|15|1|0|12|80|1","1|1024|3|16000|15|1|0|12|80|1","1|1024|0|99999|15|1|0|12|80|1","1|1024|0|16000|15|2|0|12|80|1","1|1024|0|16000|15|1|0|13|80|1","1|1024|0|16000|15|1|0|12|80|1|","bad"})check(SpectrumPreset.decode(invalid)==null);
        System.out.println("PASS: 1024/16kHz hidden preset, initial spectrum defaults, all-field preset persistence, three independent slots and malformed data rejected.");
    }
}
