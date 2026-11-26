package com.despertadorflash;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes final speech phrases so asynchronous ML Kit callbacks never
 * replace or silently skip intermediate utterances. No audio is stored.
 * Callers keep the original transcript separately before enqueueing.
 */
public final class TranslationWorkQueue {
 public static final int CAPACITY=64;
 public static final class Item {
  public final long id;
  public final String phrase, language;
  public final boolean fromMicrophone;
  private Item(long id,String phrase,String language,boolean fromMicrophone){
   this.id=id;this.phrase=phrase;this.language=language;
   this.fromMicrophone=fromMicrophone;
  }
 }
 private final ArrayDeque<Item> pending=new ArrayDeque<>();
 private Item active;
 private long nextId;
 public synchronized Item offer(String phrase,String destination,boolean fromMicrophone){
  if(phrase==null||phrase.trim().isEmpty()||destination==null||destination.isEmpty())
   throw new IllegalArgumentException("Phrase and destination required");
  if(pending.size()+(active==null?0:1)>=CAPACITY)return null;
  Item item=new Item(++nextId,phrase.trim(),destination,fromMicrophone);
  pending.addLast(item);
  return item;
 }
 /** Start exactly one job. Repeated starts are harmless until complete. */
 public synchronized Item start(){
  if(active!=null)return null;
  active=pending.pollFirst();
  return active;
 }
 /** Completes only the in-flight item; returns false for stale callbacks. */
 public synchronized boolean complete(long id){
  if(active==null||active.id!=id)return false;
  active=null;
  return true;
 }
 public synchronized boolean isActive(long id){
  return active!=null&&active.id==id;
 }
 public synchronized int size(){return pending.size()+(active==null?0:1);}
 public synchronized void clear(){pending.clear();active=null;}
 public synchronized List<String> phrases(){
  List<String> rows=new ArrayList<>();
  if(active!=null)rows.add(active.phrase);
  for(Item item:pending)rows.add(item.phrase);
  return rows;
 }
}
