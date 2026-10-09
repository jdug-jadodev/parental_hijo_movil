# SPEC-H — Aplicación administradora del hijo

**Objetivo:** convertir un Android autorizado en un dispositivo administrado que aplique localmente bloqueo tras reinicio, ausencia de internet/canal de control y órdenes temporales del padre; impedir su desinstalación normal y mantener emergencias accesibles.

Candidato inicial: Samsung Galaxy A13, variante y versión por inventariar. **No está probado ni certificado todavía.** Este documento, el protocolo compartido CP/1 y la matriz de aceptación son obligatorios. Las restricciones proceden de APIs oficiales [S01–S10, S17–S22]; los algoritmos, estados y umbrales siguientes son decisiones del proyecto.

## H1. Condiciones previas y alcance

La app debe ser Device Owner de un teléfono preparado por su propietario. Un administrador tradicional o un permiso de accesibilidad no cumplen el requisito. El teléfono puede necesitar copia de seguridad y restablecimiento para aprovisionarlo. La instalación inicial es presencial. No se sustituye otro administrador ni se eluden restricciones de un teléfono ajeno.

Base del proyecto: Android 12/API 31 o superior; compile/target API 36. La distribución v1 es privada mediante APK firmado. La app permanece visible como «Control parental — dispositivo administrado». No hay root, exploits, servicio de accesibilidad, superposición engañosa, capturas, micrófono, GPS ni lectura de conversaciones.

Un teléfono sin capacidad real de telefonía de emergencia no pasa la compatibilidad del producto. No basta con que pueda instalar el APK. Tampoco se certifica otro fabricante por pasar los tests del A13.

## H2. Componentes y límites de responsabilidad

```text
app-child/
  admin/
    ChildAdminReceiver.kt
    ProvisioningActivity.kt
    PolicyComplianceActivity.kt
    PolicyEnforcer.kt
    DeviceCapabilities.kt
  boot/
    BootReceiver.kt
    BootContextRepository.kt
  domain/
    PolicyEngine.kt          # Kotlin puro: estados, causas, tiempos
    CommandProcessor.kt     # verifica, acepta, persiste, aplica, confirma
  data/
    ChildStateStore.kt      # AtomicFile, almacenamiento Device Protected
  network/
    ControlService.kt      # foreground service visible
    NetworkObserver.kt
    ChildRelayConnection.kt
  security/
    ChildKeyStore.kt
    PairingController.kt
    RecoveryController.kt
  ui/
    LauncherActivity.kt
    LockedScreen.kt
    AllowedAppsScreen.kt
    PairingScreen.kt
    MaintenanceScreen.kt
  emergency/
    EmergencyAccess.kt
    CallSafetyObserver.kt
```

La lógica de red no llama directamente a `startLockTask`. Envía eventos al procesador serializado. Solo `PolicyEnforcer` usa DevicePolicyManager. Solo `ChildStateStore` escribe el estado. Todas las decisiones de «puede usar» pasan por una misma `PolicyEngine`; no hay una segunda lógica en la interfaz.

`core-protocol` valida bytes, firmas y modelos compartidos. `app-child` añade comprobaciones de dispositivo, arranque, secuencia, nonce y enforcement.

## H3. Requisitos funcionales

| ID | Requisito obligatorio |
|---|---|
| H-F01 | Confirmar `isDeviceOwnerApp(packageName)` y configuración antes de mostrar protección activa |
| H-F02 | Ser launcher HOME persistente y conservar Lock Task durante el uso administrado |
| H-F03 | Cada arranque real queda bloqueado hasta autorización nueva aplicable a ese bootId |
| H-F04 | Bloquear localmente cuando se pierde red validada por más de 3 segundos |
| H-F05 | Bloquear al superar 45 segundos sin PONG de aplicación válido del relay |
| H-F06 | Aplicar las cuatro acciones y duraciones de CP/1 usando reloj monotónico |
| H-F07 | Verificar firma, pareja, rol, arranque, sesión, nonce y secuencia antes de aceptar |
| H-F08 | Conservar política/contador/temporizador de forma atómica sin historial |
| H-F09 | Impedir controles normales de desinstalación, borrado de datos y modificación del controlador |
| H-F10 | Mantener emergencia del sistema accesible sin internet ni autorización parental |
| H-F11 | Permitir mantenimiento y retirada solamente con autorización presencial válida |
| H-F12 | No informar APPLIED sin comprobar la aplicación efectiva de restricciones |
| H-F13 | Recuperar proceso/conexión sin reiniciar una duración ni conceder acceso por defecto |
| H-F14 | No recolectar contenido, ubicación, contactos ni uso de aplicaciones |
| H-F15 | Instalar actualizaciones propias firmadas por el mismo desarrollador sin borrar vínculo/estado |

