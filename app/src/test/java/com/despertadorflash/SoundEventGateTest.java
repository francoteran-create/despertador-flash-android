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
 @Test public void clockRollbackAndResetDontLockOutAlerts(){
  SoundEventGate gate=new SoundEventGate();
  assertTrue(gate.accept("A",95,1,100_000));
  assertTrue(gate.accept("A",95,1,2_000));
  gate.reset();
  assertTrue(gate.accept("A",95,1,2_500));
 }
}
