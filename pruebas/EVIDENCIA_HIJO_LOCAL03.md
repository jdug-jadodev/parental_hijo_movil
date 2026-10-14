# HIJO-LOCAL03 — protección MAIN reversible; resultado físico parcial

14-10-2026. Base `4431593`, implementación `c7fb85e` (aproximadamente426 líneas
propias añadidas/modificadas, sin documentación). **PARCIAL_RESTAURADO**.
Una activación adulta real autorizada en A13 SM-A135M; no segundo intento.

## Políticas y límites escogidos

Fuentes oficiales consultadas con HTTP200:
- https://developer.android.com/reference/android/app/admin/DevicePolicyManager
  (`setUninstallBlocked`, `isUninstallBlocked`, `setUserControlDisabledPackages`,
  `getUserControlDisabledPackages`, `getUserRestrictions`, `getUserRestrictionsGlobally`,
  `addUserRestriction`, `clearUserRestriction`).
- https://developer.android.com/reference/android/os/UserManager#DISALLOW_APPS_CONTROL

`setUninstallBlocked(admin, paqueteHijo, true)`: solo paquete hijo, distinta de
protección de datos. API34 `isUninstallBlocked` devuelve política **resuelta** e
ignora admin; no declarar getter propio. Se rechaza baseline efectivo=true antes
de writes porque no permite reconstruir la contribución propia anterior.

`addUserRestriction(admin, DISALLOW_APPS_CONTROL)`: gestión de **TODAS las apps
del usuario**, no granular. Documentada para inhibir desinstalar/deshabilitar/
borrar caché y datos/forzar detención/cambiar defaults desde Ajustes o launcher.
No `addUserRestrictionGlobally`, ni suspensión de Settings, ni bloquear todo Ajustes.
La alternativa granular `setUserControlDisabledPackages` documenta solo datos y
force-stop, no deshabilitar; su getter API34 también es resuelto. No se añadió.
Restricción global de gestión avisada en UI/chat y autorizada físicamente antes
de la única aplicación. No restricciones de instalar/actualizar añadidas; estos
flujos durante uso autorizado siguen pendientes de diseño/prueba, no promesa.
No inviolabilidad ante root/recovery/factory-reset/ADB; DISALLOW_APPS_CONTROL
documenta excepciones fuera de Ajustes/launcher. ADB conservado para laboratorio.

La protección DPM puede sobrevivir proceso/reboot; no se ensayó reboot, ni se
implementó/presume Lock Task persistente. No red/tiempo/ALLOW/CP1/CP2/ACK nuevos.

## Implementación y rutas

Bajo `android/app-child/src/`:
- main/java/dev/controlparental/child/protection/ProteccionLocal.kt: modelo puro,
  constructor DI (archivo/puerto/autoridad), estados SIN_SNAPSHOT/PENDIENTE/ACTIVA/
  RESTAURADA/ERROR, causa y lectura uninstall efectiva/control propio local/global
  propio/efectivo por separado. Un fallo conserva snapshot/políticas parciales;
  no defaults ni rollback por cerrar/fondo. Requiere nueva autoridad para restaurar.
- main/.../protection/ProteccionAndroid.kt: adaptador DPM oficial API34+/Owner/admin,
  actor IO serial independiente; generación RAM y checks de autoridad antes de cada
  write. No componentes MAIN nuevos/exportados ni permisos/dependencias nuevos.
- Snapshot `noBackupFilesDir/proteccion-local-v1.snapshot`: AtomicFile privado usando
  adaptador MAIN existente, distinto de snapshot Lock Task y registro adulto. V1
  de37 bytes/checksumSHA256, sin secretos. Guardar/readback antes de writes, no alta
  libre al corromper. Checksum no prueba autenticidad/antirollback frente a root.
  Guarda solo uninstall efectivo previo y3 booleans de la restricción gestionada.
- Restricciones propias: DPM getUserRestrictions(admin) local y
  getUserRestrictionsGlobally() propio global, UserManager.hasUserRestriction
  efectivo. Baseline efectivo ajeno no representable falla antes de writes.
  Restore compara baseline o valores propios y global propio inalterado antes/
  entre writes; solo setUninstallBlocked(false) nuestro admin y clear de nuestra
  restricción local si baselinefalse. Nunca clear global/otro admin. Si otro admin
  se superpone, no se elimina su contribución; readback no-baseline queda ERROR y
  conserva obligación. Comparaciones no detectan carreras con iguales valores ni
  autentican autor del último write; no certificación de coexistencia multiadmin.
  Snapshot se borra únicamente tras lectura exacta del baseline completo.
- main/.../local/SesionAdulto.kt: propósitos PROTEGER_APP/RESTAURAR_APP separados,
  panel+código fresco, ticket/revisión/boot/plazo/primer plano, referencia acreditada
  por operación. Recuperación solo cambia código/cierra, no retira protección.
