package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
public class AlarmToneTest {
 @Test public void allTenTonesContainAudibleSamplesAndHaveExpectedLength(){
  assertEquals(10,AlarmTone.NAMES.length);
  for(int i=0;i<10;i++){
   short[] pcm=AlarmTone.samples(i);
   assertEquals(i<5?25600:57600,pcm.length);
   int peak=0;for(short x:pcm)peak=Math.max(peak,Math.abs((int)x));
   assertTrue("Tone "+i,peak>1000);
   assertTrue(peak<=16000);
   for(int j=0;j<i;j++)assertFalse(Arrays.equals(pcm,AlarmTone.samples(j)));
  }
 }
 @Test public void invalidToneIndexesAreClamped(){
  assertArrayEquals(AlarmTone.samples(0),AlarmTone.samples(-1));
  assertArrayEquals(AlarmTone.samples(9),AlarmTone.samples(99));
 }
}
