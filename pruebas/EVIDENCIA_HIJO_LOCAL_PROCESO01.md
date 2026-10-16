# LOCAL-PROCESO01 — fallo físico, corrección y comprobación

## Resultado final de esta continuación

Código en `8400a1c` y `270dd65`. Los dos casos coordinados ahora funcionan:
**bloqueo manual después de permitir uso** y **recuperación automática tras matar
el proceso con otra app delante**. Último ADB: **LOCKED**, proceso presente,
Owner y protección conservados. No certifica todos los casos H12 ni bloqueo sin
interrupción: se observó una ventana de recuperación de aproximadamente5,2s.

### Segunda actualización y confirmación de la causa

Permiso DIRECTO **«Sí, actualizar y probar»** para UNA actualización adicional,
lecturas de bloqueo/trazas y prueba manual. Único A13SM-A135M reconfirmado, API34,
APK previo56739f57…93e235 igual al verificado. UNA install-r-t **Success** al nuevo
SHA256 **29bcc2ed0e91b3a5b8cc62990fc315ebe75e7ed0511aabecbc9851f6ee8af93d**,
hash instalado confirmado. Cero diferencias de Owner/protección/directorio/
antigüedad respecto al baseline. Continuidad de directorio no es una auditoría
semántica de credenciales/PAIR: no se leyeron esos contenidos.

La traza propia en el A13 mostró **LISTA_VACIA_ANDROID elementos=1 vacios=1**:
la API efectivamente devolvía el único string vacío. A continuación
**fase=LOCKED causa=NINGUNA**, READBACK_LOCKED y ADBLOCKED. La compatibilidad
limitada resuelve esta discrepancia; no se borró ni recapturó snapshot, ni se
retiró protección para lograrlo. La hipótesis AOSP de la sección siguiente queda
confirmada en la frontera de API de este dispositivo.

### Ciclo manual después de permitir

Propietario concedió NORMAL2min, cerró acceso adulto, abrió Calculadora y volvió
a BLOQUEAR AHORA antes del vencimiento: **«Sí, funciona todo»**, adulto y emergencia
visibles. Logs propios: AUTORIZACION_LOCAL_NORMAL gen13, inicio2632811ms y plazo
2752811ms; RESTAURADO/NONE, BLOQUEAR_ADULTO gen14 a2683183ms y LOCKED/NINGUNA.
Estos elapsed son del A13. ADB posterior confirmó **LOCKED**: el resultado no se
deduce sólo de la etiqueta de la interfaz ni del vencimiento del tiempo.

### Muerte real del proceso corregido

Permiso DIRECTO nuevo **«Sí, probar y recuperar»** para UNA terminación del proceso
propio, hasta30s de observación y apertura del hijo sólo si falla. Preparación
humana **«Lista, Calculadora abierta»** con otro NORMAL2min. Preflight confirmó
APK29bcc2ed…8af93d, modelo, UID de depuración, PID único, NONE y otra app reanudada;
no se guardó contenido/nombre de esa app porADB. Cada comando dirigido al A13
mediante adb-s, sin seriales en repositorio, A56 sin consulta dirigida.

UNA `run-as dev.controlparental.child /system/bin/kill -9 <pid>` con salida correcta.
No force-stop/root/reboot/red/apagar pantalla ni segunda terminación:

| Transcurrido del host (ms) | Proceso | LockTask |
|---:|---|---|
| 346 | AUSENTE | NONE |
| 1585 | PID_NUEVO | NONE |
| 2799 | PID_NUEVO | NONE |
| 4003 | PID_NUEVO | NONE |
| 5182 | PID_NUEVO | LOCKED |

Stopwatch del host incluye latenciaADB; primera muestra LOCKED a5182ms, anterior
NONE a4003ms. No afirmar instante exacto ni garantía de máximo5,182s. **No fue
necesaria apertura de recuperación porADB.** Nuevo proceso registró carga
BLOQUEADO/sin vencimiento ni permiso, SOLICITAR_LOCKED y LOCKED/NINGUNA.
Propietario confirmó **«Sí, volvió solo y funciona»**, sin intervenir, acceso
adulto/cierre/emergencia visible correctos; lo dejó bloqueado. Lectura final:
**LOCKED/proceso presente**, protección y Owner con cero diferencias.

