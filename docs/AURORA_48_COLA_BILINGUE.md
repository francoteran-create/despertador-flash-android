# AURORA 4.8 BETA — continuidad bilingüe sin perder frases

## Problema real corregido

La 4.7 ejecutaba la traducción de la última frase final reconocida, pero cuando
llegaban frases nuevas antes de que ML Kit terminara podía ignorar el resultado
de frases anteriores. La pantalla mostraba sólo la última traducción y los
registros escritos conservaban únicamente los originales.

## Implementación que compila la 4.8

- Se creó `TranslationWorkQueue.java`, una cola ordenada y acotada a
  **64 frases**. La primera frase se traduce antes que la segunda y así
  sucesivamente. Si ML Kit está ocupado, las demás esperan sin perder el
  original ni reemplazar silenciosamente el trabajo pendiente.
- Cada resultado FINAL reconocido inicia automáticamente su solicitud de
  traducción. Los resultados parciales siguen siendo únicamente transcripción
  visible y no crean solicitudes duplicadas.
- Cada traducción **efectivamente completada antes de detener la sesión**
  se agrega al historial de CONVERSACIONES. El registro incluye el idioma
  de destino y un fragmento del original para identificar a qué frase se
  refiere. El original siempre se guarda primero.
- La tarjeta grande sigue mostrando **la traducción de la frase más reciente**,
  pero las anteriores que se completaron se pueden consultar en el historial
  de la conversación.
- Si una traducción falla, llega vacía o excede los 45 segundos de espera,
  la app lo registra como traducción no realizada y continúa intentando las
  siguientes, sin bloquear indefinidamente toda la cola.
- El modelo ML Kit se reutiliza mientras el idioma de origen/destino sea el
  mismo. Al detener la sesión o cambiar idioma, se cancela la cola y ninguna
  respuesta antigua puede escribirse en una conversación posterior.
- Para evitar consumo de memoria infinito, la cola tiene un tope de 64.
  Si se supera, **el texto original sigue guardándose**, pero la traducción
  no puede garantizarse y se informa el estado.

## Limitaciones y pruebas necesarias

Android SpeechRecognizer sigue sin garantizar una escucha continua real.
AURORA traduce frases finales, no palabras individuales en vivo.
Las traducciones que no terminaron cuando la persona detuvo la conversación
**no se guardan como traducciones**: sus originales permanecen accesibles.
La primera descarga de idioma ML Kit puede requerir Internet.

Pruebas automáticas:
- `TranslationWorkQueueTest` verifica 3 frases rápidas, orden, cola llena,
  invalidación de resultados viejos y rechazo de frases vacías.
- `test_auto_translation_contract.py` verifica conexión del resultado FINAL,
  uso de cola y guardado de traducciones terminadas, sin sustituir el historial
  de sonidos.
- Gradle compila una única APK arm64-v8a, verifica permisos y firma debug.

**No se ha confirmado aún en Galaxy S9+** ni la calidad del texto, ni los
tiempos, ni la vibración/flash con distintas configuraciones de Android.

## Entrega autorizada

Solamente dos archivos: `AURORA_4_8_ARM64.apk` y
`AURORA_4_8_MASTER_ESTADO_Y_PLAN.pdf`. El PDF se genera desde el
mismo commit que la APK y describe exactamente compilado, verificado en CI,
pendiente en teléfono y planificado. No hay APK de 32 bits ni ZIP de
fuentes entre las entregas del usuario. Código y versiones viven en GitHub.

## Plan aún no ejecutado

1. Verificar en S9+ tres frases rápidas y corroborar que el historial
   contenga originales y traducciones que terminaron.
2. Evaluar alternativas a SpeechRecognizer para escucha continua confiable.
3. Medir resultados de YAMNet y las alertas reales (batería, falsos avisos).
4. Revisar un historial bilingüe más atractivo y eventualmente guardar
   traducciones pendientes tras cerrar, sin comprometer privacidad o batería.
5. Estética futura: nube simple y tipografía escrita en el aire, ilustraciones
   de sonidos y flash/vibración accesibles. Todavía no implementado.

Rama: `aurora-sense-precision-2026-11-10`. La rama estable `main` permanece
sin fusionar mientras la beta no supere pruebas reales.
