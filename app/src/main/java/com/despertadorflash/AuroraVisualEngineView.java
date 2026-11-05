package com.despertadorflash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AURORA VISUAL ENGINE — native Android GPU/Canvas visual renderer.
 * No external assets, server calls, microphone capture or mandatory 3D SDK.
 * Perspective orbit, layered translucent aurora, animated particles and state-driven pulse.
 */
public final class AuroraVisualEngineView extends View {
 public static final int CLOCK=0, SOUND=1, SPEECH=2;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Path ribbon=new Path();
 private final float[] starsX=new float[72],starsY=new float[72],starsR=new float[72];
 private final Random random=new Random(1729);
 private final SharedPreferences prefs;
 private int mode;
 private boolean listening=false, hasEvent=false, running=false;
 private long eventPulseAt=0L;
 private float energy=0.1f;
 private long epoch=System.currentTimeMillis();
 private int widthPx=0, heightPx=0;
 private boolean reducedMotion;
 private final PowerManager power;
 private final Runnable nextFrame=new Runnable(){public void run(){invalidate();}};
 public AuroraVisualEngineView(Context context,int mode){
  super(context);
  this.mode=mode;this.prefs=context.getSharedPreferences("aurora",0);
  power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
  setLayerType(View.LAYER_TYPE_HARDWARE,null);
  for(int i=0;i<starsX.length;i++){starsX[i]=random.nextFloat();starsY[i]=random.nextFloat();starsR[i]=.5f+random.nextFloat()*1.3f;}
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  refreshMotion();
 }
 public void refreshMotion(){
  float scale=1;
  try{scale=Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f);}catch(Exception ignored){}
  reducedMotion=!prefs.getBoolean("aurora_motion",true)||scale==0f||(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled());
  invalidate();
 }
 public void setListening(boolean active){listening=active;invalidate();}
 public void setDetected(boolean detected){hasEvent=detected;eventPulseAt=detected?System.currentTimeMillis():0L;invalidate();}
 public void setEnergy(float level){
  float bounded=Math.max(0f,Math.min(1f,level));
  energy=energy*.76f+bounded*.24f;
  invalidate();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;epoch=System.currentTimeMillis();refreshMotion();}
 @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(nextFrame);super.onDetachedFromWindow();}
 @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);if(visibility!=VISIBLE)removeCallbacks(nextFrame);}
 @Override protected void onMeasure(int w,int h){
  int width=MeasureSpec.getSize(w);
  float ratio=mode==CLOCK?.80f:.40f;
  setMeasuredDimension(width,Math.max(1,(int)(width*ratio)));
 }
 private void fill(Canvas canvas,float x,float y,float radius,int inner,int outer){
  paint.setStyle(Paint.Style.FILL);
  paint.setShader(new RadialGradient(x,y,Math.max(1,radius),new int[]{inner,outer},new float[]{0f,1f},Shader.TileMode.CLAMP));
  canvas.drawCircle(x,y,radius,paint);paint.setShader(null);
 }
 private void stroke(Canvas canvas,int color,float thickness){
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(thickness);paint.setColor(color);
 }
 private void drawRibbon(Canvas canvas,float w,float h,float phase,int index,float time){
  float center=w*(index==0?.13f:index==1?.68f:.90f);
  float band=w*(index==0?.26f:index==1?.34f:.22f);
  ribbon.reset();int steps=34;
  for(int i=0;i<=steps;i++){
   float y=h*i/steps;
   float shift=(float)Math.sin((i*.29f)+phase+time*.65f)*w*.12f;
   float twist=(float)Math.sin((i*.18f)+phase*.6f-time*.4f)*w*.06f;
   if(i==0)ribbon.moveTo(center+shift+twist-band*.5f,y);
   else ribbon.lineTo(center+shift+twist-band*.5f,y);
  }
  for(int i=steps;i>=0;i--){
   float y=h*i/steps;
   float shift=(float)Math.sin((i*.29f)+phase+time*.65f)*w*.12f;
   float twist=(float)Math.sin((i*.18f)+phase*.6f-time*.4f)*w*.06f;
   ribbon.lineTo(center+shift+twist+band*.5f,y);
  }
  ribbon.close();paint.setStyle(Paint.Style.FILL);
  int c=index==0?0x4a45fdd6:index==1?0x4a5a82ff:0x404cddbd;
  paint.setShader(new LinearGradient(center-band*.6f,0,center+band*.6f,h,
   new int[]{0x00000000,c,0x00677bff},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
  canvas.drawPath(ribbon,paint);paint.setShader(null);
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  boolean animating=!reducedMotion&&isShown()&&running&&(power==null||!power.isPowerSaveMode());
  float t=animating?(System.currentTimeMillis()-epoch)/1000f:0f;
  paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff080e25,0xff0b1d38,0xff100d2c},null,Shader.TileMode.CLAMP));
  c.drawRoundRect(0,0,w,h,w*.045f,w*.045f,paint);paint.setShader(null);
  c.save();Path clip=new Path();clip.addRoundRect(0,0,w,h,w*.045f,w*.045f,Path.Direction.CW);c.clipPath(clip);
  drawRibbon(c,w,h,.4f,0,t);drawRibbon(c,w,h,2.0f,1,t);drawRibbon(c,w,h,3.7f,2,t);
  fill(c,w*.10f,h*.15f,w*.52f,0x20365beb,0x00142465);
  fill(c,w*.93f,h*.8f,w*.43f,0x3048d4ba,0x000d1020);
  for(int i=0;i<starsX.length;i++){
   float shimmer=animating?(float)Math.sin(t*(.3+i%5*.11)+i)*.24f+.76f:1f;
   paint.setColor(Color.argb((int)(shimmer*(i%6==0?140:85)),206,244,255));
   paint.setStyle(Paint.Style.FILL);
   c.drawCircle(starsX[i]*w,starsY[i]*h,starsR[i]*getResources().getDisplayMetrics().density,paint);
  }
  float cx=w*.50f,cy=mode==CLOCK?h*.42f:h*.51f;
  float r=Math.min(w*.315f,mode==CLOCK?h*.38f:h*.45f);
  float pulse=listening?.05f+(float)Math.sin(t*2.1f)*.025f:0f;
  pulse+=energy*.09f;
  float R=r*(1+pulse);
  fill(c,cx,cy,R*1.9f,listening?0x4457c4ff:0x393b98d5,0x00000000);
  fill(c,cx,cy,R*1.22f,0xc611244c,0x0010253c);
  paint.setStyle(Paint.Style.FILL);
  paint.setShader(new RadialGradient(cx-R*.30f,cy-R*.38f,R*1.45f,
    new int[]{0xff65fbe1,0xff2868ac,0xff142448,0xff050f25},
    new float[]{0f,.27f,.65f,1f},Shader.TileMode.CLAMP));
  c.drawCircle(cx,cy,R,paint);paint.setShader(null);
  stroke(c,0x6659f6f0,Math.max(2,w*.005f));c.drawCircle(cx,cy,R*.98f,paint);
  for(int j=0;j<3;j++){
   c.save();float rotation=(j*60f)+(animating?t*(j%2==0?12f:-9f):0);
   c.rotate(rotation,cx,cy);
   stroke(c,j==0?0xb66af9ec:j==1?0x9f9b8bff:0x99d89efc,Math.max(2,w*.006f));
   RectF orbit=new RectF(cx-R*1.27f,cy-R*.30f,cx+R*1.27f,cy+R*.30f);
   c.drawOval(orbit,paint);
   c.restore();
  }
  if(mode==CLOCK){
   String time=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());
   String date=new SimpleDateFormat("EEE d MMM",new Locale("es","AR")).format(new Date());
   paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);
   paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
   paint.setTextSize(w*.19f);paint.setColor(0xfff3faff);paint.setShadowLayer(4,0,0,0x8844eafa);
   c.drawText(time,cx,cy+w*.047f,paint);paint.clearShadowLayer();
   paint.setTextSize(w*.038f);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
   paint.setColor(0xffe7efff);c.drawText(date,cx,cy+w*.129f,paint);
   paint.setTextSize(w*.027f);paint.setColor(0xffa0e5f0);
   c.drawText("AURORA · VISUAL ENGINE",cx,h*.9f,paint);
  }else{
   paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
   paint.setTextSize(w*.057f);paint.setColor(0xffe7faff);
   c.drawText(listening?"ESCUCHANDO":"EN ESPERA",cx,cy+w*.014f,paint);
   paint.setTextSize(w*.029f);paint.setColor(0xff9ddff2);
   c.drawText(mode==SOUND?"ENTORNO":"CONVERSACIÓN",cx,cy+w*.081f,paint);
  }
  if(listening){
   // The bars represent microphone/recognition activity, not a raw waveform recording.
   int bars=17;float spacing=w*.022f,center=(bars-1)*spacing*.5f;
   for(int i=0;i<bars;i++){
    float wave=(float)Math.abs(Math.sin(t*3.4f+i*.71f));
    float amplitude=(.13f+.87f*energy)*(.45f+.55f*wave);
    float barHeight=h*.045f+amplitude*h*.105f;
    float x=cx+i*spacing-center;
    paint.setStyle(Paint.Style.FILL);paint.setColor(i%3==0?0xffb6e9ff:0xff7bf1e9);
    c.drawRoundRect(x-w*.006f,h*.84f-barHeight,x+w*.006f,h*.84f,w*.006f,w*.006f,paint);
   }
  }
  if(hasEvent){
   long age=System.currentTimeMillis()-eventPulseAt;
   if(age<2600L){
    float progress=age/2600f;
    int opacity=(int)(190*(1-progress));
    stroke(c,Color.argb(opacity,253,194,255),Math.max(3,w*.009f)*(1-progress*.5f));
    c.drawCircle(cx,cy,R*(1.30f+progress*.35f),paint);
   }else hasEvent=false;
  }
  c.restore();
  if(running&&isShown()){
   removeCallbacks(nextFrame);
   postDelayed(nextFrame,animating?45L:1000L);
  }
 }
}
