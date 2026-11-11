package com.despertadorflash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Short-lived display of classifier observations, independent from the
 * cooldown/confirmation gate used for notifications and the event journal.
 * No classification is retained beyond the recent window.
 */
public final class SoundLiveWindow {
 public static final long LIVE_MS=3500L;
 private static final int MAX_TRACKED=48;
 private static final class Observation {
  float score;
  long seen;
  Observation(float score,long seen){this.score=score;this.seen=seen;}
 }
 public static final class Item {
  public final String label;
  public final int score;
  public final long ageMs;
  Item(String label,int score,long ageMs){
   this.label=label;this.score=score;this.ageMs=ageMs;
  }
 }
 private final Map<String,Observation> recent=new HashMap<>();

 public void observe(Map<String,Float> evidence,long now){
  if(now<0)return;
  prune(now);
  if(evidence==null)return;
  for(Map.Entry<String,Float> e:evidence.entrySet()){
   if(e.getKey()==null||e.getKey().isEmpty()||e.getValue()==null)continue;
   float s=e.getValue();
   // Low scores are not reliable enough even to display as a hypothesis.
   if(Float.isNaN(s)||Float.isInfinite(s)||s<.20f||s>1f)continue;
   Observation previous=recent.get(e.getKey());
   if(previous==null||now<previous.seen)recent.put(e.getKey(),new Observation(s,now));
   else {previous.score=s;previous.seen=now;}
  }
  if(recent.size()>MAX_TRACKED)recent.clear();
 }

 public List<Item> snapshot(long now,int limit){
  prune(now);
  if(limit<=0)return Collections.emptyList();
  List<Item> output=new ArrayList<>();
  for(Map.Entry<String,Observation> e:recent.entrySet()){
   Observation o=e.getValue();
   output.add(new Item(e.getKey(),Math.round(o.score*100f),Math.max(0L,now-o.seen)));
  }
  // Show what was heard most recently. Do not pin the historically loudest event.
  // Ongoing music, wind and water must not visually obscure a brief
  // doorbell or impact just because the background is louder.
  Collections.sort(output,Comparator.comparingInt((Item x)->isAmbient(x.label)?1:0)
   .thenComparingLong(x->x.ageMs)
   .thenComparing((Item x)->-x.score).thenComparing(x->x.label));
  if(output.size()>limit)return new ArrayList<>(output.subList(0,limit));
  return output;
 }
 private static boolean isAmbient(String label){
  return label.equals("POSIBLE MÚSICA")||label.equals("POSIBLE VIENTO")||
   label.equals("POSIBLE AGUA");
 }
 private void prune(long now){
  recent.entrySet().removeIf(e->now<e.getValue().seen||now-e.getValue().seen>LIVE_MS);
 }
 public void clear(){recent.clear();}
}
