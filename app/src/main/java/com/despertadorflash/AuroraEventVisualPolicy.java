package com.despertadorflash;

import java.util.Locale;

/** Maps ONLY confirmed sound labels to a visual event, never raw model guesses. */
public final class AuroraEventVisualPolicy {
 public enum Event { NONE, DOOR, DOG, HORN, MUSIC, VOICES, SIREN, BABY, PHONE, OTHER }
 private AuroraEventVisualPolicy(){}
 public static Event fromConfirmedLabel(String label){
  if(label==null)return Event.NONE;
  String x=label.toUpperCase(Locale.ROOT);
  if(x.contains("PUERTA")||x.contains("PORTAZO")||x.contains("GOLPE")||
     x.contains("TIMBRE")||x.contains("CAJÓN"))return Event.DOOR;
  if(x.contains("PERRO")||x.contains("LADRANDO")||x.contains("AULLIDO"))return Event.DOG;
  if(x.contains("BOCINA"))return Event.HORN;
  if(x.contains("MÚSICA")||x.contains("CAMPANA")||x.contains("APLAUSO"))return Event.MUSIC;
  if(x.contains("CONVERSACIÓN")||x.contains("VOZ HUMANA")||x.contains("GRITO")||
     x.contains("RISAS"))return Event.VOICES;
  if(x.contains("SIRENA")||x.contains("ALARMA")||x.contains("INCENDIO"))return Event.SIREN;
  if(x.contains("LLANTO"))return Event.BABY;
  if(x.contains("TELÉFONO"))return Event.PHONE;
  return Event.OTHER;
 }
 public static String accessibleName(Event event){
  switch(event){
   case DOOR:return "Dibujo de puerta golpeada";
   case DOG:return "Dibujo de perro ladrando";
   case HORN:return "Dibujo de bocina";
   case MUSIC:return "Dibujo de música";
   case VOICES:return "Dibujo de personas conversando";
   case SIREN:return "Dibujo de sirena";
   case BABY:return "Dibujo de llanto";
   case PHONE:return "Dibujo de teléfono";
   case OTHER:return "Sonido confirmado";
   default:return "Nube AURORA en espera";
  }
 }
}