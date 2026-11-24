package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.despertadorflash.AuroraEventVisualPolicy.Event;

public final class AuroraWatchAlertTitleTest {
 @Test public void clearWatchLabels(){
  assertEquals("PUERTA / GOLPES",AuroraWatchAlert.title(Event.DOOR));
  assertEquals("PERRO LADRANDO",AuroraWatchAlert.title(Event.DOG));
  assertEquals("BOCINA",AuroraWatchAlert.title(Event.HORN));
  assertEquals("MÚSICA",AuroraWatchAlert.title(Event.MUSIC));
  assertEquals("GENTE HABLANDO",AuroraWatchAlert.title(Event.VOICES));
  assertEquals("SIRENA / ALARMA",AuroraWatchAlert.title(Event.SIREN));
  assertEquals("SIN EVENTO",AuroraWatchAlert.title(Event.NONE));
 }
}
