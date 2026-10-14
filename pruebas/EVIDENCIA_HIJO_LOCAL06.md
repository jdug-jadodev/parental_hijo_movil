# HIJO-LOCAL06 — permitir por tiempo MAIN

14 de octubre de 2026. Base `2582dea`. Código/preparación `6ef3413`; cierre en commit de esta evidencia final. Estado: **TEMPORAL_NORMAL_1MIN_LOCKED_AUTOMATICO_Y_RESTAURADO**. OFFLINE temporal/pantallaapagada/Doze/reboot/crash físicos pendientes. La preparación sin física se conserva abajo como registro previo a nuevas autorizaciones.

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

| APK final (debug después instalado/ensayado; releaseunsigned sin física) | SHA256 |
|---|---|
| `android/app-child/build/outputs/apk/debug/app-child-debug.apk` | `48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092` |
| `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk` | `c410bd84857c3c5051616952254e1ed10f3dd08ef525fbbc4d0303c896b5e9b2` |

## Autorizaciones pendientes y prueba propuesta

Antes de instalaciónapp/test/notificaciones/armado: NUEVA autorización específica sesiónhijo; «listo» no permiso. ÚnicoA13 SM-A135M por modelo/todoadb-s/A56excluido/sinseriesrepo/connectedglobalno. Adultopresente/cargador para despertar/botónaveriado/ADB/códigofresco/recuperación/emergencia/salidaMAIN/rollbackready. Leer baselineNONE/snapshotlimpio/DPred-bootdesarmado/LOCAL03restaurada antes armar.

Proponer UN armado temporal de pilotoMAIN→LOCKED→código fresco PERMITIR POR TIEMPO NORMAL con elecciónmanual1min (campo vacío)→NONE→adulto cambia a otraapp sin leer contenido ni volver a hijo/pulsarBloquear→vencimiento/revocacióndurable/readbackLOCKEDautomático→FINALIZARREDMAINcodigo fresco→cancelref/listeners/observer antesdesarme/restoreDPausente/NONE/servicioausente/sesióncerrada/uso normal. Capturar elapsedinicio/deadline/delivery/solicitud/readback y hashensayadovsfinal. No corte/redmodem heredado de R1, sin modem/routertools/identificadores/redpassword/datosmóvilescoste. OFFLINEtemporal exige aprobación específica de fase/corte/reversión, no incluir automáticamente.

Fallo: detener/restaurarseguro; repetir solo autorización individual. Sin reboot/kills/force-stop/Doze/cambiosOwner/PIN/ADB/LOCAL03/Settings/HOME/grantsespeciales/destructivos. No H05/H06/productfinal/remoto/24hfísico/zeroescape/crash garantizados. Sin aprobación: PREPARADO_FISICO y detener. No siguiente tarea automática.

## Rutas de la unidad

main/network/{MinutosUso,ProgramadorPlazo,PuertoPlazoAndroid,UsoRedLocal,ObservadorVisible,ControlRedAndroid}.kt; local/SesionAdulto.kt; ui/AccesoAdultoActivity.kt; test/network/{UsoTiempoTest,ProgramadorPlazoTest}.kt; test/local/SesionAdultoTest.kt; androidTest/network/UsoRedDpTest.kt; ESTADO/relevo/esta evidencia. Sin manifest/deps/planes/ADR/fixtures/contratos/reposajenos/push.

## Validación física posterior autorizada

Las frases «ningúnADB»/«noejecutados» anteriores corresponden a la preparación, no al resultado final.

### Autorizaciones y recorrido

