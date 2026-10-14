# HIJO-PROT04-R1 — apagado mediante lockNow y ensayo parcial

2026-10-13. Base `08ecd81`. **PARCIAL / RESTAURADO**. Sin cambios de código,
build, reinstalación ni ajustes. Se retomó con autorización física nueva explícita.
PROT04 anterior permanece como antecedente impedido, no evidencia de este ensayo.

## Autorización y preparación

Propietario pidió repetir y explicó botón físico averiado, despertar con huella/
cargador y opción desarrollador «No apagar mientras conectado» activa. No se
consultó ni cambió esa configuración. Se acordó usar botón existente del prototipo
«Emergencia / Keyguard del sistema», que solicita lockNow oficial; no ADB para
forzar pantalla, ni nuevo código/permisos. Se explicó posible exigencia de PIN/patrón
tras lockNow; adulto confirmó presencia/cargador/credencial/salida y autorizó
**un ensayo1min**. No guardar PIN ni automatizar autenticación.

Selección mínima: único A13 anunciado SM_A135M, otros0; comprobado SM-A135M/API34,
Owner propio y NONE. Cada comando físico adb-s fijo, serie solo temporal privado.
APK instalado hash igual al preparado PROT04; instrumentación focalizada1 OK sin
snapshot. Apertura Activity y adulto confirmó checkbox/notificación/TIMER_LISTO.
Gate ADB BASELINE/NINGUNA, servicio isForeground/types00000400. No restricciones
antes de confirmar salida segura ni grant nuevo.

## Lo observado: primer permiso y Keyguard

Adulto eligió1 explícitamente, autenticó PERMITIR y confirmó Plan/permiso y pantalla
apagada después de botón Emergencia/Keyguard. Log PERMISO_CONFIRMADO restanteMs59996,
ADB NONE. Una sola espera65s en host seguida de lectura, no bucle/reintento de alarma.

```text
PERMISO_CONFIRMADO restanteMs=59996
ENTREGA_VENCIDA retrasoMs=6
VENCIDO_ENFORCEMENT_PENDIENTE interactivo=false keyguard=true usuarioBloqueado=true retrasoMs=22
```

ADB seguía NONE y FGS foreground vivo. **Vencimiento original bajo pantalla no
interactiva/Keyguard acreditado, intención pendiente, no LOCKED.** Deadline60.000ms
original; latencias monotónicas del código, no calculadas por timestamps civiles.
No apagado automático natural ni Doze forzado probado: apagado por lockNow manual.

Se indicó despertar/desbloquear Android, sin volver manualmente ni pulsar Permitir.
Adulto informó: despertó con huella, Android pidió PIN, lo introdujo y apareció
prototipo. Esa descripción no demuestra nueva autorización ni LOCKED parental.

```text
SENAL_SCREEN_ON retrasoMs=120157
VENCIDO_ENFORCEMENT_PENDIENTE interactivo=true keyguard=true usuarioBloqueado=true retrasoMs=120261
```

La lectura documenta despertar aún con Keyguard. **No aparece SENAL_USER_PRESENT ni
LOCKED asociado a la primera referencia** en logs propios recuperados. No determinar
causa (recepción de señal/tiempo/acción manual) ni medir ventana real de app accesible.
No garantía cero ventana; prototipo era ya la Activity previa, verlo no prueba enforcement.

## Desviación manual y segundo permiso: no mezclar deadlines

En la lectura posterior apareció otro PERMISO_CONFIRMADO. Se avisó, detuvo ensayo y
pidió salida segura antes de diagnosticar. Adulto aclaró expresamente **«Pulsé
Permitir otra vez»**. Por ello se registran **dos permisos físicos**, no afirmar único
minuto completado correctamente ni renovación espontánea causada por desbloquear.
No lo pidió el agente ni autorizó una repetición adicional; no loop hasta verde.
Nueva acción manual autenticada sustituyó referencia/deadline original, contaminando
comprobación de retorno al desbloquear sin nueva autorización. No corregir código
ni repetir para obtener verde en esta unidad.

