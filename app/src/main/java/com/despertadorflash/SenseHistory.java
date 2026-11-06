package com.despertadorflash;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A single listening session lives in RAM until the user stops listening or
 * leaves SenseActivity. Only recognized text is persisted, never raw audio.
 */
public final class SenseHistory {
 private static final String FOLDER="aurora_sense";
 private final File folder;
 private final ArrayList<String> events=new ArrayList<>();
 private boolean active=false;
 private long started;
 private String kind;
 private SessionJournal journal;
 private boolean diskHealthy=true;
 public SenseHistory(Context context){
  folder=new File(context.getFilesDir(),FOLDER);
  if(!folder.exists())folder.mkdirs();
 }
 private static String stamp(long time,String format){
  return new SimpleDateFormat(format,new Locale("es","AR")).format(new Date(time));
 }
 public synchronized void begin(String type){
  started=System.currentTimeMillis();
  kind=type.equals("sonidos")?"sonidos":"conversacion";
  events.clear();journal=null;diskHealthy=true;active=true;
 }
 public synchronized void event(String observation){
  if(!active||observation==null||events.size()>=2000)return;
  String clean=observation.replace('\r',' ').replace('\n',' ').trim();
  if(clean.isEmpty())return;
  String dated="["+stamp(System.currentTimeMillis(),"HH:mm:ss")+"] "+clean;
  events.add(dated);
  if(!diskHealthy)return;
  try{
   if(journal==null){
    String suffix=kind.equals("sonidos")?"SONIDOS":"CONVERSACION";
    String name=stamp(started,"yyyy-MM-dd_HH-mm-ss-SSS")+"_"+suffix+".txt";
    String header="AURORA SENSE | "+(kind.equals("sonidos")?"Reconocimiento de sonidos":"Transcripción de conversación")+
      "\nFecha: "+stamp(started,"dd/MM/yyyy")+"\nInicio: "+stamp(started,"HH:mm")+
      "\nResultados estimados por IA. No se guardó audio.";
    journal=new SessionJournal(folder,name,header);
   }
   journal.append(dated);
  }catch(IOException error){
   diskHealthy=false;
   android.util.Log.w("Aurora","No se pudo registrar un evento inmediatamente",error);
  }
 }
 public synchronized File finish() throws IOException{
  if(!active)return null;
  active=false;
  if(events.isEmpty()){events.clear();return null;}
  if(diskHealthy&&journal!=null){
   try{
    File completed=journal.finish("Finalización: "+stamp(System.currentTimeMillis(),"HH:mm"));
    events.clear();journal=null;return completed;
   }catch(IOException error){
    diskHealthy=false;
    android.util.Log.w("Aurora","Error cerrando el historial incremental",error);
   }
  }
  // Fallback to original full write if the incremental writer could not persist.
  if(!folder.exists()&&!folder.mkdirs())throw new IOException("No se pudo crear carpeta de registros");
  String suffix=kind.equals("sonidos")?"SONIDOS":"CONVERSACION";
  File output=journal!=null?journal.file():new File(folder,stamp(started,"yyyy-MM-dd_HH-mm-ss-SSS")+"_"+suffix+".txt");
  try(FileOutputStream stream=new FileOutputStream(output,false)){
   StringBuilder text=new StringBuilder();
   text.append("AURORA SENSE | ").append(kind.equals("sonidos")?"Reconocimiento de sonidos":"Transcripción de conversación").append("\n");
   text.append("Fecha: ").append(stamp(started,"dd/MM/yyyy")).append("\n");
   text.append("Inicio: ").append(stamp(started,"HH:mm")).append("\n");
   text.append("Finalización: ").append(stamp(System.currentTimeMillis(),"HH:mm")).append("\n");
   text.append("Resultados estimados por IA. No se guardó audio.\n\n");
   for(int i=0;i<events.size();i++)text.append(i+1).append(". ").append(events.get(i)).append("\n");
   text.append("\nTotal de eventos: ").append(events.size()).append("\nESTADO: SESIÓN FINALIZADA\n");
   stream.write(text.toString().getBytes(StandardCharsets.UTF_8));stream.getFD().sync();
  }finally{events.clear();journal=null;}
  return output;
 }
 public synchronized boolean hasActive(){return active;}
 public File[] list(){
  File[] files=folder.listFiles((dir,name)->name.endsWith(".txt"));
  if(files==null)return new File[0];
  Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
  return files;
 }
 /** Only a TXT file belonging to this private history directory may be deleted. */
 public synchronized void delete(File file) throws IOException {
  if(file==null||!file.getName().endsWith(".txt")||!file.getCanonicalFile().getParentFile().equals(folder.getCanonicalFile()))
   throw new IOException("Registro no válido para borrar");
  if(!file.isFile()||!file.delete())throw new IOException("No se pudo borrar este registro");
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
