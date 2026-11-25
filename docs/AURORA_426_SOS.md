# AURORA 4.26 — SOS a contacto de confianza

Alcance: contacto (numero escrito manualmente), mensaje configurable,
persistencia local del numero al tocar guardar, vista previa, confirmacion
explicita y apertura de la app SMS con destinatario y cuerpo preparados.
Alternativa: compartir texto por Android Sharesheet, donde se elige
aplicacion y chat. No se envian mensajes automaticamente.

Ubicacion: casilla DESACTIVADA al abrir; solicita permiso aproximado al
activarla, y solo incorpora una posicion conocida de menos de 5 minutos
de antiguedad. Si falta permiso o posicion reciente, muestra un aviso y
continua SIN ubicacion. No rastrea continuamente.

No es un sistema certificado de emergencias. No reemplaza llamadas a
servicios de socorro ni garantiza entrega de mensajes. El usuario debe
confirmar envio en la app SMS o mensajeria. La app no puede verificar
que se recibio el mensaje.

Pruebas pendientes Samsung S9+: SMS real con contacto voluntario,
compartir alternativa, sin ubicacion, con ubicacion, permisos denegados,
sin app SMS, texto vacio. Evitar pruebas con lineas de emergencias.
