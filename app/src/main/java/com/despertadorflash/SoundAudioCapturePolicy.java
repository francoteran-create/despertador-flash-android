package com.despertadorflash;

/**
 * MediaPipe AudioClassifier AUDIO_STREAM accepts audio blocks and handles
 * internal buffering/resampling. Reading shorter blocks reduces the time
 * needed to notice changes, but does not guarantee YAMNet can separate
 * overlapping sources.
 */
public final class SoundAudioCapturePolicy {
 public static final int SAMPLE_RATE_HZ=16000;
 public static final int BLOCK_SAMPLES=8000;
 private SoundAudioCapturePolicy(){}
 public static int blockDurationMs(){return 1000*BLOCK_SAMPLES/SAMPLE_RATE_HZ;}
}
