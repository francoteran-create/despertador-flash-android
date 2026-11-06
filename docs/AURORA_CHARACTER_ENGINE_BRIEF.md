# AURORA / CHARACTER ENGINE — BRIEF DE ARTE Y ANIMACIÓN

## Objetivo
Convertir la portada Android existente en una experiencia callejera original: negro puro, un personaje lineal blanco, simpático y ligeramente pícaro, y funciones que flotan en el espacio. Evitar tanto la estética de dashboard empresarial como la copia literal de Fido Dido u otros personajes protegidos.

## Base real de integración
Repositorio: `francoteran-create/despertador-flash-android`.
Código: `app/src/main/java/com/despertadorflash/AuroraCharacterStage.java`.
Integración y navegación: `MainActivity.render()`.
MinSDK 24 (Samsung Galaxy S9+ / Android 10 compatible), arquitectura ARM64, no introducir plataformas en línea ni degradar el reconocimiento de sonidos. El motor existente dibuja Canvas en tiempo real; se puede sustituir por Rive si se entrega un runtime compatible y recursos integrables.

## Dirección artística
- Fondo negro #000000, sin tarjetas, cajas de colores, fotomontajes, estrellas ni exceso de neón.
- Trazos de tinta/blanco hueso #EDEDE8, 2 a 6 px según escala. Un solo acento menta apagado #66BDB5, principalmente para el toque seleccionado.
- Personaje juvenil urbano original, actitud relajada, gorra inclinada, ojos traviesos, sonrisa asimétrica, buzo amplio y zapatillas grandes; apariencia memorable en 2–3 segundos.
- Distribución para vertical: 8 opciones accesibles en dos columnas, el personaje en el centro. Las zonas táctiles son controles nativos transparentes, al menos 48dp de altura.
- Funciones: SONIDOS, VOZ A TEXTO, ALARMAS, MENSAJES, MI PERFIL, REGISTROS, TEMPORIZADOR, AJUSTES.

## Motion states deseados
IDLE: leve balanceo, respiración, mirada exploratoria, parpadeo ocasional.
HOVER/SELECT: mirar la opción y tocarla con la mano, acento de selección sobrio. Apertura de pantalla en no más de ~350ms, nunca bloquear el uso esperando una animación.
LISTENING: escuchar e inclinar la cabeza.
DETECTED: reconocer evento con sorpresa simpática.
NO_INPUT: gesto breve sin tono de error moralizante.
REDUCED MOTION: desactivar movimientos continuos respetando ajustes del sistema.

## Entregables externos aceptables
1. Rig vectorial limpio con capas/partes y derechos de uso claros.
2. Animaciones de estado exportables a Rive o un sistema equivalente usable desde Java/Android.
3. Archivo fuente editable y licencia de los recursos.
4. Prototipo de interacción funcionando en el Galaxy S9+.
5. Pruebas de FPS, consumo y accesibilidad (legibilidad, tamaño táctil).

## Criterios de aceptación
No alcanza con una ilustración o un video: los ocho controles deben abrir las funciones reales, deben conservarse alarmas/transcripciones/datos existentes y el APK ARM64 debe compilar y tener firma verificable. AURORA debe verse completa y legible a tamaño real en el Galaxy S9+.
