# Evidencia H04 — estado duradero y contexto de arranque

Fecha: 9 de octubre de 2026. Estado: PENDIENTE_VALIDACION Android; implementación compilada y pruebas JVM aprobadas.

## Primera unidad — commit 7122a5c

Modelos de snapshot local, codec/checksum, consistencia criptográfica y transacción
serializada mediante puerto de archivo. Tests usan fake, no AtomicFile Android.

Desde android/:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain
```

Resultado: BUILD SUCCESSFUL; 252 tests JVM, 0 fallos/errores/omitidos.
Incluye 15 tests nuevos H04; núcleo UP-TO-DATE y tests del hijo ejecutados.
100 actualizaciones concurrentes de prueba no pierden valores. Se conservan
inicio/política/secuencia/etapa al recrear el store; corrupción no es ausencia.
Repetición no reinicia duración y consumo de recuperación no retrocede dentro
del mismo vínculo. Los tests usan públicas/sobres congelados, no claves reales.

## Segunda unidad — compilada y probada en JVM

- Archivo control-state.json en Device Protected mediante AtomicFile.
- Un store compartido por ruta/proceso y una cola de IO; AtomicFile no es un lock.
- Lectura acotada; archivo vacío/excesivo/inaccesible o restos parciales no autorizan
  inicialización por defecto. Escritura con finishWrite/failWrite y lectura posterior.
- Contexto estable si BOOT_COUNT coincide y elapsedRealtime no retrocede.
- Nuevo/incierto arranque crea bootId nuevo y authorizedBootId=null. Conserva
  última orden, política, contador y estado público de recuperación.
- Sin BOOT_COUNT fiable, dos callbacks no se presumen del mismo boot. Puede crear
  otro contexto cerrado; no se otorga permiso por intentar mejorar disponibilidad.
- BootReceiver no exportado/directBootAware, con goAsync y sin red/Keystore/launcher
  automático. Solo opera cuando la app es Owner real.
- Launcher comprueba estado en IO fuera del hilo principal; error visible conserva
  emergencia y no reabre enrolamiento. No es un segundo motor de políticas.

Construcción iniciada desde android/:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
```

Resultado de cierre: BUILD SUCCESSFUL, repetido tras mejorar las aserciones y
cleanup de tests. Informes: **262 tests JVM, 0 fallos, 0 errores, 0 omitidos**:
25 nuevos H04 (16 de codec/store y 9 de boot) más los 237 anteriores. Núcleo
UP-TO-DATE y tests del hijo ejecutados. APK debug/instrumentado y release sin
firmar construidos; lint debug 0 errores y 10 avisos de actualización, lint vital
release correcto. Release sigue sin firma final y no se instala.

Desde raíz: `python -B pruebas/verificar_apk_base.py` y
`python -B pruebas/verificar_apk_h01.py`: ambos OK. App sin fixtures/clases de tests;
debug testOnly separado de release y BootReceiver/declaraciones presentes.
El inspector H01 se amplió para comprobar boot y permitir exclusivamente
RECEIVE_BOOT_COMPLETED más el permiso signature interno AndroidX. No se añadieron
permisos de monitoreo ni ejecución solicitados al usuario.

Nueve tests JVM de contexto de boot y cinco tests Android compilados NO EJECUTADOS. Los Android
solo escriben/borran tres nombres únicos de archivos temporales del paquete de
tests en Device Protected; NO acceden al control-state.json de producción ni
modifican Owner, credencial, red o pantalla. No ejecutan reinicio ni simulan que
el equipo esté antes del primer desbloqueo; esa situación requiere prueba real.

## Formato local, no cambio a CP/1

Contenedor {formatVersion:1, payloadB64, sha256B64}, con hash SHA-256 de los bytes
decodificados del payload. Registro schemaVersion=1, binding público opcional,
recoveryConsumed/recoveryFailureCount/recoveryWaitPending, bootContext, policy,
lastCommand, approvedPackages, provisioningStage y lastEnforcementErrorCode.
Solo se conserva la última orden firmada. Contador y política no se separan.

