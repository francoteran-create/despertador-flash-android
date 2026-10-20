package com.despertadorflash;
import android.app.*;
import android.content.*;
import android.os.Build;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  String action=i.getAction();
  if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&!Intent.ACTION_TIME_CHANGED.equals(action)&&!Intent.ACTION_TIMEZONE_CHANGED.equals(action))return;
  for(int id=0;id<5;id++)MainActivity.schedule(c,id);
  long end=c.getSharedPreferences("aurora",0).getLong("timer_end",0);
  if(end<=0)return;
  if(end<=System.currentTimeMillis()){new TimerReceiver().onReceive(c,new Intent(c,TimerReceiver.class));return;}
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  PendingIntent pi=PendingIntent.getBroadcast(c,3100,new Intent(c,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return;
   am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,pi);
  }catch(SecurityException e){android.util.Log.w("Aurora","No se pudo recuperar el temporizador",e);}
 }
}