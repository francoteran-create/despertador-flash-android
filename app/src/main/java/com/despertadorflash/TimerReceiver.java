package com.despertadorflash;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
public class TimerReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  android.content.SharedPreferences prefs=context.getSharedPreferences("aurora",0);
  long expected=prefs.getLong("timer_end",0L);
  if(expected<=0L)return; // Stale notification after cancellation.
  long nowWall=System.currentTimeMillis();
  int currentBoot=Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  long safeElapsed=TimerBootClock.elapsedDeadlineForBoot(
   prefs.getLong("timer_end_elapsed",0L),prefs.getInt("timer_boot_count",-1),currentBoot);
  long remaining=TimerDeadline.remaining(expected,safeElapsed,nowWall,SystemClock.elapsedRealtime());
  if(remaining>2000L){
   // A wall-clock jump may fire the RTC alarm too early. Re-arm using elapsed time.
   AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(alarmManager==null)return;
   PendingIntent next=PendingIntent.getBroadcast(context,3100,new Intent(context,TimerReceiver.class),
     PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   try{
    if(Build.VERSION.SDK_INT>=31&&!alarmManager.canScheduleExactAlarms())return;
    long newWall=TimerDeadline.newWallDeadline(remaining,nowWall);
    prefs.edit().putLong("timer_end",newWall).apply();
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,newWall,next);
   }catch(SecurityException error){android.util.Log.w("Aurora","No se pudo rearmar el temporizador",error);}
   return;
  }
  prefs.edit().remove("timer_end").remove("timer_end_elapsed").remove("timer_boot_count").apply();
  Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("timer_finished",true);
  PendingIntent pending=PendingIntent.getActivity(context,3100,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  String channel="aurora_timer";
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(channel,"Temporizadores AURORA",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true);manager.createNotificationChannel(ch);}
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
  b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("AURORA · Tiempo cumplido").setContentText("Tu temporizador terminó").setContentIntent(pending).setFullScreenIntent(pending,true).setVisibility(Notification.VISIBILITY_PUBLIC).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[]{0,700,200,700,200,700});
  try{manager.notify(3100,b.build());}catch(SecurityException ex){android.util.Log.w("Aurora","Falta permiso de notificaciones",ex);}
  Vibrator v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(new long[]{0,700,200,700,200,700},-1));else v.vibrate(new long[]{0,700,200,700,200,700},-1);}
 }
}