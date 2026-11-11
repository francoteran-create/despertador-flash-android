package com.despertadorflash;

/**
 * Finite retry policy for interrupted on-device sound recognition.
 * Prevents an infinite loop that would hide a microphone failure.
 */
public final class SoundRecoveryPolicy {
 private static final long[] DELAYS_MS={700L,1600L,3500L};
 private SoundRecoveryPolicy(){}
 public static long delayAfterFailure(int failures){
  if(failures<1||failures>DELAYS_MS.length)return -1L;
  return DELAYS_MS[failures-1];
 }
 public static boolean isStalled(long lastCallbackElapsed,long nowElapsed){
  return lastCallbackElapsed>0L&&nowElapsed>=lastCallbackElapsed&&
   nowElapsed-lastCallbackElapsed>20_000L;
 }
}
