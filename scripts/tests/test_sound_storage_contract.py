"""Regression contract: sounds never open new exported history files.

This is a source-level guard in addition to Java unit tests for the
30-event ring and conversation filtering. It cannot replace phone tests.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "app/src/main/java/com/despertadorflash"

class StorageSeparationTest(unittest.TestCase):
    def source(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_sound_monitor_uses_only_in_app_events(self):
        monitor = self.source("AuroraSoundMonitorService.java")
        self.assertIn("RecentSoundEvents.add(this,found,percent)", monitor)
        self.assertNotIn('begin("sonidos")', monitor)
        self.assertNotIn("savedHistory.event(", monitor)
        self.assertNotIn("closingHistory.finish(", monitor)
        self.assertNotIn("new SavedFolderManager(this).copyOne(", monitor)

    def test_sound_screen_skips_file_history(self):
        screen = self.source("SenseActivity.java")
        self.assertIn("RecentSoundEvents.add(this,label,score)", screen)
        self.assertIn("if(screenMode==MODE_SOUND){\n   history=null;", screen)
        self.assertIn("history.begin(type);", screen)
        self.assertIn("showRecentSounds();", screen)

    def test_conversations_remain_eligible_for_files_and_backup(self):
        records = self.source("RecordsActivity.java")
        self.assertIn("ConversationRecordFilter.conversations(history.list())", records)
        self.assertIn("Mostrar archivos antiguos de sonidos", records)
        assist = self.source("AuroraAssistActivity.java")
        self.assertIn("SoundDigest.summarizeRecent(RecentSoundEvents.snapshot(this))", assist)
        self.assertIn("ConversationRecordFilter.conversations(new SenseHistory(this).list())", assist)

if __name__ == "__main__":
    unittest.main()
