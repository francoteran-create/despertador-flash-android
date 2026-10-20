package com.despertadorflash;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.util.Log;
public final class TorchFlasher {
 private final Context context;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private CameraManager camera;
 private String cameraId;
 private boolean running=false;
 private boolean on=false;
 private final Runnable pulse=new Runnable(){
  @Override public void run(){
   if(!running)return;
   try{
    if(camera==null||cameraId==null){stop();return;}
    on=!on;camera.setTorchMode(cameraId,on);
    handler.postDelayed(this,on?400:600);
   }catch(Exception ex){Log.w("Aurora","Flash LED no disponible",ex);stop();}
  }
 };
 public TorchFlasher(Context context){this.context=context.getApplicationContext();}
 public boolean start(){
  stop();
  if(Build.VERSION.SDK_INT<23||context.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return false;
  try{
   camera=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
   if(camera==null)return false;
   for(String id:camera.getCameraIdList()){
    android.hardware.camera2.CameraCharacteristics chars=camera.getCameraCharacteristics(id);
    Boolean hasFlash=chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
    Integer facing=chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
    if(Boolean.TRUE.equals(hasFlash)&&facing!=null&&facing==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}
   }
   if(cameraId==null)return false;
   running=true;handler.post(pulse);return true;
  }catch(Exception ex){Log.w("Aurora","No se pudo iniciar flash",ex);stop();return false;}
 }
 public void stop(){
  running=false;handler.removeCallbacks(pulse);
  if(on&&camera!=null&&cameraId!=null){try{camera.setTorchMode(cameraId,false);}catch(Exception ignored){}}
  on=false;
 }
}
