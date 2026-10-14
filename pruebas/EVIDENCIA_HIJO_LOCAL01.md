# HIJO-LOCAL01 — acceso adulto local MAIN y recuperación por pregunta

2026-10-13, base `f9eeeea`, bloque1 `6d5bbf9`. **IMPLEMENTADO / VALIDACION_LOCAL_LOGRADA**.
Instalación y pruebas A13 realizadas solo después de autorización específica nueva.
Configuración real exclusivamente en UI presencial; ningún secreto leído/registrado.

## Entrega operativa acotada

Launcher MAIN abre «Acceso adulto local: código y pregunta». Activity no exportada,
sin extras autoritativos, sin debug bypass. Alta solo si almacén virgen coherente,
Device Owner y credencial Android segura: prompt oficial DEVICE_CREDENTIAL,
callback/ticket RAM120s vigente y nueva comprobación de ausencia antes de commit.
No alta al arrancar, ni reemplazo si ya configurado. USE_BIOMETRIC declarado MAIN
es permiso normal sin grant/runtime especial; su uso es solo bootstrap presencial,
no autenticación cotidiana del código adulto. Si el niño conoce el PIN Android,
ese bootstrap de laboratorio no demuestra identidad parental: alta física debe
coordinarse con el dueño antes de entregarle el equipo.

Código propio ASCII5–10 como cadena, confirmación doble, ceros conservados, sin
trim/equivalencia Unicode. Pregunta personalizada1–200 puntos de código, respuesta
1–256, no vacíos/solo espacios/controles/UTF inválido. Campos UI limitados también
en unidades UTF16. **Comparación exacta v1 de respuesta**, sin NFC/case folding/
trim: mayúsculas, acentos y espacios cuentan; explicado en alta y recuperación.
La normalización NFC del diseño anterior era propuesta, no acuerdo operativo;
se adopta aquí comparación conservadora explícita sin cambiar PIN silenciosamente.

Entrada con código abre administración RAM; no concede tiempo/uso, no cancela
permiso vigente ni ejecuta políticas. Panel mínimo indica sesión y controles de
uso/red/ajustes/desinstalación deshabilitados. Respuesta correcta solo concede
CAMBIAR_CODIGO ligado a revisión/boot/ticket, nunca administración o uso. Nuevo
código exige confirmación, conserva pregunta/verificador respuesta/cupo, incrementa
revisión, commit/readback y cierra capacidad; debe entrar con el código nuevo.
No se revela anterior ni se guarda respuesta en claro. No cambio ordinario de
pregunta, permisos o retiro implementado; recuperación por pregunta es la vía nueva.

Sesión/capacidad/tickets solo RAM120s absolutos desde solicitud, cambio no extiende
ventana original; cierre al salir/fondo/destrucción/reentrada/boot distinto/reloj
regresivo. Callback viejo no abre sesión/alta/reset. Una operación/KDF por proceso,
sin cola ilimitada; actor IO separado y motor sincronizado. Cancelar no deshace
commit durable incierto: si pasó la frontera de escritura puede quedar código
nuevo, pero callback inválido nunca abre sesión. Emergencia es siempre independiente.
Autoridad vigente se revalida antes del commit; no agregar controles futuros que
usen solo UI abierta, sin revisión/ticket fresco.

FLAG_SECURE, campos sin save/autofill/clipboard/selección, sin Bundle/rememberSaveable,
sin logs/extras/red, IME sin aprendizaje personalizado solicitado. Strings JVM/UI
no pueden borrarse garantizadamente de toda RAM; teclado/firmware/root fuera de
garantía absoluta. Pregunta puede verse localmente al recuperar; no se transmite.

## Persistencia y espera

Formato **privado local v1**, no migración/versión de CP/1. PBKDF2WithHmacSHA256
210000 iteraciones/256bits, salts independientes aleatorios16bytes, comparación
MessageDigest.isEqual. Parámetro inicial razonable pendiente de medir latencia/
coste en A13, no certificación de fuerza de cinco dígitos o respuesta deducible.
Pregunta personalizada NO factor fuerte de identidad.

AES256-GCM con clave AndroidKeystore no exportable independiente de firma/CP1,
IV aleatorio de provider, dominio local/marca de instalación como AAD. CE/noBackup,
allowBackup=false/reglas existentes conservadas, AtomicFile y readback byte a byte,
lecturas≤4160bytes. Solo salts/verificadores/pregunta/revisión/cupo/espera en plano
RAM antes de cifrar. Ningún PIN/respuesta reversible en registro. Keystore/archivo/
marca parciales/perdidos/corruptos, autenticidad/tag/versión desconocida o usuario
no desbloqueado ⇒ ERROR/acceso cerrado, no regeneración de clave/alta libre/default.
Fallo inicial después de crear marca/clave queda cerrado, no se borra para reparar.

