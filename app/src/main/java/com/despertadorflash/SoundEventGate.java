package com.despertadorflash;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure Java, deterministic confirmation/cooldown logic for sound recognition.
 * Decisions are based on monotonic elapsed time, never the phone wall clock.
 * This is intentionally an approximate classifier: not a safety alarm.
 */
public final class SoundEventGate {
 private static final long CONFIRM_WINDOW_MS=4500L;
 private static final long COOLDOWN_MS=15000L;
 private final Map<String,Long> lastAccepted=new HashMap<>();
 private String candidate="";
 private int hits=0;
 private long candidateAt=-1L;

 public synchronized boolean accept(String label,int confidence,int sensitivity,long elapsedMs){
  if(label==null||label.isEmpty())return false;
  int threshold=sensitivity<=0?22:sensitivity>=2?45:30;
  if(confidence<threshold)return false;
  if(!label.equals(candidate)||candidateAt<0||elapsedMs<candidateAt||
    elapsedMs-candidateAt>CONFIRM_WINDOW_MS){
   candidate=label;hits=1;candidateAt=elapsedMs;
  }else hits++;
  if(hits<2&&confidence<75)return false;
  Long last=lastAccepted.get(label);
  if(last!=null&&elapsedMs>=last&&elapsedMs-last<COOLDOWN_MS)return false;
  lastAccepted.put(label,elapsedMs);
  // No need to keep unbounded history. There are only a few dozen supported labels.
  if(lastAccepted.size()>100){
   long oldest=elapsedMs-COOLDOWN_MS;
   lastAccepted.entrySet().removeIf(entry->entry.getValue()<oldest);
  }
  return true;
 }
 public synchronized void reset(){candidate="";hits=0;candidateAt=-1;lastAccepted.clear();}
}
