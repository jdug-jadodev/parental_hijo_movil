# HIJO-LOCAL04 — autobloqueo tras reinicio, con latencia; ensayo desarmado

14-10-2026. Base `644dbef`, bloque1 `9cfe675`. **AUTOBOOT_LOCKED_LOGRADO_Y_RESTAURADO**.
Un armado adulto, un reboot A13 SM-A135M autorizado finalmente **por ADB**, sin retry.
Piloto MAIN funcional, no cero ventana/crash/producto final acreditados.

## Implementación acotada

Rutas bajo `android/app-child/src/`:
- main/.../boot/RegistroPiloto.kt: AlmacenPiloto puro DI/puertos de archivo y
  FuenteArranque, enrolamientoUUID local/no secreto, armado, BOOT_COUNT/elapsed/
  generación. Marca y registro DP privados/versionados/checksum/readback, no sesión,
  código, permiso/tiempo restante ni CP1. Instalación virgen no enrola/bloquea.
  Armar exige código fresco/panel; contexto incierto/nuevo/reloj regresivo conserva
  intención bloqueada y cambia generación. Pérdida parcial/corrupción no es virgen;
  no borrar para reparar. Checksum no autentica frente a root/rollback; pérdida total
  de todos los artefactos no tiene ancla externa anti-borrado certificada.
- main/.../boot/ArranquePilotoAndroid.kt: actor serial/IO DP separado de H04, estados
  DESARMADO/ARMADO/PENDIENTE_KEYGUARD/ESPERANDO_ACTIVITY/ESPERANDO_LOCKED/LOCKED/
  FINALIZANDO/ERROR. Antes UserUnlocked solo DP/contexto público; ningún almacén
  adulto/Keystore/snapshotCE abierto. Mantiene Keyguard del sistema/emergencia.
  Señales oficiales LOCKED_BOOT_COMPLETED/BOOT_COMPLETED/MY_PACKAGE_REPLACED,
  receiver dinámico USER_UNLOCKED/USER_PRESENT y Activity.onResume. Receiver
  dinámico requiere proceso vivo: no garantía de supervivencia de watchers ante
  kill/crash, ningún FGS/wakelock/alarma especial. No HOME persistente ni servicios.
  Una solicitud de lanzamiento/intento por referencia enrolamiento-generación,
  excepción Keyguard conserva pendiente para señal apta. TimeoutActivity5s y
  readbackLockTask3s/revisión100ms acotada del actor LOCAL02, no retry hasta verde.
  Exigir Owner/admin/contexto y primer plano/token vigentes antes de startLockTask.
  Recepción/intención/whitelist/callstart/Keyguard NO son éxito; solo readbackLOCKED.
- main/.../boot/BootReceiver.kt: H04 BootContextRepository/EstadoAndroid conservados
  sin migración/interpretación permiso/seqCP1 nueva; añade piloto DP independiente
  y mantiene goAsync hasta ambas ramas. El contexto H04 cambia naturalmente al
  boot5 real, **no se restaura a boot4** al finalizar. No se fabrican ACK/firmas.
- main/.../lock/{SnapshotLockLocal,BloqueoLocalAndroid,TransicionLocal}.kt: snapshot
  CE original capturado **antes de guardar armado y antes de writes DPM**; primer
  bloqueo aplica desde snapshotbaselineguardado, reentrada exige snapshot íntegro/
  propio paquete+features exactos/!PINNED sin sobrescribir baseline ni recapturar.
  Conserva obligación ante fallo/timeout/fondo del piloto, no rollback automático
  que conceda uso. LOCKED existente no llama startLockTask repetidamente.
- main/.../ui/LauncherActivity.kt y manifestMAIN: lifecycle oficial llama reentrada,
  lockTaskMode pasa if_whitelisted→normal para que Android no autoinicie Lock Task
  detrás de Keyguard. HOME sigue no persistente. Pantalla bloqueo/puerta/emergencia
  propias, sin abrir Settings/app externa para recuperar.
