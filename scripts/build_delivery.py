#!/usr/bin/env python3
"""Package the verified ARM64 APK and current MASTER PDF in one ZIP."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
from tempfile import TemporaryDirectory
from xml.etree import ElementTree
from zipfile import ZIP_DEFLATED, ZipFile
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
BASE_BRANCH="aurora-4.39-gaming"
BASE_APK_BYTES=143562983
AUDITED_BRANCH="aurora-sense-precision-2026-11-10"

def git(*args):
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True).strip()

def paragraph(txt,style):
    return Paragraph(escape(txt),style)

def create_master(path: Path, commit: str, branch: str, version: str, apk: Path, evidence: str) -> None:
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
        paragraph("Estado compilado, pruebas y próximos pasos | Beta 4.40 · Truco personalidad y legibilidad",small),
        Spacer(1,4),
    ]
    def head(text):story.append(paragraph(text,h))
    def add(text):story.append(paragraph(text,body))
    def item(name,desc):
        story.append(paragraph("- "+name+": "+desc,body))
    rows=[
        ("Versión compilada",version),
        ("Paquete independiente","com.despertadorflash.beta427"),
        ("Repositorio",REPO),
        ("Rama de trabajo",branch),
        ("Rama de base",BASE_BRANCH),
        ("Rama auditada de 4.33",AUDITED_BRANCH),
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
    current_bytes = apk.stat().st_size
    reduction = (1 - current_bytes / BASE_APK_BYTES) * 100
    delta = current_bytes - 99_399_478
    add(f"APK ARM64. Esta versión: {current_bytes:,} bytes. Diferencia respecto de 4.39 local: {delta:+,} bytes. Reducción frente a 4.35 local: {reduction:.2f}%.")
    add("Derivados WebP de 720 × 900, calidad 94 con conversión Sharp YUV. Los 32 originales de 1080 × 1350 quedan en el commit base db8ed5d. Comparación digital a 432 × 540 sobre el contenido: PSNR mínimo 34,27 dB y mediana 36,17 dB.")
    add("R8 y resource shrinking se evaluaron por composición. DEX, tabla y recursos sumaban cerca de 13,5 MB antes del piloto, mientras que las bibliotecas nativas ocupaban 69,6 MB. La entrega conserva el empaquetado probado de los motores. Se requiere validación Android de JNI, traducción, OCR y audio antes de activar shrinking.")
    head("1 | ESTADO REAL DE ESTA ENTREGA")
    add(evidence)
    add("Pendiente ejecutar las pruebas instrumentadas y validar el perfil, TalkBack y el hardware en Samsung Galaxy S9+ con Android 10.")
    head("2 | YA IMPLEMENTADO Y COMPILADO")
    item("NUEVO 4.40", "Rival de Truco con frases según jugadas, marcador, cantadas, desenlace e intención. Las cantadas se conservan y las cargadas tienen límite y enfriamiento. Subtítulos y voz opcional respetan la escucha ambiental. Cartas de mesa y mano se ajustan al ancho, con dibujos, valores y palos más legibles y una franja separada para el subtítulo.")
    item("IDENTIDAD 4.40", "Bienvenida con el avatar completo, centrado y sin marco, con aparición suave. El contenedor usa el 90% del ancho y deja espacio simétrico para el saludo. Se preservan la introducción de dos segundos, los 32 avatares, el logo y las reglas de cumpleaños. El género elegido tiene borde, iluminación y marca de selección. El nombre para mostrar tiene campo y vista previa, y se reutiliza en perfil, entrada y juegos con respaldo de apodos y nombres previos.")
    add("Las pruebas JVM cubren los contextos, el límite de intervención y anchos representativos de 320, 360 y 411 dp. Compilaron las pruebas de distribución Android. La revisión visual y táctil en un Galaxy S9+ sigue pendiente.")
    item("NUEVO 4.39", "Bloques agrega caída instantánea, pulsación lateral sostenida, sombra y fijación de 450 ms con renovaciones limitadas. El pulso háptico del juego es opcional e independiente de las alertas.")
    item("TRUCO ARGENTINO", "Mesa vectorial, baraja de 40 cartas, uno contra uno a 15 o 30, Flor opcional, envido y sus variantes, Truco y sus escalones, pardas y mazo. Falta envido usa el tramo de malas o buenas. IA offline de tres niveles con cartas propias y públicas. Subtítulos configurables, voz local opcional y exclusión entre voz y micrófono ambiental. No hay red multijugador.")
    item("AUDITORÍA 4.38", "Ampliación de foto o avatar, copia privada de PDF o imagen de CUD con eliminación voluntaria, división en Mostrador y celebración anual de 4 s con globos, confeti y fuegos vectoriales. La entrada original de 2 s y la bienvenida se conservan.")
    add("Pendiente validar en teléfono la mesa, micrófono y alertas físicas de ambos juegos, importación y borrado de certificados, celebración, rotación, TalkBack y reducción de movimiento. No hay capturas de dispositivo.")
    item("NUEVO 4.38", "La apertura conserva el arte original, con dos segundos de iluminación y barrido en las letras. Saluda con avatar, nombre visible y género de perfil cuando se declaró. Usa la fecha privada del perfil para felicitar el cumpleaños. CUD sigue siendo una declaración opcional local. Mostrador recupera textos al recrearse y libera micrófono y voz al dejar la pantalla.")
    item("JUEGOS 4.38", "Bloques de neón es un juego original en Compose con ritmo fijo, récord local, efectos de líneas, pausa y reinicio. Escuchar reutiliza el monitor YAMNet y muestra los sonidos fuera del tablero; las alertas de vibración y flash conservan la configuración. Pruebas instrumentadas y revisión física pendientes.")
    item("NUEVO 4.37", "La apertura desde el icono muestra AURORA y pasa al inicio automáticamente. El onboarding conserva su decisión anterior. Las alarmas y el temporizador llegan directo a su aviso. El mural vectorial del menú usa grafiti y luz sobre negro. Transcripción muestra el avatar, el texto y un solo control de escucha. Idiomas, traducción, historial y diagnóstico están en Opciones. Se reorganizaron Perfil, Registros, AURORA Voz, SOS, Asistente, Música, Comunicación, Alarmas y Temporizador.")
    add("Pendiente ejecutar en Android las pruebas instrumentadas de apertura, reingreso, lectura, avatar, menús y accesibilidad. No se generaron capturas ni se midió RAM o batería.")
    item("NUEVO 4.36", "Escuchar sonidos en Compose y Material 3, onda basada en el mismo PCM que recibe YAMNet, arte vectorial neón y etiquetas de confianza. La escucha se inicia desde el botón. Se conservan historial, avisos, sensibilidad, diagnóstico y registros. Las animaciones reducen su movimiento según Android y se suspenden fuera de la pantalla activa. Peso y pruebas locales documentados; RAM, batería, latencia, capturas y ejecución física pendientes.")
    item("NUEVO 4.35", "Bienvenida en Compose, personalización local y selector horizontal de 32 avatares originales. El avatar abre el perfil desde el menú. La edición de identidad conserva el perfil y permite cancelar. El catálogo incluye 20 opciones femeninas y 12 masculinas. Pendiente validar las animaciones y la navegación en un Galaxy S9+.")
    item("NUEVO 4.34", "Perfil en Kotlin, Compose y Material 3. Conserva los datos, CUD, foto local, avatares y los motores Java. Incluye animación de retrato vinculada a la escala del sistema, borrador restaurable e importación de foto en segundo plano.")
    item("NUEVO 4.33", "Tarjetas de alarma compactas con ajustes desplegables; configuración general agrupada por categorías; temporizador personalizado de 1 a 1440 minutos. Se conservan funciones y programación anteriores. Pendiente validar disposición visual y funcionamiento físico. Avatares al final.")
    item("NUEVO 4.32", "FFT local 44,1 kHz, graves/medios/agudos, línea de espectro gruesa multicolor y vibración por ataques sobre umbral dBFS configurable. Sin pulsos periódicos artificiales. Ajustes musicales persistentes; sensibilidad, pruebas de notificación e idiomas agrupados en SONIDOS/CONVERSACIÓN. Mural y paleta neón compartidos en menús, lectura horizontal negra conservada. Usuario informa alarma funcional por ahora; no se modifica su lógica. Falta probar música real y respuesta del motor.")
    item("NUEVO 4.31", "Respaldo V3 con los seis ajustes individuales de cada alarma: LED, vibración, sonido, tono, volumen y duración. Importa V1 y V2 conservando el ajuste general de LED; audio desactivado de forma predeterminada. Valida rangos e integridad. Sin cambios de avatar; sin radar.")
    item("NUEVO 4.30", "Recuperación LED limitada a cuatro intentos; diagnóstico de audio y vibración; inicio de música cancelable y prioridad al despertador. El resultado de Android no confirma el funcionamiento físico. Avatares de inicio y perfil se incorporarán en una etapa posterior.")
    item("NUEVO 4.29", "Primer inicio solicita permisos de cámara, micrófono y notificaciones según Android. Guía de alarmas exactas, pantalla completa y batería. Flash individual de alarma prevalece sobre valor inicial general; prueba LED directa y último resultado técnico. No inicia captura al conceder permisos. Ubicación SOS opcional a pedido. Falta confirmar LED físico del S9+.")
    item("NUEVO 4.28", "Inicio urbano con mural fijo, iconos cian y amarillo, diez accesos activos y ajustes. Fondo provisional de baja resolución; validar legibilidad real. El personaje se selecciona en Ajustes.")
    item("ALARMA 4.28", "Servicio de primer plano para efectos independientes de la pantalla, programación registrada, diez sonidos opcionales con vista previa, volumen y duración entre 5 y 180 segundos. Vibración, flash y audio configurables por alarma. Pendiente prueba física.")
    item("MÚSICA 4.28", "Mayor sensibilidad, pulsos de 80 a 180 ms, compatibilidad con motores sin control de amplitud y prueba directa de vibración. Los pulsos no representan melodías literalmente.")
    add("Las notas de versiones anteriores que siguen son antecedentes; el inicio de cuatro nubes fue sustituido por el diseño neón 4.28.")
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
    item("MUSICA HAPTICA 4.27",
         "pantalla SENTIR LA MUSICA en COMUNICAR con captura voluntaria "
         "de microfono PCM, potencia de graves de 80/125/180 Hz, "
         "pulsos limitados a 220 ms entre vibraciones y niveles "
         "SUAVE/MEDIO/FUERTE. Detiene microfono y vibrador al salir. "
         "No guarda ni sube audio. Necesita prueba fisica.")
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
    item("OTROS", "alarmas y temporizador, respaldo de preferencias V3 compatible "
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
    item("LED", "el servicio de alarma intenta controlar el flash sin abrir la actividad. Android puede restringir la cámara y el servicio; falta validar en S9+ con pantalla bloqueada.")
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
        "Modo Mostrador 70%, SOS 70%, musica haptica 70%, "
        "preparacion comercial 20%. Validar en dispositivos reales.")
    head("5 | PRUEBA SIGUIENTE EN GALAXY S9+")
    for line in [
        "Instalar AURORA 4.40 BETA ARM64. Si Android rechaza la actualización por firma, exportar ajustes antes de desinstalar la beta anterior; no tocar la estable.",
        "COMUNICAR > SENTIR LA MUSICA: autorizar microfono; "
        "probar música con graves cercanos y vibración.",
        "MUSICA: elegir SUAVE, MEDIO y FUERTE; verificar "
        "que la vibración se detenga inmediatamente al salir.",
        "MUSICA: probar en silencio, con permiso rechazado y "
        "tras haber activado escucha ambiental en segundo plano.",
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
        "En Ajustes cambiar el personaje y comprobar persistencia.",
        "Verificar mural, contraste, iconos neón, desplazamiento y los diez accesos activos.",
        "Probar alarma con pantalla bloqueada, vibración, flash, diez sonidos, volumen y duración; probar detener y posponer.",
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
    add("Un único ZIP de entrega contiene la APK completa ARM64 y este MASTER PDF actualizado. El código fuente se conserva en GitHub.")
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
        "Rama de desarrollo: "+branch,
        "Commit de referencia de esta entrega: "+commit,
        "Version de la APK: "+version,
        "Rama estable main: NO MODIFICAR sin autorizacion expresa.",
        "Conectate a GitHub y verifica el codigo, el ultimo commit "
        "de la rama y las pruebas, sin asumir que esta captura "
        "es necesariamente la version mas reciente.",
        "Conserva el trabajo ya programado y las decisiones del MASTER. "
        "No me pidas que repita acuerdos ya documentados.",
        "Prioriza el funcionamiento, la accesibilidad, las pruebas "
        "y la estabilidad. Conservar la estética urbana neón aprobada fuera del piloto de perfil.",
        "AURORA VOZ: texto a WAV local y compartir con WhatsApp u otra "
        "app esta implementado en codigo; falta probarlo en Galaxy S9+.",
        "Modo Mostrador 4.25: el USUARIO escribe abajo y lee abajo "
        "la RESPUESTA transcrita; la persona de ENFRENTE solo ve "
        "arriba el MENSAJE ESCRITO por el usuario girado 180 grados. "
        "No intercambiar los dos destinos. Falta validacion fisica.",
        "SOS 4.26: contacto local, mensaje editable, ubicacion "
        "aproximada optativa y preparacion de SMS o compartir. "
        "Siempre requiere confirmar envio fuera de AURORA.",
        "MUSICA HAPTICA 4.27: captura voluntaria de audio ambiental "
        "con AudioRecord, deteccion de energia/graves, pulsos limitados "
        "y tres niveles de intensidad. Sin archivos de audio, "
        "se detiene al salir; falta prueba fisica.",
        "4.29: conservar el asistente de permisos y el flash individual. Comprobar resultado técnico y parpadeo físico del LED.",
        "4.28: conservar el inicio neón y el servicio de alarma independiente; validar los diez sonidos y música háptica en S9+.",
        "Pendientes prioritarios: confirmar notificaciones y alertas "
        "reales, validar AURORA VOZ, Mostrador, SOS y Musica Haptica "
        "en telefono; fortalecer conversaciones, alarmas y traducciones.",
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
        "pruebas, riesgos y nueva referencia "
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
        canvas.drawString(18*mm,10*mm,"AURORA - 4.40 BETA - Proyecto Cero")
        canvas.drawRightString(w-18*mm,10*mm,"Página "+str(pdf.page))
        canvas.restoreState()
    doc=SimpleDocTemplate(
        str(path),pagesize=A4,leftMargin=19*mm,rightMargin=19*mm,
        topMargin=17*mm,bottomMargin=20*mm,
        title="AURORA MASTER 4.40 - Estado, compilado y planificación")
    doc.build(story,onFirstPage=footer,onLaterPages=footer)

def test_evidence() -> str:
    python_log = (ROOT / "build/python-tests.log").read_text(encoding="utf-8")
    python_total = re.search(r"^Ran (\d+) tests? in ", python_log, re.MULTILINE)
    if not python_total or not python_log.rstrip().endswith("OK"):
        raise RuntimeError("Falta el resultado exitoso de las pruebas Python")
    reports = list((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    suites = [ElementTree.parse(report).getroot() for report in reports]
    java_total = sum(int(suite.attrib["tests"]) for suite in suites)
    if not java_total or any(int(suite.get(key, 0)) for suite in suites for key in ("failures", "errors", "skipped")):
        raise RuntimeError("Falta el resultado exitoso de las pruebas Java")
    test_apks = list((ROOT / "app/build/outputs/apk/androidTest/debug").glob("*.apk"))
    if not test_apks:
        raise RuntimeError("Falta compilar el APK de pruebas instrumentadas")
    return (
        f"{python_total[1]} pruebas Python y {java_total} pruebas Java superadas. "
        "APK ARM64 con firma de desarrollo verificada. APK de pruebas de interfaz compilada."
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--branch", default=None)
    args = parser.parse_args()
    commit = git("rev-parse", "HEAD")
    branch = args.branch or git("rev-parse", "--abbrev-ref", "HEAD")
    version_text = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    version = re.search(r"versionName\s+'([^']+)'", version_text)
    if not version or version[1] != "4.40-truco-legibilidad-beta":
        raise RuntimeError("Versión de entrega inesperada")
    built = list((ROOT / "app/build/outputs/apk/debug").glob("*.apk"))
    if len(built) != 1 or "arm64-v8a" not in built[0].name or built[0].stat().st_size < 10_000_000:
        raise RuntimeError("Se necesita una APK ARM64 completa")
    with ZipFile(built[0]) as apk:
        prefix = "assets/neon/"
        logo = "interfaz/logo_aurora.png"
        catalog_name = "catalogo.json"
        portraits = json.loads(apk.read(prefix + catalog_name))
        assets = {portrait["archivo"] for portrait in portraits}
        ids = {portrait["id"] for portrait in portraits}
        genders = {portrait["genero"] for portrait in portraits}
        if len(portraits) != 32 or len(assets) != 32 or len(ids) != 32 or genders != {"hombre", "mujer"}:
            raise RuntimeError("El catálogo de avatares HD está incompleto")
        expected = assets | {logo, catalog_name}
        packaged = {name.removeprefix(prefix) for name in apk.namelist() if name.startswith(prefix) and not name.endswith("/")}
        if packaged != expected or any("/referencias/" in name or name.endswith("ABRIR_DEMO.html") for name in apk.namelist()):
            raise RuntimeError("La APK debe llevar exactamente los derivados, el logo y el catálogo, sin maquetas")
        for name in expected:
            if apk.read(prefix + name) != (ROOT / "app/src/main/assets/neon" / name).read_bytes():
                raise RuntimeError("Los bytes empaquetados no coinciden con el original: " + name)
    derivatives = json.loads((ROOT / "scripts/avatar-derivatives.json").read_text(encoding="utf-8"))
    derived = derivatives["avatars"]
    if set(derived) != {Path(asset).name for asset in assets}:
        raise RuntimeError("Los derivados no coinciden con los 32 identificadores del catálogo")
    with ZipFile(built[0]) as apk:
        for asset in assets:
            content = apk.read("assets/neon/" + asset)
            info = derived[Path(asset).name]
            if len(content) != info["bytes"] or hashlib.sha256(content).hexdigest() != info["sha256"]:
                raise RuntimeError("Derivado inesperado en el APK: " + asset)
    evidence = test_evidence()

    OUT.mkdir(exist_ok=True)
    target = OUT / "AURORA_4_40_TRUCO_LEGIBILIDAD_ARM64.zip"
    apk_name = "AURORA_4_40_ARM64.apk"
    with TemporaryDirectory(dir=OUT) as directory:
        staging = Path(directory)
        pdf = staging / "AURORA_4_40_MASTER.pdf"
        create_master(pdf, commit, branch, version[1], built[0], evidence)
        if not pdf.read_bytes().startswith(b"%PDF-"):
            raise RuntimeError("El MASTER no es un PDF válido")
        staged_zip = staging / target.name
        with ZipFile(staged_zip, "w", ZIP_DEFLATED, compresslevel=3) as bundle:
            bundle.write(built[0], apk_name)
            bundle.write(pdf, pdf.name)
        with ZipFile(staged_zip) as bundle:
            if bundle.namelist() != [apk_name, pdf.name] or bundle.testzip() is not None:
                raise RuntimeError("El ZIP de entrega no superó la verificación")
        staged_zip.replace(target)
    print("Entrega verificada:", target)
    print("Commit:", commit)


if __name__ == "__main__":
    main()
