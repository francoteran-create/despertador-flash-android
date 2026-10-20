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
 LinearLayout root; SharedPreferences prefs; CountDownTimer timer;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(5),dp(12),dp(5),dp(12));return v;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(17);b.setMinHeight(dp(58));return b;}
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREF,0);render();if(getIntent().getBooleanExtra("ring",false))showAlarm();}
 void showAlarm(){getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);new AlertDialog.Builder(this).setTitle("AURORA · ¡DESPERTÁ!").setMessage("Es hora de despertar.").setPositiveButton("Estoy despierto",(d,w)->{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}).setCancelable(false).show();}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("ring",false))showAlarm();}
 @Override protected void onResume(){super.onResume();if(prefs!=null){for(int id=0;id<5;id++){if(prefs.getBoolean("on"+id,false))schedule(this,id);}}}
 void render(){
 ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setBackgroundColor(0xff0b1325);scroll.addView(root);setContentView(scroll);
 TextView title=txt("✦ AURORA",34);title.setTextColor(0xff91f0dd);title.setTypeface(null,Typeface.BOLD);root.addView(title);
 root.addView(txt("Tu mañana, a tu manera.",21));root.addView(txt("Despertador accesible con vibración y avisos visuales.",15));
 Button perm=btn("Permitir alarmas exactas");root.addView(perm);perm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);if(!a.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}});
 if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
 root.addView(txt("MIS ALARMAS",22));
 for(int id=0;id<5;id++)alarmRow(id);
 root.addView(txt("TEMPORIZADOR TÁCTIL",22));
 TextView counter=txt("05:00",32);root.addView(counter);
 Button five=btn("Iniciar 5 minutos");root.addView(five);five.setOnClickListener(v->countdown(5,counter));
 Button ten=btn("Iniciar 10 minutos");root.addView(ten);ten.setOnClickListener(v->countdown(10,counter));
 Button stop=btn("Detener temporizador");root.addView(stop);stop.setOnClickListener(v->{if(timer!=null)timer.cancel();counter.setText("Detenido");});
 root.addView(txt("COMUNICACIÓN VISUAL",22));
 EditText msg=new EditText(this);msg.setTextColor(Color.WHITE);msg.setHintTextColor(0xffaebdd1);msg.setHint("Escribí un mensaje");root.addView(msg);
 Button show=btn("Mostrar mensaje grande");root.addView(show);show.setOnClickListener(v->{String s=msg.getText().toString().trim();if(!s.isEmpty()){TextView big=txt(s,30);big.setGravity(Gravity.CENTER);new AlertDialog.Builder(this).setView(big).setPositiveButton("Cerrar",null).show();}});
 Button test=btn("Probar vibración");root.addView(test);test.setOnClickListener(v->vibrate());
 root.addView(txt("Importante: este prototipo no reemplaza alarmas de emergencia certificadas. La ejecución en segundo plano depende de Android y del fabricante.",13));
 }
 void alarmRow(int id){
 int h=prefs.getInt("h"+id,7),m=prefs.getInt("m"+id,0);
 Switch sw=new Switch(this);sw.setText(String.format(Locale.getDefault(),"Alarma %d   %02d:%02d",id+1,h,m));sw.setTextColor(Color.WHITE);sw.setTextSize(20);sw.setChecked(prefs.getBoolean("on"+id,false));root.addView(sw);
 sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("on"+id,on).apply();schedule(this,id);});
 Button edit=btn("Cambiar hora · "+(id+1));root.addView(edit);edit.setOnClickListener(v->new TimePickerDialog(this,(picker,hh,mm)->{prefs.edit().putInt("h"+id,hh).putInt("m"+id,mm).putBoolean("on"+id,true).apply();schedule(this,id);render();},h,m,true).show());
 }
 static void schedule(Context c,int id){
 SharedPreferences p=c.getSharedPreferences(PREF,0);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 Intent i=new Intent(c,AlarmReceiver.class).putExtra("id",id);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 am.cancel(pi);if(!p.getBoolean("on"+id,false))return;
 Calendar at=Calendar.getInstance();at.set(Calendar.HOUR_OF_DAY,p.getInt("h"+id,7));at.set(Calendar.MINUTE,p.getInt("m"+id,0));at.set(Calendar.SECOND,0);at.set(Calendar.MILLISECOND,0);if(at.getTimeInMillis()<=System.currentTimeMillis())at.add(Calendar.DAY_OF_YEAR,1);
 if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){android.util.Log.w("Aurora","Permiso de alarmas exactas pendiente");return;}
 try{am.setAlarmClock(new AlarmManager.AlarmClockInfo(at.getTimeInMillis(),PendingIntent.getActivity(c,100+id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)),pi);}catch(SecurityException e){android.util.Log.e("Aurora","Exact alarm permission",e);}
 }
 void countdown(int minutes,TextView output){if(timer!=null)timer.cancel();timer=new CountDownTimer(minutes*60000L,1000){public void onTick(long ms){output.setText(String.format(Locale.getDefault(),"%02d:%02d",ms/60000,(ms/1000)%60));}public void onFinish(){output.setText("¡Tiempo!");vibrate();new AlertDialog.Builder(MainActivity.this).setTitle("Tiempo cumplido").setPositiveButton("Aceptar",null).show();}}.start();}
 void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator()){long[] pat={0,800,200,800,200,800};if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createWaveform(pat,-1));else v.vibrate(pat,-1);}}
 @Override protected void onDestroy(){if(timer!=null)timer.cancel();super.onDestroy();}
}