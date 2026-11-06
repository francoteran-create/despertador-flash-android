package com.despertadorflash;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure Java calendar planner: 7-bit mask Monday=bit0 ... Sunday=bit6. */
public final class AlarmPlanner {
 private AlarmPlanner(){}
 public static long nextTrigger(long nowMillis,int hour,int minute,int daysMask,TimeZone zone){
  if(hour<0||hour>23||minute<0||minute>59||(daysMask&127)==0)return -1L;
  Calendar base=Calendar.getInstance(zone);
  base.setTimeInMillis(nowMillis);
  for(int offset=0;offset<=7;offset++){
   Calendar candidate=(Calendar)base.clone();
   candidate.add(Calendar.DAY_OF_YEAR,offset);
   candidate.set(Calendar.HOUR_OF_DAY,hour);
   candidate.set(Calendar.MINUTE,minute);
   candidate.set(Calendar.SECOND,0);
   candidate.set(Calendar.MILLISECOND,0);
   int mondayBased=(candidate.get(Calendar.DAY_OF_WEEK)+5)%7;
   if((daysMask&(1<<mondayBased))!=0&&candidate.getTimeInMillis()>nowMillis)return candidate.getTimeInMillis();
  }
  return -1L;
 }
}
