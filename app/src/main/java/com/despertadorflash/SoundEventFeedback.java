package com.despertadorflash;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Short and non-looping feedback for confirmed environmental events.
 * Notification channels own background vibration; this class adds
 * foreground haptic feedback and optional bounded camera torch pulses.
 * Flash defaults OFF, requires an explicit setting and CAMERA permission.
 */
public final class SoundEventFeedback {
 private final Context context;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final SoundFeedbackPolicy policy=new SoundFeedbackPolicy();
 private TorchFlasher torch;
 private Runnable endFlash;
 private boolean closed=false;

 public SoundEventFeedback(Context context){
  this.context=context.getApplicationContext();
 }

 public void onConfirmed(String label,boolean vibrateInForeground){
  handler.post(()->{
   if(closed||label==null||!SoundAlertPreferences.isEnabled(context,label))return;
   long now=SystemClock.elapsedRealtime();
   if(!policy.allowEvent(now))return;
   String group=SoundAlertPreferences.category(label);
   if(vibrateInForeground&&context.getSharedPreferences("aurora",0)
     .getBoolean("monitor_sound_vibration",true)){
    Vibrator vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    if(vibrator!=null&&vibrator.hasVibrator()){
     long[] pattern=SoundAlertPreferences.vibrationPattern(group);
     try{
      if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(
       VibrationEffect.createWaveform(pattern,-1));
      else vibrator.vibrate(pattern,-1);
     }catch(SecurityException ignored){}
    }
   }
   boolean flashEnabled=context.getSharedPreferences("aurora",0)
    .getBoolean("monitor_sound_flash",false);
   if(!flashEnabled||Build.VERSION.SDK_INT<23||
     context.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED||
     !policy.allowFlash(now))return;
   if(torch==null)torch=new TorchFlasher(context);
   if(endFlash!=null){handler.removeCallbacks(endFlash);endFlash=null;}
   if(torch.start()){
    endFlash=()->{
     if(torch!=null)torch.stop();
     endFlash=null;
    };
    handler.postDelayed(endFlash,SoundFeedbackPolicy.FLASH_DURATION_MS);
   }
  });
 }

 public void stop(){
  closed=true;
  handler.post(()->{
   if(endFlash!=null){handler.removeCallbacks(endFlash);endFlash=null;}
   if(torch!=null)torch.stop();
  });
 }
}
