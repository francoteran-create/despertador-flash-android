package com.despertadorflash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Graffiti-style cloud menu: four large, accessible white-outline buttons. */
public final class AuroraHeroCloudButton extends View {
 private static final int VIOLET=0xffc719f5;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path shape=new Path();
 private final String title;
 public AuroraHeroCloudButton(Context context,String title){
  super(context);this.title=title;
  setContentDescription("Abrir "+title);setClickable(true);setFocusable(true);
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 @Override protected void onMeasure(int w,int h){
  int width=View.MeasureSpec.getSize(w),height=View.MeasureSpec.getSize(h);
  if(View.MeasureSpec.getMode(h)==View.MeasureSpec.UNSPECIFIED)height=(int)(92*getResources().getDisplayMetrics().density);
  setMeasuredDimension(width,Math.max(1,height));
 }
 private void stroke(int color,float width){
  p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.STROKE);
  p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(width);
 }
 private void fill(int color){p.reset();p.setAntiAlias(true);p.setColor(color);p.setStyle(Paint.Style.FILL);}
 private void drawSymbol(Canvas c){
  stroke(VIOLET,.06f);
  if(title.equals("SONIDOS")){
   for(int i=-3;i<=3;i++){
    float height=(i==0?.44f:i%2==0?.28f:.19f);
    c.drawLine(i*.13f,-.13f-height*.5f,i*.13f,-.13f+height*.5f,p);
   }
  }else if(title.equals("CONVERSACIÓN")){
   c.drawRoundRect(-.55f,-.39f,.27f,.08f,.16f,.16f,p);
   c.drawLine(-.27f,.08f,-.47f,.23f,p);
   c.drawRoundRect(-.03f,-.14f,.56f,.30f,.13f,.13f,p);
  }else if(title.equals("ALARMA")){
   c.drawArc(-.36f,-.45f,.36f,.19f,195f,150f,false,p);
   c.drawLine(-.43f,.20f,.43f,.20f,p);c.drawCircle(0f,.31f,.055f,p);
  }else{
   Path q=new Path();q.moveTo(-.53f,-.18f);q.lineTo(-.12f,-.18f);
   q.lineTo(.34f,-.40f);q.lineTo(.34f,.30f);
   q.lineTo(-.12f,.08f);q.lineTo(-.53f,.08f);q.close();c.drawPath(q,p);
   c.drawArc(.14f,-.33f,.72f,.32f,-56f,112f,false,p);
  }
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
  float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  c.save();c.translate(w*.5f,h*.52f);
  c.scale(Math.min(w/2.9f,h/2.08f),Math.min(w/2.9f,h/2.08f));
  shape.reset();shape.moveTo(-1.08f,.35f);
  shape.cubicTo(-1.45f,.10f,-1.34f,-.38f,-.93f,-.45f);
  shape.cubicTo(-1.03f,-.87f,-.60f,-1.09f,-.19f,-.85f);
  shape.cubicTo(.16f,-1.23f,.73f,-.97f,.80f,-.61f);
  shape.cubicTo(1.29f,-.66f,1.44f,-.12f,1.09f,.15f);
  shape.cubicTo(1.31f,.55f,.65f,.78f,.22f,.65f);
  shape.cubicTo(-.30f,.74f,-.90f,.65f,-1.08f,.35f);shape.close();
  fill(isPressed()?0xff26202c:0xff070609);c.drawPath(shape,p);
  stroke(0x884a1959,.16f);c.drawPath(shape,p);
  stroke(Color.WHITE,.065f);c.drawPath(shape,p);
  stroke(VIOLET,.032f);c.drawArc(-1.1f,-.40f,1.10f,.70f,10f,155f,false,p);
  drawSymbol(c);
  fill(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
  p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD));
  p.setTextSize(title.equals("CONVERSACIÓN")?.245f:.34f);
  c.drawText(title,0f,.48f,p);
  c.restore();
 }
 @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
}