"""AURORA 4.27: explicit music rhythm haptics, no stored microphone data."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[2]
M=(R/"app/src/main/java/com/despertadorflash/AuroraMusicActivity.java").read_text(encoding="utf-8")
MAIN=(R/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
MANIFEST=(R/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
class MusicHapticsContract(unittest.TestCase):
 def test_navigation_and_permission(self):
  self.assertIn('new Intent(this,AuroraMusicActivity.class)',MAIN)
  self.assertIn('android:name=".AuroraMusicActivity" android:exported="false"',MANIFEST)
  self.assertIn('Manifest.permission.RECORD_AUDIO',M)
  self.assertIn('requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},762)',M)
 def test_signal_is_real_pcm_and_bass(self):
  self.assertIn('AudioRecord.getMinBufferSize',M)
  self.assertIn('AudioFormat.ENCODING_PCM_16BIT',M)
  self.assertIn('owned.read(data,0,data.length)',M)
  self.assertIn('lowBandPower(data,count)',M)
  self.assertIn('80,125,180',M)
  self.assertIn('MIN_BEAT_INTERVAL_MS=220',M)
  self.assertIn('VibrationEffect.createOneShot',M)
 def test_explicit_onoff_no_recorded_files(self):
  self.assertIn('start.setOnClickListener(v->{if(running)stopCapture();else startCapture();})',M)
  self.assertIn('onPause(){super.onPause();stopCapture()',M)
  self.assertIn('vibrator.cancel()',M)
  self.assertIn('old.stop()',M)
  self.assertIn('old.release()',M)
  self.assertNotIn('FileOutputStream',M)
  self.assertNotIn('MediaRecorder.OutputFormat',M)
  self.assertNotIn('HttpURLConnection',M)
 def test_respects_single_microphone_owner(self):
  self.assertIn('AuroraSoundMonitorService.KEY_ACTIVE',M)
  self.assertIn('AuroraSoundMonitorService.STOP',M)
  self.assertIn('ui.postDelayed(this::beginRecorder,650L)',M)
if __name__=="__main__":unittest.main()
