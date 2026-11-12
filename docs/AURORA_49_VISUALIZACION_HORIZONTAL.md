# AURORA 4.9 BETA — VISTA HORIZONTAL DE CONVERSACIÓN Y SONIDOS

## Objetivo visual

En CONVERSACIÓN, durante la escucha, un botón "VISTA HORIZONTAL" abre
una pantalla negra a lo ancho del teléfono. Las palabras del reconocimiento
aparecen grandes en blanco; debajo figura la traducción automática, también
en blanco. La vista no abre un nuevo micrófono, ni cambia la estructura
interna de la conversación.

En SONIDOS, el mismo botón abre un visualizador multicolor de 32 bandas
de frecuencias. Los colores brillan con los cambios de intensidad real y
se conserva en grande la descripción de los sonidos detectados.
No pretende ser un sonómetro profesional ni un dispositivo de emergencia.

## Diseño implementado en código

- `SenseActivity` utiliza ahora un `FrameLayout` que contiene la pantalla
  normal y una capa negra a pantalla completa (`wideOverlay`). No se
  usa otra Activity ni un segundo `AudioRecord`.
- `android:configChanges` incluye `orientation|screenSize|
  smallestScreenSize|screenLayout|keyboardHidden` para evitar que
  rotar desde el botón reinicie la Activity o cierre la sesión.
- `VISTA HORIZONTAL` obliga orientación apaisada y oculta barras de sistema.
  `VOLVER` recupera la pantalla vertical sin interrumpir escucha.
- `DETENER Y GUARDAR` llama a `stopAll(true)`, que cierra el micrófono y
  finaliza la conversación en `SenseHistory`. Las frases originales
  y traducciones terminadas quedan en CONVERSACIONES GUARDADAS.
  Por seguridad, abandonar la Activity también finaliza el registro;
  las transcripciones ya se escriben incrementalmente para resistir
  cierres inesperados.
- La vista horizontal refleja el mismo `TextView` de resultados y
  traducción mediante `TextWatcher`: lo que se ve vertical es lo que
  aparece horizontal, con mayor tamaño y contraste.
- `LiveAudioSpectrum` ejecuta FFT de 1024 muestras reales del
  `AudioData.getBuffer()` que YA alimenta YAMNet. Retorna
  32 bandas logarítmicas entre aproximadamente 80 Hz y 7200 Hz,
  además de una intensidad relativa (RMS). No se graba ni envía audio.
- `AuroraSpectrumView` dibuja barras de colores con brillo, curvas de
  respuesta y caída progresiva en silencio. La altura/intensidad
  cambian con energía PCM, NO con el porcentaje de confianza de
  clasificación YAMNet.
- Durante una reconexión del micrófono o cuando la escucha se detiene,
  se apaga la respuesta del visualizador y se avisa claramente.
- El reconocimiento de sonidos sigue mostrándose como hipótesis,
  no como certeza. La lista de los últimos 30 permanece sin TXT/PDF.

## Lo que se conserva

Transcripción y traducción automática de frases finales en orden
(cola de 64 tareas), registro bilingüe de traducciones completadas,
monitor ambiental con alerta de vibración y flash voluntarios,
alarmas y respaldo de ajustes. El estilo general de AURORA no se
sustituye por la estética experimental de nube y palabras flotantes.

## Validación automatizada

- `LiveAudioSpectrumTest`: silencio = 0, sonido fuerte produce
  barras mayores, sonidos graves/agudos mueven bandas distintas.
- `test_wide_view_contract.py`: rotación sin recrear, ninguna nueva
  instancia de micrófono, monitor PCM existente, textos espejados,
  botón DETENER y cese de energía al detener.
- Compilación Gradle y firma/debug ARM64 en CI.
- PDF MASTER se genera del mismo commit que la APK.

## Qué necesita prueba real

1. En Samsung S9+, abrir conversación y tocar VISTA HORIZONTAL.
   Ver que la escucha continúe y que las palabras blancas se
   actualicen mientras se habla. La traducción debe verse debajo.
2. VOLVER no debe cortar ni guardar anticipadamente.
3. DETENER Y GUARDAR debe finalizar escucha y dejar disponible
   CONVERSACIONES GUARDADAS. Comprobar fecha, frases originales y
   traducciones que terminaron.
4. En SONIDOS, usar el modo horizontal frente a habla baja, palmas,
   ladrido o timbre y compararlo con silencio; las barras deben
   aumentar con intensidad real y cambiar según frecuencias.
5. Bloquear/desbloquear teléfono, volver a vertical, comprobar
   permisos, consumo de CPU/batería y estabilidad de Android.
6. No usar dos versiones de AURORA con el micrófono a la vez.

**Pendiente:** mediciones de precisión, compatibilidad real con
todas las configuraciones de rotación de Android y con el hardware
S9+, errores de audio, consumo energético y calidad del traductor.
No afirmar que CI equivale a validación física.

## Próximo diseño (no desarrollado)

La nube simple, letras manuscritas flotantes, dibujos de perros,
puertas y bocinas quedan como etapa futura. El espectro multicolor
horizontal sí se implementa en esta beta, pero no implica
rediseñar todo el sistema.

## Regla de entrega

Solo `AURORA_4_9_ARM64.apk` y
`AURORA_4_9_MASTER_ESTADO_Y_PLAN.pdf`. El código y las pruebas
permanecen en GitHub. `main` sigue estable y sin fusionar.
