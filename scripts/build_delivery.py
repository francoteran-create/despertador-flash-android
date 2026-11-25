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
        fontSize=11.5,leading=14.5,textColor=accent,spaceBefore=7,spaceAfter=3.5))
    styles.add(ParagraphStyle(
        name="AuroraBody",fontName="Helvetica",
        fontSize=8.7,leading=11.3,textColor=dark,spaceAfter=2.6))
    styles.add(ParagraphStyle(
        name="AuroraSmall",fontName="Helvetica",
        fontSize=8,leading=12,textColor=grey,spaceAfter=5))
    h=styles["AuroraHead"];body=styles["AuroraBody"];small=styles["AuroraSmall"]
    story=[
        paragraph("AURORA | MASTER DE CONTINUIDAD",styles["AuroraTitle"]),
        paragraph("Estado compilado, pruebas y próximos pasos | Beta 4.26",small),
        Spacer(1,4),
    ]
    def head(text):story.append(paragraph(text,h))
    def add(text):story.append(paragraph(text,body))
    def item(name,desc):
        story.append(paragraph("- "+name+": "+desc,body))
    rows=[
        ("Versión compilada",version),
        ("Paquete independiente","com.despertadorflash.beta426"),
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
        ("TOPPADDING",(0,0),(-1,-1),4),("BOTTOMPADDING",(0,0),(-1,-1),4),
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
    item("ESTABILIDAD 4.11", "la voz recupera errores temporales con "
         "pausas de hasta 30 s; los sonidos tienen hasta 5 reconexiones. "
         "Se bloquean resultados de detectores anteriores para no "
         "generar avisos falsos al reconectar.")
    item("SOS 4.26 - CONTACTO DE CONFIANZA",
         "contacto configurable y guardado localmente, mensaje "
         "editable y confirmacion antes de abrir SMS o compartir. "
         "Ubicacion aproximada optativa y reciente; sin permiso o "
         "dato reciente no se adjunta. NO envia automaticamente, "
         "no garantiza entrega ni reemplaza emergencias. "
         "Pendiente prueba fisica.")
    item("MODO MOSTRADOR 4.25 - DIRECCION CORREGIDA",
         "el usuario escribe en la mitad inferior y su mensaje se ve "
         "girado 180 grados en la mitad superior hacia la persona "
         "de enfrente. Cuando esta responde hablando, el usuario "
         "activa ESCUCHAR RESPUESTA y lee la transcripcion abajo "
         "sin sustituir el mensaje escrito de arriba. TTS opcional "
         "para leer el mensaje del usuario. No guarda conversaciones. "
         "Falta probar en telefono real.")
    item("AURORA VOZ 4.23", "nuevo modulo COMUNICAR: texto en español "
         "a archivo WAV local, frases rapidas, vista previa, "
         "WhatsApp u otra app mediante compartir archivo. "
         "La eleccion de contacto y envio requieren confirmacion "
         "del usuario; no se garantiza compatibilidad de terceros.")
    item("NOTIFICACIONES 4.22", "en SONIDOS, un boton prueba un aviso "
         "al telefono sin microfono ni deteccion, con diagnostico de "
         "permisos, canal y ultimo resultado. El aviso real en "
         "primer plano no depende de la opcion smartwatch.")
    item("PERSONAJES 4.21", "dos variantes vectoriales diferenciadas con "
         "expresiones, detalles violetas y accesorios propios. "
         "La seleccion se recuerda localmente y confirma el cambio. "
         "Sigue pendiente prueba fisica y el acabado grafico final.")
    item("HERO 4.20", "nubes 2x2 en esquinas con mascara urbana "
         "graffiti blanco negro violeta al centro, dos estilos "
         "de personaje seleccionables y persistidos localmente. "
         "Son dibujos vectoriales iniciales, no reproducciones "
         "exactas de las imagenes de referencia.")
    item("HERO 4.19", "inicio negro minimalista con nube blanca central "
         "y cuatro accesos en forma de nube: SONIDOS, CONVERSACION, "
         "ALARMA y COMUNICAR. Las pantallas de cada modulo usan "
         "fondo negro. Falta validar en Galaxy S9+.")
    item("RELOJ 4.18", "notificaciones Android ilustradas mediante "
         "dibujos monocromos de puerta, perro, bocina y otros. "
         "El espejo de notificaciones Wear OS es condicional y "
         "no garantiza dibujo a pantalla completa en cada reloj. "
         "Botón de prueba sin micrófono en CONFIGURACIÓN.")
    item("NUBE 4.17", "mascota grafiti negra con trazos blancos que "
         "reacciona a escucha y voz, con pictogramas de puerta, "
         "perro, bocina, música, conversación, sirena y otros. "
         "Solo se muestran dibujos tras una detección confirmada.")
    item("IDIOMAS 4.16", "selectores de conversación y traducción "
         "para español, inglés, portugués, francés, italiano, alemán, "
         "chino simplificado e hindi. Traducción manual con ML Kit; "
         "Android puede no disponer de todos los modelos de voz.")
    item("FLASH 4.15", "el LED de sonidos confirmados ya no depende de "
         "que Android publique una notificación. Los intentos fallidos "
         "no consumen la espera de 12 s y CONFIGURACIÓN muestra "
         "el último estado técnico del flash.")
    item("VOZ 4.14", "si Android deja de responder sin error, un "
         "temporizador recupera el reconocimiento sin descartar "
         "frases finales. Señala las pausas sin captura y registra "
         "cuántos reinicios hizo; requiere prueba física.")
    item("ALERTAS 4.12", "evita alertas duplicadas perro/ladrido, "
         "puerta/portazo y voz/conversación. Mantiene timbre, golpes "
         "y bocina independientes; confirma dos veces hipótesis genéricas.")
    item("ETIQUETAS 4.12", "evita confundir el instrumento French horn "
         "con bocina y palabras que contienen 'cat' con un gato.")
    item("EVALUACIÓN 4.13", "MODO DE VALIDACIÓN voluntario en "
         "CONFIGURACIÓN permite registrar ACERTÓ, NO COINCIDE "
         "y SONIDO NO DETECTADO, agrupados en ocho tipos, "
         "sin conservar audio ni conversaciones.")
    item("RESULTADOS 4.13", "conteos locales por categoría y "
         "porcentajes SOLO de pruebas marcadas. Permite revisar, "
         "copiar y borrar estadísticas sin afectar alarmas, "
         "conversaciones ni últimos 30 sonidos.")
    item("PRIVACIDAD 4.13", "pruebas desactivadas de fábrica; "
         "no se guardan audios, etiquetas individuales, "
         "nombres ni ubicaciones. Hasta 5 eventos evaluables.")
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
    story.append(Spacer(1,4))
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
    item("LED", "el flash de una alarma depende de que Android abra la "
         "pantalla de aviso, incluso con el móvil bloqueado. Falta "
         "probarlo físicamente en S9+; las notificaciones pueden "
         "estar restringidas.")
    item("IDIOMAS PENDIENTES", "probar voz y modelos chino/hindi en "
         "un teléfono real. La interfaz general permanece en español; "
         "suscripción, reloj y mascota final siguen sin implementar.")
    item("SMARTWATCH", "no existe aún una app Wear OS independiente. "
         "Falta ensayar espejo, vibración y presentación de imágenes "
         "en un reloj físico con notificaciones habilitadas.")
    item("DISEÑO 4.23", "se retiraron de la portada el reloj decorativo, "
         "los paneles repetidos de transcripcion y tarjetas. "
         "Las opciones avanzadas siguen en las vistas secundarias.")
    item("PRUEBA REAL", "los porcentajes del nuevo panel reflejan "
         "solo los casos que alguien decida marcar; NO son precisión "
         "científica ni sustituyen ensayos controlados. Falta usar "
         "el S9+ y medir omisiones y falsos positivos con casos reales.")
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
    head("4A | AVANCE ORIENTATIVO POR FUNCION")
    add("Porcentajes de trabajo estimados, NO precision, fiabilidad "
        "ni pruebas de uso: alarmas/temporizador 85%, detector "
        "ambiental 75%, alertas visibles 70%, conversacion 80%, "
        "traduccion voluntaria 75%, AURORA VOZ 80%, "
        "Modo Mostrador 70%, SOS 70%, musica haptica 0%, "
        "preparacion comercial 20%. Validar en dispositivos reales.")
    head("5 | PRUEBA SIGUIENTE EN GALAXY S9+")
    for line in [
        "Instalar AURORA 4.26 BETA ARM64 sin desinstalar la anterior.",
        "En COMUNICAR > SOS, probar contacto, mensaje editable "
        "y vista previa antes de abrir SMS; nunca enviar automaticamente.",
        "SOS: probar SIN ubicacion, con ubicacion autorizada y "
        "permiso denegado. Si no hay posicion reciente debe "
        "preparar texto sin coordenadas. No probar con emergencias.",
        "SOS: abrir COMPARTIR EN OTRA APP; elegir contacto "
        "manualmente y confirmar envio desde mensajeria.",
        "MOSTRADOR: escribir abajo y MOSTRAR TEXTO; el mensaje "
        "debe aparecer arriba girado 180 grados, y no en la zona "
        "de transcripcion del usuario.",
        "MOSTRADOR: hablar otra persona tras ESCUCHAR RESPUESTA; "
        "la transcripcion DEBE mostrarse abajo del lado del "
        "usuario, y NO sustituir el mensaje de arriba.",

        "Tocar ESCUCHAR y hablar; verificar transcripcion visible "
        "y que el microfono se detenga al finalizar.",
        "Escribir respuesta, tocar MOSTRAR TEXTO y DECIR EN VOZ "
        "ALTA; probar teclado, permisos y salida segura.",
        "Abrir COMUNICAR y AURORA VOZ; escribir texto y generar WAV.",
        "Escuchar la vista previa, pulsar WHATSAPP, elegir chat "
        "y verificar que aparezca audio adjunto antes de enviarlo.",
        "Probar COMPARTIR OTRA APP y controlar que nunca mande "
        "mensajes sin confirmacion del usuario.",
        "En SONIDOS tocar PROBAR NOTIFICACION EN TELEFONO sin "
        "microfono. Revisar bandeja, permiso y estado del canal.",
        "Solo si llega la prueba, iniciar ESCUCHAR y reproducir "
        "sonidos reales. Verificar categoria y evento confirmado.",
        "Cambiar entre las dos nubes del HERO y comprobar "
        "distincion visible, descripcion accesible y persistencia.",
        "Comprobar dos nubes arriba y dos abajo, mascara urbana "
        "central con acentos violetas. Tocar avatar y elegir "
        "alternativa, reiniciar y comprobar seleccion.",
        "Comprobar HERO negro con nube blanca central y exactamente "
        "cuatro accesos en nube del lado izquierdo.",
        "Tocar SONIDOS, CONVERSACION, ALARMA y COMUNICAR; probar "
        "navegacion de regreso y fondos negros.",
        "Emparejar un Wear OS y habilitar notificaciones de AURORA; "
        "en Configuración probar dibujo de puerta SIN MICRÓFONO.",
        "Verificar imagen, vibración y tamaño en el reloj real. "
        "No confundir aviso visto en teléfono con éxito en reloj.",
        "En SONIDOS comprobar que la nube permanezca quieta con "
        "micrófono detenido y reaccione tras detecciones confirmadas.",
        "En CONVERSACIÓN hablar y detener: la boca deja de moverse. "
        "Probar animaciones reducidas y batería.",
        "En CONVERSACIÓN seleccionar chino simplificado o hindi como "
        "origen; ensayar la transcripción con hablantes reales.",
        "Elegir otro idioma destino y pulsar TRADUCIR ÚLTIMA FRASE. "
        "Verificar descarga del modelo, resultado y errores visibles.",
        "En Configuración tocar PROBAR FLASH LED 5 SEGUNDOS, revisar "
        "permiso Cámara y comprobar que el LED parpadee.",
        "Activar flash para sonidos y tocar PROBAR ALERTA DE SONIDO. "
        "Consultar último estado del flash en Configuración.",
        "Probar alarma de 1 minuto con pantalla bloqueada; registrar "
        "si se abrió la alerta de Android.",
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
        "Activar MODO DE VALIDACIÓN en CONFIGURACIÓN. Abrir SONIDOS, "
        "marcar 5 aciertos / errores y 2 sonidos omitidos manualmente. "
        "Volver a CONFIGURACIÓN para revisar cifras, copiarlas y "
        "probar BORRAR RESULTADOS sin perder los 30 sonidos.",
        "En CONVERSACIÓN, dejar 45 s en silencio y comprobar que "
        "la reconexión avise sin borrar frases anteriores.",
        "No activar dos versiones AURORA escuchando a la vez.",
    ]:add("- "+line)
    head("6 | REGLA DE ENTREGA")
    add("Solamente dos archivos para el usuario: "
        "AURORA 4.26 APK ARM64 y este MASTER PDF. No se adjuntan "
        "ZIP con fuentes ni APK de 32 bits; código y historial quedan en GitHub.")
    add("Para retomar: usar el repositorio, rama y commit exactos de la "
        "primera página. No declarar estable ninguna función sólo por "
        "haber superado CI.")
    head("7 | COMO CONTINUAR EN OTRO CHAT (COPIAR ESTA INSTRUCCION)")
    add("INSTRUCCION PARA UNA NUEVA CONVERSACION - AURORA")
    for line in [
        "Quiero continuar desarrollando AURORA exactamente desde el estado "
        "documentado en este PDF MASTER adjunto, sin empezar de cero.",
        "Lee integramente este MASTER antes de hacer cambios. "
        "El ZIP de entrega contiene la APK Android ARM64 y este PDF; "
        "el codigo fuente NO esta dentro del ZIP: se conserva en GitHub.",
        "Repositorio GitHub: francoteran-create/despertador-flash-android",
        "Rama de desarrollo: aurora-sense-precision-2026-11-10",
        "Commit de referencia de esta entrega: "+commit,
        "Version de la APK: "+version,
        "Rama estable main: NO MODIFICAR sin autorizacion expresa.",
        "Conectate a GitHub y verifica el codigo, el ultimo commit "
        "de la rama y las pruebas, sin asumir que esta captura "
        "es necesariamente la version mas reciente.",
        "Conserva el trabajo ya programado y las decisiones del MASTER. "
        "No me pidas que repita acuerdos ya documentados.",
        "Prioriza el funcionamiento, la accesibilidad, las pruebas "
        "y la estabilidad. La estetica queda postergada.",
        "AURORA VOZ: texto a WAV local y compartir con WhatsApp u otra "
        "app esta implementado en codigo; falta probarlo en Galaxy S9+.",
        "Modo Mostrador 4.25: el USUARIO escribe abajo y lee abajo "
        "la RESPUESTA transcrita; la persona de ENFRENTE solo ve "
        "arriba el MENSAJE ESCRITO por el usuario girado 180 grados. "
        "No intercambiar los dos destinos. Falta validacion fisica.",
        "SOS 4.26: contacto local, mensaje editable, ubicacion "
        "aproximada optativa y preparacion de SMS o compartir. "
        "Siempre requiere confirmar envio fuera de AURORA.",
        "Pendientes prioritarios: confirmar notificaciones y alertas "
        "reales, validar AURORA VOZ, Mostrador y SOS en telefono, "
        "incorporar musica con vibraciones, fortalecer "
        "conversaciones, alarmas y traducciones.",
        "No implementar por ahora avatar interprete, lectura de labios, "
        "videointerpretacion, subtitulos de audio de otras apps, "
        "diccionario de senas ni mapa de accesibilidad. "
        "La traduccion de senas solo se investiga a futuro.",
        "Trabaja sobre la rama identificada, verifica compilaciones "
        "y corrige errores reales antes de afirmar que algo funciona.",
        "Distingue aprobado por pruebas automatizadas de comprobado "
        "en telefono; no inventes porcentajes de precision.",
        "Entrega un unico ZIP con exactamente una APK ARM64 y un "
        "PDF MASTER actualizado. Incluye estado, pendientes, "
        "pruebas, riesgos, porcentajes estimados y nueva referencia "
        "exacta de commit de GitHub.",
        "Empeza verificando GitHub y el MASTER. Continua con la "
        "primera prioridad pendiente y entrega resultados verificables.",
    ]:
        add("- "+line)
    add("IMPORTANTE: este mensaje puede copiarse del PDF en un chat nuevo. "
        "El asistente debe verificar el repositorio; no puede recuperar "
        "automaticamente trabajo que no se haya guardado.")
    def footer(canvas,pdf):
        canvas.saveState()
        w,_=A4
        canvas.setStrokeColor(gold)
        canvas.line(18*mm,14*mm,w-18*mm,14*mm)
        canvas.setFont("Helvetica",7)
        canvas.setFillColor(grey)
        canvas.drawString(18*mm,10*mm,"AURORA - 4.26 BETA - Proyecto Cero")
        canvas.drawRightString(w-18*mm,10*mm,"Página "+str(pdf.page))
        canvas.restoreState()
    doc=SimpleDocTemplate(
        str(path),pagesize=A4,leftMargin=19*mm,rightMargin=19*mm,
        topMargin=17*mm,bottomMargin=20*mm,
        title="AURORA MASTER 4.26 - Estado, compilado y planificación")
    doc.build(story,onFirstPage=footer,onLaterPages=footer)

def main():
    commit=git("rev-parse","HEAD")
    version_text=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
    m=re.search(r"versionName\s+['\"]([^'\"]+)['\"]",version_text)
    if m is None:raise RuntimeError("Falta versionName")
    version=m.group(1)
    if version!="4.26-sos-contacto-beta":raise RuntimeError("Versión inesperada")
    built=list((ROOT/"app/build/outputs/apk/debug").glob("*.apk"))
    if len(built)!=1 or "arm64-v8a" not in built[0].name:
        raise RuntimeError("Entrega debe contener exactamente una APK arm64-v8a")
    OUT.mkdir(exist_ok=True)
    apk=OUT/"AURORA_4_26_ARM64.apk"
    pdf=OUT/"AURORA_4_26_MASTER_ESTADO_Y_PLAN.pdf"
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