- main/.../ui/AccesoAdultoActivity.kt: acciones/aviso global/readbacks individuales,
  consulta fuera UI, cancelar/fondo no desprotegen. Emergencia y recuperación sin
  Settings. Salida Lock Task no toca este actor/snapshot; permitir uso no cableado.
- debug/.../local/RollbackProteccionDebug.kt + debug/AndroidManifest.xml: receptor
  DUMP shell SOLO debug, consulta/restore de obligación LOCAL03, no autoridad
  parental MAIN/credenciales/Owner/Lock Task. Release excluye receptor/tags/acciones.
  Baseline sin snapshot **rechaza** restore ERROR/SNAPSHOT sin writes; no éxito
  fabricado. Esa comprobación de ruta no prueba rollback físico desde aplicado.
- test/.../protection/ProteccionLocalTest.kt y test/.../local/SesionAdultoTest.kt;
  androidTest/.../protection/ProteccionAtomicTest.kt (fakes/archivos UUID efímeros).

Sin editar CP1/CP2/schema/fixtures/planes/ADR/deps/repos ajenos; sin nuevas políticas
de debugging/USB/safe-boot/factoryreset/HOME/red/instalación/suspensión/Owner/PINAndroid.
Código/pregunta/respuesta reales conservados; contador/espera se actualizan por
autenticación normal, no se consultaron secretos/archivo real desde herramientas.

## Pruebas/builds realmente ejecutados

