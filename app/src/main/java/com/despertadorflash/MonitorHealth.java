package com.despertadorflash;

/** Pure, testable diagnostic state of the on-device sound monitor. */
public final class MonitorHealth {
 public enum State { OFF, STARTING, LISTENING, STALE, ERROR }
 private MonitorHealth(){}
 public static State evaluate(boolean active,long startedAt,long classifierHeartbeat,long now,String error){
  if(error!=null&&!error.trim().isEmpty())return State.ERROR;
  if(!active)return State.OFF;
  if(classifierHeartbeat>0L){
   if(classifierHeartbeat<=now+5000L&&now-classifierHeartbeat<=45000L)return State.LISTENING;
   return State.STALE; // Once the model replied, stale callbacks are not "still starting".
  }
  if(startedAt>0&&startedAt<=now+5000L&&now-startedAt<=90000L)return State.STARTING;
  return State.STALE;
 }
 public static String label(State state){
  switch(state){
   case OFF:return "DETENIDA";
   case STARTING:return "INICIANDO · AÚN SIN CONFIRMACIÓN DEL MODELO";
   case LISTENING:return "RECIBIENDO RESULTADOS DEL DETECTOR";
   case STALE:return "SIN DATOS RECIENTES · NO CONFÍES EN SUS AVISOS";
   case ERROR:return "DETENIDA POR ERROR · REVISÁ EL DIAGNÓSTICO";
   default:return "ESTADO DESCONOCIDO";
  }
 }
}
