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
/** Shared display calculations, independent of Android for regression checks. */
final class ViewMath {
    static double timeAt(double x,double left,double width,double start,double span,double duration){
        if(width<=0||duration<=0)return 0;
        double fraction=Math.max(0,Math.min(1,(x-left)/width));return Math.max(0,Math.min(duration,start+fraction*span));
    }
    static double verticalWheel(double vertical,double horizontal,double generic){if(Double.isFinite(vertical)&&vertical!=0)return vertical;return horizontal==0&&Double.isFinite(generic)?generic:0;}
    static double visibleWheelStep(double ticks){if(!Double.isFinite(ticks)||ticks==0)return 0;return Math.copySign(Math.max(.25,Math.abs(ticks)),ticks);}
    // Emulator wheel-up is delivered as a downward content swipe.
    static double dragZoomTicks(double previousY,double currentY,double density,boolean emulator){return density>0?(emulator?currentY-previousY:previousY-currentY)/(24*density):0;}
    static double timeAnchor(double time,double start,double span){return Double.isFinite(time)&&time>=0&&span>0?Math.max(0,Math.min(1,(time-start)/span)):.5;}
    static double horizontalPinch(double previousX,double currentX,double minimum){return Double.isFinite(previousX)&&Double.isFinite(currentX)&&previousX>minimum&&currentX>minimum?Math.max(.5,Math.min(2,currentX/previousX)):1;}
    static final class Window {final double start,span;Window(double start,double span){this.start=start;this.span=span;}}
    /** Turn the page at its edge, leaving 10% context before the playback cursor. */
    static double followStart(double start,double span,double extent,double time,boolean enabled,boolean playing,boolean touching){
        if(!enabled||!playing||touching||span<=0||extent<=0||!Double.isFinite(time)||time<0)return start;
        time=Math.min(time,extent);
        if(time>=start&&time<start+span)return start;
        return Math.max(0,Math.min(Math.max(0,extent-span),time-span*.1));
    }
    static Window zoom(double start,double span,double minimum,double extent,double anchor,double ticks){
        anchor=Math.max(0,Math.min(1,anchor));minimum=Math.min(minimum,extent);double next=Math.max(minimum,Math.min(extent,span*Math.exp(-.18*Math.max(-12,Math.min(12,ticks)))));
        double position=Math.max(0,Math.min(extent-next,start+anchor*(span-next)));return new Window(position,next);
    }
    static final class Fit {final float scale,x,y;Fit(float s,float x,float y){scale=s;this.x=x;this.y=y;}}
    static Fit squareIcon(float width,float height){float size=Math.max(0,Math.min(width,height));return new Fit(size/24f,(width-size)/2,(height-size)/2);}
}
