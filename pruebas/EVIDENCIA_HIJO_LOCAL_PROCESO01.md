# LOCAL-PROCESO01 — fallo físico y cierre solicitado

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
