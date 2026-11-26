# AURORA 4.11 — recuperación de micrófono y descarte de detecciones antiguas

## Qué se cambió y por qué
La beta anterior podía interrumpir definitivamente la transcripción después de
seis errores temporales de Android, aunque hubiera sido posible seguir intentando.
Los callbacks de YAMNet del clasificador anterior también podían llegar tarde
después de una reconexión y confundirse con las detecciones nuevas.

### Transcripción
- `SpeechRetryController` conserva un reintento escalonado para errores
  temporales (audio, red, servicio ocupado, demasiadas solicitudes, error del
  cliente). Pasa de demoras cortas a un techo de 30 segundos; ya no devuelve
  -1 solo por haberse agotado los intentos.
- Una sesión con éxito reinicia el contador. El silencio/no coincidencia
  no se contabiliza como un error transitorio.
- Errores de permisos de micrófono e idioma no disponible se siguen
  tratando como fallas que deben comunicarse al usuario.
- La aplicación no garantiza una escucha literalmente continua: el
  proveedor Android puede detener SpeechRecognizer y el cambio de
  Activity puede cerrar la captura deliberadamente por privacidad.

### Reconocimiento de sonidos
- `SoundRecoveryPolicy` incrementa de 3 a 5 intentos escalonados:
  0,7 / 1,6 / 3,5 / 7 / 12 segundos. No intenta para siempre
  cuando una falla es persistente y debe mostrar que dejó de escuchar.
- `SenseActivity` y `AuroraSoundMonitorService` manejan un token
  `activeClassifier` distinto en cada intento. Cuando se reemplaza
  el clasificador, sus callbacks atrasados ya no actualizan el listado
  de sonidos, el umbral de confirmación ni las alertas.
- En segundo plano, si alguien detuvo el monitor mientras quedaba un
  evento en cola, el evento pendiente no se agrega posteriormente.
- Los avisos y mensajes indican hasta cinco intentos; al fallar todos,
  no se presenta el teléfono como si continuara detectando.

## Pruebas automatizadas
- `SpeechRetryControllerTest`: se reintenta más de 80 veces sin ciclo
  de CPU rápido, con espera de hasta 30 s; reseteo tras éxito.
- `SoundRecoveryPolicyTest`: cinco reintentos y parada explícita.
- `test_recovery_safety_contract.py`: control de callbacks antiguos
  dentro y fuera de pantalla, y ausencia de traducción automática.
- CI: Java/Gradle, tests Python, APK arm64-v8a, paquete independiente,
  permisos y firma de depuración.
- **No hay mediciones físicas todavía** de tasa de aciertos,
  duración de audio, gasto energético ni estabilidad real en S9+.

## Se conserva de AURORA 4.10
- Traducción únicamente al tocar TRADUCIR ÚLTIMA FRASE.
- Pantalla horizontal negra con palabras blancas y flechita de regreso;
  se abre con icono de celular y dos flechas.
- Zigzag multicolor luminoso, derivado de muestras reales del mismo
  micrófono que utiliza YAMNet, sin grabar sonido.
- Historial de conversaciones y últimos 30 eventos ambientales en lista.
- Flash optativo y vibración sujetos a permisos y pruebas físicas.

## Cómo probar en el Galaxy S9+
1. Instalar AURORA 4.11 BETA ARM64 sin borrar las versiones anteriores.
2. Abrir CONVERSACIÓN y hablar con pausas. Verificar que después de
   varias pausas o cortes temporales vuelve a mostrar la escucha.
3. Probar una frase, pulsar TRADUCIR solo cuando se necesite; confirmar
   que nunca traduce por sí sola.
4. Abrir SONIDOS, escuchar varios ruidos y observar la lista y el
   zigzag en horizontal. Si surge una reconexión, no debería generar
   eventos extra ni quedarse mostrando alertas de un detector apagado.
5. Probar vibración/flash y comprobar notificación de fallo del
   detector si éste deja definitivamente de responder.
6. Registrar un diagnóstico técnico SIN AUDIO si algo falla.

## Siguiente trabajo
- Medir cortes reales del proveedor de voz Android en S9+ y evaluar
  otro motor de voz con escucha más prolongada.
- Medir precisión YAMNet en timbres, ladridos, golpes, bocinas, etc.;
  calibrar umbrales con casos reales y evitar falsos positivos.
- Optimizar consumo, iconografía por sonido y estética futura.
- Preparar una versión de producción solo después de superar pruebas
  del teléfono y revisión de privacidad.

**Entrega:** únicamente AURORA_4_11_ARM64.apk y
AURORA_4_11_MASTER_ESTADO_Y_PLAN.pdf. La rama main estable no se fusiona.
