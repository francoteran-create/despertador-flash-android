package com.despertadorflash;

/**
 * Initial multilingual catalog for conversation input and translation.
 * Android SpeechRecognizer may not support every requested spoken locale;
 * ML Kit downloads translation models separately on demand.
 *
 * Append new entries rather than reordering: old preferences store indices.
 */
public final class AuroraLanguageCatalog {
 private AuroraLanguageCatalog(){}

 public static final String[] LABELS={
  "Español", "English", "Português", "Français", "Italiano",
  "Deutsch", "中文（简体）", "हिन्दी"
 };
 private static final String[] SPOKEN_TAGS={
  "es-AR","en-US","pt-BR","fr-FR","it-IT","de-DE","zh-CN","hi-IN"
 };
 private static final String[] TRANSLATION_TAGS={
  "es","en","pt","fr","it","de","zh","hi"
 };

 public static int count(){return LABELS.length;}
 public static int validIndex(int index){return index>=0&&index<count()?index:0;}
 public static String name(int index){return LABELS[validIndex(index)];}
 public static String speechTag(int index){return SPOKEN_TAGS[validIndex(index)];}
 public static String translationTag(int index){return TRANSLATION_TAGS[validIndex(index)];}
 public static int defaultDestination(int source){return validIndex(source)==0?1:0;}
 public static boolean isDistinct(int source,int target){
  return validIndex(source)!=validIndex(target);
 }
}
