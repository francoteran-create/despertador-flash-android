# AURORA 4.2 SENSE BACKUP BETA — Guia para instalar y conservar datos

**Estado:** software beta de prueba; no es una version comercial ni fue
validado aun con audios de referencia o en el Galaxy S9+ real.

## Importante: la AURORA original permanece intacta

- AURORA original: identificador Android `com.despertadorflash`.
- AURORA BETA 4.2: identificador Android `com.despertadorflash.beta`.
- La beta aparece como **AURORA BETA**; se instala como segunda aplicacion.
- La beta usa datos y permisos separados, por lo que **no reemplaza los
  registros, alarmas o preferencias guardados en la AURORA original**.
- Si estaba instalada una beta previa con una firma distinta, Android
  puede impedir su actualización. Exportar sus datos antes de desinstalarla.
- No desinstalar la AURORA original ni prometer una migracion automatica
  desde 2.8. Una aplicacion recien instalada no puede acceder por si sola
  a los archivos privados de una aplicacion distinta.

## Instalacion en Samsung Galaxy S9+ (64 bits)

1. Abrir el ZIP MASTER integral y, si la descarga viene de GitHub Actions,
   descomprimir el ZIP exterior para obtener el MASTER interior.
2. Abrir `APK/`, elegir `app-arm64-v8a-debug.apk` y copiarla al telefono.
3. Instalarla autorizando, si Android lo solicita, la instalacion para
   la aplicacion de origen utilizada.
4. Debe verse una nueva aplicacion **AURORA BETA** sin desaparecer
   la AURORA anterior.
5. Comprobar permisos de microfono, notificaciones y flash segun las
   funciones que se usen. Ningun permiso requiere captacion de video continua.
6. Evitar alarmas duplicadas: son dos apps independientes y ambas pueden
   programar alarmas si se activan manualmente.

## Respaldo que ofrece la beta 4.2

En **AJUSTES**, ir a **COPIA DE SEGURIDAD DE CONFIGURACION**:
- **GUARDAR ALARMAS Y AJUSTES EN UN ARCHIVO:** seleccionar la ubicacion TXT.
- **RESTAURAR ALARMAS Y AJUSTES:** seleccionar TXT previo y confirmar
  reemplazo. Se validan valores y checksum SHA-256 antes de aceptar.
- Se respaldan las cinco posiciones de alarmas, dias, horas y preferencias
  visuales/de notificacion, pero **no** perfil, foto, transcripciones,
  registros, temporizadores en curso ni muestras de audio.
- El checksum es solo control de integridad; NO autentica la procedencia.
- Restaurar permisos del sistema si hace falta: no se incluyen en el archivo.

En **REGISTROS**, tocar **ELEGIR CARPETA DEL TELEFONO** y luego
**RESPALDAR Y VERIFICAR TODOS LOS REGISTROS**.
- Cada registro TXT se lee de vuelta y compara contra su original por SHA-256.
- Una copia anterior diferente nunca se sobrescribe silenciosamente.
- El respaldo se procesa fuera del hilo principal y las escuchas que
  ya terminaron se guardan sin bloquear la pantalla.
- La funcion solo respalda los registros creados dentro de esa
  instalacion; no recupera automaticamente historiales de la otra app.

## Pruebas recomendadas en un telefono real

1. Validar portada de estilo 2.8, legibilidad y acceso a cada modulo.
2. Sonidos: timbre, golpe en puerta, golpe en mesa, portazo, ladrido,
   voces y musica; comparar tambien el silencio y sonidos simultaneos.
3. Sensibilidades ALTA, EQUILIBRADA y PRECISA; Precisa exige doble
   confirmacion para sonidos comunes.
4. En modo de validacion voluntaria, marcar `ACERTO` o `NO COINCIDE`
   y registrar los sonidos omitidos, sin grabar audio.
5. Confirmar funcionamiento y fin de la escucha con pantalla bloqueada.
6. Revisar consumo de bateria, permisos y errores; desde AJUSTES se
   puede copiar el reporte tecnico para soporte.
7. Probar despertar, vibracion, flash, temporizador, transcripcion,
   traduccion y guardado del historial.

**AURORA no sustituye detectores certificados de incendios, monoxido
de carbono, sirenas u otros dispositivos de seguridad.** Se necesitan
mediciones con grabaciones reales antes de afirmar tasas de acierto.
