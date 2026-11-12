"""AURORA 4.10: only user-requested translation may be queued.

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

    def test_final_speech_only_transcribes_and_never_auto_translates(self):
        start=self.sense.index("public void onResults(Bundle data){",self.sense.index("private void startSpeech(){"))
        finish=self.sense.index("public void onPartialResults(Bundle data){",start)
        callback=self.sense[start:finish]
        self.assertIn("recordEvent(phrase)",callback)
        self.assertNotIn("translatePhrase(phrase,true)",callback)
        self.assertIn('manualTranslationRequested=false;',callback)
        self.assertIn('translateButton.setOnClickListener(v->translateLast())',self.sense)
        self.assertIn('translatePhrase(phrase,true)',self.sense)

    def test_ui_keeps_original_and_translation_together(self):
        self.assertIn('"LO QUE SE DIJO · ORIGINAL"',self.sense)
        self.assertIn('"TRADUCCIÓN A PEDIDO"',self.sense)
        self.assertIn('"IDIOMA PARA TRADUCIR CUANDO LO PIDAS"',self.sense)
        self.assertLess(self.sense.index('"LO QUE SE DIJO · ORIGINAL"'),self.sense.index('"TRADUCCIÓN A PEDIDO"'))
        self.assertNotIn("manualTranslationInput",self.sense)
        self.assertIn("PROBAR TRADUCTOR CON TEXTO ESCRITO",self.sense)

    def test_queue_keeps_intermediate_sentences_and_persists_completed_translation(self):
        self.assertIn("translationQueue.offer(",self.sense)
        self.assertIn("pumpTranslationQueue();",self.sense)
        self.assertIn("translationQueue.complete(task.id)",self.sense)
        self.assertIn("translationQueue.isActive(task.id)",self.sense)
        self.assertIn("if(task.fromMicrophone)",self.sense)
        self.assertIn('recordEvent("TRADUCCIÓN ("+task.language+")',self.sense)
        self.assertIn("newestTranslationId",self.sense)
        self.assertIn("translationModelReady",self.sense)

    def test_stopping_speech_cancels_old_translations(self):
        self.assertIn("invalidateTranslation(); // Do not write late ML Kit results",self.sense)
        self.assertIn("translationQueue.clear()",self.sense)

    def test_main_page_links_to_single_translation_flow(self):
        self.assertIn('homeSpeechToggle.setOnClickListener(v->openSense(SenseActivity.MODE_SPEECH))',self.home)
        self.assertIn('expand.setOnClickListener(v->startActivity(new Intent(this,RecordsActivity.class)))',self.home)

if __name__=="__main__":
    unittest.main()
