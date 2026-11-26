package com.despertadorflash;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class SoundTimelineTest {
 @Test public void keepsExactlyLastThirtyInReverseChronologicalOrder(){
  List<SoundTimeline.Entry> events=new ArrayList<>();
  for(int i=1;i<=38;i++)events=SoundTimeline.append(events,"Sonido "+i,i,1000L*i);
  assertEquals(30,events.size());
  assertEquals("Sonido 38",events.get(0).label);
  assertEquals(38000L,events.get(0).timestamp);
  assertEquals("Sonido 9",events.get(29).label);
  assertEquals(9000L,events.get(29).timestamp);
 }
 @Test public void simultaneousDifferentCategoriesAreNotMerged(){
  List<SoundTimeline.Entry> events=new ArrayList<>();
  events=SoundTimeline.append(events,"Posible ladrido",81,1000);
  events=SoundTimeline.append(events,"Posible timbre",39,1000);
  assertEquals(2,events.size());
  assertEquals("Posible timbre",events.get(0).label);
  assertEquals("Posible ladrido",events.get(1).label);
 }
 @Test public void emptyAndOldEventsCannotGrowWithoutLimit(){
  List<SoundTimeline.Entry> events=new ArrayList<>();
  events=SoundTimeline.append(events,"Timbre",200,100);
  assertEquals(100,events.get(0).score);
  try{SoundTimeline.append(events," ",23,200);fail("Empty label accepted");}
  catch(IllegalArgumentException expected){}
  assertEquals(1,events.size());
  try{events.add(new SoundTimeline.Entry("music",80,100));fail("Mutable list");}
  catch(UnsupportedOperationException expected){}
 }
 @Test public void reopeningStoreNormalizesMoreThanThirtyLegacyRecords(){
  List<SoundTimeline.Entry> saved=new ArrayList<>();
  for(int i=0;i<60;i++)saved.add(new SoundTimeline.Entry("A"+i,50,i));
  assertEquals(30,SoundTimeline.normalized(saved).size());
  assertEquals("A0",SoundTimeline.normalized(saved).get(0).label);
 }
}
