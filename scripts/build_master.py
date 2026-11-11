#!/usr/bin/env python3
"""Build one self-contained AURORA MASTER ZIP from a *successful* CI revision.

Requires: built APKs for both ARM ABIs, Gradle test step passed before invoking,
the downloaded YAMNet model, and reportlab.
"""
from __future__ import annotations

import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
from datetime import datetime, timezone
from xml.sax.saxutils import escape
from zipfile import ZipFile, ZIP_DEFLATED

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / "dist"
REPO = "francoteran-create/despertador-flash-android"


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def pdf_report(commit: str, version: str, apks: list[Path]) -> bytes:
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="AuroraTitle", parent=styles["Heading1"],
        fontName="Helvetica-Bold", fontSize=20, leading=24,
        textColor=colors.HexColor("#1b435a"), spaceAfter=13,
    ))
    styles.add(ParagraphStyle(
        name="AuroraSection", parent=styles["Heading2"],
        fontName="Helvetica-Bold", fontSize=12, leading=16,
        textColor=colors.HexColor("#1b435a"), spaceBefore=13, spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        name="AuroraBody", parent=styles["Normal"], alignment=TA_LEFT,
        fontName="Helvetica", fontSize=9.5, leading=14, spaceAfter=6,
    ))
    buf = io.BytesIO()
    doc = SimpleDocTemplate(buf, pagesize=(210 * mm, 297 * mm),
                            rightMargin=18 * mm, leftMargin=18 * mm,
                            topMargin=17 * mm, bottomMargin=17 * mm)
    story = []
    def add(txt: str, style: str = "AuroraBody") -> None:
        story.append(Paragraph(escape(txt), styles[style]))
    add("AURORA - MASTER DE CONTINUIDAD", "AuroraTitle")
    add("Informe automatico vinculado a la misma revision que las APKs.", "AuroraBody")
    add("Repositorio: " + REPO)
    add("Commit: " + commit)
    add("Version del proyecto: " + version)
    add("Fecha UTC: " + datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M"))
    add("ESTADO DE VERIFICACION", "AuroraSection")
    add("Compilacion y pruebas unitarias: el empaquetado solo se ejecuta despues "
        "de los pasos de CI de Gradle exitosos en esta ejecucion.")
    add("Pruebas en Galaxy S9+ y audios reales: PENDIENTES. "
        "No hay mediciones reales de precision ni de falsos avisos.")
    add("Firma: APK de desarrollo (debug). No equivale a un lanzamiento "
        "firmado para Play Store. Conservar la clave de firma de lanzamiento.")
    add("APK INCLUIDAS", "AuroraSection")
    for p in apks:
        add(p.name + " - " + str(round(p.stat().st_size / 1048576, 1)) + " MiB")
    add("FUNCIONES Y CAMBIOS INCLUIDOS", "AuroraSection")
    for line in [
        "YAMNet local, deteccion multiclase y confirmacion independiente por sonido.",
        "Diferenciacion entre golpe de puerta, portazo, timbre, perro, ladrido, "
        "voz humana, conversacion y musica; etiquetas conservadoras.",
        "Evidencia especifica frente a generica, con supresion de avisos redundantes.",
        "Menor reuso de observaciones muy proximas; voces opcionales en avisos.",
        "Alarmas, temporizadores, registros, transcripcion y traduccion: "
        "funciones del codigo heredado, no todas verificadas en telefonos.",
    ]:
        add("- " + line)
    add("DECISIONES DEL PROYECTO", "AuroraSection")
    add("Diseno aprobado: AURORA 2.8. La pantalla principal del codigo "
        "de la version 2.8 se recupero desde el commit historico "
        "1c0f41cbd9f44a798890afdfd23103db8d7ab9e9. Se retiro el "
        "personaje animado de 3.x. Falta verificar fidelidad visual "
        "y funcionamiento en telefono real.")
    add("Mantener separados escuchar sonidos, transcribir conversacion y "
        "traducir. Guardar registros al detener la sesion segun la "
        "politica vigente y sin almacenar audio salvo autorizacion.")
    add("VERIFICACION VOLUNTARIA DE EVENTOS", "AuroraSection")
    add("Un modo de prueba opcional permite marcar detecciones como ACERTO "
        "o NO COINCIDE y registrar sonidos omitidos manualmente. Las "
        "marcas se guardan en TXT sin grabar audio. "
        "scripts/evaluate_sound_feedback.py resume solo casos evaluados; "
        "NO mide precision ni recall representativos.")
    add("Las alertas simultaneas de una misma categoria usan etiquetas "
        "independientes para no reemplazarse entre si. Al desactivar "
        "una categoria se limpian sus avisos y permanece el indicador "
        "del microfono activo.")
    add("MODO PRECISA Y RESPALDOS", "AuroraSection")
    add("Modo Precisa: dos observaciones separadas obligatorias para sonidos "
        "comunes, incluso ante un puntaje alto; las sirenas y alarmas de "
        "incendio tienen una via rapida que NO equivale a deteccion certificada.")
    add("Ajustes: exportar e importar TXT con alarmas y preferencias, "
        "checksum SHA-256 y confirmacion antes de restaurar. "
        "Excluye datos personales, foto, registros, transcripciones y audio.")
    add("Registros: copiar TXT a carpeta seleccionada por usuario y "
        "comprobar que su hash SHA-256 coincide con el original, "
        "sin sobrescribir copias antiguas diferentes.")
    add("AURORA BETA 4.2 utiliza paquete com.despertadorflash.beta, "
        "distinto del original com.despertadorflash: ambas apps pueden "
        "coexistir sin desinstalar la AURORA original. "
        "Los datos NO se comparten. Vigilar alarmas duplicadas. "
        "Las APK debug pueden tener firmas efimeras entre compilaciones.")
    add("CORRECCIONES HEREDADAS 4.3 - ESCUCHA Y TRADUCCION", "AuroraSection")
    add("La vista SONIDOS muestra hasta cuatro hipotesis recientes; no "
        "se queda mostrando indefinidamente el ultimo sonido confirmado. "
        "La ventana caduca tras 3,5 segundos y no crea alertas falsas.")
    add("MediaPipe YAMNet evalua hasta 40 resultados por ventana. Si "
        "el modelo o microfono falla, se liberan recursos y se intenta "
        "reconectar hasta tres veces antes de mostrar ESCUCHA DETENIDA. "
        "Durante la reconexion NO se esta detectando sonido.")
    add("El panel TRADUCCION es independiente de la transcripcion. "
        "Permite elegir idioma destino (espanol, ingles y portugues), "
        "ver la frase original, observar el resultado y leer errores. "
        "Descargar el modelo ML Kit por primera vez puede exigir Internet.")
    add("Resumir sonidos y conversaciones permite leer el formato actual "
        "de los registros. Guardar sesiones internas sin elegir carpeta "
        "externa no debe generar un falso error.")
    add("La BETA 4.4 instala com.despertadorflash.beta44, separada de "
        "AURORA original y de la BETA 4.2. No se comparten datos ni "
        "se deben iniciar dos microfonos al mismo tiempo.")
    add("DIAGNOSTICO REAL DE FALLAS - 4.4", "AuroraSection")
    add("Se agregaron INTERNET y ACCESS_NETWORK_STATE: faltaba el permiso "
        "INTERNET en la beta anterior. ML Kit requiere descargar sus modelos "
        "de traduccion la primera vez, y algunos reconocedores Android "
        "de voz necesitan red. No equivale a validar el funcionamiento.")
    add("Sonidos: probar bloques de audio de 500 ms, junto con prioridad "
        "visual de eventos puntuales antes que ruido de fondo musical. "
        "El modelo YAMNet no garantiza identificar sonidos simultaneos.")
    add("Pulsar COPIAR DIAGNOSTICO (SIN AUDIO) para conocer muestras, "
        "respuestas IA, ultima respuesta, reconexiones, codigos de voz "
        "y categoria de error de traduccion, sin frases ni audio.")
    add("TRADUCCION: ahora admite escribir una frase de prueba y "
        "traducirla sin microfono. Permite separar un fallo de Android "
        "SpeechRecognizer de uno de ML Kit. Avisa si no responde tras 30 s.")
    add("Aviso de privacidad: Android SpeechRecognizer puede utilizar "
        "un proveedor remoto; no se garantiza transcripcion continua. "
        "Revisar docs/AURORA_44_DIAGNOSTICO_REAL.md.")
    add("LIMITACIONES Y RIESGOS", "AuroraSection")
    add("YAMNet entrega puntuaciones no calibradas: no son porcentajes "
        "de certeza ni permiten garantizar identificacion perfecta.")
    add("AURORA no sustituye alarmas certificadas de humo, fuego o "
        "monoxido de carbono. Antes de comercializar: evaluar "
        "falsos positivos y omisiones con audios reales, pantalla "
        "bloqueada, interrupciones y consumo de bateria.")
    add("PARA CONTINUAR EN OTRO CHAT", "AuroraSection")
    add("Adjuntar este ZIP completo. Usar codigo fuente de FUENTES y el "
        "commit indicado como punto de partida. Revisar docs/"
        "AURORA_SENSE_PRECISION.md y docs/AURORA_V1_CIERRE.md; "
        "no asumir pruebas fisicas ni lanzamiento.")
    add("No integrar caracteristicas pendientes como si estuvieran terminadas. "
        "Actualizar este PDF y el manifest en cada nueva entrega.")
    doc.build(story)
    return buf.getvalue()


def main() -> None:
    commit = git("rev-parse", "HEAD")
    version_text = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    m = re.search(r"versionName\s+['\"]([^'\"]+)['\"]", version_text)
    if not m:
        raise RuntimeError("No se encontro versionName")
    version = m.group(1)
    model = ROOT / "app/src/main/assets/yamnet.tflite"
    if not model.is_file() or model.stat().st_size < 1_000_000:
        raise RuntimeError("Falta el modelo YAMNet requerido para compilar offline")

    apks = sorted((ROOT / "app/build/outputs/apk/debug").glob("*.apk"))
    if len(apks) != 2:
        raise RuntimeError("Se esperan EXACTAMENTE 2 APKs (ARM32 y ARM64)")

    DIST.mkdir(exist_ok=True)
    target = DIST / ("AURORA_MASTER_INTEGRAL_" + version.replace(".", "_") +
                     "_" + commit[:8] + ".zip")
    checksums = {}
    def add_bytes(archive: ZipFile, name: str, data: bytes) -> None:
        archive.writestr(name, data)
        checksums[name] = sha256(data)

    tracks = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT)
    rel_paths = [Path(s.decode("utf-8")) for s in tracks.split(b"\0") if s]
    readme = (
        "AURORA - MASTER INTEGRAL\n"
        "Origen: https://github.com/" + REPO + "\n"
        "Commit exacto: " + commit + "\n"
        "Version: " + version + "\n\n"
        "APK/: dos instalables ARM de DESARROLLO.\n"
        "FUENTES/: proyecto completo para editar y compilar, con modelo YAMNet.\n"
        "DOCUMENTACION/: PDF de continuidad.\n"
        "MANIFEST.json: hashes de cada archivo y estado de verificacion.\n\n"
        "NO VERSION COMERCIAL. No se han validado pruebas en telefono "
        "ni metricas de precision con audios reales.\n"
        "La portada se recupero del codigo historico 2.8; falta validacion en el telefono.\n"
        "Paquete de instalacion: com.despertadorflash.beta44; no desinstalar las AURORA anteriores.\n"
        "ESCUCHA: panel de sonidos recientes y reconexion automatica acotada.\n"
        "TRADUCCION: panel independiente, selector de idioma, errores visibles.\n"
        "Desde Ajustes se pueden respaldar alarmas/preferencias y desde Registros los TXT.\n"
    )
    with ZipFile(target, "w", compression=ZIP_DEFLATED, compresslevel=6) as out:
        for rel in rel_paths:
            file_path = ROOT / rel
            if file_path.is_file():
                add_bytes(out, "FUENTES/" + rel.as_posix(), file_path.read_bytes())
        # The model is downloaded by CI and normally remains untracked.
        add_bytes(out, "FUENTES/app/src/main/assets/yamnet.tflite", model.read_bytes())
        for apk in apks:
            add_bytes(out, "APK/" + apk.name, apk.read_bytes())
        add_bytes(out, "DOCUMENTACION/AURORA_MASTER_CONTINUIDAD.pdf",
                  pdf_report(commit, version, apks))
        add_bytes(out, "LEEME_PRIMERO.txt", readme.encode("utf-8"))
        manifest = {
            "project": "AURORA",
            "repository": REPO,
            "git_commit": commit,
            "version": version,
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "ci_gradle_unit_tests": "passed_before_package_step",
            "ci_android_debug_apks": "built_before_package_step",
            "physical_device_tests": "pending",
            "real_world_sound_accuracy": "not_measured",
            "approved_ui_reference": "AURORA 2.8",
            "approved_ui_home_source_restored": True,
            "approved_ui_verified_on_device": False,
            "debug_application_id": "com.despertadorflash.beta44",
            "can_install_alongside_original": True,
            "settings_backup_txt": "export_and_import_with_sha256_check",
            "record_backup": "user_folder_copy_verified_sha256",
            "live_sound_window_ms": 3500,
            "sound_classifier_max_results": 40,
            "sound_recovery": "max_three_attempts",
            "translation_visible_panel": True,
            "translation_target_languages": ["es", "en", "pt"],
            "internet_permission": True,
            "speech_recognizer_continuous": False,
            "sound_audio_block_ms": 500,
            "diagnostics_no_audio": True,
            "typed_translation_test": True,
            "apks_signed_for_production": False,
            "files_sha256": checksums,
        }
        out.writestr("MANIFEST.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    with ZipFile(target) as check:
        if check.testzip() is not None:
            raise RuntimeError("ZIP master corrupto")
    print("MASTER creado:", target, "bytes:", target.stat().st_size)


if __name__ == "__main__":
    main()
