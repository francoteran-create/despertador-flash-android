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
  // A generic "Knock" model category does not establish that a door
  // was involved. Require separate door evidence before saying so.
  Float knocking=rawScore(rawScores,"Knock");
  Float door=rawScore(rawScores,"Door");
  Float doorSlam=rawScore(rawScores,"Door slam");
  if(knocking!=null&&door!=null&&knocking>=.35f&&door>=.30f
     &&(doorSlam==null||doorSlam<door*.5f)){
   result.put("POSIBLES GOLPES EN UNA PUERTA",Math.min(knocking,door));
   Float general=result.get("POSIBLES GOLPES O IMPACTOS");
   if(general!=null&&general<=knocking*1.05f)
    result.remove("POSIBLES GOLPES O IMPACTOS");
  }

  // More specific evidence supersedes a broad hypothesis about the same
  // kind of event within this frame, without hiding concurrent other sounds.
  if(dominates(result,"POSIBLE PUERTA O CAJÓN",
    "POSIBLE TIMBRE DE PUERTA","POSIBLES GOLPES EN UNA PUERTA","POSIBLE PORTAZO"))
   result.remove("POSIBLE PUERTA O CAJÓN");
  if(dominates(result,"POSIBLE PERRO","POSIBLE PERRO LADRANDO"))
   result.remove("POSIBLE PERRO");
  if(dominates(result,"POSIBLE VOZ HUMANA","POSIBLE CONVERSACIÓN"))
   result.remove("POSIBLE VOZ HUMANA");
  if(dominates(result,"POSIBLE ALARMA O PITIDO","POSIBLE ALARMA DE INCENDIO"))
   result.remove("POSIBLE ALARMA O PITIDO");
  return result;
 }

 private static Float rawScore(Map<String,Float> raw,String requested){
  for(Map.Entry<String,Float> item:raw.entrySet()){
   Float value=item.getValue();
   if(item.getKey()!=null&&item.getKey().trim().equalsIgnoreCase(requested)
      &&value!=null&&!Float.isNaN(value)&&!Float.isInfinite(value)
      &&value>=0f&&value<=1f)return value;
  }
  return null;
 }

 private static boolean dominates(Map<String,Float> result,String generic,String... specific){
  Float general=result.get(generic);
  if(general==null)return false;
  for(String label:specific){
   Float precise=result.get(label);
   // A very weak specific guess must never mask strong generic evidence.
   if(precise!=null&&precise>=general*.8f)return true;
  }
  return false;
 }
}
