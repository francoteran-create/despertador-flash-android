package com.despertadorflash;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

/**
 * AURORA / STREET STAGE.
 * One original hand-drawn character, direct-touch floating actions.
 * All artwork is resolution-independent Canvas geometry with no raster assets.
 */
public final class AuroraCharacterStage extends FrameLayout {
 public interface OnFeature { void open(int which); }
 public static final int SOUNDS=0,VOICE=1,ALARMS=2,MESSAGES=3,PROFILE=4,RECORDS=5,TIMER=6,SETTINGS=7;
 private final String[] labels={"SONIDOS","VOZ A TEXTO","ALARMAS","MENSAJES","MI PERFIL","REGISTROS","TEMPORIZADOR","AJUSTES"};
 private final String[] glyphs={"◖))","◉","◷","✎","◎","▤","◴","⚙"};
 private final Button[] controls=new Button[8];
 private final StreetDrawing drawing;
 private final OnFeature action;
 private int selected=-1;
 private long gestureAt;
 private boolean opening=false;
 private final boolean reduceMotion;
 private final PowerManager power;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 public AuroraCharacterStage(Context context,OnFeature action){
  super(context);this.action=action;
  power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
  boolean motion=false;
  try{motion=Settings.Global.getFloat(context.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;}catch(Exception ignored){}
  reduceMotion=motion||!context.getSharedPreferences("aurora",0).getBoolean("aurora_motion",true);
  setBackgroundColor(Color.BLACK);
  setClipChildren(false);
  drawing=new StreetDrawing(context);addView(drawing,new LayoutParams(-1,-1));
  for(int i=0;i<controls.length;i++){
   final int index=i;
   Button b=new Button(context);
   b.setText(glyphs[i]+"\n"+labels[i]);
   b.setAllCaps(false);b.setIncludeFontPadding(false);
   b.setTextSize(13);b.setLineSpacing(dp(1),1f);
   b.setTextColor(0xffdededb);
   b.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
   b.setLetterSpacing(.04f);b.setGravity(Gravity.CENTER);
   b.setPadding(dp(4),dp(3),dp(4),dp(3));
   b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
   b.setElevation(0);b.setBackground(background(false));
   b.setContentDescription("Abrir "+labels[i]);
   b.setOnClickListener(v->choose(index));
   controls[i]=b;addView(b,new LayoutParams(dp(110),dp(65)));
  }
 }
 private android.graphics.drawable.Drawable background(boolean active){
  GradientDrawable shape=new GradientDrawable();
  shape.setColor(Color.TRANSPARENT);
  shape.setCornerRadius(dp(15));
  // Selected items get a thin chalk-like underline instead of a boxed button.
  return new RippleDrawable(ColorStateList.valueOf(0x335ebfba),shape,null);
 }
 private void choose(int which){
  if(which<0||which>=controls.length||opening)return;
  opening=true;selected=which;gestureAt=System.currentTimeMillis();
  for(int i=0;i<controls.length;i++){
   controls[i].setTextColor(i==which?0xff83e3d7:0xffdededb);
   controls[i].setBackground(background(i==which));
  }
  drawing.invalidate();
  if(action!=null)postDelayed(()->{opening=false;action.open(which);},reduceMotion?0:330);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
  if(h==0)h=(int)(w*1.8f);
  super.onMeasure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
 }
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
  super.onSizeChanged(w,h,oldW,oldH);
  // Full-bleed, eight two-line labels; all are 48dp+ tall.
  float[][] anchors={{.005f,.185f},{.68f,.185f},{.005f,.345f},{.68f,.345f},
                     {.005f,.620f},{.68f,.620f},{.005f,.790f},{.68f,.790f}};
  for(int i=0;i<8;i++){
   LayoutParams p=(LayoutParams)controls[i].getLayoutParams();
   p.width=Math.round(w*.315f);p.height=Math.max(dp(52),Math.round(h*.10f));
   p.leftMargin=Math.round(w*anchors[i][0]);p.topMargin=Math.round(h*anchors[i][1]);
   controls[i].setLayoutParams(p);
  }
 }
 private final class StreetDrawing extends View {
  private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path shape=new Path();
  private final long startTime=System.currentTimeMillis();
  private final int white=0xffedede8,grey=0xff9da19f,accent=0xff66bdb5,black=0xff000000;
  StreetDrawing(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  private void line(Canvas c,float weight,int color,float... xy){
   ink.setShader(null);ink.setColor(color);ink.setStyle(Paint.Style.STROKE);
   ink.setStrokeWidth(weight);ink.setStrokeJoin(Paint.Join.ROUND);ink.setStrokeCap(Paint.Cap.ROUND);
   shape.reset();shape.moveTo(xy[0],xy[1]);
   for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);
   c.drawPath(shape,ink);
  }
  private void curve(Canvas c,float weight,int color,float... xy){
   ink.setShader(null);ink.setColor(color);ink.setStyle(Paint.Style.STROKE);
   ink.setStrokeWidth(weight);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
   shape.reset();shape.moveTo(xy[0],xy[1]);
   for(int i=2;i+5<xy.length;i+=6)shape.cubicTo(xy[i],xy[i+1],xy[i+2],xy[i+3],xy[i+4],xy[i+5]);
   c.drawPath(shape,ink);
  }
  private void oval(Canvas c,float cx,float cy,float rx,float ry,int color,float thick){
   ink.setShader(null);ink.setColor(color);ink.setStyle(Paint.Style.STROKE);
   ink.setStrokeWidth(thick);
   c.drawOval(new RectF(cx-rx,cy-ry,cx+rx,cy+ry),ink);
  }
  private void solid(Canvas c,int color,float... xy){
   ink.setStyle(Paint.Style.FILL);ink.setColor(color);ink.setShader(null);
   shape.reset();shape.moveTo(xy[0],xy[1]);
   for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);shape.close();c.drawPath(shape,ink);
  }
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);
   float w=getWidth(),h=getHeight();
   if(w<1||h<1)return;
   c.drawColor(Color.BLACK);
   // Uniform coordinates; preserve the drawing's proportions on both tall and short phones.
   float scale=Math.min(w/360f,h/630f);
   c.save();c.translate((w-360*scale)/2,(h-630*scale)/2);c.scale(scale,scale);
   boolean activeMotion=!reduceMotion&&isShown()&&(power==null||!power.isPowerSaveMode());
   float sec=activeMotion?(System.currentTimeMillis()-startTime)/1000f:0;
   float bob=activeMotion?(float)Math.sin(sec*1.45f)*2.5f:0f;
   // Fine loose chalk marks instead of a neon/starfield background.
   line(c,1.6f,0x487cc9c0,14,124,32,116,47,121);
   line(c,1.5f,0x487cc9c0,309,127,326,118,343,125);
   line(c,1.7f,0x425c6a75,10,538,50,526,75,541);
   line(c,1.7f,0x425c6a75,295,539,320,527,351,541);
   line(c,1.8f,0x777b848d,92,563,168,556,251,564);
   // Character: oversized head, expressive face, rebellious hoodie, baggy trousers.
   c.save();c.translate(0,bob);
   // Back leg with thick outline and seam details.
   curve(c,6f,white,174,438,168,459,151,492,162,519,167,533,172,539,180,552);
   curve(c,5.8f,white,207,442,220,468,234,495,224,529,217,540,221,550);
   curve(c,3,grey,173,481,178,486,181,504,177,518);
   line(c,2.5f,grey,217,487,218,520);
   // Huge trainers, clean silhouette.
   curve(c,5.4f,white,179,547,157,553,129,552,125,565,120,580,131,587,157,586);
   curve(c,5.4f,white,157,586,178,588,191,579,183,564);
   curve(c,5.4f,white,221,545,239,549,268,552,275,566,285,583,267,590,235,589);
   curve(c,5.4f,white,235,589,219,589,210,582,216,563);
   line(c,3,white,130,574,168,573);line(c,3,white,236,577,274,579);
   line(c,2,grey,145,557,164,565);line(c,2,grey,233,560,255,568);
   // Voluminous hoodie and loose pants.
   curve(c,5.7f,white,159,357,133,381,135,418,147,444,157,455,178,462,205,458);
   curve(c,5.7f,white,205,458,229,455,236,445,235,425,230,399,221,375,211,357);
   curve(c,3.4f,grey,149,426,174,432,202,429,223,423);
   line(c,2.2f,grey,175,448,179,460);line(c,2.2f,grey,194,448,197,460);
   curve(c,4.7f,white,156,363,167,379,176,387,187,381,195,389,204,379,212,363);
   line(c,3,grey,175,389,171,410);line(c,3,grey,198,389,201,411);
   solid(c,0xffedede8,180,412,190,412,194,422,180,426);
   // Left arm hangs casually, left hand in a pocket.
   curve(c,5.6f,white,148,375,124,387,121,413,131,439,137,449,150,444,156,433);
   line(c,3,grey,128,422,136,430,148,425);
   // Right arm — compact relaxed pose; moves to the actual selected control on click.
   float handX=226f,handY=410f;
   if(selected>=0){
    Button target=controls[selected];
    float sx=(target.getLeft()+target.getWidth()*.5f);
    float sy=(target.getTop()+target.getHeight()*.5f);
    handX=(sx-(w-360*scale)/2f)/scale;
    handY=(sy-(h-630*scale)/2f)/scale-bob;
   }
   float gesture=selected>=0?Math.min(1f,(System.currentTimeMillis()-gestureAt)/260f):0f;
   float hx=226f+(handX-226f)*gesture,hy=410f+(handY-410f)*gesture;
   float elbowX=232+(hx-226)*.55f;
   float elbowY=399+(hy-410)*.48f;
   curve(c,5.5f,white,216,375,236,381,244,390,elbowX,elbowY,
                         (elbowX+hx)/2f,(elbowY+hy)/2f,hx,hy);
   oval(c,hx,hy,5,5,white,2.6f);
   if(selected>=0){
    oval(c,hx,hy,12,12,accent,1.8f);
    line(c,2,accent,hx+15,hy-12,hx+20,hy-17);
    line(c,2,accent,hx-13,hy+13,hx-18,hy+17);
   }
   // Oversized mischievous head, black fill with off-white expressive contours.
   curve(c,5.5f,white,144,265,135,283,141,321,167,339,
                           182,350,208,341,222,319);
   curve(c,5.5f,white,222,319,239,284,227,256,204,248);
   // Ears, tiny hoop earring.
   curve(c,3.6f,white,143,301,129,292,126,316,143,323);
   oval(c,136,327,6,7,white,2);
   // Cap angled and tuft of hair: recognizable graphic, not a generic stick figure.
   curve(c,5,white,140,275,135,252,149,239,174,238,
                        196,235,216,244,221,259);
   curve(c,4.5f,white,137,268,152,260,188,258,223,271);
   curve(c,4.2f,white,150,241,165,222,193,222,220,244);
   curve(c,4.2f,white,137,270,124,275,117,286,137,292,
                        153,293,162,290,170,285);
   // Hair escaping the cap.
   line(c,4.1f,white,217,254,226,246,228,259);
   line(c,3.8f,white,222,269,235,264,229,281);
   // Raised eyebrows, cheek marks and tilted confident smile.
   line(c,3.7f,white,156,288,171,284);
   line(c,3.7f,white,198,287,211,285);
   line(c,3.6f,white,161,299,168,300);
   line(c,3.6f,white,201,298,208,295);
   curve(c,3.8f,white,173,317,187,326,202,316,207,309);
   line(c,2.5f,grey,146,311,151,313);
   line(c,2,grey,215,310,219,308);
   // Streetwear loose collar.
   curve(c,4.2f,white,166,341,156,353,161,367,176,371);
   curve(c,4.2f,white,201,340,215,348,212,365,197,373);
   c.restore();
   if(activeMotion)postInvalidateDelayed(66);
   c.restore();
  }
 }
}
