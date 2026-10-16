# LOCAL-PANTALLA01 — vencimiento durante pantalla apagada

16 de octubre de 2026. Continuación focal de H12/H-POWER-01, base `40df856`.

## Fallo reportado y diagnóstico autorizado

El propietario reporta: NORMAL por tiempo, pantalla apagada al vencer; al despertar
puede seguir usando el teléfono. Sólo al abrir el hijo aparece el bloqueo.
Confirmó modalidad **Normal con internet** y autorizó **«Sí, conectado y autorizado»**
a lecturas de trazas propias/servicio/pantalla/Keyguard/políticas, sin instalación
ni nueva prueba en ese permiso.

Único A13SM-A135M seleccionado por anuncioADB y confirmado por getprop; mismo
destino que prueba anterior, todo adb-s. APK29bcc2ed0e91b3a5b8cc62990fc315ebe75e7ed0511aabecbc9851f6ee8af93d
verificado por hash instalado. No consultas dirigidas al A56, ni seriales en repo.
Trazas sólo PID propio y tags ControlRedLocal/PilotoArranque/BloqueoLocal; sin
capturas ni archivos privados. Contexto y salida completa en temporal externo.

Las trazas existentes confirman dos vencimientos con permiso revocado y Keyguard
pendiente, seguidos de reentrada posterior al volver a la app según reporte humano:

| Caso | Deadline elapsed A13 | Entrega | Revocación durable | Espera Keyguard | LOCKED posterior |
|---|---:|---:|---:|---:|---:|
| NORMAL5min, gen22→23 | 41502153 | 41502783 | 41502855 | 41502890 | 43140408 |
| NORMAL2min, gen24→25 | 43297539 | 43297810 | 43297837 | 43297875 | 43497260 |

La alarma llegó +630ms/+271ms respecto al plazo: **el contador sí venció**.
No atribuir todo el intervalo hasta LOCKED a uso desbloqueado: incluye pantalla
apagada y no hay marcas anteriores de SCREEN_ON/USER_PRESENT. La ruta de señales
actual sólo solicita reentrada si observa el cambio permitido→bloqueado; si ya
estaba revocado, reevaluar al despertar no basta. No se observó señal
PilotoArranque USER_PRESENT en estas secuencias; el registro NOT_EXPORTED también
puede excluir emisores privilegiados de Android con UID diferente de system.

Lectura inicial al terminar: FGS propio activo (foregroundId105/systemExempted,
startRequested=true), pantalla Awake/USB powered/stayOn=true, Keyguard showing=false,
secure=true. LockTaskNONE mientras había otro permiso NORMAL1min gen28 vigente en
las últimas trazas; no tomar ese NONE aislado como otra prueba fallida.
Owner mismo. Bloques de protección actuales: uninstallBlocked propio=true,
no_control_apps por usuario=true y global independiente sin admins/null. Comparación
textual amplia con contexto viejo dio18 diferencias; se revisaron los bloques,
no se modificaron políticas ni se presentó como una nueva desactivación.

## Corrección focal preparada

- ControlRedService recibe SCREEN_ON/USER_PRESENT/USER_UNLOCKED y consolida red y
  deadline en el actor antes de reevaluar la obligación de bloqueo. Solicita
  reentrada también cuando el permiso ya había vencido mientras estaba apagado.
- El receptor de arranque pasa USER_PRESENT/USER_UNLOCKED por esa misma ruta,
  evitando consultar permiso RAM antiguo antes de consolidar el vencimiento.
- Ambos receptores usan RECEIVER_EXPORTED **únicamente para esas acciones de
  sistema protegidas**. No aceptan acciones arbitrarias ni extras con autoridad;
  una app ordinaria no puede emitirlas. No se exporta un endpoint de desbloqueo.
- Guarda Keyguard coherente con el adaptador: UserUnlocked, isKeyguardLocked e
  isDeviceLocked; conserva espera segura, PINAndroid y emergencia.
- No se cambia duración, reloj, snapshots, credenciales, protección, CP1/CP2,
  permisos/manifest, notificaciones, tipoFGS ni estrategia de alarmas. No despierta
  la pantalla por código ni añade polling/wakelock. Doze puede diferir setExact;
  si ocurre, el evento de despertar también consolida el plazo original.

Fuentes primarias consultadas:
- https://developer.android.com/develop/background-work/background-tasks/broadcasts
  Indica RECEIVER_EXPORTED para recibir todos los broadcasts del sistema,
  incluidos emisores privilegiados fuera del UID system.
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/res/AndroidManifest.xml
  SCREEN_ON/USER_PRESENT/USER_UNLOCKED declarados protected-broadcast.
Se consultaron SPEC H5.1/H6/H7, ADR-H-004 y matriz H-POWER-01/ficha A13.

## Pruebas locales y APK

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests 'dev.controlparental.child.network.*' --tests 'dev.controlparental.child.lock.*' --tests 'dev.controlparental.child.boot.*' --tests 'dev.controlparental.child.local.SesionAdultoTest' --tests 'dev.controlparental.child.orders.EscritorControlCp2Test' --tests 'dev.controlparental.child.protection.ProteccionLocalTest' --tests 'dev.controlparental.child.ui.*' :app-child:assembleDebug :app-child:lintRelease --offline --console=plain
```

**SUCCESS1m23s/18 ejecutadas/52 UP-TO-DATE**. XML **178 casos/0 fallos/0 errores/
0 omitidos**, seis nuevos. Modelos UsoRedLocal/TransicionLocal reales; reloj,
disco, Keyguard y LockTask falsos en JVM. Casos nuevos: vencido antes de despertar
→KEYGUARD→desbloqueo sin renovar referencia; alarma diferida NORMAL/OFFLINE;
despertar antes del vencimiento; permiso posterior prevalece; cierre voluntario;
corrupción sin reparar archivos. No acreditan recepción de broadcasts del A13.

BuildDebug correcto; lintRelease **0 errores/27 avisos**, sin avisos en rutas
modificadas. LintDebug histórico no reejecutado. Diff-check correcto.
APK candidato NO INSTALADO:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA256 **e3d3a0e5b22ab9c5a0165406751eccc6441722d19ed8683b2eb58bf1efb1c761**.
Apksigner válido/certificado histórico
39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439.
Aapt paquete dev.controlparental.child/versión1-0.1.0/min31/target36/debug/LauncherActivity.

## Rutas y siguiente paso

MAIN: network/ControlRedAndroid.kt, ControlRedService.kt, nuevo
ReentradaDespertar.kt; boot/ArranquePilotoAndroid.kt. Test nuevo
network/ReentradaDespertarTest.kt. Documentación: ESTADO, esta evidencia,
HANDOFF_HIJO_UI_LOCAL01 y GUIA_HIJO_UI_LOCAL01.

Pedir permiso nuevo para UNA actualización conservadora y prueba temporal con
otra app, pantalla realmente apagada hasta después del vencimiento y despertar
sin abrir el hijo. Botón averiado: coordinar apagado y despertar por huella/PIN;
USB stayOn no equivale a reposo natural. Medir entrega/revocación/avisos oficiales/
LOCKED y confirmar adulto/emergencia. No reboot/kill/force-stop/red/Doze forzado/
clear/reset ni cambios de protección. No push ni otros repositorios. El fallo
permanece abierto hasta comprobar la corrección físicamente.