## H4. Manifest, permisos y aprovisionamiento

### H4.1 Declaraciones requeridas

- `ChildAdminReceiver`: exportado, protegido con `android.permission.BIND_DEVICE_ADMIN`, metadata `android.app.device_admin` y acción `DEVICE_ADMIN_ENABLED`. El XML de políticas declara `force-lock` y `wipe-data` para las funciones de emergencia/retirada. No se ejecuta un borrado al recibir una orden ordinaria.
- `LauncherActivity`: exportada para HOME, categorías MAIN/HOME/DEFAULT y entrada LAUNCHER separada; `lockTaskMode="if_whitelisted"`. La pantalla inicial siempre parte de bloqueado, no de permitido.
- `BootReceiver`: `directBootAware=true`, `exported=false`; escucha `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`. Son eventos del sistema, no una API de control remoto. La recuperación adicional de UI responde a `USER_UNLOCKED` mediante el mecanismo apropiado y registrado solo cuando haga falta.
- `ControlService`: `exported=false`, `directBootAware=true`. En Android 14+ declarar tipo `systemExempted` y permiso específico, después de comprobar elegibilidad real como Device Owner [S05, S06].
- Los componentes que acceden a almacenamiento antes de desbloqueo usan exclusivamente contexto Device Protected. Revisar también inicializadores automáticos de bibliotecas: ninguno puede leer datos Credential Encrypted durante Direct Boot.

Permisos básicos: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`; añadir `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` para API 34+, `POST_NOTIFICATIONS` para aviso visible en API 33+, `CAMERA` solo para vinculación/recuperación QR, `ACCESS_WIFI_STATE` y `CHANGE_WIFI_STATE` para mantenimiento de red. `READ_PHONE_STATE` se limita a observar si hay una llamada en curso cuando sea necesario para no interrumpir la interfaz telefónica; no se solicitan números ni registros. Solicitar los permisos de ejecución correspondientes durante instalación presencial y explicar cada uno.

No declarar `CALL_PHONE`, `READ_CALL_LOG`, `READ_CONTACTS`, `RECORD_AUDIO`, ubicación, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES` o `SCHEDULE_EXACT_ALARM` por defecto. Usar consultas de paquetes limitadas a actividades MAIN/LAUNCHER y los componentes de sistema necesarios. No añadir permisos privilegiados que una app normal no puede obtener.

### H4.2 Aprovisionamiento

La ruta de laboratorio usa ADB sobre equipo limpio sin cuentas y la propia app como DPC [S17]. La guía `INSTALACION_A13.md` contiene pasos y diferencias debug/release. La app no ejecuta `dpm set-device-owner` sobre sí misma.

Si se automatiza instalación desde Setup Wizard mediante QR, implementar actividades para `ACTION_GET_PROVISIONING_MODE` y `ACTION_ADMIN_POLICY_COMPLIANCE`, comprobar que el resultado es FULLY_MANAGED y el flujo termina de verdad. No usar únicamente el antiguo `ACTION_PROVISION_MANAGED_DEVICE` para Android 12+ [S18, S19]. La ruta manual de laboratorio es suficiente para el primer prototipo; el aprovisionamiento reproducible debe quedar documentado antes de otra instalación.

## H5. Restricciones y aplicación del quiosco

Políticas base elegidas, después de probar recuperación y emergencias:

