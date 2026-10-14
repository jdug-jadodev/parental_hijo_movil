# HIJO-LOCAL05 — red validada y excepción OFFLINE LOCAL

Fecha: 14 de octubre de 2026. Base: `6ab6c1c`. Primer bloque: `552d4b9` (534 inserciones/8 eliminaciones). Cierre: commit que incorpora esta evidencia.

## Resultado y bloqueo concreto

**IMPLEMENTADO_LOCAL_SIN_VALIDACION_FISICA_BLOQUEADO_LINT_RELEASE**. No PREPARADO_FISICO: gate release fallido. No afirmar bloqueo real por pérdida de red, latencia física, FGS operativo, supervivencia de proceso, producto final, H05/H06, permiso temporal ni remoto.

No se usó ADB, no se instaló ningún APK, no se concedieron notificaciones, no se activaron políticas ni se cambió conectividad. A56 excluido. Último estado físico conocido sigue siendo LOCAL04 desarmado/NONE/LOCAL03 restaurada; **no se releyó el teléfono** en esta unidad. No reboot, force-stop, kill, Doze, llamadas, secretos, datos de redes ni repositorios externos.

## Reglas implementadas

- Señal: `ConnectivityManager.activeNetwork` y capacidades INTERNET+VALIDATED del default actual, con segunda lectura de default para detectar intercambio. Callbacks oficiales available/lost/capabilities disparan lectura actual; no confían en capacidades de una red antigua. Identificador default solo RAM, no registrado. Nada DNS/relay/ACK/PONG/peer como autoridad de uso.
- Startup desconocido no autoriza NORMAL. NORMAL libre LOCAL requiere código adulto fresco y validación actual. Señal negativa crea deadline único elapsedRealtime+10.000; duplicados no lo prolongan. A9.999 sigue autorizado; a10.000 se revoca antes de procesar reconexión. Volver antes cancela gracia sin renovar referencia; después no abre ni borra latch: nuevo código/autorización.
- EXCEPCIÓN OFFLINE LOCAL tiene propósito adulto separado, código fresco y casilla explícita inicialmente desmarcada. Falta/retorno de red no la revocan ni cambian a NORMAL. Sin expiración automática nueva; bloqueo adulto cancela ambas. Recuperación por pregunta no concede uso. Cerrar/fondo del panel no revoca uso ya aceptado.
- Permisos solo RAM, separados de sesión adulta. DP privado versionado (`uso-red-v1.marca`/`.registro`, AtomicFile/readback/checksum) conserva únicamente obligación de bloqueo, nunca ALLOW/OFFLINE. Reinicio de proceso o boot no reconstruye uso. Error/corrupción/pérdida parcial no genera enrolamiento libre ni sobrescritura reparadora. Checksum no es ancla externa contra root o pérdida total.
- Armar RED usa piloto MAIN autoboot y snapshot CE previo; no activa LOCAL03. Uso libre restaura baseline Lock Task conservando snapshot/enrolamiento de boot. Nueva revocación invalida intento previo y pide reenforcement desde baseline sin recapturarlo. Comparación acepta exclusivamente baseline o par de políticas propio. Keyguard conserva obligación pendiente y señales oficiales; no se declara LOCKED por Keyguard.
- FINALIZAR RED autentica, revoca referencias, cancela grace/callback antes de desarmar boot y restaurar snapshot; elimina registro red solo después del cierre confirmado. Error no acredita restauración. Rollback DEBUG/DUMP LOCAL05 separado, no authority normal, excluido release; no ensayado desde estado aplicado. LOCAL04 rollback rechaza desarme directo mientras LOCAL05 activo en RAM.
- Después de recreación, observador no se reinicia silenciosamente: panel ofrece reanudar observador con código fresco (no concede uso); después nuevo NORMAL/OFFLINE. Corrupción queda ERROR, sin bypass.

## Servicio y manifest

FGS MAIN visible `ControlRedService`, no exportado, START_NOT_STICKY; `systemExempted` con comprobación Device Owner/API34/notificaciones y canal no deshabilitado. Observa mientras se usan apps externas. Sin sondeo, tráfico, wakelock, alarma, FGS falso/dataSync, HOME, suspensión Settings ni cambio automático de red. Permisos MAIN nuevos: ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED y POST_NOTIFICATIONS. Este último se solicita en UI adulta si faltan notificaciones; no se ejecutó tal solicitud físicamente y requiere coordinación antes de probar.

Fuente oficial consultada, HTTP200:
<https://developer.android.com/develop/background-work/services/fgs/service-types>
Sección System exempted: permiso FOREGROUND_SERVICE_SYSTEM_EXEMPTED; Device Owner figura como criterio alternativo elegible. **Runtime A13 no comprobado**.

