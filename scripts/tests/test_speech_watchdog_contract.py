"""Static regression checks for SpeechRecognizer's hang recovery wiring.

These supplement JUnit's pure Java watchdog tests. They cannot substitute a
real Android phone and do not claim uninterrupted microphone capture.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SENSE = ROOT / "app/src/main/java/com/despertadorflash/SenseActivity.java"


class SpeechWatchdogContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.code = SENSE.read_text(encoding="utf-8")

    def test_watchdog_starts_before_android_recognizer(self):
        self.assertIn("beginSpeechWatchdog(current);\n   current.startListening(intent);", self.code)
        self.assertIn("speechWatchdog.started(SystemClock.elapsedRealtime());", self.code)

    def test_real_callbacks_report_progress(self):
        for statement in (
            "speechWatchdog.ready(SystemClock.elapsedRealtime());",
            "speechWatchdog.speechBegan(SystemClock.elapsedRealtime());",
            "speechWatchdog.speechEnded(SystemClock.elapsedRealtime());",
            "speechWatchdog.partialResult(SystemClock.elapsedRealtime());",
        ):
            self.assertIn(statement, self.code)

    def test_cleanup_on_release_cancels_pending_restart(self):
        start = self.code.index("private void releaseSpeech(){")
        release = self.code[start : start + 260]
        self.assertIn("cancelSpeechWatchdog();", release)
        self.assertIn("recognizer=null;", release)
        self.assertIn("old.destroy();", release)
        self.assertIn("if(mode!=MODE_SPEECH||recognizer!=current)return;", self.code)

    def test_history_is_not_stopped_by_watchdog(self):
        start = self.code.index("private void beginSpeechWatchdog(")
        end = self.code.index("private volatile boolean soundRunning=false;", start)
        watchdog = self.code[start:end]
        self.assertIn("releaseSpeech();", watchdog)
        self.assertIn("scheduleSpeechRetry(delay);", watchdog)
        self.assertNotIn("stopAll(", watchdog)
        self.assertNotIn("finishHistory(", watchdog)
        self.assertNotIn("startActivity(", watchdog)

    def test_manual_translation_remains_opt_in(self):
        start = self.code.index("public void onResults(Bundle data)")
        end = self.code.index("public void onPartialResults(Bundle data)", start)
        self.assertNotIn("translatePhrase(", self.code[start:end])
        self.assertIn("TRADUCIR ÚLTIMA FRASE", self.code[start:end])


if __name__ == "__main__":
    unittest.main()
