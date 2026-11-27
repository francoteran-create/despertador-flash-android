#!/usr/bin/env bash
set -euo pipefail
shopt -s nullglob
apks=(app/build/outputs/apk/debug/*.apk)
[[ ${#apks[@]} -eq 1 && ${apks[0]} == *arm64-v8a* ]]
apk=${apks[0]}
tools="${ANDROID_HOME:?Definí ANDROID_HOME}/build-tools/35.0.0"
badging=$("$tools/aapt" dump badging "$apk")
grep -F "package: name='com.despertadorflash.beta427'" <<< "$badging"
grep -F "application-label:'AURORA 4.37 BETA'" <<< "$badging"
grep -Fx "native-code: 'arm64-v8a'" <<< "$badging"
grep -Fx "sdkVersion:'24'" <<< "$badging"
manifest=$("$tools/aapt" dump xmltree "$apk" AndroidManifest.xml)
grep -F "com.despertadorflash.beta427.photos" <<< "$manifest"
permissions=$("$tools/aapt" dump permissions "$apk")
for permission in INTERNET RECORD_AUDIO VIBRATE CAMERA; do
  grep -F "android.permission.$permission" <<< "$permissions"
done
"$tools/apksigner" verify --verbose "$apk"
entries=$(unzip -Z1 "$apk")
grep -Fx "assets/yamnet.tflite" <<< "$entries"
sha256sum "$apk"
