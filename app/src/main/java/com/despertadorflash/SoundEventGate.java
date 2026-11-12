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
 private static final long MIN_EVIDENCE_GAP_MS=300L;
 private static final int MAX_TRACKED=128;
 private final Map<String,Candidate> candidates=new HashMap<>();
 private final Map<String,Long> familyAlertAt=new HashMap<>();

 private static final class Candidate{
  long firstAt=-1L,lastSeen=-1L,lastAlertAt=-1L;
  int hits=0;
  int sensitivity=-1;
 }

 public synchronized boolean accept(String label,int confidence,int sensitivity,long elapsedMs){
  if(label==null||label.isEmpty()||elapsedMs<0||confidence<0||confidence>100)return false;
  int threshold=sensitivity<=0?22:sensitivity>=2?45:30;
  if(confidence<threshold)return false;
  Candidate candidate=candidates.get(label);
  if(candidate==null){
   if(candidates.size()>=MAX_TRACKED)trim(elapsedMs);
   candidate=new Candidate();
   candidates.put(label,candidate);
  }
  if(candidate.sensitivity!=sensitivity){
   // Do not carry confirmations from a more permissive listening mode.
   candidate.sensitivity=sensitivity;
   candidate.hits=0;
   candidate.firstAt=-1L;
  }
  if(candidate.firstAt<0||elapsedMs<candidate.firstAt||elapsedMs-candidate.firstAt>CONFIRM_WINDOW_MS){
   candidate.firstAt=elapsedMs;
   candidate.lastSeen=elapsedMs;
   candidate.hits=1;
  }else if(elapsedMs-candidate.lastSeen>=MIN_EVIDENCE_GAP_MS){
   // Rapid repeats of one frame are not independent confirmations.
   candidate.lastSeen=elapsedMs;
   candidate.hits++;
  }
  // Ordinary sounds need either independent evidence or a particularly
  // strong single score. Urgent alarms retain a faster path, but this
  // classifier does not replace a certified safety detector.
  int immediate=(label.contains("SIRENA")||label.contains("INCENDIO"))?75:88;
  // PRECISE requires independent confirmation for ordinary sounds even
  // when one model frame has a high score. Urgent sirens and fire alarms
  // keep a fast path, but are not certified emergency detectors.
  boolean urgent=label.contains("SIRENA")||label.contains("INCENDIO");
  boolean ambiguousGeneric=label.equals("POSIBLE PERRO")||
   label.equals("POSIBLE PUERTA O CAJÓN")||
   label.equals("POSIBLE VEHÍCULO")||
   label.equals("POSIBLE VOZ HUMANA");
  // Generic classes can be mistakenly high-scoring on one short frame.
  if(candidate.hits<2&&((sensitivity>=2&&!urgent)||ambiguousGeneric||confidence<immediate))
   return false;
  String family=SoundEventFamily.key(label);
  Long familyPrevious=familyAlertAt.get(family);
  if((candidate.lastAlertAt>=0&&elapsedMs>=candidate.lastAlertAt&&
      elapsedMs-candidate.lastAlertAt<COOLDOWN_MS)||
     (familyPrevious!=null&&elapsedMs>=familyPrevious&&
      elapsedMs-familyPrevious<COOLDOWN_MS)){
   // Observations suppressed inside the cooldown are not evidence for
   // a new alert once that cooldown expires.
   candidate.hits=0;
   candidate.firstAt=-1L;
   return false;
  }
  candidate.lastAlertAt=elapsedMs;
  familyAlertAt.put(family,elapsedMs);
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
  if(candidates.size()>=MAX_TRACKED)candidates.clear(); // Defensive bound.
  familyAlertAt.entrySet().removeIf(e->
   elapsedMs<e.getValue()||elapsedMs-e.getValue()>COOLDOWN_MS*2);
  if(familyAlertAt.size()>MAX_TRACKED)familyAlertAt.clear();
 }
 public synchronized void reset(){candidates.clear();familyAlertAt.clear();}
}
