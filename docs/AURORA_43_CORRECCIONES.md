# AURORA 4.3 SENSE LIVE / TRADUCCIÓN — CORRECCIÓN DE FALLOS

Fecha: 2026-11-11. Proyecto: `francoteran-create/despertador-flash-android`,
rama `aurora-sense-precision-2026-11-10`, PR #1. MASTER estable `main`
no se modifica. AURORA 4.3 es BETA, no producto validado.

## Síntomas reportados

1. Escuchar sonidos mostraba una sola clase dominante y parecía dejar de
   escuchar. La pantalla no se refrescaba cuando no había un NUEVO evento
   confirmado, aunque el clasificador siguiera emitiendo resultados.
2. El detector podía interrumpirse por errores de AudioRecord o MediaPipe;
   la interfaz de primer plano terminaba la escucha ante el primer fallo.
3. Traducción devolvía un panel aparentemente vacío: compartía la misma
   etiqueta de texto que la transcripción, que podía sobrescribirla; además
   no mostraba de forma prominente la necesidad de descargar el modelo.
4. Consultas de sonidos/conversaciones podían aparecer vacías por reglas
   de lectura del formato de historial obsoleto.
5. No elegir carpeta externa de copias causaba un falso aviso de error pese
   a haberse guardado la sesión en la carpeta privada.

## Cambios introducidos realmente en código

- `SoundLiveWindow.java`: observaciones recientes del clasificador en
  ventana deslizante de 3,5 segundos, hasta cuatro etiquetas ordenadas por
  momento de detección; se descartan etiquetas caducadas. No reemplaza la
  confirmación de alertas (`SoundEventGate`).
- `SenseActivity.java`: los resultados de audio actualizan el panel en
  vivo aunque no generen una notificación, con temporizador de limpieza
  visual independiente de los resultados; muestra «sin sonidos
  identificados ahora» al expirar el último resultado.
- YAMNet: evaluar hasta 40 categorías por ventana (antes, 10) y usar
  umbral inicial de 0,12 (antes, 0,15) tanto en primer plano como en
  escucha continua. Estos números no son porcentajes de precisión.
- `SoundRecoveryPolicy.java`: si el modelo deja de responder durante
  más de 20 segundos, no entrega muestras o comunica un error, se cierran
  el grabador y el modelo y se reintenta con esperas de 700, 1.600 y
  3.500 ms. Después de tres fallos consecutivos, se detiene y avisa.
  Si funcionó por un periodo prolongado el contador vuelve a empezar.
- La escucha continua indica explícitamente «RECONECTANDO» durante
  interrupciones; no mantiene un aviso engañoso de detección activa.
- Traducción: cuadro visible separado del de voz a texto, controles de
  idioma destino ES/EN/PT, traducción de la última frase final o parcial,
  aviso explícito mientras se prepara el modelo, resultado visible
  y errores de descarga o traducción explicados. El reconocimiento de
  voz sigue dependiendo del servicio SpeechRecognizer de Android.
- Mayor pausa entre reinicios de SpeechRecognizer y sugerencias de
  duración de silencios; el proveedor Android puede ignorarlas.
- `SoundDigest` y `ConversationRecall` aceptan formatos actuales de
  historial sin numeración; los tests evitan contar feedback manual
  como si fueran detecciones de audio.
- Guardar historial privado no da error si no se eligió carpeta adicional.

## Instalar junto a las versiones anteriores

- AURORA original: `com.despertadorflash`.
- AURORA 4.2 BETA: `com.despertadorflash.beta`.
- AURORA 4.3 BETA: `com.despertadorflash.beta43`.

Por tanto, la 4.3 permite probar sin desinstalar original o 4.2.
No arrancar dos escuchas de micrófono en apps diferentes a la vez.
Cada aplicación tiene datos y alarmas independientes. No activar
alarmas duplicadas. No se transfieren historiales entre paquetes.
La compilación se firma en modo debug y NO es la release comercial.

## Plan de comprobación en Galaxy S9+

**Escucha del entorno**
- Habilitar únicamente AURORA 4.3 BETA, no otros monitores.
- Con música de fondo, probar timbre, puerta, ladrido y voz; comprobar
  que no quede siempre fija la primera categoría fuerte.
- Dejar silencio por al menos 4 segundos: se debe limpiar la etiqueta
  anterior, sin detener el micrófono.
- Mantener escucha 5-10 minutos y buscar errores visibles; confirmar
  que la reconexión no informe falsamente «escuchando».
- Probar la escucha continua en segundo plano y con pantalla apagada,
  especialmente en el Galaxy S9+ y su ahorro de batería.

**Traducción**
- Abrir CONVERSACIÓN, seleccionar el idioma hablado, decir una frase.
- Elegir el idioma destino ES/EN/PT diferente del origen.
- Tocar TRADUCIR: deben verse ORIGINAL, TRADUCCIÓN y un estado.
- Con Internet disponible la primera vez, permitir descargar el modelo.
- Repetir sin Internet después de descargarlo; verificar la frase,
  errores y cambio de conversación durante una traducción en curso.
- Si no se reconocieron palabras debe decir «traducción pendiente»,
  nunca dejar el campo vacío.

**Historial**
- Detener y verificar que el archivo se guardó incluso sin carpeta
  externa elegida. Si la hay, comprobar exportación.
- Consultar últimas conversaciones y detecciones: deben mostrarse
  registros reales de la 4.3, no retornar vacío por formato viejo.

## Límites de confianza

Los cambios son de código y pruebas automatizadas. No se ejecutaron
en un teléfono físico desde este entorno y no hay datos medidos de
sensibilidad, falsos avisos/hora, batería o calidad de traducción.
AURORA no sustituye detectores de seguridad certificados.

## Entrega y continuidad

Solo un ZIP MASTER integral por versión consolidada: dos APK ARM,
fuentes completas, YAMNet, README, PDF de continuidad y manifest con
commit y hashes. El usuario pidió no fragmentar la entrega.
