package com.despertadorflash;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Independent detection confirmation per label. Multiple simultaneous sound
 * classes must not repeatedly reset one another's confirmation window.
 * Uses monotonic elapsed time (never user-adjustable wall clock).
 */
public final class SoundEventGate {
 private static final long CONFIRM_WINDOW_MS=4500L;
 private static final long COOLDOWN_MS=15000L;
 private static final int MAX_TRACKED=128;
 private final Map<String,Candidate> candidates=new HashMap<>();

 private static final class Candidate{
  long firstAt=-1L,lastSeen=-1L,lastAlertAt=-1L;
  int hits=0;
 }

 public synchronized boolean accept(String label,int confidence,int sensitivity,long elapsedMs){
  if(label==null||label.isEmpty()||elapsedMs<0)return false;
  int threshold=sensitivity<=0?22:sensitivity>=2?45:30;
  if(confidence<threshold)return false;
  Candidate candidate=candidates.get(label);
  if(candidate==null){
   candidate=new Candidate();
   candidates.put(label,candidate);
  }
  if(candidate.firstAt<0||elapsedMs<candidate.firstAt||elapsedMs-candidate.firstAt>CONFIRM_WINDOW_MS){
   candidate.firstAt=elapsedMs;
   candidate.hits=1;
  }else candidate.hits++;
  candidate.lastSeen=elapsedMs;
  if(candidate.hits<2&&confidence<75)return false;
  if(candidate.lastAlertAt>=0&&elapsedMs>=candidate.lastAlertAt&&
     elapsedMs-candidate.lastAlertAt<COOLDOWN_MS){
   // Observations suppressed inside the cooldown are not evidence for
   // a new alert once that cooldown expires.
   candidate.hits=0;
   candidate.firstAt=-1L;
   return false;
  }
  candidate.lastAlertAt=elapsedMs;
  // Each emitted event consumes its confirmation. A single later observation
  // must never inherit the two hits of an earlier sound from 15 seconds ago.
  candidate.hits=0;
  candidate.firstAt=-1L;
  if(candidates.size()>MAX_TRACKED)trim(elapsedMs);
  return true;
 }

 private void trim(long elapsedMs){
  Iterator<Map.Entry<String,Candidate>> it=candidates.entrySet().iterator();
  while(it.hasNext()){
   Candidate v=it.next().getValue();
   if(elapsedMs-v.lastSeen>COOLDOWN_MS*2)it.remove();
  }
  if(candidates.size()>MAX_TRACKED)candidates.clear(); // Defensive bound.
 }
 public synchronized void reset(){candidates.clear();}
}
