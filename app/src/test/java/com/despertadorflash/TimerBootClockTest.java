package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public final class TimerBootClockTest {
 @Test public void currentBootKeepsElapsedClock(){
  assertEquals(60000L,TimerBootClock.elapsedDeadlineForBoot(60000L,42,42));
 }
 @Test public void rebootForcesWallClockRecovery(){
  assertEquals(0L,TimerBootClock.elapsedDeadlineForBoot(60000L,41,42));
 }
 @Test public void unavailableBootCountPreservesLegacyTimers(){
  assertEquals(60000L,TimerBootClock.elapsedDeadlineForBoot(60000L,-1,-1));
 }
}
