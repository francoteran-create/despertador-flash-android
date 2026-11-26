package com.despertadorflash;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Static neon line icons; native controls own all input and accessibility. */
public final class NeonHomeIcon extends View {
 private final int kind;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private Canvas canvas;
 public NeonHomeIcon(Context c,int kind){super(c);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 private void color(int color){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setShadowLayer(4,0,0,color);}
 private void line(float a,float b,float c,float d){canvas.drawLine(a,b,c,d,paint);}
 private void oval(float a,float b,float c,float d){canvas.drawOval(a,b,c,d,paint);}
 private void arc(float a,float b,float c,float d,float start,float sweep){canvas.drawArc(a,b,c,d,start,sweep,false,paint);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);canvas=c;float scale=Math.min(getWidth(),getHeight())/64f;c.save();c.translate((getWidth()-64*scale)/2,(getHeight()-64*scale)/2);c.scale(scale,scale);
 int Y=0xffffff24,C=0xff32efff;color(Y);
 switch(kind){
 case 0: {Path p=new Path();p.moveTo(16,43);p.lineTo(21,37);p.lineTo(21,27);p.cubicTo(21,13,43,13,43,27);p.lineTo(43,37);p.lineTo(48,43);p.close();c.drawPath(p,paint);arc(27,42,37,51,0,180);line(32,6,32,11);line(11,15,16,20);line(53,15,48,20);break;}
 case 1: color(C);oval(12,15,52,55);color(Y);line(32,35,32,23);line(32,35,43,30);line(32,7,32,15);line(26,7,38,7);line(48,17,53,12);break;
 case 2: {Path p=new Path();p.moveTo(22,23);p.cubicTo(23,6,52,6,51,26);p.cubicTo(51,39,37,38,37,47);p.cubicTo(37,57,24,55,24,46);c.drawPath(p,paint);arc(27,17,43,34,170,245);color(C);for(int i=0;i<3;i++){float x=5+i*5;line(x,30-i*4,x,34+i*4);}break;}
 case 3: color(C);c.drawRoundRect(13,9,29,37,8,8,paint);arc(7,20,35,46,0,180);line(21,46,21,55);line(12,55,30,55);color(Y);line(40,18,56,18);line(40,29,52,29);line(40,40,56,40);break;
 case 4: {arc(17,14,47,45,180,180);line(17,29,17,43);line(47,29,47,43);color(C);c.drawRoundRect(10,43,54,52,3,3,paint);color(Y);line(32,5,32,10);line(9,13,14,18);line(55,13,50,18);line(24,30,24,39);break;}
 case 5: for(int i=0;i<3;i++){color(Y);oval(8,12+i*15,14,18+i*15);color(C);line(22,15+i*15,54,15+i*15);}break;
 case 6: color(C);line(24,14,48,8);line(24,14,24,44);line(48,8,48,37);color(Y);oval(10,39,24,51);oval(34,32,48,44);break;
 case 7: color(C);c.drawRoundRect(7,10,57,43,7,7,paint);line(15,43,15,54);line(15,54,28,43);color(Y);line(16,21,48,21);line(16,31,40,31);break;
 case 8: color(C);oval(23,7,41,25);arc(13,26,51,59,180,180);line(13,42,13,53);line(51,42,51,53);line(13,53,51,53);break;
 case 9: color(Y);line(9,40,55,40);line(9,40,9,54);line(55,40,55,54);color(C);oval(11,8,25,22);oval(39,8,53,22);arc(7,22,29,42,180,180);arc(35,22,57,42,180,180);break;
 default: color(C);oval(17,17,47,47);oval(26,26,38,38);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(32+(float)Math.cos(a)*16,32+(float)Math.sin(a)*16,32+(float)Math.cos(a)*25,32+(float)Math.sin(a)*25);}break;
 }
 paint.clearShadowLayer();c.restore();}
}
