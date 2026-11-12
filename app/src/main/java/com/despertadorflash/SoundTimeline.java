package com.despertadorflash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java bounded list of sound events. No files and no audio samples.
 * The most recent event is always first; a 31st drops the oldest entry.
 */
public final class SoundTimeline {
 public static final int LIMIT=30;
 private SoundTimeline(){}

 public static final class Entry{
  public final String label;
  public final int score;
  public final long timestamp;
  public Entry(String label,int score,long timestamp){
   if(label==null||label.trim().isEmpty())throw new IllegalArgumentException("Empty sound label");
   if(timestamp<0)throw new IllegalArgumentException("Invalid event timestamp");
   this.label=label.trim();
   this.score=Math.max(0,Math.min(100,score));
   this.timestamp=timestamp;
  }
 }

 public static List<Entry> append(List<Entry> previous,String label,int score,long timestamp){
  if(previous==null)throw new IllegalArgumentException("Missing previous sounds");
  Entry latest=new Entry(label,score,timestamp);
  List<Entry> result=new ArrayList<>(Math.min(LIMIT,previous.size()+1));
  result.add(latest);
  for(Entry item:previous){
   if(result.size()>=LIMIT)break;
   if(item!=null)result.add(item);
  }
  return Collections.unmodifiableList(result);
 }
 public static List<Entry> normalized(List<Entry> previous){
  if(previous==null||previous.isEmpty())return Collections.emptyList();
  List<Entry> result=new ArrayList<>(Math.min(previous.size(),LIMIT));
  for(Entry item:previous){
   if(result.size()>=LIMIT)break;
   if(item!=null)result.add(item);
  }
  return Collections.unmodifiableList(result);
 }
}
