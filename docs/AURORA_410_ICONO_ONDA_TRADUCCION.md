# AURORA 4.10 BETA — PANTALLA HORIZONTAL DESPEJADA

## Decisiones aprobadas

1. En CONVERSACIÓN y en SONIDOS, el acceso a pantalla horizontal se
   realiza con un ícono de **celular vertical con dos flechas**:
   una apuntando hacia arriba y otra hacia abajo.
2. En horizontal la superficie es **negra**, sin encabezado,
   paneles decorativos ni botones grandes. Solo queda una
   **flechita discreta en la esquina superior izquierda** para volver.
   Pulsarla **no detiene el micrófono ni guarda anticipadamente**.
3. En CONVERSACIÓN se ven las palabras reconocidas en blanco y
   letras grandes. La traducción **NO se dispara sola**:
   únicamente comienza cuando el usuario vuelve a vertical y
   pulsa **TRADUCIR ÚLTIMA FRASE**. Si la solicitó, el resultado
   también se puede consultar en la vista horizontal.
4. Para terminar la conversación y conservar el historial se
   regresa a vertical y se pulsa **DETENER**. Se mantienen además
   los cierres de seguridad existentes cuando se abandona la app.
   El texto original se escribe incrementalmente.
5. En SONIDOS, en lugar de las barras del antiguo ecualizador,
   se muestra una **onda continua zigzag con gradiente de neón
   multicolor**. Oscila usando muestras de audio PCM reales que
   ya alimentan YAMNet. Cuanto más fuerte el sonido, más grandes
   son los picos y mayor el brillo. Silencio: línea casi plana.
   Debajo permanece la descripción de los sonidos identificados.

## Código implementado

- `RotatePhoneIconView.java`: dibuja teléfono de contorno
  con flecha izquierda hacia arriba y derecha hacia abajo.
  Área táctil de 56 dp e identificación accesible para Android.
- `SenseActivity.java`: elimina el botón de texto extenso
  para entrar en horizontal; ahora usa el ícono. La superposición
  horizontal tiene únicamente el botón discreto de regreso de
  48 dp. Se elimina DETENER Y GUARDAR dentro del horizontal.
  Se conserva la misma Activity y el micrófono existente.
- `LiveAudioSpectrum.java`: incluye 160 muestras firmadas de
  la forma de onda del mismo PCM, sin crear otra grabación.
  La FFT de 32 bandas queda disponible para análisis.
- `AuroraSpectrumView.java`: reemplaza columnas por trazo
  zigzag con tres capas de brillo y gradiente multicolor,
  decaimiento al silencio y pausa cuando queda oculto.
- `MainActivity.java`: explica transcripción en vivo y
  traducción a demanda, sin prometer traducción automática.
- Las sesiones de conversación se guardan al detener;
  las de sonidos continúan usando únicamente la lista interna
  de los últimos 30 eventos sin documentos de sonidos.

## Pruebas automáticas

- JUnit: waveform firmada de 160 puntos, silencio, amplitud
  creciente, bandas graves y agudas.
- Python: el callback de voz NO llama automáticamente al
  traductor; el botón manual sí lo invoca. El modo horizontal
  no abre un segundo micrófono ni llama a DETENER.
- GitHub Actions: una APK ARM64, tests, permisos/firma de
  depuración y PDF MASTER generado desde el mismo commit.

## Pendiente — pruebas físicas S9+

- Que el icono de rotación abra el paisaje sin reiniciar
  la escucha; que VOLVER recupere vertical sin corte.
- Que el texto sea legible a distancia y no se superponga
  con la flechita de regreso.
- Que el zigzag sea visible, reaccione al audio real y
  vuelva a plano en silencio, sin alto consumo de batería.
- Que la conversación conserve las frases al pulsar DETENER.
- Que **no se traduzca** sin pulsar el botón correspondiente.
- Que ML Kit descargue y traduzca al solicitarlo, mostrando
  errores claros si no puede hacerlo.

## Entrega

Exclusivamente dos archivos: `AURORA_4_10_ARM64.apk` y
`AURORA_4_10_MASTER_ESTADO_Y_PLAN.pdf`.
La rama principal `main` sigue sin modificaciones.
La vista más elaborada con nube y letras flotantes continúa
planificada, no implementada en esta beta.
