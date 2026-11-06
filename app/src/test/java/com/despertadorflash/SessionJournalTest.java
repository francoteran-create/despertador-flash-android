package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class SessionJournalTest {
 @Test public void keepsCompletedAndInterruptedSessionRecords() throws Exception {
  File folder=Files.createTempDirectory("aurora_history").toFile();
  SessionJournal interrupted=new SessionJournal(folder,"incomplete.txt","AURORA TEST");
  interrupted.append("[09:40:00] POSIBLE TIMBRE");
  String before=new String(Files.readAllBytes(interrupted.file().toPath()),StandardCharsets.UTF_8);
  assertTrue(before.contains("POSIBLE TIMBRE"));
  assertFalse(before.contains("ESTADO: SESIÓN FINALIZADA"));
  SessionJournal completed=new SessionJournal(folder,"complete.txt","AURORA TEST");
  completed.append("[09:41:00] La puerta se abrió\ntexto de prueba");
  File record=completed.finish("Finalización: 09:42");
  String after=new String(Files.readAllBytes(record.toPath()),StandardCharsets.UTF_8);
  assertTrue(after.contains("ESTADO: SESIÓN FINALIZADA"));
  assertTrue(after.contains("Total de eventos: 1"));
  assertTrue(after.contains("se abrió texto de prueba"));
  assertEquals(1,completed.count());
 }
 @Test public void duplicateFileNamesAreNotSilentlyOverwritten() throws Exception {
  File folder=Files.createTempDirectory("aurora_collision").toFile();
  new SessionJournal(folder,"a.txt","TEST");
  try{
   new SessionJournal(folder,"a.txt","OVERWRITE");
   fail("Existing history must never be overwritten on startup");
  }catch(IOException expected){
   assertTrue(expected.getMessage().contains("existe"));
  }
 }
}
