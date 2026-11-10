# AURORA - Accesibilidad sonora y visual

Aplicación Android para ayudar a personas sordas e hipoacúsicas a percibir
sonidos del entorno, recibir avisos, usar alarmas y convertir voz en texto.
El reconocimiento de sonidos es probabilístico y NO sustituye dispositivos
certificados de seguridad ni servicios de emergencia.

## Fuentes oficiales y estado

- Repositorio: francoteran-create/despertador-flash-android
- Rama estable de código: `main` (MASTER interna 4.0, versionCode 40).
- Rama de trabajo de precisión: `aurora-sense-precision-2026-11-10`
  (4.1-sense-beta, versionCode 41), revisión en PR #1.
- Referencia visual elegida por el usuario: APK AURORA 2.8 proporcionada
  en la conversación. La estética de 2.8 TODAVÍA NO se restauró en 4.1.
  NO presentar un personaje que mueva las manos como diseño aprobado.
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

## Entrega oficial: un único ZIP MASTER

Al ejecutar manualmente el workflow `.github/workflows/android-apk.yml`,
después de superar pruebas y compilación, se construye un único ZIP con:

1. `APK/` - dos APK debug para ARM de la MISMA revisión.
2. `FUENTES/` - todo el proyecto rastreado y modelo YAMNet incluido.
3. `DOCUMENTACION/AURORA_MASTER_CONTINUIDAD.pdf` - estado e instrucciones.
4. `MANIFEST.json` - commit exacto, versión, hash de archivos y estado real.
5. `LEEME_PRIMERO.txt` - guía rápida y restricciones.

El workflow de PR verifica el empaquetado sin publicar APKs. Solo la
ejecución manual publica el archivo como artefacto de GitHub Actions.

**El paquete debug NO ES un APK de lanzamiento firmado para Play Store.**
Para distribuir: pruebas físicas, privacidad, firma de release, validación
con usuarios sordos e hipoacúsicos y métricas de sonido verificadas.

## Retomar desde otro chat

Abrir este repositorio y el último ZIP MASTER. Verificar el commit del
MANIFEST, la rama y estado del CI. Usar el ZIP como entrega completa, no
inferir cambios desde los mensajes del chat. Continuar siempre desde el
último commit verificado, preservando la referencia estética AURORA 2.8.
