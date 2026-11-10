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
  String encoded=AuroraSettingsBackup.serialize(saved);
  Map<String,Object> decoded=AuroraSettingsBackup.parse(encoded);
  assertEquals(2,AuroraSettingsBackup.configuredAlarmCount(decoded));
  assertEquals(6,decoded.get("h0"));assertEquals(35,decoded.get("m0"));
  assertEquals(85,decoded.get("days0"));
  assertEquals(false,decoded.get("torch_alert"));
  assertEquals(true,decoded.get("monitor_alert_voices"));
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
  expectInvalid(valid.replace("AURORA_CONFIGURACION_V1","AURORA_CONFIGURACION_V2"));
  expectInvalid(valid.replace("m0=0","m0=90"));
  expectInvalid(valid.replace("days0=127","days0=200"));
  expectInvalid(valid.replace("on0=false","on0=yes"));
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
 private void expectInvalid(String text){
  try{AuroraSettingsBackup.parse(text);fail("Malformed backup accepted");}
  catch(IllegalArgumentException expected){}
 }
}