### Cierre y límites

Último APK instalado29bcc2ed…8af93d; JVM172/0fallos/builddebug/lintRelease0errores/
27avisos ya acreditados abajo, no suites repetidas tras la prueba física.
Guía/ESTADO/relevo actualizados. Esta continuación consumió dos permisos de
actualización (una ejecución por permiso) y uno de muerte de proceso. No más
operaciones físicas sin alcance nuevo. No nuevo push ni trabajo remoto.

Pendientes generales conservados: pantalla apagada/otra app hasta vencimiento,
recuperación por pregunta completa, marcador de emergencia bajo políticas actuales,
otros métodos de terminar procesos y variantes de reinicio. El reinicio humano
anterior sigue siendo evidencia histórica, no repetido con este APK. La ventana
de recreación existe y Android decide el plazo; el arreglo no promete ejecución
permanente ni protección frente a root/reset físico.

## Corrección retomada — 15 de octubre de 2026

### Avance posterior: primera actualización y fallo restante

Código inicial `8400a1c`. Permiso DIRECTO **«Sí, actualizar y comprobar»** para
lecturas, UNA actualización conservadora y prueba manual; trazas propias si falla.
Único A13SM-A135M y mismo destino comprobados, cada comando dirigido con adb-s;
A56 sin consultas dirigidas. APK previo e097e002…33bce5, paquete/versión/certificado
comparados con candidato. La copia del APK se completó, pero PowerShell trató su
progreso stderr como error: se reanudaron sólo lecturas verificando hash de la copia;
no instalación intentada en esa primera salida fallida.

UNA `install -r -t` **Success**, hash instalado
**56739f579540f214b14292f3ab08a0f1db87b2fe6cf2f5b4d6697a59e493e235** confirmado.
Protección/Owner/directorio/firstInstallTime sin diferencias respecto a preflight.
El propietario probó BLOQUEAR AHORA y respondió **Estado por comprobar**; ADB
confirmó NONE. Traces limitadas al PID propio y tags PilotoArranque/ControlRedLocal:
carga bloqueada, observador/red operativos, BLOQUEAR_ADULTO y READBACK_ERROR.
Diagnóstico visible reportado por propietario: **POLITICA_AJENA**. No kill/reboot.

Permiso adicional **«Sí, comparar ese archivo»**: lectura puntual run-as/base64 de
`no_backup/lock-local-v1.snapshot`, decodificada en RAM, sólo resumen en temporal
externo. Checksum válido, versión1, paquetes0, flags16. No escritura ni borrado de
snapshot, no lectura de credenciales/Keystore/PAIR/pending. Dumpsys DevicePolicy
también muestra paquetes vacíos/flags16 para nuestro admin y política resuelta;
ese texto no distingue conjunto vacío de conjunto con único string vacío.

Fuente AOSP consultada y decodificada: Android14 `LockTaskPolicySerializer` guarda
String.join(";", packages) y recupera Set.of(packagesStr.split(";")); para lista
vacía produce **un elemento vacío**. No afirmar todavía que el firmware exacto
del A13 hace esto hasta leer la traza de API de la siguiente versión.
https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/services/devicepolicy/java/com/android/server/devicepolicy/LockTaskPolicySerializer.java

Segundo bloque: `lock/PaquetesLockLeidos.kt`, `PoliticasLockAndroid.kt`,
`BloqueoLocalAndroid.kt`, `SnapshotLockLocalTest.kt`. Normalización exclusivamente
de array con UN string vacío a conjunto vacío, en frontera de lectura Android;
no recorta espacios ni filtra mezclas/paquetes/flags distintos, no migra snapshot
ni adopta una política externa. Traza LISTA_VACIA_ANDROID sólo cuenta elementos y
vacíos; traza de transición sólo fase/causa. Pruebas recrean join/split Java AOSP,
enforcement con adaptador real/puertos falsos y conservación byte a byte del
snapshot; rechazan paquetes ajenos, espacios, mezcla y flags distintos sin writes.

