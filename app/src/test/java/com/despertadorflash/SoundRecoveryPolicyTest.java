package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundRecoveryPolicyTest {
 @Test public void boundedRetriesNeverLoopForever(){
  assertEquals(700L,SoundRecoveryPolicy.delayAfterFailure(1));
  assertEquals(1600L,SoundRecoveryPolicy.delayAfterFailure(2));
  assertEquals(3500L,SoundRecoveryPolicy.delayAfterFailure(3));
  assertEquals(-1L,SoundRecoveryPolicy.delayAfterFailure(4));
  assertEquals(-1L,SoundRecoveryPolicy.delayAfterFailure(0));
 }
 @Test public void silentClassifierStallMustBeDetected(){
  assertFalse(SoundRecoveryPolicy.isStalled(100L,20099L));
  assertTrue(SoundRecoveryPolicy.isStalled(100L,20101L));
  assertFalse(SoundRecoveryPolicy.isStalled(0L,45000L));
  assertFalse(SoundRecoveryPolicy.isStalled(120000L,100L));
 }
}
