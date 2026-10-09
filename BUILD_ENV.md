# Entorno de construcción — hijo Android

Fecha de registro: 8 de octubre de 2026. Tarea: C00.

## Herramientas observadas y versiones fijadas

| Herramienta | Versión / decisión |
|---|---|
| Sistema operativo | Windows 11, arquitectura amd64 |
| Android Studio instalado | Identificador AI-253.32098.37.2534.15232325, obtenido de product-info.json |
| JDK usado para construir | OpenJDK 21.0.10, incluido en Android Studio (jbr) |
| Java adicional en PATH | Oracle JDK 21.0.6; no es el elegido para las pruebas de esta tarea |
| Gradle Wrapper del proyecto | 8.13, distribución bin, con SHA-256 verificado contra el publicado por Gradle |
| Android Gradle Plugin | 8.11.1 |
| Kotlin y plugin Compose | 2.2.21 |
| Compose BOM | 2025.06.01 |
| Activity Compose | 1.10.1 |
| JUnit | 4.13.2 |
| JSON para leer vectores descriptivos en tests JVM | kotlinx-serialization-json 1.9.0, nunca en producción |
| Pruebas instrumentadas Android | androidx.test.ext:junit 1.2.1 / runner 1.6.2 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk del hijo | 31 (Android 12) |
| Build Tools predeterminados de AGP 8.11 | 35.0.0, disponibles en el SDK local |
| Bytecode Java / Kotlin | JVM 17, compilado con JDK 21 |
| Git | 2.47.1.windows.1 |
| Node para verificar fixtures | 24.21.0 |

AGP 8.11 admite API 36 y requiere como mínimo Gradle 8.13 y JDK 17:
https://developer.android.com/build/releases/past-releases/agp-8-11-0-release-notes

Kotlin 2.2.20–2.2.21 documenta compatibilidad con Gradle hasta 8.14 y AGP hasta 8.11.1:
https://kotlinlang.org/docs/gradle-configure-project.html

Las versiones se fijan en `android/gradle/libs.versions.toml` y el Wrapper.
No es necesario usar el Gradle global ni instalar un JDK distinto para cada módulo.

Integridad del Wrapper comprobada contra los valores publicados por Gradle:

- Distribución 8.13 bin: `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.
- JAR del Wrapper: `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.

## Abrir y construir

1. Abrir la carpeta `android/` desde Android Studio.
2. Elegir el JDK incluido en Android Studio para Gradle.
3. Configurar el SDK local desde el IDE. `android/local.properties` es local y está excluido de Git.

Desde PowerShell, en la raíz del repositorio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location android
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest
.\gradlew.bat :app-child:assembleDebug :app-child:lintDebug
```

APK de laboratorio: `android/app-child/build/outputs/apk/debug/app-child-debug.apk`.
No instalarlo aún como controlador: C00 solo prepara una pantalla de «No configurado».
La firma debug que genera Android no es la firma final ni la identidad criptográfica CP/1.

Comprobación adicional desde la raíz, sin dispositivo: `python -B pruebas/verificar_apk_base.py`.
Resultados de construcción y limitaciones: `ESTADO.md`.

## Pruebas de C02 sin teléfono

Desde `android/`, con el mismo JDK:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest
.\gradlew.bat :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug
```

Desde la raíz: `node pruebas/verificar_interoperabilidad_jvm.mjs`.
Los tests generan únicamente datos públicos de interoperabilidad bajo build/, fuera de Git.
Compilar el APK de pruebas **no ejecuta** Android Keystore ni acredita que el A13 funcione.

Para ejecutar después, solo con el dispositivo de laboratorio autorizado y desbloqueado:
`.\gradlew.bat :app-child:connectedDebugAndroidTest`.
Ese comando instala app/test APK y crea/elimina claves efímeras con alias únicos de test.
No ejecutarlo hasta coordinar esa instalación con el propietario; no solicita Device Owner ni restablece el equipo.

## Separación de módulos

- `app-child`: aplicación Android Kotlin/Compose, sin permisos de administración en C00.
- `core-protocol`: núcleo JVM independiente de Android; C01 y C02 implementarán el protocolo y las firmas.
- `core-protocol/src/testFixtures`: simulador local que entrega los ejemplos congelados como bytes. Es soporte de pruebas, no un relay WSS ni una validación de seguridad.
- Los ejemplos de `compartido/ejemplos/` se leen como recursos de testFixtures; no se modifican ni se incluyen en el APK.
- El manifest deshabilita backups y las reglas de extracción excluyen datos tanto de nube como de transferencia entre dispositivos. Comportamiento físico pendiente de validación.

Referencia oficial de las reglas de extracción:
https://developer.android.com/identity/data/autobackup

## Teléfono y límites

El propietario identifica el teléfono como Samsung A13 y lo ofrece como equipo de laboratorio.
Código SM, Android/API, One UI, parche, estado de cuentas y posibilidad de aprovisionamiento:
**NO COMPROBADOS**. Se registrarán en H00, sin asumir que utiliza Android 14.

No se ha conectado, instalado, aprovisionado ni restablecido el teléfono en C00.
Antes de pruebas físicas se avisará al propietario para habilitar y autorizar ADB.
El siguiente diagnóstico no destructivo será obtener modelo y versión, sin publicar el número de serie.

La configuración del proyecto no certifica el A13 ni otras marcas. G0–G4 permanecen pendientes.
