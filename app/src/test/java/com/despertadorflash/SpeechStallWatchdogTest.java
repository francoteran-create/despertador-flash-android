package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechStallWatchdogTest {
 @Test public void waitingForSpeechHasBoundedRecovery(){
  SpeechStallWatchdog clock=new SpeechStallWatchdog();
  clock.started(1000L);
  assertFalse(clock.timedOut(45999L));
  assertTrue(clock.timedOut(46000L));
  clock.ready(50000L);
  assertFalse(clock.timedOut(94999L));
  assertTrue(clock.timedOut(95000L));
 }
 @Test public void actualSpeechAndPartialsKeepSessionAlive(){
  SpeechStallWatchdog clock=new SpeechStallWatchdog();
  clock.started(0L);
  clock.speechBegan(1000L);
  assertFalse(clock.timedOut(90999L));
  assertTrue(clock.timedOut(91000L));
  clock.partialResult(90000L);
  assertFalse(clock.timedOut(179999L));
  assertTrue(clock.timedOut(180000L));
 }
 @Test public void waitingForFinalResultUsesShorterTimeout(){
  SpeechStallWatchdog clock=new SpeechStallWatchdog();
  clock.started(0L);
  clock.speechBegan(1000L);
  clock.speechEnded(4000L);
  assertFalse(clock.timedOut(21999L));
  assertTrue(clock.timedOut(22000L));
  assertTrue(clock.timeoutReason().contains("palabras finales"));
 }
 @Test public void stoppingPreventsLateTimeouts(){
  SpeechStallWatchdog clock=new SpeechStallWatchdog();
  clock.started(0L);
  clock.reset();
  assertFalse(clock.timedOut(Long.MAX_VALUE));
  clock.partialResult(100L);
  assertFalse(clock.timedOut(1000000L));
 }
 @Test public void monotonicClockGuardHandlesUnexpectedBackwardValue(){
  SpeechStallWatchdog clock=new SpeechStallWatchdog();
  clock.started(1000L);
  assertFalse(clock.timedOut(900L));
 }
}