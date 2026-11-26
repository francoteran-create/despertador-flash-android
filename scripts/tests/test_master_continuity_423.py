"""The AURORA MASTER embeds a complete cross-chat continuation instruction."""
from pathlib import Path
import unittest
source=(Path(__file__).resolve().parents[2]/"scripts/build_delivery.py").read_text(encoding="utf-8")
class MasterContinuity(unittest.TestCase):
 def test_instructions_present_in_generated_pdf_source(self):
  for fragment in ("COMO CONTINUAR EN OTRO CHAT","francoteran-create/despertador-flash-android",
   "aurora-sense-precision-2026-11-10","Rama estable main",
   "unico ZIP","APK ARM64","AURORA VOZ","Modo Mostrador","SOS 4.26",
   "Conservar la estética urbana neón aprobada","+commit","+version"):
   self.assertIn(fragment,source)
 def test_instructions_created_before_pdf_build(self):
  self.assertLess(source.index('head("7 | COMO CONTINUAR'),source.index("def footer(canvas,pdf):"))
if __name__=="__main__":unittest.main()