```text
PERMISO_CONFIRMADO restanteMs=59996
ENTREGA_VENCIDA retrasoMs=5
DESBLOQUEO_APTO retrasoMs=12
BLOQUEO_SOLICITADO retrasoMs=18
ESPERAR_LOCKED/NINGUNA
LOCKED_VERIFICADO/NINGUNA
FINAL_LOCKED_VERIFICADO retrasoMs=397
LOCKED_DESDE_APTO_MS=386
```

**ADB LOCKED real**, pero correspondiente al **segundo** deadline, no al primero.
+397ms no latencia de desbloqueo de la primera referencia. No afirmar supervivencia
de proceso muerto/reboot/Doze/pantalla apagada al segundo vencimiento. No evidencia
de retorno autónomo desde otra app ni de ausencia de pantalla normal accesible.
Primer desbloqueo sin reemplazo sigue pendiente, incluso con vencimiento acreditado.

## Salida y estado final

Adulto autenticó «Salir con credencial Android del adulto» y confirmó uso normal/
notificación desaparecida. Logs históricos propios recuperados por PID observado
del ensayo (sin exigir que siguiera vivo):

```text
PLAN_CANCELADO
SERVICIO_TERMINADO_SIN_SUPERVIVENCIA
RESTAURADO/NINGUNA
```

ADB NONE/sin servicio; instrumentación focalizada1 OK sin snapshot; rollback ADB
final result1 RESTAURADO/NINGUNA. No último deadline observado físicamente para
descartar relock tardío; cancelación de referencia/listener/operación y readbacks
reales de restauración observados. Cancelación/emergencia bajo LOCKED no repetidas
al detectar desviación: se priorizó restauración. Sin llamadas ni capturas/contenido.

## Comandos, pruebas y APK

Sin builds/JVM/release nuevos:247 JVM de PROT04 son antecedente, no reejecutados.
Instrumentación de capacidades1 OK **dos veces**, baseline/final. Comandos reales:

```text
adb devices -l                                 # selección sin publicar series
adb -s <A13> shell getprop ro.product.model
adb -s <A13> shell getprop ro.build.version.sdk
adb -s <A13> shell pm path dev.controlparental.child
adb -s <A13> shell sha256sum <base.apk propio>
adb -s <A13> shell dumpsys device_policy         # filtro Owner propio
adb -s <A13> shell dumpsys activity activities   # filtro mLockTaskModeState
adb -s <A13> shell dumpsys activity services dev.controlparental.child # servicio propio
adb -s <A13> shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
adb -s <A13> shell am start -n dev.controlparental.child/.prototype.PrototipoBloqueoActivity
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_COMPROBAR_ADB -n dev.controlparental.child/.prototype.RollbackPrototipo
adb -s <A13> shell pidof dev.controlparental.child
adb -s <A13> shell logcat -d --pid=<PID propio observado> -s PROT02:I PROT01:I '*:S'
adb -s <A13> shell am broadcast -a dev.controlparental.child.PROT01_RESTAURAR -n dev.controlparental.child/.prototype.RollbackPrototipo
```

Tras salida pidof no devolvió proceso; manejado sin excepción y sin último logcat
en ese comando. Luego lectura histórica por PID previamente observado sí recuperó
salida/cancelación/restauración. Código de salida individual de pidof sin proceso
no registrado; ausencia manejada sin excepción. Resto sin errores reportados,
instrumentación correcta, sin builds ni matarproceso.
Sin connectedtests globales, A56, inventario, credenciales en logs o grants ocultos.

**APK realmente usado = instalado/final**, sin nueva compilación/instalación:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA-256 `5eff03e57afc9cc1046708e994aebac2c390598718b6a31cd240f4646632c5d6`.
Base código08ecd81; solo esta evidencia/ESTADO/relevo cambian en cierre.

## Resultado y siguiente coordinación

PARCIAL: primer vencimiento apagado/Keyguard/FGS vivo acreditado; primer retorno
LOCKED al desbloquear con deadline original/sin PERMITIR nuevo **no validado**.
Dos permisos, segundo manual explicitado; segundo LOCKED y salida/restauración
confirmados. Ninguna interpretación de producto/H05/H06/masterPIN/remoto/persistencia.
No nueva prueba/tarea automática, código/CP1/CP2/fixtures/planes/ADR/repos ajenos
intactos; sin gitpush/despliegue. Cerrar commit propio y esperar revisión/asignación.
