package com.despertadorflash;

/**
 * Frequency analysis from the SAME normalized PCM buffer feeding YAMNet.
 * No second microphone, recording, saving or uploads.
 * These values indicate relative spectrum/level, not calibrated decibels.
 */
public final class LiveAudioSpectrum {
 public static final int BANDS=32;
 public static final int WAVEFORM_POINTS=160;
 private static final int FFT_SIZE=1024;
 private static final int SAMPLE_RATE=16000;
 private LiveAudioSpectrum(){}
 public static final class Reading {
  public final float[] bands;
  public final float level;
  public final float[] waveform;
  public Reading(float[] bands,float level,float[] waveform){this.bands=bands;this.level=level;this.waveform=waveform;}
 }
 public static Reading fromPcm(float[] pcm){
  float[] bands=new float[BANDS];
  float[] waveform=new float[WAVEFORM_POINTS];
  if(pcm==null||pcm.length<FFT_SIZE)return new Reading(bands,0f,waveform);
  double[] re=new double[FFT_SIZE],im=new double[FFT_SIZE];
  int start=pcm.length-FFT_SIZE;
  double energy=0;
  for(int point=0;point<WAVEFORM_POINTS;point++){
   int ix=start+(point*(FFT_SIZE-1)/(WAVEFORM_POINTS-1));
   waveform[point]=Math.max(-1f,Math.min(1f,pcm[ix]));
  }
  for(int i=0;i<FFT_SIZE;i++){
   double raw=Math.max(-1d,Math.min(1d,pcm[start+i]));
   energy+=raw*raw;
   double hann=.5d-.5d*Math.cos(2d*Math.PI*i/(FFT_SIZE-1));
   re[i]=raw*hann;
  }
  // Iterative radix-2 FFT, no allocations for captured audio after return.
  for(int i=1,j=0;i<FFT_SIZE;i++){
   int bit=FFT_SIZE>>1;
   for(; (j&bit)!=0;bit>>=1)j^=bit;
   j^=bit;
   if(i<j){
    double t=re[i];re[i]=re[j];re[j]=t;
    t=im[i];im[i]=im[j];im[j]=t;
   }
  }
  for(int len=2;len<=FFT_SIZE;len<<=1){
   double angle=-2d*Math.PI/len;
   double wc=Math.cos(angle),ws=Math.sin(angle);
   for(int offset=0;offset<FFT_SIZE;offset+=len){
    double wr=1,wi=0;
    for(int k=0;k<len/2;k++){
     int a=offset+k,d=a+len/2;
     double tr=wr*re[d]-wi*im[d];
     double ti=wr*im[d]+wi*re[d];
     re[d]=re[a]-tr;im[d]=im[a]-ti;
     re[a]+=tr;im[a]+=ti;
     double nr=wr*wc-wi*ws;
     wi=wr*ws+wi*wc;wr=nr;
    }
   }
  }
  float rms=(float)Math.sqrt(energy/FFT_SIZE);
  float level=Math.min(1f,rms*5f);
  final double lowest=80d,highest=7200d;
  for(int band=0;band<BANDS;band++){
   int from=Math.max(1,(int)Math.round(lowest*Math.pow(highest/lowest,(double)band/BANDS)*FFT_SIZE/SAMPLE_RATE));
   int until=Math.min(FFT_SIZE/2-1,(int)Math.round(lowest*Math.pow(highest/lowest,(double)(band+1)/BANDS)*FFT_SIZE/SAMPLE_RATE));
   until=Math.max(from,until);
   double power=0;
   for(int k=from;k<=until;k++){
    double magnitude=Math.hypot(re[k],im[k])*4d/FFT_SIZE;
    power+=magnitude*magnitude;
   }
   double average=Math.sqrt(power/(until-from+1));
   // Saturation is smooth and remains linked to real PCM energy.
   float magnitude=(float)Math.min(1d,Math.log1p(average*13d)/Math.log(14d));
   bands[band]=Math.max(0f,Math.min(1f,magnitude));
  }
  return new Reading(bands,level,waveform);
 }
}
