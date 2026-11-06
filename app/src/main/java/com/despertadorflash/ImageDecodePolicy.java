package com.despertadorflash;

/** Memory budget for image analysis on older phones (e.g. Galaxy S9+). */
public final class ImageDecodePolicy {
 private ImageDecodePolicy(){}
 public static int sampleSize(int width,int height,int longestSide){
  if(width<=0||height<=0||longestSide<1)return 1;
  int largest=Math.max(width,height);
  int sample=1;
  while(sample<=Integer.MAX_VALUE/2 && largest/sample>longestSide)sample*=2;
  return sample;
 }
 public static boolean dimensionsAllowed(int width,int height){
  return width>0&&height>0&&width<=30000&&height<=30000;
 }
}
