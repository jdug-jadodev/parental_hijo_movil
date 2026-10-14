# HIJO-LOCAL02 — bloqueo real MAIN y salida adulta reversible

2026-10-13. Base `8533ae4`, bloque1 `602586e`. **RECORRIDO_MAIN_LOGRADO_Y_RESTAURADO**.
Un bloqueo físico autorizado en A13 SM-A135M; API34 según inventario previo,
SDK no consultado de nuevo. Sin código/pregunta/respuesta/PIN/serie en evidencia.

## Implementación acotada

- main/lock/TransicionLocal: intención PREPARANDO/espera/readback LOCKED/error
  distintos, una solicitud/generación, espera3s y restore2s, sin sleeps/reaplicar.
  Primer NONE espera, PINNED no éxito; autoridad obsoleta antes de LOCKED falla.
  LOCKED ya observado o detectado durante error **no se restaura por fondo/cierre**.
- main/lock/SnapshotLockLocal: puerto DI y snapshot mínimo paquetes/features en
  formato privado v1/checksumSHA256, no secretos/CP1. Checksum detecta corrupción
  accidental, no autenticidad/antirollback frente a root. Snapshot antes de writes,
  baseline NONE/sin snapshot/paquete hijo ausente. Compare actual contra baseline
  o valores propios; no defaults ni sobrescribir política ajena. Borrado únicamente
  después de paquetes/features originales leídos y modo NONE confirmados.
- main/lock/PoliticasLockAndroid: DPM oficial whitelist solo propio paquete y
  KEYGUARD|GLOBAL_ACTIONS. startLockTask en UI foreground, IO de snapshot en worker.
  Owner/admin/credencial sistema/Keyguard comprobados antes de entrar; no PINAndroid
  como autoridad de bloquear o salir. No stopLockTask automático de lifecycle.
- main/lock/BloqueoLocalAndroid: actor único RAM, una transición admitida, callbacks
  limitados/readbacks cada100ms, no reenforcement/proceso/reboot garantizados.
  Snapshot durable es obligación de restauración, NO enforcement duradero.
- main/local/SesionAdulto: propósitos BLOQUEAR/SALIR_ENSAYO y referencia acreditada.
  Panel requiere código local; cada operación sensible exige **otro código fresco**,
  ticket vigente/revisión/boot/primer plano/plazo. Pregunta sola no autoriza salida:
  cambia código/cierra, luego autenticar nuevo código (regresión JVM, sin rotación
  física nueva). Fondo/caducidad/callback viejo no acreditan acción.
- main/ui/AccesoAdultoActivity: acciones nativas, confirmación/cancelación, modo
  real/diagnóstico visibles. Salida rotulada **REVERSIBLE DE ENSAYO**, no ALLOWCP2
  ni permiso productivo offline. Controles libre/tiempo/red/ajustes/desinstalación
  deshabilitados. Confirmación aceptada inicia restore; finalizar task al retirar
  whitelist no cancela restauración ya solicitada legítimamente.
- Launcher MAIN bajo LOCKED muestra pantalla bloqueada/acceso adulto/recuperación
  pregunta/emergencia propios. Cerrar Activity/sesión no retira políticas ni permite
  apps externas. Root público conserva rutas sin autenticar, no controles libres.

Todos bajo `android/app-child/src/main/java/dev/controlparental/child/`; comentario
PolicyEnforcer actualizado para diferenciar emergencia de nuevo puerto Lock Task.
No importaciones debug en MAIN, firmas/ACK/CP1/CP2/RecoveryController/registro adulto
reinterpretados. Sin HOMEpersistente/Settings/suspensión/desinstalación/restricciones
amplias/red/servicios/permisos/dependencias nuevos ni provisión/Owner/PINAndroid/ADB.

Rollback técnico: `src/debug/java/dev/controlparental/child/local/RollbackLocalDebug.kt`
y receiver debug protegido DUMP shell. Acciones LOCAL02_COMPROBAR/RESTAURAR, solo
snapshot de este piloto; baseline también rechaza snapshot/servicio prototipo.
Puertos MAIN de restauración no son componentes exportados ni autoridad normal:
caller shell/acreditación técnica solo desde código DEBUG; no grant de uso ni
lectura/modificación de credenciales. DEX/manifest release excluyen ese receptor.
No usa GateADB/Androidcredential para autenticar acciones MAIN.

## Pruebas y builds realmente ejecutados

Bloque1 aproximadamente466 líneas propias, commit602586e tras dos ejecuciones
testDebugUnitTest/testReleaseUnitTest correctas:281/226,15 nuevos compartidos.
Bloque2 UI/guardas/rollback/testAtomic, otros3 JVM nuevos y cierre bajo500 líneas.
Comando final desde `android/`, dos veces BUILD SUCCESSFUL:

