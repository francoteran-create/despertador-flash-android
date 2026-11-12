package com.despertadorflash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

/** Small portrait-phone-with-up/down-arrows icon to enter landscape mode. */
public final class RotatePhoneIconView extends View {
 private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
 public RotatePhoneIconView(Context context){
  super(context);
  setContentDescription("Girar celular y abrir vista horizontal");
  setClickable(true);setFocusable(true);
  TypedValue val=new TypedValue();
  if(context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,val,true))
   setBackgroundResource(val.resourceId);
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  int save=canvas.save();
  float size=Math.min(getWidth(),getHeight());
  canvas.translate((getWidth()-size)/2f,(getHeight()-size)/2f);
  canvas.scale(size/48f,size/48f);
  stroke.setColor(Color.WHITE);
  stroke.setStrokeWidth(2.25f);
  stroke.setStyle(Paint.Style.STROKE);
  stroke.setStrokeCap(Paint.Cap.ROUND);
  stroke.setStrokeJoin(Paint.Join.ROUND);
  canvas.drawRoundRect(new RectF(16,7,32,41),3f,3f,stroke);
  canvas.drawLine(22,36,26,36,stroke);
  // Left arrow points UP; right arrow points DOWN.
  canvas.drawLine(9,36,9,13,stroke);
  canvas.drawLine(5,17,9,13,stroke);
  canvas.drawLine(13,17,9,13,stroke);
  canvas.drawLine(39,12,39,35,stroke);
  canvas.drawLine(35,31,39,35,stroke);
  canvas.drawLine(43,31,39,35,stroke);
  canvas.restoreToCount(save);
 }
}
