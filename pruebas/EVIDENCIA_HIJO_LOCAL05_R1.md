# HIJO-LOCAL05-R1 — gate FGS y preparación de validación

Fecha: 14 de octubre de 2026. Base `b2f59f8`. Corrección/preparación `6d3e085`; cierre físico en commit que incorpora esta actualización. Estado final: **NORMAL_LOCKED_POR_RED_RECONEXION_NO_ABRE_Y_RESTAURADO**. Runtime Device Owner/FGS y UN bloqueo por red acreditados solo en el A13/proceso vivo; EXCEPCIÓN OFFLINE física pendiente. Reglas LOCAL05 intactas; no tarea nueva ni temporales/remoto.

## Excepción autorizada y justificación

Consulta propia HTTP200 a fuente oficial:
<https://developer.android.com/develop/background-work/services/fgs/service-types>, sección **System exempted**, actualizada 2026-10-01 según página.

Declara `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` y elegibilidad al cumplir **al menos uno** de los criterios: Device Owner **o**, entre otros, permisos de alarmas exactas. No exige alarmas además de Device Owner. El diagnóstico reproducido en LOCAL05 con AGP8.11.1 exige permisoFGS **AND** alguno de SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM sin alternativa DO. Ese requisito conjuntivo es falso positivo para nuestra ruta exclusivamente Device Owner, condición runtime no expresable mediante permiso de manifest.

Se aplica SOLO `tools:ignore="ForegroundServicePermission"` al nodo `.network.ControlRedService` MAIN y `xmlns:tools`. Sin alarmas nuevas, tipo distinto, AGP/deps/SDK/roles, disableglobal/baseline/abortOnError/suppressall ni otro issue silenciado. Detector fuente no investigado adicionalmente: documentación primaria y diagnóstico literal bastan para demostrar la discrepancia para esta ruta. No afirmar comportamiento runtime por esta excepción. Guardas obligatorias API>=34/Device Owner/notificaciones habilitadas/canal visible se conservan antes de startForeground. Riesgo: lint ya no comprueba ese issue en ese nodo; futuras modificaciones del nodo/permisos/elegibilidad requieren revisión explícita. Resto lintRelease activo.

## Fail-closed revisado

- Puerta pura `ObservadorVisible`: requiere dos confirmaciones separadas (retorno exitoso de startForeground y registro callback exitoso) y condiciones oficiales actuales (API/Owner/notificaciones/canal). Callback por sí solo o servicio solicitado por sí solo no acepta NORMAL/OFFLINE.
- `ControlRedService` notifica explícitamente fallo antes de stopSelf, no espera silenciosamente onDestroy. Destrucción/inicio fallido/callback fallido invalidan disponibilidad y permiso público inmediatamente; actor cancela callback/gracia, conserva obligación durable y pide bloqueo. Notificación/servicio se detienen en fallo; deduplicación evita reenforcement repetido por fallo seguido de onDestroy. No implica LOCKED aplicado.
- Actor reconsulta condiciones al evaluar señales y antes de autorización/ejecución de apertura. Pérdida de notificaciones/Owner no concede OFFLINE ni conserva permiso aceptado como fallback; rechazo retira permiso RAM y guarda obligación mediante puerto existente. No hay sondeo añadido: cambio sin evento no garantiza detección instantánea, y no se promete supervivencia crash.
- Modelo de red/plazo/excepción/autorización adulta/boot no modificado. UI ya no describe gate fallido; mantiene advertencia de coordinación específica de instalación/notificaciones/armado/conectividad.

## Pruebas y artefactos

Comando ejecutado dos veces tras corrección (segunda tras actualizar texto UI), desde `android/`:

