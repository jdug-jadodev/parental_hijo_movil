# Evidencia H05 — recuperación local antes de endurecer

Fecha: 9 de octubre de 2026. Estado: EN_CURSO — primera unidad JVM.

## Alcance implementado

Núcleo sin Android para códigos impresos CP/1 §9 y §9.A. No está conectado a UI,
Device Owner, Wi-Fi, instalador, revinculación ni borrado. No abre ninguna app ni
modifica restricciones. No hay código maestro, identidad o código fijo en release.
Pruebas usan códigos ficticios exclusivamente bajo src/test y públicas/sobres de
fixtures congelados. No se modificó CP/1, esquema, fixtures ni servidor Render.

- Normalización ASCII: minusculas → mayúsculas, eliminación solo de espacio ASCII
  y guion. Exactamente 32 símbolos A–Z/2–7; no coerciones Unicode ni 0/1/8/9.
  Entrada de texto acotada a 128 caracteres; hash requiere pairId UUID v4.
- Hash exacto SHA256(UTF8("CPv1/RECOVERY\n" + pairId + "\n" + purpose + "\n" + código)).
  Comparación con MessageDigest.isEqual sobre hashes decodificados de 32 bytes.
- Consumo y contador/obligación de espera en la transacción H04. La sesión RAM
  se emite solo tras escritura, lectura posterior y validación comprobadas.
  No se almacenan códigos recibidos ni se escriben logs de intentos/secretos.
- Cinco fallos: 60 s; diez: 300 s. Contador acumulativo saturado en Int.MAX_VALUE,
  sin reset automático por éxito/recreación. Mientras hay espera no cuenta nuevos
  intentos ni gasta códigos. Después de alcanzar un umbral, cada nuevo fallo
  reimpone conservadoramente el margen de ese nivel.
- La espera pendiente persiste; nuevo controlador/boot o incertidumbre de reloj
  la reimpone completa. No se persiste un deadline de reloj civil. Al terminar la
  espera, el indicador se elimina en una escritura comprobada.
- Sesiones por propósito de máximo 5 minutos, solo RAM, vinculadas al boot/vínculo,
  cancelables y revisadas con estado/reloj/usuario desbloqueado en cada consulta.
  No sobreviven recreación, boot distinto, error de lectura ni cambio de vínculo.
- MAINTENANCE produce solo SesionMantenimiento para el motor, sin ALLOWED,
  sin autorizar boot y sin red requerida. Las herramientas reales serán H11.
- RECOVER_PARENT consume código y deja diagnóstico de recuperación pendiente;
  conserva pública/vínculo anterior y no reabre enrolamiento al recrear proceso.
  La revinculación/commit de clave nueva pertenece al flujo posterior autorizado.
- RETIRE consume su código y abre solo autorización local RAM: no tiene llamada
  wipeDevice/wipeData ni ejecuta borrado. Confirmaciones y retirada real son H11.
- Un error de almacenamiento/corrupción no emite sesión. Si el consumo llegó a
  disco pero falló el readback, el código sigue consumido sin devolver acceso;
  no se intenta revertirlo o fabricar una nueva credencial.

Emergencia no depende de este controlador; no se cambió su UI ni su adaptador.
No afirmar que haya sido validada físicamente con estos cambios.

## Pruebas locales ejecutadas

