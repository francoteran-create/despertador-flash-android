package com.despertadorflash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

/**
 * A lightweight native Canvas mascot: black cloud, crisp white strokes and
 * grey graffiti-style shadows. No GIF decoding, file downloads, camera,
 * microphone or background scheduler. Events are only passed by callers
 * after an actual confirmed observation.
 */
public final class AuroraCloudMascotView extends View {
 private static final long EVENT_MS=3400L;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 private final PowerManager power;
 private boolean listening=false,speaking=false;
 private AuroraEventVisualPolicy.Event event=AuroraEventVisualPolicy.Event.NONE;
 private long eventAt=-1L;
 private boolean attached=false;
 private boolean reducedMotion=false;
 private final Runnable tick=new Runnable(){public void run(){invalidate();}};
 public AuroraCloudMascotView(Context context){
  super(context);
  power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
  setContentDescription("Nube AURORA en espera");
  refreshMotion();
 }
 public void refreshMotion(){
  float scale=1f;
  try{scale=Settings.Global.getFloat(getContext().getContentResolver(),
   Settings.Global.ANIMATOR_DURATION_SCALE,1f);}catch(Exception ignored){}
  reducedMotion=scale==0f||(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled());
  invalidate();
 }
 public void setListening(boolean value){
  listening=value;
  if(!value){speaking=false;event=AuroraEventVisualPolicy.Event.NONE;eventAt=-1L;}
  updateDescription();invalidate();
 }
 public void setSpeaking(boolean value){
  speaking=listening&&value;
  updateDescription();invalidate();
 }
 public void showConfirmedEvent(String label){
  if(!listening)return;
  event=AuroraEventVisualPolicy.fromConfirmedLabel(label);
  if(event==AuroraEventVisualPolicy.Event.NONE)return;
  eventAt=SystemClock.elapsedRealtime();
  updateDescription();invalidate();
 }
 private void updateDescription(){
  setContentDescription(event!=AuroraEventVisualPolicy.Event.NONE?
   AuroraEventVisualPolicy.accessibleName(event):
   speaking?"Nube AURORA detectando voz":listening?"Nube AURORA escuchando":"Nube AURORA en espera");
 }
 @Override protected void onAttachedToWindow(){
  super.onAttachedToWindow();attached=true;refreshMotion();
 }
 @Override protected void onDetachedFromWindow(){
  attached=false;removeCallbacks(tick);super.onDetachedFromWindow();
 }
 @Override protected void onMeasure(int w,int h){
  int width=MeasureSpec.getSize(w);
  setMeasuredDimension(width,Math.max(1,(int)(width*.49f)));
 }
 private void line(Canvas c,float x1,float y1,float x2,float y2){
  c.drawLine(x1,y1,x2,y2,p);
 }
 private void oval(Canvas c,float left,float top,float right,float bottom){
  c.drawOval(left,top,right,bottom,p);
 }
 private void rounded(Canvas c,float a,float b,float d,float e,float radius){
  c.drawRoundRect(a,b,d,e,radius,radius,p);
 }
 private void stroke(float width,int color){
  p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.STROKE);
  p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
 }
 private void fill(int color){
  p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.FILL);
 }
 private void cloudShape(Canvas c){
  path.reset();
  path.moveTo(-.84f,.36f);
  path.cubicTo(-1.23f,.23f,-1.12f,-.27f,-.80f,-.35f);
  path.cubicTo(-.87f,-.69f,-.53f,-.89f,-.23f,-.79f);
  path.cubicTo(.02f,-1.13f,.54f,-.94f,.65f,-.62f);
  path.cubicTo(.99f,-.60f,1.08f,-.28f,.94f,-.10f);
  path.cubicTo(1.27f,.16f,1.04f,.52f,.75f,.54f);
  path.cubicTo(.31f,.70f,-.42f,.65f,-.84f,.36f);
  path.close();
  fill(Color.BLACK);c.drawPath(path,p);
  stroke(.058f,Color.WHITE);c.drawPath(path,p);
  stroke(.018f,0x998f98a7);c.drawPath(path,p);
 }
 private void eye(Canvas c,float x,float y,boolean blink){
  if(blink){stroke(.047f,Color.WHITE);line(c,x-.11f,y,x+.11f,y);return;}
  fill(Color.WHITE);oval(c,x-.12f,y-.14f,x+.12f,y+.15f);
  fill(Color.BLACK);oval(c,x-.035f,y-.075f,x+.085f,y+.08f);
  fill(Color.WHITE);c.drawCircle(x+.043f,y-.047f,.027f,p);
 }
 private void drawCloud(Canvas c,float t,boolean animated){
  float bob=animated?(float)Math.sin(t*1.65f)*.027f:0f;
  c.save();c.translate(0,bob);
  boolean blink=animated&&((int)(t*10)%59==0);
  cloudShape(c);
  stroke(.042f,Color.WHITE);
  line(c,-.47f,-.26f,-.29f,-.30f);
  line(c,.20f,-.30f,.39f,-.25f);
  eye(c,-.35f,-.08f,blink);eye(c,.29f,-.08f,blink);
  stroke(.037f,Color.WHITE);
  if(speaking&&animated){
   oval(c,-.13f,.19f,.13f,.40f);
  }else{
   path.reset();path.moveTo(-.15f,.24f);path.quadTo(0f,.37f,.17f,.24f);
   c.drawPath(path,p);
  }
  // Arms remain close to the silhouette except when an event points outward.
  stroke(.063f,Color.WHITE);
  path.reset();path.moveTo(-.76f,.23f);path.quadTo(-.63f,.49f,-.48f,.45f);c.drawPath(path,p);
  c.drawCircle(-.48f,.46f,.08f,p);
  if(event!=AuroraEventVisualPolicy.Event.NONE){
   path.reset();path.moveTo(.72f,.25f);path.quadTo(.92f,.37f,1.13f,.21f);
   c.drawPath(path,p);line(c,1.12f,.21f,1.27f,.17f);
  }else{
   path.reset();path.moveTo(.73f,.24f);path.quadTo(.65f,.45f,.50f,.45f);
   c.drawPath(path,p);c.drawCircle(.50f,.46f,.08f,p);
  }
  c.restore();
 }
 private void eventDrawing(Canvas c,AuroraEventVisualPolicy.Event e,float t){
  // Compact monochrome pictograms beside the mascot; no colored flashing.
  c.save();c.translate(.98f,-.02f);c.scale(.43f,.43f);
  float shift=reducedMotion?0f:(float)Math.sin(t*6f)*.055f;
  stroke(.075f,Color.WHITE);
  switch(e){
   case DOOR:
    c.save();c.rotate(shift*20,0,.65f);
    rounded(c,-.48f,-.82f,.45f,.86f,.08f);
    oval(c,.21f,-.02f,.31f,.10f);c.restore();
    line(c,.57f,-.43f,.72f,-.43f);line(c,.59f,-.02f,.81f,-.02f);
    break;
   case DOG:
    oval(c,-.53f,-.45f,.53f,.55f);
    path.reset();path.moveTo(-.44f,-.38f);path.lineTo(-.72f,-.80f);path.lineTo(-.72f,.05f);c.drawPath(path,p);
    path.reset();path.moveTo(.44f,-.38f);path.lineTo(.70f,-.80f);path.lineTo(.70f,.05f);c.drawPath(path,p);
    c.drawCircle(-.22f,-.13f,.065f,p);c.drawCircle(.22f,-.13f,.065f,p);
    oval(c,-.08f,.13f,.08f,.27f);
    line(c,.74f,-.34f,.96f,-.47f);line(c,.77f,0f,1.04f,0f);
    break;
   case HORN:
    rounded(c,-.82f,-.20f,-.40f,.20f,.04f);
    path.reset();path.moveTo(-.40f,-.20f);path.lineTo(.48f,-.70f);path.lineTo(.48f,.70f);
    path.lineTo(-.40f,.20f);path.close();c.drawPath(path,p);
    line(c,.66f,-.40f,.92f,-.56f);line(c,.69f,0f,1.03f,0f);
    line(c,.66f,.40f,.92f,.56f);break;
   case MUSIC:
    line(c,-.33f,-.55f,-.33f,.48f);line(c,-.33f,-.55f,.48f,-.75f);
    line(c,.48f,-.75f,.48f,.27f);
    oval(c,-.66f,.36f,-.27f,.61f);oval(c,.15f,.14f,.54f,.40f);break;
   case VOICES:
    oval(c,-.85f,-.61f,.40f,.26f);oval(c,-.10f,-.15f,.85f,.71f);
    line(c,-.37f,.20f,-.53f,.54f);line(c,.47f,.61f,.71f,.83f);break;
   case SIREN:
    rounded(c,-.74f,.30f,.72f,.62f,.08f);
    path.reset();path.moveTo(-.52f,.30f);path.quadTo(-.48f,-.64f,0f,-.72f);
    path.quadTo(.50f,-.63f,.53f,.30f);c.drawPath(path,p);
    line(c,0f,-.80f,0f,-1.04f);line(c,-.73f,-.60f,-.95f,-.78f);
    line(c,.73f,-.60f,.95f,-.78f);break;
   case BABY:
    c.drawCircle(0f,-.11f,.68f,p);c.drawCircle(-.22f,-.20f,.045f,p);
    c.drawCircle(.22f,-.20f,.045f,p);oval(c,-.16f,.10f,.16f,.48f);break;
   case PHONE:
    rounded(c,-.55f,-.89f,.55f,.89f,.15f);
    line(c,-.27f,-.64f,.29f,-.64f);c.drawCircle(0f,.68f,.055f,p);break;
   case OTHER:
    c.drawCircle(0f,0f,.57f,p);line(c,0f,-.37f,0f,.08f);
    c.drawCircle(0f,.35f,.035f,p);break;
   default:break;
  }
  c.restore();
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
  float w=getWidth(),h=getHeight();
  if(w<=0||h<=0)return;
  long now=SystemClock.elapsedRealtime();
  if(eventAt>=0&&now-eventAt>=EVENT_MS){
   event=AuroraEventVisualPolicy.Event.NONE;eventAt=-1L;updateDescription();
  }
  boolean powerSave=power!=null&&power.isPowerSaveMode();
  boolean animated=attached&&isShown()&&!reducedMotion&&!powerSave;
  float t=animated?now/1000f:0f;
  fill(0xff08090b);c.drawRoundRect(0,0,w,h,w*.06f,w*.06f,p);
  c.save();float scale=Math.min(w/4f,h/2.55f);
  c.translate(w*.39f,h*.52f);c.scale(scale,scale);
  // Soft monochrome graffiti halo and grounding shadow, no bitmap textures.
  for(int i=6;i>=1;i--){
   fill(Color.argb(7,205,211,219));
   oval(c,-1.31f-i*.03f,-1.16f-i*.02f,1.27f+i*.04f,1.00f+i*.025f);
  }
  fill(0x4457616a);oval(c,-.95f,.88f,.97f,1.04f);
  drawCloud(c,t,animated);
  if(event!=AuroraEventVisualPolicy.Event.NONE)eventDrawing(c,event,t);
  c.restore();
  if(attached&&isShown()){
   removeCallbacks(tick);
   if(animated&&(speaking||event!=AuroraEventVisualPolicy.Event.NONE||listening))
    postDelayed(tick,120L);
   else if(event!=AuroraEventVisualPolicy.Event.NONE)postDelayed(tick,EVENT_MS);
  }
 }
}
