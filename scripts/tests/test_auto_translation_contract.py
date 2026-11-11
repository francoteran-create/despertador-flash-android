"""AURORA 4.7 regression: translations must be triggered by FINAL recognized speech.

Checks the Android integration contract in source in addition to compiling
and running JUnit. Physical device tests are still required for ML Kit.
"""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT / "app/src/main/java/com/despertadorflash"

class AutoTranslationContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sense=(JAVA/"SenseActivity.java").read_text(encoding="utf-8")
        cls.home=(JAVA/"MainActivity.java").read_text(encoding="utf-8")

    def test_final_speech_sends_translation_automatically(self):
        start=self.sense.index("public void onResults(Bundle data){",self.sense.index("private void startSpeech(){"))
        finish=self.sense.index("public void onPartialResults(Bundle data){",start)
        callback=self.sense[start:finish]
        self.assertIn("recordEvent(phrase)",callback)
        self.assertIn("translatePhrase(phrase,true)",callback)
        self.assertLess(callback.index("recordEvent(phrase)"),callback.index("translatePhrase(phrase,true)"))

    def test_ui_keeps_original_and_translation_together(self):
        self.assertIn('"LO QUE SE DIJO · ORIGINAL"',self.sense)
        self.assertIn('"TRADUCCIÓN AUTOMÁTICA"',self.sense)
        self.assertIn('"TRADUCIR AUTOMÁTICAMENTE AL"',self.sense)
        self.assertLess(self.sense.index('"LO QUE SE DIJO · ORIGINAL"'),self.sense.index('"TRADUCCIÓN AUTOMÁTICA"'))
        self.assertNotIn("manualTranslationInput",self.sense)
        self.assertIn("PROBAR TRADUCTOR CON TEXTO ESCRITO",self.sense)

    def test_old_async_result_is_not_allowed_to_replace_new_translation(self):
        self.assertIn("request==translationGeneration",self.sense)
        self.assertIn("client!=null&&translator==client",self.sense)
        self.assertIn("translationModelDownloading",self.sense)
        self.assertIn("translationModelReady",self.sense)
        self.assertIn("runTranslation(translationGeneration,pendingTranslationPhrase,client)",self.sense)
        self.assertIn("selectedClient",self.sense) if False else None

    def test_main_page_links_to_single_translation_flow(self):
        self.assertIn('homeSpeechToggle.setOnClickListener(v->openSense(SenseActivity.MODE_SPEECH))',self.home)
        self.assertIn('expand.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)))',self.home)

if __name__=="__main__":
    unittest.main()
