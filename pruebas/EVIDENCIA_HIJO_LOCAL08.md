# HIJO-LOCAL08 — OFFLINE temporal y recorrido Keyguard

14 de octubre de 2026. Base `45476e6`. **PARCIAL_OFFLINE_1MIN_VENCE_Y_RESTAURADO; vencimiento con pantalla apagada/Keyguard pendiente NO acreditado**. Sin cambios de código ni instalación; cierre en commit que incorpora esta evidencia.

## Alcance y procedencia

- LOCAL07 sigue cancelada: no restricciones ni pruebas DNS/VPN. El único cambio pendiente inicial era `ESTADO.md`, sección «Decisión del propietario — LOCAL07 cancelada». Se comparó con HEAD: corresponde al registro solicitado expresamente por el propietario en esta conversación, no a implementación LOCAL08 ni a código ajeno. Se preserva íntegro y se incorpora documentalmente al cierre tras revisar su procedencia.
- Regla NORMAL10s y reconexión no abre sin autorización nueva siguen vigentes; no hubo cambio adicional del propietario que las contradiga. EXCEPCIÓN_OFFLINE solo adulta/local/explícita, sin otorgamiento automático.
- Lectura de MAIN/LOCAL06: actorFGSvivo/AlarmListener/obligaciónDP/readbacks/Keyguard existentes; no defecto concreto previo que exigiera modificar o reinstalar. No tests/instrumentación nuevos ni DPMwrites instrumentales. Este recorrido no cierra H05/H06/productofinal/remoto/bootuso/Doze/crash/24hfísico/ceroescape.

## Autorizaciones nuevas y baseline

1. **«Autorizar preparación A13»**: seleccionar únicoSM-A135M/leerbaseline/hashpropio/abrirlauncher, sin armado/corte/permiso/apagado. A13 seleccionado por modelo anunciado, getpropSM-A135M comprobado; todos los comandos posteriores adb-s mismo destino en temporal externo, sin seriesrepo ni consultasA56. Opción de ruta **«App del módem, solo A13»** todavía no fue permiso de corte.
2. Baseline: REDactivo=false/listo=false/deadline=null; bootDESARMADO/enrolado=false; LOCAL02BASELINE_LOCAL/NINGUNA; LOCAL03SIN_SNAPSHOT/NINGUNA y uninstallEfectivo/controlPropio/controlGlobalPropio/controlEfectivo todosfalse; mLockTaskModeStateNONE; servicio propio sinServiceRecord. RollbacksDEBUG/DUMP disponibles/COMPROBARcorrectos, no ejecutados desde aplicado. Registro adulto conservado/sin lectura de contenidos; autenticaciones físicas posteriores prueban acceso, no recuperación por pregunta.
3. **«Autorizar ese único recorrido»**: UNarmadoMAIN/FGSvisible; UNcorte y reversión manual SOLOA13 desde appmódem del adulto, excepción nueva/reversible expresamente aprobada (NO heredada de LOCAL05R1); UNOFFLINEtemporal1min/casilla+códigofresco estandoNO_VALIDADA; UNlockNow manual por botón existente; esperar, despertar/desbloquearAndroid sin otroPERMITIR/abrirMAINmanual y cierreFINALIZARREDcodecódigofresco. Adultopresente/ADB/cargador-huella/PINAndroid no compartido/emergencia/recuperación/salida incluidos en aprobación. Sin datosmóviles añadidos por agente/routertools/radiosshell/olvidarred/DNSVPN/reboot/kills/Doze inducido.
4. Adulto **«Armado y corte realizado»**. Lecturas: FGSforeground=true/foregroundId105/types00000400(systemExempted), startRequested=true, observadorlisto=true, LockTaskLOCKED. Android inicialmente mantuvoVALIDADA pese corte manual; posteriormente NO_VALIDADA11417850. No instante del corte medido ni detección física inmediata afirmada.

## Primer permiso coordinado — referencia red gen2

