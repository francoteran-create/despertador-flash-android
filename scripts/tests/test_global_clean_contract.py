"""Cold and icon relaunch wiring, secondary routes, and presentation-only changes."""
from pathlib import Path
import unittest
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[2] / "app/src/main"
ANDROID = "{http://schemas.android.com/apk/res/android}"
JAVA = ROOT / "java/com/despertadorflash"


class GlobalCleanContract(unittest.TestCase):
    def test_launcher_routes_through_entry_without_touching_onboarding(self):
        manifest = ElementTree.parse(ROOT / "AndroidManifest.xml")
        launchers = [
            activity.attrib[ANDROID + "name"]
            for activity in manifest.findall("./application/activity")
            if any(
                category.attrib[ANDROID + "name"] == "android.intent.category.LAUNCHER"
                for category in activity.findall("./intent-filter/category")
            )
        ]
        self.assertEqual(launchers, [".AuroraEntryActivity"])
        entry = (JAVA / "AuroraEntryActivity.kt").read_text()
        self.assertIn("delay(ENTRY_MS)", entry)
        self.assertIn("MainActivity.ACTION_OPEN_HOME", entry)
        self.assertIn("FLAG_ACTIVITY_CLEAR_TOP", entry)
        self.assertNotIn("getSharedPreferences", entry)

    def test_alarms_and_timers_keep_direct_intents(self):
        for receiver in ("AlarmReceiver.java", "TimerReceiver.java"):
            with self.subTest(receiver=receiver):
                code = (JAVA / receiver).read_text()
                self.assertIn("MainActivity.class", code)
                self.assertNotIn("AuroraEntryActivity", code)

    def test_conversation_uses_the_saved_avatar_and_keeps_secondary_actions(self):
        sense = (JAVA / "SenseActivity.java").read_text()
        stage = (JAVA / "ConversationStage.kt").read_text()
        self.assertIn("NeonIdentity.menuPortrait(this,dp(50))", sense)
        self.assertIn("NeonAvatar(portrait, name)", stage)
        self.assertNotIn("Cloud", stage)
        for action in ("languages", "translation", "history", "records", "diagnostic"):
            self.assertIn(f'"{action}"', stage)
            self.assertIn(f'case "{action}"', sense)
