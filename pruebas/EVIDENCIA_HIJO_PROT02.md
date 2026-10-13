# HIJO-PROT02 — permiso temporal debug y bloqueo automático

2026-10-13. Base `4faea1f`. **LOGRADO en un ensayo de 1 minuto, A13 interactivo,
servicio/proceso vivo; restaurado al terminar.** No producto ni H05/H06/puertas,
tiempo/remoto/reboot/crash/Doze/escapes exhaustivos acreditados. Solo hijo/debug.

## Resultado físico y latencias

Adulto eligió explícitamente `1` en campo inicialmente vacío, autenticó con
DEVICE_CREDENTIAL y abrió otra app normal elegida por él, sin inventario/contenido.
Confirmó retorno **sin volver manualmente al prototipo**. ADB corroboró LOCKED.
Logs solo enums, tiempo restante y retrasos respecto al deadline monotónico:

```text
TIMER_LISTO
PERMISO_CONFIRMADO restanteMs=59997
ENTREGA_VENCIDA retrasoMs=17
BLOQUEO_SOLICITADO retrasoMs=60
ESPERAR_LOCKED/NINGUNA
LOCKED_VERIFICADO/NINGUNA
FINAL_LOCKED_VERIFICADO retrasoMs=1308
```

Deadline original de 60.000ms; entrega+17ms, solicitud+60ms, LOCKED verificado+1308ms.
**No prometer bloqueo al segundo exacto.** No calcular duración por timestamps
civiles de logcat: el código usa elapsedRealtime, no fecha/hora de pared.
No causa histórica PROT01 deducida de este resultado.

Adulto confirmó cancelación de autenticación sin salida, Emergencia/Keyguard sin
marcar/llamar y vuelta aún LOCKED. Después autenticó salida adulta: uso normal y
notificación desaparecida. Logs PLAN_CANCELADO, SERVICIO_TERMINADO_SIN_SUPERVIVENCIA
y RESTAURADO/NINGUNA; ADB NONE y test persistido sin snapshot pendiente.
Dos PLAN_CANCELADO corresponden a terminar y onDestroy idempotente, no dos permisos.
Rollback final ADB `result=1,data=RESTAURADO/NINGUNA`; no nueva alarma ni servicio iniciado.
Restauración acreditada por readback de paquetes/features frente al snapshot propio,
sin publicar listas. El ensayo empezó en NONE: **permitir saliendo de LOCKED** está
implementado, no probado físicamente aquí. Rollback externo desde LOCKED tampoco probado.

## Código y alcance preciso

- `PermisoTemporal.kt` puro RAM/reloj inyectado: minutos1..1440 enteros sin default,
  Math.addExact/multiplyExact, propuesta y confirmación separadas, referencia por
  identidad/generación/BOOT_COUNT, deadline original. Entrega temprana/obsoleta/
  reloj regresivo/boot distinto no autorizan bloqueo; entrega válida una sola vez.
  Cancelación invalida propuesta y callback, reemplazo confirmado sustituye referencia
  y cancela listener anterior. Fallo de programación no equivale a permiso aceptado.
- `ServicioTemporal.kt`: solo debug, no exportado, START_NOT_STICKY, FGS visible
  `systemExempted` por rol Device Owner. API34+ para este camino de laboratorio.
  AlarmManager.setExact(ELAPSED_REALTIME_WAKEUP, deadline, OnAlarmListener, Handler)
  ligado al servicio/proceso **vivo**, no receiver persistente/exact-alarm permission.
  Si Android mata componente/proceso, puede perder alarma/plan: sin supervivencia.
  Sin wakelock, alarmas reiteradas, rearmado escondido ni bucle esperando el minuto.
- Antes de aceptar: Owner/admin/credencial, servicio/notificación y gate ADB actual,
  emergencia confirmada, boot conocido; baseline guardado sin escribir políticas.
  Si hay Lock Task, solicita restauración con readback acotado antes de preparar
  nuevo permiso. Candidate/deadline se capturan al confirmar operación local con
  baseline listo, no al abrir diálogo. Alarma se programa antes de commit RAM;
  APIs no son transacción única, fallo se diagnostica y prepara recuperación.