Mismo comando focal documentado abajo: **SUCCESS18s/17 ejecutadas/53 UP-TO-DATE**,
**172 casos/0 fallos/0 errores/0 omitidos**, 2 nuevos (12 nuevos en toda corrección).
BuildDebug/lintRelease correctos, 0 errores/27 avisos; diff-check correcto.
Nuevo APK NO INSTALADO SHA256
**29bcc2ed0e91b3a5b8cc62990fc315ebe75e7ed0511aabecbc9851f6ee8af93d**,
apksigner válido/certificado histórico igual. Último instalado sigue56739f57…93e235.
Siguiente: pedir otro permiso concreto de UNA actualización y repetir bloqueo;
terminación de proceso aún NO autorizada para esta continuación.

### Primera corrección local (histórico de preparación)

El propietario anuló el cierre: **«no, retomemos, necesito solucionar eso especifico»**.
Se continúa este subcaso de H12 (muerte de proceso), con las rutas locales de
enforcement/servicio H06/H07. Se consultaron SPEC H5/H7, ADR-H-001/004 y la API
oficial `Service.START_STICKY`: Android intenta recrear el servicio y puede
entregar intent null; no hay garantía de plazo ni ejecución ininterrumpida.
Fuente: https://developer.android.com/reference/android/app/Service#START_STICKY

### Defectos y corrección de código

- El servicio retornaba START_NOT_STICKY. Ahora solicita START_STICKY y vuelve a
  cargar exclusivamente la obligación preparada, sin recuperar permiso/minutos
  ni sesión adulta. Al quedar bloqueado desde la carga, solicita enforcement sin
  esperar una transición desde permitido que nunca ocurrió en ese proceso.
- Bloquear desde el panel pasaba applicationContext: con escritor CONTROL presente,
  la ruta intentaba encontrar una Activity del piloto remoto, ausente en uso local.
  Ahora recibe la Activity adulta, solicita reentrada en todos los casos aceptados
  y comunica resultado después del callback y lectura LOCKED. Una intención durable
  no es APPLIED; callback obsoleto no revoca una autorización posterior.
- Arranque descartaba todo intento con la misma referencia, incluso tras error o
  tras permitir uso (NONE). Ahora sólo omite el intento pendiente o LOCKED leído;
  errores permiten una nueva señal. Señales repetidas no invalidan un intento que
  ya se está aplicando. Launcher atiende también SINGLE_TOP/onNewIntent.
- Launcher reanuda un observador previamente preparado cuando reaparece el proceso.
  No crea enrolamiento, no concede uso y evita bucles de arranque tras un fallo.
  Desarmado/ausente/CIERRE no reactiva servicio. Carga de CIERRE queda FINALIZANDO
  incluso en boot nuevo, conservando bytes originales y requiriendo finalización
  explícita; onDestroy no transforma esa desactivación en un fallo nuevo.

Son defectos comprobados por inspección y regresión local. La secuencia exacta
de «control local» y toda la causa del fallo físico anterior no se reconstruyeron
mediante trazas: no se atribuye a una acción errónea del propietario ni se declara
resuelto físicamente antes de actualizar y repetir el caso.