Desde android/, con JDK registrado en BUILD_ENV.md:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
```

Resultado final: BUILD SUCCESSFUL. Informes:

- Núcleo: 169 tests, UP-TO-DATE.
- Hijo debug: 124 tests ejecutados, incluidos **24 nuevos H05**.
- Total núcleo+debug: **293 tests, 0 fallos/errores/omitidos**.
- Hijo release: 117 tests ejecutados, 0 fallos/errores/omitidos (repite casos,
  no sumarlos como nuevos).
- APK debug/instrumentado y release sin firmar construidos; lint debug 0 errores
  y 10 avisos, lint vital release correcto.
- verificar_apk_base.py y verificar_apk_h01.py: OK. Sin fixtures/clases de tests
  en APK app; separación de traza H04 debug/release conservada.

Un intento intermedio del último test de integración con PolicyEngine no compiló
por referirse a modo en lugar de modoSolicitado. Corregido en el test, no cambió
la API del motor; comando completo repetido y resultados finales anteriores.

Casos: hash/normalización/contexto, consumo/replay, sesiones y expiración exacta,
esperas 5/10 intentos, proceso/boot nuevos y reloj incierto, almacenamiento fallido
y readback perdido, corrupción intacta, revinculación/retirada interrumpidas,
dos controladores concurrentes (una sola aceptación), y mantenimiento offline
con boot no autorizado que nunca produce ALLOWED ni autoriza el arranque.
Estos son tests JVM con archivo fake/reloj inyectado, NO recuperación física.

## Rutas modificadas

- android/app-child/src/main/java/dev/controlparental/child/recovery/CodigoRecuperacion.kt
- android/app-child/src/main/java/dev/controlparental/child/recovery/ModelosRecuperacion.kt
- android/app-child/src/main/java/dev/controlparental/child/recovery/RecoveryController.kt
- android/app-child/src/test/java/dev/controlparental/child/recovery/CodigoRecuperacionTest.kt
- android/app-child/src/test/java/dev/controlparental/child/recovery/RecoveryControllerTest.kt
- ESTADO.md y pruebas/EVIDENCIA_H05.md

## Pendientes

Desafío/respuesta OFFLINE con firma confiable, nonce de un solo uso y expiración;
la espera de códigos NO debe bloquear QR firmado válido. Pantalla de código y
sesión limitada con emergencia siempre accesible; limpieza del texto al salir.
Puertos Android y pruebas instrumentadas sin tocar estado real de producción,
identidades efímeras solo de laboratorio. Producción espera vínculo H08.
Revisar integración/cancelación al reemplazar vínculo y singleton por proceso.
H-REC-01/02/03 físicos no aprobados; H05 NO cerrada. No avanzar a endurecimiento.

No se usó ADB, instaló, reinició ni cambió ningún teléfono en esta unidad.
El A13 conserva APK H04 con traza debug; A56 intacto. Servidor externo no consultado.

## Segunda unidad — desafío y respuesta OFFLINE firmados

RecoveryController incorpora un único desafío en RAM, nonce aleatorio de 32 bytes,
pareja/boot/acción y vigencia de cinco minutos. Solo MAINTENANCE o RETIRE; no ALLOW
offline ni RECOVER_PARENT por QR, conforme a CP/1. Emitir desafío devuelve
DESAFIO_CREADO, no una autorización de sesión. Firmante inyectado mediante el puerto
existente: su pública debe coincidir con childPublicKeyB64 del vínculo. Por defecto
no hay firmante y no se fabrica identidad; todavía no está conectado a Keystore/UI.

La respuesta se verifica con la pública del padre del vínculo LOCAL, rol PARENT y
propósito OFFLINE, antes de comparar tipo, pareja, boot, acción y nonce vigentes.
No se admiten claves aportadas por la respuesta ni un desafío del hijo como respuesta.
Revisar contexto/expiración de nuevo después de la operación criptográfica evita
conceder sesión si pasó el límite o se bloqueó Android durante la comprobación.

El desafío se consume en RAM antes del commit/readback que precede a la sesión.
Crash/reinicio/cancelación/nuevo desafío lo pierden; respuesta repetida no prolonga
una sesión. Fallo de persistencia no devuelve el desafío consumido ni emite sesión.
Las respuestas inválidas no afectan el contador de códigos impresos ni gastan su
consumo. Un QR válido funciona durante la espera de códigos, sin borrar esa espera.
MAINTENANCE por QR no gasta código impreso ni autoriza boot/política; RETIRE sigue
siendo una autorización RAM limitada y NO ejecuta borrado. Cambio del vínculo o boot
invalida el desafío/sesión. No persistir nonce, sobre ni historial de desafíos.
Entrada de respuesta acotada a MAX_FRAME_BYTES antes de copiar/verificar.

24 tests nuevos con identidades P-256 efímeras en memoria JVM, sin privadas en archivo:
firma confiable del desafío, respuesta válida y replay, espera independiente,
RETIRE sin mantenimiento/borrado, clave/rol/pair/boot/acción/nonce incorrectos,
caducidad exacta y retroceso de reloj, reemplazo de desafío, cancelación/proceso,
Android bloqueado, cambio de pública, fallo de persistencia, identidad ausente/
equivocada/inaccesible, nonce inválido/repetido, entrada vacía/excesiva/corrupta,
sesión expirada y concurrencia, caducidad/bloqueo durante criptografía, firma antes
del desbloqueo no invocada, corrupción intacta y cambio de boot duradero.

Comando Gradle completo anterior ejecutado y repetido tras añadir los casos de carrera:
BUILD SUCCESSFUL. Informes finales: núcleo 169 tests UP-TO-DATE; hijo debug **148**
ejecutados, total núcleo+debug **317/0 fallos/0 errores/0 omitidos**; hijo release **141**
ejecutados, sin fallos/errores/omitidos (repite casos). APK debug/instrumentado y release
sin firmar construidos o UP-TO-DATE; lint 0 errores/10 avisos. Inspectores base/H01 OK.

Rutas: recovery/ModelosRecuperacion.kt, recovery/RecoveryController.kt,
tests recovery/OfflineRecoveryTest.kt, ESTADO.md y esta evidencia. Contrato CP/1,
esquema y fixtures congelados sin cambios; servidor Render fuera de alcance.

NO se ha generado/escaneado un QR en Android ni probado esta recuperación en A13.
Faltan pantalla/puertos Android, singleton por proceso, integración Keystore con
vínculo confiable y pruebas instrumentadas de recuperación aisladas de producción.
No se cambia la espera de códigos desde la vía OFFLINE ni se modifica emergencia.
No se usó ADB ni se instaló, reinició, endureció o borró ningún equipo en esta unidad.
H05 sigue EN_CURSO; H06/puertas siguen pendientes.

## Tercera unidad — presentación y puertos Android seguros

- Pantalla RecoveryScreen accesible desde launcher; emergencia permanece fuera del
  formulario, sin depender de envío/código/espera. No se abre Ajustes ni otra app.
- Formulario por propósito, texto en RAM con remember (NO rememberSaveable/Bundle),
  máscara de contraseña y autocorrección deshabilitada. Texto se quita al enviar,
  cambiar propósito o cancelar; pausa desmonta la pantalla y descarta callbacks.
  No se promete borrado físico de memoria ni protección frente a teclado malicioso.
- FLAG_SECURE se activa en la ventana del launcher al abrir recuperación y se
  mantiene durante esa instancia. No afecta keyguard/marcador del sistema; su
  comportamiento y emergencia presencial en A13 aún requieren validación.
- Trabajo en EjecutorEstado fuera del hilo principal; contador atómico invalida
  respuestas tardías/operaciones aún no iniciadas al salir o pausar. Cancelación
  de sesión se serializa detrás de cualquier transacción en curso. Si una operación
  ya consumió código, cancelarla no restaura ese código ni muestra autorización vieja.
- RecuperacionAndroid mantiene un controlador por proceso, sin crear/importar claves.
  FuenteRecuperacionAndroid comprueba Owner, UserManager desbloqueado y continuidad
  BOOT_COUNT/elapsed contra estado DP; no usa privadas ni concede acceso por error.
- Proyección de UI sin códigos/hashes/identidad; consulta periódica de sesión/espera
  cada segundo mientras se muestra, sin reiniciar duración. Expiración elimina
  autorización mostrada; consulta de espera no elimina marcador durable ni cuenta
  intentos. No habilita apps normales ni cambia política/contador CP/1.

**Consumo deshabilitado en interfaz Android:** aún faltan herramientas H11 y vínculo
H08. No permitir quemar una vía real de recuperación sin poder ejecutar su función.
El botón/puerto de envío está cerrado por defecto, incluso con vínculo, y la pantalla
lo explica. Núcleos se prueban aisladamente; no hay flag de preferencia, intent o
botón de bypass para habilitarlo. En el A13 actual debe mostrar SIN_VINCULO.
No se afirma recuperación funcional por construir esta presentación. QR/cámara,
alias Keystore confiable y acciones operativas siguen pendientes; H06 bloqueada.

Pruebas locales: mismo comando completo Gradle de esta evidencia, ejecutado y
repetido tras corregir aviso de plurales del texto de segundos: BUILD SUCCESSFUL.
Núcleo 169 UP-TO-DATE; hijo debug 152 ejecutados; total **321/0 fallos/0 errores/0
omitidos**, cuatro tests JVM nuevos. Hijo release 145 ejecutados, sin fallos/errores/
omitidos (repite casos). APK debug/test/release sin firmar construidos; lint final
0 errores/10 avisos, vital release correcto. Inspectores base/H01 repetidos OK.

Tres tests Android nuevos COMPILADOS NO EJECUTADOS, total previsto 15 con los 12
anteriores: lectura de capacidades reales, consumo duradero y recreación de sesión,
espera duradera recreada. Solo subdirectorio UUID efímero en DP del destino y sus
tres nombres AtomicFile, con limpieza acotada. Públicos de fixture y código ficticio
solo en APK test; nunca tocar control-state.json de producción ni Owner/credencial.
El test de espera usa reloj inyectado, no cambia la hora del teléfono ni demuestra
60 segundos de suspensión real. No hay test automatizado Compose de UI en esta unidad.

Rutas: recovery/EstadoPantallaRecuperacion.kt, FuenteRecuperacionAndroid.kt,
RecuperacionAndroid.kt y ampliación RecoveryController.kt; ui/RecoveryScreen.kt,
LauncherActivity.kt y strings.xml; tests EstadoPantallaRecuperacionTest.kt,
RecoveryControllerTest.kt y androidTest/RecuperacionArchivoAndroidTest.kt;
ESTADO.md y esta evidencia. Fuente primaria FLAG_SECURE:
https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE

No ADB ni instalación/reinicio/endurecimiento/borrado en esta unidad. Próximo paso:
coordinar actualizar app/test SOLO A13 y ejecutar 15 tests con Owner/credencial
exigidos; después confirmar presentación SIN_VINCULO, cancelar y comprobar ruta
de emergencia SIN marcar/llamar. No habilitar consumo/inyectar vínculo en producción.

## Instalación y pruebas Android autorizadas — 9 de octubre de 2026

El propietario autorizó actualizar app/test SOLO A13 y ejecutar 15 tests sin
reiniciar/endurecer/borrar. ADB anunció un único SM_A135M; se comprobó SM-A135M
antes de cada operación y todo se dirigió al mismo destino `adb -s`, sin guardar
serie. Owner previo esperado, usuario 0, DeviceOwner/Affiliated. A56 excluido.

Ambas actualizaciones `install -r -t` devolvieron Success.
SHA-256 de APK instalados:

- App: `19b8e074d3393477cacbff525c5fde59ef6a334c9accbf3508265eb5f59ed410`.
- Tests: `d9c0ce2a04e451b4bef223409c015bee3f200625d365e5e505c6d18415108c24`.

```powershell
adb -s $serieA13 shell am instrument -w -r -e exigirDeviceOwner true -e exigirCredencialAndroid true dev.controlparental.child.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: **OK (15 tests), Time: 3.581 s**. Los tres nuevos H05 y los doce anteriores
aprobados. Se comprobó consumo/espera persistidos en archivos DP efímeros aislados,
recreación del controlador y fuente Android comparada con APIs públicas. Espera
usa reloj inyectado: no prueba reposo/reinicio real durante esos 60 segundos.
No se consumieron códigos de producción ni se accedió al control-state.json real
desde los tests H05. No se inyectó vínculo en producción ni se habilitó consumo UI.

