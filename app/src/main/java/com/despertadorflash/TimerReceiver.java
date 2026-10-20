package com.despertadorflash;
import android.app.*;
import android.content.*;
import android.os.*;
public class TimerReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  context.getSharedPreferences("aurora",0).edit().remove("timer_end").apply();
  Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("timer_finished",true);
  PendingIntent pending=PendingIntent.getActivity(context,3100,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  String channel="aurora_timer";
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(channel,"Temporizadores AURORA",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true);manager.createNotificationChannel(ch);}
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
  b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("AURORA · Tiempo cumplido").setContentText("Tu temporizador terminó").setContentIntent(pending).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[]{0,700,200,700,200,700});
  try{manager.notify(3100,b.build());}catch(SecurityException ex){android.util.Log.w("Aurora","Falta permiso de notificaciones",ex);}
  Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(new long[]{0,700,200,700,200,700},-1));else v.vibrate(new long[]{0,700,200,700,200,700},-1);}
 }
}