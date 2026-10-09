# Estado de implementación

Última actualización: 9 de octubre de 2026
Commit de partida: ac7d801 (C01 completada)
Tarea actual: H04 — estado atómico y contexto de arranque
Estado: VALIDACION_PARCIAL — 12 tests Android y contexto de preparación conservado tras reinicio/recreación real del proceso; Direct Boot y políticas reales pendientes
Commit de partida de H03: 499b31f.
Primer commit H03: 80e1cb4. Commit de cierre: «Cerrar H03 con pruebas de mantenimiento y evidencia».
C02 comprobada en JVM/Node y Android/Keystore. Commit de esta unidad: «Validar C02 en el A13 y excluir expresamente el A56».

## Entorno observado

Android Studio/JDK/Gradle/AGP/Kotlin: registrados en BUILD_ENV.md.
JDK de construcción 21.0.10 / Wrapper 8.13 / AGP 8.11.1 / Kotlin 2.2.21 / SDK 36.
Node 24.21.0 / Windows 11 amd64.
Hijo: Samsung SM-A135M / Android 14 / API 34 / parche 2026-02-05 / firmware A135MUBSDDZB3.
One UI: 6.1 confirmada por el propietario; propiedad 60100 observada. Device Owner de laboratorio activo: dev.controlparental.child/.admin.ChildAdminReceiver, usuario 0.

## Terminado y demostrado