No se abrió marcador, llamó lockNow, reinició, borró, retiró administración o
endureció el A13. No comandos dirigidos al A56. Sin nueva construcción/JVM en
esta unidad documental. Presentación SIN_VINCULO, cancelación, FLAG_SECURE y
regresión presencial de emergencia de este APK siguen pendientes. H05 NO cerrada;
QR/identidad operativos y herramientas reales no se consideran implementados.

## Revisión presencial de presentación y emergencia

El propietario respondió «correcto» a los pasos: abrir Recuperación local con el
adulto, comprobar SIN_VINCULO y consumo de códigos deshabilitado, cancelar/volver,
abrir y salir del marcador de emergencia SIN marcar/llamar, desbloquear y volver
a nuestra app. Confirmación presencial comunicada, no captura automatizada.
No se pidieron ni comunicaron códigos o credenciales. No se infiere comprobación
de FLAG_SECURE por esta respuesta: no se solicitó intentar captura de pantalla.

Presentación/cancelación y ruta básica de emergencia del APK H05 inicial confirmadas
en A13 sin quiosco. No valida recuperación con código real, QR, herramientas,
borrado, retorno bajo quiosco o variantes SIM. H05 sigue EN_CURSO y H06 bloqueada.
En este registro solo se actualizó documentación; no ADB ni nuevas pruebas.

