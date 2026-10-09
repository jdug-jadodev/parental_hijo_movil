# Evidencia H01 — DPC y launcher mínimos

Fecha: 9 de octubre de 2026. Estado: H01 completada en laboratorio mínimo; Owner real/tests Android comprobados y pantalla actual confirmada por el propietario.

Antecedente: bloqueada inicialmente por 3 registros de cuentas; precondición resuelta presencialmente por el propietario.

## Alcance

- ChildAdminReceiver estable, exportado y protegido por BIND_DEVICE_ADMIN.
- Metadata device_admin con force-lock/wipe-data previstos por SPEC-H H4;
  ningún callback bloquea o borra el equipo.
- LauncherActivity exportada como HOME/DEFAULT y LAUNCHER en filtros separados.
- Alias de ActividadInicial conserva el componente explícito de la versión anterior.
- Diagnóstico de isDeviceOwnerApp e isAdminActive, solo lectura y con error cerrado.
- Sin Owner real: «No configurado». Con Owner/admin comprobados: «Bloqueado — en
  preparación», acompañado de aviso explícito de que NO aplica bloqueo/quiosco.
- Sin startLockTask, whitelist, HOME persistente ni restricciones añadidas.
- Debug testOnly solo en variante debug; no debe estar en release.
- No se implementa enrolamiento QR, servicio, boot, persistencia o recuperación en H01.

La documentación principal `00_EMPIEZA_AQUI.md` y `AGENTS.md` prohíben tocar el A56.
La guía de laboratorio usa destino A13 comprobado y explícito para cada comando.

## Pruebas

Cuatro tests JVM nuevos distinguen app normal, admin tradicional, Owner real y
datos ausentes/incoherentes. Tres tests Android nuevos comprueban metadata,
resolución HOME/LAUNCHER y diagnóstico frente a DevicePolicyManager; el parámetro
`exigirDeviceOwner=true` obliga a demostrar Owner real en la prueba posterior.

Construcción iniciada desde android/:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
```

Resultado final: BUILD SUCCESSFUL. Informes XML: 231 tests JVM totales, 0 fallos,
0 errores y 0 omitidos; incluye los cuatro tests nuevos de diagnóstico. Núcleo
UP-TO-DATE; tests del hijo ejecutados. APK debug, APK instrumentado y release sin
firmar construidos. Lint debug: 0 errores y 10 avisos de actualización; lint vital
release correcto. Aviso de libandroidx.graphics.path.so sin stripping conservado.
La variante release sin firmar no se instala ni se presenta como versión final.

Primer intento de construcción: falló la compilación de tests Android por usar
ActivityInfo en lugar de ResolveInfo y una propiedad no pública de DeviceAdminInfo.
Corregido usando ResolveInfo y únicamente constantes/métodos del SDK público,
comprobados también con javap sobre android-36/android.jar. No se añadió reflexión.

Inspecciones locales ejecutadas desde la raíz:

```powershell
python -B pruebas/verificar_apk_base.py
python -B pruebas/verificar_apk_h01.py
```

Resultados: app debug sin fixtures ni clases de tests; DPC/HOME y políticas
declaradas en ambos APK. Debug testOnly=true; release sin testOnly ni debuggable.
Permisos: únicamente el permiso signature interno que añade AndroidX para
receivers no exportados, sin permisos de monitoreo ni ejecución solicitados.

El inspector H01 se ajustó tras detectar el permiso interno AndroidX y las rutas
XML acortadas por optimización release. Resuelve ahora el XML desde la tabla de
recursos; la ejecución final pasó para debug y release. No demuestra Owner real.

## Preflight físico autorizado — 9 de octubre de 2026

El propietario autorizó actualizar ambos APK y activar Device Owner de laboratorio
en el A13. Se seleccionó de forma única SM_A135M de los modelos anunciados por ADB,
con serie solo en memoria; cada consulta del equipo usó el mismo destino `adb -s`.
`getprop ro.product.model`: SM-A135M. El A56 no recibió ningún comando.

- `dpm list-owners`: no owners.
- `pm list users`: un usuario principal; nombres descartados sin publicarlos.
- `am get-current-user`: 0.
- `dumpsys account`: un bloque de recuento, **3 cuentas**. Se extrajo solo el número
  de la salida mantenida en memoria; no se mostraron ni guardaron nombres/detalles.

El chequeo exigía cero cuentas y detuvo el procedimiento. Una segunda consulta
limitada al recuento confirmó las 3 cuentas. **No se ejecutó instalación, actualización,
set-device-owner, retirada de admin, reinicio ni borrado.** La app H01 no llegó al
teléfono; siguen instalados los APK C02 anteriores. No se eliminan cuentas ni se
buscan bypasses. Esperar al propietario y repetir el preflight después de su preparación.

Ante la observación del propietario de que Ajustes solo muestra una cuenta, se
consultaron únicamente los tipos técnicos en el A13: com.osp.app.signin,
com.samsung.android.mobileservice y com.samsung.android.coreapps (uno de cada tipo).
Son registros de servicios Samsung; el recuento 3 de AccountManager NO demuestra
tres cuentas personales distintas ni tres entradas visibles en Ajustes. No se
compararon nombres o identidades ni se mostraron correos. No se deshabilitaron
servicios ni se borraron sus datos. Preparar la cuenta visible por vías normales
y volver a comprobar el recuento, sin exigir eliminar cuentas internas a ciegas.

## Continuación física autorizada — 9 de octubre de 2026

Tras «listo, continua», el preflight dirigido únicamente al A13 confirmó cero
cuentas, un único usuario principal actual 0 y ausencia de Owner previo. La ayuda
de DPM anuncia remove-active-admin para aplicaciones testOnly; devuelve código
255 pese a mostrar la ayuda. El guard inicial detuvo esa ejecución por dicho
código, y la lectura posterior confirmó el comando sin modificar nada.
No se ejecutó retirada y no se presenta la ayuda como una recuperación probada.

Inspector local verificar_apk_h01.py repetido: OK en debug/release. Ambas
actualizaciones `adb -s $serieA13 install -r -t ...`: Success.

Antes de aprovisionar, ejecución dirigida de ComponentesAdministracionTest:
**OK (3 tests), 0.116 s**. Comprueba metadata, HOME/LAUNCHER y diagnóstico frente
a Android. Apertura explícita LauncherActivity: Status ok, COLD.

Comando de aprovisionamiento ejecutado con el destino A13 comprobado:

```powershell
& $adb -s $serieA13 shell dpm set-device-owner dev.controlparental.child/.admin.ChildAdminReceiver
& $adb -s $serieA13 shell dpm list-owners
& $adb -s $serieA13 shell am instrument -w -r -e exigirDeviceOwner true dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado DPM: Success; Device Owner y admin activo establecidos para nuestro
componente. list-owners: un Owner en usuario 0, DeviceOwner, Affiliated.
Instrumentación posterior: **OK (6 tests), 1.643 s**: tres H01 y tres C02.
El test exige isDeviceOwnerApp=true, isAdminActive=true, diagnóstico de preparación
y ausencia de permiso Lock Task. No aplica whitelist, HOME persistente ni quiosco.
Tras instrumentación se abrió de nuevo LauncherActivity: Status ok, COLD.

