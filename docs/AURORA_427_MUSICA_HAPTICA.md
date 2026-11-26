# AURORA 4.27 — SENTIR LA MÚSICA
Música háptica de micrófono: el usuario activa voluntariamente la
captura en COMUNICAR > SENTIR LA MÚSICA. AudioRecord toma muestras PCM
mono 16 kHz y aplica una medición de energía con potencia en bajas
frecuencias (Goertzel 80/125/180 Hz). Los acentos respecto al promedio
adaptativo generan pulsos de vibración de 35 a 70 ms, con al menos
220 ms entre pulsos. Tres niveles SUAVE/MEDIO/FUERTE, botón detener.

Privacidad: no se guarda ni transmite audio. Se detiene audio y
vibración al salir, cambiar de app o pulsar detener. Para mantener
un solo dueño del micrófono, detiene la escucha ambiental persistente
antes de iniciar este modo.

Limitaciones: no identifica melodía, notas ni canciones. No reproduce
una secuencia literal de música; convierte acentos rítmicos en pulsos.
Puede resultar menos sensible con fuentes graves débiles y altavoces
pequeños. El micrófono físico y el motor háptico condicionan resultados.
Debe verificarse en Samsung Galaxy S9+; ensayar permiso denegado,
silencio, música con y sin graves, intensidad y cancelación al salir.

El MASTER conserva instrucciones exactas de continuidad en otro chat,
rama de GitHub, estado y pruebas pendientes. Próximas prioridades:
validación física de alertas, AURORA VOZ, Mostrador, SOS y Música Háptica.
