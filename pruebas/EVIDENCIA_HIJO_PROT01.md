# HIJO-PROT01 — prototipo debug reversible, resultado parcial

Fecha: 2026-10-13. Asignación excepcional del coordinador y autorización del dueño
para actualizar APK DEBUG y probar bloqueo reversible **solo A13 SM-A135M**.
No cierre H05/H06 ni aprobación de sus dependencias. CP/1/CP/2 intactos.

## Resultado y bloqueo concreto

**PARCIAL: APK implementado/instalado; bloqueo real NO acreditado.** En el intento
manual el propietario informó: «no hizo nada, todo siguio igual y si, lo puedo usar».
No se obtuvo estado LOCKED. No se sabe si falló una precondición/ticket, la llamada
Android o su comprobación inmediata; no afirmar causa ni PINNED observado.
La UI de error genérica no permite distinguir estos casos. No se repitió la entrada
después del resultado negativo. Revisión posterior deberá mejorar diagnóstico sin
datos sensibles y resolver la entrada antes de otra prueba física coordinada.

Rollback externo posterior respondió `result=1`; ADB observó
`mLockTaskModeState=NONE`. Prueba instrumentada posterior pasó: sin snapshot
pendiente, capacidad debug/Owner/credencial disponibles. Adulto confirmó uso normal.
El adaptador solo devuelve restauración correcta tras readback de ambas políticas
si había snapshot; sin snapshot confirma NONE. **No demuestra que llegasen a cambiar
políticas ni que se haya ejercitado rollback desde LOCKED**. No valores de baseline
publicados: snapshot mínimo de paquetes/features se guarda localmente antes de
cualquier escritura DPM y se elimina solo después de restauración verificada.

## Implementación y límites

- `src/debug/.../prototype/TicketsPrototipo.kt`: tickets por identidad, reloj
  inyectado, autorización RAM 120s, comprobación previa consumible una sola vez.
  Fondo/cancelación/recreación no autorizan; callback viejo no consume ticket nuevo.
- `PoliticasPrototipo.kt`: adaptador oficial DPM; requiere debug, Device Owner,
  admin y credencial existente; NONE inicial, propio paquete ausente de baseline.
  Snapshot durable previo; solo `setLockTaskPackages` y `setLockTaskFeatures`.
  Lista del propio paquete; `KEYGUARD | GLOBAL_ACTIONS`. `startLockTask`, acepta
  únicamente estado observado LOCKED. Error intenta restaurar, sin inventar APPLIED.
  Restauración valida estado esperado para no sobrescribir cambios ajenos detectados.
- `PrototipoBloqueoActivity.kt`: entrada launcher independiente solo debug;
  BiometricPrompt de plataforma API30+ con `DEVICE_CREDENTIAL` exclusivamente,
  sin leer/conocer/almacenar PIN. Verificación previa sin políticas, salida autenticada,
  emergencia existente independiente, checkbox de comprobación manual de emergencia.
  No tickets en Bundle; pause invalida. Si plataforma enviara la actividad a fondo
  durante autenticación, se rechazaría: el baseline manual, no una simulación, es gate.
- `RollbackPrototipo.kt`: receptor solo debug protegido por permiso del sistema
  `android.permission.DUMP`; app ordinaria no tiene ese permiso. ADB shell ya
  autorizado puede comprobar la ruta (RAM5min para entrar) o restaurar solo snapshot
  propio. La restauración nunca concede permiso parental ni cambia claves. No se
  exporta entrada/restauración de prototipo en release.
- `src/debug/AndroidManifest.xml`: componentes/USE_BIOMETRIC/testOnly debug.
  `src/testDebug/.../TicketsPrototipoTest.kt`: seis tests puros, sin adulto simulado
  como evidencia física. `src/androidTestDebug/.../CapacidadPrototipoTest.kt`:
  lectura del propio contexto, Owner/capacidad/NONE/snapshot/permisos del receptor;
  no aplica políticas ni autentica.

Sin nuevas dependencias; main/productivo/CP/1/fixtures/planes/ADR aceptados intactos.

Documentación oficial consultada de forma precisa antes de implementar:
- https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt.Builder
  (`setAllowedAuthenticators`, no botón negativo al habilitar DEVICE_CREDENTIAL).
- https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt
  (`authenticate` y tipo de resultado DEVICE_CREDENTIAL).
- https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
  (allowlist, salida retirando paquete y preservación Keyguard).
