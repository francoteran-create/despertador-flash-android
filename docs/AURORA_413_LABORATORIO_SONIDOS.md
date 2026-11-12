# AURORA 4.13 BETA — laboratorio manual de sonidos, sin audio

## Problema resuelto
Las betas anteriores incluían un modo voluntario de prueba con
ACERTÓ / NO COINCIDE y SONIDO NO DETECTADO, pero las observaciones
solo se mostraban durante unos segundos. No era posible revisar
las pruebas de timbres, perros, golpes o bocinas ni comparar
resultados después de cerrar la escucha.

## Qué implementa exactamente la 4.13
- En CONFIGURACIÓN Y PRUEBAS está el interruptor
  **MODO DE VALIDACIÓN DE SONIDOS**, apagado inicialmente.
- Al activarlo, abrir SONIDOS y comenzar a escuchar.
  Cada detección puede marcarse **ACERTÓ** o **NO COINCIDE**.
  La interfaz conserva hasta cinco preguntas simultáneas
  para que otro evento no reemplace una pregunta anterior.
- Si se oyó algo real y AURORA no lo detectó, tocar
  **REGISTRAR SONIDO NO DETECTADO** y elegir el tipo:
  timbre, golpe/portazo, mascota/ladrido, bocina/tránsito,
  sirena/alarma, voz/conversación, llanto/gritos u otros.
- `SoundValidationStore` guarda únicamente contadores de
  evaluaciones en SharedPreferences privadas:
  aciertos / falsas alarmas marcadas / sonidos omitidos
  por tipo. No guarda audio, etiquetas detalladas de
  eventos, ubicación, fecha, hora, persona ni transcripciones.
- CONFIGURACIÓN ahora muestra un resumen local de
  evaluaciones voluntarias, con actualización, copia de
  conteos al portapapeles y borrado separado con confirmación.
  Borrar estos conteos NO borra alarmas, conversaciones
  ni la lista de los últimos 30 sonidos.
- `SoundValidationMetrics` limita cada contador a 1 millón
  para evitar acumulación indefinida/corrupción; controla
  resultados inválidos y diferencia omisiones de falsos avisos.
- Si el modo voluntario está apagado, no se registran
  evaluaciones de sonido.

## Qué significan los indicadores
- **Coincidencia entre alertas evaluadas**:
  aciertos marcados / (aciertos + alertas marcadas incorrectas).
- **Cobertura de pruebas marcadas**:
  aciertos marcados / (aciertos + omisiones marcadas).

Ambas cifras usan **exclusivamente ejemplos que una persona
eligió evaluar**. El proceso no sabe cuántos sonidos ocurrieron
fuera de esos ejemplos y está sujeto a sesgos. Por eso
**NO SON precisión/recall de laboratorio**, ni permiten asegurar
que la aplicación detecta emergencias o sonidos importantes.

## Pruebas automatizadas
- JUnit `SoundValidationMetricsTest`: ratios, vacíos sin
  porcentajes ficticios, contadores acotados, tipos distintos
  y restauración de conteos agregados.
- Python `test_sound_validation_contract.py`: interacción
  de los botones con el almacenamiento local, voluntariedad,
  privacidad, informe, copia y borrado sin afectar el historial.
- Gradle compila una APK ARM64; GitHub Actions verifica
  permisos, firma de depuración y entrega MASTER del mismo
  commit. Ninguna CI reemplaza el Galaxy S9+.

## Próximo ensayo práctico en S9+
1. Configuración → activar MODO DE VALIDACIÓN DE SONIDOS.
2. Entrar en SONIDOS → ESCUCHAR.
3. Probar timbre, ladrido, golpe, bocina y voces en un
   entorno controlado, sin depender de esta app como alarma.
4. Tras cada detección, tocar ACERTÓ o NO COINCIDE.
5. Cuando no detecte un sonido, tocar SONIDO NO DETECTADO
   y registrar su categoría manualmente.
6. Abrir CONFIGURACIÓN y revisar los contadores; usar
   COPIAR RESULTADOS SIN AUDIO para analizarlos y
   evaluar ajustes de precisión en próximas betas.
7. Confirmar que BORRAR SOLO RESULTADOS no borra
   conversaciones, alarmas ni los últimos 30 sonidos.

## Lo que NO cambió
La traducción sigue solo a pedido. Las pantallas horizontales
son negras con botón discreto para volver; sonidos muestran
zigzag multicolor de muestras reales. Se conserva la
recuperación de voz/sonidos de 4.11 y la deduplicación
de avisos de 4.12. La rama `main` sigue sin fusionarse.

## Entrega
Únicamente `AURORA_4_13_ARM64.apk` y
`AURORA_4_13_MASTER_ESTADO_Y_PLAN.pdf`, construidos
desde el mismo commit. No se adjunta APK 32 bits ni
fuentes ZIP. Pendientes: pruebas físicas S9+, corpus
acústico validado, consumo de batería y estilo definitivo.