1. Nueva respuesta **«Autorizar instalación y tests»** para seleccionar únicoA13SM-A135M/instalarapp-test con `-r -t`/ejecutar4DP UUID/fakes, sin armado/notificación/red. Selección devices-l capturada y filtrada por model:SM_A135M, getpropSM-A135M correcto; todos los comandos posteriores adb-s al mismo destino guardado temporal externo, sin serialrepo/A56consultado. Instalar app y test: **Success ambos**. Instrumentación limitada a UsoRedDpTest: **OK(4tests),0.339s**, sin políticas/credencial real. No fallo ni reintento en LOCAL06.
2. Nueva respuesta **«Autorizar ensayo NORMAL 1min»**: baseline/notificaciónvisible(si necesaria)/UNarmadoMAINporcodecódigofresco/UNTEMPORALNORMAL1min/otraapp/esperarLOCKEDauto/cierreseguro. Sin corte/redmodem/OFFLINE/pantallaapagadaintencional/reboot/kills/LOCAL03. No heredar autorización/excepciónmódemR1. Adulto presente y medios de recuperación/cargador/ADB incluidos en confirmación; emergencia/recuperación no usadas ni llamadas ensayadas.
3. Baseline leído antes de armar: REDactivo=false/listo=false/deadline=null, bootDESARMADO/enrolado=false, LOCAL02BASELINE_LOCAL/NINGUNA, LOCAL03SIN_SNAPSHOT/todosfalse y mLockTaskModeStateNONE. Rollbacks DEBUG/DUMP disponibles/excluidosrelease; **no ejecutados desde aplicado**.
4. Adulto **«Armado, pantalla bloqueada»**: panel/código fresco UNARMARRED. Readbacks FGSisForeground=true/foregroundId105/tipo00000400(systemExempted)/startRequested=true, observadorlisto=true, redVALIDADA, bootLOCKED/enrolado=true y mLockTaskModeStateLOCKED. Notificación MAIN autorizada para esta fase; no grantsADB ni cambios de permisos especiales. No se registró si hubo nuevo diálogo Android o se conservó permiso previo; no afirmar grant nuevo efectuado.
5. Adulto **«Otra app y bloqueo automático»** en pregunta explícita: campo minutos vacío, elecciónmanual1min/códigofresco, salida a otraapp/panelcerrado/pantallaencendida y sin volver al hijo/pulsarBloquear/código al vencer. No nombre ni contenido de esa app inspeccionado. Traza confirma NORMAL/gen2/inicio-deadlineoriginal/USO_APLICADO_NONE y, después, listener/obligaciónBLOQUEADO/gen3/solicitud/readbackLOCKED. Lectura ADB final mLockTaskModeStateLOCKED y bootLOCKED. No acción manual de bloqueo: automatismo del scheduler MAIN con procesoFGS vivo.
6. Adulto **«Finalizado y panel cerrado»**: FINALIZARREDMAIN con código fresco, panel/sesión cerrados, uso normal y notificación desaparecida. No desarmeboot separado ni permiso adicional. Comprobaciones finales abajo.

### Traza, entrega y enforcement separados

ElapsedRealtime ms del mismo boot5/proceso vivo. Generaciónred distinta de generaciónboot/seqCP1. Logcat leído solo tags propios enums/tiempos, sin UI/payload/credenciales; filtrado al proceso del ensayo, no atribuir trazas anteriores LOCAL05R1 a LOCAL06.

| Evento | Tiempo | Resultado observado |
|---|---:|---|
| ARMADO boot5/gen1 |6612545|Una autorización adulta MAIN|
| INICIAL_LOCKED |6612708|+163ms armado, baseline bloqueado previo al temporal|
| PREPARADO_BLOQUEADO red/gen1 |6612743|DPobligación lista|
| RED_ACTUAL VALIDADA |6612914|Observador MAIN listo|
| Inicio temporal aceptado RAM |6811673|Adulto eligió1min,deadline6871673|
| AUTORIZACION_LOCAL_NORMAL gen2 |6811685|Obligación antes de abrir/readback;log+12ms inicio|
| USO_APLICADO_NONE |6811866|+193ms inicio;baseline/restauración conservandoenrolamiento|
| RED_ACTUAL NORMAL/VALIDADA |6851009..6851226|Inicio6811673/deadline6871673 **no renovados**|
| ENTREGA_PLAZO gen2 |6871690|Listener+17ms deadline, NO éxito por sí solo|
| PLAZO_REEVALUADO BLOQUEADO gen3 |6871714|Revocación/obligación durable,+41ms deadline;redseguíaVALIDADA|
| BLOQUEO_REQUERIDO_LOCAL05 |6871748|Petición reenforcement,+75ms deadline, aún noLOCKED acreditado|
| SOLICITAR_LOCKED |6871986|Petición LockTask,+313ms deadline|
| READBACK_LOCKED |6872782|LOCKED real,+1109msdeadline/+796mssolicitud/+61109msinicio|
| DESACTIVAR_OBSERVER_ANTES_RESTORE gen4 |7172282|Cierre adulto/ref-alarmas-callback cancelados antesrestore|
| DESARMADO_ANTES_RESTORE boot |7172359|Desarmar antes políticas|
| FINAL_RESTAURADO boot |7172548|Restore/readbacks|
| FINAL_RESTAURADO red gen=null |7172558|DPred eliminado,+276ms desactivación|

