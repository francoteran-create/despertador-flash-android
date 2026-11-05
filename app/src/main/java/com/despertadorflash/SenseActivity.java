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
import java.io.File;
import java.io.IOException;
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
 private int mode=MODE_NONE, requestedMode=MODE_NONE, screenMode=MODE_SPEECH;
 private LinearLayout root;
 private TextView state, result, detail;
 private AuroraVisualEngineView visual;
 private Button speechButton, soundsButton, translateButton;
 private Spinner languagePicker;
 private Translator translator;
 private String lastTranscript="";
 private boolean speechLaunching=false;
 private SenseHistory history;
 private LinearLayout eventsView;
 private ScrollView eventsScroll;
 private Button openSaved;
 private File lastSaved;
 private File pendingExport;
 private String lastSoundLabel="";
 private long lastSoundTime=0;
 private String candidateSound=""; private int candidateHits=0; private long candidateSince=0;
 private int minConfidence=30;
 private int eventCount=0;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private GradientDrawable bg(int a,int b){return AuroraDesign.surface(this,a,b,0xff427997,24);}
 private TextView text(String content,int size,int color){TextView t=new TextView(this);t.setText(content);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;}
 private Button button(String content){Button b=new Button(this);b.setText(content);AuroraDesign.action(b,0xff1a4264,0xff292656,0xff4b7898);return b;}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);history=new SenseHistory(this);
  screenMode=getIntent().getIntExtra(EXTRA_MODE,MODE_SPEECH)==MODE_SOUND?MODE_SOUND:MODE_SPEECH;
  draw();
  main.post(()->toggle(screenMode));
 }
 private void draw(){
  ScrollView scroll=new ScrollView(this);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(dp(17),dp(14),dp(17),dp(25));
  root.setBackground(new AuroraSkyDrawable());scroll.addView(root);setContentView(scroll);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top);
  Button back=button("‹");back.setContentDescription("Volver a AURORA");
  top.addView(back,new LinearLayout.LayoutParams(dp(56),dp(54)));back.setOnClickListener(v->finish());
  TextView heading=text(screenMode==MODE_SOUND?"SONIDOS":"CONVERSACIÓN",23,Color.WHITE);
  heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));heading.setGravity(Gravity.CENTER);
  top.addView(heading,new LinearLayout.LayoutParams(0,dp(62),1f));
  Button other=button(screenMode==MODE_SOUND?"◉  VOZ":"◖))  SONIDOS");
  top.addView(other,new LinearLayout.LayoutParams(dp(112),dp(54)));
  other.setOnClickListener(v->{
   stopAll(true);
   Intent destination=new Intent(this,SenseActivity.class);
   destination.putExtra(EXTRA_MODE,screenMode==MODE_SOUND?MODE_SPEECH:MODE_SOUND);
   startActivity(destination);finish();
  });
  root.addView(AuroraDesign.overline(this,"AURORA  /  INTELIGENCIA SENSORIAL"));
  TextView intro=text(screenMode==MODE_SOUND?"Escuchá lo que sucede a tu alrededor.":"Convertí conversaciones en texto claro.",15,0xffcbddee);
  intro.setGravity(Gravity.CENTER);root.addView(intro);
  visual=new AuroraVisualEngineView(this,screenMode==MODE_SOUND?AuroraVisualEngineView.SOUND:AuroraVisualEngineView.SPEECH);
  LinearLayout.LayoutParams vis=new LinearLayout.LayoutParams(-1,-2);vis.bottomMargin=dp(6);root.addView(visual,vis);
  state=text("MICRÓFONO APAGADO",16,0xff8fe8df);state.setGravity(Gravity.CENTER);
  state.setBackground(bg(0xff0c303f,0xff14264b));root.addView(state);
  String initial=getSharedPreferences("aurora",0).getString("sense_last_transcript","");
  lastTranscript=initial;
  if(screenMode==MODE_SOUND){
   root.addView(AuroraDesign.overline(this,"SENSIBILIDAD DE ESCUCHA"));
   LinearLayout sensitivityRow=new LinearLayout(this);sensitivityRow.setOrientation(LinearLayout.HORIZONTAL);
   root.addView(sensitivityRow,new LinearLayout.LayoutParams(-1,dp(65)));
   String[] titles={"ALTA","EQUILIBRADA","PRECISA"};
   final Button[] choices=new Button[3];
   int initial=Math.max(0,Math.min(2,getPreferences(MODE_PRIVATE).getInt("sense_sensitivity",1)));
   minConfidence=initial==0?22:initial==2?45:30;
   for(int i=0;i<choices.length;i++){
    final int selected=i;
    Button choice=button(titles[i]);choice.setTextSize(12);choices[i]=choice;
    int from=i==initial?0xff126979:0xff182b42;
    int to=i==initial?0xff255b79:0xff242849;
    AuroraDesign.action(choice,from,to,i==initial?0xff89fbeb:0xff426783);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(63),1f);
    if(i>0)lp.leftMargin=dp(6);
    sensitivityRow.addView(choice,lp);
    choice.setOnClickListener(v->{
     minConfidence=selected==0?22:selected==2?45:30;
     getPreferences(MODE_PRIVATE).edit().putInt("sense_sensitivity",selected).apply();
     for(int k=0;k<choices.length;k++){
      AuroraDesign.action(choices[k],k==selected?0xff126979:0xff182b42,
       k==selected?0xff255b79:0xff242849,k==selected?0xff89fbeb:0xff426783);
      choices[k].setSelected(k==selected);
     }
     Toast.makeText(this,"Sensibilidad: "+titles[selected].toLowerCase(Locale.ROOT),Toast.LENGTH_SHORT).show();
    });
   }
   TextView sensitivityHint=text("Alta: más detecciones · Precisa: menos avisos falsos",13,0xffa8cadd);
   root.addView(sensitivityHint);
  }else{
   root.addView(AuroraDesign.overline(this,"IDIOMA DE LA CONVERSACIÓN"));
   languagePicker=new Spinner(this);
   ArrayAdapter<String> languages=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Español","Inglés","Portugués"});
   languagePicker.setAdapter(languages);languagePicker.setBackground(bg(0xff182a43,0xff222653));
   root.addView(languagePicker,new LinearLayout.LayoutParams(-1,dp(58)));
  }
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
  LinearLayout.LayoutParams controlsParams=new LinearLayout.LayoutParams(-1,-2);controlsParams.topMargin=dp(12);
  root.addView(controls,controlsParams);
  if(screenMode==MODE_SPEECH){
   speechButton=button("🎙  ESCUCHAR");controls.addView(speechButton,new LinearLayout.LayoutParams(0,dp(68),1f));
   speechButton.setOnClickListener(v->toggle(MODE_SPEECH));
  }else{
   soundsButton=button("◖))  ESCUCHAR");controls.addView(soundsButton,new LinearLayout.LayoutParams(0,dp(68),1f));
   soundsButton.setOnClickListener(v->toggle(MODE_SOUND));
  }
  Button stop=button("■  DETENER");AuroraDesign.action(stop,0xff66334d,0xff4a253c,0xffa76e89);
  LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(0,dp(68),1f);stopParams.leftMargin=dp(9);
  controls.addView(stop,stopParams);stop.setOnClickListener(v->stopAll(true));
  root.addView(AuroraDesign.overline(this,"RESULTADO ACTUAL"));
  result=text(screenMode==MODE_SOUND?"Buscando sonidos…":initial.isEmpty()?"Esperando conversación…":initial,25,Color.WHITE);
  result.setGravity(Gravity.CENTER);result.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
  result.setMinHeight(dp(126));result.setBackground(bg(0xff143447,0xff201e45));
  root.addView(result);
  detail=text("El micrófono funciona únicamente mientras la escucha está activa.",14,0xffcad9f0);
  root.addView(detail);
  if(screenMode==MODE_SPEECH){
   translateButton=button("🌐  TRADUCIR ÚLTIMA FRASE");
   root.addView(translateButton,new LinearLayout.LayoutParams(-1,dp(58)));
   translateButton.setOnClickListener(v->translateLast());
  }
  root.addView(AuroraDesign.overline(this,"HISTORIAL DE ESTA SESIÓN"));
  eventsScroll=new ScrollView(this);eventsScroll.setFillViewport(false);
  eventsScroll.setBackground(bg(0xff122b3c,0xff171b35));
  eventsView=new LinearLayout(this);eventsView.setOrientation(LinearLayout.VERTICAL);
  eventsView.setPadding(dp(8),dp(7),dp(8),dp(8));
  eventsScroll.addView(eventsView);
  root.addView(eventsScroll,new LinearLayout.LayoutParams(-1,dp(205)));
  TextView empty=text("Las detecciones y frases aparecen acá, sin borrar las anteriores.",15,0xffa3b8d2);
  eventsView.addView(empty);
  File[] files=history.list();lastSaved=files.length>0?files[0]:null;
  openSaved=button("ABRIR ÚLTIMO ARCHIVO");
  root.addView(openSaved);openSaved.setOnClickListener(v->showFile(lastSaved));
  openSaved.setVisibility(lastSaved!=null?View.VISIBLE:View.GONE);
  Button folder=button("📁  CARPETA DE REGISTROS");
  root.addView(folder);folder.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)));
  TextView safety=text("Las detecciones son aproximadas. AURORA no sustituye las alarmas certificadas ni los dispositivos de emergencia.",13,0xffc3d6eb);
  root.addView(safety);
  updateModeVisibility();
 }
 private void addTimeline(String content){
  if(eventsView==null)return;
  if(eventCount==0)eventsView.removeAllViews();
  TextView item=text(content,17,0xffe7f6ff);
  item.setBackground(bg(eventCount%2==0?0xff183d52:0xff252e56,0xff12253b));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(3),0,dp(3));
  eventsView.addView(item,params);eventCount++;
  eventsScroll.post(()->eventsScroll.fullScroll(View.FOCUS_DOWN));
 }
 private void beginHistory(String type){
  eventCount=0;eventsView.removeAllViews();lastSoundLabel="";lastSoundTime=0;
  candidateSound="";candidateHits=0;candidateSince=0;
  history.begin(type);
  TextView placeholder=text("Escuchando… Las detecciones se agregarán aquí.",15,0xffa3b8d2);
  eventsView.addView(placeholder);
  detail.setText("Resultados en vivo. Se guardarán en un archivo cuando detengas la escucha o salgas.");
 }
 private void recordEvent(String observation){
  addTimeline(observation);
  history.event(observation);
 }
 private void finishHistory(){
  try{
   File saved=history.finish();
   if(saved!=null){
    lastSaved=saved;
    try{new SavedFolderManager(this).copyOne(saved);}
    catch(IOException copyError){if(detail!=null)detail.setText("Archivo guardado en AURORA, pero la copia a la carpeta elegida falló: "+copyError.getMessage());}
    if(openSaved!=null){openSaved.setVisibility(View.VISIBLE);openSaved.setText("ABRIR ARCHIVO GUARDADO · "+SenseHistory.readableName(saved));}
   }
  }catch(IOException e){if(detail!=null)detail.setText("No se pudo guardar el archivo: "+e.getMessage());}
 }
 private void showFile(File selected){
  if(selected==null){Toast.makeText(this,"Todavía no hay archivos guardados",Toast.LENGTH_SHORT).show();return;}
  try{
   ScrollView scroll=new ScrollView(this);
   TextView content=text(SenseHistory.read(selected),16,0xffeaf7ff);content.setTextIsSelectable(true);
   content.setBackgroundColor(0xff101b30);scroll.addView(content);
   new android.app.AlertDialog.Builder(this).setTitle(SenseHistory.readableName(selected)).setView(scroll).setNeutralButton("EXPORTAR", (d,w)->exportFile(selected)).setPositiveButton("CERRAR",null).show();
  }catch(IOException e){new android.app.AlertDialog.Builder(this).setMessage("No se pudo abrir el archivo: "+e.getMessage()).setPositiveButton("OK",null).show();}
 }
 private void exportFile(File selected){
  if(selected==null)return;
  pendingExport=selected;
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("text/plain");
  intent.putExtra(Intent.EXTRA_TITLE,selected.getName());
  try{startActivityForResult(intent,71);}
  catch(Exception e){Toast.makeText(this,"No se puede abrir el selector de archivos",Toast.LENGTH_LONG).show();}
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode!=71)return;
  if(resultCode!=RESULT_OK||data==null||data.getData()==null||pendingExport==null){pendingExport=null;return;}
  try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){
   if(out==null)throw new IOException("No se pudo abrir el destino");
   out.write(SenseHistory.read(pendingExport).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   out.flush();
   Toast.makeText(this,"Historial exportado correctamente",Toast.LENGTH_LONG).show();
  }catch(Exception e){Toast.makeText(this,"Error al exportar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  pendingExport=null;
 }
 private void showHistory(){
  File[] files=history.list();if(files.length==0){Toast.makeText(this,"No hay escuchas guardadas todavía",Toast.LENGTH_SHORT).show();return;}
  String[] labels=new String[files.length];for(int i=0;i<files.length;i++)labels[i]=SenseHistory.readableName(files[i]);
  new android.app.AlertDialog.Builder(this).setTitle("Escuchas guardadas en AURORA").setItems(labels,(d,which)->showFile(files[which])).setNegativeButton("CERRAR",null).show();
 }
 private void toggle(int next){
  if(next!=screenMode)return;
  if(mode==next){stopAll(true);return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestedMode=next;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;
  }
  stopAll(false);mode=next;if(visual!=null){visual.setListening(true);visual.setDetected(false);}beginHistory(next==MODE_SOUND?"sonidos":"conversacion");updateModeVisibility();
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
  boolean isSpeech=screenMode==MODE_SPEECH;
  if(languagePicker!=null)languagePicker.setVisibility(isSpeech?View.VISIBLE:View.GONE);
  if(translateButton!=null)translateButton.setVisibility(isSpeech?View.VISIBLE:View.GONE);
 }
 private String recognizedLanguage(){
  int selected=languagePicker==null?0:languagePicker.getSelectedItemPosition();return selected==1?"en-US":selected==2?"pt-BR":"es-AR";
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
  state.setText("ESCUCHANDO CONVERSACIÓN");
  if(lastTranscript.isEmpty()&&eventCount==0)result.setText("Hablá cerca del teléfono…");
  detail.setText("Transcribiendo en vivo. Guardado automático al detener o salir.");
  if(!SpeechRecognizer.isRecognitionAvailable(this)){state.setText("SIN RECONOCIMIENTO DE VOZ");result.setText("Este teléfono no tiene un servicio de transcripción disponible.");mode=MODE_NONE;finishHistory();return;}
  try{
   if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
   else recognizer=SpeechRecognizer.createSpeechRecognizer(this);
   recognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle params){state.setText("ESCUCHANDO · HABLÁ AHORA");}
    public void onBeginningOfSpeech(){state.setText("DETECTANDO VOZ");}
    public void onRmsChanged(float rms){if(mode==MODE_SPEECH&&visual!=null)visual.setEnergy(Math.max(0f,Math.min(1f,rms/12f)));}
    public void onBufferReceived(byte[] buffer){}
    public void onEndOfSpeech(){state.setText("PROCESANDO PALABRAS…");}
    public void onError(int error){
     if(mode!=MODE_SPEECH)return;
     releaseSpeech();
     if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
      state.setText("ESCUCHANDO · REINICIANDO");
      main.postDelayed(()->{if(mode==MODE_SPEECH)startSpeech();},650);
     }else{
      state.setText("TRANSCRIPCIÓN DETENIDA");
      detail.setText("Android no pudo continuar (código "+error+"). Se guardó la sesión.");
      mode=MODE_NONE;finishHistory();
     }
    }
    public void onResults(Bundle data){ArrayList<String> matches=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(mode==MODE_SPEECH){
      String phrase=matches!=null&&!matches.isEmpty()?matches.get(0):"";
      if(!phrase.isEmpty()){result.setText(phrase);saveTranscript(phrase);recordEvent(phrase);if(visual!=null){visual.setEnergy(.75f);visual.setDetected(true);}}
      state.setText("ESCUCHANDO · NUEVA FRASE");
      releaseSpeech();
      main.postDelayed(()->{if(mode==MODE_SPEECH)startSpeech();},650);
     }}
    public void onPartialResults(Bundle data){ArrayList<String> partial=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(mode==MODE_SPEECH&&partial!=null&&!partial.isEmpty())result.setText(partial.get(0));}
    public void onEvent(int event,Bundle params){}
   });
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognizedLanguage());
   intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   recognizer.startListening(intent);
  }catch(Exception e){state.setText("NO SE PUDO INICIAR");detail.setText(e.getMessage());mode=MODE_NONE;releaseSpeech();finishHistory();}
 }
 private void releaseSpeech(){if(recognizer!=null){SpeechRecognizer old=recognizer;recognizer=null;old.destroy();}}
 private void startSounds(){
  soundRunning=true;state.setText("INICIANDO RECONOCIMIENTO LOCAL");
  result.setText("Preparando modelo YAMNet…");
  detail.setText("Reconocimiento local. Resultados en castellano; archivo al detener o salir.");
  work.execute(()->{
   AudioClassifier localClassifier=null;AudioRecord localRecord=null;
   try{
    BaseOptions options=BaseOptions.builder().setModelAssetPath("yamnet.tflite").build();
    AudioClassifierOptions settings=AudioClassifierOptions.builder().setBaseOptions(options).setRunningMode(RunningMode.AUDIO_STREAM)
     .setMaxResults(6).setScoreThreshold(0.15f)
     .setResultListener(r->{
      if(!soundRunning||r.classificationResults().isEmpty()||r.classificationResults().get(0).classifications().isEmpty())return;
      java.util.List<Category> cats=r.classificationResults().get(0).classifications().get(0).categories();
      String label=null;float score=0f;
      for(Category cat:cats){
       String possible=SoundLabelsEs.name(cat.categoryName());
       if(possible!=null&&cat.score()>score){label=possible;score=cat.score();}
      }
      if(label==null)return;
      final String found=label;
      final int percent=Math.round(score*100f);
      main.post(()->{
       if(mode!=MODE_SOUND||percent<minConfidence)return;
       long now=System.currentTimeMillis();
       if(!found.equals(candidateSound)||now-candidateSince>4500L){
        candidateSound=found;candidateHits=1;candidateSince=now;
       }else candidateHits++;
       boolean confirmed=candidateHits>=2||percent>=72;
       if(!confirmed)return;
       state.setText("ESCUCHANDO EL ENTORNO");
       result.setText(found);
       if(visual!=null){visual.setEnergy(percent/100f);visual.setDetected(true);}
       detail.setText("Reconocimiento aproximado · confianza "+percent+"% · sensibilidad ajustable");
       if(!found.equals(lastSoundLabel)||now-lastSoundTime>=12000L){
        lastSoundLabel=found;lastSoundTime=now;
        recordEvent(found+" · confianza "+percent+"%");
       }
      });
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
    main.post(()->{if(mode==MODE_SOUND){state.setText("NO SE PUDO ESCUCHAR");result.setText("Error de análisis");detail.setText(String.valueOf(e.getMessage()));mode=MODE_NONE;finishHistory();}});
   }finally{
    if(localRecord!=null){try{localRecord.stop();}catch(Exception ignored){}localRecord.release();}
    if(localClassifier!=null)localClassifier.close();
    if(audioRecord==localRecord)audioRecord=null;
    if(classifier==localClassifier)classifier=null;
   }
  });
 }
 private void stopAll(boolean show){
  mode=MODE_NONE;soundRunning=false;
  if(visual!=null){visual.setListening(false);visual.setDetected(false);visual.setEnergy(0f);}
  finishHistory();
  releaseSpeech();
  AudioRecord record=audioRecord;if(record!=null){try{record.stop();}catch(Exception ignored){}}
  if(show){state.setText("MICRÓFONO APAGADO");detail.setText("Escucha detenida. Ninguna función está capturando audio.");}updateModeVisibility();
 }
 @Override protected void onStop(){stopAll(true);super.onStop();}
 @Override protected void onDestroy(){stopAll(false);if(translator!=null)translator.close();work.shutdownNow();super.onDestroy();}
}