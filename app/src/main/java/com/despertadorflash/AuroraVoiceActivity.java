package com.despertadorflash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.Locale;
import java.util.UUID;

/** On-device TTS to app-private file; explicit preview and SEND with contact selection. */
public final class AuroraVoiceActivity extends Activity implements TextToSpeech.OnInitListener {
 private TextToSpeech tts;
 private boolean ready=false,working=false;
 private final Handler main=new Handler(Looper.getMainLooper());
 private EditText input;
 private TextView state;
 private Button create,preview,share,whatsapp;
 private File audio;
 private MediaPlayer player;
 private String currentUtterance="";
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(dp(4),dp(10),dp(4),dp(10));return t;
 }
 private Button button(String value){
  Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);
  AuroraDesign.neonAction(b);
  b.setMinHeight(dp(58));return b;
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.TRANSPARENT);
  LinearLayout panel=new LinearLayout(this);panel.setPadding(dp(16),dp(16),dp(16),dp(30));
  panel.setOrientation(LinearLayout.VERTICAL);scroll.addView(panel);setContentView(AuroraDesign.menuFrame(this,scroll));
  Button back=button("← VOLVER A COMUNICAR");panel.addView(back);back.setOnClickListener(v->finish());
  TextView heading=text("AURORA VOZ",29);heading.setGravity(Gravity.CENTER);
  panel.addView(heading);
  panel.addView(text("Escribí un mensaje. AURORA lo convierte en un archivo de voz que podés escuchar y adjuntar a un chat.",15));
  input=new EditText(this);input.setTextColor(Color.WHITE);input.setHintTextColor(0xffbbbbbb);
  input.setHint("Escribí tu mensaje");input.setTextSize(20);
  input.setMinLines(3);input.setMaxLines(7);input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffbd77d9));
  panel.addView(input,new LinearLayout.LayoutParams(-1,-2));
  input.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int st,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){clearOldAudio();}
   public void afterTextChanged(android.text.Editable e){}
  });
  panel.addView(text("FRASES RÁPIDAS",17));
  String[] labels={"Soy una persona sorda. Respondeme por escrito.","Estoy llegando en diez minutos.","Necesito ayuda.","Gracias, ¿podés repetirlo por escrito?"};
  for(String phrase:labels){
   Button sample=button(phrase);panel.addView(sample);
   sample.setOnClickListener(v->input.setText(phrase));
  }
  state=text("Preparando voz del teléfono…",15);state.setTextColor(0xffd6b4f0);panel.addView(state);
  create=button("1 · GENERAR AUDIO");panel.addView(create);
  preview=button("2 · ESCUCHAR AUDIO");panel.addView(preview);
  whatsapp=button("3 · ABRIR WHATSAPP CON AUDIO");panel.addView(whatsapp);
  share=button("COMPARTIR CON OTRA APP");panel.addView(share);
  create.setOnClickListener(v->createAudio());
  preview.setOnClickListener(v->playAudio());
  whatsapp.setOnClickListener(v->shareAudio("com.whatsapp"));
  share.setOnClickListener(v->shareAudio(null));
  panel.addView(text("El mensaje no se envía automáticamente. Elegís el contacto y confirmás desde WhatsApp o la app de destino. Algunas aplicaciones pueden mostrar el audio como archivo adjunto.",13));
  updateButtons();
  tts=new TextToSpeech(getApplicationContext(),this);
 }
 @Override public void onInit(int status){
  main.post(()->{
   if(isFinishing()||isDestroyed())return;
   if(status!=TextToSpeech.SUCCESS){state.setText("No se pudo iniciar la voz de Android. Revisá los motores de texto a voz del teléfono.");return;}
   int availability=tts.setLanguage(new Locale("es","AR"));
   ready=availability!=TextToSpeech.LANG_MISSING_DATA&&availability!=TextToSpeech.LANG_NOT_SUPPORTED;
   if(!ready){state.setText("No hay voz española instalada. Configurá los datos del motor de voz en Android.");return;}
   tts.setSpeechRate(.95f);
   state.setText("Voz lista. Escribí y generá el audio.");
   updateButtons();
  });
 }
 private void updateButtons(){
  if(create==null)return;
  create.setEnabled(ready&&!working);
  boolean exists=!working&&audio!=null&&audio.exists()&&audio.length()>0;
  preview.setEnabled(exists);whatsapp.setEnabled(exists);share.setEnabled(exists);
 }
 private void stopPlayer(){
  if(player!=null){try{player.stop();}catch(IllegalStateException ignored){}
   player.release();player=null;}
 }
 private void clearOldAudio(){
  if(tts!=null&&working)tts.stop();
  working=false;currentUtterance="";
  stopPlayer();
  if(audio!=null){audio.delete();audio=null;}
  if(state!=null&&ready)state.setText("Texto actualizado. Generá nuevamente el audio.");
  updateButtons();
 }
 private void createAudio(){
  if(!ready||working)return;
  String message=input.getText().toString().trim();
  if(message.isEmpty()){Toast.makeText(this,"Escribí un mensaje primero",Toast.LENGTH_SHORT).show();return;}
  if(message.length()>TextToSpeech.getMaxSpeechInputLength()){
   state.setText("Mensaje demasiado largo. Usá menos de "+TextToSpeech.getMaxSpeechInputLength()+" caracteres.");return;
  }
  clearOldAudio();
  File dir=new File(getCacheDir(),"aurora_voice");
  if(!dir.exists()&&!dir.mkdirs()){state.setText("No se pudo preparar el archivo de audio.");return;}
  audio=new File(dir,"aurora_voz_"+UUID.randomUUID().toString()+".wav");
  working=true;
  currentUtterance=UUID.randomUUID().toString();
  final String id=currentUtterance;
  state.setText("Generando audio en el teléfono…");
  updateButtons();
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String utteranceId){}
   @Override public void onDone(String utteranceId){
    main.post(()->{
     if(!id.equals(currentUtterance)||isFinishing()||isDestroyed())return;
     working=false;
     if(audio!=null&&audio.isFile()&&audio.length()>44){
      state.setText("Audio listo. Escuchalo antes de compartirlo.");
     }else{
      state.setText("La voz no produjo un archivo válido.");
      if(audio!=null){audio.delete();audio=null;}
     }
     updateButtons();
    });
   }
   @Override public void onError(String utteranceId){
    main.post(()->{if(!id.equals(currentUtterance)||isFinishing()||isDestroyed())return;
     working=false;state.setText("Error al crear el audio. Probá otra vez.");updateButtons();});
   }
  });
  Bundle params=new Bundle();
  int result=tts.synthesizeToFile(message,params,audio,id);
  if(result==TextToSpeech.ERROR){working=false;state.setText("El motor TTS rechazó la solicitud.");updateButtons();}
 }
 private void playAudio(){
  if(audio==null||!audio.isFile())return;
  try{
   stopPlayer();
   player=new MediaPlayer();
   player.setDataSource(audio.getAbsolutePath());
   player.setOnCompletionListener(mp->{stopPlayer();});
   player.prepare();player.start();
   state.setText("Reproduciendo vista previa…");
  }catch(Exception ex){stopPlayer();state.setText("No se pudo reproducir el archivo. "+ex.getClass().getSimpleName());}
 }
 private void shareAudio(String packageName){
  if(audio==null||!audio.isFile()||audio.length()==0){state.setText("Primero generá un audio.");return;}
  try{
   Uri uri=FileProvider.getUriForFile(this,getPackageName()+".photos",audio);
   Intent send=new Intent(Intent.ACTION_SEND);
   send.setType("audio/wav");
   send.putExtra(Intent.EXTRA_STREAM,uri);
   send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   if(packageName!=null){
    try{
     send.setPackage(packageName);
     startActivity(send);
     state.setText("WhatsApp abierto: elegí el chat y confirmá el envío.");
     return;
    }catch(ActivityNotFoundException absent){
     send.setPackage(null);
     new AlertDialog.Builder(this)
      .setTitle("WhatsApp no está disponible")
      .setMessage("Podés elegir otra aplicación compatible para compartir el audio.")
      .setPositiveButton("COMPARTIR",(d,w)->startActivity(Intent.createChooser(send,"Compartir voz AURORA")))
      .setNegativeButton("CERRAR",null).show();
     return;
    }
   }
   startActivity(Intent.createChooser(send,"Compartir voz AURORA"));
   state.setText("Seleccioná la aplicación, el chat y confirmá.");
  }catch(Exception ex){state.setText("No se pudo abrir el envío de audio: "+ex.getClass().getSimpleName());}
 }
 @Override protected void onDestroy(){
  stopPlayer();
  if(tts!=null){tts.stop();tts.shutdown();tts=null;}
  super.onDestroy();
 }
}
