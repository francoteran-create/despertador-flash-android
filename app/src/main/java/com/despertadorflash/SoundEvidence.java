package com.despertadorflash;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consolidates YAMNet's independent AudioSet scores into conservative
 * Spanish event hypotheses. This class does not convert scores into
 * calibrated probabilities or infer the cause of a sound.
 *
 * Specific and generic labels from the same classifier frame must not
 * produce redundant notifications. Unrelated concurrent sounds survive.
 */
public final class SoundEvidence {
 private SoundEvidence(){}

 public static Map<String,Float> fromRawScores(Map<String,Float> rawScores){
  Map<String,Float> result=new LinkedHashMap<>();
  if(rawScores==null)return result;
  for(Map.Entry<String,Float> entry:rawScores.entrySet()){
   Float score=entry.getValue();
   if(score==null||Float.isNaN(score)||Float.isInfinite(score)||score<0f||score>1f)continue;
   String label=SoundLabelsEs.name(entry.getKey());
   if(label==null)continue;
   Float previous=result.get(label);
   if(previous==null||score>previous)result.put(label,score);
  }
  // More specific evidence supersedes a broad hypothesis about the same
  // kind of event within this frame, without hiding concurrent other sounds.
  if(result.containsKey("POSIBLE TIMBRE DE PUERTA")
    ||result.containsKey("POSIBLES GOLPES EN UNA PUERTA")
    ||result.containsKey("POSIBLE PORTAZO")){
   result.remove("POSIBLE PUERTA O CAJÓN");
  }
  if(result.containsKey("POSIBLE PERRO LADRANDO"))result.remove("POSIBLE PERRO");
  if(result.containsKey("POSIBLE CONVERSACIÓN"))result.remove("POSIBLE VOZ HUMANA");
  if(result.containsKey("POSIBLE ALARMA DE INCENDIO"))result.remove("POSIBLE ALARMA O PITIDO");
  return result;
 }
}
