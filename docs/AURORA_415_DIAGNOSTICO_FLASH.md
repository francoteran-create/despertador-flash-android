# AURORA 4.15 BETA — Diagnóstico y fiabilidad del flash

## Problema identificado en 4.14
La app distingue tres tipos de aviso visual:
- Flash LED de alarmas y temporizador: activado por defecto, se intenta
  al abrirse la pantalla de aviso.
- Flash LED por sonidos reconocidos: DESACTIVADO por defecto; requiere
  habilitarlo explícitamente, permiso Cámara y categoría de aviso activa.
- Destellos de pantalla: opcionales y distintos del LED trasero.

En el servicio de escucha ambiental, el intento de flash estaba situado
después de publicar una notificación. Si el sistema rechazaba ese aviso,
AURORA podía omitir también el LED. Se corrigió esa dependencia.

Además, si la cámara rechazaba `setTorchMode()`, el intento fallido
consumía la espera de 12 segundos antes de permitir otro flash.

## Cambios
1. El flash ambiental se intenta al confirmar una detección habilitada,
   independientemente del éxito del aviso visible de Android.
2. El bloqueo de 12 segundos se mantiene solo después de un inicio de
   flash exitoso; un intento fallido no consume ese intervalo.
3. Las fallas de permisos y de encendido se guardan como un estado técnico
   LOCAL (no se conserva audio, frases ni ubicación).
4. Configuración incorpora «CONSULTAR ÚLTIMO ESTADO DEL FLASH DE SONIDOS».
5. La duración máxima y los límites anteriores continúan sin cambios,
   por seguridad ante fotosensibilidad y consumo.

## Qué no está resuelto
- Android puede restringir la linterna cuando la cámara está ocupada.
- Alarmas: la función LED empieza al abrirse la pantalla de alarma.
  Android puede no presentar una alerta a pantalla completa con la
  pantalla bloqueada. No se garantiza parpadeo desde el receptor de alarma
  si la interfaz nunca se abrió.
- No se ha medido el comportamiento físico del Galaxy S9+.

## Ensayo corto
1. Configuración → «PROBAR FLASH LED · 5 SEGUNDOS».
   Si no parpadea, revisar permiso Cámara y que la cámara esté libre.
2. Habilitar «Encender flash LED con sonidos detectados» y
   la categoría «Timbre y puertas».
3. Pulsar «PROBAR ALERTA DE SONIDO · SIN MICRÓFONO».
4. Consultar el estado del flash por sonidos; revisar resultado.
5. Probar una alarma de 1 minuto con la pantalla bloqueada.
   Comprobar si se abre la alerta y si el LED parpadea.
6. No depender de esta beta como alarma de seguridad certificada.

## Verificación
JUnit del cooldown tras fallo, contratos Python de aviso/LED,
compilación de APK ARM64 y firma debug en GitHub Actions. El S9+ debe
validarse por separado; superar CI no demuestra eficacia física.
