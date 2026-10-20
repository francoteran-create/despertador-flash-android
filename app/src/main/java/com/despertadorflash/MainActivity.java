package com.despertadorflash;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
 static final String PREF="aurora";
 LinearLayout root; SharedPreferences prefs; CountDownTimer timer; android.os.Handler flashHandler=new android.os.Handler(android.os.Looper.getMainLooper()); Runnable flashTask; boolean alarmActive=false; int activeAlarmId=-1;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(5),dp(12),dp(5),dp(12));return v;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(17);b.setMinHeight(dp(58));return b;}
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,0);render();if(getIntent().getBooleanExtra("ring",false))showAlarm();if(getIntent().getBooleanExtra("timer_finished",false))new AlertDialog.Builder(this).setTitle("AURORA").setMessage("¡Tiempo cumplido!").setPositiveButton("Aceptar",null).show();}
 void stopAlarmEffects(){
  alarmActive=false; activeAlarmId=-1;
  if(flashTask!=null){flashHandler.removeCallbacks(flashTask);flashTask=null;}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)for(int id=0;id<5;id++)nm.cancel(2000+id);
 }
 void snoozeAlarm(){
  AlarmManager manager=(AlarmManager)getSystemService(ALARM_SERVICE);
  if(activeAlarmId<0){Toast.makeText(this,"La alarma de prueba no se puede posponer",Toast.LENGTH_LONG).show();return;}
  Intent intent=new Intent(this,AlarmReceiver.class).putExtra("snooze",true).putExtra("id",activeAlarmId);
  PendingIntent pending=PendingIntent.getBroadcast(this,4100+activeAlarmId,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  try{long when=System.currentTimeMillis()+10*60000L;if(Build.VERSION.SDK_INT>=31&&!manager.canScheduleExactAlarms()){Toast.makeText(this,"Habilitá alarmas exactas para posponer",Toast.LENGTH_LONG).show();return;}manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);Toast.makeText(this,"Alarma pospuesta 10 minutos",Toast.LENGTH_LONG).show();}catch(SecurityException ex){Toast.makeText(this,"No se pudo posponer: falta permiso de alarmas",Toast.LENGTH_LONG).show();}
 }
 void showAlarm(){
  if(alarmActive)return;
  alarmActive=true; activeAlarmId=getIntent().getBooleanExtra("ring",false)?getIntent().getIntExtra("alarm_id",-1):-1;
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(24),dp(22),dp(24));
  TextView big=txt("¡DESPERTÁ!",38);big.setGravity(Gravity.CENTER);big.setTypeface(null,Typeface.BOLD);panel.addView(big);
  TextView sub=txt("AURORA está avisándote. Tocá el botón para detener la alerta.",19);sub.setGravity(Gravity.CENTER);panel.addView(sub);
  final boolean[] bright={false};
  flashTask=new Runnable(){public void run(){bright[0]=!bright[0];panel.setBackgroundColor(bright[0]?0xffffffff:0xff10182e);big.setTextColor(bright[0]?0xff08162c:0xffffffff);sub.setTextColor(bright[0]?0xff08162c:0xffffffff);flashHandler.postDelayed(this,650);}};
  if(prefs.getBoolean("visual_flash",false)&&!isFinishing())flashHandler.post(flashTask);else{panel.setBackgroundColor(0xff10182e);big.setTextColor(Color.WHITE);sub.setTextColor(Color.WHITE);}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){long[] pattern={0,900,300,900,300,900,300};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pattern,0));else v.vibrate(pattern,0);}
  new AlertDialog.Builder(this).setView(panel).setPositiveButton("ESTOY DESPIERTO",(d,w)->stopAlarmEffects()).setNeutralButton("POSPONER 10 MIN",(d,w)->{snoozeAlarm();stopAlarmEffects();}).setOnDismissListener(d->stopAlarmEffects()).setCancelable(false).show();
 }

 @Override public void onBackPressed(){if(alarmActive){new AlertDialog.Builder(this).setTitle("Alarma activa").setMessage("Confirmá que estás despierto para detener la alarma.").setPositiveButton("Entendido",null).show();return;}super.onBackPressed();}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("ring",false))showAlarm();if(i.getBooleanExtra("timer_finished",false))new AlertDialog.Builder(this).setTitle("AURORA").setMessage("¡Tiempo cumplido!").setPositiveButton("Aceptar",null).show();}
 @Override protected void onResume(){super.onResume();if(prefs!=null){for(int id=0;id<5;id++){if(prefs.getBoolean("on"+id,false))schedule(this,id);}}}
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==1&&grantResults.length>0&&grantResults[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Sin notificaciones, algunas alarmas podrían no mostrarse",Toast.LENGTH_LONG).show();}}
 void render(){
 if(timer!=null){timer.cancel();timer=null;}
 ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setBackgroundColor(0xff0b1325);scroll.addView(root);setContentView(scroll);
 TextView title=txt("✦ AURORA",34);title.setTextColor(0xff91f0dd);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 root.addView(txt("Tu mañana, a tu manera.",21));root.addView(txt("Despertador accesible con vibración y avisos visuales.",15));
 AlarmManager alarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
 if(Build.VERSION.SDK_INT>=31&&!alarmManager.canScheduleExactAlarms()){TextView warning=txt("ATENCIÓN: las alarmas exactas no están habilitadas. Activá el permiso para que el despertador funcione a la hora elegida.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){TextView warning=txt("ATENCIÓN: faltan permisos de notificaciones. Los avisos pueden no aparecer.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 Button perm=btn("Permitir alarmas exactas");root.addView(perm);perm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);if(!a.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}});
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){Button notificationPermission=btn("Habilitar notificaciones");root.addView(notificationPermission);notificationPermission.setOnClickListener(v->requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1));}
 if(Build.VERSION.SDK_INT>=34){NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(!notificationManager.canUseFullScreenIntent()){TextView fullWarning=txt("Android restringe las alarmas a pantalla completa. Revisá el permiso de alertas de pantalla completa.",16);fullWarning.setTextColor(0xffffcf70);root.addView(fullWarning);Button fullPermission=btn("Permitir alertas a pantalla completa");root.addView(fullPermission);fullPermission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,android.net.Uri.parse("package:"+getPackageName()))));}}
 Switch visualFlash=new Switch(this);visualFlash.setText("Destellos visuales (opcional)");visualFlash.setTextColor(Color.WHITE);visualFlash.setTextSize(17);visualFlash.setChecked(prefs.getBoolean("visual_flash",false));root.addView(visualFlash);visualFlash.setOnCheckedChangeListener((button,enabled)->{if(enabled)new AlertDialog.Builder(this).setTitle("Aviso sobre destellos").setMessage("Los cambios rápidos de luz pueden afectar a personas con fotosensibilidad. Activá esta opción solo si es adecuada para vos.").setPositiveButton("Activar",(d,w)->prefs.edit().putBoolean("visual_flash",true).apply()).setNegativeButton("Cancelar",(d,w)->visualFlash.setChecked(false)).show();else prefs.edit().putBoolean("visual_flash",false).apply();});
 root.addView(txt("MIS ALARMAS",22));
 for(int id=0;id<5;id++)alarmRow(id);
 root.addView(txt("TEMPORIZADOR TÁCTIL",22));
 long savedEnd=prefs.getLong("timer_end",0);long remaining=Math.max(0,savedEnd-System.currentTimeMillis());TextView counter=txt(remaining>0?String.format(Locale.getDefault(),"%02d:%02d",remaining/60000,(remaining/1000)%60):"05:00",32);root.addView(counter);if(remaining>0){timer=new CountDownTimer(remaining,1000){public void onTick(long ms){counter.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){counter.setText("Esperando aviso…");}}.start();}
 Button five=btn("Iniciar 5 minutos");root.addView(five);five.setOnClickListener(v->countdown(5,counter));
 Button ten=btn("Iniciar 10 minutos");root.addView(ten);ten.setOnClickListener(v->countdown(10,counter));
 Button stop=btn("Detener temporizador");root.addView(stop);stop.setOnClickListener(v->{if(timer!=null)timer.cancel();cancelTimer();counter.setText("Detenido");});
 root.addView(txt("COMUNICACIÓN VISUAL",22));
 EditText msg=new EditText(this);msg.setTextColor(Color.WHITE);msg.setHintTextColor(0xffaebdd1);msg.setHint("Escribí un mensaje");root.addView(msg);
 Button show=btn("Mostrar mensaje grande");root.addView(show);show.setOnClickListener(v->{String message=msg.getText().toString().trim();if(!message.isEmpty())showFullMessage(message);});
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
 void alarmRow(int id){
 int h=prefs.getInt("h"+id,7),m=prefs.getInt("m"+id,0);
 Switch sw=new Switch(this);sw.setText(String.format(Locale.getDefault(),"Alarma %d   %02d:%02d",id+1,h,m));sw.setTextColor(Color.WHITE);sw.setTextSize(20);sw.setChecked(prefs.getBoolean("on"+id,false));root.addView(sw);
 sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("on"+id,on).apply();schedule(this,id);if(on&&prefs.getInt("days"+id,127)==0)Toast.makeText(this,"Elegí al menos un día para esta alarma",Toast.LENGTH_LONG).show();});
 Button days=btn("Días de la alarma · "+(id+1));root.addView(days);days.setOnClickListener(v->{String[] labels={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};boolean[] checked=new boolean[7];int mask=prefs.getInt("days"+id,127);for(int j=0;j<7;j++)checked[j]=(mask&(1<<j))!=0;new AlertDialog.Builder(this).setTitle("Elegí los días").setMultiChoiceItems(labels,checked,(d,which,on)->checked[which]=on).setPositiveButton("Guardar",(d,w)->{int msk=0;for(int j=0;j<7;j++)if(checked[j])msk|=(1<<j);prefs.edit().putInt("days"+id,msk).apply();schedule(this,id);if(msk==0)Toast.makeText(this,"Sin días seleccionados: esta alarma no sonará",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancelar",null).show();});
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
  timer=new CountDownTimer(minutes*60000L,1000){public void onTick(long ms){output.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){output.setText("Esperando aviso…");}}.start();
 }
 void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator()){long[] pat={0,800,200,800,200,800};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}}
 @Override protected void onDestroy(){if(timer!=null)timer.cancel();if(alarmActive)stopAlarmEffects();super.onDestroy();}
}