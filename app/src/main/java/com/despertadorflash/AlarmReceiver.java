package com.despertadorflash;
import android.content.*;
import android.os.*;
import android.app.*;
public class AlarmReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  int id=i.getIntExtra("id",0);
  if(!i.getBooleanExtra("snooze",false))MainActivity.schedule(c,id);
  Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("ring",true);
  try {c.startActivity(open);}catch(Exception e){android.util.Log.e("Aurora","Cannot open alarm",e);}
  Vibrator v=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){long[] pat={0,900,200,900,200,900,200,900};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}
 }
}