"""AURORA: the optional flash must not depend on notification delivery."""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SERVICE=(ROOT/"app/src/main/java/com/despertadorflash/AuroraSoundMonitorService.java").read_text(encoding="utf-8")
FEEDBACK=(ROOT/"app/src/main/java/com/despertadorflash/SoundEventFeedback.java").read_text(encoding="utf-8")
HOME=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")

class SoundFlashDeliveryContractTest(unittest.TestCase):
 def test_led_attempt_precedes_notification_permission_check(self):
  start=SERVICE.index("private synchronized void consider(")
  end=SERVICE.index("private synchronized void shutdown()",start)
  body=SERVICE[start:end]
  feedback=body.index("soundFeedback.onConfirmed(found,false);")
  notification_permission=body.index("checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)")
  notify=body.index("nm.notify(")
  self.assertLess(feedback,notification_permission)
  self.assertLess(feedback,notify)
  self.assertEqual(body.count("soundFeedback.onConfirmed(found,false);"),1)

 def test_flash_failure_is_visible_and_does_not_reserve_cooldown(self):
  self.assertIn("policy.flashStartFailed(now);",FEEDBACK)
  self.assertIn("recordFlashStatus(\"LED no disponible:",FEEDBACK)
  self.assertIn("monitor_sound_flash_last_status",FEEDBACK)
  self.assertIn("monitor_sound_flash_last_status",HOME)

 def test_flash_remains_opt_in_and_permission_gated(self):
  self.assertIn('getBoolean("monitor_sound_flash",false)',FEEDBACK)
  self.assertIn("checkSelfPermission(Manifest.permission.CAMERA)",FEEDBACK)
  self.assertIn("SoundFeedbackPolicy.FLASH_DURATION_MS",FEEDBACK)
  self.assertIn("if(closed||label==null||!SoundAlertPreferences.isEnabled(context,label))return;",FEEDBACK)

if __name__=="__main__":
 unittest.main()