## Cuarta unidad — OFFLINE con Android Keystore, preparado

Se añadió OfflineKeystoreAndroidTest con **ocho tests instrumentados**, compilados
NO EJECUTADOS. No cambió la app, interfaz, consumo UI o contrato; solo APK de pruebas.
Total previsto de instrumentación: **23 tests** (15 anteriores + ocho nuevos).

Cada test exige Android desbloqueado, crea dos alias UUID únicos en Android Keystore
para identidad del hijo y firma parental SIMULADA exclusivamente en laboratorio.
Las privadas se comprueban no exportables y jamás se serializan. No es desarrollo
de la app padre ni requiere su repo. Al terminar elimina solo esos dos alias y
comprueba ausencia. El caso de identidad perdida elimina únicamente su propio
alias temporal del hijo y exige error sin crear reemplazo.

Archivo AtomicFile en subdirectorio DP UUID efímero con públicas/hashes ficticios,
sin control-state.json de producción, códigos reales ni política real. Limpieza de
tres nombres conocidos y directorio vacío, no namespaces o borrado recursivo.
No llama wipe/lockNow, no instala restricciones ni cambia Owner/credencial/red.

Casos: desafío CHILD firmado por Keystore, respuesta parental temporal confiable,
replay/recreación/cancelación, espera impresa no bloquea OFFLINE ni desaparece,
acción/nonce/pair/boot/clave incorrectos, RETIRE solo autorización RAM sin borrado,
caducidad exacta de desafío/sesión, identidad Keystore ausente sin reemplazo,
contexto bloqueado simulado y boot duradero distinto. Reloj/contexto inyectados para
límites: no prueban cinco minutos reales de reposo ni bloqueo previo al PIN.
Las firmas y el archivo sí serán APIs reales de Android cuando se ejecute en A13.

