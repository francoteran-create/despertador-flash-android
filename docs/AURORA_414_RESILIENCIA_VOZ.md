# AURORA 4.14 BETA — Resiliencia de conversaciones largas

Fecha: 2026-11-12. Base de código: AURORA 4.13 en la rama
`aurora-sense-precision-2026-11-10`.

## Qué corregimos
El reconocedor de Android a veces deja de emitir callbacks sin llamar a
`onError()`. La beta 4.13 reintentaba en presencia de errores, pero podía
permanecer mostrando una sesión de voz activa aunque el reconocedor estuviera
bloqueado. En 4.14 agregamos un watchdog para detectar esa situación.

- 45 segundos sin nueva actividad reconocida desde el inicio o `onReadyForSpeech`.
- 90 segundos desde el inicio de voz o la última frase parcial.
- 18 segundos después de `onEndOfSpeech` sin resultado final.
- Control cada 5 segundos: los tiempos de reacción pueden superar el umbral
  hasta aproximadamente 5 segundos.
- Se destruye el reconocedor anterior **antes** de programar uno nuevo.
  Los callbacks de instancias anteriores se ignoran.
- La reconexión usa el backoff existente de hasta 30 segundos y no inicia
  una segunda captura en paralelo.
- En pantalla se muestra explícitamente cuando no hay captura. El informe
  de diagnóstico cuenta reinicios por ausencia de respuesta.

## Qué conserva
Las frases **finales** recibidas antes de la falla permanecen en el
historial. Los resultados parciales no son frases finales verificadas y
pueden perderse al interrumpirse el servicio. La traducción únicamente se
ejecuta cuando la persona toca TRADUCIR ÚLTIMA FRASE.

## Qué NO podemos prometer
Android SpeechRecognizer no está diseñado para captura de voz continua
ilimitada y puede depender de un proveedor que utilice red. Este watchdog
reduce bloqueos sin aviso, pero no elimina interrupciones ni demuestra
porcentaje de exactitud. Se necesitan pruebas en Samsung Galaxy S9+.

## Pruebas
- JUnit: ausencia de callbacks, parciales, espera de final, parada y reloj.
- Python: enlazado del watchdog a callbacks, limpieza y ausencia de
  traducción automática en `onResults`.
- CI: compilar ARM64 y verificar paquete, permisos y firma.
- Físicas aún pendientes: conversación de 20 minutos, pausas mayores
  que 45 segundos, cambios de Wi-Fi, pérdida de permisos y cierre durante
  reconexión; verificar que no se duplica texto ni queda una falsa escucha.

El código original de AURORA 4.13 permanece en el historial Git. `main`
no se modifica. Este trabajo sigue siendo una BETA.
