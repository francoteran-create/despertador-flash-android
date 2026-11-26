# AURORA 4.18 — Prueba de notificaciones ilustradas en smartwatch

APK ARM64 de desarrollo, independiente de 4.17.

1. En el teléfono, comprobar permisos de notificaciones.
2. Emparejar un reloj con Wear OS desde su aplicación oficial.
3. En la app del reloj habilitar la recepción de notificaciones AURORA.
4. En AURORA → CONFIGURACIÓN, verificar el interruptor
   «Avisos con dibujo para relojes compatibles».
5. Pulsar «PROBAR DIBUJO DE PUERTA EN RELOJ · SIN MICRÓFONO».
6. Verificar que aparece una notificación en el teléfono. Comprobar
   por separado si el reloj muestra dibujo, texto y vibración.
7. Revisar notificación expandida y diferencias según modelo de reloj.
8. Activar SONIDOS y ensayar timbre real, perro y bocina; los dibujos
   deben corresponder únicamente a etiquetas realmente confirmadas.

No se garantiza el formato de la imagen en todos los relojes. Un reloj
no Wear OS puede requerir otra integración. La vibración está controlada
por los canales de Android y la configuración de cada reloj. No se
ha implementado aún una app Wear OS dedicada.

Si falla, registrar modelo de reloj, sistema, companion app, permisos,
si el aviso llegó al móvil, si llegó al reloj, si vibró y si se mostró
el dibujo. No hacen falta audios ni conversaciones privadas.