- H04 físico parcial: tras reinicio manual BOOT_COUNT 2→3 y bootId nuevo sin autorización; reanudación y recreación real del proceso PID 2745→ausente→11819 conservan bootId del arranque 3 y checksum correcto. Solo PREPARACION sin vínculo/política/orden; no prueba temporizador/enforcement. Direct Boot antes del PIN no observable por ADB en esta sesión.
- H04: adaptador AtomicFile en Device Protected con lectura acotada, readback de escritura, singleton por ruta/proceso y cola de IO. BootContextRepository conserva inicio/política/contador y retira autorización en arranque nuevo o incierto. BootReceiver directBootAware no exportado; UI muestra error de estado sin ocultar emergencia. No hace enforcement ni usa Keystore privado.
- H04: 25 tests JVM nuevos aprobados dentro de 262 totales. Repetición Android tras corregir aislamiento: OK (12 tests), 1.892 s, incluidos cinco H04. AtomicFile/rollback/corrupción comprobados en archivos efímeros DP del destino; no control-state.json ni boot/proceso reales. App H04 instalada; A56 intacto. Primer intento fallido conservado como evidencia histórica.
- H04 primera unidad: registro local versionado con checksum sobre bytes del payload, parser estricto, última orden firmada y campos derivados comprobados con pública del vínculo. Sin claves privadas, códigos reales ni historial. Puerto de almacenamiento y transacciones serializadas probados con fake, NO AtomicFile Android aún.
- H04 primera unidad: 15 tests nuevos JVM aprobados dentro de 252 totales; corrupción no se convierte en ausencia, contador no retrocede, repetición no reinicia inicio y 100 actualizaciones concurrentes no se pierden. No demuestra persistencia física ni bloqueo aplicado.
- H02 cierre inicial sin SIM: propietario confirma «correcto» al reiniciar manualmente, abrir/salir del marcador antes de PIN Android SIN llamar, desbloquear y comprobar Owner/admin Sí. H-EMG-01 parcial por variantes SIM no disponibles; G0 completo y emergencia bajo quiosco pendientes. No se ejecutó reboot ADB ni se tocó A56.
- H02: botón de emergencia sin dependencia de red/clave parental, solicitud lockNow pública con precondiciones y error explícito. Seis tests JVM nuevos correctos. APK H02 instalado solo en A13; runner con Owner/credencial exigidos: OK (7 tests). Propietario confirma rutas básica, sin internet y arranque sin SIM, sin marcar/llamar. Confirmación presencial, no observación automatizada; variantes SIM/PIN SIM pendientes.
- H01: APK actualizados y Device Owner real establecido por ADB únicamente en el A13, sin quiosco/endurecimiento añadido. Tres tests H01 antes de Owner aprobados; seis tests H01+C02 después, exigiendo Owner/admin real, aprobados. Debug testOnly separado de release. El propietario respondió «perfecto» a la comprobación de pantalla de preparación; confirmación presencial comunicada, no captura automatizada. A56 intacto.
- H00: equipo autorizado ADB e inventariado sin instalar ni modificar datos. One UI 6.1 y preparación del laboratorio confirmadas por el propietario: teléfono ya formateado, sin datos que conservar. Ficha y plan en pruebas/FICHA_DISPOSITIVO.md. SIM/PIN, emergencia y elegibilidad real de aprovisionamiento siguen NO COMPROBADOS para fases posteriores; no se autoriza otro borrado.
- H03: cuatro acciones, prioridades locales y reloj monotónico inyectable; no hay importaciones Android ni reloj civil en el motor.
- H03: cuenta regresiva sin reiniciar por reevaluación/reconexión, bloqueo por boot distinto, márgenes de red/canal y siguiente transición positiva.
- H03: 57 nuevas pruebas del motor correctas, dentro de 227 pruebas JVM totales; mantenimiento y 1024 combinaciones de condiciones incluidos. Evidencia en pruebas/EVIDENCIA_H03.md.
- C02 parcial: firma/verificación SHA256withECDSA sobre prefijo de propósito y bytes originales, SPKI DER y curva P-256 exacta, DER de firma y r/s en rango.
- C02 parcial: diez vectores originales verificados en JVM; rechazadas firmas, claves, payloads, propósitos y roles alterados; JSON ambiguo firmado no se acepta.
- C02 parcial: diez sobres nuevos firmados en JVM y verificados en Node 24.21.0, 70 comprobaciones de interoperabilidad.
- C02: 170 pruebas JVM totales correctas. Los resultados verificados conservan bytes defensivamente y hash del payload, nunca de la firma.
- C02: adaptador Android Keystore compilado, sin generación/importación/exportación de privadas ni reemplazo automático de identidad; requiere primer desbloqueo.
- C02: tres tests instrumentados EJECUTADOS en el A13: OK (3 tests), 1.676 s. App y APK test instalados por ADB con destino explícito; A56 excluido, sin comandos dirigidos a él. Evidencia en pruebas/EVIDENCIA_C02.md.
- C01: JSON con UTF-8 estricto, ASCII imprimible, rechazo de claves duplicadas, números ambiguos y tamaños excesivos; comparación byte a byte del payload canónico.
- C01: base64url canónico con límites, DTO y codec de todos los tipos del esquema.
- C01: 28 ejemplos congelados aceptados con ida/vuelta a los mismos bytes canónicos; campos extra y cada campo omitido se rechazan.
- C01: secuencias positivas de 64 bits, null explícitos, UUID v4, nonces/hashes de 32 bytes, duraciones, coherencia STATE, roles fijos, propósito y transporte comprobados.
- C01: sus 112 pruebas estructurales siguen correctas dentro del total actual. La lectura del codec sigue sin acreditar firmas; la criptografía se usa explícitamente a través de CriptografiaCp1.
- Proyecto Android Kotlin/Compose compilado, núcleo JVM separado y Wrapper reproducible.
- C00 conserva sus siete pruebas de preparación dentro de las 112 pruebas actuales.
- Simulador que entrega los bytes de ejemplos congelados, aislado en testFixtures.
- APK debug mínimo con pantalla «No configurado»; sin clases de pruebas ni fixtures congelados dentro del APK.
Hay Device Owner de laboratorio en el A13, pero no quiosco, bloqueo parental aplicado, persistencia ni temporizador Android conectado a la UI. El motor calcula intenciones, no APPLIED. Emergencia/recuperación/endurecimiento siguen sin validar.

## Archivos modificados en la última tarea

