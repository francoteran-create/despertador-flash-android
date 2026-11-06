package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonitorHealthTest {
 @Test public void showsOffAndErrorRatherThanFalseListening(){
  assertEquals(MonitorHealth.State.OFF,MonitorHealth.evaluate(false,0L,0L,100000L,""));
  assertEquals(MonitorHealth.State.ERROR,MonitorHealth.evaluate(false,5000L,0L,100000L,"Micrófono interrumpido"));
 }
 @Test public void reportsStartingUntilFirstInference(){
  assertEquals(MonitorHealth.State.STARTING,MonitorHealth.evaluate(true,10000L,0L,20000L,""));
  assertEquals(MonitorHealth.State.STALE,MonitorHealth.evaluate(true,10000L,0L,120000L,""));
 }
 @Test public void recentModelCallbacksCountButStaleInputDoesNot(){
  assertEquals(MonitorHealth.State.LISTENING,MonitorHealth.evaluate(true,10000L,75000L,100000L,""));
  assertEquals(MonitorHealth.State.STALE,MonitorHealth.evaluate(true,10000L,30000L,100000L,""));
  assertEquals(MonitorHealth.State.STALE,MonitorHealth.evaluate(true,10000L,120000L,100000L,""));
 }
}
