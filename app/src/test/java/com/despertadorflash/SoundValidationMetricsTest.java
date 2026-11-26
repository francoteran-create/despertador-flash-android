package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SoundValidationMetricsTest {
 @Test public void threeVerdictsAndHonestDenominators(){
  SoundValidationMetrics m=new SoundValidationMetrics();
  assertEquals(-1,m.confirmedRatio());
  assertEquals(-1,m.observedCoverage());
  m.add("perro",true,false);
  m.add("perro",false,false);
  m.add("perro",false,true);
  assertEquals(50,m.confirmedRatio());
  assertEquals(50,m.observedCoverage());
  assertEquals(1,m.totalYes());
  assertEquals(1,m.totalNo());
  assertEquals(1,m.totalMissed());
  assertTrue(m.report().contains("NO ES PRECISIÓN CIENTÍFICA"));
 }
 @Test public void soundTypesDoNotMixAndUnrecognizedTypesFallBack(){
  SoundValidationMetrics m=new SoundValidationMetrics();
  m.add(SoundValidationMetrics.kindForSound("POSIBLE TIMBRE DE PUERTA"),true,false);
  m.add(SoundValidationMetrics.kindForSound("POSIBLE PERRO LADRANDO"),false,false);
  m.add(SoundValidationMetrics.kindForSound("POSIBLE BOCINA"),false,true);
  assertEquals(1,m.get("timbre").yes);
  assertEquals(1,m.get("perro").no);
  assertEquals(1,m.get("bocina").missed);
  assertEquals(0,m.get("puerta").trials());
  m.add("inexistente",true,false);
  assertEquals(1,m.get("otros").yes);
 }
 @Test public void manuallyOmittedEventsAreNotFalseAlarms(){
  SoundValidationMetrics m=new SoundValidationMetrics();
  m.add("sirena",false,true);
  assertEquals(-1,m.confirmedRatio());
  assertEquals(0,m.observedCoverage());
  assertEquals(0,m.totalNo());
 }
 @Test public void countersStayBoundedEvenWithCorruptInput(){
  SoundValidationMetrics m=new SoundValidationMetrics();
  m.set("voz",-100,Long.MAX_VALUE,Long.MAX_VALUE);
  assertEquals(0,m.get("voz").yes);
  assertEquals(1_000_000L,m.get("voz").no);
  assertEquals(1_000_000L,m.get("voz").missed);
  assertEquals(2_000_000L,m.get("voz").trials());
  assertEquals("Otros",SoundValidationMetrics.displayName("invalid"));
 }
 @Test public void persistedSnapshotCountsCanBeRestored(){
  SoundValidationMetrics original=new SoundValidationMetrics();
  original.add("perro",true,false);
  original.add("perro",false,true);
  SoundValidationMetrics restored=new SoundValidationMetrics();
  for(java.util.Map.Entry<String,SoundValidationMetrics.Count> e:original.snapshot().entrySet()){
   SoundValidationMetrics.Count c=e.getValue();
   restored.set(e.getKey(),c.yes,c.no,c.missed);
  }
  assertEquals(original.report(),restored.report());
 }
}
