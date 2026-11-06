package com.despertadorflash;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioRecord;
import android.os.*;
import android.util.Log;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier.AudioClassifierOptions;
import com.google.mediapipe.tasks.audio.core.RunningMode;
import com.google.mediapipe.tasks.components.containers.AudioData;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.core.BaseOptions;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicitly started foreground microphone monitor. Never auto-start on boot. */
public final class AuroraSoundMonitorService extends Service {
 public static final String START="com.despertadorflash.START_MONITOR",STOP="com.despertadorflash.STOP_MONITOR";
 public static final String KEY_ACTIVE="monitor_active", KEY_LAST="monitor_last_sound", KEY_ERROR="monitor_last_error", KEY_HEARTBEAT="monitor_heartbeat";
 private static final String CHANNEL="aurora_monitor", EVENTS="aurora_monitor_events";
 private static final int FOREGROUND_ID=5400;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler ui=new Handler(Looper.getMainLooper());
 private volatile boolean listening=false;
 private volatile AudioRecord recorder;
 private volatile AudioClassifier classifier;
 private final SoundEventGate gate=new SoundEventGate();
 private long lastHeartbeatAt=0L;
 private SenseHistory history;
 @Override public void onCreate(){super.onCreate();history=new SenseHistory(this);createChannels();
  // Reset any stale status from a previous process termination; no automatic restart.
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&STOP.equals(intent.getAction())){shutdown();stopSelf();return START_NOT_STICKY;}
  if(intent==null||!START.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(listening)return START_NOT_STICKY;
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   Log.w("AURORA","No se inicia escucha sin permiso de micrófono");stopSelf();return START_NOT_STICKY;
  }
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   Log.w("AURORA","Sin permiso de notificaciones no se inicia el monitoreo");stopSelf();return START_NOT_STICKY;
  }
  try{
   Notification notice=status("Escucha activa · tocá para abrir AURORA");
   if(Build.VERSION.SDK_INT>=29)startForeground(FOREGROUND_ID,notice,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
   else startForeground(FOREGROUND_ID,notice);
  }catch(Exception ex){Log.e("AURORA","No se pudo iniciar servicio de micrófono",ex);stopSelf();return START_NOT_STICKY;}
  listening=true;gate.reset();lastHeartbeatAt=0L;
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,true).remove(KEY_ERROR).putLong(KEY_HEARTBEAT,System.currentTimeMillis()).apply();
  history.begin("sonidos");worker.execute(this::classifyLoop);
  return START_NOT_STICKY; // Do not restart microphone capture without user interaction.
 }
 private void createChannels(){
  if(Build.VERSION.SDK_INT<26)return;
  NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  NotificationChannel active=new NotificationChannel(CHANNEL,"AURORA · Escucha activa",NotificationManager.IMPORTANCE_LOW);
  active.setDescription("Indicador permanente mientras se usa el micrófono");
  active.setSound(null,null);n.createNotificationChannel(active);
  NotificationChannel alarms=new NotificationChannel(EVENTS,"AURORA · Sonidos detectados",NotificationManager.IMPORTANCE_HIGH);
  alarms.setDescription("Avisos visuales y vibratorios de sonidos seleccionados");
  alarms.enableVibration(true);alarms.enableLights(true);alarms.setLightColor(Color.CYAN);n.createNotificationChannel(alarms);
 }
 private Notification status(String message){
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent tap=PendingIntent.getActivity(this,5500,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Intent stop=new Intent(this,AuroraSoundMonitorService.class).setAction(STOP);
  PendingIntent off=PendingIntent.getService(this,5501,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  return b.setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("AURORA · Reconociendo sonidos")
   .setContentText(message).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
   .setContentIntent(tap).addAction(android.R.drawable.ic_media_pause,"DETENER ESCUCHA",off).build();
 }
 private void classifyLoop(){
  AudioRecord localRecord=null;AudioClassifier localClassifier=null;
  try{
   AudioClassifierOptions options=AudioClassifierOptions.builder().setBaseOptions(BaseOptions.builder().setModelAssetPath("yamnet.tflite").build())
    .setRunningMode(RunningMode.AUDIO_STREAM).setMaxResults(6).setScoreThreshold(.15f)
    .setResultListener(result->{
     if(!listening||result.classificationResults().isEmpty()||result.classificationResults().get(0).classifications().isEmpty())return;
     List<Category> categories=result.classificationResults().get(0).classifications().get(0).categories();
     String best=null;float confidence=0;
     for(Category item:categories){
      String name=SoundLabelsEs.name(item.categoryName());
      if(name!=null&&item.score()>confidence){best=name;confidence=item.score();}
     }
     if(best!=null)consider(best,Math.round(confidence*100));
    }).setErrorListener(err->{
     Log.e("AURORA","Clasificador de fondo: "+err.getMessage());
     reportFailure("Error interno del detector de sonidos");
    }).build();
   localClassifier=AudioClassifier.createFromOptions(this,options);classifier=localClassifier;
   localRecord=localClassifier.createAudioRecord(1,16000,16000);recorder=localRecord;
   AudioData samples=AudioData.create(AudioData.AudioDataFormat.create(localRecord.getFormat()),16000);
   localRecord.startRecording();
   if(localRecord.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("El micrófono no comenzó a grabar");
   int emptyReads=0;
   while(listening){
    int count=samples.load(localRecord);
    if(count>0){localClassifier.classifyAsync(samples,SystemClock.uptimeMillis());emptyReads=0;}
    else if(++emptyReads>=6)throw new IllegalStateException("Micrófono sin muestras");
    long now=SystemClock.elapsedRealtime();
    if(now-lastHeartbeatAt>=15000){
     lastHeartbeatAt=now;
     getSharedPreferences("aurora",0).edit().putLong(KEY_HEARTBEAT,System.currentTimeMillis()).apply();
    }
   }
  }catch(Exception error){
   if(listening){Log.e("AURORA","No se pudo mantener la escucha",error);reportFailure("El micrófono o el detector se interrumpió");}
  }
  finally{
   if(localRecord!=null){try{localRecord.stop();}catch(Exception ignored){}localRecord.release();}
   if(localClassifier!=null)localClassifier.close();
   recorder=null;classifier=null;
  }
 }
 private void reportFailure(String message){
  if(!listening)return;
  getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,message).apply();
  ui.post(()->{if(listening){shutdown();stopSelf();}});
 }
 private synchronized void consider(String found,int percent){
  if(!listening)return;
  int sensitivity=getSharedPreferences("aurora",0).getInt("monitor_sensitivity",1);
  if(!gate.accept(found,percent,sensitivity,SystemClock.elapsedRealtime()))return;
  getSharedPreferences("aurora",0).edit().putString(KEY_LAST,found+" · "+percent+"%").apply();
  history.event(found+" · confianza estimada "+percent+"%");
  if(!SoundAlertPreferences.isEnabled(this,found))return;
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  Intent open=new Intent(this,SenseActivity.class).putExtra(SenseActivity.EXTRA_MODE,SenseActivity.MODE_SOUND);
  PendingIntent tap=PendingIntent.getActivity(this,5600,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,EVENTS):new Notification.Builder(this);
  b.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("AURORA · Sonido detectado")
   .setContentText(found+" · estimación "+percent+"%").setContentIntent(tap)
   .setCategory(Notification.CATEGORY_STATUS).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PUBLIC)
   .setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[]{0,350,120,350,120,550});
  try{nm.notify(5601,b.build());}catch(SecurityException ignored){}
 }
 private synchronized void shutdown(){
  if(!listening){getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();return;}
  listening=false;
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();
  // Release a blocking audio read on the capture worker before it disposes native objects.
  AudioRecord active=recorder;
  if(active!=null){try{if(active.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)active.stop();}catch(Exception ignored){}}
  try{history.finish();}catch(Exception e){Log.w("AURORA","Error guardando registro de escucha",e);}
 }
 @Override public void onDestroy(){
  shutdown();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();
 }
 @Override public IBinder onBind(Intent intent){return null;}
}
