# HIJO-PROT04 — intención vencida pendiente de desbloqueo (debug)

2026-10-13. Base `478f97d`. **PREPARADO / BLOQUEADO_FISICO**: botón de
apagado/encendido averiado, declarado por el propietario durante la coordinación.
APK debug actualizado con autorización; ensayo apagado/Keyguard NO acreditado.
Final seguro NONE, sin snapshot/servicio, rollback completado y uso normal confirmado.

## Cambio mínimo y alcance

PROT02 declara en evidencia líneas106–108 que Keyguard impide entrada y el
adaptador diagnostica/restaura. Revisión propia confirmó que vencer llamaba
directamente a bloquear y no conservaba una intención aplazada por Keyguard.
Es riesgo de fuente, **no fallo físico demostrado** de la versión anterior.

Solo archivos debug/testDebug:
- `android/app-child/src/debug/java/dev/controlparental/child/prototype/VencimientoKeyguard.kt`
  mantiene referencia original/generación/boot/deadline, reloj monotónico y estados
  INACTIVO/VENCIDO_PENDIENTE/SOLICITADO/LOCKED/ERROR. Un único intento reclamable;
  señales repetidas no duplican solicitud, referencias viejas no ejecutan ni completan.
- `android/app-child/src/debug/java/dev/controlparental/child/prototype/ServicioTemporal.kt`
  conserva permiso vencido/snapshot cuando pantalla no interactiva o Keyguard/
  usuario bloqueados. No aplica paquetes/features en ese aplazamiento. Registra
  enforcement pendiente separado de LOCKED y estados de capacidades leídos.
  Receptor dinámico no exportado del servicio existente para señales protegidas
  SCREEN_ON/USER_PRESENT, con una comprobación100ms después de cada señal pertinente.
  Revalida referencia actual/boot/deadline/reloj/Keyguard/interactividad antes de
  reclamar un intento oficial de entrada. No nuevas alarmas ni bucles de reintento.
  Entrada conserva readbacks/timeout3s y rollback2s existentes, diagnóstico concreto;
  un error es terminal, no renovación del permiso ni reintento al volver a despertar.
- `android/app-child/src/testDebug/java/dev/controlparental/child/prototype/VencimientoKeyguardTest.kt`
  nueve tests nuevos: elapsed con apagado simulado/deadline original, Keyguard
  pendiente/no LOCKED, transición/solicitud única, cancelación, reemplazo/callback
  viejo, boot/reloj inválidos, temprano, resultado fallido y timeout real del modelo
  EntradaPrototipo con restauración única; fondo de administración no cancela permiso.
  Regresión existente de readbacks/errores incluida. Fakes JVM NO evidencia física.

Al cancelar salida/rollback: intención/ref invalidada y operación incrementada antes
de restore; listener cancelado. Callbacks ya encolados quedan inertes por identidad/
estado/operación; receptor se desregistra al terminar servicio. No persistencia ni
supervivencia añadidas, START_NOT_STICKY/RAM/API34+ existentes.

Sin cambios main/manifest/permisos/dependencias/CP1/CP2/fixtures/planes/ADR/Owner/
PIN/HOME/ADB ni restricciones amplias; sin servidor/repos ajenos. No ocultar encima
de Keyguard, bypass, accesibilidad, overlays, wakelock o pantalla forzada.

Fuentes oficiales consultadas por HTTP200:
- https://developer.android.com/reference/android/content/Intent : ACTION_USER_PRESENT
  señala presencia después de despertar/desaparecer Keyguard; ACTION_SCREEN_ON
  señala interactividad, no prueba de desbloqueo. Ambas protegidas por el sistema.
- https://developer.android.com/reference/android/app/KeyguardManager :
  isDeviceLocked/isKeyguardLocked. Se comprueban además de la señal, no autoridad adulta.

**Límite**: hay demora de señal/comprobación/entrada/readback; ninguna garantía de
bloquear antes de que una app normal quede accesible al desbloquear. Se añadieron
logs monotónicos de deadline/entrega/señal/apto/LOCKED para medirla, pero no hay
valores físicos en esta unidad. FGS vivo no garantiza entrega puntual en Doze ni BAL
en todos los dispositivos. Carrera si Keyguard vuelve durante entrada termina con
error/rollback existente, sin otro intento automático. Sin crash/reboot/proceso muerto.

## Coordinación física y resultado real

Se explicó ajuste y se pidió autorización específica ANTES de instalar; propietario
autorizó actualización/servicio visible sin nuevos permisos y declaró presencia/
credencial disponible. Selección adb devices -l: único A13 SM_A135M; otros0.
Modelo/API34 comprobados; todos comandos dirigidos con adb -s al mismo destino,
serie solo temporal privado, A56 no consultado. Owner propio, NONE y baseline sin
snapshot por test antes de actualizar. Instalación Success, rollback baseline
RESTAURADO/NINGUNA y apertura de Activity debug.

