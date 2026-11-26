package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SoundEventFamilyTest {
 @Test public void redundantDogHypothesesShareOneFamily(){
  assertEquals(SoundEventFamily.key("POSIBLE PERRO"),SoundEventFamily.key("POSIBLE PERRO LADRANDO"));
 }
 @Test public void movementDoorAndSlamShareOneFamilyButDoorbellDoesNot(){
  assertEquals(SoundEventFamily.key("POSIBLE PORTAZO"),SoundEventFamily.key("POSIBLE PUERTA O CAJÓN"));
  assertNotEquals(SoundEventFamily.key("POSIBLE TIMBRE DE PUERTA"),SoundEventFamily.key("POSIBLE PORTAZO"));
  assertNotEquals(SoundEventFamily.key("POSIBLES GOLPES EN UNA PUERTA"),SoundEventFamily.key("POSIBLE TIMBRE DE PUERTA"));
 }
 @Test public void carHornAndVehicleAreIndependentEvents(){
  assertNotEquals(SoundEventFamily.key("POSIBLE BOCINA"),SoundEventFamily.key("POSIBLE VEHÍCULO"));
 }
 @Test public void voiceAndConversationOverlapButSirensAndFireStayIndependent(){
  assertEquals(SoundEventFamily.key("POSIBLE CONVERSACIÓN"),SoundEventFamily.key("POSIBLE VOZ HUMANA"));
  assertNotEquals(SoundEventFamily.key("POSIBLE SIRENA"),SoundEventFamily.key("POSIBLE ALARMA DE INCENDIO"));
  assertEquals("",SoundEventFamily.key(null));
 }
}
