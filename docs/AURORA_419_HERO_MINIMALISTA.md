# AURORA 4.19 — HERO minimalista negro

## Decisión visual
La portada se simplifica radicalmente, a pedido del creador:
- Fondo **negro pleno**; sin estrellas, reloj decorativo, paneles
  de estadísticas ni cuadros de transcripción en la portada.
- Una **nube blanca grande** en el centro, con boca, ojos y un leve
  movimiento cuando las animaciones del teléfono lo permiten.
- Cuatro nubecitas blancas de navegación, alineadas a la izquierda:
  **SONIDOS, CONVERSACIÓN, ALARMA y COMUNICAR**.
- Un botón de configuración discreto arriba a la derecha.
- Las pantallas secundarias abiertas por esos accesos usan fondo
  negro; la funcionalidad antigua y los ajustes siguen disponibles.

## Implementación
- `AuroraHeroCloudView`: dibujo nativo Canvas blanco/negro, sin
  motor 3D ni GIF pesado; nunca indica una escucha activa ficticia.
- `AuroraHeroCloudButton`: botones dibujados como nubes con
  contenido accesible y enlaces a las rutas existentes.
- `MainActivity.renderMinimalHero()` sustituye el antiguo inicio
  recargado. No toca el planificador de alarmas ni el motor de audio.
- Las vistas SONIDOS/CONVERSACIÓN mantienen los controles originales
  y ahora tienen fondo negro.
- Se respeta la reducción de movimiento de Android y el ahorro de
  batería. La nube central es una mascota, no un indicador de micrófono.

## Límites y pruebas
Este es un primer diseño funcional, no una captura validada en S9+.
Las pantallas secundarias mantienen algunos paneles y botones con
colores heredados; el siguiente paso visual es unificar los controles
monocromáticos sin afectar su contraste y accesibilidad.
En teléfonos pequeños los cuatro botones deben conservar áreas
táctiles utilizables: comprobar orientación y tamaño real.
Verificar que cada nubecita abre el módulo correcto; el botón
Volver debe retornar al HERO. Alarmas programadas y registros
permanecen independientes de los cambios visuales.

## Próximos pasos
Comprobar visualmente en Galaxy S9+ y ajustar espaciados de la
portada si fuera necesario. Smartwatch independiente y cobro
por suscripción siguen pendientes.
