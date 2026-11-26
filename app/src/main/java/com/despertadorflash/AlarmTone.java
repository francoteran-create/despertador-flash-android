package com.despertadorflash;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Original synthesized alarm sounds, no downloaded media. STREAM_ALARM usage. */
public final class AlarmTone {
 public static final String[] NAMES={"Pitido digital","Doble pitido","Campanilla","Pulso electrónico","Alarma clásica","Despertador tradicional","Melodía suave","Tono ascendente","Sirena electrónica","Campanas profundas"};
 public static final int[] DURATIONS={5,15,30,60,180};
 private static final int RATE=16000;
 private AudioTrack track;
 public static short[] samples(int selected){
  int tone=Math.max(0,Math.min(NAMES.length-1,selected));
  int ms=tone<5?1600:3600;
  short[] data=new short[RATE*ms/1000];
  double phase=0;
  for(int i=0;i<data.length;i++){
   double t=i/(double)RATE,f=0,gain=0,harmonic=0;
   int part=(int)(t*1000);
   switch(tone){
    case 0: if(part%800<240){f=1100;gain=.8;} break;
    case 1: if(part%800<140||(part%800>=220&&part%800<360)){f=850;gain=.8;} break;
    case 2: if(part%800<450){f=1300;gain=.7*Math.exp(-(part%800)/180.0);harmonic=.25;} break;
    case 3: if(part%400<160){f=600;gain=.85;harmonic=.3;} break;
    case 4: if(part%400<280){f=(part/200)%2==0?750:1100;gain=.8;} break;
    case 5: if(part%1200<850){f=(part/90)%2==0?900:1450;gain=.8;} break;
    case 6: int[] notes={523,659,784,659,587,698,880,784}; f=notes[(part/450)%notes.length];gain=.6*Math.exp(-(part%450)/300.0);break;
    case 7: f=450+part*.30;gain=.25+.6*part/3600.0;break;
    case 8: f=650+450*(.5+.5*Math.sin(2*Math.PI*t/1.2));gain=.8;break;
    case 9: f=330;gain=.85*Math.exp(-(part%1200)/420.0);harmonic=.4;break;
   }
   phase+=2*Math.PI*f/RATE;
   double edge=Math.min(1,Math.min(i,data.length-1-i)/160.0);
   data[i]=(short)(16000*edge*gain*(Math.sin(phase)+harmonic*Math.sin(2*phase))/(1+harmonic));
  }
  return data;
 }
 public void play(int selected,int volume,boolean loop){
  stop(); if(volume<=0)return;
  try{
   short[] pcm=samples(selected);
   track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();
   if(track.getState()==AudioTrack.STATE_UNINITIALIZED||track.write(pcm,0,pcm.length)!=pcm.length){stop();return;}
   track.setVolume(Math.max(0,Math.min(100,volume))/100f);
   if(loop)track.setLoopPoints(0,pcm.length,-1);
   track.play();
  }catch(RuntimeException ex){stop();android.util.Log.w("Aurora","No se pudo reproducir alarma",ex);}
 }
 public void stop(){AudioTrack old=track;track=null;if(old!=null){try{old.stop();}catch(Exception ignored){}try{old.release();}catch(Exception ignored){}}}
}
