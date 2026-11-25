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
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String DONATION_URL="https://ko-fi.com/noadsstudio";
    private SpectrogramView graph;private TextView status,fileHint,appTitle;private LinearLayout header,headerActions;private ImageButton play,record,fit,open,cancelRecord,capture,microphone,zoomIn,zoomOut;
    private SeekBar seek;private TextView clock;private LinearLayout transport;private boolean seeking,seekPending;private long seekDeadline;
    private MediaPlayer player;private boolean playerReady,playerPreparing;private int seekPosition;
    private String viewedSource="";private File audioSource;private android.media.AudioFocusRequest focusRequest;
    private FrameLayout footer;private LinearLayout root,controls;private Thread importer;private int importGeneration;private boolean loading;private String fileName="";private ProgressDialog importDialog;
    private volatile boolean running=false,paused=false;private volatile int fftSize=1024,method=0;
    private Thread worker;private AudioRecord mic;private final Object fileLock=new Object();private WavFile wav;
    private final java.util.concurrent.ExecutorService saves=java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean emulatorWheel,followPlayback=true;
    private int orientationMode=OrientationMode.AUTO;private int exportFormat=RecordingFormat.DEFAULT,recordFormat=RecordingFormat.DEFAULT,pendingSaves=0;private boolean destroyed=false;
    private File recording,lastFile,exportFile;private long recordedSamples;private volatile long frames;
    private boolean adaptive=true,gray=false;private int displayGain=0,displayRange=100;
    private int maxHz=16000,duration=15;private boolean log=false;private int pendingAction;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){refresh();ui.postDelayed(this,300);}};
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    public void onCreate(Bundle b){super.onCreate(b);setVolumeControlStream(AudioManager.STREAM_MUSIC);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);android.content.SharedPreferences prefs=getPreferences(0);if(!prefs.getBoolean("defaults118",false)){StartupDefaults upgraded=new StartupDefaults(prefs.getInt("fft",2048),prefs.getInt("hz",12000),prefs.getInt("exportFormat",0),false);prefs.edit().putInt("fft",upgraded.fft).putInt("hz",upgraded.hz).putInt("exportFormat",upgraded.format).putBoolean("defaults118",true).apply();}I18n.load(this,prefs.getString("language","auto"));emulatorWheel=prefs.getBoolean("emulatorWheel",false);followPlayback=prefs.getBoolean("followPlayback",true);orientationMode=OrientationMode.valid(prefs.getInt("orientationMode",OrientationMode.AUTO));fftSize=prefs.getInt("fft",1024);method=prefs.getInt("method",0);maxHz=prefs.getInt("hz",16000);duration=prefs.getInt("duration",15);log=prefs.getBoolean("log",false);exportFormat=RecordingFormat.valid(prefs.getInt("exportFormat",RecordingFormat.DEFAULT));adaptive=prefs.getBoolean("adaptive",true);gray=prefs.getBoolean("gray",false);displayGain=prefs.getInt("gain",0);displayRange=prefs.getInt("range",100);build();graph.emulatorWheel(emulatorWheel);graph.followPlayback(followPlayback);graph.analysisSettings(fftSize,method,adaptive,displayGain,displayRange,gray);graph.configure(maxHz,duration,log);applyOrientation();}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xFFE9F2FA);return t;}
    private android.graphics.drawable.GradientDrawable shape(int color,int border,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    private android.graphics.drawable.Drawable surface(int color,int border){return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FF9A3C),shape(color,border,16),null);}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(0xFFE9F2FA);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(8),dp(8),dp(8),dp(8));b.setMaxLines(1);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setBackground(surface(0xFF13171B,0xFF242B31));b.setOnClickListener(v->{if(graph!=null)graph.userActivity();action.run();});return b;}
    private void graphic(ImageButton b,String label,String icon,boolean selected,boolean danger){String key=label+icon+selected+danger+b.isEnabled();if(key.equals(b.getTag()))return;b.setTag(key);b.setContentDescription(label);b.setTooltipText(label);b.setImageDrawable(new ActionIcon(icon,danger?0xFFFF6475:selected?0xFFFF9A3C:0xFFD7DFE7));b.setBackground(surface(danger?0xFF321016:selected?0xFF2B1A0D:0xFF13171B,danger?0xFFFF475D:selected?0xFF8A4A1B:0xFF242B31));b.setAlpha(b.isEnabled()?1:.38f);}
    private ImageButton iconButton(String label,String icon,Runnable action){ImageButton button=new ImageButton(this);button.setImageDrawable(new ActionIcon(icon,0xFFD7DFE7));button.setPadding(dp(12),dp(12),dp(12),dp(12));button.setScaleType(ImageView.ScaleType.FIT_CENTER);button.setBackground(surface(0xFF13171B,0xFF242B31));button.setContentDescription(label);button.setTooltipText(label);button.setOnClickListener(v->{if(graph!=null)graph.userActivity();action.run();});return button;}
    private void cell(LinearLayout row,View child,int height){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(height),1);lp.setMargins(dp(1),0,dp(1),0);row.addView(child,lp);}
    private AlertDialog showDialog(AlertDialog.Builder builder){AlertDialog dialog=builder.create();dialog.setOnShowListener(v->{if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(0xFF121619,0xFF293138,24));if(dialog.getButton(-1)!=null)dialog.getButton(-1).setTextColor(0xFFFF9A3C);if(dialog.getButton(-2)!=null)dialog.getButton(-2).setTextColor(0xFFADB9C5);});dialog.show();return dialog;}
    private void menu(){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(8),dp(16),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(box);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(t("Outils")).setView(scroll).create();
        addMenu(box,t("Exporter la vue en PNG"),"camera",()->{dialog.dismiss();captureImage();});
        addMenu(box,t("Mes enregistrements"),"folder",()->{dialog.dismiss();recordings();});
        addMenu(box,t("Exporter le dernier enregistrement"),"export",()->{dialog.dismiss();exportWav();}).setEnabled(lastFile!=null&&wav==null);
        addMenu(box,"Langue / Language","settings",()->{dialog.dismiss();languages();});
        addMenu(box,t("Orientation : ")+t(OrientationMode.label(orientationMode)),"rotate",()->{dialog.dismiss();chooseOrientation();});
        addMenu(box,t("Effacer / réinitialiser la vue"),"trash",()->{dialog.dismiss();graph.clear();});
        addMenu(box,t("Formats et gestes"),"info",()->{dialog.dismiss();showDialog(new AlertDialog.Builder(this).setTitle("Spectrum Audio · 1.21").setMessage(t("Formats : M4A/AAC, MP3, WAV, FLAC, OGG, Opus, AMR et audio MP4, selon les codecs du téléphone.\n\nClic / appui : placer le curseur. Glisser : déplacer. Pincer : zoomer. Double appui : tout voir.\n\n− / + : zoom temporel autour du curseur de lecture. Molette : zoom temporel sous le pointeur. Ctrl + molette : déplacement.\n\nSi l’émulateur transforme la molette en gestes tactiles, activez « Mode molette émulateur » dans les réglages. Le glissement vertical zoome alors dans le temps.\n\nLa capture et la lecture s’arrêtent en arrière-plan.")).setPositiveButton(t("Compris"),null));});
        dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(0xFF121619,0xFF293138,24));}
    private Button addMenu(LinearLayout row,String label,String icon,Runnable action){Button b=button(label,action);ActionIcon d=new ActionIcon(icon,0xFFFF9A3C);d.setBounds(0,0,dp(20),dp(20));b.setCompoundDrawables(d,null,null,null);b.setCompoundDrawablePadding(dp(12));b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(4),0,0);row.addView(b,lp);return b;}
    private void chooseOrientation(){String[] labels={t(OrientationMode.label(0)),t(OrientationMode.label(1)),t(OrientationMode.label(2))};showDialog(new AlertDialog.Builder(this).setTitle(t("Orientation de l’écran")).setSingleChoiceItems(labels,orientationMode,(dialog,which)->{orientationMode=OrientationMode.valid(which);getPreferences(0).edit().putInt("orientationMode",orientationMode).apply();applyOrientation();dialog.dismiss();}).setNegativeButton(t("Fermer"),null));}
    private void languages(){String[] labels=I18n.NAMES.clone();labels[0]=I18n.t("Langue du téléphone");int selected=I18n.index(getPreferences(0).getString("language","auto"));showDialog(new AlertDialog.Builder(this).setTitle("Langue / Language").setSingleChoiceItems(labels,selected,(dialog,which)->{String code=I18n.CODES[which];getPreferences(0).edit().putString("language",code).apply();I18n.load(this,code);dialog.dismiss();relabel();}).setNegativeButton(t("Fermer"),null));}
    private void relabel(){SpectrogramView previous=graph;if(previous.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)previous.getParent()).removeView(previous);build();graph.languageChanged();graph.emulatorWheel(emulatorWheel);seek.setProgress((int)(seekPosition/Math.max(1,graph.fileDuration()*1000)*10000));refresh();}
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(8),1,dp(8),1);root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(8)+bars.left,1+bars.top,dp(8)+bars.right,1+bars.bottom);return insets;});
        // WindowInsets.Type is API 30+: Android 8–10 use the legacy fitting behavior.
        if(Build.VERSION.SDK_INT<30){root.setOnApplyWindowInsetsListener(null);root.setFitsSystemWindows(true);}
        setContentView(root);
        header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("SPECTRUM",17);appTitle=title;title.setTypeface(null,android.graphics.Typeface.BOLD);title.setLetterSpacing(.12f);title.setTextColor(0xFFFF9A3C);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,dp(34),1));
        LinearLayout actions=new LinearLayout(this);headerActions=actions;actions.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);header.addView(actions,new LinearLayout.LayoutParams(0,dp(34),1));
        ImageButton settingsButton=iconButton(t("Réglages"),"settings",this::settings);settingsButton.setPadding(dp(8),dp(8),dp(8),dp(8));actions.addView(settingsButton,new LinearLayout.LayoutParams(dp(32),dp(32)));
        ImageButton more=iconButton(t("Outils et enregistrements"),"more",this::menu);more.setPadding(dp(8),dp(8),dp(8),dp(8));LinearLayout.LayoutParams moreLp=new LinearLayout.LayoutParams(dp(32),dp(32));moreLp.setMargins(dp(4),0,0,0);actions.addView(more,moreLp);
        microphone=iconButton(t("Revenir au microphone"),"mic",()->{if(loading)return;releasePlayer();if(graph.hasFile())graph.live();refresh();});microphone.setPadding(dp(8),dp(8),dp(8),dp(8));LinearLayout.LayoutParams micLp=new LinearLayout.LayoutParams(dp(32),dp(32));micLp.setMargins(dp(4),0,0,0);actions.addView(microphone,micLp);root.addView(header);
        status=text(t("Microphone prêt"),12);status.setTextColor(0xFF9BAAB7);status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.END);status.setGravity(Gravity.CENTER_VERTICAL);status.setPadding(dp(4),0,0,0);root.addView(status,new LinearLayout.LayoutParams(-1,dp(22)));placeStatus(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        if(graph==null)graph=new SpectrogramView(this);graph.setOnSeekListener(this::seekToTime);root.addView(graph,new LinearLayout.LayoutParams(-1,0,1));
        footer=new FrameLayout(this);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(24)));fileHint=text(t("−100 dBFS   •   bleu → vert → jaune   •   0 dBFS"),10);fileHint.setTextColor(0xFF7B8A96);fileHint.setGravity(Gravity.CENTER);fileHint.setSingleLine(true);fileHint.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.addView(fileHint,new FrameLayout.LayoutParams(-1,-1));
        transport=new LinearLayout(this);transport.setGravity(Gravity.CENTER_VERTICAL);seek=new SeekBar(this);seek.setMax(10000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF9A3C));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFF9A3C));transport.addView(seek,new LinearLayout.LayoutParams(0,dp(24),1));clock=text("0:00 / 0:00",11);transport.addView(clock);footer.addView(transport,new FrameLayout.LayoutParams(-1,-1));transport.setVisibility(View.INVISIBLE);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar bar){seeking=true;}public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){if(fromUser){graph.userActivity();seekPosition=(int)(graph.fileDuration()*1000*progress/10000.0);graph.playbackTime(seekPosition/1000.0);clock.setText(time(seekPosition)+" / "+time((int)(graph.fileDuration()*1000)));}}public void onStopTrackingTouch(SeekBar bar){seeking=false;seekToTime(seekPosition/1000.0);}});
        controls=new LinearLayout(this);controls.setPadding(0,0,0,0);
        open=iconButton(t("Ouvrir un fichier audio"),"folder",this::openAudio);play=iconButton(t("Démarrer"),"play",()->{if(graph.hasFile())togglePlayback();else requestAudio(1);});record=iconButton(t("Enregistrer"),"record",()->requestAudio(2));fit=iconButton(t("Vue entière"),"fit",()->graph.resetFile());cancelRecord=iconButton(t("Annuler la prise en cours"),"trash",this::cancelRecording);capture=iconButton(t("Exporter la vue en PNG"),"camera",this::captureImage);
        zoomOut=iconButton(t("Dézoomer dans le temps"),"minus",()->graph.zoomTime(-2));zoomIn=iconButton(t("Zoomer dans le temps"),"plus",()->graph.zoomTime(2));
        cell(controls,open,48);cell(controls,play,48);cell(controls,record,48);cell(controls,cancelRecord,48);cell(controls,fit,48);cell(controls,capture,48);cell(controls,zoomOut,48);cell(controls,zoomIn,48);root.addView(controls,new LinearLayout.LayoutParams(-1,dp(48)));refresh();
    }
    /** Reparent only the status text; retain the graph, audio session and controls. */
    private void placeStatus(boolean landscape){
        if(status==null||header==null)return;
        if(status.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)status.getParent()).removeView(status);
        if(landscape){
            appTitle.setLayoutParams(new LinearLayout.LayoutParams(-2,dp(34)));
            headerActions.setLayoutParams(new LinearLayout.LayoutParams(-2,dp(34)));
            LinearLayout.LayoutParams info=new LinearLayout.LayoutParams(0,dp(34),1);info.setMargins(dp(8),0,dp(8),0);header.addView(status,1,info);
        }else{
            appTitle.setLayoutParams(new LinearLayout.LayoutParams(0,dp(34),1));
            headerActions.setLayoutParams(new LinearLayout.LayoutParams(0,dp(34),1));
            root.addView(status,root.indexOfChild(header)+1,new LinearLayout.LayoutParams(-1,dp(22)));
        }
    }
    private void applyOrientation(){int request=orientationMode==OrientationMode.LANDSCAPE?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:orientationMode==OrientationMode.PORTRAIT?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;setRequestedOrientation(request);}
    private void seekToTime(double seconds){
        if(!graph.hasFile()||loading)return;graph.userActivity();
        int limit=(int)Math.round(graph.fileDuration()*1000);if(playerReady&&player!=null)limit=Math.min(limit,player.getDuration());seekPosition=Math.max(0,Math.min(limit,(int)Math.round(seconds*1000)));
        graph.playbackTime(seekPosition/1000.0);seek.setProgress((int)(seekPosition/Math.max(1,graph.fileDuration()*1000)*10000));clock.setText(time(seekPosition)+" / "+time((int)(graph.fileDuration()*1000)));
        if(playerReady&&player!=null){seekPending=true;seekDeadline=SystemClock.uptimeMillis()+2500;try{player.seekTo(seekPosition,MediaPlayer.SEEK_CLOSEST);}catch(IllegalStateException e){seekPending=false;toast(t("Déplacement impossible : ")+AudioBuffers.error(e));}}
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(graph!=null)graph.cursorTouch(event.getActionMasked());if(event.getActionMasked()==MotionEvent.ACTION_SCROLL&&routeWheel(event))return true;return super.dispatchTouchEvent(event);}
    private boolean routeWheel(MotionEvent event){if(graph==null||loading)return false;int[] location=new int[2];graph.getLocationOnScreen(location);return graph.handleWheel(event,event.getRawX()-location[0],event.getRawY()-location[1]);}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_SCROLL&&routeWheel(event))return true;
        return super.dispatchGenericMotionEvent(event);
    }
    private void captureImage(){
        if(Build.VERSION.SDK_INT<29&&checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},11);return;}
        final android.graphics.Bitmap image;try{image=graph.snapshot();}catch(Exception e){toast(t("Capture impossible : ")+AudioBuffers.error(e));return;}
        capture.setEnabled(false);saves.execute(()->{try{String name="Spectrum_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date())+".png";ImageExport.save(getApplicationContext(),image,name);toast(t("Capture PNG enregistrée : Images/Spectrum Audio/")+name);}catch(Exception e){toast(t("Export PNG impossible : ")+AudioBuffers.error(e));}finally{image.recycle();ui.post(()->{if(!destroyed)capture.setEnabled(true);});}});
    }
    private String time(int ms){return String.format(I18n.locale(),"%d:%02d",Math.max(0,ms)/60000,Math.max(0,ms)/1000%60);}
    private void abandonFocus(){if(focusRequest!=null){((AudioManager)getSystemService(AUDIO_SERVICE)).abandonAudioFocusRequest(focusRequest);focusRequest=null;}}
    private boolean requestFocus(){abandonFocus();focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{if(change<0&&player!=null&&playerReady){player.pause();abandonFocus();refresh();}},ui).build();return ((AudioManager)getSystemService(AUDIO_SERVICE)).requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
    private void releasePlayer(){if(graph!=null)graph.playbackState(false);if(player!=null){player.release();player=null;}playerReady=playerPreparing=seekPending=false;abandonFocus();}
    private void togglePlayback(){if(loading||playerPreparing||audioSource==null)return;try{
        if(playerReady){if(player.isPlaying()){player.pause();abandonFocus();}else if(requestFocus()){if(player.getCurrentPosition()>=player.getDuration()-50)player.seekTo(0);player.start();}else toast(t("La sortie audio est occupée."));refresh();return;}
        player=new MediaPlayer();playerPreparing=true;player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());player.setDataSource(audioSource.getAbsolutePath());
        player.setOnPreparedListener(p->{if(player!=p)return;playerPreparing=false;playerReady=true;seekToTime(seekPosition/1000.0);if(requestFocus())p.start();else toast(t("La sortie audio est occupée."));refresh();});
        player.setOnSeekCompleteListener(p->{if(player!=p||!playerReady)return;if(Math.abs(p.getCurrentPosition()-seekPosition)<=250||SystemClock.uptimeMillis()>=seekDeadline)seekPending=false;refresh();});
        player.setOnCompletionListener(p->{if(player!=p)return;abandonFocus();seekPosition=p.getDuration();graph.playbackTick(seekPosition/1000.0);refresh();});player.setOnErrorListener((p,what,extra)->{if(player==p){releasePlayer();toast(t("Ce codec ne peut pas être lu sur ce téléphone."));refresh();}return true;});player.prepareAsync();refresh();
        }catch(Exception e){releasePlayer();toast(t("Lecture impossible : ")+e.getMessage());refresh();}}

    private void requestAudio(int action){ArrayList<String> permissions=new ArrayList<>();if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.RECORD_AUDIO);if(action==2&&Build.VERSION.SDK_INT<29&&checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);if(!permissions.isEmpty()){pendingAction=action;requestPermissions(permissions.toArray(new String[0]),10);return;}perform(action);}
    public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==11){if(Build.VERSION.SDK_INT>=29||checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)captureImage();else toast(t("Autorisez le stockage pour enregistrer l’image."));return;}if(request!=10)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)perform(pendingAction);else toast(t("Autorisez le microphone pour analyser le son."));}
    private void perform(int action){
        if(loading){toast(t("Attendez la fin de l’analyse ou annulez-la."));return;}if(graph.hasFile()){releasePlayer();graph.live();}
        if(action==1){if(!running){paused=false;startAudio();}else paused=!paused;}
        if(action==2){synchronized(fileLock){if(wav!=null){finishRecording();refresh();return;}}
            try {recordFormat=exportFormat;recording=new File(getFilesDir(),"Spectrum_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date())+".draft");synchronized(fileLock){wav=new WavFile(recording);recordedSamples=0;}paused=false;if(!running)startAudio();}
            catch(Exception e){toast(t("Enregistrement impossible : ")+e.getMessage());}
        }refresh();
    }
    private void startAudio(){
        try {
            int min=AudioRecord.getMinBufferSize(48000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IOException("48 kHz non pris en charge");
            mic=new AudioRecord(MediaRecorder.AudioSource.MIC,48000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min*2,32768));
            if(mic.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException(t("Microphone indisponible"));mic.startRecording();running=true;
            worker=new Thread(this::capture,"Spectre-capture");worker.start();
        }catch(Exception e){running=false;if(mic!=null){mic.release();mic=null;}finishRecording();toast("Microphone : "+e.getMessage());}
    }
    private void capture(){
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        try {
            short[] data=new short[0];Spectrum.Analyzer analyzer=null;int cachedSize=0,cachedMethod=-1;
            while(running){int n=fftSize,m=method;if(n!=cachedSize||m!=cachedMethod){data=new short[n];analyzer=new Spectrum.Analyzer(n,m);cachedSize=n;cachedMethod=m;}int filled=0;
                while(running&&filled<n){int read=mic.read(data,filled,n-filled,AudioRecord.READ_BLOCKING);if(read<0)throw new IOException(t("Lecture audio interrompue (")+read+")");filled+=read;}
                if(!running)break;if(paused)continue;
                synchronized(fileLock){if(wav!=null){wav.write(data,n);recordedSamples+=n;}}
                graph.add(analyzer.analyze(data),n);frames++;
            }
        }catch(Exception e){ui.post(()->{stopAudio();toast(t("Analyse arrêtée : ")+e.getMessage());refresh();});}
    }
    private void finishRecording(){
        final File draft;final int format;final long samples;
        synchronized(fileLock){if(wav==null)return;draft=recording;format=recordFormat;samples=recordedSamples;try{wav.close();}catch(IOException e){toast(t("Finalisation WAV : ")+AudioBuffers.error(e));return;}finally{wav=null;recording=null;}}
        if(samples==0){draft.delete();toast(t("Prise vide : aucun fichier créé."));return;}
        pendingSaves++;refresh();
        saves.execute(()->{File saved=null;String warning="";
            String name=draft.getName().substring(0,draft.getName().length()-6);
            try {
                try{saved=RecordingStore.finish(draft,getFilesDir(),name,format);}
                catch(Exception encoding){if(format==0)throw encoding;warning=t("Le format choisi n’a pas pu être créé ; la prise est conservée en WAV. ")+AudioBuffers.error(encoding);saved=RecordingStore.finish(draft,getFilesDir(),name,0);}
                try{RecordingStore.publish(getApplicationContext(),saved);}catch(Exception publishing){warning+=(warning.isEmpty()?"":"\n")+t("Copie dans Musique impossible. La prise est conservée dans Mes enregistrements. ")+AudioBuffers.error(publishing);}
                File result=saved;String note=warning;ui.post(()->{pendingSaves--;if(destroyed)return;lastFile=result;refresh();if(note.isEmpty())toast(t("Sauvegardé automatiquement : Musique/Spectrum Audio/")+result.getName());else showDialog(new AlertDialog.Builder(this).setTitle(t("Prise conservée")).setMessage(note).setPositiveButton(t("Compris"),null));});
            }catch(Exception failure){String reason=AudioBuffers.error(failure);ui.post(()->{pendingSaves--;if(destroyed)return;refresh();showDialog(new AlertDialog.Builder(this).setTitle(t("Sauvegarde à reprendre")).setMessage(t("La prise brute a été conservée. Ouvrez Réglages → Mes enregistrements pour la récupérer en WAV.\n\n")+reason).setPositiveButton(t("Compris"),null));});}
        });
    }
    private void cancelRecording(){File draft;synchronized(fileLock){if(wav==null)return;draft=recording;try{wav.close();}catch(IOException ignored){}wav=null;recording=null;recordedSamples=0;}if(draft!=null&&draft.exists()&&!draft.delete())toast(t("Prise annulée, mais le fichier temporaire n’a pas pu être supprimé."));else toast(t("Prise annulée."));refresh();}
    private void stopAudio(){running=false;if(mic!=null){try{mic.stop();}catch(Exception ignored){}}if(worker!=null&&worker!=Thread.currentThread()){try{worker.join(2000);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}}if(mic!=null){mic.release();mic=null;}finishRecording();}
    protected void onResume(){super.onResume();ui.removeCallbacks(ticker);ui.post(ticker);}
    protected void onPause(){super.onPause();ui.removeCallbacks(ticker);if(playerPreparing)releasePlayer();else if(player!=null&&playerReady){player.pause();abandonFocus();}graph.playbackState(false);if(running){stopAudio();paused=false;refresh();}}
    protected void onDestroy(){destroyed=true;graph.dispose();cancelImport();releasePlayer();if(audioSource!=null)audioSource.delete();ui.removeCallbacks(ticker);stopAudio();saves.shutdown();super.onDestroy();}
    public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(getPreferences(0).getString("language","auto").equals("auto")){I18n.load(this,"auto");relabel();}else placeStatus(c.orientation==Configuration.ORIENTATION_LANDSCAPE);}
    private void refresh(){if(play==null)return;boolean file=graph.hasFile(),listening=player!=null&&playerReady&&player.isPlaying();graph.playbackState(listening);play.setEnabled(!loading&&!playerPreparing);record.setEnabled(!loading&&!file&&pendingSaves==0);open.setEnabled(!loading);microphone.setEnabled(!loading);fit.setEnabled(file&&!loading);zoomIn.setEnabled(file&&!loading);zoomOut.setEnabled(file&&!loading);zoomIn.setAlpha(file&&!loading?1:.38f);zoomOut.setAlpha(file&&!loading?1:.38f);
        graphic(open,t("Ouvrir un fichier audio"),"folder",false,false);graphic(play,playerPreparing?t("Patientez"):file?(listening?t("Pause"):t("Lire")):!running?t("Démarrer"):paused?t("Reprendre"):t("Pause"),file?(listening?"pause":"play"):running&&!paused?"pause":"play",true,false);
        String mode=file?"file":"micro";if(!mode.equals(microphone.getTag())){microphone.setTag(mode);microphone.setImageDrawable(new ActionIcon("mic",file?0xFFD7DFE7:0xFFFF9A3C));microphone.setBackground(surface(file?0xFF13171B:0xFF2B1A0D,file?0xFF242B31:0xFF8A4A1B));}graphic(fit,t("Vue entière"),"fit",false,false);transport.setVisibility(file?View.VISIBLE:View.INVISIBLE);fileHint.setVisibility(file?View.INVISIBLE:View.VISIBLE);
        synchronized(fileLock){boolean rec=wav!=null;cancelRecord.setEnabled(rec);graphic(cancelRecord,t("Annuler la prise en cours"),"trash",false,rec);graphic(record,rec?t("Arrêter"):"REC",rec?"stop":"record",false,rec);graphic(capture,t("Exporter la vue en PNG"),"camera",false,false);record.setContentDescription(rec?t("Terminer l’enregistrement"):t("Enregistrer le microphone"));record.setTooltipText(rec?t("Terminer l’enregistrement"):t("Enregistrer le microphone"));if(file){
            if(seekPending&&SystemClock.uptimeMillis()>=seekDeadline)seekPending=false;if(playerReady&&!seeking&&!seekPending){seekPosition=player.getCurrentPosition();seek.setProgress((int)(seekPosition/Math.max(1,graph.fileDuration()*1000)*10000));graph.playbackTick(seekPosition/1000.0);}clock.setText(time(seekPosition)+" / "+time((int)(graph.fileDuration()*1000)));
            status.setText((listening?"▶ ":"")+graph.detailDescription()+" · "+fileName);fileHint.setText(t("Glisser pour déplacer · Pincer pour zoomer · Double appui : tout voir"));
        }else{fileHint.setText(t("−100 dBFS   •   bleu → vert → jaune   •   0 dBFS"));if(!loading)status.setText((!running?t("MICRO · À l’arrêt"):paused?t("MICRO · En pause"):t("● MICRO · En direct"))+"  ·  48 kHz  ·  FFT "+fftSize+(rec?"  ·  REC "+String.format(I18n.locale(),"%.1f s",recordedSamples/48000.0):""));}if(pendingSaves>0)status.setText(t("Sauvegarde automatique en cours…"));status.setTextColor(rec?0xFFFF6475:0xFF9BAAB7);}}
    private void exportWav(){if(lastFile==null){toast(t("Aucun enregistrement à exporter."));return;}synchronized(fileLock){if(wav!=null){toast("Terminez d’abord l’enregistrement.");return;}}exportFile=lastFile;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(RecordingStore.mime(lastFile));i.putExtra(Intent.EXTRA_TITLE,lastFile.getName());startActivityForResult(i,20);}
    protected void onActivityResult(int r,int result,Intent i){super.onActivityResult(r,result,i);if(r==30&&result==RESULT_OK&&i!=null&&i.getData()!=null){loadAudio(i.getData());return;}if(r==20&&result==RESULT_OK&&i!=null&&i.getData()!=null&&exportFile!=null){File source=exportFile;android.net.Uri uri=i.getData();new Thread(()->{try(InputStream in=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException(t("Destination inaccessible"));byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);toast(t("Enregistrement exporté."));}catch(Exception e){toast(t("Export impossible : ")+e.getMessage());}}).start();}}
    private void openAudio(){if(loading)return;if(playerPreparing)releasePlayer();else if(playerReady){player.pause();abandonFocus();}stopAudio();paused=false;refresh();Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.putExtra(Intent.EXTRA_TITLE,t("Choisir un audio · M4A, MP3, WAV, FLAC, OGG…"));startActivityForResult(intent,30);}
    private void cancelImport(){importGeneration++;if(importer!=null)importer.interrupt();loading=false;if(importDialog!=null){importDialog.dismiss();importDialog=null;}}
    private void loadAudio(android.net.Uri uri){
        cancelImport();releasePlayer();final int generation=importGeneration;final int size=fftSize,algorithm=method;loading=true;
        String displayName=t("Audio importé");try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())displayName=cursor.getString(0);}catch(Exception ignored){}final String importedName=displayName;
        importDialog=new ProgressDialog(this);importDialog.setTitle(t("Ouvrir un fichier audio"));importDialog.setMessage(t("Préparation…"));importDialog.setCancelable(true);importDialog.setOnCancelListener(d->{cancelImport();refresh();});importDialog.setButton(DialogInterface.BUTTON_NEGATIVE,t("Annuler"),(d,w)->{cancelImport();refresh();});importDialog.show();refresh();
        importer=new Thread(()->{File source=null;boolean retained=false;try{
            source=File.createTempFile("spectre-source-",".audio",getCacheDir());
            try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(source))){if(in==null)throw new IOException(t("Fichier inaccessible."));byte[] buffer=new byte[65536];int n;long total=0;while((n=in.read(buffer))!=-1){FileAnalysis.check();total+=n;if(total>512L*1024*1024)throw new IOException(t("Fichier trop volumineux (512 Mo maximum)."));out.write(buffer,0,n);}}
            FileAnalysis.check();File imported=source;FileAnalysis.Result result=FileAnalysis.load(this,android.net.Uri.fromFile(source),size,algorithm,message->ui.post(()->{if(generation==importGeneration&&importDialog!=null)importDialog.setMessage(I18n.progress(message));}));ui.post(()->{if(generation!=importGeneration){result.close();imported.delete();return;}loading=false;if(importDialog!=null){importDialog.dismiss();importDialog=null;}if(audioSource!=null)audioSource.delete();audioSource=imported;viewedSource=uri.toString();fileName=importedName;seekPosition=0;seek.setProgress(0);graph.showFile(result);graph.playbackTime(0);refresh();});retained=true;}
            catch(Exception e){ui.post(()->{if(generation!=importGeneration)return;loading=false;if(importDialog!=null){importDialog.dismiss();importDialog=null;}refresh();String details=AudioBuffers.error(e);showDialog(new AlertDialog.Builder(this).setTitle(t("Import impossible")).setMessage(t("Le fichier n’a pas pu être décodé.\n\n")+details).setPositiveButton(t("Fermer"),null).setNeutralButton(t("Copier les détails"),(dialog,which)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(t("Erreur import"),details));toast(t("Détails copiés."));}));});}finally{if(!retained&&source!=null)source.delete();}},"Spectre-import");importer.start();
    }
    private Spinner choice(LinearLayout box,String title,String[] values,int selected){TextView label=text(title,12);label.setTextColor(0xFF9BAAB7);label.setPadding(0,dp(16),0,dp(6));box.addView(label);Spinner spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){public View getView(int position,View convert,ViewGroup parent){TextView view=text(getItem(position)+"   ▾",14);view.setGravity(Gravity.CENTER_VERTICAL);view.setPadding(dp(14),dp(10),dp(12),dp(10));view.setMinHeight(dp(48));return view;}public View getDropDownView(int position,View convert,ViewGroup parent){TextView view=text(getItem(position),14);view.setPadding(dp(16),dp(14),dp(16),dp(14));return view;}};spinner.setAdapter(adapter);spinner.setBackground(surface(0xFF1D242A,0xFF303C45));spinner.setPopupBackgroundDrawable(shape(0xFF20272D,0xFF3A4853,16));spinner.setSelection(selected);box.addView(spinner,new LinearLayout.LayoutParams(-1,-2));return spinner;}
    private int index(int[] a,int n){for(int i=0;i<a.length;i++)if(a[i]==n)return i;return 0;}
    private void settings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(10),dp(20),0);ScrollView scroll=new ScrollView(this);scroll.addView(box);
        int[] times={5,10,15,30,60}, hz={2000,4000,8000,12000,16000,24000},sizes={512,1024,2048,4096,8192};
        Spinner t=choice(box,t("Fenêtre de temps · direct"),new String[]{t("5 secondes"),t("10 secondes"),t("15 secondes"),t("30 secondes"),t("60 secondes")},index(times,duration));
        Spinner h=choice(box,t("Fréquence maximale"),new String[]{"2 000 Hz","4 000 Hz","8 000 Hz","12 000 Hz","16 000 Hz","24 000 Hz"},index(hz,maxHz));
        Spinner a=choice(box,t("Traitement audio"),new String[]{t("FFT · fenêtre de Hann"),t("FFT · fenêtre de Blackman"),t("Welch · moyenne de 3 FFT Hann")},method);
        Spinner n=choice(box,t("Précision de l’analyse"),new String[]{t("512 échantillons"),t("1 024 échantillons"),t("2 048 échantillons"),t("4 096 échantillons"),t("8 192 échantillons")},index(sizes,fftSize));
        Spinner format=choice(box,t("Format de sauvegarde automatique"),new String[]{t("WAV · sans compression"),"M4A / AAC · 128 kbit/s","OGG / Opus · 128 kbit/s"},exportFormat);TextView location=text(t("À l’arrêt : sauvegarde dans Musique/Spectrum Audio et dans Mes enregistrements. Le format choisi s’applique aux prochaines prises."),12);location.setPadding(0,dp(8),0,dp(8));box.addView(location);TextView opusHelp=text(t("OGG/Opus : Android 10 ou plus récent. Secours en WAV si l’encodage échoue."),12);box.addView(opusHelp);
        Switch detail=new Switch(this);detail.setText(t("Définition adaptative des fichiers"));detail.setTextColor(0xFFE9F2FA);detail.setTextSize(14);detail.setChecked(adaptive);detail.setPadding(0,dp(14),0,dp(8));box.addView(detail);
        Switch wheel=new Switch(this);wheel.setText(t("Mode molette émulateur"));wheel.setTextColor(0xFFE9F2FA);wheel.setTextSize(14);wheel.setChecked(emulatorWheel);wheel.setPadding(0,dp(14),0,dp(8));box.addView(wheel);
        Switch follow=new Switch(this);follow.setText(t("Suivre automatiquement la lecture"));follow.setTextColor(0xFFE9F2FA);follow.setTextSize(14);follow.setChecked(followPlayback);follow.setPadding(0,dp(14),0,dp(8));box.addView(follow);
        TextView followHelp=text(t("Déplace la vue quand le curseur atteint le bord, en conservant le zoom."),12);box.addView(followHelp);
        int[] gains={0,6,12,24,36},ranges={40,60,80,100,120};
        Spinner g=choice(box,t("Gain d’affichage (ne modifie pas le son)"),new String[]{"0 dB","+6 dB","+12 dB","+24 dB","+36 dB"},index(gains,displayGain));
        Spinner dr=choice(box,t("Dynamique des couleurs"),new String[]{"40 dB","60 dB","80 dB","100 dB","120 dB"},index(ranges,displayRange));
        Spinner palette=choice(box,t("Palette"),new String[]{t("Couleurs"),t("Noir et blanc")},gray?1:0);
        Switch l=new Switch(this);l.setText(t("Fréquences logarithmiques (dès 20 Hz)"));l.setChecked(log);l.setTextColor(0xFFE9F2FA);l.setTextSize(14);l.setPadding(0,dp(18),0,dp(18));box.addView(l);
        java.util.function.Supplier<SpectrumPreset> readPreset=()->new SpectrumPreset(sizes[n.getSelectedItemPosition()],a.getSelectedItemPosition(),hz[h.getSelectedItemPosition()],times[t.getSelectedItemPosition()],detail.isChecked(),l.isChecked(),gains[g.getSelectedItemPosition()],ranges[dr.getSelectedItemPosition()],palette.getSelectedItemPosition()==1);
        java.util.function.Consumer<SpectrumPreset> recallPreset=preset->{n.setSelection(index(sizes,preset.fft));a.setSelection(preset.method);h.setSelection(index(hz,preset.hz));t.setSelection(index(times,preset.duration));detail.setChecked(preset.adaptive);l.setChecked(preset.log);g.setSelection(index(gains,preset.gain));dr.setSelection(index(ranges,preset.range));palette.setSelection(preset.gray?1:0);};
        LinearLayout presets=new LinearLayout(this);presets.setOrientation(1);
        presets.addView(button(t("Préréglage par défaut"),()->recallPreset.accept(SpectrumPreset.defaults())));
        presets.addView(button(t("Préréglage images cachées"),()->recallPreset.accept(SpectrumPreset.hiddenImages())));
        TextView presetTitle=text(t("Préréglages personnels"),12);presetTitle.setPadding(0,dp(12),0,dp(6));presets.addView(presetTitle);
        android.content.SharedPreferences memories=getSharedPreferences("spectral-presets",0);LinearLayout slots=new LinearLayout(this);
        for(int slot=0;slot<3;slot++){final String key="slot"+slot,label="P"+(slot+1);Button memory=button(label,()->{SpectrumPreset saved=SpectrumPreset.decode(memories.getString(key,null));if(saved==null){toast(t("Emplacement vide : appui long pour enregistrer."));return;}recallPreset.accept(saved);toast(label+" · "+t("Préréglage rappelé. Appuyez sur Appliquer."));});
            Runnable decorate=()->{boolean saved=SpectrumPreset.decode(memories.getString(key,null))!=null;memory.setText(label+(saved?" ●":""));memory.setTextColor(saved?0xFFFF9A3C:0xFFD7DFE7);memory.setBackground(surface(saved?0xFF2B1A0D:0xFF13171B,saved?0xFF8A4A1B:0xFF242B31));memory.setContentDescription(t("Préréglages personnels")+" "+label+(saved?" ✓":""));};
            decorate.run();memory.setOnLongClickListener(v->{memories.edit().putString(key,readPreset.get().encode()).apply();decorate.run();memory.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);toast(label+" · "+t("Préréglage enregistré."));return true;});cell(slots,memory,48);
        }
        presets.addView(slots);TextView presetHelp=text(t("Appui court : rappeler · Appui long : enregistrer. Puis Appliquer."),12);presetHelp.setPadding(0,dp(6),0,dp(6));presets.addView(presetHelp);box.addView(presets,0);
        TextView help=text(t("Le détail visible est recalculé après zoom et déplacement. Pour les images cachées, essayez le préréglage puis zoomez. Mode molette émulateur : un glissement vertical zoome dans le temps, un glissement horizontal déplace la vue."),12);box.addView(help);
        box.addView(button(t("Mes enregistrements"),this::recordings));
        Button donate=button(t("Faire un don ♥"),this::openDonationPage);donate.setContentDescription(t("Soutenir No Ads Studio sur Ko-fi"));box.addView(donate);
        TextView donationHint=text(t("Soutien volontaire, sans contrepartie. Ouvre Ko-fi dans votre navigateur."),12);donationHint.setPadding(0,dp(8),0,dp(8));box.addView(donationHint);
        showDialog(new AlertDialog.Builder(this).setTitle(t("Réglages du spectre")).setView(scroll).setNegativeButton(t("Annuler"),null).setPositiveButton(t("Appliquer"),(d,w)->{emulatorWheel=wheel.isChecked();graph.emulatorWheel(emulatorWheel);followPlayback=follow.isChecked();graph.followPlayback(followPlayback);getPreferences(0).edit().putBoolean("emulatorWheel",emulatorWheel).putBoolean("followPlayback",followPlayback).apply();adaptive=detail.isChecked();displayGain=gains[g.getSelectedItemPosition()];displayRange=ranges[dr.getSelectedItemPosition()];gray=palette.getSelectedItemPosition()==1;exportFormat=format.getSelectedItemPosition();duration=times[t.getSelectedItemPosition()];maxHz=hz[h.getSelectedItemPosition()];fftSize=sizes[n.getSelectedItemPosition()];int next=a.getSelectedItemPosition();if(next!=method&&!graph.hasFile())graph.clear();method=next;log=l.isChecked();graph.analysisSettings(fftSize,method,adaptive,displayGain,displayRange,gray);graph.configure(maxHz,duration,log);getPreferences(0).edit().putInt("fft",fftSize).putInt("method",method).putInt("hz",maxHz).putInt("duration",duration).putBoolean("log",log).putInt("exportFormat",exportFormat).putBoolean("adaptive",adaptive).putBoolean("gray",gray).putInt("gain",displayGain).putInt("range",displayRange).apply();refresh();}));
    }
    private void openDonationPage(){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(DONATION_URL)));}catch(ActivityNotFoundException error){showDialog(new AlertDialog.Builder(this).setTitle(t("Faire un don ♥")).setMessage(t("Aucun navigateur disponible. Ouvrez ce lien pour soutenir No Ads Studio : ")+DONATION_URL).setPositiveButton(t("Fermer"),null));}}
    private void recordings(){File[] files=getFilesDir().listFiles((directory,name)->RecordingFormat.saved(name));if(files==null){toast(t("Aucun enregistrement."));return;}Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));ArrayList<File> done=new ArrayList<>();synchronized(fileLock){for(File f:files)if((wav==null||!f.equals(recording))&&(!f.getName().endsWith(".draft")||pendingSaves==0))done.add(f);}if(done.isEmpty()){toast(t("Aucun enregistrement terminé."));return;}String[] names=new String[done.size()];for(int j=0;j<names.length;j++)names[j]=done.get(j).getName().replace(".draft",t(" · prise à récupérer"));showDialog(new AlertDialog.Builder(this).setTitle(t("Mes enregistrements")).setItems(names,(dialog,item)->recordingDetails(done.get(item))).setNegativeButton(t("Fermer"),null));}
    private void recordingDetails(File selected){
        if(selected.getName().endsWith(".draft")){showDialog(new AlertDialog.Builder(this).setTitle(t("Prise à récupérer")).setMessage(t("La dernière sauvegarde n’a pas pu aboutir. Récupérer la prise en WAV ?")).setPositiveButton(t("Récupérer"),(d,w)->{pendingSaves++;refresh();saves.execute(()->{try{String name=selected.getName().replace(".draft","");File recovered=RecordingStore.finish(selected,getFilesDir(),name,0);String note=t("Prise récupérée dans Mes enregistrements.");try{RecordingStore.publish(getApplicationContext(),recovered);note=t("WAV récupéré et sauvegardé dans Musique/Spectrum Audio.");}catch(Exception ignored){}String message=note;ui.post(()->{pendingSaves--;if(destroyed)return;lastFile=recovered;refresh();toast(message);});}catch(Exception e){ui.post(()->{pendingSaves--;if(destroyed)return;refresh();toast(t("Récupération impossible : ")+AudioBuffers.error(e));});}});}).setNeutralButton(t("Supprimer"),(d,w)->deleteRecording(selected)).setNegativeButton(t("Fermer"),null));return;}
        showDialog(new AlertDialog.Builder(this).setTitle(selected.getName()).setItems(new String[]{t("Visualiser et écouter"),t("Exporter une copie"),t("Supprimer")},(dialog,item)->{if(item==0){stopAudio();loadAudio(android.net.Uri.fromFile(selected));}else if(item==1){lastFile=selected;exportWav();}else deleteRecording(selected);}).setNegativeButton(t("Fermer"),null));
    }
    private void deleteRecording(File selected){String publicReference=getSharedPreferences("exports",0).getString(selected.getName(),"");boolean displayed=viewedSource.equals(android.net.Uri.fromFile(selected).toString())||(!publicReference.isEmpty()&&viewedSource.equals(publicReference));showDialog(new AlertDialog.Builder(this).setTitle(t("Supprimer cet enregistrement ?")).setMessage(selected.getName()+t("\n\nLa copie de l’application et sa copie automatique dans Musique seront supprimées. Les copies exportées manuellement ne sont pas concernées.")).setNegativeButton(t("Conserver"),null).setPositiveButton(t("Supprimer"),(dialog,which)->saves.execute(()->{try{RecordingStore.delete(getApplicationContext(),selected);ui.post(()->{if(destroyed)return;if(selected.equals(lastFile))lastFile=null;if(displayed){releasePlayer();if(audioSource!=null)audioSource.delete();audioSource=null;viewedSource="";graph.live();}refresh();toast(t("Enregistrement supprimé."));});}catch(Exception e){ui.post(()->{if(!destroyed)toast(t("Suppression incomplète : ")+AudioBuffers.error(e));});}})));}
    private void toast(String s){ui.post(()->{if(!destroyed)Toast.makeText(this,s,Toast.LENGTH_LONG).show();});}
}