Mismo comando completo Gradle: BUILD SUCCESSFUL; tests JVM debug/release/núcleo
UP-TO-DATE (321 resultados núcleo+debug y 145 release anteriores, no nueva ejecución).
APK test recompilado, app debug y release sin firmar UP-TO-DATE; lint correcto con
10 avisos anteriores. Ambos inspectores APK OK; clase de laboratorio fuera del APK app.

Rutas: androidTest/recovery/OfflineKeystoreAndroidTest.kt, ESTADO.md y esta evidencia.
NO ADB, instalación/reinicio/endurecimiento/borrado en esta unidad. Próxima coordinación:
actualizar SOLO APK test en A13 y ejecutar 23 tests exigiendo Owner/credencial. No
reinstalar app ni habilitar consumo UI, QR real o identidades de producción.

## OFFLINE Keystore ejecutado en A13 — 9 de octubre de 2026

Autorización del propietario: «ok, hazlo» para actualizar SOLO APK test y ejecutar
23 tests. Se seleccionó un único A13 anunciado SM_A135M y se comprobó SM-A135M
antes de cada operación dirigida mediante `adb -s`, sin registrar serie. Owner
esperado comprobado: dev.controlparental.child/.admin.ChildAdminReceiver, usuario
0, DeviceOwner/Affiliated. Ningún comando dirigido al A56.

Actualización `install -r -t` del APK test: **Success**. SHA-256 instalado:
`e073ec0da03f8b0de6b137834948c9c43c773d23f87ea363bea0c446d5a204ba`.
La app NO se reinstaló: conserva el APK de presentación H05 registrado arriba.

Runner con `exigirDeviceOwner=true` y `exigirCredencialAndroid=true`, mismo comando
de la instalación inicial: **OK (23 tests)**. Quince anteriores y ocho OFFLINE
nuevos aprobados. Keystore y AtomicFile reales, privadas no exportables, alias y
archivos temporales con limpieza acotada comprobados por los tests. Firma parental
solo simulada mediante identidad efímera, no vínculo ni códigos de producción.

Reloj/contexto inyectados: caducidad no demuestra cinco minutos de suspensión real
ni uso antes del PIN. RETIRE solo cambia el estado de laboratorio y autoriza RAM;
no ejecuta borrado ni retirada de Owner. No se habilitó consumo UI, creó vínculo
real, reinició, endureció o borró el teléfono. Sin nueva construcción o ejecución
JVM en este registro. H05 sigue EN_CURSO: QR/cámara, identidad de producción y
herramientas autorizadas operativas pendientes; H06 continúa bloqueada.

Rutas de este registro: ESTADO.md, pruebas/EVIDENCIA_H05.md y
pruebas/FICHA_DISPOSITIVO.md. Próxima unidad: presentación/captura QR e integración
con identidad confiable, sin fabricar vínculo; nueva prueba física requiere coordinación.