### Verificación local realmente ejecutada

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests 'dev.controlparental.child.network.*' --tests 'dev.controlparental.child.lock.*' --tests 'dev.controlparental.child.boot.*' --tests 'dev.controlparental.child.local.SesionAdultoTest' --tests 'dev.controlparental.child.orders.EscritorControlCp2Test' --tests 'dev.controlparental.child.protection.ProteccionLocalTest' --tests 'dev.controlparental.child.ui.*' :app-child:assembleDebug :app-child:lintRelease --offline --console=plain
```

Primera pasada SUCCESS30s/17 ejecutadas/53 UP-TO-DATE. Tras acotar también la
señal automática en estado incierto, pasada final **SUCCESS20s/14 ejecutadas/56
UP-TO-DATE**, XML **170 casos de 17 clases, 0 fallos/errores/omitidos**; 10 casos
nuevos distintos, no contar las dos pasadas como casos adicionales.
Pruebas nuevas: recreación con NORMAL/OFFLINE libre o temporal, cierre interrumpido
con boot igual/nuevo, ausencia/desarme/corrupción, solicitud real del puerto aun
con writer, PENDING antes del callback, NONE/PINNED/incierto nunca VERIFIED,
ocupado/duplicado, callback antiguo frente a permiso nuevo, timeout y reintento
en misma generación, coalescencia de señales. Modelos/escritor reales; disco,
reloj y puertos Android FALSOS en JVM. No prueba de scheduling Android ni UI física.
LintRelease analizado: **0 errores/27 avisos** (reporte sin cambios). LintDebug no
reejecutado; su fallo histórico del prototipo ServicioTemporal sigue registrado.

APK candidato `android/app-child/build/outputs/apk/debug/app-child-debug.apk`:
SHA256 **56739f579540f214b14292f3ab08a0f1db87b2fe6cf2f5b4d6697a59e493e235**.
Firma apksigner válida, certificado SHA256 histórico
**39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439**.
Aapt: paquete dev.controlparental.child, versión1/0.1.0, min31/target36, debug,
LauncherActivity. Diff-check correcto. **NO INSTALADO**: el A13 sigue con el APK
e097e002…33bce5 según última evidencia, no reconsultado durante esta corrección.

Rutas modificadas bajo `android/app-child/src/main/java/dev/controlparental/child/`:
`boot/ArranquePilotoAndroid.kt`, nuevo `boot/ReentradaPiloto.kt`,
`network/ControlRedAndroid.kt`, `ControlRedService.kt`, `UsoRedLocal.kt`, nuevo
`network/RecuperacionControlLocal.kt`, nuevo `lock/ConfirmacionBloqueoLocal.kt`,
`ui/AccesoAdultoActivity.kt` y `LauncherActivity.kt`. Tres archivos nuevos de
pruebas bajo src/test para RecuperacionControlLocal, ConfirmacionBloqueoLocal y
ReentradaPiloto. Documentación: esta evidencia, ESTADO, guía y relevo UI_LOCAL01.

### Siguiente comprobación necesaria

Solicitar permiso DIRECTO nuevo para preflight del único A13SM-A135M, UNA
actualización conservadora y bloqueo manual con lectura real de LockTask. Confirmar
pantalla/acceso adulto/emergencia por botón averiado. Después coordinar una nueva
muerte de proceso con otra app delante, medir ventana y recuperar si falla.
No reutilizar el permiso consumido de kill/instalación. El tiempo de recreación
depende de Android; que compile START_STICKY no acredita recuperación ni cero ventana.
Conservar datos, credencial, identidad/PAIR/pending y protección. Sin clear/reset,
políticas genéricas nuevas, contratos modificados, otros repositorios ni push.

## Evidencia física anterior a la corrección (conservada)

## Reinicio reportado por el propietario

El propietario reinició manualmente por iniciativa propia y respondió:
- Antes: **Permitido por tiempo**.
- Después de PINAndroid, sin nuevo permiso: **Sigue bloqueado**, acceso adulto y
  emergencia disponibles según la opción confirmada.
No reboot ejecutado por el agente ni medición de ventana de arranque. No
atribuir todos los estados posteriores al reinicio: el propietario continuó
autorizando uso y manipulando «control local» (acción exacta no identificada).

## Prueba de proceso autorizada y realmente ejecutada

Permiso DIRECTO **«Sí, probar y recuperar»** a preparar NORMAL2min con otraapp
delante, cerrar UNA vez sólo el proceso del hijo por UID de depuración,
comprobar recuperación y abrir LauncherActivity si falla. Sin force-stop/root/
borrados/retirarprotección. Preparación humana confirmada **«Lista, otra app abierta»**.

Preflight: únicoA13SM-A135M anunciado y confirmado, mismo destino; APK instalado
SHA256 **e097e002434aee19982eb0f643c89ef65f30665909c6f3978dd532ba8133bce5**.
UID propio de depuración comprobado. Baseline LockTaskNONE; proceso identificado
por pidof y otraapp reanudada antes del cierre, sin guardar su contenido/nombre.
Cada operación ADB dirigida por `-s`; serial/PID sólo RAM/contexto temporal
externo. A56 sin consultas dirigidas ni cambios.

**UNA terminación `run-as dev.controlparental.child /system/bin/kill -9 <pid>`**,
salida correcta; nunca `am force-stop`. Muestras desde Stopwatch del host después
de iniciar la operación (incluye latenciaADB, no cronómetroAndroid):

| elapsed ms | Proceso hijo | LockTask |
|---:|---|---|
| 359 | AUSENTE | NONE |
| 1589 | AUSENTE | NONE |
| 2805 | AUSENTE | NONE |
| 4025 | AUSENTE | NONE |
| 5247 | AUSENTE | NONE |
| 6460 | AUSENTE | NONE |
| 7678 | AUSENTE | NONE |
| 8893 | AUSENTE | NONE |
| 10112 | AUSENTE | NONE |
| 11347 | AUSENTE | NONE |
| 12569 | AUSENTE | NONE |
| 13793 | AUSENTE | NONE |

**FALLO:** no recuperación automática durante13,793s observados, sin bloqueo
real. No es una prueba aprobada porque la UI del hijo se cierre o no muestre apps.
Código leído: ControlRedService retorna START_NOT_STICKY; coherente con ausencia
de reinicio automático, pero no explica por sí solo todo el fallo posterior.
No se cambió el código para ocultar o saltar esta prueba.

## Recuperación intentada y discrepancia confirmada

Se usó UNA apertura autorizada de LauncherActivity mediante am-start explícito.
Android informó que trajo su task existente al frente. Muestras posteriores a
105/729/1332/1945/2545/3153/3765/4367ms: **NONE en todas**. No recuperaciónLOCKED.
Comparación de protección/Owner con baseline: cero diferencias; protección activa
conservada, no permiso de uso concedido desdeADB.

Se pidió recuperación MANUAL: código adulto → BLOQUEAR AHORA → código. Propietario
respondió inicialmente **«Sí, quedó bloqueado»**. Nueva lectura: **LockTaskNONE**,
proceso hijo presente. Se aclaró discrepancia y el propietario confirmó el texto
exacto **«Estado por comprobar»**. Último reporte: trasreiniciar sí bloqueó,
después lo desbloqueó y actuó sobre «control local»; ahora al pulsar Bloquearahora
puede cerrar la app y sigue desbloqueado. **No registrar recuperación exitosa.**

Se pidió permiso de lectura de trazas técnicas; la respuesta fue esa descripción
del problema, no autorización inequívoca. No se leyeron logs ni archivosprivados/
CE/DP/Keystore/códigos/capturas. No más kill, reset, instalación, redes o políticas.

## Cierre solicitado: «haz push y cerramos ya»

Se detiene investigación física y de código por instrucción expresa. Fallo de
continuidad/enforcement **ABIERTO**, incluido bloquear manual tras la secuencia.
Último estado verificado **NONE/proceso presente**, UIhumana Estado por comprobar;
no dejar anotado «teléfono bloqueado» ni «listo para versiónfinal». Antidesinstalación
continúa activa según última comparación, independiente de LockTask.

Relevo: reproducir/diagnosticar secuencia exacta de «control local» y revisar
ControlRedService/ControlRedAndroid/ArranquePilotoAndroid/TransicionLocal, con
nuevo permiso físico concreto. Muerte de proceso con otraapp es bloqueante para
certificar control. No asumir que START_STICKY solo arregla restauración o garantiza
cero ventana. Conservar identidad/PAIR/pending/protección; ninguna reparación por
clear/reset. Padre/Render/C8 siguen pausados.

Sólo evidencia/ESTADO/guía/relevo modificados en este cierre. Sin nuevos builds/
tests porque APK inalterado. Push solicitado exclusivamente del repositorio hijo
a su originGitHub `jdug-jadodev/parental_hijo_movil`, sin forzar; resultado se
comunicará tras ejecutar. No operación sobre Render ni otro repositorio.
