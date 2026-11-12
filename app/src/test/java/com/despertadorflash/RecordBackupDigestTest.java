package com.despertadorflash;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecordBackupDigestTest {
 private byte[] sha(String text) throws IOException {
  return RecordBackupDigest.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
 }
 @Test public void detectsTruncatedAndChangedCopies() throws Exception{
  byte[] original=sha("AURORA\n[16:20] POSIBLE TIMBRE DE PUERTA\n");
  assertTrue(RecordBackupDigest.same(original,sha("AURORA\n[16:20] POSIBLE TIMBRE DE PUERTA\n")));
  assertFalse(RecordBackupDigest.same(original,sha("AURORA\n")));
  assertFalse(RecordBackupDigest.same(original,sha("AURORA\n[16:20] POSIBLE LADRIDO\n")));
 }
 @Test public void refusesUnavailableOrNullDigest() throws Exception{
  try{RecordBackupDigest.read(null);fail("null input accepted");}
  catch(IOException expected){}
  assertFalse(RecordBackupDigest.same(null,sha("x")));
 }
}