- Expiración no solicita PIN/comprobación humana renovados: usa intención temporal
  comprometida, boot/referencia y baseline guardado, revalida capacidades/políticas.
  Reutiliza EntradaPrototipo (3s entrada/2s rollback) y whitelist/features propias.
  Política reducida primero; ActivityOptions.setLockTaskEnabled(true) con lanzamiento
  oficial de actividad propia relanzada, no overlays/accesibilidad/HOME persistente.
  Solicitud o alarma entregada no son LOCKED; timeout/error no es APPLIED.
- Autenticación PERMITIR ligada a minutos capturados y ticket RAM vigente. Fondo/
  cancelación/callback viejo no autorizan otro permiso. Cerrar administración no
  cancela permiso ya comprometido; service/alarm no dependen de volver a la Activity.
  Salida adulta o rollback cancela listener/referencia/generación antes de restaurar;
  callbacks obsoletos no vuelven a lanzar. Bloquear ahora conserva autoridad del
  recorrido R1, cancela plan y reutiliza bloqueo observado.
- Revisión final detectó borde NO ejercitado: bloquear ahora estando ya LOCKED podía
  fallar baseline y llegar a recuperación. Guarda añadida: cancelar plan y conservar
  LOCKED sin escrituras. No afirmar reproducción del defecto ni prueba física de
  esa rama. APK final instalado por nueva confirmación, sin segundo ensayo.

Rutas bajo `android/app-child/src/`: nuevos debug/prototype/{PermisoTemporal,
ServicioTemporal}.kt y testDebug/prototype/PermisoTemporalTest.kt; modificados
debug/prototype/{EntradaPrototipo,PoliticasPrototipo,PrototipoBloqueoActivity,
RollbackPrototipo,TicketsPrototipo}.kt, debug/AndroidManifest.xml y
androidTestDebug/prototype/CapacidadPrototipoTest.kt (paquete común
java/dev/controlparental/child/). Además esta evidencia, ESTADO y relevo.
Sin main/dependencias/CP1/CP2/fixtures/planes/ADR/repos ajenos modificados.

Permisos debug nuevos: FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED,
POST_NOTIFICATIONS. Dueño autorizó antes de instalar/configurar, notificación
concedida en UI Android, **sin pm grant/appops/special-access por ADB**. Canal/grant
de notificación y APK debug permanecen; servicio detenido, no endurecimiento ni
concesión oculta. PIN Android es autoridad temporal, no código parental definitivo.

## Documentación oficial precisa

- https://developer.android.com/develop/background-work/services/fgs/service-types
  — systemExempted enum/permiso, Device Owner elegible; no dataSync/messaging comodín.
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
  — roles/Device Owner y restricciones; FGS no prueba privilegio BAL.
- https://developer.android.com/guide/components/activities/background-starts
  — restricciones BAL, lanzamiento puede fallar sin excepción. No prometer por target36.
- https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode y
  https://developer.android.com/reference/android/app/ActivityOptions
  — relanzar actividad con setLockTaskEnabled, permiso y estado verificados aparte.
- https://developer.android.com/reference/android/app/AlarmManager
  — setExact con OnAlarmListener/Handler; API34 cancela si app sale de lifecycle;
  requisitos distintos en31/32/33. Por ello este temporizador rechaza Android<34.

ELAPSED_WAKEUP cuenta apagado en deadline, pero ensayo fue interactivo, no prueba
entrega/enforcement bajo pantalla apagada/Keyguard/Doze. Si Keyguard impide entrada,
el adaptador diagnostica fallo/restaura, sin bypass ni afirmar bloqueo automático
al desbloquear. No se implementa recuperación productiva de contador/boot/ledger.

## Builds, JVM, exclusión y APKs

Desde `android/`, dos ejecuciones de:

