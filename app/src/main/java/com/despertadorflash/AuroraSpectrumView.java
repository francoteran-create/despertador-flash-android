package com.despertadorflash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Landscape neon ZIGZAG waveform. Uses the SAME physical mic PCM used by
 * YAMNet, without another microphone, recording, or invented oscillation.
 * This is an illustrative real-time waveform, NOT a calibrated sound meter.
 */
public final class AuroraSpectrumView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path trace=new Path();
 private final float[] target=new float[LiveAudioSpectrum.WAVEFORM_POINTS];
 private final float[] showing=new float[LiveAudioSpectrum.WAVEFORM_POINTS];
 private float targetLevel=0f,showingLevel=0f;
 private long lastSample=0L;
 private boolean listening=false,drawing=false;
 private final Runnable repaint=new Runnable(){
  @Override public void run(){
   if(!drawing||!listening||!isShown())return;
   invalidate();
   postDelayed(this,33L);
  }
 };
 private void scheduleFrames(){
  removeCallbacks(repaint);
  if(drawing&&listening&&isShown())post(repaint);
 }
 public AuroraSpectrumView(Context context){
  super(context);
  setBackgroundColor(Color.BLACK);
  setContentDescription("Onda multicolor que cambia según las vibraciones captadas por el micrófono");
 }
 public void setListening(boolean enabled){
  listening=enabled;
  if(!enabled)clear();
  scheduleFrames();
 }
 public void setSpectrum(LiveAudioSpectrum.Reading reading){
  if(reading==null||reading.waveform==null)return;
  System.arraycopy(reading.waveform,0,target,0,
   Math.min(target.length,reading.waveform.length));
  targetLevel=Math.max(0f,Math.min(1f,reading.level));
  lastSample=SystemClock.uptimeMillis();
  invalidate();
 }
 private void clear(){
  for(int i=0;i<target.length;i++)target[i]=0f;
  targetLevel=0f;
  lastSample=0L;
  invalidate();
 }
 @Override protected void onAttachedToWindow(){
  super.onAttachedToWindow();
  drawing=true;
  scheduleFrames();
 }
 @Override protected void onVisibilityChanged(View changedView,int visibility){
  super.onVisibilityChanged(changedView,visibility);
  scheduleFrames();
 }
 @Override protected void onDetachedFromWindow(){
  drawing=false;
  removeCallbacks(repaint);
  super.onDetachedFromWindow();
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  final float w=getWidth(),h=getHeight();
  if(w<=0f||h<=0f)return;
  canvas.drawColor(Color.BLACK);
  long now=SystemClock.uptimeMillis();
  boolean recent=listening&&lastSample>0&&now-lastSample<1250L;
  float desired=recent?targetLevel:0f;
  showingLevel+=(desired-showingLevel)*.24f;
  float center=h*.52f;
  float amplitude=h*.42f;
  float margin=w*.02f;
  trace.reset();
  for(int i=0;i<showing.length;i++){
   float wave=recent?target[i]:0f;
   showing[i]+=(wave-showing[i])*.33f;
   float x=margin+(w-2f*margin)*i/(showing.length-1);
   // Direct signed PCM, amplified enough to see quiet speech.
   // Actual strong PCM creates much higher peaks than quiet samples.
   float y=center-amplitude*Math.max(-1f,Math.min(1f,showing[i]*1.75f));
   if(i==0)trace.moveTo(x,y);else trace.lineTo(x,y);
  }
  int[] neon={0xff00dfff,0xff7c6aff,0xffff3fca,0xffffd04b,0xff4dffdd};
  Shader gradient=new LinearGradient(margin,0,w-margin,0,neon,null,Shader.TileMode.CLAMP);
  paint.setShader(null);
  paint.setStyle(Paint.Style.STROKE);
  paint.setStrokeCap(Paint.Cap.ROUND);
  paint.setStrokeJoin(Paint.Join.ROUND);
  paint.setColor(Color.argb(30,255,255,255));
  paint.setStrokeWidth(Math.max(1f,h*.002f));
  canvas.drawLine(margin,center,w-margin,center,paint);
  float energy=.22f+.78f*showingLevel;
  paint.setShader(gradient);
  paint.setStrokeWidth(h*.050f*energy+3f);
  paint.setAlpha(Math.min(125,(int)(35f+90f*showingLevel)));
  canvas.drawPath(trace,paint);
  paint.setStrokeWidth(h*.024f*energy+2f);
  paint.setAlpha(Math.min(215,(int)(95f+120f*showingLevel)));
  canvas.drawPath(trace,paint);
  paint.setStrokeWidth(Math.max(2f,h*.007f+2f*showingLevel));
  paint.setAlpha(255);
  canvas.drawPath(trace,paint);
  paint.setShader(null);
 }
}
