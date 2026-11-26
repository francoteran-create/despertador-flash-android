# AURORA 4.23 — VOZ desde texto a audio compartible

## Entregado en código
- Pantalla AURORA VOZ accesible desde COMUNICAR.
- Editor de texto y cuatro frases rápidas para personas sordas.
- Voz local de Android (TTS), español de Argentina por defecto.
- Generación a archivo WAV privado de la app mediante synthesizeToFile.
- Vista previa reproducida localmente con MediaPlayer.
- Compartir a WhatsApp por ACTION_SEND o a otra app por Sharesheet.
- FileProvider sirve una URI de lectura temporal del archivo en caché.
- Nunca accede al chat privado ni envía automáticamente mensajes.
- Mensajes generados no se suben a un servidor de AURORA.
- Errores de voz faltante, archivo vacío y aplicaciones no instaladas.

## Límites expresos
WhatsApp es responsable de abrir su propio selector de contacto,
y puede mostrar el contenido como adjunto de audio, no nota de voz.
No es posible garantizar selección automática de contacto específico
con un archivo adjunto en todos los dispositivos.
Instagram y Facebook Messenger dependen de admitir ACTION_SEND
y el tipo audio/wav en cada versión instalada.
La entrega 4.23 se enfoca en compartir voz; el modo mostrador y el
SOS todavía son tareas pendientes.

## Prueba manual esencial
1. Instalar beta 4.23 en Galaxy S9+.
2. Abrir COMUNICAR → AURORA VOZ.
3. Escribir un mensaje; tocar GENERAR AUDIO.
4. Reproducir el archivo y evaluar pronunciación.
5. Compartir en WhatsApp, elegir un contacto y verificar que
   el audio está adjunto antes de confirmar envío.
6. Probar sin WhatsApp instalado y usar COMPARTIR CON OTRA APP.
7. Cambiar el texto, regenerar, comprobar que no adjunte versión vieja.
8. Probar motor de voz desactivado y texto en blanco.
No considerar validada la entrega sin esas pruebas físicas.