Adulto **«OFFLINE aceptado, MAIN visible»** en instrucción: minutos inicialmente vacíos/casilla desmarcada; elección1min/casilla explícita/códigofresco y vueltaMAIN aún sin apagar. Se comprobó EXCEPCION_OFFLINE/NO_VALIDADA/NONE, FGSvivo y deadlineoriginal. Lectura `/proc/uptime`11628.31s: restaban aproximadamente38335ms hasta11666645, no instante humano garantizado. Se indicó pulsar inmediatamente «Ir a pantalla de bloqueo» existente/lockNow, sin despertar ni renovar.

| Evento (elapsedRealtime ms, boot5/proceso vivo) | Tiempo | Resultado |
|---|---:|---|
| ARMADO boot/gen1 |11253200|Una operación MAIN adulta|
| PREPARADO_BLOQUEADO red/gen1 |11253387|DPobligación lista|
| Señal NO_VALIDADA |11417850|Todavía BLOQUEADO, sin gracia de usoNORMAL|
| Inicio OFFLINE temporal RAM |11606645|Elección1min,deadline11666645|
| AUTORIZACION_LOCAL_EXCEPCION_OFFLINE red/gen2 |11606657|NO_VALIDADA,deadlinegracia=null|
| USO_APLICADO_NONE |11606869|Uso autorizado aplicado,+224msinicio|
| EventosOFFLINE/NO_VALIDADA |11614477..11636636|Conservan inicio11606645/deadline11666645; no revocaciónRED10s|
| ENTREGA_PLAZO gen2 |11666657|+12msdeadline, no pruebaLOCKED|
| PLAZO_REEVALUADO BLOQUEADO gen3 |11666701|Revocación/readbackobligación,+56msdeadline|
| BLOQUEO_REQUERIDO_LOCAL05 |11666724|Intención reenforcement, no éxito por sí sola|
| SOLICITAR_LOCKED |11666785|+140msdeadline|
| READBACK_LOCKED |11666982|LOCKEDreal,+337msdeadline/+197mssolicitud/+60337msinicio|

Revocación por plazo temporal con redNO_VALIDADA, sin graciaNORMAL y sin Bloquearmanual. El evento no rotula MotivoRed explícito en log: atribuciónTIEMPO corresponde a ramaMAINdeadline temporal/usoOFFLINE y contexto íntegro, no lectura de credenciales/otra política. No almacenamientoALLOWEDdurable ni nuevoinicio en esta referencia.

### Por qué el objetivo Keyguard quedó parcial

Adulto inicialmente **«Pantalla apagada, no desbloqueé»**. Lectura posterior: screenStateOFF/interactiveSLEEP, Keyguardshowing=true; power mWakefulness=Dozing (lectura de estado automático, **NO ensayo/inducciónDoze**). Para entonces el timer ya había aplicado LOCKED: traza directa SOLICITAR→READBACKLOCKED, **sin PENDIENTE_KEYGUARD durante ese vencimiento**. No se atribuyó Keyguard a LOCKED ni pantallaoff posterior a off-antes-deadline.

Se preguntó orden concreto; respuesta del adulto: **«le di un minuto, estoy seguro que ya se habia bloqueado»**. Se explicó que apagado fue posterior/vencimiento con Keyguard pendiente NO acreditado; no se dirigió segundoPERMITIR ni repetición. Instante exacto de pulsarlockNow no registrado; la confirmación y traza impiden certificar el recorrido pedido.

Adulto luego **«Android desbloqueado, LOCKED»**: solo desbloqueoAndroid, sin abrirMAINmanual/nuevaautorización parental en ese punto. Readbacks pantallaON/Keyguardshowing=false/usoBLOQUEADO/gen3/mLockTaskModeStateLOCKED. Diagnóstico pilotoPENDIENTE_KEYGUARD después de esa interacción, pero el LockTask real ya persistía LOCKED desde11666982; **ese diagnóstico no demuestra reenforcement del primer deadline trasunlock**. No se atribuyó a un nuevo intento ni a ventana cero; no nuevoREADBACKLOCKED de gen2 despuésunlock acreditado.

