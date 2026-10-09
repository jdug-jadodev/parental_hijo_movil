# Ficha de validación física

Equipo declarado por el propietario: A13.
Fabricante / código SM completo: samsung / SM-A135M (ADB)
Versión Android / API / parche: 14 / 34 / 2026-02-05 (ADB)
One UI: 6.1, confirmada por el propietario; propiedad ADB ro.build.version.oneui = 60100
Firmware incremental: A135MUBSDDZB3 (ADB)
SIM: ausente, según propietario (9 de octubre de 2026). Operador/PIN SIM: no aplican en esta prueba; variantes con SIM/PIN SIM no verificadas.
APK instalado por este proyecto: dev.controlparental.child debug H04 0.1.0 / versionCode 1, más dev.controlparental.child.test corregido (12 tests aprobados); actualización ADB autorizada el 9 de octubre de 2026
Huella SHA-256 del certificado debug: 39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439 (apksigner local C02; actualización normal con la misma firma). Hash de APK instalados actuales en EVIDENCIA_H04.md.
Estado de Device Owner: dev.controlparental.child/.admin.ChildAdminReceiver en usuario 0, DeviceOwner/Affiliated según dpm list-owners; confirmado además por isDeviceOwnerApp en test Android. Sin Profile Owner listado.
Estado de arranque: ro.boot.verifiedbootstate = green; ro.boot.flash.locked = 1. Son propiedades observadas, no una certificación de integridad.
Fecha / responsable autorizado: 8 de octubre de 2026 / propietario del laboratorio, autorización de consultas ADB en esta sesión

## Intento de inventario H00 — 8 de octubre de 2026

El propietario confirmó que el equipo estaba listo. ADB detectó un dispositivo
USB inicialmente en estado `unauthorized`; faltaba aceptar el diálogo RSA de depuración USB.
No se registró el número de serie ni se consultaron propiedades del teléfono.
Sin instalación, aprovisionamiento, cambios de ajustes ni borrado.
En ese primer intento no se comprobaron modelo ni versiones.

## Inventario autorizado H00 — 8 de octubre de 2026

Tras la segunda confirmación del propietario, un único equipo apareció en estado
`device`. Se consultaron únicamente las propiedades enumeradas arriba y
`adb shell dpm list-owners`, que devolvió `no owners`.
No se guardaron serie, IMEI, cuentas, números de teléfono ni contenido del equipo.
Sin instalación, aprovisionamiento, cambio de ajustes, reinicio ni borrado.

Confirmación del propietario — 9 de octubre de 2026:

- One UI visible: 6.1.
- Equipo ya formateado por el propietario, sin datos que necesite conservar. Respondió afirmativamente a la preparación de copia/acceso a cuentas. No se solicitaron ni registraron credenciales.
- Esto no autoriza un nuevo borrado, instalación o aprovisionamiento. El agente no ejecutó el formateo.

Pendientes para las fases posteriores:

- SIM/PIN de SIM y ruta de emergencia: NO COMPROBADOS. No registrar operador ni números si no son necesarios.
- Preflight H01 inicial: 3 registros de servicios Samsung, una cuenta visible según el propietario; procedimiento detenido. Tras preparación presencial: recuento 0, un usuario principal actual 0, y aprovisionamiento realizado. Owner/admin de nuestra app comprobados; ninguna identidad de cuenta guardada.

## Plan de laboratorio posterior

1. Preparación del laboratorio confirmada por el propietario; volver a comprobar precondiciones antes de aprovisionar y no pedir contraseñas ni códigos.
2. Coordinar aparte la instalación de APK/app de pruebas C02 para comprobar Keystore; no activa Device Owner.
3. H01: implementar DPC/launcher mínimo y coordinar su instalación/aprovisionamiento, verificando precondiciones. No borrar automáticamente ante errores.
4. Validar emergencia H02 y recuperación H05 antes de endurecer H06. No retirar ADB ni bloquear Wi-Fi en esta preparación.
5. Mantener NO CERTIFICADO hasta las pruebas de aceptación físicas correspondientes.

## Puertas

G0: PENDIENTE
G1: PENDIENTE
G2: PENDIENTE
G3: PENDIENTE
G4: PENDIENTE

## Evidencias

