# HIJO-PROT03 — LOCKED → permitir1min → otra app → LOCKED → salida

2026-10-13. Base `5dbfa3f`. **RECORRIDO_LOGRADO_Y_RESTAURADO**, un único ensayo
reversible en A13 SM-A135M/API34, interactivo/servicio vivo. A56 excluido.

## Ajuste mínimo previo y autorización

Primero se verificó el APK instalado: SHA-256
`22b89600580c18f51da638229c024d184ac9f4a0fb17dee7c51dc1985f56bcc4`, exactamente
el final PROT02, no su APK anterior del ensayo desde NONE. Owner propio, NONE,
sin servicio temporal; instrumentación de baseline OK/sin snapshot pendiente.

Fuente identificó incompatibilidad antes de restringir: entrada R1 consume gate
ADB; PERMITIR exige gate vigente y COMPROBAR anterior rechazaba cualquier snapshot
pendiente. Por tanto la ruta desde LOCKED no podía renovar el gate. **No se hizo
ensayo fallido con APK previo ni se atribuye causa histórica a PROT01.**

Se explicó causa, se obtuvo autorización expresa para ajuste/instalación debug y
presencia del adulto. Cambios solo:
- Nuevo debug/prototype/GateAdbPrototipo.kt, función pura que conserva baseline
  NONE/sin snapshot y admite renovar bajo LOCKED propio/snapshot íntegro/lista
  y features propios; rechaza PINNED, corrupción, política ajena y NONE pendiente.
- PoliticasPrototipo.validarComprobacionAdb: Owner/admin/debug/credencial y lecturas
  acotadas de snapshot/whitelist/features; **sin writes ni borrar snapshot**.
- RollbackPrototipo.COMPROBAR usa esas lecturas y responde enum separado
  ROLLBACK_LOCKED_PROPIO/NINGUNA. DUMP/exportación solo debug/caducidad RAM intactos.
- Nuevo testDebug/prototype/GateAdbPrototipoTest.kt: cinco tests positivos/negativos.

No temporizador/autoridad adulta/contrato reimplementados, permisos nuevos,
restricciones añadidas o checks retirados. Rutas bajo
`android/app-child/src/*/java/dev/controlparental/child/`. Además esta evidencia,
ESTADO y relevo. Main/CP1/CP2/fixtures/planes/ADR/dependencias/repos ajenos intactos.

## Ensayo: observación automática vs confirmación manual

Adulto confirmó nuevamente credencial en UI Android, Emergencia/Keyguard sin
llamadas, checkbox y notificación TIMER_LISTO existente; no permisos nuevos.
Se avisó antes de aplicar y obtuvo confirmación concreta del recorrido completo.

| Paso | Automático realmente observado | Manual adulto |
|---|---|---|
| Baseline | NONE, Owner, sin snapshot/servicio; rollback RESTAURADO/NINGUNA | Presente/dispuesto, credencial/emergencia/notificación correctas |
| Entrada R1 | Gate BASELINE/NINGUNA; ADB LOCKED | Comprobó credencial y pulsó Entrar una vez; LOCKED_VERIFICADO |
| Renovación bajo bloqueo | result1 ROLLBACK_LOCKED_PROPIO/NINGUNA; ADB LOCKED antes/después | No salida otorgada por renovar gate |
| Permitir desde LOCKED | Servicio RESTAURADO/NINGUNA antes de PERMISO_CONFIRMADO restanteMs59988; ADB NONE durante permiso | Eligió1 explícito en campo vacío, autenticó PERMITIR, uso normal y abrió otra app elegida por él |
| Vencimiento | ENTREGA_VENCIDA+4ms, solicitud+12ms, ESPERAR_LOCKED→LOCKED_VERIFICADO; ADB LOCKED | Regresó automáticamente desde otra app, sin volver manualmente ni introducir nuevo PIN |
| Seguridad | ADB volvió a observar LOCKED tras pasos | Canceló autenticación sin salida, Emergencia visible sin marcar/llamar, desbloqueó Android y siguió LOCKED |
| Salida final | PLAN_CANCELADO, servicio terminado, RESTAURADO/NINGUNA, ADB NONE, sin servicio; test sin snapshot; rollback final result1 | Autenticó salida, uso normal y notificación desaparecida |

