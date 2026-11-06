package com.despertadorflash;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * User-initiated visual accessibility. Camera is NEVER opened on its own;
 * photos aren't uploaded or kept by AURORA. Image labels are approximate,
 * OCR detects written text. No faces or identities are recognized.
 */
public final class AuroraAssistActivity extends Activity {
 private static final int PHOTO=380,CAMERA=381,CAMERA_PERMISSION=382;
 private TextView output,status;
 private boolean processing=false;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);
  t.setPadding(dp(13),dp(12),dp(13),dp(12));return t;
 }
 private Button button(String label){
  Button b=new Button(this);b.setText(label);
  AuroraDesign.action(b,0xff1c4260,0xff222d56,0xff5a9cb1);return b;
 }
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  ScrollView scroll=new ScrollView(this);
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
  layout.setBackground(new AuroraSkyDrawable());layout.setPadding(dp(16),dp(14),dp(16),dp(24));
  scroll.addView(layout);setContentView(scroll);
  Button back=button("← VOLVER A AURORA");layout.addView(back);back.setOnClickListener(v->finish());
  TextView title=text("AURORA · MIRAR CUANDO YO QUIERA",23);title.setTypeface(null,Typeface.BOLD);
  title.setGravity(Gravity.CENTER);layout.addView(title);
  layout.addView(text("Podés elegir una foto o abrir la cámara voluntariamente. AURORA busca palabras y objetos aproximados usando modelos locales. No graba video ni observa de manera continua.",15));
  Button choose=button("ELEGIR UNA FOTO");layout.addView(choose);
  choose.setOnClickListener(v->{
   if(processing)return;
   Intent pick=new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
   pick.addCategory(Intent.CATEGORY_OPENABLE);
   try{startActivityForResult(Intent.createChooser(pick,"Elegir imagen para analizar"),PHOTO);}
   catch(Exception ex){status.setText("No se encontró un selector de imágenes.");}
  });
  Button camera=button("ABRIR CÁMARA Y TOMAR UNA FOTO");layout.addView(camera);
  camera.setOnClickListener(v->{
   if(processing)return;
   if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
    requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);return;
   }
   launchCamera();
  });
  status=text("No se está utilizando la cámara.",14);layout.addView(status);
  output=text("El resultado aparecerá acá. La IA puede equivocarse; no utilices este análisis para tomar decisiones de seguridad o emergencia.",21);
  output.setTextIsSelectable(true);output.setMinHeight(dp(180));
  output.setBackground(AuroraDesign.surface(this,0xff0e2f42,0xff242641,0xff4c8399,20));
  layout.addView(output);
  Button soundSummary=button("RESUMIR MIS DETECCIONES DE SONIDOS");layout.addView(soundSummary);
  soundSummary.setOnClickListener(v->{
   StringBuilder observations=new StringBuilder();
   int sessions=0;
   for(java.io.File file:new SenseHistory(this).list()){
    if(!file.getName().contains("SONIDOS"))continue;
    try{
     observations.append(SenseHistory.read(file)).append("\n");
     if(++sessions>=3)break;
    }catch(java.io.IOException ignored){}
   }
   output.setText(SoundDigest.summarize(observations.toString()));
   status.setText("Resumen local de hasta tres registros de sonidos; sin enviar información a internet.");
  });
  Button copy=button("COPIAR RESULTADO");layout.addView(copy);
  copy.setOnClickListener(v->{
   android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(cm!=null)cm.setPrimaryClip(android.content.ClipData.newPlainText("AURORA · Imagen",output.getText()));
  });
  layout.addView(text("PRIVACIDAD: la foto se procesa en el dispositivo y no se guarda en el historial de AURORA. Si usás la cámara, solo analizamos la foto que tomaste. Esto no identifica personas ni garantiza la detección de peligros.",13));
 }
 private void launchCamera(){
  Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
  try{startActivityForResult(intent,CAMERA);}
  catch(Exception error){status.setText("No se encontró una aplicación de cámara disponible.");}
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(request,permissions,grants);
  if(request==CAMERA_PERMISSION){
   if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)launchCamera();
   else status.setText("Sin permiso de cámara. Podés analizar una foto de la galería.");
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null)return;
  try{
   Bitmap bitmap=null;
   if(request==PHOTO&&data.getData()!=null){
    Uri uri=data.getData();
    BitmapFactory.Options options=new BitmapFactory.Options();
    options.inJustDecodeBounds=true;
    try(InputStream stream=getContentResolver().openInputStream(uri)){if(stream!=null)BitmapFactory.decodeStream(stream,null,options);}
    if(options.outWidth<=0||options.outHeight<=0){status.setText("No se pudo leer esta imagen.");return;}
    options.inJustDecodeBounds=false;
    int max=Math.max(options.outWidth,options.outHeight);options.inSampleSize=1;
    while(max/options.inSampleSize>1600)options.inSampleSize*=2;
    try(InputStream stream=getContentResolver().openInputStream(uri)){
     if(stream!=null)bitmap=BitmapFactory.decodeStream(stream,null,options);
    }
   }else if(request==CAMERA){
    Object thumbnail=data.getExtras()==null?null:data.getExtras().get("data");
    if(thumbnail instanceof Bitmap)bitmap=(Bitmap)thumbnail;
   }
   if(bitmap==null){status.setText("La foto no está disponible. Probá con otra.");return;}
   analyze(bitmap);
  }catch(Exception error){status.setText("No se pudo procesar la foto seleccionada.");}
 }
 private void analyze(Bitmap bitmap){
  if(processing)return;processing=true;
  output.setText("Analizando objetos y texto de la imagen…");
  status.setText("Procesamiento local activo. No se está grabando video.");
  InputImage input=InputImage.fromBitmap(bitmap,0);
  final ImageLabeler labels=ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(.55f).build());
  final TextRecognizer ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  final String[] parts={"",""};final int[] complete={0};
  Runnable finish=()->{
   if(++complete[0]!=2)return;
   processing=false;
   if(isFinishing()||isDestroyed()){labels.close();ocr.close();return;}
   String result="LECTURA DE LA FOTO\n\n"+
    (parts[0].isEmpty()?"No pude reconocer objetos con suficiente confianza.\n":parts[0])+
    "\nTEXTO VISIBLE\n"+(parts[1].isEmpty()?"No se reconoció texto legible.":parts[1])+
    "\n\nLos resultados son aproximados y podrían omitir elementos.";
   output.setText(result);
   status.setText("Análisis finalizado. La cámara no está activa en AURORA.");
   labels.close();ocr.close();
  };
  labels.process(input).addOnSuccessListener(found->{
   StringBuilder b=new StringBuilder("OBJETOS POSIBLES (etiquetas automáticas):\n");
   for(ImageLabel label:found){
    if(b.length()>800)break;
    b.append("• ").append(label.getText()).append(" (").append(Math.round(label.getConfidence()*100)).append("%)\n");
   }
   if(!found.isEmpty())parts[0]=b.toString();
   finish.run();
  }).addOnFailureListener(e->{parts[0]="Clasificación de objetos no disponible en este teléfono.\n";finish.run();});
  ocr.process(input).addOnSuccessListener(text->{
   String read=text.getText().trim();
   parts[1]=read.length()>5000?read.substring(0,5000)+"…":read;
   finish.run();
  }).addOnFailureListener(e->{parts[1]="Lectura de texto no disponible en este teléfono.";finish.run();});
 }
}