```powershell
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

Ambos exitosos. Final:26s,131tareas/22ejecutadas/109UP-TO-DATE. **348debug/293release,0fallos/errores**; ambas tareas JVM ejecutadas.6casos nuevos compartidos, no sumar release como otros6: foreground ausente/callback ausente/API-Owner-notificaciones-canal ausentes/autoridad fresca requerida/destrucción/pérdida notificaciones con uso vigente. Son tests puros con almacenamiento fake, no prueba física de Service/callback.

`lintRelease`: **0errores/11avisos**. En última invocación lintAnalyze ejecutado/reporteUP-TO-DATE (resultado verde existente de primera invocación R1). `core-protocol:test` UP-TO-DATE en ambas, no nueva ejecución de vectores. APK debug/releaseunsigned construidos; AndroidTest compilación en primera invocación y assembleUP-TO-DATE, no ejecución conectada.

Tres DP UUID/fakes LOCAL05 preparados **NO ejecutados físicamente**. No almacenar/leer/regenerar credencial adulta real. lintDebug preexistente prototypeFGS queda pendiente: no reejecutado ni excepción al prototipo. Verificador previo Fake no reejecutado/resuelto.

Inspección XML MAIN: exactamente UN ignore, SOLO servicio autorizado. Manifest fusionado release: tipo systemExempted/no exportado/permisoFGS y permisoSYSTEM_EXEMPTED; sin alarmas/rollbacks. DEX debug/release: ControlRedService/ObservadorVisible presentes; RollbackRedDebug solo debug; UsoRedDpTest/ObservadorVisibleTest ausentes APKapp. Inspección de cadenas DEX no reemplaza prueba runtime ni verificador completo. `git diff --check` correcto.

APK finales (al preparar: generados/no instalados; después: debug instalado/ensayado y releaseunsigned sin física):

| Ruta | SHA256 |
|---|---|
| `android/app-child/build/outputs/apk/debug/app-child-debug.apk` | `d6de016608a22245843def8d32062004943e0de6d3aafec2ab2d4692221dfc84` |
| `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk` | `13c529467439eee57046c4b901b965ae4568f365f67145051a8aef3bf7ba4120` |

## Física pendiente y alcance

Hasta este registro: ningún ADB/instalación/grant/armado/corte/reboot. A13 último estado conocido LOCAL04 desarmado/NONE/LOCAL03 restaurada, no reconsultado. No asumir «listo» como permiso. Obtener autorización específica antes de instalar APK app/test, notificación MAIN y ejecutar los3DP aislados; otra confirmación concreta antes de armar y antes de conectividad manual. Único A13 SM-A135M seleccionado por modelo; todo adb-s a mismo destino, A56 excluido, sin series en repo ni connectedtests global.

Adulto presente, código fresco no compartido, recuperación/emergencia/salida MAIN, cargador para despertar/botón averiado y ADB. FGS visible; gracia desde señal Android, no instante físico ni DNS/relay. Proponer UN NORMAL libre adulto→UN corte manual de medio concretamente autorizado→señal negativa/deadline→solicitud→readbackLOCKED→reconexión no abre→FINALIZAR RED adulto→desarme/baseline/NONE/servicio-observador-gracia detenidos/snapshotsDPausentes. Garantizar ruta manual de restaurar red sin atraparlo; nada olvidarred/passwordchat/radiosshell/grantsADB/router/DNS/VPN. Datos móviles pueden tener coste: no activarlos automáticamente. OFFLINE/corte adicional requieren aprobación aparte; no reboot/kills/Doze/force-stop/LOCAL03permanente/acciones destructivas.

Sin autorización: entregar PREPARADO_FISICO y detener. Sin trazas físicas ni latencias aún; no afirmar ensayo real ni APKensayado=final. Próxima acción únicamente coordinación de esta validación, no otra tarea.

## Rutas R1

MAIN manifest; network/{ObservadorVisible,ControlRedAndroid,ControlRedService}.kt; ui/AccesoAdultoActivity.kt; test/network/ObservadorVisibleTest.kt; esta evidencia; ESTADO; relevo (devolución también posterior a LOCAL04). Sin CP/1/CP/2/fixtures/planes/ADR/deps/reposajenos/push.

## Validación física posterior, con nuevas autorizaciones

La sección de preparación anterior refleja el punto previo a pedir aprobación, no el resultado final. Desde ella se obtuvieron confirmaciones específicas:

1. **«Autorizar instalación y 3 tests»**: identificar único A13 SM-A135M/instalarapp-test/3DP aislados, sin notificaciones/armado/conectividad. Selección por anuncio model:SM_A135M y verificación ro.product.model SM-A135M; todos los comandos posteriores adb-s mismo destino. A56 no consultado. Destino mantenido solo en temporal local externo al repositorio; no serial en evidencia.
2. Primer installapp `-r` rechazado INSTALL_FAILED_TEST_ONLY: se detuvo antes del APKtest. Nueva aprobación **«Autorizar reintento con -t»**. Comando app `-r -t` devolvió exit1/fallo sin causa detallada; no se intentó test ni se repitió app automáticamente. Lectura SHA256 del base.apk instalado confirmó **d6de016608a22245843def8d32062004943e0de6d3aafec2ab2d4692221dfc84**, igual al APKfinal: instalación efectiva verificada por hash, **NO comando exitoso**. El adulto posteriormente indicó **«intenta de nuevo por que estaba mal conectado el cable»** al solicitar aprobación solo para APKtest. No se reinstaló app; APKtest `-r -t` Success.
3. `am instrument` limitado a UsoRedDpTest: **OK(3tests),0.322s**. UUID/fakes, sin políticas/credencial real. No connectedtests global ni otros tests instrumentales.
4. **«Autorizar notificación y armado»**: notificación visible y UN armado adulto MAIN, verificación/cierre; no corte todavía. Baseline previo COMPROBAR: REDactivo=false/listo=false/deadline=null, bootDESARMADO/enrolado=false, LOCAL02BASELINE_LOCAL/NINGUNA. Adulto confirmó **«Armado, pantalla bloqueada»** tras panel/código fresco, sin código en conversación. Readbacks: FGSisForeground=true/foregroundId105/tipo00000400(systemExempted), callbacklisto=true/usoBLOQUEADO, bootLOCKED/enrolado=true y mLockTaskModeStateLOCKED. Runtime startForeground elegible en este equipo, no garantía universal.
5. El adulto propuso **«bloqueo de señal desde la app del modem»**. No se tomó esa propuesta como permiso automático: se explicó que modificar router estaba excluido y se solicitó excepción específica limitada/reversible/manual al A13, sin afectar otros equipos y conservando ruta de reversión fuera del teléfono bloqueado. Respondió **«Autorizar NORMAL y corte A13»**: autorizó UN NORMAL con código fresco y UN corte/restablecimiento manual desde su app del módem, como excepción al límite anterior. La sesión no consultó/controló router ni SSID/IP/MAC/credenciales, no shellradios ni otras redes/datos móviles añadidos. Sin faseOFFLINE/cortesextra/reboot.
6. Adulto **«Uso normal aceptado»** con código fresco y panel cerrado; readbacks usoNORMAL/FGSactivo y LockTaskNONE. Solo después se indicó el único corte autorizado. Adulto **«Se bloqueó automáticamente»**, sin pulsarBloquear/código. ReadbackBLOQUEADO/bootLOCKED/LockTaskLOCKED; ninguna acción manual de enforcement ni shellrollback.
7. Adulto revirtió el corte desde la misma app del módem y confirmó **«Red restaurada, sigue bloqueado»**. Traza validadaactual/usoBLOQUEADO/gen3/deadline=null, mLockTaskModeStateLOCKED: **red vuelve NO abre**, sin nueva autorización.
8. Adulto **«Finalizado, uso normal»**: FINALIZAR RED MAIN/código fresco (no desarmeboot separado), notificación desaparecida. Cierre/readbacks descritos abajo. No preguntas de recuperación/cambio de código ni ejecución de emergencia/llamadas; su disponibilidad anterior no se presenta como nueva prueba física de esas rutas.

### Comandos físicos empleados (destino anonimizado)

```text
adb devices -l                       # salida capturada/filtrada, no mostrada con series
adb -s <A13> shell getprop ro.product.model
adb -s <A13> install -r <app-debug>    # rechazo test-only; detener
adb -s <A13> install -r -t <app-debug> # nueva aprobación; exit1 sin detalle, hash instalado sí coincide
adb -s <A13> shell pm path dev.controlparental.child
adb -s <A13> shell sha256sum <base.apk propio> # salida reducida a hash
adb -s <A13> install -r -t <androidTest-debug> # aprobación adicional; Success
adb -s <A13> shell am instrument -w -e class dev.controlparental.child.network.UsoRedDpTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
adb -s <A13> shell am start -n dev.controlparental.child/.ui.LauncherActivity
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL05_COMPROBAR -n dev.controlparental.child/.local.RollbackRedDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL04_COMPROBAR -n dev.controlparental.child/.local.RollbackArranqueDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL03_COMPROBAR -n dev.controlparental.child/.local.RollbackProteccionDebug
adb -s <A13> shell dumpsys activity activities # se muestra solo mLockTaskModeState
adb -s <A13> shell dumpsys activity services dev.controlparental.child/.network.ControlRedService # solo estado servicio propio
adb -s <A13> logcat -d -s ControlRedLocal:I PilotoArranque:I *:S -v brief # solo enums/tiempos, filtrado a proceso del ensayo
adb -s <A13> shell run-as dev.controlparental.child /system/bin/toybox test -e <rutaDPpiloto concreta>
```

No instrumentación como adulto físico, no logs UI/payload/credenciales, dumps UI/appscontenido ni capturas. No grantsADB/hiddenFGS/alarmas/LOCAL03/Owner/PIN/ADBdisable/destructivos. Notificación aprobada en dispositivo por adulto; no registrar códigos ni respuestas.

### Traza real y latencias relativas

Tiempos elapsedRealtime en ms, mismo boot5/proceso vivo. Generaciónred diferente de generaciónboot y de CP/seq. Primer logcat incluía trazasLOCAL04 previas: **NO forman parte de este ensayo**, aquí solo el proceso actual.

| Evento | Tiempo | Lectura/acción |
|---|---:|---|
| ARMADO boot5/gen1 |4412911|Una operación adulta MAIN|
| INICIAL_LOCKED |4413070|+159ms desde armado; no bloqueo por pérdida aún|
| PREPARADO_BLOQUEADO red/gen1 |4413101|DPobligación red escrita|
| RED_ACTUAL VALIDADA |4413237|Observador actual listo|
| AUTORIZACION_LOCAL_NORMAL red/gen2 |4645740|Código adulto fresco|
| USO_APLICADO_NONE |4645937|Baseline aplicado,+197ms|
| RED_ACTUAL NO_VALIDADA |4772255|Señal negativa, deadline4782255|
| GRACIA_VENCIDA red/gen3 |4782281|Revocación,+10026ms desde señal/+26ms deadline|
| BLOQUEO_REQUERIDO_LOCAL05 |4782307|Solicitud de reenforcement, NO éxito por sí sola|
| SOLICITAR_LOCKED |4782507|Solicitud Lock Task desde Activity|
| READBACK_LOCKED |4783220|LOCKED aplicado,+713ms solicitud/+10965ms señal/+965ms deadline|
| RED_ACTUAL VALIDADA red/gen3 |4877235|Sigue BLOQUEADO; reconexión no abre|
| DESACTIVAR_OBSERVER_ANTES_RESTORE red/gen4 |5016642|FINALIZARadulto/cancelación previa|
| DESARMADO_ANTES_RESTORE boot |5016707|Obligaciónboot desarmada|
| FINAL_RESTAURADO boot |5016885|Readbacks de restauración|
| FINAL_RESTAURADO red |5016893|Archivosred borrados,gen=null,+251ms desde desactivación|

Instante del corte manual y ventana de acceso efectiva no medidos. Antes de la señal negativa existieron lecturas todavíaVALIDADA; no se afirma detectar corte inmediatamente ni DNS/reachabilityrelay.10.965s señal→LOCKED no es certificación de bloqueo físico a10.000 exactos. No Keyguard pendiente durante esta transición ni prueba de suspensión/crash/reboot/Doze; no callbacks de red vieja físicos inducidos.

### Estado final comprobado

- LOCAL05: usoBLOQUEADO (baseline del modelo sin permiso), activo=false/listo=false/deadline=null. No equivale a teléfono bloqueado: **LockTaskNONE**.
- Boot: DESARMADO/enrolado=false. LOCAL02: BASELINE_LOCAL/NINGUNA; restauración conservó baseline original y eliminó snapshot después de readback.
- LOCAL03: SIN_SNAPSHOT/NINGUNA, snapshot=false, uninstallEfectivo/controlPropio/controlGlobalPropio/controlEfectivo **todosfalse**. No activación de esa protección en R1.
- `dumpsys activity services` propio sin ServiceRecord; adulto confirma notificación desaparecida/uso normal. Cancelacióncallback/gracia y referencia invalidada confirmadas por cierre del actor y diagnóstico, sin servicio tardío observado.
- `toybox test -e` dirigido solo a cuatro nombresDP: uso-red-v1.marca/.registro y boot-piloto-v1.marca/.registro **ausentes**. No listar ni leer credenciales privadas. No shellrollback aplicado.
- APKdebug **instalado y ensayado=final**, hash íntegro anterior; no código cambiado tras instalación. Releaseunsigned no instalado. Registroadulto/código conservado; no cambio de pregunta ni recuperación.

### Cierre y pendientes

Gate liberado y ensayoNORMAL/proceso vivo en A13 logrado/restaurado. EXCEPCIÓN OFFLINE física **no autorizada ni ensayada** (puerta/casilla/código/redindependencia/bloquear en JVM no evidencia física). Caducidades/controltiempo/redretornaantes10s/bootinvalidauso/crash/erroresFGS runtime/callbackviejo/Keyguard/transicionesotrosfirmwares/rollbackexternoaplicado pendientes según caso. No H05/H06/productofinal/CP2/remoto/24h/ceroescape/supervivencia/DNSgarantizados.

Sin nuevos cortes, armado permanente, reboot, force-stop/kills ni acciones fuera del alcance. Documentar, commit de cierre y detener. Cualquier nueva prueba requiere asignación/confirmación específica, no continuar automáticamente.

Antes del commit documental final: `gradlew :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:lintRelease --console=plain` correcto2s/57tareas/1ejecutada/56UP-TO-DATE; ambos tests y análisis/reporte lint UP-TO-DATE, **no sumar nueva ejecución de casos**. Sin cambios de código/APK tras instalación. `git diff --check` correcto.
