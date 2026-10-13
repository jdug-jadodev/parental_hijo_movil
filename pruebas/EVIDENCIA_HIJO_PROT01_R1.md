# HIJO-PROT01-R1 — bloqueo debug real y salida reversible

2026-10-13. Base `d38d3ad`. UNA unidad, solo hijo; nuevas confirmaciones expresas
en esta sesión antes de instalar y antes de aplicar Lock Task. A13 SM-A135M único
por modelo anunciado; todos los comandos al teléfono con selector `adb -s` fijo.
A56 excluido, sin consultas/operaciones sobre él ni serie publicada.

## Resultado

**BLOQUEO_REAL_DEBUG_LOGRADO, RESTAURADO.** Un único intento de entrada.
UI manual LOCKED_VERIFICADO y ADB `mLockTaskModeState=LOCKED`. Después de navegación,
cancelación y Keyguard, ADB volvió a observar LOCKED. Salida adulta autenticada en
UI Android: log sanitizado RESTAURADO/NINGUNA, ADB NONE, prueba de capacidad posterior
OK (sin snapshot pendiente), adulto confirma uso normal. Restauración de las dos
políticas verificada por readback del adaptador frente al snapshot privado previo;
sin publicar sus valores. No se usó rollback ADB para salir de LOCKED: esa ruta
externa está comprobada sin snapshot, no ejercitada desde quiosco en esta unidad.

No certificación H05/H06/producto/puertas/temporizador/remoto/reboot; PIN Android
es autoridad **temporal de laboratorio**, no credencial parental final. Sin segunda
entrada, otras políticas, HOME persistente, suspensión, endurecimiento, llamadas,
capturas/UI dump/secretos, CP1/CP2/fixtures/planes/ADR o repos ajenos modificados.

## Diagnóstico real vs hipótesis

El fallo anterior «no hizo nada» continúa **sin causa demostrada**. No reconstruir
lo que no se midió. Este intento nuevo sí observó:

```text
EMERGENCIA/NINGUNA -> CREDENCIAL/NINGUNA -> ROLLBACK_ADB/NINGUNA
ESPERAR_LOCKED/NINGUNA -> LOCKED_VERIFICADO/NINGUNA
```

ESPERAR_LOCKED se registró a las 15:16:08.621 y LOCKED_VERIFICADO a 15:16:08.734,
113ms después. El modelo solo permanece en ESPERAR_LOCKED si readback devuelve
NONE: en esta ejecución el primer readback tras start todavía no era LOCKED.
Es evidencia de necesidad de espera, **no prueba de causa histórica PROT01**.
No se reprodujo invalidación por onPause durante credencial ni falta/caducidad ADB.
Esos casos siguen protegidos y probados con fakes, no declarados fallo físico.

Cancelación manual dejó log CREDENCIAL/CREDENCIAL_RECHAZADA y no salió del quiosco.
Autenticación final: RESTAURADO/NINGUNA a las 15:22:10.882. Solo enums en log PROT01,
sin mensajes de excepción, payload, valores de políticas, PIN ni otra actividad.

## Cambios debug y pruebas

- Nuevo `EntradaPrototipo.kt`: modelo puro/PuertoEntrada con reloj inyectado,
  etapas/causas cerradas, generación por intento. Una preparación, dos writes,
  readbacks de paquetes/features antes de start, un start. NONE inicial pendiente;
  LOCKED solo por modo observado. 3s monotónicos globales para entrada, 2s para
  readback de rollback. Error/cancelación/fondo mientras pendiente restaura una
  vez; fondo tras LOCKED **no** concede salida. Fallo previo al snapshot no restaura
  un snapshot preexistente. Readbacks obsoletos no autorizan. No reaplicación/poll infinito.
- `PoliticasPrototipo.kt`: puerto Android, causas debug/Owner/admin/credencial/
  capacidad/Keyguard/baseline/snapshot; separado writes/readbacks/start/restauración.
  Snapshot antes de escribir; conserva checks contra política ajena y readback
  antes de borrar snapshot. No cambia ámbito de políticas ni usa callbacks como APPLIED.
- `PrototipoBloqueoActivity.kt`: UI desplazable, etapa/causa/modo visibles; Handler
  main cada100ms con generación vigente hasta confirmar o agotar plazo. No sleep.
  Rollback pendiente continúa incluso al destruir actividad; UI destruida no se
  actualiza. Tickets/invalidation de autenticación se mantienen. Sin extras autoritativos.
- `TicketsPrototipo.kt`: causas falta/caducidad/fondo/cancelación/reloj. Comprobar
  antes de consumir permite diagnosticar gate ADB sin perder credencial comprobada.
- `RollbackPrototipo.kt`: comprobación ADB muestra enums, restauración goAsync
  acotada con readback; invalida autorización ADB al restaurar. DUMP y solo debug
  intactos. No nuevo bypass release ni permiso Android adicional.
