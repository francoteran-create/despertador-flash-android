# AURORA — Hoja de ruta consolidada (26/11/2026)
Base de entrega 4.30 beta ARM64: 4bd306c0af4f1f5444d3e5bbd4aee041a92278fb. CI principal 45876668623 exitoso; artefacto 15143974731. Compilar no equivale a validar el equipo físico. Mantener main estable sin cambios y no sustituir APK verificada por otra no verificada.

## Alcance aprobado
No desarrollar radar, orientación ni localización direccional de sonidos. No incluirlo en futuros planes. Priorizar fiabilidad, experiencia visual y accesibilidad. Los avatares se incorporan después de estabilizar funciones; seleccionar una vez al iniciar, conservar en almacenamiento local y permitir cambio en Perfil; mascota central y estética neón aprobada. Retirar nube previa, auriculares musicales y símbolos de discapacidad de los avatares; audífonos auditivos discretos. Usar recursos originales aprobados, no recrear todas las ilustraciones. Los botones deben ser controles reales accesibles, no elementos dibujados en una captura.

## P0 — Cierre funcional (bloquea release estable)
- Galaxy S9+: primera apertura y permisos, alarma programada con pantalla bloqueada, LED intermitente, vibración, audio opcional con diez tonos, volumen y duración, parar y posponer. Validar reinicio, ahorro de batería, concurrencia de alarmas y temporizador. Usar diagnóstico 4.30 sin confundir respuesta Android con efecto físico. No depender de AURORA como única alarma hasta validar.
- Música háptica: prueba motor, ritmo ambiental, sensibilidad y fuerza, silencio, salida, cancelación de inicio, prioridad de alarma. No almacenar audio.
- Sonidos ambientales: detectar múltiples eventos, evitar falsas atribuciones, medir aciertos, omisiones, falsos avisos por hora y latencia con banco de sonidos; validar avisos y monitoreo en segundo plano. Pantalla horizontal negra, onda zigzag multicolor, últimos 30 eventos, pictogramas, vibración y LED voluntarios.
- Conversación: módulo separado de sonidos; transcripción en vivo, vista horizontal negra de texto blanco, traducción únicamente cuando se solicite, guardar al detener con fecha/hora, registros y exportación. Validar errores, idiomas y sesiones largas.
- Revisar SOS, AURORA Voz, Mostrador, notificaciones al reloj y perfil; comprobar funcionamiento y permisos según cada dispositivo. SOS ubicación opcional; no sustituye servicios de emergencia.
- Respaldo/restauración de alarmas: incorporar ajustes por alarma de flash, vibración, sonido, volumen, tono y duración sin romper copias anteriores. Comprobar instalación/actualización.
- Revisión de diseño: mural de alta resolución, contraste, tamaños, accesibilidad, orientación. Conservar navegación actual.

## P1 — Investigación y prototipos aislados
AURORA LEARN: partir del modo de evaluación de SenseActivity ya existente (ACERTÓ/NO COINCIDE y REGISTRAR SONIDO NO DETECTADO). No presentar feedback como exactitud medida ni decir que ya entrena un modelo. Explorar muestras explícitamente autorizadas, representación de audio en el dispositivo, clasificación personalizada, separación de validación y entrenamiento y eliminación de muestras a voluntad. Aprendizaje activado por el usuario, sin cambiar automáticamente clases críticas ni prometer detección fiable.
AURORA INTELLIGENCE: interpretar secuencias de detecciones con incertidumbre, conservar etiquetas y evidencia, evitar deducir causas no confirmadas. Distinguir comunicación textual de interpretación ambiental.
AURORA MUSIC: comparación de detección de energía/graves, latencia y patrones hápticos; mantener compatibilidad con motores sin amplitud.
AURORA WATCH: probar notificaciones existentes, luego integración Wear OS si es útil; no garantizar compatibilidad universal.
AURORA COMPANION: cuando P0 esté consolidado, integrar avatares en inicio y Perfil y después asistencia visual. Entrenamiento de pronunciación/voz como investigación posterior, con consentimiento y validación; no afirmar eficacia clínica.

## Referencias externas, aún no importadas
- ProtoSound (MIT): https://github.com/makeabilitylab/ProtoSound — personalización con muestras; auditar implementación, pesos y dependencias.
- AdaptiveSound: https://github.com/SoundabilityLab/AdaptiveSound — retroalimentación adaptativa; verificar licencia específica del código y modelo antes de reutilizar.
- SoundVisualizer (Apache 2.0): https://github.com/SOVIS-TEAM/SoundVisualizer-Android — patrones visuales y hápticos; captura audio de reproducción, distinta de micrófono ambiental. No adoptar captura de pantalla ni funcionalidad direccional.
- BeAware: https://github.com/BeAware-DeafAssistant/mobile — referencia de experiencia accesible, código iOS, sin portado directo.
- SoundWatch: https://github.com/SoundabilityLab/SoundWatch — referencia para reloj; comprobar licencias y compatibilidad.
Registrar licencia, revisión exacta, dependencias y limitaciones antes de reutilizar código. Preferir prototipos independientes y revisión por herramientas especializadas/Codex si disponible. Nada de estos repos se ha integrado todavía.

## Entregas
Cada incremento de código: pruebas de regresión y Java, CI ARM64, APK + MASTER PDF juntos en ZIP con commit exacto. Mantener estado separado de implementación, CI y pruebas físicas. No abrir release estable hasta cerrar P0 y documentar resultados. Los pendientes se mantienen hasta verificación real, no se cierran por compilar.
