# HIJO-LOCAL06 — permitir por tiempo MAIN

14 de octubre de 2026. Base `2582dea`. Estado de preparación: **PREPARADO_FISICO, pendiente de autorización nueva**. No ensayo temporal real todavía.

## Implementación acotada

- Reutiliza modelo/actor/FGS visible LOCAL05, obligaciónDP privada y snapshot Lock Task/boot. Sin prototipo nuevo/importdebug, CP/1/CP/2, contratos, planes, ADR, dependencias ni repositorios externos. Sin permisos/manifest nuevos: ignoreFGS autorizado R1 permanece único/mismo nodo.
- Dos acciones MAIN: PERMITIR POR TIEMPO NORMAL LOCAL / OFFLINE LOCAL. Campo minutos inicialmente vacío; admite ASCII numérico entero1..1440, rechaza vacío/decimal/signo/espacios/overflow/fuera de rango. Ninguna preselección/default/LOCK_FOR/aperturaautomática. Checkbox OFFLINE empieza false, confirma excepción antes de verificar código; recuperación por pregunta no concede uso.
- Elección inmutable capturada antes de solicitar ticket de propósito específico/código fresco; revisioncredencial/boot/epocasesión vigentes. Captura referencia de uso previa a KDF; actor rechaza si cambió, incluida revocación durante preparación. Inicio/deadline elapsedRealtime capturados al aceptar con preparación lista, `multiplyExact`/`addExact`; IO previo a apertura consume tiempo, no reinicia desde readback. Inicio/deadline viven solo RAM.
- Cada autorización válida reemplaza anterior; libre borra temporal, bloqueo cancela ambos. Fondo/login caduco/pantallaapagada/cambio de app no pausa ni cancela uso aceptado. Reboot/proceso recreado nunca reconstruyen permiso ni deadline. DP conserva solo obligación/version/contexto; evento privado TIEMPO añadido al final del enum, sin cambiar ordinals anteriores ni formatoCP/1. Downgrade sin ese evento falla cerrado; checksum no ancla externa contra root/pérdida total.
- NORMAL temporal pierde uso con gracia10s aunque quede tiempo; red vuelve antes cancela gracia sin renovar deadline; después NO abre. OFFLINE temporal no se revoca solo por falta de red, pero vence al mismo deadline. Primer deadline cronológico entre gracia y temporal genera una única revocación; empate se registra TIEMPO. Callback cancelado/viejo no bloquea nueva referencia.
- Expiración guarda/readback de obligación antes de solicitar enforcement oficial. Timer entregado/revocación/solicitud no son LOCKED. Keyguard mantiene bloqueo requerido pendiente y reentrada oficial LOCAL04, sin PIN pedido al vencer ni bypass, sin renovar deadline. Error de almacenamiento/scheduler/FGS/readback no retorna ALLOWED.

## Scheduler y fuentes primarias

Un solo siguiente plazo `ProgramadorPlazo`/`PlanPlazo`, puerto inyectable puro. Duplicados no rearman misma referencia/deadline; cambio cancela anterior; cierre invalida generación/callback antes de desarmar/restaurar. PuertoAndroid `AlarmManager.setExact(ELAPSED_REALTIME_WAKEUP, deadline, tag, OnAlarmListener, Handler)` API>=34. Entrega en Handler y decisión/IO en actor separado; pertenece al FGS vivo, no a Activity. Cancelación de listener explícita, sin polling/cron/wakelock permanente/alarmas repetitivas. No enciende pantalla.

Fuentes HTTP200 consultadas:

- <https://developer.android.com/develop/background-work/services/alarms/schedule>: `OnAlarmListener`/setExact no requiere SCHEDULE_EXACT_ALARM.
- <https://developer.android.com/reference/android/app/AlarmManager>, sobrecarga listener/setExact: Android14 elimina alarmas al salir de lifecycle; pueden cancelarse si no hay componentes vivos. Anclaje en FGS MAIN confirmado necesario. Documenta diferencia con APIs mientras idle. Se elige API34 existente, no añadir permiso para versiones31/33 ni contentarlint.

`setExact` listener **no es** `setExactAndAllowWhileIdle`/alarma reloj. Doze/batería/firmware/callback tardío/Keyguard pueden retrasar enforcement; elapsedRealtime incluye suspensión y reevaluación al despertar/señales oficiales no renueva tiempo. No prometer segundo exacto, supervivencia crash ni pantallaapagada física sin ensayo específico. Si falla registro de alarma, se invalida observador/permiso y pide bloqueo, no fallback libre/Handler/UI.

