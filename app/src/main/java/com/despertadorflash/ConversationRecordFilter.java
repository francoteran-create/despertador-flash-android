package com.despertadorflash;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Keep conversations accessible as files while environmental sounds are
 * represented by a bounded in-app event list. Legacy sound files are not
 * deleted: they remain available in the archived view.
 */
public final class ConversationRecordFilter {
 private ConversationRecordFilter(){}
 public static boolean isConversation(File file){
  return file!=null&&file.isFile()&&
   file.getName().toUpperCase(java.util.Locale.ROOT).endsWith("_CONVERSACION.TXT");
 }
 public static File[] conversations(File[] files){
  if(files==null)return new File[0];
  List<File> found=new ArrayList<>();
  for(File file:files)if(isConversation(file))found.add(file);
  return found.toArray(new File[0]);
 }
}