- https://developer.android.com/reference/android/app/admin/DevicePolicyManager
  (`setLockTaskPackages`, `setLockTaskFeatures`, KEYGUARD).

No HOME persistente, suspensión, restricciones, cambios de credencial, quita de
Owner, borrado, reprovisionamiento, ADB deshabilitado, redes, padre o relay.
PIN conocido por el menor **no es autoridad parental final**. Sin temporizador,
control remoto, reboot o persistencia productiva certificados.

## Ejecución local

Desde `android/`:

```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:compileReleaseKotlin --console=plain
.\gradlew.bat :app-child:testDebugUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:compileReleaseKotlin --console=plain
```

Ambos BUILD SUCCESSFUL. Segunda ejecución: debug JVM **205 tests/0 fallos/0 errores**,
incluidos seis nuevos del prototipo. Núcleo `:core-protocol:test` UP-TO-DATE:
no afirmar nueva ejecución de sus 169 tests. APK debug y de pruebas construidos;
compilación release UP-TO-DATE, no APK release instalado/construido en esta unidad.

Inspección corregida: cero clases en paquete `prototype` de clases release y cero
componentes PROT01/USE_BIOMETRIC en manifest release. Primera lectura PowerShell
del manifest falló por usar objeto FileInfo sin FullName; segunda comprobación
demasiado amplia detectó DUMP del **ProfileInstaller preexistente**, no prototipo.
Tercera comprobación con FullName/paquete-componentes exactos pasó. No declarar
exclusión de todo permiso DUMP ni inspección DEX/APK release no ejecutada.
`git diff --check` pasó antes del cierre; no lint nuevo.

APK local (sin subida):
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`

SHA-256:
`c0e442a06fad0c821257819b318369c0b9cd0c52130b372b92a277b107a67157`

## Fase física y comandos

`adb devices -l` usado solo para seleccionar modelo anunciado: un A13 SM-A135M,
otro destino excluido. Identificador solo en temporal privado fuera del repositorio,
no en evidencia. Confirmación `ro.product.model=SM-A135M`; **cada comando al equipo
usó `adb -s` sobre el mismo destino**. No consultas ni operaciones al A56.
Antes de instalar/aplicar se avisó al adulto y se obtuvo disponibilidad presencial.

Comandos efectivos, con selector omitido aquí para no publicar serie:

```text
adb -s <A13> shell getprop ro.product.model
adb -s <A13> shell dumpsys device_policy
adb -s <A13> shell dumpsys package dev.controlparental.child
adb -s <A13> shell dumpsys activity activities
adb -s <A13> install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
adb -s <A13> install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
adb -s <A13> shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
adb -s <A13> shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
```

Dumps filtrados a Owner/propio paquete/modo Lock Task; sin inventario de apps o
actividad. Inicial: Owner ChildAdminReceiver, APK DEBUGGABLE/TEST_ONLY, modo NONE.
Instalaciones ambas Success. Instrumentación focalizada **1 test OK** antes y después
del intento. No connected tests indiscriminados, sin credenciales automatizadas.
Rollback previo sin snapshot `result=1`; comprobación ADB `result=1`; rollback final
`result=1`, modo NONE y sin snapshot verificados. No uninstall de APKs de laboratorio.

Confirmaciones **manuales**, no observación automática ni captura:

1. Adulto disponible con A13.
2. Seleccionó «Ambas comprobaciones correctas»: credencial Android validada en UI
   previa sin bloqueo y acceso a Emergencia desde Keyguard visto sin marcar/llamar.
3. Se avisó antes de aplicar dos políticas; se indicó renovar credencial tras Keyguard.
4. Selección «No pude hacerlo» fue corregida por el adulto como clic accidental.
   Se repitieron opciones; seleccionó «Error / NONE / PINNED» y precisó que no hubo
   cambio visible y ahora usa normalmente el equipo. No reinterpretar como éxito.

No pruebas Inicio/Atrás/Recientes, cancelación **bajo LOCKED**, emergencia bajo
LOCKED, salida autenticada desde LOCKED ni nueva entrada acreditadas: falló el gate
de entrada. Salida segura del estado final vía rollback, no prueba de salida adulta
de quiosco. Sin llamadas, capturas, UI dump de credenciales ni secretos en logs.

Cerrar esta unidad parcial con commit propio. Esperar revisión; no repetir pruebas
físicas, corregir otra unidad o afirmar producto listo por iniciativa automática.
