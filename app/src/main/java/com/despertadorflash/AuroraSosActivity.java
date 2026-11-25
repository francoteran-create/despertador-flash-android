package com.despertadorflash;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** Manual SOS to a trusted contact: preview, optional recent location, confirm in SMS app. */
public final class AuroraSosActivity extends Activity {
 private static final String PREF="aurora_sos";
 private EditText phone,message;
 private CheckBox withLocation;
 private TextView status;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(dp(5),dp(10),dp(5),dp(10));return t;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setMinHeight(dp(58));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff582c41));return b;}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(12),dp(16),dp(30));scroll.addView(panel);setContentView(scroll);
  Button back=button("← VOLVER A COMUNICAR");panel.addView(back);back.setOnClickListener(v->finish());
  TextView heading=text("SOS · CONTACTO DE CONFIANZA",25);heading.setGravity(Gravity.CENTER);panel.addView(heading);
  panel.addView(text("Esta opción prepara un mensaje para una persona elegida. No llama a emergencias ni envía nada automáticamente. En peligro inmediato, utilizá el servicio local de emergencias.",15));
  phone=new EditText(this);phone.setTextColor(Color.WHITE);phone.setHintTextColor(0xffaaaaaa);phone.setHint("Número del contacto, con código de área");phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
  phone.setText(getSharedPreferences(PREF,MODE_PRIVATE).getString("contact",""));panel.addView(phone);
  Button save=button("GUARDAR ESTE CONTACTO EN EL TELÉFONO");panel.addView(save);
  save.setOnClickListener(v->{String n=phone.getText().toString().trim();if(!n.matches("[+0-9() .-]{6,24}")){status.setText("Revisá el número. No se guardó.");return;}getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("contact",n).apply();status.setText("Contacto guardado solo en AURORA de este teléfono.");});
  panel.addView(text("MENSAJE QUE RECIBIRÁ TU CONTACTO",17));
  message=new EditText(this);message.setTextColor(Color.WHITE);message.setHintTextColor(0xffaaaaaa);message.setMinLines(3);message.setTextSize(19);
  message.setText("Soy una persona sorda o con discapacidad auditiva. Necesito ayuda. Por favor, respondeme por mensaje de texto.");
  panel.addView(message);
  withLocation=new CheckBox(this);withLocation.setText("Agregar mi ubicación reciente, solo si la autorizo");withLocation.setTextColor(Color.WHITE);withLocation.setChecked(false);panel.addView(withLocation);
  withLocation.setOnCheckedChangeListener((b,checked)->{if(checked&&Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},704);}});
  status=text("El envío requiere confirmación dentro de la aplicación de mensajes.",14);panel.addView(status);
  Button sms=button("PREPARAR SMS · REVISAR Y CONFIRMAR");panel.addView(sms);
  sms.setOnClickListener(v->prepare(false));
  Button share=button("COMPARTIR MENSAJE EN OTRA APP");panel.addView(share);
  share.setOnClickListener(v->prepare(true));
  panel.addView(text("Importante: SOS no verifica que el destinatario haya recibido el mensaje. La posición, si está disponible, puede ser aproximada. La confirmación final del envío siempre sucede fuera de AURORA.",13));
 }
 private Location recentPosition(){
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return null;
  try{
   LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
   if(lm==null)return null;
   Location best=null;
   for(String provider:lm.getProviders(true)){
    Location candidate=lm.getLastKnownLocation(provider);
    if(candidate==null)continue;
    long age=SystemClock.elapsedRealtimeNanos()-candidate.getElapsedRealtimeNanos();
    if(age<0||age>5L*60*1000000000L)continue;
    if(best==null||candidate.getAccuracy()<best.getAccuracy())best=candidate;
   }
   return best;
  }catch(SecurityException ignored){return null;}
 }
 private String fullMessage(){
  String base=message.getText().toString().trim();
  if(!withLocation.isChecked())return base;
  Location loc=recentPosition();
  if(loc==null){status.setText("No hay ubicación reciente disponible. Se enviará el mensaje SIN ubicación.");return base;}
  return base+"\nUbicación aproximada: https://maps.google.com/?q="+String.format(Locale.US,"%.6f,%.6f",loc.getLatitude(),loc.getLongitude());
 }
 private void prepare(boolean otherApp){
  String number=phone.getText().toString().trim();
  if(!otherApp&&!number.matches("[+0-9() .-]{6,24}")){status.setText("Elegí un número válido antes de preparar SMS.");return;}
  if(message.getText().toString().trim().isEmpty()){status.setText("Escribí un mensaje antes de continuar.");return;}
  final String body=fullMessage();
  final String target=number;
  new AlertDialog.Builder(this)
   .setTitle("REVISAR MENSAJE SOS")
   .setMessage((otherApp?"Seleccionarás una aplicación.\n":"Destinatario: "+target+"\n")+
    "\n"+body+"\n\nAURORA no lo enviará automáticamente.")
   .setNegativeButton("CANCELAR",null)
   .setPositiveButton("ABRIR PARA ENVIAR",(dialog,which)->{
    if(otherApp)openShare(body);else openSms(target,body);
   }).show();
 }
 private void openSms(String number,String body){
  Intent intent=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(number)));
  intent.putExtra("sms_body",body);
  try{startActivity(intent);status.setText("SMS preparado. Confirmá el envío desde tu aplicación de mensajes.");}
  catch(ActivityNotFoundException e){status.setText("No hay aplicación SMS disponible. Probá COMPARTIR MENSAJE EN OTRA APP.");}
 }
 private void openShare(String body){
  Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
  share.putExtra(Intent.EXTRA_TEXT,body);
  try{startActivity(Intent.createChooser(share,"Compartir SOS con un contacto"));}
  catch(ActivityNotFoundException e){status.setText("No se encontró una aplicación compatible.");}
 }
}
