package com.despertadorflash;

import android.content.Context;
import android.content.SharedPreferences;

/** User controlled categories for foreground sound monitoring. */
public final class SoundAlertPreferences {
 private SoundAlertPreferences(){}
 public static final String[] LABELS={"Timbre y puertas","Sirenas y alarmas","Llanto y gritos","Mascotas","Impactos y roturas","Teléfonos","Tránsito","Otros sonidos"};
 public static final String[] KEYS={"doors","alarms","people","pets","impacts","phones","traffic","other"};
 public static String channel(String group){return "aurora_sound_"+group;}
 public static int notificationId(String group){
  for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(group))return 5700+i;
  return 5700+KEYS.length;
 }
 /** Distinct alert rhythms; Android users can override per-channel vibration in system settings. */
 public static long[] vibrationPattern(String group){
  switch(group){
   case "doors":return new long[]{0,190,110,190,110,190};
   case "alarms":return new long[]{0,650,160,650,160,650};
   case "people":return new long[]{0,470,110,470};
   case "pets":return new long[]{0,120,110,120,110,120};
   case "impacts":return new long[]{0,340,250,340};
   case "phones":return new long[]{0,200,80,200,80,450};
   case "traffic":return new long[]{0,350,150,350,150,350};
   default:return new long[]{0,220,170,220};
  }
 }
 public static boolean isEnabled(Context c,String text){
  String k=category(text);
  SharedPreferences p=c.getSharedPreferences("aurora",0);
  return p.getBoolean("monitor_alert_"+k,!"other".equals(k));
 }
 public static String category(String s){
  if(s.contains("TIMBRE")||s.contains("PUERTA")||s.contains("CAJÓN")||s.contains("CAMPANA"))return "doors";
  if(s.contains("SIRENA")||s.contains("ALARMA")||s.contains("PITIDO")||s.contains("INCENDIO"))return "alarms";
  if(s.contains("LLANTO")||s.contains("GRITO"))return "people";
  if(s.contains("PERRO")||s.contains("GATO")||s.contains("AVES"))return "pets";
  if(s.contains("GOLPE")||s.contains("IMPACTO")||s.contains("ROTURA")||s.contains("ESTALLIDO"))return "impacts";
  if(s.contains("TELÉFONO"))return "phones";
  if(s.contains("VEHÍCULO")||s.contains("BOCINA")||s.contains("MOTO")||s.contains("TREN"))return "traffic";
  return "other";
 }
}
