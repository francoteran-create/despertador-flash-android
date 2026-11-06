package com.despertadorflash;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Append-only text journal for recognized labels and text; never stores raw audio.
 * An interrupted session remains readable as a .txt file (without a closing marker).
 * Reopening an output stream per event avoids keeping a file descriptor open on Android.
 */
public final class SessionJournal {
 private final File file;
 private int written=0;
 private boolean closed=false;
 public SessionJournal(File parent,String name,String header) throws IOException{
  if(!parent.exists()&&!parent.mkdirs())throw new IOException("No se pudo crear la carpeta");
  file=new File(parent,name);
  if(file.exists())throw new IOException("La sesión ya existe");
  try(FileOutputStream out=new FileOutputStream(file,false)){
   byte[] bytes=(header+"\nESTADO: SESIÓN EN CURSO (si falta el cierre, fue interrumpida)\n\n")
     .getBytes(StandardCharsets.UTF_8);
   out.write(bytes);out.getFD().sync();
  }
 }
 public synchronized void append(String line) throws IOException{
  if(closed)throw new IOException("Registro finalizado");
  if(written>=2000)return;
  String clean=line.replace('\r',' ').replace('\n',' ').trim();
  if(clean.isEmpty())return;
  try(FileOutputStream out=new FileOutputStream(file,true)){
   out.write((++written+". "+clean+"\n").getBytes(StandardCharsets.UTF_8));
   out.getFD().sync();
  }
 }
 public synchronized File finish(String footer) throws IOException{
  if(closed)return file;
  closed=true;
  try(FileOutputStream out=new FileOutputStream(file,true)){
   out.write(("\n"+footer+"\nTotal de eventos: "+written+"\nESTADO: SESIÓN FINALIZADA\n").getBytes(StandardCharsets.UTF_8));
   out.getFD().sync();
  }
  return file;
 }
 public synchronized int count(){return written;}
 public File file(){return file;}
}
