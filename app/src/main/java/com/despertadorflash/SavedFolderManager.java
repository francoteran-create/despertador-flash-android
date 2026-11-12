package com.despertadorflash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Copies text records to a user-selected folder; no broad storage permissions. */
public final class SavedFolderManager {
 private static final String PREFS="aurora";
 private static final String KEY="sense_export_folder";
 private final Context context;
 public SavedFolderManager(Context context){this.context=context.getApplicationContext();}
 public Uri selectedFolder(){
  String saved=context.getSharedPreferences(PREFS,0).getString(KEY,null);
  if(saved==null||saved.isEmpty())return null;
  try{return Uri.parse(saved);}catch(Exception e){return null;}
 }
 public void select(Uri folder,int flags){
  int granted=flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  context.getContentResolver().takePersistableUriPermission(folder,granted);
  context.getSharedPreferences(PREFS,0).edit().putString(KEY,folder.toString()).apply();
 }
 public void forget(){context.getSharedPreferences(PREFS,0).edit().remove(KEY).apply();}
 public void copyOne(File source) throws IOException{
  if(source==null||!source.isFile())throw new IOException("El archivo a respaldar no existe");
  Uri folderUri=selectedFolder();
  if(folderUri==null)throw new IOException("Elegí una carpeta antes de guardar");
  try{
   DocumentFile folder=DocumentFile.fromTreeUri(context,folderUri);
   if(folder==null||!folder.canWrite())throw new IOException("La carpeta elegida ya no permite escribir");
   byte[] expected;
   try(FileInputStream input=new FileInputStream(source)){expected=RecordBackupDigest.read(input);}
   DocumentFile target=folder.findFile(source.getName());
   // Never overwrite an earlier backup. The existing file may contain the
   // only surviving copy of a previous session or a different revision.
   if(target!=null){
    try(java.io.InputStream previous=context.getContentResolver().openInputStream(target.getUri())){
     if(RecordBackupDigest.same(expected,RecordBackupDigest.read(previous)))return;
    }
    String base=source.getName().replaceFirst("\\.txt$","");
    int suffix=2;
    do{
     target=folder.findFile(base+"_COPIA_"+suffix+".txt");
     suffix++;
    }while(target!=null&&suffix<1000);
    if(target!=null)throw new IOException("Hay demasiadas copias con el mismo nombre");
    target=folder.createFile("text/plain",base+"_COPIA_"+(suffix-1)+".txt");
   }else{
    target=folder.createFile("text/plain",source.getName());
   }
   if(target==null)throw new IOException("No se pudo crear el archivo en la carpeta");
   try{
    try(FileInputStream in=new FileInputStream(source);
        OutputStream out=context.getContentResolver().openOutputStream(target.getUri(),"wt")){
     if(out==null)throw new IOException("No se pudo abrir el archivo de destino");
     byte[] buffer=new byte[8192];int count;
     while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
     out.flush();
    }
    byte[] actual;
    try(java.io.InputStream confirmed=context.getContentResolver().openInputStream(target.getUri())){
     actual=RecordBackupDigest.read(confirmed);
    }
    if(!RecordBackupDigest.same(expected,actual))throw new IOException("El archivo se copió incompleto o cambió durante el guardado");
   }catch(IOException|RuntimeException error){
    // Only this freshly created backup may be removed; existing files stay intact.
    if(!target.delete())android.util.Log.w("AURORA","Quedó una copia incompleta que deberá revisarse");
    throw error;
   }
  }catch(SecurityException e){throw new IOException("Android revocó el acceso a la carpeta. Elegila nuevamente.",e);}
 }
 public int copyAll(File[] files) throws IOException {
  int copied=0;
  for(File f:files){copyOne(f);copied++;}
  return copied;
 }
}
