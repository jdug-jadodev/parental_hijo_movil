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

## NO EJECUTADO

- Instalación de H04 o ejecución de sus cinco tests Android en el A13.
- Recepción real de ambos broadcasts, arranque antes del PIN Android, recreación
  del proceso y observación del archivo DP en el firmware A135MUBSDDZB3.
- Recuperación, migraciones de producción, quiosco, temporizador Android o red.

El A13 conserva H02; no se ha actualizado ni reiniciado en esta tarea.
El A56 no se toca. G0–G4 no se aprueban por estos tests o por compilar el receiver.

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
