package com.despertadorflash;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationRecordFilterTest {
 @Test public void neverTreatSoundFileAsConversation() throws Exception {
  File dir=Files.createTempDirectory("aurora-conversation-test").toFile();
  try{
   File speech=new File(dir,"2026_11_11_CONVERSACION.txt");
   File sounds=new File(dir,"2026_11_11_SONIDOS.txt");
   assertTrue(speech.createNewFile());
   assertTrue(sounds.createNewFile());
   File[] filtered=ConversationRecordFilter.conversations(new File[]{sounds,speech});
   assertEquals(1,filtered.length);
   assertEquals(speech,filtered[0]);
   assertTrue(sounds.exists()); // Historical sound files are never deleted.
  }finally{
   File[] files=dir.listFiles();
   if(files!=null)for(File file:files)file.delete();
   dir.delete();
  }
 }
 @Test public void handlesNoFiles(){
  assertEquals(0,ConversationRecordFilter.conversations(null).length);
 }
}
