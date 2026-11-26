package com.despertadorflash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

/** Proud cloud mascot: monochrome graffiti, violet highlights, tiny idle gesture. */
public final class AuroraHeroCloudView extends View {
 public static final String PREF_CHARACTER="hero_character";
 private static final int VIOLET=0xffbe18ed;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path shape=new Path();
 private final PowerManager power;
 private boolean active=false,reducedMotion=false;
 private final Runnable frame=()->invalidate();
 private int character;
 public AuroraHeroCloudView(Context c){
  super(c);
  power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
  character=c.getSharedPreferences("aurora",0).getInt(PREF_CHARACTER,0)==1?1:0;
  setCharacterDescription();
  setFocusable(true);
  float scale=1f;
  try{scale=Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f);}
  catch(Exception ignored){}
  reducedMotion=scale==0f||(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled());
 }
 private void setCharacterDescription(){
  setContentDescription((character==0?"Nube urbana":"Nube alternativa")+". Tocá para cambiar de personaje");
 }
 public void setCharacter(int selected){character=selected==1?1:0;setCharacterDescription();invalidate();}
 public int getCharacter(){return character;}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();active=true;invalidate();}
 @Override protected void onDetachedFromWindow(){active=false;removeCallbacks(frame);super.onDetachedFromWindow();}
 @Override protected void onMeasure(int w,int h){
  int width=View.MeasureSpec.getSize(w),height=View.MeasureSpec.getSize(h);
  if(View.MeasureSpec.getMode(h)==View.MeasureSpec.UNSPECIFIED)height=width;
  setMeasuredDimension(width,Math.max(1,height));
 }
 private void stroke(int color,float width){
  p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.STROKE);
  p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(width);
 }
 private void fill(int color){p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.FILL);}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  boolean animate=active&&isShown()&&!reducedMotion&&!(power!=null&&power.isPowerSaveMode());
  float t=animate?SystemClock.uptimeMillis()/1000f:0f;
  float bob=animate?(float)Math.sin(t*1.30f)*.025f:0f;
  canvas.save();canvas.translate(w*.5f,h*.5f+bob*w);
  float scale=Math.min(w/2.95f,h/3.0f);canvas.scale(scale,scale);
  // Graffiti aura: restrained spray rings, hand-drawn violet orbit.
  stroke(0x885e116d,.14f);canvas.drawArc(-1.28f,-1.32f,1.29f,1.32f,-35f,265f,false,p);
  stroke(VIOLET,.045f);canvas.drawArc(-1.29f,-1.30f,1.27f,1.29f,54f,148f,false,p);
  stroke(0x99ffffff,.027f);
  canvas.drawLine(-1.16f,-1.08f,-.91f,-1.33f,p);
  canvas.drawLine(1.12f,-1.05f,1.27f,-1.18f,p);
  // Hoodie underneath face.
  fill(Color.BLACK);canvas.drawRoundRect(-.80f,.10f,.83f,1.06f,.31f,.31f,p);
  stroke(Color.WHITE,.085f);canvas.drawRoundRect(-.80f,.10f,.83f,1.06f,.31f,.31f,p);
  stroke(VIOLET,.047f);canvas.drawLine(-.43f,.80f,.42f,.80f,p);
  // Character cloud: white with black crisp outline and mild grey halftone effect.
  shape.reset();shape.moveTo(-.93f,.23f);
  shape.cubicTo(-1.29f,.13f,-1.24f,-.37f,-.94f,-.49f);
  shape.cubicTo(-1.09f,-.91f,-.68f,-1.17f,-.28f,-.99f);
  shape.cubicTo(.10f,-1.31f,.66f,-1.13f,.77f,-.72f);
  shape.cubicTo(1.19f,-.77f,1.34f,-.30f,1.08f,-.08f);
  shape.cubicTo(1.39f,.23f,1.13f,.62f,.75f,.60f);
  shape.cubicTo(.20f,.81f,-.52f,.72f,-.93f,.23f);shape.close();
  fill(0xffeeeeee);canvas.drawPath(shape,p);
  stroke(Color.BLACK,.065f);canvas.drawPath(shape,p);
  stroke(Color.WHITE,.032f);canvas.drawPath(shape,p);
  // Purple headphones with a black headband.
  stroke(Color.BLACK,.18f);canvas.drawArc(-.82f,-1.11f,.76f,.50f,185f,172f,false,p);
  stroke(VIOLET,.055f);canvas.drawArc(-.82f,-1.11f,.76f,.50f,185f,172f,false,p);
  fill(Color.BLACK);canvas.drawRoundRect(-1.13f,-.61f,-.78f,.17f,.13f,.13f,p);
  stroke(VIOLET,.065f);canvas.drawRoundRect(-1.13f,-.61f,-.78f,.17f,.13f,.13f,p);
  fill(Color.WHITE);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
  p.setTextSize(.31f);p.setTextAlign(Paint.Align.CENTER);canvas.drawText("A",-.96f,-.10f,p);
  // Real visual distinction: the two options differ in accessories, eyes,
  // eyebrows, headwear and expression, not merely a hidden preference.
  if(character==0){
   fill(Color.BLACK);
   canvas.drawRoundRect(-.64f,-.35f,.66f,-.04f,.09f,.09f,p);
   fill(VIOLET);canvas.drawRect(-.48f,-.29f,-.23f,-.21f,p);
   canvas.drawRect(.27f,-.29f,.48f,-.21f,p);
  }else{
   // Alternative lead cloud: confident expressive eyes, bold lashes,
   // violet hair sweep and a visible star badge. Never stereotyping users.
   stroke(0xff222222,.078f);
   canvas.drawArc(-.62f,-.54f,-.10f,-.03f,194f,152f,false,p);
   canvas.drawArc(.10f,-.54f,.63f,-.03f,194f,152f,false,p);
   fill(Color.BLACK);
   canvas.drawOval(-.45f,-.29f,-.24f,-.10f,p);
   canvas.drawOval(.29f,-.29f,.50f,-.10f,p);
   stroke(Color.BLACK,.048f);
   canvas.drawLine(-.58f,-.29f,-.68f,-.37f,p);
   canvas.drawLine(.58f,-.28f,.70f,-.37f,p);
   stroke(VIOLET,.092f);
   Path swept=new Path();swept.moveTo(-.20f,-1.06f);
   swept.cubicTo(.16f,-1.27f,.61f,-1.17f,.78f,-.89f);
   swept.cubicTo(.93f,-.61f,.80f,-.56f,.64f,-.59f);
   canvas.drawPath(swept,p);
   stroke(Color.BLACK,.047f);
   canvas.drawLine(.57f,-.78f,.85f,-.53f,p);
  }
  stroke(Color.BLACK,.055f);
  shape.reset();shape.moveTo(-.25f,.12f);shape.quadTo(.02f,.33f,.31f,.09f);
  canvas.drawPath(shape,p);
  // Crossed arms, confident pose.
  stroke(Color.BLACK,.20f);
  canvas.drawLine(-.60f,.59f,.45f,.95f,p);
  canvas.drawLine(.62f,.55f,-.36f,.95f,p);
  stroke(Color.WHITE,.10f);
  canvas.drawLine(-.59f,.60f,.43f,.94f,p);
  canvas.drawLine(.60f,.56f,-.33f,.93f,p);
  fill(Color.WHITE);p.setTextSize(.36f);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
  canvas.drawText(character==0?"A":"✦",.06f,.82f,p);
  canvas.restore();
  removeCallbacks(frame);
  if(animate)postDelayed(frame,220L);
 }
}
