package com.despertadorflash;

/**
 * In-memory microphone/IA diagnostics. No raw audio, labels, transcript,
 * profile or identifying personal data are recorded here.
 */
public final class SoundCaptureTelemetry {
 private long startedAt=-1L,lastCallbackAt=-1L,samples=0L,results=0L,recoveries=0L;
 private String lastFailure="ninguno";

 public synchronized void begin(long elapsedMs){
  startedAt=elapsedMs;lastCallbackAt=-1L;samples=0L;results=0L;
  recoveries=0L;lastFailure="ninguno";
 }
 public synchronized void samplesRead(int count){
  if(count>0)samples+=count;
 }
 public synchronized void classified(long elapsedMs){
  if(elapsedMs>=0L){results++;lastCallbackAt=elapsedMs;}
 }
 public synchronized void recovering(String reason){
  recoveries++;
  lastFailure=safeReason(reason);
 }
 public synchronized void failed(String reason){
  lastFailure=safeReason(reason);
 }
 public synchronized long callbackAge(long now){
  return lastCallbackAt<0L||now<lastCallbackAt?-1L:now-lastCallbackAt;
 }
 public synchronized long getResults(){return results;}
 public synchronized long getSamples(){return samples;}
 public synchronized long getRecoveries(){return recoveries;}
 public synchronized String getLastFailure(){return lastFailure;}
 public synchronized String report(long now){
  return "Muestras recibidas: "+samples+
   "\nRespuestas del modelo: "+results+
   "\nEdad ultima respuesta (ms): "+(lastCallbackAt<0?"sin respuesta":String.valueOf(callbackAge(now)))+
   "\nRecuperaciones del micrófono/modelo: "+recoveries+
   "\nÚltimo fallo: "+lastFailure;
 }
 private static String safeReason(String raw){
  if(raw==null||raw.isEmpty())return "fallo sin detalle";
  // Restrict to a short technical explanation to avoid accidentally exposing
  // strings that contain user-provided text from framework exceptions.
  String clean=raw.replace('\n',' ').replace('\r',' ').trim();
  return clean.length()>180?clean.substring(0,180):clean;
 }
}
