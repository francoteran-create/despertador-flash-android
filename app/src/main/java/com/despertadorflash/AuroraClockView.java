package com.despertadorflash;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

public final class AuroraClockView extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Calendar calendar=Calendar.getInstance();
 private final int cyan=0xff4df4fa, violet=0xffbb69ff, white=0xfff6f8ff;
 public AuroraClockView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription("Reloj AURORA con hora actual");}
 @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,(int)(width*.94f));}
 private void glow(Canvas c,float x,float y,float rad,int color){
  p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x,y,rad,new int[]{color,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,rad,p);p.setShader(null);
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2,cy=h*.49f,r=Math.min(w*.382f,h*.43f);
  calendar.setTimeInMillis(System.currentTimeMillis());
  p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff0c1026,0xff071b30,0xff100d2d},null,Shader.TileMode.CLAMP));
  c.drawRoundRect(w*.012f,4,w*.988f,h-3,34,34,p);p.setShader(null);
  glow(c,w*.15f,h*.28f,w*.47f,0x554e37ef);glow(c,w*.91f,h*.75f,w*.53f,0x5542e9d9);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(4,w*.014f));
  p.setShader(new SweepGradient(cx,cy,new int[]{cyan,0xff5c8eff,violet,0xffff97ec,cyan},null));
  c.drawCircle(cx,cy,r,p);p.setShader(null);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(10,w*.034f));p.setColor(0x225ac7ff);c.drawCircle(cx,cy,r+9,p);
  p.setStyle(Paint.Style.FILL);p.setColor(0xee080f23);c.drawCircle(cx,cy,r-7,p);
  String time=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(calendar.getTime());
  String date=new SimpleDateFormat("EEE, d 'de' MMMM",new Locale("es","AR")).format(calendar.getTime());
  p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setColor(white);
  p.setTextSize(w*.206f);c.drawText(time,cx,cy+w*.045f,p);
  p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setColor(0xffdce7ff);
  p.setTextSize(w*.044f);c.drawText(date,cx,cy+w*.135f,p);
  p.setTextSize(w*.030f);p.setColor(0xff8cdded);c.drawText("ALERTA VISUAL · SENSORIAL",cx,cy+w*.205f,p);
  if(isAttachedToWindow())postInvalidateDelayed(1000);
 }
}