- H04 recreación de proceso y balance parcial: ESTADO.md, pruebas/EVIDENCIA_H04.md y pruebas/FICHA_DISPOSITIVO.md; documentación solamente, sin nuevos tests/código.
- H04 repetición física aprobada: ESTADO.md, pruebas/EVIDENCIA_H04.md y pruebas/FICHA_DISPOSITIVO.md; sin cambios de código.
- H04 primer intento físico y corrección: AtomicFileEstadoAndroidTest.kt (targetContext y subdirectorio UUID efímero, limpieza limitada), ESTADO.md, pruebas/EVIDENCIA_H04.md y pruebas/FICHA_DISPOSITIVO.md.
- H04 segunda unidad: data/EstadoAndroid.kt; boot/BootContextRepository.kt, FuenteArranqueAndroid.kt y BootReceiver.kt; LauncherActivity.kt, manifest y strings.xml; tests BootContextRepositoryTest.kt y AtomicFileEstadoAndroidTest.kt, ampliación CodecEstadoTest.kt; verificar_apk_h01.py, pruebas/EVIDENCIA_H04.md y ESTADO.md.
- H04: data/ModelosEstado.kt, ValidadorEstado.kt, CodecEstado.kt y ChildStateStore.kt; tests data/PreparacionEstado.kt, CodecEstadoTest.kt y ChildStateStoreTest.kt; app-child/build.gradle.kts (testFixtures solo JVM), ESTADO.md.
- H02: domain/ModelosEmergencia.kt, emergency/EmergencyAccess.kt, admin/PolicyEnforcer.kt, ui/LockedScreen.kt y LauncherActivity.kt; strings.xml; EmergencyAccessTest.kt y CondicionesEmergenciaAndroidTest.kt; pruebas/EVIDENCIA_H02.md y ESTADO.md.
- H02 validación Android: CondicionesEmergenciaAndroidTest.kt (credencial exigible), pruebas/EVIDENCIA_H02.md, pruebas/FICHA_DISPOSITIVO.md y ESTADO.md.
- H01 físico: ESTADO.md, pruebas/EVIDENCIA_H01.md, pruebas/FICHA_DISPOSITIVO.md y BUILD_ENV.md. Sin cambios de código ni contrato.
- H01: admin/ChildAdminReceiver.kt y DeviceCapabilities.kt; domain/DiagnosticoAdministracion.kt; ui/LauncherActivity.kt (antes ActividadInicial.kt); manifests main/debug, strings.xml y device_admin.xml; DiagnosticoAdministracionTest.kt y ComponentesAdministracionTest.kt.
- Documentación H01: 00_EMPIEZA_AQUI.md (A56 prohibido), hijo/INSTALACION_A13.md, pruebas/EVIDENCIA_H01.md y ESTADO.md.
- pruebas/verificar_apk_h01.py: inspector local de declaraciones debug/release sin usar ADB.
- C02 cierre Android: AGENTS.md (no tocar A56), pruebas/EVIDENCIA_C02.md, pruebas/FICHA_DISPOSITIVO.md, BUILD_ENV.md y ESTADO.md. Sin cambios de código o contrato.
- H00: pruebas/FICHA_DISPOSITIVO.md, BUILD_ENV.md y ESTADO.md (inventario y preparación pendiente, sin código nuevo).
- Antecedente H03:
- android/app-child/src/main/java/dev/controlparental/child/domain/: PolicyEngine.kt y ModelosEvaluacion.kt.
- android/app-child/src/test/java/dev/controlparental/child/domain/: PreparacionPoliticas.kt, PoliticasTablaTest.kt, PrioridadesPoliticaTest.kt, RedYArranquePoliticaTest.kt y TiempoYCoherenciaTest.kt.
- Cierre: domain/LEEME.md, MantenimientoPoliticaTest.kt e InvariantesPoliticaTest.kt; pruebas/EVIDENCIA_H03.md y hijo/PLAN_HIJO.md.
- ESTADO.md: resultados, riesgos y siguiente tarea.
android/local.properties es local, está ignorado y no se incluye en commits.

