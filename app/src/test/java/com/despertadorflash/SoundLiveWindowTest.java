package com.despertadorflash;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SoundLiveWindowTest {
 private Map<String,Float> obs(Object... values){
  Map<String,Float> m=new LinkedHashMap<>();
  for(int i=0;i<values.length;i+=2)m.put((String)values[i],(Float)values[i+1]);
  return m;
 }
 @Test public void newDoorbellIsShownBeforeLouderSimultaneousMusic(){
  SoundLiveWindow w=new SoundLiveWindow();
  w.observe(obs("POSIBLE MÚSICA",.99f,"POSIBLE TIMBRE DE PUERTA",.28f),1000);
  List<SoundLiveWindow.Item> shown=w.snapshot(1100,2);
  assertEquals("POSIBLE TIMBRE DE PUERTA",shown.get(0).label);
  assertEquals("POSIBLE MÚSICA",shown.get(1).label);
 }
 @Test public void oldLoudSoundDoesNotHideNewQuieterSound(){
  SoundLiveWindow window=new SoundLiveWindow();
  window.observe(obs("MÚSICA",.94f),1000);
  window.observe(obs("TIMBRE",.41f),3000);
  List<SoundLiveWindow.Item> current=window.snapshot(3100,4);
  assertEquals(2,current.size());
  assertEquals("TIMBRE",current.get(0).label);
  assertEquals("MÚSICA",current.get(1).label);
  assertEquals(41,current.get(0).score);
 }
 @Test public void staleSoundClearsWithoutMoreCallbacks(){
  SoundLiveWindow window=new SoundLiveWindow();
  window.observe(obs("LADRIDO",.90f),1000);
  assertEquals(1,window.snapshot(4499,4).size());
  assertTrue(window.snapshot(4501,4).isEmpty());
 }
 @Test public void concurrentSoundsAndBadScores(){
  SoundLiveWindow window=new SoundLiveWindow();
  window.observe(obs("VOZ",.55f,"LADRIDO",.65f,"RUIDO",.09f,"INVALIDO",Float.NaN),1000);
  assertEquals(2,window.snapshot(1500,4).size());
  window.clear();
  assertTrue(window.snapshot(1700,4).isEmpty());
 }
 @Test public void doesNotUseFutureTimestampAfterClockReset(){
  SoundLiveWindow window=new SoundLiveWindow();
  window.observe(obs("MÚSICA",.80f),10000);
  assertTrue(window.snapshot(100,4).isEmpty());
 }
}
