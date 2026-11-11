package com.despertadorflash;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic explanation of the sound classifier's observations.
 * Never invents sources, causes, dangers or facts not present in the log.
 */
public final class SoundDigest {
 private SoundDigest(){}
 public static String summarize(String data){
  if(data==null||data.trim().isEmpty())return "No hay detecciones disponibles.";
  LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
  String latest="";
  int total=0;
  for(String line:data.split("\\r?\n")){
   if(total>=200)break;
   int start=line.indexOf("POSIBLE");
   if(start<0)start=line.indexOf("POSIBLES");
   if(start<0)continue;
   // Saved formats differ across app versions; new 4.x writes "puntaje IA".
   // Manual tester feedback must not be counted as a new sound.
   if(line.contains("VALIDACIÓN MANUAL")||line.contains("OMISIÓN MANUAL"))continue;
   if(!line.contains("confianza estimada")&&!line.contains("puntaje IA")
      &&!line.contains("puntaje de IA"))continue;
   String text=line.substring(start);
   int end=text.indexOf(" · ");
   if(end>=0)text=text.substring(0,end);
   text=text.trim();
   if(text.isEmpty()||text.length()>120)continue;
   counts.put(text,counts.getOrDefault(text,0)+1);
   latest=text;
   total++;
  }
  if(total==0)return "En los registros elegidos no hay detecciones de sonido para resumir.";
  StringBuilder b=new StringBuilder("RESUMEN DE DETECCIONES REGISTRADAS\n");
  b.append("Observaciones analizadas: ").append(total).append(".\n");
  for(Map.Entry<String,Integer> entry:counts.entrySet())
   b.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" registro(s).\n");
  b.append("Última categoría observada: ").append(latest).append(".\n");
  b.append("Estas son clasificaciones aproximadas del micrófono; no confirman qué ocurrió ni quién produjo un sonido.");
  return b.toString();
 }
}
