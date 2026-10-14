# HIJO-LOCAL05-R1 — gate FGS y preparación de validación

Fecha: 14 de octubre de 2026. Base `b2f59f8`. Estado: **PREPARADO_FISICO, pendiente de autorización específica**. Runtime Device Owner/FGS y bloqueo por red no acreditados. Reglas LOCAL05 intactas; no tarea nueva ni temporales/remoto.

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

APK finales **generados, no instalados ni ensayados**:

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
