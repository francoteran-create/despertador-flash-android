package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SoundFeedbackPolicyTest {
 @Test public void limitsSimultaneousVibrationEvents(){
  SoundFeedbackPolicy p=new SoundFeedbackPolicy();
  assertTrue(p.allowEvent(1000));
  assertFalse(p.allowEvent(1000));
  assertFalse(p.allowEvent(3000));
  assertTrue(p.allowEvent(4500));
 }
 @Test public void limitsCameraTorchMoreThanHaptics(){
  SoundFeedbackPolicy p=new SoundFeedbackPolicy();
  assertTrue(p.allowFlash(1));
  assertFalse(p.allowFlash(2500));
  assertFalse(p.allowFlash(12000));
  assertTrue(p.allowFlash(12001));
  assertEquals(2600L,SoundFeedbackPolicy.FLASH_DURATION_MS);
 }
 @Test public void failedCameraDoesNotWasteFlashCooldown(){
  SoundFeedbackPolicy p=new SoundFeedbackPolicy();
  assertTrue(p.allowFlash(1000));
  p.flashStartFailed(1000);
  assertTrue(p.allowFlash(2000)); // Failed start must not block a later event.
  assertFalse(p.allowFlash(3000)); // Successful start retains cooldown.
 }
 @Test public void staleFailureCannotUndoLaterSuccessfulFlash(){
  SoundFeedbackPolicy p=new SoundFeedbackPolicy();
  assertTrue(p.allowFlash(1000));
  p.flashStartFailed(1000);
  assertTrue(p.allowFlash(2000));
  p.flashStartFailed(1000); // Callback for an older attempt.
  assertFalse(p.allowFlash(3000));
 }
 @Test public void resetAllowsNewSessionAndBackwardsClock(){
  SoundFeedbackPolicy p=new SoundFeedbackPolicy();
  assertFalse(p.allowFlash(-1));
  assertTrue(p.allowEvent(40000));
  assertTrue(p.allowFlash(40000));
  p.reset();
  assertTrue(p.allowEvent(0));
  assertTrue(p.allowFlash(0));
 }
}
