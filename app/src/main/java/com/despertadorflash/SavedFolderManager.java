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
  if(source==null||!source.exists())return;
  Uri folderUri=selectedFolder();
  if(folderUri==null)return;
  try{
   DocumentFile folder=DocumentFile.fromTreeUri(context,folderUri);
   if(folder==null||!folder.canWrite())throw new IOException("La carpeta elegida ya no permite escribir");
   DocumentFile target=folder.findFile(source.getName());
   if(target==null)target=folder.createFile("text/plain",source.getName());
   if(target==null)throw new IOException("No se pudo crear el archivo en la carpeta");
   try(FileInputStream in=new FileInputStream(source);
       OutputStream out=context.getContentResolver().openOutputStream(target.getUri(),"wt")){
    if(out==null)throw new IOException("No se pudo abrir el archivo de destino");
    byte[] buffer=new byte[8192];int count;
    while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
    out.flush();
   }
  }catch(SecurityException e){throw new IOException("Android revocó el acceso a la carpeta. Elegila nuevamente.",e);}
 }
 public int copyAll(File[] files) throws IOException {
  int copied=0;
  for(File f:files){copyOne(f);copied++;}
  return copied;
 }
}
