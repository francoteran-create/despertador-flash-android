# AURORA 4.22 — Diagnóstico de notificaciones en teléfono

## Reporte recibido
El usuario no observa alertas en su Samsung. No afirmar que la
detección real está resuelta sin ensayos físicos.

## Cambio de arquitectura
Se desacopló la publicación de notificaciones del teléfono de la
preferencia opcional de imágenes para smartwatch. El nuevo canal
`aurora_phone_sound_v422` tiene alta importancia, vibración y etiqueta
de evento; respeta permisos y filtros de categorías. Los dibujos son
decoración opcional. En segundo plano se conservan los canales por
categoría de sonidos ya implementados.

## Prueba sin micrófono
En SONIDOS aparece **PROBAR NOTIFICACIÓN EN TELÉFONO · SIN MICRÓFONO**.
En CONFIGURACIÓN aparecen la prueba equivalente y el reporte del
último intento. Se informa si:
- falta permiso de notificaciones;
- AURORA tiene todas las notificaciones bloqueadas;
- el canal nuevo está bloqueado;
- Android recibió una solicitud de publicación;
- falló la publicación.

Una publicación aceptada NO garantiza que el usuario haya visto
la alerta. Revisar modo No molestar, configuración del canal,
pantalla bloqueada, batería y comportamientos OEM. La prueba
no se guarda como detección ambiental y no abre el micrófono.

## Flujo para depurar
1. Instalar AURORA 4.22 y probar aviso sin micrófono.
2. Si no aparece, abrir Ajustes de notificaciones de Android desde
   el diagnóstico. No iniciar pruebas de clasificación todavía.
3. Si aparece, abrir SONIDOS, pulsar ESCUCHAR y mantener visible.
4. Probar timbre o ladrido reales; revisar la etiqueta de YAMNet,
   confirmar que la categoría esté habilitada y consultar último aviso.
5. Para escuchar con la app en segundo plano, activar explícitamente
   ESCUCHA CONTINUA desde CONFIGURACIÓN: entrar en SONIDOS y cerrar
   pantalla no equivale a iniciar automáticamente el servicio.
6. Medir falsas alarmas y omisiones antes de usar la app como ayuda
   diaria. No es un sistema de seguridad certificado.

## Pendientes
La recepción en Galaxy S9+ no se ha confirmado. Smartwatch,
calidad de los dibujos, alarmas bajo bloqueo y traducciones
siguen requiriendo prueba física y trabajos separados.
