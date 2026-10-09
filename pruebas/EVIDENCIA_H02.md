# Evidencia H02 — emergencia por keyguard del sistema

Fecha: 9 de octubre de 2026. Estado: PENDIENTE_VALIDACION física; código compilado y lógica JVM probada.

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

H-EMG-01/02 y G0 siguen PENDIENTES. No se activa Lock Task ni endurecimiento y no se
considera probado un marcador por la ausencia de excepción en lockNow. Llamadas
reales no autorizadas están prohibidas; tampoco se prueba borrar/retirar el equipo.

APK instalado en el A13 sigue siendo H01. H02 no se ha instalado ni ejecutado allí.
