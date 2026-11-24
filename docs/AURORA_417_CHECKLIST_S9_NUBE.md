# Prueba rápida de AURORA 4.17: nube grafiti

APK: `com.despertadorflash.beta417`. ARM64 debug beta.
Se instala en paralelo; no desinstalar versiones anteriores ni
activar dos micrófonos AURORA a la vez.

1. Entrar a la pantalla de inicio: aparece una nube negra de contorno
   blanco, quieta y sin afirmar que escucha.
2. Abrir SONIDOS. Antes de iniciar, queda en espera.
3. Tocar ESCUCHAR; la nube realiza un movimiento pequeño. Probar
   un timbre o golpe de puerta: si AURORA lo reconoce y confirma,
   debe mostrar una puerta y señalarla.
4. Probar un ladrido, música, bocina y gente conversando con fuentes
   reales. El dibujo debe corresponder a la etiqueta confirmada
   y desaparecer luego de unos segundos.
5. Cambiar a CONVERSACIÓN. Al hablar, la nube abre ligeramente la
   boca. Al detener el micrófono debe quedar quieta.
6. Comprobar modo horizontal negro y que no corta la captura
   al regresar a la vista normal.
7. Activar ahorro de batería/reducción de animaciones: el dibujo
   debe seguir visible, con menos movimiento.
8. Si falla una detección, no es un error del dibujo: comprobar
   la etiqueta de YAMNet y sus permisos por separado.

No implementado aún: visuales en smartwatch, sistema comercial
de suscripción y localización integral de la interfaz.
Pendiente probar rendimiento y legibilidad en Samsung Galaxy S9+.
