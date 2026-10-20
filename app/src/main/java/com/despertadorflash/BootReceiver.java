package com.despertadorflash;
import android.content.*;
public class BootReceiver extends BroadcastReceiver{
 public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))for(int id=0;id<5;id++)MainActivity.schedule(c,id);}
}