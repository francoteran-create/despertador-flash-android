"""AURORA 4.21: character choice actually affects drawing/accessibility."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[2]/"app/src/main/java/com/despertadorflash"
H=(R/"AuroraHeroCloudView.java").read_text(encoding="utf-8")
M=(R/"MainActivity.java").read_text(encoding="utf-8")
class Hero421(unittest.TestCase):
 def test_alternative_is_distinct_and_restorable(self):
  self.assertIn('getInt(PREF_CHARACTER,0)==1?1:0',H)
  self.assertIn('if(character==0)',H)
  self.assertIn('Path swept=new Path()',H)
  self.assertIn('canvas.drawPath(swept,p)',H)
  self.assertIn('character==0?"A":"✦"',H)
  self.assertIn('getCharacter()',H)
 def test_character_choice_accessible(self):
  self.assertIn('setCharacterDescription()',H)
  self.assertIn('"Nube urbana":"Nube alternativa"',H)
  self.assertIn('setSingleChoiceItems(new String[]{"Nube urbana","Nube alternativa"}',M)
  self.assertIn('putInt(AuroraHeroCloudView.PREF_CHARACTER,which)',M)
  self.assertIn('Toast.makeText(this,which==0?',M)
 def test_actual_code_does_not_download_assets_or_use_microphone(self):
  for bad in ['AudioRecord','MediaRecorder','SpeechRecognizer','CameraManager','HttpURLConnection']:
   self.assertNotIn(bad,H)
  self.assertIn('power.isPowerSaveMode()',H)
  self.assertIn('removeCallbacks(frame)',H)
if __name__=="__main__":unittest.main()
