package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ImageOrientationTest {
 @Test public void handlesPortraitAndReversePortrait(){
  assertEquals(90,ImageOrientation.degrees(6));
  assertEquals(270,ImageOrientation.degrees(8));
 }
 @Test public void handlesLandscapeAndMirroredExifOrientation(){
  assertEquals(0,ImageOrientation.degrees(1));
  assertEquals(180,ImageOrientation.degrees(3));
  assertEquals(90,ImageOrientation.degrees(5));
  assertEquals(270,ImageOrientation.degrees(7));
 }
 @Test public void unknownMetadataDoesNotRotate(){
  assertEquals(0,ImageOrientation.degrees(0));
  assertEquals(0,ImageOrientation.degrees(99));
 }
}
