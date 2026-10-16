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

## Preparación anterior al permiso físico (histórico)

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

## Actualización y prueba física posteriores — completadas

Código en `8cc4823`. Propietario autorizó DIRECTAMENTE **«Sí, actualizar y probar»**
a una actualización adicional conservadora, A13 conectado/pantalla-adulto-salida/
emergencia disponibles, preparación MANUAL y permiso NORMAL de1min hasta bloqueo.
LecturasADB limitadas a compatibilidad/paquete/políticas; sin reiniciar, apagar
pantalla ni cortar internet. A56 excluido.

- Inventario seleccionó únicoA13 anunciado, destino coincidente con actualización
  anterior y modeloSM-A135M reconfirmado. Todas las operaciones dirigidas poradb-s.
  Serial sólo RAM/contexto temporal externo, no repositorio.
- APKprevio recuperado SHA2b5ea130…15a821, firma/certificado y paqueteversión
  compatibles. Baseline ya protegido: uninstallBlockedhijo=true y
  no_control_appsusuario=true; globalindependiente sinadmins/null; LockTask
  lista vacía/flags16. Candidato/hash/firma/modelo reconfirmados.
- **UNA install-r-t → Success**, sin reintento. APKposterior recuperado/hash
  **e097e002434aee19982eb0f643c89ef65f30665909c6f3978dd532ba8133bce5** y
  certificado verificados. Cero diferencias metadatos/versión/firstInstallTime/
  ceDataInode/estado, Owner y políticas consultadas tras actualizar. Ninguna
  lectura de contenidoCE/DP/Keystore/identidad/PAIR/pending; continuidad de
  directorio no es auditoría semántica de los almacenes.
- POSTactualización el propietario confirmó **«Sí, todo correcto»**: código
  habitual, entrar/cerrar/volver a entrar, protecciónactiva y emergencia visible.
- Ejecutó manualmente «Seguridad y configuración → Preparar control local»,
  código fresco y «Confirmar» UNA vez; informó **«Teléfono bloqueado»**. No se
  retiró protección para preparar. No se reportó petición de notificaciones.
- Ejecutó manualmente «PERMITIR POR TIEMPO → Normal · con internet», eligió
  **1min**, código fresco y «Confirmar»; cerró acceso adulto y observó contador
  con pantalla/internet encendidos. Resultado **«Sí, contó y se bloqueó»**.
  Evidencia HUMANA de cuenta regresiva/bloqueo al terminar, no cronometraje externo
  milisegundo a milisegundo ni lectura de modoActivity porADB.
- LecturaADB posterior de políticas: uninstallBlockedhijo=true, no_control_apps
  usuario=true resueltos, globalindependiente sinadmins/null; LockTask resuelto
  paquetes={dev.controlparental.child}/flags48 (KEYGUARD+GLOBAL_ACTIONS), mismo
  DeviceOwnerChildAdminReceiver. Son políticas leídas; el bloqueo visible fue
  confirmado por el usuario, no inferido exclusivamente de la lista.
- POSTbloqueo final propietario **«Sí, todo correcto»**: entrada con código,
  cierre, Emergencia visible y Protecciónactiva. Se pidió dejar bloqueado.

### Cierre y relevo actual

**Compatibilidad protección+control, actualización y prueba manualNORMAL1min
completadas.** Último instalado AHORA e097e002…33bce5. Protección activa, control
local preparado y último estado humano bloqueado. Permiso de actualización
consumido; sin otra instalación ni tarea física iniciada.

Límites: no se probó liberarRAM/muerte de proceso/reboot/pantallaapagada/Doze/
cortered/OFFLINE/libre/desinstalar/actualizar otraapp/recuperar cambiando código/
llamada de emergencia en esta unidad. Preservación de registros basada en diseño
y uso humano de credencial, no lecturas de secretos. No nuevas garantías de
enforcement instantáneo ante proceso muerto; emergencia visible no prueba llamada.
Sin WSS/C8/padre/Render/otrosrepos/subagentes/push/deploy/root/reset/Owner/PIN/
uninstall/clear ni protecciones desactivadas. Sin builds/tests repetidos después
del APK verificado. diff-check y commit documental de cierre.

Siguiente: uso manual del control local mediante etiquetas de la guía; otra
prueba física distinta sólo con alcance concreto directo. No reanudar T10/remoto.
