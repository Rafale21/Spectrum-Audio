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
import static studio.noads.spectrumaudio.I18n.t;
import android.content.Context;
import android.graphics.*;
import android.view.*;

public final class SpectrogramView extends View {
    private final Bitmap history=Bitmap.createBitmap(4096,256,Bitmap.Config.ARGB_8888);
    private final LiveColumns liveColumns=new LiveColumns();private final int[] liveColors=new int[256];
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    // No bilinear blending between measured spectral cells, including PNG exports.
    private final Paint spectralPaint=new Paint();
    private FileAnalysis.Result file; private Bitmap fileImage;
    private double playhead=-1;
    private final CursorVisibility cursorVisibility=new CursorVisibility();
    private final Runnable hideCursor=()->{synchronized(this){invalidate();}};
    synchronized void userActivity(){if(file==null||disposed)return;cursorVisibility.show(android.os.SystemClock.uptimeMillis());removeCallbacks(hideCursor);if(!cursorVisibility.playing())postDelayed(hideCursor,CursorVisibility.DELAY_MS);invalidate();}
    synchronized void cursorTouch(int action){if(file==null||disposed)return;if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_POINTER_DOWN){cursorVisibility.touch(true,android.os.SystemClock.uptimeMillis());userActivity();}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){cursorVisibility.touch(false,android.os.SystemClock.uptimeMillis());userActivity();}}

    private final java.util.concurrent.ScheduledExecutorService details=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private java.util.concurrent.Future<?> detailJob;private int detailGeneration;private boolean disposed,adaptive=true;private String detailStatus="";
    private Bitmap detailImage;private AdaptiveSpectrum.Tile detailTile;private int analysisFft=1024,analysisMethod=0,gain=0,range=100;private boolean gray=false;
    private double frequencyLow=0,frequencyHigh=1;private float downY,touchY;
    synchronized String detailDescription(){return detailStatus;}
    synchronized void languageChanged(){setContentDescription(t("Spectrogramme : temps horizontal, fréquences verticales, intensité en couleurs"));requestDetail();invalidate();}
    synchronized void analysisSettings(int fft,int method,boolean enabled,int gain,int range,boolean gray){analysisFft=fft;analysisMethod=method;adaptive=enabled;this.gain=gain;this.range=range;this.gray=gray;detailImage=null;detailTile=null;if(file!=null)renderFile();else clear();requestDetail();invalidate();}
    private void cancelDetail(){detailGeneration++;if(detailJob!=null)detailJob.cancel(true);}
    synchronized void dispose(){disposed=true;removeCallbacks(hideCursor);cursorVisibility.clear();cancelDetail();details.shutdownNow();if(file!=null)file.close();file=null;detailImage=null;detailTile=null;}
    private synchronized void requestDetail(){
        cancelDetail();if(disposed||file==null||file.pcm==null||!adaptive){detailStatus=file==null?"":t("Aperçu");detailImage=null;detailTile=null;return;}
        final int generation=detailGeneration,g=gain,r=range;final boolean bw=gray;
        final AdaptiveSpectrum.Request request=new AdaptiveSpectrum.Request(file.pcm,file.rate,(int)plotWidth(),(int)plotHeight(),analysisFft,analysisMethod,fileStart,fileSpan,freq(0),freq(1),logarithmic);
        detailStatus=t("Affinage…");
        detailJob=details.schedule(()->{try{AdaptiveSpectrum.Tile tile=AdaptiveSpectrum.render(request);int[] pixels=tile.pixels(g,r,bw);if(Thread.currentThread().isInterrupted())return;Bitmap image=Bitmap.createBitmap(pixels,request.width,request.height,Bitmap.Config.ARGB_8888);
            post(()->{synchronized(SpectrogramView.this){if(disposed||generation!=detailGeneration)return;detailTile=tile;detailImage=image;detailStatus=String.format(I18n.locale(),"HD · FFT %d · %.1f Hz",tile.fft,tile.frequencyStep);invalidate();}});
        }catch(java.io.InterruptedIOException ignored){}catch(Exception error){post(()->{synchronized(SpectrogramView.this){if(!disposed&&generation==detailGeneration){detailStatus=t("Détail indisponible");setContentDescription(t("Affinage : ")+AudioBuffers.error(error));invalidate();}}});}},220,java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    @Override protected synchronized void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(file!=null)requestDetail();}

    synchronized double fileDuration(){return file==null?0:file.duration;}
    synchronized void playbackState(boolean playing){
        if(disposed||file==null||!cursorVisibility.playback(playing,android.os.SystemClock.uptimeMillis()))return;
        removeCallbacks(hideCursor);if(!playing)postDelayed(hideCursor,CursorVisibility.DELAY_MS);invalidate();
    }
    synchronized void followPlayback(boolean enabled){followPlayback=enabled;}
    // Seeking/previewing a position never scrolls the viewport on its own.
    synchronized void playbackTime(double time){if(playhead==time)return;playhead=time;invalidate();}
    synchronized void playbackTick(double time){
        if(disposed||file==null)return;
        time=Math.max(0,Math.min(file.duration,time));
        double start=ViewMath.followStart(fileStart,fileSpan,file.duration,time,followPlayback,cursorVisibility.playing(),plotTouching);
        if(start==fileStart&&playhead==time)return;
        if(start!=fileStart){fileStart=start;requestDetail();}
        playhead=time;invalidate();
    }
    private double fileStart=0,fileSpan=1; private float touchX; private boolean scaling,plotTouching,followPlayback=true;
    private final ScaleGestureDetector zoom;
    private final GestureDetector gestures;private final int touchSlop;
    interface OnSeekListener {void seek(double seconds);}
    private OnSeekListener seekListener;private float downX;private boolean dragging;private boolean suppressMouse,mouseGesture,emulatorWheel;private int dragAxis;
    synchronized void emulatorWheel(boolean enabled){emulatorWheel=enabled;}
    synchronized void setOnSeekListener(OnSeekListener listener){seekListener=listener;}
    private synchronized void seekAt(float x,float y){float density=getResources().getDisplayMetrics().density;if(file==null||scaling||x<plotLeft()||x>plotLeft()+plotWidth()||y<plotTop()||y>plotBottom())return;double time=ViewMath.timeAt(x,plotLeft(),plotWidth(),fileStart,fileSpan,file.duration);playhead=time;userActivity();invalidate();if(seekListener!=null)seekListener.seek(time);}
    private int cursor=0,count=0; private double step=1024.0/48000;
    private int maxHz=16000,seconds=15; private boolean logarithmic=false;
    public SpectrogramView(Context c){super(c);spectralPaint.setFilterBitmap(false);spectralPaint.setAntiAlias(false);spectralPaint.setDither(false);touchSlop=ViewConfiguration.get(c).getScaledTouchSlop();setBackgroundColor(Color.BLACK);setFocusable(true);setFocusableInTouchMode(true);
        zoom=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            public boolean onScale(ScaleGestureDetector detector){synchronized(SpectrogramView.this){if(file==null)return false;
                float minimum=24*getResources().getDisplayMetrics().density;
                double sx=ViewMath.horizontalPinch(detector.getPreviousSpanX(),detector.getCurrentSpanX(),minimum);
                scaling=true;userActivity();if(sx==1)return true;
                double anchor=Math.max(0,Math.min(1,(detector.getFocusX()-plotLeft())/plotWidth()));double time=fileStart+anchor*fileSpan;
                fileSpan=Math.max(Math.min(file.duration,.02),Math.min(file.duration,fileSpan/Math.max(.5,Math.min(2,sx))));fileStart=time-anchor*fileSpan;clamp();
                scaling=true;requestDetail();invalidate();return true;}}});
        gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public boolean onSingleTapConfirmed(MotionEvent e){seekAt(e.getX(),e.getY());return true;}public boolean onDoubleTap(MotionEvent e){resetFile();return true;}});
        history.eraseColor(Color.BLACK);setContentDescription(t("Spectrogramme : temps horizontal, fréquences verticales, intensité en couleurs"));}
    public synchronized void configure(int hz,int duration,boolean log){if(hz!=maxHz||log!=logarithmic)clear();maxHz=hz;seconds=duration;logarithmic=log;if(file!=null){renderFile();requestDetail();}invalidate();}
    public synchronized void clear(){if(file!=null){resetFile();return;}count=cursor=0;liveColumns.reset();history.eraseColor(Color.BLACK);postInvalidate();}
    public synchronized void add(double[] amplitude,int frameSize) {
        if(disposed||file!=null)return;
        if(liveColumns.configure(frameSize,amplitude.length,maxHz,seconds,logarithmic)){clear();step=liveColumns.step();}
        double[] powers=liveColumns.add(amplitude);if(powers==null)return;
        for(int y=0;y<256;y++)liveColors[y]=powers[y]==0?Color.BLACK:color((float)(20*Math.log10(Math.max(1e-6,powers[y]))));
        history.setPixels(liveColors,0,1,cursor,0,1,256);cursor=(cursor+1)%4096;count=Math.min(4096,count+1);postInvalidateOnAnimation();
    }
    synchronized Bitmap snapshot(){if(getWidth()<=0||getHeight()<=0)throw new IllegalStateException(t("Spectrogramme non affiché."));Bitmap image=Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.BLACK);draw(canvas);return image;}
    synchronized boolean hasFile(){return file!=null;}
    synchronized void showFile(FileAnalysis.Result result){cancelDetail();if(file!=null)file.close();file=result;detailImage=null;detailTile=null;frequencyLow=0;frequencyHigh=1;playhead=-1;plotTouching=false;cursorVisibility.clear();userActivity();fileStart=0;fileSpan=result.duration;renderFile();requestDetail();invalidate();}
    synchronized void live(){removeCallbacks(hideCursor);cursorVisibility.clear();cancelDetail();if(file!=null)file.close();detailImage=null;detailTile=null;detailStatus="";frequencyLow=0;frequencyHigh=1;file=null;fileImage=null;playhead=-1;plotTouching=false;clear();}
    private float plotLeft(){return Math.round(58*getResources().getDisplayMetrics().density);}
    private float plotWidth(){return Math.max(1,Math.round(getWidth()-14*getResources().getDisplayMetrics().density)-plotLeft());}
    private float plotTop(){return 3f;}
    private float plotBottom(){return Math.round(getHeight()-16*getResources().getDisplayMetrics().density-3f);}
    private float plotHeight(){return Math.max(1,plotBottom()-plotTop());}
    private void clamp(){fileStart=Math.max(0,Math.min(Math.max(0,file.duration-fileSpan),fileStart));}
    synchronized void resetFile(){if(file!=null){userActivity();fileStart=0;fileSpan=file.duration;frequencyLow=0;frequencyHigh=1;requestDetail();invalidate();}}
    private void renderFile(){
        int width=file.levels.length;fileImage=Bitmap.createBitmap(width,256,Bitmap.Config.ARGB_8888);int[] pixels=new int[width*256];
        for(int y=0;y<256;y++){double low=baseFreq(1-(y+1)/256.0),high=baseFreq(1-y/256.0);int a=(int)(low/(file.rate/2.0)*512),b=(int)(high/(file.rate/2.0)*512);
            for(int x=0;x<width;x++){int value=0;for(int k=Math.max(0,a);k<=Math.min(511,b);k++)value=Math.max(value,file.levels[x][k]&255);pixels[y*width+x]=value==0?Color.BLACK:color(value/2.55f-100);}}
        fileImage.setPixels(pixels,0,width,0,0,width,256);
    }
    private boolean insidePlot(float x,float y){float density=getResources().getDisplayMetrics().density;return x>=plotLeft()&&x<=plotLeft()+plotWidth()&&y>=plotTop()&&y<=plotBottom();}
    @Override public synchronized PointerIcon onResolvePointerIcon(MotionEvent event,int pointerIndex){return PointerIcon.getSystemIcon(getContext(),file!=null&&insidePlot(event.getX(),event.getY())?(dragging?PointerIcon.TYPE_GRABBING:PointerIcon.TYPE_CROSSHAIR):PointerIcon.TYPE_DEFAULT);}
    synchronized void zoomTime(double ticks){if(file==null)return;zoomAt(ViewMath.timeAnchor(playhead,fileStart,fileSpan),ticks);}
    private void zoomAt(double anchor,double ticks){userActivity();ViewMath.Window view=ViewMath.zoom(fileStart,fileSpan,.02,file.duration,anchor,ticks);if(view.start==fileStart&&view.span==fileSpan)return;fileStart=view.start;fileSpan=view.span;requestDetail();invalidate();}
    @Override public synchronized boolean onTouchEvent(MotionEvent event){return handleTouch(event);}
    private synchronized boolean handleTouch(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_SCROLL)return handleWheel(event,event.getX(),event.getY());
        return handleGesture(event);
    }
    @Override public synchronized boolean onGenericMotionEvent(MotionEvent event){if(handleWheel(event,event.getX(),event.getY()))return true;return super.onGenericMotionEvent(event);}
    synchronized boolean handleWheel(MotionEvent event,float x,float y){
        if(file==null||event.getActionMasked()!=MotionEvent.ACTION_SCROLL||!insidePlot(x,y))return false;
        double horizontal=event.getAxisValue(MotionEvent.AXIS_HSCROLL);if(!Double.isFinite(horizontal))horizontal=0;
        double vertical=ViewMath.verticalWheel(event.getAxisValue(MotionEvent.AXIS_VSCROLL),horizontal,event.getAxisValue(MotionEvent.AXIS_SCROLL));if(vertical==0&&horizontal==0)return false;
        userActivity();requestFocus();scaling=false;double oldStart=fileStart,oldSpan=fileSpan,oldLow=frequencyLow,oldHigh=frequencyHigh;
        if((event.getMetaState()&KeyEvent.META_CTRL_ON)!=0){fileStart+=(horizontal-vertical)*fileSpan*.12;clamp();}
        else {
            if(vertical!=0){ViewMath.Window view=ViewMath.zoom(fileStart,fileSpan,.02,file.duration,(x-plotLeft())/plotWidth(),ViewMath.visibleWheelStep(vertical));fileStart=view.start;fileSpan=view.span;}
            if(horizontal!=0){fileStart+=horizontal*fileSpan*.12;clamp();}
        }
        if(oldStart!=fileStart||oldSpan!=fileSpan||oldLow!=frequencyLow||oldHigh!=frequencyHigh){requestDetail();invalidate();}return true;
    }
    private synchronized boolean handleGesture(MotionEvent event){
        if(file==null)return super.onTouchEvent(event);cursorTouch(event.getActionMasked());
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)plotTouching=true;
        else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)plotTouching=false;
        boolean mouse=event.isFromSource(InputDevice.SOURCE_MOUSE)||event.getToolType(0)==MotionEvent.TOOL_TYPE_MOUSE;
        if(mouse){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){requestFocus();suppressMouse=(event.getButtonState()&(MotionEvent.BUTTON_SECONDARY|MotionEvent.BUTTON_TERTIARY))!=0;}if(suppressMouse){if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)suppressMouse=false;return true;}}
        getParent().requestDisallowInterceptTouchEvent(true);zoom.onTouchEvent(event);gestures.onTouchEvent(event);
        switch(event.getActionMasked()){case MotionEvent.ACTION_DOWN:touchY=downY=event.getY();touchX=downX=event.getX();scaling=false;dragging=false;dragAxis=0;mouseGesture=mouse||emulatorWheel;break;
        case MotionEvent.ACTION_POINTER_DOWN:scaling=true;break;
        case MotionEvent.ACTION_MOVE:if(event.getPointerCount()==1&&!zoom.isInProgress()&&!scaling){if(!dragging&&Math.hypot(event.getX()-downX,event.getY()-downY)>touchSlop){dragging=true;touchX=downX;touchY=downY;dragAxis=Math.abs(event.getY()-downY)>Math.abs(event.getX()-downX)?2:1;}if(dragging){if(mouseGesture&&dragAxis==2){zoomAt((downX-plotLeft())/plotWidth(),ViewMath.dragZoomTicks(touchY,event.getY(),getResources().getDisplayMetrics().density,emulatorWheel));}else{fileStart-=(event.getX()-touchX)/plotWidth()*fileSpan;clamp();requestDetail();invalidate();}}}touchX=event.getX();touchY=event.getY();break;
        case MotionEvent.ACTION_POINTER_UP:scaling=true;break;
        case MotionEvent.ACTION_UP:dragging=false;performClick();getParent().requestDisallowInterceptTouchEvent(false);break;
        case MotionEvent.ACTION_CANCEL:dragging=false;getParent().requestDisallowInterceptTouchEvent(false);break;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private double baseFreq(double f){return logarithmic?20*Math.pow(maxHz/20.0,f):f*maxHz;}
    private double freq(double f){return baseFreq(frequencyLow+f*(frequencyHigh-frequencyLow));}
    private float fraction(double f){double base=logarithmic?Math.log(Math.max(20,f)/20)/Math.log(maxHz/20.0):f/maxHz;return (float)((base-frequencyLow)/(frequencyHigh-frequencyLow));}
    private int color(float db){return ColorMap.color(db,gain,range,gray);}
    private void drawFile(Canvas c,float left,float top,float right,float bottom,float d){
        c.save();c.clipRect(left,top,right,bottom);
        float unit=(float)((right-left)*file.step/fileSpan);float x=left-(float)(fileStart/fileSpan*(right-left));
        float fullTop=bottom-(float)((1-frequencyLow)/(frequencyHigh-frequencyLow))*(bottom-top),fullBottom=bottom+(float)(frequencyLow/(frequencyHigh-frequencyLow))*(bottom-top);
        c.drawBitmap(fileImage,null,new RectF(x,fullTop,x+unit*file.levels.length,fullBottom),spectralPaint);
        if(detailImage!=null&&detailTile!=null){AdaptiveSpectrum.Request view=detailTile.view;float dx=left+(float)((view.start-fileStart)/fileSpan)*(right-left),end=left+(float)((view.start+view.span-fileStart)/fileSpan)*(right-left);float dy=bottom-fraction(view.high)*(bottom-top),endY=bottom-fraction(view.low)*(bottom-top);c.drawBitmap(detailImage,null,new RectF(dx,dy,end,endY),spectralPaint);}
        if(cursorVisibility.visible(android.os.SystemClock.uptimeMillis())&&playhead>=fileStart&&playhead<=fileStart+fileSpan){float px=left+(float)((playhead-fileStart)/fileSpan)*(right-left);p.setColor(0xFFE9FFF9);p.setStrokeWidth(1.5f*d);c.drawLine(px,top,px,bottom,p);}c.restore();
        p.setTextSize(10*d);p.setColor(0xFFABC0D5);
        for(int i=0;i<=4;i++){double f=freq(i/4.0);float y=bottom-(bottom-top)*i/4;c.drawText(f>=1000?String.format(I18n.locale(),"%.1f k",f/1000):""+(int)f,3*d,Math.max(10*d,y+4*d),p);double t=fileStart+fileSpan*i/4;c.drawText(String.format(I18n.locale(),"%.2f s",t),Math.min(right-44*d,left+(right-left)*i/4),bottom+12*d,p);}
    }
    protected synchronized void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density;
        float left=plotLeft(),top=plotTop(),right=left+plotWidth(),bottom=plotBottom();
        if(right<=left||bottom<=top)return;
        p.setColor(Color.BLACK);c.drawRect(left,top,right,bottom,p);
        if(file!=null){drawFile(c,left,top,right,bottom,d);return;}
        int wanted=Math.max(1,(int)Math.round(seconds/step)),visible=Math.min(count,wanted),start=(cursor-visible+4096)%4096;
        int first=Math.min(visible,4096-start);float unit=(right-left)/wanted,x=right-visible*unit;
        if(first>0)c.drawBitmap(history,new Rect(start,0,start+first,256),new RectF(x,top,x+first*unit,bottom),spectralPaint);
        if(visible>first)c.drawBitmap(history,new Rect(0,0,visible-first,256),new RectF(x+first*unit,top,right,bottom),spectralPaint);
        p.setTextSize(10*d);p.setStrokeWidth(d);
        int[] ticks=logarithmic?new int[]{20,100,500,1000,5000,10000,24000}:new int[]{0,maxHz/4,maxHz/2,3*maxHz/4,maxHz};
        for(int f:ticks){if(f>maxHz)continue;float y=bottom-fraction(f)*(bottom-top);p.setColor(0x304C6686);c.drawLine(left,y,right,y,p);p.setColor(0xFFABC0D5);c.drawText(f>=1000?String.format(I18n.locale(),"%.1f k",f/1000f):""+f,3*d,Math.max(10*d,y+4*d),p);}
        for(int i=0;i<=4;i++){float tx=left+(right-left)*i/4;p.setColor(0x254C6686);c.drawLine(tx,top,tx,bottom,p);p.setColor(0xFFABC0D5);c.drawText(i==4?"0 s":"−"+(seconds*(4-i)/4)+" s",Math.min(tx,right-22*d),bottom+12*d,p);}
        if(count==0){p.setColor(0xFFABC0D5);p.setTextSize(14*d);c.drawText(t("Appuyez sur Démarrer"),left+14*d,top+38*d,p);}
    }
}
