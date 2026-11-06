package com.despertadorflash;

/** Pure rule for recovering a user's pending alarm snooze after reboot. */
public final class SnoozePolicy {
 private SnoozePolicy(){}
 public static long recoverAt(long storedDeadline,long nowMillis,boolean alarmEnabled){
  if(!alarmEnabled||storedDeadline<=0L)return -1L;
  // A wake-up a whole day later must not revive an old snoozed alarm.
  if(nowMillis-storedDeadline>10*60_000L)return -1L;
  // Give Android a moment to finish boot and deliver the restored alarm.
  return Math.max(storedDeadline,nowMillis+3000L);
 }
}