El registro interno usa cadenas decimales para secuencia y tiempos monotónicos
de 64 bits. BOOT_COUNT es entero opcional. Los campos null son explícitos y se
rechazan campos ausentes/desconocidos, tipos incorrectos y estados incoherentes.
Límites locales: registro 11000 bytes, archivo 16384 bytes, hasta 32 paquetes
aprobados de máximo 255 caracteres. Son límites locales, no cambios al contrato.
El código usa el parser/base64 públicos existentes sin ampliar los límites CP/1.

La firma de la última SET_POLICY se revalida con la pública del vínculo y se
comparan pairId, commandId, seq, hash, acción, duración y boot de política.
El inicio monotónico es un dato local de aceptación duradera, no se reconstruye
al cargar. APPLIED persistido es un resultado histórico, NO prueba de restricciones
actuales ni permiso para reenviar un ACK viejo como si aplicara al nuevo arranque.

Sin archivo, solo se prepara un contexto local sin vínculo/orden/autorización.
Un archivo inválido nunca se borra o sustituye por esa preparación. No se realiza
enrolamiento en H04. H08/H09 siguen siendo responsables de autorizar vínculo y
comandos; transformar/escribir un snapshot NO los autoriza por sí mismo.

El checksum detecta corrupción accidental, NO modificaciones deliberadas por
root ni eliminación/restauración arbitraria de archivos. No hay privadas, códigos
de recuperación, contraseñas Wi-Fi ni historial. recoveryWaitPending conserva
solo un indicador: la espera/autorización de recuperación corresponde a H05,
no está implementada aquí. Mantenimiento y canal no se persisten como permisos.

## Límites al cerrar la implementación (antes del intento físico)

- Instalación de H04 o ejecución de sus cinco tests Android en el A13.
- Recepción real de ambos broadcasts, arranque antes del PIN Android, recreación
  del proceso y observación del archivo DP en el firmware A135MUBSDDZB3.
- Recuperación, migraciones de producción, quiosco, temporizador Android o red.

El A13 conserva H02; no se ha actualizado ni reiniciado en esta tarea.
El A56 no se toca. G0–G4 no se aprueban por estos tests o por compilar el receiver.

## Primer intento físico autorizado — 9 de octubre de 2026

El propietario autorizó actualizar y ejecutar tests SIN reiniciar/endurecer.
Un único SM_A135M anunciado por ADB fue seleccionado; modelo SM-A135M
comprobado antes de cada operación mediante el mismo destino `adb -s`.
No se consultó ni cambió el A56 y no se guardaron series.
Owner previo: nuestra app, usuario 0, DeviceOwner/Affiliated.
Ambas instalaciones `install -r -t` devolvieron Success.

SHA-256 de los APK efectivamente instalados en este intento:

- App: `580a5b1cdc106d544d97e3b2720a36c1fe43ee82419cb427055677db1fe4eb3c`.
- Tests (versión inicial): `1b490d9acc70159caa161c488344c9d571dc434aa124d509b0bee0555cbeb641`.

