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
 private static final int EXPORT_SETTINGS=9201,IMPORT_SETTINGS=9202;
 static final String EXTRA_SETTINGS_SECTION="aurora_settings_section";
 private String openedSettingsSection;
 LinearLayout root; SharedPreferences prefs; CountDownTimer timer; android.os.Handler flashHandler=new android.os.Handler(android.os.Looper.getMainLooper()); Runnable flashTask; boolean alarmActive=false; boolean timerAlertActive=false; int activeAlarmId=-1; TorchFlasher torch; boolean alarmsPage=false; boolean timerPage=false; boolean communicationPage=false; boolean settingsPage=false;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(5),dp(12),dp(5),dp(12));return v;}
 GradientDrawable auroraBackground(int a,int z,int stroke){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,z});d.setCornerRadius(dp(20));d.setStroke(dp(1),stroke);return d;}
 void accent(Button b,int a,int z,int line){AuroraDesign.action(b,a,z,line);}
 Button btn(String s){Button b=new Button(this);b.setText(s);AuroraDesign.neonAction(b);return b;}
 TextView section(String s){TextView v=txt(s,21);v.setTag("aurora_settings_heading");v.setTextColor(0xffa5daf3);v.setTypeface(null,Typeface.BOLD);return v;}
 // Retain existing controls and listeners; group them only in the settings page.
 private void collapseSettingsSections(int start){
  java.util.ArrayList<View> items=new java.util.ArrayList<>();
  while(root.getChildCount()>start){View child=root.getChildAt(start);root.removeViewAt(start);items.add(child);}
  LinearLayout body=null;
  for(View item:items){
   if(item instanceof TextView&&"aurora_settings_heading".equals(item.getTag())){
    String name=((TextView)item).getText().toString();
    Button header=btn("＋  "+name);root.addView(header);
    LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(dp(5),dp(3),dp(5),dp(10));group.setVisibility(name.equals(openedSettingsSection)?View.VISIBLE:View.GONE);root.addView(group);
    if(group.getVisibility()==View.VISIBLE){header.setText("−  "+name);header.post(()->header.requestRectangleOnScreen(new android.graphics.Rect(0,0,header.getWidth(),header.getHeight()),false));}
    header.setOnClickListener(v->{boolean open=group.getVisibility()!=View.VISIBLE;group.setVisibility(open?View.VISIBLE:View.GONE);header.setText((open?"−  ":"＋  ")+name);});
    body=group;
   }else if(body!=null)body.addView(item);else root.addView(item);
  }
 }
 private final AlarmTone alarmPreview=new AlarmTone();
 private static final int STARTUP_PERMISSIONS=4210;
 private boolean startupPermissionDialog=false;
 private boolean firstSetupScheduled=false;
 private boolean neonWelcomeStarted=false;
 private android.widget.ImageView profileShortcut;
 private TextView profileShortcutLabel;
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
  if(!pendingAlarmLaunch&&!pendingTimerLaunch&&getIntent().hasExtra(EXTRA_SETTINGS_SECTION)){
   settingsPage=true;
   openedSettingsSection=getIntent().getStringExtra(EXTRA_SETTINGS_SECTION);
  }
  render();
 }
 @Override protected void onPostResume(){
  super.onPostResume();
  if(pendingAlarmLaunch){
   pendingAlarmLaunch=false;
   showAlarm(getIntent().getIntExtra("alarm_id",-1));
  }else if(pendingTimerLaunch){
   pendingTimerLaunch=false;
   showTimerFinished();
  }else if(!alarmActive&&!timerAlertActive&&!neonWelcomeStarted&&NeonIdentity.shouldWelcome(this)){
   neonWelcomeStarted=true;
   startActivity(new Intent(this,NeonWelcomeActivity.class));
  }else if(!alarmActive&&!timerAlertActive&&!firstSetupScheduled&&!prefs.getBoolean("permissions_setup_429",false)){
   firstSetupScheduled=true;
   flashHandler.post(()->{if(!isFinishing()&&!alarmActive&&!timerAlertActive)showPermissionSetup(true);});
  }
 }
 boolean hasPermission(String permission){return checkSelfPermission(permission)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
 boolean exactReady(){AlarmManager a=getSystemService(AlarmManager.class);return a!=null&&(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms());}
 boolean notificationsReady(){NotificationManager n=getSystemService(NotificationManager.class);return n!=null&&n.areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||hasPermission(android.Manifest.permission.POST_NOTIFICATIONS));}
 void openPermissionSettings(String action){
  try{startActivity(new Intent(action,android.net.Uri.parse("package:"+getPackageName())));}
  catch(RuntimeException e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}
 }
 void showPermissionSetup(boolean requestMissing){
  if(startupPermissionDialog||isFinishing()||alarmActive||timerAlertActive)return;
  java.util.ArrayList<String> missing=new java.util.ArrayList<>();
  if(!hasPermission(android.Manifest.permission.CAMERA))missing.add(android.Manifest.permission.CAMERA);
  if(!hasPermission(android.Manifest.permission.RECORD_AUDIO))missing.add(android.Manifest.permission.RECORD_AUDIO);
  if(Build.VERSION.SDK_INT>=33&&!hasPermission(android.Manifest.permission.POST_NOTIFICATIONS))missing.add(android.Manifest.permission.POST_NOTIFICATIONS);
  if(requestMissing&&!missing.isEmpty()){
   startupPermissionDialog=true;
   new AlertDialog.Builder(this).setTitle("Prepará AURORA")
    .setMessage("Cámara: necesaria para el flash LED. Micrófono: detección de sonidos, música y transcripción. Notificaciones: avisos. Autorizar no inicia ninguna captura. Podés rechazar permisos y cambiarlos luego.")
    .setPositiveButton("SOLICITAR PERMISOS",(d,w)->{startupPermissionDialog=false;requestPermissions(missing.toArray(new String[0]),STARTUP_PERMISSIONS);})
    .setNegativeButton("AHORA NO",(d,w)->{startupPermissionDialog=false;showPermissionSetup(false);})
    .setOnCancelListener(d->{startupPermissionDialog=false;showPermissionSetup(false);}).show();
   return;
  }
  startupPermissionDialog=true;
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(15),dp(8),dp(15),dp(8));
  panel.addView(txt("Cámara / flash: "+(hasPermission(android.Manifest.permission.CAMERA)?"AUTORIZADO":"FALTA PERMISO"),16));
  panel.addView(txt("Micrófono: "+(hasPermission(android.Manifest.permission.RECORD_AUDIO)?"AUTORIZADO":"FALTA PERMISO"),16));
  panel.addView(txt("Notificaciones: "+(notificationsReady()?"HABILITADAS":"BLOQUEADAS"),16));
  panel.addView(txt("Alarmas exactas: "+(exactReady()?"PERMITIDAS":"FALTA AUTORIZAR"),16));
  NotificationManager nm=getSystemService(NotificationManager.class);
  if(Build.VERSION.SDK_INT>=34)panel.addView(txt("Pantalla completa: "+(nm!=null&&nm.canUseFullScreenIntent()?"PERMITIDA":"RESTRINGIDA"),16));
  panel.addView(txt("La ubicación para SOS es opcional y se pide únicamente cuando se usa. No se graba audio ni se enciende el flash al conceder permisos.",14));
  if(!missing.isEmpty()){Button fix=btn("ABRIR PERMISOS DE LA APP");panel.addView(fix);fix.setOnClickListener(v->openPermissionSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));}
  if(!notificationsReady()){Button fix=btn("AJUSTES DE NOTIFICACIONES");panel.addView(fix);fix.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=26)startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));else openPermissionSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);});}
  if(Build.VERSION.SDK_INT>=31&&!exactReady()){Button fix=btn("AUTORIZAR ALARMAS EXACTAS");panel.addView(fix);fix.setOnClickListener(v->openPermissionSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));}
  if(Build.VERSION.SDK_INT>=34&&nm!=null&&!nm.canUseFullScreenIntent()){Button fix=btn("AUTORIZAR PANTALLA COMPLETA");panel.addView(fix);fix.setOnClickListener(v->openPermissionSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT));}
  Button battery=btn("REVISAR AHORRO DE BATERÍA");panel.addView(battery);battery.setOnClickListener(v->openPermissionSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Estado de permisos").setView(scroll)
   .setPositiveButton("CONTINUAR",(d,w)->prefs.edit().putBoolean("permissions_setup_429",true).apply())
   .setNeutralButton("ACTUALIZAR",null).create();
  dialog.setOnDismissListener(d->startupPermissionDialog=false);
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dialog.dismiss();showPermissionSetup(false);}));
  dialog.show();
 }
 void startTorchAlert(){if(prefs.getBoolean("torch_alert",true)){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Para usar el flash LED, autorizá Cámara en AURORA",Toast.LENGTH_LONG).show();return;}if(torch!=null&&!torch.start())Toast.makeText(this,"Flash LED: "+torch.getLastError(),Toast.LENGTH_LONG).show();}}
 void releaseAlertWindow(){
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
   WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(false);setTurnScreenOn(false);}
 }
 void stopAlarmEffects(){
  if(torch!=null)torch.stop();
  int finishedAlarm=activeAlarmId;
  alarmPreview.stop();
  if(finishedAlarm>=0&&AlarmEffectsService.activeId==finishedAlarm)stopService(new Intent(this,AlarmEffectsService.class));
  alarmActive=false; activeAlarmId=-1;
  if(flashTask!=null){flashHandler.removeCallbacks(flashTask);flashTask=null;}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();
  releaseAlertWindow();
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(nm!=null&&finishedAlarm>=0&&finishedAlarm<5)nm.cancel(2000+finishedAlarm);
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
 void showAlarm(){showAlarm(-1);} // A manual preview never snoozes a real alarm.
 void showAlarm(int notificationAlarmId){
  if(alarmActive)return;
  alarmActive=true;
  activeAlarmId=(notificationAlarmId>=0&&notificationAlarmId<5)?notificationAlarmId:-1;
  boolean serviceOwns=activeAlarmId>=0&&AlarmEffectsService.activeId==activeAlarmId;
  if(!serviceOwns){if(activeAlarmId<0)startTorchAlert();else if(prefs.getBoolean("alarm_flash"+activeAlarmId,prefs.getBoolean("torch_alert",true))&&torch!=null)torch.start();}
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(24),dp(22),dp(24));
  TextView big=txt("¡DESPERTÁ!",38);big.setGravity(Gravity.CENTER);big.setTypeface(null,Typeface.BOLD);panel.addView(big);
  TextView sub=txt("AURORA está avisándote. Tocá el botón para detener la alerta.",19);sub.setGravity(Gravity.CENTER);panel.addView(sub);
  final boolean[] bright={false};
  flashTask=new Runnable(){public void run(){bright[0]=!bright[0];panel.setBackgroundColor(bright[0]?0xffffffff:0xff10182e);big.setTextColor(bright[0]?0xff08162c:0xffffffff);sub.setTextColor(bright[0]?0xff08162c:0xffffffff);flashHandler.postDelayed(this,650);}};
  if(prefs.getBoolean("visual_flash",false)&&!isFinishing())flashHandler.post(flashTask);else{panel.setBackgroundColor(0xff10182e);big.setTextColor(Color.WHITE);sub.setTextColor(Color.WHITE);}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  if(!serviceOwns&&(activeAlarmId<0||prefs.getBoolean("alarm_vibration"+activeAlarmId,true))&&v!=null&&v.hasVibrator()){long[] pattern={0,900,300,900,300,900,300};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pattern,0));else v.vibrate(pattern,0);}
  if(!serviceOwns&&activeAlarmId>=0&&prefs.getBoolean("alarm_sound"+activeAlarmId,false))alarmPreview.play(prefs.getInt("alarm_tone"+activeAlarmId,0),prefs.getInt("alarm_volume"+activeAlarmId,75),true);
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
   if(pendingAlarmLaunch){pendingAlarmLaunch=false;showAlarm(i.getIntExtra("alarm_id",-1));}
   else if(pendingTimerLaunch){pendingTimerLaunch=false;showTimerFinished();}
  }
 }
 @Override public void onWindowFocusChanged(boolean hasFocus){
  super.onWindowFocusChanged(hasFocus);
  if(hasFocus&&!alarmActive&&!timerAlertActive){
   if(pendingAlarmLaunch){pendingAlarmLaunch=false;showAlarm(getIntent().getIntExtra("alarm_id",-1));}
   else if(pendingTimerLaunch){pendingTimerLaunch=false;showTimerFinished();}
  }
 }
 @Override protected void onResume(){super.onResume();refreshProfileShortcut();if(prefs!=null){for(int id=0;id<5;id++){if(prefs.getBoolean("on"+id,false)){long armed=prefs.getLong("armed_at"+id,0L);if(armed==0L||armed<System.currentTimeMillis()-120000L)schedule(this,id);}}}}
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==STARTUP_PERMISSIONS){startupPermissionDialog=false;showPermissionSetup(false);return;}if((requestCode==73||requestCode==74)&&grantResults.length>0){Toast.makeText(this,grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED?"Permiso concedido. Volvé a tocar Iniciar escucha continua.":"La escucha continua necesita ese permiso.",Toast.LENGTH_LONG).show();}if(requestCode==1&&grantResults.length>0&&grantResults[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Sin notificaciones, algunas alarmas podrían no mostrarse",Toast.LENGTH_LONG).show();}if(requestCode==44&&grantResults.length>0){if(grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startHomeSpeech();else Toast.makeText(this,"AURORA necesita permiso de micrófono para transcribir",Toast.LENGTH_LONG).show();}if(requestCode==2&&grantResults.length>0){boolean granted=grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED;Toast.makeText(this,granted?"Flash LED autorizado. Probá una alarma de 1 minuto.":"Flash LED deshabilitado: falta permiso de Cámara.",Toast.LENGTH_LONG).show();if(!alarmActive&&!timerAlertActive)render();}}
 private void exportSettings(){
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("text/plain");
  String day=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
  intent.putExtra(Intent.EXTRA_TITLE,"AURORA_CONFIGURACION_"+day+".txt");
  try{startActivityForResult(intent,EXPORT_SETTINGS);}
  catch(Exception problem){new AlertDialog.Builder(this).setTitle("Respaldo no disponible")
   .setMessage("Android no permitió elegir un archivo: "+problem.getMessage())
   .setPositiveButton("ENTENDIDO",null).show();}
 }
 private void importSettings(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("text/plain");
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(intent,IMPORT_SETTINGS);}
  catch(Exception problem){new AlertDialog.Builder(this).setTitle("Restauración no disponible")
   .setMessage("Android no permitió abrir un archivo: "+problem.getMessage())
   .setPositiveButton("ENTENDIDO",null).show();}
 }
 @Override protected void onActivityResult(int request,int response,Intent result){
  super.onActivityResult(request,response,result);
  if(response!=RESULT_OK||result==null||result.getData()==null)return;
  if(request==EXPORT_SETTINGS){
   try{
    String backup=AuroraSettingsBackup.serialize(prefs.getAll());
    try(java.io.OutputStream output=getContentResolver().openOutputStream(result.getData(),"wt")){
     if(output==null)throw new java.io.IOException("No se pudo abrir el destino");
     output.write(backup.getBytes(java.nio.charset.StandardCharsets.UTF_8));
     output.flush();
    }
    Toast.makeText(this,"Copia de alarmas y ajustes guardada",Toast.LENGTH_LONG).show();
   }catch(Exception error){new AlertDialog.Builder(this).setTitle("No se guardó el respaldo")
    .setMessage(error.getMessage()).setPositiveButton("ENTENDIDO",null).show();}
  }else if(request==IMPORT_SETTINGS){
   try{
    java.io.ByteArrayOutputStream data=new java.io.ByteArrayOutputStream();
    try(java.io.InputStream input=getContentResolver().openInputStream(result.getData())){
     if(input==null)throw new java.io.IOException("No se pudo leer el archivo");
     byte[] block=new byte[2048];int n;
     while((n=input.read(block))!=-1){
      if(data.size()+n>AuroraSettingsBackup.MAX_IMPORT_BYTES)
       throw new java.io.IOException("La copia supera el límite de seguridad");
      data.write(block,0,n);
     }
    }
    final Map<String,Object> recovered=AuroraSettingsBackup.parse(
     data.toString("UTF-8"));
    int alarmCount=AuroraSettingsBackup.configuredAlarmCount(recovered);
    new AlertDialog.Builder(this)
     .setTitle("Confirmar restauración de AURORA")
     .setMessage("Se encontraron "+alarmCount+" alarmas configuradas. "
      +"Esta operación reemplazará las 5 posiciones de alarmas, sus ajustes de flash, vibración, sonido, tono, volumen y duración, y las preferencias de notificación y sensibilidad. "
      +"Los registros y el perfil NO serán modificados. ¿Querés continuar?")
     .setNegativeButton("CANCELAR",null)
     .setPositiveButton("RESTAURAR",(dialog,choice)->applyRestoredSettings(recovered))
     .show();
   }catch(Exception error){new AlertDialog.Builder(this).setTitle("No se pudo restaurar")
    .setMessage("El archivo no es una copia válida de AURORA o está incompleto: "+error.getMessage())
    .setPositiveButton("ENTENDIDO",null).show();}
  }
 }
 private void applyRestoredSettings(Map<String,Object> backup){
  SharedPreferences.Editor edit=prefs.edit();
  for(Map.Entry<String,Object> preference:backup.entrySet()){
   Object value=preference.getValue();
   if(value instanceof Boolean)edit.putBoolean(preference.getKey(),(Boolean)value);
   else if(value instanceof Integer)edit.putInt(preference.getKey(),(Integer)value);
  }
  if(!edit.commit()){
   new AlertDialog.Builder(this).setTitle("Restauración cancelada")
    .setMessage("Android no pudo guardar los ajustes. No se reprogramaron alarmas.")
    .setPositiveButton("ENTENDIDO",null).show();
   return;
  }
  for(int id=0;id<5;id++){cancelSnooze(this,id);schedule(this,id);}
  Toast.makeText(this,"Ajustes restaurados, incluidos efectos individuales. Revisá permisos y alarmas.",Toast.LENGTH_LONG).show();
  settingsPage=true;render();
 }

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
     try{SavedFolderManager copies=new SavedFolderManager(this);if(copies.selectedFolder()!=null)copies.copyOne(saved);}catch(java.io.IOException copyError){android.util.Log.w("Aurora","No se pudo copiar el archivo a carpeta elegida",copyError);}
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
  profileShortcut=null;
  if(timer!=null){timer.cancel();timer=null;}
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(26));
  root.setBackgroundColor(Color.TRANSPARENT);
  scroll.addView(root);setContentView(AuroraDesign.menuFrame(this,scroll));
  getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
 }
 void pageTitle(String heading){
  Button back=btn("←   VOLVER AL INICIO");root.addView(back);back.setOnClickListener(v->{alarmsPage=false;timerPage=false;communicationPage=false;settingsPage=false;render();});
  TextView title=txt(heading,27);title.setGravity(Gravity.CENTER);title.setTextColor(0xffffd633);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 }
 void render(){
  if(alarmsPage){stopHomeSpeech();renderAlarmsPage();return;}
  if(timerPage){stopHomeSpeech();renderTimerPage();return;}
  if(communicationPage){stopHomeSpeech();renderCommunicationPage();return;}
  if(settingsPage){stopHomeSpeech();renderSettingsPage();return;}
  renderMinimalHero();
 }
 /**
  * One full-black home screen. Every cloud is a genuine focusable navigation
  * action. Alarm and communication screens retain their existing logic.
  */
 void renderMinimalHero(){
  stopHomeSpeech();
  if(timer!=null){timer.cancel();timer=null;}
  android.widget.FrameLayout screen=new android.widget.FrameLayout(this);
  screen.setBackgroundColor(Color.BLACK);
  android.widget.ImageView mural=new android.widget.ImageView(this);
  mural.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
  mural.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  try(java.io.InputStream input=getAssets().open("aurora_mural.jpg.b64")){
   java.io.ByteArrayOutputStream encoded=new java.io.ByteArrayOutputStream();
   byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)encoded.write(buffer,0,n);
   byte[] image=android.util.Base64.decode(encoded.toByteArray(),android.util.Base64.DEFAULT);
   android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(image,0,image.length);
   if(bitmap!=null)mural.setImageBitmap(bitmap);
  }catch(Exception e){android.util.Log.w("Aurora","Mural no disponible",e);}
  screen.addView(mural,new android.widget.FrameLayout.LayoutParams(-1,-1));
  View shade=new View(this);shade.setBackgroundColor(0x39000000);
  screen.addView(shade,new android.widget.FrameLayout.LayoutParams(-1,-1));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(13),dp(13),dp(13),dp(22));
  scroll.addView(hero);screen.addView(scroll,new android.widget.FrameLayout.LayoutParams(-1,-1));setContentView(screen);
  getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
  TextView brand=txt("AURORA",42);brand.setGravity(Gravity.CENTER);brand.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));brand.setTextColor(0xfff1ffff);brand.setShadowLayer(dp(7),0,0,0xff00dfff);
  brand.setContentDescription("AURORA, asistente de accesibilidad sonora");
  top.addView(brand,new LinearLayout.LayoutParams(0,dp(76),1));
  LinearLayout settings=neonTile("AJUSTES",10,()->{settingsPage=true;render();});
  top.addView(settings,new LinearLayout.LayoutParams(dp(70),dp(76)));
  profileShortcut=new android.widget.ImageView(this);
  profileShortcut.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
  profileShortcut.setPadding(dp(4),dp(4),dp(4),dp(4));
  profileShortcut.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  android.widget.FrameLayout profileAction=new android.widget.FrameLayout(this);
  profileAction.setContentDescription("Abrir mi perfil");
  profileAction.setFocusable(true);
  profileAction.setOnClickListener(v->startActivity(new Intent(this,ProfileActivity.class)));
  profileAction.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());}});
  profileAction.setBackground(new android.graphics.drawable.RippleDrawable(
   android.content.res.ColorStateList.valueOf(0x66ff59da),
   AuroraDesign.surface(this,0xff171326,0xff07181d,0xffff59da,17),null));
  profileAction.addView(profileShortcut,new android.widget.FrameLayout.LayoutParams(-1,-1));
  profileShortcutLabel=txt("Perfil",11);profileShortcutLabel.setGravity(Gravity.CENTER);
  profileShortcutLabel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  profileAction.addView(profileShortcutLabel,new android.widget.FrameLayout.LayoutParams(-1,-1));
  LinearLayout.LayoutParams portraitSize=new LinearLayout.LayoutParams(dp(52),dp(52));
  portraitSize.leftMargin=dp(8);top.addView(profileAction,portraitSize);
  refreshProfileShortcut();
  hero.addView(top);
  TextView slogan=txt("TU MUNDO, ACCESIBLE",15);slogan.setGravity(Gravity.CENTER);slogan.setTypeface(null,Typeface.BOLD);slogan.setTextColor(0xffffff49);slogan.setPadding(0,0,0,dp(13));hero.addView(slogan);
  neonPair(hero,"ALARMA",0,()->{alarmsPage=true;render();},"TEMPORIZADOR",1,()->{timerPage=true;render();});
  neonPair(hero,"DETECCIÓN DE SONIDOS",2,()->openSense(SenseActivity.MODE_SOUND),"TRANSCRIPCIÓN",3,()->openSense(SenseActivity.MODE_SPEECH));
  neonPair(hero,"SOS",4,()->startActivity(new Intent(this,AuroraSosActivity.class)),"REGISTROS",5,()->startActivity(new Intent(this,RecordsActivity.class)));
  neonPair(hero,"SENTIR LA MÚSICA",6,()->startActivity(new Intent(this,AuroraMusicActivity.class)),"COMUNICAR",7,()->{communicationPage=true;render();});
  neonPair(hero,"AURORA VOZ",8,()->startActivity(new Intent(this,AuroraVoiceActivity.class)),"MOSTRADOR",9,()->startActivity(new Intent(this,AuroraCounterActivity.class)));
 }
 private void refreshProfileShortcut(){
  if(profileShortcut==null)return;
  android.graphics.Bitmap avatar=NeonIdentity.menuPortrait(this,dp(52));
  profileShortcut.setImageBitmap(avatar);
  profileShortcut.setVisibility(avatar==null?View.GONE:View.VISIBLE);
  profileShortcutLabel.setVisibility(avatar==null?View.VISIBLE:View.GONE);
 }
 LinearLayout neonTile(String label,int icon,Runnable navigate){
  LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(4),dp(6),dp(4),dp(6));
  tile.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x55fff94a),AuroraDesign.surface(this,0xf012242b,0xf2070b14,0xff25eaff,20),null));
  tile.setElevation(dp(5));if(Build.VERSION.SDK_INT>=28){tile.setOutlineAmbientShadowColor(0xff00eaff);tile.setOutlineSpotShadowColor(0xff00eaff);}
  tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription(label);tile.setOnClickListener(v->navigate.run());
  tile.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());}});
  NeonHomeIcon symbol=new NeonHomeIcon(this,icon);tile.addView(symbol,new LinearLayout.LayoutParams(dp(icon==10?29:52),dp(icon==10?29:52)));
  TextView name=txt(label,icon==10?10:14);name.setPadding(0,dp(3),0,0);name.setGravity(Gravity.CENTER);name.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));name.setTextColor(Color.WHITE);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(name,new LinearLayout.LayoutParams(-1,-2));
  return tile;
 }
 void neonPair(LinearLayout hero,String a,int ai,Runnable ar,String b,int bi,Runnable br){
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
  LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(108),1);left.rightMargin=dp(8);
  row.addView(neonTile(a,ai,ar),left);row.addView(neonTile(b,bi,br),new LinearLayout.LayoutParams(0,dp(108),1));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);hero.addView(row,lp);
 }
 void addHeroShortcut(LinearLayout row,String text,Runnable navigate){
  AuroraHeroCloudButton cloud=new AuroraHeroCloudButton(this,text);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);
  lp.leftMargin=dp(3);lp.rightMargin=dp(3);
  row.addView(cloud,lp);
  cloud.setOnClickListener(v->navigate.run());
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
  int currentBoot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  long safeElapsed=TimerBootClock.elapsedDeadlineForBoot(prefs.getLong("timer_end_elapsed",0L),
    prefs.getInt("timer_boot_count",-1),currentBoot);
  long remaining=TimerDeadline.remaining(savedEnd,safeElapsed,System.currentTimeMillis(),SystemClock.elapsedRealtime());
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
    public void onFinish(){counter.setText("00:00");counterInfo.setText("AVISO A CARGO DE ANDROID");}
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
  Button custom=btn("＋  TIEMPO PERSONALIZADO");root.addView(custom);
  custom.setOnClickListener(v->{
   LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(8),dp(18),dp(8));
   EditText minutes=new EditText(this);minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setHint("Minutos (1 a 1440)");minutes.setSingleLine(true);panel.addView(minutes);
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Duración personalizada").setView(panel).setNegativeButton("CANCELAR",null).setPositiveButton("INICIAR",null).create();
   dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
    int value;try{value=Integer.parseInt(minutes.getText().toString().trim());}catch(NumberFormatException ex){minutes.setError("Ingresá una cantidad de minutos");return;}
    if(value<1||value>1440){minutes.setError("Entre 1 y 1440 minutos");return;}
    dialog.dismiss();if(countdown(value,counter))counterInfo.setText("CUENTA REGRESIVA ACTIVA");
   }));dialog.show();
  });
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
  Button music=btn("SENTIR LA MÚSICA · VIBRACIÓN POR RITMO");
  root.addView(music,new LinearLayout.LayoutParams(-1,dp(66)));
  music.setOnClickListener(v->startActivity(new Intent(this,AuroraMusicActivity.class)));
  Button sos=btn("SOS · AVISAR A UN CONTACTO DE CONFIANZA");
  root.addView(sos,new LinearLayout.LayoutParams(-1,dp(65)));
  sos.setOnClickListener(v->startActivity(new Intent(this,AuroraSosActivity.class)));
  Button counter=btn("MODO MOSTRADOR · CONVERSAR CARA A CARA");
  root.addView(counter,new LinearLayout.LayoutParams(-1,dp(66)));
  counter.setOnClickListener(v->startActivity(new Intent(this,AuroraCounterActivity.class)));
  Button auroraVoice=btn("AURORA VOZ · TEXTO A AUDIO Y WHATSAPP");
  root.addView(auroraVoice,new LinearLayout.LayoutParams(-1,dp(66)));
  auroraVoice.setOnClickListener(v->startActivity(new Intent(this,AuroraVoiceActivity.class)));
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
  NotificationManager alerts=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=24&&alerts!=null&&!alerts.areNotificationsEnabled()){
   new AlertDialog.Builder(this).setTitle("Avisos de AURORA bloqueados")
    .setMessage("Android tiene desactivadas las notificaciones para AURORA. Aunque el micrófono funcione, no recibirías los avisos. Activá las notificaciones antes de comenzar.")
    .setPositiveButton("CONFIGURAR",(d,w)->{
     Intent settings=Build.VERSION.SDK_INT>=26?
      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra("android.provider.extra.APP_PACKAGE",getPackageName()):
      new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()));
     startActivity(settings);
    }).setNegativeButton("Cancelar",null).show();return;
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
  setupPage();pageTitle("CONFIGURACIÓN");
  int firstSetting=root.getChildCount();
  root.addView(section("PERSONALIZACIÓN"));
  AuroraHeroCloudView mascotPreview=new AuroraHeroCloudView(this);root.addView(mascotPreview,new LinearLayout.LayoutParams(-1,dp(160)));
  Button mascotChoice=btn("PERSONAJE AURORA");root.addView(mascotChoice);
  mascotChoice.setOnClickListener(v->{
   new AlertDialog.Builder(this).setTitle("Elegí el personaje")
    .setSingleChoiceItems(new String[]{"Nube urbana","Nube alternativa"},mascotPreview.getCharacter(),(dialog,which)->{
     mascotPreview.setCharacter(which);prefs.edit().putInt(AuroraHeroCloudView.PREF_CHARACTER,which).apply();
     Toast.makeText(this,which==0?"Nube urbana elegida":"Nube alternativa elegida",Toast.LENGTH_SHORT).show();
    }).setPositiveButton("CERRAR",null).show();
  });
  root.addView(section("DIAGNÓSTICO RÁPIDO DE NOTIFICACIONES"));
  root.addView(txt("Primero probá un aviso del teléfono sin micrófono ni smartwatch. "+
   "Si no llega, el problema es de permisos o de configuración Android; "+
   "si llega, revisaremos la detección y las categorías de sonidos.",14));
  Button testPhoneAlert=btn("PROBAR NOTIFICACIÓN · TELÉFONO");
  root.addView(testPhoneAlert);
  testPhoneAlert.setOnClickListener(v->{
   String result=AuroraPhoneAlerts.post(this,"POSIBLE TIMBRE DE PUERTA",true);
   new AlertDialog.Builder(this).setTitle("Diagnóstico de notificaciones")
    .setMessage(result+"\n\n"+AuroraPhoneAlerts.report(this))
    .setPositiveButton("AJUSTES DE NOTIFICACIONES",(d,w)->{
     Intent go=Build.VERSION.SDK_INT>=26?
      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
       .putExtra("android.provider.extra.APP_PACKAGE",getPackageName()):
      new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
       android.net.Uri.parse("package:"+getPackageName()));
     startActivity(go);
    }).setNegativeButton("CERRAR",null).show();
  });
  Button reportPhoneAlert=btn("CONSULTAR ÚLTIMO AVISO");
  root.addView(reportPhoneAlert);
  reportPhoneAlert.setOnClickListener(v->new AlertDialog.Builder(this)
   .setTitle("Estado de avisos del teléfono")
   .setMessage(AuroraPhoneAlerts.report(this))
   .setPositiveButton("CERRAR",null).show());
  root.addView(section("ESCUCHA DEL ENTORNO EN SEGUNDO PLANO"));
  root.addView(txt("Activala solo cuando la necesites. Android mostrará una notificación permanente mientras se usa el micrófono. La escucha se detiene al apagarla y no se reactiva sola al reiniciar.",15));
  Button bgStart=btn("◉  INICIAR ESCUCHA CONTINUA");
  AuroraDesign.action(bgStart,0xff18524c,0xff224845,0xff61c7b7);
  root.addView(bgStart);bgStart.setOnClickListener(v->startBackgroundMonitor());
  Button bgStop=btn("■  DETENER ESCUCHA CONTINUA");root.addView(bgStop);
  bgStop.setOnClickListener(v->{stopBackgroundMonitor();Toast.makeText(this,"Deteniendo escucha",Toast.LENGTH_SHORT).show();});
  root.addView(txt("Última detección: "+prefs.getString(AuroraSoundMonitorService.KEY_LAST,"Sin eventos"),14));
  long now=System.currentTimeMillis();
  String monitorFailure=prefs.getString(AuroraSoundMonitorService.KEY_ERROR,"");
  MonitorHealth.State monitorStatus=MonitorHealth.evaluate(
    prefs.getBoolean(AuroraSoundMonitorService.KEY_ACTIVE,false),
    prefs.getLong(AuroraSoundMonitorService.KEY_STARTED,0L),
    prefs.getLong(AuroraSoundMonitorService.KEY_HEARTBEAT,0L),
    now,monitorFailure);
  TextView monitorState=txt("Estado del detector: "+MonitorHealth.label(monitorStatus),15);
  monitorState.setTextColor(monitorStatus==MonitorHealth.State.LISTENING?0xffa5f2df:
    monitorStatus==MonitorHealth.State.OFF?0xffc9d5e1:0xffffc47a);
  root.addView(monitorState);
  if(!monitorFailure.isEmpty())root.addView(txt("Última interrupción: "+monitorFailure,14));
  if(monitorStatus==MonitorHealth.State.LISTENING)root.addView(txt(
    "El modelo responde; eso no garantiza que todas las detecciones sean correctas.",13));
  if(monitorStatus==MonitorHealth.State.STALE)root.addView(txt(
    "Android podría haber interrumpido la captura. Detené la escucha y volvé a iniciarla desde AURORA.",14));
  Button monitorRefresh=btn("ACTUALIZAR DIAGNÓSTICO");root.addView(monitorRefresh);
  monitorRefresh.setOnClickListener(v->renderSettingsPage());
  NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=24&&notifications!=null&&!notifications.areNotificationsEnabled()){
   TextView warning=txt("IMPORTANTE: Android bloquea las notificaciones de AURORA.",16);
   warning.setTextColor(0xffffc47a);root.addView(warning);
  }
  if(Build.VERSION.SDK_INT>=26&&notifications!=null){
   int disabledChannels=0;
   for(String key:SoundAlertPreferences.KEYS){
    android.app.NotificationChannel channel=notifications.getNotificationChannel(SoundAlertPreferences.channel(key));
    if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE)disabledChannels++;
   }
   if(disabledChannels>0)root.addView(txt("Hay "+disabledChannels+" categoría(s) de avisos bloqueadas desde los ajustes de Android.",14));
  }
  if(Build.VERSION.SDK_INT>=23){
   PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
   if(power!=null&&!power.isIgnoringBatteryOptimizations(getPackageName())){
    root.addView(txt("Batería: Android podría limitar la escucha cuando el teléfono está bloqueado. La configuración depende del fabricante.",14));
    Button battery=btn("VER CONFIGURACIÓN DE BATERÍA");
    root.addView(battery);battery.setOnClickListener(v->{
     try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
     catch(Exception e){Toast.makeText(this,"Abrí Batería desde los ajustes del teléfono.",Toast.LENGTH_LONG).show();}
    });
   }
  }

  root.addView(section("AVISOS POR TIPO DE SONIDO"));
  for(int cat=0;cat<SoundAlertPreferences.KEYS.length;cat++){
   String key=SoundAlertPreferences.KEYS[cat];
   android.widget.Switch check=new android.widget.Switch(this);
   check.setText(SoundAlertPreferences.LABELS[cat]);
   check.setTextColor(Color.WHITE);check.setTextSize(16);
   check.setPadding(dp(10),dp(10),dp(10),dp(10));
   check.setChecked(prefs.getBoolean("monitor_alert_"+key,!"other".equals(key)&&!"voices".equals(key)));
   root.addView(check);
   check.setOnCheckedChangeListener((v,on)->{
    prefs.edit().putBoolean("monitor_alert_"+key,on).apply();
    if(!on){
     NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
     if(manager!=null){
      // Remove every active notice from this category only, including
      // distinct simultaneous events. Never cancel the microphone service.
      try{
       for(android.service.notification.StatusBarNotification note:manager.getActiveNotifications()){
        if(SoundAlertPreferences.isNotificationForCategory(note.getTag(),key))
         manager.cancel(note.getTag(),note.getId());
       }
       // Clear any legacy untagged notice issued by older AURORA builds.
       manager.cancel(SoundAlertPreferences.notificationId(key));
      }catch(RuntimeException issue){
       android.util.Log.w("AURORA","No se pudieron limpiar avisos de "+key,issue);
      }
     }
    }
   });
  }
  root.addView(txt("Las categorías que no marques pueden seguir siendo detectadas, pero no generarán un aviso emergente. La clasificación es aproximada.",13));
  root.addView(section("AVISOS AL DETECTAR SONIDOS"));
  android.widget.Switch soundHaptics=new android.widget.Switch(this);
  soundHaptics.setText("Vibrar por sonidos importantes en pantalla");
  soundHaptics.setTextColor(Color.WHITE);soundHaptics.setTextSize(16);
  soundHaptics.setPadding(dp(10),dp(9),dp(10),dp(9));
  soundHaptics.setChecked(prefs.getBoolean("monitor_sound_vibration",true));
  root.addView(soundHaptics);
  soundHaptics.setOnCheckedChangeListener((button,enabled)->
   prefs.edit().putBoolean("monitor_sound_vibration",enabled).apply());
  root.addView(txt("En segundo plano, la vibración depende de las categorías de notificación de Android; no se duplica desde la app.",13));
  android.widget.Switch soundFlash=new android.widget.Switch(this);
  soundFlash.setText("Encender flash LED con sonidos detectados");
  soundFlash.setTextColor(Color.WHITE);soundFlash.setTextSize(16);
  soundFlash.setPadding(dp(10),dp(9),dp(10),dp(9));
  soundFlash.setChecked(prefs.getBoolean("monitor_sound_flash",false));
  root.addView(soundFlash);
  soundFlash.setOnCheckedChangeListener((button,enabled)->{
   if(!enabled){prefs.edit().putBoolean("monitor_sound_flash",false).apply();return;}
   if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
    soundFlash.setChecked(false);
    requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);
    Toast.makeText(this,"Autorizá Cámara y después volvé a activar el flash de sonidos",Toast.LENGTH_LONG).show();
    return;
   }
   new AlertDialog.Builder(this)
    .setTitle("Flash LED para sonidos")
    .setMessage("Este aviso usa destellos de la linterna tras detectar sonidos habilitados. Dura hasta 2,6 segundos y limita las repeticiones. Puede afectar a personas fotosensibles, consumir batería o no funcionar si otra app usa la cámara. Es opcional y no sustituye alarmas de seguridad.")
    .setPositiveButton("ACTIVAR",(dialog,which)->prefs.edit().putBoolean("monitor_sound_flash",true).apply())
    .setNegativeButton("CANCELAR",(dialog,which)->soundFlash.setChecked(false))
    .setOnCancelListener(dialog->soundFlash.setChecked(false))
    .show();
  });
  root.addView(txt("Los sonidos se incorporan a la lista de los últimos 30 aunque no tengan avisos habilitados. Vibración y flash sólo se disparan por detecciones confirmadas de las categorías elegidas.",13));
  Button testSoundSignal=btn("PROBAR ALERTA DE SONIDO · SIN MICRÓFONO");
  root.addView(testSoundSignal);
  testSoundSignal.setOnClickListener(v->{
   if(!SoundAlertPreferences.isEnabled(this,"POSIBLE TIMBRE")){
    Toast.makeText(this,"Habilitá Timbre y puertas para probar este aviso",Toast.LENGTH_LONG).show();
    return;
   }
   SoundEventFeedback testFeedback=new SoundEventFeedback(this);
   testFeedback.onConfirmed("POSIBLE TIMBRE",true);
   new Handler(Looper.getMainLooper()).postDelayed(testFeedback::stop,5000);
   Toast.makeText(this,"Probando vibración y flash según la configuración. No se registró ningún sonido.",Toast.LENGTH_LONG).show();
  });
  Button flashDiagnostic=btn("CONSULTAR ÚLTIMO ESTADO DEL FLASH DE SONIDOS");
  root.addView(flashDiagnostic);
  flashDiagnostic.setOnClickListener(v->{
   String report=prefs.getString("monitor_sound_flash_last_status","Aún no hubo un intento de flash ambiental.");
   boolean enabled=prefs.getBoolean("monitor_sound_flash",false);
   boolean authorized=Build.VERSION.SDK_INT>=23&&
    checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED;
   new AlertDialog.Builder(this).setTitle("Diagnóstico de flash por sonidos")
    .setMessage("Flash por sonidos: "+(enabled?"ACTIVADO":"APAGADO")+
     "\nPermiso Cámara: "+(authorized?"CONCEDIDO":"NO CONCEDIDO")+
     "\nÚltimo resultado: "+report+
     "\n\nSolo parpadea ante sonidos confirmados y categorías habilitadas. "+
     "El teléfono puede bloquear la linterna si otra aplicación utiliza la cámara.")
    .setPositiveButton("CERRAR",null).show();
  });
  root.addView(section("SMARTWATCH · AVISOS CON DIBUJOS"));
  android.widget.Switch watchPictures=new android.widget.Switch(this);
  watchPictures.setText("Avisos con dibujo para relojes compatibles");
  watchPictures.setTextColor(Color.WHITE);watchPictures.setTextSize(16);
  watchPictures.setChecked(AuroraWatchAlert.isEnabled(this));
  root.addView(watchPictures);
  watchPictures.setOnCheckedChangeListener((v,enabled)->
   prefs.edit().putBoolean(AuroraWatchAlert.PREF,enabled).apply());
  root.addView(txt("AURORA dibuja el evento en la notificación de Android. "+
   "Un reloj Wear OS vinculado puede reflejarla y vibrar si sus avisos están habilitados. "+
   "Algunos relojes muestran solo texto o un ícono pequeño; todavía no hay app de reloj independiente.",13));
  Button watchPreview=btn("PROBAR DIBUJO DE PUERTA EN RELOJ · SIN MICRÓFONO");
  root.addView(watchPreview);
  watchPreview.setOnClickListener(v->{
   if(!AuroraWatchAlert.isEnabled(this)){
    Toast.makeText(this,"Activá primero los avisos con dibujos",Toast.LENGTH_LONG).show();return;
   }
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
      !=android.content.pm.PackageManager.PERMISSION_GRANTED){
    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
    Toast.makeText(this,"Autorizá notificaciones y repetí la prueba",Toast.LENGTH_LONG).show();return;
   }
   AuroraWatchAlert.previewDoor(this);
   Toast.makeText(this,"Prueba enviada a las notificaciones del teléfono. "+
    "Revisá si también aparece en tu reloj vinculado.",Toast.LENGTH_LONG).show();
  });
  root.addView(section("MODO DE VALIDACIÓN DE SONIDOS"));
  android.widget.Switch validation=new android.widget.Switch(this);
  validation.setText("Evaluar detecciones con ACERTÓ / NO COINCIDE");
  validation.setTextColor(Color.WHITE);validation.setTextSize(16);
  validation.setPadding(dp(10),dp(10),dp(10),dp(10));
  validation.setChecked(prefs.getBoolean("sense_feedback_mode",false));
  root.addView(validation);
  validation.setOnCheckedChangeListener((v,enabled)->
   prefs.edit().putBoolean("sense_feedback_mode",enabled).apply());
  root.addView(txt("Modo voluntario: en SONIDOS podés marcar ACERTÓ, NO COINCIDE o SONIDO NO DETECTADO. "
   +"Se guardan únicamente conteos por tipo, sin audio, conversaciones, nombres ni ubicaciones. "
   +"Los porcentajes reflejan solo las pruebas que marques vos; no son precisión de laboratorio.",13));
  root.addView(section("RESULTADOS DE PRUEBAS REALES"));
  SoundValidationMetrics metrics=SoundValidationStore.read(this);
  TextView validationReport=txt(metrics.report(),13);
  validationReport.setTextColor(0xffb3e5ec);
  validationReport.setTextIsSelectable(true);
  root.addView(validationReport);
  Button refreshValidation=btn("ACTUALIZAR RESULTADOS DE PRUEBAS");
  root.addView(refreshValidation);
  refreshValidation.setOnClickListener(v->
   validationReport.setText(SoundValidationStore.read(this).report()));
  Button copyValidation=btn("COPIAR RESULTADOS SIN AUDIO");
  root.addView(copyValidation);
  copyValidation.setOnClickListener(v->{
   String report=SoundValidationStore.read(this).report();
   android.content.ClipboardManager clipboard=
    (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(clipboard!=null){
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AURORA EVALUACIÓN MANUAL",report));
    Toast.makeText(this,"Resultados copiados, sin audio ni conversaciones",Toast.LENGTH_SHORT).show();
   }
  });
  Button resetValidation=btn("BORRAR SOLO RESULTADOS DE PRUEBAS");
  root.addView(resetValidation);
  resetValidation.setOnClickListener(v->new AlertDialog.Builder(this)
   .setTitle("Borrar estadísticas de validación")
   .setMessage("Se borran únicamente los conteos manuales. Las conversaciones, "
    +"alarmas y los últimos 30 sonidos no se modifican.")
   .setNegativeButton("CANCELAR",null)
   .setPositiveButton("BORRAR",(dialog,which)->{
    SoundValidationStore.clear(this);
    validationReport.setText(SoundValidationStore.read(this).report());
   }).show());
  root.addView(section("COPIA DE SEGURIDAD DE CONFIGURACIÓN"));
  root.addView(txt("Guardá tus alarmas, horarios y preferencias en un TXT que podrás restaurar en AURORA. "
   +"No incluye fotos ni conversaciones. Los sonidos se conservan únicamente como lista de los últimos 30 eventos dentro de la app; no se exportan como archivos.",14));
  Button backup=btn("GUARDAR ALARMAS Y AJUSTES EN UN ARCHIVO");
  root.addView(backup);
  backup.setOnClickListener(v->exportSettings());
  Button restore=btn("RESTAURAR ALARMAS Y AJUSTES");
  root.addView(restore);
  restore.setOnClickListener(v->importSettings());
  root.addView(txt("Importar reemplaza las alarmas y ajustes actuales. No recupera datos eliminados de versiones antiguas; "
   +"guardá antes las conversaciones desde la carpeta CONVERSACIONES GUARDADAS.",13));
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

 root.addView(section("PERMISOS Y ALERTAS DE ALARMA"));
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
 Button permissionWizard=btn("REVISAR TODOS LOS PERMISOS");root.addView(permissionWizard);permissionWizard.setOnClickListener(v->showPermissionSetup(true));
 Switch torchSwitch=new Switch(this);torchSwitch.setText("Flash LED del temporizador y valor inicial de alarmas");torchSwitch.setTextColor(Color.WHITE);torchSwitch.setTextSize(17);torchSwitch.setChecked(prefs.getBoolean("torch_alert",true));root.addView(torchSwitch);torchSwitch.setOnCheckedChangeListener((button,enabled)->{prefs.edit().putBoolean("torch_alert",enabled).apply();if(enabled&&Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);if(!enabled&&torch!=null)torch.stop();});
 if(Build.VERSION.SDK_INT>=23&&prefs.getBoolean("torch_alert",true)&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){Button torchPermission=btn("Autorizar flash LED (Cámara)");root.addView(torchPermission);torchPermission.setOnClickListener(v->requestPermissions(new String[]{android.Manifest.permission.CAMERA},2));}
 root.addView(txt("Aviso: el flash LED intermitente puede afectar a personas fotosensibles. Podés desactivarlo con el interruptor anterior. Si el flash funciona en PRUEBA pero no con la pantalla bloqueada, revisá permisos de notificaciones y alertas a pantalla completa: Android puede no abrir la pantalla de alarma.",13));
 Switch visualFlash=new Switch(this);visualFlash.setText("Destellos visuales (opcional)");visualFlash.setTextColor(Color.WHITE);visualFlash.setTextSize(17);visualFlash.setChecked(prefs.getBoolean("visual_flash",false));root.addView(visualFlash);visualFlash.setOnCheckedChangeListener((button,enabled)->{if(enabled)new AlertDialog.Builder(this).setTitle("Aviso sobre destellos").setMessage("Los cambios rápidos de luz pueden afectar a personas con fotosensibilidad. Activá esta opción solo si es adecuada para vos.").setPositiveButton("Activar",(d,w)->prefs.edit().putBoolean("visual_flash",true).apply()).setNegativeButton("Cancelar",(d,w)->visualFlash.setChecked(false)).show();else prefs.edit().putBoolean("visual_flash",false).apply();});


 root.addView(section("ASISTENTE DE IMÁGENES"));
 Button photoAssistant=btn("ANALIZAR UNA FOTO");
 root.addView(photoAssistant);
 photoAssistant.setOnClickListener(v->startActivity(new Intent(this,AuroraAssistActivity.class)));
 root.addView(section("DIAGNÓSTICO TÉCNICO PARA PRUEBAS"));
 root.addView(txt("Podés copiar un resumen de permisos, batería y estado de alertas para revisar problemas en el teléfono. No incluye conversaciones, sonidos grabados ni datos personales.",14));
 Button diagnostic=btn("COPIAR REPORTE TÉCNICO");root.addView(diagnostic);
 diagnostic.setOnClickListener(v->{
  android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(clipboard!=null){
   clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AURORA · Diagnóstico",AuroraDiagnostics.report(this)));
   Toast.makeText(this,"Reporte copiado. Podés pegarlo para revisar la configuración.",Toast.LENGTH_LONG).show();
  }
 });
 Button test=btn("Probar vibración");root.addView(test);test.setOnClickListener(v->vibrate());
 Button preview=btn("Probar pantalla de alarma");root.addView(preview);preview.setOnClickListener(v->showAlarm());
 Button stopVibration=btn("Detener vibración");root.addView(stopVibration);stopVibration.setOnClickListener(v->stopAlarmEffects());
 root.addView(txt("Aviso: las alertas dependen de los permisos, la batería y las restricciones de Android. Esta aplicación no reemplaza alarmas de emergencia certificadas. La ejecución en segundo plano depende de Android y del fabricante.",13));
  collapseSettingsSections(firstSetting);
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
  if(!hasPermission(android.Manifest.permission.CAMERA)||!exactReady()||!notificationsReady()){Button fix=btn("REVISAR PERMISOS DE ALARMA");root.addView(fix);fix.setOnClickListener(v->showPermissionSetup(true));}
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
 long next=prefs.getBoolean("on"+id,false)?prefs.getLong("armed_at"+id,0L):-1L;
 TextView nextLabel=txt(next>0?"Próxima fecha prevista: "+
  new java.text.SimpleDateFormat("EEE dd/MM · HH:mm",new Locale("es","AR")).format(new Date(next))+
  " (programada)":prefs.getBoolean("on"+id,false)?"No programada: revisá días y permiso de alarmas exactas":"Alarma desactivada",14);
 nextLabel.setTextColor(0xffbbd5e6);card.addView(nextLabel);
 Switch sw=new Switch(this);sw.setText(String.format(Locale.getDefault(),"Alarma %d   %02d:%02d",id+1,h,m));sw.setTextColor(Color.WHITE);sw.setTextSize(25);sw.setPadding(dp(9),dp(8),dp(9),dp(8));sw.setChecked(prefs.getBoolean("on"+id,false));card.addView(sw);
 sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("on"+id,on).apply();schedule(this,id);if(on&&prefs.getInt("days"+id,127)==0)Toast.makeText(this,"Elegí al menos un día para esta alarma",Toast.LENGTH_LONG).show();});
 Button options=btn("⚙  CONFIGURAR ALARMA");card.addView(options);
 LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);details.setVisibility(View.GONE);card.addView(details);
 options.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);options.setText(open?"⚙  CERRAR CONFIGURACIÓN":"⚙  CONFIGURAR ALARMA");});
 Button days=btn("Días de la alarma · "+(id+1));details.addView(days);days.setOnClickListener(v->{String[] labels={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};boolean[] checked=new boolean[7];int mask=prefs.getInt("days"+id,127);for(int j=0;j<7;j++)checked[j]=(mask&(1<<j))!=0;new AlertDialog.Builder(this).setTitle("Elegí los días").setMultiChoiceItems(labels,checked,(d,which,on)->checked[which]=on).setPositiveButton("Guardar",(d,w)->{int msk=0;for(int j=0;j<7;j++)if(checked[j])msk|=(1<<j);prefs.edit().putInt("days"+id,msk).apply();cancelSnooze(this,id);schedule(this,id);if(msk==0)Toast.makeText(this,"Sin días seleccionados: esta alarma no sonará",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancelar",null).show();});
 Button delete=btn("Eliminar alarma · "+(id+1));details.addView(delete);delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar alarma").setMessage("¿Querés eliminar esta alarma?").setPositiveButton("Eliminar",(d,w)->{prefs.edit().putBoolean("on"+id,false).putBoolean("created"+id,false).apply();schedule(this,id);render();}).setNegativeButton("Cancelar",null).show());
 Button edit=btn("Cambiar hora · "+(id+1));details.addView(edit);edit.setOnClickListener(v->new TimePickerDialog(this,(picker,hh,mm)->{prefs.edit().putInt("h"+id,hh).putInt("m"+id,mm).putBoolean("on"+id,true).apply();cancelSnooze(this,id);schedule(this,id);render();},h,m,true).show());
  String flashState=!prefs.getBoolean("alarm_flash"+id,prefs.getBoolean("torch_alert",true))?"DESACTIVADO":hasPermission(android.Manifest.permission.CAMERA)?"ACTIVADO":"SIN PERMISO DE CÁMARA";
  details.addView(txt("Flash LED: "+flashState+" · Vibración: "+(prefs.getBoolean("alarm_vibration"+id,true)?"SÍ":"NO")+" · Sonido: "+(prefs.getBoolean("alarm_sound"+id,false)?"SÍ":"NO"),14));
  String last=prefs.getString("last_alarm_flash"+id,"Sin prueba programada");details.addView(txt("Último flash: "+last,13));
  details.addView(txt("Última vibración: "+prefs.getString("last_alarm_vibration"+id,"Sin prueba programada"),13));
  details.addView(txt("Último sonido: "+prefs.getString("last_alarm_audio"+id,"Sin prueba programada"),13));
  Button effects=btn("Sonido, vibración y flash · "+(id+1));details.addView(effects);effects.setOnClickListener(v->configureAlarmEffects(id));
  root.addView(card);
 }
 void configureAlarmEffects(int id){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(12),dp(18),dp(12));
  CheckBox vib=new CheckBox(this);vib.setText("Vibración");vib.setChecked(prefs.getBoolean("alarm_vibration"+id,true));panel.addView(vib);
  CheckBox flash=new CheckBox(this);flash.setText("Flash LED (requiere permiso de Cámara)");flash.setChecked(prefs.getBoolean("alarm_flash"+id,prefs.getBoolean("torch_alert",true)));panel.addView(flash);
  Button testLed=btn("PROBAR FLASH LED · 5 SEGUNDOS");panel.addView(testLed);testLed.setOnClickListener(v->{if(!hasPermission(android.Manifest.permission.CAMERA)){requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);return;}if(torch!=null&&torch.start()){flashHandler.postDelayed(()->{if(torch!=null)torch.stop();},5000);}else Toast.makeText(this,"Flash no disponible: "+(torch==null?"Sin controlador":torch.getLastError()),Toast.LENGTH_LONG).show();});
  CheckBox sound=new CheckBox(this);sound.setText("Sonido de alarma");sound.setChecked(prefs.getBoolean("alarm_sound"+id,false));panel.addView(sound);
  panel.addView(txt("Elegí uno de los 10 sonidos",16));
  Spinner tones=new Spinner(this);tones.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,AlarmTone.NAMES));tones.setSelection(Math.max(0,Math.min(9,prefs.getInt("alarm_tone"+id,0))));panel.addView(tones);
  panel.addView(txt("Volumen (relativo al volumen de alarma de Android)",14));
  SeekBar vol=new SeekBar(this);vol.setMax(100);vol.setProgress(prefs.getInt("alarm_volume"+id,75));panel.addView(vol);
  Button listen=btn("ESCUCHAR SONIDO");panel.addView(listen);listen.setOnClickListener(v->{if(!alarmPreview.play(tones.getSelectedItemPosition(),vol.getProgress(),false))Toast.makeText(this,"No se inició el sonido. Revisá el volumen.",Toast.LENGTH_LONG).show();});
  panel.addView(txt("Duración de alerta",16));
  Spinner duration=new Spinner(this);String[] labels={"5 segundos","15 segundos","30 segundos","1 minuto","3 minutos"};duration.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));int selected=3;for(int j=0;j<AlarmTone.DURATIONS.length;j++)if(AlarmTone.DURATIONS[j]==prefs.getInt("alarm_duration"+id,60))selected=j;duration.setSelection(selected);panel.addView(duration);
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Configurar alarma "+(id+1)).setView(scroll).setPositiveButton("GUARDAR",(dlg,w)->{
   prefs.edit().putBoolean("alarm_vibration"+id,vib.isChecked()).putBoolean("alarm_flash"+id,flash.isChecked()).putBoolean("alarm_sound"+id,sound.isChecked()).putInt("alarm_tone"+id,tones.getSelectedItemPosition()).putInt("alarm_volume"+id,vol.getProgress()).putInt("alarm_duration"+id,AlarmTone.DURATIONS[duration.getSelectedItemPosition()]).apply();
   if(flash.isChecked()&&checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.CAMERA},2);
  }).setNegativeButton("CANCELAR",null).create();d.setOnDismissListener(x->{alarmPreview.stop();if(torch!=null)torch.stop();if(alarmsPage&&!alarmActive)renderAlarmsPage();});d.show();
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
  if(!p.getBoolean("on"+id,false)){
   am.cancel(event);p.edit().remove("armed_at"+id).apply();
   cancelSnooze(c,id);
   return;
  }
  long when=AlarmPlanner.nextTrigger(System.currentTimeMillis(),p.getInt("h"+id,7),
   p.getInt("m"+id,0),p.getInt("days"+id,127),TimeZone.getDefault());
  if(when<0){am.cancel(event);p.edit().remove("armed_at"+id).apply();return;}
  if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){
   p.edit().remove("armed_at"+id).apply();android.util.Log.w("Aurora","Falta permiso de alarmas exactas");return;
  }
  PendingIntent show=PendingIntent.getActivity(c,100+id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),event);p.edit().putLong("armed_at"+id,when).apply();}
  catch(RuntimeException ex){
   android.util.Log.w("Aurora","setAlarmClock falló; intentando mecanismo del temporizador",ex);
   try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,event);p.edit().putLong("armed_at"+id,when).apply();}
   catch(RuntimeException fallback){p.edit().remove("armed_at"+id).apply();android.util.Log.e("Aurora","No se pudo programar alarma exacta",fallback);}
  }
 }
 void cancelTimer(){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);PendingIntent pi=PendingIntent.getBroadcast(this,3100,new Intent(this,TimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);prefs.edit().remove("timer_end").remove("timer_end_elapsed").remove("timer_boot_count").apply();}
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
   prefs.edit().putLong("timer_end",endWall).putLong("timer_end_elapsed",endElapsed)
    .putInt("timer_boot_count",Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1)).apply();
  }catch(SecurityException ex){
   new AlertDialog.Builder(this).setMessage("Android no permitió programar el temporizador exacto.").setPositiveButton("Aceptar",null).show();return false;
  }
  timer=new CountDownTimer(minutes*60000L,1000){
   public void onTick(long ms){output.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}
   public void onFinish(){
    output.setText("¡TIEMPO CUMPLIDO!");
    // Android entrega el aviso mediante TimerReceiver.
   }
  }.start();
  return true;
 }
 void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator()){long[] pat={0,800,200,800,200,800};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}}
 @Override protected void onPause(){stopHomeSpeech();super.onPause();}
 @Override protected void onDestroy(){stopHomeSpeech();alarmPreview.stop();if(timer!=null)timer.cancel();if(alarmActive&&AlarmEffectsService.activeId!=activeAlarmId)stopAlarmEffects();if(torch!=null)torch.stop();super.onDestroy();}
}