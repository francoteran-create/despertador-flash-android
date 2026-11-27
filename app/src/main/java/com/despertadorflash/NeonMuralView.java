package com.despertadorflash;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;

/** Resolution-independent graffiti. The paths are recorded only when the view changes size. */
final class NeonMuralView extends View {
 private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Picture wall=new Picture();
 private final ValueAnimator light=ValueAnimator.ofFloat(0f,1f);
 private float phase;
 private int frame=-1;
 NeonMuralView(Context context){
  super(context);
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  light.setDuration(6400L);
  light.setRepeatCount(ValueAnimator.INFINITE);
  light.setInterpolator(new LinearInterpolator());
  light.addUpdateListener(a->{
   phase=(Float)a.getAnimatedValue();
   int next=(int)(phase*151);
   if(next!=frame){frame=next;invalidate();}
  });
 }
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
  if(w==0||h==0)return;
  Canvas c=wall.beginRecording(w,h);
  c.scale(w/480f,h/900f);
  c.drawColor(Color.BLACK);
  ink.setStrokeWidth(1f);
  ink.setColor(0xff10131a);
  for(int row=0;row<19;row++){
   float y=row*52f;
   c.drawLine(0,y,480,y,ink);
   for(int col=0;col<6;col++){
    float x=col*101f+(row%2)*48f;
    c.drawLine(x,y,x,y+52,ink);
   }
  }
  Random random=new Random(4215);
  for(int j=0;j<290;j++){
   float x=random.nextFloat()*480,y=random.nextFloat()*900;
   float side=Math.min(x,480-x);
   ink.setColor(new int[]{0x6632ecff,0x66ed51ce,0x80fff075}[j%3]);
   ink.setAlpha(side<100?30+random.nextInt(44):12+random.nextInt(21));
   c.drawCircle(x,y,.45f+random.nextFloat()*1.45f,ink);
  }
  paintTag(c,38,128,-13,"AURORA",87,0xff37ebfa,0xffef51d7);
  paintTag(c,9,774,-17,"AURORA",101,0xffed56db,0xff5a65d9);
  ribbon(c,0xff35eaff,0,-20,190,69,285,-76,470,37);
  ribbon(c,0xffffde46,470,995,327,770,512,579,404,411);
  ribbon(c,0xfff952cc,435,-34,398,79,564,169,422,296);
  wall.endRecording();
 }
 private void paintTag(Canvas c,float x,float y,float angle,String text,float size,int edge,int shadow){
  c.save();c.rotate(angle,x,y);
  ink.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(getContext(),R.font.permanent_marker));
  ink.setTextSize(size);
  ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(8);
  ink.setColor(shadow);ink.setAlpha(57);c.drawText(text,x+4,y+4,ink);
  ink.setStrokeWidth(2);ink.setColor(edge);ink.setAlpha(108);c.drawText(text,x,y,ink);
  ink.setStyle(Paint.Style.FILL);ink.setAlpha(33);c.drawText(text,x,y,ink);
  c.restore();
 }
 private void ribbon(Canvas c,int color,float x,float y,float a,float b,float d,float e,float endX,float endY){
  Path stroke=new Path();stroke.moveTo(x,y);stroke.cubicTo(a,b,d,e,endX,endY);
  ink.setStyle(Paint.Style.STROKE);ink.setStrokeCap(Paint.Cap.ROUND);
  for(int pass=2;pass>=0;pass--){
   ink.setColor(color);ink.setAlpha(new int[]{125,45,18}[pass]);ink.setStrokeWidth(new int[]{2,7,20}[pass]);
   c.drawPath(stroke,ink);
  }
  ink.setStyle(Paint.Style.FILL);
 }
 @Override protected void onDraw(Canvas c){
  c.drawPicture(wall);
  int w=getWidth(),h=getHeight();
  if(w==0||h==0)return;
  float wave=(float)(.5+.5*Math.sin(phase*2*Math.PI));
  glow(c,w*.25f,h*(.28f+.16f*wave),w*.68f,0xff00dcf5,(int)(27+34*wave));
  glow(c,w*.82f,h*(.7f-.12f*wave),w*.62f,0xfff900bf,(int)(23+32*(1-wave)));
  glow(c,w*.48f,h*.94f,w*.64f,0xffffc72c,24);
 }
 private void glow(Canvas c,float x,float y,float radius,int color,int alpha){
  ink.setShader(new RadialGradient(x,y,radius,new int[]{(color&0xffffff)|(alpha<<24),Color.TRANSPARENT},
   null,Shader.TileMode.CLAMP));
  ink.setAlpha(255);c.drawCircle(x,y,radius,ink);ink.setShader(null);
 }
 @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);syncLight();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);syncLight();}
 @Override protected void onDetachedFromWindow(){light.cancel();super.onDetachedFromWindow();}
 private void syncLight(){
  PowerManager power=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
  boolean allowed=Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0
   &&(power==null||!power.isPowerSaveMode());
  if(isAttachedToWindow()&&isShown()&&hasWindowFocus()&&allowed){if(!light.isStarted())light.start();}
  else light.cancel();
 }
}
