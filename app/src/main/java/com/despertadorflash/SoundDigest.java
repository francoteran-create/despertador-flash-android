package com.despertadorflash;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic explanation of the sound classifier's observations.
 * Never invents sources, causes, dangers or facts not present in the log.
 */
public final class SoundDigest {
 private SoundDigest(){}
 public static String summarizeRecent(java.util.List<SoundTimeline.Entry> recent){
  if(recent==null||recent.isEmpty())
   return "Todavía no hay sonidos confirmados en los últimos 30 eventos.";
  LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
  int total=0;
  for(SoundTimeline.Entry event:recent){
   if(event==null)continue;
   counts.put(event.label,counts.getOrDefault(event.label,0)+1);
   total++;
   if(total>=SoundTimeline.LIMIT)break;
  }
  if(total==0)return "Todavía no hay sonidos confirmados en los últimos 30 eventos.";
  StringBuilder output=new StringBuilder("ÚLTIMOS SONIDOS EN AURORA\n");
  output.append("Eventos observados: ").append(total).append(" (máximo 30).\n");
  for(Map.Entry<String,Integer> item:counts.entrySet()){
   output.append("• ").append(item.getKey()).append(": ").append(item.getValue()).append(" evento(s).\n");
  }
  output.append("Más reciente: ").append(recent.get(0).label).append(".\n");
  output.append("Son posibles clasificaciones de audio, no hechos confirmados. ");
  output.append("Esta lista vive dentro de la app, sin archivos de sonidos.");
  return output.toString();
 }
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
