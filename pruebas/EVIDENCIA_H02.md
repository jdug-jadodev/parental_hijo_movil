# Evidencia H02 — emergencia por keyguard del sistema

Fecha: 9 de octubre de 2026. Estado: VALIDACION_PARCIAL — rutas básica y sin internet confirmadas; variantes de arranque y SIM pendientes.

## Implementación

- Acceso de emergencia incluido en el launcher incluso sin Owner o con diagnóstico fallido.
- Texto de ruta del sistema y botón explícito «Ir a pantalla de bloqueo».
- EmergencyAccess puro: sin red, reloj, política, PIN parental ni números telefónicos.
- PolicyEnforcer es el único adaptador que llama DevicePolicyManager.lockNow(),
  sin flags, y vuelve a comprobar Owner/admin, capacidad Device Admin y credencial Android.
- No se llaman resetPassword, setKeyguardDisabled, APIs privadas de Telecom, intents
  privados ni paquetes Samsung fijos. No hay permisos de llamadas/telefonía nuevos.
- Una llamada sin excepción devuelve SOLICITUD_ENVIADA, NO APPLIED ni confirmación de marcador.
- Sin credencial Android se informa preparación pendiente: lockNow puede solo dormir
  el equipo sin bloquearlo, por lo que no se presenta esa situación como ruta validada.
- Ante un error se indica la opción del sistema mediante botón lateral; su
  disponibilidad concreta sigue requiriendo prueba presencial.

## Fuentes públicas consultadas

- https://developer.android.com/reference/android/app/admin/DevicePolicyManager#lockNow()
  — permiso/política force-lock, autenticación fuerte posterior y comportamiento sin credencial.
- https://developer.android.com/reference/android/app/KeyguardManager#isDeviceSecure()
  — credencial del dispositivo, a diferencia de isKeyguardSecure que también considera PIN SIM.
- SPEC-H H9 y ADR-H-004: ruta por keyguard y validación antes del endurecimiento.

## Pruebas preparadas

Seis tests JVM: acción solo explícita, cuatro precondiciones negativas, dieciséis
snapshots completos/incompletos, fallo de lectura y fallo de API sin falsa confirmación.
Un test Android de solo lectura contrasta las precondiciones con las APIs públicas.
No ejecuta lockNow ni abre marcador: el adulto realiza la prueba explícita coordinada.
El argumento exigirCredencialAndroid=true exige credencial Android y todas las
precondiciones de solicitud, sin conocer ni leer el PIN/patrón/contraseña.

Construcción iniciada desde android/:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
```

Resultado final: BUILD SUCCESSFUL (dos construcciones correctas, la última tras
priorizar el acceso de emergencia antes de los textos de diagnóstico). Informes
XML: **237 tests JVM, 0 fallos, 0 errores, 0 omitidos**; seis tests nuevos H02.
Tests del hijo ejecutados; núcleo UP-TO-DATE. Debug, instrumentado y release sin
firmar construidos; lint debug 0 errores y 10 avisos de actualización, lint vital
release correcto. No se instala la variante release sin firmar.

Desde la raíz se ejecutaron verificar_apk_base.py y verificar_apk_h01.py: ambos OK.
App sin fixtures/clases de tests y sin permisos nuevos; metadata DPC y separación
testOnly exclusivo debug conservadas. Un test Android H02 compilado, NO EJECUTADO.
No se ejecutó ADB ni se actualizó ningún teléfono durante esta unidad de código.

## Procedimiento físico a coordinar — solo A13 SM-A135M

1. Adulto presente, ADB autorizado y A13 desbloqueado. Mantener A56 excluido,
   seleccionar destino comprobado mediante adb -s; nunca runner de todos los equipos.
2. Confirmar que el adulto conoce el PIN/patrón/contraseña de Android, sin compartirlo.
   Si no hay credencial, configurarla presencialmente; la app no la crea ni modifica.
3. Autorizar la actualización de ambos APK H02 y ejecutar sus tests de diagnóstico.
4. En la app, pulsar «Ir a pantalla de bloqueo». Puede apagarse la pantalla;
   encenderla con botón lateral y, si hace falta, deslizar hasta la pantalla del PIN.
5. Abrir «Llamada de emergencia» del sistema **sin marcar ni llamar**. Salir del
   marcador y desbloquear con la credencial Android. Volver al launcher y comprobar
   que Owner/admin siguen activos y no se habilita control parental ficticio.
6. Registrar SIM/PIN SIM sin números ni códigos, comportamiento observado y pasos exactos.
7. Coordinar aparte variantes sin red y de arranque. No desconectar Wi-Fi/datos,
   reiniciar, bloquear la depuración ni cambiar credenciales automáticamente.

H-EMG-01 y G0 siguen PENDIENTES; H-EMG-02 tiene confirmación posterior abajo. No se activa Lock Task ni endurecimiento y no se
considera probado un marcador por la ausencia de excepción en lockNow. Llamadas
reales no autorizadas están prohibidas; tampoco se prueba borrar/retirar el equipo.

## Actualización y diagnóstico autorizados en A13

El propietario confirmó que conoce su credencial Android y autorizó actualizar
ambos APK H02 para la prueba sin llamar. Cada operación usó adb -s dirigido al
único SM_A135M autorizado, con comprobación de modelo SM-A135M antes de actuar;
serie solo en memoria. El Owner previo de nuestra app se comprobó sin sustituirlo.
El A56 no recibió consultas, instalaciones ni tests.

Construcción previa con core-protocol:test, app-child:testDebugUnitTest,
assembleDebug, assembleDebugAndroidTest y lintDebug: BUILD SUCCESSFUL, JVM
UP-TO-DATE (237 resultados anteriores conservados), tests Android recompilados.
Inspector verificar_apk_base.py repetido: OK.

Ambos `adb -s $serieA13 install -r -t ...`: Success. Instrumentación:

```powershell
& $adb -s $serieA13 shell am instrument -w -r -e exigirDeviceOwner true -e exigirCredencialAndroid true dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: **OK (7 tests), 1.666 s**: tres H01, tres C02 y uno H02.
Owner/admin real, capacidad de administración y credencial Android comprobados.
El test H02 es solo lectura: no ejecuta lockNow ni verifica marcador.
Salida en android/app-child/build/resultado-h02-diagnostico-a13.txt, ignorado por Git.
LauncherActivity abierto explícitamente: Status ok, COLD.