```text
.\gradlew.bat :app-child:testDebugUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:compileReleaseKotlin --console=plain
```

Ambas BUILD SUCCESSFUL; final **233 JVM debug/0 fallos/0 errores**, 12 nuevos de
PermisoTemporal. Deadline1min/24h, input vacío/rango/overflow, confirmar después
de preparar, referencia/boot/generación, entrega temprana/atrasada, reemplazo,
cancelación/salida con propuesta antigua, regresión del reloj, admin fondo y
recreación sin RAM. Tests R1 de callbacks/readback/timeout/restore también corrieron.
Núcleo y compileReleaseKotlin UP-TO-DATE: no tests núcleo o nueva compilación release.
APK debug/test construidos; última assembleAndroidTest UP-TO-DATE. No lint nuevo.
Inspección: cero clases prototype y manifest release sin componentes ni permisos
debug nuevos (POST_NOTIFICATIONS/SYSTEM_EXEMPTED incluidos); no APK release/DEX instalado.
Git diff --check correcto; ninguna prueba JVM/build fallida en esta unidad.

APK del ensayo (copia local generada antes de guarda final):
`android/app-child/build/outputs/apk/prot02-ensayado/app-child-debug.apk`
SHA-256 `658182d029c5c17b013199d68d49b3a729bf6134d9b561f9a998c55bb3f5839c`.

APK final instalado con guarda (baseline comprobado, NO otro minuto):
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA-256 `22b89600580c18f51da638229c024d184ac9f4a0fb17dee7c51dc1985f56bcc4`.
No subir binarios/gitpush ni incluir generados en commit.

## Operaciones físicas realmente ejecutadas

Selección `adb devices -l`: único A13 anunciado, otro destino excluido; modelo
SM-A135M/API34 confirmados solo en A13. Cada consulta/instalación/instrumentación
usó `adb -s <A13>` fijo, serie solo temporal privado fuera del repositorio.

```text
shell getprop ro.product.model / shell getprop ro.build.version.sdk
shell dumpsys device_policy                          # Owner/admin propio filtrado
shell dumpsys activity activities                   # solo mLockTaskModeState
install -r -t <APK debug> / install -r -t <APK test>  # ambas Success
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
shell dumpsys activity services dev.controlparental.child  # solo servicio propio/foreground
shell logcat -d -s PROT02:I '*:S'                     # enums/latencias propias
shell logcat -d -s PROT02:I PROT01:I '*:S'             # salida final, filtro propios
```

Instrumentación focalizada 1 test OK antes/después y tras actualización final (3
ejecuciones); no simula adulto, solo capacidades/permisos/servicio no exportado/
snapshot/NONE. FGS propio observado isForeground=true/types00000400; no servicio
temporal tras salida. Gate ADB BASELINE/NINGUNA; rollback baseline/final correctos.
Ningún connected test global, A56/app inventories/contenido/UsageStats/UI dumps.

Error de comando al actualizar guarda: invocación accidental
`adb -s <A13> shell install` sin argumentos, código1; stderr se descartó por error
del comando, no conservado ni causa inventada. No APK enviado ni instalación
declarada por esa invocación. Corregido con `adb -s <A13> install -r -t <APK debug>`:
Success, instrumentación OK, rollback RESTAURADO/NINGUNA y NONE verificados.

Confirmaciones manuales: presencia/instalación y notificación; credencial/emergencia/
notificación TIMER_LISTO; autorización separada de único minuto; elección1 y otra
app/retorno autónomo; cancelación/emergencia aúnLOCKED; salida/uso normal/sin
notificación; autorización adicional de actualización final SIN ensayo. No secret,
capturas, llamadas ni automatización de credencial. Final: plan/listener cancelados,
servicio terminado, políticas restauradas, NONE/sin snapshot. No espera física
adicional de otro deadline ni ensayo de callbacks tardíos: cancelación/generación
probadas en JVM y parada observada, sin afirmar prueba de reboot/crash.

Cerrar con commit y esperar revisión, sin otra tarea automática.
