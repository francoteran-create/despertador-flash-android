# AURORA — Despertador accesible

Proyecto gratuito de accesibilidad para personas sordas e hipoacúsicas.

## Estado real (20 de octubre de 2026)
- **Código Android subido:** interfaz de alto contraste, cinco alarmas diarias configurables, vibración, temporizadores de 5/10 minutos y cartel de texto grande.
- **GitHub Actions configurado:** compila APK debug mediante Gradle y publica el artefacto si el build tiene éxito.
- **Sin verificación aún:** no se ha comprobado el resultado del build ni probado en teléfonos reales.
- **Limitaciones actuales:** las alarmas todavía no incluyen repetición semanal configurable, destellos de pantalla/linterna, notificación de pantalla completa ni servicio persistente; la activación de pantalla desde segundo plano puede ser bloqueada por Android. El temporizador requiere mantener la actividad activa.

## Próximas etapas
1. Compilación comprobada y corrección de errores.
2. Alarmas confiables con pantalla bloqueada, reinicio, batería y permisos.
3. Configuración de vibración, luz y accesibilidad sin destellos obligatorios.
4. Transcripción de voz a texto y avisos prioritarios.
5. Integraciones opcionales con timbre, dispositivos vibratorios externos y sensores certificados.

**Seguridad:** AURORA no sustituye detectores certificados de humo, incendio o monóxido de carbono ni servicios de emergencia. No confiar exclusivamente en el teléfono para alertas críticas.
