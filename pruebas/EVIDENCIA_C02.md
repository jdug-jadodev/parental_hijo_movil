# Evidencia C02 — firmas P-256

Fecha inicial: 8 de octubre de 2026. Cierre: 9 de octubre de 2026.
Estado: C02 comprobada en JVM/Node y tres tests Android aprobados en el A13.

## Entorno y comandos ejecutados

Windows 11 amd64, JDK 21.0.10 de Android Studio, Gradle 8.13, Kotlin 2.2.21,
AGP 8.11.1, Node 24.21.0. Versiones completas en `BUILD_ENV.md`.

Desde `android/`:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain
```

Resultado: BUILD SUCCESSFUL; 170 tests, cero fallos y cero errores.

Desde la raíz:

```powershell
node pruebas/verificar_vectores.mjs
node pruebas/verificar_interoperabilidad_jvm.mjs
```

Resultados: 78 comprobaciones de los 10 vectores congelados y 70 comprobaciones
de diez sobres nuevos generados por el código JVM y verificados en Node.
Los datos nuevos contienen solo públicas y sobres ficticios en un archivo build/ ignorado.
Las privadas de pruebas se generan en memoria; no se exportan ni se guardan.

## Demostrado

- Verificación de los diez sobres originales con públicas P-256 del fixture.
- Firma mediante SHA256withECDSA de JCA, sobre bytes originales y prefijo de propósito CP/1.
- SPKI DER con round-trip exacto y comprobación de parámetros P-256 completos.
- Contenedor DER de firma: enteros positivos/mínimos, longitudes y rango r/s.
- Rechazo de claves, payloads, firmas, propósitos y roles distintos.
- JSON no canónico sigue rechazado aunque tenga una firma matemáticamente válida.
- Firma inválida del puerto local no se emite como sobre legítimo.
- Firmas S complementarias válidas conservan el hash de payload, sin imponer low-S unilateralmente.
- PAIR_ACCEPT no sustituye la pública del firmante ni permite otra curva en sus públicas.

## Preparación Android del 8 de octubre (antecedente)

`FirmanteAndroidKeystore` usa una identidad existente por alias, exige primer desbloqueo,
firma con su handle y devuelve KEY_UNAVAILABLE si la identidad no está disponible.
No genera identidades de producción, no importa privadas ni habilita un bypass.

Tres tests instrumentados preparados: verificar fixtures en Android, firmar con Keystore
sin privada exportable y rechazar alias inexistente sin crear identidad de reemplazo.
Solo crean/eliminan claves de test con alias efímeros únicos; no modifican políticas.

Construcción ejecutada desde android/: `:core-protocol:test :app-child:testDebugUnitTest
:app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug --console=plain`.
Resultado: BUILD SUCCESSFUL; APK de app y APK instrumentado compilados; lint con cero errores
y diez avisos de versiones más recientes, sin supresiones. La compilación final reutilizó
los tests de núcleo ya aprobados cuando sus entradas no habían cambiado.

`python -B pruebas/verificar_apk_base.py`: APK del hijo sin fixtures ni clases de tests,
incluyendo las clases Android instrumentadas. Inspección ZIP/DEX adicional del APK de tests:
contiene sus diez sobres y CriptografiaAndroidTest, solo dentro de esa variante de pruebas.
En esa preparación no se ejecutó connectedDebugAndroidTest ni se instaló nada en el A13.

## Ejecución Android autorizada — 9 de octubre de 2026

El propietario autorizó la instalación de app y APK de tests exclusivamente en el
A13 y prohibió tocar el A56. Ambos estaban conectados. `adb devices -l` anunció
los modelos SM_A135M y SM_A566E, sin publicar sus series.
Se seleccionó un único SM_A135M autorizado y su serie se mantuvo solo en memoria.
Antes de cada instalación y de instrumentación se verificó por `adb -s` que el
modelo del destino seguía siendo SM-A135M. Ningún comando se dirigió al A56.
No se utilizó `connectedDebugAndroidTest`, para evitar seleccionar ambos equipos.

Equipo: Samsung SM-A135M, Android 14/API 34, One UI 6.1, firmware A135MUBSDDZB3,
parche 2026-02-05. No se activó Device Owner ni se aplicaron restricciones.

Construcción previa: `:core-protocol:test :app-child:testDebugUnitTest
:app-child:assembleDebug :app-child:assembleDebugAndroidTest --console=plain`:
BUILD SUCCESSFUL; 74 tareas UP-TO-DATE, sin afirmar una nueva ejecución JVM.

Tras comprobar que los dos paquetes no existían en el A13, comandos dirigidos
desde la raíz (`$serieA13` es el destino comprobado, no un valor publicado):

```powershell
& $adb -s $serieA13 install -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
& $adb -s $serieA13 install -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
& $adb -s $serieA13 shell am instrument -w -r -e class dev.controlparental.child.security.CriptografiaAndroidTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Ambas instalaciones: Success. Instrumentación: **OK (3 tests)**, 1.676 s,
tres resultados individuales correctos. Salida en
`android/app-child/build/resultado-c02-a13.txt` (ignorado por Git, sin serie).

- Android verificó los diez sobres congelados y sus bytes, rechazando otra clave.
- Keystore firmó y permitió verificar la firma; la privada devolvió encoded null.
- Alias inexistente produjo KEY_UNAVAILABLE sin crear identidad de reemplazo.
- El test de Keystore elimina su alias efímero en finally; no se generan identidades de producción.

APK app SHA-256: `d03f3104d3a3b967c334e1a919aa57000c30fdaa7b79e0233cd56a560561354f`.
APK test SHA-256: `248799278933a11e3ca3663d039e6b14525ecab12d9e8b9bd8708582d22befbd`.
Certificado debug SHA-256, verificado con apksigner:
`39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439`.

Inspección APK repetida: OK, sin fixtures ni clases de tests en la app.
`node pruebas/verificar_vectores.mjs` repetido: 10 vectores / 78 comprobaciones OK.
Los APK quedan instalados en el A13; no se desinstaló ni borró nada.

## Pendiente

- Direct Boot y manejo de identidad indisponible en el flujo completo: H04/H08.
- Vinculación física, pareja/boot/sesión/nonce/secuencia: H08/H09.
- No certifica bloqueo, emergencia, quiosco, energía ni latencia. G0–G4 siguen pendientes.

Esta evidencia comprueba la unidad criptográfica C02 en JVM, Node y Android/Keystore.
No acredita autorización de comandos ni funcionamiento completo del control parental.
