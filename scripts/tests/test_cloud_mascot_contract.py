"""Static contracts: mascot reflects ONLY real app listening states."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
SENSE=(ROOT/"app/src/main/java/com/despertadorflash/SenseActivity.java").read_text(encoding="utf-8")
CLOUD=(ROOT/"app/src/main/java/com/despertadorflash/AuroraCloudMascotView.java").read_text(encoding="utf-8")
HOME=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
class MascotContractTest(unittest.TestCase):
 def test_portrait_view_and_home_preview(self):
  self.assertIn("new AuroraCloudMascotView(this)",SENSE)
  self.assertIn("new AuroraCloudMascotView(this)",HOME)
  self.assertIn("mascotPreview.setListening(false);",HOME)
 def test_audio_state_drives_mascot(self):
  self.assertIn("cloudMascot.setListening(true)",SENSE)
  self.assertIn("cloudMascot.setListening(false)",SENSE)
  self.assertIn("cloudMascot.setSpeaking(true)",SENSE)
  self.assertIn("cloudMascot.setSpeaking(false)",SENSE)
 def test_confirmed_sound_only(self):
  start=SENSE.index("private void recordSoundEvent(")
  stop=SENSE.index("private void refreshFeedbackSummary(",start)
  self.assertIn("cloudMascot.showConfirmedEvent(label)",SENSE[start:stop])
  self.assertIn("if(!listening)return;",CLOUD)
 def test_no_camera_audio_or_network_in_mascot(self):
  for bad in ["AudioRecord","SpeechRecognizer","CameraManager","HttpURLConnection","MediaRecorder"]:
   self.assertNotIn(bad,CLOUD)
 def test_anim_respects_device_settings(self):
  self.assertIn("PowerManager",CLOUD)
  self.assertIn("ANIMATOR_DURATION_SCALE",CLOUD)
  self.assertIn("onDetachedFromWindow()",CLOUD)
if __name__=="__main__":unittest.main()