| Finalidad | API/restricción candidata | Comprobación |
|---|---|---|
| Propietario | `isDeviceOwnerApp` | Debe ser true, no solo admin activo |
| HOME controlado | `addPersistentPreferredActivity` para nuestro launcher | Pulsar Home y comprobar destino |
| Quiosco real | `setLockTaskPackages` + `startLockTask` | Estado `LOCK_TASK_MODE_LOCKED`, no `PINNED` |
| Interfaz del sistema | `setLockTaskFeatures` | Conservar KEYGUARD y HOME seguro; restringir otras salidas |
| Desinstalación | `setUninstallBlocked` para el controlador | Verificar y probar interfaz de desinstalar |
| Control sobre la app | `setUserControlDisabledPackages` para el controlador | Detener/borrar datos no accesibles por vía normal |
| Ajustes peligrosos | `DISALLOW_APPS_CONTROL`, `DISALLOW_DEBUGGING_FEATURES`, `DISALLOW_SAFE_BOOT`, `DISALLOW_FACTORY_RESET` | Pruebas normales, no promesa sobre recovery |
| Cuentas y usuarios alternativos | `DISALLOW_ADD_USER`, `DISALLOW_USER_SWITCH`, `DISALLOW_MODIFY_ACCOUNTS` | No cambiar a otro usuario/espacio desde interfaces permitidas |
| Instalación no autorizada | `DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY`, instalación de apps restringida en producción | Prueba con instalador y enlaces de descarga |
| Cambio de reloj | `DISALLOW_CONFIG_DATE_TIME`; hora automática cuando la API corresponda | La duración no depende del reloj de pared |
| Ventanas ajenas | `DISALLOW_CREATE_WINDOWS` durante Lock Task | Pruebas de overlays sin usarlos nosotros |
| Llamadas ordinarias al bloquear | `DISALLOW_OUTGOING_CALLS` | La restricción deja permitidas emergencias; comprobar acceso real |

Las descripciones de esta tabla son un mapa de implementación, no una licencia para deshabilitar todos los paquetes del sistema. Las APIs y restricciones deben comprobarse en el SDK y al ejecutarse [S03, S04]. Nunca suspender indiscriminadamente Telecom, SystemUI, SIM, marcador de emergencia, teclado o conectividad.

Lista base bloqueada: solo paquete del hijo; servicios/actividades imprescindibles del sistema continúan por sus mecanismos oficiales. No añadir todo Ajustes ni todo el marcador normal «para que emergencia funcione». Si un fabricante necesita una excepción, documentar componente, motivo, alcance y pruebas contra escapes antes de declararlo compatible.

Lista permitida: paquete del hijo más los paquetes de aplicaciones aprobadas presencialmente. Mantener el launcher propio y Lock Task. Retirar un paquete de la lista al bloquear puede finalizar sus tareas administradas; comprobar que no queda un task accesible [S02, S03]. No afirmar que una lista de paquetes aísla automáticamente toda actividad interna: probar enlaces, ventanas, intents y componentes. Evaluar `LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK` en la base API soportada y conservar el conjunto mínimo de flags que supera emergencias.

En el modo permitido no debe existir un puente a Ajustes, otra pantalla HOME, instaladores, VPN o cambio de usuarios. Instalar previamente las apps aprobadas, no implementar una tienda en v1. Al actualizar Android o una app permitida se repiten las pruebas de salida relevantes.

### H5.1 Orden de enforcement

Para cerrar: persistir intención aceptada; reducir inmediatamente permisos/lista; llevar a pantalla controlada cuando sea seguro; comprobar resultado; guardar resultado; enviar ACK. Para abrir: persistir primero; verificar estado de red, reloj y contexto; ampliar solo los paquetes aprobados; comprobar; confirmar. El estado de política solicitado y el estado realmente permitido son campos distintos.

En Android 14+ observar también resultados de políticas cuando los exponga `PolicyUpdateReceiver`; una llamada sin excepción no garantiza éxito. En todas las versiones usar lectura de políticas y estado de Lock Task, más validación de UI en pruebas. Si falta evidencia, devolver `ACCEPTED_PENDING_ENFORCEMENT` o `ENFORCEMENT_FAILED` [S03].

No llamar `startLockTask` sobre el keyguard activo de manera que encierre al usuario detrás de su PIN. Antes del primer desbloqueo, conservar restricciones y mostrar estado de arranque; entrar en quiosco cuando el sistema permita hacerlo [S02]. El PIN de Android/SIM no es la clave parental y no se elimina desde nuestra app.

## H6. Máquina de estados y reloj

La función pura recomendada:

```kotlin
fun evaluate(input: EvaluationInput): AccessDecision
```

Entradas: integridad, aprovisionamiento, disponibilidad de credenciales de usuario, bootId actual/autorizado, fase de mantenimiento, política/duración/startElapsed, red validada y tiempo de pérdida, último PONG, tiempo monotónico actual. Salida: modo solicitado efectivo, razón y tiempo restante. No hace llamadas Android ni red.

Requisitos invariables:

