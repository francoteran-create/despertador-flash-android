package com.despertadorflash;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SoundEvidenceTest {
 private Map<String,Float> raw(Object... pairs){
  Map<String,Float> input=new LinkedHashMap<>();
  for(int i=0;i<pairs.length;i+=2)input.put((String)pairs[i],(Float)pairs[i+1]);
  return input;
 }
 @Test public void specificDoorSoundSuppressesGenericDoorButNotBark(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Door",.48f,"Doorbell",.81f,"Bark",.74f));
  assertFalse(found.containsKey("POSIBLE PUERTA O CAJÓN"));
  assertEquals(.81f,found.get("POSIBLE TIMBRE DE PUERTA"),.0001f);
  assertEquals(.74f,found.get("POSIBLE PERRO LADRANDO"),.0001f);
 }
 @Test public void footstepsOrImpactAreNotInventedAsDoorKnocks(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Door slam",.91f,"Thump, thud",.53f));
  assertTrue(found.containsKey("POSIBLE PORTAZO"));
  assertTrue(found.containsKey("POSIBLES GOLPES O IMPACTOS"));
  assertFalse(found.containsKey("POSIBLES GOLPES EN UNA PUERTA"));
 }
 @Test public void knockWithoutDoorEvidenceStaysGeneric(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Knock",.87f,"Music",.74f));
  assertTrue(found.containsKey("POSIBLES GOLPES O IMPACTOS"));
  assertFalse(found.containsKey("POSIBLES GOLPES EN UNA PUERTA"));
  assertTrue(found.containsKey("POSIBLE MÚSICA"));
 }
 @Test public void knockAndIndependentDoorEvidenceBecomePossibleDoorKnocks(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Knock",.76f,"Door",.57f,"Music",.67f));
  assertEquals(.57f,found.get("POSIBLES GOLPES EN UNA PUERTA"),.0001f);
  assertFalse(found.containsKey("POSIBLES GOLPES O IMPACTOS"));
  assertFalse(found.containsKey("POSIBLE PUERTA O CAJÓN"));
  assertTrue(found.containsKey("POSIBLE MÚSICA"));
 }
 @Test public void portazoDoesNotTurnKnockIntoKnockAtDoor(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Knock",.65f,"Door",.61f,"Door slam",.85f));
  assertTrue(found.containsKey("POSIBLE PORTAZO"));
  assertFalse(found.containsKey("POSIBLES GOLPES EN UNA PUERTA"));
 }
 @Test public void weakDoorEvidenceDoesNotTurnKnockIntoDoorKnock(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Knock",.86f,"Door",.19f));
  assertFalse(found.containsKey("POSIBLES GOLPES EN UNA PUERTA"));
  assertTrue(found.containsKey("POSIBLES GOLPES O IMPACTOS"));
 }
 @Test public void speechAndMusicRemainSeparateWhenBothArePresent(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Speech",.80f,"Music",.70f));
  assertTrue(found.containsKey("POSIBLE VOZ HUMANA"));
  assertTrue(found.containsKey("POSIBLE MÚSICA"));
  assertFalse(found.containsKey("POSIBLE CONVERSACIÓN"));
 }
 @Test public void conversationSupersedesGenericVoiceOnly(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Speech",.88f,"Conversation",.73f,"Music",.68f));
  assertFalse(found.containsKey("POSIBLE VOZ HUMANA"));
  assertTrue(found.containsKey("POSIBLE CONVERSACIÓN"));
  assertTrue(found.containsKey("POSIBLE MÚSICA"));
 }
 @Test public void barkSupersedesGenericDog(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Dog",.75f,"Bark",.83f,"Doorbell",.61f));
  assertFalse(found.containsKey("POSIBLE PERRO"));
  assertTrue(found.containsKey("POSIBLE PERRO LADRANDO"));
  assertTrue(found.containsKey("POSIBLE TIMBRE DE PUERTA"));
 }
 @Test public void weakSpecificGuessDoesNotHideStrongGenericEvidence(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Door",.90f,"Doorbell",.20f));
  assertTrue(found.containsKey("POSIBLE PUERTA O CAJÓN"));
  assertTrue(found.containsKey("POSIBLE TIMBRE DE PUERTA"));
 }
 @Test public void invalidAndUnknownScoresAreIgnored(){
  Map<String,Float> found=SoundEvidence.fromRawScores(raw("Speech",Float.NaN,"Dog",1.2f,"Unknown event",.98f,"Bark",.77f));
  assertEquals(1,found.size());
  assertTrue(found.containsKey("POSIBLE PERRO LADRANDO"));
  assertTrue(SoundEvidence.fromRawScores(null).isEmpty());
 }
}
