#!/usr/bin/env python3
"""Produce EXACTLY two AURORA delivery files: one ARM64 APK and one PDF master.

This runs only after Python regression tests, Gradle unit tests,
APK build, permission checks and debug signature verification succeeded.
It never packages source code or an extra 32-bit installer for the user.
"""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    KeepTogether, PageBreak, Paragraph, SimpleDocTemplate,
    Spacer, Table, TableStyle
)

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"dist"
REPO="francoteran-create/despertador-flash-android"
BRANCH="aurora-sense-precision-2026-11-10"

def git(*args):
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True).strip()

def paragraph(txt,style):
    return Paragraph(escape(txt),style)

def create_master(path:Path,commit:str,version:str,apk:Path)->None:
    styles=getSampleStyleSheet()
    dark=colors.HexColor("#142237")
    accent=colors.HexColor("#18758A")
    gold=colors.HexColor("#A78543")
    grey=colors.HexColor("#526071")
    styles.add(ParagraphStyle(
        name="AuroraTitle",fontName="Helvetica-Bold",
        fontSize=22,leading=27,textColor=dark,spaceAfter=8))
    styles.add(ParagraphStyle(
        name="AuroraHead",fontName="Helvetica-Bold",
        fontSize=11.5,leading=15,textColor=accent,spaceBefore=8,spaceAfter=4))
    styles.add(ParagraphStyle(
        name="AuroraBody",fontName="Helvetica",
        fontSize=9,leading=12.8,textColor=dark,spaceAfter=4))
    styles.add(ParagraphStyle(
        name="AuroraSmall",fontName="Helvetica",
        fontSize=8,leading=12,textColor=grey,spaceAfter=5))
    h=styles["AuroraHead"];body=styles["AuroraBody"];small=styles["AuroraSmall"]
    story=[
        paragraph("AURORA | MASTER DE CONTINUIDAD",styles["AuroraTitle"]),
        paragraph("Estado compilado, pruebas y próximos pasos | Beta 4.12",small),
        Spacer(1,4),
    ]
    def head(text):story.append(paragraph(text,h))
    def add(text):story.append(paragraph(text,body))
    def item(name,desc):
        story.append(paragraph("- "+name+": "+desc,body))
    rows=[
        ("Versión compilada",version),
        ("Paquete independiente","com.despertadorflash.beta412"),
        ("Repositorio",REPO),
        ("Rama de trabajo",BRANCH),
        ("Commit exacto",commit),
        ("Instalador","Solo ARM64 / 64 bits - Galaxy S9+"),
        ("Firma","APK de desarrollo (debug), no publicacion comercial"),
        ("Última generación UTC",datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M")),
        ("SHA-256 APK",hashlib.sha256(apk.read_bytes()).hexdigest()),
    ]
    table=Table([
        [paragraph(k,small),paragraph(v,small)] for k,v in rows
    ],colWidths=[42*mm,129*mm])
    table.setStyle(TableStyle([
        ("ROWBACKGROUNDS",(0,0),(-1,-1),[colors.HexColor("#F0F3F6"),colors.white]),
        ("GRID",(0,0),(-1,-1),.35,colors.HexColor("#D9E0E6")),
        ("VALIGN",(0,0),(-1,-1),"TOP"),
        ("LEFTPADDING",(0,0),(-1,-1),7),("RIGHTPADDING",(0,0),(-1,-1),7),
        ("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),
    ]))
    story.append(table)
    head("1 | ESTADO REAL DE ESTA ENTREGA")
    add("Compilación automática en GitHub: aprobada ANTES de producir estos archivos. "
        "Las pruebas unitarias Java, regresiones Python, compilación ARM64, firma "
        "de desarrollo y permisos de micrófono/Internet se verifican en CI. "
        "Esto NO equivale a pruebas en un teléfono.")
    add("Pruebas físicas de reconocimiento, calidad de traducción, batería, "
        "flash, vibración y escucha con la pantalla bloqueada: PENDIENTES "
        "en Samsung Galaxy S9+. La rama main estable permanece intacta.")
    head("2 | YA IMPLEMENTADO Y COMPILADO")
    item("CONVERSACIÓN", "transcripción en vivo. La traducción es "
         "VOLUNTARIA: se solicita solo al tocar TRADUCIR ÚLTIMA FRASE.")
    item("TRADUCCIONES PEDIDAS", "la cola secuencial admite hasta "
         "64 trabajos solo cuando el usuario pide una traducción. "
         "El original queda guardado aunque no se traduzca.")
    item("HORIZONTAL 4.10", "icono pequeño de celular con flechas "
         "arriba y abajo. Pantalla completamente negra con palabras "
         "blancas grandes y un único botón pequeño de volver, "
         "sin cerrar la sesión. Se detiene y guarda en vertical.")
    item("ONDA ZIGZAG MULTICOLOR", "línea continua luminosa y "
         "multicolor que responde a las muestras PCM REALES del "
         "micrófono YAMNet. Sonidos más fuertes hacen picos "
         "mayores. Sin barras ni medición calibrada de decibelios.")
    item("MEJORA 4.11 - RECONEXIÓN DE VOZ", "los errores temporales del "
         "reconocedor no agotan definitivamente el reinicio tras seis intentos: "
         "se siguen reintentando con pausas escalonadas hasta 30 segundos. "
         "Los errores permanentes de permisos e idioma sí detienen la escucha. "
         "Android puede cortar el servicio igualmente.")
    item("MEJORA 4.11 - RECONEXIÓN DE SONIDOS", "hasta cinco reintentos "
         "escalonados antes de detener el monitor e informar el fallo. "
         "Se descartan resultados viejos del clasificador anterior "
         "para evitar eventos falsos tras la reconexión.")
    item("SEGURIDAD DE LA SESIÓN", "la escucha de fondo no reinicia por sí "
         "misma después de que la persona la detiene; no registra eventos "
         "atrasados tras la detención. Las pruebas incluyen estas garantías.")
    item("MEJORA 4.12 - ALERTAS SIN DUPLICADOS", "perro y ladrido, "
         "puerta y portazo, voz y conversación comparten un breve "
         "enfriamiento cuando son hipótesis de un mismo ruido. "
         "Timbre, golpes en puerta, bocina y motor permanecen "
         "como alertas independientes.")
    item("MEJORA 4.12 - CONFIRMACIÓN PRUDENTE", "las etiquetas "
         "genéricas PERRO, PUERTA, VEHÍCULO y VOZ HUMANA requieren "
         "al menos dos observaciones separadas. Sirenas y alertas "
         "urgentes conservan su vía rápida, no certificada.")
    item("MEJORA 4.12 - ETIQUETAS CORREGIDAS", "French horn "
         "(instrumento musical) ya no se etiqueta como bocina; "
         "la palabra inglesa cat se reconoce como gato solo "
         "si aparece como palabra completa o en un sonido felino.")
    item("HISTORIAL BILINGÜE", "el original se registra siempre; las traducciones "
         "que terminan antes de cerrar la conversación también se guardan "
         "en el archivo de sesión con referencia a su texto original.")
    item("CONTROL DE ERRORES", "las respuestas tardías y canceladas no pueden "
         "sobrescribir la traducción más nueva. Hay mensajes visibles de "
         "descarga, espera, tiempo agotado y fallo. El texto original se conserva.")
    item("IDIOMAS", "selector de idioma hablado y destino al principio de la "
         "pantalla, original y traducción contiguos. La primera descarga del "
         "modelo ML Kit puede necesitar Internet.")
    item("SONIDOS", "lista local de las últimas 30 detecciones con hora, "
         "sin nuevos archivos TXT ni PDF de sonidos. El evento 31 sustituye "
         "al más antiguo.")
    item("ALERTAS", "vibración y flash LED optativo para categorías elegidas; "
         "flash apagado de fábrica, requiere permiso y se limita la repetición.")
    item("OTROS", "alarmas y temporizador, respaldo de preferencias V2 compatible "
         "con V1, recuperación del detector YAMNet, y consulta de conversaciones.")
    story.append(Spacer(1,8))
    head("3 | LIMITACIONES QUE SIGUEN ABIERTAS")
    item("VOZ", "Android SpeechRecognizer puede pausar o interrumpir sesiones. "
         "La traducción solo se realiza si la persona la solicita; "
         "no se garantiza escucha continua sin interrupciones.")
    item("COLA", "admite hasta 64 solicitudes para no agotar memoria. Si se "
         "llena, la transcripción original queda guardada, pero la traducción "
         "sobrecargada puede fallar.")
    item("CIERRE", "si una traducción todavía está pendiente cuando la persona "
         "detiene la sesión, la frase original queda guardada. Sólo se guarda "
         "una traducción si termina antes del cierre.")
    item("PRIMER USO", "ML Kit necesita disponer de sus modelos; algunos "
         "servicios de transcripción de Android también requieren red.")
    item("MODO HORIZONTAL", "Android gestiona la orientación sin reiniciar "
         "esta Activity. Volver a vertical conserva la captura. Al salir "
         "de la app se cierra la sesión para proteger privacidad.")
    item("PRUEBA REAL", "las nuevas reglas de etiquetas y supresión "
         "de duplicados tienen pruebas automáticas, pero no hay aún "
         "mediciones de tasa real de aciertos, falsos positivos o "
         "tiempos de alerta en el S9+.")
    head("4 | PRÓXIMOS OBJETIVOS (NO COMPILADOS)")
    item("Continuidad", "investigar un reconocedor de voz local o más "
         "resistente a cortes para conversaciones largas.")
    item("Historial", "mejorar visualización frase a frase y recuperación "
         "de traducciones pendientes, si la prueba real lo justifica.")
    item("Calidad", "medir errores de YAMNet y la demora de ML Kit "
         "con pruebas repetibles y un teléfono real.")
    item("Diseño futuro", "vistas negras y zigzag ya están "
         "programadas. Aún quedan la mascota nube, tipografía "
         "manuscrita flotante e ilustraciones de perros, puertas y bocinas.")
    head("5 | PRUEBA SIGUIENTE EN GALAXY S9+")
    for line in [
        "Instalar AURORA 4.12 BETA ARM64 sin desinstalar la anterior.",
        "En CONVERSACIÓN tocar icono celular/flechas: texto grande "
        "en negro; la flecha pequeña permite volver sin cortar.",
        "Verificar que NO traduce automáticamente. Cuando se quiera, "
        "tocar TRADUCIR ÚLTIMA FRASE y comprobar el resultado.",
        "Desde vertical tocar DETENER y abrir CONVERSACIONES GUARDADAS.",
        "En SONIDOS abrir horizontal: línea zigzag más alta y "
        "brillante cuando hay audio real más fuerte.",
        "Probar cortes temporales de Wi-Fi, sonidos y pausas de voz: "
        "la app debe mostrar reconexión sin inventar detecciones.",
        "Probar fallo persistente del detector: debe avisar que se detuvo.",
        "Probar ladridos repetidos, timbres, portazos y bocinas. "
        "Comprobar que un mismo ruido no genere dos alertas "
        "equivalentes, y que sonidos distintos sigan apareciendo.",
        "No activar dos versiones AURORA escuchando a la vez.",
    ]:add("- "+line)
    head("6 | REGLA DE ENTREGA")
    add("Solamente dos archivos para el usuario: "
        "AURORA 4.12 APK ARM64 y este MASTER PDF. No se adjuntan "
        "ZIP con fuentes ni APK de 32 bits; código y historial quedan en GitHub.")
    add("Para retomar: usar el repositorio, rama y commit exactos de la "
        "primera página. No declarar estable ninguna función sólo por "
        "haber superado CI.")
    def footer(canvas,pdf):
        canvas.saveState()
        w,_=A4
        canvas.setStrokeColor(gold)
        canvas.line(18*mm,14*mm,w-18*mm,14*mm)
        canvas.setFont("Helvetica",7)
        canvas.setFillColor(grey)
        canvas.drawString(18*mm,10*mm,"AURORA - 4.11 BETA - Proyecto Cero")
        canvas.drawRightString(w-18*mm,10*mm,"Página "+str(pdf.page))
        canvas.restoreState()
    doc=SimpleDocTemplate(
        str(path),pagesize=A4,leftMargin=19*mm,rightMargin=19*mm,
        topMargin=17*mm,bottomMargin=20*mm,
        title="AURORA MASTER 4.12 - Estado, compilado y planificación")
    doc.build(story,onFirstPage=footer,onLaterPages=footer)

def main():
    commit=git("rev-parse","HEAD")
    version_text=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
    m=re.search(r"versionName\s+['\"]([^'\"]+)['\"]",version_text)
    if m is None:raise RuntimeError("Falta versionName")
    version=m.group(1)
    if version!="4.12-sound-evidence-beta":raise RuntimeError("Versión inesperada")
    built=list((ROOT/"app/build/outputs/apk/debug").glob("*.apk"))
    if len(built)!=1 or "arm64-v8a" not in built[0].name:
        raise RuntimeError("Entrega debe contener exactamente una APK arm64-v8a")
    OUT.mkdir(exist_ok=True)
    apk=OUT/"AURORA_4_12_ARM64.apk"
    pdf=OUT/"AURORA_4_12_MASTER_ESTADO_Y_PLAN.pdf"
    shutil.copy2(built[0],apk)
    create_master(pdf,commit,version,apk)
    if not apk.stat().st_size>10_000_000 or not pdf.stat().st_size>2500:
        raise RuntimeError("Faltan bytes de entrega")
    if not pdf.read_bytes().startswith(b"%PDF-"):
        raise RuntimeError("MASTER PDF no valido")
    print("Tamaños APK/PDF:",apk.stat().st_size,pdf.stat().st_size)
    files=sorted(p.name for p in OUT.iterdir() if p.is_file())
    if files!=sorted([apk.name,pdf.name]):
        raise RuntimeError("Entrega debe tener exactamente 2 archivos: "+str(files))
    print("VERIFICADO: dos entregables",files)
    print("SHA256 APK",hashlib.sha256(apk.read_bytes()).hexdigest())
    print("Git commit",commit)

if __name__=="__main__":
    main()
