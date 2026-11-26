"""AURORA 4.16: language selectors must feed SpeechRecognizer and ML Kit."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
SENSE=(ROOT/"app/src/main/java/com/despertadorflash/SenseActivity.java").read_text(encoding="utf-8")
LANG=(ROOT/"app/src/main/java/com/despertadorflash/AuroraLanguageCatalog.java").read_text(encoding="utf-8")

class InternationalConversationTest(unittest.TestCase):
 def test_legacy_indices_are_preserved(self):
  self.assertIn('"es-AR","en-US","pt-BR","fr-FR"',LANG)
  self.assertIn('"es","en","pt","fr","it","de","zh","hi"',LANG)

 def test_spoken_language_is_applied_to_android(self):
  self.assertIn('AuroraLanguageCatalog.speechTag(languageSelection)',SENSE)
  self.assertIn('intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognizedLanguage());',SENSE)

 def test_translation_uses_catalog(self):
  self.assertIn('AuroraLanguageCatalog.translationTag(selected)',SENSE)
  self.assertIn('.setSourceLanguage(source).setTargetLanguage(destination)',SENSE)
  self.assertIn('client.downloadModelIfNeeded()',SENSE)

 def test_spinners_and_manual_translation(self):
  self.assertIn('sourcePicker.setOnItemSelectedListener(',SENSE)
  self.assertIn('translationTargetPicker.setOnItemSelectedListener(',SENSE)
  self.assertIn('if(chosen==languageSelection){',SENSE)
  self.assertIn('translateButton.setOnClickListener(v->translateLast());',SENSE)

 def test_no_automatic_translation_of_recognized_speech(self):
  area=SENSE[SENSE.index('public void onResults(Bundle data)'):SENSE.index('public void onPartialResults(Bundle data)')]
  self.assertNotIn('translatePhrase(',area)
  self.assertIn('TRADUCIR ÚLTIMA FRASE',area)

if __name__=="__main__":
 unittest.main()
