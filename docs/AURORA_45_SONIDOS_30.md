# AURORA 4.5 BETA — últimos 30 sonidos dentro de la app

## Objetivo aprobado

La pantalla SONIDOS no debe producir informes, TXT ni PDF. Es un
monitor de eventos en tiempo real. Mantiene una lista de hasta
**30 eventos confirmados**, del más reciente al más antiguo,
mostrando hora, fecha y categoría aproximada. Cuando llega el
evento 31 se descarta el número 1, sin crecer indefinidamente.

La pantalla CONVERSACIÓN sigue guardando sus transcripciones
con fecha y hora para abrirlas y repasarlas más tarde; las
conversaciones sí admiten exportación y respaldo.

## Código implementado

- `SoundTimeline.java`: lista acotada de 30 eventos, inmutable,
  orden de llegada descendente; nuevas detecciones ocupan el
  primer lugar y eliminan la más antigua al superar 30.
- `RecentSoundEvents.java`: guarda únicamente etiqueta, puntaje IA
  aproximado y hora en SharedPreferences privadas de AURORA.
  Esta información no es audio ni se escribe en documentos TXT/PDF.
- `SenseActivity.java`: panel «ÚLTIMOS 30 SONIDOS», contador N/30,
  fecha/hora de cada detección, botón «BORRAR LISTA» con confirmación.
  La lista se actualiza en pantalla mientras detecta y se conserva
  entre sesiones de la misma instalación.
- `AuroraSoundMonitorService.java`: las detecciones confirmadas en
  segundo plano alimentan la misma lista; continúan las alertas
  visuales/vibratorias configuradas según permisos. No crea TXT.
- `AuroraAssistActivity.java`: «CONSULTAR MIS ÚLTIMAS
  DETECCIONES» consulta los 30 eventos internos, no archivos.
- Modo de pruebas ACERTÓ/NO COINCIDE u omisión manual: solo pantalla,
  separado del contador de eventos. No crea archivos.
- `RecordsActivity.java`: carpeta principal «CONVERSACIONES
  GUARDADAS», búsqueda y copia de transcripciones. Respaldo
  verificado por hash exclusivamente de conversaciones.
- No se borran los TXT de sonidos generados por versiones
  anteriores: existe la opción «Mostrar archivos antiguos
  de sonidos» en el explorador. Su presencia NO significa
  que la 4.5 produzca nuevos archivos.

## Verificaciones automatizadas

- `SoundTimelineTest`: 38 eventos, retener últimos 30; orden correcto,
  simultaneidad de timbre y ladrido, rechazo de valores inválidos,
  limitación de lista al reabrir.
- `ConversationRecordFilterTest`: archivos de voz solamente
  para el explorador normal; un antiguo archivo de sonidos no
  se elimina al filtrar.
- `SoundDigestTest`: resumen del asistente directamente desde
  los eventos recientes de memoria.
- CI compila dos APK ARM, verifica identificador de beta,
  permisos y firmas, luego genera ZIP MASTER integral.

## Cómo probar en Galaxy S9+

1. Instalar `AURORA 4.5 BETA` como app independiente,
   sin desinstalar la original ni otras betas.
2. Entrar en SONIDOS, hacer sonar timbre, golpear una puerta,
   hablar o reproducir un ladrido. Solo eventos confirmados
   entran en la lista (no cada muestra del micrófono).
3. Verificar orden (más reciente arriba) y contador N/30.
   La lista conserva los eventos al detener y volver a abrir.
4. Presionar BORRAR LISTA: después de confirmar, los eventos
   desaparecen; las conversaciones siguen guardadas.
5. Probar el monitor de sonidos en segundo plano; al volver
   al panel SONIDOS se deben ver sus eventos recientes.
6. Entrar en CONVERSACIÓN, decir una frase y DETENER;
   ir a CONVERSACIONES GUARDADAS para abrirla y releerla.
7. Comprobar que no se produjo un nuevo TXT `SONIDOS`.
   Si hay antiguos, deben estar solamente en el modo
   «Mostrar archivos antiguos de sonidos».
8. Si el detector se corta o la traducción falla, usar
   COPIAR DIAGNÓSTICO (SIN AUDIO) de AURORA 4.4/4.5.

## Estado real y límites

El sistema aún requiere pruebas físicas de duración, batería,
permisos, cámara flash y vibración en el Samsung Galaxy S9+.
Las detecciones son hipótesis probabilísticas de YAMNet;
las pruebas unitarias no demuestran precisión en el mundo real.
El texto de conversaciones puede tener errores del proveedor
Android SpeechRecognizer, que no es un motor continuo garantizado.

La estética de la mascota y tipografía flotante, mostrada como
idea visual, queda para una etapa posterior y NO se aplicó
a esta versión funcional.

**Rama beta**: `aurora-sense-precision-2026-11-10`.
**Main estable** permanece sin cambios.