## Pruebas ejecutadas y resultado

- H04 reapertura tras proceso ausente: consultas de lectura al único A13 comprobado; PID 11819 distinto de 2745, mismo bootId/BOOT_COUNT=3, elapsed 451173→1088871 ms, checksum correcto, PREPARACION sin autorización/vínculo/política/orden. No se ejecutó am kill/force-stop ni se cambió A56. Recreación real de contexto comprobada, no continuidad de políticas aún inexistentes.
- H04 proceso: autorización para am kill en segundo plano recibida. Consulta inicial PID 2745 con app en primer plano: no se ejecutó. Tras pulsar Inicio por propietario, pidof vacío/código 1 confirma proceso ausente ANTES de am kill. Causa no determinada; no se enviaron señales ni force-stop. Falta reabrir manualmente y comparar PID/contexto; no prueba crash entre fases ni enforcement.
- H04 launcher después del desbloqueo: propietario confirma pantalla con estado local comprobado/Owner/admin Sí. Lectura ADB solo A13: BOOT_COUNT=3 y mismo hash bootId, elapsed 230143→451173 ms, checksum correcto y PREPARACION sin autorización/vínculo/política/orden. Estabilidad al reanudar comprobada; no equivale a dos broadcasts ni proceso muerto. Sin nuevas pruebas automáticas ni modificaciones del teléfono por ADB.
- H04 tras reinicio manual y primer desbloqueo: A13 único comprobado, Owner conservado, dumpsys user State RUNNING_UNLOCKED. BOOT_COUNT 2→3, hash bootId distinto, checksum correcto, autorización/vínculo/política/orden ausentes y PREPARACION. Contexto nuevo persistido observado después del PIN; no demuestra recepción individual de broadcasts ni Direct Boot. cmd user is-user-unlocked no disponible en intento (código no cero); se usó lectura filtrada dumpsys, sin evasión.
- H04 intento antes del PIN tras aviso del propietario: detenido en selección ADB; solo aparece A56 SM_A566E, ningún A13 anunciado. Ningún comando dirigido a teléfonos, lectura posterior al reinicio NO realizada. No inferir fallo del receiver. Pendiente revisar conexión A13 sin saltar protección USB.
- H04 referencia para boot manual autorizada: solo lecturas ADB al único A13 comprobado, Owner esperado presente. BOOT_COUNT sistema/registro=2, checksum correcto, inicio de observación 2449109 ms, autorización/vínculo/política/última orden ausentes y etapa PREPARACION. Hash de bootId y límites en EVIDENCIA_H04.md. No se escribió estado ni se reinició/mató proceso; esperar aviso antes del PIN tras reinicio manual. Sin nuevas pruebas JVM/Android automáticas ni comandos al A56.
- H04 repetición autorizada: actualizar SOLO APK test corregido mediante adb -s al único A13 comprobado; instalación Success. Owner previo esperado confirmado; runner con exigirDeviceOwner=true/exigirCredencialAndroid=true: OK (12 tests), 1.892 s. No se tocó control-state.json, reinició, endureció ni dirigió comandos al A56. Sin nueva construcción o ejecución JVM en esta repetición.
- H04 físico autorizado: A13 único seleccionado por modelo anunciado y comprobado con getprop antes de cada operación; Owner esperado confirmado. Ambas actualizaciones Success; runner con exigirDeviceOwner=true/exigirCredencialAndroid=true: 12 tests/4 fallos, 1.98 s. Directorio DP del paquete test inaccesible desde instrumentación: dos ENOENT al escribir y dos ErrorEstadoLocal al leer ausencia con padre inaccesible. No se reinició/endureció/borró ni se tocó A56.
- H04 corrección local: test usa targetContext y subdirectorio efímero UUID, nunca control-state.json. Construcción completa BUILD SUCCESSFUL, APK test recompilado; 262 resultados JVM anteriores UP-TO-DATE, no nueva ejecución. Corrección física NO EJECUTADA; requiere coordinar actualización del APK test y repetición.
- H04 cierre: comando Gradle en EVIDENCIA_H04.md repetido tras últimos ajustes: BUILD SUCCESSFUL; informes 262 tests/0 fallos/0 errores/0 omitidos, tests hijo ejecutados y núcleo UP-TO-DATE. APK debug/instrumentado y release sin firmar construidos; lint 0 errores/10 avisos. verificar_apk_base.py y verificar_apk_h01.py OK. No se usó ADB ni se instaló/reinició el A13.
- H04 primera unidad: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain`: BUILD SUCCESSFUL; informes 252 tests/0 fallos/0 errores/0 omitidos. Tests del hijo ejecutados; núcleo UP-TO-DATE. Se añadió únicamente soporte de fixtures en testImplementation, no en APK.
- H02 arranque sin SIM: confirmación presencial del propietario «correcto» a los pasos de reinicio manual, emergencia antes de PIN Android sin llamar y comprobación posterior Owner/admin Sí. No es captura automatizada ni prueba de persistencia del temporizador. Solo actualización documental; no se repitieron tests ni se usó ADB.
- H-EMG-02 presencial: propietario responde «correcto» a repetir ruta de emergencia en A13 con Wi-Fi/datos apagados, SIN modo avión ni llamadas, y reactivar conectividad. Aprobado en etapa H02 sin quiosco; confirmación comunicada, no medición de red automatizada. No se ejecutó ADB ni se repitieron tests automáticos para este registro.
- H02 manual básico: propietario confirma «todo correcto» después de las instrucciones de pulsar botón, acceder a emergencia sin marcar/llamar, salir y volver a la app con credencial Android. No comunicó diferencias; no se infiere apagado/deslizamiento exacto. Evidencia H02 y ficha actualizadas. No se repitieron tests automáticos ni se ejecutó ADB en este registro documental.
- H02 físico autorizado: Owner previo comprobado, actualizaciones de ambos APK Success y runner dirigido exclusivamente a A13 con exigirDeviceOwner=true/exigirCredencialAndroid=true: OK (7 tests), 1.666 s. Launcher Status ok/COLD. No se llamó lockNow, no se abrió marcador ni se tocó A56. Previa construcción BUILD SUCCESSFUL con JVM UP-TO-DATE y test instrumentado recompilado; verificar_apk_base.py OK.
- H02: comando Gradle documentado en EVIDENCIA_H02.md repetido tras último ajuste UI: BUILD SUCCESSFUL, 237 tests JVM/0 fallos/0 errores/0 omitidos; tests hijo ejecutados, núcleo UP-TO-DATE. Debug, instrumentado y release sin firmar compilados; lint 0 errores/10 avisos. verificar_apk_base.py y verificar_apk_h01.py OK. No se usó ADB ni se instaló o bloqueó ningún equipo en esta unidad.
- H01 continuación física: preflight con 0 cuentas/un usuario 0/sin Owner; dos actualizaciones APK Success. Tres tests H01 previos al Owner: OK (3 tests), 0.116 s. set-device-owner Success; list-owners confirma nuestro Owner. Runner dirigido al A13 con exigirDeviceOwner=true: OK (6 tests), 1.643 s (H01+C02). Apertura launcher Status ok/COLD; observación visual pendiente. A56 excluido; nada borrado.
- H01: ayuda DPM muestra remove-active-admin pero devuelve 255; no se ejecutó retirada. Inspector local APK H01 repetido OK. Lectura filtrada de restricciones efectivas observa no_add_managed_profile; globales none, sin afirmar auditoría completa. Intentos UIAutomator sin textos de la app: NO aprobación de UI ni árbol guardado.
- H01 preflight físico autorizado: A13 SM-A135M seleccionado de forma única y cada consulta dirigida con adb -s. dpm list-owners: no owners; pm list users: un usuario principal; am get-current-user: 0. Recuento de dumpsys account: 3 cuentas, sin mostrar ni guardar nombres/credenciales. Se detuvo ANTES de instalar o ejecutar set-device-owner. No se modificaron cuentas, ajustes ni A56. No se ejecutó el comando de ayuda/retirada posterior porque falló la precondición.
- H01 construcción final: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL; informes 231 tests/0 fallos/0 errores/0 omitidos, tests del hijo ejecutados y núcleo UP-TO-DATE. Debug, instrumentado y release sin firmar construidos; lint 0 errores/10 avisos.
- H01 inspección: verificar_apk_base.py OK; verificar_apk_h01.py OK en debug/release, testOnly exclusivo debug y políticas/manifest verificados. Fallos iniciales de compilación de instrumentación e inspector corregidos y registrados en pruebas/EVIDENCIA_H01.md; no se usaron APIs ocultas.
- C02 Android, 9 de octubre: instalación de ambos APK Success y `adb -s $serieA13 shell am instrument -w -r -e class dev.controlparental.child.security.CriptografiaAndroidTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner`: OK (3 tests). Destino seleccionado únicamente SM-A135M; comprobación de modelo antes de cada operación. No se usó connectedDebugAndroidTest con ambos equipos conectados.
- Construcción C02 repetida antes de instalar: core-protocol:test, app-child:testDebugUnitTest, assembleDebug y assembleDebugAndroidTest: BUILD SUCCESSFUL, 74 tareas UP-TO-DATE. Se reutilizaron resultados JVM (227), no se afirma una nueva ejecución.
- C02 cierre: verificar_apk_base.py OK; verificar_vectores.mjs OK (78 comprobaciones); apksigner verify --print-certs OK. Hashes de APK y certificado en evidencia C02. Sin cambios de restricciones, borrado ni desinstalación.
- H00 segundo intento: equipo autorizado en estado device; `adb shell getprop` de manufacturer/model/release/sdk/security_patch/oneui/incremental/verifiedbootstate/flash.locked y `adb shell dpm list-owners` correctos. Modelo/firmware arriba; propiedades de arranque green y 1; no owners. No se inspeccionaron cuentas, paquetes ni contenido. Sin instalación, aprovisionamiento, cambios, reinicio o borrado.
- H00: `adb version` comprobado: Platform Tools 37.0.0-14910828 / ADB 1.0.41. `adb devices` detectó un equipo USB en estado unauthorized; número de serie omitido. No se consultaron propiedades ni administración, no se instaló ni modificó el teléfono. Falta aceptar la autorización RSA en su pantalla.
- Cierre H03: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL, informes con 227 tests/0 fallos/0 errores/0 omitidos; núcleo UP-TO-DATE y nuevos tests de cierre ejecutados. Dos APK compilados; lint 0 errores y 10 avisos de actualización.
- Cierre H03: `python -B pruebas/verificar_apk_base.py`: OK, APK sin fixtures ni clases declaradas de pruebas.
- H03 primera unidad: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain`: BUILD SUCCESSFUL, 216 tests totales, 0 fallos/errores; 24 de tabla y 22 de prioridades/red/arranque/tiempo, más los 170 anteriores.
- Cambios de hora y suspensión solo simulados con reloj inyectado; no se cambió la hora de un teléfono ni se probó Doze real.
- Resultados anteriores C02:
- C02 JVM: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain`: BUILD SUCCESSFUL; 170 tests correctos, 0 fallos y 0 errores.
- `node pruebas/verificar_interoperabilidad_jvm.mjs`: OK, 10 firmas JVM nuevas y 70 comprobaciones en Node 24.21.0.
- `node pruebas/verificar_vectores.mjs`: repetido, OK, 10 vectores y 78 comprobaciones en Node 24.21.0.
- Los archivos de interoperabilidad están bajo build/ e incluyen solo públicas y sobres ficticios; las privadas efímeras no se escriben.
- C02 Android, construcción: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL; dos APK compilados, lint 0 errores y 10 avisos de nuevas versiones disponibles.
- `python -B pruebas/verificar_apk_base.py`: OK, app sin fixtures ni clases de pruebas JVM/instrumentadas.
- Inspección ZIP/DEX del APK instrumentado: contiene diez sobres de ejemplo y CriptografiaAndroidTest; no se ejecutó el APK.
- SHA-256 del contrato/esquema repetidos y sin cambios; vectores congelados intactos. Detalles en pruebas/EVIDENCIA_C02.md.
- No se ejecutó Keystore Android, connectedDebugAndroidTest ni se instaló nada en un teléfono.
- Resultados anteriores C01:
- C01, primera unidad: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain`, desde android/ con el entorno registrado: BUILD SUCCESSFUL, 17 tests, 0 fallos.
- C01, segunda unidad: mismo comando, BUILD SUCCESSFUL; 112 tests, 0 fallos, 0 errores: 84 parametrizados de fixtures, 11 codec, 10 parser/base64, 6 preparación y 1 app.
- Cierre C01: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL; pruebas ya ejecutadas UP-TO-DATE, APK reconstruido y lint con 0 errores/7 avisos.
- `python -B pruebas/verificar_apk_base.py`: OK con inspección ampliada de todas las clases declaradas del simulador/tests.
- `node pruebas/verificar_vectores.mjs`: repetido correctamente en Node 24.21.0, 78 comprobaciones.
- SHA-256 del contrato y esquema repetidos: sin cambios; coinciden con INTEGRACION_FUTURA.md.
- En el cierre de C01 aún no había verificación criptográfica Kotlin ni pruebas físicas; la criptografía JVM se añadió después en C02. Detalles históricos en pruebas/EVIDENCIA_C01.md.
- Resultados anteriores de C00, conservados como antecedente:
- `node pruebas/verificar_vectores.mjs`: OK, 10 vectores y 78 comprobaciones, Node 24.21.0.
- Gradle Wrapper generado con Gradle 8.13 y JDK 21.0.10: OK.
- Primera construcción con Kotlin 2.2.0: seis tests del núcleo y uno de app correctos, APK generado; lint falló por el separador de unidad sin escapar en local.properties.
- Corregido local.properties; añadidos icono y reglas de extracción. Kotlin ajustado a 2.2.21 por compatibilidad documentada con AGP 8.11.1.
- Repetición con JDK 21.0.10, Gradle 8.13, AGP 8.11.1 y Kotlin 2.2.21, desde android/: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL, 7 tests, 0 fallos, 0 omitidos.
- Lint final: 0 errores y 7 avisos por versiones más recientes disponibles; no se ocultaron ni se creó baseline. Se mantienen versiones fijadas con compatibilidad documentada.
- Aviso de empaquetado: libandroidx.graphics.path.so se incluye sin eliminar símbolos de depuración; no impidió construir el APK debug.
- Inspección ZIP/DEX con Python estándar: APK sin fixtures congelados ni clases del simulador/tests; se deja script reproducible en pruebas/verificar_apk_base.py.
- `python -B pruebas/verificar_apk_base.py`: OK, sin fixtures ni clases de prueba en el APK.
- `aapt dump badging` del APK: paquete dev.controlparental.child, minSdk 31, targetSdk 36, actividad inicial y variante depurable. No se ejecutó la interfaz.
- SHA-256 de contrato y esquema coinciden con INTEGRACION_FUTURA.md; los archivos compartidos no cambiaron.
- SHA-256 del JAR del Wrapper coincide con el publicado por Gradle.
- No se ejecutaron pruebas de dispositivo ni se solicitó conexión ADB.

