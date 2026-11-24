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

/** Single white cloud on a truly black hero. Never suggests that the mic is on. */
public final class AuroraHeroCloudView extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path outline=new Path();
 private final PowerManager power;
 private boolean active=false,reducedMotion=false;
 private final Runnable frame=()->invalidate();
 public AuroraHeroCloudView(Context c){
  super(c);
  power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
  setContentDescription("AURORA, nube blanca en espera");
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  float scale=1f;
  try{scale=Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f);}
  catch(Exception ignored){}
  reducedMotion=scale==0f||(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled());
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();active=true;invalidate();}
 @Override protected void onDetachedFromWindow(){active=false;removeCallbacks(frame);super.onDetachedFromWindow();}
 @Override protected void onMeasure(int w,int h){
  int width=MeasureSpec.getSize(w);
  setMeasuredDimension(width,Math.max(1,(int)(width*.97f)));
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  int w=getWidth(),h=getHeight();
  if(w==0||h==0)return;
  boolean animate=active&&isShown()&&!reducedMotion&&!(power!=null&&power.isPowerSaveMode());
  float t=animate?SystemClock.uptimeMillis()/1000f:0f;
  float bob=animate?(float)Math.sin(t*1.45f)*.015f:0f;
  canvas.save();canvas.translate(w*.5f,h*.52f+bob*w);
  float r=Math.min(w/2.65f,h/2.56f);canvas.scale(r,r);
  p.setStyle(Paint.Style.FILL);p.setColor(0x55373737);
  canvas.drawOval(-1.09f,.93f,1.10f,1.05f,p);
  outline.reset();
  outline.moveTo(-.88f,.47f);
  outline.cubicTo(-1.35f,.29f,-1.32f,-.29f,-.90f,-.46f);
  outline.cubicTo(-.96f,-.92f,-.52f,-1.14f,-.17f,-.93f);
  outline.cubicTo(.13f,-1.30f,.78f,-1.00f,.85f,-.57f);
  outline.cubicTo(1.34f,-.53f,1.45f,.21f,1.06f,.50f);
  outline.cubicTo(.68f,.86f,-.44f,.84f,-.88f,.47f);
  outline.close();
  p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);canvas.drawPath(outline,p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.035f);p.setColor(0xff777777);canvas.drawPath(outline,p);
  p.setStyle(Paint.Style.FILL);p.setColor(0xff171717);
  boolean blink=animate&&((int)(t*5)%31==0);
  if(blink){
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.049f);
   canvas.drawLine(-.52f,-.12f,-.23f,-.12f,p);canvas.drawLine(.17f,-.12f,.46f,-.12f,p);
  }else{
   canvas.drawOval(-.54f,-.29f,-.24f,.06f,p);
   canvas.drawOval(.17f,-.29f,.47f,.06f,p);
  }
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.052f);p.setStrokeCap(Paint.Cap.ROUND);
  Path smile=new Path();smile.moveTo(-.18f,.23f);smile.quadTo(.04f,.41f,.25f,.23f);
  canvas.drawPath(smile,p);
  p.setStrokeWidth(.075f);
  canvas.drawLine(-.90f,.35f,-.72f,.64f,p);canvas.drawLine(-.72f,.64f,-.51f,.63f,p);
  canvas.drawLine(.94f,.35f,1.08f,.54f,p);canvas.drawLine(1.08f,.54f,1.17f,.45f,p);
  canvas.restore();
  removeCallbacks(frame);
  if(animate)postDelayed(frame,220L);
 }
}
