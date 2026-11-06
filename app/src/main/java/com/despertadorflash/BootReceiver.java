package com.despertadorflash;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

/** Restores user-scheduled alerts after reboot, update, or a manual clock change.
 * Never starts microphone capture on boot.
 */
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(i==null)return;
  String action=i.getAction();
  boolean boot=Intent.ACTION_BOOT_COMPLETED.equals(action);
  if(!boot&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&
     !Intent.ACTION_TIME_CHANGED.equals(action)&&!Intent.ACTION_TIMEZONE_CHANGED.equals(action))return;
  for(int id=0;id<5;id++)MainActivity.schedule(c,id);
  SharedPreferences p=c.getSharedPreferences("aurora",0);
  // Preserve a pending ten-minute snooze after reboot or manual clock changes.
  AlarmManager snoozeManager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  for(int id=0;id<5;id++){
   long desired=p.getLong("snooze_until"+id,0L);
   if(desired<=0L)continue;
   long recovery=SnoozePolicy.recoverAt(desired,System.currentTimeMillis(),p.getBoolean("on"+id,false));
   if(recovery<=0L){MainActivity.cancelSnooze(c,id);continue;}
   if(snoozeManager==null)continue;
   if(Build.VERSION.SDK_INT>=31&&!snoozeManager.canScheduleExactAlarms())continue;
   Intent snoozed=new Intent(c,AlarmReceiver.class).putExtra("snooze",true).putExtra("id",id);
   PendingIntent delivery=PendingIntent.getBroadcast(c,4100+id,snoozed,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   try{snoozeManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,recovery,delivery);}
   catch(SecurityException error){android.util.Log.w("Aurora","No se pudo recuperar pospuesto",error);}
  }
  long endWall=p.getLong("timer_end",0L);
  if(endWall<=0L)return;
  long nowWall=System.currentTimeMillis();
  long nowElapsed=SystemClock.elapsedRealtime();
  int currentBoot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  long endElapsed=TimerBootClock.elapsedDeadlineForBoot(p.getLong("timer_end_elapsed",0L),
    p.getInt("timer_boot_count",-1),currentBoot);
  // Reboot broadcasts must rebase even when the boot counter is not available.
  if(boot)endElapsed=0L;
  long remaining=TimerDeadline.remaining(endWall,endElapsed,nowWall,nowElapsed);
  if(remaining<=0L){
   p.edit().putLong("timer_end",nowWall).remove("timer_end_elapsed").apply();
   new TimerReceiver().onReceive(c,new Intent(c,TimerReceiver.class));
   return;
  }
  long revisedWall=TimerDeadline.newWallDeadline(remaining,nowWall);
  p.edit().putLong("timer_end",revisedWall)
   .putLong("timer_end_elapsed",nowElapsed+remaining).putInt("timer_boot_count",currentBoot).apply();
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  if(am==null)return;
  PendingIntent pi=PendingIntent.getBroadcast(c,3100,new Intent(c,TimerReceiver.class),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){
    android.util.Log.w("Aurora","No se pudo restaurar temporizador: falta permiso exacto");return;
   }
   am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,revisedWall,pi);
  }catch(SecurityException ex){
   android.util.Log.w("Aurora","No se pudo restaurar temporizador",ex);
  }
 }
}