- main/.../ui/AccesoAdultoActivity.kt y local/SesionAdulto.kt: ARMAR_BOOT/FINALIZAR_BOOT,
  panel+código fresco/ticket/revisión/boot/primer plano/generación. Recuperar pregunta
  solo cambia código/cierra, no desarma. Sesión soloRAM, muerte/fondo/reboot no concede
  salida. Salida ordinaria de ensayo deshabilitada mientras enrolado; aviso explícito
  de que armado seguirá bloqueando en siguiente reboot hasta FINALIZAR ENSAYO.
  Finalizar autenticado persiste desarmado/invalida tokens/retira watchers **antes**
  de restore propio/readbackpaquetes-features-NONE, y después elimina registro/marca
  DP temporal. Fallos no vuelven a armar; dejan obligación/error para recuperación.
- debug/.../local/RollbackArranqueDebug.kt y debugmanifest: ruta DUMP shell mínima,
  consulta/desarme técnico antes de rollback snapshot propio; no receptor MAIN ni
  extras/bootflags/universalPIN/autoridad parental por Androidcredential. Release
  excluye receptor. Ruta baseline responde DESARMADO sin writes; **no prueba de
  rollback externo desde aplicado**. Salida física se realizó por código adulto.
- Tests main/test boot/RegistroPilotoTest, lock/{SnapshotLockLocalTest,TransicionLocalTest},
  local/SesionAdultoTest; androidTest/boot/PilotoDpAtomicTest (3 DP efímeros/fuentefake).

DP en `noBackupFilesDir` del DeviceProtectedStorageContext: boot-piloto-v1.marca y
boot-piloto-v1.registro. Marca enlaza UUID al registro; version1/booleans/boot/monotónico/
checksum/tamaño acotados. No privadas ni códigos ni datos de usuario. Escritura parcial
o artefactos incoherentes ERROR, no alta automática; no certificación de bloqueo real
si Android/storage impiden enforcement (intención fallacerrado no equivale LOCKED).
La recuperación de corrupción severa sin artefactos íntegros no se probó físicamente.

Armar exige LOCAL03 sin snapshot/todosfalse, LockTaskNONE/sin snapshot al capturar;
solo whitelist/features hijo KEYGUARD|GLOBAL_ACTIONS como LOCAL02. No activar políticas
LOCAL03, perseguir caché ni cambiar HOME/settings/USB/ADB/PINAndroid/Owner/red/CP/deps.
No nuevo ALLOW: en esta unidad no existe permiso productivo previo que ensayar; ausencia
de campos de permiso y cancelación de autoridad vieja comprobadas por modelo/tests,
no fingir un recorrido físico ALLOW_FOR antiguo→boot. Nueva regla desconexión sigue
vigente pero señal/plazo/usooffline pendientes, no implementada.

Fuentes oficiales HTTP200: developer.android.com/work/dpc/dedicated-devices/lock-task-mode,
privacy-and-security/direct-boot y guide/components/activities/background-starts.
La guía BAL actual no lista excepciónDPC; comprobada fuente oficial Android14
android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/services/core/java/com/android/server/wm/BackgroundActivityStartController.java:
ramaisDeviceOwner(callingUid) permite BAL_ALLOW_ALLOWLISTED_COMPONENT. No se usa API
privada/reflexión: solo Context.startActivity y Activity.startLockTask oficiales.
Esto no promete comportamiento idéntico de todo firmware; readbacks y prueba A13 abajo.

## Pruebas y construcción reales

Bloque1 `9cfe675`: aproximadamente470 líneas propias incluyendo cambios,13 tests
nuevos compartidos;319debug/264release correctos. Primer comando tests/build/lint
excedió120s después de tareas assemble (no BUILD SUCCESSFUL global entonces);
lint separado sí BUILD SUCCESSFUL,0errores12avisos con nuevoInlinedApi flag
RECEIVER_NOT_EXPORTED. GuardSDK directo>=33 corregido sin suppress/minSDK/deps.
Validación global posterior correcta69s (tests/debug/release/lint).

Bloque2: captura snapshotbaseline antesarmado,3AndroidDP nuevos y1JVM autoridadboot.
Comando final desde android/, **BUILD SUCCESSFUL39s**:
```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```
**320debug/265release JVM0fallos/errores**,14 nuevos compartidos (release repite),
coretestUP-TO-DATE. JVM cubrevirgen/armarmismo/nuevoboot/contextoincierto/reloj/
desarme/callbackviejo/partiallost/corrupción/version/readbackfail/snapshotpropioajeno/
conservarfallotimeout/Keyguard/sesiónadultaRAM/vigencia/ticketpropósito. APKdebug/
test/releaseunsigned construidos; lintRelease0errores11avisos (último reportUP-TO-DATE,
analyze/tasks pertinentes reales). lintDebugFGS/verificadorFake previos no reejecutados
ni declarados resueltos/suprimidos/reparados. Ninguna connectedtest global.

