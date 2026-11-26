package com.despertadorflash;

/** On-device FFT and onset detector. Levels are digital dBFS, not calibrated SPL. */
public final class MusicSpectrum {
 public static final int FRAME=2048, RATE=44100, BANDS=48;
 private final double[] re=new double[FRAME],im=new double[FRAME],window=new double[FRAME];
 private final double[] previous=new double[3],average=new double[3];
 private boolean initialized=false;
 private long lastBeat=-10000;
 public MusicSpectrum(){for(int i=0;i<FRAME;i++)window[i]=.5-.5*Math.cos(2*Math.PI*i/(FRAME-1));}
 public static final class Result {
  public final float[] spectrum;
  public final double db,bass,mid,treble;
  public final boolean beat;
  public final int band;
  Result(float[] spectrum,double db,double bass,double mid,double treble,boolean beat,int band){
   this.spectrum=spectrum;this.db=db;this.bass=bass;this.mid=mid;this.treble=treble;this.beat=beat;this.band=band;
  }
 }
 private static double db(double power){return 10*Math.log10(Math.max(1e-10,power));}
 public Result analyze(short[] samples,int n,long now,int threshold,int sensitivity){
  n=Math.max(0,Math.min(Math.min(n,samples.length),FRAME));
  double mean=0,energy=0;
  for(int i=0;i<n;i++)mean+=samples[i]/32768.0;
  mean/=Math.max(1,n);
  for(int i=0;i<FRAME;i++){
   double x=i<n?samples[i]/32768.0-mean:0;
   energy+=x*x;re[i]=x*window[i];im[i]=0;
  }
  double level=db(energy/Math.max(1,n));
  for(int i=1,j=0;i<FRAME;i++){
   int bit=FRAME>>1;for(; (j&bit)!=0;bit>>=1)j^=bit;j^=bit;
   if(i<j){double tmp=re[i];re[i]=re[j];re[j]=tmp;}
  }
  for(int len=2;len<=FRAME;len<<=1){
   double angle=-2*Math.PI/len,wr0=Math.cos(angle),wi0=Math.sin(angle);
   for(int i=0;i<FRAME;i+=len){double wr=1,wi=0;
    for(int j=0;j<len/2;j++){int u=i+j,v=u+len/2;
     double tr=re[v]*wr-im[v]*wi,ti=re[v]*wi+im[v]*wr;
     re[v]=re[u]-tr;im[v]=im[u]-ti;re[u]+=tr;im[u]+=ti;
     double next=wr*wr0-wi*wi0;wi=wr*wi0+wi*wr0;wr=next;
    }
   }
  }
  float[] visual=new float[BANDS];double[] power=new double[3];
  for(int k=1;k<FRAME/2;k++){
   double hz=k*(double)RATE/FRAME;if(hz<40||hz>16000)continue;
   double p=2*(re[k]*re[k]+im[k]*im[k])/(FRAME*(double)FRAME*.375);
   int band=hz<250?0:hz<2000?1:2;power[band]+=p;
   int bin=Math.min(BANDS-1,Math.max(0,(int)(Math.log(hz/40)/Math.log(16000.0/40)*BANDS)));
   visual[bin]=Math.max(visual[bin],(float)Math.max(0,Math.min(1,(db(p)+80)/65)));
  }
  double ratio=sensitivity==2?1.65:sensitivity==1?2.2:3.0;
  double best=0;int dominant=0;
  for(int i=0;i<3;i++){
   if(!initialized){average[i]=Math.max(1e-9,power[i]);previous[i]=power[i];}
   double base=Math.max(1e-9,Math.max(average[i],previous[i]*.8));
   double rise=power[i]/base;
   if(rise>best&&power[i]>Math.pow(10,(threshold-9)/10.0)){best=rise;dominant=i;}
   previous[i]=power[i];average[i]=average[i]*.94+power[i]*.06;
  }
  boolean beat=initialized&&level>=threshold&&best>=ratio&&now-lastBeat>=180;
  if(beat)lastBeat=now;
  initialized=true;
  return new Result(visual,level,db(power[0]),db(power[1]),db(power[2]),beat,dominant);
 }
}
