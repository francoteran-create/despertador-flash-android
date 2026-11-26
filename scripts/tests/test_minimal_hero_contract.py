"""AURORA HERO: the front page has only four cloud navigation choices."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
MAIN=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
SENSE=(ROOT/"app/src/main/java/com/despertadorflash/SenseActivity.java").read_text(encoding="utf-8")
HERO=(ROOT/"app/src/main/java/com/despertadorflash/AuroraHeroCloudView.java").read_text(encoding="utf-8")
BUTTON=(ROOT/"app/src/main/java/com/despertadorflash/AuroraHeroCloudButton.java").read_text(encoding="utf-8")

class HeroNavigationTest(unittest.TestCase):
 def test_neon_navigation_and_real_targets(self):
  area=MAIN[MAIN.index("void renderMinimalHero(){"):MAIN.index("LinearLayout neonTile(")]
  self.assertEqual(area.count("neonPair(hero,"),5)
  for title in ('"DETECCIÓN DE SONIDOS"','"TRANSCRIPCIÓN"','"ALARMA"','"COMUNICAR"','"TEMPORIZADOR"','"SOS"','"REGISTROS"','"SENTIR LA MÚSICA"','"AURORA VOZ"','"MOSTRADOR"'):
   self.assertIn(title,area)
  for target in ("openSense(SenseActivity.MODE_SOUND)","openSense(SenseActivity.MODE_SPEECH)","alarmsPage=true;render()","communicationPage=true;render()","timerPage=true;render()","AuroraSosActivity.class","RecordsActivity.class","AuroraMusicActivity.class","AuroraVoiceActivity.class","AuroraCounterActivity.class"):
   self.assertIn(target,area)
 def test_dark_fallback_and_static_mural(self):
  self.assertIn("screen.setBackgroundColor(Color.BLACK)",MAIN)
  self.assertIn('getAssets().open("aurora_mural.jpg.b64")',MAIN)
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
  self.assertIn("tile.setContentDescription(label)",MAIN)
  self.assertIn("tile.setFocusable(true)",MAIN)
  self.assertIn("info.setClassName(Button.class.getName())",MAIN)
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
