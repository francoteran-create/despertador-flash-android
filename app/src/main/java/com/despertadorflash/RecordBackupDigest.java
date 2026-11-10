package com.despertadorflash;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Streaming SHA-256 for cross-provider SAF copies; does not retain file contents. */
public final class RecordBackupDigest {
 private RecordBackupDigest(){}
 public static byte[] read(InputStream input) throws IOException {
  if(input==null)throw new IOException("No se puede verificar la copia");
  MessageDigest sha;
  try{sha=MessageDigest.getInstance("SHA-256");}
  catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 no disponible",e);}
  byte[] buffer=new byte[8192];int read;
  while((read=input.read(buffer))!=-1){
   if(read>0)sha.update(buffer,0,read);
  }
  return sha.digest();
 }
 public static boolean same(byte[] expected,byte[] actual){
  return expected!=null&&actual!=null&&MessageDigest.isEqual(expected,actual);
 }
}
