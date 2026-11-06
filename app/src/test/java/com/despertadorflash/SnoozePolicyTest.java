package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SnoozePolicyTest {
 @Test public void futureSnoozeIsKept(){
  assertEquals(120_000L,SnoozePolicy.recoverAt(120_000L,50_000L,true));
 }
 @Test public void recentMissedSnoozeRingsShortlyAfterRestart(){
  assertEquals(104_000L,SnoozePolicy.recoverAt(99_000L,101_000L,true));
 }
 @Test public void obsoleteOrDisabledSnoozesDontRing(){
  assertEquals(-1L,SnoozePolicy.recoverAt(99_000L,101_000L,false));
  assertEquals(-1L,SnoozePolicy.recoverAt(0,101_000L,true));
  assertEquals(-1L,SnoozePolicy.recoverAt(100_000L,800_001L,true));
 }
}
