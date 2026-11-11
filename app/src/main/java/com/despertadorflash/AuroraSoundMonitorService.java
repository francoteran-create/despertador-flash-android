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
 public static final String KEY_ACTIVE="monitor_active", KEY_LAST="monitor_last_sound", KEY_ERROR="monitor_last_error", KEY_HEARTBEAT="monitor_heartbeat", KEY_STARTED="monitor_started";
 private static final String CHANNEL="aurora_monitor", EVENTS="aurora_monitor_events";
 private static final int FOREGROUND_ID=5400;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ExecutorService eventWorker=Executors.newSingleThreadExecutor();
 private final Handler ui=new Handler(Looper.getMainLooper());
 private volatile boolean listening=false;
 private volatile long sessionId=0L;
 private volatile AudioRecord recorder;
 private volatile AudioClassifier classifier;
 private final SoundEventGate gate=new SoundEventGate();
 private volatile long lastClassifierCallbackElapsed=0L;
 private volatile long lastHeartbeatAt=0L;
 private long lastAlertsCheckAt=0L;
 private volatile SenseHistory history;
 @Override public void onCreate(){super.onCreate();history=new SenseHistory(this);createChannels();
  // Reset any stale status from a previous process termination; no automatic restart.
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&STOP.equals(intent.getAction())){shutdown();stopSelf();return START_NOT_STICKY;}
  if(intent==null||!START.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(listening)return START_NOT_STICKY;
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,"El permiso de micrófono está desactivado").apply();
   Log.w("AURORA","No se inicia escucha sin permiso de micrófono");stopSelf();return START_NOT_STICKY;
  }
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,"El permiso de notificaciones está desactivado").apply();
   Log.w("AURORA","Sin permiso de notificaciones no se inicia el monitoreo");stopSelf();return START_NOT_STICKY;
  }
  NotificationManager availableAlerts=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=24&&availableAlerts!=null&&!availableAlerts.areNotificationsEnabled()){
   getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,
    "Las notificaciones están bloqueadas; no se activó la escucha sin posibilidad de aviso").apply();
   stopSelf();return START_NOT_STICKY;
  }
  try{
   Notification notice=status("Iniciando micrófono y reconocimiento…");
   if(Build.VERSION.SDK_INT>=29)startForeground(FOREGROUND_ID,notice,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
   else startForeground(FOREGROUND_ID,notice);
  }catch(Exception ex){
   getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,"Android no permitió activar el micrófono en segundo plano").apply();
   Log.e("AURORA","No se pudo iniciar servicio de micrófono",ex);stopSelf();return START_NOT_STICKY;
  }
  listening=true;sessionId++;gate.reset();lastHeartbeatAt=0L;lastAlertsCheckAt=0L;lastClassifierCallbackElapsed=0L;
  final long run=sessionId;
  final SenseHistory currentHistory=new SenseHistory(this);
  history=currentHistory;
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,true).remove(KEY_ERROR)
   .putLong(KEY_STARTED,System.currentTimeMillis()).putLong(KEY_HEARTBEAT,0L).apply();
  // Histories and notifications are serialized on a different thread from audio.
  eventWorker.execute(()->currentHistory.begin("sonidos"));
  worker.execute(()->classifyLoop(run));
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
  for(int i=0;i<SoundAlertPreferences.KEYS.length;i++){
   String key=SoundAlertPreferences.KEYS[i];
   NotificationChannel group=new NotificationChannel(SoundAlertPreferences.channel(key),
    "AURORA · "+SoundAlertPreferences.LABELS[i],NotificationManager.IMPORTANCE_HIGH);
   group.setDescription("Avisos del entorno detectados aproximadamente por IA");
   group.enableVibration(true);
   group.setVibrationPattern(SoundAlertPreferences.vibrationPattern(key));
   group.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
   n.createNotificationChannel(group);
  }
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
 private void classifyLoop(final long run){
  if(!listening||sessionId!=run)return;
  int failures=0;
  while(listening&&sessionId==run){
   AudioRecord localRecord=null;AudioClassifier localClassifier=null;
   final java.util.concurrent.atomic.AtomicReference<String> callbackError=new java.util.concurrent.atomic.AtomicReference<>();
   long started=SystemClock.elapsedRealtime();
   long retryDelay=0L;
   try{
    lastClassifierCallbackElapsed=0L;
    gate.reset();
   AudioClassifierOptions options=AudioClassifierOptions.builder().setBaseOptions(BaseOptions.builder().setModelAssetPath("yamnet.tflite").build())
    .setRunningMode(RunningMode.AUDIO_STREAM).setMaxResults(40).setScoreThreshold(.12f)
    .setResultListener(result->{
     if(!listening||sessionId!=run)return;
     long now=SystemClock.elapsedRealtime();
     boolean firstResult=lastClassifierCallbackElapsed==0L;
     lastClassifierCallbackElapsed=now;
     if(firstResult)ui.post(()->{
      if(listening&&sessionId==run){
       NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
       if(nm!=null)nm.notify(FOREGROUND_ID,status("Detector respondiendo · tocá para abrir AURORA"));
      }
     });
     // Heartbeat means the model actually responded, not merely that AudioRecord exists.
     if(now-lastHeartbeatAt>=15000L||lastHeartbeatAt==0L){
      lastHeartbeatAt=now;
      getSharedPreferences("aurora",0).edit().putLong(KEY_HEARTBEAT,System.currentTimeMillis()).apply();
     }
     if(result.classificationResults().isEmpty()||result.classificationResults().get(0).classifications().isEmpty())return;
     List<Category> categories=result.classificationResults().get(0).classifications().get(0).categories();
     java.util.Map<String,Float> rawScores=new java.util.LinkedHashMap<>();
     for(Category item:categories){
      String raw=item.categoryName();
      Float previous=rawScores.get(raw);
      if(previous==null||item.score()>previous)rawScores.put(raw,item.score());
     }
     java.util.Map<String,Float> strongestByLabel=SoundEvidence.fromRawScores(rawScores);
     // Two simultaneous sound types can both be confirmed and reported.
     for(java.util.Map.Entry<String,Float> detected:strongestByLabel.entrySet()){
      consider(detected.getKey(),Math.round(detected.getValue()*100f),run);
     }
    }).setErrorListener(err->callbackError.compareAndSet(null,
     err==null?"Fallo del clasificador":err.getMessage())).build();
   localClassifier=AudioClassifier.createFromOptions(this,options);classifier=localClassifier;
   localRecord=localClassifier.createAudioRecord(1,16000,16000);recorder=localRecord;
   AudioData samples=AudioData.create(AudioData.AudioDataFormat.create(localRecord.getFormat()),SoundAudioCapturePolicy.BLOCK_SAMPLES);
   if(!listening||sessionId!=run)return;
   localRecord.startRecording();
   final long monitoringStarted=SystemClock.elapsedRealtime();
   if(localRecord.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("El micrófono no comenzó a grabar");
   int emptyReads=0;
   while(listening&&sessionId==run){
    if(callbackError.get()!=null)throw new IllegalStateException("El clasificador interrumpió la escucha");
    int count=samples.load(localRecord);
    if(count>0){localClassifier.classifyAsync(samples,SystemClock.uptimeMillis());emptyReads=0;}
    else {SystemClock.sleep(120);if(++emptyReads>=12)throw new IllegalStateException("Micrófono sin muestras");}
    long now=SystemClock.elapsedRealtime();
    long lastInference=lastClassifierCallbackElapsed>0L?lastClassifierCallbackElapsed:monitoringStarted;
    if(SoundRecoveryPolicy.isStalled(lastInference,now))
     throw new IllegalStateException("El detector dejó de responder");
    // Recheck user notification permission: a monitor with every alert blocked is misleading.
    if(Build.VERSION.SDK_INT>=24&&now-lastAlertsCheckAt>15000L){
     lastAlertsCheckAt=now;
     NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
     if(nm!=null&&!nm.areNotificationsEnabled()){
      throw new IllegalStateException("Android bloqueó las notificaciones mientras AURORA escuchaba");
     }
    }
   }
   }catch(Exception error){
    if(!listening||sessionId!=run)break;
    Log.w("AURORA","Interrupción en escucha continua, intentando recuperación",error);
    String why=error.getMessage()==null?"Error de audio":error.getMessage();
    if(why.contains("notificaciones")){
     reportFailure("Android bloqueó las notificaciones; escucha detenida",run);break;
    }
    if(SystemClock.elapsedRealtime()-started>30000L)failures=0;
    retryDelay=SoundRecoveryPolicy.delayAfterFailure(++failures);
    if(retryDelay<0){
     reportFailure("No se pudo recuperar el detector; escucha detenida",run);break;
    }
    final int retry=failures;
    getSharedPreferences("aurora",0).edit()
     .putString(KEY_ERROR,"Recuperando escucha · intento "+retry+" de 3").apply();
    ui.post(()->{
     if(!listening||sessionId!=run)return;
     NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
     if(nm!=null)nm.notify(FOREGROUND_ID,
      status("RECONECTANDO ("+retry+"/3) · no detecta durante la reconexión"));
    });
   }
   finally{
   if(localRecord!=null){try{localRecord.stop();}catch(Exception ignored){}localRecord.release();}
   if(localClassifier!=null)localClassifier.close();
   if(recorder==localRecord)recorder=null;
   if(classifier==localClassifier)classifier=null;
   }
   if(retryDelay>0L)SystemClock.sleep(retryDelay);
  }
 }
 private void reportFailure(String message,long run){
  if(!listening||sessionId!=run)return;
  getSharedPreferences("aurora",0).edit().putString(KEY_ERROR,message).apply();
  ui.post(()->{
   if(!listening||sessionId!=run)return;
   shutdown();
   stopSelf();
   // Explicit warning: a stopped detector must not appear to protect the user.
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   Intent open=new Intent(this,MainActivity.class);
   PendingIntent tap=PendingIntent.getActivity(this,5699,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,EVENTS):new Notification.Builder(this);
   b.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("AURORA · Escucha detenida")
    .setContentText("No se están detectando sonidos. Tocá para revisar.")
    .setPriority(Notification.PRIORITY_HIGH).setCategory(Notification.CATEGORY_ERROR).setAutoCancel(true).setContentIntent(tap);
   try{notifications.notify(5699,b.build());}catch(SecurityException ignored){}
  });
 }
 private synchronized void consider(String found,int percent,long run){
  if(!listening||sessionId!=run)return;
  int sensitivity=getSharedPreferences("aurora",0).getInt("monitor_sensitivity",1);
  if(!gate.accept(found,percent,sensitivity,SystemClock.elapsedRealtime()))return;
  final SenseHistory savedHistory=history;
  // No synchronous file writes, SharedPreferences writes, or NotificationManager
  // calls on MediaPipe's classifier callback thread.
  eventWorker.execute(()->{
   savedHistory.event(found+" · puntaje de IA "+percent+"/100");
   getSharedPreferences("aurora",0).edit().putString(KEY_LAST,found+" · puntaje IA "+percent+"/100").apply();
   if(!listening||sessionId!=run)return;
   if(!SoundAlertPreferences.isEnabled(this,found))return;
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   if(nm==null)return;
   String group=SoundAlertPreferences.category(found);
   Intent open=new Intent(this,SenseActivity.class).putExtra(SenseActivity.EXTRA_MODE,SenseActivity.MODE_SOUND);
   PendingIntent tap=PendingIntent.getActivity(this,5600,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,SoundAlertPreferences.channel(group)):new Notification.Builder(this);
   b.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("AURORA · Sonido detectado")
    .setContentText(found+" · puntaje IA "+percent+"/100").setContentIntent(tap)
    .setCategory(Notification.CATEGORY_STATUS).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PUBLIC)
    .setPriority(Notification.PRIORITY_HIGH).setVibrate(SoundAlertPreferences.vibrationPattern(group));
   try{nm.notify(SoundAlertPreferences.notificationTag(group,found),SoundAlertPreferences.notificationId(group),b.build());}
   catch(SecurityException ignored){}
  });
 }
 private synchronized void shutdown(){
  if(!listening){getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();return;}
  listening=false;sessionId++;
  getSharedPreferences("aurora",0).edit().putBoolean(KEY_ACTIVE,false).apply();
  // Release a blocking audio read on the capture worker before it disposes native objects.
  AudioRecord active=recorder;
  if(active!=null){try{if(active.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)active.stop();}catch(Exception ignored){}}
  final SenseHistory closingHistory=history;
  eventWorker.execute(()->{
   try{
    java.io.File saved=closingHistory.finish();
    if(saved!=null){
     SavedFolderManager copies=new SavedFolderManager(this);
     if(copies.selectedFolder()!=null)copies.copyOne(saved);
    }
   }catch(Exception e){Log.w("AURORA","No se pudo guardar o copiar el registro",e);}
  });
 }
 @Override public void onDestroy(){
  shutdown();worker.shutdownNow();eventWorker.shutdown();ui.removeCallbacksAndMessages(null);super.onDestroy();
 }
 @Override public IBinder onBind(Intent intent){return null;}
}
