package com.despertadorflash;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Random;

/** Lightweight locally-rendered aurora sky; no network or bitmap required. */
public final class AuroraSkyDrawable extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private void curtain(Canvas canvas,int w,int h,float x,float width,int color,float phase){
  Path ribbon=new Path();float top=h*.08f;
  for(int i=0;i<=22;i++){
   float y=h*i/22f;
   float mid=x+(float)Math.sin(i*.38f+phase)*w*.16f;
   if(i==0)ribbon.moveTo(mid-width*.5f,y);else ribbon.lineTo(mid-width*.5f,y);
  }
  for(int i=22;i>=0;i--){
   float y=h*i/22f;
   float mid=x+(float)Math.sin(i*.38f+phase)*w*.16f;
   ribbon.lineTo(mid+width*.5f,y);
  }
  ribbon.close();
  paint.setStyle(Paint.Style.FILL);
  paint.setShader(new LinearGradient(x-width,top,x+width*.6f,h*.88f,
    new int[]{0x0020ffb7,color,0x005ffddd},new float[]{0f,.43f,1f},Shader.TileMode.CLAMP));
  canvas.drawPath(ribbon,paint);paint.setShader(null);
 }
 @Override public void draw(Canvas canvas){
  android.graphics.Rect b=getBounds();int w=b.width(),h=b.height();if(w==0||h==0)return;
  paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff070d26,0xff102348,0xff090c24},null,Shader.TileMode.CLAMP));
  canvas.drawRect(b,paint);paint.setShader(null);
  curtain(canvas,w,h,w*.20f,w*.45f,0x5540f6c6,.6f);
  curtain(canvas,w,h,w*.72f,w*.38f,0x66468bff,2.1f);
  curtain(canvas,w,h,w*.88f,w*.24f,0x665fddaa,4.5f);
  Random rng=new Random(1407);
  paint.setStyle(Paint.Style.FILL);
  for(int i=0;i<110;i++){
   float x=rng.nextFloat()*w;float y=rng.nextFloat()*h;
   paint.setColor(i%5==0?0x99e9faff:0x5bd5e9ff);
   canvas.drawCircle(x,y,i%7==0?1.5f:.7f,paint);
  }
 }
 @Override public void setAlpha(int alpha){}
 @Override public void setColorFilter(ColorFilter colorFilter){}
 @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
