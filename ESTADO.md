# Estado de implementación

Última actualización: 8 de octubre de 2026
Commit de partida: bb70002 (C00 completada)
Tarea actual: C01
Estado: COMPLETADA — C01 estructural; criptografía C02 y controles Android pendientes
Commits de código: 2d2afb3 y dcff87f.
Commit de cierre: «Cerrar C01 con evidencia y preparar coordinación de pruebas físicas».

## Entorno observado

Android Studio/JDK/Gradle/AGP/Kotlin: registrados en BUILD_ENV.md.
JDK de construcción 21.0.10 / Wrapper 8.13 / AGP 8.11.1 / Kotlin 2.2.21 / SDK 36.
Node 24.21.0 / Windows 11 amd64.
SM / Android / One UI / parche del hijo: NO COMPROBADO

## Terminado y demostrado

- C01: JSON con UTF-8 estricto, ASCII imprimible, rechazo de claves duplicadas, números ambiguos y tamaños excesivos; comparación byte a byte del payload canónico.
- C01: base64url canónico con límites, DTO y codec de todos los tipos del esquema.
- C01: 28 ejemplos congelados aceptados con ida/vuelta a los mismos bytes canónicos; campos extra y cada campo omitido se rechazan.
- C01: secuencias positivas de 64 bits, null explícitos, UUID v4, nonces/hashes de 32 bytes, duraciones, coherencia STATE, roles fijos, propósito y transporte comprobados.
- C01: 112 pruebas JVM totales correctas incluyendo la app. Las firmas y claves todavía solo tienen validación estructural; C02 debe verificar criptografía real.
- Proyecto Android Kotlin/Compose compilado, núcleo JVM separado y Wrapper reproducible.
- C00 conserva sus siete pruebas de preparación dentro de las 112 pruebas actuales.
- Simulador que entrega los bytes de ejemplos congelados, aislado en testFixtures.
- APK debug mínimo con pantalla «No configurado»; sin clases de pruebas ni fixtures congelados dentro del APK.
No hay Device Owner, políticas, cuenta regresiva implementada ni validación física.

## Archivos modificados en la última tarea

- android/core-protocol/src/main/kotlin/dev/controlparental/protocol/CodecCp1.kt
- android/core-protocol/src/main/kotlin/dev/controlparental/protocol/EscritorMensajes.kt
- android/core-protocol/src/main/kotlin/dev/controlparental/protocol/LectorCampos.kt
- android/core-protocol/src/test/kotlin/dev/controlparental/protocol/EjemplosCp1Test.kt
- android/core-protocol/src/test/kotlin/dev/controlparental/protocol/CodecCp1Test.kt
- Unidades anteriores de C01: JsonCp1.kt, Base64Cp1.kt, ErrorProtocolo.kt, ModelosCp1.kt y JsonCp1Test.kt, bajo los mismos paquetes del núcleo.
- android/core-protocol/LEEME.md: flujo y límites de seguridad de C01.
- pruebas/verificar_apk_base.py: inspección ampliada a todas las clases declaradas en las fuentes de prueba.
- pruebas/EVIDENCIA_C01.md: comandos, resultados, cobertura y pruebas no ejecutadas.
- AGENTS.md: coordinación del avance autónomo y primer punto físico.
- ESTADO.md: resultados, riesgos y siguiente tarea.
android/local.properties es local, está ignorado y no se incluye en commits.

## Pruebas ejecutadas y resultado

- C01, primera unidad: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain`, desde android/ con el entorno registrado: BUILD SUCCESSFUL, 17 tests, 0 fallos.
- C01, segunda unidad: mismo comando, BUILD SUCCESSFUL; 112 tests, 0 fallos, 0 errores: 84 parametrizados de fixtures, 11 codec, 10 parser/base64, 6 preparación y 1 app.
- Cierre C01: `.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:lintDebug --console=plain`: BUILD SUCCESSFUL; pruebas ya ejecutadas UP-TO-DATE, APK reconstruido y lint con 0 errores/7 avisos.
- `python -B pruebas/verificar_apk_base.py`: OK con inspección ampliada de todas las clases declaradas del simulador/tests.
- `node pruebas/verificar_vectores.mjs`: repetido correctamente en Node 24.21.0, 78 comprobaciones.
- SHA-256 del contrato y esquema repetidos: sin cambios; coinciden con INTEGRACION_FUTURA.md.
- No hay verificaciones criptográficas Kotlin, autorización ni pruebas físicas. Detalles en pruebas/EVIDENCIA_C01.md.
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

Primer punto físico: H00 — inventario y laboratorio, depende de C00 ya completada. Solicitar conexión y autorización ADB al propietario para identificar SM/Android/API/parche y registrar disponibilidad; sin instalar, aprovisionar ni borrar.
Lectura mínima H00: hijo/PLAN_HIJO.md H00, hijo/INSTALACION_A13.md §1–3, compartido/02_FUENTES_Y_COMPATIBILIDAD.md.
C02 sigue PENDIENTE y puede desarrollarse independientemente del teléfono: CP/1 §4 y pruebas/vectores_crypto.json. El inventario H00 no exige que C02 esté terminada.
No proceder al endurecimiento H06 sin comprobar emergencia H02 y recuperación H05 en el equipo real.

## Seguridad

No almacenar claves, códigos, QR sensibles, contraseñas ni datos del menor aquí.
