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
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
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
 private File pendingPhoto;
 private Uri pendingPhotoUri;
 private final ExecutorService imageWorker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private volatile boolean destroyed=false;
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
  if(state!=null&&state.containsKey("temporary_photo")){
   pendingPhoto=new File(state.getString("temporary_photo"));
   pendingPhotoUri=FileProvider.getUriForFile(this,getPackageName()+".photos",pendingPhoto);
  }
  deleteAbandonedPhotos();
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
  Button soundSummary=button("CONSULTAR MIS ÚLTIMAS DETECCIONES");layout.addView(soundSummary);
  soundSummary.setOnClickListener(v->reviewHistory(true));
  Button speechRecall=button("¿QUÉ DIJERON? · ÚLTIMA CONVERSACIÓN");layout.addView(speechRecall);
  speechRecall.setOnClickListener(v->reviewHistory(false));
  Button copy=button("COPIAR RESULTADO");layout.addView(copy);
  copy.setOnClickListener(v->{
   android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(cm!=null)cm.setPrimaryClip(android.content.ClipData.newPlainText("AURORA · Imagen",output.getText()));
  });
  layout.addView(text("PRIVACIDAD: la foto se procesa en el dispositivo y no se guarda en el historial de AURORA. Si usás la cámara, solo analizamos la foto que tomaste. Esto no identifica personas ni garantiza la detección de peligros.",13));
 }
 private void reviewHistory(boolean sounds){
  if(processing){status.setText("Finalizá la operación actual antes de consultar la lista.");return;}
  if(sounds){
   // Sound events no longer live in TXT files. Read the rolling in-app list.
   output.setText(SoundDigest.summarizeRecent(RecentSoundEvents.snapshot(this)));
   status.setText("Últimos 30 sonidos confirmados en AURORA. No se crea un archivo.");
   return;
  }
  processing=true;
  status.setText("Leyendo las conversaciones guardadas en este teléfono…");
  imageWorker.execute(()->{
   StringBuilder observations=new StringBuilder();
   boolean readError=false;
   File[] files=ConversationRecordFilter.conversations(new SenseHistory(this).list());
   for(File file:files){
    try{
     observations.append(SenseHistory.read(file)).append("\n");
     break; // Only the latest saved conversation.
    }catch(IOException ignored){readError=true;}
   }
   final String review=ConversationRecall.lastPhrases(observations.toString());
   final boolean error=readError;
   main.post(()->{
    if(destroyed)return;
    processing=false;
    output.setText(review);
    status.setText(error?"Alguna conversación no se pudo leer.":"Frases transcritas por Android; pueden tener errores.");
   });
  });
 }
 private void launchCamera(){
  try{
   File directory=new File(getCacheDir(),"aurora_camera");
   if(!directory.exists()&&!directory.mkdirs())throw new IOException("No se pudo preparar la cámara");
   pendingPhoto=File.createTempFile("aurora_photo_",".jpg",directory);
   pendingPhotoUri=FileProvider.getUriForFile(this,getPackageName()+".photos",pendingPhoto);
   Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
   intent.putExtra(MediaStore.EXTRA_OUTPUT,pendingPhotoUri);
   intent.setClipData(android.content.ClipData.newUri(getContentResolver(),"Foto AURORA",pendingPhotoUri));
   intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
   startActivityForResult(intent,CAMERA);
  }catch(Exception error){
   clearPhoto();status.setText("No se pudo abrir la cámara: revisá los permisos del teléfono.");
  }
 }
 private void deleteAbandonedPhotos(){
  File directory=new File(getCacheDir(),"aurora_camera");
  File[] photos=directory.listFiles();
  if(photos==null)return;
  for(File file:photos){
   if(!file.isFile()||!file.getName().startsWith("aurora_photo_")||!file.getName().endsWith(".jpg"))continue;
   if(pendingPhoto!=null&&file.equals(pendingPhoto))continue;
   if(!file.delete())android.util.Log.w("Aurora","No se pudo borrar foto temporal anterior");
  }
 }
 private void clearPhoto(){
  if(pendingPhoto!=null&&pendingPhoto.exists()&&!pendingPhoto.delete()){
   android.util.Log.w("Aurora","No se pudo limpiar foto temporal");
  }
  pendingPhoto=null;pendingPhotoUri=null;
 }
 private void decodeImage(Uri uri,boolean cameraFile){
  if(uri==null){status.setText("No se recibió una imagen.");clearPhoto();return;}
  processing=true;
  output.setText("Preparando la foto para analizar…");
  imageWorker.execute(()->{
   Bitmap bitmap=null;String failure=null;
   int imageRotation=0;
   try{
    BitmapFactory.Options bounds=new BitmapFactory.Options();
    bounds.inJustDecodeBounds=true;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     if(in==null)throw new IOException("Imagen inaccesible");
     BitmapFactory.decodeStream(in,null,bounds);
    }
    if(!ImageDecodePolicy.dimensionsAllowed(bounds.outWidth,bounds.outHeight))
     throw new IOException("Dimensiones de imagen no admitidas");
    BitmapFactory.Options options=new BitmapFactory.Options();
    options.inSampleSize=ImageDecodePolicy.sampleSize(bounds.outWidth,bounds.outHeight,1600);
    options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     if(in==null)throw new IOException("No se pudo abrir la foto");
     bitmap=BitmapFactory.decodeStream(in,null,options);
    }
    if(bitmap==null)throw new IOException("No se pudo decodificar la imagen");
    // The JPEG pixels can be landscape even when the phone was held upright.
    // Pass EXIF rotation to ML Kit instead of allocating another rotated bitmap.
    try(InputStream metadata=getContentResolver().openInputStream(uri)){
     if(metadata!=null){
      ExifInterface exif=new ExifInterface(metadata);
      imageRotation=ImageOrientation.degrees(
       exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL));
     }
    }catch(Exception ignored){imageRotation=0;}
   }catch(Exception err){failure="No pude leer esta foto. Probá con otra imagen."; }
   catch(OutOfMemoryError oom){failure="La foto es demasiado grande para este teléfono."; }
   final Bitmap ready=bitmap;
   final int rotation=imageRotation;
   final String error=failure;
   main.post(()->{
    if(cameraFile)clearPhoto();
    if(destroyed){
     if(ready!=null&&!ready.isRecycled())ready.recycle();
     return;
    }
    if(error!=null){processing=false;status.setText(error);return;}
    analyze(ready,rotation);
   });
  });
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
  if(request!=PHOTO&&request!=CAMERA)return;
  if(result!=RESULT_OK){if(request==CAMERA)clearPhoto();status.setText("Selección de imagen cancelada.");return;}
  if(processing)return;
  if(request==CAMERA){
   if(pendingPhoto==null||!pendingPhoto.isFile()||pendingPhoto.length()==0){
    clearPhoto();status.setText("La cámara no devolvió una foto completa.");return;
   }
   decodeImage(pendingPhotoUri,true);
  }else if(data!=null&&data.getData()!=null){
   decodeImage(data.getData(),false);
  }else status.setText("No se recibió una foto de la galería.");
 }
 @Override protected void onSaveInstanceState(Bundle out){
  super.onSaveInstanceState(out);
  if(pendingPhoto!=null)out.putString("temporary_photo",pendingPhoto.getAbsolutePath());
 }
 private void analyze(Bitmap bitmap,int rotation){
  processing=true;
  output.setText("Analizando objetos y texto de la imagen…");
  status.setText("Procesamiento local activo. No se está grabando video.");
  InputImage input=InputImage.fromBitmap(bitmap,rotation);
  final ImageLabeler labels=ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(.55f).build());
  final TextRecognizer ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  final String[] parts={"",""};final int[] complete={0};
  Runnable finish=()->{
   if(++complete[0]!=2)return;
   processing=false;
   if(isFinishing()||isDestroyed()){labels.close();ocr.close();if(!bitmap.isRecycled())bitmap.recycle();return;}
   String result="LECTURA DE LA FOTO\n\n"+
    (parts[0].isEmpty()?"No pude reconocer objetos con suficiente confianza.\n":parts[0])+
    "\nTEXTO VISIBLE\n"+(parts[1].isEmpty()?"No se reconoció texto legible.":parts[1])+
    "\n\nLos resultados son aproximados y podrían omitir elementos.";
   output.setText(result);
   status.setText("Análisis finalizado. La cámara no está activa en AURORA.");
   labels.close();ocr.close();
   if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();
  };
  labels.process(input).addOnSuccessListener(found->{
   StringBuilder b=new StringBuilder("OBJETOS POSIBLES (etiquetas automáticas):\n");
   for(ImageLabel label:found){
    if(b.length()>800)break;
    b.append("• ").append(ImageLabelEs.name(label.getText())).append(" (").append(Math.round(label.getConfidence()*100)).append("%)\n");
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
 @Override protected void onDestroy(){
  destroyed=true;
  imageWorker.shutdownNow();
  main.removeCallbacksAndMessages(null);
  if(isFinishing())clearPhoto();
  super.onDestroy();
 }
}
