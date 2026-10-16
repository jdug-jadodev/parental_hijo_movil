# LOCAL-COMPAT01 — preparar control conservando protección

## Motivo y alcance

El propietario preguntó si liberarRAM borra todo y pidió continuar. Antes de
probar el control se encontró en `ArranquePilotoAndroid.armar` una precondición
antigua: exigía snapshot de protección ausente y cuatro políticas false. Era
incompatible con la protección que el propietario acababa de activar/verificar.
No se pidió retirar protección ni se ejecutó el fallo en el A13.

Corrección mínima: preparación acepta baseline original sin restricciones O
protección ACTIVA propia con snapshot íntegro y políticas coherentes, comprobados
por `ProteccionLocal.consultar`. Parcial/pending/corrupción/IO/ausencia de snapshot
con restricciones/cambios ajenos siguen rechazados. Lectura serializada con el
actor de aplicar/restaurar, desde worker de arranque, no desde hiloUI. Conserva
la guardia adulta posterior y las precondiciones de LockTask/registroboot.
No escribe, borra, restaura ni cambia políticas/snapshot de protección; no cambia
timer, sesión, red10s, permisos, claves, identidad, PAIR/pending o contratos.

## RAM y persistencia

- Sesión adulta y permiso de uso: RAM; no sobreviven a proceso muerto/reinicio.
  Reabrir requiere código y autorización nueva; no recupera minutos para abrir.
- Código/pregunta/validadores: registro cifrado CE/AtomicFile + claveKeystore.
  No se guarda el código adulto en texto para revelarlo. LiberarRAM no borra
  ese archivo ni la clave. Una nueva instancia del motor relee el registro.
- Protección: políticas de Android más snapshot persistido; liberarRAM/salir no
  la retira. Registros PAIR durables permanecen; ofertas/candidatas sóloRAM caducan.
- Borrar datos de la app/reset/desinstalar son operaciones diferentes, no
  realizadas ni autorizadas. No se ha probado muerte del proceso físicamente;
  no se afirma bloqueo instantáneo ante toda terminación Android.

## Comprobaciones locales

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests dev.controlparental.child.protection.ProteccionLocalTest --tests dev.controlparental.child.boot.RegistroPilotoTest --tests dev.controlparental.child.lock.SnapshotLockLocalTest --tests dev.controlparental.child.lock.TransicionLocalTest --tests dev.controlparental.child.local.SesionAdultoTest --tests dev.controlparental.child.network.UsoRedLocalTest --tests dev.controlparental.child.network.UsoTiempoTest :app-child:assembleDebug :app-child:lintRelease --offline --console=plain
```

**SUCCESS22s/15ejecutadas55UPTODATE**, XML **110casos0fallos0errores**, incluyendo
4nuevos: activa propia/read-only/recreación; baseline sin protección vs ajena;
parcial/corrupto/perdido; relectura cambios/capacidad/IO. PuertosDPM/disco/reloj
FALSOS en JVM, sin Android real. Builddebug nuevo y compileRelease ejecutados;
lintRelease análisis ejecutado/reporteUPTODATE **0errores27avisos**. No suite
histórica completa, no nueva pasada lintDebug (error previo del prototipo
registrado en UI-LOCAL01), ningún cruceC8 ni acceso otrorepo.

APK `android/app-child/build/outputs/apk/debug/app-child-debug.apk` SHA256
**e097e002434aee19982eb0f643c89ef65f30665909c6f3978dd532ba8133bce5**.
apksigner verifica firma/certificadohistórico
**39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439**.
aapt paquete dev.controlparental.child/versión1-0.1.0/min31target36/testOnly/
únicoLauncherActivity. Sin nuevos permisos ni dependencias. diff-check correcto.

## Relevo y siguiente paso físico (pendiente de permiso directo nuevo)

Cambios: boot/ArranquePilotoAndroid; protection/ProteccionLocal y ProteccionAndroid;
testProteccionLocal; ESTADO/guía/evidencia. Código listo, todavía NOinstalado;
último instalado2b5ea130…15a821 con protección activa acreditada en evidencia
UI_LOCAL01_ACTUALIZACION. No ADB ni teléfono durante esta corrección.

Siguiente: pedir permiso directo para comprobar compatibilidad y UNA nueva
actualización conservadora con protección activa; reconfirmar adulto/salida/
emergencia, mismoA13SM-A135M/adb-s, A56excluido. Después preparaciónMANUAL con
código fresco y prueba local coordinada de tiempo/bloqueo, conservando protección.
Sin reboot/apagarpantalla/cortesred-energía/clear/reset/uninstall/Owner/PIN/QR/
remoto/lecturasCE-Keystore; no instalar si compatibilidad incierta ni reintentar.
No atribuir pruebas físicas a JVM ni extender permisos anteriores consumidos.
