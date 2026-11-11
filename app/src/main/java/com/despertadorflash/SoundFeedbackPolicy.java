package com.despertadorflash;

/**
 * Global feedback throttling, independent of the classifier's per-label gate.
 * This prevents many simultaneous classes from triggering a constant torch
 * or vibrator pulse. Timestamps come from SystemClock.elapsedRealtime().
 */
public final class SoundFeedbackPolicy {
 public static final long MIN_EVENT_GAP_MS=3500L;
 public static final long MIN_FLASH_GAP_MS=12000L;
 public static final long FLASH_DURATION_MS=2600L;
 private long lastFeedback=-1L;
 private long lastFlash=-1L;

 public synchronized boolean allowEvent(long now){
  if(now<0)return false;
  if(lastFeedback>=0&&now>=lastFeedback&&now-lastFeedback<MIN_EVENT_GAP_MS)return false;
  lastFeedback=now;
  return true;
 }
 public synchronized boolean allowFlash(long now){
  if(now<0)return false;
  if(lastFlash>=0&&now>=lastFlash&&now-lastFlash<MIN_FLASH_GAP_MS)return false;
  lastFlash=now;
  return true;
 }
 public synchronized void reset(){
  lastFeedback=-1L;
  lastFlash=-1L;
 }
}
