# AURORA - Accesibilidad sonora y visual

Aplicación Android para ayudar a personas sordas e hipoacúsicas a percibir
sonidos del entorno, recibir avisos, usar alarmas y convertir voz en texto.
El reconocimiento de sonidos es probabilístico y NO sustituye dispositivos
certificados de seguridad ni servicios de emergencia.

## Fuentes oficiales y estado

- Repositorio: francoteran-create/despertador-flash-android
- Rama estable de código: `main` (MASTER interna 4.0, versionCode 40).
- Rama de trabajo de precisión: `aurora-sense-precision-2026-11-10`
  (4.8-bilingual-queue-beta, versionCode 48), revisión en PR #1.
- Referencia visual elegida por el usuario: APK AURORA 2.8 proporcionada
  en la conversacion. La pantalla inicial se restauro desde el codigo
  fuente historico del commit 1c0f41cbd9f44a798890afdfd23103db8d7ab9e9.
  Se elimino el personaje animado de versiones 3.x.
  PENDIENTE: verificacion visual y de interaccion en Galaxy S9+.
- CI: pruebas JUnit y compilación Android solo ARM64 (64 bits). El resultado se
  consulta en GitHub Actions por commit; no equivale a verificación física.
- Pruebas en Samsung Galaxy S9+, batería y clips reales: PENDIENTES.
- Evaluación de precisión, falsos positivos y omisiones: NO MEDIDA.

## Funciones representadas en el código

YAMNet local para sonido ambiental, escucha explícita de primer plano,
servicio de micrófono con notificación, categorías configurables de avisos,
alarmas y temporizadores, transcripción mediante SpeechRecognizer Android,
traducción ML Kit, registros locales y consulta de información registrada.
Ver `docs/AURORA_V1_CIERRE.md`: distingue código existente de pruebas reales.

En la rama 4.1: etiquetas conservadoras para timbre/puerta/golpes,
portazo, perro/ladridos, voz/conversación y música; normalización de
eventos simultáneos, antirrebote de confirmaciones y avisos opcionales
de voces. Ver `docs/AURORA_SENSE_PRECISION.md` y los tests JUnit.

## Modo de validacion de sonidos (opcional)
En Ajustes, activar "Evaluar detecciones con ACERTÓ / NO COINCIDE".
En la pantalla SONIDOS se puede marcar una etiqueta correcta o incorrecta,
y registrar manualmente sonidos que no fueron detectados. Las marcas
se muestran en el panel de prueba y NO crean archivos de sonido.
El analizador historico `scripts/evaluate_sound_feedback.py` sigue
disponible solo para evaluar antiguos TXT de prueba y NO calcula
tasas representativas de precision.
El script tiene tests en `scripts/tests/` ejecutados por GitHub Actions.

Las notificaciones de eventos simultaneos se identifican por etiqueta,
para que un timbre y unos golpes de puerta no se reemplacen dentro de
la misma categoria. Desactivar avisos retira solo esa categoria.

## Instalación de la beta y seguridad de los datos
La version 4.8 BETA usa el paquete independiente
`com.despertadorflash.beta48`, nombre visible `AURORA 4.8 BETA`.
La app original es `com.despertadorflash`, y las betas previas
(4.2, 4.3, 4.4, 4.5, 4.6 y 4.7) tienen identificadores distintos. Se instala en paralelo
**sin desinstalar las anteriores**, y NO migra sus alarmas o datos de
forma automatica. No mantener dos escuchas de microfono activas ni
activar alarmas duplicadas.

No ejecutar ambos detectores de microfono a la vez: detener la escucha
en la AURORA original antes de probar la beta (Android puede bloquear
el acceso simultaneo al microfono).

Instrucciones de esta beta: `docs/AURORA_48_COLA_BILINGUE.md`, `docs/AURORA_47_TRADUCCION_AUTOMATICA.md`, `docs/AURORA_46_AVISOS_SONOROS.md` y `docs/AURORA_45_SONIDOS_30.md`. Diagnóstico de voz y traducción: `docs/AURORA_44_DIAGNOSTICO_REAL.md`.

La beta tiene firma de desarrollo efimera. Puede no actualizar otra
beta ya instalada con firma distinta: exportar primero alarmas y
registros de la beta anterior. **No desinstalar la AURORA original.**
La beta 4.8 no restaura por si sola la informacion de una AURORA 2.8
anterior: el exportador nuevo solo existe en las versiones que lo
incorporan. La copia nueva de configuracion no contiene datos privados
de perfil ni historiales.

En 4.6, el respaldo de preferencias es V2: incluye vibración y flash de sonidos. Sigue aceptando copias V1 de betas anteriores sin activar flash involuntariamente.

