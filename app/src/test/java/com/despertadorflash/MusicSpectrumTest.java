package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public class MusicSpectrumTest {
 private short[] tone(double hz,double gain){
  short[] v=new short[MusicSpectrum.FRAME];
  for(int i=0;i<v.length;i++)v[i]=(short)(32767*gain*Math.sin(2*Math.PI*hz*i/MusicSpectrum.RATE));
  return v;
 }
 @Test public void separatesBassMidAndTreble(){
  for(double hz:new double[]{100,1000,5000}){
   MusicSpectrum.Result r=new MusicSpectrum().analyze(tone(hz,.4),MusicSpectrum.FRAME,0,-42,2);
   double wanted=hz<250?r.bass:hz<2000?r.mid:r.treble;
   double other=hz<250?Math.max(r.mid,r.treble):hz<2000?Math.max(r.bass,r.treble):Math.max(r.bass,r.mid);
   assertTrue(wanted>other+15);
   assertEquals(-11,r.db,1.0);
  }
 }
 @Test public void noPulsesInSilenceOrSteadyTone(){
  MusicSpectrum a=new MusicSpectrum();
  for(int i=0;i<30;i++)assertFalse(a.analyze(tone(100,0),MusicSpectrum.FRAME,i*46,-42,2).beat);
  a=new MusicSpectrum();
  for(int i=0;i<80;i++)assertFalse(a.analyze(tone(100,.4),MusicSpectrum.FRAME,i*46,-42,2).beat);
 }
 @Test public void pulsesFollowActualAttacksAndRespectThreshold(){
  MusicSpectrum a=new MusicSpectrum();int pulses=0;
  for(int i=0;i<80;i++){
   MusicSpectrum.Result r=a.analyze(tone(100,i%10==2?.5:0),MusicSpectrum.FRAME,i*46,-42,2);
   if(r.beat){assertEquals(2,i%10);pulses++;}
  }
  assertEquals(8,pulses);
  a=new MusicSpectrum();
  a.analyze(tone(4000,0),MusicSpectrum.FRAME,0,-30,2);
  assertFalse(a.analyze(tone(4000,.001),MusicSpectrum.FRAME,250,-30,2).beat);
  MusicSpectrum.Result r=a.analyze(tone(4000,.6),MusicSpectrum.FRAME,500,-30,2);
  assertTrue(r.beat);assertEquals(2,r.band);
 }
}