## Acción adicional autónoma del adulto — NO mezclar deadlines

Al solicitar reversión del único corte, adulto respondió **«lo reactive pero creo que no se conecto»**. Nueva lectura mostró VALIDADA/LOCKED pero gen5, diferente de gen3. Se verificó traza propia para esclarecerlo antes de registrar un éxito:

| Segunda autorización, fuera del recorrido coordinado | Tiempo | Resultado |
|---|---:|---|
| Inicio OFFLINE RAM red/gen4 |14581667|**2min**,deadline14701667;redVALIDADA al aceptar|
| AUTORIZACION_LOCAL_EXCEPCION_OFFLINE |14581680|Nueva acción adulta, no continuación del minuto original|
| USO_APLICADO_NONE |14581856|Uso aplicado con nuevo deadline|
| ENTREGA_PLAZO gen4 |14701679|+12ms segundo deadline|
| PLAZO_REEVALUADO BLOQUEADO gen5 |14701699|+32ms segundo deadline|
| BLOQUEO_REQUERIDO_LOCAL05 |14701730|Diagnóstico previoPENDIENTE_KEYGUARD, no pruebaKeyguardcerrado en ese instante|
| SOLICITAR_LOCKED |14701909|Segundo intento separado|
| READBACK_LOCKED |14702629|+962mssegundodeadline/+720mssolicitud; **NO valida primer retornoKeyguard**|
| RED_ACTUAL VALIDADA/gen5 |14746594 y posteriores|SigueBLOQUEADO/LOCKED, sin permisoNORMALautomático|

El agente no pidió/concedió/reintentó ese segundo permiso. El adulto explicó: **«sí, por que la app de wifi falló y no caragaba así que lo desbloquie le di otro wifi (el de mi cel) y espere a que se bloqueara de nuevo»**. Se registra como segunda autorización/cambio de conexión por decisión propia del adulto, fuera de los pasos coordinados, no pruebaOFFLINEsinred ni apagado-antes-del-primerdeadline. Ninguna sesión operó módem/otro teléfono, activó datos, inspeccionó apps/redes/SSID/IP/MAC/clave ni almacenó identificadores. No repetir para obtener verde.

## Cierre y estado final

Se indicó detener permisos/ensayos y ejecutar únicamente FINALIZARREDMAIN/códigofresco; adulto **«Finalizado y panel cerrado»**.

| Cierre | Tiempo elapsed ms |
|---|---:|
| DESACTIVAR_OBSERVER_ANTES_RESTORE red/gen6 |15775028|
| DESARMADO_ANTES_RESTORE boot |15775098|
| FINAL_RESTAURADO boot |15775272|
| FINAL_RESTAURADO red/gen=null |15775278|

Comprobado tras cierre: REDusoBLOQUEADO/activo=false/listo=false/deadline=null; inicioUso/vencimientoUso=null/gen=null en log. Ese modelo sin permiso **no significa teléfonoLOCKED**: mLockTaskModeState **NONE**. BootDESARMADO/enrolado=false, LOCAL02BASELINE_LOCAL/NINGUNA/snapshotlimpio, LOCAL03SIN_SNAPSHOT/todosfalse, servicio propio sinServiceRecord. Cuatro nombresDPpiloto (uso-red-v1.marca/.registro, boot-piloto-v1.marca/.registro) ausentes mediante existencia-only; no leer/listar storecredencial. Plazo/ref/callbacks cancelados antes de desarmar/restaurar. Sin shellrollback aplicado/Owner-PIN-ADB-grants modificados. Adulto confirma uso normal/notificación ausente/panelcerrado y finalmente **«Conexión previa restablecida»**, confirmación manual sin verificar identificadores de red. No afirmar continuidad/internetreal por ella.

