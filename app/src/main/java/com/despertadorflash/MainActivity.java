package com.despertadorflash;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
 static final String PREF="aurora";
 LinearLayout root; SharedPreferences prefs; CountDownTimer timer; android.os.Handler flashHandler=new android.os.Handler(android.os.Looper.getMainLooper()); Runnable flashTask; boolean alarmActive=false; boolean timerAlertActive=false; int activeAlarmId=-1; TorchFlasher torch; boolean alarmsPage=false; boolean timerPage=false; boolean communicationPage=false; boolean settingsPage=false;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(5),dp(12),dp(5),dp(12));return v;}
 GradientDrawable auroraBackground(int a,int z,int stroke){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,z});d.setCornerRadius(dp(20));d.setStroke(dp(1),stroke);return d;}
 void accent(Button b,int a,int z,int line){b.setBackground(auroraBackground(a,z,line));}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(-1);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));b.setTextSize(16);b.setMinHeight(dp(60));accent(b,0xff18294d,0xff192047,0xff466eac);return b;}
 TextView section(String s){TextView v=txt(s,21);v.setTextColor(0xffa5daf3);v.setTypeface(null,Typeface.BOLD);return v;}
 boolean pendingAlarmLaunch=false; boolean pendingTimerLaunch=false;
 SpeechRecognizer homeRecognizer; boolean homeListening=false; TextView homeTranscript,homeSpeechStatus; Button homeSpeechToggle;
 SenseHistory homeSpeechHistory;
 final Handler speechHandler=new Handler(Looper.getMainLooper());
 public void onCreate(Bundle b){
  super.onCreate(b);
  prefs=getSharedPreferences(PREF,0);torch=new TorchFlasher(this);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  render();
  pendingAlarmLaunch=getIntent().getBooleanExtra("ring",false);
  pendingTimerLaunch=getIntent().getBooleanExtra("timer_finished",false);
 }
 @Override protected void onPostResume(){
  super.onPostResume();
  if(pendingAlarmLaunch){
   pendingAlarmLaunch=false;
   showAlarm();
  }else if(pendingTimerLaunch){
   pendingTimerLaunch=false;
   showTimerFinished();
  }
 }
 void startTorchAlert(){if(prefs.getBoolean("torch_alert",true)){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Para usar el flash LED, autorizá Cámara en AURORA",Toast.LENGTH_LONG).show();return;}if(torch!=null&&!torch.start())Toast.makeText(this,"Flash LED: "+torch.getLastError(),Toast.LENGTH_LONG).show();}}
 void stopAlarmEffects(){
  if(torch!=null)torch.stop();
  alarmActive=false; activeAlarmId=-1;
  if(flashTask!=null){flashHandler.removeCallbacks(flashTask);flashTask=null;}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)for(int id=0;id<5;id++)nm.cancel(2000+id);
 }
 boolean snoozeAlarm(){
  AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
  if(activeAlarmId<0){Toast.makeText(this,"La alarma de prueba no se puede posponer",Toast.LENGTH_LONG).show();return false;}
  Intent intent=new Intent(this,AlarmReceiver.class).putExtra("snooze",true).putExtra("id",activeAlarmId);
  PendingIntent pending=PendingIntent.getBroadcast(this,4100+activeAlarmId,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{long when=System.currentTimeMillis()+10*60000L;if(Build.VERSION.SDK_INT>=31&&!manager.canScheduleExactAlarms()){Toast.makeText(this,"Habilitá alarmas exactas para posponer",Toast.LENGTH_LONG).show();return false;}manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);Toast.makeText(this,"Alarma pospuesta 10 minutos",Toast.LENGTH_LONG).show();return true;}catch(SecurityException ex){Toast.makeText(this,"No se pudo posponer: falta permiso de alarmas",Toast.LENGTH_LONG).show();return false;}
 }
 void showTimerFinished(){
  if(isFinishing()||timerAlertActive||alarmActive)return;
  timerAlertActive=true;
  NotificationManager timerNotifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(timerNotifications!=null)timerNotifications.cancel(3100);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
  startTorchAlert();
  Vibrator vib=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  if(vib!=null&&vib.hasVibrator()){long[] pattern={0,900,250,900,250,900};if(Build.VERSION.SDK_INT>=26)vib.vibrate(VibrationEffect.createWaveform(pattern,0));else vib.vibrate(pattern,0);}
  TextView message=txt("¡TIEMPO CUMPLIDO!",32);message.setGravity(Gravity.CENTER);message.setTypeface(null,Typeface.BOLD);message.setBackgroundColor(0xffffd633);message.setTextColor(Color.BLACK);
  new AlertDialog.Builder(this).setTitle("AURORA · TEMPORIZADOR").setView(message).setPositiveButton("DETENER ALERTA",(d,w)->{if(vib!=null)vib.cancel();if(torch!=null)torch.stop();timerAlertActive=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}).setOnDismissListener(d->{if(vib!=null)vib.cancel();if(torch!=null)torch.stop();timerAlertActive=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}).setCancelable(false).show();
 }
 void showAlarm(){
  if(alarmActive)return;
  alarmActive=true; startTorchAlert(); activeAlarmId=getIntent().getBooleanExtra("ring",false)?getIntent().getIntExtra("alarm_id",-1):-1;
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(24),dp(22),dp(24));
  TextView big=txt("¡DESPERTÁ!",38);big.setGravity(Gravity.CENTER);big.setTypeface(null,Typeface.BOLD);panel.addView(big);
  TextView sub=txt("AURORA está avisándote. Tocá el botón para detener la alerta.",19);sub.setGravity(Gravity.CENTER);panel.addView(sub);
  final boolean[] bright={false};
  flashTask=new Runnable(){public void run(){bright[0]=!bright[0];panel.setBackgroundColor(bright[0]?0xffffffff:0xff10182e);big.setTextColor(bright[0]?0xff08162c:0xffffffff);sub.setTextColor(bright[0]?0xff08162c:0xffffffff);flashHandler.postDelayed(this,650);}};
  if(prefs.getBoolean("visual_flash",false)&&!isFinishing())flashHandler.post(flashTask);else{panel.setBackgroundColor(0xff10182e);big.setTextColor(Color.WHITE);sub.setTextColor(Color.WHITE);}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){long[] pattern={0,900,300,900,300,900,300};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pattern,0));else v.vibrate(pattern,0);}
  AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).setPositiveButton("ESTOY DESPIERTO",(d,w)->stopAlarmEffects()).setNeutralButton("POSPONER 10 MIN",null).setOnDismissListener(d->stopAlarmEffects()).setCancelable(false).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(buttonView->{if(snoozeAlarm())dialog.dismiss();}));
  dialog.show();
 }

 @Override public void onBackPressed(){if(alarmActive){new AlertDialog.Builder(this).setTitle("Alarma activa").setMessage("Confirmá que estás despierto para detener la alarma.").setPositiveButton("Entendido",null).show();return;}if(alarmsPage||timerPage||communicationPage||settingsPage){alarmsPage=false;timerPage=false;communicationPage=false;settingsPage=false;render();return;}super.onBackPressed();}
 @Override protected void onNewIntent(Intent i){
  super.onNewIntent(i);setIntent(i);
  pendingAlarmLaunch=i.getBooleanExtra("ring",false);
  pendingTimerLaunch=i.getBooleanExtra("timer_finished",false);
  if(hasWindowFocus()){
   if(pendingAlarmLaunch){pendingAlarmLaunch=false;showAlarm();}
   else if(pendingTimerLaunch){pendingTimerLaunch=false;showTimerFinished();}
  }
 }
 @Override public void onWindowFocusChanged(boolean hasFocus){
  super.onWindowFocusChanged(hasFocus);
  if(hasFocus&&!alarmActive&&!timerAlertActive){
   if(pendingAlarmLaunch){pendingAlarmLaunch=false;showAlarm();}
   else if(pendingTimerLaunch){pendingTimerLaunch=false;showTimerFinished();}
  }
 }
 @Override protected void onResume(){super.onResume();if(prefs!=null){for(int id=0;id<5;id++){if(prefs.getBoolean("on"+id,false))schedule(this,id);}}}
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==1&&grantResults.length>0&&grantResults[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Sin notificaciones, algunas alarmas podrían no mostrarse",Toast.LENGTH_LONG).show();}if(requestCode==44&&grantResults.length>0){if(grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startHomeSpeech();else Toast.makeText(this,"AURORA necesita permiso de micrófono para transcribir",Toast.LENGTH_LONG).show();}if(requestCode==2&&grantResults.length>0){boolean granted=grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;Toast.makeText(this,granted?"Flash LED autorizado. Probá una alarma de 1 minuto.":"Flash LED deshabilitado: falta permiso de Cámara.",Toast.LENGTH_LONG).show();if(!alarmActive&&!timerAlertActive)render();}}
 LinearLayout auroraCard(int first,int second){
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));
  card.setBackground(auroraBackground(first,second,0xff5f82b1));
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(9),0,dp(8));card.setLayoutParams(p);return card;
 }
 void openSense(int requested){
  stopHomeSpeech();Intent i=new Intent(this,SenseActivity.class);i.putExtra(SenseActivity.EXTRA_MODE,requested);startActivity(i);
 }
 void startHomeSpeech(){
  if(checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},44);return;
  }
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   homeSpeechStatus.setText("No hay un servicio de reconocimiento de voz disponible en Android.");return;
  }
  homeListening=true;
  if(homeSpeechHistory==null)homeSpeechHistory=new SenseHistory(this);
  homeSpeechHistory.begin("conversacion");
  homeSpeechToggle.setText("■  DETENER ESCUCHA");
  homeSpeechStatus.setText("MICRÓFONO ACTIVO · escuchando conversación");
  try{
   if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))homeRecognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
   else homeRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
   homeRecognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){if(homeListening)homeSpeechStatus.setText("ESCUCHANDO · hablá cerca del teléfono");}
    public void onBeginningOfSpeech(){if(homeListening)homeSpeechStatus.setText("DETECTANDO VOZ…");}
    public void onRmsChanged(float r){}
    public void onBufferReceived(byte[] data){}
    public void onEndOfSpeech(){if(homeListening)homeSpeechStatus.setText("Procesando conversación…");}
    public void onError(int code){if(!homeListening)return;
     if(code==SpeechRecognizer.ERROR_NO_MATCH||code==SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
      speechHandler.postDelayed(()->{if(homeListening)listenHomeSpeech();},650);
     }else {stopHomeSpeech();homeSpeechStatus.setText("Reconocimiento detenido · código de Android: "+code);}
    }
    public void onResults(Bundle data){if(!homeListening)return;
     java.util.ArrayList<String> matches=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(matches!=null&&!matches.isEmpty()){String phrase=matches.get(0);homeTranscript.setText(phrase);prefs.edit().putString("sense_last_transcript",phrase).apply();if(homeSpeechHistory!=null)homeSpeechHistory.event(phrase);}
     speechHandler.postDelayed(()->{if(homeListening)listenHomeSpeech();},650);
    }
    public void onPartialResults(Bundle data){if(!homeListening)return;
     java.util.ArrayList<String> matches=data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(matches!=null&&!matches.isEmpty())homeTranscript.setText(matches.get(0));
    }
    public void onEvent(int event,Bundle args){}
   });
   listenHomeSpeech();
  }catch(Exception e){stopHomeSpeech();homeSpeechStatus.setText("No se pudo abrir el micrófono: "+e.getMessage());}
 }
 void listenHomeSpeech(){
  if(!homeListening||homeRecognizer==null)return;
  Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-AR");
  i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
  try{homeRecognizer.startListening(i);}catch(Exception e){stopHomeSpeech();homeSpeechStatus.setText("Escucha interrumpida: "+e.getMessage());}
 }
 void stopHomeSpeech(){
  boolean wasListening=homeListening;
  homeListening=false;speechHandler.removeCallbacksAndMessages(null);
  if(homeSpeechHistory!=null&&homeSpeechHistory.hasActive()){
   try{
    java.io.File saved=homeSpeechHistory.finish();
    if(saved!=null){
     try{new SavedFolderManager(this).copyOne(saved);}catch(java.io.IOException copyError){android.util.Log.w("Aurora","No se pudo copiar el archivo a carpeta elegida",copyError);}
     if(wasListening&&homeSpeechStatus!=null)homeSpeechStatus.setText("Guardado: "+SenseHistory.readableName(saved));
    }
   }catch(java.io.IOException e){
    if(homeSpeechStatus!=null)homeSpeechStatus.setText("Error guardando conversación: "+e.getMessage());
   }
  }
  if(homeRecognizer!=null){SpeechRecognizer old=homeRecognizer;homeRecognizer=null;old.destroy();}
  if(homeSpeechToggle!=null)homeSpeechToggle.setText("🎙  ESCUCHAR CONVERSACIÓN");
  if(homeSpeechStatus!=null&&homeSpeechStatus.getText().toString().startsWith("MICRÓFONO"))homeSpeechStatus.setText("Micrófono apagado · tocá para escuchar");
 }
 void setupPage(){
  if(timer!=null){timer.cancel();timer=null;}
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(26));
  root.setBackground(new AuroraSkyDrawable());
  scroll.addView(root);setContentView(scroll);
  getWindow().setStatusBarColor(0xff080e27);getWindow().setNavigationBarColor(0xff090d20);
 }
 void pageTitle(String heading){
  Button back=btn("←   VOLVER AL INICIO");root.addView(back);back.setOnClickListener(v->{alarmsPage=false;timerPage=false;communicationPage=false;settingsPage=false;render();});
  TextView title=txt(heading,27);title.setGravity(Gravity.CENTER);title.setTextColor(0xffb3eeff);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 }
 void render(){
  if(alarmsPage){stopHomeSpeech();renderAlarmsPage();return;}
  if(timerPage){stopHomeSpeech();renderTimerPage();return;}
  if(communicationPage){stopHomeSpeech();renderCommunicationPage();return;}
  if(settingsPage){stopHomeSpeech();renderSettingsPage();return;}
  setupPage();
  LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.HORIZONTAL);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading);
  TextView title=txt("A U R O R A",28);title.setGravity(Gravity.CENTER);title.setTextColor(Color.WHITE);title.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
  heading.addView(title,new LinearLayout.LayoutParams(0,dp(68),1f));
  Button profile=btn("◉");profile.setContentDescription("Abrir Mi perfil AURORA");profile.setTextSize(25);
  accent(profile,0xff1c4866,0xff32285a,0xff7bdcec);
  LinearLayout.LayoutParams profileParams=new LinearLayout.LayoutParams(dp(54),dp(54));profileParams.rightMargin=dp(8);
  heading.addView(profile,profileParams);
  profile.setOnClickListener(v->{stopHomeSpeech();startActivity(new Intent(this,ProfileActivity.class));});
  Button settings=btn("⚙");settings.setContentDescription("Configuración y permisos");settings.setTextSize(24);accent(settings,0xff17203e,0xff22284c,0xff3e5981);
  heading.addView(settings,new LinearLayout.LayoutParams(dp(54),dp(54)));settings.setOnClickListener(v->{settingsPage=true;render();});
  TextView subtitle=txt("ALARMA VISUAL Y SENSORIAL ACCESIBLE",12);subtitle.setGravity(Gravity.CENTER);subtitle.setTextColor(0xffa2edfd);root.addView(subtitle);
  AuroraVisualEngineView clock=new AuroraVisualEngineView(this,AuroraVisualEngineView.CLOCK);root.addView(clock,new LinearLayout.LayoutParams(-1,-2));
  Button alarms=btn("◷    MIS ALARMAS       ›");accent(alarms,0xff60431e,0xff6b4d26,0xffffd775);root.addView(alarms);alarms.setOnClickListener(v->{alarmsPage=true;render();});
  LinearLayout soundsCard=auroraCard(0xff123b59,0xff241d58);
  TextView soundsHeading=txt("◖))  RECONOCER SONIDOS",20);soundsHeading.setTypeface(null,Typeface.BOLD);soundsHeading.setTextColor(0xffbcecff);soundsCard.addView(soundsHeading);
  TextView soundsHint=txt("Detectar sonidos del entorno con IA",14);soundsHint.setTextColor(0xffc5d8f0);soundsCard.addView(soundsHint);
  Button listenSounds=btn("◖))  ESCUCHAR");accent(listenSounds,0xff145b8a,0xff2c3877,0xff5bdbff);soundsCard.addView(listenSounds);
  soundsCard.setOnClickListener(v->openSense(SenseActivity.MODE_SOUND));
  listenSounds.setOnClickListener(v->openSense(SenseActivity.MODE_SOUND));
  root.addView(soundsCard);
  LinearLayout transcriptCard=auroraCard(0xff22183e,0xff15385c);
  TextView speechHeading=txt("◉  TRANSCRIPCIÓN EN VIVO",20);speechHeading.setTypeface(null,Typeface.BOLD);speechHeading.setTextColor(0xffe7ccff);transcriptCard.addView(speechHeading);
  homeTranscript=txt(prefs.getString("sense_last_transcript","").isEmpty()?"Acá aparecerá lo que se está diciendo…":prefs.getString("sense_last_transcript",""),21);
  homeTranscript.setMinHeight(dp(80));homeTranscript.setMaxLines(3);homeTranscript.setEllipsize(android.text.TextUtils.TruncateAt.END);transcriptCard.addView(homeTranscript);
  homeSpeechStatus=txt("Tocá el micrófono para escuchar · tocá el cuadro para ampliar",13);homeSpeechStatus.setTextColor(0xffbad3ec);transcriptCard.addView(homeSpeechStatus);
  homeSpeechToggle=btn("🎙  ESCUCHAR CONVERSACIÓN");accent(homeSpeechToggle,0xff6540a0,0xff234c78,0xffbd8ef4);transcriptCard.addView(homeSpeechToggle);
  transcriptCard.setOnClickListener(v->openSense(SenseActivity.MODE_SPEECH));
  homeSpeechToggle.setOnClickListener(v->{if(homeListening)stopHomeSpeech();else startHomeSpeech();});
  root.addView(transcriptCard);
  Button messages=btn("▣    COMUNICACIÓN       ›");accent(messages,0xff5d287e,0xff35205e,0xffc889ff);root.addView(messages);messages.setOnClickListener(v->{stopHomeSpeech();communicationPage=true;render();});
 }
 void renderTimerPage(){
  setupPage();pageTitle("TEMPORIZADOR");
  TextView timersHeading=section("◷  TEMPORIZADORES");root.addView(timersHeading);
 long savedEnd=prefs.getLong("timer_end",0);long remaining=Math.max(0,savedEnd-System.currentTimeMillis());TextView counter=txt(remaining>0?String.format(Locale.getDefault(),"%02d:%02d",remaining/60000,(remaining/1000)%60):"05:00",32);root.addView(counter);if(remaining>0){timer=new CountDownTimer(remaining,1000){public void onTick(long ms){counter.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){counter.setText("¡TIEMPO CUMPLIDO!");prefs.edit().remove("timer_end").apply();showTimerFinished();}}.start();}
 Button one=btn("Prueba rápida · 1 minuto");root.addView(one);one.setOnClickListener(v->countdown(1,counter));
 Button five=btn("Iniciar 5 minutos");root.addView(five);five.setOnClickListener(v->countdown(5,counter));
 Button ten=btn("Iniciar 10 minutos");root.addView(ten);ten.setOnClickListener(v->countdown(10,counter));
 Button stop=btn("Detener temporizador");root.addView(stop);stop.setOnClickListener(v->{if(timer!=null)timer.cancel();cancelTimer();counter.setText("Detenido");});

 }
 void renderCommunicationPage(){
  setupPage();pageTitle("COMUNICACIÓN");
  Button recognize=btn("◖))  ESCUCHAR SONIDOS");accent(recognize,0xff285b93,0xff19415b,0xff6af7ef);root.addView(recognize);recognize.setOnClickListener(v->openSense(SenseActivity.MODE_SOUND));
  Button transcribe=btn("🎙  TRANSCRIBIR CONVERSACIÓN");accent(transcribe,0xff632e8e,0xff28285a,0xffbb8fff);root.addView(transcribe);transcribe.setOnClickListener(v->openSense(SenseActivity.MODE_SPEECH));
  Button myProfile=btn("◉  MI PERFIL AURORA");accent(myProfile,0xff22506d,0xff362a62,0xff83d6ed);root.addView(myProfile);myProfile.setOnClickListener(v->startActivity(new Intent(this,ProfileActivity.class)));
  Button records=btn("📁  CARPETA DE REGISTROS");accent(records,0xff1a3f62,0xff23305a,0xff62cbe8);root.addView(records);records.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)));
  root.addView(txt("Elegí una frase o escribí un mensaje para mostrarlo en pantalla completa.",16));
 EditText msg=new EditText(this);msg.setTextColor(Color.WHITE);msg.setHintTextColor(0xffaebdd1);msg.setHint("Escribí un mensaje");root.addView(msg);
 Button show=btn("Mostrar mensaje grande");root.addView(show);show.setOnClickListener(v->{String message=msg.getText().toString().trim();if(!message.isEmpty())showFullMessage(message);});
 Button yes=btn("Mostrar SÍ");root.addView(yes);yes.setOnClickListener(v->showFullMessage("SÍ"));
 Button no=btn("Mostrar NO");root.addView(no);no.setOnClickListener(v->showFullMessage("NO"));
 Button help=btn("Mostrar NECESITO AYUDA");root.addView(help);help.setOnClickListener(v->showFullMessage("NECESITO AYUDA"));
 Button okay=btn("Mostrar ESTOY BIEN");root.addView(okay);okay.setOnClickListener(v->showFullMessage("ESTOY BIEN"));
 Button write=btn("Pedir que escriban");root.addView(write);write.setOnClickListener(v->showFullMessage("POR FAVOR, ESCRIBÍ LO QUE DECÍS"));
 Button urgent=btn("Mostrar LLAMÁ A EMERGENCIAS");root.addView(urgent);urgent.setOnClickListener(v->showFullMessage("LLAMÁ A EMERGENCIAS"));
 Button repeat=btn("Pedir que repitan despacio");root.addView(repeat);repeat.setOnClickListener(v->showFullMessage("POR FAVOR, REPETÍ MÁS DESPACIO"));

 }
 void renderSettingsPage(){
  setupPage();pageTitle("CONFIGURACIÓN Y PRUEBAS");
  TextView motionTitle=section("AURORA VISUAL ENGINE");root.addView(motionTitle);
  android.widget.Switch motion=new android.widget.Switch(this);motion.setText("Auroras y núcleo animados");
  motion.setTextColor(Color.WHITE);motion.setTextSize(17);motion.setPadding(dp(12),dp(12),dp(12),dp(12));
  motion.setChecked(prefs.getBoolean("aurora_motion",true));
  root.addView(motion);motion.setOnCheckedChangeListener((control,enabled)->{
   prefs.edit().putBoolean("aurora_motion",enabled).apply();
   Toast.makeText(this,enabled?"Animaciones habilitadas":"Animaciones reducidas",Toast.LENGTH_SHORT).show();
  });
  root.addView(txt("Los efectos se reducen automáticamente si Android tiene activada la opción de reducir animaciones o el modo ahorro de batería.",13));

 Button testFlash=btn("⚡  PROBAR FLASH LED · 5 SEGUNDOS");accent(testFlash,0xff752c97,0xff116a92,0xff64e8f5);root.addView(testFlash);testFlash.setOnClickListener(v->{
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);Toast.makeText(this,"Autorizá Cámara y volvé a tocar Probar flash",Toast.LENGTH_LONG).show();return;}
  if(torch==null||!torch.start()){new AlertDialog.Builder(this).setTitle("Flash no disponible").setMessage("AURORA no pudo encender la linterna: "+(torch==null?"Control no inicializado":torch.getLastError())+". Comprobá que la cámara esté cerrada y que AURORA tenga permiso de Cámara.").setPositiveButton("Entendido",null).show();return;}
  Toast.makeText(this,"El flash debe parpadear durante 5 segundos",Toast.LENGTH_LONG).show();
  new Handler(Looper.getMainLooper()).postDelayed(()->{if(torch!=null)torch.stop();},5000);
 });

 AlarmManager alarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
 if(Build.VERSION.SDK_INT>=31&&!alarmManager.canScheduleExactAlarms()){TextView warning=txt("ATENCIÓN: las alarmas exactas no están habilitadas. Activá el permiso para que el despertador funcione a la hora elegida.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){TextView warning=txt("ATENCIÓN: faltan permisos de notificaciones. Los avisos pueden no aparecer.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 TextView statusTitle=section("◇  ESTADO DEL SISTEMA");root.addView(statusTitle);
 LinearLayout statusPanel=new LinearLayout(this);statusPanel.setOrientation(1);statusPanel.setPadding(dp(14),dp(10),dp(14),dp(10));statusPanel.setBackground(auroraBackground(0xff14233e,0xff102a40,0xff387c9e));root.addView(statusPanel);
 boolean cameraReady=Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED;
 boolean notificationsReady=Build.VERSION.SDK_INT<33||checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED;
 boolean exactReady=Build.VERSION.SDK_INT<31||alarmManager.canScheduleExactAlarms();
 TextView permissionSummary=txt("⚡  Flash LED     "+(cameraReady?"✓ Autorizado":"! Falta permiso")+"\n◉  Notificaciones     "+(notificationsReady?"✓ Autorizadas":"! Falta permiso")+"\n◷  Alarmas exactas     "+(exactReady?"✓ Permitidas":"! Falta permiso"),15);
 permissionSummary.setTextColor(0xffa9f4e0);permissionSummary.setLineSpacing(dp(5),1f);statusPanel.addView(permissionSummary);
 Button perm=btn("Permitir alarmas exactas");root.addView(perm);perm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);if(!a.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}});
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){Button notificationPermission=btn("Habilitar notificaciones");root.addView(notificationPermission);notificationPermission.setOnClickListener(v->requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1));}
 if(Build.VERSION.SDK_INT>=34){NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(!notificationManager.canUseFullScreenIntent()){TextView fullWarning=txt("Android restringe las alarmas a pantalla completa. Revisá el permiso de alertas de pantalla completa.",16);fullWarning.setTextColor(0xffffcf70);root.addView(fullWarning);Button fullPermission=btn("Permitir alertas a pantalla completa");root.addView(fullPermission);fullPermission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,android.net.Uri.parse("package:"+getPackageName()))));}}
 Switch torchSwitch=new Switch(this);torchSwitch.setText("Flash LED intermitente en alarmas y temporizador");torchSwitch.setTextColor(Color.WHITE);torchSwitch.setTextSize(17);torchSwitch.setChecked(prefs.getBoolean("torch_alert",true));root.addView(torchSwitch);torchSwitch.setOnCheckedChangeListener((button,enabled)->{prefs.edit().putBoolean("torch_alert",enabled).apply();if(enabled&&Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);if(!enabled&&torch!=null)torch.stop();});
 if(Build.VERSION.SDK_INT>=23&&prefs.getBoolean("torch_alert",true)&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){Button torchPermission=btn("Autorizar flash LED (Cámara)");root.addView(torchPermission);torchPermission.setOnClickListener(v->requestPermissions(new String[]{android.Manifest.permission.CAMERA},2));}
 root.addView(txt("Aviso: el flash LED intermitente puede afectar a personas fotosensibles. Podés desactivarlo con el interruptor anterior.",13));
 Switch visualFlash=new Switch(this);visualFlash.setText("Destellos visuales (opcional)");visualFlash.setTextColor(Color.WHITE);visualFlash.setTextSize(17);visualFlash.setChecked(prefs.getBoolean("visual_flash",false));root.addView(visualFlash);visualFlash.setOnCheckedChangeListener((button,enabled)->{if(enabled)new AlertDialog.Builder(this).setTitle("Aviso sobre destellos").setMessage("Los cambios rápidos de luz pueden afectar a personas con fotosensibilidad. Activá esta opción solo si es adecuada para vos.").setPositiveButton("Activar",(d,w)->prefs.edit().putBoolean("visual_flash",true).apply()).setNegativeButton("Cancelar",(d,w)->visualFlash.setChecked(false)).show();else prefs.edit().putBoolean("visual_flash",false).apply();});


 Button test=btn("Probar vibración");root.addView(test);test.setOnClickListener(v->vibrate());
 Button preview=btn("Probar pantalla de alarma");root.addView(preview);preview.setOnClickListener(v->showAlarm());
 Button stopVibration=btn("Detener vibración");root.addView(stopVibration);stopVibration.setOnClickListener(v->stopAlarmEffects());
 root.addView(txt("Aviso: las alertas dependen de los permisos, la batería y las restricciones de Android. Esta aplicación no reemplaza alarmas de emergencia certificadas. La ejecución en segundo plano depende de Android y del fabricante.",13));
 }
 void showFullMessage(String message){
  final Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(Color.WHITE);layout.setPadding(dp(20),dp(24),dp(20),dp(24));
  TextView text=new TextView(this);text.setText(message);text.setTextColor(Color.BLACK);text.setTextSize(40);text.setGravity(Gravity.CENTER);text.setTypeface(null,Typeface.BOLD);layout.addView(text,new LinearLayout.LayoutParams(-1,0,1));
  Button invert=btn("Invertir colores");layout.addView(invert);final boolean[] dark={false};invert.setOnClickListener(v->{dark[0]=!dark[0];layout.setBackgroundColor(dark[0]?Color.BLACK:Color.WHITE);text.setTextColor(dark[0]?Color.WHITE:Color.BLACK);});
  Button close=btn("Cerrar mensaje");layout.addView(close);close.setOnClickListener(v->dialog.dismiss());
  dialog.setContentView(layout);dialog.show();
 }
 void renderAlarmsPage(){
  if(timer!=null){timer.cancel();timer=null;}
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff080d21,0xff0e1735,0xff071122}));getWindow().setStatusBarColor(0xff080d21);getWindow().setNavigationBarColor(0xff071122);scroll.addView(root);setContentView(scroll);
  Button back=btn("← Volver al inicio");root.addView(back);back.setOnClickListener(v->{alarmsPage=false;render();});
  TextView heading=txt("MIS ALARMAS",32);heading.setTextColor(0xffffd633);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);
  root.addView(txt("Creá una alarma y elegí la hora que quieras. No hay horarios predefinidos.",16));
  Button timerTool=btn("⌛  TEMPORIZADOR Y PRUEBAS");root.addView(timerTool);timerTool.setOnClickListener(v->{alarmsPage=false;timerPage=true;render();});
  int count=0;
  for(int id=0;id<5;id++)if(prefs.getBoolean("created"+id,false)){alarmRow(id);count++;}
  if(count==0)root.addView(txt("Todavía no creaste ninguna alarma.",18));
  if(count<5){Button add=btn("+ Crear alarma");root.addView(add);add.setOnClickListener(v->{
   int slot=-1;for(int j=0;j<5;j++)if(!prefs.getBoolean("created"+j,false)){slot=j;break;}
   if(slot<0)return;final int id=slot;
   Calendar now=Calendar.getInstance();
   new TimePickerDialog(this,(picker,h,m)->{
    prefs.edit().putBoolean("created"+id,true).putInt("h"+id,h).putInt("m"+id,m).putBoolean("on"+id,true).apply();
    schedule(this,id);render();
   },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();
  });}
 }
 void alarmRow(int id){
 int h=prefs.getInt("h"+id,7),m=prefs.getInt("m"+id,0);
 Switch sw=new Switch(this);sw.setText(String.format(Locale.getDefault(),"Alarma %d   %02d:%02d",id+1,h,m));sw.setTextColor(Color.WHITE);sw.setTextSize(20);sw.setChecked(prefs.getBoolean("on"+id,false));root.addView(sw);
 sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("on"+id,on).apply();schedule(this,id);if(on&&prefs.getInt("days"+id,127)==0)Toast.makeText(this,"Elegí al menos un día para esta alarma",Toast.LENGTH_LONG).show();});
 Button days=btn("Días de la alarma · "+(id+1));root.addView(days);days.setOnClickListener(v->{String[] labels={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};boolean[] checked=new boolean[7];int mask=prefs.getInt("days"+id,127);for(int j=0;j<7;j++)checked[j]=(mask&(1<<j))!=0;new AlertDialog.Builder(this).setTitle("Elegí los días").setMultiChoiceItems(labels,checked,(d,which,on)->checked[which]=on).setPositiveButton("Guardar",(d,w)->{int msk=0;for(int j=0;j<7;j++)if(checked[j])msk|=(1<<j);prefs.edit().putInt("days"+id,msk).apply();schedule(this,id);if(msk==0)Toast.makeText(this,"Sin días seleccionados: esta alarma no sonará",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancelar",null).show();});
 Button delete=btn("Eliminar alarma · "+(id+1));root.addView(delete);delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar alarma").setMessage("¿Querés eliminar esta alarma?").setPositiveButton("Eliminar",(d,w)->{prefs.edit().putBoolean("on"+id,false).putBoolean("created"+id,false).apply();schedule(this,id);render();}).setNegativeButton("Cancelar",null).show());
 Button edit=btn("Cambiar hora · "+(id+1));root.addView(edit);edit.setOnClickListener(v->new TimePickerDialog(this,(picker,hh,mm)->{prefs.edit().putInt("h"+id,hh).putInt("m"+id,mm).putBoolean("on"+id,true).apply();schedule(this,id);render();},h,m,true).show());
 }
 static void schedule(Context c,int id){
 SharedPreferences p=c.getSharedPreferences(PREF,0);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 Intent i=new Intent(c,AlarmReceiver.class).putExtra("id",id);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 am.cancel(pi);if(!p.getBoolean("on"+id,false))return;
 Calendar at=Calendar.getInstance();at.set(Calendar.HOUR_OF_DAY,p.getInt("h"+id,7));at.set(Calendar.MINUTE,p.getInt("m"+id,0));at.set(Calendar.SECOND,0);at.set(Calendar.MILLISECOND,0);int days=p.getInt("days"+id,127);if(days==0)return;boolean found=false;for(int offset=0;offset<8;offset++){Calendar candidate=(Calendar)at.clone();candidate.add(Calendar.DAY_OF_YEAR,offset);int mondayBased=(candidate.get(Calendar.DAY_OF_WEEK)+5)%7;if((days&(1<<mondayBased))!=0&&candidate.getTimeInMillis()>System.currentTimeMillis()){at=candidate;found=true;break;}}if(!found)return;
 if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){android.util.Log.w("Aurora","Permiso de alarmas exactas pendiente");return;}
 try{am.setAlarmClock(new AlarmManager.AlarmClockInfo(at.getTimeInMillis(),PendingIntent.getActivity(c,100+id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)),pi);}catch(SecurityException e){android.util.Log.e("Aurora","Exact alarm permission",e);}
 }
 void cancelTimer(){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);PendingIntent pi=PendingIntent.getBroadcast(this,3100,new Intent(this,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);prefs.edit().remove("timer_end").apply();}
 void countdown(int minutes,TextView output){
  if(timer!=null)timer.cancel();cancelTimer();
  long end=System.currentTimeMillis()+minutes*60000L;
  AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
  PendingIntent pi=PendingIntent.getBroadcast(this,3100,new Intent(this,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){new AlertDialog.Builder(this).setTitle("Permiso necesario").setMessage("Para avisarte a tiempo con la aplicación cerrada, habilitá las alarmas exactas.").setPositiveButton("Configurar",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())))).setNegativeButton("Cancelar",null).show();return;}
   am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,pi);prefs.edit().putLong("timer_end",end).apply();
  }catch(SecurityException ex){new AlertDialog.Builder(this).setMessage("Android no permitió programar el temporizador exacto.").setPositiveButton("Aceptar",null).show();return;}
  timer=new CountDownTimer(minutes*60000L,1000){public void onTick(long ms){output.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){output.setText("¡TIEMPO CUMPLIDO!");prefs.edit().remove("timer_end").apply();showTimerFinished();}}.start();
 }
 void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator()){long[] pat={0,800,200,800,200,800};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}}
 @Override protected void onPause(){stopHomeSpeech();super.onPause();}
 @Override protected void onDestroy(){stopHomeSpeech();if(timer!=null)timer.cancel();if(alarmActive)stopAlarmEffects();if(torch!=null)torch.stop();super.onDestroy();}
}