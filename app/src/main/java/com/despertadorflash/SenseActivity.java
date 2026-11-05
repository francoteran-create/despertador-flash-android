package com.despertadorflash;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioRecord;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier;
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier.AudioClassifierOptions;
import com.google.mediapipe.tasks.audio.core.RunningMode;
import com.google.mediapipe.tasks.components.containers.AudioData;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.core.BaseOptions;
import java.util.ArrayList;
import java.util.Locale;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Real microphone analysis. No fake observations and no background capture. */
public final class SenseActivity extends Activity {
 private static final int MIC_REQUEST=33;
 public static final String EXTRA_MODE="aurora_sense_mode";
 public static final int MODE_NONE=0, MODE_SPEECH=1, MODE_SOUND=2;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService work=Executors.newSingleThreadExecutor();
 private SpeechRecognizer recognizer;
 private volatile boolean soundRunning=false;
 private AudioRecord audioRecord;
 private AudioClassifier classifier;
 private int mode=MODE_NONE, requestedMode=MODE_NONE;
 private LinearLayout root;
 private TextView state, result, detail;
 private Button speechButton, soundsButton, translateButton;
 private Spinner languagePicker;
 private Translator translator;
 private String lastTranscript="";
 private boolean speechLaunching=false;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private GradientDrawable bg(int a,int b){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(20));d.setStroke(dp(1),0xff427997);return d;}
 private TextView text(String content,int size,int color){TextView t=new TextView(this);t.setText(content);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;}
 private Button button(String content){Button b=new Button(this);b.setText(content);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(17);b.setMinHeight(dp(62));b.setBackground(bg(0xff334477,0xff25194c));return b;}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);draw();int requested=getIntent().getIntExtra(EXTRA_MODE,MODE_NONE);if(requested==MODE_SPEECH||requested==MODE_SOUND)main.post(()->toggle(requested));}
 private void draw(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(25));root.setBackground(new AuroraSkyDrawable());scroll.addView(root);setContentView(scroll);
  Button back=button("← VOLVER");root.addView(back);back.setOnClickListener(v->finish());
  TextView heading=text("AURORA SENSE",30,0xffa5f6ff);heading.setGravity(Gravity.CENTER);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);
  TextView intro=text("Tu entorno, convertido en información visual. Activá una función cuando la necesites.",16,0xffe0edff);intro.setGravity(Gravity.CENTER);root.addView(intro);
  state=text("MICRÓFONO APAGADO",17,0xff8fe8df);state.setGravity(Gravity.CENTER);state.setBackground(bg(0xff0f293c,0xff14204b));root.addView(state);
  String initial=getSharedPreferences("aurora",0).getString("sense_last_transcript","");
  lastTranscript=initial;
  result=text(initial.isEmpty()?"Sin resultados todavía":initial,29,Color.WHITE);result.setGravity(Gravity.CENTER);result.setTypeface(null,Typeface.BOLD);result.setMinHeight(dp(160));result.setBackground(bg(0xff142c44,0xff19193b));root.addView(result);
  detail=text("El audio se analiza solamente mientras mantenés activa una función.",14,0xffced9ef);root.addView(detail);
  TextView speechHeading=text("IDIOMA DE LA CONVERSACIÓN",15,0xffa6ecf9);root.addView(speechHeading);
  languagePicker=new Spinner(this);String[] languages={"Español","Inglés","Portugués"};
  ArrayAdapter<String> langAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,languages);languagePicker.setAdapter(langAdapter);root.addView(languagePicker);
  speechButton=button("🎙  TRANSCRIBIR CONVERSACIÓN");root.addView(speechButton);speechButton.setOnClickListener(v->toggle(MODE_SPEECH));
  soundsButton=button("◉  RECONOCER SONIDOS");root.addView(soundsButton);soundsButton.setOnClickListener(v->toggle(MODE_SOUND));
  translateButton=button("🌐  TRADUCIR ESTE TEXTO");root.addView(translateButton);translateButton.setOnClickListener(v->translateLast());
  Button stop=button("DETENER ESCUCHA");root.addView(stop);stop.setOnClickListener(v->stopAll(true));
  updateModeVisibility();
  TextView safety=text("Los resultados son aproximados. Las detecciones no sustituyen alarmas de incendios ni equipos de emergencia. No uses SENSE como único medio para detectar peligros.",13,0xffb4c9dc);root.addView(safety);
 }
 private void toggle(int next){
  if(mode==next){stopAll(true);return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestedMode=next;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;
  }
  stopAll(false);mode=next;updateModeVisibility();
  if(next==MODE_SPEECH)startSpeech();else startSounds();
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(code,permissions,grants);
  if(code==MIC_REQUEST){int next=requestedMode;requestedMode=MODE_NONE;
   if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED&&next!=MODE_NONE)toggle(next);
   else {state.setText("PERMISO DE MICRÓFONO DENEGADO");detail.setText("Autorizá el micrófono desde los ajustes de Android para usar AURORA SENSE.");}
  }
 }
 private void updateModeVisibility(){
  boolean isSpeech=mode!=MODE_SOUND;
  if(languagePicker!=null)languagePicker.setVisibility(isSpeech?View.VISIBLE:View.GONE);
  if(translateButton!=null)translateButton.setVisibility(isSpeech?View.VISIBLE:View.GONE);
 }
 private String recognizedLanguage(){
  int selected=languagePicker.getSelectedItemPosition();return selected==1?"en-US":selected==2?"pt-BR":"es-AR";
 }
 private void saveTranscript(String phrase){
  lastTranscript=phrase;getSharedPreferences("aurora",0).edit().putString("sense_last_transcript",phrase).apply();
 }
 private void translateLast(){
  String phrase=lastTranscript.trim();if(phrase.isEmpty()){detail.setText("Primero transcribí una conversación para poder traducirla.");return;}
  if(translator!=null){translator.close();translator=null;}
  String input=recognizedLanguage();String src=input.startsWith("en")?TranslateLanguage.ENGLISH:input.startsWith("pt")?TranslateLanguage.PORTUGUESE:TranslateLanguage.SPANISH;
  String target=src.equals(TranslateLanguage.SPANISH)?TranslateLanguage.ENGLISH:TranslateLanguage.SPANISH;
  TranslatorOptions options=new TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(target).build();
  translator=Translation.getClient(options);final Translator client=translator;
  detail.setText("Preparando traducción… El modelo puede descargarse la primera vez.");
  client.downloadModelIfNeeded().addOnSuccessListener(unused->client.translate(phrase)
   .addOnSuccessListener(translated->{result.setText(translated);detail.setText("Texto traducido "+src+" → "+target+". Original: "+phrase);})
   .addOnFailureListener(e->detail.setText("No se pudo traducir: "+e.getMessage())))
   .addOnFailureListener(e->detail.setText("No se pudo descargar el modelo de traducción: "+e.getMessage()));
 }
 private void startSpeech(){
  state.setText("ESCUCHANDO VOZ · ESPAÑOL");
  result.setText(lastTranscript.isEmpty()?"Hablá cerca del teléfono…":lastTranscript);
  detail.setText("Reconocimiento de voz de Android. Puede usar internet según el servicio instalado.");
  if(!SpeechRecognizer.isRecognitionAvailable(this)){state.setText("SIN RECONOCIMIENTO DE VOZ");result.setText("Este teléfono no tiene un servicio de transcripción disponible.");mode=MODE_NONE;return;}
  try{
   if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
   else recognizer=SpeechRecognizer.createSpeechRecognizer(this);
   recognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle params){state.setText("ESCUCHANDO · HABLÁ AHORA");}
    public void onBeginningOfSpeech(){state.setText("DETECTANDO VOZ");}
    public void onRmsChanged(float rms){}
    public void onBufferReceived(byte[] buffer){}
    public void onEndOfSpeech(){state.setText("PROCESANDO PALABRAS…");}
    public void onError(int error){if(mode==MODE_SPEECH){state.setText("TRANSCRIPCIÓN DETENIDA");detail.setText("Android informó un problema de reconocimiento (código "+error+"). Tocá Transcribir para reintentar.");mode=MODE_NONE;releaseSpeech();}}
    public void onResults(Bundle data){ArrayList<String> matches=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(mode==MODE_SPEECH){String phrase=matches!=null&&!matches.isEmpty()?matches.get(0):"";if(!phrase.isEmpty()){result.setText(phrase);saveTranscript(phrase);}else result.setText("No se entendieron palabras.");state.setText("TRANSCRIPCIÓN COMPLETADA");detail.setText("Tocá Transcribir otra vez para escuchar una nueva frase. Traducir es opcional.");mode=MODE_NONE;releaseSpeech();}}
    public void onPartialResults(Bundle data){ArrayList<String> partial=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(mode==MODE_SPEECH&&partial!=null&&!partial.isEmpty())result.setText(partial.get(0));}
    public void onEvent(int event,Bundle params){}
   });
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognizedLanguage());
   intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   recognizer.startListening(intent);
  }catch(Exception e){state.setText("NO SE PUDO INICIAR");detail.setText(e.getMessage());mode=MODE_NONE;releaseSpeech();}
 }
 private void releaseSpeech(){if(recognizer!=null){SpeechRecognizer old=recognizer;recognizer=null;old.destroy();}}
 private void startSounds(){
  soundRunning=true;state.setText("INICIANDO RECONOCIMIENTO LOCAL");
  result.setText("Preparando modelo YAMNet…");
  detail.setText("Los sonidos se clasifican en este teléfono. No se suben grabaciones a AURORA.");
  work.execute(()->{
   AudioClassifier localClassifier=null;AudioRecord localRecord=null;
   try{
    BaseOptions options=BaseOptions.builder().setModelAssetPath("yamnet.tflite").build();
    AudioClassifierOptions settings=AudioClassifierOptions.builder().setBaseOptions(options).setRunningMode(RunningMode.AUDIO_STREAM)
     .setMaxResults(3).setScoreThreshold(0.18f)
     .setResultListener(r->{
      if(!soundRunning||r.classificationResults().isEmpty()||r.classificationResults().get(0).classifications().isEmpty())return;
      java.util.List<Category> cats=r.classificationResults().get(0).classifications().get(0).categories();
      if(cats.isEmpty())return;
      Category best=cats.get(0);String label=toSpanish(best.categoryName());
      int percent=Math.round(best.score()*100f);
      main.post(()->{if(mode==MODE_SOUND){state.setText("ESCUCHANDO EL ENTORNO");result.setText(label);detail.setText("Posible sonido detectado · confianza "+percent+"%\n"+best.categoryName());}});
     })
     .setErrorListener(e->main.post(()->{if(mode==MODE_SOUND)detail.setText("Error del clasificador: "+e.getMessage());})).build();
    localClassifier=AudioClassifier.createFromOptions(this,settings);classifier=localClassifier;
    localRecord=localClassifier.createAudioRecord(1,16000,16000);audioRecord=localRecord;
    AudioData data=AudioData.create(AudioData.AudioDataFormat.create(localRecord.getFormat()),16000);
    localRecord.startRecording();
    main.post(()->{if(mode==MODE_SOUND){state.setText("ESCUCHANDO EL ENTORNO");result.setText("Buscando sonidos…");}});
    while(soundRunning&&mode==MODE_SOUND){
     if(data.load(localRecord)>0)localClassifier.classifyAsync(data,SystemClock.uptimeMillis());
    }
   }catch(Exception e){
    main.post(()->{if(mode==MODE_SOUND){state.setText("NO SE PUDO ESCUCHAR");result.setText("Error de análisis");detail.setText(String.valueOf(e.getMessage()));mode=MODE_NONE;}});
   }finally{
    if(localRecord!=null){try{localRecord.stop();}catch(Exception ignored){}localRecord.release();}
    if(localClassifier!=null)localClassifier.close();
    if(audioRecord==localRecord)audioRecord=null;
    if(classifier==localClassifier)classifier=null;
   }
  });
 }
 private static String toSpanish(String label){
  String l=label.toLowerCase(Locale.ROOT);
  if(l.contains("speech")||l.contains("conversation"))return "Posible conversación";
  if(l.contains("dog")||l.contains("bark"))return "Posible ladrido";
  if(l.contains("siren"))return "Posible sirena";
  if(l.contains("alarm")||l.contains("smoke detector"))return "Posible alarma";
  if(l.contains("doorbell"))return "Posible timbre";
  if(l.contains("knock")||l.contains("tap"))return "Posibles golpes";
  if(l.contains("telephone")||l.contains("ringtone"))return "Posible teléfono";
  if(l.contains("cry")||l.contains("baby"))return "Posible llanto";
  if(l.contains("car")||l.contains("vehicle")||l.contains("engine"))return "Posible vehículo";
  if(l.contains("music"))return "Posible música";
  if(l.contains("water")||l.contains("rain"))return "Posible agua";
  return "Posible sonido: "+label;
 }
 private void stopAll(boolean show){
  mode=MODE_NONE;soundRunning=false;
  releaseSpeech();
  AudioRecord record=audioRecord;if(record!=null){try{record.stop();}catch(Exception ignored){}}
  if(show){state.setText("MICRÓFONO APAGADO");detail.setText("Escucha detenida. Ninguna función está capturando audio.");}updateModeVisibility();
 }
 @Override protected void onStop(){stopAll(true);super.onStop();}
 @Override protected void onDestroy(){stopAll(false);if(translator!=null)translator.close();work.shutdownNow();super.onDestroy();}
}