package com.despertadorflash;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Map;

/**
 * Private, bounded, opt-in feedback aggregates. Never saves raw audio,
 * transcripts, event labels, identities, dates or locations.
 * This is NOT an automatic accuracy measurement.
 */
public final class SoundValidationStore {
 private static final String PREF="aurora_sound_validation_v1";
 private static final String COUNTS="counts";
 private SoundValidationStore(){}
 public static synchronized SoundValidationMetrics read(Context context){
  SoundValidationMetrics m=new SoundValidationMetrics();
  try{
   String value=context.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(COUNTS,"{}");
   JSONObject all=new JSONObject(value);
   for(String key:SoundValidationMetrics.KEYS){
    JSONObject row=all.optJSONObject(key);
    if(row==null)continue;
    m.set(key,row.optLong("yes",0),row.optLong("no",0),row.optLong("missed",0));
   }
  }catch(Exception error){
   android.util.Log.w("AURORA","Registro de validación local inválido",error);
  }
  return m;
 }
 public static synchronized boolean record(Context context,String kind,boolean correct,boolean missed){
  if(!context.getSharedPreferences("aurora",0).getBoolean("sense_feedback_mode",false))
   return false;
  SoundValidationMetrics m=read(context);
  m.add(kind,correct,missed);
  JSONObject all=new JSONObject();
  try{
   for(Map.Entry<String,SoundValidationMetrics.Count> e:m.snapshot().entrySet()){
    SoundValidationMetrics.Count c=e.getValue();
    JSONObject row=new JSONObject();
    row.put("yes",c.yes);row.put("no",c.no);row.put("missed",c.missed);
    all.put(e.getKey(),row);
   }
   // Synchronous commit of very small JSON prevents silently losing the
   // last explicit user vote when Android closes this Activity.
   SharedPreferences storage=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
   return storage.edit().putString(COUNTS,all.toString()).commit();
  }catch(Exception error){
   android.util.Log.w("AURORA","No se pudo guardar la evaluación",error);
   return false;
  }
 }
 public static synchronized void clear(Context context){
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(COUNTS).commit();
 }
}
