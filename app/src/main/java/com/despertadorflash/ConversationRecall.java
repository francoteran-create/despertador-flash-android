package com.despertadorflash;

import java.util.ArrayDeque;

/**
 * Local, extractive conversation recall. Shows only words already returned by
 * Android SpeechRecognizer; it never paraphrases, fabricates or uploads them.
 */
public final class ConversationRecall {
 private static final int MAX_LINES=8;
 private static final int MAX_LINE_CHARS=250;
 private ConversationRecall(){}
 public static String lastPhrases(String record){
  if(record==null||record.trim().isEmpty())
   return "Todavía no hay una conversación registrada para consultar.";
  ArrayDeque<String> snippets=new ArrayDeque<>();
  int total=0;
  for(String line:record.split("\\r?\\n")){
   // SenseHistory writes: 1. [10:25:01] texto reconocido
   String trimmed=line.trim();
    if(!trimmed.matches("^(?:[0-9]{1,4}\\. )?\\[\\d\\d:\\d\\d:\\d\\d\\] .+"))continue;
   if(trimmed.contains("confianza estimada"))continue;
   int endTimestamp=trimmed.indexOf("] ");
   if(endTimestamp<0)continue;
   String fragment=trimmed.substring(endTimestamp+2).trim();
   if(fragment.isEmpty())continue;
   if(fragment.length()>MAX_LINE_CHARS)fragment=fragment.substring(0,MAX_LINE_CHARS)+"…";
   snippets.addLast(fragment);
   if(snippets.size()>MAX_LINES)snippets.removeFirst();
   total++;
   if(total>=2000)break;
  }
  if(snippets.isEmpty())return "Este registro no contiene frases reconocibles.";
  StringBuilder out=new StringBuilder("ÚLTIMAS FRASES RECONOCIDAS\\n");
  out.append("Frases registradas: ").append(total).append(". Se muestran hasta ").append(MAX_LINES).append(".\\n\\n");
  for(String phrase:snippets)out.append("• ").append(phrase).append("\\n");
  out.append("\\nEs una transcripción automática y podría contener errores. No es un resumen interpretativo.");
  return out.toString();
 }
}
