package com.despertadorflash;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Real spectral amplitude display. No simulated motion in silence. */
public final class MusicSpectrumView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path line=new Path(),fill=new Path();
 private final float[] current=new float[MusicSpectrum.BANDS];
 private boolean beat;
 public MusicSpectrumView(Context c){super(c);setContentDescription("Espectro de frecuencias: graves a la izquierda, agudos a la derecha");}
 public void show(MusicSpectrum.Result data){
  for(int i=0;i<current.length;i++)current[i]=Math.max(data.spectrum[i],current[i]*.68f);
  beat=data.beat;invalidate();
 }
 public void reset(){java.util.Arrays.fill(current,0);beat=false;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight(),pad=12*getResources().getDisplayMetrics().density;
  if(w<=pad*2||h<=pad*2)return;
  paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xe0080b12);
  canvas.drawRoundRect(0,0,w,h,pad,pad,paint);
  paint.setColor(0xff23404a);paint.setStrokeWidth(1);
  for(int i=1;i<5;i++)canvas.drawLine(pad,h*i/5,w-pad,h*i/5,paint);
  float base=h*.82f,span=h*.68f;
  line.reset();fill.reset();fill.moveTo(pad,base);
  for(int i=0;i<current.length;i++){
   float x=pad+(w-2*pad)*i/(current.length-1),y=base-span*current[i];
   if(i==0)line.moveTo(x,y);else line.lineTo(x,y);
   fill.lineTo(x,y);
  }
  fill.lineTo(w-pad,base);fill.close();
  paint.setShader(new LinearGradient(0,0,w,0,new int[]{0x55ffdc24,0x5532efff,0x55fa3fc8},null,Shader.TileMode.CLAMP));
  canvas.drawPath(fill,paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
  paint.setShader(new LinearGradient(0,0,w,0,new int[]{0xffffd633,0xff32efff,0xfff648d5},null,Shader.TileMode.CLAMP));
  paint.setStrokeWidth(pad*(beat?.94f:.62f));canvas.drawPath(line,paint);
  paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xffe5f8ff);
  paint.setTextSize(pad*.85f);canvas.drawText("GRAVES",pad,h-pad*.5f,paint);
  String mid="MEDIOS",hi="AGUDOS";canvas.drawText(mid,(w-paint.measureText(mid))/2,h-pad*.5f,paint);
  canvas.drawText(hi,w-pad-paint.measureText(hi),h-pad*.5f,paint);
 }
}
