# AURORA 4.20 — HERO graffiti 2×2 y dos personajes

## Implementación comprobable en código
La pantalla inicial ahora tiene dos botones de nube en la fila superior
(SONIDOS y CONVERSACIÓN), una nube protagonista en el centro y dos
nubes inferiores (ALARMA y COMUNICAR). Se conservan los cuatro destinos
de la versión 4.19 y el engranaje de ajustes.

Estética: negro pleno, líneas blancas, acentos violeta, estilo urbano,
mascota con auriculares, antebrazos cruzados, sudadera y sonrisa confiada.
Las nubes de navegación tienen iconos vectoriales blancos/violetas y
son superficies pulsables y accesibles.

Al tocar la mascota aparece una opción de dos personajes:
«Nube urbana» y «Nube alternativa». La elección se conserva en
preferencias locales. La diferencia es **una primera variación gráfica
de expresiones**, no dos personajes ilustrados terminados con calidad
de la referencia generada por IA. El dibujo de referencia no se ha
importado como asset editable.

Android Canvas dibuja todo en el teléfono, sin GIF, sin dependencia Lottie
ni llamadas al servidor. La animación de reposo respeta ahorro de batería
y el ajuste de animaciones de Android. La portada no inicia micrófonos.

## Validación pendiente
Compilar y pasar contratos no demuestra que el diseño sea idéntico a
la referencia ni que se vea perfecto en la resolución del Galaxy S9+.
Verificar particularmente la distribución 2×2 con tamaños de fuente
mayores, el contraste y la zona táctil. La compatibilidad general de
smartwatch sigue pendiente. El selector de personajes requiere revisión
visual antes del lanzamiento.

## Prueba
1. Instalar beta 4.20 en paralelo.
2. Ver dos nubes superiores y dos inferiores con nube grande central.
3. Probar los cuatro accesos y volver al inicio.
4. Tocar la nube central, elegir alternativa y reiniciar: verificar que
   se conserve el personaje.
5. Comprobar apertura de Sonidos, Conversación, Alarma y Comunicar.
6. Probar detecciones, alarmas y traducciones por separado.
