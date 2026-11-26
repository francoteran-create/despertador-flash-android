# AURORA 4.6 BETA — avisos de sonidos con vibración y flash opcional

## Qué se implementó

Se conserva la versión 4.5 de **últimos 30 sonidos** exclusivamente en
una lista dentro de la app, sin crear archivos. Las conversaciones
siguen creando TXT, con lectura y respaldo.

En 4.6, al confirmar YAMNet un sonido de categoría habilitada:

- **En pantalla:** vibración breve si la persona tiene habilitada
  la opción «Vibrar por sonidos importantes en pantalla».
- **Escucha en segundo plano:** se conserva la vibración del canal
  de notificaciones de Android. No se duplica artificialmente.
- **Flash LED:** opción separada, apagada por defecto. Solicita
  CAMERA antes de usar la linterna y pide confirmación explícita
  por posibles molestias de fotosensibilidad. Produce destellos
  durante 2,6 s como máximo, al menos 12 s entre pulsos de flash.
- Un grupo de detecciones simultáneas se limita a una señal
  de la app cada 3,5 s. El monitor continúa registrando todas las
  clases confirmadas por separado en la lista de 30, incluso
  cuando el aviso de vibración/flash se omite.
- Los sonidos sin notificación habilitada siguen visibles en
  la lista de eventos, pero no activan avisos.
- En AJUSTES se agregó «PROBAR ALERTA DE SONIDO · SIN MICRÓFONO»:
  utiliza la categoría de timbre, no inserta una falsa detección
  en el historial y no registra ningún audio.

## Reglas de seguridad y privacidad

La app no captura ni graba audio en el componente de alertas.
Los destellos son voluntarios y se cancelan al detener la
escucha, salir del módulo o finalizar el servicio de fondo.
No se activa flash sin permiso CAMERA.

**Advertencias:** el flash no debe considerarse una alarma
certificada ni garantiza funcionamiento con cámara ocupada,
permisos revocados o batería restringida. Los destellos pueden
afectar a personas fotosensibles. Probar este comportamiento
en el S9+ y permitir al usuario desactivarlo en cualquier momento.

## Implementación concreta

- \`SoundFeedbackPolicy.java\`: regulación de pulsos y enfriamiento.
- \`SoundFeedbackPolicyTest.java\`: límites por tiempo, sesiones y
  eventos simultáneos.
- \`SoundEventFeedback.java\`: controla el \`TorchFlasher\` existente
  y la vibración solo cuando procede.
- \`SenseActivity.java\`: activa señal tras sonido confirmado.
- \`AuroraSoundMonitorService.java\`: añade flash opcional tras
  publicar la notificación; Android se encarga de la vibración.
- \`MainActivity.java\`: opciones y prueba sin micrófono.
- El modelo YAMNet, las etiquetas y las sesiones de
  conversaciones conservan la lógica de la beta 4.5.

## Respaldo de ajustes y compatibilidad
La copia de alarmas y configuraciones pasa al formato
`AURORA_CONFIGURACION_V2`, incluyendo los interruptores
`monitor_sound_vibration` y `monitor_sound_flash`.
Las copias anteriores `V1` siguen siendo importables con validación
de integridad; las nuevas opciones toman valores seguros,
especialmente flash de sonidos **apagado**. Se incluyen pruebas
automáticas del recorrido V1 → V2 y rechazo de archivos alterados.

## Prueba física necesaria

1. Instalar AURORA 4.6 BETA aparte, sin desinstalar las otras apps.
2. En Ajustes, habilitar «Timbre y puertas» y probar vibración con
   «PROBAR ALERTA DE SONIDO · SIN MICRÓFONO».
3. Autorizar cámara, habilitar flash de sonidos tras leer aviso,
   volver a usar el botón. Confirmar pulsos breves y apagado.
4. Deshabilitar flash y repetir: debe vibrar pero NO encender LED.
5. Abrir SONIDOS, reproducir timbre y ladrido y verificar
   la lista de últimos 30 sin archivos TXT.
6. Activar escucha de fondo, bloquear la pantalla y probar
   un timbre. Verificar notificación/vibración/LED en S9+.
7. Probar batería, permisos, cámara ocupada, llamada, pantalla
   bloqueada y alarmas activas. Si falla un aviso, copiar
   diagnóstico de escucha, sin audio.

La CI de GitHub sólo verifica código, pruebas y APKs; no mide
la fiabilidad de los sensores ni de las alertas físicas en S9+.

## Próximos objetivos aún pendientes

La precisión de YAMNet y los cortes de voz requieren mediciones
reales. \`SpeechRecognizer\` no es un motor de transcripción
continua garantizada; se investiga un motor local alternativo.
La interfaz espacial con mascota y tipografía en el aire
es una referencia futura y no se agregó a esta beta.