## Decisiones locales sin cambiar ADR

- Separación confirmada por el propietario: servidor WebSocket/Render exclusivamente en `C:\Users\Usuario\Documents\parental_render`. Revisión local: no hay implementación de servidor, package.json, render.yaml ni fuentes TypeScript; los dos scripts .mjs son verificadores del protocolo. Aquí solo app del niño, futuro cliente WSS y contrato/fixtures congelados. No se consultó ni modificó el repositorio Render.
- Actualización documental de alcance: AGENTS.md, hijo/AGENTS.md, 00_EMPIEZA_AQUI.md, INTEGRACION_FUTURA.md, hijo/PLAN_HIJO.md y ESTADO.md. Sin cambios de código/CP/1 ni pruebas Android/JVM nuevas; próxima comprobación H04 boot/proceso sigue pendiente de coordinación.
- Acuerdos del propietario guardados en AGENTS.md: español, commits cada aproximadamente 500 líneas, cuenta regresiva y aviso antes de ADB.
- Cuenta regresiva basada en tiempo transcurrido, incluso con pantalla apagada; sin cambios a CP/1.
- Simulador aislado en testFixtures, sin dependencia de producción ni ACK fabricados.
- Respetado el repositorio Git y sus commits existentes.

## Bloqueos y riesgos abiertos

