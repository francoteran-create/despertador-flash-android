package com.despertadorflash;

/**
 * Monotonic timer deadline: manual clock/time-zone changes do not change the
 * remaining duration while the phone is running. The wall clock is a reboot
 * recovery fallback because elapsedRealtime resets on reboot.
 */
public final class TimerDeadline {
 private TimerDeadline(){}
 public static long remaining(long wallDeadline,long elapsedDeadline,long nowWall,long nowElapsed){
  if(wallDeadline<=0)return 0L;
  if(elapsedDeadline>0L)return Math.max(0L,elapsedDeadline-nowElapsed);
  return Math.max(0L,wallDeadline-nowWall);
 }
 public static long newWallDeadline(long remaining,long nowWall){
  if(remaining<=0)return nowWall;
  return nowWall+remaining;
 }
}
