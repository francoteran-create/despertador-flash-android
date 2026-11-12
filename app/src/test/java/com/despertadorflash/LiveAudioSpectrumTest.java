package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveAudioSpectrumTest {
 @Test public void silenceHasZeroIntensity(){
  LiveAudioSpectrum.Reading a=LiveAudioSpectrum.fromPcm(new float[2048]);
  assertEquals(32,a.bands.length);
  assertEquals(0f,a.level,0.0001f);
  for(float v:a.bands)assertEquals(0f,v,0.0001f);
 }
 @Test public void louderSoundProducesStrongerGraph(){
  float[] weak=new float[2048],strong=new float[2048];
  for(int i=0;i<2048;i++){
   weak[i]=(float)(.015*Math.sin(2d*Math.PI*900*i/16000));
   strong[i]=(float)(.65*Math.sin(2d*Math.PI*900*i/16000));
  }
  LiveAudioSpectrum.Reading a=LiveAudioSpectrum.fromPcm(weak);
  LiveAudioSpectrum.Reading z=LiveAudioSpectrum.fromPcm(strong);
  assertTrue(z.level>a.level);
  float wa=0,wb=0;
  for(int i=0;i<32;i++){wa+=a.bands[i];wb+=z.bands[i];}
  assertTrue(wb>wa);
 }
 @Test public void lowerAndHigherPitchesHaveDifferentPeaks(){
  float[] low=new float[2048],high=new float[2048];
  for(int i=0;i<2048;i++){
   low[i]=(float)(.45*Math.sin(2d*Math.PI*250*i/16000));
   high[i]=(float)(.45*Math.sin(2d*Math.PI*2800*i/16000));
  }
  int lowPeak=peak(LiveAudioSpectrum.fromPcm(low).bands);
  int highPeak=peak(LiveAudioSpectrum.fromPcm(high).bands);
  assertTrue("Spectral frequency must affect band position",highPeak>lowPeak+8);
 }
 @Test public void zigzagUsesActualSignedWaveformSamples(){
  float[] quiet=new float[2048],loud=new float[2048];
  for(int i=0;i<2048;i++){
   quiet[i]=(float)(.012*Math.sin(2*Math.PI*430*i/16000));
   loud[i]=(float)(.72*Math.sin(2*Math.PI*430*i/16000));
  }
  LiveAudioSpectrum.Reading a=LiveAudioSpectrum.fromPcm(quiet);
  LiveAudioSpectrum.Reading b=LiveAudioSpectrum.fromPcm(loud);
  assertEquals(LiveAudioSpectrum.WAVEFORM_POINTS,a.waveform.length);
  float quietPeak=0,loudPeak=0;
  boolean positive=false,negative=false;
  for(int i=0;i<a.waveform.length;i++){
   quietPeak=Math.max(quietPeak,Math.abs(a.waveform[i]));
   loudPeak=Math.max(loudPeak,Math.abs(b.waveform[i]));
   if(b.waveform[i]>.2f)positive=true;
   if(b.waveform[i]<-.2f)negative=true;
  }
  assertTrue(loudPeak>quietPeak*10);
  assertTrue(positive&&negative);
 }
 @Test public void invalidBuffersDoNotCrashOrPretendSound(){
  assertEquals(0f,LiveAudioSpectrum.fromPcm(null).level,0f);
  assertEquals(0f,LiveAudioSpectrum.fromPcm(new float[30]).level,0f);
 }
 private int peak(float[] x){int top=0;for(int i=1;i<x.length;i++)if(x[i]>x[top])top=i;return top;}
}