## Pruebas disponibles ejecutadas

Desde `android/`:

```powershell
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

Primera ejecución: éxito24s,131tareas/28ejecutadas/103UP-TO-DATE. **377debug/322release0fallos/errores**;29casos nuevos compartidos (20modelo/8scheduler/1sesión), release repite, no sumar58. Cubren1min/24hfalseclock/invalids/overflow/deadlineoriginal/reconexiónantes-después/plazoscompetidores/empate/OFFLINEtemporal/últimaacción/referenciavieja/bloquear/sessionfondo/boot-reloj/DPsinreconstrucción/corrupción/readback/Keyguard con TransicionLocal real y puerto fake/listenercancelado/early-late/fallo scheduler. Son JVM/fakes, no adulto ni Androidreal.

Tras añadir UN casoAndroidDP temporalRAM/recreación, mismo comando final: éxito3s/5ejecutadas/126UP-TO-DATE, testsJVM/core/APKapp/analisis-reporte lint UP-TO-DATE, compilaciónAPKAndroidTest ejecutada. **No sumar otra ejecución de casos**. `core-protocol:test` UP-TO-DATE en ambas. `lintRelease`0errores11avisos. lintDebug preexistente prototypeFGS y verificadorFake no reejecutados ni resueltos.

Android DP `UsoRedDpTest` ahora4casos (3previos+1nuevo) UUID/contextoFAKE, sin políticas/credencial real: compilados **NO ejecutados físicamente aún**. NingúnADB/teléfono/grant/cambio de conectividad a esta altura. Manualinspection XML/DEX: únicoignore FGS MAIN R1, sin permisosalarmas/rollbacksrelease; MinutosUso/ProgramadorPlazo/PuertoPlazoAndroid presentes debug/release, rollbackred soloDEBUG y testsausentes APKapp. Inspección cadenasDEX no certifica APK completo. `git diff --check`correcto.

| APK final generado, aún no instalado/ensayado | SHA256 |
|---|---|
| `android/app-child/build/outputs/apk/debug/app-child-debug.apk` | `48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092` |
| `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk` | `c410bd84857c3c5051616952254e1ed10f3dd08ef525fbbc4d0303c896b5e9b2` |

## Autorizaciones pendientes y prueba propuesta

Antes de instalaciónapp/test/notificaciones/armado: NUEVA autorización específica sesiónhijo; «listo» no permiso. ÚnicoA13 SM-A135M por modelo/todoadb-s/A56excluido/sinseriesrepo/connectedglobalno. Adultopresente/cargador para despertar/botónaveriado/ADB/códigofresco/recuperación/emergencia/salidaMAIN/rollbackready. Leer baselineNONE/snapshotlimpio/DPred-bootdesarmado/LOCAL03restaurada antes armar.

Proponer UN armado temporal de pilotoMAIN→LOCKED→código fresco PERMITIR POR TIEMPO NORMAL con elecciónmanual1min (campo vacío)→NONE→adulto cambia a otraapp sin leer contenido ni volver a hijo/pulsarBloquear→vencimiento/revocacióndurable/readbackLOCKEDautomático→FINALIZARREDMAINcodigo fresco→cancelref/listeners/observer antesdesarme/restoreDPausente/NONE/servicioausente/sesióncerrada/uso normal. Capturar elapsedinicio/deadline/delivery/solicitud/readback y hashensayadovsfinal. No corte/redmodem heredado de R1, sin modem/routertools/identificadores/redpassword/datosmóvilescoste. OFFLINEtemporal exige aprobación específica de fase/corte/reversión, no incluir automáticamente.

Fallo: detener/restaurarseguro; repetir solo autorización individual. Sin reboot/kills/force-stop/Doze/cambiosOwner/PIN/ADB/LOCAL03/Settings/HOME/grantsespeciales/destructivos. No H05/H06/productfinal/remoto/24hfísico/zeroescape/crash garantizados. Sin aprobación: PREPARADO_FISICO y detener. No siguiente tarea automática.

## Rutas de la unidad

main/network/{MinutosUso,ProgramadorPlazo,PuertoPlazoAndroid,UsoRedLocal,ObservadorVisible,ControlRedAndroid}.kt; local/SesionAdulto.kt; ui/AccesoAdultoActivity.kt; test/network/{UsoTiempoTest,ProgramadorPlazoTest}.kt; test/local/SesionAdultoTest.kt; androidTest/network/UsoRedDpTest.kt; ESTADO/relevo/esta evidencia. Sin manifest/deps/planes/ADR/fixtures/contratos/reposajenos/push.
