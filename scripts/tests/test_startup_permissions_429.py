"""Regression contracts for first-start permissions and alarm LED."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[2]/"app/src/main/java/com/despertadorflash"
M=(R/"MainActivity.java").read_text(encoding="utf-8")
S=(R/"AlarmEffectsService.java").read_text(encoding="utf-8")
class StartupPermissionTest(unittest.TestCase):
 def test_first_start_and_return(self):
  self.assertIn('!prefs.getBoolean("permissions_setup_429",false)',M)
  self.assertIn("showPermissionSetup(true)",M)
  self.assertIn("requestPermissions(missing.toArray(new String[0]),STARTUP_PERMISSIONS)",M)
  self.assertIn("if(requestCode==STARTUP_PERMISSIONS)",M)
  self.assertIn("Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM",M)
  self.assertIn("Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT",M)
 def test_no_capture_in_permission_wizard(self):
  body=M[M.index("void showPermissionSetup("):M.index("void startTorchAlert()")]
  for forbidden in ("startRecording(","startListening(","ACCESS_COARSE_LOCATION","torch.start()"):
   self.assertNotIn(forbidden,body)
  for permission in ("Manifest.permission.CAMERA","Manifest.permission.RECORD_AUDIO","Manifest.permission.POST_NOTIFICATIONS"):
   self.assertIn(permission,body)
 def test_per_alarm_flash_setting_and_report(self):
  self.assertIn('p.getBoolean("alarm_flash"+id,p.getBoolean("torch_alert",true))',S)
  self.assertNotIn('p.getBoolean("torch_alert",true)&&',S)
  self.assertIn('putString("last_alarm_flash"+id',S)
  self.assertIn('getString("last_alarm_flash"+id',M)
  self.assertIn('PROBAR FLASH LED · 5 SEGUNDOS',M)
if __name__=="__main__":unittest.main()
