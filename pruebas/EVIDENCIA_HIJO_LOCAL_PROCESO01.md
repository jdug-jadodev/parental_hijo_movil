# LOCAL-PROCESO01 — fallo físico y cierre solicitado

## Corrección retomada — 15 de octubre de 2026

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
