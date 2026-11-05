package com.despertadorflash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** An optional, offline personal profile. CUD status is self-declared, not verified. */
public final class ProfileActivity extends Activity {
 private static final int PICK_PHOTO=124;
 private static final String PREF="aurora_profile";
 private SharedPreferences prefs;
 private EditText name, nickname, birthDate, contact, notes;
 private Spinner cud;
 private ImageView portrait;
 private TextView portraitHint, savedNote;
 private boolean usePhoto=false;
 private int avatarColor=0, avatarStyle=0;
 private final int[] colors={0xff4f87b6,0xff7254bb,0xff248b84,0xffba6f40,0xffb45881,0xff546b8c};
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private GradientDrawable gradient(int a,int b){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(18));d.setStroke(dp(1),0xff447c9d);return d;}
 private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xffe1f4ff);v.setPadding(dp(8),dp(9),dp(8),dp(5));return v;}
 private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(-1);b.setBackground(gradient(0xff244a70,0xff382f68));return b;}
 private EditText edit(String hint){EditText field=new EditText(this);field.setHint(hint);field.setHintTextColor(0xff94b8cb);field.setTextColor(-1);field.setTextSize(17);field.setSingleLine(true);field.setPadding(dp(14),dp(13),dp(14),dp(13));field.setBackground(gradient(0xff112b3d,0xff1b2244));return field;}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);prefs=getSharedPreferences(PREF,0);avatarColor=prefs.getInt("avatar_color",0);avatarStyle=prefs.getInt("avatar_style",0);usePhoto=prefs.getBoolean("photo",false);draw();}
 private void draw(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackground(new AuroraSkyDrawable());scroll.addView(root);setContentView(scroll);
  Button back=button("← VOLVER");root.addView(back);back.setOnClickListener(v->finish());
  TextView title=label("MI PERFIL AURORA",28);title.setTextColor(0xffa7f0ff);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);root.addView(title);
  TextView intro=label("Personalizá tu identidad en AURORA. Todos estos datos son opcionales y permanecen en este teléfono.",15);intro.setGravity(Gravity.CENTER);root.addView(intro);
  portrait=new ImageView(this);portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams photoParams=new LinearLayout.LayoutParams(dp(150),dp(150));photoParams.gravity=Gravity.CENTER;photoParams.setMargins(0,dp(13),0,dp(4));root.addView(portrait,photoParams);
  portraitHint=label("",14);portraitHint.setGravity(Gravity.CENTER);root.addView(portraitHint);
  refreshPortrait();
  LinearLayout imageActions=new LinearLayout(this);imageActions.setOrientation(LinearLayout.HORIZONTAL);root.addView(imageActions);
  Button choosePhoto=button("FOTO DEL CELULAR");imageActions.addView(choosePhoto,new LinearLayout.LayoutParams(0,dp(58),1f));choosePhoto.setOnClickListener(v->pickPhoto());
  Button chooseAvatar=button("CREAR AVATAR");LinearLayout.LayoutParams avatarBtnParams=new LinearLayout.LayoutParams(0,dp(58),1f);avatarBtnParams.leftMargin=dp(8);imageActions.addView(chooseAvatar,avatarBtnParams);chooseAvatar.setOnClickListener(v->showAvatarEditor());
  root.addView(label("DATOS PERSONALES",20));
  root.addView(label("Nombre (opcional)",14));name=edit("¿Cómo querés que te llamemos?");name.setText(prefs.getString("name",""));root.addView(name);
  root.addView(label("Apodo o nombre para mostrar (opcional)",14));nickname=edit("Nombre visible en AURORA");nickname.setText(prefs.getString("nickname",""));root.addView(nickname);
  root.addView(label("Fecha de nacimiento (opcional)",14));birthDate=edit("DD/MM/AAAA");birthDate.setInputType(android.text.InputType.TYPE_NULL);birthDate.setFocusable(false);birthDate.setText(prefs.getString("birth",""));root.addView(birthDate);birthDate.setOnClickListener(v->showBirthdayPicker());
  root.addView(label("Contacto de confianza (opcional)",14));contact=edit("Nombre o teléfono de contacto");contact.setText(prefs.getString("contact",""));root.addView(contact);
  TextView cudTitle=label("CERTIFICADO ÚNICO DE DISCAPACIDAD (CUD)",20);root.addView(cudTitle);
  TextView cudHelp=label("Indicá si contás con un CUD. Es una declaración personal, no una verificación oficial. No se necesita cargar el certificado.",14);root.addView(cudHelp);
  cud=new Spinner(this);
  String[] cudValues={"Prefiero no indicarlo","Sí, tengo CUD","No tengo CUD","En trámite"};
  ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cudValues);cud.setAdapter(a);
  cud.setSelection(Math.max(0,Math.min(3,prefs.getInt("cud",0))));root.addView(cud);
  root.addView(label("Preferencias o información que quieras compartir (opcional)",14));notes=edit("Ej.: prefiero comunicarme por escrito");notes.setSingleLine(false);notes.setMinLines(2);notes.setText(prefs.getString("notes",""));root.addView(notes);
  savedNote=label("",14);savedNote.setTextColor(0xffa9efcc);root.addView(savedNote);
  Button save=button("✓  GUARDAR MI PERFIL");root.addView(save);save.setMinHeight(dp(65));save.setOnClickListener(v->save());
  Button delete=button("BORRAR DATOS DEL PERFIL");root.addView(delete);delete.setOnClickListener(v->confirmDelete());
  root.addView(label("Privacidad: estos datos y la foto se guardan dentro de AURORA, sin enviarse a servidores. Si desinstalás la aplicación, se pueden perder. El CUD no se usa para determinar derechos ni prestaciones.",13));
 }
 private File photoFile(){return new File(getFilesDir(),"aurora_profile_photo.jpg");}
 private void refreshPortrait(){
  if(usePhoto&&photoFile().exists()){
   Bitmap bitmap=BitmapFactory.decodeFile(photoFile().getAbsolutePath());
   if(bitmap!=null){portrait.setImageBitmap(bitmap);portraitHint.setText("FOTO PERSONAL");return;}
  }
  usePhoto=false;
  portrait.setImageDrawable(new AvatarDrawable());
  portraitHint.setText("AVATAR PERSONALIZADO");
 }
 private void pickPhoto(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");
  try{startActivityForResult(i,PICK_PHOTO);}catch(Exception e){message("No se pudo abrir la galería.");}
 }
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);
  if(req!=PICK_PHOTO||result!=RESULT_OK||data==null||data.getData()==null)return;
  try(InputStream in=getContentResolver().openInputStream(data.getData())){
   if(in==null)throw new IOException("No se pudo leer la imagen");
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
   BitmapFactory.decodeStream(in,null,bounds);
   if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("Formato de imagen no compatible");
   int scale=1;while(bounds.outWidth/scale>1000||bounds.outHeight/scale>1000)scale*=2;
   BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=scale;
   try(InputStream again=getContentResolver().openInputStream(data.getData())){
    if(again==null)throw new IOException("No se pudo volver a abrir la imagen");
    Bitmap b=BitmapFactory.decodeStream(again,null,opts);if(b==null)throw new IOException("Imagen no compatible");
    try(FileOutputStream out=new FileOutputStream(photoFile())){
     if(!b.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("Error al convertir la foto");
    }
    b.recycle();
   }
   usePhoto=true;prefs.edit().putBoolean("photo",true).apply();refreshPortrait();
  }catch(Exception e){message("No se pudo importar la foto: "+e.getMessage());}
 }
 private void showAvatarEditor(){
  String[] options={"Aurora azul · circular","Aurora violeta · circular","Aurora turquesa · circular","Aurora ámbar · circular","Aurora rosa · circular","Aurora gris · circular","Avatar con monograma","Avatar con figura"};
  new AlertDialog.Builder(this).setTitle("PERSONALIZAR AVATAR").setItems(options,(d,which)->{
   if(which<6)avatarColor=which;else avatarStyle=which==6?0:1;
   usePhoto=false;prefs.edit().putInt("avatar_color",avatarColor).putInt("avatar_style",avatarStyle).putBoolean("photo",false).apply();
   refreshPortrait();
  }).setPositiveButton("LISTO",null).show();
 }
 private void showBirthdayPicker(){
  java.util.Calendar c=java.util.Calendar.getInstance();
  String existing=birthDate.getText().toString();
  if(existing.matches("\\d{2}/\\d{2}/\\d{4}")){
   try{String[] p=existing.split("/");c.set(Integer.parseInt(p[2]),Integer.parseInt(p[1])-1,Integer.parseInt(p[0]));}catch(Exception ignored){}
  }
  android.app.DatePickerDialog picker=new android.app.DatePickerDialog(this,(v,y,m,d)->birthDate.setText(String.format(Locale.ROOT,"%02d/%02d/%04d",d,m+1,y)),c.get(java.util.Calendar.YEAR),c.get(java.util.Calendar.MONTH),c.get(java.util.Calendar.DAY_OF_MONTH));
  picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();
 }
 private void save(){
  String n=name.getText().toString().trim();
  String display=nickname.getText().toString().trim();
  if(n.length()>100||display.length()>100||contact.length()>160||notes.length()>500){message("Acortá los textos del perfil para poder guardarlos.");return;}
  prefs.edit().putString("name",n).putString("nickname",display).putString("birth",birthDate.getText().toString())
   .putString("contact",contact.getText().toString().trim()).putString("notes",notes.getText().toString().trim())
   .putInt("cud",cud.getSelectedItemPosition()).putInt("avatar_color",avatarColor).putInt("avatar_style",avatarStyle)
   .putBoolean("photo",usePhoto).apply();
  savedNote.setText("PERFIL GUARDADO EN ESTE TELÉFONO");
  refreshPortrait();
 }
 private void confirmDelete(){
  new AlertDialog.Builder(this).setTitle("BORRAR PERFIL").setMessage("¿Querés borrar el nombre, los datos sobre CUD, las preferencias y la foto? Las alarmas y los historiales no se modifican.")
   .setNegativeButton("CANCELAR",null).setPositiveButton("BORRAR",(d,w)->{
    prefs.edit().clear().apply();photoFile().delete();avatarColor=0;avatarStyle=0;usePhoto=false;draw();message("Perfil borrado");
   }).show();
 }
 private void message(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 private final class AvatarDrawable extends Drawable {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  @Override public void draw(Canvas canvas){
   android.graphics.Rect b=getBounds();float cx=b.exactCenterX(),cy=b.exactCenterY(),rad=Math.min(b.width(),b.height())/2.1f;
   paint.setStyle(Paint.Style.FILL);paint.setColor(colors[Math.max(0,Math.min(colors.length-1,avatarColor))]);canvas.drawCircle(cx,cy,rad,paint);
   paint.setColor(0x33ffffff);canvas.drawCircle(cx-rad*0.28f,cy-rad*0.33f,rad*0.54f,paint);
   paint.setColor(Color.WHITE);
   if(avatarStyle==0){
    String initials=prefs.getString("nickname","").trim();
    if(initials.isEmpty())initials=prefs.getString("name","").trim();
    if(initials.isEmpty())initials="A";
    String[] parts=initials.split("\\s+");String letters=parts[0].substring(0,1).toUpperCase(Locale.ROOT);
    if(parts.length>1)letters+=parts[parts.length-1].substring(0,1).toUpperCase(Locale.ROOT);
    paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));paint.setTextSize(rad*0.9f);paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText(letters,cx,cy-(paint.ascent()+paint.descent())/2f,paint);
   }else{
    canvas.drawCircle(cx,cy-rad*0.29f,rad*0.29f,paint);
    RectF shoulders=new RectF(cx-rad*0.62f,cy+rad*0.02f,cx+rad*0.62f,cy+rad*0.78f);
    canvas.drawRoundRect(shoulders,rad*0.29f,rad*0.29f,paint);
   }
  }
  @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
  @Override public void setColorFilter(android.graphics.ColorFilter cf){paint.setColorFilter(cf);}
  @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
}