1. La rama inicial es bloqueado. Ningún `null`, error de lectura o campo desconocido equivale a permitir.
2. Nueva política reemplaza a la anterior. Repetición exacta no modifica `startElapsed`.
3. Un `LOCK_FOR` que vence no habilita uso sin red/canal o en otro arranque.
4. Un `ALLOW_FOR` que vence bloquea incluso si el socket está conectado y el padre desconectado.
5. Reiniciar crea otra autorización de arranque; nunca continúa una sesión permitida anterior.
6. El reloj de pared se usa para texto de interfaz, no para decidir expiración. `elapsedRealtime()` incluye suspensión y reinicia con el dispositivo [S20].
7. Un reinicio del proceso en el mismo arranque conserva el inicio del temporizador y las secuencias. Se considera canal no sano hasta nueva autenticación/latido.
8. Si no puede identificarse de manera fiable que es el mismo arranque, se bloquea y exige nueva autorización.

Guardar el contexto de arranque de forma idempotente. Se puede contrastar `Settings.Global.BOOT_COUNT` con el registro local y el contador monotónico; errores de acceso o valores incoherentes provocan contexto nuevo bloqueado. El contador de secuencia del padre no vuelve a cero por ese cambio.

Temporizador: programar la próxima transición con el mecanismo local apropiado y reevaluar en recepción de comandos, cambios de red, heartbeat, inicio/reinicio de servicio y antes de interacción en launcher. No usar `WorkManager` periódico para la garantía de segundos. No mantener un wake lock permanente ni encender pantalla de forma continua para aparentar tiempo real. Las restricciones de suspensión siguen requiriendo las pruebas G4.

## H7. Ejecución y red

`ControlService` es un servicio en primer plano visible, iniciado solo cuando la app está aprovisionada como propietaria y cumple las condiciones del tipo [S05, S06]. Promoverlo a foreground dentro del plazo del sistema; no hacer una conexión bloqueante antes de publicar su notificación. En API 31–33 no pasar flags de tipos que no existen; en API 34+ usar `systemExempted` si la comprobación de Device Owner pasó. No etiquetar el servicio como reproducción multimedia, `remoteMessaging` ni `dataSync` para mantenerlo artificialmente.

`START_STICKY` y la recepción de boot ayudan a recuperar el servicio; **no garantizan ejecución permanente**. Si Android mata el proceso mientras otra app permitida estaba abierta, la lista de quiosco previa puede persistir hasta que el controlador actúe. Por eso la prueba de muerte de proceso con otra app en primer plano es bloqueante para certificar este diseño. No ocultar esta ventana con una afirmación de «fail closed» absoluta.

Un solo `NetworkCallback` observa la red predeterminada; `onAvailable` no basta: hay que revisar `NET_CAPABILITY_INTERNET` y `NET_CAPABILITY_VALIDATED` con `onCapabilitiesChanged`. Un cambio Wi-Fi a datos se evalúa con la red actual, no con un `onLost` antiguo [S09]. Un solo socket autenticado y una coroutine de reconexión. Cancelar el intento anterior al reemplazarlo. Mantener `lastPongElapsed` solo para la conexión actual.

Una pérdida de validación que dura más de 3 s bloquea. Aunque Android siga validando internet, 45 s sin PONG fresco bloquean. Al conectar de nuevo, obtener un PONG válido antes de considerar el canal saludable. Un fallo de certificado jamás se resuelve desactivando TLS.

Probar Doze forzado y suspensión natural, ahorro de energía y ajustes específicos del A13. Una eventual exclusión de optimización de batería se solicita de forma visible y documentada durante instalación; no se presume que Device Owner elimina todos los comportamientos del fabricante [S08]. No instalar mecanismos ocultos de autoinicio.

## H8. Persistencia y aplicación idempotente

Un único registro atómico, versión 1, contiene:

```text
schemaVersion
pairId, parentPublicKeyB64, childPublicKeyB64, relayOrigin
recoveryHashes y recoveryConsumed por propósito
recoveryFailureCount y recuperación de espera pendiente
bootContext: systemBootCount, bootId, authorizedBootId
policy: action, durationSec, acceptedAtElapsed, policyBootId
lastCommand: seq, commandId, payloadSha256B64, signedEnvelope, stage, result
approvedPackages: lista aprobada presencialmente
provisioningStage, lastEnforcementErrorCode
```

