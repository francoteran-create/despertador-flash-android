package com.despertadorflash;

/**
 * Retry state for Android's short-lived SpeechRecognizer sessions.
 * Pure Java so the policy is tested without an Android device.
 * Temporary recognizer failures are retried with capped backoff,
 * not in a tight loop that drains the battery.
 */
public final class SpeechRetryController {
 private static final long[] BACKOFF_MS={800,1400,2400,4000,6000,9000,15000,22000,30000};
 private int failures=0;
 public void reset(){failures=0;}
 public int failures(){return failures;}
 public long silenceDelay(){return 1200L;}
 /** Keep recovering after the initial failures instead of silently ending
  * a user's conversation. Delays cap at 30 s to avoid tight Android loops.
  * Fatal permission/language errors are handled separately by the caller.
  */
 public long onTemporaryFailure(){
  int index=Math.min(failures,BACKOFF_MS.length-1);
  if(failures<Integer.MAX_VALUE)failures++;
  return BACKOFF_MS[index];
 }
 public static boolean isSilence(int error){return error==6||error==7;} // NO_MATCH or SPEECH_TIMEOUT
 public static boolean isTemporary(int error){
  return error==1||error==2||error==3||error==4||error==5||error==8||error==10||error==11;
  // Includes temporary audio/client conditions; permission and language errors remain fatal.
 }
 public static String explanation(int error){
  switch(error){
   case 1:case 2:return "Conexión del reconocimiento interrumpida";
   case 3:return "No se pudo acceder al audio del micrófono";
   case 4:case 11:return "Servicio de reconocimiento temporalmente no disponible";
   case 5:return "Android rechazó la solicitud de transcripción";
   case 6:case 7:return "No hubo voz reconocible";
   case 8:case 10:return "Reconocedor ocupado o con demasiadas solicitudes";
   case 9:return "Permiso de micrófono insuficiente";
   case 12:case 13:return "Idioma no admitido o no disponible";
   default:return "Código de Android: "+error;
  }
 }
}
