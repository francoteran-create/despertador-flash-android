# Prueba de AURORA 4.16 — Ocho idiomas

Esta versión agrega **idiomas en el módulo CONVERSACIÓN**. No traduce
todavía todos los botones y pantallas del sistema. Versión Android ARM64
independiente de 4.15; conservar versiones anteriores.

1. Instalar AURORA 4.16 BETA sin borrar la 4.15. No escuchar dos
   versiones a la vez.
2. Abrir CONVERSACIÓN. Verificar dos listas de idiomas: hablado y destino.
3. Elegir español hablado y chino simplificado como traducción.
   Hablar español, tocar TRADUCIR ÚLTIMA FRASE y observar el resultado.
4. Probar con hindi como destino y repetir. La primera ejecución de
   ML Kit puede descargar modelos y requerir Internet.
5. Elegir chino simplificado como idioma hablado; comprobar si el servicio
   Android del teléfono reconoce frases. Repetir con hindi hablado.
   Si falla, no declarar que el idioma de voz está soportado por ese móvil.
6. Comprobar francés, italiano y alemán, y que un cambio de origen
   no deje transcripciones de la sesión anterior.
7. Verificar pantalla horizontal, vuelta a vertical, DETENER y consulta
   de las frases guardadas.
8. Revisar batería y conexiones, y no depender del teléfono como
   dispositivo certificado de alarma de emergencias.

Pendientes para lanzamiento comercial: traducción de toda la UI,
Wear OS, mascota animada, Google Play Billing y validaciones reales
de escucha, precisión y privacidad. La suscripción USD 1/mes es una
idea comercial, no una funcionalidad habilitada.
