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
 private final ExecutorService historyWorker=Executors.newSingleThreadExecutor();
 private final SoundEventGate foregroundSoundGate=new SoundEventGate();
 private SoundEventFeedback soundFeedback;
 private volatile int soundSession=0;
 private int historyGeneration=0;
 private SpeechRecognizer recognizer;
 private final SpeechRetryController speechRetries=new SpeechRetryController();
 private int speechSession=0;
 private Runnable speechRetryTask;
 private void scheduleSpeechRetry(long delay){
  if(mode!=MODE_SPEECH)return;
  final int token=speechSession;
  if(speechRetryTask!=null)main.removeCallbacks(speechRetryTask);
  speechRetryTask=()->{
   speechRetryTask=null;
   if(mode==MODE_SPEECH&&speechSession==token&&recognizer==null)startSpeech();
  };
  main.postDelayed(speechRetryTask,delay);
 }
 private void cancelSpeechRetry(){
  speechSession++;
  if(speechRetryTask!=null){main.removeCallbacks(speechRetryTask);speechRetryTask=null;}
 }

 private volatile boolean soundRunning=false;
 private volatile boolean soundReconnecting=false;
 private AudioRecord audioRecord;
 private AudioClassifier classifier;
 private int mode=MODE_NONE, requestedMode=MODE_NONE, screenMode=MODE_SPEECH;
 private LinearLayout root;
 private ScrollView pageScroll;
 private final SoundCaptureTelemetry captureStats=new SoundCaptureTelemetry();
 private volatile int lastSpeechError=-1;
 private volatile int speechFinalResults=0,speechPartialResults=0;
 private TextView audioHealth;
 private TextView state, result, detail;
 private AuroraVisualEngineView visual;
 private Button speechButton, soundsButton, translateButton;
 private TextView translationResult, translationStatus, translationOriginal;
 private Button[] translationTargetButtons;
 private Button[] sourceLanguageButtons;
 private String activeTranslationPair="";
 private boolean translationModelReady=false,translationModelDownloading=false;
 private final TranslationWorkQueue translationQueue=new TranslationWorkQueue();
 private long newestTranslationId=-1L;
 private volatile String lastTranslationFailure="ninguno";
 private String partialTranscript="";
 private int translationTarget=-1, translationGeneration=0;
 private Spinner languagePicker;
 private int languageSelection=0;
 private Translator translator;
 private String lastTranscript="";
 private boolean speechLaunching=false;
 private SenseHistory history;
 private LinearLayout eventsView;
 private LinearLayout feedbackPanel;
 private TextView soundCount;
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
  boolean backgroundWasActive=getSharedPreferences("aurora",0).getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false);
  // One microphone owner at a time. Entering live Sense stops the background monitor.
  if(getSharedPreferences("aurora",0).getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false)){
   try{startService(new Intent(this,AuroraSoundMonitorService.class).setAction(AuroraSoundMonitorService.STOP));}
   catch(Exception ignored){}
   Toast.makeText(this,"Escucha continua detenida para usar el micrófono aquí",Toast.LENGTH_LONG).show();
  }
  screenMode=getIntent().getIntExtra(EXTRA_MODE,MODE_SPEECH)==MODE_SOUND?MODE_SOUND:MODE_SPEECH;
  draw();
  // Let Android release the previous microphone owner before acquiring it here.
  main.postDelayed(()->toggle(screenMode),backgroundWasActive?550L:0L);
 }
 private void draw(){
  ScrollView scroll=new ScrollView(this);
  pageScroll=scroll;
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
  TextView intro=text(screenMode==MODE_SOUND?"Escuchá lo que sucede a tu alrededor.":"Transcribí y traducí automáticamente, frase por frase.",15,0xffcbddee);
  intro.setGravity(Gravity.CENTER);root.addView(intro);
  visual=new AuroraVisualEngineView(this,screenMode==MODE_SOUND?AuroraVisualEngineView.SOUND:AuroraVisualEngineView.SPEECH);
  LinearLayout.LayoutParams vis=new LinearLayout.LayoutParams(-1,-2);vis.bottomMargin=dp(6);root.addView(visual,vis);
  if(screenMode==MODE_SPEECH)visual.setVisibility(View.GONE);
  state=text("MICRÓFONO APAGADO",16,0xff8fe8df);state.setGravity(Gravity.CENTER);
  state.setBackground(bg(0xff0c303f,0xff14264b));root.addView(state);
  String initial="";
  lastTranscript="";
  if(screenMode==MODE_SOUND){
   root.addView(AuroraDesign.overline(this,"SENSIBILIDAD DE ESCUCHA"));
   LinearLayout sensitivityRow=new LinearLayout(this);sensitivityRow.setOrientation(LinearLayout.HORIZONTAL);
   root.addView(sensitivityRow,new LinearLayout.LayoutParams(-1,dp(65)));
   String[] titles={"ALTA","EQUILIBRADA","PRECISA"};
   final Button[] choices=new Button[3];
   int selectedSensitivity=Math.max(0,Math.min(2,getPreferences(MODE_PRIVATE).getInt("sense_sensitivity",1)));
   minConfidence=selectedSensitivity==0?22:selectedSensitivity==2?45:30;
   for(int i=0;i<choices.length;i++){
    final int selected=i;
    Button choice=button(titles[i]);choice.setTextSize(12);choices[i]=choice;
    int from=i==selectedSensitivity?0xff126979:0xff182b42;
    int to=i==selectedSensitivity?0xff255b79:0xff242849;
    AuroraDesign.action(choice,from,to,i==selectedSensitivity?0xff89fbeb:0xff426783);
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
   LinearLayout languageRow=new LinearLayout(this);languageRow.setOrientation(LinearLayout.HORIZONTAL);
   root.addView(languageRow,new LinearLayout.LayoutParams(-1,dp(64)));
   String[] languageLabels={"ESPAÑOL","INGLÉS","PORTUGUÉS"};
   Button[] languageButtons=new Button[3];
   sourceLanguageButtons=languageButtons;
   languageSelection=Math.max(0,Math.min(2,getPreferences(MODE_PRIVATE).getInt("sense_language",0)));
   for(int k=0;k<3;k++){
    int chosen=k;
    Button item=button(languageLabels[k]);item.setTextSize(12);languageButtons[k]=item;
    AuroraDesign.action(item,k==languageSelection?0xff155a70:0xff1b2e46,
     k==languageSelection?0xff2c4b7a:0xff242741,k==languageSelection?0xff86f4ed:0xff44647d);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(60),1f);
    if(k>0)lp.leftMargin=dp(6);
    languageRow.addView(item,lp);
    item.setOnClickListener(v->{
     if(languageSelection==chosen)return;
     languageSelection=chosen;
     if(translationTarget==chosen){
      translationTarget=chosen==0?1:0;
      getPreferences(MODE_PRIVATE).edit().putInt("translation_target",translationTarget).apply();
     }
     getPreferences(MODE_PRIVATE).edit().putInt("sense_language",chosen).apply();
     refreshLanguageSelections();
     invalidateTranslation();
     lastTranscript="";partialTranscript="";
     if(result!=null)result.setText("Esperando nueva conversación…");
     if(translationResult!=null)translationResult.setText("Esperando una frase para traducir…");
     if(translationOriginal!=null)translationOriginal.setText(selectedLanguageName(languageSelection)+" → "+selectedLanguageName(translationTarget));
     if(mode==MODE_SPEECH){stopAll(true);toggle(MODE_SPEECH);}
    });
   }
  }
  if(screenMode==MODE_SPEECH){
   root.addView(AuroraDesign.overline(this,"TRADUCIR AUTOMÁTICAMENTE AL"));
   LinearLayout targets=new LinearLayout(this);
   targets.setOrientation(LinearLayout.HORIZONTAL);
   root.addView(targets,new LinearLayout.LayoutParams(-1,dp(61)));
   final String[] names={"ESPAÑOL","INGLÉS","PORTUGUÉS"};
   translationTargetButtons=new Button[3];
   int preferred=getPreferences(MODE_PRIVATE).getInt("translation_target",-1);
   translationTarget=preferred>=0&&preferred<3&&preferred!=languageSelection?
    preferred:(languageSelection==0?1:0);
   for(int k=0;k<3;k++){
    final int chosen=k;
    Button target=button(names[k]);target.setTextSize(12);
    translationTargetButtons[k]=target;
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1f);
    if(k>0)lp.leftMargin=dp(5);
    targets.addView(target,lp);
    target.setOnClickListener(v->{
     if(chosen==languageSelection){
      Toast.makeText(this,"Elegí un idioma distinto del original",Toast.LENGTH_SHORT).show();
      return;
     }
     translationTarget=chosen;
     getPreferences(MODE_PRIVATE).edit().putInt("translation_target",chosen).apply();
     refreshLanguageSelections();
     if(translationOriginal!=null)translationOriginal.setText(selectedLanguageName(languageSelection)+" → "+selectedLanguageName(translationTarget));
     if(!lastTranscript.trim().isEmpty())translatePhrase(lastTranscript,true);
     else if(translationStatus!=null)translationMessage("Destino cambiado. La próxima frase se traducirá automáticamente.");
    });
   }
   refreshLanguageSelections();
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
  root.addView(AuroraDesign.overline(this,screenMode==MODE_SOUND?"SONIDO ACTUAL":"LO QUE SE DIJO · ORIGINAL"));
  result=text(screenMode==MODE_SOUND?"Buscando sonidos…":initial.isEmpty()?"Esperando conversación…":initial,25,Color.WHITE);
  result.setGravity(Gravity.CENTER);result.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
  result.setMinHeight(dp(126));result.setBackground(bg(0xff143447,0xff201e45));
  root.addView(result);
  if(screenMode==MODE_SPEECH){
   root.addView(AuroraDesign.overline(this,"TRADUCCIÓN AUTOMÁTICA"));
   translationOriginal=text(selectedLanguageName(languageSelection)+" → "+selectedLanguageName(translationTarget),14,0xffb8e8fa);
   root.addView(translationOriginal);
   translationResult=text("Esperando una frase para traducir…",26,Color.WHITE);
   translationResult.setMinHeight(dp(114));
   translationResult.setTextIsSelectable(true);
   translationResult.setGravity(Gravity.CENTER_VERTICAL);
   translationResult.setBackground(bg(0xff0d434e,0xff1a3658));
   root.addView(translationResult,new LinearLayout.LayoutParams(-1,-2));
   translationStatus=text("AUTOMÁTICO: al finalizar cada frase aparecerá aquí su traducción. La primera vez puede necesitar Internet.",14,0xffbee9eb);
   root.addView(translationStatus);
   translateButton=button("REINTENTAR TRADUCCIÓN DE LA ÚLTIMA FRASE");
   root.addView(translateButton,new LinearLayout.LayoutParams(-1,dp(54)));
   translateButton.setOnClickListener(v->translateLast());
  }
  detail=text("El micrófono funciona únicamente mientras la escucha está activa.",14,0xffcad9f0);
  root.addView(detail);
  audioHealth=text("DIAGNÓSTICO: todavía sin respuestas del detector.",12,0xffb8ddec);
  Button diagnostic=button("COPIAR DIAGNÓSTICO (SIN AUDIO)");
  diagnostic.setOnClickListener(v->copySoundDiagnostic());
  if(screenMode==MODE_SOUND&&getSharedPreferences("aurora",0).getBoolean("sense_feedback_mode",false)){
   TextView note=text("MODO PRUEBA: marcá ACERTÓ o NO COINCIDE debajo de cada detección. "
    +"Si escuchaste algo que AURORA no detectó, usá el botón. No se guarda audio.",13,0xffb0dfee);
   root.addView(note);
   Button missed=button("REGISTRAR SONIDO NO DETECTADO");
   root.addView(missed,new LinearLayout.LayoutParams(-1,dp(62)));
   missed.setOnClickListener(v->{
    String[] categories={"Timbre de puerta","Golpes en una puerta","Ladrido de perro",
     "Música","Voces o conversación","Otro sonido"};
    new android.app.AlertDialog.Builder(this)
     .setTitle("¿Qué sonido no detectó AURORA?")
     .setItems(categories,(d,which)->{
      if(mode!=MODE_SOUND){
       Toast.makeText(this,"Iniciá la escucha para registrar una omisión",Toast.LENGTH_SHORT).show();
       return;
      }
      String line="OMISIÓN MANUAL · "+categories[which]+" · informado por la persona que prueba";
      if(feedbackPanel!=null){
       feedbackPanel.removeAllViews();
       feedbackPanel.addView(text(line+" · solo durante esta prueba",14,0xffd5f0ff));
      }
      // This manual observation is screen-only; no sound TXT.
     })
     .setNegativeButton("Cancelar",null).show();
   });
  }
  if(screenMode==MODE_SOUND&&getSharedPreferences("aurora",0).getBoolean("sense_feedback_mode",false)){
   feedbackPanel=new LinearLayout(this);
   feedbackPanel.setOrientation(LinearLayout.VERTICAL);
   root.addView(feedbackPanel);
  }
  root.addView(AuroraDesign.overline(this,screenMode==MODE_SOUND?"ÚLTIMOS 30 SONIDOS":"HISTORIAL DE ESTA CONVERSACIÓN"));
  if(screenMode==MODE_SOUND){
   LinearLayout topHistory=new LinearLayout(this);topHistory.setGravity(Gravity.CENTER_VERTICAL);
   soundCount=text("0/30 eventos",13,0xffb8ddec);
   topHistory.addView(soundCount,new LinearLayout.LayoutParams(0,-2,1f));
   Button clearSounds=button("BORRAR LISTA");clearSounds.setTextSize(11);
   topHistory.addView(clearSounds,new LinearLayout.LayoutParams(dp(138),dp(48)));
   clearSounds.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
    .setTitle("Borrar los últimos sonidos")
    .setMessage("Solo se borran los últimos eventos detectados. Las conversaciones guardadas no se modifican.")
    .setNegativeButton("CANCELAR",null)
    .setPositiveButton("BORRAR",(dialog,which)->{
     RecentSoundEvents.clear(this);
     showRecentSounds();
    }).show());
   root.addView(topHistory);
  }
  eventsScroll=new ScrollView(this);eventsScroll.setFillViewport(false);
  eventsScroll.setBackground(bg(0xff122b3c,0xff171b35));
  eventsView=new LinearLayout(this);eventsView.setOrientation(LinearLayout.VERTICAL);
  eventsView.setPadding(dp(8),dp(7),dp(8),dp(8));
  eventsScroll.addView(eventsView);
  root.addView(eventsScroll,new LinearLayout.LayoutParams(-1,dp(screenMode==MODE_SOUND?300:205)));
  TextView empty=text(screenMode==MODE_SOUND?"Los últimos 30 sonidos detectados aparecen aquí.":"Las frases de la conversación aparecen aquí.",15,0xffa3b8d2);
  eventsView.addView(empty);
  File[] files=ConversationRecordFilter.conversations(history.list());lastSaved=files.length>0?files[0]:null;
  if(screenMode==MODE_SOUND)showRecentSounds();
  openSaved=button("ABRIR ÚLTIMO ARCHIVO");
  root.addView(openSaved);openSaved.setOnClickListener(v->showFile(lastSaved));
  openSaved.setVisibility(screenMode==MODE_SPEECH&&lastSaved!=null?View.VISIBLE:View.GONE);
  Button assistant=button("ASISTENTE AURORA · FOTO Y SONIDOS");
  root.addView(assistant);
  assistant.setOnClickListener(v->startActivity(new Intent(this,AuroraAssistActivity.class)));
  Button folder=button("📁  CARPETA DE REGISTROS");
  root.addView(folder);folder.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)));
  folder.setVisibility(screenMode==MODE_SPEECH?View.VISIBLE:View.GONE);
  root.addView(AuroraDesign.overline(this,"HERRAMIENTAS OPCIONALES"));
  root.addView(audioHealth);
  root.addView(diagnostic);
  if(screenMode==MODE_SPEECH){
   Button testTranslation=button("PROBAR TRADUCTOR CON TEXTO ESCRITO");
   root.addView(testTranslation);
   testTranslation.setOnClickListener(v->showManualTranslationTest());
  }
  TextView safety=text("Las detecciones son aproximadas. AURORA no sustituye las alarmas certificadas ni los dispositivos de emergencia.",13,0xffc3d6eb);
  root.addView(safety);
  updateModeVisibility();
 }
 private void showRecentSounds(){
  if(screenMode!=MODE_SOUND||eventsView==null)return;
  eventsView.removeAllViews();
  java.util.List<SoundTimeline.Entry> recent=RecentSoundEvents.snapshot(this);
  if(soundCount!=null)soundCount.setText(recent.size()+"/30 eventos detectados");
  if(recent.isEmpty()){
   eventsView.addView(text("Todavía no hay sonidos confirmados. La lista se completa al detectar eventos.",15,0xffa3b8d2));
  }else{
   java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("dd/MM HH:mm:ss",Locale.getDefault());
   for(SoundTimeline.Entry entry:recent){
    LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(dp(7),dp(4),dp(7),dp(5));item.setBackground(bg(0xff162f44,0xff202943));
    TextView clock=text(format.format(new java.util.Date(entry.timestamp)),12,0xffb0d5e7);
    clock.setPadding(dp(5),0,dp(5),0);
    TextView label=text(entry.label,16,0xfff3faff);
    label.setPadding(dp(5),0,dp(5),dp(4));
    item.addView(clock);item.addView(label);
    LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);
    margin.bottomMargin=dp(5);
    eventsView.addView(item,margin);
   }
  }
  eventCount=recent.size();
  if(eventsScroll!=null)eventsScroll.post(()->eventsScroll.scrollTo(0,0));
 }
 private void addTimeline(String content){
  if(screenMode==MODE_SOUND||eventsView==null)return;
  if(eventCount==0)eventsView.removeAllViews();
  // Keep UI responsive after long monitoring sessions. The saved journal
  // still retains the complete session up to its separate storage limit.
  while(eventsView.getChildCount()>=(screenMode==MODE_SOUND?RecentSoundEvents.LIMIT:90))eventsView.removeViewAt(0);
  TextView item=text(content,17,0xffe7f6ff);
  item.setBackground(bg(eventCount%2==0?0xff183d52:0xff252e56,0xff12253b));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(3),0,dp(3));
  eventsView.addView(item,params);eventCount++;
  eventsScroll.post(()->eventsScroll.fullScroll(View.FOCUS_DOWN));
 }
 /**
  * Optional tester feedback for one detected event. Simultaneous sounds
  * get independent controls. No audio is recorded or retained.
  */
 private void recordSoundEvent(String label,int score){
  RecentSoundEvents.add(this,label,score);
  showRecentSounds();
  if(soundFeedback!=null)soundFeedback.onConfirmed(label,true);
  if(!getSharedPreferences("aurora",0).getBoolean("sense_feedback_mode",false)||feedbackPanel==null)return;
  feedbackPanel.removeAllViews();
  feedbackPanel.addView(text("¿Coincidió el último sonido? "+label,14,0xffe5f8ff));
  LinearLayout feedback=new LinearLayout(this);
  feedback.setOrientation(LinearLayout.HORIZONTAL);
  Button yes=button("ACERTÓ");
  Button no=button("NO COINCIDE");
  yes.setTextSize(12);no.setTextSize(12);
  LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(54),1f);
  LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(54),1f);
  b.leftMargin=dp(7);
  feedback.addView(yes,a);feedback.addView(no,b);
  feedbackPanel.addView(feedback);
  yes.setContentDescription("Confirmar detección correcta: "+label);
  no.setContentDescription("Marcar detección incorrecta: "+label);
  yes.setOnClickListener(v->saveSoundFeedback(label,score,true,yes,no));
  no.setOnClickListener(v->saveSoundFeedback(label,score,false,yes,no));
 }
 private void saveSoundFeedback(String label,int score,boolean correct,Button yes,Button no){
  if(mode!=MODE_SOUND){
   Toast.makeText(this,"Iniciá la escucha para evaluar este sonido",Toast.LENGTH_SHORT).show();
   return;
  }
  yes.setEnabled(false);no.setEnabled(false);
  String verdict=correct?"ACERTÓ":"NO COINCIDE";
  // This is user feedback, not an independently measured accuracy metric.
  String entry="VALIDACIÓN MANUAL · "+verdict+" · "+label+" · puntaje IA "+score+"/100";
  if(feedbackPanel!=null){
   feedbackPanel.removeAllViews();
   feedbackPanel.addView(text(entry+" · solo durante esta prueba",14,0xffd5f0ff));
  }
  Toast.makeText(this,"Evaluación mostrada en pantalla",Toast.LENGTH_SHORT).show();
 }
 private void beginHistory(String type){
  eventCount=0;eventsView.removeAllViews();lastSoundLabel="";lastSoundTime=0;
  candidateSound="";candidateHits=0;candidateSince=0;
  historyGeneration++;
  if(screenMode==MODE_SOUND){
   history=null;
   showRecentSounds();
   detail.setText("Últimos 30 sonidos en la app. No se generan archivos de sonidos.");
   return;
  }
  invalidateTranslation(); // New session must not inherit pending translations.
  if(screenMode==MODE_SPEECH){
   lastTranscript="";partialTranscript="";
   if(result!=null)result.setText("Escuchando… Esperando la primera frase.");
   if(translationResult!=null)translationResult.setText("Esperando una frase para traducir…");
   if(translationOriginal!=null)
    translationOriginal.setText(selectedLanguageName(languageSelection)+" → "+selectedLanguageName(translationTarget));
   translationMessage("AUTOMÁTICO: las frases se traducen en orden; las traducciones completadas se guardan.");
  }
  history=new SenseHistory(this);
  history.begin(type);
  TextView placeholder=text("Escuchando… Las detecciones se agregarán aquí.",15,0xffa3b8d2);
  eventsView.addView(placeholder);
  detail.setText("Se guardan las frases originales y las traducciones que alcancen a completarse. No se graba audio.");
 }
 private void recordEvent(String observation){
  addTimeline(observation);
  if(screenMode==MODE_SOUND)return;
  final SenseHistory captured=history;
  if(captured!=null&&captured.hasActive())historyWorker.execute(()->captured.event(observation));
 }
 private void finishHistory(){
  final SenseHistory closing=history;
  if(closing==null)return;
  history=null;
  final int generation=historyGeneration;
  historyWorker.execute(()->{
   try{
    File saved=closing.finish();
    if(saved==null)return;
    String copyError=null;
    try{
     SavedFolderManager copies=new SavedFolderManager(this);
     // Saving in AURORA's private history is already successful.
     // External copying is OPTIONAL; no selected folder is not an error.
     if(copies.selectedFolder()!=null)copies.copyOne(saved);
    }catch(IOException error){copyError=error.getMessage();}
    final String warning=copyError;
    main.post(()->{
     if(isFinishing()||isDestroyed()||generation!=historyGeneration)return;
     lastSaved=saved;
     if(warning!=null&&detail!=null)detail.setText("Registro guardado, pero no se copió a la carpeta elegida: "+warning);
     if(openSaved!=null){openSaved.setVisibility(View.VISIBLE);openSaved.setText("ABRIR ARCHIVO GUARDADO · "+SenseHistory.readableName(saved));}
    });
   }catch(IOException error){
    main.post(()->{
     if(!isFinishing()&&!isDestroyed()&&generation==historyGeneration&&detail!=null)
      detail.setText("No se pudo guardar el registro: "+error.getMessage());
    });
   }
  });
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
  File[] files=ConversationRecordFilter.conversations(history.list());if(files.length==0){Toast.makeText(this,"No hay escuchas guardadas todavía",Toast.LENGTH_SHORT).show();return;}
  String[] labels=new String[files.length];for(int i=0;i<files.length;i++)labels[i]=SenseHistory.readableName(files[i]);
  new android.app.AlertDialog.Builder(this).setTitle("Escuchas guardadas en AURORA").setItems(labels,(d,which)->showFile(files[which])).setNegativeButton("CERRAR",null).show();
 }
 private void copySoundDiagnostic(){
  StringBuilder report=new StringBuilder();
  report.append("AURORA · DIAGNÓSTICO DE ESCUCHA (sin audio ni textos)\n");
  report.append("Aplicación: ").append(getPackageName()).append("\n");
  try{report.append("Versión: ").append(getPackageManager().getPackageInfo(getPackageName(),0).versionName).append("\n");}
  catch(Exception ignored){report.append("Versión: no disponible\n");}
  report.append("Sistema: ").append(android.os.Build.MANUFACTURER).append(" ")
   .append(android.os.Build.MODEL).append(" · Android SDK ")
   .append(android.os.Build.VERSION.SDK_INT).append("\n");
  report.append("Módulo: ").append(screenMode==MODE_SOUND?"SONIDOS":"CONVERSACIÓN").append("\n");
  report.append("Modo activo: ").append(mode).append("\n");
  report.append("Micrófono autorizado: ")
   .append(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED).append("\n");
  android.net.ConnectivityManager network=
   (android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
  android.net.NetworkCapabilities capabilities=network==null?null:
   network.getNetworkCapabilities(network.getActiveNetwork());
  report.append("Conexión validada: ").append(capabilities!=null&&
   capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)).append("\n");
  report.append("Motor de voz Android disponible: ")
   .append(SpeechRecognizer.isRecognitionAvailable(this)).append("\n");
  report.append("Frases completas recibidas: ").append(speechFinalResults).append("\n");
  report.append("Frases parciales recibidas: ").append(speechPartialResults).append("\n");
  report.append("Último código error de voz: ").append(lastSpeechError).append("\n");
  report.append("Último fallo de traducción: ").append(lastTranslationFailure).append("\n");
  if(lastSpeechError>0)report.append("Detalle de voz: ")
   .append(SpeechRetryController.explanation(lastSpeechError)).append("\n");
  report.append(captureStats.report(SystemClock.elapsedRealtime())).append("\n");
  report.append("AVISO: el informe no contiene sonidos grabados, etiquetas ni frases.");
  android.content.ClipboardManager clipboard=
   (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(clipboard!=null){
   clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AURORA DIAGNÓSTICO",report.toString()));
   Toast.makeText(this,"Diagnóstico copiado, pegalo en el chat para revisar el error.",Toast.LENGTH_LONG).show();
  }
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
  int selected=languageSelection;return selected==1?"en-US":selected==2?"pt-BR":"es-AR";
 }
 private void saveTranscript(String phrase){
  lastTranscript=phrase;partialTranscript="";
  getSharedPreferences("aurora",0).edit().putString("sense_last_transcript",phrase).apply();
 }
 private String targetLanguage(int selected){
  return selected==0?TranslateLanguage.SPANISH:
   selected==2?TranslateLanguage.PORTUGUESE:TranslateLanguage.ENGLISH;
 }
 private void translationMessage(String message){
  if(translationStatus!=null)translationStatus.setText(message);
 }
 private String selectedLanguageName(int choice){
  return choice==0?"Español":choice==1?"Inglés":"Portugués";
 }
 private void refreshLanguageSelections(){
  if(sourceLanguageButtons!=null)for(int i=0;i<sourceLanguageButtons.length;i++)
   AuroraDesign.action(sourceLanguageButtons[i],i==languageSelection?0xff155a70:0xff1b2e46,
    i==languageSelection?0xff2c4b7a:0xff242741,i==languageSelection?0xff86f4ed:0xff44647d);
  if(translationTargetButtons!=null)for(int i=0;i<translationTargetButtons.length;i++)
   AuroraDesign.action(translationTargetButtons[i],i==translationTarget?0xff155a70:0xff1b2e46,
    i==translationTarget?0xff2c4b7a:0xff242741,i==translationTarget?0xff86f4ed:0xff44647d);
 }
 private void invalidateTranslation(){
  translationGeneration++;
  translationQueue.clear();
  newestTranslationId=-1L;
  if(translator!=null){translator.close();translator=null;}
  activeTranslationPair="";
  translationModelReady=false;translationModelDownloading=false;
 }
 private void showManualTranslationTest(){
  EditText written=new EditText(this);
  written.setInputType(android.text.InputType.TYPE_CLASS_TEXT|
   android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  written.setTextSize(18);written.setTextColor(Color.WHITE);
  written.setHintTextColor(0xffbedbea);
  written.setHint("Escribí una frase en "+selectedLanguageName(languageSelection));
  written.setMinLines(2);
  written.setPadding(dp(18),dp(15),dp(18),dp(15));
  new android.app.AlertDialog.Builder(this)
   .setTitle("PRUEBA MANUAL DEL TRADUCTOR")
   .setMessage("Este ensayo no usa el micrófono ni se agrega a la conversación guardada.")
   .setView(written)
   .setNegativeButton("CERRAR",null)
   .setPositiveButton("TRADUCIR",(dialog,which)->{
    String phrase=written.getText().toString().trim();
    if(phrase.isEmpty()){Toast.makeText(this,"Escribí una frase primero",Toast.LENGTH_SHORT).show();return;}
    translatePhrase(phrase,false);
    if(pageScroll!=null&&translationResult!=null)
     pageScroll.post(()->pageScroll.smoothScrollTo(0,Math.max(0,translationResult.getTop()-dp(60))));
   }).show();
 }
 private void translateLast(){
  String phrase=lastTranscript.trim();
  if(phrase.isEmpty())phrase=partialTranscript.trim();
  if(phrase.isEmpty()){
   translationMessage("Todavía no reconocí ninguna frase. También podés probar un texto escrito en herramientas.");
   return;
  }
  translatePhrase(phrase,true);
 }
 /**
  * Translate every FINAL recognized phrase automatically. Cache ML Kit
  * for each source/target pair, and ignore superseded async callbacks.
  */
 private void translatePhrase(String phrase,boolean automatic){
  if(translationResult==null||phrase==null)return;
  final String clean=phrase.trim();
  if(clean.isEmpty())return;
  final String source=targetLanguage(languageSelection);
  final String destination=targetLanguage(translationTarget);
  if(source.equals(destination)){
   translationResult.setText("Elegí dos idiomas diferentes.");
   translationMessage("El idioma de la conversación y el destino no pueden coincidir.");
   return;
  }
  final String pair=source+"→"+destination;
  if(!pair.equals(activeTranslationPair)||translator==null){
   invalidateTranslation();
   TranslatorOptions options=new TranslatorOptions.Builder()
    .setSourceLanguage(source).setTargetLanguage(destination).build();
   try{translator=Translation.getClient(options);}
   catch(Exception error){
    lastTranslationFailure="INICIALIZACIÓN "+error.getClass().getSimpleName();
    translationResult.setText("No pude iniciar el traductor.");
    translationMessage("Error al preparar ML Kit. El texto original seguirá guardándose.");
    return;
   }
   activeTranslationPair=pair;
  }
  TranslationWorkQueue.Item item=translationQueue.offer(
   clean,selectedLanguageName(translationTarget),automatic);
  if(item==null){
   translationResult.setText("Hay demasiadas frases esperando traducción.");
   translationMessage("La cola está llena. El texto original se conserva. Detené o probá más tarde.");
   lastTranslationFailure="COLA COMPLETA";
   if(automatic)recordEvent("TRADUCCIÓN NO REALIZADA · cola llena · original conservado");
   return;
  }
  newestTranslationId=item.id;
  translationOriginal.setText((automatic?"ÚLTIMA FRASE RECONOCIDA":"PRUEBA ESCRITA")+
   " · "+selectedLanguageName(languageSelection)+" → "+selectedLanguageName(translationTarget));
  translationResult.setText(translationModelReady?"Esperando turno de traducción…":"Preparando el idioma…");
  translationMessage("Traducción automática en cola: "+translationQueue.size()+" frase(s).");
  pumpTranslationQueue();
 }
 private void pumpTranslationQueue(){
  if(translator==null||isFinishing()||isDestroyed())return;
  TranslationWorkQueue.Item task=translationQueue.start();
  if(task==null)return;
  final Translator client=translator;
  final int generation=translationGeneration;
  if(translationModelReady){
   runQueuedTranslation(generation,task,client);
   return;
  }
  translationModelDownloading=true;
  if(task.id==newestTranslationId){
   translationResult.setText("Descargando o preparando idioma…");
   translationMessage("La primera traducción puede necesitar Internet. Las frases se mantienen en orden.");
  }
  client.downloadModelIfNeeded()
   .addOnSuccessListener(value->{
    if(!isCurrentTranslation(generation,task,client))return;
    translationModelDownloading=false;
    translationModelReady=true;
    runQueuedTranslation(generation,task,client);
   })
   .addOnFailureListener(error->{
    if(!isCurrentTranslation(generation,task,client))return;
    translationModelDownloading=false;
    lastTranslationFailure="DESCARGA "+error.getClass().getSimpleName();
    translationQueue.clear();
    if(task.id==newestTranslationId){
     translationResult.setText("No pude preparar el idioma de traducción.");
     translationMessage("Revisá Internet y tocá REINTENTAR. Las frases originales están guardadas.");
    }
   });
 }
 private boolean isCurrentTranslation(int generation,TranslationWorkQueue.Item task,Translator client){
  return generation==translationGeneration&&task!=null&&
   translationQueue.isActive(task.id)&&client!=null&&translator==client&&
   !isFinishing()&&!isDestroyed();
 }
 private void runQueuedTranslation(int generation,TranslationWorkQueue.Item task,Translator client){
  if(!isCurrentTranslation(generation,task,client))return;
  if(task.id==newestTranslationId){
   translationResult.setText("Traduciendo…");
   translationMessage("Procesando la frase más reciente. Otras traducciones se completan en orden.");
  }
  // A stalled remote task must not block the remaining queue indefinitely.
  main.postDelayed(()->{
   if(!isCurrentTranslation(generation,task,client))return;
   lastTranslationFailure="TIEMPO DE TRADUCCIÓN AGOTADO";
   if(task.fromMicrophone)recordEvent("TRADUCCIÓN NO REALIZADA ("+
    task.language+") · se agotó el tiempo · original: "+shortOriginal(task.phrase));
   if(task.id==newestTranslationId){
    translationResult.setText("La traducción está demorando demasiado.");
    translationMessage("El original quedó guardado. Podés reintentar la última frase.");
   }
   translationQueue.complete(task.id);
   pumpTranslationQueue();
  },45000L);
  client.translate(task.phrase)
   .addOnSuccessListener(translated->{
    if(!isCurrentTranslation(generation,task,client))return;
    String clean=translated==null?"":translated.trim();
    if(clean.isEmpty()){
     if(task.fromMicrophone)recordEvent("TRADUCCIÓN NO REALIZADA ("+
      task.language+") · sin texto · original: "+shortOriginal(task.phrase));
     if(task.id==newestTranslationId){
      translationResult.setText("El traductor no devolvió texto.");
      translationMessage("La transcripción original está guardada. Probá REINTENTAR.");
     }
    }else{
     if(task.fromMicrophone){
      // Store a bilingual reference that can be read again after stopping.
      recordEvent("TRADUCCIÓN ("+task.language+") de «"+
       shortOriginal(task.phrase)+"»: "+clean);
     }
     if(task.id==newestTranslationId){
      translationResult.setText(clean);
      translationMessage("TRADUCCIÓN LISTA · Las frases anteriores también quedan en el historial.");
     }
    }
    translationQueue.complete(task.id);
    pumpTranslationQueue();
   })
   .addOnFailureListener(error->{
    if(!isCurrentTranslation(generation,task,client))return;
    lastTranslationFailure="TRADUCTOR "+error.getClass().getSimpleName();
    if(task.fromMicrophone)recordEvent("TRADUCCIÓN NO REALIZADA ("+
     task.language+") · original: "+shortOriginal(task.phrase));
    if(task.id==newestTranslationId){
     translationResult.setText("No pude traducir esta frase.");
     translationMessage("Error de ML Kit. El original se guardó; podés reintentar.");
    }
    translationQueue.complete(task.id);
    pumpTranslationQueue();
   });
 }
 private String shortOriginal(String original){
  String compact=original.replace('\n',' ').replace('\r',' ').trim();
  return compact.length()>90?compact.substring(0,90)+"…":compact;
 }
 private void startSpeech(){
  if(mode!=MODE_SPEECH||recognizer!=null)return;
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   state.setText("MICRÓFONO SIN PERMISO");detail.setText("Volvé a autorizar el micrófono desde Android.");
   stopAll(false);return;
  }
  state.setText("ESCUCHANDO CONVERSACIÓN");
  if(lastTranscript.isEmpty()&&eventCount==0)result.setText("Hablá cerca del teléfono…");
  detail.setText("Transcripción activa. Las pausas no interrumpen el historial; el micrófono se detiene al salir.");
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   state.setText("SIN RECONOCIMIENTO DE VOZ");
   result.setText("No hay servicio de transcripción en este teléfono.");
   stopAll(false);return;
  }
  try{
   final SpeechRecognizer current=(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))
    ?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);
   recognizer=current;
   current.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle params){if(mode==MODE_SPEECH&&recognizer==current)state.setText("ESCUCHANDO · HABLÁ AHORA");}
    public void onBeginningOfSpeech(){if(mode==MODE_SPEECH&&recognizer==current)state.setText("DETECTANDO VOZ");}
    public void onRmsChanged(float rms){if(mode==MODE_SPEECH&&recognizer==current&&visual!=null)visual.setEnergy(Math.max(0f,Math.min(1f,rms/12f)));}
    public void onBufferReceived(byte[] buffer){}
    public void onEndOfSpeech(){if(mode==MODE_SPEECH&&recognizer==current)state.setText("PROCESANDO PALABRAS…");}
    public void onError(int error){
     if(mode!=MODE_SPEECH||recognizer!=current)return;
     lastSpeechError=error;
     if(audioHealth!=null)audioHealth.setText("Error del reconocimiento de voz: código "+
      error+" · "+SpeechRetryController.explanation(error));
     releaseSpeech();
     if(SpeechRetryController.isSilence(error)){
      // Silence is normal during long conversations; do not count it as a failure.
      state.setText("ESCUCHANDO · EN PAUSA");
      scheduleSpeechRetry(speechRetries.silenceDelay());
     }else if(SpeechRetryController.isTemporary(error)){
      long delay=speechRetries.onTemporaryFailure();
      if(delay>=0){
       state.setText("RECONEXIÓN AUTOMÁTICA");
       detail.setText(SpeechRetryController.explanation(error)+" · próximo intento en "+((delay+999)/1000)+" s");
       scheduleSpeechRetry(delay);
      }else{
       state.setText("TRANSCRIPCIÓN PAUSADA");
       detail.setText("Android rechazó varios intentos. Tocá ESCUCHAR para reintentar cuando quieras.");
       stopAll(false);
      }
     }else{
      state.setText("TRANSCRIPCIÓN DETENIDA");
      detail.setText(SpeechRetryController.explanation(error)+". Podés iniciar una nueva sesión.");
      stopAll(false);
     }
    }
    public void onResults(Bundle data){
     if(mode!=MODE_SPEECH||recognizer!=current)return;
     ArrayList<String> matches=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     String phrase=matches!=null&&!matches.isEmpty()?matches.get(0).trim():"";
     if(!phrase.isEmpty()){
      speechFinalResults++;
      lastSpeechError=-1;
      result.setText(phrase);saveTranscript(phrase);recordEvent(phrase);
       translatePhrase(phrase,true);
      speechRetries.reset();
      if(visual!=null){visual.setEnergy(.75f);visual.setDetected(true);}
     }
     state.setText("ESCUCHANDO · NUEVA FRASE");
     releaseSpeech();
     scheduleSpeechRetry(speechRetries.silenceDelay());
    }
    public void onPartialResults(Bundle data){
     if(mode!=MODE_SPEECH||recognizer!=current)return;
     ArrayList<String> partial=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(partial!=null&&!partial.isEmpty()){
      speechPartialResults++;
      partialTranscript=partial.get(0).trim();
      if(!partialTranscript.isEmpty()){
         result.setText(partialTranscript);
         if(translationStatus!=null)translationMessage("Escuchando la frase… Se traducirá sola cuando Android la termine de reconocer.");
        }
     }
    }
    public void onEvent(int event,Bundle params){}
   });
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognizedLanguage());
   intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   // Advisory to speech services: wait longer before ending a spoken phrase.
   // Some Android recognizers may ignore this suggestion.
   intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,3000L);
   intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,1800L);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   current.startListening(intent);
  }catch(Exception ex){
   releaseSpeech();
   long delay=speechRetries.onTemporaryFailure();
   if(delay>=0&&mode==MODE_SPEECH){
    state.setText("INTENTANDO RECUPERAR MICRÓFONO");
    detail.setText("No se pudo iniciar el servicio de voz. Reintentando sin perder la sesión.");
    scheduleSpeechRetry(delay);
   }else{
    state.setText("TRANSCRIPCIÓN DETENIDA");
    detail.setText("No se pudo iniciar el reconocimiento: "+ex.getMessage());
    stopAll(false);
   }
  }
 }
 private void releaseSpeech(){if(recognizer!=null){SpeechRecognizer old=recognizer;recognizer=null;old.destroy();}}
 private final SoundLiveWindow soundLiveWindow=new SoundLiveWindow();
 private Runnable soundDisplayTick;

 private void updateLiveSoundDisplay(long now){
  if(mode!=MODE_SOUND||!soundRunning)return;
  if(soundReconnecting){
   result.setText("RECONECTANDO EL MICRÓFONO…\nNo se están detectando sonidos durante la reconexión");
   return;
  }
  java.util.List<SoundLiveWindow.Item> recent=soundLiveWindow.snapshot(now,4);
  if(recent.isEmpty()){
   result.setText("Escuchando…\nSin sonidos identificados ahora");
   return;
  }
  StringBuilder lines=new StringBuilder();
  for(SoundLiveWindow.Item item:recent){
   if(lines.length()>0)lines.append("\n");
   lines.append(item.label).append(" · IA ").append(item.score).append("/100");
  }
  result.setText(lines.toString());
 }
 private void stopSoundDisplayTick(){
  if(soundDisplayTick!=null){main.removeCallbacks(soundDisplayTick);soundDisplayTick=null;}
  soundLiveWindow.clear();
 }
 private void beginSoundDisplayTick(final int run){
  stopSoundDisplayTick();
  soundDisplayTick=new Runnable(){
   @Override public void run(){
    if(soundRunning&&mode==MODE_SOUND&&soundSession==run){
     long current=SystemClock.elapsedRealtime();
     updateLiveSoundDisplay(current);
     if(audioHealth!=null){
      long age=captureStats.callbackAge(current);
      audioHealth.setText(age<0?
       "IA: sin respuesta todavía · muestras "+captureStats.getSamples():
       "IA: "+captureStats.getResults()+" respuestas · última hace "+(age/1000)+
        " s · reconexiones "+captureStats.getRecoveries());
     }
     main.postDelayed(this,1000L);
    }
   }
  };
  main.postDelayed(soundDisplayTick,1000L);
 }
 private void startSounds(){
  soundFeedback=new SoundEventFeedback(this);
  soundRunning=true;
  soundReconnecting=false;
  captureStats.begin(SystemClock.elapsedRealtime());
  final int run=++soundSession;
  foregroundSoundGate.reset();
  beginSoundDisplayTick(run);
  state.setText("INICIANDO RECONOCIMIENTO LOCAL");
  result.setText("Preparando modelo YAMNet…");
  detail.setText("La pantalla actualiza sonidos recientes aunque no generen alertas. No se graba audio.");
  work.execute(()->{
   int consecutiveFailures=0;
   while(soundRunning&&soundSession==run&&mode==MODE_SOUND){
    AudioClassifier localClassifier=null;
    AudioRecord localRecord=null;
    final java.util.concurrent.atomic.AtomicLong lastResult=new java.util.concurrent.atomic.AtomicLong(0L);
    final java.util.concurrent.atomic.AtomicReference<String> classifierError=new java.util.concurrent.atomic.AtomicReference<>();
    long started=SystemClock.elapsedRealtime();
    long recoveryDelay=0L;
    try{
     BaseOptions base=BaseOptions.builder().setModelAssetPath("yamnet.tflite").build();
     AudioClassifierOptions settings=AudioClassifierOptions.builder().setBaseOptions(base)
      .setRunningMode(RunningMode.AUDIO_STREAM).setMaxResults(40).setScoreThreshold(.12f)
      .setResultListener(r->{
       if(!soundRunning||soundSession!=run||r==null)return;
       lastResult.set(SystemClock.elapsedRealtime());
       captureStats.classified(SystemClock.elapsedRealtime());
       if(r.classificationResults().isEmpty()||r.classificationResults().get(0).classifications().isEmpty())return;
       java.util.Map<String,Float> rawScores=new java.util.LinkedHashMap<>();
       for(Category cat:r.classificationResults().get(0).classifications().get(0).categories()){
        String raw=cat.categoryName();
        Float previous=rawScores.get(raw);
        if(previous==null||cat.score()>previous)rawScores.put(raw,cat.score());
       }
       final java.util.Map<String,Float> current=SoundEvidence.fromRawScores(rawScores);
       main.post(()->{
        if(!soundRunning||soundSession!=run||mode!=MODE_SOUND)return;
        long now=SystemClock.elapsedRealtime();
        soundLiveWindow.observe(current,now);
        updateLiveSoundDisplay(now);
        int sensitivity=minConfidence<=22?0:minConfidence>=45?2:1;
        int maxConfirmed=0;
        for(java.util.Map.Entry<String,Float> event:current.entrySet()){
         String label=event.getKey();
         int score=Math.round(event.getValue()*100f);
         if(!foregroundSoundGate.accept(label,score,sensitivity,now))continue;
         maxConfirmed=Math.max(maxConfirmed,score);
         recordSoundEvent(label,score);
        }
        if(maxConfirmed>0&&visual!=null){
         visual.setEnergy(maxConfirmed/100f);
         visual.setDetected(true);
        }
       });
      })
      .setErrorListener(error->{
       classifierError.compareAndSet(null,error==null?"Error del clasificador":error.getMessage());
      }).build();
     localClassifier=AudioClassifier.createFromOptions(this,settings);
     classifier=localClassifier;
     localRecord=localClassifier.createAudioRecord(1,16000,16000);
     audioRecord=localRecord;
     AudioData data=AudioData.create(AudioData.AudioDataFormat.create(localRecord.getFormat()),SoundAudioCapturePolicy.BLOCK_SAMPLES);
     if(!soundRunning||soundSession!=run)return;
     localRecord.startRecording();
     if(localRecord.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)
      throw new IllegalStateException("Android no habilitó el micrófono");
     soundReconnecting=false;
     started=SystemClock.elapsedRealtime();
     main.post(()->{
      if(soundRunning&&soundSession==run&&mode==MODE_SOUND){
       state.setText("ESCUCHANDO EL ENTORNO");
       detail.setText("Sonidos en vivo. Los avisos sólo aparecen al confirmarse. No se guarda audio.");
      }
     });
     int emptyReads=0;
     while(soundRunning&&soundSession==run&&mode==MODE_SOUND){
      if(classifierError.get()!=null)throw new IllegalStateException("El modelo perdió la conexión de audio");
      int frames=data.load(localRecord);
      if(frames>0){
       captureStats.samplesRead(frames);
       localClassifier.classifyAsync(data,SystemClock.uptimeMillis());
       emptyReads=0;
      }else{
       if(++emptyReads>=12)throw new IllegalStateException("Micrófono sin muestras");
       SystemClock.sleep(120L);
      }
      long now=SystemClock.elapsedRealtime();
      long last=lastResult.get();
      if(SoundRecoveryPolicy.isStalled(last>0?last:started,now))
       throw new IllegalStateException("El modelo dejó de responder");
     }
    }catch(Exception error){
     if(!soundRunning||soundSession!=run||mode!=MODE_SOUND)break;
     android.util.Log.w("AURORA","Escucha interrumpida: recuperación de micrófono/modelo",error);
     captureStats.recovering(error.getClass().getSimpleName()+": "+error.getMessage());
     soundReconnecting=true;
     if(SystemClock.elapsedRealtime()-started>30000L)consecutiveFailures=0;
     recoveryDelay=SoundRecoveryPolicy.delayAfterFailure(++consecutiveFailures);
     if(recoveryDelay<0){
      final String reason=error.getMessage()==null?"Error persistente":error.getMessage();
      captureStats.failed(error.getClass().getSimpleName()+": "+reason);
      main.post(()->{
       if(soundRunning&&soundSession==run&&mode==MODE_SOUND){
        stopAll(false);
        state.setText("ESCUCHA DETENIDA · ERROR");
        result.setText("El detector no está escuchando");
        detail.setText(reason+". Revisá permisos y tocá ESCUCHAR para reiniciar.");
       }
      });
      break;
     }
     final int attempt=consecutiveFailures;
     main.post(()->{
      if(soundRunning&&soundSession==run&&mode==MODE_SOUND){
       soundLiveWindow.clear();
       state.setText("RECONECTANDO EL DETECTOR");
       result.setText("Reconectando micrófono…");
       detail.setText("Interrupción detectada. Reintento "+attempt+" de 3.");
      }
     });
    }finally{
     if(localRecord!=null){
      try{localRecord.stop();}catch(Exception ignored){}
      try{localRecord.release();}catch(Exception ignored){}
     }
     if(localClassifier!=null){try{localClassifier.close();}catch(Exception ignored){}}
     if(audioRecord==localRecord)audioRecord=null;
     if(classifier==localClassifier)classifier=null;
    }
    // The old AudioRecord must be released BEFORE waiting to reconnect.
    if(recoveryDelay>0L)SystemClock.sleep(recoveryDelay);
   }
  });
 }
 private void stopAll(boolean show){
  invalidateTranslation(); // Do not write late ML Kit results after closing the journal.
  cancelSpeechRetry();speechRetries.reset();
  soundSession++;
  if(soundFeedback!=null){soundFeedback.stop();soundFeedback=null;}
  mode=MODE_NONE;soundRunning=false;soundReconnecting=false;stopSoundDisplayTick();
  if(visual!=null){visual.setListening(false);visual.setDetected(false);visual.setEnergy(0f);}
  finishHistory();
  releaseSpeech();
  AudioRecord record=audioRecord;if(record!=null){try{record.stop();}catch(Exception ignored){}}
  if(show){state.setText("MICRÓFONO APAGADO");detail.setText("Escucha detenida. Ninguna función está capturando audio.");}updateModeVisibility();
 }
 @Override protected void onStop(){stopAll(true);super.onStop();}
 @Override protected void onDestroy(){
  stopAll(false);
  work.shutdownNow();historyWorker.shutdown();super.onDestroy();
 }
}