`lintRelease` del AGP8.11.1 exige además SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM y falla ForegroundServicePermission en service MAIN (manifest línea18). No se añadieron alarmas, dependencias, suppress, baseline lint ni tipo distinto para eludirlo. Requiere resolver discrepancia del gate en revisión acotada; no saltarlo para instalar. Error previo lintDebug del FGS DEBUG no reejecutado ni resuelto.

## Pruebas realmente ejecutadas

Comando final, desde `android/`:

```powershell
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

Resultado final: exit1 exclusivamente al gate lintRelease; **1error/11avisos**, 26s,131tareas (25ejecutadas/106UP-TO-DATE).

- JVM app: **342debug/287release,0fallos/errores**.22casos nuevos compartidos (release repite los mismos):20network/1snapshotalternancia/1sesiónpropósitosred. Las dos tareas JVM ejecutadas en comando final.
- Cubren frontera/gracia/reconexión/latch/duplicados/unknown/defaultswap/revisióncallbackviejo/generaciónobservadorviejo/OFFLINE/autoridadrechazada/fondo/bloquear/relojboot/procesorecreado/IOreadback/corrupciónpersistente/refreadbackviejo/cierre/codec/snapshotbaseline y fines adultos frescos. La selección default es prueba pura, no prueba callback Android real.
- `core-protocol:test` **UP-TO-DATE**, no afirmar nueva ejecución de vectores CP/1.
- APKdebug/releaseunsigned construidos. APKAndroidTest compilado (tarea final assemble UP-TO-DATE tras compilación anterior de esta unidad).3tests DP UUID/fakes creados: readback sin reconstrucción OFFLINE/corrupción/AtomicFilebackup. **No ejecutados en Android**, ninguna escritura instrumental DPM/credencial real.
- `git diff --check` correcto antes de cierre. Inspección propia por cadenas DEX del ZIP: ControlRedService/ControlRedAndroid/UsoRedLocal presentes debug y release; RollbackRedDebug solo debug; UsoRedDpTest ausente en ambos APKapp. No equivale al verificador previo `verificar_apk_base.py`, que no se reejecutó y conserva pendiente Fake.

APK **generados, no instalados ni ensayados**:

| Archivo | SHA256 |
|---|---|
| `android/app-child/build/outputs/apk/debug/app-child-debug.apk` | `9e201749b87f3ef2547427608630205435d99e2a755bc30c1aceadc5684e83f2` |
| `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk` | `5fabece8607f320ee9fee79bd8f6a3521ba7b5814d01603eb7fa9aaf5b725ad0` |

## Limitaciones y futura coordinación física

No trazas físicas signalnegative/deadline/solicitud/readbackLOCKED ni latencias. Handler usa tiempo de espera uptime; decisiones usan elapsedRealtime y se reevalúan ante señales oficiales: no prometer alarma exacta durante suspensión ni enforcement a10.000 exactos. FGS/proceso vivo no implica supervivencia crash/force-stop/reboot, cero ventana ni cobertura de otros firmwares. Reentrada acotada previa LOCAL04 conserva limitaciones.

Tras resolver gate, obtener NUEVA autorización antes de instalación/notificación/armado y ANTES de cortar/restaurar conectividad. Único A13 SM-A135M identificado sin series almacenadas; cada adb con destino explícito. Adulto presente/código/recuperación/emergencia/cargador/ADB y ruta manual de restaurar red. Alcance propuesto: UN NORMAL→UN corte manual autorizado→señal/gracia→LOCKEDreal→reconexión NO abre→cierre adulto/restauración. OFFLINE y cualquier corte adicional requieren aprobación concreta separada; explicar coste si datos móviles, sin activarlos automáticamente. No reboot/kills/radios shell/router/DNS/VPN/olvidarred. No llamadas de emergencia para medir.

## Rutas de la unidad y siguiente paso

`android/app-child/src/main/java/dev/controlparental/child/network/` (modelo/selección/actor/FGS); lock/{SnapshotLockLocal,BloqueoLocalAndroid}; boot/ArranquePilotoAndroid; local/SesionAdulto; ui/{AccesoAdultoActivity,LauncherActivity}; manifestMAIN/DEBUG; strings; tests JVM network/lock/local; androidTest/network/UsoRedDpTest; debug/local/{RollbackRedDebug,RollbackArranqueDebug}; ESTADO/relevo/esta evidencia.

Sin CP/1/CP/2schema/fixtures/planes/ADR/deps/padre/Render/QR/push. Unidad entregada con impedimento concreto; detener. Próximo paso únicamente revisión/asignación de resolución del gate FGS y, después, autorización física específica; no otra tarea automática.