TresAndroid DPAtomicFile/fuente boot simulada **OK(3tests),0.258s** una ejecución
tras autorización installapp/test. ArchivosUUID efímeros, no stateH04/código/snapshot
real/DPM ni adulto simulado como prueba física. CapacidadPrototipoTest1OKantes0.061s/
final0.065s; no nuevos ni sumados con DP. LOCAL01/02/03Android no reejecutados.
Inspección DEX ambos APK: actor/AlmacenPiloto MAIN presentes, testsbootausentes,
rollbacksoloDEBUG/prototypeausenterelease. Git diff --check correcto antes cierre.

## Autorizaciones y único recorrido físico

1. Nueva confirmación instalación debug/test y3DPtestsaislados, adulto presente.
   Único anuncio A13SM_A135M/verificadoSM-A135M, otros0; todos adb-s mismo destino.
   A56 excluido/serie solo temporal privado/no series en evidencia. Installapp/test
   Success. BaselineNONE/LOCAL02baseline1/LOCAL03SINsnapshot-todosfalse/pilotoDESARMADO.
   Ruta LOCAL04FINALIZAR baseline result1 DESARMADO sinwrites, noaplicadoprobado.
2. Adulto confirmó código/puerta/recuperación/emergencia y supuesto menú manual,
   método despertar/cargador/credencial Android; se avisó no armar todavía.
3. Confirmación específica diferente UNarmado/UNreboot **manual**/restorefinal,
   ninguna persistencia permanente. Adulto panel+códigofresco ARMAR una vez;
   LOCKED inicial visual yADB, enroladotrue/BOOT_COUNT4. Sin otroPERMISO.
4. Menú manual no accesible bajo quiosco/botón averiado: **no hubo reboot manual**.
   Dueño preguntó salir o reiniciar por agente. No se salió ni interpretó autorización
   implícita: nueva confirmación explícita **adb-s A13 reboot UNA vez**, reemplazando
   método manual. Comando terminóexit0, único reboot. No método manual acreditado.
5. Dueño confirmó Keyguard/Emergencia antes PINAndroid sin llamadas. Tres lecturas
   ADB antes primer unlock fallaron dispositivo no disponible (sin cambios/grants/
   nuevo reboot); no entonces afirmar bootcount/log observados. Después desbloqueó
   Android y confirmó hijoBLOQUEADO automático **sin abrir app/pulsarBloquear/código
   parental**. Modelo nuevamenteSM-A135M/bootcount5/logDP/readbackLOCKED porADB.
6. Adulto confirmó código exigido/no sesión restaurada/recuperación disponible y
   FINALIZAR ENSAYO con código fresco→uso normal. No cambio código/pregunta/real
   recuperación ni aplicación extra. Final automático: pilotoDESARMADO/enroladofalse,
   LOCAL02baseline1/LOCAL03SINsnapshot-todosfalse/NONE/capacidad1OK. **Sin rollback
   técnico después de aplicado**; restauración vía MAIN autenticada.

## Trazas monotónicas sin secretos y alcance de tiempos

Tag solo `PilotoArranque`, logcat filtrado, sin UI dumps/capturas/inventario/contenido.
No preguntar/ver/capturar códigos/PIN/pregunta/respuesta reales. Recibir acción muestra
fase RAM previa antes de decidir: primera línea DESARMADO del proceso nuevo **no
indica DP desarmado**, registro boot5/gen2 existe y siguiente línea queda pendiente.

| Evento | Boot/gen | elapsed ms | Resultado |
|---|---|---:|---|
| ARMADO |4/1|429743739|IntenciónDP, no éxitoLOCKED aún|
| INICIAL_LOCKED |4/1|429743904|ReadbackLOCKED, +165ms desde armado|
| LOCKED_BOOT_COMPLETED |5/2|42487|Registro DP nuevo contexto leído|
| KEYGUARD |5/2|42488|PENDIENTE_KEYGUARD; sin CE/startLockTask|
| USER_UNLOCKED |5/2|121587|Señal oficial, lanzamiento controlado|
| SOLICITAR_LOCKED |5/2|123069|ESPERANDO_LOCKED, no éxito por llamada|
| READBACK_LOCKED |5/2|125655|LOCKED real|
| BOOT_COMPLETED |5/2|145270|Duplicado contexto, no reinicio permiso ni segundo intento|
| DESARMADO_ANTES_RESTORE |—|337712|FINALIZANDO/invalida watchers|
| FINAL_RESTAURADO |—|337933|DESARMADO, snapshot/DPeliminados tras readbacks|