En AJUSTES aparecen los botones `GUARDAR ALARMAS Y AJUSTES EN UN
ARCHIVO` y `RESTAURAR ALARMAS Y AJUSTES`. Exportan/importan un TXT
con checksum SHA-256 y valores restringidos a alarmas, preferencias de
vibracion/flash y avisos. No incluye sesiones, perfil ni audio. La
importacion solicita confirmacion y reprograma las alarmas. El checksum
detecta corrupcion, **no autentica al emisor del archivo**.

En CONVERSACIONES GUARDADAS, elegir una carpeta del telefono y tocar
`RESPALDAR Y VERIFICAR CONVERSACIONES`. Unicamente las conversaciones
se respaldan por TXT, con hash verificado. Se conservan antiguos archivos
SONIDOS de otras versiones sin borrarlos: pueden verse activando
`Mostrar archivos antiguos de sonidos`. No se crean nuevos archivos de sonidos.

La beta 4.8 sigue siendo una version de PRUEBA, no un lanzamiento para
Play Store; hacen falta validacion en Galaxy S9+ y sonidos reales.

## Correcciones criticas 4.3: escucha y traduccion
- La pantalla SONIDOS muestra hasta cuatro hipotesis de sonido recientes,
  actualiza cuando cambia el entorno y limpia resultados vencidos a los
  3,5 segundos. El historial solo agrega sonidos confirmados.
- Ambos detectores YAMNet examinan hasta 40 categorias con umbral inicial
  reducido. El clasificador no se limita a una sola clase dominante.
- Si falla el micrófono o el clasificador, se liberan recursos y se
  permiten hasta tres reconexiones con backoff. La interfaz o notificacion
  indica RECONECTANDO y no da a entender que protege durante ese lapso.
  Tras errores persistentes informa claramente ESCUCHA DETENIDA.
- En CONVERSACION, la transcripcion y la TRADUCCION son dos paneles
  independientes. El usuario elige Espanol, Ingles o Portugues de destino.
  Puede traducir una frase final o parcial reconocida, con estado de
  descarga del modelo, progreso y errores visibles. La primera descarga
  de ML Kit puede requerir Internet.
- Se corrigieron resumenes de sonidos y consultas de conversaciones
  que buscaban el formato de registros antiguo y podian devolver vacío.
- Si no se eligio carpeta externa, guardar el historial interno NO
  genera un falso mensaje de error de copia.
- La pausa entre sesiones de Android SpeechRecognizer aumenta; aun puede
  haber cortes propios del servicio de Android, que no es una escucha
  ininterrumpida garantizada.

Detalles tecnicos y plan de prueba: `docs/AURORA_43_CORRECCIONES.md`.

## Correcciones y prueba de diagnóstico 4.4
- **Hallazgo técnico:** faltaba `android.permission.INTERNET` en el
  manifest de 4.3. Agregados INTERNET y ACCESS_NETWORK_STATE para
  descarga inicial de modelos ML Kit y proveedores de voz con red.
  GitHub Actions inspecciona ambos permisos en las APK compiladas.
- SONIDOS prueba bloques de entrada de 500 ms (antes 1 segundo) y
  muestra eventos breves antes que música/viento/agua del ambiente.
  No significa que YAMNet separe toda mezcla de sonidos.
- `SoundCaptureTelemetry`: estados de muestreo, respuestas del modelo,
  tiempo desde la última respuesta y recuperaciones, sin grabar sonido.
  Botón **COPIAR DIAGNÓSTICO (SIN AUDIO)**. También incluye código de
  error SpeechRecognizer, cantidad de frases parciales/finales y
  categoría de error ML Kit, sin guardar frases ni audio.
- CONVERSACIÓN permite escribir un texto de prueba y traducirlo
  SIN depender del micrófono. Se desplaza a la traducción al tocar
  el botón y avisa si no responde tras 30 segundos.
- Se limita la vista de historial a 90 elementos para evitar que
  una pantalla larguísima degrade la escucha; el archivo de sesión
  tiene su propia conservación de registros.
- **Limitación técnica real:** Android SpeechRecognizer no garantiza
  transcripción continua. Según el proveedor Android, puede usar
  procesamiento remoto. AURORA no guarda audio, pero ese servicio
  externo puede procesarlo. Para transcripción continua de verdad
  hace falta evaluar otro motor, no prometerlo por estas correcciones.
- **Prueba recomendada**: escribir "Buenos días, ¿cómo estás?"
  con idioma origen español y destino inglés; si traduce, el fallo
  de voz está antes de ML Kit. Si no traduce, copiar diagnóstico.
  En SONIDOS observar que suban los contadores de respuestas IA.

