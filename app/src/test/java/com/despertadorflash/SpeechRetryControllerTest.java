package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechRetryControllerTest {
 @Test public void silenceDoesNotCountAsRepeatedFailure(){
  SpeechRetryController policy=new SpeechRetryController();
  for(int i=0;i<50;i++)assertEquals(1200L,policy.silenceDelay());
  assertEquals(0,policy.failures());
 }
 @Test public void temporaryErrorsRecoverIndefinitelyWithBoundedBatterySafeBackoff(){
  SpeechRetryController policy=new SpeechRetryController();
  long[] expected={800,1400,2400,4000,6000,9000,15000,22000,30000};
  for(long delay:expected)assertEquals(delay,policy.onTemporaryFailure());
  for(int k=0;k<80;k++)assertEquals(30000L,policy.onTemporaryFailure());
  assertEquals(expected.length+80,policy.failures());
  policy.reset();
  assertEquals(0,policy.failures());
  assertEquals(800L,policy.onTemporaryFailure());
 }
 @Test public void classifiesRecoverableAndFatalCodes(){
  assertTrue(SpeechRetryController.isSilence(6));
  assertTrue(SpeechRetryController.isSilence(7));
  assertTrue(SpeechRetryController.isTemporary(1));
  assertTrue(SpeechRetryController.isTemporary(3));
  assertTrue(SpeechRetryController.isTemporary(5));
  assertTrue(SpeechRetryController.isTemporary(8));
  assertTrue(SpeechRetryController.isTemporary(10));
  assertTrue(SpeechRetryController.isTemporary(11));
  assertFalse(SpeechRetryController.isTemporary(9));
  assertFalse(SpeechRetryController.isTemporary(12));
  assertFalse(SpeechRetryController.isSilence(3));
 }
 @Test public void errorTextIsUnderstandable(){
  assertTrue(SpeechRetryController.explanation(9).contains("Permiso"));
  assertTrue(SpeechRetryController.explanation(12).contains("Idioma"));
 }
}
