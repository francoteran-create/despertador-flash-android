package com.despertadorflash;
import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.os.*;

/** Keeps the selected alarm effects alive independently of activity launch. */
public final class AlarmEffectsService extends Service {
 public static final String STOP="com.despertadorflash.STOP_ALARM";
 public static volatile int activeId=-1;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final AlarmTone tone=new AlarmTone();
 private TorchFlasher torch;
 private Vibrator vibrator;
 private PowerManager.WakeLock wake;
 private final Runnable timeout=()->stopSelf();
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  if(i==null||STOP.equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  int id=i.getIntExtra("id",-1);
  if(id<0||id>=5){stopSelf();return START_NOT_STICKY;}
  clearEffects();
  android.content.SharedPreferences p=getSharedPreferences("aurora",0);
  String channel="aurora_alarm_effects_428";
  NotificationManager manager=getSystemService(NotificationManager.class);
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel ch=new NotificationChannel(channel,"Alarmas AURORA 4.28",NotificationManager.IMPORTANCE_HIGH);
   ch.setSound(null,null);ch.enableVibration(false);ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
   manager.createNotificationChannel(ch);
  }
  Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("ring",true).putExtra("alarm_id",id);
  PendingIntent tap=PendingIntent.getActivity(this,1000+id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,6000,new Intent(this,AlarmEffectsService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
  builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("AURORA · Despertador").setContentText("Es hora de despertar").setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC).setPriority(Notification.PRIORITY_MAX).setOngoing(true).setContentIntent(tap).setFullScreenIntent(tap,true).addAction(android.R.drawable.ic_menu_close_clear_cancel,"DETENER",stop);
  activeId=id;
  try{startForeground(2500,builder.build());}catch(RuntimeException ex){activeId=-1;android.util.Log.e("Aurora","No se pudo iniciar servicio de alarma",ex);stopSelf();return START_NOT_STICKY;}
  int duration=Math.max(5,Math.min(180,p.getInt("alarm_duration"+id,60)));
  try{PowerManager pm=getSystemService(PowerManager.class);wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Aurora:Alarm");wake.acquire((duration+5)*1000L);}catch(RuntimeException ex){android.util.Log.w("Aurora","Wake lock no disponible",ex);}
  if(p.getBoolean("alarm_vibration"+id,true)){
   try{vibrator=getSystemService(Vibrator.class);long[] pattern={0,900,300,900,300,900,300};
    if(vibrator!=null&&vibrator.hasVibrator()){
     AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
     if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createWaveform(pattern,0),attrs);else vibrator.vibrate(pattern,0,attrs);
    }
   }catch(RuntimeException ex){android.util.Log.w("Aurora","Vibración de alarma no disponible",ex);}
  }
  if(p.getBoolean("alarm_flash"+id,p.getBoolean("torch_alert",true))){
   torch=new TorchFlasher(this);
   boolean started=torch.start();
   p.edit().putString("last_alarm_flash"+id,started?"Android aceptó encender el LED; falta confirmar visualmente":torch.getLastError()).apply();
   if(!started){
    android.util.Log.w("Aurora","Flash de alarma: "+torch.getLastError());
    builder.setContentText("Flash no disponible: "+torch.getLastError());
    try{manager.notify(2500,builder.build());}catch(RuntimeException ignored){}
   }
  }else p.edit().putString("last_alarm_flash"+id,"Desactivado por configuración").apply();
  if(p.getBoolean("alarm_sound"+id,false))tone.play(p.getInt("alarm_tone"+id,0),p.getInt("alarm_volume"+id,75),true);
  handler.postDelayed(timeout,duration*1000L);
  return START_NOT_STICKY;
 }
 private void clearEffects(){
  handler.removeCallbacks(timeout);
  tone.stop();
  if(torch!=null){torch.stop();torch=null;}
  if(vibrator!=null){vibrator.cancel();vibrator=null;}
  if(wake!=null){if(wake.isHeld())wake.release();wake=null;}
 }
 @Override public void onDestroy(){clearEffects();activeId=-1;stopForeground(true);super.onDestroy();}
}
