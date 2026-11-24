package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.despertadorflash.AuroraEventVisualPolicy.Event;

public class AuroraEventVisualPolicyTest {
 @Test public void knownEventsHaveDistinctDrawings(){
  assertEquals(Event.DOOR,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE TIMBRE DE PUERTA"));
  assertEquals(Event.DOOR,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLES GOLPES O IMPACTOS"));
  assertEquals(Event.DOG,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE PERRO LADRANDO"));
  assertEquals(Event.HORN,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE BOCINA"));
  assertEquals(Event.MUSIC,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE MÚSICA"));
  assertEquals(Event.VOICES,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE CONVERSACIÓN"));
  assertEquals(Event.SIREN,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE SIRENA"));
  assertEquals(Event.BABY,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE LLANTO"));
  assertEquals(Event.PHONE,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE TELÉFONO"));
 }
 @Test public void unknownAndNullNeverInventASound(){
  assertEquals(Event.NONE,AuroraEventVisualPolicy.fromConfirmedLabel(null));
  assertEquals(Event.OTHER,AuroraEventVisualPolicy.fromConfirmedLabel("POSIBLE AGUA"));
  assertEquals("Dibujo de perro ladrando",AuroraEventVisualPolicy.accessibleName(Event.DOG));
 }
}