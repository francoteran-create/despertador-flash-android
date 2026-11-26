package com.despertadorflash;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;

/**
 * Phone notification enriched with an illustrated sound. Compatible Wear OS
 * watches MAY mirror this notification if paired, connected and allowed by
 * their companion app. A large illustration is not guaranteed in every
 * watch UI; a dedicated Wear OS module is still planned.
 */
public final class AuroraWatchAlert {
 private AuroraWatchAlert(){}
 public static final String PREF="aurora_watch_visual_notifications";
 private static final String CHANNEL="aurora_watch_visual_v1";
 private static long lastTestElapsed=-1L;

 public static boolean isEnabled(Context c){
  return c.getSharedPreferences("aurora",0).getBoolean(PREF,true);
 }
 public static String title(AuroraEventVisualPolicy.Event e){
  switch(e){
   case DOOR:return "PUERTA / GOLPES";
   case DOG:return "PERRO LADRANDO";
   case HORN:return "BOCINA";
   case MUSIC:return "MÚSICA";
   case VOICES:return "GENTE HABLANDO";
   case SIREN:return "SIRENA / ALARMA";
   case BABY:return "LLANTO";
   case PHONE:return "TELÉFONO";
   case OTHER:return "SONIDO DETECTADO";
   default:return "SIN EVENTO";
  }
 }
 public static void ensureChannel(Context context){
  if(Build.VERSION.SDK_INT<26)return;
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  NotificationChannel c=new NotificationChannel(CHANNEL,"AURORA · Avisos ilustrados al reloj",
   NotificationManager.IMPORTANCE_HIGH);
  c.setDescription("Eventos confirmados con imagen grande y vibración; un reloj compatible puede reflejar el aviso");
  c.enableVibration(true);
  c.setVibrationPattern(new long[]{0,240,120,240,120,340});
  c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  manager.createNotificationChannel(c);
 }

