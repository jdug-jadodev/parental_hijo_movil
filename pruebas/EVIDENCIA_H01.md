# Evidencia H01 — DPC y launcher mínimos

Fecha: 9 de octubre de 2026. Estado: PENDIENTE_VALIDACION física; código compilado y pruebas JVM aprobadas.

Actualización de laboratorio: BLOQUEADA_POR_PRECONDICION (3 cuentas en el A13).

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

## Pruebas físicas NO EJECUTADAS en esta unidad

- Actualización de APK H01 e instalación de sus tests en el A13.
- dpm set-device-owner, observación UI con Owner false/true y H-OWNER-01.
- Comprobación de retirada segura del admin testOnly de laboratorio.
- HOME persistente, Lock Task, emergencia, PIN/SIM y recuperación.

El A13 conserva los APK C02 anteriormente instalados; no se ha actualizado con H01.
El A56 queda fuera de alcance: ninguna operación sobre él está autorizada.

## Fuentes oficiales consultadas

- https://developer.android.com/work/dpc/dedicated-devices/cookbook — HOME y ruta ADB de laboratorio sin cuentas.
- https://developer.android.com/guide/topics/admin/device-admin — receiver protegido y metadata de políticas.

No activar endurecimiento antes de H02/H05. No borrar ni cambiar cuentas para
resolver errores de aprovisionamiento sin coordinar la acción concreta.
