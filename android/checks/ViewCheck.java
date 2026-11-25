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
public class ViewCheck {
    static void equal(double a,double b){if(Math.abs(a-b)>1e-6)throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        equal(ViewMath.horizontalPinch(100,100,24),1); // pure vertical spread has unchanged horizontal span
        equal(ViewMath.horizontalPinch(100,150,24),1.5);equal(ViewMath.horizontalPinch(150,100,24),2.0/3);
        equal(ViewMath.horizontalPinch(10,20,24),1);equal(ViewMath.horizontalPinch(100,1000,24),2);equal(ViewMath.horizontalPinch(100,25,24),.5);equal(ViewMath.horizontalPinch(Double.NaN,100,24),1);
        equal(ViewMath.timeAt(258,58,400,60,20,180),70); // zoomed viewport, not entire file
        equal(ViewMath.timeAt(458,58,800,60,20,180),70); // same fraction in landscape
        equal(ViewMath.timeAt(58,58,400,60,20,180),60);
        equal(ViewMath.timeAt(458,58,400,60,20,180),80);
        equal(ViewMath.timeAt(458,58,400,175,20,180),180); // padded tail
        equal(ViewMath.timeAt(-100,58,400,60,20,180),60);
        for(float[] bounds:new float[][]{{240,24},{24,240},{24,24},{72,48},{48,72},{0,24}}){ViewMath.Fit fit=ViewMath.squareIcon(bounds[0],bounds[1]);float size=fit.scale*24;equal(fit.x+size/2,bounds[0]/2);equal(fit.y+size/2,bounds[1]/2);equal(size,Math.min(bounds[0],bounds[1]));}
        ViewMath.Window zoom=ViewMath.zoom(40,20,.02,120,.25,1);equal(zoom.start+.25*zoom.span,45);if(zoom.span>=20)throw new AssertionError("wheel up zooms in");
        ViewMath.Window undo=ViewMath.zoom(zoom.start,zoom.span,.02,120,.25,-1);equal(undo.start,40);equal(undo.span,20);
        // Toolbar zoom keeps the visible playback cursor fixed, even far from the center.
        for(double head:new double[]{40,41,45,59,60}){double anchor=ViewMath.timeAnchor(head,40,20);ViewMath.Window atCursor=ViewMath.zoom(40,20,.02,120,anchor,2);equal(atCursor.start+anchor*atCursor.span,head);ViewMath.Window back=ViewMath.zoom(atCursor.start,atCursor.span,.02,120,anchor,-2);equal(back.start,40);equal(back.span,20);}
        equal(ViewMath.timeAnchor(20,40,20),0);equal(ViewMath.timeAnchor(80,40,20),1);equal(ViewMath.timeAnchor(-1,40,20),.5);equal(ViewMath.timeAnchor(Double.NaN,40,20),.5);
        ViewMath.Window vertical=ViewMath.zoom(.2,.5,.002,1,.75,2);equal(vertical.start+.75*vertical.span,.575);
        ViewMath.Window full=ViewMath.zoom(0,120,.02,120,.9,-100);equal(full.start,0);equal(full.span,120);
        ViewMath.Window tiny=ViewMath.zoom(0,.02,.02,120,.5,100);equal(tiny.span,.02);equal(tiny.start,0);
        ViewMath.Window shortFile=ViewMath.zoom(0,.005,.02,.005,.5,1);equal(shortFile.span,.005);equal(shortFile.start,0);
        equal(ViewMath.verticalWheel(0,0,1),1);equal(ViewMath.verticalWheel(1,0,-1),1);equal(ViewMath.verticalWheel(0,1,1),0);equal(ViewMath.verticalWheel(Double.NaN,0,-1),-1);
        equal(ViewMath.visibleWheelStep(.001),.25);equal(ViewMath.visibleWheelStep(-.001),-.25);equal(ViewMath.visibleWheelStep(1),1);equal(ViewMath.visibleWheelStep(0),0);equal(ViewMath.visibleWheelStep(Double.NaN),0);
        double emulatorUp=ViewMath.dragZoomTicks(100,124,1,true),emulatorDown=ViewMath.dragZoomTicks(124,100,1,true);
        equal(emulatorUp,1);equal(emulatorDown,-1);equal(ViewMath.dragZoomTicks(100,148,2,true),1);equal(ViewMath.dragZoomTicks(124,100,1,false),1);
        ViewMath.Window wheelUp=ViewMath.zoom(40,20,.02,120,.25,emulatorUp);if(wheelUp.span>=20)throw new AssertionError("emulator wheel-up must zoom in");
        ViewMath.Window wheelDown=ViewMath.zoom(wheelUp.start,wheelUp.span,.02,120,.25,emulatorDown);equal(wheelDown.start,40);equal(wheelDown.span,20);
        ViewMath.Window smallWheel=ViewMath.zoom(0,120,.02,120,.5,ViewMath.visibleWheelStep(.001));if(smallWheel.span>115)throw new AssertionError("tiny wheel event not visible");
        // A page remains still until its edge; the next page retains the zoom.
        equal(ViewMath.followStart(40,20,180,59.99,true,true,false),40);
        equal(ViewMath.followStart(40,20,180,60,true,true,false),58);
        equal(ViewMath.followStart(58,20,180,60.3,true,true,false),58);
        equal(ViewMath.followStart(40,20,180,20,true,true,false),18); // restart/seek backward
        equal(ViewMath.followStart(160,20,180,180,true,true,false),160); // final page
        equal(ViewMath.followStart(0,180,180,180,true,true,false),0); // full view
        equal(ViewMath.followStart(0,.005,.005,.005,true,true,false),0); // tiny file
        equal(ViewMath.followStart(40,20,180,61,false,true,false),40); // disabled
        equal(ViewMath.followStart(40,20,180,61,true,false,false),40); // paused
        equal(ViewMath.followStart(40,20,180,61,true,true,true),40); // finger/mouse down
        equal(ViewMath.followStart(40,20,180,Double.NaN,true,true,false),40);
        equal(ViewMath.followStart(40,20,180,-1,true,true,false),40);
        // Skip multiple pages on a seek; never leave the cursor off-screen.
        for(double span:new double[]{.02,.3,5,20,180})for(double time:new double[]{0,.001,1,59.99,60,120,179.999,180}){
            double start=ViewMath.followStart(0,span,180,time,true,true,false);
            if(start<0||start+span>180+1e-9||time<start-1e-9||time>start+span+1e-9)throw new AssertionError("playhead outside followed page");
        }
        System.out.println("PASS: playback page edges, previous context, backward seek/restart, final/short/full pages, disabled and paused follow, gesture priority, deep zoom and large jumps.");
        System.out.println("PASS: seek within zoomed viewport, portrait/landscape mapping, bounds, and proportional centered icons in wide/tall/square bounds; anchored mouse wheel zoom, inverse wheel, frequency zoom and duration limits.");
    }
}
