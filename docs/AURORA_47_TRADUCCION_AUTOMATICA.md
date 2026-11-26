# AURORA 4.7 — traducción automática fácil de entender

## Qué se corrigió
Antes el teléfono transcribía y la traducción estaba muy abajo:
para verla había que pulsar TRADUCIR. También había dos entradas
diferentes a la transcripción, una desde la portada que no traducía.

Ahora los accesos CONVERSACIÓN y ABRIR TRADUCTOR conducen a
la misma pantalla. Se elige el idioma original y el idioma
de destino en la parte superior.

Dos paneles consecutivos:
- LO QUE SE DIJO · ORIGINAL: texto reconocido.
- TRADUCCIÓN AUTOMÁTICA: versión en el otro idioma con letra grande.

Cada frase FINAL reconocida por Android se envía automáticamente
al traductor ML Kit, sin que haya que tocar un botón. Las frases
parciales se muestran sólo como originales: Android debe terminar
de reconocerlas para pedir su traducción. No se promete traducción
simultánea palabra por palabra.

## Comportamiento técnico
- ML Kit mantiene el modelo y cliente de cada par de idiomas
  mientras siga seleccionado; no se recrea en cada frase.
- Si la primera descarga está en curso y llega una frase más
  nueva, se conserva la última solicitud para cuando termine.
- Las respuestas antiguas se ignoran usando un contador
  por solicitud. Así una traducción tardía no tapa la nueva.
- Al cambiar el destino se retraduce la última frase reconocida.
- Al cambiar el idioma original se reinicia la escucha y se
  limpian los textos de una sesión anterior.
- Los mensajes de espera o error aparecen en el panel de
  traducción, no como un espacio vacío.
- REINTENTAR TRADUCCIÓN permite recuperación manual si falla,
  pero no es un paso obligatorio.
- La prueba de texto escrito sin micrófono quedó aislada
  en herramientas opcionales, fuera del flujo normal.
- La animación de adorno se oculta en CONVERSACIÓN para priorizar
  texto visible; SONIDOS conserva su estilo y sus 30 eventos.
- Se conservan los TXT de conversación (originales) para releerlos.
  No se guarda audio del micrófono.

## Limitaciones y privacidad
Android SpeechRecognizer no garantiza escucha continua y puede
depender del proveedor externo de reconocimiento. ML Kit requiere
modelos descargados la primera vez; según conectividad la primera
traducción puede tardar o fallar. Almacenar un modelo permite
traducir localmente después de la descarga, pero eso no equivale
a probar funcionamiento real sin conexión. La CI no mide tasa
de aciertos en audio de un Galaxy S9+.

## Pruebas necesarias en Samsung Galaxy S9+
1. Entrar en CONVERSACIÓN. Elegir español como origen e inglés
   como destino. Confirmar que el resultado traducido está
   inmediatamente debajo del original.
2. Decir "Buenos días, ¿cómo estás?" y hacer una pausa.
   La traducción debería empezar sola al llegar el resultado
   final del reconocedor Android, sin tocar TRADUCIR.
3. Decir otra frase. Comprobar que no aparezca una traducción
   de la frase anterior reemplazando la nueva.
4. Cambiar el destino a portugués y comprobar la retraducción.
5. Si falla, probar texto escrito desde herramientas para
   distinguir un problema del micrófono de uno de ML Kit.
6. Detener. Confirmar que las frases originales permanecen
   en CONVERSACIONES GUARDADAS.
7. Probar modo avión sólo después de descargar modelos,
   sabiendo que SpeechRecognizer podría necesitar red.

Esta es una beta, no una versión estable. La experiencia en
S9+ y la precisión real de transcripción/traducción siguen
sin validación física. La rama main estable no se altera.
