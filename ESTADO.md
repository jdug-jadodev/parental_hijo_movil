# Estado de implementación

Última actualización: 8 de octubre de 2026
Commit de partida: ac7d801 (C01 completada)
Tarea actual: H00
Estado: PENDIENTE_AUTORIZACION_ADB — equipo USB detectado como unauthorized; inventario aún no ejecutado
Commit de partida de H03: 499b31f.
Primer commit H03: 80e1cb4. Commit de cierre: «Cerrar H03 con pruebas de mantenimiento y evidencia».
C02 continúa PENDIENTE_VALIDACION Android/Keystore; no bloquea H03.

## Entorno observado

Android Studio/JDK/Gradle/AGP/Kotlin: registrados en BUILD_ENV.md.
JDK de construcción 21.0.10 / Wrapper 8.13 / AGP 8.11.1 / Kotlin 2.2.21 / SDK 36.
Node 24.21.0 / Windows 11 amd64.
SM / Android / One UI / parche del hijo: NO COMPROBADO

## Terminado y demostrado

- H03: cuatro acciones, prioridades locales y reloj monotónico inyectable; no hay importaciones Android ni reloj civil en el motor.
- H03: cuenta regresiva sin reiniciar por reevaluación/reconexión, bloqueo por boot distinto, márgenes de red/canal y siguiente transición positiva.
- H03: 57 nuevas pruebas del motor correctas, dentro de 227 pruebas JVM totales; mantenimiento y 1024 combinaciones de condiciones incluidos. Evidencia en pruebas/EVIDENCIA_H03.md.
- C02 parcial: firma/verificación SHA256withECDSA sobre prefijo de propósito y bytes originales, SPKI DER y curva P-256 exacta, DER de firma y r/s en rango.
- C02 parcial: diez vectores originales verificados en JVM; rechazadas firmas, claves, payloads, propósitos y roles alterados; JSON ambiguo firmado no se acepta.
- C02 parcial: diez sobres nuevos firmados en JVM y verificados en Node 24.21.0, 70 comprobaciones de interoperabilidad.
- C02: 170 pruebas JVM totales correctas. Los resultados verificados conservan bytes defensivamente y hash del payload, nunca de la firma.
- C02: adaptador Android Keystore compilado, sin generación/importación/exportación de privadas ni reemplazo automático de identidad; requiere primer desbloqueo.
- C02: tres tests instrumentados compilados, todavía NO EJECUTADOS en Android. No se considera C02 validada físicamente.
- C01: JSON con UTF-8 estricto, ASCII imprimible, rechazo de claves duplicadas, números ambiguos y tamaños excesivos; comparación byte a byte del payload canónico.
- C01: base64url canónico con límites, DTO y codec de todos los tipos del esquema.
- C01: 28 ejemplos congelados aceptados con ida/vuelta a los mismos bytes canónicos; campos extra y cada campo omitido se rechazan.
- C01: secuencias positivas de 64 bits, null explícitos, UUID v4, nonces/hashes de 32 bytes, duraciones, coherencia STATE, roles fijos, propósito y transporte comprobados.
- C01: sus 112 pruebas estructurales siguen correctas dentro del total actual. La lectura del codec sigue sin acreditar firmas; la criptografía se usa explícitamente a través de CriptografiaCp1.
- Proyecto Android Kotlin/Compose compilado, núcleo JVM separado y Wrapper reproducible.
- C00 conserva sus siete pruebas de preparación dentro de las 112 pruebas actuales.
- Simulador que entrega los bytes de ejemplos congelados, aislado en testFixtures.
- APK debug mínimo con pantalla «No configurado»; sin clases de pruebas ni fixtures congelados dentro del APK.
No hay Device Owner, aplicación física de políticas, persistencia ni temporizador Android conectado a la UI. El motor calcula intenciones, no APPLIED. No hay validación física.

## Archivos modificados en la última tarea

- android/app-child/src/main/java/dev/controlparental/child/domain/: PolicyEngine.kt y ModelosEvaluacion.kt.
- android/app-child/src/test/java/dev/controlparental/child/domain/: PreparacionPoliticas.kt, PoliticasTablaTest.kt, PrioridadesPoliticaTest.kt, RedYArranquePoliticaTest.kt y TiempoYCoherenciaTest.kt.
- Cierre: domain/LEEME.md, MantenimientoPoliticaTest.kt e InvariantesPoliticaTest.kt; pruebas/EVIDENCIA_H03.md y hijo/PLAN_HIJO.md.
- ESTADO.md: resultados, riesgos y siguiente tarea.
android/local.properties es local, está ignorado y no se incluye en commits.

## Pruebas ejecutadas y resultado

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

- Acuerdos del propietario guardados en AGENTS.md: español, commits cada aproximadamente 500 líneas, cuenta regresiva y aviso antes de ADB.
- Cuenta regresiva basada en tiempo transcurrido, incluso con pantalla apagada; sin cambios a CP/1.
- Simulador aislado en testFixtures, sin dependencia de producción ni ACK fabricados.
- Respetado el repositorio Git y sus commits existentes.

## Bloqueos y riesgos abiertos

G0–G4 pendientes. No hay validación física del A13.
El validador Python del esquema no se ha repetido con éxito: instalación de jsonschema 4.26.0 en un venv temporal falló por conexión/timeout. No se modificó Python global; ver evidencia C01.
La supervivencia del control ante muerte de proceso y suspensión sigue pendiente de pruebas reales.

## Próxima tarea y lectura mínima

Continuar H00 cuando el propietario acepte la autorización de depuración USB en el teléfono. Detectado como unauthorized tras su confirmación «listo»; no confundir conexión USB con autorización ADB. H03 completada en lógica pura. H04 depende de H01 y no se adelanta saltando esa dependencia.
Primer punto físico: H00 — inventario y laboratorio, depende de C00 ya completada. Solicitar conexión y autorización ADB al propietario para identificar SM/Android/API/parche y registrar disponibilidad; sin instalar, aprovisionar ni borrar.
Lectura mínima H00: hijo/PLAN_HIJO.md H00, hijo/INSTALACION_A13.md §1–3, compartido/02_FUENTES_Y_COMPATIBILIDAD.md.
C02 queda PENDIENTE_VALIDACION: coordinar después de H00 la instalación de app/test APK y ejecutar sus tres tests en Android desbloqueado. El inventario H00 no instala ni borra nada; no confundir compilar los tests con aprobarlos.
No proceder al endurecimiento H06 sin comprobar emergencia H02 y recuperación H05 en el equipo real.

## Seguridad

No almacenar claves, códigos, QR sensibles, contraseñas ni datos del menor aquí.