```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

**284 JVM debug/229 release,0 fallos/errores**,18 nuevos compartidos; release repite,
no sumar como tests diferentes. Snapshotcorrupto/ajeno/IO/readback/NONE/PINNED/
timeout/autoridadexpira/fondo/callbackviejo/noauto salir/recuperación no salida
cubiertos con fakes. NúcleoUP-TO-DATE; no nuevas pruebas núcleo. APKdebug/release
construidos y releasecompilation real; último APKtest UP-TO-DATE tras build previo.
lintRelease0errores/11avisos, reporttask últimoUP-TO-DATE. lintDebug/errorFGS previo
y verificadorFake genérico de LOCAL01 **no reejecutados ni reparados/suprimidos**.

Dos Android nuevos SnapshotLockAndroidTest: AtomicFile efímero/políticasFAKE,
readback/restore/corrupción, ninguna escritura DPM real/autenticación instrumental.
Compilados y **ejecutados una vez OK(2tests),0.161s** con autorización previa.
CapacidadPrototipoTest1 OK antes de actualizar y final. CincoAndroidLOCAL01 no
reejecutados ni sumados nuevos. No connectedglobal/capturas/UI dumps/inventario.
Inspección tablas DEX: puerto MAIN presente ambos APK, tests lock ausentes, rollback
solo debug, prototype ausente release. Clases/manifestrelease coherentes, sin FGS/
receptorLOCAL02. Git diff --check correcto; sin comandos/build/tests fallidos en unidad.

Fuentes oficiales HTTP200 consultadas: developer.android.com/work/dpc/dedicated-devices/lock-task-mode
y reference/android/app/admin/DevicePolicyManager. SPEC H5/H10/H11/ADR-H-001/004
consultados solo para alcance; planes/ADR aceptados sin edición.

## Coordinación y ensayo MAIN único

Se obtuvo confirmación específica antes de install app/test y dos pruebas aisladas.
adb devices-l: único A13 anunciadoSM_A135M, otros0; modelo comprobadoSM-A135M.
Todos comandos al mismo destino adb-s, serie solo temporal privado; A56 excluido.
Owner propio/NONE/sin servicio y testcapacidadsin snapshot antes; installs Success.
Rollback shell ANTES de restringir: result1 RESTAURADO/NINGUNA, sin snapshot en
baseline, no writes necesarios; comprobación result1 BASELINE_LOCAL/NINGUNA.
Esto prueba ruta shell disponible, **no rollback externo físico desdeLOCKED**.

Adulto confirmó entrada con código actual/panel/Salida visible (inactiva enNONE),
emergencia/Keyguard sin llamar y método despertar/credencialAndroid. Después se
pidió/obtuvo confirmación distinta de UN bloqueo reversible MAIN. No configurar
ni sustituir código/pregunta real; no registrar/verificar secretos por herramientas.

| Paso | Automático | Confirmación manual adulta |
|---|---|---|
| Entrar | UI LOCKED/NINGUNA reportada; ADB mLockTaskModeState=LOCKED | Código para panel y código fresco BLOQUEARAHORA una vez |
| Mantener | ADB LOCKED posterior | Inicio/Recientes mínimo no uso normal; recuperación visible/cancelada sin respuesta; salida cancelada sin confirmación |
| Emergencia | ADB continuó LOCKED | Keyguard y Emergencia accesibles sin llamar, desbloqueo Android no concedió administración ni quitó quiosco |
| Salida | ADB NONE; comprobación LOCAL02 result1 BASELINE_LOCAL/NINGUNA **sin restauración shell posterior** | Panel/código fresco SALIDAENSAYO, volvió uso normal |
| Final | Sin servicio, testcapacidad1OK/sin snapshotprototipo; archivoLOCAL02 ausente por baseline | Código/pregunta no cambiados, uso normal |

No medir latencia de entrada a partir de horas civiles: solo readback y timeout
monotónicos del modelo, sin cifras de latencia física capturadas. Snapshot ausente
postsalida confirma completitud del readback de restauración (única ruta de borrado
en flujo MAIN), no solo que se pidió start/restore. Salida no concedió permiso de
uso persistido ni envió ACK. No más bloques/ensayos tras logro.

Comandos reales (prefijo `adb -s <A13>` omitido; selección inicial devices-l):

```text
shell getprop ro.product.model
shell dumpsys device_policy                     # filtro Owner propio
shell dumpsys activity activities               # solo mLockTaskModeState
shell dumpsys activity services dev.controlparental.child # servicio temporal propio
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
shell am instrument -w -e class dev.controlparental.child.lock.SnapshotLockAndroidTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am broadcast -a dev.controlparental.child.LOCAL02_RESTAURAR -n dev.controlparental.child/.local.RollbackLocalDebug
shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
shell am start -n dev.controlparental.child/.ui.LauncherActivity
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>
```

RESTAURAR una vez baseline, COMPROBAR dos veces baseline/final. No grant/A13reboot/
crash/force-stop/wipe/reprovisión/red/Doze/desinstalación/ADB deshabilitado. Sin
leer código/pregunta/respuesta o archivos de credencial real, secretos no en chat.

## Binarios y cierre

**APK realmente ensayado = final instalado/local**, sin código posterior a instalación:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA256 `717a2cce3db35dcf34785c93cb0c3a68f64fc2fcebfb7be0d9d2020d3cb967a0`.
Releaseunsigned no físico: `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`
SHA256 `1d5a4920feedea0386f8222923082a8483078f135ac8ec21d535cf9f1e615f81`.
Registroadulto real conservado/ningún reset ni codechange; finalsesióncerrada/
NONE/snapshotsninguno/serviciotemporalninguno/uso normal. APK debug queda instalado.

PilotoMAIN funcional, **no H05/H06/puertas/remoto/reboot/producto final acreditados**.
Sin persistencia enforcement/muerteproceso/reboot/ceroescapes garantizados, rollback
externo desdeLOCKED/corrupciónpolíticas físicas/caducidad durante transición no
ensayados. No recuperación real cambiando código bajoLOCKED, solo cancelar pantalla
pregunta; recuperaciónrealLOCAL01 y regresión lógica son antecedentes distintos.
Regla nueva red registrada: perder conexión requerida bloquea aunque tiempo,
reconexión no abre sin nueva autorización; señal/plazo/uso offline pendientes,
ninguna detección/red/permiso normal implementados aquí. Esperar revisión y asignación,
no endurecer ni iniciar otra unidad automáticamente. Sin repos ajenos/Push/QR02/
CP1/CP2/fixtures/planes/ADR/deps/servidor/gitpush/despliegue/subagentes/sesiones.
