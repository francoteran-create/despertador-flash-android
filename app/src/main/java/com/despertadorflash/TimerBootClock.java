package com.despertadorflash;

/**
 * elapsedRealtime resets at boot. A wall deadline is the recovery fallback
 * whenever a deadline belongs to a different Android boot session.
 */
public final class TimerBootClock {
 private TimerBootClock(){}
 public static long elapsedDeadlineForBoot(long storedElapsed,int savedBoot,int currentBoot){
  if(currentBoot>=0&&savedBoot!=currentBoot)return 0L;
  // If the current boot count is available but the saved one is not,
  // legacy timers must recover from the wall clock instead of trusting a
  // monotonic deadline that might belong to an earlier boot.
  return storedElapsed;
 }
}
