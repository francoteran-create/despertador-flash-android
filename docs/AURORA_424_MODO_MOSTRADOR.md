# AURORA 4.24 — Modo Mostrador, hito funcional

## Programado
- Módulo independiente en COMUNICAR: MODO MOSTRADOR.
- Pantalla dividida: arriba, texto rotado 180° para la otra persona;
  abajo, editor de respuesta para usuario de AURORA.
- La persona de enfrente toca ESCUCHAR solo cuando quiere dictar
  una frase; Android SpeechRecognizer transcribe texto. Captura visible.
- La persona usuaria escribe y toca MOSTRAR TEXTO o DECIR EN VOZ ALTA.
- Texto a voz local del sistema Android en español de Argentina.
- La aplicación no almacena conversaciones de esta pantalla;
  no abre servidores ni envía mensajes a terceros.
- Controles para detener el micrófono, salir de la pantalla y
  timeout de 30 segundos para sesiones que no finalicen.

## Límites y comprobaciones
- Transcripción depende de SpeechRecognizer y del servicio del sistema,
  que puede usar red según motor instalado. No prometer 100% offline.
- Es interacción por turnos, no grabación continua simultánea.
- Probar en Samsung Galaxy S9+: legibilidad a ambos lados,
  pantalla con teclado, permisos, voz instalada, giro 180°, cierre,
  reconexión de micrófono y batería.
- El estado compilado no equivale a ejecución física validada.

## Continuidad y próximos pasos
Ver MASTER PDF, sección COMO CONTINUAR EN OTRO CHAT.
Después del Mostrador: botón SOS voluntario, música háptica,
verificación física de alertas y prueba real de AURORA VOZ.
Diseño y personajes siguen postergados.
