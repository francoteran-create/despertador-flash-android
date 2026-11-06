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
 void accent(Button b,int a,int z,int line){AuroraDesign.action(b,a,z,line);}
 Button btn(String s){Button b=new Button(this);b.setText(s);accent(b,0xff192b45,0xff222b50,0xff4d728e);return b;}
 TextView section(String s){TextView v=txt(s,21);v.setTextColor(0xffa5daf3);v.setTypeface(null,Typeface.BOLD);return v;}
 boolean pendingAlarmLaunch=false; boolean pendingTimerLaunch=false;
 SpeechRecognizer homeRecognizer; boolean homeListening=false; TextView homeTranscript,homeSpeechStatus; Button homeSpeechToggle;
 SenseHistory homeSpeechHistory;
 final Handler speechHandler=new Handler(Looper.getMainLooper());
 public void onCreate(Bundle b){
  super.onCreate(b);
  prefs=getSharedPreferences(PREF,0);torch=new TorchFlasher(this);
  pendingAlarmLaunch=getIntent().getBooleanExtra("ring",false);
  pendingTimerLaunch=getIntent().getBooleanExtra("timer_finished",false);
  if(Build.VERSION.SDK_INT>=27&&(pendingAlarmLaunch||pendingTimerLaunch)){
   setShowWhenLocked(true);setTurnScreenOn(true);
  }
  render();
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
 void releaseAlertWindow(){
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
   WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(false);setTurnScreenOn(false);}
 }
 void stopAlarmEffects(){
  if(torch!=null)torch.stop();
  alarmActive=false; activeAlarmId=-1;
  if(flashTask!=null){flashHandler.removeCallbacks(flashTask);flashTask=null;}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();
  releaseAlertWindow();
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)for(int id=0;id<5;id++)nm.cancel(2000+id);
 }
 boolean snoozeAlarm(){
  AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
  if(activeAlarmId<0){Toast.makeText(this,"La alarma de prueba no se puede posponer",Toast.LENGTH_LONG).show();return false;}
  Intent intent=new Intent(this,AlarmReceiver.class).putExtra("snooze",true).putExtra("id",activeAlarmId);
  PendingIntent pending=PendingIntent.getBroadcast(this,4100+activeAlarmId,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{long when=System.currentTimeMillis()+10*60000L;if(Build.VERSION.SDK_INT>=31&&!manager.canScheduleExactAlarms()){Toast.makeText(this,"Habilitá alarmas exactas para posponer",Toast.LENGTH_LONG).show();return false;}manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);prefs.edit().putLong("snooze_until"+activeAlarmId,when).apply();Toast.makeText(this,"Alarma pospuesta 10 minutos",Toast.LENGTH_LONG).show();return true;}catch(SecurityException ex){Toast.makeText(this,"No se pudo posponer: falta permiso de alarmas",Toast.LENGTH_LONG).show();return false;}
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
  new AlertDialog.Builder(this).setTitle("AURORA · TEMPORIZADOR").setView(message).setPositiveButton("DETENER ALERTA",(d,w)->{if(vib!=null)vib.cancel();if(torch!=null)torch.stop();timerAlertActive=false;releaseAlertWindow();}).setOnDismissListener(d->{if(vib!=null)vib.cancel();if(torch!=null)torch.stop();timerAlertActive=false;releaseAlertWindow();}).setCancelable(false).show();
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
  if(Build.VERSION.SDK_INT>=27&&(pendingAlarmLaunch||pendingTimerLaunch)){setShowWhenLocked(true);setTurnScreenOn(true);}
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
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if((requestCode==73||requestCode==74)&&grantResults.length>0){Toast.makeText(this,grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED?"Permiso concedido. Volvé a tocar Iniciar escucha continua.":"La escucha continua necesita ese permiso.",Toast.LENGTH_LONG).show();}if(requestCode==1&&grantResults.length>0&&grantResults[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Sin notificaciones, algunas alarmas podrían no mostrarse",Toast.LENGTH_LONG).show();}if(requestCode==44&&grantResults.length>0){if(grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startHomeSpeech();else Toast.makeText(this,"AURORA necesita permiso de micrófono para transcribir",Toast.LENGTH_LONG).show();}if(requestCode==2&&grantResults.length>0){boolean granted=grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;Toast.makeText(this,granted?"Flash LED autorizado. Probá una alarma de 1 minuto.":"Flash LED deshabilitado: falta permiso de Cámara.",Toast.LENGTH_LONG).show();if(!alarmActive&&!timerAlertActive)render();}}
 LinearLayout auroraCard(int first,int second){
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));
  AuroraDesign.panel(card,first,second);
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
  if(homeSpeechToggle!=null)homeSpeechToggle.setText("🎙  ESCUCHAR");
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
  // The home is intentionally full-bleed black, with NO card grid or scroll.
  android.widget.FrameLayout stage=new android.widget.FrameLayout(this);
  stage.setBackgroundColor(Color.BLACK);
  getWindow().setStatusBarColor(Color.BLACK);
  getWindow().setNavigationBarColor(Color.BLACK);
  AuroraCharacterStage actor=new AuroraCharacterStage(this,feature->{
   if(feature==AuroraCharacterStage.SOUNDS){openSense(SenseActivity.MODE_SOUND);}
   else if(feature==AuroraCharacterStage.VOICE){openSense(SenseActivity.MODE_SPEECH);}
   else if(feature==AuroraCharacterStage.ALARMS){alarmsPage=true;render();}
   else if(feature==AuroraCharacterStage.MESSAGES){stopHomeSpeech();communicationPage=true;render();}
   else if(feature==AuroraCharacterStage.PROFILE){stopHomeSpeech();startActivity(new Intent(this,ProfileActivity.class));}
   else if(feature==AuroraCharacterStage.RECORDS){stopHomeSpeech();startActivity(new Intent(this,RecordsActivity.class));}
   else if(feature==AuroraCharacterStage.TIMER){timerPage=true;render();}
   else if(feature==AuroraCharacterStage.SETTINGS){settingsPage=true;render();}
  });
  stage.addView(actor,new android.widget.FrameLayout.LayoutParams(-1,-1));
  TextView brand=new TextView(this);brand.setText("A U R O R A");
  brand.setTextColor(0xfff2f3f1);brand.setTextSize(29);
  brand.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
  brand.setLetterSpacing(.18f);brand.setGravity(Gravity.CENTER);
  brand.setPadding(0,dp(7),0,0);
  android.widget.FrameLayout.LayoutParams head=new android.widget.FrameLayout.LayoutParams(-1,dp(68),Gravity.TOP);
  stage.addView(brand,head);
  TextView hint=new TextView(this);hint.setText("TOCÁ UNA FUNCIÓN");
  hint.setTextColor(0xff879797);hint.setTextSize(11);
  hint.setLetterSpacing(.16f);hint.setGravity(Gravity.CENTER);
  android.widget.FrameLayout.LayoutParams foot=new android.widget.FrameLayout.LayoutParams(-1,dp(45),Gravity.BOTTOM);
  stage.addView(hint,foot);
  setContentView(stage);
 }
 LinearLayout homeTile(String symbol,String name,String subtitle,int top,int bottom){
  LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
  AuroraDesign.panel(tile,top,bottom);tile.setPadding(dp(16),dp(12),dp(12),dp(10));
  tile.setClickable(true);tile.setFocusable(true);
  TextView glyph=txt(symbol,34);glyph.setTextColor(0xff84ecf5);glyph.setPadding(0,0,0,dp(3));tile.addView(glyph);
  TextView title=txt(name,19);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));title.setPadding(0,0,0,0);tile.addView(title);
  TextView hint=txt(subtitle,12);hint.setTextColor(0xffbed5e9);hint.setPadding(0,dp(4),0,0);tile.addView(hint);
  return tile;
 }
 void renderTimerPage(){
  setupPage();pageTitle("TEMPORIZADOR");
  root.addView(AuroraDesign.overline(this,"CONTROL DE TIEMPO  /  AURORA"));
  LinearLayout timerCard=auroraCard(0xff132e41,0xff1c1f40);
  timerCard.setGravity(Gravity.CENTER_HORIZONTAL);
  TextView caption=txt("CUENTA REGRESIVA",14);caption.setTextColor(0xff94e9e9);
  caption.setGravity(Gravity.CENTER);timerCard.addView(caption);
  long savedEnd=prefs.getLong("timer_end",0);
  long remaining=TimerDeadline.remaining(savedEnd,prefs.getLong("timer_end_elapsed",0L),System.currentTimeMillis(),SystemClock.elapsedRealtime());
  TextView counter=txt(remaining>0?String.format(Locale.getDefault(),"%02d:%02d",remaining/60000,(remaining/1000)%60):"05:00",64);
  counter.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
  counter.setTextColor(0xfff4faff);counter.setGravity(Gravity.CENTER);counter.setLetterSpacing(.08f);
  timerCard.addView(counter);
  TextView counterInfo=txt(remaining>0?"CUENTA REGRESIVA ACTIVA":"LISTO PARA COMENZAR",14);
  counterInfo.setGravity(Gravity.CENTER);counterInfo.setTextColor(0xffa3d1eb);timerCard.addView(counterInfo);
  root.addView(timerCard);
  if(remaining>0){
   timer=new CountDownTimer(remaining,1000){
    public void onTick(long ms){counter.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}
    public void onFinish(){counter.setText("00:00");counterInfo.setText("¡TIEMPO CUMPLIDO!");prefs.edit().remove("timer_end").remove("timer_end_elapsed").apply();showTimerFinished();}
   }.start();
  }
  root.addView(AuroraDesign.overline(this,"INICIAR TEMPORIZADOR"));
  LinearLayout presets=new LinearLayout(this);presets.setOrientation(LinearLayout.HORIZONTAL);
  root.addView(presets);
  Button one=btn("01 MIN"),five=btn("05 MIN"),ten=btn("10 MIN");
  Button[] buttons={one,five,ten};
  for(int i=0;i<buttons.length;i++){
   accent(buttons[i],i==0?0xff204b63:0xff34305f,0xff252d4e,0xff77b5df);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(70),1f);
   if(i>0)lp.leftMargin=dp(6);
   presets.addView(buttons[i],lp);
  }
  one.setOnClickListener(v->{if(countdown(1,counter))counterInfo.setText("CUENTA REGRESIVA ACTIVA");});
  five.setOnClickListener(v->{if(countdown(5,counter))counterInfo.setText("CUENTA REGRESIVA ACTIVA");});
  ten.setOnClickListener(v->{if(countdown(10,counter))counterInfo.setText("CUENTA REGRESIVA ACTIVA");});
  Button stop=btn("■  DETENER TEMPORIZADOR");
  accent(stop,0xff66364d,0xff42283f,0xffaa6f91);
  root.addView(stop);stop.setOnClickListener(v->{if(timer!=null)timer.cancel();cancelTimer();counter.setText("00:00");counterInfo.setText("TEMPORIZADOR DETENIDO");});
  TextView foot=txt("Al terminar, AURORA te avisará mediante las alertas que tengas configuradas.",14);
  foot.setTextColor(0xffbed4e4);root.addView(foot);
 }
 void renderCommunicationPage(){
  setupPage();pageTitle("COMUNICACIÓN");
  root.addView(AuroraDesign.overline(this,"EXPRESATE DE LA FORMA QUE PREFIERAS"));
  TextView intro=txt("Mensajes claros, con un solo toque y sin depender del audio.",17);
  intro.setTextColor(0xffc6def0);root.addView(intro);
  LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);root.addView(quick);
  LinearLayout sound=homeTile("◖))","SONIDOS","Detectar ambiente",0xff102f43,0xff1a3551);
  LinearLayout speech=homeTile("◉","CONVERSAR","Voz a texto",0xff272041,0xff17374e);
  LinearLayout.LayoutParams half1=new LinearLayout.LayoutParams(0,dp(155),1f);half1.rightMargin=dp(9);
  quick.addView(sound,half1);quick.addView(speech,new LinearLayout.LayoutParams(0,dp(155),1f));
  sound.setOnClickListener(v->openSense(SenseActivity.MODE_SOUND));
  speech.setOnClickListener(v->openSense(SenseActivity.MODE_SPEECH));
  root.addView(AuroraDesign.overline(this,"MENSAJE PERSONALIZADO"));
  EditText msg=new EditText(this);msg.setTextColor(Color.WHITE);
  msg.setTextSize(20);msg.setMinLines(2);msg.setHintTextColor(0xffabc5d8);
  msg.setHint("Escribí lo que quieras comunicar");
  AuroraDesign.entry(msg);root.addView(msg);
  Button show=btn("MOSTRAR EN PANTALLA COMPLETA  ↗");
  accent(show,0xff19576b,0xff294768,0xff77e1e9);
  root.addView(show);show.setOnClickListener(v->{
   String message=msg.getText().toString().trim();
   if(!message.isEmpty())showFullMessage(message);
   else Toast.makeText(this,"Escribí un mensaje primero",Toast.LENGTH_SHORT).show();
  });
  root.addView(AuroraDesign.overline(this,"FRASES RÁPIDAS"));
  LinearLayout phrases=new LinearLayout(this);phrases.setOrientation(LinearLayout.VERTICAL);
  root.addView(phrases);
  phrasePair(phrases,"SÍ","NO","SÍ","NO");
  phrasePair(phrases,"NECESITO AYUDA","ESTOY BIEN","NECESITO AYUDA","ESTOY BIEN");
  phrasePair(phrases,"POR FAVOR, ESCRIBÍ","REPETÍ DESPACIO","POR FAVOR, ESCRIBÍ LO QUE DECÍS","POR FAVOR, REPETÍ MÁS DESPACIO");
  Button urgent=btn("¡LLAMÁ A EMERGENCIAS!");
  accent(urgent,0xff82373f,0xff5b263d,0xffdf848c);
  root.addView(urgent);urgent.setOnClickListener(v->showFullMessage("LLAMÁ A EMERGENCIAS"));
  root.addView(AuroraDesign.overline(this,"TU ESPACIO"));
  LinearLayout myTools=new LinearLayout(this);myTools.setOrientation(LinearLayout.HORIZONTAL);root.addView(myTools);
  Button profile=btn("◉  MI PERFIL");
  Button records=btn("📁  REGISTROS");
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(62),1f);lp.rightMargin=dp(9);myTools.addView(profile,lp);
  myTools.addView(records,new LinearLayout.LayoutParams(0,dp(62),1f));
  profile.setOnClickListener(v->startActivity(new Intent(this,ProfileActivity.class)));
  records.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)));
 }
 void phrasePair(LinearLayout parent,String left,String right,String first,String second){
  LinearLayout pair=new LinearLayout(this);pair.setOrientation(LinearLayout.HORIZONTAL);
  LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);margin.topMargin=dp(7);parent.addView(pair,margin);
  Button a=btn(left),b=btn(right);
  accent(a,0xff1c4656,0xff263859,0xff7ac8d6);
  accent(b,0xff2e3156,0xff27324e,0xff8c9cd3);
  LinearLayout.LayoutParams leftSize=new LinearLayout.LayoutParams(0,dp(77),1f);leftSize.rightMargin=dp(8);
  pair.addView(a,leftSize);pair.addView(b,new LinearLayout.LayoutParams(0,dp(77),1f));
  a.setOnClickListener(v->showFullMessage(first));
  b.setOnClickListener(v->showFullMessage(second));
 }
 void startBackgroundMonitor(){
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   new AlertDialog.Builder(this).setTitle("Micrófono necesario")
    .setMessage("AURORA solo puede escuchar en segundo plano con tu autorización. El micrófono se indica mediante una notificación permanente y podés detenerlo cuando quieras.")
    .setPositiveButton("Autorizar",(d,w)->requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},73))
    .setNegativeButton("Cancelar",null).show();return;
  }
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},74);
   Toast.makeText(this,"Concedé notificaciones y volvé a activar la escucha",Toast.LENGTH_LONG).show();
   return;
  }
  new AlertDialog.Builder(this).setTitle("Activar escucha del entorno")
   .setMessage("Mientras esté activa, AURORA usa el micrófono y consume batería. Solo registra las categorías detectadas, no guarda audio. No sustituye equipos de seguridad certificados.")
   .setPositiveButton("ACTIVAR",(d,w)->{
    Intent it=new Intent(this,AuroraSoundMonitorService.class).setAction(AuroraSoundMonitorService.START);
    try{if(Build.VERSION.SDK_INT>=26)startForegroundService(it);else startService(it);
     Toast.makeText(this,"Iniciando escucha. Verificá la notificación permanente.",Toast.LENGTH_LONG).show();
    }catch(Exception e){new AlertDialog.Builder(this).setMessage("Android no permitió iniciar la escucha: "+e.getMessage()).setPositiveButton("OK",null).show();}
   }).setNegativeButton("Cancelar",null).show();
 }
 void stopBackgroundMonitor(){
  Intent stop=new Intent(this,AuroraSoundMonitorService.class).setAction(AuroraSoundMonitorService.STOP);
  try{startService(stop);}catch(Exception e){stopService(new Intent(this,AuroraSoundMonitorService.class));}
  prefs.edit().putBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false).apply();
 }
 void renderSettingsPage(){
  setupPage();pageTitle("CONFIGURACIÓN Y PRUEBAS");
  root.addView(section("ESCUCHA DEL ENTORNO EN SEGUNDO PLANO"));
  root.addView(txt("Activala solo cuando la necesites. Android mostrará una notificación permanente mientras se usa el micrófono. La escucha se detiene al apagarla y no se reactiva sola al reiniciar.",15));
  Button bgStart=btn("◉  INICIAR ESCUCHA CONTINUA");
  AuroraDesign.action(bgStart,0xff18524c,0xff224845,0xff61c7b7);
  root.addView(bgStart);bgStart.setOnClickListener(v->startBackgroundMonitor());
  Button bgStop=btn("■  DETENER ESCUCHA CONTINUA");root.addView(bgStop);
  bgStop.setOnClickListener(v->{stopBackgroundMonitor();Toast.makeText(this,"Deteniendo escucha",Toast.LENGTH_SHORT).show();});
  root.addView(txt("Última detección: "+prefs.getString(AuroraSoundMonitorService.KEY_LAST,"Sin eventos"),14));
  long beat=prefs.getLong(AuroraSoundMonitorService.KEY_HEARTBEAT,0L);
  boolean marked=prefs.getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false);
  boolean recent=beat>0&&Math.abs(System.currentTimeMillis()-beat)<45000L;
  root.addView(txt("Estado del detector: "+(marked&&recent?"ACTIVO (confirmación reciente)":marked?"SIN CONFIRMACIÓN RECIENTE":"DETENIDO"),15));
  String monitorFailure=prefs.getString(AuroraSoundMonitorService.KEY_ERROR,"");
  if(!monitorFailure.isEmpty()){TextView failed=txt("Última interrupción: "+monitorFailure,15);failed.setTextColor(0xffffc47a);root.addView(failed);}

  root.addView(section("AVISOS POR TIPO DE SONIDO"));
  for(int cat=0;cat<SoundAlertPreferences.KEYS.length;cat++){
   String key=SoundAlertPreferences.KEYS[cat];
   android.widget.Switch check=new android.widget.Switch(this);
   check.setText(SoundAlertPreferences.LABELS[cat]);
   check.setTextColor(Color.WHITE);check.setTextSize(16);
   check.setPadding(dp(10),dp(10),dp(10),dp(10));
   check.setChecked(prefs.getBoolean("monitor_alert_"+key,!"other".equals(key)));
   root.addView(check);
   check.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("monitor_alert_"+key,on).apply());
  }
  root.addView(txt("Las categorías que no marques pueden seguir siendo detectadas, pero no generarán un aviso emergente. La clasificación es aproximada.",13));
  root.addView(section("SENSIBILIDAD DE ALERTAS"));
  final String[] levels={"ALTA","EQUILIBRADA","PRECISA"};
  android.widget.RadioGroup rg=new android.widget.RadioGroup(this);rg.setOrientation(0);
  for(int i=0;i<3;i++){
   android.widget.RadioButton b=new android.widget.RadioButton(this);
   b.setText(levels[i]);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setId(600+i);
   rg.addView(b,new android.widget.RadioGroup.LayoutParams(0,dp(55),1));
  }
  rg.check(600+Math.max(0,Math.min(2,prefs.getInt("monitor_sensitivity",1))));
  rg.setOnCheckedChangeListener((group,id)->prefs.edit().putInt("monitor_sensitivity",id-600).apply());
  root.addView(rg);

  TextView motionTitle=section("AURORA VISUAL ENGINE");root.addView(motionTitle);
  android.widget.Switch motion=new android.widget.Switch(this);motion.setText("Auroras y núcleo animados");
  motion.setTextColor(Color.WHITE);motion.setTextSize(17);motion.setPadding(dp(12),dp(12),dp(12),dp(12));
  motion.setChecked(prefs.getBoolean("aurora_motion",true));
  root.addView(motion);motion.setOnCheckedChangeListener((control,enabled)->{
   prefs.edit().putBoolean("aurora_motion",enabled).apply();
   Toast.makeText(this,enabled?"Animaciones habilitadas":"Animaciones reducidas",Toast.LENGTH_SHORT).show();
  });
  root.addView(txt("Los efectos se reducen automáticamente si Android tiene activada la opción de reducir animaciones o el modo ahorro de batería.",13));
  android.widget.Switch liteGraphics=new android.widget.Switch(this);
  liteGraphics.setText("Modo fluido para teléfonos anteriores");
  liteGraphics.setTextColor(Color.WHITE);liteGraphics.setTextSize(17);
  liteGraphics.setPadding(dp(12),dp(12),dp(12),dp(12));
  liteGraphics.setChecked(prefs.getBoolean("aurora_smooth_mode",true));
  root.addView(liteGraphics);
  liteGraphics.setOnCheckedChangeListener((v,enabled)->{
   prefs.edit().putBoolean("aurora_smooth_mode",enabled).apply();
   Toast.makeText(this,"La preferencia se aplicará al volver a abrir la pantalla",Toast.LENGTH_SHORT).show();
  });
  root.addView(txt("Recomendado para Galaxy S9+: menos partículas y menos cuadros por segundo, manteniendo las auroras y la respuesta a sonidos.",13));

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
  setupPage();pageTitle("MIS ALARMAS");
  root.addView(AuroraDesign.overline(this,"DESPERTADOR  /  MIS HORARIOS"));
  TextView intro=txt("Elegí tus horarios y días. Cada alarma se puede activar, editar o eliminar.",16);
  intro.setTextColor(0xffc2d7ea);root.addView(intro);
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
 LinearLayout card=auroraCard(0xff16283f,0xff211f3e);
 card.setPadding(dp(16),dp(15),dp(16),dp(13));
 TextView mark=AuroraDesign.overline(this,"ALARMA  "+(id+1));card.addView(mark);
 long next=prefs.getBoolean("on"+id,false)?
  AlarmPlanner.nextTrigger(System.currentTimeMillis(),h,m,prefs.getInt("days"+id,127),TimeZone.getDefault()):-1L;
 TextView nextLabel=txt(next>0?"Próxima fecha prevista: "+
  new java.text.SimpleDateFormat("EEE dd/MM · HH:mm",new Locale("es","AR")).format(new Date(next))+
  " (según los permisos de Android)":prefs.getBoolean("on"+id,false)?"Sin días seleccionados":"Alarma desactivada",14);
 nextLabel.setTextColor(0xffbbd5e6);card.addView(nextLabel);
 Switch sw=new Switch(this);sw.setText(String.format(Locale.getDefault(),"Alarma %d   %02d:%02d",id+1,h,m));sw.setTextColor(Color.WHITE);sw.setTextSize(25);sw.setPadding(dp(9),dp(8),dp(9),dp(8));sw.setChecked(prefs.getBoolean("on"+id,false));card.addView(sw);
 sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("on"+id,on).apply();schedule(this,id);if(on&&prefs.getInt("days"+id,127)==0)Toast.makeText(this,"Elegí al menos un día para esta alarma",Toast.LENGTH_LONG).show();});
 Button days=btn("Días de la alarma · "+(id+1));card.addView(days);days.setOnClickListener(v->{String[] labels={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};boolean[] checked=new boolean[7];int mask=prefs.getInt("days"+id,127);for(int j=0;j<7;j++)checked[j]=(mask&(1<<j))!=0;new AlertDialog.Builder(this).setTitle("Elegí los días").setMultiChoiceItems(labels,checked,(d,which,on)->checked[which]=on).setPositiveButton("Guardar",(d,w)->{int msk=0;for(int j=0;j<7;j++)if(checked[j])msk|=(1<<j);prefs.edit().putInt("days"+id,msk).apply();cancelSnooze(this,id);schedule(this,id);if(msk==0)Toast.makeText(this,"Sin días seleccionados: esta alarma no sonará",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancelar",null).show();});
 Button delete=btn("Eliminar alarma · "+(id+1));card.addView(delete);delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar alarma").setMessage("¿Querés eliminar esta alarma?").setPositiveButton("Eliminar",(d,w)->{prefs.edit().putBoolean("on"+id,false).putBoolean("created"+id,false).apply();schedule(this,id);render();}).setNegativeButton("Cancelar",null).show());
 Button edit=btn("Cambiar hora · "+(id+1));card.addView(edit);edit.setOnClickListener(v->new TimePickerDialog(this,(picker,hh,mm)->{prefs.edit().putInt("h"+id,hh).putInt("m"+id,mm).putBoolean("on"+id,true).apply();cancelSnooze(this,id);schedule(this,id);render();},h,m,true).show());
  root.addView(card);
 }
 static void cancelSnooze(Context c,int id){
  if(id<0||id>=5)return;
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  Intent snooze=new Intent(c,AlarmReceiver.class).putExtra("snooze",true).putExtra("id",id);
  PendingIntent pending=PendingIntent.getBroadcast(c,4100+id,snooze,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  if(am!=null)am.cancel(pending);
  c.getSharedPreferences(PREF,0).edit().remove("snooze_until"+id).apply();
 }
 static void schedule(Context c,int id){
  if(id<0||id>=5)return;
  SharedPreferences p=c.getSharedPreferences(PREF,0);
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  if(am==null)return;
  Intent alarm=new Intent(c,AlarmReceiver.class).putExtra("id",id);
  PendingIntent event=PendingIntent.getBroadcast(c,id,alarm,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  am.cancel(event);
  if(!p.getBoolean("on"+id,false)){
   cancelSnooze(c,id);
   return;
  }
  long when=AlarmPlanner.nextTrigger(System.currentTimeMillis(),p.getInt("h"+id,7),
   p.getInt("m"+id,0),p.getInt("days"+id,127),TimeZone.getDefault());
  if(when<0)return;
  if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){
   android.util.Log.w("Aurora","Falta permiso de alarmas exactas");return;
  }
  PendingIntent show=PendingIntent.getActivity(c,100+id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),event);}
  catch(SecurityException ex){android.util.Log.e("Aurora","No se pudo programar alarma exacta",ex);}
 }
 void cancelTimer(){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);PendingIntent pi=PendingIntent.getBroadcast(this,3100,new Intent(this,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);prefs.edit().remove("timer_end").remove("timer_end_elapsed").apply();}
 boolean countdown(int minutes,TextView output){
  if(minutes<1||minutes>24*60)return false;
  if(timer!=null)timer.cancel();
  cancelTimer();
  long endWall=System.currentTimeMillis()+minutes*60000L;
  long endElapsed=SystemClock.elapsedRealtime()+minutes*60000L;
  AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
  if(am==null)return false;
  PendingIntent pi=PendingIntent.getBroadcast(this,3100,new Intent(this,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){
    new AlertDialog.Builder(this).setTitle("Permiso necesario")
     .setMessage("Para avisarte a tiempo con la aplicación cerrada, habilitá las alarmas exactas.")
     .setPositiveButton("Configurar",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName()))))
     .setNegativeButton("Cancelar",null).show();
    return false;
   }
   // A monotonic elapsed deadline remains accurate even if the user changes the clock.
   am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,endWall,pi);
   prefs.edit().putLong("timer_end",endWall).putLong("timer_end_elapsed",endElapsed).apply();
  }catch(SecurityException ex){
   new AlertDialog.Builder(this).setMessage("Android no permitió programar el temporizador exacto.").setPositiveButton("Aceptar",null).show();return false;
  }
  timer=new CountDownTimer(minutes*60000L,1000){
   public void onTick(long ms){output.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}
   public void onFinish(){
    output.setText("¡TIEMPO CUMPLIDO!");
    prefs.edit().remove("timer_end").remove("timer_end_elapsed").apply();
    showTimerFinished();
   }
  }.start();
  return true;
 }
 void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator()){long[] pat={0,800,200,800,200,800};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}}
 @Override protected void onPause(){stopHomeSpeech();super.onPause();}
 @Override protected void onDestroy(){stopHomeSpeech();if(timer!=null)timer.cancel();if(alarmActive)stopAlarmEffects();if(torch!=null)torch.stop();super.onDestroy();}
}