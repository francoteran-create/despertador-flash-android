# AURORA 4.16 BETA — Conversaciones internacionales

## Implementación realizada
El selector fijo de tres botones se reemplaza por dos listas desplegables
accesibles (idioma hablado y destino), con estos ocho idiomas iniciales:

| Lengua | Reconocimiento solicitado a Android | Traducción ML Kit |
|---|---|---|
| Español | es-AR | es |
| Inglés | en-US | en |
| Portugués | pt-BR | pt |
| Francés | fr-FR | fr |
| Italiano | it-IT | it |
| Alemán | de-DE | de |
| Chino simplificado | zh-CN | zh |
| Hindi (हिन्दी) | hi-IN | hi |

El idioma que se escucha se transmite a `SpeechRecognizer` y el
destino a `ML Kit Translation`. La traducción sigue siendo **solo
cuando se toca el botón**. Se conserva la indexación anterior de
español/inglés/portugués en las preferencias.

## Límites importantes
- ML Kit soporta traducción para los idiomas listados, pero sus modelos
  deben descargarse y la calidad no está garantizada en todos los pares.
- Que Android acepte una etiqueta de idioma no garantiza que el servicio
  del dispositivo pueda transcribirla. Un S9+ concreto puede requerir
  servicio de voz, conectividad o paquete de idioma.
- **La interfaz completa continúa en español**; esta beta internacionaliza
  primero la función de conversación, no toda la aplicación.
- `zh` de ML Kit representa chino; el reconocimiento solicitado es
  mandarín con etiqueta zh-CN. No se promete traducción entre dialectos.
- Hindi es el idioma solicitado para India, no «hindú» como religión.
- No hay aún lanzamiento comercial, sincronización con smartwatch,
  Play Billing ni personaje animado final en esta beta.

## Pruebas
JUnit verifica ocho idiomas, etiquetas únicas, valores anteriores y que
origen/destino nunca sean iguales. Python comprueba conexión de selectores
con reconocimiento y traducción manual. CI compila ARM64 y genera MASTER.
Se necesitan grabaciones consentidas con hablantes reales de cada idioma
para verificar transcripción y traducciones.

## Próximos pasos
1. Localizar todos los textos UI por recursos Android para es/en/pt/fr/
   it/de/zh/hi, con revisión humana y accesibilidad de fuentes.
2. Pruebas físicas y autorización por usuario para modelos descargados.
3. Integración Wear OS con notificaciones verificables (no prometer todos
   los relojes), y evaluación de sensores, batería y privacidad.
4. Mascota reactiva sin representar un detector activo cuando no escucha.
5. Suscripción mediante Google Play Billing después de pruebas reales,
   términos, privacidad y requisitos de Play Store.
