package com.despertadorflash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Four accessible black/white cloud navigation targets, not decorative labels. */
public final class AuroraHeroCloudButton extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path cloud=new Path();
 private final String title;
 public AuroraHeroCloudButton(Context context,String title){
  super(context);
  this.title=title;
  setContentDescription("Abrir "+title);
  setClickable(true);
  setFocusable(true);
  setMinimumHeight(dp(76));
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  setMeasuredDimension(resolveSize(dp(132),widthSpec),resolveSize(dp(90),heightSpec));
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight();
  float scale=Math.min(w/2.8f,h/2.05f);
  canvas.save();
  canvas.translate(w*.5f,h*.48f);canvas.scale(scale,scale);
  cloud.reset();
  cloud.moveTo(-1.05f,.42f);
  cloud.cubicTo(-1.45f,.13f,-1.24f,-.47f,-.78f,-.48f);
  cloud.cubicTo(-.80f,-.96f,-.26f,-1.08f,.01f,-.86f);
  cloud.cubicTo(.37f,-1.15f,.89f,-.82f,.90f,-.41f);
  cloud.cubicTo(1.38f,-.27f,1.39f,.36f,.98f,.51f);
  cloud.cubicTo(.40f,.75f,-.57f,.73f,-1.05f,.42f);
  cloud.close();
  p.setStyle(Paint.Style.FILL);
  p.setColor(isPressed()?0xffc7c7c7:Color.WHITE);
  canvas.drawPath(cloud,p);
  p.setStyle(Paint.Style.STROKE);p.setColor(0xff777777);p.setStrokeWidth(.035f);
  canvas.drawPath(cloud,p);
  p.setStyle(Paint.Style.FILL);
  p.setColor(Color.BLACK);p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",1));
  p.setTextAlign(Paint.Align.CENTER);p.setTextSize(title.equals("CONVERSACIÓN")?.29f:.36f);
  canvas.drawText(title,0f,.15f,p);
  canvas.restore();
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
}
