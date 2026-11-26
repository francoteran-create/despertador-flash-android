"""AURORA 4.11 regression contracts for robust speech and sound recovery.

Physical validation is still required on the Galaxy S9+.
"""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"app/src/main/java/com/despertadorflash"

class RecoverySafetyContract(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.speech=(JAVA/"SpeechRetryController.java").read_text("utf-8")
  cls.sense=(JAVA/"SenseActivity.java").read_text("utf-8")
  cls.service=(JAVA/"AuroraSoundMonitorService.java").read_text("utf-8")
  cls.policy=(JAVA/"SoundRecoveryPolicy.java").read_text("utf-8")
 def test_speech_temporary_errors_never_stop_due_to_attempt_count(self):
  self.assertIn("Math.min(failures,BACKOFF_MS.length-1)",self.speech)
  self.assertIn("30000",self.speech)
  self.assertIn("if(delay>=0)",self.sense)
  self.assertIn("SpeechRetryController.isSilence(error)",self.sense)
  self.assertIn("SpeechRetryController.isTemporary(error)",self.sense)
  self.assertIn("releaseSpeech();",self.sense)
 def test_sound_activity_rejects_old_model_callbacks_before_touching_history(self):
  self.assertIn("activeClassifier.set(false);",self.sense)
  self.assertIn("if(!activeClassifier.get()||!soundRunning||soundSession!=run||r==null)return;",self.sense)
  self.assertIn("if(!activeClassifier.get()||!soundRunning||soundSession!=run||mode!=MODE_SOUND)return;",self.sense)
 def test_background_monitor_rejects_old_model_callbacks(self):
  self.assertIn("activeClassifier.set(false);",self.service)
  self.assertIn("if(!activeClassifier.get()||!listening||sessionId!=run)return;",self.service)
  self.assertIn("if(!activeClassifier.get())return;",self.service)
 def test_sound_recovery_is_finite_and_honest(self):
  self.assertIn("7000L,12000L",self.policy)
  self.assertIn("reportFailure(",self.service)
  self.assertIn("ESCUCHA DETENIDA · ERROR",self.sense)
  self.assertIn("intento \"+retry+\" de 5",self.service)
 def test_no_auto_translation_regression(self):
  callback=self.sense.split("public void onResults(Bundle data){",1)[1].split("public void onPartialResults(Bundle data){",1)[0]
  self.assertNotIn("translatePhrase(",callback)
  self.assertIn("recordEvent(phrase)",callback)

if __name__=="__main__":
 unittest.main()
