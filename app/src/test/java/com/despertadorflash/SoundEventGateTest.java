package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundEventGateTest {
 @Test public void requiresConfirmationBelowHighConfidence(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("POSIBLE TIMBRE",45,1,1000));
  assertTrue(gate.accept("POSIBLE TIMBRE",48,1,1900));
 }
 @Test public void confidenceAbove75IsImmediate(){
  assertTrue(new SoundEventGate().accept("POSIBLE SIRENA",77,1,100));
 }
 @Test public void suppressesSameLabelButNotAnother(){
  SoundEventGate gate=new SoundEventGate();
  assertTrue(gate.accept("A",90,1,1000));
  assertFalse(gate.accept("A",98,1,1200));
  assertTrue(gate.accept("B",91,1,1400));
  assertTrue(gate.accept("A",90,1,16300));
 }
 @Test public void sensitivityAndWindowAreRespected(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("A",24,2,1000));
  assertFalse(gate.accept("A",24,0,1100));
  assertTrue(gate.accept("A",24,0,1500));
  gate.reset();
  assertFalse(gate.accept("B",40,1,3000));
  assertFalse(gate.accept("B",40,1,9000));
  assertTrue(gate.accept("B",40,1,9500));
 }
 @Test public void alternatingSimultaneousSoundsConfirmIndependently(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("TIMBRE",50,1,1000));
  assertFalse(gate.accept("SIRENA",55,1,1200));
  assertTrue(gate.accept("TIMBRE",54,1,1500));
  assertTrue(gate.accept("SIRENA",51,1,1700));
  assertFalse(gate.accept("TIMBRE",85,1,2100));
  assertFalse(gate.accept("TIMBRE",80,1,18000));
  assertTrue(gate.accept("TIMBRE",82,1,18600));
 }
 @Test public void expiredCandidateNeedsTwoFreshObservations(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("PUERTA",35,1,1000));
  assertFalse(gate.accept("PUERTA",35,1,7000));
  assertTrue(gate.accept("PUERTA",35,1,7600));
 }
 @Test public void cooldownExpiryDoesNotReuseOldConfirmation(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("TIMBRE",49,1,1000));
  assertTrue(gate.accept("TIMBRE",49,1,1800));
  assertFalse(gate.accept("TIMBRE",49,1,17100));
  assertTrue(gate.accept("TIMBRE",49,1,17600));
 }
 @Test public void observationsInsideCooldownDoNotPreConfirmNextAlert(){
  SoundEventGate gate=new SoundEventGate();
  assertTrue(gate.accept("A",88,1,1000));
  assertFalse(gate.accept("A",50,1,14900));
  assertFalse(gate.accept("A",50,1,15200));
  assertFalse(gate.accept("A",50,1,16050));
  assertTrue(gate.accept("A",50,1,16500));
 }
 @Test public void repeatedClassifierFrameIsNotIndependentConfirmation(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("POSIBLE TIMBRE DE PUERTA",58,1,1000));
  assertFalse(gate.accept("POSIBLE TIMBRE DE PUERTA",60,1,1050));
  assertFalse(gate.accept("POSIBLE TIMBRE DE PUERTA",72,1,1220));
  assertTrue(gate.accept("POSIBLE TIMBRE DE PUERTA",61,1,1400));
 }
 @Test public void ordinarySoundsNeedStrongerEvidenceThanOneAmbiguousFrame(){
  SoundEventGate gate=new SoundEventGate();
  assertFalse(gate.accept("POSIBLE PERRO LADRANDO",80,1,1000));
  assertTrue(gate.accept("POSIBLE PERRO LADRANDO",65,1,1700));
  assertTrue(new SoundEventGate().accept("POSIBLE TIMBRE DE PUERTA",91,1,500));
 }
 @Test public void clockRollbackAndResetDontLockOutAlerts(){
  SoundEventGate gate=new SoundEventGate();
  assertTrue(gate.accept("A",95,1,100_000));
  assertTrue(gate.accept("A",95,1,2_000));
  gate.reset();
  assertTrue(gate.accept("A",95,1,2_500));
 }
}