Ver documentación: `docs/AURORA_44_DIAGNOSTICO_REAL.md`.

## Cambios 4.5 — sonido con lista de 30, conversaciones con archivos
- Los sonidos confirmados por YAMNet, tanto en pantalla como en el
  servicio de escucha continua, pasan a `RecentSoundEvents`:
  **30 eventos como máximo, más reciente primero**, fecha/hora,
  sin archivos TXT ni PDF de sonidos. Cada nueva detección desplaza
  el evento más antiguo al completar 30.
- La lista está dentro de AURORA y puede borrarse con confirmación;
  la acción NO modifica transcripciones ni alarmas.
- Los eventos se limitan mediante `SoundTimeline` con JUnit
  que comprueba 31+ eventos, simultaneidad, orden y control de tamaños.
- Los botones opcionales de validación `ACERTÓ/NO COINCIDE` se
  muestran aparte, sin contar como nuevos eventos detectados ni crear TXT.
- La consulta del asistente usa la lista de 30; ya no busca los
  sonidos en antiguos archivos.
- `RecordsActivity` se centra en **CONVERSACIONES GUARDADAS**;
  ofrece un selector para ver archivos de sonido de betas anteriores,
  sin eliminarlos. Respaldos masivos solo de conversaciones.
- La transcripción sigue guardándose al detener o salir,
  con opción de releer y exportar. El reconocimiento continuo
  de voz sigue limitado por Android SpeechRecognizer.
- La interfaz visual histórica 2.8 no se reemplazó con la
  mascota experimental; las ideas de diseño espacial quedaron para futuro.

Especificación completa: `docs/AURORA_45_SONIDOS_30.md`.

## Mejoras 4.6 — vibración y flash por sonidos confirmados
- **SONIDOS en pantalla:** avisos de vibración al confirmar una categoría
  habilitada. El sonido queda visible en la lista de 30 incluso sin aviso.
- **Escucha en segundo plano:** las notificaciones de Android siguen
  realizando la vibración, sin duplicar la vibración desde la app.
- **Flash LED opcional:** desactivado de fábrica. Requiere permiso de
  Cámara y confirmación informada sobre fotosensibilidad; se detiene
  al finalizar la escucha. Máximo 2,6 s por aviso, con enfriamiento
  de 12 s entre flashes para no parpadear continuamente.
- Una nueva clase `SoundFeedbackPolicy` evita avisos por múltiples
  clasificaciones seguidas; `SoundEventFeedback` controla vibración
  y linterna. Incluye pruebas JUnit.
- **Prueba sin micrófono** en Ajustes para verificar vibración/flash sin
  añadir un evento falso a la lista de sonidos.
- No se tocaron las alarmas ni la estética principal 2.8.
- **Faltan pruebas físicas** del flash y vibración en el Galaxy S9+.

Detalles y protocolo: `docs/AURORA_46_AVISOS_SONOROS.md`.

## Corrección 4.7 — traducir automáticamente, sin pantallas confusas
- Dos paneles juntos: **LO QUE SE DIJO · ORIGINAL** y
  **TRADUCCIÓN AUTOMÁTICA**, con texto traducido grande.
- El idioma de la conversación y el de destino se eligen arriba.
  No se permite elegir un mismo idioma para ambos.
- Cada resultado final de Android SpeechRecognizer comienza la
  traducción con ML Kit automáticamente, sin tocar un botón.
  Los parciales aún se transcriben hasta que Android cierra la frase.
- Se reutiliza un cliente de traducción por par de idiomas y se
  descartan respuestas antiguas. La primera descarga de modelos
  puede necesitar Internet; los fallos se muestran claramente.
- El inicio de AURORA lleva al módulo único desde
  **ABRIR TRADUCTOR**; desaparece el flujo independiente
  de transcripción inmediata sin traducción de la portada.
- Los controles de diagnóstico y prueba escrita quedan apartados.
  **REINTENTAR TRADUCCIÓN** sigue disponible solo para fallas.
- Se conserva el guardado de conversaciones (frases originales),
  la lista de 30 sonidos sin archivos y los avisos de 4.6.
- No se promete traducción palabra por palabra o voz continua
  hasta evaluar un motor nuevo y medir comportamiento en S9+.

Detalles: `docs/AURORA_47_TRADUCCION_AUTOMATICA.md`.

## Mejora 4.8: cola de traducciones y registros bilingües
- Se procesan frases finales por orden con `TranslationWorkQueue`,
  de capacidad máxima 64. Una frase nueva no hace que el resultado
  de una anterior se descarte silenciosamente.
