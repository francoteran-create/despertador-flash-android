"""AURORA HERO: the front page has only four cloud navigation choices."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
MAIN=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
SENSE=(ROOT/"app/src/main/java/com/despertadorflash/SenseActivity.java").read_text(encoding="utf-8")
HERO=(ROOT/"app/src/main/java/com/despertadorflash/AuroraHeroCloudView.java").read_text(encoding="utf-8")
BUTTON=(ROOT/"app/src/main/java/com/despertadorflash/AuroraHeroCloudButton.java").read_text(encoding="utf-8")

class HeroNavigationTest(unittest.TestCase):
 def test_exact_four_cloud_nav_targets(self):
  area=MAIN[MAIN.index("void renderMinimalHero(){"):MAIN.index("LinearLayout homeTile(")]
  self.assertEqual(area.count("addHeroShortcut(upper,"),2)
  self.assertEqual(area.count("addHeroShortcut(lower,"),2)
  for title in ('"SONIDOS"','"CONVERSACIÓN"','"ALARMA"','"COMUNICAR"'):
   self.assertIn(title,area)
  self.assertIn("openSense(SenseActivity.MODE_SOUND)",area)
  self.assertIn("openSense(SenseActivity.MODE_SPEECH)",area)
  self.assertIn("alarmsPage=true;render()",area)
  self.assertIn("communicationPage=true;render()",area)
 def test_black_background_on_main_and_sense(self):
  self.assertIn("hero.setBackgroundColor(Color.BLACK)",MAIN)
  self.assertIn("root.setBackgroundColor(Color.BLACK)",MAIN)
  self.assertIn("root.setBackgroundColor(Color.BLACK)",SENSE)
  self.assertIn("new AuroraHeroCloudView(this)",MAIN)
 def test_no_old_home_panels_in_hero(self):
  area=MAIN[MAIN.index("void renderMinimalHero(){"):MAIN.index("LinearLayout homeTile(")]
  for old in ["PALABRAS EN VIVO","EXPLORAR  /  ACCESOS DIRECTOS","new AuroraVisualEngineView","homeTile("]:
   self.assertNotIn(old,area)
 def test_clouds_are_clickable_accessible_and_nonlistening(self):
  self.assertIn("setContentDescription(\"Abrir \"+title)",BUTTON)
  self.assertIn("setFocusable(true)",BUTTON)
  self.assertIn("setOnClickListener(v->navigate.run())",MAIN)
  self.assertIn('PREF_CHARACTER',MAIN)
  self.assertIn('setSingleChoiceItems',MAIN)
  self.assertIn("Tocá para cambiar de personaje",HERO)
  for bad in ["AudioRecord","SpeechRecognizer","CameraManager","HttpURLConnection"]:
   self.assertNotIn(bad,HERO)
 def test_motion_can_be_reduced(self):
  self.assertIn("ANIMATOR_DURATION_SCALE",HERO)
  self.assertIn("power.isPowerSaveMode()",HERO)

if __name__=="__main__":
 unittest.main()
