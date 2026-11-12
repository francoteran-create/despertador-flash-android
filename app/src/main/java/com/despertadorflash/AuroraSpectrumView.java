package com.despertadorflash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Large landscape LED frequency display. Driven ONLY by actual microphone PCM
 * captured for the existing classifier; no decorative random/fake audio levels.
 * The band heights and overall brightness increase with real sound energy.
 */
public final class AuroraSpectrumView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float[] target=new float[LiveAudioSpectrum.BANDS];
 private final float[] showing=new float[LiveAudioSpectrum.BANDS];
 private float targetLevel=0f,showingLevel=0f;
 private long lastSample=0L;
 private boolean listening=false;
 private boolean drawing=false;
 private final Runnable repaint=new Runnable(){
  @Override public void run(){
   if(!drawing)return;
   invalidate();
   postDelayed(this,33L);
  }
 };
 public AuroraSpectrumView(Context context){super(context);setBackgroundColor(Color.BLACK);}
 public void setListening(boolean live){
  listening=live;
  if(!live)clear();
 }
 public void setSpectrum(LiveAudioSpectrum.Reading reading){
  if(reading==null)return;
  System.arraycopy(reading.bands,0,target,0,
   Math.min(target.length,reading.bands.length));
  targetLevel=Math.max(0f,Math.min(1f,reading.level));
  lastSample=SystemClock.uptimeMillis();
  invalidate();
 }
 private void clear(){
  for(int i=0;i<target.length;i++)target[i]=0f;
  targetLevel=0f;lastSample=0L;invalidate();
 }
 @Override protected void onAttachedToWindow(){
  super.onAttachedToWindow();
  drawing=true;
  post(repaint);
 }
 @Override protected void onDetachedFromWindow(){
  drawing=false;removeCallbacks(repaint);super.onDetachedFromWindow();
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight();
  if(w<=0||h<=0)return;
  canvas.drawColor(Color.BLACK);
  paint.setShader(new LinearGradient(0,0,w,h,
   new int[]{0xff000000,0xff070f22,0xff03000b},null,Shader.TileMode.CLAMP));
  canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
  long now=SystemClock.uptimeMillis();
  boolean recent=listening&&lastSample>0&&now-lastSample<1250L;
  float wanted=recent?targetLevel:0f;
  showingLevel+=(wanted-showingLevel)*.24f;
  float midY=h*.69f;
  float step=w/(LiveAudioSpectrum.BANDS+2f);
  float maxH=h*.52f;
  float xStart=(w-step*(LiveAudioSpectrum.BANDS-1))*.5f;
  for(int i=0;i<showing.length;i++){
   float bandGoal=recent?target[i]:0f;
   showing[i]+=(bandGoal-showing[i])*.28f;
   // Very low real energy gives tiny dim tick marks; no fake movement.
   float strength=showing[i]*(.42f+.58f*showingLevel);
   float height=h*.008f+maxH*strength;
   float x=xStart+i*step;
   float y=midY-height;
   float hue=(185f+i*8.6f)%360f;
   int solid=Color.HSVToColor(new float[]{hue,.74f,Math.min(1f,.45f+strength*.72f)});
   int glow=Color.argb((int)(35+130*strength),Color.red(solid),Color.green(solid),Color.blue(solid));
   paint.setShader(null);paint.setStyle(Paint.Style.FILL);
   paint.setColor(glow);
   canvas.drawRoundRect(x-step*.45f,y-h*.018f,x+step*.45f,midY+h*.02f,step*.27f,step*.27f,paint);
   paint.setColor(solid);
   canvas.drawRoundRect(x-step*.24f,y,x+step*.24f,midY,step*.2f,step*.2f,paint);
   if(strength>.06f){
    paint.setColor(Color.argb((int)(100*strength),255,255,255));
    canvas.drawRoundRect(x-step*.085f,y,x+step*.085f,midY,step*.07f,step*.07f,paint);
   }
   // Subtle colored reflection below the baseline.
   paint.setColor(Color.argb((int)(40*strength),Color.red(solid),Color.green(solid),Color.blue(solid)));
   canvas.drawRoundRect(x-step*.18f,midY+h*.016f,x+step*.18f,
    midY+h*.016f+height*.18f,step*.16f,step*.16f,paint);
  }
  paint.setShader(null);
 }
}
