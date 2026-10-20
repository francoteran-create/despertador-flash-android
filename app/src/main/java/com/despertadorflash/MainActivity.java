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
 LinearLayout root; SharedPreferences prefs; CountDownTimer timer; android.os.Handler flashHandler=new android.os.Handler(android.os.Looper.getMainLooper()); Runnable flashTask;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(5),dp(12),dp(5),dp(12));return v;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(17);b.setMinHeight(dp(58));return b;}
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,0);render();if(getIntent().getBooleanExtra("ring",false))showAlarm();if(getIntent().getBooleanExtra("timer_finished",false))new AlertDialog.Builder(this).setTitle("AURORA").setMessage("¡Tiempo cumplido!").setPositiveButton("Aceptar",null).show();}
 void stopAlarmEffects(){
  if(flashTask!=null){flashHandler.removeCallbacks(flashTask);flashTask=null;}
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);for(int id=0;id<5;id++)nm.cancel(2000+id);
 }
 void showAlarm(){
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(24),dp(22),dp(24));
  TextView big=txt("¡DESPERTÁ!",38);big.setGravity(Gravity.CENTER);big.setTypeface(null,Typeface.BOLD);panel.addView(big);
  TextView sub=txt("AURORA está avisándote. Tocá el botón para detener la alerta.",19);sub.setGravity(Gravity.CENTER);panel.addView(sub);
  final boolean[] bright={false};
  flashTask=new Runnable(){public void run(){bright[0]=!bright[0];panel.setBackgroundColor(bright[0]?0xffffffff:0xff10182e);big.setTextColor(bright[0]?0xff08162c:0xffffffff);sub.setTextColor(bright[0]?0xff08162c:0xffffffff);flashHandler.postDelayed(this,650);}};
  flashHandler.post(flashTask);
  Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);
  if(v!=null&&v.hasVibrator()){long[] pattern={0,900,300,900,300,900,300};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pattern,0));else v.vibrate(pattern,0);}
  new AlertDialog.Builder(this).setView(panel).setPositiveButton("ESTOY DESPIERTO",(d,w)->stopAlarmEffects()).setOnDismissListener(d->stopAlarmEffects()).setCancelable(false).show();
 }

 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("ring",false))showAlarm();if(i.getBooleanExtra("timer_finished",false))new AlertDialog.Builder(this).setTitle("AURORA").setMessage("¡Tiempo cumplido!").setPositiveButton("Aceptar",null).show();}
 @Override protected void onResume(){super.onResume();if(prefs!=null){for(int id=0;id<5;id++){if(prefs.getBoolean("on"+id,false))schedule(this,id);}}}
 void render(){
 if(timer!=null){timer.cancel();timer=null;}
 ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setBackgroundColor(0xff0b1325);scroll.addView(root);setContentView(scroll);
 TextView title=txt("✦ AURORA",34);title.setTextColor(0xff91f0dd);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 root.addView(txt("Tu mañana, a tu manera.",21));root.addView(txt("Despertador accesible con vibración y avisos visuales.",15));
 AlarmManager alarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
 if(Build.VERSION.SDK_INT>=31&&!alarmManager.canScheduleExactAlarms()){TextView warning=txt("ATENCIÓN: las alarmas exactas no están habilitadas. Activá el permiso para que el despertador funcione a la hora elegida.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){TextView warning=txt("ATENCIÓN: faltan permisos de notificaciones. Los avisos pueden no aparecer.",17);warning.setTextColor(0xffffcf70);root.addView(warning);}
 Button perm=btn("Permitir alarmas exactas");root.addView(perm);perm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);if(!a.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}});
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
 root.addView(txt("MIS ALARMAS",22));
 for(int id=0;id<5;id++)alarmRow(id);
 root.addView(txt("TEMPORIZADOR TÁCTIL",22));
 long savedEnd=prefs.getLong("timer_end",0);long remaining=Math.max(0,savedEnd-System.currentTimeMillis());TextView counter=txt(remaining>0?String.format(Locale.getDefault(),"%02d:%02d",remaining/60000,(remaining/1000)%60):"05:00",32);root.addView(counter);if(remaining>0){timer=new CountDownTimer(remaining,1000){public void onTick(long ms){counter.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){counter.setText("Esperando aviso…");}}.start();}
 Button five=btn("Iniciar 5 minutos");root.addView(five);five.setOnClickListener(v->countdown(5,counter));
 Button ten=btn("Iniciar 10 minutos");root.addView(ten);ten.setOnClickListener(v->countdown(10,counter));
 Button stop=btn("Detener temporizador");root.addView(stop);stop.setOnClickListener(v->{if(timer!=null)timer.cancel();cancelTimer();counter.setText("Detenido");});
 root.addView(txt("COMUNICACIÓN VISUAL",22));
 EditText msg=new EditText(this);msg.setTextColor(Color.WHITE);msg.setHintTextColor(0xffaebdd1);msg.setHint("Escribí un mensaje");root.addView(msg);
 Button show=btn("Mostrar mensaje grande");root.addView(show);show.setOnClickListener(v->{String s=msg.getText().toString().trim();if(!s.isEmpty()){TextView big=txt(s,30);big.setGravity(Gravity.CENTER);new AlertDialog.Builder(this).setView(big).setPositiveButton("Cerrar",null).show();}});
 Button test=btn("Probar vibración");root.addView(test);test.setOnClickListener(v->vibrate());
 root.addView(txt("Importante: este prototipo no reemplaza alarmas de emergencia certificadas. La ejecución en segundo plano depende de Android y del fabricante.",13));
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
 @Override protected void onDestroy(){if(timer!=null)timer.cancel();if(flashTask!=null)flashHandler.removeCallbacks(flashTask);super.onDestroy();}
}