 /** Paints an actual black-and-white picture. Does not store any image/audio. */
 public static Bitmap picture(AuroraEventVisualPolicy.Event event){
  Bitmap bitmap=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888);
  Canvas c=new Canvas(bitmap);
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  p.setColor(Color.BLACK);c.drawColor(Color.BLACK);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(13f);p.setStrokeCap(Paint.Cap.ROUND);
  p.setStrokeJoin(Paint.Join.ROUND);p.setColor(Color.WHITE);
  c.save();c.translate(180f,160f);
  Path path=new Path();
  switch(event){
   case DOOR:
    c.drawRoundRect(-93,-112,91,113,12,12,p);
    c.drawCircle(55,13,7,p);
    c.drawLine(108,-35,138,-35,p);c.drawLine(108,5,147,5,p);
    break;
   case DOG:
    c.drawOval(-85,-75,85,78,p);
    path.moveTo(-65,-51);path.lineTo(-110,-125);path.lineTo(-110,9);c.drawPath(path,p);
    path.reset();path.moveTo(65,-51);path.lineTo(110,-125);path.lineTo(110,9);c.drawPath(path,p);
    c.drawCircle(-30,-12,7,p);c.drawCircle(30,-12,7,p);
    c.drawOval(-13,16,13,36,p);
    c.drawLine(116,-40,143,-55,p);c.drawLine(116,3,147,3,p);
    break;
   case HORN:
    c.drawRoundRect(-125,-29,-62,29,5,5,p);
    path.moveTo(-62,-29);path.lineTo(80,-95);path.lineTo(80,95);
    path.lineTo(-62,29);path.close();c.drawPath(path,p);
    c.drawLine(102,-64,136,-85,p);c.drawLine(112,0,150,0,p);
    c.drawLine(102,64,136,85,p);break;
   case MUSIC:
    c.drawLine(-54,-88,-54,58,p);c.drawLine(-54,-88,78,-116,p);
    c.drawLine(78,-116,78,31,p);
    c.drawOval(-98,39,-54,73,p);c.drawOval(34,12,79,48,p);break;
   case VOICES:
    c.drawOval(-123,-100,37,23,p);c.drawOval(-12,-18,121,107,p);
    c.drawLine(-83,16,-110,57,p);c.drawLine(83,100,112,125,p);break;
   case SIREN:
    c.drawRoundRect(-114,52,114,91,9,9,p);
    path.moveTo(-77,52);path.quadTo(-71,-108,0,-112);
    path.quadTo(72,-108,77,52);c.drawPath(path,p);
    c.drawLine(0,-122,0,-148,p);break;
   case BABY:
    c.drawCircle(0,0,103,p);c.drawCircle(-34,-20,6,p);
    c.drawCircle(34,-20,6,p);c.drawOval(-22,18,22,75,p);break;
   case PHONE:
    c.drawRoundRect(-73,-118,73,117,15,15,p);
    c.drawLine(-33,-92,33,-92,p);c.drawCircle(0,92,6,p);break;
   case OTHER:
   default:
    c.drawCircle(0,0,101,p);c.drawLine(0,-66,0,15,p);
    c.drawCircle(0,56,6,p);break;
  }
  c.restore();
  p.clearShadowLayer();p.setStyle(Paint.Style.FILL);
  p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);
  p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextSize(22f);
  c.drawText(title(event),180f,325f,p);
  return bitmap;
 }

 /** Used by existing confirmed notifications in background; no duplicate. */
 public static void illustrate(Context context,Notification.Builder builder,String label){
  if(!isEnabled(context))return;
  AuroraEventVisualPolicy.Event event=AuroraEventVisualPolicy.fromConfirmedLabel(label);
  if(event==AuroraEventVisualPolicy.Event.NONE)return;
  Bitmap art=picture(event);
  builder.setLargeIcon(art);
  builder.setStyle(new Notification.BigPictureStyle().bigPicture(art)
   .setBigContentTitle("AURORA · "+title(event)).setSummaryText(label));
 }

 /** Additional phone notification for confirmed foreground audio events. */
 public static void notifyForeground(Context c,String label){
  if(!isEnabled(c)||label==null||!SoundAlertPreferences.isEnabled(c,label))return;
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
   !=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(n==null)return;
  if(Build.VERSION.SDK_INT>=24&&!n.areNotificationsEnabled())return;
  ensureChannel(c);
  AuroraEventVisualPolicy.Event event=AuroraEventVisualPolicy.fromConfirmedLabel(label);
  if(event==AuroraEventVisualPolicy.Event.NONE)return;
  Intent open=new Intent(c,SenseActivity.class).putExtra(SenseActivity.EXTRA_MODE,SenseActivity.MODE_SOUND);
  PendingIntent tap=PendingIntent.getActivity(c,5600,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_dialog_alert)
   .setContentTitle("AURORA · "+title(event))
   .setContentText(label)
   .setContentIntent(tap)
   .setAutoCancel(true)
   .setCategory(Notification.CATEGORY_EVENT)
   .setVisibility(Notification.VISIBILITY_PUBLIC)
   .setPriority(Notification.PRIORITY_HIGH)
   .setVibrate(new long[]{0,240,120,240,120,340});
  illustrate(c,b,label);
  try{
   // Individual event label tags avoid replacing distinct simultaneous sounds.
   n.notify(SoundAlertPreferences.notificationTag(SoundAlertPreferences.category(label),label),
    SoundAlertPreferences.notificationId(SoundAlertPreferences.category(label)),b.build());
  }catch(SecurityException ignored){}
 }

 /** Diagnostics preview only. No fake event enters saved history. */
 public static void previewDoor(Context context){
  long now=SystemClock.elapsedRealtime();
  if(lastTestElapsed>=0&&now-lastTestElapsed<4000L)return;
  lastTestElapsed=now;
  if(!isEnabled(context))return;
  ensureChannel(context);
  NotificationManager n=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(n==null)return;
  if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
   !=PackageManager.PERMISSION_GRANTED)return;
  Intent open=new Intent(context,MainActivity.class);
  PendingIntent tap=PendingIntent.getActivity(context,5611,open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
  b.setSmallIcon(android.R.drawable.ic_dialog_alert)
   .setContentTitle("AURORA · PRUEBA DE RELOJ")
   .setContentText("PRUEBA: dibujo de puerta · no fue un sonido detectado")
   .setContentIntent(tap).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH)
   .setVisibility(Notification.VISIBILITY_PUBLIC)
   .setVibrate(new long[]{0,240,120,240,120,340});
  b.setStyle(new Notification.BigPictureStyle().bigPicture(
   picture(AuroraEventVisualPolicy.Event.DOOR))
   .setBigContentTitle("AURORA · PRUEBA PUERTA")
   .setSummaryText("Prueba visual; sin activar micrófono"));
  try{n.notify(5711,b.build());}catch(SecurityException ignored){}
 }
}
