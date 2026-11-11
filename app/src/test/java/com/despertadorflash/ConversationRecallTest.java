package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public class ConversationRecallTest {
 @Test public void recallsExactWordsWithoutInventingSummary(){
  String text="AURORA SENSE | Transcripción de conversación\n"
   +"1. [14:15:00] ¿Dónde están mis llaves?\n"
   +"2. [14:15:01] Están sobre la mesa.\n";
  String recap=ConversationRecall.lastPhrases(text);
  assertTrue(recap.contains("¿Dónde están mis llaves?"));
  assertTrue(recap.contains("Están sobre la mesa."));
  assertTrue(recap.contains("Frases registradas: 2"));
  assertFalse(recap.toLowerCase().contains("llaves perdidas"));
 }
 @Test public void neverTreatsSoundEventsAsSpeech(){
  assertTrue(ConversationRecall.lastPhrases("1. [10:00:00] POSIBLE SIRENA · confianza estimada 90%")
   .contains("no contiene frases"));
 }
 @Test public void returnsUsefulMessageWhenEmpty(){
  assertTrue(ConversationRecall.lastPhrases("").contains("no hay una conversación"));
 }
 @Test public void readsCurrentIncrementalHistoryFormat(){
  String record="AURORA SENSE | Transcripción de conversación\n"+
   "[09:14:12] Quiero traducir esta frase\n"+
   "[09:14:13] La segunda conversación sigue aquí\n";
  String recap=ConversationRecall.lastPhrases(record);
  assertTrue(recap.contains("Quiero traducir esta frase"));
  assertTrue(recap.contains("La segunda conversación sigue aquí"));
  assertTrue(recap.contains("Frases registradas: 2"));
 }

}
