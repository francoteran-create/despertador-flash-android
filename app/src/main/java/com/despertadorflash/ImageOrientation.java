package com.despertadorflash;

/** Maps standard EXIF orientation flags to the rotations accepted by ML Kit. */
public final class ImageOrientation {
 private ImageOrientation(){}
 public static int degrees(int orientation){
  switch(orientation){
   case 3:case 4:return 180;
   case 5:case 6:return 90;
   case 7:case 8:return 270;
   default:return 0;
  }
 }
}
