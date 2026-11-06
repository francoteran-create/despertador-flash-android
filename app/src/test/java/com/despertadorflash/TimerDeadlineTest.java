package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimerDeadlineTest {
 @Test public void remainingUsesElapsedClockWhenUserSetsClockForward(){
  long endWall=100000L+300000L,endElapsed=1000L+300000L;
  assertEquals(240000L,TimerDeadline.remaining(endWall,endElapsed,100000L+1800000L,61000L));
 }
 @Test public void remainingUsesElapsedClockWhenUserSetsClockBackward(){
  assertEquals(240000L,TimerDeadline.remaining(400000L,301000L,1000L,61000L));
 }
 @Test public void rebootFallsBackToSavedWallDeadline(){
  assertEquals(120000L,TimerDeadline.remaining(300000L,0L,180000L,2000L));
 }
 @Test public void expiredAndCanceledNeverBecomeActive(){
  assertEquals(0L,TimerDeadline.remaining(0L,12000L,10000L,10L));
  assertEquals(0L,TimerDeadline.remaining(10000L,9000L,15000L,10000L));
  assertEquals(0L,TimerDeadline.remaining(10000L,0L,15000L,10000L));
 }
 @Test public void timerRearmsAtCurrentWallTimePlusRemaining(){
  assertEquals(75000L,TimerDeadline.newWallDeadline(25000L,50000L));
 }
}
