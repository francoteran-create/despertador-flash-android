# AURORA - Accesibilidad sonora y visual

Aplicación Android para ayudar a personas sordas e hipoacúsicas a percibir
sonidos del entorno, recibir avisos, usar alarmas y convertir voz en texto.
El reconocimiento de sonidos es probabilístico y NO sustituye dispositivos
certificados de seguridad ni servicios de emergencia.

## Fuentes oficiales y estado

- Repositorio: francoteran-create/despertador-flash-android
- Rama estable de código: `main` (MASTER interna 4.0, versionCode 40).
- Rama de trabajo de precisión: `aurora-sense-precision-2026-11-10`
  (4.2-sense-backup-beta, versionCode 42), revisión en PR #1.
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
La version 4.2 BETA se compila con el identificador Android
`com.despertadorflash.beta` y nombre visible `AURORA BETA`. La
AURORA original utiliza `com.despertadorflash`. Por tanto, la APK
debug 4.2 puede instalarse **EN PARALELO** a la original, sin
desinstalarla ni reemplazar sus datos. Los datos de ambas instalaciones
estan aislados; no se copian automaticamente y las alarmas de cada app
son independientes. Comprobar que no queden alarmas duplicadas activas.

La beta tiene firma de desarrollo efimera. Puede no actualizar otra
beta ya instalada con firma distinta: exportar primero alarmas y
registros de la beta anterior. **No desinstalar la AURORA original.**
La beta 4.2 no restaura por si sola la informacion de una AURORA 2.8
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

La beta 4.2 sigue siendo una version de PRUEBA, no un lanzamiento para
Play Store; hacen falta validacion en Galaxy S9+ y sonidos reales.

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

## Retomar desde otro chat

Abrir este repositorio y el último ZIP MASTER. Verificar el commit del
MANIFEST, la rama y estado del CI. Usar el ZIP como entrega completa, no
inferir cambios desde los mensajes del chat. Continuar siempre desde el
último commit verificado, preservando la referencia estética AURORA 2.8.
