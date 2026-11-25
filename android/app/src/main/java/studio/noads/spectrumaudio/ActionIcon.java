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

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small vector icons: crisp at any screen density, independent of emoji fonts. */
final class ActionIcon extends Drawable {
    private final String name;private final Paint p=new Paint(3);private final int color;
    ActionIcon(String name,int color){this.name=name;this.color=color;}
    public void draw(Canvas canvas){canvas.save();Rect b=getBounds();ViewMath.Fit fit=ViewMath.squareIcon(b.width(),b.height());canvas.translate(b.left+fit.x,b.top+fit.y);canvas.scale(fit.scale,fit.scale);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(name){
        case "plus":canvas.drawLine(5,12,19,12,p);canvas.drawLine(12,5,12,19,p);break;
        case "minus":canvas.drawLine(5,12,19,12,p);break;
        case "camera":canvas.drawRoundRect(2,6,22,20,3,3,p);path(canvas,7,6,9,3,15,3,17,6);canvas.drawCircle(12,13,4,p);break;
        case "play":p.setStyle(Paint.Style.FILL);path(canvas,8,5,20,12,8,19);break;
        case "pause":p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(6,5,10,19,1,1,p);canvas.drawRoundRect(14,5,18,19,1,1,p);break;
        case "record":canvas.drawCircle(12,12,8,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(12,12,4,p);break;
        case "stop":p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(5,5,19,19,3,3,p);break;
        case "folder":path(canvas,3,7,3,5,10,5,12,7,21,7,21,19,3,19,3,7);break;
        case "fit":path(canvas,3,9,3,4,8,4);path(canvas,16,4,21,4,21,9);path(canvas,21,15,21,20,16,20);path(canvas,8,20,3,20,3,15);break;
        case "mic":canvas.drawRoundRect(9,3,15,14,3,3,p);path(canvas,6,11,6,13,8,17,12,18,16,17,18,13,18,11);path(canvas,12,18,12,22);path(canvas,8,22,16,22);break;
        case "settings":for(int y=5;y<=19;y+=7)canvas.drawLine(3,y,21,y,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(8,5,3,p);canvas.drawCircle(16,12,3,p);canvas.drawCircle(10,19,3,p);break;
        case "more":p.setStyle(Paint.Style.FILL);for(int y=5;y<=19;y+=7)canvas.drawCircle(12,y,1.7f,p);break;
        case "export":path(canvas,12,16,12,3);path(canvas,7,8,12,3,17,8);path(canvas,4,14,4,21,20,21,20,14);break;
        case "rotate":path(canvas,4,9,4,4,9,4);path(canvas,4,4,10,2,17,4,21,10);path(canvas,20,15,20,20,15,20);path(canvas,20,20,14,22,7,20,3,14);break;
        case "trash":path(canvas,3,6,21,6);path(canvas,9,3,15,3);path(canvas,6,6,7,21,17,21,18,6);break;
        default:canvas.drawCircle(12,12,9,p);canvas.drawLine(12,10,12,17,p);canvas.drawPoint(12,6,p);
        }canvas.restore();}
    private void path(Canvas c,float... values){Path path=new Path();path.moveTo(values[0],values[1]);for(int i=2;i<values.length;i+=2)path.lineTo(values[i],values[i+1]);if(p.getStyle()==Paint.Style.FILL)path.close();c.drawPath(path,p);}
    public int getIntrinsicWidth(){return 24;}public int getIntrinsicHeight(){return 24;}
    public void setAlpha(int alpha){p.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
