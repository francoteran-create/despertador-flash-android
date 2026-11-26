# AURORA 4.17 — Nube urbana y pictogramas de sonidos

## Primer bloque implementado
La mascota monocroma tiene cuerpo negro, contorno blanco, pequeños gestos
de boca y brazos y sombras grises tipo grafiti. Se dibuja con Android
Canvas vectorial, sin descargar GIF ni añadir dependencias externas.

- EN ESPERA: nube tranquila, **no simula micrófono activo**.
- ESCUCHANDO: movimiento leve y sobrio.
- VOZ: abre la boca mientras recibe actividad de reconocimiento;
  no genera voz ni reproduce audio.
- EVENTO CONFIRMADO: estira el brazo y muestra un dibujo durante unos
  3,4 segundos, vinculado al evento registrado.

Pictogramas ya codificados: puerta/golpe/timbre, perro, bocina, notas
musicales, globos de diálogo, sirena, llanto y teléfono. Otros sonidos
muestran un signo genérico. Se basan exclusivamente en las mismas
etiquetas que pasan por `recordSoundEvent`; no generan detecciones nuevas.

## Rendimiento y accesibilidad
Se respetan los ajustes de animación reducida y ahorro de batería.
La vista no abre micrófono, cámara, red ni servicio permanente. Al salir
se cancelan callbacks de animación. Los dibujos tienen descripción
accesible y se mantienen blancos sobre fondo negro.
No usamos aún Lottie: el dibujo directo evita assets y dependencia extra.

## Aún pendiente
El diseño generado anteriormente era una referencia estética: esto es
una aproximación vectorial programática, no una copia perfecta del GIF.
Falta comprobar en Samsung S9+ si el usuario aprueba estilo y gestos.
Los pictogramas **en el smartwatch** siguen sin implementar: no afirmar
notificaciones al reloj hasta probar la integración Wear OS.
No hay cobro ni publicación comercial todavía.

## Pruebas sugeridas
1. En SONIDOS, comprobar nube tranquila antes de tocar ESCUCHAR.
2. Iniciar captura: la nube empieza movimientos suaves.
3. Probar puerta, ladrido, música y bocina con fuentes reales. Solo los
   eventos realmente confirmados deben mostrar el pictograma.
4. En CONVERSACIÓN, hablar: la boca reacciona; DETENER la deja quieta.
5. Habilitar animaciones reducidas y comprobar que la nube siga legible.
6. Probar pantalla horizontal: se conserva la interfaz de texto/onda.
7. Medir batería y fluidez en S9+; CI no equivale a esa prueba.
