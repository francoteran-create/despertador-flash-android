"""AURORA landscape screens share the existing mic and preserve one session."""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"app/src/main/java/com/despertadorflash"

class WideViewContract(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.sense=(JAVA/"SenseActivity.java").read_text(encoding="utf-8")
  cls.spectrum=(JAVA/"AuroraSpectrumView.java").read_text(encoding="utf-8")
  cls.manifest=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
 def test_orientation_is_handled_without_activity_recreation(self):
  self.assertIn('android:name=".SenseActivity" android:exported="false" android:configChanges=',self.manifest)
  self.assertIn('orientation|screenSize|smallestScreenSize|screenLayout',self.manifest)
  self.assertIn('void onConfigurationChanged(Configuration config)',self.sense)
  self.assertIn('ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE',self.sense)
  self.assertIn('ActivityInfo.SCREEN_ORIENTATION_PORTRAIT',self.sense)
 def test_fullscreen_does_not_take_another_microphone(self):
  wide=self.sense[self.sense.index('private void buildWideView()'):self.sense.index('private void showRecentSounds()')]
  self.assertNotIn('new AudioRecord(',wide)
  self.assertNotIn('startSpeech()',wide)
  self.assertNotIn('startSounds()',wide)
  self.assertNotIn('finishHistory()',wide)
  self.assertIn('wideMode=true',wide)
  self.assertIn('wideMode=false',wide)
  self.assertIn('stopAll(true)',wide)
 def test_live_pcm_frequencies_not_classifier_scores(self):
  self.assertIn('LiveAudioSpectrum.fromPcm(data.getBuffer())',self.sense)
  self.assertIn('wideSpectrum.setSpectrum(spectrum)',self.sense)
  self.assertIn('spectrum.level>=.70f',self.sense)
  self.assertNotIn('new AudioRecord(',self.spectrum)
  self.assertIn('Color.HSVToColor',self.spectrum)
 def test_original_and_translation_mirrored(self):
  self.assertIn('wideOriginal.setText(result.getText())',self.sense)
  self.assertIn('wideTranslation.setText(translationResult.getText())',self.sense)
  self.assertIn('wideLabel.setText(result.getText())',self.sense)
  self.assertIn('result.addTextChangedListener(mirror)',self.sense)
  self.assertIn('translationResult.addTextChangedListener(mirror)',self.sense)
 def test_stopping_closes_spectrum(self):
  self.assertIn('wideSpectrum.setListening(false)',self.sense)
  self.assertIn('soundRunning=false',self.sense)
if __name__=="__main__":
 unittest.main()
