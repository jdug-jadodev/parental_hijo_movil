# Relevo — HIJO-UI-LOCAL01

**CONTINUACIÓN ACTUAL:** el propietario pidió resolver LOCAL-PROCESO01 y anuló el
cierre. Corrección de servicio/reentrada/bloqueo adulto preparada; 170 pruebas JVM
sin fallos, buildDebug/lintRelease correctos (0 errores/27 avisos). Candidato
SHA256 `56739f579540f214b14292f3ab08a0f1db87b2fe6cf2f5b4d6697a59e493e235` NO instalado.
Último A13 verificado sigue e097e002…33bce5/NONE, fallo físico abierto. Pedir permiso
nuevo para actualización conservadora y bloqueo manual; luego coordinar muerte de
proceso y medir recuperación. Detalles/rutas en la sección inicial de
`EVIDENCIA_HIJO_LOCAL_PROCESO01.md`. El cierre/push del párrafo siguiente es histórico.

**FALLO MÁS RECIENTE, NO RESUELTO:** `pruebas/EVIDENCIA_HIJO_LOCAL_PROCESO01.md`.
Reinicio permitidoportiempo→bloqueado reportado por propietario. Después, prueba
autorizada de UNA muerte real de proceso: ausente/LockTaskNONE13,793s. Abrir hijo
no recuperóLOCKED; bloqueo manual tampoco acreditado, usuario confirma «Estado
por comprobar» y puede cerrarapp/seguirusando. Última lecturaNONE/procesopresente,
protecciónOwner sin cambios. No datos borrados ni protección retirada. Código
START_NOT_STICKY conocido; causa completa del fallo manual pendiente. Usuario
pidió **«haz push y cerramos ya»**: detener sin investigar más, commit/push repoHIJO
únicamente. No repetir pruebas ni declarar listo para entregar. Detalles/permiso
de nuevo ensayo deberán coordinarse luego. Estados exitosos inferiores históricos.

ÚLTIMA RONDA: `pruebas/EVIDENCIA_HIJO_LOCAL_VALID01.md`. Propietario reportó que
probó sininternet→bloqueo y desinstalaciónnormalimpedida, y dio por exitosa esa
prueba. Ronda cerrada PARCIAL; no certificaciónfinal. LecturasADB autorizadas
acreditaron APKe097e002…33bce5/protecciónactiva/procesopresente y snapshotNONE/
pantallaON durante sus pruebas. Estadofinal no reconsultado. Ningún cambio
ejecutado porADB. Pantallaapagada/procesomuerto/reinicio/recuperación/marcador
deemergencia bajo políticasactuales siguen pendientes; no asumir otros aprobados.

CONTINUACIÓN MÁS RECIENTE: `pruebas/EVIDENCIA_HIJO_LOCAL_COMPAT01.md` sustituye
el pendiente de preparar control. Corrección8cc4823 permite protecciónpropiaactiva
verificada, APKe097e002…33bce5 actualizado con permisoDIRECTO nuevo, protección
conservada. PreparaciónMANUAL y NORMAL1min contadobloqueado confirmados por humano;
ADBpolíticas posteriores LockTaskhijo/flags48 y proteccióntrue. Acceso adulto/
cierre/emergencia visible confirmados al final. Último estado bloqueado. No más
instalaciones ni otras pruebas físicas autorizadas. Detener; remoto pausado.

ACTUALIZACIÓN POSTENTREGA CERRADA: propietario dio permiso DIRECTO y confirmó
preparación física. UNA actualización A13 completada/verificada APK2b5ea130…15a821,
Owner/políticas/directorio continuos; UI/adulto/salidaemergencia confirmados.
Después autorizaciónDIRECTA «Sí, activar y comprobar», activación MANUAL adulta:
«Protección activa». ADBleyó uninstallBlockedhijo=true/no_control_appsusuario=true
resueltos, mismoOwner; globalindependiente sinadmins/null. POSTprotección adulto/
cierre/emergencia visible confirmados «Sí, todo funciona». No desinstalación,
recovery/reset/cambiodecódigo ni llamada probados. No se ejecutó «Preparar control
local» ni permiso/temporizador/red/boot; siguiente fase sólo con alcance directo
nuevo, no otra instalación ni remoto. Ver `EVIDENCIA_HIJO_UI_LOCAL01_ACTUALIZACION.md`.
Párrafos inferiores conservan preparación previa, superada en instalación y
protección. Detener después del commit documental.

**Preparación local terminada, validación física pendiente.** Bloque de interfaz
`af7908d`; cierre en commit de esta entrega. C8-T10 pausada/NO finalizada en
`490495d`, con sus avances y registros conservados. No reanudar mensajes antiguos
de T10/arnés ni tocar padre/Render.

Resultado: MAIN «Mi teléfono», adulto «Control del teléfono» con resumen real,
bloquear/libre/tiempo, Seguridad y diagnóstico separados. Huella/vínculo/remoto
no contaminan recorrido habitual; sólo consulta secundaria local sin conexión.
«Mis aplicaciones» exige permiso/NONE/observador listo/límites tiempo y gracia
vigentes antes de mostrar y abrir; no almacena historial ni cambia autoridad.
La protección ya existía: uninstall del hijo + controles globales de apps,
snapshot/verificación/restauración adulta. No activación automática ni nuevas
restricciones de instalación. No afirmar activa basándose en Owner o APK.

Archivos: ui/AccesoAdultoActivity, LauncherActivity, LockedScreen, nuevos
PresentacionLocal/EstiloLocal y testPresentacionLocal; manifestMAIN queries de
launcher(sinpermisoQUERY_ALL), manifestDEBUG sin icono secundario del prototipo
(componente explícito y recuperación conservados), ESTADO/evidencia/guía/relevo.
No APIs de protocolo/cambios enforcement/sesión/plazos productivos.

Comprobaciones: primer focal JVM14clases158casos0fallos; final UI8/0fallos/
0skip+assembleDebug+lintRelease SUCCESS23s; total159casosdistintos,8UI nuevos.
lintRelease0errores27avisos; lintDebug intentado y NOaprobado, error preexistente
en ServicioTemporalDEBUG/ForegroundServicePermission. No añadir permisos ni
suppress para esconderlo. Evidencia con comandos/fallos y límites exactos.

APK `android/app-child/build/outputs/apk/debug/app-child-debug.apk` SHA256
**2b5ea13052d7c45a34bf8b98f4f95667ef074e0a3a58468797e22573b815a821**,
firma verificada/certificado histórico39f25d28…a9f439/paqueteversión1-0.1.0/
min31target36/testOnly, un launcher productivo; DEXMAIN presentes/fixturesausentes.
NO instalado. Sigue instalado6221f3…250751; F04 confirmó adulto/emergencia por
reporte humano, no permisos físicos nuevos. Última protección física inactiva.

NO realizados: renderizado/navegación táctil, APIs Android reales, Keystore,
IOAtomicFile, enforcement, recuperación/actualizaciones/antidesinstalación físicas.
JVM sólo modelos/presentación con puertos falsos. Sin ADB/otrosrepos/endpoints/
push/deploy/subagentes. Detener. Próximo paso sólo tras permiso DIRECTO NUEVO aquí:
comprobar adulto y salida/emergencia (botón A13 averiado), compatibilidad y UNA
actualización conservadora si autorizada; activación/prueba local con alcance
separado explícito. ÚnicoA13SM-A135M/adb-s; A56 excluido. Incertidumbre=parar,
sin clear/reset/uninstall/migración/claves nuevas. No ejecutar esa fase por este
relevo. Leer ESTADO, evidencia y guía de UI-LOCAL01, no repetir suites históricas.
