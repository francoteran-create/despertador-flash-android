# AURORA - Accesibilidad sonora y visual

Aplicación Android para ayudar a personas sordas e hipoacúsicas a percibir
sonidos del entorno, recibir avisos, usar alarmas y convertir voz en texto.
El reconocimiento de sonidos es probabilístico y NO sustituye dispositivos
certificados de seguridad ni servicios de emergencia.

## Fuentes oficiales y estado

- Repositorio: francoteran-create/despertador-flash-android
- Rama estable de código: `main` (MASTER interna 4.0, versionCode 40).
- Rama de trabajo de precisión: `aurora-sense-precision-2026-11-10`
  (4.3-sense-live-translate-beta, versionCode 43), revisión en PR #1.
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
y registrar manualmente sonidos que no fueron detectados. Las marcas se
guardan en registros TXT al detener, sin audio. El analizador local
`scripts/evaluate_sound_feedback.py` resume solo los casos evaluados y
NO calcula ni afirma tasas de precision a partir de muestras voluntarias.
El script tiene tests en `scripts/tests/` ejecutados por GitHub Actions.

Las notificaciones de eventos simultaneos se identifican por etiqueta,
para que un timbre y unos golpes de puerta no se reemplacen dentro de
la misma categoria. Desactivar avisos retira solo esa categoria.

## Instalación de la beta y seguridad de los datos
La version 4.3 BETA se compila con el identificador Android
`com.despertadorflash.beta43` y nombre visible `AURORA 4.3 BETA`.
La AURORA original utiliza `com.despertadorflash` y la anterior 4.2
beta utiliza `com.despertadorflash.beta`. Pueden coexistir en el telefono;
**no desinstales ninguna para probar la 4.3**. Sus datos no se comparten. Los datos de ambas instalaciones
estan aislados; no se copian automaticamente y las alarmas de cada app
son independientes. Comprobar que no queden alarmas duplicadas activas.

No ejecutar ambos detectores de microfono a la vez: detener la escucha
en la AURORA original antes de probar la beta (Android puede bloquear
el acceso simultaneo al microfono).

Instrucciones detalladas: `docs/AURORA_BETA_42_INSTALACION.md`.

La beta tiene firma de desarrollo efimera. Puede no actualizar otra
beta ya instalada con firma distinta: exportar primero alarmas y
registros de la beta anterior. **No desinstalar la AURORA original.**
La beta 4.3 no restaura por si sola la informacion de una AURORA 2.8
anterior: el exportador nuevo solo existe en las versiones que lo
incorporan. La copia nueva de configuracion no contiene datos privados
de perfil ni historiales.

En AJUSTES aparecen los botones `GUARDAR ALARMAS Y AJUSTES EN UN
ARCHIVO` y `RESTAURAR ALARMAS Y AJUSTES`. Exportan/importan un TXT
con checksum SHA-256 y valores restringidos a alarmas, preferencias de
vibracion/flash y avisos. No incluye sesiones, perfil ni audio. La
importacion solicita confirmacion y reprograma las alarmas. El checksum
detecta corrupcion, **no autentica al emisor del archivo**.

En REGISTROS, elegir una carpeta del telefono y tocar `RESPALDAR Y
VERIFICAR TODOS LOS REGISTROS`. Las copias TXT se verifican por hash,
no sobrescriben archivos previos distintos y se procesan sin bloquear
la interfaz. Estos respaldos se guardan en la carpeta elegida.

La beta 4.3 sigue siendo una version de PRUEBA, no un lanzamiento para
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

## Entrega de la beta 4.3 para comprobar en el S9+
Esta version se identifica como AURORA 4.3 BETA, con paquete
`com.despertadorflash.beta43`, diferente de la beta anterior y de la
original. Para instalar en Samsung Galaxy S9+ usar la APK ARM64 del
ZIP MASTER. No desinstalar ninguna instalacion anterior, y no usar dos
microfonos a la vez. En la primera prueba verificar si SONIDOS cambia
ante eventos nuevos y borra los viejos, y si TRADUCCION muestra
ORIGINAL, RESULTADO y ESTADO, incluso al fallar la descarga de ML Kit.

Los resultados de pruebas en telefono, calidad con audio real y
disponibilidad de modelos de traduccion en cada dispositivo estan
pendientes. No se debe declarar AURORA 4.3 comercial ni detector de
emergencias certificado.

## Retomar desde otro chat

Abrir este repositorio y el último ZIP MASTER. Verificar el commit del
MANIFEST, la rama y estado del CI. Usar el ZIP como entrega completa, no
inferir cambios desde los mensajes del chat. Continuar siempre desde el
último commit verificado, preservando la referencia estética AURORA 2.8.
