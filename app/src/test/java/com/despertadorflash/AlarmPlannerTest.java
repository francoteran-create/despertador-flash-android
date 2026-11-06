package com.despertadorflash;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;
public class AlarmPlannerTest {
 private static final TimeZone BA=TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
 private long timestamp(int year,int month,int day,int hour,int minute){
  Calendar c=Calendar.getInstance(BA);
  c.set(year,month-1,day,hour,minute,0);c.set(Calendar.MILLISECOND,0);
  return c.getTimeInMillis();
 }
 @Test public void sameDayIfFuture(){
  long now=timestamp(2026,11,6,6,30); // Friday
  assertEquals(timestamp(2026,11,6,7,0),AlarmPlanner.nextTrigger(now,7,0,1<<4,BA));
 }
 @Test public void rollsToNextWeekIfAlreadyPassed(){
  long now=timestamp(2026,11,6,7,30);
  assertEquals(timestamp(2026,11,13,7,0),AlarmPlanner.nextTrigger(now,7,0,1<<4,BA));
 }
 @Test public void canSkipWeekends(){
  long now=timestamp(2026,11,6,20,0);
  assertEquals(timestamp(2026,11,9,7,0),AlarmPlanner.nextTrigger(now,7,0,31,BA));
 }
 @Test public void validatesDisabledAndInvalidTimes(){
  long now=timestamp(2026,11,6,20,0);
  assertEquals(-1,AlarmPlanner.nextTrigger(now,7,0,0,BA));
  assertEquals(-1,AlarmPlanner.nextTrigger(now,24,0,127,BA));
  assertEquals(-1,AlarmPlanner.nextTrigger(now,7,60,127,BA));
 }
 @Test public void futureMinuteCountsAsNextDay(){
  long now=timestamp(2026,11,6,7,0);
  assertEquals(timestamp(2026,11,7,7,0),AlarmPlanner.nextTrigger(now,7,0,127,BA));
 }
}