Cupo agregado código+respuesta, reserva durable ANTES de KDF, cancelación/fondo no
regalan intentos:5fallos→60s,10→300s, contador saturado10. Reinicio o reloj regresivo
reimpone espera pendiente completa, proceso/reentrada mismo boot conservan restante
elapsedRealtime; sin hora civil. Solo código correcto fuera de espera resetea cupo;
respuesta/cambio no lo limpian. Cancelación durante KDF conserva reserva sin acceso.
Los valores son parámetros privados v1 de esta unidad, no campos de protocolo.
Posible denegación administrativa por intentos; emergencia/uso no se modifican.
Olvido de código+respuesta o corrupción no tiene bypass universal ni factoryreset.
No contador hardware antirollback: una copia íntegra antigua de archivos y clave
puede retroceder credencial/cupo; no prometer protección absoluta frente a root.
Backup/muerte/reboot nunca restauran sesión porque no se persiste ninguna autoridad.

## Código y pruebas reales

Rutas bajo `android/app-child/src/`:
- main/java/dev/controlparental/child/local/{RegistroAdulto,SesionAdulto,
  AccesoAdultoLocal,AlmacenAdultoAndroid}.kt.
- main/java/dev/controlparental/child/ui/AccesoAdultoActivity.kt y LauncherActivity.kt;
  main/AndroidManifest.xml (actividad no exportada, permiso normal bootstrap).
- test/java/dev/controlparental/child/local/{AccesoAdultoLocalTest,SesionAdultoTest}.kt.
- androidTest/java/dev/controlparental/child/local/AlmacenAdultoAndroidTest.kt.
- Esta evidencia, ESTADO y relevo. Sin CP1/CP2/fixtures/planes/ADR/deps/RecoveryController/
  debug/prototipos/red/enforcement/servidor/repos ajenos modificados.

Bloque1: aproximadamente485 líneas propias y commit6d5bbf9 tras tres ejecuciones
testDebugUnitTest/testReleaseUnitTest BUILD SUCCESSFUL (263/208,16 locales nuevos).
Aviso de nombre de parámetro de almacén se corrigió antes de commit. Bloque2 UI,
tests Android y ajustes/tres tests nuevos; cierre menor al siguiente umbral500.

Desde `android/`:

```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest --console=plain
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :core-protocol:test :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

Segunda línea **BUILD FAILED por lintDebug**, después de tests/APK correctos:
ForegroundServicePermission del manifest DEBUG previo systemExempted;1error/21avisos.
No se suprimió/baselined ni añadieron permisos de alarmas. No inferir fallo físico
de FGS por lint, ni cambiar prototipo fuera de alcance.
Última línea BUILD SUCCESSFUL: **266 JVM debug/211 release,0 fallos/errores**,
19 locales nuevos compartidos (release repite, no sumar como distintos). Núcleo
UP-TO-DATE, no nueva ejecución. APK debug, test y release unsigned construidos,
compilación producto release real; lintRelease **0 errores/11 avisos** de versiones.
Cinco tests Android nuevos compilados y **ejecutados OK** tras autorización: Keystore/AtomicFile/readback,
tag corrupto/clave y archivo perdidos/no regeneración y Activity no exportada. Solo
alias/ruta efímeros UUID/noBackup; nunca almacén adulto real ni políticas/auth.
Runner focalizado OK(5tests),72.075s; duración total, no calibración de un KDF.

`python -B pruebas/verificar_apk_base.py` falló por nombre genérico Fake buscado como
subcadena en DEX, no descriptor. Inspección adicional de tablas de strings/class_defs
confirmó coincidencias de AndroidX Compose SemanticsNode.emitFakeNodes y metadata
Kotlin FakeKt; clases de tests locales ausentes, UI local de producto presente en
ambos APK, prototype ausente en release. Verificador genérico NO corregido en unidad.
Inspección manifest/clases release: AccesoAdultoActivity exported=false; sin
prototype/FGS, permiso USE_BIOMETRIC de MAIN intencional. Git diff --check correcto.

Fuentes oficiales HTTP200 consultadas: KeyGenParameterSpec.Builder,
BiometricPrompt.Builder y AtomicFile en developer.android.com/reference/android.

## APK y validación presencial realizada

**APK realmente instalado/validado = final local**, sin cambios de código posteriores:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`
SHA256 `41fdde66a55c113342fb8f3ac6a6ec2e5205ecebd5abc4450dafb3a94f53a205`.
Release unsigned: `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`
SHA256 `d97da1956c4846b77aa77a9e1c3ef8e183c7b93db7b7f6bf418b50775acd7d2a`.
No confundir con APK PROT04 instalado anteriormente (5eff03e5…). Releaseunsigned
no instalado/ensayado, pruebas manuales corresponden únicamente a debug con MAIN.

