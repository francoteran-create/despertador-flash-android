package com.despertadorflash;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native UI system: glass surfaces, ripple controls and high contrast. */
public final class AuroraDesign {
 private AuroraDesign(){}
 public static final int INK=Color.BLACK, CYAN=0xff39edff, SOFT=0xffc6d6f2;
 public static int dp(Context c,float n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
 public static GradientDrawable surface(Context context,int start,int end,int border,int radius){
  GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
  g.setCornerRadius(dp(context,radius));
  if(border!=0)g.setStroke(dp(context,1),border);
  return g;
 }
 public static void action(Button button,int from,int to,int edge){
  Context c=button.getContext();
  button.setAllCaps(false);
  button.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
  button.setTextColor(Color.WHITE);
  button.setTextSize(15);
  button.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
  button.setMinHeight(dp(c,58));
  button.setPadding(dp(c,15),dp(c,10),dp(c,15),dp(c,10));
  button.setElevation(dp(c,4));
  button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff),
    surface(c,from,to,edge,23),null));
 }
 public static void panel(LinearLayout card,int from,int to){
  Context c=card.getContext();
  card.setBackground(surface(c,from,to,0xff426b86,26));
  card.setElevation(dp(c,6));
  card.setPadding(dp(c,20),dp(c,19),dp(c,20),dp(c,19));
 }
 public static void neonAction(Button button){
  action(button,0xff10141a,0xff14111a,0xff37d8ed);
  button.setTextColor(0xffffdf33);
 }
 /** Functional screens keep the whole canvas black, including scroll gaps. */
 public static android.widget.FrameLayout menuFrame(Context c,View content){
  android.widget.FrameLayout screen=new android.widget.FrameLayout(c);
  screen.setBackgroundColor(INK);
  screen.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-1));
  return screen;
 }
 public static void headline(TextView title){
  title.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(title.getContext(),R.font.permanent_marker));
  title.setTextColor(Color.WHITE);
  androidx.core.view.ViewCompat.setAccessibilityHeading(title,true);
 }
 public static LinearLayout heading(LinearLayout parent,String title,Runnable onBack){
  Context c=parent.getContext();
  LinearLayout bar=new LinearLayout(c);bar.setGravity(Gravity.CENTER_VERTICAL);
  Button back=new Button(c);back.setText("‹");back.setContentDescription("Volver");
  neonAction(back);back.setMinWidth(dp(c,48));back.setPadding(0,0,0,0);
  bar.addView(back,new LinearLayout.LayoutParams(dp(c,48),-2));back.setOnClickListener(v->onBack.run());
  TextView label=new TextView(c);label.setText(title);label.setTextSize(23);
  headline(label);label.setGravity(Gravity.CENTER);label.setPadding(dp(c,8),dp(c,10),0,dp(c,10));
  bar.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
  parent.addView(bar);
  return bar;
 }
 /** Groups existing native controls without resetting their listeners or values. */
 public static LinearLayout disclosure(LinearLayout parent,String label){
  Context c=parent.getContext();
  LinearLayout section=new LinearLayout(c);section.setOrientation(LinearLayout.VERTICAL);
  section.setVisibility(View.GONE);
  Button control=new Button(c);neonAction(control);control.setText("＋  "+label);
  control.setOnClickListener(v->{
   boolean open=section.getVisibility()!=View.VISIBLE;
   section.setVisibility(open?View.VISIBLE:View.GONE);
   control.setText((open?"−  ":"＋  ")+label);
   control.setSelected(open);
   if(android.os.Build.VERSION.SDK_INT>=30)control.setStateDescription(open?"Expandido":"Contraído");
  });
  parent.addView(control);parent.addView(section);
  return section;
 }
 public static void entry(View field){
  Context c=field.getContext();
  field.setBackground(surface(c,0xff11151b,0xff111219,0xff42636b,17));
  field.setPadding(dp(c,16),dp(c,13),dp(c,16),dp(c,13));
 }
 public static TextView overline(Context c,String t){
  TextView text=new TextView(c);
  text.setText(t);text.setTextSize(12);text.setLetterSpacing(.18f);
  text.setTextColor(CYAN);text.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
  text.setPadding(0,dp(c,8),0,dp(c,6));
  return text;
 }
 public static void spacing(View v,int top,int bottom){
  if(v.getLayoutParams() instanceof LinearLayout.LayoutParams){
   LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();
   p.topMargin=dp(v.getContext(),top);p.bottomMargin=dp(v.getContext(),bottom);v.setLayoutParams(p);
  }
 }
}
