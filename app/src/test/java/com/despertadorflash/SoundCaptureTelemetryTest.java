package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;

public class SoundCaptureTelemetryTest {
 @Test public void reportsModelStallVersusSilentAudioWithoutRecordingSounds(){
  SoundCaptureTelemetry stats=new SoundCaptureTelemetry();
  stats.begin(1000);
  assertEquals(-1L,stats.callbackAge(2000));
  stats.samplesRead(8000);stats.classified(3500);
  assertEquals(8000L,stats.getSamples());
  assertEquals(1L,stats.getResults());
  assertEquals(500L,stats.callbackAge(4000));
  assertTrue(stats.report(4000).contains("Respuestas del modelo: 1"));
 }
 @Test public void preservesFaultForDiagnosticUntilNewSession(){
  SoundCaptureTelemetry stats=new SoundCaptureTelemetry();
  stats.begin(0);stats.recovering("Micrófono sin muestras");
  stats.failed("El modelo dejó de responder");
  assertEquals(1L,stats.getRecoveries());
  assertEquals("El modelo dejó de responder",stats.getLastFailure());
  stats.begin(1000);
  assertEquals("ninguno",stats.getLastFailure());
  assertEquals(0L,stats.getRecoveries());
 }
 @Test public void ignoresNonPositiveReadCounts(){
  SoundCaptureTelemetry stats=new SoundCaptureTelemetry();
  stats.begin(0);stats.samplesRead(-1);stats.samplesRead(0);
  assertEquals(0L,stats.getSamples());
 }
}
