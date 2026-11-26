# AURORA 4.24 — Modo Mostrador, hito funcional

## Programado
- Módulo independiente en COMUNICAR: MODO MOSTRADOR.
- Pantalla dividida: arriba, mensaje ESCRITO por usuario rotado 180° para la otra persona;
  abajo, editor de respuesta para usuario de AURORA.
- El usuario toca ESCUCHAR RESPUESTA cuando la otra persona habla;
  Android SpeechRecognizer transcribe abajo HACIA EL USUARIO. Captura visible.
- La persona usuaria escribe abajo y toca MOSTRAR TEXTO o DECIR EN VOZ ALTA;
  su mensaje se muestra arriba HACIA ENFRENTE.
- Texto a voz local del sistema Android en español de Argentina.
- La aplicación no almacena conversaciones de esta pantalla;
  no abre servidores ni envía mensajes a terceros.
- Controles para detener el micrófono, salir de la pantalla y
  timeout de 30 segundos para sesiones que no finalicen.

## Límites y comprobaciones
- Transcripción depende de SpeechRecognizer y del servicio del sistema,
  que puede usar red según motor instalado. No prometer 100% offline.
- Es interacción por turnos, no grabación continua simultánea.
- Importante: las respuestas reconocidas nunca reemplazan el mensaje
  que escribió la persona usuaria del lado opuesto.
- Probar en Samsung Galaxy S9+: legibilidad a ambos lados,
  pantalla con teclado, permisos, voz instalada, giro 180°, cierre,
  reconexión de micrófono y batería.
- El estado compilado no equivale a ejecución física validada.

## Continuidad y próximos pasos
Ver MASTER PDF, sección COMO CONTINUAR EN OTRO CHAT.
Después del Mostrador: botón SOS voluntario, música háptica,
verificación física de alertas y prueba real de AURORA VOZ.
Diseño y personajes siguen postergados.
