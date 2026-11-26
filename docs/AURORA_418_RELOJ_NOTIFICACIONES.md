# AURORA 4.18 — Avisos con dibujos para relojes compatibles

## Implementación
AURORA crea avisos Android con dibujos grandes originales negros y blancos
(Canvas bitmap de 360×360, sin audio ni capturas) para una notificación
`BigPictureStyle`. Incluye puerta, perro, bocina, música, gente hablando,
sirena, bebé y teléfono. La etiqueta es la detección confirmada, no una
estimación nueva del reloj.

- MONITOR EN SEGUNDO PLANO: se enriquece la notificación que ya se enviaba;
  se conserva el canal y patrón de vibración por categoría.
- SONIDOS EN PRIMER PLANO: además del dibujo en pantalla, se publica un aviso
  ilustrado cuando el evento pasó por el filtro de confirmación.
- CONFIGURACIÓN: interruptor de «Avisos con dibujo para relojes compatibles»,
  y botón «PROBAR DIBUJO DE PUERTA EN RELOJ · SIN MICRÓFONO».
- La prueba está rotulada como PRUEBA y no se guarda como detección real.
- El usuario puede desactivar los avisos ilustrados. Su foto no requiere
  permiso de cámara; se dibuja dentro de la aplicación.

## Alcance real de Wear OS
En muchos relojes Wear OS, las notificaciones del teléfono se reflejan de
manera automática si están vinculados y si los permisos y canales lo permiten.
Pero **el sistema del reloj decide** cómo mostrar una notificación: puede
mostrar texto, un ícono pequeño o el dibujo ampliado al abrir el aviso.
No se garantiza la aparición automática de una puerta a pantalla completa
ni vibración cuando el reloj o su companion tienen restricciones.
No se ha creado ni compilado una app Wear OS independiente.

## Prueba necesaria
1. Instalar AURORA 4.18 en el S9+, con permiso de notificaciones.
2. Vincular un smartwatch Wear OS y habilitar avisos de AURORA en el reloj.
3. En Configuración usar PROBAR DIBUJO DE PUERTA SIN MICRÓFONO.
4. Abrir la notificación en teléfono y reloj; observar el tamaño de imagen
   y la vibración. No afirmar que funcionó si solo llegó al teléfono.
5. Probar detector real y verificar que el evento visual corresponde a
   una detección efectivamente confirmada. Evaluar duplicados y batería.
6. Repetir con un reloj Samsung, Pixel o de otra marca solo si está
   disponible para pruebas.

## Próximos objetivos
Para el requerimiento de dibujo grande que aparece directamente en el reloj,
se necesita una pequeña aplicación Wear OS dedicada (Data Layer + UI de
alertas) y pruebas en relojes concretos. Esta beta es el primer escalón
integrado con la infraestructura de notificaciones del sistema.
La app sigue siendo beta no certificada para emergencias.