`lastCommand` es el único registro de orden, no un historial. Conservar el sobre firmado de esa última orden permite verificar su firma y consistencia con los campos derivados al cargar; verificarlo no autoriza ejecutarlo en otro arranque. Usar además un checksum SHA-256 de los bytes del registro para detectar corrupción accidental de campos locales: no tratar ese checksum como defensa frente a root. Un estado imposible o incoherente queda bloqueado. Para el checksum, envolver el registro serializado en `{formatVersion:1, payloadB64, sha256B64}`: SHA-256 se calcula sobre los bytes decodificados de payloadB64, no sobre el contenedor que incluye el checksum. Validar tamaño y esquema después de verificarlo. Este contenedor local no es un mensaje CP/1. Las claves privadas no están ahí. No persistir firmas o payloads anteriores salvo el último necesario para idempotencia. Los secretos Wi-Fi introducidos se entregan al sistema, no se guardan en este archivo ni se envían al padre.

Etapas locales: `ACCEPTED` -> `ENFORCING` -> `APPLIED` o `FAILED`. El paso de recepción valida todo antes de escribir. Usar un actor/canal de comandos o Mutex que serialice escritura y aplicación. Nunca aceptar dos órdenes en paralelo y terminar aplicando la más antigua al final.

Si falla almacenamiento antes de aceptación, no ampliar permisos. Si el proceso muere después de persistir y antes de aplicar, al reiniciar se reanuda esa última política con su inicio original, respetando arranque y red. Si vuelve en un arranque diferente, conservar contador y registro pero mantener bloqueo de arranque; un ACK idempotente antiguo no debe describir permiso vigente.

Antes del primer desbloqueo guardar/leer solo el contexto mínimo público necesario en Device Protected. La identidad privada del hijo se usa cuando Keystore esté disponible; no exportarla a un archivo «para que Direct Boot funcione». Sin identidad disponible no se autentica el socket, pero se conserva el bloqueo y las emergencias [S07, S10, S21].

## H9. Emergencias: puerta obligatoria, no una maqueta

**Requisito de aceptación:** desde reinicio, bloqueo parental, pérdida de red y recuperación, el usuario puede acceder a la interfaz de emergencia del sistema sin clave parental. No se reemplaza por VoIP ni por una llamada al servidor.

Ruta portable elegida para el primer prototipo: mantener el keyguard real habilitado y `LOCK_TASK_FEATURE_KEYGUARD`; conservar la credencial de Android. El acceso de emergencia se realiza desde el keyguard del sistema. Nuestra pantalla indica «Emergencia: usar llamada de emergencia de la pantalla de bloqueo» y ofrece «Ir a pantalla de bloqueo», que solicita `DevicePolicyManager.lockNow()` de forma explícita. En modelos donde apaga la pantalla, explicar que se enciende con el botón lateral y se usa la opción del sistema. Este flujo puede necesitar más de una pulsación; no afirmar que es un marcador embebido.

La app no ejecuta `resetPassword`, no elimina el PIN de Android y no pide el PIN del padre para emergencia. El niño puede conocer su PIN ordinario de Android: introducirlo no elimina la política parental.

No usar `TelecomManager.createLaunchEmergencyDialerIntent` como si fuese una API pública: en la fuente de Android está marcada `@SystemApi/@hide` [S16]. No añadir reflexión, strings de intents privados o nombres fijos de paquetes Samsung como solución genérica. Una ruta directa adicional solo se aprueba mediante API pública documentada y validación por modelo.

`DISALLOW_OUTGOING_CALLS` conserva la posibilidad de emergencia por política [S04], pero la interfaz se valida en el A13. No basta con un botón cuyo clic no llegue al marcador. Nunca hacer llamadas de prueba a números reales de emergencia sin coordinación formal con el servicio correspondiente. La verificación ordinaria abre y sale del marcador **sin llamar**; el curso completo de una llamada se simula en entorno autorizado o se coordina formalmente. No se marca probado lo que no se probó.

Las nuevas órdenes no deben finalizar llamadas ni retirar componentes telefónicos del sistema. `CallSafetyObserver` usa únicamente estado de llamada en curso cuando el fabricante lo requiera; aplaza tomar el primer plano hasta que termine, sin suspender telefonía. Las devoluciones de llamada e interfaces imprescindibles de seguridad del sistema tienen prioridad sobre la regla «ninguna otra app». No implementar bloqueo de llamadas entrantes en v1 ni garantizar que desaparece toda UI telefónica del sistema.

Si la única ruta de emergencia disponible exige abrir todo Ajustes o todo el marcador normal con acceso lateral a otras apps, el equipo no pasa G0 hasta corregirlo. No entregar el modo estricto sin una ruta clara, repetible y observada por el adulto.

