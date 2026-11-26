"""Privacy and integration guardrails for user-rated acoustic evaluation."""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"app/src/main/java/com/despertadorflash"
def source(name):
 return (JAVA/name).read_text(encoding="utf-8")

class SoundValidationContract(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.activity=source("SenseActivity.java")
  cls.settings=source("MainActivity.java")
  cls.store=source("SoundValidationStore.java")
  cls.metrics=source("SoundValidationMetrics.java")
  cls.history=source("RecentSoundEvents.java")
 def test_three_manual_verdicts_are_persisted(self):
  self.assertIn('SoundValidationStore.record(this,kind,false,true)',self.activity)
  self.assertIn('SoundValidationStore.record(this,',self.activity)
  self.assertIn('SoundValidationMetrics.kindForSound(label),correct,false)',self.activity)
  self.assertIn('SoundValidationStore.read(this)',self.activity)
  self.assertIn('refreshFeedbackSummary()',self.activity)
 def test_audio_and_transcripts_never_enter_metrics_store(self):
  for forbidden in ['AudioRecord', 'AudioData', 'SpeechRecognizer', 'MediaRecorder',
                    'getLastKnownLocation', 'getStringArrayList', 'wav', 'mp3', 'm4a']:
   self.assertNotIn(forbidden,self.store)
  self.assertIn('context.getSharedPreferences(PREF,Context.MODE_PRIVATE)',self.store)
  self.assertIn('getBoolean("sense_feedback_mode",false)',self.store)
  self.assertIn('putString(COUNTS,all.toString()).commit()',self.store)
  self.assertIn('MAX=1_000_000L',self.metrics)
 def test_per_sound_type_analysis_and_reset(self):
  self.assertIn('RESULTADOS DE PRUEBAS REALES',self.settings)
  self.assertIn('ACTUALIZAR RESULTADOS DE PRUEBAS',self.settings)
  self.assertIn('COPIAR RESULTADOS SIN AUDIO',self.settings)
  self.assertIn('BORRAR SOLO RESULTADOS DE PRUEBAS',self.settings)
  self.assertIn('SoundValidationStore.clear(this)',self.settings)
  self.assertIn('Las conversaciones,',self.settings)
  self.assertIn('get("sense_feedback_mode"',self.settings.replace('getBoolean("sense_feedback_mode"','get("sense_feedback_mode"'))
 def test_keeps_30_sounds_separate_from_manual_trials(self):
  self.assertIn('RecentSoundEvents.add(this,label,score)',self.activity)
  self.assertIn('SoundTimeline.append(',self.history)
  self.assertNotIn('RecentSoundEvents.add(',self.store)
  self.assertIn('MAX',self.metrics)
 def test_overflow_and_explicit_scoring_only(self):
  self.assertIn('Math.min(MAX,yes)',self.metrics)
  self.assertIn('Math.min(MAX,no)',self.metrics)
  self.assertIn('Math.min(MAX,missed)',self.metrics)
  self.assertIn('if(!context.getSharedPreferences("aurora",0)',self.store)
  self.assertIn('return false;',self.store)
if __name__=="__main__":
 unittest.main()
