"""SOS 4.26: deliberate outgoing messages and opt-in approximate location."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
SOS=(ROOT/"app/src/main/java/com/despertadorflash/AuroraSosActivity.java").read_text(encoding="utf-8")
MAIN=(ROOT/"app/src/main/java/com/despertadorflash/MainActivity.java").read_text(encoding="utf-8")
MANIFEST=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
class SosContract(unittest.TestCase):
 def test_entrypoint_and_permissions(self):
  self.assertIn('new Intent(this,AuroraSosActivity.class)',MAIN)
  self.assertIn('android:name=".AuroraSosActivity" android:exported="false"',MANIFEST)
  self.assertIn('android.permission.ACCESS_COARSE_LOCATION',MANIFEST)
 def test_no_automatic_send(self):
  self.assertIn('new AlertDialog.Builder(this)',SOS)
  self.assertIn('ABRIR PARA ENVIAR',SOS)
  self.assertIn('Intent.ACTION_SENDTO',SOS)
  self.assertIn('Uri.parse("smsto:"+Uri.encode(number))',SOS)
  self.assertIn('sms_body',SOS)
  self.assertIn('Intent.ACTION_SEND',SOS)
  self.assertNotIn('SmsManager',SOS)
  self.assertNotIn('sendTextMessage(',SOS)
 def test_location_is_opt_in_and_recent(self):
  self.assertIn('withLocation.setChecked(false)',SOS)
  self.assertIn('if(!withLocation.isChecked())return base;',SOS)
  self.assertIn('getLastKnownLocation(provider)',SOS)
  self.assertIn('age>5L*60*1000000000L',SOS)
  self.assertIn('Se enviará el mensaje SIN ubicación.',SOS)
 def test_contact_local_and_manual(self):
  self.assertIn('getSharedPreferences(PREF,MODE_PRIVATE)',SOS)
  self.assertIn('GUARDAR ESTE CONTACTO EN EL TELÉFONO',SOS)
  self.assertIn('No llama a emergencias ni envía nada automáticamente.',SOS)
if __name__=="__main__":unittest.main()
