package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public class SoundDigestTest {
 @Test public void summarizesCurrentThirtyEventsWithoutAccessingSoundFiles(){
  java.util.List<SoundTimeline.Entry> records=new java.util.ArrayList<>();
  records=SoundTimeline.append(records,"POSIBLE LADRIDO",68,1000);
  records=SoundTimeline.append(records,"POSIBLE TIMBRE DE PUERTA",44,1500);
  records=SoundTimeline.append(records,"POSIBLE LADRIDO",71,2000);
  String summary=SoundDigest.summarizeRecent(records);
  assertTrue(summary.contains("Eventos observados: 3"));
  assertTrue(summary.contains("POSIBLE LADRIDO: 2"));
  assertTrue(summary.contains("POSIBLE TIMBRE DE PUERTA: 1"));
  assertTrue(summary.contains("Más reciente: POSIBLE LADRIDO"));
  assertTrue(SoundDigest.summarizeRecent(new java.util.ArrayList<>()).contains("no hay sonidos confirmados"));
 }
 @Test public void countsOnlyKnownDetections(){
  String sample="1. POSIBLE TIMBRE DE PUERTA · confianza estimada 91%\n"+
                "2. POSIBLE SIRENA · confianza estimada 80%\n"+
                "3. POSIBLE TIMBRE DE PUERTA · confianza estimada 76%\n";
  String summary=SoundDigest.summarize(sample);
  assertTrue(summary.contains("Observaciones analizadas: 3"));
  assertTrue(summary.contains("POSIBLE TIMBRE DE PUERTA: 2"));
  assertFalse(summary.contains("INCENDIO"));
 }
 @Test public void ignoresEmptyConversation(){
  assertTrue(SoundDigest.summarize("1. Hola, buen dia").contains("no hay detecciones"));
 }
 @Test public void readsModernScoresAndExcludesTesterFeedback(){
  String data="[09:01:00] POSIBLE TIMBRE DE PUERTA · puntaje IA 72/100\n"+
   "[09:01:01] POSIBLES GOLPES O IMPACTOS · puntaje de IA 64/100\n"+
   "[09:01:02] VALIDACIÓN MANUAL · NO COINCIDE · POSIBLE TIMBRE DE PUERTA · puntaje IA 72/100\n"+
   "[09:01:03] OMISIÓN MANUAL · Música · informado por la persona que prueba\n";
  String report=SoundDigest.summarize(data);
  assertTrue(report.contains("Observaciones analizadas: 2"));
  assertTrue(report.contains("POSIBLES GOLPES O IMPACTOS"));
  assertFalse(report.contains("Observaciones analizadas: 3"));
 }

}