Se avisó y obtuvo confirmación específica para instalar APKdebug/test, cinco tests
aislados y alta/código/recuperación reales con dueño presente. Único A13 anunciado
SM_A135M, otros0, comprobado SM-A135M; Owner propio/NONE y testcapacidades1 OK
antes. Ambos installs Success. Todos comandos al mismo destino adb-s, serie solo
temporal privado, A56 excluido. No se interpretó «avanzar rápido» como autorización.

Manual separado de automático:
- Alta: dueño recibió «operación en curso/no acceso». Se indicó esperar y Actualizar
  una vez, sin repetir alta/borrar estado; después confirmó que pedía código. No se
  determinó en qué etapa apareció ni inferir corrupción/fallo de guardado. UI tiene
  rechazo acotado de operaciones concurrentes; no reintento automático de secreto.
- Entrada: dueño introdujo su código elegido y confirmó panel autenticado/controles
  sensibles deshabilitados; Inicio y regreso exigieron código de nuevo.
- Recuperación: dueño confirmó pregunta propia visible/respuesta solo habilitó cambio;
  eligió otro código, guardó y entró con él correctamente. No se probó código viejo
  en teléfono ni se provocaron fallos/espera. No se pidió ni observó ningún valor.
- Cierre: dueño pulsó Cerrar sesión/salió, confirmó nueva entrada pide código,
  emergencia visible sin autenticar y uso normal. No nuevo lockNow/llamada.
- Automático: final ADB NONE/sin servicio temporal, APKinstalado hash coincide;
  testcapacidades1 OK sin snapshot. No se consultó almacén adulto real/Keystorealias
  real/verificadores/pregunta/respuesta ni dumps UI/capturas/logs secretos.

Comandos realmente ejecutados (selección inicial adb devices-l, resto adb-s fijo):

```text
shell getprop ro.product.model
shell dumpsys device_policy                     # filtro Owner propio
shell dumpsys activity activities               # filtro mLockTaskModeState
shell dumpsys activity services dev.controlparental.child # servicio temporal propio
shell am instrument -w -e class dev.controlparental.child.prototype.CapacidadPrototipoTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
install -r -t android/app-child/build/outputs/apk/debug/app-child-debug.apk
install -r -t android/app-child/build/outputs/apk/androidTest/debug/app-child-debug-androidTest.apk
shell am instrument -w -e class dev.controlparental.child.local.AlmacenAdultoAndroidTest dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
shell am start -n dev.controlparental.child/.ui.LauncherActivity
shell pm path dev.controlparental.child
shell sha256sum <base.apk propio>
```

Capacidades1 OK dos veces (antes/final), almacén5 OK una vez, sin connectedglobal.
Ningún comando ADB/test físico fallido; UI ocupado aclarado con actualización, no
repetición de alta/recuperación automática. Sin nuevas restricciones/permisos
especiales/grants/Owner/PINAndroid/HOME/ADB/reboot/force-stop o redes cambiados.

Estado final: registro adulto configurado/código cambiado presencialmente se conserva,
sesión cerrada manualmente, NONE/sin snapshot ni servicio temporal/uso normal.
No prometer borrado de secretos reales ni deshacer configuración autorizada.
Pendientes: cancelación de prompt/respuesta/cambio y caducidad120s físicas, esperas/
reboot/readback incierto/rollback adversarial/latenciaKDF y sinredreal con conectividad
deshabilitada. Fakes cubren casos lógicos; no se forzaron reboot/muerte/radio/Doze.
Recuperación sin peticiones de red por implementación; no afirmar prueba de red
desconectada, no se consultó ni cambió conectividad. Bootstrap Android fue el gate
de alta por código; no prueba de identidad adulta si niño conoce PINAndroid.

Regla nueva posteriorC08: perder conexión requerida bloquea aunque tiempo restante,
reconectar no revive permiso sin NUEVAautorización. Supersede continuidad offline
del diseño; señal/plazo/uso estando offline pendientes, **no implementados aquí**.
Puerta adulta/recuperación offline no conceden uso. Reboot enforcement/ajustes/
desinstalación/endurecimiento pertenecen a unidades posteriores con salida segura
física validada. No H05/H06/producto final/QR/remoto/Push acreditados. Cierre commit
propio y detener, sin gitpush/otra tarea ni modificación de repos ajenos.