```powershell
adb -s $serieA13 shell am instrument -w -r -e exigirDeviceOwner true -e exigirCredencialAndroid true dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: **Tests run: 12, Failures: 4**, Time: 1.98 s. NO aprobado.
Siete tests anteriores y declaración/permisos del BootReceiver correctos.
Fallaron cuatro tests de archivo: dos ENOENT al intentar escribir dentro del
directorio DP del paquete test y dos ErrorEstadoLocal al intentar leer una ruta
ausente cuyo padre no era accesible. No demuestran un fallo del codec ni éxito
de AtomicFile: la preparación del entorno falló antes de esos pasos.

La instrumentación corre en el proceso/UID del destino, no del APK test.
Se corrigió el helper para usar targetContext y crear un subdirectorio UUID único
dentro de filesDir Device Protected del destino. Solo escribe estado-prueba.json
y sus dos compañeros AtomicFile allí; limpieza explícita de esos tres nombres
y del directorio vacío, sin borrado recursivo. **Nunca accede a control-state.json**.
Esto reemplaza el diseño anterior de archivos en el paquete test; requiere nueva
coordinación antes de instalar/ejecutar el APK test corregido.

Tras la corrección: comando Gradle completo arriba, BUILD SUCCESSFUL; APK test
recompilado. JVM UP-TO-DATE (262 resultados anteriores), no nueva ejecución.
Corrección instrumentada NO EJECUTADA. La app no cambió en esta corrección.
No se abrió launcher por ADB, reinició, endureció, llamó lockNow ni borró datos
de producción. El receiver de actualización puede preparar contexto local;
su ejecución y el archivo de producción no se inspeccionaron en este intento.
H04 sigue PENDIENTE_VALIDACION; boot/proceso reales y emergencia H04 pendientes.

## Repetición corregida autorizada — 9 de octubre de 2026

El propietario autorizó actualizar SOLO el APK test y repetir la instrumentación
con el aislamiento corregido. Un único A13 anunciado SM_A135M fue seleccionado
y comprobado como SM-A135M antes de cada operación; todas con el mismo `adb -s`.
Owner previo esperado confirmado, usuario 0, DeviceOwner/Affiliated.
Sin comandos al A56 ni registro de series.

- Actualización `install -r -t` SOLO del APK test: Success.
- SHA-256 APK test instalado: `4c6aa0ba13f4b9dbf791880b699709c21970787fc90f950aabf4fda17772dd33`.
- App sin reinstalar, hash instalado del primer intento conservado arriba.
- Mismo runner y opciones exigirDeviceOwner/exigirCredencialAndroid: **OK (12 tests)**,
  Time: **1.892 s**. Los cinco H04 y los siete anteriores aprobados.

Comprobados AtomicFile real en Device Protected, recreación del lector,
failWrite conservando registro anterior, corrupción intacta/error seguro,
resto .new parcial sin convertirlo en ausencia, y declaración/permisos del
receiver. Los cuatro tests de archivo crearon subdirectorios UUID únicos en DP
del destino instrumentado y comprobaron su limpieza limitada al finalizar.
NO accedieron a control-state.json ni probaron muerte del proceso real.

No se reinició, endureció, abrió marcador ni llamó lockNow. No se ejecutó nueva
construcción/JVM en esta repetición; 262 resultados JVM locales anteriores.
H04 sigue PENDIENTE_VALIDACION: broadcasts reales, arranque antes del PIN,
continuidad tras muerte de proceso y emergencia en nueva versión pendientes.
No se aprueban G0–G4 ni bloqueo efectivo por estos resultados.

## Referencia previa al reinicio manual — 9 de octubre de 2026

Propietario autoriza preparar prueba con consultas ADB de lectura y reinicio
manual realizado por él. Se seleccionó un único A13 anunciado SM_A135M y se
comprobó SM-A135M; Owner esperado presente. Sin comandos al A56.

Lecturas con destino explícito: `settings get global boot_count` y
`run-as dev.controlparental.child cat /data/user_de/0/dev.controlparental.child/files/control-state.json`.
El contenido completo se procesó en memoria; no se mostró ni guardó. Se comprobó
SHA-256 del payload contra el checksum y se extrajo solo un resumen público.
Esta inspección PowerShell no sustituye al validador completo CodecEstado.

- BOOT_COUNT sistema y registro: **2**.
- SHA-256 del bootId (no identificador del dispositivo):
  `00f4d01f06acb6e87176c4fbfc322e879ef6e988a10c3ff74caa29045ea8105d`.
- lastObservedElapsed: **2449109 ms**.
- authorizedBootId=null; binding, policy y lastCommand=null.
- provisioningStage=PREPARACION; checksum correcto.

No se escribió ni reparó el archivo de producción. Resumen sin claves/sobres ni
serie guardado fuera del repositorio, en el temporal aprobado, para comparación.
No se reinició ni mató el proceso. Se espera reinicio manual y aviso del propietario
ANTES de introducir el PIN Android para intentar lectura DP en esa fase.

Límite del escenario: no hay vínculo ni política real aceptada. Puede comprobarse
contexto de preparación y callbacks, no H-BOOT-01 desde ALLOW ni H-STATE-01 entre
aceptación/enforcement o continuidad de un temporizador real. Esos casos quedan
pendientes de sus componentes y no se simularán escribiendo órdenes en producción.

## Intento de observación antes del PIN — detenido

Tras el aviso «listo» del propietario se enumeró ADB para seleccionar el A13.
La única entrada disponible anuncia SM_A566E (A56); no hay SM_A135M anunciado.
Se detuvo antes de dirigir cualquier comando a un teléfono. No se consultó ni
modificó el A56, ni se reinició ADB o intentó eludir la protección USB del A13.
No se leyó BOOT_COUNT, estado de desbloqueo o archivo DP después del reinicio.
Esto NO prueba que BootReceiver fallara: falta acceso al destino comprobado.
Solicitar comprobar cable/conexión del A13 sin introducir aún el PIN; si no
aparece en ADB, registrar esta fase como no observable y coordinar desbloqueo.

## Lectura después del primer desbloqueo — 9 de octubre de 2026

El propietario informó que ADB se desconecta al reiniciar; se le indicó desbloquear
solo A13 y avisó «listo». Un único A13 volvió a estar anunciado y se comprobó
SM-A135M. Owner esperado sigue presente (usuario 0, DeviceOwner/Affiliated).
Intento `cmd user is-user-unlocked 0` devolvió código no cero; no se usó como
evidencia. Se consultó `dumpsys user`, mostrando solo State: RUNNING_UNLOCKED.
Sin APIs privadas/reflexión ni cambio de ajustes.

Lectura de BOOT_COUNT y archivo DP por run-as, sin escribir en el teléfono:

- BOOT_COUNT sistema y registro: **3**, antes **2**.
- SHA-256 del bootId: `310d32120e63b40e42558460fadd2afacd416c35a3f92b5a5c7d36dbca244f22`,
  distinto del hash previo.
- lastObservedElapsed: **230143 ms**; checksum correcto.
- authorizedBootId=null; binding, policy, lastCommand=null; PREPARACION.

Observado contexto de nuevo arranque persistido y sin autorización, después del
primer desbloqueo. No puede atribuirse el cambio a un broadcast específico sin
evidencia adicional; no se capturó el archivo antes del PIN. H-BOOT-02/03 completos
siguen pendientes. No hubo nuevo reinicio ADB, instalación, muerte de proceso,
enforcement ni cambios al A56. Próximo paso: abrir app presencialmente y repetir
lectura para comprobar que el callback del launcher conserva este bootId.

## Fuentes oficiales consultadas para Android

- https://developer.android.com/reference/android/util/AtomicFile — openRead, startWrite, finishWrite/failWrite y exclusión externa requerida.
- https://developer.android.com/reference/android/content/Context#createDeviceProtectedStorageContext()
- https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT
- https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()
- https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()

## Próxima comprobación coordinada

Actualizar app/test APK únicamente en A13 y ejecutar los 12 tests Android actuales
con destino explícito, exigiendo Owner/credencial como en H02. Los cinco H04 usan
archivos efímeros del paquete de tests, no el archivo de producción. El nuevo
receiver/launcher sí inicializa el contexto de preparación de producción si
todavía no existe; no crea vínculo, órdenes ni autorización.

Solo después coordinar aparte arranque real antes del PIN y recreación de proceso,
sin forzar detención de emergencia ni borrar archivos de producción. Antes de
desbloqueo aún no se puede afirmar ejecución del receiver o durabilidad por la
pantalla: hay que registrar evidencia específica. No marcar H-BOOT/H-STATE por
estas simulaciones JVM ni APPLIED por un registro local bien formado.
