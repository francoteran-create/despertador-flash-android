package com.despertadorflash;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class AuroraSettingsBackupTest {
 @Test public void backupsRoundTripAllFiveAlarmsAndNotificationSettings(){
  Map<String,Object> saved=new LinkedHashMap<>();
  saved.put("created0",true);saved.put("on0",true);
  saved.put("h0",6);saved.put("m0",35);saved.put("days0",85);
  saved.put("created4",true);saved.put("on4",false);
  saved.put("monitor_alert_voices",true);saved.put("monitor_sensitivity",2);
  saved.put("torch_alert",false);
  saved.put("alarm_flash0",true);saved.put("alarm_vibration0",false);saved.put("alarm_sound0",true);
  saved.put("alarm_tone0",9);saved.put("alarm_volume0",42);saved.put("alarm_duration0",180);
  saved.put("monitor_sound_flash",true);saved.put("monitor_sound_vibration",false);
  String encoded=AuroraSettingsBackup.serialize(saved);
  Map<String,Object> decoded=AuroraSettingsBackup.parse(encoded);
  assertEquals(2,AuroraSettingsBackup.configuredAlarmCount(decoded));
  assertEquals(6,decoded.get("h0"));assertEquals(35,decoded.get("m0"));
  assertEquals(85,decoded.get("days0"));
  assertEquals(false,decoded.get("torch_alert"));
  assertEquals(true,decoded.get("monitor_sound_flash"));
  assertEquals(false,decoded.get("monitor_sound_vibration"));
  assertEquals(true,decoded.get("monitor_alert_voices"));
  assertEquals(true,decoded.get("alarm_flash0"));assertEquals(false,decoded.get("alarm_flash4"));
  assertEquals(false,decoded.get("alarm_vibration0"));assertEquals(true,decoded.get("alarm_sound0"));
  assertEquals(9,decoded.get("alarm_tone0"));assertEquals(42,decoded.get("alarm_volume0"));assertEquals(180,decoded.get("alarm_duration0"));
  assertEquals(encoded,AuroraSettingsBackup.serialize(decoded));
 }
 @Test public void doesNotExportSensitiveOrEphemeralValues(){
  Map<String,Object> values=new LinkedHashMap<>();
  values.put("name","Nombre sensible");values.put("contact","012345678");
  values.put("sense_last_transcript","frase privada");values.put("timer_end",999999L);
  values.put("monitor_active",true);
  String text=AuroraSettingsBackup.serialize(values);
  assertFalse(text.contains("Nombre sensible"));
  assertFalse(text.contains("frase privada"));
  assertFalse(text.contains("timer_end"));
  assertFalse(text.contains("contact"));
  assertFalse(text.contains("monitor_active"));
 }
 @Test public void catchesChangedOrTruncatedContent(){
  String valid=AuroraSettingsBackup.serialize(new LinkedHashMap<>());
  expectInvalid(valid.replace("h0=7","h0=8"));
  expectInvalid(valid.substring(0,valid.length()-16));
  expectInvalid(valid.replace("AURORA_CONFIGURACION_V3","AURORA_CONFIGURACION_V4"));
  expectInvalid(valid.replace("m0=0","m0=90"));
  expectInvalid(valid.replace("days0=127","days0=200"));
  expectInvalid(valid.replace("on0=false","on0=yes"));
 }
 @Test public void importsValidV1BackupsWithoutTurningOnNewFlash(){
  String current=AuroraSettingsBackup.serialize(new LinkedHashMap<>());
  String legacyBody=current.substring(0,current.indexOf("SHA256="))
    .replaceAll("(?m)^alarm_(flash|vibration|sound|tone|volume|duration)[0-4]=.*\\n","")
    .replace("AURORA_CONFIGURACION_V3","AURORA_CONFIGURACION_V1")
    .replace("monitor_sound_flash=false\n","")
    .replace("monitor_sound_vibration=true\n","");
  String hash;
  try{
   byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(
    legacyBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder();
   for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
   hash=hex.toString();
  }catch(Exception ex){throw new AssertionError(ex);}
  String legacy=legacyBody+"SHA256="+hash+"\n";
  Map<String,Object> imported=AuroraSettingsBackup.parse(legacy);
  assertEquals(false,imported.get("monitor_sound_flash"));
  assertEquals(true,imported.get("monitor_sound_vibration"));
  assertTrue(AuroraSettingsBackup.serialize(imported).startsWith("AURORA_CONFIGURACION_V3\n"));
  expectInvalid(legacy.replace("h0=7","h0=8"));
 }
 @Test public void refusesOutOfRangeValuesBeforeExport(){
  Map<String,Object> bad=new LinkedHashMap<>();
  bad.put("h0",24);
  try{AuroraSettingsBackup.serialize(bad);fail("Invalid hours accepted");}
  catch(IllegalArgumentException expected){}
 }
 @Test public void refusesMissingRequiredFieldsAndTooLargeFiles(){
  String valid=AuroraSettingsBackup.serialize(new LinkedHashMap<>());
  expectInvalid(valid.replace("created0=false\n",""));
  expectInvalid(valid+"x".repeat(9000));
 }
 @Test public void importsV2WithGlobalLedDisabled(){
  Map<String,Object> values=new LinkedHashMap<>();values.put("torch_alert",false);
  String current=AuroraSettingsBackup.serialize(values);
  String body=current.substring(0,current.indexOf("SHA256="))
   .replaceAll("(?m)^alarm_(flash|vibration|sound|tone|volume|duration)[0-4]=.*\\n","")
   .replace("AURORA_CONFIGURACION_V3","AURORA_CONFIGURACION_V2");
  try{
   byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder hash=new StringBuilder();for(byte v:digest)hash.append(String.format(java.util.Locale.ROOT,"%02x",v&0xff));
   Map<String,Object> migrated=AuroraSettingsBackup.parse(body+"SHA256="+hash+"\n");
   for(int i=0;i<5;i++){assertEquals(false,migrated.get("alarm_flash"+i));assertEquals(false,migrated.get("alarm_sound"+i));assertEquals(60,migrated.get("alarm_duration"+i));}
  }catch(java.security.NoSuchAlgorithmException ex){throw new AssertionError(ex);}
 }
 @Test public void rejectsInvalidAlarmEffects(){
  String[] keys={"alarm_tone0","alarm_volume2","alarm_duration4"};
  int[] invalid={10,101,45};
  for(int i=0;i<keys.length;i++){
   Map<String,Object> values=new LinkedHashMap<>();values.put(keys[i],invalid[i]);
   try{AuroraSettingsBackup.serialize(values);fail("Invalid setting accepted");}catch(IllegalArgumentException expected){}
  }
 }
 private void expectInvalid(String text){
  try{AuroraSettingsBackup.parse(text);fail("Malformed backup accepted");}
  catch(IllegalArgumentException expected){}
 }
}
