"""Regression checks for bounded LED recovery and microphone cancellation."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[2]/"app/src/main/java/com/despertadorflash"
S=(R/"AlarmEffectsService.java").read_text(encoding="utf-8")
T=(R/"TorchFlasher.java").read_text(encoding="utf-8")
M=(R/"AuroraMusicActivity.java").read_text(encoding="utf-8")
A=(R/"AlarmTone.java").read_text(encoding="utf-8")
class AlarmEffectRecovery(unittest.TestCase):
 def test_led_recovery_is_bounded_and_cancelled(self):
  self.assertIn("ledAttempts>=4",S)
  self.assertIn("handler.postDelayed(ledWatchdog,2000L)",S)
  self.assertIn("handler.removeCallbacks(ledWatchdog)",S)
  self.assertIn("boolean next=!on;camera.setTorchMode(cameraId,next);on=next",T)
 def test_alarm_priority_and_cancel_pending_capture(self):
  self.assertIn("if(!startPending||isFinishing()",M)
  self.assertIn("startPending=false;running=false;ui.removeCallbacksAndMessages(null)",M)
  self.assertIn("if(vibrator!=null&&AlarmEffectsService.activeId<0)vibrator.cancel()",M)
  self.assertIn("if(AlarmEffectsService.activeId>=0||vibrator==null",M)
  self.assertIn("recorder==owned",M)
 def test_independent_effect_diagnostics(self):
  self.assertIn('putString("last_alarm_vibration"+id',S)
  self.assertIn('putString("last_alarm_audio"+id',S)
  self.assertIn("AudioManager.STREAM_ALARM",S)
  self.assertIn("AudioTrack.PLAYSTATE_PLAYING",A)
  self.assertIn("AudioTrack.SUCCESS",A)
if __name__=="__main__":unittest.main()
