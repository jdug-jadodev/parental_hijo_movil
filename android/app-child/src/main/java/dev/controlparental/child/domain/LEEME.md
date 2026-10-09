# Motor local de políticas — H03

`PolicyEngine` calcula una intención a partir de un snapshot inmutable y un reloj
monotónico inyectado. No importa Android, no consulta la fecha civil, no acepta
comandos, no persiste nada y no aplica restricciones.

## Prioridades

Configuración → error seguro → primer desbloqueo Android → mantenimiento local
autorizado → autorización del arranque → red validada → canal autenticado con PONG
vigente → política del padre → aplicaciones permitidas dentro del quiosco.

Las comprobaciones de coherencia de reloj/política nunca conceden acceso por error.
El motor no controla el acceso de emergencia: el sistema debe conservarlo en todas
las ramas; esa ruta se implementará y validará físicamente en H02/H06.

## Tiempo y transiciones

- LOCK permanece bloqueado; ALLOW depende de las condiciones locales.
- LOCK_FOR vence y reevalúa; ALLOW_FOR vence y bloquea.
- Duraciones de 60 a 604800 segundos desde la primera aceptación duradera.
- Se usa diferencia de tiempos, sin sumar un deadline que pueda desbordar Long.
- Los segundos restantes se redondean hacia arriba, sin prolongar el permiso.
- Los límites se cierran al cumplirse 3 s de pérdida de red y 45 s sin PONG válido.
- Mantenimiento dura 5 min, no concede ALLOWED y no detiene la política parental.
- Las duraciones de otro boot no se reinterpretan con el reloj reiniciado.

`reevaluarEnMs` devuelve la próxima transición positiva pendiente; null significa
que no hay transición temporal programable desde este snapshot. H07 programará
esa evaluación sin bucles ocupados ni WorkManager periódico para plazos de segundos.
Los cambios de red, comandos, servicio y vuelta a primer plano también deben reevaluar.

## Responsabilidades posteriores

H04 debe guardar inicio/política/contexto de boot de forma atómica. H09 debe validar
órdenes, sustituir la política y conservar el inicio ante repetición exacta; el motor
solo lee la política que ya fue aceptada y no implementa deduplicación de comandos.
H05 proporciona sesiones de mantenimiento ya autorizadas, ligadas a boot y en RAM.
H10 solo proporciona PONG correlacionado de la conexión autenticada actual y descarta
datos del canal previo al recrear servicio o reconectar.

`modoSolicitado` NO es el estado observado del teléfono. H06/H09 deben aplicar y
comprobar restricciones antes de emitir APPLIED. Una decisión ALLOWED de tests no
prueba que el quiosco sea seguro ni que la app resista muerte de proceso o Doze.
