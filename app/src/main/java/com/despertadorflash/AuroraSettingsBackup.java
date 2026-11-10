package com.despertadorflash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small, authenticated-by-checksum (not encrypted) portable configuration.
 * Explicitly excludes profile, transcriptions, microphone state, live timers,
 * snooze state, credentials, photo assets and all sound histories.
 *
 * A checksum detects accidental edits/truncation; it does NOT prove authorship.
 */
public final class AuroraSettingsBackup {
 private static final String HEADER="AURORA_CONFIGURACION_V1";
 private static final String CHECKSUM="SHA256=";
 public static final int MAX_IMPORT_BYTES=8192;
 private static final Map<String,Object> DEFAULTS=makeDefaults();

 private AuroraSettingsBackup(){}

 private static Map<String,Object> makeDefaults(){
  LinkedHashMap<String,Object> result=new LinkedHashMap<>();
  for(int i=0;i<5;i++){
   result.put("created"+i,false);
   result.put("on"+i,false);
   result.put("h"+i,7);
   result.put("m"+i,0);
   result.put("days"+i,127);
  }
  for(String group:new String[]{"doors","alarms","people","pets","impacts",
      "phones","traffic","voices","other"}){
   result.put("monitor_alert_"+group,
    !group.equals("voices")&&!group.equals("other"));
  }
  result.put("monitor_sensitivity",1);
  result.put("sense_feedback_mode",false);
  result.put("torch_alert",true);
  result.put("visual_flash",false);
  result.put("aurora_motion",true);
  result.put("aurora_smooth_mode",true);
  return Collections.unmodifiableMap(result);
 }

 public static Map<String,Object> normalize(Map<String,?> saved){
  if(saved==null)throw new IllegalArgumentException("Configuracion inexistente");
  Map<String,Object> result=new LinkedHashMap<>();
  for(String key:DEFAULTS.keySet()){
   Object value=saved.containsKey(key)?saved.get(key):DEFAULTS.get(key);
   validate(key,value);
   result.put(key,value);
  }
  return result;
 }

 private static void validate(String key,Object value){
  Object type=DEFAULTS.get(key);
  if(type==null)throw new IllegalArgumentException("Parametro desconocido");
  if(type instanceof Boolean){
   if(!(value instanceof Boolean))throw new IllegalArgumentException("Valor booleano invalido: "+key);
   return;
  }
  if(!(value instanceof Integer))throw new IllegalArgumentException("Numero invalido: "+key);
  int n=(Integer)value;
  if(key.startsWith("h")){if(n<0||n>23)throw new IllegalArgumentException("Hora fuera de rango");}
  else if(key.startsWith("m")&&!key.equals("monitor_sensitivity")){
   if(n<0||n>59)throw new IllegalArgumentException("Minutos fuera de rango");
  }else if(key.startsWith("days")){if(n<0||n>127)throw new IllegalArgumentException("Dias fuera de rango");}
  else if(key.equals("monitor_sensitivity")){if(n<0||n>2)throw new IllegalArgumentException("Sensibilidad fuera de rango");}
 }

 public static String serialize(Map<String,?> current){
  Map<String,Object> prefs=normalize(current);
  StringBuilder body=new StringBuilder(HEADER).append('\n');
  List<String> ordered=new ArrayList<>(prefs.keySet());
  Collections.sort(ordered);
  for(String key:ordered)body.append(key).append('=').append(prefs.get(key)).append('\n');
  return body.append(CHECKSUM).append(hexDigest(body.toString())).append('\n').toString();
 }

 public static Map<String,Object> parse(String file){
  if(file==null||file.getBytes(StandardCharsets.UTF_8).length>MAX_IMPORT_BYTES)
   throw new IllegalArgumentException("Copia vacia o demasiado grande");
  String[] lines=file.split("\\n",-1);
  if(lines.length<3||!lines[0].equals(HEADER)||!lines[lines.length-1].isEmpty()||
      !lines[lines.length-2].startsWith(CHECKSUM))
   throw new IllegalArgumentException("Formato o version de copia no reconocido");
  LinkedHashMap<String,Object> raw=new LinkedHashMap<>();
  for(int n=1;n<lines.length-2;n++){
   String row=lines[n];
   int sep=row.indexOf('=');
   if(sep<1)throw new IllegalArgumentException("Linea invalida");
   String key=row.substring(0,sep);
   String str=row.substring(sep+1);
   if(!DEFAULTS.containsKey(key)||raw.containsKey(key))
    throw new IllegalArgumentException("Parametro desconocido o duplicado: "+key);
   Object expected=DEFAULTS.get(key);
   Object value;
   if(expected instanceof Boolean){
    if(!str.equals("true")&&!str.equals("false"))
     throw new IllegalArgumentException("Booleano invalido: "+key);
    value=Boolean.valueOf(str);
   }else{
    if(!str.matches("[0-9]{1,3}"))
     throw new IllegalArgumentException("Numero invalido: "+key);
    value=Integer.valueOf(str);
   }
   validate(key,value);
   raw.put(key,value);
  }
  if(raw.size()!=DEFAULTS.size())throw new IllegalArgumentException("Copia incompleta");
  String canonical=serialize(raw);
  if(!MessageDigest.isEqual(canonical.getBytes(StandardCharsets.UTF_8),
    file.getBytes(StandardCharsets.UTF_8)))
   throw new IllegalArgumentException("Checksum incorrecto o archivo modificado");
  return normalize(raw);
 }
 private static String hexDigest(String text){
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
   StringBuilder result=new StringBuilder(bytes.length*2);
   for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&0xff));
   return result.toString();
  }catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 no disponible",e);}
 }
 public static int configuredAlarmCount(Map<String,?> settings){
  int total=0;
  for(int i=0;i<5;i++)if(Boolean.TRUE.equals(settings.get("created"+i)))total++;
  return total;
 }
}
