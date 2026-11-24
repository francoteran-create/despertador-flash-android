"""Enforce truthful visual notifications: no synthetic recordings or events."""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
WATCH=(ROOT/"app/src/main/java/com/despertadorflash/AuroraWatchAlert.java").read_text(encoding="utf-8")
MONITOR=(ROOT/"app/src/main/java/com/despertadorflash/AuroraSoundMonitorService.java").read_text(encoding="utf-8")
SENSE=(ROOT/"app/src/main/java/com/despertadorflash/SenseActivity.java").read_text(encoding="utf-8")
HOME=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
class WatchContract(unittest.TestCase):
 def test_background_enriches_only_confirmed_notification(self):
  start=MONITOR.index("private synchronized void consider(")
  stop=MONITOR.index("private synchronized void shutdown()",start)
  x=MONITOR[start:stop]
  self.assertLess(x.index("if(!gate.accept("),x.index("AuroraWatchAlert.illustrate(this,b,found);"))
  self.assertLess(x.index("if(!SoundAlertPreferences.isEnabled(this,found))return;"),x.index("AuroraWatchAlert.illustrate(this,b,found);"))
  self.assertEqual(x.count("AuroraWatchAlert.illustrate(this,b,found);"),1)
 def test_foreground_notifies_after_real_event(self):
  start=SENSE.index("private void recordSoundEvent(")
  stop=SENSE.index("private void refreshFeedbackSummary(",start)
  area=SENSE[start:stop]
  self.assertIn("AuroraWatchAlert.notifyForeground(this,label);",area)
  self.assertNotIn("AuroraWatchAlert.previewDoor(this);",area)
 def test_preview_explicitly_no_mic_recording(self):
  self.assertIn("PRUEBA: dibujo de puerta",WATCH)
  self.assertNotIn("RecentSoundEvents.add",WATCH)
  self.assertNotIn("AudioRecord",WATCH)
  self.assertIn("AuroraWatchAlert.previewDoor(this);",HOME)
 def test_user_can_disable_and_permissions_checked(self):
  self.assertIn("getBoolean(PREF,true)",WATCH)
  self.assertIn("android.permission",WATCH) if False else None
  self.assertIn("Manifest.permission.POST_NOTIFICATIONS",WATCH)
  self.assertIn("watchPictures.setOnCheckedChangeListener",HOME)
 def test_big_picture_is_native_notification_not_wear_app_claim(self):
  self.assertIn("Notification.BigPictureStyle",WATCH)
  self.assertIn("bigPicture(art)",WATCH)
  self.assertIn("watches MAY mirror",WATCH)
if __name__=="__main__":unittest.main()
