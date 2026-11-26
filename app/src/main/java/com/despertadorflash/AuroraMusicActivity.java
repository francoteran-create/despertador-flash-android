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
 private static final int SAMPLE_RATE=16000;
 private static final int FRAME=1024;
 private static final long MIN_BEAT_INTERVAL_MS=220;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private volatile boolean running=false;
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
  b.setTextColor(Color.WHITE);
  b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff343257));
  return b;
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.BLACK);root.setPadding(dp(18),dp(22),dp(18),dp(24));setContentView(root);
  Button back=button("← VOLVER A AURORA");root.addView(back);back.setOnClickListener(v->finish());
  root.addView(label("SENTIR LA MÚSICA",28));
  root.addView(label("AURORA escucha el ritmo del ambiente y lo convierte en pulsos de vibración. "
    +"Usalo con música cerca del teléfono. La intensidad es limitada para cuidar la batería. "
    +"No se guarda ni se envía audio.",16));
  level=label("Nivel: MEDIO",19);root.addView(level);
  SeekBar slider=new SeekBar(this);slider.setMax(2);slider.setProgress(1);root.addView(slider);
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int value,boolean fromUser){
    strength=value+1;level.setText("Nivel: "+(strength==1?"SUAVE":strength==2?"MEDIO":"FUERTE"));
   }
   public void onStartTrackingTouch(SeekBar b){}
   public void onStopTrackingTouch(SeekBar b){}
  });
  TextView sensitivityLabel=label("Sensibilidad: ALTA",16);root.addView(sensitivityLabel);
  SeekBar sensitivityBar=new SeekBar(this);sensitivityBar.setMax(2);sensitivityBar.setProgress(2);root.addView(sensitivityBar);
  sensitivityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int value,boolean user){sensitivity=value;sensitivityLabel.setText("Sensibilidad: "+(value==0?"BAJA":value==1?"MEDIA":"ALTA"));}
   public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
  });
  Button test=button("PROBAR VIBRACIÓN");root.addView(test);test.setOnClickListener(v->{if(!running){pulse(300,255);}});
  status=label("Micrófono apagado. Tocá INICIAR para escuchar música.",16);root.addView(status);
  start=button("▶ INICIAR MÚSICA HÁPTICA");root.addView(start);
  start.setOnClickListener(v->{if(running)stopCapture();else startCapture();});
  Button stop=button("■ DETENER VIBRACIÓN Y MICRÓFONO");root.addView(stop);
  stop.setOnClickListener(v->stopCapture());
  root.addView(label("No es un sistema de reconocimiento de canciones ni distingue melodías. "
    +"Los pulsos dependen de los graves, el ritmo y la vibración disponible en tu teléfono. "
    +"Al salir de esta pantalla se detiene todo.",13));
 }
 private void startCapture(){
  if(running)return;
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},762);
   status.setText("Autorizá el micrófono y tocá INICIAR otra vez.");return;
  }
  if(vibrator==null||!vibrator.hasVibrator()){status.setText("El teléfono no tiene vibrador disponible.");return;}
  boolean background=getSharedPreferences("aurora",0).getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false);
  if(background){
   try{startService(new Intent(this,AuroraSoundMonitorService.class).setAction(AuroraSoundMonitorService.STOP));}
   catch(Exception ignored){}
   status.setText("Deteniendo escucha ambiental para liberar el micrófono…");
   ui.postDelayed(this::beginRecorder,650L);
  }else beginRecorder();
 }
 private void beginRecorder(){
  if(isFinishing()||isDestroyed()||running)return;
  try{
   int minimum=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT);
   if(minimum<=0){status.setText("Android no admite esta configuración de micrófono.");return;}
   AudioRecord made=new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE,
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
   worker=new Thread(()->analyze(made),"AuroraMusicHaptics");
   worker.start();
  }catch(SecurityException error){status.setText("Falta autorización para micrófono.");}
   catch(Exception error){status.setText("Error de micrófono: "+error.getClass().getSimpleName());}
 }
 private void analyze(AudioRecord owned){
  short[] data=new short[FRAME];
  double energyAverage=0;
  double bassAverage=0;
  long lastBeat=0;
  int frames=0;
  int errors=0;
  while(running&&recorder==owned){
   int count;
   try{count=owned.read(data,0,data.length);}catch(Exception ex){break;}
   if(count<=0){if(++errors>5)break;continue;}
   errors=0;
   double energy=0;
   for(int i=0;i<count;i++){double sample=data[i]/32768.0;energy+=sample*sample;}
   energy=Math.sqrt(energy/count);
   double bass=lowBandPower(data,count);
   if(frames==0){energyAverage=energy;bassAverage=bass;}
   energyAverage=energyAverage*.94+energy*.06;
   bassAverage=bassAverage*.94+bass*.06;
   frames++;
   long now=SystemClock.elapsedRealtime();
   int mode=sensitivity;
   double floor=mode==2?.003:mode==1?.008:.018;
   double ratio=mode==2?1.10:mode==1?1.20:1.40;
   boolean loudEnough=energy>floor;
   boolean accented=energy>energyAverage*ratio || bass>Math.max(1e-8,bassAverage*ratio);
   // High sensitivity also renders steady audible music as spaced pulses, not silence.
   boolean steady=mode==2&&energy>floor*3&&now-lastBeat>=500L;
   if(loudEnough&&(accented||steady)&&now-lastBeat>=MIN_BEAT_INTERVAL_MS){
    int levelNow=strength;
    int amplitude=levelNow==1?70:levelNow==2?135:200;
    int duration=levelNow==1?80:levelNow==2?130:180;
    pulse(duration,amplitude);
    lastBeat=now;beats++;
   }
   if(frames%5==0){
    final int countBeats=beats;
    final int loud=(int)Math.round(Math.min(100,energy*1700));
    ui.post(()->{if(running)status.setText("Nivel de sonido: "+loud+" / 100 · Pulsos: "+countBeats);});
   }
  }
  if(running&&recorder==owned)ui.post(()->{
   if(running&&recorder==owned){
    stopCapture();status.setText("La captura de música se interrumpió. Podés reintentar.");
   }
  });
 }
 private void pulse(int duration,int amplitude){
  if(vibrator==null||!vibrator.hasVibrator())return;
  try{
   if(Build.VERSION.SDK_INT>=26){int amp=vibrator.hasAmplitudeControl()?amplitude:VibrationEffect.DEFAULT_AMPLITUDE;vibrator.vibrate(VibrationEffect.createOneShot(duration,amp));}
   else vibrator.vibrate(duration);
  }catch(RuntimeException ex){android.util.Log.w("Aurora","Vibración musical no disponible",ex);}
 }
 /** Goertzel power for low frequency musical pulses; no raw audio retained. */
 private static double lowBandPower(short[] data,int n){
  double total=0;
  for(int hz:new int[]{80,125,180}){
   double w=2*Math.PI*hz/SAMPLE_RATE;
   double coeff=2*Math.cos(w),s1=0,s2=0;
   for(int i=0;i<n;i++){
    double s=(data[i]/32768.0)+coeff*s1-s2;s2=s1;s1=s;
   }
   double power=s1*s1+s2*s2-coeff*s1*s2;
   total+=Math.max(0,power)/(n*n);
  }
  return total;
 }
 private void stopCapture(){
  running=false;
  AudioRecord old=recorder;recorder=null;
  if(old!=null){try{old.stop();}catch(Exception ignored){}try{old.release();}catch(Exception ignored){}}
  if(vibrator!=null)vibrator.cancel();
  if(start!=null)start.setText("▶ INICIAR MÚSICA HÁPTICA");
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  if(status!=null)status.setText("Detenido. Micrófono y vibración apagados.");
 }
 @Override protected void onPause(){super.onPause();stopCapture();ui.removeCallbacksAndMessages(null);}
 @Override protected void onDestroy(){stopCapture();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
