package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ImageDecodePolicyTest {
 @Test public void handlesVeryLargeCameraImages(){
  assertEquals(4,ImageDecodePolicy.sampleSize(8000,6000,2000));
  assertEquals(2,ImageDecodePolicy.sampleSize(4032,3024,2500));
  assertEquals(1,ImageDecodePolicy.sampleSize(800,600,1600));
 }
 @Test public void rejectsInvalidDimensions(){
  assertFalse(ImageDecodePolicy.dimensionsAllowed(0,100));
  assertFalse(ImageDecodePolicy.dimensionsAllowed(100,40000));
  assertTrue(ImageDecodePolicy.dimensionsAllowed(4000,3000));
 }
}
