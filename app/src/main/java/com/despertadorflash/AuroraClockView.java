package com.despertadorflash;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;
public class AuroraClockView extends View {
 private final Paint p=new Paint(3);
 private final int yellow=Color.rgb(255,214,51);
 private final int cyan=Color.rgb(87,228,232);
 private final int white=Color.rgb(242,247,255);
 private final Calendar cal=Calendar.getInstance();
 public AuroraClockView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
 protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,(int)(width*0.98f));}
 private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.clearShadowLayer();c.drawLine(x1,y1,x2,y2,p);}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*.45f,r=Math.min(w*.40f,h*.40f);
  cal.setTimeInMillis(System.currentTimeMillis());
  p.setStyle(Paint.Style.FILL);p.clearShadowLayer();p.setColor(0xff101824);canvas.drawRoundRect(w*.025f,h*.02f,w*.975f,h*.97f,36,36,p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xff27384a);canvas.drawCircle(cx,cy,r,p);
  p.setColor(yellow);p.setStrokeWidth(5);p.setShadowLayer(13,0,0,0x99ffd633);canvas.drawArc(cx-r,cy-r,cx+r,cy+r,-90,(cal.get(Calendar.SECOND)/60f)*360,false,p);p.clearShadowLayer();
  for(int i=0;i<60;i++){double a=Math.toRadians(i*6-90);float outer=r*.92f,inner=i%5==0?r*.79f:r*.86f;line(canvas,cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,i%5==0?white:0xff526174,i%5==0?4:2);}
  float sec=cal.get(Calendar.SECOND)+cal.get(Calendar.MILLISECOND)/1000f;
  float min=cal.get(Calendar.MINUTE)+sec/60f;
  float hour=cal.get(Calendar.HOUR)+min/60f;
  hand(canvas,cx,cy,hour*30-90,r*.46f,8,white);
  hand(canvas,cx,cy,min*6-90,r*.68f,6,white);
  hand(canvas,cx,cy,sec*6-90,r*.76f,3,cyan);
  p.setStyle(Paint.Style.FILL);p.setColor(yellow);p.setShadowLayer(9,0,0,yellow);canvas.drawCircle(cx,cy,7,p);p.clearShadowLayer();
  String time=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(cal.getTime());
  p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(r*.24f);p.setColor(yellow);p.setShadowLayer(10,0,0,0x77ffd633);canvas.drawText(time,cx,h*.90f,p);p.clearShadowLayer();
  if(isAttachedToWindow())postInvalidateDelayed(1000);
 }
 private void hand(Canvas c,float x,float y,float angle,float length,float stroke,int color){double a=Math.toRadians(angle);line(c,x,y,x+(float)Math.cos(a)*length,y+(float)Math.sin(a)*length,color,stroke);}
}