Salidas bajo build/ ignorado: resultado-h01-sin-owner-a13.txt y resultado-h01-owner-a13.txt.
APK app SHA-256: `334ea686acad062ad73bcf21f57e0714502b467cba8ea448f738266c08bf2c51`.
APK tests SHA-256: `4def031695ef9220c3bf4eda5291cfbb4b75ad92d9381c3cfdff66b0ab4931d6`.

Lectura filtrada de dumpsys user: observada no_add_managed_profile en restricciones
efectivas y globales none; no se afirma auditoría completa de políticas. No se
añadieron restricciones desde el código H01; Android aplica sus valores propios.

Intentos de observar la UI anterior al Owner con uiautomator hacia /dev/tty:
el primero no devolvió XML; el segundo con shell -tt devolvió jerarquía pero sin
textos de nuestra app. No se guardó la jerarquía ni se publicó contenido de otras
apps. Por tanto NO se considera observada la pantalla «No configurado» ni se
atribuye una causa no comprobada. La UI previa al Owner sigue sin observación directa.

Confirmación de pantalla actual: tras pedir al propietario comprobar «Bloqueado —
en preparación», Owner Sí/admin Sí y aviso de quiosco NO APLICADO, respondió
«perfecto». Se registra como confirmación presencial comunicada por el propietario,
no como captura automatizada ni prueba de bloqueo. Con Owner real comprobado por
Android y seis tests correctos, se cierra el alcance mínimo H01.

Sin borrado, retirada, cambios de cuentas por el agente ni endurecimiento añadido.
El A56 no recibió consultas, instalaciones ni pruebas; cada operación usó adb -s
con destino SM-A135M comprobado y serie solo en memoria.

## Pruebas físicas pendientes

- Observación directa de UI previa sin Owner; cubierta solo por lógica JVM y diagnóstico Android, no por captura de aquella pantalla. La confirmación del propietario corresponde a la pantalla actual con Owner.
- Comprobación de retirada segura del admin testOnly de laboratorio.
- HOME persistente, Lock Task, emergencia, PIN/SIM y recuperación.

El A13 tiene los APK H01 actualizados y nuestro Device Owner de laboratorio activo.
El A56 queda fuera de alcance: ninguna operación sobre él está autorizada.

## Fuentes oficiales consultadas

- https://developer.android.com/work/dpc/dedicated-devices/cookbook — HOME y ruta ADB de laboratorio sin cuentas.
- https://developer.android.com/guide/topics/admin/device-admin — receiver protegido y metadata de políticas.

No activar endurecimiento antes de H02/H05. No borrar ni cambiar cuentas para
resolver errores de aprovisionamiento sin coordinar la acción concreta.
