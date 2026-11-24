package com.despertadorflash;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Two-way counter conversation on a single phone. No server, no saved messages.
 * Staff text is rotated 180 degrees (opposite side of the counter).
 * Speech recognition is user-triggered, never background recording.
 */
public final class AuroraCounterActivity extends Activity implements TextToSpeech.OnInitListener {
 private SpeechRecognizer recognizer;
 private TextToSpeech voice;
 private boolean voiceReady=false,listening=false;
 private TextView heard,status;
 private EditText typed;
 private Button listen;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final Runnable timeout=()->{
  if(listening){stopListening();status.setText("Escucha finalizada por tiempo. Volvé a tocar ESCUCHAR.");}
 };
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int size){
  TextView t=new TextView(this);t.setText(s);t.setTextSize(size);
  t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);
  t.setPadding(dp(10),dp(5),dp(10),dp(5));return t;
 }
 private Button button(String s){
  Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);
  b.setTextSize(14);b.setMinHeight(dp(48));
  b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff412e50));
  return b;
 }
 private LinearLayout vertical(){
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);return c;
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  LinearLayout screen=vertical();screen.setBackgroundColor(Color.BLACK);
  screen.setPadding(dp(9),dp(6),dp(9),dp(8));setContentView(screen);

  // Upper half faces the hearing staff member across the counter.
  LinearLayout staff=vertical();staff.setGravity(Gravity.CENTER);
  staff.setBackgroundColor(0xff12121b);
  staff.setRotation(180f);
  screen.addView(staff,new LinearLayout.LayoutParams(-1,0,1f));
  staff.addView(label("PERSONA DE ENFRENTE",14));
  heard=label("Tocá ESCUCHAR y hablá hacia el teléfono.",24);
  heard.setTextIsSelectable(true);
  staff.addView(heard,new LinearLayout.LayoutParams(-1,0,1f));
  listen=button("ESCUCHAR · MICRÓFONO APAGADO");
  staff.addView(listen,new LinearLayout.LayoutParams(-1,dp(53)));
  listen.setOnClickListener(v->{
   if(listening)stopListening();else beginListening();
  });

  // Lower half faces the AURORA user; typed answers remain editable.
  LinearLayout user=vertical();
  screen.addView(user,new LinearLayout.LayoutParams(-1,0,1f));
  status=label("MODO MOSTRADOR · LISTO",13);status.setTextColor(0xffd9b4ef);
  user.addView(status,new LinearLayout.LayoutParams(-1,dp(42)));
  typed=new EditText(this);typed.setTextColor(Color.WHITE);typed.setHintTextColor(0xffaaaaaa);
  typed.setTextSize(21);typed.setHint("Escribí tu respuesta aquí");
  typed.setGravity(Gravity.TOP);typed.setMinLines(2);
  user.addView(typed,new LinearLayout.LayoutParams(-1,0,1f));
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
  user.addView(actions,new LinearLayout.LayoutParams(-1,dp(54)));
  Button show=button("MOSTRAR TEXTO");Button say=button("DECIR EN VOZ ALTA");
  actions.addView(show,new LinearLayout.LayoutParams(0,-1,1f));
  actions.addView(say,new LinearLayout.LayoutParams(0,-1,1f));
  say.setEnabled(false);
  show.setOnClickListener(v->{
   String message=typed.getText().toString().trim();
   if(message.isEmpty()){status.setText("Primero escribí un mensaje.");return;}
   heard.setText(message);status.setText("Mensaje mostrado a la otra persona.");
  });
  say.setOnClickListener(v->{
   String message=typed.getText().toString().trim();
   if(message.isEmpty()){status.setText("Primero escribí un mensaje.");return;}
   if(!voiceReady){status.setText("La voz Android no está disponible.");return;}
   voice.speak(message,TextToSpeech.QUEUE_FLUSH,null,"aurora_counter_speak");
   heard.setText(message);
   status.setText("Leyendo la respuesta en voz alta.");
  });
  Button close=button("← VOLVER A COMUNICAR");
  user.addView(close,new LinearLayout.LayoutParams(-1,dp(48)));
  close.setOnClickListener(v->finish());
  voice=new TextToSpeech(getApplicationContext(),this);
  say.setEnabled(true); // reports readiness when user presses the button
 }
 @Override public void onInit(int result){
  if(voice==null)return;
  if(result==TextToSpeech.SUCCESS){
   int lang=voice.setLanguage(new Locale("es","AR"));
   voiceReady=lang!=TextToSpeech.LANG_NOT_SUPPORTED&&lang!=TextToSpeech.LANG_MISSING_DATA;
  }else voiceReady=false;
 }
 private void beginListening(){
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},741);
   status.setText("Autorizá el micrófono y volvé a tocar ESCUCHAR.");
   return;
  }
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   status.setText("Este teléfono no tiene un servicio de reconocimiento de voz disponible.");return;
  }
  try{
   stopListening();
   recognizer=SpeechRecognizer.createSpeechRecognizer(this);
   recognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle p){status.setText("Escuchando…");}
    public void onBeginningOfSpeech(){}
    public void onRmsChanged(float rms){}
    public void onBufferReceived(byte[] data){}
    public void onEndOfSpeech(){status.setText("Procesando frase…");}
    public void onError(int error){
     stopListening();status.setText("No se reconoció la frase (código "+error+"). Probá de nuevo.");
    }
    public void onResults(Bundle result){receiveText(result);stopListening();status.setText("Frase recibida. Tocá ESCUCHAR para la siguiente.");}
    public void onPartialResults(Bundle result){receiveText(result);}
    public void onEvent(int type,Bundle params){}
   });
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-AR");
   intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   listening=true;listen.setText("DETENER ESCUCHA");
   status.setText("Iniciando micrófono…");
   recognizer.startListening(intent);
   main.postDelayed(timeout,30000L);
  }catch(Exception error){stopListening();status.setText("No se pudo iniciar el micrófono: "+error.getClass().getSimpleName());}
 }
 private void receiveText(Bundle result){
  ArrayList<String> words=result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
  if(words!=null&&!words.isEmpty()){
   String recognized=words.get(0).trim();
   if(!recognized.isEmpty())heard.setText(recognized);
  }
 }
 private void stopListening(){
  main.removeCallbacks(timeout);
  listening=false;
  if(listen!=null)listen.setText("ESCUCHAR · MICRÓFONO APAGADO");
  if(recognizer!=null){
   SpeechRecognizer old=recognizer;recognizer=null;
   try{old.cancel();old.destroy();}catch(RuntimeException ignored){}
  }
 }
 @Override public void onBackPressed(){stopListening();super.onBackPressed();}
 @Override protected void onDestroy(){
  stopListening();
  main.removeCallbacksAndMessages(null);
  if(voice!=null){voice.stop();voice.shutdown();voice=null;}
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  super.onDestroy();
 }
}