## H10. Recuperación y mantenimiento sin internet

Implementar CP/1 sección 9 exactamente. Tres propósitos distintos: mantenimiento de red, reemplazar padre y retirada. La app no muestra un menú de administración libre; solicita código de un solo uso o respuesta QR válida. Nunca muestra los hashes como si fueran códigos ni acepta un PIN de prueba en release.

Mantenimiento: herramientas propias de red (SSID y contraseña WPA2-PSK, reintento y diagnóstico básico) y actualización del propio controlador según H11. No concede un escritorio ni un instalador general. No abrir Ajustes completo. Para el primer A13 se pueden usar las APIs de Wi-Fi con excepción oficial de Device Owner, comprobando retorno y comportamiento por SDK [S22]. No intentar activar datos móviles mediante APIs ocultas. Durante preparación se dejan configuradas las redes habituales y datos móviles; portales cautivos y redes empresariales quedan fuera del asistente inicial.

El maintenance token no añade apps ordinarias a la lista. Tras 5 minutos, salir al bloqueo. La configuración Wi-Fi aceptada queda en el sistema para permitir reconectar; no queda una sesión de mantenimiento abierta. Reinicio cancela la sesión. Emergencias siguen accesibles en todos los pasos.

El código `RECOVER_PARENT` se consume antes de permitir un nuevo vínculo. Hasta finalizarlo y actualizar Render, el teléfono continúa bloqueado. La ventana dura 5 minutos y no sobrevive a muerte del proceso, reinicio o cancelación. El cambio de vínculo/revocación de la pública anterior es un commit atómico, no un borrado anticipado. Se conserva un diagnóstico de recuperación interrumpida sin habilitar enrolamiento; se requiere una nueva autorización o RETIRE disponible, según CP/1 §9.A.

## H11. Retirada y actualización

Retirada no equivale a `uninstall()` con contraseña. En este diseño, el dueño autoriza **restablecer el dispositivo** para retirar de forma limpia la administración. La pantalla avisa sobre pérdida de datos, requiere autorización local `RETIRE`, una confirmación adicional y escribir `BORRAR`. Asegurar que el adulto conserva credenciales de cuentas que el sistema pueda requerir tras restablecer. No intentar eludir FRP.

En Android 14/API 34+ usar `wipeDevice(0)` con las precondiciones oficiales; en versiones anteriores usar la variante soportada de `wipeData(0)`. La selección se hace por versión de sistema y target, no por suposición. No usar `clearDeviceOwnerApp` como salida de producción: es obsoleta y destinada a pruebas. No añadir flags para borrar eSIM, almacenamiento externo o protección de reset por defecto [S03].

Las pruebas de borrado son destructivas y se hacen solo en equipo de prueba con autorización; no forman parte de un script de CI automático sobre el equipo diario. El servidor no ofrece `WIPE` ni `RETIRE` remoto.

Actualizaciones: conservar packageName y firma de APK. La política de instalación ordinaria no debe impedir el mecanismo autorizado de mantenimiento de la propia app. Para v1, durante mantenimiento presencial el adulto inicia un flujo de instalación de APK desde una pantalla propia mediante PackageInstaller y verifica firma/versión antes de instalar; se comprueba el comportamiento del fabricante y los permisos oficiales. No se abre un gestor general de archivos sin restricciones. Si el primer prototipo aún no dispone de actualizador, no endurecer definitivamente la instalación diaria hasta diseñarlo y probarlo. ADB queda para laboratorio, no como puerta permanente.

Tras `MY_PACKAGE_REPLACED`: validar migración de estado y conservar secuencia, identidad y bloqueo. Nunca eliminar el archivo corrupto para «arreglar» una migración y permitir acceso. Una migración fallida queda en error seguro y recuperación autorizada.

## H12. Finalización y evidencia

Se considera implementado cuando las pruebas automatizadas de máquina de estados y protocolo pasan; las pruebas Android ejercitan persistencia y APIs; y las puertas G0–G4 se completan en el modelo exacto. El desarrollador debe entregar evidencia tanto de lo que se bloquea como de lo que sigue disponible (emergencia, conectividad y recuperación).

Una IA puede implementar tareas pequeñas, pero sus afirmaciones de «completo» no sustituyen pruebas físicas ni revisión de seguridad del protocolo. Si el dispositivo permite un intervalo de uso no autorizado tras suspensión o muerte del proceso que viola los requisitos, el resultado se registra como incompatible hasta corregirlo.