- Toda frase original sigue escribiéndose en la conversación.
  Las traducciones que alcanzan a completarse antes de DETENER
  se agregan a ese mismo historial, referenciando su original.
- Errores, tiempos agotados o cola llena se distinguen de
  traducciones exitosas. No se promete un traductor continuo
  palabra por palabra ni recuperar traducciones pendientes
  después de cerrar la sesión.
- Las pruebas JUnit cubren orden, saturación y cancelación;
  los contratos Python comprueban integración al recibir
  frases finales.
- El código de la estética futura no se introdujo ni se cambió
  la rama `main` estable.

Detalles: `docs/AURORA_48_COLA_BILINGUE.md`.

## Entrega vigente: EXACTAMENTE DOS ARCHIVOS
El workflow `.github/workflows/android-apk.yml` verifica primero
las pruebas automatizadas, luego compila **solo arm64-v8a**.
En entregas marcadas `[DELIVERY]` o ejecuciones manuales
publica exactamente:
1. `AURORA_4_8_ARM64.apk`: un instalador 64 bits para el Galaxy S9+.
2. `AURORA_4_8_MASTER_ESTADO_Y_PLAN.pdf`: estado compilado,
   verificado en CI, pendiente de prueba física y plan futuro.

**No entregamos ZIP de fuentes ni APK de 32 bits**. El historial
del código, la rama y los commits están en GitHub. GitHub Actions
puede envolver ambos archivos en un ZIP de descarga externo, pero
dentro solo hay los dos documentos previstos.

La firma es DEBUG y requiere pruebas reales antes de uso de producción.

## Cómo probar AURORA 4.8
Instalar `AURORA 4.8 BETA` en paralelo. Elegir español
como idioma de conversación e inglés como destino. Decir tres
frases con pausas breves y confirmar que se traduzcan sin
pulsar un botón; detener y abrir CONVERSACIONES GUARDADAS
para leer los originales y las traducciones que terminaron.
Si falla, usar COPIAR DIAGNÓSTICO SIN AUDIO. Los tiempos de
respuesta y la tasa de error en Samsung Galaxy S9+ aún no
fueron medidos.

## Cierre del bloque SONIDOS 30 — beta 4.5
La implementacion de sonidos esta consolidada en el codigo, con
tests automatizados de orden, limite, simultaneidad y separacion
de archivos. Los sonidos nuevos se almacenan solo como los
30 eventos de la lista dentro de la app; las conversaciones
se siguen guardando en TXT para releerlas. El guardado de eventos
de sonidos no crea documentos. Los antiguos TXT se preservan.

**Aún pendiente:** probar en Galaxy S9+ la interfaz y eventos
reales, vibracion/flash, el comportamiento en segundo plano,
consumo de bateria y traduccion. La beta no se fusiona a main.

## Estado de entrega 4.6
Se incluye el modulo de alertas vibracion/flash opcional que se
activa por los eventos confirmados, sin grabar audio.
Se preserva la lista local de 30 sonidos y el guardado de
conversaciones. La app 4.6 usa `com.despertadorflash.beta46`.
Verificar CI y pruebas de Galaxy S9+ antes de declararla estable.

## Bloque de entrega 4.6 consolidado
Código de alertas físicas, pruebas de confirmación/repetición,
conservación de 30 sonidos, transcripciones y respaldo de ajustes V2
con lectura de V1 quedan documentados en esta rama.

**No aprobado como versión estable:** faltan pruebas físicas de
Galaxy S9+, precisión del detector, vibración/linterna bajo permisos
reales y continuidad de transcripción/traducción.

## Estado de cierre de AURORA 4.7
La pantalla de conversacion incluye original y traduccion automatica
en tarjetas contiguas. Cada frase final de SpeechRecognizer se traduce
sin pedir una accion extra; ML Kit conserva el traductor por par de idiomas
y evita mostrar respuestas viejas. Inicio abre directamente este modulo.
Los textos originales de conversacion se guardan; los sonidos se limitan
a una lista interna de 30 eventos sin nuevos TXT ni PDF.

No se afirma que funcione con precision real sin comprobarlo en un
Galaxy S9+; los modelos de idioma pueden necesitar Internet al principio.
Las betas anteriores y el MASTER 4.0 de main no se sustituyen.

## Retomar desde otro chat
Buscar en GitHub la rama `aurora-sense-precision-2026-11-10`
y el último commit verificado por CI. Consultar el MASTER PDF de
esa revisión, `docs/AURORA_48_COLA_BILINGUE.md` y los registros
de GitHub Actions. No suponer que CI prueba la precisión real
en un teléfono ni que la estética futura ya se implementó.