Adulto objetó repetición de pasos de seguridad; se explicó y redujo a confirmar
presencia/credencial/notificación preparada, sin repetir pasos ya comprobados.
Respondió sí. Se pidió y obtuvo después autorización específica para UN recorrido
de1min/apagado manual/Keyguard y regreso. ADB observó BASELINE/NINGUNA, NONE y
servicio foreground visible types00000400 antes de indicar el permiso/apagado.

La respuesta inicial «Permiso aceptado; apagada» se **corrigió** después: propietario
indicó que no podía apagar porque botón de apagado/encendido está averiado. Logs
propios solo TIMER_LISTO (más etapas CREDENCIAL/ROLLBACK_ADB previas), sin
PERMISO_CONFIRMADO/entrega/vencido/LOCKED. ADB observó NONE. Por tanto no consta
permiso comprometido ni pantalla apagada ni ejecución del recorrido autorizado.
No atribuir causa a Android/auth/gate ni afirmar fallo físico de enforcement.

Se detuvo la coordinación al conocer impedimento, sin repetir minuto, proponer
atajo físico, forzar pantalla por ADB o conceder permisos. Rollback autorizado:
result1 RESTAURADO/NINGUNA; ADB NONE/sin servicio temporal; instrumentación sin
snapshot pendiente OK. Adulto confirmó uso normal/sin notificación. No se ejercitó
salida autenticada desde LOCKED/cancelación/emergencia bajo bloqueo en PROT04;
PROT03 sigue siendo evidencia histórica de esos pasos, no prueba nueva.

## Ejecuciones y binario

Dos ejecuciones desde `android/`, ambas BUILD SUCCESSFUL:

```text
.\gradlew.bat :app-child:testDebugUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:compileReleaseKotlin --console=plain
```

Primera con ocho nuevos tests; segunda añade timeout integrado/logs de ventana.
Informes finales: **247 JVM debug,0 fallos/0 errores (9 nuevos)**, no sumar ambas
ejecuciones como tests distintos. Núcleo/compileReleaseKotlin/assembleAndroidTest
UP-TO-DATE, no nuevas pruebas núcleo/compilación release/testAPK. Inspección clases/
manifest release sin prototype/permisos debug, no APK release/DEX certificado.

Comandos físicos reales con prefijo `adb -s <A13>` salvo listado inicial:

```text
shell getprop ro.product.model / shell getprop ro.build.version.sdk
shell dumpsys device_policy                     # filtro Owner propio
shell dumpsys activity activities               # solo mLockTaskModeState
shell dumpsys activity services dev.controlparental.child # servicio propio
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
shell pidof dev.controlparental.child
shell logcat -d --pid=<proceso propio> -s PROT02:I PROT01:I '*:S'
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>
```

Instrumentación focalizada1 OK **dos veces**: baseline previo y final, no adulto
simulado/connected global. Sin llamadas/capturas/UI dumps/inventario/contenido.
Rollback dos veces (baseline/final), gate una vez; no installs adicionales.

Error real del comando final de comprobación: después de comprobar hash instalado,
pidof no devolvió proceso y PowerShell `.Trim()` sobre null produjo salida1. No se
ejecutó ese último logcat ni diff-check del mismo comando. No prueba de crash ni
supervivencia: servicio estaba ya terminado; no se mató ningún proceso. Consultas
anteriores de log sí correctas; diff-check ejecutado aparte antes de commit.

APK **preparado/instalado/final**, no APK ensayado en apagado/Keyguard:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA-256 `5eff03e57afc9cc1046708e994aebac2c390598718b6a31cd240f4646632c5d6`,
igual local/instalado comprobado. Ningún cambio de código tras instalación.

## Cierre y pendientes

PREPARADO/BLOQUEADO_FISICO, no logrado físico ni fallo de enforcement demostrado.
Final NONE/sin snapshot/servicio; rollback cancela referencia/listener/operación y
uso normal confirmado. No seguimiento físico de otro deadline para relock tardío.
APK/canal/grant de notificación previos permanecen; sin nuevas concesiones.
Botón averiado bloquea el recorrido manual previsto; cualquier alternativa/ensayo
requiere coordinación y autorización nuevas, no iniciadas aquí. Esperar revisión.
No control parental final/masterPIN/remoto/historial/Push/H05/H06/producto ni
apagado natural/Keyguard/Doze/reboot/crash acreditados. Sin gitpush ni otra tarea.
