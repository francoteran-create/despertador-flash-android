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

/** Last 30 confirmed sound events. Stored only in app preferences, never as exported TXT/PDF. */
public final class RecentSoundEvents {
 public static final int LIMIT=30;
 private static final String PREF="aurora_recent_sounds";
 private static final String KEY="events";
 private RecentSoundEvents(){}

 public static synchronized void add(Context context,String label,int score){
  if(label==null||label.trim().isEmpty())return;
  SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  JSONArray previous=readArray(pref.getString(KEY,"[]"));
  JSONArray next=new JSONArray();
  JSONObject event=new JSONObject();
  try{
   event.put("label",label.trim());
   event.put("score",Math.max(0,Math.min(100,score)));
   event.put("time",System.currentTimeMillis());
   next.put(event);
   for(int i=0;i<previous.length()&&next.length()<LIMIT;i++){
    JSONObject old=previous.optJSONObject(i);
    if(old!=null)next.put(old);
   }
   pref.edit().putString(KEY,next.toString()).apply();
  }catch(Exception ignored){}
 }
 public static synchronized List<String> list(Context context){
  JSONArray items=readArray(context.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
  List<String> result=new ArrayList<>();
  SimpleDateFormat format=new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
  for(int i=0;i<items.length()&&result.size()<LIMIT;i++){
   JSONObject item=items.optJSONObject(i);
   if(item==null)continue;
   String label=item.optString("label","");
   if(label.isEmpty())continue;
   result.add(format.format(new Date(item.optLong("time",0)))+" · "+label);
  }
  return result;
 }
 public static synchronized void clear(Context context){
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply();
 }
 private static JSONArray readArray(String raw){
  try{return new JSONArray(raw);}catch(Exception ignored){return new JSONArray();}
 }
}
