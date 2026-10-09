# Ficha de validación física

Equipo declarado por el propietario: A13.
Fabricante / código SM completo: samsung / SM-A135M (ADB)
Versión Android / API / parche: 14 / 34 / 2026-02-05 (ADB)
One UI: 6.1, confirmada por el propietario; propiedad ADB ro.build.version.oneui = 60100
Firmware incremental: A135MUBSDDZB3 (ADB)
Operador / SIM / PIN de SIM: REGISTRAR LOCALMENTE SIN NÚMEROS PERSONALES
APK en el teléfono / versionCode / huella de firma instalada: NO INSTALADO por este proyecto; no se inspeccionaron paquetes existentes
Estado de Device Owner / Profile Owner: ninguno según `dpm list-owners`
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
- Cuentas/usuarios, administradores activos y elegibilidad real para Device Owner: NO COMPROBADOS. Ausencia de Owner no basta para aprovisionar.

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

## Emergencia

Ruta exacta desde pantalla bloqueada y desde boot:
Prueba de abrir/salir del marcador sin llamada:
Prueba de llamada completa en entorno autorizado, si existe:
Aspectos NO verificados:

## Ejecución y consumo

Tiempo recepción → enforcement con pantalla activa:
Tiempo sin red → bloqueo:
Tiempo sin PONG → bloqueo:
Comportamiento con otra app abierta y proceso controlador muerto:
Comportamiento tras despertar:
Comparación de batería y condiciones medidas:

## Veredicto

NO CERTIFICADO. Cambiarlo solo con evidencia de las puertas aplicables y autorización del responsable. Un modelo/firmware distinto requiere una nueva ficha.