No apertura automática, segundo temporal ni repetición dearmado. No código al vencer, redno negativa durante temporal. «1min» significa plazo original60000ms; **LOCKED se leyó61109ms tras inicio**, no certificado de segundo exacto/ceroescapes. Tiempo de permanencia en otraapp/ventana acceso no medidos instrumentalmente: recorrido confirmado por adulto y logs de transición. Nada fuerza pantalla/despierta por UI; pantallaapagada/Doze/Keyguard en este vencimiento no ensayados.

### Readbacks finales y APK real

- REDmodelo BLOQUEADO, activo=false/listo=false/deadline=null; logs inicioUso/vencimientoUso=null/gen=null. Ese baselineRAM sin permiso **NO significa teléfonoLOCKED**: mLockTaskModeState **NONE**.
- BootDESARMADO/enrolado=false. LOCAL02BASELINE_LOCAL/NINGUNA/snapshotlimpio tras restauración/readbacks. LOCAL03SIN_SNAPSHOT/NINGUNA y uninstallEfectivo/controlPropio/controlGlobalPropio/controlEfectivo **todosfalse**; no activación de LOCAL03.
- Servicio propio sin ServiceRecord; adulto confirma notificación ausente/uso normal/panelcerrado. Cancelación de únicoPlanPlazo/listener/callback antes de desarme/restore y referencias invalidas por actor. No relock tardío observado durante comprobaciones, no garantía indefinida sin watcher.
- Existence-only mediante `run-as`/toyboxtest-e de cuatro nombres concretosDP: uso-red-v1.marca/.registro, boot-piloto-v1.marca/.registro **ausentes**. No listar/leer stores/aliases/credencialesadultas. No shellrollback aplicado. Registro/código adulto conservados; ningún cambio de código/pregunta.
- LecturaSHA256 del base.apk propio instalado: **48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092**, coincideAPKfinal/debug **ensayado=finalinstalado**. Sin cambio de código/buildapp después de instalar. Releaseunsigned no instalado.

### Comandos físicos reales, destino anonimizado

```text
adb devices -l # capturado/filtrado sin imprimirseries
adb -s <A13> shell getprop ro.product.model
adb -s <A13> install -r -t <app-debug>
adb -s <A13> install -r -t <androidTest-debug>
adb -s <A13> shell am instrument -w -e class dev.controlparental.child.network.UsoRedDpTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
adb -s <A13> shell am start -n dev.controlparental.child/.ui.LauncherActivity
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL05_COMPROBAR -n dev.controlparental.child/.local.RollbackRedDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL04_COMPROBAR -n dev.controlparental.child/.local.RollbackArranqueDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL03_COMPROBAR -n dev.controlparental.child/.local.RollbackProteccionDebug
adb -s <A13> shell dumpsys activity activities # salida reducida a mLockTaskModeState
adb -s <A13> shell dumpsys activity services dev.controlparental.child/.network.ControlRedService # solo estado servicio propio
adb -s <A13> logcat -d -s ControlRedLocal:I PilotoArranque:I *:S -v brief # enums/tiempos/procesoensayo
adb -s <A13> shell run-as dev.controlparental.child /system/bin/toybox test -e <archivoDPpiloto concreto>
adb -s <A13> shell pm path dev.controlparental.child
adb -s <A13> shell sha256sum <base.apk propio> # salida solo hash
```

### Límites y cierre

OFFLINEtemporalreal/cortes/restauración no autorizados ni ensayados; sin heredarappmódem.1minNORMAL/proceso vivo sí;24hsoloJVM, pantallaapagada/Doze/bootinvalidauso/crash/force-stop/errorescheduler-FGSruntime/Keyguard-vencimientofísico/otrofirmware/reemplazo callbackviejo físico/rollbackexternoaplicado no acreditados. Ningún permiso especial/cambioOwner/PIN/ADB/radios/Settings/HOME/LOCAL03/reposajenos/destructivo/push. NoH05/H06/productofinal/remoto/CP2/24hfísico/ceroescape/supervivencia certificada. Emergencia/recuperación física completa siguen pendientes separados, no inferidas por pruebasUUID.

Unidad entregada/restaurada; commit documental de cierre y detener. Cualquier nueva fase requiere asignación/autorización nueva; no otros ensayos/tareas automáticamente.

Antes del commit de cierre: `gradlew :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:lintRelease --console=plain` correcto2s/57tareas/1ejecutada/56UP-TO-DATE. TestsJVM/analisis-reporte lint UP-TO-DATE: no nueva ejecución de casos ni cambio de APK/código. `git diff --check`correcto.
