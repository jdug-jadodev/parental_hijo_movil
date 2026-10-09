# Instalación y validación inicial del A13

Esta es una guía de preparación del propietario. No ejecutar restablecimiento ni activar restricciones en el único teléfono de emergencia antes de probar la recuperación. **El paquete contiene documentación, no un APK listo.**

## 1. Identificar el equipo sin suposiciones

En Ajustes → Acerca del teléfono y Software, registrar código SM-…, Android, One UI, parche y si tiene SIM/PIN de SIM. «A13» no basta para certificar. Si ya existe administrador de una organización u otra persona, no intentar sustituirlo. Confirmar propiedad y permiso para restablecer.

Con ADB ya habilitado para pruebas se pueden consultar, sin modificar datos:

```powershell
adb devices
# Tras seleccionar y comprobar el A13 como se indica en §3:
& $adb -s $serieA13 shell getprop ro.product.manufacturer
& $adb -s $serieA13 shell getprop ro.product.model
& $adb -s $serieA13 shell getprop ro.build.version.release
& $adb -s $serieA13 shell getprop ro.build.version.sdk
& $adb -s $serieA13 shell getprop ro.build.version.security_patch
```

Estos comandos suponen Platform Tools en PATH. En Windows, también puede invocarse `adb.exe` mediante su ruta dentro de `%LOCALAPPDATA%\Android\Sdk\platform-tools`. El número de serie que muestre ADB no se publica ni se envía al relay.

## 2. Preparar Windows y el proyecto

Instalar Android Studio estable y los SDK Platform Tools. Crear proyecto Kotlin/Compose y fijar nombres de módulos de la especificación. Usar el JDK incluido compatible con la versión de Gradle generada por el IDE; no cambiar versiones de Kotlin/Gradle al azar. Instalar plataforma API 36, configurar minSdk 31 en hijo y 30 en padre. Guardar wrapper, catálogo de versiones y configuración en Git. No guardar keystores ni contraseñas en el repositorio.

Desde la carpeta `android`, cuando los módulos ya estén implementados:

```powershell
.\gradlew.bat :core-protocol:test
.\gradlew.bat :app-child:testDebugUnitTest
.\gradlew.bat :app-child:assembleDebug
```

Los comandos son objetivos de construcción para el repositorio por crear; **no pueden ejecutarse sobre este ZIP de documentación**. Para instrumentación se usará `:app-child:connectedDebugAndroidTest` con un dispositivo de laboratorio conectado.

## 3. Copia de seguridad y entorno limpio

Guardar previamente información que el propietario desee conservar. Verificar credenciales Google/Samsung y obligaciones de protección de restablecimiento. Cualquier restablecimiento se hace manualmente y con confirmación del propietario; esta guía no lo ejecuta. Dejar el equipo limpio, sin cuentas añadidas ni otro propietario. Activar opciones de desarrollador/depuración únicamente para laboratorio, autorizar el computador del propietario e instalar la compilación de pruebas.

H01 prepara el receiver y HOME sin activar quiosco, HOME persistente, bloqueo de
ADB/Wi-Fi ni políticas de endurecimiento. El debug declara `testOnly=true`
exclusivamente en `src/debug/AndroidManifest.xml`; release no lleva ese atributo.
La retirada del admin de prueba por ADB deberá comprobarse en laboratorio, no
se considera recuperación H05 implementada. Android puede aplicar sus valores
predeterminados al aprovisionar un Device Owner.

**Regla obligatoria: A13 SM-A135M únicamente. No tocar el A56.** Obtener el destino
sin publicar su serie y detenerse si no hay un único A13 autorizado:

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$candidatos = @(& $adb devices -l | Where-Object { $_ -match '^\S+\s+device\s+.*\bmodel:SM_A135M(?:\s|$)' })
if ($candidatos.Count -ne 1) { throw 'No hay un único A13 autorizado' }
$serieA13 = ($candidatos[0] -split '\s+')[0]
if ((& $adb -s $serieA13 shell getprop ro.product.model).Trim() -ne 'SM-A135M') {
    throw 'Destino incorrecto: no continuar'
}
```

Ejemplo después de construir un APK debug de laboratorio, **solo después de
autorizar por separado actualización y aprovisionamiento H01**. Comprobar de
nuevo el modelo antes de cada operación. Desde `android/`:

```powershell
& $adb -s $serieA13 install -r -t .\app-child\build\outputs\apk\debug\app-child-debug.apk
& $adb -s $serieA13 shell dpm set-device-owner dev.controlparental.child/.admin.ChildAdminReceiver
& $adb -s $serieA13 shell dpm list-owners
```

El comando `dpm` requiere que esa clase exista y esté correctamente declarada. Se comprueba además en la UI de diagnóstico `isDeviceOwnerApp == true`. Ante error de cuentas, usuarios o aprovisionamiento, corregir las precondiciones; no buscar bypasses ni hacer root. El procedimiento ADB está documentado para desarrollo [S17].

No volcar cuentas o `dumpsys device_policy` completo en documentación/logs públicos.
H-OWNER-01 requiere observar la UI y ejecutar el diagnóstico de Android, no basta
con compilar el APK. Tras una instalación de tests coordinada, el runner puede
exigir explícitamente Owner real:

```powershell
& $adb -s $serieA13 shell am instrument -w -r -e class dev.controlparental.child.admin.ComponentesAdministracionTest -e exigirDeviceOwner true dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Ese test solo lee diagnóstico y metadata; no aprovisiona ni modifica políticas.
Mientras no se ejecute después de aprovisionar y se observe la UI, H01 continúa
PENDIENTE_VALIDACION. La autorización anterior C02 no incluye estas operaciones.

