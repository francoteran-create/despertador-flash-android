package com.despertadorflash;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Explicit foreground microphone analysis for accessible music haptics.
 * Never saves, streams, uploads or records audio to a file.
 */
public final class AuroraMusicActivity extends Activity {
 private static final int SAMPLE_RATE=MusicSpectrum.RATE;
 private static final int FRAME=MusicSpectrum.FRAME;
 private volatile int threshold=-42;
 private MusicSpectrumView spectrum;
 private TextView bands;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private volatile boolean running=false;
 private boolean startPending=false;
 private volatile int strength=2;
 private volatile int sensitivity=2;
 private volatile AudioRecord recorder;
 private Thread worker;
 private Vibrator vibrator;
 private TextView status,level;
 private Button start;
 private int beats=0;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
  t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);
  t.setPadding(dp(8),dp(13),dp(8),dp(13));return t;
 }
 private Button button(String value){
  Button b=new Button(this);b.setText(value);b.setMinHeight(dp(64));
  AuroraDesign.action(b,0xff11191f,0xff101018,0xff32efff);
  b.setTextColor(0xffffdf33);
  return b;
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  android.content.SharedPreferences p=getSharedPreferences("aurora",0);
  strength=Math.max(1,Math.min(3,p.getInt("music_strength",2)));
  sensitivity=Math.max(0,Math.min(2,p.getInt("music_sensitivity",2)));
  threshold=Math.max(-65,Math.min(-15,p.getInt("music_threshold",-42)));
  android.widget.FrameLayout screen=new android.widget.FrameLayout(this);screen.setBackgroundColor(Color.BLACK);
  android.widget.ImageView mural=new android.widget.ImageView(this);mural.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
  mural.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  try(java.io.InputStream input=getAssets().open("aurora_mural.jpg.b64")){
   java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[2048];int n;
   while((n=input.read(block))!=-1)bytes.write(block,0,n);
   byte[] raw=android.util.Base64.decode(bytes.toByteArray(),android.util.Base64.DEFAULT);
   mural.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(raw,0,raw.length));
  }catch(Exception ignored){}
  screen.addView(mural,new android.widget.FrameLayout.LayoutParams(-1,-1));
  android.view.View shade=new android.view.View(this);shade.setBackgroundColor(0xd0000000);screen.addView(shade,new android.widget.FrameLayout.LayoutParams(-1,-1));
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setFillViewport(true);screen.addView(scroll,new android.widget.FrameLayout.LayoutParams(-1,-1));
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(18));scroll.addView(root);setContentView(screen);
  LinearLayout top=new LinearLayout(this);root.addView(top);
  Button back=button("← VOLVER");top.addView(back,new LinearLayout.LayoutParams(0,-2,1));back.setOnClickListener(v->finish());
  Button settings=button("⚙ AJUSTES");top.addView(settings,new LinearLayout.LayoutParams(0,-2,1));settings.setOnClickListener(v->showSettings());
  NeonHomeIcon symbol=new NeonHomeIcon(this,6);LinearLayout.LayoutParams icon=new LinearLayout.LayoutParams(dp(64),dp(64));icon.gravity=Gravity.CENTER;root.addView(symbol,icon);
  TextView title=label("SENTIR LA MÚSICA",27);title.setTextColor(0xffffd633);root.addView(title);
  spectrum=new MusicSpectrumView(this);root.addView(spectrum,new LinearLayout.LayoutParams(-1,dp(250)));
  bands=label("GRAVES  ·  MEDIOS  ·  AGUDOS",15);bands.setTextColor(0xff32efff);root.addView(bands);
  status=label("Micrófono apagado",16);root.addView(status);
  start=button("▶ INICIAR MÚSICA HÁPTICA");root.addView(start);
  start.setOnClickListener(v->{if(running)stopCapture();else if(startPending)stopCapture();else startCapture();});
 }
 private void showSettings(){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(10),dp(18),dp(12));panel.setBackgroundColor(0xff101018);
  android.content.SharedPreferences p=getSharedPreferences("aurora",0);
  slider(panel,"Intensidad",2,strength-1,v->{strength=v+1;p.edit().putInt("music_strength",strength).apply();},v->new String[]{"SUAVE","MEDIA","FUERTE"}[v]);
  slider(panel,"Sensibilidad de ritmo",2,sensitivity,v->{sensitivity=v;p.edit().putInt("music_sensitivity",v).apply();},v->new String[]{"BAJA","MEDIA","ALTA"}[v]);
  slider(panel,"Umbral digital",50,threshold+65,v->{threshold=v-65;p.edit().putInt("music_threshold",threshold).apply();},v->(v-65)+" dBFS");
  Button test=button("PROBAR VIBRACIÓN");panel.addView(test);test.setOnClickListener(v->{if(!running&&!startPending)pulse(250,255);});
  panel.addView(label("Nivel relativo del micrófono (dBFS), no decibeles ambientales calibrados. Vibración por ataques musicales, no pulsos inventados. No se guarda ni envía audio.",13));
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(panel);
  new android.app.AlertDialog.Builder(this).setTitle("Configuración de música").setView(scroll).setPositiveButton("LISTO",null).show();
 }
 private interface Change{void set(int value);}
 private interface Format{String text(int value);}
 private void slider(LinearLayout panel,String name,int max,int initial,Change change,Format format){
  TextView label=label(name+": "+format.text(initial),16);panel.addView(label);
  SeekBar bar=new SeekBar(this);bar.setMax(max);bar.setProgress(initial);panel.addView(bar);
  bar.setContentDescription(name);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int value,boolean user){change.set(value);label.setText(name+": "+format.text(value));}
   public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
  });
 }
 private void startCapture(){
  if(running||startPending)return;
  if(AlarmEffectsService.activeId>=0){status.setText("Hay una alarma activa. Detenela antes de usar música háptica.");return;}
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},762);
   status.setText("Autorizá el micrófono y tocá INICIAR otra vez.");return;
  }
  if(vibrator==null||!vibrator.hasVibrator()){status.setText("El teléfono no tiene vibrador disponible.");return;}
  startPending=true;start.setText("■ CANCELAR INICIO");
  boolean background=getSharedPreferences("aurora",0).getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false);
  if(background){
   try{startService(new Intent(this,AuroraSoundMonitorService.class).setAction(AuroraSoundMonitorService.STOP));}
   catch(Exception ignored){}
   status.setText("Deteniendo escucha ambiental para liberar el micrófono…");
   ui.postDelayed(this::beginRecorder,650L);
  }else beginRecorder();
 }
 private void beginRecorder(){
  if(!startPending||isFinishing()||isDestroyed()||running)return;
  startPending=false;start.setText("▶ INICIAR MÚSICA HÁPTICA");
  if(AlarmEffectsService.activeId>=0){status.setText("Hay una alarma activa.");return;}
  AudioRecord made=null;
  try{
   int minimum=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT);
   if(minimum<=0){status.setText("Android no admite esta configuración de micrófono.");return;}
   made=new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE,
      AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(FRAME*4,minimum*2));
   if(made.getState()!=AudioRecord.STATE_INITIALIZED){
    made.release();status.setText("No se pudo iniciar el micrófono.");return;
   }
   made.startRecording();
   if(made.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING){
    made.release();status.setText("Android no habilitó la captura de música.");return;
   }
   recorder=made;
   running=true;beats=0;
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   start.setText("■ DETENER MÚSICA HÁPTICA");
   status.setText("Escuchando música. La vibración responde a golpes destacados del ritmo.");
   final AudioRecord owned=made;
   worker=new Thread(()->analyze(owned),"AuroraMusicHaptics");
   worker.start();
  }catch(Exception error){
   if(made!=null){try{made.stop();}catch(Exception ignored){}try{made.release();}catch(Exception ignored){}}
   recorder=null;running=false;start.setText("▶ INICIAR MÚSICA HÁPTICA");
   getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   status.setText(error instanceof SecurityException?"Falta autorización para micrófono.":"Error de micrófono: "+error.getClass().getSimpleName());
  }
 }
 private void analyze(AudioRecord owned){
  short[] data=new short[FRAME];
  MusicSpectrum dsp=new MusicSpectrum();
  int errors=0;
  while(running&&recorder==owned){
   if(AlarmEffectsService.activeId>=0){ui.post(()->{if(recorder==owned){stopCapture();status.setText("Música detenida por una alarma activa.");}});break;}
   int count;
   try{count=owned.read(data,0,data.length);}catch(Exception ex){break;}
   if(count<=0){if(++errors>5)break;continue;}errors=0;
   MusicSpectrum.Result result=dsp.analyze(data,count,SystemClock.elapsedRealtime(),threshold,sensitivity);
   if(result.beat&&running&&recorder==owned){
    int s=strength;int duration=result.band==0?80:result.band==1?55:40;
    pulse(duration+(s-1)*10,s==1?90:s==2?175:255);beats++;
   }
   final int total=beats;
   ui.post(()->{if(running&&recorder==owned){
    spectrum.show(result);
    bands.setText(String.format(java.util.Locale.ROOT,"G %.0f  ·  M %.0f  ·  A %.0f dBFS",result.bass,result.mid,result.treble));
    status.setText(String.format(java.util.Locale.ROOT,"%.0f dBFS · Umbral %d · Pulsos %d",result.db,threshold,total));
   }});
  }
  if(running&&recorder==owned)ui.post(()->{
   if(running&&recorder==owned){
    stopCapture();status.setText("La captura de música se interrumpió. Podés reintentar.");
   }
  });
 }
 private void pulse(int duration,int amplitude){
  if(AlarmEffectsService.activeId>=0||vibrator==null||!vibrator.hasVibrator())return;
  try{
   if(Build.VERSION.SDK_INT>=26){int amp=vibrator.hasAmplitudeControl()?amplitude:VibrationEffect.DEFAULT_AMPLITUDE;vibrator.vibrate(VibrationEffect.createOneShot(duration,amp));}
   else vibrator.vibrate(duration);
  }catch(RuntimeException ex){android.util.Log.w("Aurora","Vibración musical no disponible",ex);}
 }
 private void stopCapture(){
  startPending=false;running=false;ui.removeCallbacksAndMessages(null);
  AudioRecord old=recorder;recorder=null;
  if(old!=null){try{old.stop();}catch(Exception ignored){}try{old.release();}catch(Exception ignored){}}
  if(vibrator!=null&&AlarmEffectsService.activeId<0)vibrator.cancel();
  if(start!=null)start.setText("▶ INICIAR MÚSICA HÁPTICA");
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  if(spectrum!=null)spectrum.reset();
  if(status!=null)status.setText("Detenido. Micrófono y vibración apagados.");
 }
 @Override protected void onPause(){super.onPause();stopCapture();ui.removeCallbacksAndMessages(null);}
 @Override protected void onDestroy(){stopCapture();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
