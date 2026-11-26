package com.despertadorflash;

import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

public class AuroraLanguageCatalogTest {
 @Test public void hasEightDistinctHumanReadableLanguages(){
  assertEquals(8,AuroraLanguageCatalog.count());
  assertEquals("Español",AuroraLanguageCatalog.name(0));
  assertEquals("中文（简体）",AuroraLanguageCatalog.name(6));
  assertEquals("हिन्दी",AuroraLanguageCatalog.name(7));
  HashSet<String> spoken=new HashSet<>(),translated=new HashSet<>();
  for(int i=0;i<AuroraLanguageCatalog.count();i++){
   assertTrue(spoken.add(AuroraLanguageCatalog.speechTag(i)));
   assertTrue(translated.add(AuroraLanguageCatalog.translationTag(i)));
  }
 }
 @Test public void speechAndTranslationUseSupportedLanguageTags(){
  assertEquals("zh-CN",AuroraLanguageCatalog.speechTag(6));
  assertEquals("zh",AuroraLanguageCatalog.translationTag(6));
  assertEquals("hi-IN",AuroraLanguageCatalog.speechTag(7));
  assertEquals("hi",AuroraLanguageCatalog.translationTag(7));
  assertEquals("fr",AuroraLanguageCatalog.translationTag(3));
  assertEquals("it",AuroraLanguageCatalog.translationTag(4));
  assertEquals("de",AuroraLanguageCatalog.translationTag(5));
 }
 @Test public void originalThreePreferencesKeepTheirIndices(){
  assertEquals("es-AR",AuroraLanguageCatalog.speechTag(0));
  assertEquals("en-US",AuroraLanguageCatalog.speechTag(1));
  assertEquals("pt-BR",AuroraLanguageCatalog.speechTag(2));
  assertEquals("en",AuroraLanguageCatalog.translationTag(1));
  assertEquals("pt",AuroraLanguageCatalog.translationTag(2));
 }
 @Test public void invalidPreferenceFallsBackAndSourceDestinationDiffer(){
  assertEquals(0,AuroraLanguageCatalog.validIndex(-1));
  assertEquals(0,AuroraLanguageCatalog.validIndex(1000));
  for(int i=0;i<AuroraLanguageCatalog.count();i++)
   assertTrue(AuroraLanguageCatalog.isDistinct(i,AuroraLanguageCatalog.defaultDestination(i)));
 }
}
