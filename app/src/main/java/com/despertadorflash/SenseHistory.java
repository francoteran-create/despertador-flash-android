package com.despertadorflash;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Locally stored text event sessions; no microphone audio is written to disk. */
public final class SenseHistory {
 private static final String FOLDER="aurora_sense";
 private final File folder;
 private File current;
 private long started;
 private int events;
 public SenseHistory(Context context){
  folder=new File(context.getFilesDir(),FOLDER);
  if(!folder.exists())folder.mkdirs();
 }
 private static String stamp(long time,String format){return new SimpleDateFormat(format,new Locale("es","AR")).format(new Date(time));}
 public synchronized File begin(String type) throws IOException {
  finish();
  started=System.currentTimeMillis();events=0;
  String name=stamp(started,"yyyy-MM-dd_HH-mm-ss-SSS")+"_"+(type.equals("sonidos")?"SONIDOS":"CONVERSACION")+".txt";
  current=new File(folder,name);
  appendRaw("AURORA SENSE | "+(type.equals("sonidos")?"Reconocimiento de sonidos":"Transcripción de conversación")+"\n");
  appendRaw("Inicio: "+stamp(started,"dd/MM/yyyy HH:mm:ss")+"\n");
  appendRaw("Las detecciones son estimaciones automáticas. No se almacena el audio.\n\n");
  return current;
 }
 private void appendRaw(String line) throws IOException {
  if(current==null)return;
  try(FileOutputStream out=new FileOutputStream(current,true)){
   out.write(line.getBytes(StandardCharsets.UTF_8));out.flush();out.getFD().sync();
  }
 }
 public synchronized void event(String observation) throws IOException {
  if(current==null)return;
  String clean=observation.replace('\r',' ').replace('\n',' ').trim();
  if(clean.isEmpty())return;
  appendRaw("["+stamp(System.currentTimeMillis(),"HH:mm:ss")+"] "+clean+"\n");
  events++;
 }
 public synchronized File finish() throws IOException{
  File saved=current;
  if(saved!=null){
   appendRaw("\nFin: "+stamp(System.currentTimeMillis(),"dd/MM/yyyy HH:mm:ss")+"\n");
   appendRaw("Eventos reconocidos: "+events+"\n");
  }
  current=null;return saved;
 }
 public synchronized File getCurrent(){return current;}
 public File[] list(){
  File[] files=folder.listFiles((dir,name)->name.endsWith(".txt"));
  if(files==null)return new File[0];
  Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
  return files;
 }
 public static String read(File file) throws IOException{
  if(file==null||!file.isFile()||file.length()>4_000_000)throw new IOException("Archivo no disponible o demasiado grande");
  ByteArrayOutputStream output=new ByteArrayOutputStream();
  try(FileInputStream input=new FileInputStream(file)){
   byte[] buffer=new byte[8192];int n;
   while((n=input.read(buffer))!=-1)output.write(buffer,0,n);
  }
  return output.toString("UTF-8");
 }
 public static String readableName(File file){
  String label=file.getName().replace(".txt","").replace("_SONIDOS"," · Sonidos").replace("_CONVERSACION"," · Conversación");
  return label.replaceFirst("^(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2})-\\d{3}","$3/$2/$1 · $4:$5:$6");
 }
}