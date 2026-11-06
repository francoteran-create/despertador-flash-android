package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechRetryControllerTest {
 @Test public void silenceDoesNotCountAsRepeatedFailure(){
  SpeechRetryController policy=new SpeechRetryController();
  for(int i=0;i<50;i++)assertEquals(650L,policy.silenceDelay());
  assertEquals(0,policy.failures());
 }
 @Test public void temporaryErrorsBackOffAndEventuallyPause(){
  SpeechRetryController policy=new SpeechRetryController();
  assertEquals(800,policy.onTemporaryFailure());
  assertEquals(1400,policy.onTemporaryFailure());
  assertEquals(2400,policy.onTemporaryFailure());
  assertEquals(4000,policy.onTemporaryFailure());
  assertEquals(6000,policy.onTemporaryFailure());
  assertEquals(9000,policy.onTemporaryFailure());
  assertEquals(-1,policy.onTemporaryFailure());
  policy.reset();
  assertEquals(800,policy.onTemporaryFailure());
 }
 @Test public void classifiesRecoverableAndFatalCodes(){
  assertTrue(SpeechRetryController.isSilence(6));
  assertTrue(SpeechRetryController.isSilence(7));
  assertTrue(SpeechRetryController.isTemporary(1));
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
