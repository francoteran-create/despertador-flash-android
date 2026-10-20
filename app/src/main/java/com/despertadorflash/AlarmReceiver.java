package com.despertadorflash;
import android.content.*;
import android.os.*;
import android.app.*;
import android.graphics.Color;
public class AlarmReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  int id=i.getIntExtra("id",0);
  if(!i.getBooleanExtra("snooze",false))MainActivity.schedule(c,id);
  Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("ring",true);
  PendingIntent tap=PendingIntent.getActivity(c,1000+id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  String channel="aurora_alarm";
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel ch=new NotificationChannel(channel,"Alarmas AURORA",NotificationManager.IMPORTANCE_HIGH);
   ch.setDescription("Alarmas visuales y vibratorias");ch.enableVibration(true);ch.enableLights(true);ch.setLightColor(Color.CYAN);ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
   manager.createNotificationChannel(ch);
  }
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);
  builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("AURORA · Despertador").setContentText("Es hora de despertar").setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC).setPriority(Notification.PRIORITY_MAX).setAutoCancel(true).setContentIntent(tap).setFullScreenIntent(tap,true).setVibrate(new long[]{0,900,200,900,200,900});
  try{manager.notify(2000+id,builder.build());}catch(SecurityException e){android.util.Log.w("Aurora","Notification permission missing",e);}
  Vibrator v=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){long[] pat={0,900,200,900,200,900,200,900};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}
 }
}