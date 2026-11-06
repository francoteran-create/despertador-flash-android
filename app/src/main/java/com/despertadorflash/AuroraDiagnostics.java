package com.despertadorflash;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shareable support snapshot. No personal profile, recognized phrases or audio. */
public final class AuroraDiagnostics {
 private AuroraDiagnostics(){}
 public static String report(Context context){
  SharedPreferences p=context.getSharedPreferences("aurora",0);
  StringBuilder out=new StringBuilder();
  out.append("AURORA - REPORTE TECNICO LOCAL\n");
  out.append("Fecha: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm",new Locale("es","AR")).format(new Date())).append("\n");
  out.append("Dispositivo: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
  out.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
  out.append("Micrófono: ").append(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"AUTORIZADO":"SIN PERMISO").append("\n");
  out.append("Cámara/flash: ").append(context.checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED?"AUTORIZADA":"SIN PERMISO").append("\n");
  NotificationManager notification=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  boolean enabled=(Build.VERSION.SDK_INT<24||notification==null||notification.areNotificationsEnabled());
  boolean permission=(Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);
  out.append("Notificaciones del sistema: ").append(enabled&&permission?"HABILITADAS":"BLOQUEADAS").append("\n");
  AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
  boolean exact=alarms!=null&&(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms());
  out.append("Alarmas exactas: ").append(exact?"PERMITIDAS":"NO DISPONIBLES").append("\n");
  if(Build.VERSION.SDK_INT>=34&&notification!=null)out.append("Alertas pantalla completa: ").append(notification.canUseFullScreenIntent()?"PERMITIDAS":"RESTRINGIDAS").append("\n");
  if(Build.VERSION.SDK_INT>=23){
   PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
   out.append("Excepción optimización batería: ").append(pm!=null&&pm.isIgnoringBatteryOptimizations(context.getPackageName())?"SI":"NO").append("\n");
  }
  MonitorHealth.State health=MonitorHealth.evaluate(p.getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false),
   p.getLong(AuroraSoundMonitorService.KEY_STARTED,0L),
   p.getLong(AuroraSoundMonitorService.KEY_HEARTBEAT,0L),
   System.currentTimeMillis(),p.getString(AuroraSoundMonitorService.KEY_ERROR,""));
  out.append("Monitor de sonidos: ").append(MonitorHealth.label(health)).append("\n");
  String error=p.getString(AuroraSoundMonitorService.KEY_ERROR,"");
  if(!error.isEmpty())out.append("Último error del detector: ").append(error).append("\n");
  int activeAlarms=0;
  for(int id=0;id<5;id++)if(p.getBoolean("on"+id,false))activeAlarms++;
  out.append("Alarmas habilitadas: ").append(activeAlarms).append("\n");
  out.append("Temporizador pendiente: ").append(p.getLong("timer_end",0L)>0?"SI":"NO").append("\n");
  out.append("AVISO: detector aproximado; no sustituye equipos certificados.\n");
  out.append("Privacidad: no se incluye audio, transcripciones ni información del perfil.\n");
  return out.toString();
 }
}
