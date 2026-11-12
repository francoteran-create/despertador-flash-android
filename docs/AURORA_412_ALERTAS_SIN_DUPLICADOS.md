# AURORA 4.12 — avisos ambientales más consistentes

## Qué sí se desarrolló

AURORA seguía pudiendo generar alertas repetidas para un mismo ruido
cuando YAMNet oscilaba entre etiquetas genéricas y específicas.
La versión 4.12 agrega una capa de confirmación y familias semánticas
conservadoras para distinguir mejor el mismo evento de otro diferente.

### 1. Familias de eventos equivalentes

`SoundEventFamily.java` agrupa exclusivamente parejas de hipótesis
que probablemente describan el mismo ruido:
- PERRO / PERRO LADRANDO.
- PUERTA O CAJÓN / PORTAZO.
- VOZ HUMANA / CONVERSACIÓN.

El control `SoundEventGate` comparte un cooldown de 15 segundos
entre los miembros de cada pareja, evitando dos avisos consecutivos
para el mismo suceso. También mantiene el cooldown propio de cada
etiqueta para controlar repeticiones del evento idéntico.

**No se agrupan** TIMBRE y GOLPES EN UNA PUERTA, PORTAZO
y TIMBRE, BOCINA y VEHÍCULO, ni SIRENA y ALARMA DE INCENDIO:
son avisos distintos y podrían ser importantes simultáneamente.

### 2. Confirmación prudente para hipótesis genéricas

Incluso con un único puntaje alto de YAMNet, las etiquetas
PERRO, PUERTA O CAJÓN, VEHÍCULO y VOZ HUMANA requieren
dos observaciones separadas antes de lanzar el aviso.

Se conserva el criterio de sensibilidad elegido en el teléfono.
Las clases urgentes como SIRENA mantienen una vía de aviso rápido
por puntaje alto. AURORA NO reemplaza sistemas certificados
de alarma, emergencias ni seguridad personal.

### 3. Etiquetas en castellano más coherentes

- El instrumento musical `French horn` ya no se convierte
  erróneamente en la etiqueta BOCINA: se rotula MÚSICA.
- La cadena inglesa `cat` debe representar la palabra "cat"
  o una señal explícita de maullido/ronroneo. Deja de coincidir
  con secuencias de letras internas en palabras sin relación,
  como `Location`.
- No se inventan identidades de objetos a partir de golpes:
  sin evidencia de una puerta, Knock se mantiene como
  GOLPES O IMPACTOS.

### 4. Pruebas desarrolladas

- `SoundEventFamilyTest`: familias equivalentes y eventos
  independientes de timbre, bocina, puerta e incendios.
- `SoundEventGateTest`: ladridos redundantes, categorías
  genéricas de alta confianza, recuperación de cooldown,
  separación entre timbre y portazo, y reseteo.
- `SoundClassificationTest`: French horn, Car horn,
  Cat y una cadena de texto que contiene "cat" sin ser gato.
- Toda la batería Java/Python, APK arm64, permisos y firma debug
  se verifican en GitHub Actions.

## Qué NO está demostrado todavía

No existe una medición real de precisión, recall, latencia,
sensibilidad o falsos positivos antes y después de esta beta.
Las pruebas mencionadas son unitarias y de integración de código,
no ensayos del modelo con un corpus etiquetado de grabaciones.
Por eso no corresponde subir el porcentaje de precisión
acústica solo por cambiar los filtros.

## Prueba física en Galaxy S9+

1. Escuchar un perro ladrando varias veces y revisar que no
   aparezcan dos alertas repetidas "PERRO" y "PERRO LADRANDO".
2. Hacer un portazo y tocar un timbre después: verificar que
   dos sonidos diferentes puedan notificarse.
3. Reproducir una bocina y después un motor: deben poder
   registrarse como eventos distintos.
4. Someter AURORA a sonido musical con instrumentos de viento
   y verificar que no lo confunda con una bocina.
5. Registrar ejemplos de falsos avisos u omisiones para
   calibrar umbrales con evidencia real.
6. Confirmar que siguen vigentes la traducción a pedido,
   el modo horizontal negro, el zigzag multicolor real
   y el listado interno de los últimos 30 sonidos.

## Continuidad

La lógica de recuperación de voz y sonido de 4.11 permanece.
La rama main se mantiene estable y sin fusionar. La versión
4.12 se instala en paralelo como beta. Entrega al usuario:
únicamente una APK de 64 bits y el MASTER PDF generado
desde el mismo commit.

Pendientes: prueba física de S9+, comparación con corpus real,
optimización de batería, asistente contextual y estética final.
