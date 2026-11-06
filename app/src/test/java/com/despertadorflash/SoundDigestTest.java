package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public class SoundDigestTest {
 @Test public void countsOnlyKnownDetections(){
  String sample="1. POSIBLE TIMBRE DE PUERTA · confianza estimada 91%\n"+
                "2. POSIBLE SIRENA · confianza estimada 80%\n"+
                "3. POSIBLE TIMBRE DE PUERTA · confianza estimada 76%\n";
  String summary=SoundDigest.summarize(sample);
  assertTrue(summary.contains("Observaciones analizadas: 3"));
  assertTrue(summary.contains("POSIBLE TIMBRE DE PUERTA: 2"));
  assertFalse(summary.contains("INCENDIO"));
 }
 @Test public void ignoresEmptyConversation(){
  assertTrue(SoundDigest.summarize("1. Hola, buen dia").contains("no hay detecciones"));
 }
}