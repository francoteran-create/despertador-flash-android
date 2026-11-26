"""AURORA VOZ 4.23: explicit, local TTS output and safe Android sharing."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"app/src/main/java/com/despertadorflash"
VOICE=(JAVA/"AuroraVoiceActivity.java").read_text(encoding="utf-8")
MAIN=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
MANIFEST=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
PATHS=(ROOT/"app/src/main/res/xml/aurora_photo_paths.xml").read_text(encoding="utf-8")

class AuroraVoiceContract(unittest.TestCase):
 def test_tts_is_local_and_has_clear_state(self):
  self.assertIn('new TextToSpeech(getApplicationContext(),this)',VOICE)
  self.assertIn('tts.synthesizeToFile(message,params,audio,id)',VOICE)
  self.assertIn('new Locale("es","AR")',VOICE)
  self.assertIn('TextToSpeech.getMaxSpeechInputLength()',VOICE)
  self.assertIn('onDone(String utteranceId)',VOICE)
  self.assertIn('onError(String utteranceId)',VOICE)
  self.assertNotIn('HttpURLConnection',VOICE)
  self.assertNotIn('api.openai.com',VOICE)

 def test_preview_only_after_file_exists(self):
  self.assertIn('audio!=null&&audio.exists()&&audio.length()>0',VOICE)
  self.assertIn('player.prepare();player.start()',VOICE)
  self.assertIn('void clearOldAudio()',VOICE)

 def test_user_chooses_destination_and_confirms(self):
  self.assertIn('Intent.ACTION_SEND',VOICE)
  self.assertIn('Intent.EXTRA_STREAM,uri',VOICE)
  self.assertIn('FLAG_GRANT_READ_URI_PERMISSION',VOICE)
  self.assertIn('send.setPackage(packageName)',VOICE)
  self.assertIn('com.whatsapp',VOICE)
  self.assertIn('Intent.createChooser(send',VOICE)
  self.assertNotIn('SmsManager',VOICE)
  self.assertNotIn('sendBroadcast',VOICE)

 def test_provider_only_shares_app_cache_and_declared_activity(self):
  self.assertIn('FileProvider.getUriForFile',VOICE)
  self.assertIn('new File(getCacheDir(),"aurora_voice")',VOICE)
  self.assertIn('<cache-path name="voice_messages" path="aurora_voice/"/>',PATHS)
  self.assertIn('android:name=".AuroraVoiceActivity"',MANIFEST)
  self.assertIn('android:exported="false"',MANIFEST)

 def test_existing_communication_route(self):
  self.assertIn('AURORA VOZ · TEXTO A AUDIO Y WHATSAPP',MAIN)
  self.assertIn('startActivity(new Intent(this,AuroraVoiceActivity.class))',MAIN)

if __name__=="__main__":unittest.main()
