"""AURORA 4.24: counter mode is visible, two-sided, consent-based, offline UI."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"app/src/main/java/com/despertadorflash"
COUNTER=(JAVA/"AuroraCounterActivity.java").read_text(encoding="utf-8")
MAIN=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
MANIFEST=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
class CounterMode(unittest.TestCase):
 def test_communication_entry_and_lifecycle(self):
  self.assertIn('MODO MOSTRADOR · CONVERSAR CARA A CARA',MAIN)
  self.assertIn('new Intent(this,AuroraCounterActivity.class)',MAIN)
  self.assertIn('android:name=".AuroraCounterActivity"',MANIFEST)
  self.assertIn('android:exported="false" android:windowSoftInputMode="adjustResize"',MANIFEST)
 def test_two_facing_panels_and_real_typed_reply(self):
  self.assertIn('staff.setRotation(180f)',COUNTER)
  self.assertIn('screen.addView(staff,new LinearLayout.LayoutParams(-1,0,1f))',COUNTER)
  self.assertIn('screen.addView(user,new LinearLayout.LayoutParams(-1,0,1f))',COUNTER)
  self.assertIn('typed.getText().toString().trim()',COUNTER)
  self.assertIn('facingMessage.setText(message)',COUNTER)
  self.assertIn('replyTranscript.setText(recognized)',COUNTER)
  self.assertIn('user.addView(listen',COUNTER)
  self.assertNotIn('staff.addView(listen',COUNTER)
  self.assertNotIn('facingMessage.setText(recognized)',COUNTER)
  self.assertNotIn('replyTranscript.setText(message)',COUNTER)
  self.assertIn('TextToSpeech.QUEUE_FLUSH',COUNTER)
 def test_microphone_only_on_request(self):
  self.assertIn('listen.setOnClickListener',COUNTER)
  self.assertIn('if(listening)stopListening();else beginListening()',COUNTER)
  self.assertIn('Manifest.permission.RECORD_AUDIO',COUNTER)
  self.assertIn('SpeechRecognizer.createSpeechRecognizer(this)',COUNTER)
  self.assertIn('recognizer.startListening(intent)',COUNTER)
  self.assertIn('public void onResults(Bundle result){receiveText(result);stopListening()',COUNTER)
  self.assertIn('public void onPartialResults(Bundle result){receiveText(result);}',COUNTER)
 def test_timeout_and_privacy(self):
  self.assertIn('main.postDelayed(timeout,30000L)',COUNTER)
  self.assertIn('getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)',COUNTER)
  self.assertIn('old.cancel();old.destroy()',COUNTER)
  self.assertNotIn('RecentSoundEvents.add',COUNTER)
  self.assertNotIn('HttpURLConnection',COUNTER)
  self.assertNotIn('FileOutputStream',COUNTER)
if __name__=="__main__":unittest.main()
