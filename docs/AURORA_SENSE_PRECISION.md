# AURORA SENSE - Control de precision y validacion

Fecha: 2026-11-10
Origen: rama aurora-sense-precision-2026-11-10, derivada de AURORA MASTER 4.0.
Este documento describe cambios en codigo, NO una validacion de eficacia real.

## Objetivo de producto
La persona usuaria debe recibir informacion conservadora y comprensible
sobre sonidos del entorno. "Reconocer un sonido" NO equivale a "transcribir voz".
La transcripcion textual tiene un modulo separado.

## Cambios incorporados al codigo
- Knock (golpe aislado) solo implica POSIBLES GOLPES O IMPACTOS.
  Para mostrar POSIBLES GOLPES EN UNA PUERTA se exige evidencia
  independiente de Knock y Door con umbrales minimos, sin un portazo
  dominante en el mismo resultado. Estos umbrales son heuristicas de
  desarrollo y NO han sido validados con grabaciones reales.
- Etiquetas conservadoras: Speech -> POSIBLE VOZ HUMANA, Conversation ->
  POSIBLE CONVERSACION, Dog -> POSIBLE PERRO, Bark -> POSIBLE PERRO LADRANDO,
  Knock -> POSIBLES GOLPES EN UNA PUERTA, Door slam -> POSIBLE PORTAZO.
- Un clasificador puede devolver multiples clases simultaneamente: se
  consolidan por etiqueta, reteniendo el puntaje mayor por clase.
- Cuando la evidencia especifica tiene fuerza comparable con la generica,
  se suprime la etiqueta generica redundante de ese mismo fotograma.
  Esto NO elimina detecciones independientes (por ejemplo, musica y ladridos).
- El mismo normalizador se usa tanto en primer plano como en segundo plano.
- Se amplian a 10 los resultados evaluados por YAMNet en cada ventana.
- Una observacion repetida con menos de 300 ms de separacion no constituye
  otra confirmacion. Los sonidos ordinarios requieren dos observaciones
  independientes o un puntaje muy alto. Las sirenas y alarmas de incendio
  conservan su via rapida existente, sin garantias de seguridad.
- Se agrega la categoria opcional de avisos de voces y conversaciones,
  desactivada por defecto; las detecciones siguen mostrandose en pantalla.

## Validacion voluntaria sin guardar audio
- En Ajustes, activar "Evaluar detecciones con ACERTÓ / NO COINCIDE"
  (desactivado por defecto). Abrir el modulo SONIDOS.
- Cada evento visible ofrece acciones independientes para marcar si la
  etiqueta coincide o no, incluso cuando hay varios sonidos simultaneos.
- "REGISTRAR SONIDO NO DETECTADO" permite anotar omisiones declaradas
  por la persona que realiza la prueba. El sistema no presume que esa
  anotacion haya sido verificada de forma independiente.
- Los datos se escriben en el historial local al detener la sesion.
  No se almacena audio; no se transmiten las respuestas a servidores.
- Tras exportar los TXT de AURORA, un desarrollador puede analizar
  los marcados mediante:
  `python3 scripts/evaluate_sound_feedback.py ruta/registro1.txt ruta/registro2.txt`
  El script NO presenta estos marcados voluntarios como precision,
  recall o una tasa representativa de aciertos.
- Las notificaciones simultaneas del mismo grupo se mantienen separadas
  por etiqueta; desactivar una categoria retira solo sus avisos y
  no detiene la indicacion de escucha permanente.
- En etiquetas inglesas se evitan falsos positivos por subcadenas,
  como Outdoor confundido con Door o Carillon confundido con Car.

## Pruebas automatizadas previstas en CI
- Nombres de clases no atribuyen causas no detectadas.
- Timbre y ladrido simultaneos no se anulan mutuamente.
- La etiqueta especifica no oculta una etiqueta generica cuando su evidencia
  es demasiado debil.
- Voz y musica coexisten; Speech no significa Conversation.
- Antirrebote de observaciones sucesivas de un mismo fotograma.
- Pruebas previas de alertas, temporizadores, alarmas y Android.

## Validacion indispensable ANTES de vender
Estado actual: PENDIENTE DE PRUEBAS CON AUDIOS REALES Y DISPOSITIVOS FISICOS.

### Banco de sonidos reales
Crear un conjunto de prueba con consentimiento y origen identificado, con:
1. Golpes de puerta, portazos, puertas corredizas, golpes sobre mesa.
2. Timbres mecanicos, electronicos, notificaciones de telefono y campanas.
3. Ladridos, aullidos, perro quieto, otros animales.
4. Conversaciones cercanas/lejos, TV, monologo, voz grabada, silencio.
5. Musica instrumental y con voz, sonidos de instrumentos aislados.
6. Combinaciones simultaneas: timbre + musica; ladrido + conversacion;
   golpes + television; voz + musica, en volumenes y distancias variados.
7. Negativos confundibles: percusion musical, golpe metalico, cierre de
   cajon, alerta de movil, ruido de calle.

Registrar por fragmento: verdad de referencia, categoria esperada, detecciones,
latencia, falsos avisos, omisiones, puntaje del modelo y condiciones de microfono.
El conjunto de entrenamiento y el de prueba deben mantenerse separados si
se recalibran umbrales. No usar clips del ajuste para declarar resultados.

### Metricas de aceptacion
Evaluar matriz de confusion, precision y exhaustividad por clase, latencia en
segundos y falsos avisos por hora de escucha. Informar cantidad y contexto de
clips; no inventar porcentajes. Comparar sensibilidad alta/media/precisa.
Validar Galaxy S9+, pantalla apagada, llamada entrante, interferencia de audio,
temperatura y consumo de bateria durante una hora.

## Limitaciones que deben mostrarse en la aplicacion
- Las puntuaciones de YAMNet NO son probabilidades calibradas.
- No se puede garantizar que un sonido se reconozca siempre.
- Ningun telefono sustituye detectores certificados ni sistemas de emergencia.
- Diferenciar etiquetado de sonidos, transcripcion de palabras y avisos.
- No afirmar "llaman a la puerta" si solo se oyeron golpes.

## Diseño y continuidad
AURORA 2.8 enviada por el usuario es REFERENCIA VISUAL APROBADA.
Se restauro en MainActivity.render() el bloque exacto de inicio de
AURORA 2.8 desde el commit Git 1c0f41cbd9f44a798890afdfd23103db8d7ab9e9.
Incluye reloj AURORA Visual Engine, accesos de Sonidos/Conversacion,
controles de alarmas y comunicacion, tarjetas de transcripcion en vivo,
registros y temporizador. El personaje de versiones 3.x fue eliminado
del codigo nuevo. PENDIENTE: comprobar visualmente fidelidad, legibilidad
y comportamiento en Samsung Galaxy S9+; no se declara validado en telefono.

## Empaquetado
La siguiente entrega al usuario debe ser UN SOLO ZIP con:
- APK de la misma revision de codigo; identificar si es debug o release.
- Codigo fuente completo, incluyendo yamnet.tflite para recompilar.
- PDF de continuidad generado desde ese mismo commit.
- README y manifest de hashes/versiones/estado de pruebas.
No crear una entrega etiquetada como "compilada" si el CI no pasa.
