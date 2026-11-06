# AURORA 1.0 — Contrato de cierre funcional

Estado de revisión: código acumulativo de la rama main, MASTER interna 4.0. Este documento NO declara que la app esté validada en hardware.

## Alcance fijo de V1
1. Clasificación local de sonidos YAMNet. El reconocimiento es probabilístico y no constituye un detector certificado.
2. Escucha explícita en segundo plano, con servicio de primer plano, indicador permanente, botón detener y errores visibles.
3. Avisos del entorno configurables por categorías y sensibilidad, con vibración y notificaciones. Sin inferir causas o peligros no confirmados.
4. Conversaciones a texto por el SpeechRecognizer disponible en Android; reintentos limitados y fin explícito ante errores.
5. Traducción de frases por modelo ML Kit (puede requerir descarga inicial).
6. Alarmas repetidas, pospuestos de 10 minutos y temporizadores con recuperación.
7. Mensajes de comunicación visual, perfil, registros locales exportables.
8. Asistente de IA visual local *solamente a demanda*: seleccionar foto o iniciar cámara, clasificar objetos y leer texto de la imagen. Sin vigilancia de cámara, sin vídeo permanente, sin identificación facial ni subida de foto.
9. Consulta local de sonidos registrados y de las últimas frases realmente transcritas, sin enviar audio ni textos a servidores. Es recuperación/extracción de registros, NO un asistente generativo de lenguaje.
10. Confirmación independiente por sonido, intervalos de aviso sin reuso de detecciones antiguas, guardado fuera del hilo de inferencia y protección contra callbacks de sesiones detenidas.

## Mejoras integradas en la MASTER interna 4.0
- Detecciones en vivo y segundo plano usan la misma política de confirmación de sonidos y categorías independientes.
- Los sonidos detectados en primer plano utilizan un historial compatible con la consulta posterior.
- El reconocimiento del micrófono queda separado del guardado de registros y notificaciones para evitar bloqueos del clasificador.
- Se recuperan las últimas frases reconocidas de una conversación; no se inventa una síntesis.
- Las fotografías se leen con su orientación EXIF y solo bajo demanda.
- La prueba manual del despertador no hereda alarmas antiguas; detener una alarma no cancela las otras.
- La verificación automatizada de código no reemplaza pruebas de cámara, micrófono, notificaciones y batería en teléfonos reales.

## Fuera de alcance V1
- Monitorización visual continua, reconocimiento de personas, predicción de peligros o identificación de intenciones.
- Garantizar detectar incendios, alarmas de humo, emergencias o eventos críticos: no sustituir dispositivos certificados.
- Agente generativo conectado a un proveedor externo sin servicio seguro, controles de privacidad, costos definidos y autorización del usuario.
- Rehacer estética, personaje, animaciones o diseño general.

## Condiciones de aceptación antes de distribuir a usuarios
- [ ] Samsung Galaxy S9+: instalación y apertura sin cierres, prueba en Android real.
- [ ] Micrófono: inicio/detención, pantalla apagada, volver desde otras aplicaciones y llamada entrante.
- [ ] 1 hora de escucha: registro de temperatura/batería, latidos de YAMNet, fiabilidad de aviso y tasa de falsos positivos.
- [ ] Detecciones simultáneas y distintos niveles de sensibilidad; notificaciones y vibraciones habilitadas/bloqueadas.
- [ ] Sin permiso de micrófono o notificaciones: no simular que el servicio funciona.
- [ ] Despertador en hora establecida, varios días, pantalla bloqueada, reinicio y pospuesto.
- [ ] Temporizador en primer plano, segundo plano, con cambio manual de hora y después de reinicio.
- [ ] Voz: transcribir frases de distinta duración, silencio sostenido, servicio ocupado y permiso revocado.
- [ ] Traducción: español, inglés y portugués con modelo descargado y con ausencia de conexión.
- [ ] Historial: cierre voluntario, cierre inesperado, exportación a carpeta y eliminación individual.
- [ ] Foto voluntaria: galería, cámara, permiso denegado, imagen extensa, OCR y etiqueta imprecisa.
- [ ] Evaluación con usuarios sordos o hipoacúsicos, incluidas pantallas legibles, lenguaje claro y configuración comprensible.
- [ ] Verificación de CI: pruebas unitarias y compilación de ambas ABI satisfactorias.
- [ ] Firma de lanzamiento consistente, política de privacidad, revisión de permisos y restricciones de Play Store.

## Regla de distribución durante desarrollo
Los pushes deben ejecutar pruebas y compilar sin subir APKs como artefactos. Solo ejecutar el proceso de distribución manual cuando el usuario solicite un instalable.

## Nota de arquitectura
En Galaxy S9+ la aplicación puede usar ML Kit/YAMNet y servicios Android disponibles. Un LLM externo para texto o visión general NO debe integrarse incrustando claves privadas en el APK: requiere backend autenticado, política de retención y consentimiento explícito. Las etiquetas de objetos de ML Kit no equivalen a describir una escena con razonamiento humano.