Las dos restauraciones (antes de permitir y salida final) comprueban paquetes/
features contra snapshot privado y NONE antes de eliminar snapshot. No publicar
listas ni pisar políticas ajenas. Permitido confirmado **después** de readback
de salida, con deadline original60.000ms: sin nueva duración por cambiar de app/
cerrar actividad administrativa/entregar alarma. Cierre/fondo de Activity no
canceló permiso comprometido; no se ensayó la frontera120s de ticket humano.
El vencimiento no necesitó gate ADB/credencial humana renovados.

Log propio sanitizado, sin contenido/app elegida/PIN/serie/stack:

```text
RESTAURADO/NINGUNA
PERMISO_CONFIRMADO restanteMs=59988
ENTREGA_VENCIDA retrasoMs=4
BLOQUEO_SOLICITADO retrasoMs=12
ESPERAR_LOCKED/NINGUNA
LOCKED_VERIFICADO/NINGUNA
FINAL_LOCKED_VERIFICADO retrasoMs=429
PLAN_CANCELADO
SERVICIO_TERMINADO_SIN_SUPERVIVENCIA
RESTAURADO/NINGUNA
```

**LOCKED confirmado a+429ms del deadline**, no bloqueo al segundo exacto.
Latencias monotónicas de código, no inferidas de timestamps civiles. Dos logs
PLAN_CANCELADO corresponden a terminar/onDestroy, no otra autorización/minuto.
No segunda entrada/permiso de ensayo ni repetición tras fallo; no falló el recorrido.

## Comandos y pruebas ejecutadas

Selección mínima `adb devices -l`: único A13 anunciado, otro destino excluido.
Toda operación al teléfono usó `adb -s <A13>` fijo; serie solo en temporal privado.
Comandos efectivamente ejecutados (prefijo/serie omitidos aquí):

```text
shell getprop ro.product.model / shell getprop ro.build.version.sdk
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>                  # hashes previo/final, ruta no publicada
shell dumpsys device_policy                     # filtro Owner/admin propio
shell dumpsys activity activities               # filtro mLockTaskModeState
shell dumpsys activity services dev.controlparental.child # servicio propio
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
shell pidof dev.controlparental.child
shell logcat -d --pid=<proceso propio> -s PROT02:I PROT01:I '*:S' # enums/latencias, filtrados
```

Instalación Success; instrumentación focalizada **1 test OK tres veces** (APK
previo baseline, actualizado baseline y final). APK de pruebas existente compatible
no reinstalado; assembleAndroidTest UP-TO-DATE. No instrumentación de credencial,
connected tests globales, inventario, contenido, UI dumps, llamadas ni grants ADB.
Rollback baseline/final `result=1,data=RESTAURADO/NINGUNA`; FGS propio observado
isForeground/types00000400 y ausente al finalizar. Final instalado coincide por
SHA-256 con APK local ensayado; no APK distinto posterior al ensayo.

Desde `android/`, una ejecución:

```text
.\gradlew.bat :app-child:testDebugUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:compileReleaseKotlin --console=plain
```

BUILD SUCCESSFUL: **238 JVM debug/0 fallos/0 errores**, cinco nuevos, regresión
pertinente incluida. Núcleo y compileReleaseKotlin UP-TO-DATE: no ejecución nueva
de tests núcleo ni nueva compilación release. APK debug construido. Inspección
clases/manifest release: cero prototype/permisos debug; sin APK release/DEX.
Git diff --check correcto; ningún comando/build/test fallido en esta unidad.

APK previo solo baseline: hash22b896… anterior, no ensayo PROT03 con ese binario.
**APK realmente ensayado = APK final instalado/local**, con ajuste autorizado:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA-256 `2d6f6468730a230667b3d92ca39156e2d7fc2b2edcc6297027c7e8c3dbeccd2a`.
No subida/binary commit/gitpush.

## Estado final y límites

NONE, snapshot eliminado tras readback, plan/listener cancelados, servicio detenido,
uso normal confirmado. Notificación grant/canal/APK debug previos permanecen; sin
modificar Owner/PIN/HOME/emergencia/ADB ni otras políticas. No observación física
adicional de un segundo deadline para callbacks tardíos; cancelación/generación
probadas en regresión JVM y parada real observada. Rollback externo no ejercitado
desde LOCKED: se comprobó ruta renovable y salida adulta fue la que restauró.

Solo A13/API34 interactivo/FGS-proceso vivo; no crash/reboot/Doze/pantalla apagada
forzados o persistencia/promesa productiva, control parental final/masterPIN/remoto/
contador/historial/H05/H06 acreditados. No probar guarda yaLOCKED/reemplazo/24h o
política ajena físicamente. Cerrar con commit propio y esperar revisión, sin otra tarea.
