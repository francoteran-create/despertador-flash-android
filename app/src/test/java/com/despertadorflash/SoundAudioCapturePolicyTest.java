package com.despertadorflash;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundAudioCapturePolicyTest {
 @Test public void blockUsesHalfSecondAt16kHz(){
  assertEquals(16000,SoundAudioCapturePolicy.SAMPLE_RATE_HZ);
  assertEquals(8000,SoundAudioCapturePolicy.BLOCK_SAMPLES);
  assertEquals(500,SoundAudioCapturePolicy.blockDurationMs());
 }
}