| Caso | Procedimiento realmente ejecutado | Resultado observado | Evidencia y entorno |
|---|---|---|---|
| H00 (inventario ADB parcial) | Consultas getprop de modelo, Android/API, parche, One UI, firmware y arranque; dpm list-owners | SM-A135M, Android 14/API 34, parche 2026-02-05; no owners | ADB 1.0.41 / Platform Tools 37.0.0; valores arriba; pendientes de preparación presencial |
| H00 (cierre de inventario y plan) | Confirmación presencial comunicada por el propietario | One UI 6.1; equipo ya formateado y sin datos que conservar | 9 de octubre de 2026; inventario no certifica quiosco ni emergencia; campos de fases posteriores siguen NO COMPROBADOS |
| C02 Android | Instalar ambos APK y ejecutar solo CriptografiaAndroidTest con adb -s dirigido al A13 | OK (3 tests), 1.676 s; verificación de fixtures, Keystore no exportable y alias ausente | 9 de octubre de 2026; SM-A135M / API 34 / A135MUBSDDZB3; A56 excluido, sin Owner ni restricciones nuevas |
| H01 preflight | Consultas con adb -s al A13: modelo, Owners, recuento de usuarios/cuentas y usuario actual | Sin Owner; usuario principal único; 3 cuentas. Detenido antes de instalar/aprovisionar | 9 de octubre de 2026; cuentas y nombres no guardados; no se modificó A13 ni A56 |
| H01 aprovisionamiento y tests | Repetir preflight, actualizar ambos APK, ejecutar 3 tests antes de Owner, set-device-owner y 6 tests con exigirDeviceOwner=true | Cero cuentas; Owner real/admin activo; OK (3 tests) y OK (6 tests). Launcher abierto; confirmación visual pendiente | 9 de octubre de 2026; SM-A135M; todo dirigido con adb -s; A56 intacto; sin quiosco añadido |
| H01 pantalla actual y cierre mínimo | Solicitar comprobación de pantalla de preparación, Owner Sí/admin Sí y quiosco NO APLICADO | El propietario responde «perfecto»; confirmación presencial comunicada, no captura automatizada | 9 de octubre de 2026; no prueba bloqueo, emergencia ni UI previa sin Owner |
| H02 diagnóstico Android | Actualizar ambos APK H02 solo en A13 y ejecutar runner exigiendo Owner y credencial Android | OK (7 tests), 1.666 s; credencial y Owner comprobados, launcher abierto. Botón/marcador pendientes | 9 de octubre de 2026; SM-A135M; sin lockNow automático, sin llamadas y A56 intacto |
| H02 ruta básica presencial | Solicitar al propietario botón → keyguard → marcador sin marcar/llamar → salida/desbloqueo → app | Responde «todo correcto»; no comunica diferencias | 9 de octubre de 2026; confirmación presencial comunicada, no captura automática; sin red/arranque/PIN SIM pendientes |
| H-EMG-02 sin internet | Pedir apagar Wi-Fi/datos en A13, SIN modo avión; repetir ruta sin llamar y restaurar conectividad | Propietario responde «correcto»; funcionamiento igual confirmado | 9 de octubre de 2026; aprobado etapa H02 sin quiosco, no prueba llamada/cobertura; A56 intacto |
| H-EMG-01 arranque sin SIM (parcial) | Pedir reinicio manual A13, abrir/salir del marcador antes de PIN Android SIN llamar, desbloquear y comprobar Owner/admin Sí | Propietario responde «correcto» | 9 de octubre de 2026; confirmación presencial, no reinicio ADB; con SIM/PIN SIM pendiente; no prueba persistencia de política |
| H04 primera instrumentación | Actualizar app/test con adb -s solo al A13 y ejecutar runner con Owner/credencial exigidos | 12 tests/4 fallos (1.98 s): ocho aprobados, cuatro de acceso al directorio DP del paquete test fallan | 9 de octubre de 2026; corrección local del aislamiento compilada, no instalada/ejecutada; sin reinicio, endurecimiento ni cambios en A56 |
| H04 repetición corregida | Actualizar SOLO APK test; runner con Owner/credencial exigidos dirigido al único A13 comprobado | OK (12 tests), 1.892 s; cinco H04 aprobados, incluidos AtomicFile/rollback/corrupción en archivos efímeros DP | 9 de octubre de 2026; no se accedió a control-state.json ni se reinició/endureció; A56 intacto; boot/proceso reales pendientes |

## Emergencia

Ruta básica confirmada: botón de la app «Ir a pantalla de bloqueo» → keyguard (encender con botón lateral si se apaga) → «Llamada de emergencia» → salir → desbloquear con credencial Android → volver a app. Propietario no comunicó diferencias ni detalló si hubo apagado/deslizamiento.
Ruta desde boot SIN SIM: propietario confirma «correcto» al abrir/salir de Llamada de emergencia antes de PIN Android, luego desbloquear y volver a la app con Owner/admin Sí. No prueba variantes con SIM/PIN SIM.
Prueba de abrir/salir del marcador sin llamada: propietario confirma «todo correcto» en la ruta básica H02.
Prueba de llamada completa en entorno autorizado, si existe:
Ruta sin internet: propietario confirma «correcto» tras Wi-Fi/datos apagados SIN modo avión, recorrido sin llamar y restauración indicada; etapa H02 sin quiosco.
Aspectos NO verificados: con SIM/PIN SIM, quiosco estricto, curso completo de llamada/cobertura y devolución. G0 completo sigue pendiente.

## Ejecución y consumo

Tiempo recepción → enforcement con pantalla activa:
Tiempo sin red → bloqueo:
Tiempo sin PONG → bloqueo:
Comportamiento con otra app abierta y proceso controlador muerto:
Comportamiento tras despertar:
Comparación de batería y condiciones medidas:

## Veredicto

NO CERTIFICADO. Cambiarlo solo con evidencia de las puertas aplicables y autorización del responsable. Un modelo/firmware distinto requiere una nueva ficha.
