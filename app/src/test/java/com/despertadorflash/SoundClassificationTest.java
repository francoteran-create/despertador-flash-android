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
 @Test public void unknownClassesDoNotPretendRecognition(){
  assertNull(SoundLabelsEs.name(null));
  assertNull(SoundLabelsEs.name("Unknown audio class"));
 }
 @Test public void soundCategoriesAreStable(){
  assertEquals("doors",SoundAlertPreferences.category("POSIBLE TIMBRE DE PUERTA"));
  assertEquals("alarms",SoundAlertPreferences.category("POSIBLE SIRENA"));
  assertEquals("people",SoundAlertPreferences.category("POSIBLES GRITOS"));
  assertEquals("pets",SoundAlertPreferences.category("POSIBLE PERRO LADRANDO"));
  assertEquals("other",SoundAlertPreferences.category("POSIBLE MÚSICA"));
 }
}