- Tests: nuevo `EntradaPrototipoTest.kt` (14), dos casos nuevos de tickets (8 en
  total). Generaciones/tiempos/lifecycle, políticas retardadas, NONE→LOCKED, timeout,
  PINNED, cada fase fallida, restauración retardada/fallida/ajena, baseline rechazado,
  snapshot/contexto obsoleto, salida y callbacks/tickets antiguos. Fakes no acreditan adulto.

Documentación oficial precisa consultada (HTTP200):
- https://developer.android.com/reference/android/app/Activity — startLockTask solo
  entre resume/pause; solicitud y permiso no equivalen a lectura de LOCKED.
- https://developer.android.com/reference/android/app/admin/DeviceAdminReceiver —
  callbacks de entrada/salida Lock Task, API21.
- https://developer.android.com/reference/android/app/admin/DevicePolicyManager —
  packages/features, retirada acaba tasks, API34 agrupa política y notifica resultado.
- https://developer.android.com/reference/android/app/admin/PolicyUpdateReceiver —
  onPolicySetResult API34 notifica política, **no** estado de actividad en quiosco.

Se eligieron readbacks oficiales acotados de política y ActivityManager, sin añadir
receivers ni modificar el admin productivo. No deducir compatibilidad por target36.
Operaciones DPM aún pueden fallar/ser conflictivas; no se quitan comprobaciones.

Desde `android/`, comando ejecutado tres veces:

```text
.\gradlew.bat :app-child:testDebugUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:compileReleaseKotlin --console=plain
```

1. Primera versión: BUILD SUCCESSFUL, 219 JVM debug (14 nuevos).
2. Tras extender dos casos: BUILD FAILED, 221 tests/1 fallo. Mensaje completo del
   fallo de aserción: `java.lang.AssertionError: expected:<ERROR> but was:<RESTAURAR>`
   en `EntradaPrototipoTest.readback obsoleto impide start y restauracion ajena se informa`,
   línea150. El test intentaba iniciar segundo rollback mientras el primero estaba
   pendiente; la guarda lo ignoró correctamente. Se corrigió el test completando
   readback del primer rollback, no relajando la implementación.
3. Regresión final: BUILD SUCCESSFUL, **221 JVM debug/0 fallos/0 errores**, 16 nuevos.

Núcleo `:core-protocol:test` y compileReleaseKotlin UP-TO-DATE en estas ejecuciones:
no afirmar nuevos tests núcleo ni nueva compilación release. APK debug y de tests
construidos; última ejecución assemble UP-TO-DATE. Inspección clases/manifest release:
cero clases paquete prototype y cero componentes PROT01/USE_BIOMETRIC. No APK release
construido/instalado/DEX inspeccionado ni nuevo lint. Git diff --check correcto.

## Fase física: comandos y comprobaciones separados

Nueva selección `adb devices -l`: un A13 anunciado SM_A135M; otro destino excluido.
Getprop al seleccionado confirmó SM-A135M; dump filtrado Owner propio y NONE.
Se autorizaron presencialmente actualización y, por separado, único intento.

```text
adb -s <A13> shell getprop ro.product.model
adb -s <A13> shell dumpsys device_policy                 # filtro Owner/propio admin
adb -s <A13> shell dumpsys activity activities           # filtro mLockTaskModeState
adb -s <A13> install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
adb -s <A13> install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
adb -s <A13> shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
adb -s <A13> shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
adb -s <A13> shell logcat -d -s PROT01:I '*:S'             # solo diagnóstico de este prototipo
```

Ambas instalaciones Success; instrumentación focalizada **1 test OK antes y después**.
Rollback baseline y final `result=1,data=RESTAURADO/NINGUNA` sin snapshot; gate ADB
`result=1,data=BASELINE/NINGUNA`. ADB operativo durante LOCKED. No connected tests.

**Confirmaciones manuales del adulto** (sin observar/automatizar credencial):
1. Credencial en UI Android y Emergencia/Keyguard antes de entrar correctas, sin llamadas.
2. Autorizó único intento, renovó comprobación, marcó emergencia, pulsó entrar una vez.
3. LOCKED_VERIFICADO visible; Inicio/Atrás/Recientes no dieron uso externo.
4. Canceló la autenticación de salida y siguió LOCKED.
5. Emergencia visible bajo quiosco; volvió/desbloqueó Android y siguió LOCKED.
6. Autenticó salida del prototipo y confirmó uso normal.

**Automático**: NONE inicial, LOCKED tras entrada y tras pasos intermedios, NONE final;
diagnóstico de transición/salida y test final sin snapshot. Restablecer políticas
previas acreditado por readback del adaptador al salir; sin inspección de listas externas.

No auditoría exhaustiva, reinstalación/reentrada posterior, reloj/boot/proceso muerto
físicos o llamadas probados. Cerrar y esperar revisión; sin otra tarea automática.

APK local: `android/app-child/build/outputs/apk/debug/app-child-debug.apk`

SHA-256: `375367506344b47c7cc2644c2cc611947ed9c8458b0a20c5cadee339bc5a04c9`
No subida/gitpush. Archivos: siete fuentes debug/testDebug listadas arriba, evidencia,
ESTADO.md y COORDINACION_PADRE_HIJO_RENDER.md. Main y androidTest existentes intactos.