H01: bloqueo por registros de cuentas resuelto por el propietario; recuento ADB 0 y Owner establecido. Pantalla de preparación confirmada por el propietario. Retirada del admin testOnly, UI previa sin Owner y recuperación completa aún requieren pruebas coordinadas; no se consideran comprobadas por la confirmación de pantalla actual.
G0–G4 pendientes. Hay inventario físico ADB del A13, no validación del control, emergencia ni recuperación.
H04: AtomicFile real y recreación real de contexto PREPARACION comprobados; faltan recepción individual/idempotente de broadcasts y Direct Boot antes de PIN. No hay temporizador/enforcement Android ni reanudación de servicio de control implementados; no afirmar conservación de políticas reales por la prueba de contexto vacío.
H02: rutas básica, sin internet y tras reinicio antes de PIN Android confirmadas por propietario SIN SIM. Variantes con SIM/PIN SIM no disponibles/no comprobadas. G0 completo y emergencia bajo quiosco pendientes; no endurecer todavía. Marcador accesible sin SIM no demuestra cobertura ni llamada efectiva.
El validador Python del esquema no se ha repetido con éxito: instalación de jsonschema 4.26.0 en un venv temporal falló por conexión/timeout. No se modificó Python global; ver evidencia C01.
La supervivencia del control ante muerte de proceso y suspensión sigue pendiente de pruebas reales.

