package com.despertadorflash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import java.util.Random;

/**
 * Interactive, entirely native vector character. Buttons are real Android controls.
 * No video, embedded browser, external image or external rendering SDK.
 */
public final class AuroraCharacterStage extends FrameLayout {
 public interface OnFeature { void open(int which); }
 public static final int SOUNDS=0, VOICE=1, ALARMS=2, MESSAGES=3, PROFILE=4, RECORDS=5;
 private final String[] labels={"◖)) SONIDOS","◉ VOZ","◷ ALARMAS","▣ MENSAJES","◉ PERFIL","▤ REGISTROS"};
 private final Button[] features=new Button[6];
 private final Sketch drawing;
 private final OnFeature callback;
 private int selected=-1;
 private long gestureStart=0L;
 private boolean reduceMotion=false;
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 public AuroraCharacterStage(Context c,OnFeature open){
  super(c);callback=open;
  setClipChildren(false);setClipToPadding(false);
  drawing=new Sketch(c);addView(drawing,new LayoutParams(-1,-1));
  for(int i=0;i<features.length;i++){
   final int index=i;Button b=new Button(c);
   b.setText(labels[i]);b.setAllCaps(false);b.setTextSize(11);
   b.setTextColor(0xfff1f4f8);b.setGravity(Gravity.CENTER);
   b.setPadding(dp(3),dp(3),dp(3),dp(3));
   b.setContentDescription("Abrir "+labels[i].replaceAll("[^\\p{L}\\p{N} ]","").trim());
   b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);
   b.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
   b.setBackground(bubble(false));
   features[i]=b;addView(b,new LayoutParams(dp(122),dp(50)));
   b.setOnClickListener(v->activate(index));
  }
  setFocusable(false);
  try{reduceMotion=Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;}catch(Exception ignored){}
 }
 private android.graphics.drawable.Drawable bubble(boolean hot){
  GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
   new int[]{hot?0xff145055:0xff10131b,hot?0xff132c38:0xff0e0e17});
  g.setCornerRadius(dp(18));
  g.setStroke(dp(hot?2:1),hot?0xff81e3d5:0xff56616c);
  return new RippleDrawable(ColorStateList.valueOf(0x4453d8d2),g,null);
 }
 public void activate(int which){
  if(which<0||which>=6)return;
  selected=which;gestureStart=System.currentTimeMillis();
  for(int j=0;j<features.length;j++)features[j].setBackground(bubble(j==which));
  drawing.invalidate();
  // Feature opens immediately; the character's reaction never blocks access.
  if(callback!=null)callback.open(which);
 }
 @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
  int width=MeasureSpec.getSize(widthMeasureSpec);
  int target=(int)(width*1.16f);
  super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(target,MeasureSpec.EXACTLY));
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
  super.onSizeChanged(w,h,oldw,oldh);
  // Bubble grid leaves the middle of the stage to the animated character.
  float[][] places={{.02f,.13f},{.63f,.13f},{.015f,.43f},{.65f,.43f},{.02f,.74f},{.64f,.74f}};
  for(int i=0;i<features.length;i++){
   LayoutParams lp=(LayoutParams)features[i].getLayoutParams();
   lp.width=Math.round(w*.34f);lp.height=Math.round(h*.115f);
   lp.leftMargin=Math.round(w*places[i][0]);lp.topMargin=Math.round(h*places[i][1]);
   features[i].setLayoutParams(lp);
  }
 }
 private final class Sketch extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path line=new Path();
  private final float[] stars=new float[52];
  private final long created=System.currentTimeMillis();
  Sketch(Context c){super(c);Random rand=new Random(445);
   for(int i=0;i<stars.length;i++)stars[i]=rand.nextFloat();
   setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }
  private void path(Canvas c,float width,int color,float... coords){
   p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);
   p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   line.reset();line.moveTo(coords[0],coords[1]);
   for(int i=2;i<coords.length;i+=2)line.lineTo(coords[i],coords[i+1]);
   c.drawPath(line,p);
  }
  private void circle(Canvas c,float x,float y,float r,int color,float lineWidth){
   p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(lineWidth);
   c.drawCircle(x,y,r,p);
  }
  @Override protected void onDraw(Canvas actual){
   float width=getWidth(),height=getHeight();if(width<=0||height<=0)return;
   actual.save();actual.scale(width/360f,height/430f);
   float t=reduceMotion?0:(System.currentTimeMillis()-created)/1000f;
   int chalk=0xffedf0ef,dim=0xff9aa7b2,teal=0xff5fbcb9;
   p.setStyle(Paint.Style.FILL);
   p.setShader(new LinearGradient(0,0,360,430,0xff05070a,0xff08090f,Shader.TileMode.CLAMP));
   actual.drawRoundRect(0,0,360,430,25,25,p);p.setShader(null);
   for(int i=0;i<26;i++){
    p.setColor(i%4==0?0x335ed4c9:0x22567e9a);
    actual.drawCircle(stars[i]*360,stars[(i+17)%52]*430,i%7==0?1.5f:.7f,p);
   }
   path(actual,1.6f,0x554f6477,8,393,56,375,90,394);
   path(actual,1.6f,0x554f6477,268,395,308,378,353,392);
   p.setColor(0xffd9e5e8);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
   p.setTextSize(17);p.setTextAlign(Paint.Align.CENTER);
   actual.drawText("AURORA  /  STREET CONTROL",180,28,p);
   path(actual,2f,teal,118,41,154,41);
   path(actual,2f,0xff6b567a,207,41,244,41);

   float bob=reduceMotion?0:(float)Math.sin(t*1.7f)*2.2f;
   actual.save();actual.translate(0,bob);
   // Shoes and baggy clothing remain hand-drawn independent paths.
   path(actual,5.5f,chalk,154,301,143,336,159,365,138,386);
   path(actual,5.5f,chalk,204,302,218,338,206,363,226,385);
   path(actual,5.1f,chalk,138,385,111,390,104,401,148,404,161,392);
   path(actual,5.1f,chalk,225,384,246,390,259,404,208,403,199,393);
   path(actual,4.8f,chalk,151,311,176,324,204,312);
   path(actual,4.9f,chalk,150,255,137,298,151,315,175,319,207,315,224,293,213,254);
   path(actual,4.1f,chalk,155,265,173,279,189,265);
   path(actual,3f,dim,177,282,177,316);
   // Open hoodie and cap.
   path(actual,4.6f,chalk,147,244,125,267,135,285);
   path(actual,4.6f,chalk,212,245,234,265,220,285);
   path(actual,3f,dim,158,255,176,266,204,255);
   circle(actual,174,291,3,teal,2f);
   // Arms; right hand selects the right or left floated function.
   path(actual,5,chalk,137,270,112,294,125,321);
   path(actual,4,chalk,125,321,133,328,141,322);
   float fx=252f,fy=180f;
   if(selected>=0){float[][] targets={{95,94},{263,94},{94,225},{266,225},{100,354},{267,354}};
    fx=targets[selected][0];fy=targets[selected][1];
   }
   float startX=210,startY=266;
   float bendX=(startX+fx)*.5f+12;
   float bendY=(startY+fy)*.5f-5;
   path(actual,5f,chalk,startX,startY,bendX,bendY,fx,fy);
   circle(actual,fx,fy,5,teal,2.6f);
   if(selected>=0){
    circle(actual,fx,fy,12,0x9957dacf,2f);
    path(actual,2.3f,teal,fx-17,fy-12,fx-22,fy-18);
    path(actual,2.3f,teal,fx+13,fy-15,fx+20,fy-22);
   }
   // Head drawn as a distinctive independent street character.
   p.setColor(0xff090c12);p.setStyle(Paint.Style.FILL);
   actual.drawOval(new RectF(126,128,223,235),p);
   path(actual,5f,chalk,129,174,121,185,126,208,140,223,166,236,199,233,220,217,227,189,217,169);
   // Spiked hair and a cap at a cocky angle.
   path(actual,5f,chalk,126,170,125,154,142,158,144,138,157,148,170,127,180,146,198,132,200,151,216,152,219,173);
   path(actual,4f,chalk,128,157,157,126,202,127,224,151);
   path(actual,3f,chalk,140,139,197,142);
   path(actual,4f,chalk,128,157,112,183,152,184);
   path(actual,3.7f,chalk,153,186,165,183);
   path(actual,3.7f,chalk,192,185,203,184);
   path(actual,3.3f,chalk,165,207,177,213,188,209,198,200);
   circle(actual,143,207,2.5f,teal,2f);
   path(actual,3.1f,dim,215,212,222,213,219,222);
   path(actual,3.7f,chalk,164,233,161,249,179,255,200,249,197,233);
   actual.restore();
   if(isShown()&&!reduceMotion)postInvalidateDelayed(80);
   actual.restore();
  }
 }
}
