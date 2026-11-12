package com.despertadorflash;

/**
 * Detects SpeechRecognizer sessions that stop returning callbacks without
 * reporting an error. Android controls the recognizer lifetime; this class
 * never claims that audio capture is uninterrupted.
 *
 * Timings use elapsedRealtime(), not the wall clock.
 */
public final class SpeechStallWatchdog {
 public static final long WAITING_FOR_SPEECH_MS=45000L;
 public static final long SPEAKING_MS=90000L;
 public static final long WAITING_FOR_RESULT_MS=18000L;

 private enum Phase { STOPPED, WAITING_FOR_SPEECH, SPEAKING, WAITING_FOR_RESULT }
 private Phase phase=Phase.STOPPED;
 private long lastProgressMs=-1L;

 public void started(long nowMs){
  phase=Phase.WAITING_FOR_SPEECH;
  lastProgressMs=nowMs;
 }
 public void ready(long nowMs){
  if(phase==Phase.STOPPED)return;
  phase=Phase.WAITING_FOR_SPEECH;
  lastProgressMs=nowMs;
 }
 public void speechBegan(long nowMs){
  if(phase==Phase.STOPPED)return;
  phase=Phase.SPEAKING;
  lastProgressMs=nowMs;
 }
 public void partialResult(long nowMs){
  if(phase==Phase.STOPPED)return;
  phase=Phase.SPEAKING;
  lastProgressMs=nowMs;
 }
 public void speechEnded(long nowMs){
  if(phase==Phase.STOPPED)return;
  phase=Phase.WAITING_FOR_RESULT;
  lastProgressMs=nowMs;
 }
 public boolean timedOut(long nowMs){
  if(phase==Phase.STOPPED||nowMs<lastProgressMs)return false;
  long elapsed=nowMs-lastProgressMs;
  switch(phase){
   case WAITING_FOR_RESULT:return elapsed>=WAITING_FOR_RESULT_MS;
   case SPEAKING:return elapsed>=SPEAKING_MS;
   default:return elapsed>=WAITING_FOR_SPEECH_MS;
  }
 }
 public String timeoutReason(){
  return phase==Phase.WAITING_FOR_RESULT
   ?"El reconocimiento no devolvió las palabras finales"
   :"El reconocimiento dejó de responder";
 }
 public void reset(){
  phase=Phase.STOPPED;
  lastProgressMs=-1L;
 }
}