## Próxima tarea y lectura mínima

Continuar solo H04: diseñar evidencia acotada de callbacks/Direct Boot sin depender de ADB antes del PIN ni registrar historial/secretos; cualquier cambio/instalación y reinicio requieren coordinación. Contexto nuevo, estabilidad y recreación real en PREPARACION comprobados. Sin vínculo/política aceptada no se aprueban H-BOOT-01 ni H-STATE-01 completos. No saltar dependencias ni activar quiosco: H05/recuperación y variantes SIM/G0 siguen pendientes antes de H06. A56 fuera de alcance: no tocarlo.
H00 cerrado: ADB autorizado, inventario obtenido y preparación confirmada. El formateo fue previo y declarado por el propietario, no ejecutado por el agente.
Lectura mínima H00: hijo/PLAN_HIJO.md H00, hijo/INSTALACION_A13.md §1–3, compartido/02_FUENTES_Y_COMPATIBILIDAD.md.
C02 cerrada tras ejecución real de tres tests Android. Lectura mínima H01: hijo/PLAN_HIJO.md H01, hijo/SPEC_HIJO.md H2/H4 e hijo/INSTALACION_A13.md §3; consultar fuentes oficiales correspondientes.
No proceder al endurecimiento H06 sin comprobar emergencia H02 y recuperación H05 en el equipo real.

## Seguridad

No almacenar claves, códigos, QR sensibles, contraseñas ni datos del menor aquí.
