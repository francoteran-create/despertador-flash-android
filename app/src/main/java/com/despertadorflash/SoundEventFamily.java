package com.despertadorflash;

/**
 * Only unquestionably overlapping YAMNet hypotheses share an alert cooldown.
 * Do NOT group every sound from the same setting: doorbells and door knocks,
 * or horns and vehicle engines, are independent useful alerts.
 */
public final class SoundEventFamily {
 private SoundEventFamily(){}
 public static String key(String label){
  if(label==null||label.trim().isEmpty())return "";
  switch(label){
   case "POSIBLE PERRO":
   case "POSIBLE PERRO LADRANDO":
    return "FAMILIA_PERRO";
   case "POSIBLE PUERTA O CAJÓN":
   case "POSIBLE PORTAZO":
    return "FAMILIA_PUERTA_MOVIMIENTO";
   case "POSIBLE VOZ HUMANA":
   case "POSIBLE CONVERSACIÓN":
    return "FAMILIA_VOZ";
   default:
    return label;
  }
 }
}
