# AURORA - Accesibilidad sonora y visual

Aplicación Android para ayudar a personas sordas e hipoacúsicas a percibir
sonidos del entorno, recibir avisos, usar alarmas y convertir voz en texto.
El reconocimiento de sonidos es probabilístico y NO sustituye dispositivos
certificados de seguridad ni servicios de emergencia.

## Fuentes oficiales y estado

- Repositorio: francoteran-create/despertador-flash-android
- Rama estable de código: `main` (MASTER interna 4.0, versionCode 40).
- Rama de trabajo de precisión: `aurora-sense-precision-2026-11-10`
  (4.5-sound-list-beta, versionCode 45), revisión en PR #1.
- Referencia visual elegida por el usuario: APK AURORA 2.8 proporcionada
  en la conversacion. La pantalla inicial se restauro desde el codigo
  fuente historico del commit 1c0f41cbd9f44a798890afdfd23103db8d7ab9e9.
  Se elimino el personaje animado de versiones 3.x.
  PENDIENTE: verificacion visual y de interaccion en Galaxy S9+.
- CI: pruebas JUnit y compilación Android de dos ABI. El resultado se
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
La version 4.5 BETA usa el paquete independiente
`com.despertadorflash.beta45`, nombre visible `AURORA 4.5 BETA`.
La app original es `com.despertadorflash`, y las betas previas
(4.2, 4.3 y 4.4) tienen identificadores distintos. Se instala en paralelo
**sin desinstalar las anteriores**, y NO migra sus alarmas o datos de
forma automatica. No mantener dos escuchas de microfono activas ni
activar alarmas duplicadas.

No ejecutar ambos detectores de microfono a la vez: detener la escucha
en la AURORA original antes de probar la beta (Android puede bloquear
el acceso simultaneo al microfono).

Instrucciones de esta beta: `docs/AURORA_45_SONIDOS_30.md`. Diagnóstico de voz y traducción: `docs/AURORA_44_DIAGNOSTICO_REAL.md`.

La beta tiene firma de desarrollo efimera. Puede no actualizar otra
beta ya instalada con firma distinta: exportar primero alarmas y
registros de la beta anterior. **No desinstalar la AURORA original.**
La beta 4.5 no restaura por si sola la informacion de una AURORA 2.8
anterior: el exportador nuevo solo existe en las versiones que lo
incorporan. La copia nueva de configuracion no contiene datos privados
de perfil ni historiales.

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

La beta 4.5 sigue siendo una version de PRUEBA, no un lanzamiento para
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

## Entrega oficial: un único ZIP MASTER

Al ejecutar manualmente el workflow `.github/workflows/android-apk.yml`,
después de superar pruebas y compilación, se construye un único ZIP con:

1. `APK/` - dos APK debug para ARM de la MISMA revisión.
2. `FUENTES/` - todo el proyecto rastreado y modelo YAMNet incluido.
3. `DOCUMENTACION/AURORA_MASTER_CONTINUIDAD.pdf` - estado e instrucciones.
4. `MANIFEST.json` - commit exacto, versión, hash de archivos y estado real.
5. `LEEME_PRIMERO.txt` - guía rápida y restricciones.

El workflow de PR verifica el empaquetado sin publicar APKs. La
ejecucion manual o un commit especial marcado `[MASTER-ZIP]`
publica el archivo como artefacto de GitHub Actions.

**El paquete debug NO ES un APK de lanzamiento firmado para Play Store.**
Para distribuir: pruebas físicas, privacidad, firma de release, validación
con usuarios sordos e hipoacúsicos y métricas de sonido verificadas.

## Entrega beta 4.5 para Galaxy S9+
Elegir APK ARM64 del ZIP MASTER. En Android debe aparecer
`AURORA 4.5 BETA`, con datos propios. Probar texto manual,
habla y sonidos por separado y copiar diagnóstico si falla.
La compilación y tests NO equivalen a validación física.

## Verificacion de beta 4.5 (NO declarada estable)
Cambios y tests JUnit verificados con GitHub Actions. El paquete instala
`AURORA 4.5 BETA` en paralelo con las versiones previas. Pruebas de
YAMNet real, calidad de voz, idioma/modelo de traduccion y consumo
del Galaxy S9+ siguen PENDIENTES. Entrega para diagnostico reproducible,
no para prometer que ya se resolvio la incidencia reportada. No
fusionar en `main` hasta validar. Proxima evaluacion: issue #3,
motor de voz realmente continuo.

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

## Retomar desde otro chat

Abrir este repositorio y el último ZIP MASTER. Verificar el commit del
MANIFEST, la rama y estado del CI. Usar el ZIP como entrega completa, no
inferir cambios desde los mensajes del chat. Continuar siempre desde el
último commit verificado, preservando la referencia estética AURORA 2.8.
