package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic checks for the on-device classifier's public Spanish label routing. */
public class SoundClassificationTest {
 @Test public void knownSoundsAreClassifiedInSpanish(){
  assertEquals("POSIBLE TIMBRE DE PUERTA",SoundLabelsEs.name("Doorbell"));
  assertEquals("POSIBLE SIRENA",SoundLabelsEs.name("Siren"));
  assertEquals("POSIBLE LLANTO",SoundLabelsEs.name("Baby cry, infant cry"));
  assertEquals("POSIBLE PERRO LADRANDO",SoundLabelsEs.name("Bark"));
  assertEquals("POSIBLE ALARMA DE INCENDIO",SoundLabelsEs.name("Fire alarm"));
 }
 @Test public void eventNamesDoNotInventCauses(){
  assertEquals("POSIBLES GOLPES EN UNA PUERTA",SoundLabelsEs.name("Knock"));
  assertEquals("POSIBLE PORTAZO",SoundLabelsEs.name("Door slam"));
  assertEquals("POSIBLE PUERTA O CAJÓN",SoundLabelsEs.name("Sliding door"));
  assertEquals("POSIBLE PERRO",SoundLabelsEs.name("Dog"));
  assertEquals("POSIBLE PERRO LADRANDO",SoundLabelsEs.name("Bark"));
  assertEquals("POSIBLE AULLIDO",SoundLabelsEs.name("Howl"));
  assertEquals("POSIBLE CONVERSACIÓN",SoundLabelsEs.name("Conversation"));
  assertEquals("POSIBLE VOZ HUMANA",SoundLabelsEs.name("Speech"));
  assertEquals("POSIBLE VOZ HUMANA",SoundLabelsEs.name("Narration, monologue"));
  assertEquals("POSIBLE MÚSICA",SoundLabelsEs.name("Music"));
 }
 @Test public void unknownClassesDoNotPretendRecognition(){
  assertNull(SoundLabelsEs.name(null));
  assertNull(SoundLabelsEs.name("Unknown audio class"));
 }
 @Test public void alertVibrationIsDifferentPerCategory(){
  long[] door=SoundAlertPreferences.vibrationPattern("doors");
  long[] alarm=SoundAlertPreferences.vibrationPattern("alarms");
  assertEquals(0L,door[0]);
  assertEquals(0L,alarm[0]);
  assertFalse(java.util.Arrays.equals(door,alarm));
  assertNotEquals(SoundAlertPreferences.channel("doors"),SoundAlertPreferences.channel("alarms"));
  assertNotEquals(SoundAlertPreferences.notificationId("doors"),SoundAlertPreferences.notificationId("alarms"));
 }
 @Test public void soundCategoriesAreStable(){
  assertEquals("doors",SoundAlertPreferences.category("POSIBLE TIMBRE DE PUERTA"));
  assertEquals("doors",SoundAlertPreferences.category("POSIBLE PORTAZO"));
  assertEquals("alarms",SoundAlertPreferences.category("POSIBLE SIRENA"));
  assertEquals("people",SoundAlertPreferences.category("POSIBLES GRITOS"));
  assertEquals("pets",SoundAlertPreferences.category("POSIBLE PERRO LADRANDO"));
  assertEquals("other",SoundAlertPreferences.category("POSIBLE MÚSICA"));
  assertEquals("voices",SoundAlertPreferences.category("POSIBLE VOZ HUMANA"));
  assertEquals("voices",SoundAlertPreferences.category("POSIBLE CONVERSACIÓN"));
 }
}
