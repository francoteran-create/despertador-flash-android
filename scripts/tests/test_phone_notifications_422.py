"""AURORA 4.22: foreground phone alerts must not require a paired watch."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
J=ROOT/"app/src/main/java/com/despertadorflash"
ALERT=(J/"AuroraPhoneAlerts.java").read_text(encoding="utf-8")
SENSE=(J/"SenseActivity.java").read_text(encoding="utf-8")
MAIN=(J/"MainActivity.java").read_text(encoding="utf-8")
BACKGROUND=(J/"AuroraSoundMonitorService.java").read_text(encoding="utf-8")
class NotificationFlowTest(unittest.TestCase):
 def test_foreground_posts_independently_of_watch(self):
  start=SENSE.index("private void recordSoundEvent(")
  end=SENSE.index("private void refreshFeedbackSummary(",start)
  area=SENSE[start:end]
  self.assertIn('AuroraPhoneAlerts.post(this,label,false)',area)
  self.assertNotIn('AuroraWatchAlert.notifyForeground(this,label)',area)
  self.assertIn('if(!preview&&AuroraWatchAlert.isEnabled(c))',ALERT)
 def test_test_button_has_no_audio_or_fake_history(self):
  self.assertIn('PROBAR NOTIFICACIÓN EN TELÉFONO',SENSE)
  self.assertIn('AuroraPhoneAlerts.post(this,"POSIBLE TIMBRE DE PUERTA",true)',SENSE)
  self.assertIn('AuroraPhoneAlerts.post(this,"POSIBLE TIMBRE DE PUERTA",true)',MAIN)
  self.assertNotIn('RecentSoundEvents.add',ALERT)
  self.assertNotIn('AudioRecord',ALERT)
 def test_channels_permission_and_block_diagnosis(self):
  for text in ('Manifest.permission.POST_NOTIFICATIONS','areNotificationsEnabled()',
    'getImportance()==NotificationManager.IMPORTANCE_NONE',
    'NotificationManager.IMPORTANCE_HIGH','nm.notify(tag,id,b.build())',
    'phone_alert_last_result','ENVIADO A ANDROID'):
   self.assertIn(text,ALERT)
 def test_background_is_still_always_monitored_and_independent(self):
  self.assertIn('SoundAlertPreferences.channel(group)',BACKGROUND)
  self.assertIn('nm.notify(SoundAlertPreferences.notificationTag(group,found)',BACKGROUND)
 def test_sends_only_confirmed_events_unless_explicitly_preview(self):
  self.assertIn('if(!preview&&!SoundAlertPreferences.isEnabled(c,label))',ALERT)
  self.assertIn('recordSoundEvent(label,score)',SENSE)
if __name__=="__main__":unittest.main()