USER_UNLOCKED→readbackLOCKED **4068ms**, solicitud→readback **2586ms**, señal→solicitud
1482ms. No tiempos civiles/credencialKDF inferidos; 0.258s es runner3tests, no DPM.
Estos son tiempos de señal/observación, no medición exacta de inicioLockTask ni del
instantePIN ni de una ventana aprovechable de escritorio. Dueño no reportó ventana
de otraapp, pero **no prueba cero ventana ni ausencia de acceso** durante ese lapso.
Primer arranque pendingKeyguard se conserva79099ms hasta señal tras desbloqueo,
no permiso de uso ni obligación de que adulto introduzca nueva autorización para
reentrar. Latencia física alta queda documentada; mejorar garantías requiere unidad
posterior/asignación, no HOME/suspensiones/permisos añadidos incidentalmente.

## Comandos físicos realmente ejecutados

Todos dirigidos `adb -s <A13>` salvo devices-l inicial; sin serial en repositorio.
```text
devices -l
shell getprop ro.product.model
shell dumpsys activity activities             # solo mLockTaskModeState
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
shell am instrument -w -e class dev.controlparental.child.boot.PilotoDpAtomicTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
shell am broadcast -a dev.controlparental.child.LOCAL03_COMPROBAR -n dev.controlparental.child/.local.RollbackProteccionDebug
shell am broadcast -a dev.controlparental.child.LOCAL04_COMPROBAR -n dev.controlparental.child/.local.RollbackArranqueDebug
shell am broadcast -a dev.controlparental.child.LOCAL04_FINALIZAR -n dev.controlparental.child/.local.RollbackArranqueDebug # SOLO baseline
shell settings get global boot_count
shell am start -n dev.controlparental.child/.ui.LauncherActivity # SOLO antes armado
shell logcat -d -v brief -s PilotoArranque:I *:S
reboot                                       # UNA vez, autorización explícita separada
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>
```
Lecturas previasPIN no disponibles registradas, no repetir reboot. Sin force-stop/
kill/crash/Doze/radios/DNSVPN/Settings/grants/Owner/PINAndroid/ADBdisable/borrados/
desinstalación/wipe/reprovisión/otra app/A56. No logcat general ni limpiarlogs.

## Binarios, final y pendientes

APK ensayado = final instalado/local, código no cambiado tras instalar:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA256 `3c2833033dd71bab59a7c4dcd0d417ec1ad42cf5fefdb6364ee769bba8f4333c`.
Releaseunsigned no físico: `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`
SHA256 `6491d8a3f3e3e01c0d0c9abf6d5d7256b7e38bc3ccec0d245203b6c6362388b1`.

Final: DP piloto restaurado a virgen/desarmado, ninguna sorpresa de armado para
próximo reboot; snapshotLock/prototipo/protecciónLOCAL03 ausentes, políticasbaseline
exactas/readbackNONE/serviciotemporalninguno/sesióncerrada/uso normal. Código/pregunta/
respuesta y datos preservados, H04 ahora contexto boot5 legítimo. Ninguna protección
LOCAL03 nueva ni cachehardening. Sin consentimiento de persistencia permanente.

Pendientes: ventana efectiva preLOCKED/latencia, crash/muerteproceso/watchers,
rollbackexternodesdeaplicado/corrupciónDPfísica/otrofirmware/contextoinciertofísico,
rebootmanualseguro bajoquiosco y red/permiso productivo. No H05/H06/productfinal/
remoto/red/CP2/migración/interop acreditados por piloto boot. Contratos/fixtures/
planes/ADR/deps/reposajenos/padre/Render/Push/QR02 intactos, sin gitpush/despliegue/
subagentes/sesiones/promptsdevuelta. Esperar revisión/asignación, detener; cada otra
instalación/activación/reboot/ensayo requiere confirmación específica nueva.