En la fase de laboratorio no activar todavía `DISALLOW_DEBUGGING_FEATURES` ni bloquear la única ruta de mantenimiento. Las variantes debug y release deben separarse de manera que las salidas de laboratorio no estén presentes en release. No dejar `android:testOnly=true` en producción.

## 4. Orden seguro para las primeras pruebas

H02 preparado: seguir el procedimiento de `pruebas/EVIDENCIA_H02.md` tras autorizar
actualización y prueba concreta. El adulto debe conocer su PIN/patrón/contraseña
Android, sin compartirlo. Pulsar el botón de la app, encender pantalla con botón
lateral si se apaga y abrir/salir de Llamada de emergencia **sin marcar ni llamar**.
No configurar credenciales, reiniciar ni cortar la red automáticamente; esas
variantes se coordinan aparte. Solicitar lockNow no acredita acceso al marcador.

Primero implementar y comprobar emergencia/keyguard con políticas mínimas. Luego implementar recuperación local. Solo después activar quiosco y endurecimiento progresivo. Registrar el resultado de cada política y la vuelta segura a mantenimiento.

Orden mínimo: diagnóstico → emergencia → persistencia bloqueada/boot → recuperación → quiosco mínimo → apps aprobadas → red/servicio → firma/vínculo → relay → padre → pruebas de proceso y energía. Durante el desarrollo temprano pueden usarse fakes y fixtures, pero no una clave maestra dentro del APK del equipo diario.

Antes de una nueva política que afecte pantalla, redes o instalación, comprobar que el adulto tiene ruta de recuperación operativa. No bloquear Wi-Fi y depuración a la vez si todavía no existe mantenimiento local funcional.

## 5. Vincular y preparar Render

Completar el flujo de dos QR descrito en CP/1. Configurar aplicaciones autorizadas presencialmente. Conservar externamente los códigos generados por la app; este ZIP no incluye códigos reales. Copiar a Render exclusivamente las claves públicas/pairId/origen exportados. Comprobar conexión, estado fresco y una orden de cada tipo.

No introducir ninguna clave privada ni código de recuperación en variables de entorno, repositorios, instrucciones a una IA, capturas de chat o logs. Un QR de aceptación contiene claves públicas y hashes, no las privadas.

## 6. Ensayar reinicio, red y suspensión

Cerrar la app padre después de permitir uso: la política recibida debe continuar. Desconectar internet en el hijo: al vencer el margen debe bloquearse sin esperar al padre. Restablecer red: reevalúa la política. Reiniciar hijo: permanece bloqueado hasta autorización nueva, aunque internet funcione.

Para laboratorio se pueden usar las pruebas oficiales de Doze [S08]:

```powershell
& $adb -s $serieA13 shell dumpsys battery unplug
& $adb -s $serieA13 shell dumpsys deviceidle force-idle
# Ejecutar observaciones de red/estado previstas en la matriz.
& $adb -s $serieA13 shell dumpsys deviceidle unforce
& $adb -s $serieA13 shell dumpsys battery reset
```

Restaurar siempre el estado simulado de batería al terminar. Estos comandos son solo de pruebas autorizadas antes de cerrar la depuración. Probar también suspensión natural sin cable USB; un equipo cargando y conectado al IDE no reproduce necesariamente el uso real.

No intentar inferir bloqueo por falta de mensajes: observar el estado de la pantalla y las apps que podrían seguir accesibles. Probar muerte del proceso controlador mientras una app permitida está delante. Si hay acceso no autorizado, registrar fallo; no esconderlo.

## 7. Versión final privada

Construir release con firma propia y guardar material de firma fuera del repositorio en lugar protegido. El keystore de firma del APK pertenece al desarrollador y es distinto de la clave parental generada en cada teléfono. Sin firma de APK conservada no habrá actualización normal del mismo paquete.

Probar actualización, recuperación y retirada antes de aplicar endurecimiento definitivo. Instalar release mediante un procedimiento limpio/autorizado, volver a vincular cuando cambie la identidad de instalación y repetir la matriz crítica. Retirar depuración y sus autorizaciones del computador desde la preparación del dispositivo; luego aplicar las restricciones finales.

**No entregar un debug endurecido como solución final.** El adulto confirma las limitaciones de recuperación física y el acceso a emergencia del modelo concreto. Guardar la ficha de certificación en `pruebas/FICHA_DISPOSITIVO.md`.
