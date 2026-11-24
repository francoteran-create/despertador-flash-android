"""AURORA 4.20: visually consistent four-corner HERO with two avatars."""
from pathlib import Path
import unittest
root=Path(__file__).resolve().parents[2]
main=(root/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text()
hero=(root/"app/src/main/java/com/despertadorflash/AuroraHeroCloudView.java").read_text()
cloud=(root/"app/src/main/java/com/despertadorflash/AuroraHeroCloudButton.java").read_text()
class Hero420(unittest.TestCase):
 def test_exactly_two_shortcuts_at_top_and_bottom(self):
  part=main[main.index("void renderMinimalHero(){"):main.index("LinearLayout homeTile(")]
  top=part[part.index("LinearLayout upper="):part.index("AuroraHeroCloudView centerCloud=")]
  bottom=part[part.index("LinearLayout lower="):]
  self.assertEqual(top.count('addHeroShortcut(upper,'),2)
  self.assertEqual(bottom.count('addHeroShortcut(lower,'),2)
  self.assertIn('"SONIDOS"',top)
  self.assertIn('"CONVERSACIÓN"',top)
  self.assertIn('"ALARMA"',bottom)
  self.assertIn('"COMUNICAR"',bottom)
 def test_mascot_two_characters_saved_only_on_selection(self):
  self.assertIn('PREF_CHARACTER="hero_character"',hero)
  self.assertIn('setCharacter(which)',main)
  self.assertIn('putInt(AuroraHeroCloudView.PREF_CHARACTER,which)',main)
  self.assertIn("if(character==0)",hero)
  self.assertIn("}else{",hero)
 def test_low_cost_vector_only(self):
  for token in ['Canvas','PowerManager','ANIMATOR_DURATION_SCALE','onDetachedFromWindow']:
   self.assertIn(token,hero)
  for forbidden in ["AudioRecord","CameraManager","HttpURLConnection"]:
   self.assertNotIn(forbidden,hero)
 def test_buttons_have_icons_and_accessibility(self):
  self.assertIn('setContentDescription("Abrir "+title)',cloud)
  self.assertIn('title.equals("SONIDOS")',cloud)
  self.assertIn('title.equals("CONVERSACIÓN")',cloud)
  self.assertIn('title.equals("ALARMA")',cloud)
  self.assertIn('VIOLET',cloud)
if __name__=="__main__":unittest.main()