Desde `android/`, mismo comando dos ejecuciones:
```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

Primera ejecución: tests/buildAPK correctos, lintRelease FAILED por NewApi:
getUserRestrictionsGlobally API34 con guard SDK indirecto no reconocido por lint.
Corregido guard directo SDK>=34, no suppress/minSDK/deps/modificación ajena.
Segunda BUILD SUCCESSFUL, **306debug/251release JVM,0fallos/errores**,22 nuevos
compartidos (19modelo/3sesión), release repite, no sumar como diferentes.
Núcleo UP-TO-DATE, APKtest/compilaciónAndroidtest UP-TO-DATE última ejecución;
habían sido realmente construidos en la primera. APKdebug/release finales reconstruidos.
lintRelease0errores/11avisos. lintDebugFGS previo/verificadorFake previo **no
reejecutados ni reparados/ocultados**. No connectedtests globales.

JVM: baseline propio/efectivo/ajeno, readback fallido, corrupción/IO, restoreajeno,
capacidad incompatible, boot/caducidad/fondo/ticketviejo, pregunta no restauración,
ACTIVA noautoquita, fallo de delete, cambio entre writes. AtomicFile Android tres
tests nuevos compilados y **ejecutados una vez OK(3tests),0.217s** tras autorización
de install app/test. RutaUUID/noBackup efímera, políticasFAKE, no DPM real ni
simulación de credencial adulta como evidencia. CapacidadPrototipoTest1OK antes y
final (0.063s cada ejecución), no suma nuevo. TestsLOCAL01/02 no reejecutados.

Inspección DEX directa de tablas de ambos APK: ProteccionLocal/adaptador MAIN
presentes, clasesTest/Prueba/Aisladas protection ausentes; rollback soloDEBUG,
prototype ausente release. Manifestrelease excluye rollback/prototype.
Git diff --check correcto antes commit implementación; no genérico Fake aprobado.

## Coordinación física y recorrido único

Autorización nueva install/debug/test y3Androidaislados, adulto presente. Selección
devices-l único anuncioSM_A135M, otros0, getprop modeloSM-A135M; serie solo temporal
privado, todos comandos físicos adb-s fijo, A56 excluido. Baseline NONE, capacidades
OK, installapp/test Success. LOCAL02_COMPROBAR result1 baseline sin snapshots/
serviciotemporal. LOCAL03_COMPROBAR: SIN_SNAPSHOT, uninstallEfectivo=false,
controlPropio=false/controlGlobalPropio=false/controlEfectivo=false.
LOCAL03_RESTAURAR antes de aplicación: result0 ERROR/SNAPSHOT, sin snapshot y
sin cambio de valores (fallo cerrado esperado, ruta shell disponible).

Durante preparación manual dueño dijo haber pulsado Proteger y observado
desinstalar/forzar detención inhibidos, caché disponible. Se advirtió falta de
autorización de alcance global y se hizo solo readback: **SIN_SNAPSHOT/todosfalse/
NONE**. No hubo aplicación: botón inicial solo abre formulario/código fresco.
Esos controles ya inhibidos pertenecen al baseline/Owner previo, no efecto LOCAL03.

Después se pidió/obtuvo autorización explícita diferente para **UNA aplicación
temporal** de las dos políticas, gestión de TODASapps del usuario incluida, navegación
breve a Ajustes SOLO hijo y restauración al cierre. No autorización permanente.

| Paso | Automático | Manual adulto |
|---|---|---|
| Aplicar | ACTIVA/NINGUNA, snapshottrue, uninstallEfectivotrue/controlPropiotrue/globalPropiofalse/controlEfectivotrue; modoNONE | Panel+código fresco PROTEGERAPP UNA vez, ACTIVA reportada |
| Ajustes | Sin captura/UI dump ni intento destructivo automatizado | Desinstalar/Deshabilitar/Borrar datos/Forzar detención inhibidos; **Borrar caché disponible** |
| Detener | No nueva aplicación ni ampliación de restricciones | Se indicó no borrar caché ni ejecutar acción; finalizar por resultado parcial |
| Restaurar | Post MAIN: SIN_SNAPSHOT/NINGUNA/todosfalse; LOCAL02baseline1; NONE/capacidad1OK | CódigoMAIN/recuperación/emergencia disponibles, restore adulto fresco, RESTAURADA reportada; Deshabilitar inhibido confirmado |

La pregunta de cierre pidió opcionalmente verificar Keyguard; la respuesta agrupada
confirma disponibilidad de código/recuperación/emergencia y restauración, **no
acredita una nueva navegación Keyguard detallada ni llamada ni quiosco LOCAL02**.
No pregunta respondida/codechange/recoveryreal/reboot/LockTasknuevo. Controles
inhibidos observados sin intentar uninstall/clear-data/force-stop/deshabilitar.
Desinstalar/force-stop ya inhibidos en baseline: no atribuir su cambio a estas APIs.
Caché: contradicción con alcance documentado en este fabricante, causa no
determinada; no probar borrado para resolverla ni declarar inviolabilidad.

Restore por acción MAIN autorizada, no shellrestore después de aplicado. Baseline
final exacto/todosfalse y snapshot ausente verifican obligación cerrada: única ruta
MAIN de borrado tras readbacks completos. Rollback shell real desde aplicado sigue
NO ENSAYADO. No tiempos de latencia de operación física capturados; 0.217s es runner
3tests, no latencia DPM/KDF. Sin actualización/instalación normal bajo protección
ensayada ni coexistencia multiadmin física ni persistencia de protección ensayada.

Comandos reales (cada físico con `adb -s <A13>`; serie no en repositorio):
```text
devices -l                                    # solo seleccionar modelo
shell getprop ro.product.model
shell dumpsys activity activities             # filtro solo mLockTaskModeState
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
shell am instrument -w -e class dev.controlparental.child.protection.ProteccionAtomicTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
shell am broadcast -a dev.controlparental.child.LOCAL03_COMPROBAR -n dev.controlparental.child/.local.RollbackProteccionDebug
shell am broadcast -a dev.controlparental.child.LOCAL03_RESTAURAR -n dev.controlparental.child/.local.RollbackProteccionDebug
shell am start -n dev.controlparental.child/.ui.LauncherActivity
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>
```
COMPROBAR03 cuatro lecturas baseline/preconfirmación/aplicado/final; RESTAURAR03
solo baseline. Sin inventario/contenido/Settings automatizado/UI dumps/capturas/
logcat/grants especiales/force-stop/matar proceso/ADBdisable/radios/Doze/DNS/VPN/
desinstalación/borrado/Ownerretirar/PINcambiar/reprovisión/wipe.

## Binarios/final/pendientes

APK ensayado = final instalado/local, no código cambiado después de instalación:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA256 `40891e2146cf10b44664af423f7380e4292b1e34960c1a1347c8c8f35fea7d69`.
Releaseunsigned no físico: `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`
SHA256 `ef1dd1ba0ae1e391a92831b12f36e83f3734f95cd4e87e57348109006da9ee10`.

Final A13NONE/sin snapshotProteccion/LockTask/prototipo/serviciotemporal/sesiónadulta
cerrada/uso normal, datos y secretos conservados. Ninguna protección nueva queda
permanente. **PARCIAL_RESTAURADO**: núcleo/puerta/restauración/readbacks logrados,
datos y deshabilitar observados inhibidos, caché sigue disponible. No H05/H06/
producto final/remoto/reinicio acreditados. Red nueva perder conexión requerida
bloquea/reconectar noabre sin nueva autorización vigente sigue pendiente de señal/
plazo/usooffline, no implementada aquí. Esperar revisión/asignación, no más ensayo
ni tarea automática. Sin reposajenos/padre/Render/Push/QR02/pushgit/despliegue/
subagentes/sesiones/promptsdevuelta. Un segundo ensayo requiere nueva autorización.
