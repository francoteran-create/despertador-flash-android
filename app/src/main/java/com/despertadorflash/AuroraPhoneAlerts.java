package com.despertadorflash;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * One phone-alert pipeline: distinct from optional wearable illustration.
 * Does not initiate capture or fabricate events; preview always says PRUEBA.
 */
public final class AuroraPhoneAlerts {
 public static final String CHANNEL="aurora_phone_sound_v422";
 public static final String PREF_LAST="phone_alert_last_result";
 public static final String PREF_COUNT="phone_alert_posted_count";
 private AuroraPhoneAlerts(){}
 public static void createChannel(Context c){
  if(Build.VERSION.SDK_INT<26)return;
  NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  NotificationChannel channel=new NotificationChannel(CHANNEL,
   "AURORA · Avisos de sonidos en teléfono",NotificationManager.IMPORTANCE_HIGH);
  channel.setDescription("Alertas ilustradas por sonidos detectados; vibración y presentación dependen de Android");
  channel.enableVibration(true);
  channel.setVibrationPattern(new long[]{0,220,100,220,120,320});
  channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  manager.createNotificationChannel(channel);
 }
 public static String readiness(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
   !=PackageManager.PERMISSION_GRANTED)return "FALTA PERMISO DE NOTIFICACIONES";
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm==null)return "SIN SERVICIO DE NOTIFICACIONES";
  if(Build.VERSION.SDK_INT>=24&&!nm.areNotificationsEnabled())return "AVISOS BLOQUEADOS PARA AURORA";
  createChannel(c);
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=nm.getNotificationChannel(CHANNEL);
   if(channel==null)return "CANAL NO CREADO";
   if(channel.getImportance()==NotificationManager.IMPORTANCE_NONE)
    return "CANAL DE AVISOS BLOQUEADO";
  }
  return "LISTO PARA ENVIAR (NO GARANTIZA RECEPCIÓN)";
 }
 public static String post(Context c,String label,boolean preview){
  if(label==null||label.isEmpty())return "ETIQUETA VACÍA";
  String ready=readiness(c);
  if(!ready.startsWith("LISTO")){saveStatus(c,ready);return ready;}
  if(!preview&&!SoundAlertPreferences.isEnabled(c,label)){
   saveStatus(c,"CATEGORÍA DESACTIVADA: "+SoundAlertPreferences.category(label));
   return "CATEGORÍA DESACTIVADA";
  }
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm==null)return "SIN SERVICIO";
  AuroraEventVisualPolicy.Event event=AuroraEventVisualPolicy.fromConfirmedLabel(label);
  Intent open=new Intent(c,SenseActivity.class).putExtra(SenseActivity.EXTRA_MODE,SenseActivity.MODE_SOUND);
  PendingIntent tap=PendingIntent.getActivity(c,5831,open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_dialog_alert)
   .setContentTitle(preview?"AURORA · PRUEBA DE AVISO":"AURORA · "+AuroraWatchAlert.title(event))
   .setContentText(preview?"PRUEBA de notificación, sin escuchar sonidos":label)
   .setAutoCancel(true).setContentIntent(tap)
   .setCategory(Notification.CATEGORY_EVENT)
   .setVisibility(Notification.VISIBILITY_PUBLIC)
   .setPriority(Notification.PRIORITY_HIGH)
   .setVibrate(new long[]{0,220,100,220,120,320});
  if(!preview&&AuroraWatchAlert.isEnabled(c)){
   // Wearable image is an optional decoration; alerts never depend on it.
   AuroraWatchAlert.illustrate(c,b,label);
  }
  try{
   int id=preview?5832:SoundAlertPreferences.notificationId(SoundAlertPreferences.category(label));
   String tag=preview?"aurora_phone_preview":
    SoundAlertPreferences.notificationTag(SoundAlertPreferences.category(label),label);
   nm.notify(tag,id,b.build());
   c.getSharedPreferences("aurora",0).edit().putInt(PREF_COUNT,
    c.getSharedPreferences("aurora",0).getInt(PREF_COUNT,0)+1).apply();
   saveStatus(c,(preview?"PRUEBA ENVIADA A ANDROID":"EVENTO ENVIADO A ANDROID")+" · "+label);
   return "ENVIADO A ANDROID (revisar bandeja)";
  }catch(RuntimeException e){
   String failure="ERROR AL PUBLICAR: "+e.getClass().getSimpleName();
   saveStatus(c,failure);
   return failure;
  }
 }
 private static void saveStatus(Context c,String s){
  c.getSharedPreferences("aurora",0).edit().putString(PREF_LAST,s)
   .putLong("phone_alert_last_at",System.currentTimeMillis()).apply();
 }
 public static String report(Context c){
  String base=readiness(c);
  String last=c.getSharedPreferences("aurora",0).getString(PREF_LAST,"SIN INTENTOS");
  int posted=c.getSharedPreferences("aurora",0).getInt(PREF_COUNT,0);
  return "Estado: "+base+"\nÚltima solicitud: "+last+
   "\nAvisos entregados a Android por AURORA: "+posted+
   "\nSi dice ENVIADO pero no aparece, revisar Notificaciones de AURORA, "+
   "No molestar y el canal de avisos del teléfono. El contador no confirma su visualización.";
 }
}
