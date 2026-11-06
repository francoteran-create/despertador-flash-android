package com.despertadorflash;

/**
 * elapsedRealtime resets at boot. A wall deadline is the recovery fallback
 * whenever a deadline belongs to a different Android boot session.
 */
public final class TimerBootClock {
 private TimerBootClock(){}
 public static long elapsedDeadlineForBoot(long storedElapsed,int savedBoot,int currentBoot){
  if(savedBoot>=0&&currentBoot>=0&&savedBoot!=currentBoot)return 0L;
  return storedElapsed;
 }
}
