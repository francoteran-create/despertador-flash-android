package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TranslationWorkQueueTest {
 @Test public void handlesRapidPhrasesWithoutSkippingOrReordering(){
  TranslationWorkQueue q=new TranslationWorkQueue();
  TranslationWorkQueue.Item first=q.offer("Hola","Inglés",true);
  TranslationWorkQueue.Item second=q.offer("¿Cómo estás?","Inglés",true);
  TranslationWorkQueue.Item third=q.offer("Hasta mañana","Inglés",true);
  assertEquals(3,q.size());
  assertEquals(first.id,q.start().id);
  assertNull(q.start());
  assertFalse(q.complete(third.id));
  assertEquals(3,q.size());
  assertTrue(q.complete(first.id));
  assertEquals(second.id,q.start().id);
  assertTrue(q.complete(second.id));
  assertEquals(third.id,q.start().id);
  assertTrue(q.complete(third.id));
  assertEquals(0,q.size());
 }
 @Test public void doesNotLoseOriginalOnFullQueue(){
  TranslationWorkQueue q=new TranslationWorkQueue();
  for(int i=0;i<TranslationWorkQueue.CAPACITY;i++)
   assertNotNull(q.offer("Frase "+i,"Portugués",true));
  assertNull(q.offer("Frase extra","Portugués",true));
  assertEquals(TranslationWorkQueue.CAPACITY,q.size());
  assertEquals("Frase 0",q.phrases().get(0));
 }
 @Test public void cancellingInvalidatesInFlightCompletion(){
  TranslationWorkQueue q=new TranslationWorkQueue();
  long id=q.offer("Antes","Inglés",true).id;
  q.start();
  q.clear();
  assertFalse(q.complete(id));
  assertFalse(q.isActive(id));
  assertEquals(0,q.size());
  assertEquals("Después",q.offer("Después","Inglés",false).phrase);
 }
 @Test public void rejectsEmptyPhrase(){
  TranslationWorkQueue q=new TranslationWorkQueue();
  try{q.offer(" ","Inglés",true);fail("Empty accepted");}
  catch(IllegalArgumentException expected){}
 }
}
