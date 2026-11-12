# AURORA 4.14 BETA — Checklist breve para Samsung Galaxy S9+

Versión esperada: `4.14-voice-watchdog-beta`.
Paquete: `com.despertadorflash.beta414`. APK ARM64 debug.

## Instalación
- Mantener instalada AURORA 4.13. La 4.14 utiliza paquete separado
  y NO importa automáticamente sus alarmas ni sus conversaciones.
- No habilitar dos sesiones de escucha de AURORA al mismo tiempo.
- Probar sin usar la app como dispositivo certificado de emergencias.

## Conversación
1. Entrar a CONVERSACIÓN y hablar en español durante varios minutos;
   comprobar que cada frase final aparece y se conserva al detener.
2. Dejar el teléfono silencioso más de 50 segundos: puede reiniciarse
   la sesión del reconocedor. Verificar aviso de reconexión y recuperación.
3. Hablar luego de la pausa y comprobar que se agregan frases nuevas
   sin duplicar las antiguas.
4. Pulsar TRADUCIR ÚLTIMA FRASE solamente cuando se quiera traducir;
   nunca debe traducir por su cuenta.
5. Abrir la vista horizontal negra, volver sin interrumpir la escucha
   y después detener desde vertical.
6. Apagar y encender Wi-Fi, probar con pantalla apagada, una llamada
   entrante y volver a la app. No presumir que escucha en ese intervalo.
7. Pulsar COPIAR DIAGNÓSTICO (SIN AUDIO) y revisar el contador
   de reinicios; solo contiene cifras técnicas y códigos de error.

## Sonidos y alarmas
- Confirmar que timbre, ladrido, bocina y golpe se registran
  conservadoramente sin duplicados evidentes.
- Validar el zigzag con silencio y sonidos fuertes.
- Probar vibración, flash opcional y alarmas individualmente.

## Criterio de aceptación
La CI puede aprobar compilación y tests sin medir precisión ni
consumo. Anotar el resultado de cada prueba física antes de considerar
estable esta beta. El reconocimiento de voz depende de Android y
no garantiza transcripción ininterrumpida.
