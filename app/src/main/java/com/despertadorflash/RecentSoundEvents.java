package com.despertadorflash;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Small in-app ring history. No environmental audio, TXT or PDF is created.
 * Each confirmed event is stored as a label and time, newest first.
 */
public final class RecentSoundEvents {
 public static final int LIMIT=SoundTimeline.LIMIT;
 private static final String PREF="aurora_recent_sounds";
 private static final String KEY="events";
 private RecentSoundEvents(){}

 public static synchronized void add(Context context,String label,int score){
  if(label==null||label.trim().isEmpty())return;
  SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  List<SoundTimeline.Entry> existing=fromJson(pref.getString(KEY,"[]"));
  List<SoundTimeline.Entry> recent=SoundTimeline.append(existing,label,score,System.currentTimeMillis());
  JSONArray stored=new JSONArray();
  try{
   for(SoundTimeline.Entry entry:recent){
    JSONObject record=new JSONObject();
    record.put("label",entry.label);
    record.put("score",entry.score);
    record.put("time",entry.timestamp);
    stored.put(record);
   }
   pref.edit().putString(KEY,stored.toString()).apply();
  }catch(Exception e){android.util.Log.e("AURORA","No se pudo guardar lista interna de sonidos",e);}
 }

 public static synchronized List<SoundTimeline.Entry> snapshot(Context context){
  return SoundTimeline.normalized(fromJson(context.getSharedPreferences(
   PREF,Context.MODE_PRIVATE).getString(KEY,"[]")));
 }

 public static synchronized List<String> list(Context context){
  List<String> rows=new ArrayList<>();
  SimpleDateFormat clock=new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
  for(SoundTimeline.Entry entry:snapshot(context)){
   rows.add(clock.format(new Date(entry.timestamp))+" · "+entry.label);
  }
  return rows;
 }

 public static synchronized void clear(Context context){
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply();
 }

 private static List<SoundTimeline.Entry> fromJson(String text){
  List<SoundTimeline.Entry> existing=new ArrayList<>();
  try{
   JSONArray saved=new JSONArray(text);
   for(int i=0;i<saved.length()&&existing.size()<LIMIT;i++){
    JSONObject row=saved.optJSONObject(i);
    if(row==null)continue;
    try{
     existing.add(new SoundTimeline.Entry(row.optString("label",""),
      row.optInt("score",0),row.optLong("time",0L)));
    }catch(IllegalArgumentException ignored){}
   }
  }catch(Exception ignored){}
  return existing;
 }
}