APK app SHA-256: `3227dee29d5fd0e1c3af8f5f18c658f5a372a7976fe47665aec4daec4ba19a02`.
APK tests SHA-256: `66ccf16604e7dfcb20975e811c710d51d94c0ef79a7fb6da6da4ea50f5ab8f16`.

El A13 ya tiene H02 instalado, conservando Device Owner y credencial Android.
No se bloquearon pantalla/redes automáticamente ni se marcaron números desde el agente.

## Confirmación presencial de la ruta básica

Tras indicar al adulto pulsar «Ir a pantalla de bloqueo», encender pantalla con
botón lateral si se apaga, abrir/salir de «Llamada de emergencia» SIN marcar ni
llamar y volver al launcher con su credencial Android, respondió «todo correcto».
Se registra como confirmación presencial comunicada por el propietario, no como
captura automatizada. No comunicó diferencias en los pasos; no se infiere si la
pantalla se apagó ni si fue necesario deslizar. No se registró PIN ni información SIM.

Resultado: ruta básica de botón → keyguard → marcador → salida/desbloqueo → app
confirmada en SM-A135M con el firmware inventariado. Sin llamada real ni quiosco.
Esto NO aprueba por sí solo H-EMG-01 completo (arranque/PIN SIM), H-EMG-02 sin red,
G0 completo ni emergencia con endurecimiento H-EMG-03/04.

Siguiente comprobación a coordinar: repetir la ruta básica en A13 con Wi-Fi y
datos móviles desactivados presencialmente, conservando radio/SIM (NO modo avión),
sin marcar/llamar. Reactivar conectividad al terminar. No ejecutar desconexiones
ADB, reinicios ni modificaciones del A56 automáticamente.

## Confirmación presencial sin internet — H-EMG-02

Se indicó al propietario, solo en A13: desactivar Wi-Fi y datos móviles sin usar
modo avión, repetir botón → marcador → salida/vuelta a app SIN marcar/llamar y
reactivar la conectividad. Respondió «correcto». Se registra como confirmación
presencial comunicada de funcionamiento igual y restauración indicada, no como
medición automatizada de red ni llamada probada.

H-EMG-02 aprobado en etapa H02 sin quiosco/endurecimiento, con el firmware actual.
Esto no acredita cobertura, llamada completa, arranque antes de PIN Android,
variantes PIN SIM ni emergencia bajo quiosco. G0 completo continúa pendiente.
No se ejecutó ADB ni se modificó el A56 en este registro documental.

Siguiente coordinación: conocer presencia de SIM/PIN SIM sin números ni códigos,
y solicitar al adulto prueba manual de reinicio y apertura/salida del marcador
antes de introducir la credencial Android. No reiniciar automáticamente ni
modificar la SIM o las credenciales.
