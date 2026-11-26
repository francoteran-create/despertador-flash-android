# AURORA 4.4 — diagnóstico de reconocimiento y traducción

**Fecha:** 2026-11-11. Beta de pruebas, no versión comercial. Responde a
los fallos persistentes observados por la persona que prueba: predominio
de un ruido fuerte, errores y cortes de escucha, traducción sin resultado.

## Hallazgo verificable en el código 4.3

`AndroidManifest.xml` NO declaraba `android.permission.INTERNET`.
La traducción ML Kit Android descarga modelos dinámicos la primera vez
(`downloadModelIfNeeded`), por lo que esa omisión podía causar el fallo
de descarga/traducción. También hay proveedores Android SpeechRecognizer
que necesitan conectividad. Agregados `INTERNET` y
`ACCESS_NETWORK_STATE`; GitHub CI revisa ambas APK con `aapt`.

**Esto no prueba que todos los errores del S9+ tengan esa causa.**

## Mejoras funcionales de 4.4 para experimentar en teléfono real

- `SoundAudioCapturePolicy`: bloques de 8.000 muestras a 16 kHz
  (~500 ms) en vez de 16.000 (~1 s), tanto en primer plano como en
  servicio. El AudioClassifier en modo AUDIO_STREAM realiza buffering;
  aún es necesario validar si mejora la detección de eventos breves
  sin añadir consumo o errores al Galaxy S9+.
- `SoundLiveWindow`: ante un evento contemporáneo breve, presenta
  timbre/golpes/voz antes que clases de fondo como música/viento/agua.
  No equivale a reconocer un sonido que YAMNet nunca predijo.
- `SoundCaptureTelemetry`: cuenta muestras entregadas, respuestas IA,
  edad del último resultado, reconexiones y fallo de micrófono/modelo.
  Se ve un resumen en SONIDOS y hay botón
  **COPIAR DIAGNÓSTICO (SIN AUDIO)**. También registra último código
  de error de voz, cantidad de resultados parciales/finales, tipo
  de fallo de ML Kit, conectividad, modelo de teléfono, versión
  de aplicación y permisos. El informe no incluye audio, frases
  ni eventos detectados.
- TRADUCCIÓN permite escribir una frase opcional para comprobar ML Kit
  sin depender del servicio de voz. El botón desplaza la pantalla
  hacia el resultado para que se vea en un teléfono pequeño; avisa
  si la descarga o traducción sigue pendiente tras 30 segundos.
- Se limita el historial **visual** a 90 elementos a la vez para
  reducir la carga de UI en sesiones largas. El registro en disco
  tiene su propia política independiente de eventos.
- La 4.4 usa el identificador `com.despertadorflash.beta44`:
  puede instalarse en paralelo a AURORA original, 4.2 y 4.3.
  Los historiales y alarmas NO se comparten entre apps.

## Distinción importante: dos mecanismos diferentes

**SONIDOS:** YAMNet usa el micrófono y ejecuta clasificación local de
eventos acústicos. Muestra hipótesis, NO una garantía de que se
reconocerá cada sonido solapado ni avisos de emergencia certificados.

**CONVERSACIÓN:** usa `android.speech.SpeechRecognizer`, que según
la documentación de Android NO fue diseñado para reconocimiento
continuo ilimitado. Puede interrumpirse entre frases, aplicar
límites del servicio y transmitir audio a servidores del proveedor
según la configuración del teléfono. AURORA no guarda el audio,
pero el proveedor de voz Android puede procesarlo remotamente.
Hacer transcripción verdaderamente continua requeriría evaluar un
motor de streaming dedicado y medir consumo, calidad y privacidad.

**TRADUCCIÓN:** ML Kit traduce texto (no audio). El primer modelo de
idioma necesita una descarga online; una vez disponible puede
funcionar sin conexión. Si no hubo transcripción, comprobar con
una frase escrita permite separar fallo de voz de fallo del traductor.

## Prueba corta que permite identificar la falla

1. Comprobar en aplicaciones que el icono se llama **AURORA 4.4 BETA**.
   No desinstalar las versiones anteriores ni permitir que dos
   aplicaciones capturen el micrófono simultáneamente.
2. Conectarse a una red Wi-Fi funcional. Abrir CONVERSACIÓN, seleccionar
   español, elegir inglés y escribir en el campo de prueba
   `Buenos días, ¿cómo estás?`. Tocar TRADUCIR. Si funciona por
   teclado pero no al hablar, el problema está antes de ML Kit.
3. Si no traduce por teclado, copiar el diagnóstico. Debe indicar
   conexión validada y, si aparece, la categoría de error de descarga
   o traducción.
4. En SONIDOS, probar música, luego timbre/golpe, después silencio.
   Confirmar que la cantidad de **respuestas IA** aumenta. Un
   contador inmóvil con muestras que suben indica problema del
   clasificador, mientras que muestras inmóviles indican captura.
5. Si hay cortes, copiar el diagnóstico mientras está activa la
   escucha o inmediatamente después. Registrar si aparece
   `RECONECTANDO`, `ESCUCHA DETENIDA` o un código de error de voz.

## Evidencia y límites

Los tests JUnit/CI y la firma del ZIP prueban que compila y que pasan
regresiones concretas. No se ha medido la tasa de omisiones o falsas
alarmas, latencia, calidad de voz ni consumo con micrófono real en el
Galaxy S9+. Una sola prueba manual de teléfono ayudará a delimitar
el origen antes de rediseñar la tecnología de reconocimiento.

Fuentes técnicas:
- Android SpeechRecognizer: https://developer.android.com/reference/android/speech/SpeechRecognizer
- ML Kit: https://developers.google.com/ml-kit/language/translation/android
- MediaPipe AudioClassifier Android: https://developers.google.com/edge/mediapipe/solutions/audio/audio_classifier/android
