#!/usr/bin/env python3
# This file is part of Spectrum Audio.
# Copyright (C) 2026 No Ads Studio and contributors.
# SPDX-License-Identifier: GPL-3.0-only
#
# Spectrum Audio is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, version 3 only.
# Spectrum Audio is distributed WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License in the repository root LICENSE file.

"""Exercise the independent M4A reader with real AAC/M4A files, without Android."""
import json, pathlib, subprocess, tempfile, numpy as np
project=pathlib.Path(__file__).resolve().parent.parent

def run(args):return subprocess.check_output(args,stderr=subprocess.PIPE)
with tempfile.TemporaryDirectory(prefix='spectre-mp4-') as tmp:
    tmp=pathlib.Path(tmp)
    for name,extra in [('standard',[]),('faststart',['-movflags','+faststart'])]:
        source=tmp/(name+'.m4a');adts=tmp/(name+'.aac')
        run(['ffmpeg','-v','error','-f','lavfi','-i','sine=frequency=1000:sample_rate=44100:duration=0.35','-c:a','aac','-b:a','128k',*extra,str(source)])
        actual=run(['java','-cp',str(project/'checks/classes'),'studio.noads.spectrumaudio.Mp4Check',str(source),str(adts)]).decode().splitlines()
        packets=json.loads(run(['ffprobe','-v','error','-show_packets','-select_streams','a','-of','json',str(source)]))['packets']
        assert len(actual)==len(packets),(name,'packet count')
        first=int(packets[0]['pts'])
        for found,expected in zip(actual,packets):
            pos,size,pts=map(int,found.split(','))
            assert (pos,size,pts)==(int(expected['pos']),int(expected['size']),int(expected['pts'])-first),(name,found,expected)
        pcm=run(['ffmpeg','-v','error','-i',str(adts),'-f','s16le','-ac','1','-ar','44100','-'])
        values=np.frombuffer(pcm,dtype='<i2').astype(float);window=values[2048:10240];freq=np.fft.rfftfreq(len(window),1/44100)[np.abs(np.fft.rfft(window*np.hanning(len(window)))).argmax()]
        assert abs(freq-1000)<10,(name,freq)
        # Both truncated input and bad sample/chunk tables must fail without excessive allocation.
        broken=tmp/'broken.m4a';broken.write_bytes(source.read_bytes()[:35])
        failed=subprocess.run(['java','-cp',str(project/'checks/classes'),'studio.noads.spectrumaudio.Mp4Check',str(broken),str(tmp/'bad.aac')],capture_output=True)
        assert failed.returncode!=0,'truncated M4A accepted'
        print(f'PASS: {name} M4A — offsets, sizes, timestamps match ffprobe; extracted AAC decodes to {freq:.1f} Hz; truncation rejected.')