APKdebug **instalado=ensayado=final existente**, SHA256 leído del base.apk propio: `48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092`. Coincide artefacto local `android/app-child/build/outputs/apk/debug/app-child-debug.apk`. Releaseunsigned existente `c410bd84857c3c5051616952254e1ed10f3dd08ef525fbbc4d0303c896b5e9b2`, sin física. **No builds/instalaciones/código nuevos en LOCAL08**.

## Comandos y pruebas realmente realizados

```text
adb devices -l # capturado/filtrado modelo, sin series mostradas
adb -s <A13> shell getprop ro.product.model
adb -s <A13> shell pm path dev.controlparental.child
adb -s <A13> shell sha256sum <base.apk propio> # salida reducida al hash
adb -s <A13> shell am start -n dev.controlparental.child/.ui.LauncherActivity # solo preparación antes de ensayo
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL05_COMPROBAR -n dev.controlparental.child/.local.RollbackRedDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL04_COMPROBAR -n dev.controlparental.child/.local.RollbackArranqueDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL02_COMPROBAR -n dev.controlparental.child/.local.RollbackLocalDebug
adb -s <A13> shell am broadcast -a dev.controlparental.child.LOCAL03_COMPROBAR -n dev.controlparental.child/.local.RollbackProteccionDebug
adb -s <A13> shell dumpsys activity activities # solo mLockTaskModeState
adb -s <A13> shell dumpsys activity services dev.controlparental.child/.network.ControlRedService # estado propio
adb -s <A13> shell dumpsys window policy # solo booleansKeyguard/estadospantalla, no ventanas/appscontenido
adb -s <A13> shell dumpsys power # solo mWakefulness
adb -s <A13> shell cat /proc/uptime # tiempo relativo, no secretos
adb -s <A13> logcat -d -s ControlRedLocal:I PilotoArranque:I *:S -v brief # solo enums/ref/tiempos/procesoensayo
adb -s <A13> shell run-as dev.controlparental.child /system/bin/toybox test -e <nombreDPpiloto concreto>
```

Sin logcat-clear, UI dumps/capturas/contenido/inventario/credenciales/códigos. Comando host antes de commit, desde android:
`gradlew :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:lintRelease --console=plain`, SUCCESS2s/57tareas/1ejecutada/56UP-TO-DATE. TestsJVM/análisis-reporte lint **UP-TO-DATE, no nueva ejecución de casos**. Resultado existente377debug/322release0fallos y lintRelease0errores11avisos; cero tests nuevos. Los4AndroidDP de LOCAL06 **no reejecutados** ni instrumentalDPM/credencial. No volver a declarar esos resultados físicos como LOCAL08. lintDebugprototype/verificadorFake previos pendientes/no reejecutados; ignoreFGS únicoR1 intacto. `git diff --check` antes del commit.

## Entrega y límites

**PARCIAL_Y_RESTAURADO**: OFFLINEtemporal1min real estandoNO_VALIDADA→vencimientooriginal→LOCKED sí; pantallaapagadaANTESdeadline→Keyguardpendiente→primerunlockreenforce sin otroPERMITIR **NO**. Segunda acción autónoma2min/redVALIDADA separada, no subsana esa carencia. Código no modificado y no fallo técnico de expiración inferido por la secuencia manual fuera de plazo. No errores ocultos, falsoPLIED ni garantíaDoze/crash/ventana cero. No pruebasDNS/VPN/LOCAL07/H05/H06/productfinal/remoto/bootuso/24hfísico.

Rutas modificadas solo `pruebas/EVIDENCIA_HIJO_LOCAL08.md`, `ESTADO.md` (registroLOCAL07 pendiente preservado) y `COORDINACION_PADRE_HIJO_RENDER.md`. Sin CP1/CP2/fixtures/planes/ADR/deps/reposajenos/push. Entregar/commitcierre/detener. Siguiente paso únicamente revisión/asignación y autorización nueva si se decide repetir el recorrido incompleto; no otro permiso, corte ni ensayo automático.
