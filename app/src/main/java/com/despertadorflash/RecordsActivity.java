package com.despertadorflash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/** Searchable, in-app record folder and Android folder selection for automatic copies. */
public final class RecordsActivity extends Activity {
 private static final int CHOOSE_FOLDER=81;
 private static final int BROWSE_FOLDER=82;
 private final ArrayList<File> shown=new ArrayList<>();
 private SenseHistory history;
 private SavedFolderManager destination;
 private LinearLayout results;
 private TextView folderStatus, count;
 private EditText search;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(dp(10),dp(10),dp(10),dp(10));return v;}
 private Button btn(String name){Button b=new Button(this);b.setText(name);AuroraDesign.action(b,0xff163e5a,0xff282857,0xff477e9c);return b;}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);history=new SenseHistory(this);destination=new SavedFolderManager(this);draw();}
 private void draw(){
  ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(18),dp(16),dp(24));root.setBackground(new AuroraSkyDrawable());sc.addView(root);setContentView(sc);
  Button back=btn("← VOLVER");root.addView(back);back.setOnClickListener(v->finish());
  TextView title=txt("CARPETA DE REGISTROS",25);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xffb2f4ff);root.addView(title);
  root.addView(txt("Buscá y abrí las escuchas guardadas. Las copias en una carpeta del teléfono también se podrán ver desde Archivos.",15));
  folderStatus=txt("",14);folderStatus.setTextColor(0xffc0e2ef);root.addView(folderStatus);
  Button select=btn("📁  ELEGIR CARPETA DEL TELÉFONO");root.addView(select);select.setOnClickListener(v->chooseFolder());
  Button browse=btn("↗  BUSCAR ARCHIVOS EN LA CARPETA ELEGIDA");root.addView(browse);browse.setOnClickListener(v->browseFolder());
  root.addView(AuroraDesign.overline(this,"EXPLORADOR DE SESIONES"));
  count=txt("",15);root.addView(count);
  search=new EditText(this);search.setSingleLine(true);AuroraDesign.entry(search);search.setTextColor(Color.WHITE);search.setHintTextColor(0xffbed3e2);search.setHint("Buscar por fecha, tipo o nombre");root.addView(search);
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence a,int b,int c,int d){}
   public void onTextChanged(CharSequence a,int b,int before,int c){refresh();}
   public void afterTextChanged(android.text.Editable e){}
  });
  results=new LinearLayout(this);results.setOrientation(1);root.addView(results);refreshFolder();refresh();
 }
 private void refreshFolder(){
  Uri uri=destination.selectedFolder();
  folderStatus.setText(uri==null?
    "Guardado interno de AURORA. Elegí una carpeta externa para copiar todos los registros y las próximas escuchas automáticamente.":
    "Carpeta externa configurada. Los próximos historiales se copiarán al detener la escucha.");
 }
 private void refresh(){
  if(results==null)return;
  results.removeAllViews();shown.clear();
  String q=search.getText().toString().toLowerCase(Locale.ROOT).trim();
  for(File file:history.list()){
   String label=SenseHistory.readableName(file);
   if(!label.toLowerCase(Locale.ROOT).contains(q)&&!file.getName().toLowerCase(Locale.ROOT).contains(q))continue;
   shown.add(file);
   Button item=btn("📄  "+label+"\nTocar para abrir");
   results.addView(item);item.setOnClickListener(v->openFile(file));
  }
  count.setText(shown.size()+" archivo(s) encontrado(s)");
  if(shown.isEmpty())results.addView(txt("Todavía no hay registros que coincidan con la búsqueda.",15));
 }
 private void openFile(File file){
  try{
   ScrollView sc=new ScrollView(this);TextView display=txt(SenseHistory.read(file),16);display.setTextIsSelectable(true);display.setBackgroundColor(0xff101a2d);sc.addView(display);
   new AlertDialog.Builder(this).setTitle(SenseHistory.readableName(file)).setView(sc)
    .setNeutralButton("COPIAR A CARPETA",(d,w)->{
     if(destination.selectedFolder()==null){chooseFolder();return;}
     try{destination.copyOne(file);Toast.makeText(this,"Archivo copiado a tu carpeta",Toast.LENGTH_LONG).show();}
     catch(IOException e){showError(e.getMessage());}
    }).setNegativeButton("ELIMINAR REGISTRO",(d,w)->new AlertDialog.Builder(this)
     .setTitle("Eliminar este registro")
     .setMessage("¿Borrar este archivo del almacenamiento interno de AURORA? Si lo copiaste a otra carpeta, esa copia no se eliminará.")
     .setNegativeButton("CANCELAR",null)
     .setPositiveButton("ELIMINAR",(confirm,which)->{
      try{history.delete(file);refresh();Toast.makeText(this,"Registro local eliminado",Toast.LENGTH_SHORT).show();}
      catch(IOException error){showError(error.getMessage());}
     }).show()).setPositiveButton("CERRAR",null).show();
  }catch(IOException e){showError(e.getMessage());}
 }
 private void chooseFolder(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  Uri saved=destination.selectedFolder();
  if(saved!=null&&android.os.Build.VERSION.SDK_INT>=26)i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,saved);
  try{startActivityForResult(i,CHOOSE_FOLDER);}
  catch(Exception e){showError("No se pudo abrir el selector de carpetas.");}
 }
 private void browseFolder(){
  Uri selected=destination.selectedFolder();
  if(selected==null){chooseFolder();return;}
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");
  if(android.os.Build.VERSION.SDK_INT>=26)i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,selected);
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(i,BROWSE_FOLDER);}
  catch(Exception e){showError("No se pudo abrir el explorador de archivos.");}
 }
 private void showError(String message){new AlertDialog.Builder(this).setTitle("Carpeta de registros").setMessage(message).setPositiveButton("ENTENDIDO",null).show();}
 @Override protected void onActivityResult(int code,int result,Intent data){
  super.onActivityResult(code,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  if(code==CHOOSE_FOLDER){
   try{
    destination.select(data.getData(),data.getFlags());
    int copied=destination.copyAll(history.list());
    refreshFolder();Toast.makeText(this,copied+" archivos copiados. Las próximas escuchas se copiarán automáticamente.",Toast.LENGTH_LONG).show();
   }catch(Exception e){showError("No se pudo configurar o copiar a la carpeta: "+e.getMessage());}
  }else if(code==BROWSE_FOLDER){
   try{
    Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(data.getData(),"text/plain");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(i);
   }catch(Exception e){showError("No se pudo abrir el archivo elegido.");}
  }
 }
 @Override protected void onResume(){super.onResume();if(results!=null)refresh();}
}
