# HIJO — ADR y especificaciones de control parental

**Versión 1.0 · 8 de octubre de 2026 · Especificación para implementar, no una aplicación terminada.**

## Objetivo de este documento

Aplicar el bloqueo local en Android administrado, mantener emergencias y recuperación autorizada, y ejecutar solamente órdenes válidas del padre.

Este archivo reúne el material del rol y el contrato compartido. Las rutas citadas corresponden a la raíz del paquete ZIP; al copiarlo bajo `docs/`, ajustar solo la ruta de lectura, nunca nombres del protocolo. El esquema JSON, los ejemplos y sus verificadores están en el ZIP. Para una IA de contexto reducido, usar los archivos modulares y una tarea por conversación, en lugar de cargar esta lectura completa.

Se eligieron **dos APK Kotlin separados** con núcleo compartido y **un relay Node/TypeScript/WSS en Render**. No hay base de datos central, Firebase ni historial. El A13 es candidato inicial; su modelo exacto/Android y compatibilidad física están pendientes. Emergencias, suspensión, reinicio, recuperación y desinstalación se validan en el teléfono antes de uso diario.

## Índice

1. [Instrucciones para una IA pequeña](#parte-1)
2. [SPEC de la app hijo](#parte-2)
3. [ADR-H-001](#parte-3)
4. [ADR-H-002](#parte-4)
5. [ADR-H-003](#parte-5)
6. [ADR-H-004](#parte-6)
7. [ADR-H-005](#parte-7)
8. [ADR-H-006](#parte-8)
9. [Instalación y validación del A13](#parte-9)
10. [Plan paso a paso del hijo](#parte-10)
11. [Contrato compartido CP/1](#parte-11)
12. [Uso del esquema y validaciones](#parte-12)
13. [Modelo de amenazas](#parte-13)
14. [Fuentes y compatibilidad](#parte-14)
15. [Tareas comunes C00–C02](#parte-15)
16. [Trazabilidad de requisitos](#parte-16)
17. [Matriz de aceptación](#parte-17)
18. [Ficha del dispositivo](#parte-18)
19. [Plantilla de estado](#parte-19)
20. [Plantilla de traspaso de contexto](#parte-20)
21. [Resultados de validación del paquete](#parte-21)

---

<a id="parte-1"></a>

## Parte 1 · Instrucciones para una IA pequeña

Archivo modular: `hijo/AGENTS.md`.

## Instrucciones para una IA de contexto reducido — hijo

Tu responsabilidad es **DPC, quiosco y ejecución local del hijo**. El repositorio es por crear; este paquete es documentación. No afirmes que el producto funciona hasta ejecutar pruebas apropiadas.

### Lectura mínima por tarea

1. Lee este archivo y `ESTADO.md` del repositorio.
2. Abre únicamente la tarea elegida en `PLAN_HIJO.md` y las secciones citadas de `SPEC_HIJO.md`.
3. Para protocolos, lee las secciones correspondientes de `compartido/00_CONTRATO_V1.md` y el esquema JSON. Para Android específico, consulta las fuentes primarias de compatibilidad antes de inventar una API.
4. Si cambias arquitectura, consulta el ADR afectado; no cambies decisiones globales para resolver un error local.

### Invariantes

Dos APK Kotlin. Hijo Device Owner y Lock Task; padre sin permisos de administración. Render es relay WSS de una instancia, Node24/TypeScript/`ws`, sin DB, Firebase, Redis, historial ni secretos privados del padre. Configuración pública de claves en env sí está permitida y es obligatoria.

Claves P-256, firmas SHA-256 ECDSA DER y bytes deterministas CP/1. No aceptar claves suministradas por un comando. Un ACK del relay no prueba bloqueo. Duraciones con reloj monotónico, contador persistido, contexto de boot/sesión y desafío. Nunca ampliar permisos ante errores.

Emergencia del sistema siempre accesible. App y servicio visibles, sin espionaje ni accesibilidad/overlays como sustituto de políticas oficiales. Recuperación local antes de endurecer. Retirada presencial con aviso de borrado, nunca remota. A13 candidato no significa A13 certificado ni compatibilidad universal.

### Forma de trabajar

Implementa una tarea pequeña por turno. Propón división antes de modificar muchos módulos. Mantén las funciones puras separadas de Android. Usa interfaces/fakes en tests y jamás una clave maestra, bypass o simulación de APPLIED en release. No ejecutes restablecimientos ni restricciones que corten recuperación en un teléfono diario.

Prueba el cambio. Si no hay SDK/dispositivo/permisos, reporta exactamente qué no pudiste verificar y deja la tarea como pendiente de esa prueba. No marques pruebas manuales como aprobadas por inspección del código.

Entrega: breve resultado, rutas cambiadas, pruebas realmente ejecutadas, pendientes y siguiente tarea. Actualiza `ESTADO.md` sin guardar claves, códigos o información personal. Para continuar usa `plantillas/HANDOFF.md`.

### Prompt inicial sugerido

```text
Trabaja solo en la tarea [ID] de hijo/PLAN_HIJO.md.
Lee hijo/AGENTS.md, ESTADO.md y las secciones que esa tarea indica.
No implementes las tareas posteriores. Respeta CP/1 y los ADR aceptados.
Implementa, ejecuta los tests disponibles y actualiza ESTADO.md.
Indica de forma explícita cualquier prueba que requiera el A13 real.
```

---

<a id="parte-2"></a>

## Parte 2 · SPEC de la app hijo

Archivo modular: `hijo/SPEC_HIJO.md`.

## SPEC-H — Aplicación administradora del hijo

**Objetivo:** convertir un Android autorizado en un dispositivo administrado que aplique localmente bloqueo tras reinicio, ausencia de internet/canal de control y órdenes temporales del padre; impedir su desinstalación normal y mantener emergencias accesibles.

Candidato inicial: Samsung Galaxy A13, variante y versión por inventariar. **No está probado ni certificado todavía.** Este documento, el protocolo compartido CP/1 y la matriz de aceptación son obligatorios. Las restricciones proceden de APIs oficiales [S01–S10, S17–S22]; los algoritmos, estados y umbrales siguientes son decisiones del proyecto.

### H1. Condiciones previas y alcance

La app debe ser Device Owner de un teléfono preparado por su propietario. Un administrador tradicional o un permiso de accesibilidad no cumplen el requisito. El teléfono puede necesitar copia de seguridad y restablecimiento para aprovisionarlo. La instalación inicial es presencial. No se sustituye otro administrador ni se eluden restricciones de un teléfono ajeno.

Base del proyecto: Android 12/API 31 o superior; compile/target API 36. La distribución v1 es privada mediante APK firmado. La app permanece visible como «Control parental — dispositivo administrado». No hay root, exploits, servicio de accesibilidad, superposición engañosa, capturas, micrófono, GPS ni lectura de conversaciones.

Un teléfono sin capacidad real de telefonía de emergencia no pasa la compatibilidad del producto. No basta con que pueda instalar el APK. Tampoco se certifica otro fabricante por pasar los tests del A13.

### H2. Componentes y límites de responsabilidad

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

### H3. Requisitos funcionales

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

### H4. Manifest, permisos y aprovisionamiento

#### H4.1 Declaraciones requeridas

- `ChildAdminReceiver`: exportado, protegido con `android.permission.BIND_DEVICE_ADMIN`, metadata `android.app.device_admin` y acción `DEVICE_ADMIN_ENABLED`. El XML de políticas declara `force-lock` y `wipe-data` para las funciones de emergencia/retirada. No se ejecuta un borrado al recibir una orden ordinaria.
- `LauncherActivity`: exportada para HOME, categorías MAIN/HOME/DEFAULT y entrada LAUNCHER separada; `lockTaskMode="if_whitelisted"`. La pantalla inicial siempre parte de bloqueado, no de permitido.
- `BootReceiver`: `directBootAware=true`, `exported=false`; escucha `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`. Son eventos del sistema, no una API de control remoto. La recuperación adicional de UI responde a `USER_UNLOCKED` mediante el mecanismo apropiado y registrado solo cuando haga falta.
- `ControlService`: `exported=false`, `directBootAware=true`. En Android 14+ declarar tipo `systemExempted` y permiso específico, después de comprobar elegibilidad real como Device Owner [S05, S06].
- Los componentes que acceden a almacenamiento antes de desbloqueo usan exclusivamente contexto Device Protected. Revisar también inicializadores automáticos de bibliotecas: ninguno puede leer datos Credential Encrypted durante Direct Boot.

Permisos básicos: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`; añadir `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` para API 34+, `POST_NOTIFICATIONS` para aviso visible en API 33+, `CAMERA` solo para vinculación/recuperación QR, `ACCESS_WIFI_STATE` y `CHANGE_WIFI_STATE` para mantenimiento de red. `READ_PHONE_STATE` se limita a observar si hay una llamada en curso cuando sea necesario para no interrumpir la interfaz telefónica; no se solicitan números ni registros. Solicitar los permisos de ejecución correspondientes durante instalación presencial y explicar cada uno.

No declarar `CALL_PHONE`, `READ_CALL_LOG`, `READ_CONTACTS`, `RECORD_AUDIO`, ubicación, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES` o `SCHEDULE_EXACT_ALARM` por defecto. Usar consultas de paquetes limitadas a actividades MAIN/LAUNCHER y los componentes de sistema necesarios. No añadir permisos privilegiados que una app normal no puede obtener.

#### H4.2 Aprovisionamiento

La ruta de laboratorio usa ADB sobre equipo limpio sin cuentas y la propia app como DPC [S17]. La guía `INSTALACION_A13.md` contiene pasos y diferencias debug/release. La app no ejecuta `dpm set-device-owner` sobre sí misma.

Si se automatiza instalación desde Setup Wizard mediante QR, implementar actividades para `ACTION_GET_PROVISIONING_MODE` y `ACTION_ADMIN_POLICY_COMPLIANCE`, comprobar que el resultado es FULLY_MANAGED y el flujo termina de verdad. No usar únicamente el antiguo `ACTION_PROVISION_MANAGED_DEVICE` para Android 12+ [S18, S19]. La ruta manual de laboratorio es suficiente para el primer prototipo; el aprovisionamiento reproducible debe quedar documentado antes de otra instalación.

### H5. Restricciones y aplicación del quiosco

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

#### H5.1 Orden de enforcement

Para cerrar: persistir intención aceptada; reducir inmediatamente permisos/lista; llevar a pantalla controlada cuando sea seguro; comprobar resultado; guardar resultado; enviar ACK. Para abrir: persistir primero; verificar estado de red, reloj y contexto; ampliar solo los paquetes aprobados; comprobar; confirmar. El estado de política solicitado y el estado realmente permitido son campos distintos.

En Android 14+ observar también resultados de políticas cuando los exponga `PolicyUpdateReceiver`; una llamada sin excepción no garantiza éxito. En todas las versiones usar lectura de políticas y estado de Lock Task, más validación de UI en pruebas. Si falta evidencia, devolver `ACCEPTED_PENDING_ENFORCEMENT` o `ENFORCEMENT_FAILED` [S03].

No llamar `startLockTask` sobre el keyguard activo de manera que encierre al usuario detrás de su PIN. Antes del primer desbloqueo, conservar restricciones y mostrar estado de arranque; entrar en quiosco cuando el sistema permita hacerlo [S02]. El PIN de Android/SIM no es la clave parental y no se elimina desde nuestra app.

### H6. Máquina de estados y reloj

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

### H7. Ejecución y red

`ControlService` es un servicio en primer plano visible, iniciado solo cuando la app está aprovisionada como propietaria y cumple las condiciones del tipo [S05, S06]. Promoverlo a foreground dentro del plazo del sistema; no hacer una conexión bloqueante antes de publicar su notificación. En API 31–33 no pasar flags de tipos que no existen; en API 34+ usar `systemExempted` si la comprobación de Device Owner pasó. No etiquetar el servicio como reproducción multimedia, `remoteMessaging` ni `dataSync` para mantenerlo artificialmente.

`START_STICKY` y la recepción de boot ayudan a recuperar el servicio; **no garantizan ejecución permanente**. Si Android mata el proceso mientras otra app permitida estaba abierta, la lista de quiosco previa puede persistir hasta que el controlador actúe. Por eso la prueba de muerte de proceso con otra app en primer plano es bloqueante para certificar este diseño. No ocultar esta ventana con una afirmación de «fail closed» absoluta.

Un solo `NetworkCallback` observa la red predeterminada; `onAvailable` no basta: hay que revisar `NET_CAPABILITY_INTERNET` y `NET_CAPABILITY_VALIDATED` con `onCapabilitiesChanged`. Un cambio Wi-Fi a datos se evalúa con la red actual, no con un `onLost` antiguo [S09]. Un solo socket autenticado y una coroutine de reconexión. Cancelar el intento anterior al reemplazarlo. Mantener `lastPongElapsed` solo para la conexión actual.

Una pérdida de validación que dura más de 3 s bloquea. Aunque Android siga validando internet, 45 s sin PONG fresco bloquean. Al conectar de nuevo, obtener un PONG válido antes de considerar el canal saludable. Un fallo de certificado jamás se resuelve desactivando TLS.

Probar Doze forzado y suspensión natural, ahorro de energía y ajustes específicos del A13. Una eventual exclusión de optimización de batería se solicita de forma visible y documentada durante instalación; no se presume que Device Owner elimina todos los comportamientos del fabricante [S08]. No instalar mecanismos ocultos de autoinicio.

### H8. Persistencia y aplicación idempotente

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

### H9. Emergencias: puerta obligatoria, no una maqueta

**Requisito de aceptación:** desde reinicio, bloqueo parental, pérdida de red y recuperación, el usuario puede acceder a la interfaz de emergencia del sistema sin clave parental. No se reemplaza por VoIP ni por una llamada al servidor.

Ruta portable elegida para el primer prototipo: mantener el keyguard real habilitado y `LOCK_TASK_FEATURE_KEYGUARD`; conservar la credencial de Android. El acceso de emergencia se realiza desde el keyguard del sistema. Nuestra pantalla indica «Emergencia: usar llamada de emergencia de la pantalla de bloqueo» y ofrece «Ir a pantalla de bloqueo», que solicita `DevicePolicyManager.lockNow()` de forma explícita. En modelos donde apaga la pantalla, explicar que se enciende con el botón lateral y se usa la opción del sistema. Este flujo puede necesitar más de una pulsación; no afirmar que es un marcador embebido.

La app no ejecuta `resetPassword`, no elimina el PIN de Android y no pide el PIN del padre para emergencia. El niño puede conocer su PIN ordinario de Android: introducirlo no elimina la política parental.

No usar `TelecomManager.createLaunchEmergencyDialerIntent` como si fuese una API pública: en la fuente de Android está marcada `@SystemApi/@hide` [S16]. No añadir reflexión, strings de intents privados o nombres fijos de paquetes Samsung como solución genérica. Una ruta directa adicional solo se aprueba mediante API pública documentada y validación por modelo.

`DISALLOW_OUTGOING_CALLS` conserva la posibilidad de emergencia por política [S04], pero la interfaz se valida en el A13. No basta con un botón cuyo clic no llegue al marcador. Nunca hacer llamadas de prueba a números reales de emergencia sin coordinación formal con el servicio correspondiente. La verificación ordinaria abre y sale del marcador **sin llamar**; el curso completo de una llamada se simula en entorno autorizado o se coordina formalmente. No se marca probado lo que no se probó.

Las nuevas órdenes no deben finalizar llamadas ni retirar componentes telefónicos del sistema. `CallSafetyObserver` usa únicamente estado de llamada en curso cuando el fabricante lo requiera; aplaza tomar el primer plano hasta que termine, sin suspender telefonía. Las devoluciones de llamada e interfaces imprescindibles de seguridad del sistema tienen prioridad sobre la regla «ninguna otra app». No implementar bloqueo de llamadas entrantes en v1 ni garantizar que desaparece toda UI telefónica del sistema.

Si la única ruta de emergencia disponible exige abrir todo Ajustes o todo el marcador normal con acceso lateral a otras apps, el equipo no pasa G0 hasta corregirlo. No entregar el modo estricto sin una ruta clara, repetible y observada por el adulto.

### H10. Recuperación y mantenimiento sin internet

Implementar CP/1 sección 9 exactamente. Tres propósitos distintos: mantenimiento de red, reemplazar padre y retirada. La app no muestra un menú de administración libre; solicita código de un solo uso o respuesta QR válida. Nunca muestra los hashes como si fueran códigos ni acepta un PIN de prueba en release.

Mantenimiento: herramientas propias de red (SSID y contraseña WPA2-PSK, reintento y diagnóstico básico) y actualización del propio controlador según H11. No concede un escritorio ni un instalador general. No abrir Ajustes completo. Para el primer A13 se pueden usar las APIs de Wi-Fi con excepción oficial de Device Owner, comprobando retorno y comportamiento por SDK [S22]. No intentar activar datos móviles mediante APIs ocultas. Durante preparación se dejan configuradas las redes habituales y datos móviles; portales cautivos y redes empresariales quedan fuera del asistente inicial.

El maintenance token no añade apps ordinarias a la lista. Tras 5 minutos, salir al bloqueo. La configuración Wi-Fi aceptada queda en el sistema para permitir reconectar; no queda una sesión de mantenimiento abierta. Reinicio cancela la sesión. Emergencias siguen accesibles en todos los pasos.

El código `RECOVER_PARENT` se consume antes de permitir un nuevo vínculo. Hasta finalizarlo y actualizar Render, el teléfono continúa bloqueado. La ventana dura 5 minutos y no sobrevive a muerte del proceso, reinicio o cancelación. El cambio de vínculo/revocación de la pública anterior es un commit atómico, no un borrado anticipado. Se conserva un diagnóstico de recuperación interrumpida sin habilitar enrolamiento; se requiere una nueva autorización o RETIRE disponible, según CP/1 §9.A.

### H11. Retirada y actualización

Retirada no equivale a `uninstall()` con contraseña. En este diseño, el dueño autoriza **restablecer el dispositivo** para retirar de forma limpia la administración. La pantalla avisa sobre pérdida de datos, requiere autorización local `RETIRE`, una confirmación adicional y escribir `BORRAR`. Asegurar que el adulto conserva credenciales de cuentas que el sistema pueda requerir tras restablecer. No intentar eludir FRP.

En Android 14/API 34+ usar `wipeDevice(0)` con las precondiciones oficiales; en versiones anteriores usar la variante soportada de `wipeData(0)`. La selección se hace por versión de sistema y target, no por suposición. No usar `clearDeviceOwnerApp` como salida de producción: es obsoleta y destinada a pruebas. No añadir flags para borrar eSIM, almacenamiento externo o protección de reset por defecto [S03].

Las pruebas de borrado son destructivas y se hacen solo en equipo de prueba con autorización; no forman parte de un script de CI automático sobre el equipo diario. El servidor no ofrece `WIPE` ni `RETIRE` remoto.

Actualizaciones: conservar packageName y firma de APK. La política de instalación ordinaria no debe impedir el mecanismo autorizado de mantenimiento de la propia app. Para v1, durante mantenimiento presencial el adulto inicia un flujo de instalación de APK desde una pantalla propia mediante PackageInstaller y verifica firma/versión antes de instalar; se comprueba el comportamiento del fabricante y los permisos oficiales. No se abre un gestor general de archivos sin restricciones. Si el primer prototipo aún no dispone de actualizador, no endurecer definitivamente la instalación diaria hasta diseñarlo y probarlo. ADB queda para laboratorio, no como puerta permanente.

Tras `MY_PACKAGE_REPLACED`: validar migración de estado y conservar secuencia, identidad y bloqueo. Nunca eliminar el archivo corrupto para «arreglar» una migración y permitir acceso. Una migración fallida queda en error seguro y recuperación autorizada.

### H12. Finalización y evidencia

Se considera implementado cuando las pruebas automatizadas de máquina de estados y protocolo pasan; las pruebas Android ejercitan persistencia y APIs; y las puertas G0–G4 se completan en el modelo exacto. El desarrollador debe entregar evidencia tanto de lo que se bloquea como de lo que sigue disponible (emergencia, conectividad y recuperación).

Una IA puede implementar tareas pequeñas, pero sus afirmaciones de «completo» no sustituyen pruebas físicas ni revisión de seguridad del protocolo. Si el dispositivo permite un intervalo de uso no autorizado tras suspensión o muerte del proceso que viola los requisitos, el resultado se registra como incompatible hasta corregirlo.

---

<a id="parte-3"></a>

## Parte 3 · ADR-H-001

Archivo modular: `hijo/adr/ADR-H-001.md`.

## ADR-H-001 — Device Owner y quiosco persistente

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

El niño puede cerrar o desactivar controles convencionales. Una pantalla que cubre otras apps no establece autoridad de administración.

### Decisión

Usar APIs oficiales Device Owner, HOME persistente y Lock Task real. Mantener el quiosco también en el modo permitido, cambiando solo la lista aprobada.

### Consecuencias y alternativas descartadas

Requiere preparación del dispositivo y compatibilidad por modelo. No root, accesibilidad, pantalla fijada ni ocultación. No se promete impedir recovery o reinstalación del sistema.

### Validación y revisión

H-F01/02/09, H-SEC-02, G0. Fuentes [S01–S04, S17].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-4"></a>

## Parte 4 · ADR-H-002

Archivo modular: `hijo/adr/ADR-H-002.md`.

## ADR-H-002 — Decisión de bloqueo local y estados cerrados

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

Un relay sin base de datos no puede ser la memoria ni el ejecutor del bloqueo. Reiniciar o desconectar no debe devolver acceso.

### Decisión

La máquina de estados del hijo decide con datos duraderos. Nuevo arranque exige autorización fresca. Red perdida y canal vencido bloquean independientemente del padre.

### Consecuencias y alternativas descartadas

Una caída de Render puede bloquear. El padre apagado no bloquea por sí mismo. Reanudar red no elimina una orden de bloqueo o una autorización de arranque faltante. Las limitaciones de ejecución del sistema se verifican, no se ocultan.

### Validación y revisión

H-BOOT-01, H-NET-01/03, H-TIME-01/02, H-PROC-01. Fuentes [S07–S09, S20].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-5"></a>

## Parte 5 · ADR-H-003

Archivo modular: `hijo/adr/ADR-H-003.md`.

## ADR-H-003 — Servicio visible y eficiencia sin vigilancia

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

Se necesitan eventos de red y WSS persistente en el hijo, sin capturar datos personales ni agotar batería con un bucle permanente.

### Decisión

Servicio en primer plano visible y elegible como `systemExempted` cuando corresponda; NetworkCallback, heartbeat y reconexión con backoff. Sin FCM, WorkManager como reloj de segundos ni wake lock permanente.

### Consecuencias y alternativas descartadas

La etiqueta del servicio no garantiza inmunidad a suspensión o al fabricante. Doze, muerte de proceso y batería son puertas de compatibilidad. No usar tipos de servicio falsos para evitar restricciones.

### Validación y revisión

H-NET-04, H-PROC-01, H-POWER-01/02. Fuentes [S05, S06, S08, S09].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-6"></a>

## Parte 6 · ADR-H-004

Archivo modular: `hijo/adr/ADR-H-004.md`.

## ADR-H-004 — Emergencia mediante el sistema antes que endurecimiento

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

El producto debe mantener una ruta de emergencia real, incluso sin internet y tras arrancar. Un botón sin destino comprobado no cumple.

### Decisión

Conservar keyguard y telefonía. Primer prototipo usa la ruta de emergencia del keyguard; navegación explícita a pantalla bloqueada mediante API pública. Probar físicamente antes de cerrar otras salidas.

### Consecuencias y alternativas descartadas

No API privada de marcador ni allowlist indiscriminada de Ajustes. No suprimir llamadas o devoluciones de seguridad. Las pruebas reales de llamada requieren coordinación; no llamar a emergencias para probar.

### Validación y revisión

G0, H-EMG-01/02/03/04. Fuentes [S02–S04, S15, S16].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-7"></a>

## Parte 7 · ADR-H-005

Archivo modular: `hijo/adr/ADR-H-005.md`.

## ADR-H-005 — Estado atómico, secuencia y reloj monotónico

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

Cierres, ACK perdidos y cambios de hora pueden hacer repetir una orden o reiniciar un temporizador.

### Decisión

Un solo escritor y archivo AtomicFile para política, contador, hash/id y fase de aplicación. Medir duración con elapsedRealtime en el mismo arranque; vincular nuevas órdenes a sesión, boot y desafío.

### Consecuencias y alternativas descartadas

No separar contador y política en escrituras independientes; no comparar firmas DER como identificadores; no restablecer contadores por reinicio. Datos corruptos mantienen bloqueo y requieren recuperación.

### Validación y revisión

C-SEQ-01/02/03/04, H-STATE-01/02, H-TIME-02. Fuentes [S07, S20, S21, S25].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-8"></a>

## Parte 8 · ADR-H-006

Archivo modular: `hijo/adr/ADR-H-006.md`.

## ADR-H-006 — Portabilidad por capacidades, no promesa universal

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

### Contexto

A13 no identifica versión exacta. Android y fabricantes tienen diferencias de energía, aprovisionamiento y telefonía.

### Decisión

Primero certificar modelo/firmware real; probar APIs y capacidades. Base hijo API31+, sin APIs privadas ni paquetes Samsung fijados como solución universal.

### Consecuencias y alternativas descartadas

Otras marcas, versiones y variantes requieren la misma matriz. iPhone hijo queda fuera. Cambiar una dependencia, target o firmware requiere volver a ejecutar pruebas críticas.

### Validación y revisión

G0–G4 y FICHA_DISPOSITIVO. Fuentes [S01, S05, S06, S14, S24].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.

---

<a id="parte-9"></a>

## Parte 9 · Instalación y validación del A13

Archivo modular: `hijo/INSTALACION_A13.md`.

## Instalación y validación inicial del A13

Esta es una guía de preparación del propietario. No ejecutar restablecimiento ni activar restricciones en el único teléfono de emergencia antes de probar la recuperación. **El paquete contiene documentación, no un APK listo.**

### 1. Identificar el equipo sin suposiciones

En Ajustes → Acerca del teléfono y Software, registrar código SM-…, Android, One UI, parche y si tiene SIM/PIN de SIM. «A13» no basta para certificar. Si ya existe administrador de una organización u otra persona, no intentar sustituirlo. Confirmar propiedad y permiso para restablecer.

Con ADB ya habilitado para pruebas se pueden consultar, sin modificar datos:

```powershell
adb devices
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.security_patch
```

Estos comandos suponen Platform Tools en PATH. En Windows, también puede invocarse `adb.exe` mediante su ruta dentro de `%LOCALAPPDATA%\Android\Sdk\platform-tools`. El número de serie que muestre ADB no se publica ni se envía al relay.

### 2. Preparar Windows y el proyecto

Instalar Android Studio estable y los SDK Platform Tools. Crear proyecto Kotlin/Compose y fijar nombres de módulos de la especificación. Usar el JDK incluido compatible con la versión de Gradle generada por el IDE; no cambiar versiones de Kotlin/Gradle al azar. Instalar plataforma API 36, configurar minSdk 31 en hijo y 30 en padre. Guardar wrapper, catálogo de versiones y configuración en Git. No guardar keystores ni contraseñas en el repositorio.

Desde la carpeta `android`, cuando los módulos ya estén implementados:

```powershell
.\gradlew.bat :core-protocol:test
.\gradlew.bat :app-child:testDebugUnitTest
.\gradlew.bat :app-child:assembleDebug
```

Los comandos son objetivos de construcción para el repositorio por crear; **no pueden ejecutarse sobre este ZIP de documentación**. Para instrumentación se usará `:app-child:connectedDebugAndroidTest` con un dispositivo de laboratorio conectado.

### 3. Copia de seguridad y entorno limpio

Guardar previamente información que el propietario desee conservar. Verificar credenciales Google/Samsung y obligaciones de protección de restablecimiento. Cualquier restablecimiento se hace manualmente y con confirmación del propietario; esta guía no lo ejecuta. Dejar el equipo limpio, sin cuentas añadidas ni otro propietario. Activar opciones de desarrollador/depuración únicamente para laboratorio, autorizar el computador del propietario e instalar la compilación de pruebas.

Ejemplo después de construir un APK debug de laboratorio:

```powershell
adb install -t .\app-child\build\outputs\apk\debug\app-child-debug.apk
adb shell dpm set-device-owner dev.controlparental.child/.admin.ChildAdminReceiver
adb shell dumpsys device_policy
```

El comando `dpm` requiere que esa clase exista y esté correctamente declarada. Se comprueba además en la UI de diagnóstico `isDeviceOwnerApp == true`. Ante error de cuentas, usuarios o aprovisionamiento, corregir las precondiciones; no buscar bypasses ni hacer root. El procedimiento ADB está documentado para desarrollo [S17].

En la fase de laboratorio no activar todavía `DISALLOW_DEBUGGING_FEATURES` ni bloquear la única ruta de mantenimiento. Las variantes debug y release deben separarse de manera que las salidas de laboratorio no estén presentes en release. No dejar `android:testOnly=true` en producción.

### 4. Orden seguro para las primeras pruebas

Primero implementar y comprobar emergencia/keyguard con políticas mínimas. Luego implementar recuperación local. Solo después activar quiosco y endurecimiento progresivo. Registrar el resultado de cada política y la vuelta segura a mantenimiento.

Orden mínimo: diagnóstico → emergencia → persistencia bloqueada/boot → recuperación → quiosco mínimo → apps aprobadas → red/servicio → firma/vínculo → relay → padre → pruebas de proceso y energía. Durante el desarrollo temprano pueden usarse fakes y fixtures, pero no una clave maestra dentro del APK del equipo diario.

Antes de una nueva política que afecte pantalla, redes o instalación, comprobar que el adulto tiene ruta de recuperación operativa. No bloquear Wi-Fi y depuración a la vez si todavía no existe mantenimiento local funcional.

### 5. Vincular y preparar Render

Completar el flujo de dos QR descrito en CP/1. Configurar aplicaciones autorizadas presencialmente. Conservar externamente los códigos generados por la app; este ZIP no incluye códigos reales. Copiar a Render exclusivamente las claves públicas/pairId/origen exportados. Comprobar conexión, estado fresco y una orden de cada tipo.

No introducir ninguna clave privada ni código de recuperación en variables de entorno, repositorios, instrucciones a una IA, capturas de chat o logs. Un QR de aceptación contiene claves públicas y hashes, no las privadas.

### 6. Ensayar reinicio, red y suspensión

Cerrar la app padre después de permitir uso: la política recibida debe continuar. Desconectar internet en el hijo: al vencer el margen debe bloquearse sin esperar al padre. Restablecer red: reevalúa la política. Reiniciar hijo: permanece bloqueado hasta autorización nueva, aunque internet funcione.

Para laboratorio se pueden usar las pruebas oficiales de Doze [S08]:

```powershell
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle
# Ejecutar observaciones de red/estado previstas en la matriz.
adb shell dumpsys deviceidle unforce
adb shell dumpsys battery reset
```

Restaurar siempre el estado simulado de batería al terminar. Estos comandos son solo de pruebas autorizadas antes de cerrar la depuración. Probar también suspensión natural sin cable USB; un equipo cargando y conectado al IDE no reproduce necesariamente el uso real.

No intentar inferir bloqueo por falta de mensajes: observar el estado de la pantalla y las apps que podrían seguir accesibles. Probar muerte del proceso controlador mientras una app permitida está delante. Si hay acceso no autorizado, registrar fallo; no esconderlo.

### 7. Versión final privada

Construir release con firma propia y guardar material de firma fuera del repositorio en lugar protegido. El keystore de firma del APK pertenece al desarrollador y es distinto de la clave parental generada en cada teléfono. Sin firma de APK conservada no habrá actualización normal del mismo paquete.

Probar actualización, recuperación y retirada antes de aplicar endurecimiento definitivo. Instalar release mediante un procedimiento limpio/autorizado, volver a vincular cuando cambie la identidad de instalación y repetir la matriz crítica. Retirar depuración y sus autorizaciones del computador desde la preparación del dispositivo; luego aplicar las restricciones finales.

**No entregar un debug endurecido como solución final.** El adulto confirma las limitaciones de recuperación física y el acceso a emergencia del modelo concreto. Guardar la ficha de certificación en `pruebas/FICHA_DISPOSITIVO.md`.

---

<a id="parte-10"></a>

## Parte 10 · Plan paso a paso del hijo

Archivo modular: `hijo/PLAN_HIJO.md`.

## Plan de implementación — hijo

Estado de todas las tareas: **PENDIENTE**. Ninguna aplicación ha sido compilada o probada por este paquete. Cada tarea termina con código, tests y actualización de `ESTADO.md`. Leer únicamente la sección indicada y las dependencias necesarias; no enviar todo el documento completo a una IA pequeña.

### Índice de ejecución

| Tarea | Entregable | Depende de |
|---|---|---|
| H00 | Inventario y plan de laboratorio | C00 |
| H01 | DPC y launcher mínimos | H00 |
| H02 | Ruta de emergencia del sistema | H01 |
| H03 | Motor puro de políticas | C01 |
| H04 | Estado atómico y contexto de arranque | H01, H03 |
| H05 | Recuperación local antes de endurecer | C02, H04 |
| H06 | Enforcement de quiosco y aplicaciones permitidas | H02, H04, H05 |
| H07 | Servicio visible y detección de red | H04, H06 |
| H08 | Identidad y vinculación QR del hijo | C02, H05; P02 para integración |
| H09 | Procesador de comandos seguro | H03, H04, H06, H08 |
| H10 | Conexión WSS y estado firmado | H07, H08, H09, R03 |
| H11 | Mantenimiento Wi-Fi, actualización y retirada | H05, H06, H08; P07 para token local |
| H12 | Pruebas adversas y release | H02–H11, P06, P07, R03; coordinar evidencia con P08 sin depender de su cierre |

### H00 — Inventario y plan de laboratorio

**Depende de:** C00.

**Leer:** Fuentes/compatibilidad; INSTALACION_A13 §1–3.

**Archivos/componentes previstos:** FICHA_DISPOSITIVO.md, BUILD_ENV.md.

**Implementar:** Registrar SM, Android/API, One UI, parche y disponibilidad de equipo de prueba. Confirmar copia/propiedad y rutas de recuperación.

**Criterio de aceptación:** No se escribe que el teléfono usa Android14 sin comprobarlo. No se hace ningún borrado automático.

**Comprobación:** Inventario completo o campos explícitos NO COMPROBADO.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H01 — DPC y launcher mínimos

**Depende de:** H00.

**Leer:** SPEC-H H2/H4; INSTALACION_A13 §3.

**Archivos/componentes previstos:** ChildAdminReceiver, LauncherActivity, manifest y device_admin.xml.

**Implementar:** Crear componentes sin endurecimiento definitivo, pantalla bloqueada por defecto y diagnóstico isDeviceOwner. Aprovisionar en laboratorio con ADB.

**Criterio de aceptación:** Se demuestra propietario real; abrir como app normal muestra NO CONFIGURADO, no simula control.

**Comprobación:** H-OWNER-01 y arranque de APK laboratorio.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H02 — Ruta de emergencia del sistema

**Depende de:** H01.

**Leer:** SPEC-H H9; ADR-H-004.

**Archivos/componentes previstos:** EmergencyAccess, LockedScreen, prueba de keyguard.

**Implementar:** Conservar keyguard y probar transición explícita a pantalla de bloqueo. Acceder al marcador del sistema sin clave parental. Revisar PIN/SIM y vuelta al launcher.

**Criterio de aceptación:** Se documenta la ruta exacta del A13 sin usar APIs privadas ni hacer llamadas de prueba injustificadas.

**Comprobación:** H-EMG-01/02; primera comprobación G0, todavía sin endurecimiento total.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H03 — Motor puro de políticas

**Depende de:** C01.

**Leer:** CP/1 §2/3; SPEC-H H6.

**Archivos/componentes previstos:** PolicyEngine, modelos de evaluación, tests de tabla.

**Implementar:** Implementar estados, prioridad y cuatro acciones con un reloj inyectable. No importar Android ni usar fecha del sistema en decisiones.

**Criterio de aceptación:** LOCK_FOR vencido no permite sin canal; reboot siempre exige nueva autorización; padre offline no invalida política.

**Comprobación:** H-TIME-01/02/03 y casos de máquina de estados.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H04 — Estado atómico y contexto de arranque

**Depende de:** H01, H03.

**Leer:** SPEC-H H8; CP/1 §10.

**Archivos/componentes previstos:** ChildStateStore, BootReceiver, BootContextRepository.

**Implementar:** Persistir última política/seq/id/hash/fase. Leer contexto Device Protected. Distinguir callbacks de un mismo arranque de uno nuevo. Reanudar sin reiniciar duración.

**Criterio de aceptación:** Archivo corrupto bloquea; proceso muerto conserva tiempos; antes de desbloqueo no se exige Keystore para conservar restricciones.

**Comprobación:** H-STATE-01/02, H-BOOT-01/02/03.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H05 — Recuperación local antes de endurecer

**Depende de:** C02, H04.

**Leer:** CP/1 §9; SPEC-H H10/H11.

**Archivos/componentes previstos:** RecoveryController, pantalla de código, sesión de mantenimiento.

**Implementar:** Implementar hashes por propósito, consumo atómico, límites de intentos y desafío OFFLINE firmado. Empezar con identidades inyectadas solo en tests/debug.

**Criterio de aceptación:** Reinicio no devuelve un código consumido. Código de mantenimiento no permite apps ni borrar. Emergencia nunca se bloquea por intentos.

**Comprobación:** H-REC-01/02/03; regenerar vínculo/códigos de producción después de pruebas.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H06 — Enforcement de quiosco y aplicaciones permitidas

**Depende de:** H02, H04, H05.

**Leer:** SPEC-H H5; ADR-H-001/004.

**Archivos/componentes previstos:** PolicyEnforcer, AllowedAppsScreen, diagnóstico de políticas.

**Implementar:** Aplicar HOME/lista/LockTask y flags mínimos; aprobar apps presencialmente. Reducir lista al bloquear, conservar emergencias y verificar resultados. Endurecer gradualmente.

**Criterio de aceptación:** No queda pantalla fijada ni selector HOME libre. No se abre todo Ajustes para resolver un fallo. La ruta de emergencia sigue operativa.

**Comprobación:** H-ENF-01/02/03, H-SEC-01/02/03, H-EMG-03/04; cerrar G0 solo aquí.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H07 — Servicio visible y detección de red

**Depende de:** H04, H06.

**Leer:** SPEC-H H7; CP/1 §2.3/8.

**Archivos/componentes previstos:** ControlService, NetworkObserver, prueba de callbacks.

**Implementar:** Crear foreground service elegible por versión. Registrar una sola red predeterminada, evaluar capacidades y pérdidas con reloj monotónico. Añadir backoff, sin bucles ocupados.

**Criterio de aceptación:** Un Wi-Fi sin internet no permite uso. Cambio Wi-Fi/datos breve no duplica servicios ni sockets.

**Comprobación:** H-NET-01/02/04; no simular aplicación de políticas en esta prueba.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H08 — Identidad y vinculación QR del hijo

**Depende de:** C02, H05; P02 para integración.

**Leer:** CP/1 §4/5; SPEC-H H8/H10.

**Archivos/componentes previstos:** ChildKeyStore, PairingController, PairingScreen.

**Implementar:** Generar identidad no exportable, oferta con desafío visible, validar aceptación física y firmada. Guardar claves públicas/hashes y bloquear nueva vinculación libre.

**Criterio de aceptación:** La app vinculada rechaza otro padre salvo recuperación autorizada. No se exporta la privada para soportar Direct Boot.

**Comprobación:** C-PAIR-01, H-KEY-01 y pruebas de reinicio de oferta.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H09 — Procesador de comandos seguro

**Depende de:** H03, H04, H06, H08.

**Leer:** CP/1 §7; SPEC-H H5/H8.

**Archivos/componentes previstos:** CommandProcessor, generador ACK, tests secuenciales.

**Implementar:** Validar contexto/nonce/seq, aceptar atómicamente, aplicar con único escritor y confirmar resultado real. Repetición exacta no reinicia duración.

**Criterio de aceptación:** Mismo seq con otro payload se rechaza. Un fallo de política no genera APPLIED. No ampliar permisos si falla persistencia.

**Comprobación:** C-SEQ-01/02/03/04, H-STATE-01, H-ENF-03.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H10 — Conexión WSS y estado firmado

**Depende de:** H07, H08, H09, R03.

**Leer:** CP/1 §6–8; SPEC-H H7.

**Archivos/componentes previstos:** ChildRelayConnection, STATE/SYNC, heartbeat tests.

**Implementar:** Autenticar identidad contra relay, rotar childSessionId en cada conexión, emitir estado con syncNonce/desafío y usar PONG correlacionado.

**Criterio de aceptación:** 45s sin canal bloquean aunque siga habiendo internet; volver a conectar no acepta comandos de la sesión vieja.

**Comprobación:** H-NET-03/04, P-ACK-01, G2/G3 con evidencia.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H11 — Mantenimiento Wi-Fi, actualización y retirada

**Depende de:** H05, H06, H08; P07 para token local.

**Leer:** SPEC-H H10/H11.

**Archivos/componentes previstos:** MaintenanceScreen, WifiConfigurator, LocalUpdater, RetireDevice.

**Implementar:** Configurar red desde UI propia con APIs permitidas para DPC. Verificar firma/versión de actualización. Implementar retirada local con doble confirmación y selección correcta de API.

**Criterio de aceptación:** Mantenimiento no abre apps normales; actualización preserva identidad y secuencia; retiro no se ejecuta por una orden remota.

**Comprobación:** H-REC-04, H-UPDATE-01/02, H-RET-01 en dispositivo de prueba autorizado.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### H12 — Pruebas adversas y release

**Depende de:** H02–H11, P06, P07, R03; coordinar evidencia con P08 sin depender de su cierre.

**Leer:** MATRIZ_ACEPTACION y FICHA_DISPOSITIVO.

**Archivos/componentes previstos:** Evidencias G0–G4, manifest release, APK firmado.

**Implementar:** Probar proceso muerto con app permitida delante, suspensión, apagado, cortes, cambio de reloj, intentos de desinstalar y rutas de escapes. Medir consumo en reposo natural.

**Criterio de aceptación:** Solo marcar compatible al cumplir requisitos en SM/firmware real. Retirar testOnly/debug/ADB y repetir pruebas críticas con release.

**Comprobación:** Toda la matriz H/C aplicable; fallos explícitos no se convierten en aprobaciones.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

### Coordinación entre roles

H08/P02 y H11/P07 pueden implementarse con fakes locales, sin esperar que el otro módulo termine primero; las pruebas cruzadas se cierran en P08/H12. Estas dos tareas finales comparten evidencia, pero no son dependencias circulares. Nunca reemplazar la validación integrada por fakes para declarar el dispositivo compatible.

---

<a id="parte-11"></a>

## Parte 11 · Contrato compartido CP/1

Archivo modular: `compartido/00_CONTRATO_V1.md`.

## Contrato compartido de control parental — CP/1

Versión de protocolo: 1.0. Fecha: 2026-10-08. Estado: especificación normativa por implementar.

Este archivo es la única fuente de verdad del protocolo. Padre, hijo y relay deben usar los mismos nombres, unidades y validaciones. «DEBE» es obligatorio. Los intervalos y límites de este documento son decisiones del proyecto. Referencias externas: [S09–S13, S20, S23–S26] en `02_FUENTES_Y_COMPATIBILIDAD.md`.

### 1. Alcance y arquitectura mínima

Una familia, un padre y un hijo en v1. Dos APK Kotlin comparten una biblioteca `core-protocol`. El padre conserva su clave privada, configuración y una única intención pendiente. El hijo conserva su clave privada de identidad, la clave pública del padre, restricciones y estado actual. Un proceso Node.js + TypeScript + `ws` en Render conecta ambas partes mediante WSS.

No hay Firebase, base de datos, Redis, colas persistentes, historial, analítica, login con correo ni panel web en v1. Docker no es necesario. El relay conserva **configuración pública estable** en variables de entorno y conexiones temporales en RAM. Eso no es una base de datos ni contiene la clave privada del padre. Sin esta raíz de confianza no se podría autenticar a los clientes tras reiniciar.

El relay usa exactamente una instancia y un proceso; no hay clúster ni escalado horizontal. Puede desconectarse por despliegue o fallo. Los móviles reconectan. El padre necesita la aplicación abierta y autenticada para enviar una orden; el hijo no necesita que el padre esté conectado para ejecutar una política ya aceptada.

### 2. Semántica del producto

#### 2.1 Órdenes

| Acción | Estado solicitado al aceptar | Qué ocurre después |
|---|---|---|
| `LOCK` | Bloqueado | Permanece así hasta nueva autorización |
| `LOCK_FOR` | Bloqueado | Al vencer, permite uso solo con red/canal válidos y en el mismo arranque autorizado |
| `ALLOW` | Permitido | Uso permitido mientras las condiciones locales lo permitan |
| `ALLOW_FOR` | Permitido | Al vencer, vuelve a bloqueado |

`LOCK_FOR` y `ALLOW_FOR` requieren `durationSec` entero entre 60 y 604800 (1 minuto a 7 días). `LOCK` y `ALLOW` requieren `durationSec: null`. Una nueva orden sustituye la anterior: no se acumulan temporizadores. Las duraciones comienzan en la primera aceptación duradera por el hijo, no al pulsar el padre ni al recibir el relay. El ACK devuelve el tiempo restante real.

El bloqueo significa quiosco con pantalla del hijo y acceso de emergencia del sistema; no apagar telefonía, internet ni el servicio controlador. Permitir significa acceso a las aplicaciones expresamente aprobadas durante configuración; **no devolver acceso libre a Ajustes ni retirar Device Owner**. La selección local inicial de aplicaciones autorizadas es parte necesaria del quiosco, no un sistema de seguimiento de aplicaciones.

#### 2.2 Condiciones con prioridad

```text
Si no está aprovisionado o vinculado: CONFIGURACION, no apto para entrega.
Si falla integridad o aplicación de restricciones: ERROR_SEGURO, mantener bloqueo.
Si está antes del primer desbloqueo Android: ARRANQUE_BLOQUEADO.
Si hay mantenimiento presencial autorizado y credenciales Android disponibles: solo herramientas de mantenimiento.
Si no hay autorización aplicable a este arranque: ARRANQUE_BLOQUEADO.
Si falta red validada tras margen: BLOQUEADO_SIN_RED.
Si el heartbeat del relay venció: BLOQUEADO_SIN_CANAL.
Si la política temporal vigente pide bloquear: BLOQUEADO_POR_PADRE.
En los demás casos: PERMITIDO dentro del quiosco.
Emergencia: siempre accesible por el sistema, independientemente de la rama anterior.
```

Cada arranque nuevo queda bloqueado incluso si antes estaba permitido. **Volver a tener internet no elimina ese bloqueo de arranque**: el padre debe enviar una nueva autorización para ese `bootId`. `ALLOW`, `ALLOW_FOR` y `LOCK_FOR` autorizan explícitamente ese arranque; `LOCK` no lo libera. No se acepta una orden pendiente destinada al arranque anterior.

En una desconexión sin reinicio, la política y el temporizador siguen vigentes. Cuando vuelve la red y un heartbeat nuevo demuestra el canal, se reevalúa la misma política; solo vuelve a permitido si no queda ninguna otra causa de bloqueo. Esto permite que el teléfono del padre esté apagado después de emitir una autorización válida.

#### 2.3 Valores iniciales

| Parámetro | Valor v1 | Regla |
|---|---|---|
| Heartbeat de aplicación | Cada 15 segundos | Un solo reloj de reconexión; no bucle ocupado |
| Caducidad del canal | 45 segundos desde último PONG válido | No depende de presencia del padre |
| Margen al perder red validada | 3 segundos | Local; tolera cambio breve Wi-Fi/datos |
| Timeout de autenticación WSS | 10 segundos | El relay cierra conexiones sin autenticar |
| Reconexión | 1, 2, 4, 8, 16, 30 segundos con jitter | Reiniciar al recuperar red; nunca múltiples sockets |
| Espera inicial de ACK | 5 segundos | Después: «sin confirmar», no «falló» definitivo |
| Frescura de emisión de una orden | 30 segundos | Desafío emitido por hijo; reloj monotónico local |
| Ventana de enrolamiento QR | 5 minutos visibles | Medida localmente por el hijo |
| Mantenimiento local | 5 minutos | Sin acceso a apps ordinarias; reinicio lo cancela |
| Frame WSS | Máximo 16384 bytes | Sin compresión ni binarios |
| Payload firmado decodificado | Máximo 8192 bytes | Validar antes de procesar |

Objetivo de latencia del proyecto: desde recepción y validación de una orden en un hijo activo hasta aplicar el bloqueo, ≤1 segundo en el A13 de pruebas; objetivo extremo a extremo p95 ≤3 segundos con ambos clientes activos y red estable. Son métricas por medir, **no garantías con internet interrumpido, Doze o proceso suspendido**. Ante wake/resume se evalúa la política antes de habilitar interacción; demostrarlo con pruebas de procesos y otras apps.

### 3. Identidades y datos

- `pairId`, `bootId`, `childSessionId`, `connectionId`, `commandId`: UUID v4 en minúsculas.
- `nonce`, `syncNonce`, `commandNonce`, `enrollNonce`: 32 bytes aleatorios, base64url sin `=`.
- `seq`: entero positivo de 64 bits representado como **cadena decimal**. Rango 1..9223372036854775807; sin signo, espacios ni ceros iniciales. Estado sin órdenes usa `lastAcceptedSeq: "0"`.
- `stateSeq`: contador de estado de la sesión del hijo, también cadena decimal. Se reinicia solo al cambiar `childSessionId`.
- Tiempos de duración: segundos enteros. Reloj de medición: `elapsedRealtime()` en hijo, reloj monotónico en relay. No se usan fechas de pared para desbloquear.
- Nombres visibles y datos del menor son locales. El protocolo lleva identificadores aleatorios, modos y códigos, no nombre, edad, teléfono ni ubicación.

El hijo genera un `bootId` persistido por arranque, no por cada callback. `LOCKED_BOOT_COMPLETED` y `BOOT_COMPLETED` pertenecen al mismo arranque y no deben crear dos identidades. Se contrasta el contador de arranque del sistema y el estado guardado; si no se puede probar continuidad, se adopta un nuevo contexto bloqueado. Un simple reinicio del proceso no reinicia el contador de órdenes ni el temporizador de la política.

El hijo crea un `childSessionId` nuevo cada vez que autentica una conexión WSS. Las órdenes nuevas se dirigen a ese identificador. Reconectar no borra la política existente, pero invalida órdenes nuevas dirigidas a la sesión anterior.

### 4. Firma y serialización interoperable

Se utilizan APIs criptográficas estándar, no una implementación propia de curvas. Claves EC P-256 (`secp256r1`), SHA-256 y ECDSA; firmas **ASN.1 DER**, no el formato de 64 bytes `r || s` de JOSE. Clave pública: SPKI DER codificada en base64url. En Android se usa `SHA256withECDSA`; en Node `crypto.sign/verify('sha256', ..., {dsaEncoding:'der'})`. No llamar ES256/JWT a este contenedor.

Contenedor externo:

```json
{"v":1,"kind":"SIGNED","purpose":"MESSAGE","payloadB64":"BASE64URL_DE_PAYLOAD","sigB64":"BASE64URL_DE_FIRMA_DER"}
```

Las cadenas anteriores son marcadores explicativos, no un mensaje aceptable. Los vectores de `pruebas/vectores_crypto.json` sí incluyen firmas verificables.

Bytes que se firman:

```text
UTF8("CPv1/" + purpose + "\n") || payloadBytes
```

Propósitos válidos: `AUTH`, `MESSAGE`, `PAIR`, `OFFLINE`. Un mensaje firmado para un propósito no puede usarse para otro.

**Serialización determinista restringida:** objetos con claves ASCII ordenadas lexicográficamente; arrays conservan orden; sin espacios, saltos finales, claves duplicadas ni propiedades desconocidas. Strings de protocolo ASCII imprimible, sin controles. Números: solo enteros seguros, sin exponentes, negativos ni decimales; contadores grandes van como strings. `null` es explícito en los campos opcionales aquí definidos. Para todos los mensajes, el emisor genera estos bytes una sola vez. El receptor verifica la firma sobre los bytes recibidos, después valida UTF-8 estricto, esquema y que recanonizar produzca exactamente los mismos bytes; así rechaza también claves duplicadas y variantes ambiguas. No verifica una firma sobre un JSON reconstruido distinto.

El contenedor externo se valida estructuralmente; ningún campo externo decide autoridad por sí solo. Nunca se acepta `alg` elegible por el cliente ni una clave pública enviada en un comando como sustituto de la clave vinculada.

### 5. Vinculación física inicial, sin servidor de cuentas

1. El hijo, todavía en configuración presencial, genera su clave de identidad no exportable. Muestra un QR `CHILD_OFFER` con `childPublicKeyB64`, `enrollNonce` y `enrollId` aleatorios. El QR no contiene secretos.
2. El padre genera su clave de autoridad en Keystore, protegida por autenticación del dueño del teléfono. Escanea la oferta y crea un `pairId` nuevo. La aplicación solicita el origen del relay (`https://nombre.onrender.com`); nunca acepta `http` en release.
3. El padre genera tres códigos de recuperación diferentes de 160 bits y prepara sus hashes con propósito. Los muestra para guardarlos fuera del teléfono. No los transmite; solo se vinculan sus hashes.
4. El padre presenta un QR `PAIR_ACCEPT` firmado con propósito `PAIR`: `pairId`, `enrollId`, `enrollNonce`, ambas claves públicas, `relayOrigin` y los tres hashes. El hijo lo escanea, verifica coincidencia con su oferta vigente, firma y confirmación física del adulto en ambos equipos. Cada pantalla muestra el mismo resumen de huellas de claves para comparar. Una vez vinculado, esta pantalla no puede cambiar claves sin recuperación autorizada.
5. El padre exporta únicamente configuración **pública** para Render: `ACTIVE_PAIR_ID`, `PARENT_PUBLIC_KEY_B64`, `CHILD_PUBLIC_KEY_B64`, `PUBLIC_ORIGIN`. El desarrollador la coloca en el servicio. Las claves privadas y códigos no se exportan.
6. Padre e hijo prueban autenticación con el relay. La vinculación no se da por terminada hasta recibir estado firmado del hijo y comprobar una recuperación local en entorno de prueba.

Este paso manual evita un sistema de registro, usuarios y base de datos. No se entrega una credencial de administración de Render a ninguna app. El QR de aprovisionamiento Android para instalar un DPC es **otro QR distinto** del QR que vincula padre e hijo. Si se utiliza ese aprovisionamiento se implementan sus handlers oficiales [S18, S19].

### 6. Conexión y autenticación del relay

Endpoint único: `wss://<servicio>/ws`, subprotocolo `cp.v1`. Endpoint de salud: `GET /healthz`, sin datos de la familia. Render termina TLS; Node escucha HTTP en `0.0.0.0:$PORT` internamente [S11, S13].

Al conectar, el relay manda:

```json
{"v":1,"kind":"CHALLENGE","connectionId":"00000000-0000-4000-8000-000000000001","nonce":"NONCE_ALEATORIO","timeoutSec":10}
```

El cliente responde contenedor firmado `AUTH` cuyo payload es:

```json
{"connectionId":"00000000-0000-4000-8000-000000000001","kind":"AUTH_PROOF","nonce":"NONCE_ALEATORIO","pairId":"00000000-0000-4000-8000-000000000002","role":"PARENT","v":1}
```

El relay compara `pairId`, selecciona la clave configurada del rol (`PARENT` o `CHILD`), verifica firma y desafío no usado de esa conexión, y devuelve `AUTH_OK`. No acepta claves propuestas por el cliente. El desafío se consume en RAM. Solo después se incorpora el socket al mapa de la pareja.

Hay máximo un socket por rol. Una conexión nueva **ya autenticada** sustituye a la anterior; una conexión sin autenticar no puede expulsar a nadie. Un desafío antiguo no sirve después de reiniciar Render ni de abrir otro socket.

El padre requiere autenticación local para usar su clave. Su conexión solo se mantiene mientras su interfaz esté activa; si una reconexión requiere nueva autenticación, se pide de nuevo. No se debilita la protección de la clave para lograr envíos en segundo plano.

### 7. Sincronización, emisión y confirmación

#### 7.1 Obtener estado fresco

El padre crea `syncNonce` nuevo y envía `SYNC_REQUEST` firmado con `pairId`, `sender:"PARENT"`, `v:1`, `syncNonce`. No consume `seq`.

El hijo responde `STATE` firmado con la misma pareja y ese `syncNonce`, su `bootId`, `childSessionId`, `stateSeq`, `lastAcceptedSeq`, `lastCommandId`, `lastCommandPayloadSha256B64`, `lastResult`, `mode`, `reason`, `remainingSec`, `networkValidated`, `channelHealthy`, `isDeviceOwner`, `lockTaskActive` y un **nuevo `commandNonce` válido 30 segundos en su reloj monotónico**. El hijo conserva ese desafío en RAM para esa sesión. Cada sincronización reemplaza el anterior. Si no hubo orden previa, id/hash son null y `lastResult:"NONE"`.

El padre acepta el estado solo con firma de la clave del hijo y `syncNonce` de su sesión actual; en actualizaciones posteriores exige `stateSeq` creciente y la misma sesión del hijo. Al reconectar solicita nueva sincronización. La presencia indicada por el relay no reemplaza este proceso.

#### 7.2 Enviar una política

```json
{
  "action":"LOCK_FOR",
  "bootId":"00000000-0000-4000-8000-000000000003",
  "childSessionId":"00000000-0000-4000-8000-000000000004",
  "commandId":"00000000-0000-4000-8000-000000000005",
  "commandNonce":"NONCE_EMITIDO_POR_HIJO",
  "durationSec":1800,
  "kind":"SET_POLICY",
  "pairId":"00000000-0000-4000-8000-000000000002",
  "sender":"PARENT",
  "seq":"7",
  "v":1
}
```

El ejemplo está expandido para lectura. Para firmarlo debe serializarse según la sección 4. El padre reserva `seq = max(contadorLocal, lastAcceptedSeqVerificado) + 1` de forma duradera, guarda la intención y el comando firmado, y lo envía. Un contador reservado nunca se reutiliza para una orden distinta. Se permiten saltos para recuperar cierres inesperados.

El hijo valida firma, esquema, pareja, rol, arranque y sesión. Para una orden nueva exige `seq > lastAcceptedSeq`, `commandNonce` pendiente y no vencido. Consume el desafío y persiste aceptación, hash del **payload**, id y tiempo de inicio de forma atómica antes de ampliar permisos. La firma DER puede tener representaciones distintas; **no usar hash de la firma para detectar duplicados**.

El comando exactamente igual al último aceptado, con mismo id, seq y hash de payload, es idempotente: devuelve el resultado actual sin reiniciar duración ni exigir nuevamente el desafío ya consumido. Esta excepción solo se acepta si bootId y childSessionId del comando siguen siendo los actuales, y solo consulta el último registro. Tras una reconexión o un nuevo arranque se responde STALE_SESSION/STALE_BOOT y se reconcilia mediante STATE; nunca se reaplica la política antigua como una autorización nueva. El mismo seq con otro id o hash se rechaza. Un comando antiguo desconocido se rechaza.

#### 7.3 ACK real

`ACK` está firmado por el hijo y contiene `pairId`, `sender:"CHILD"`, `bootId`, `childSessionId`, `commandId`, `seq`, `commandPayloadSha256B64`, `result`, `mode`, `reason`, `remainingSec` y `v:1`.

Valores de `result`:

- `APPLIED`: la intención está persistida y el controlador comprobó las restricciones efectivas; `mode` puede seguir bloqueado por red o por el PIN inicial de Android. La interfaz explica la causa.
- `ACCEPTED_PENDING_ENFORCEMENT`: existe aceptación duradera pero todavía no prueba suficiente de aplicación. No mostrar «Aplicado».
- `ENFORCEMENT_FAILED`: no se pudo comprobar la aplicación. El hijo intenta mantener bloqueo y el padre muestra error.
- `REJECTED`: no se aceptó ni aplicó esa orden. El campo `reason` explica el código permitido.

`STATE.lastResult` describe solo la última orden aceptada: `APPLIED`, `ACCEPTED_PENDING_ENFORCEMENT`, `ENFORCEMENT_FAILED` o `NONE` si no existe aceptación. Un rechazo posterior no sobrescribe ese registro. `mode` es `LOCKED`, `ALLOWED`, `MAINTENANCE` o `SETUP`. `reason` usa los códigos del esquema. `remainingSec` es null si no existe temporizador, o entero ≥0. No se usa un mensaje de Render para marcar `APPLIED`.

Si el ACK se pierde, el padre retransmite exactamente el mismo comando como máximo dos veces mientras la sesión siga siendo la misma. Luego solicita estado fresco. Solo reconoce la aplicación previa si el estado firmado incluye id, seq y hash coincidentes. No crea una nueva orden con nueva duración por el simple hecho de perder el ACK.

#### 7.4 Órdenes pendientes y cierre del padre

La aplicación guarda una única **intención pendiente**, no una cola histórica. Si no hay hijo conectado no puede firmar un comando nuevo con sesión y desafío válidos. Muestra «Pendiente local; abre esta app cuando el hijo tenga conexión».

Después de reconectar o reiniciar el hijo, no reproduce un desbloqueo firmado antiguo. Sincroniza, reconcilia el último resultado y pide confirmación al padre para emitir otra intención con nuevos identificadores de sesión, desafío y secuencia. Esto también evita desbloqueos diferidos sorpresivos. Ninguna orden tiene garantía de entrega tras cerrar el padre.

### 8. Heartbeats y disponibilidad

Después de `AUTH_OK`, cada cliente envía `PING` de aplicación con nonce aleatorio; el relay devuelve `PONG` con el mismo nonce. El hijo usa solo respuesta a su desafío vigente, en la conexión autenticada actual, como latido. Las tramas de control ping/pong WebSocket pueden usarse para limpiar sockets, pero no reemplazan ese estado de aplicación.

Red validada y canal sano son condiciones independientes. `NetworkCallback` actualiza la primera [S09]. PONG actualiza la segunda. Una respuesta HTTP 200, DNS resuelto, socket abierto o icono Wi-Fi no demuestran un canal autenticado. Captive portal, DNS roto o TLS inválido no permiten uso normal. Si Android pausa el proceso, al reanudarse se calcula el tiempo transcurrido antes de permitir interacción; no se reinicia el margen desde cero.

El relay emite `PEER_UNAVAILABLE` cuando no hay receptor; no almacena el mensaje. Los errores del relay son informativos y nunca autorizan al hijo a liberar el teléfono.

### 9. Recuperación y retirada

No existe PIN maestro fijo en código ni endpoint de desbloqueo administrativo en Render. Durante la vinculación se crean tres códigos independientes, cada uno con 20 bytes aleatorios mostrados como Base32 de 32 caracteres, agrupados para lectura. Se admite quitar guiones/espacios y normalizar mayúsculas; se rechaza cualquier otra transformación.

Hash almacenado en hijo:

```text
SHA256(UTF8("CPv1/RECOVERY\n" + pairId + "\n" + purpose + "\n" + codigoBase32Normalizado))
```

Propósitos: `MAINTENANCE`, `RECOVER_PARENT`, `RETIRE`. Solo se almacenan hashes y bits de consumo, en el archivo transaccional del hijo. La comparación es de tiempo constante. Cada código se consume duraderamente antes de habilitar su función. No se envía al relay ni se registra.

- `MAINTENANCE`: habilita durante 5 minutos únicamente herramientas propias de red y actualización firmada del propio controlador (SPEC-H H11). No concede uso normal sin internet.
- `RECOVER_PARENT`: permite reemplazar presencialmente al padre mediante el flujo QR, crea otro `pairId`, nuevas claves del padre y nuevos códigos, conserva bloqueo y revoca localmente la clave vieja. Exige actualizar las claves públicas de Render antes de retomar conexión. El propietario conserva acceso a su cuenta de Render por una vía independiente.
- `RETIRE`: abre la retirada local, explica que se restablecerá el teléfono, exige confirmación adicional y el texto `BORRAR`. Solo después solicita el restablecimiento mediante la API aplicable. No se expone como comando remoto.

Alternativa habitual sin gastar un código impreso: el hijo muestra un desafío QR nuevo firmado con su clave de identidad y propósito `OFFLINE` y acción `MAINTENANCE` o `RETIRE`, ligado a pairId, bootId y nonce vigente 5 minutos. El padre lo escanea sin internet, verifica la firma con la pública del hijo vinculado, autentica al adulto y firma la respuesta con ese mismo propósito/acción/nonce. El hijo verifica con la clave pública del padre, consume el desafío y aplica exactamente la función autorizada. `RETIRE` sigue requiriendo confirmación destructiva en el hijo. No se admite `ALLOW` offline en v1: mantener la regla «sin internet, bloqueado».

Los desafíos offline se pierden al reiniciar. Los códigos impresos usados **no** vuelven a habilitarse. Cinco intentos erróneos producen espera de 60 segundos; diez, espera de 5 minutos. El número de intentos y la obligación de espera se persisten. Al reiniciar una espera pendiente se reimpone conservadoramente completa. Nunca se bloquea emergencia por fallar un código. La espera afecta solo la interfaz de códigos, no respuestas QR válidas firmadas por el padre.

### 10. Persistencia sin base de datos

Padre: archivo privado cifrado con AES-GCM y clave local de Keystore para metadatos, sin backups automáticos; alias de clave de firma, vínculo, contador y una intención pendiente. La clave privada de firma nunca se serializa. Guardar no equivale a sincronizar.

Hijo: un archivo `control-state.json` mediante `AtomicFile` en almacenamiento protegido del dispositivo y un único escritor serializado. Contiene vínculo público, hashes de recuperación, última aceptación, etapa de aplicación, temporizador y contexto de arranque. No contiene clave privada del padre ni códigos. Su clave de identidad está en Keystore; se usa después del primer desbloqueo. Si antes no puede accederse a Keystore, **se mantiene bloqueado sin intentar fingir conectividad** [S07, S10, S21].

Separar archivo de política y contador sin transacción produciría fallos de repetición; no hacerlo. No guardar duración solo en memoria. Al leer datos desconocidos o corruptos, usar `ERROR_SEGURO`, no reabrir el enrolamiento sin autorización.

### 11. Errores y evolución

Códigos mínimos: `BAD_SCHEMA`, `BAD_SIGNATURE`, `WRONG_PAIR`, `WRONG_ROLE`, `STALE_BOOT`, `STALE_SESSION`, `STALE_NONCE`, `STALE_SEQUENCE`, `SEQUENCE_CONFLICT`, `NOT_DEVICE_OWNER`, `NOT_PROVISIONED`, `BEFORE_USER_UNLOCK`, `BOOT_AUTH_REQUIRED`, `NETWORK_LOST`, `CHANNEL_EXPIRED`, `PARENT_LOCK`, `TIMER_EXPIRED`, `OK`, `MAINTENANCE_ACTIVE`, `STATE_CORRUPT`, `ENFORCEMENT_ERROR`, `KEY_UNAVAILABLE`, `PEER_OFFLINE`, `RATE_LIMITED`, `UNKNOWN_VERSION`, `SYSTEM_KEYGUARD`, `CALL_IN_PROGRESS`.

Un comando con versión desconocida se rechaza. No se agrega «compatibilidad» aceptando campos sin validación. Cualquier cambio incompatible exige CP/2, otro ADR, nuevos fixtures y cambio coordinado de ambas aplicaciones. El futuro navegador necesita su propia delegación de clave/autorización: **no puede usar automáticamente una clave privada no exportable del Android del padre**.

#### 9.A. Recuperación interrumpida y alcance del mantenimiento

El mantenimiento autorizado no concede ALLOWED ni elimina el bloqueo del arranque: puede accederse a sus herramientas limitadas aun con ese bloqueo, después de desbloquear el Android ordinario.

RECOVER_PARENT habilita una ventana presencial de 5 minutos ligada al proceso/arranque. El código se consume al abrirla. La clave vieja se revoca y el nuevo vínculo sustituye al anterior **solo en el commit atómico de la nueva vinculación**. Antes del commit, un fallo/cancelación conserva el vínculo anterior pero cierra la ventana; no permite retomarla sin nueva autorización. Si el código único ya fue consumido y se perdió el padre, se usa la vía RETIRE todavía disponible para restablecer y configurar de nuevo. Después del commit, nunca se restaura silenciosamente la clave anterior. No persistir un booleano que reabra enrolamiento libre después de reiniciar.

---

<a id="parte-12"></a>

## Parte 12 · Uso del esquema y validaciones

Archivo modular: `compartido/04_USO_ESQUEMA.md`.

## Cómo usar el esquema JSON

`protocolo.schema.json` valida un frame o un payload **ya decodificado**. No descodifica automáticamente `payloadB64`, no verifica firmas ni demuestra que una orden esté autorizada. Se valida primero el contenedor y después la definición correspondiente al `kind` de los bytes internos.

| Purpose | Payloads válidos | Clave confiable |
|---|---|---|
| AUTH | AUTH_PROOF | Pública del rol fijada en la configuración del relay |
| MESSAGE | SYNC_REQUEST, SET_POLICY | Pública del padre ya vinculada |
| MESSAGE | STATE, ACK | Pública del hijo ya vinculada |
| PAIR | PAIR_ACCEPT | Nueva pública del padre solo en enrolamiento físico vigente, ligada a la oferta del hijo |
| OFFLINE | OFFLINE_CHALLENGE | Pública del hijo vinculado |
| OFFLINE | OFFLINE_RESPONSE | Pública del padre vinculado |

CHILD_OFFER es un QR local sin firma de autoridad previa. CHALLENGE, AUTH_OK, PING, PONG, PEER_STATUS, PEER_UNAVAILABLE y ERROR son frames de transporte; jamás sustituyen una orden o confirmación firmada. CHALLENGE/AUTH_OK vienen del relay sobre WSS; los latidos solo se aceptan después de autenticar. PAIR/OFFLINE no se aceptan en el relay.

Validaciones adicionales obligatorias: tamaño en bytes, UTF-8 estricto, ASCII del subconjunto, base64url canónico sin padding, SPKI de curva P-256, DER de firma, prefijo de propósito, serialización determinista, máximo real de seq (64 bits), relación entre estado/campos, identidad, boot, sesión, nonce y secuencia. El esquema no aplica por sí mismo esas reglas.

`STATE` describe la última orden **aceptada**. Rechazar una orden posterior no cambia lastAcceptedSeq ni sobrescribe el resultado de esa aceptación. `ACK(REJECTED)` puede describir un comando nuevo inválido en contexto sin modificar ese registro. Un mensaje malformado que no permite construir un ACK seguro se descarta o produce ERROR; no se inventan campos del comando.

`OFFLINE_CHALLENGE` del hijo y `OFFLINE_RESPONSE` del padre están firmados con propósito OFFLINE; ambos contienen pairId, bootId, nonce, action y sender según el esquema. El padre valida primero la firma/pareja del desafío. El hijo conserva el desafío y su vencimiento monotónico en RAM, y lo consume antes de habilitar mantenimiento/retirada. Una foto antigua no sirve después de consumo, vencimiento o reinicio.

`AUTH_OK` tiene connectionId. `PEER_STATUS` lleva peerRole y online, solo informativos. `PEER_UNAVAILABLE` usa reason PEER_OFFLINE y refCommandId (null en una sincronización); no es un ACK del hijo. Los payloads firmados no pueden cambiar estas definiciones unilateralmente.

Los ejemplos bajo `compartido/ejemplos` son objetos válidos de prueba. Sus identidades, nonces y claves no se usan en producción. Los ejemplos se firman y verifican en `pruebas/vectores_crypto.json`.

---

<a id="parte-13"></a>

## Parte 13 · Modelo de amenazas

Archivo modular: `compartido/01_MODELO_AMENAZAS.md`.

## Modelo de amenazas y criterios de seguridad

Versión: 1.0. Aplicable al padre, al hijo y al relay. Consultar fuentes en `02_FUENTES_Y_COMPATIBILIDAD.md`.

### Activos y confianza

El activo principal es la autoridad del padre para cambiar la política. También importan la integridad del estado local, la disponibilidad de emergencias y la posibilidad de recuperar un equipo administrado sin dejarlo inutilizado.

Se confía en Android sin root, el bloqueo de pantalla del padre, Android Keystore, el aprovisionamiento físico autorizado y la cuenta de Render del propietario. El relay transporta metadatos de control y se confía en él para disponibilidad, **no para generar órdenes válidas**. El hijo acepta órdenes únicamente de la clave pública del padre vinculada físicamente.

### Amenazas incluidas

| Amenaza | Respuesta de diseño | Prueba requerida |
|---|---|---|
| Cambiar el teléfono al modo padre | Dos APK con identificadores distintos; ninguna elevación por selector | H-SEC-01 |
| Cerrar, desinstalar o borrar datos desde la interfaz normal | Device Owner, restricciones y políticas oficiales | H-SEC-02 |
| Reiniciar para recuperar acceso | Nuevo contexto de arranque y estado bloqueado; autorización fresca del padre | H-BOOT-01 |
| Quitar internet o bloquear el dominio del relay | Evaluación local de red y vencimiento del heartbeat | H-NET-01/03 |
| Alterar la hora | Duraciones con reloj monotónico; no usar el reloj de pared para liberar | H-TIME-02 |
| Reenviar un desbloqueo antiguo | Firma, pairId, sesión, bootId, secuencia y desafío de emisión | C-SEQ-01/04 |
| Suplantar un teléfono en el relay | Claves públicas fijadas en configuración; reto firmado por conexión | R-AUTH-01 |
| Inventar una confirmación | ACK firmado por el hijo, ligado al comando y al estado aplicado | P-ACK-01 |
| Modificar o truncar el archivo local | Escritura atómica, validación, estado cerrado ante corrupción | H-STATE-02 |
| Adivinar recuperación | Códigos aleatorios de 160 bits, separados por propósito, un solo uso y límite de intentos | H-REC-03 |
| Pérdida del padre o de su clave | Recuperación física previamente preparada y nuevo vínculo; no puerta trasera del servidor | H-REC-04 |
| API de administración no aplicada por el sistema | Comprobación del resultado; nunca informar éxito sin evidencia | H-ENF-03 |

### Amenazas y garantías excluidas

No se promete resistencia a reflasheo, extracción física avanzada, vulnerabilidades del sistema, APK de desarrollo manipulado, cuenta del padre comprometida, poseedor de códigos de recuperación o control administrativo de Render. No se promete tiempo real estricto cuando el sistema no ejecuta la app.

Un relay malicioso puede ocultar órdenes, interrumpir conexiones, mentir sobre presencia o responder latidos mientras descarta mensajes. Las firmas impiden inventar un desbloqueo, pero no solucionan esa denegación/selectividad. La interfaz distingue estado verificado, estado antiguo y disponibilidad del relay. WSS protege cada conexión, pero **no es cifrado de extremo a extremo**: Render puede ver modo, duración y mensajes mínimos de control. No se transmiten claves privadas, códigos de recuperación, contenido de aplicaciones, ubicación, contactos ni historial.

### Reglas de seguridad para implementar

La pantalla de emergencia no depende del servidor y no debe quedar detrás de un PIN parental. No se ocultan la administración ni la notificación del servicio. No se añaden grabación, captura de pantalla, lectura de chats, seguimiento, accesibilidad ni permisos «por si acaso».

Los APK de prueba nunca se entregan al niño como versión final. No se añade borrado remoto automático. La retirada del dispositivo exige presencia física, autorización explícita y dos confirmaciones de pérdida de datos.

Los logs de pruebas del desarrollador no son un historial de uso del niño: se generan con datos ficticios o códigos técnicos, y nunca incluyen firmas completas, QR, claves, códigos, políticas completas ni identificadores innecesarios. En producción solo se usan códigos de error no identificables; los logs de infraestructura de Render pueden existir aunque la app no guarde historial.

### Puertas de salida

**G0:** emergencias y quiosco en el A13 real. **G1:** firma, vinculación y recuperación. **G2:** reinicio/red/reloj y ejecución local. **G3:** padre/relay/hijo integrados. **G4:** batería, actualización, proceso muerto y pruebas de evasión de interfaz.

Si una puerta falla, no se certifica el modo estricto para ese dispositivo. Se documenta el fallo y se corrige la implementación o se declara incompatibilidad; no se sustituye por una pantalla que aparenta bloquear.

---

<a id="parte-14"></a>

## Parte 14 · Fuentes y compatibilidad

Archivo modular: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`.

## Fuentes, compatibilidad y límites de la especificación

Versión documental: 1.0 · Fecha de consulta: 8 de octubre de 2026.
Estado: diseño para implementar; no existe certificación del teléfono ni una aplicación probada en este paquete.

### 1. Qué sabemos y qué no

El propietario identificó el teléfono del hijo como **A13**. Se toma como candidato un Samsung Galaxy A13, pero **no conocemos su código SM-…, versión de Android, One UI, operador ni parche de seguridad**. No se debe completar ese inventario inventando datos. Samsung publica documentación Android 14 para variantes SM-A135M y SM-A135F [S14]; eso no demuestra que el teléfono concreto la tenga instalada.

«Cualquier celular» se traduce en una arquitectura portable y una matriz de pruebas, no en una garantía imposible de verificar. Device Owner y Lock Task pertenecen a Android [S01, S02]. Este diseño no implementa control del hijo en iPhone. El panel web futuro podría administrar un hijo Android, pero no convierte un iPhone en un dispositivo Android administrado.

Decisión del proyecto, no mínimo universal de las APIs:

| Componente | Base elegida | Estado inicial |
|---|---|---|
| Padre | Android 11 / API 30 o superior | Por implementar y probar |
| Hijo | Android 12 / API 31 o superior | Por implementar y probar |
| Hijo A13 Android 12–14 | Primera familia a validar | No certificado todavía |
| Otros fabricantes y Android 15–17 | Candidatos, con pruebas específicas | No prometer compatibilidad |
| iPhone del hijo | Fuera de alcance | No soportado por este diseño |
| Navegador del padre | Evolución posterior | No implementado en v1 |

Se fija `compileSdk = 36`, `targetSdk = 36` como base deliberada del proyecto, no como afirmación de que sea la última API disponible. No se bajará el target para eludir restricciones. Los cambios de servicios en primer plano y comportamiento se revisan por versión [S05, S06, S24]. Las versiones concretas de Gradle, Kotlin, Android Gradle Plugin y dependencias se fijan al crear el proyecto desde Android Studio estable y se conservan en el repositorio.

### 2. Límites que no deben ocultarse

- El quiosco no reemplaza el cargador de arranque, la pantalla del PIN de la SIM ni el primer desbloqueo de Android.
- No se promete sobrevivir a un borrado por recovery, reinstalación del sistema, root, cargador de arranque desbloqueado ni fallos del fabricante. Restringir el restablecimiento desde Ajustes no elimina todas las vías físicas de restablecimiento [S03, S04].
- Una notificación o un WebSocket no son una garantía de ejecución continua. Hay que comprobar suspensión, reinicio del proceso y pantalla apagada [S05–S08].
- Emergencias accesibles no significa cobertura garantizada: la disponibilidad depende también del sistema de telefonía, radio, operador y estado del equipo. Nunca se exige internet al botón de emergencia [S04, S15].
- Las APIs oficiales de Device Owner son la base; no se usarán accesibilidad, superposiciones engañosas, ocultación de la app ni APIs privadas para simular control total.
- Sin servidor persistente de mensajes no se entrega una orden pendiente cuando el padre ya cerró su aplicación. El padre debe volver a abrirla, autenticarse y sincronizar.
- Solo se declara un dispositivo compatible después de aprobar las puertas de aceptación de este paquete. **Los campos de evidencia se entregan vacíos a propósito.**

### 3. Fuentes primarias

Los identificadores [Sxx] usados en el paquete apuntan a estas referencias. Los valores de intervalos, estados, límites y decisiones arquitectónicas son propuestas de este proyecto, no garantías de los proveedores.

| ID | Fuente oficial | Aplicación en este diseño |
|---|---|---|
| S01 | https://developer.android.com/work/dpc/dedicated-devices | Dispositivos dedicados y DPC |
| S02 | https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode | Lock Task, lista de paquetes y diferencia con fijar pantalla |
| S03 | https://developer.android.com/reference/android/app/admin/DevicePolicyManager | APIs de administración, propietario, quiosco, borrado y retirada |
| S04 | https://developer.android.com/reference/android/os/UserManager | Restricciones del usuario; excepción de llamadas de emergencia |
| S05 | https://developer.android.com/develop/background-work/services/fgs/service-types | Tipo `systemExempted` y sus condiciones, entre ellas Device Owner |
| S06 | https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start | Excepciones y restricciones de inicio desde segundo plano |
| S07 | https://developer.android.com/privacy-and-security/direct-boot | Almacenamiento y componentes disponibles antes del desbloqueo |
| S08 | https://developer.android.com/training/monitoring-device-state/doze-standby | Suspensión, red y pruebas de Doze |
| S09 | https://developer.android.com/develop/connectivity/network-ops/reading-network-state | Callbacks y capacidades de red validadas |
| S10 | https://developer.android.com/privacy-and-security/keystore | Claves no exportables y autenticación del usuario |
| S11 | https://render.com/docs/websocket | WSS, conexiones, reinicios y reconexión |
| S12 | https://render.com/docs/free | Suspensión y límites de los servicios gratuitos |
| S13 | https://render.com/docs/web-services | Puerto de escucha y despliegue de un web service |
| S14 | https://www.samsung.com/latin/support/model/SM-A135MZKNTPA/ ; https://www.samsung.com/levant/support/model/SM-A135FZKGMEB/ | Identificación de variantes y manuales disponibles |
| S15 | https://developer.android.com/reference/android/telecom/TelecomManager | Telefonía del sistema |
| S16 | https://android.googlesource.com/platform/frameworks/base.git/+/master/telecomm/java/android/telecom/TelecomManager.java | `createLaunchEmergencyDialerIntent` está marcado `@SystemApi` / `@hide`: no usarlo como API pública |
| S17 | https://developer.android.com/work/dpc/dedicated-devices/cookbook | Launcher persistente y aprovisionamiento de desarrollo con ADB |
| S18 | https://source.android.com/docs/devices/admin/provision | Aprovisionamiento administrado |
| S19 | https://developer.android.com/work/versions/android-12 | Handlers de aprovisionamiento requeridos desde Android 12 |
| S20 | https://developer.android.com/reference/android/os/SystemClock | Reloj monotónico para duraciones |
| S21 | https://developer.android.com/reference/android/util/AtomicFile | Escritura de archivos con sustitución atómica |
| S22 | https://developer.android.com/reference/android/net/wifi/WifiManager | Configuración de Wi-Fi; restricciones y excepciones para administración |
| S23 | https://nodejs.org/en/about/previous-releases | Selección de Node.js 24 LTS |
| S24 | https://developer.android.com/about/versions/16/behavior-changes-16 | Base target API 36 |
| S25 | https://nodejs.org/api/crypto.html | Firmas ECDSA, SPKI y codificación DER; verificar contra Node 24 al implementar |
| S26 | https://github.com/websockets/ws | Biblioteca WebSocket del relay y latidos |

### 4. Evidencia que debe completar el desarrollador

No completar «APROBADO» por haber leído documentación o por pasar pruebas unitarias. Registrar: código exacto del equipo, Android/API, One UI, parche, versión y firma del APK, fecha, caso ejecutado, resultado observado y limitación. Las pruebas de un emulador no certifican llamadas ni comportamiento de batería del A13.

---

<a id="parte-15"></a>

## Parte 15 · Tareas comunes C00–C02

Archivo modular: `compartido/03_INICIO_TECNICO.md`.

## Inicio técnico común — tareas C00 a C02

### C00 — Crear el repositorio y fijar herramientas

**Entrada:** README, SPEC-P P2 y SPEC-H H2. **Salida:** repo con `android/core-protocol`, `app-parent`, `app-child`, `relay` y `docs`.

Crear proyecto en Android Studio estable. Incorporar módulos y applicationId del contrato. Registrar en `docs/BUILD_ENV.md` versiones reales de Android Studio, JDK, Gradle, AGP y Kotlin generadas por el IDE; no inventar combinaciones de versiones. Instalar SDK 36 y fijar minSdk de cada app. Añadir `.gitignore` para `.env`, keystores, APK, `local.properties`, secretos y salidas de build. En relay fijar Node 24 y lockfile cuando se añadan dependencias.

**Prueba:** compilan ambos APK vacíos y el test del núcleo; no declarar que existe bloqueo por ello. **Criterio de cierre:** comandos de build registrados y repetibles desde checkout limpio. No configurar Device Owner sobre el teléfono diario aún.

### C01 — Modelos, validación y serialización

**Entrada:** CP/1 secciones 3–8, `protocolo.schema.json`. **Archivos principales:** DTO, CanonicalJsonCodec, ProtocolValidator y sus tests.

Crear modelos sellados por `kind`/acción. Validar enteros seguros, seq como cadena de 64 bits, UUID/nonce, unknown fields y límites. La serialización es determinista del subconjunto ASCII definido, ordena objetos recursivamente y preserva arrays. Separar el contenedor externo y el payload. Nunca tratar un JSON recibido como una clase válida sin comprobar tipos/campos.

**Pruebas:** reordenación de propiedades, `null`, campos extra, valores fuera de rango, cero inicial en seq, claves duplicadas, bytes UTF-8 inválidos, base64url inválido, payload y frame demasiado grandes. Dado un mismo modelo, Android/JVM/Node producen idénticos bytes. **Cierre:** un único contrato implementado, no dos versiones parecidas.

### C02 — Firma, verificación y fixtures

**Entrada:** CP/1 sección 4, vectores de `pruebas/`. **Archivos:** interfaces Signer/Verifier, implementación JCA y pruebas cruzadas.

Verificar SPKI P-256, SHA256withECDSA y DER. Las privadas de los teléfonos van en Keystore; el núcleo recibe una interfaz, no rutas de claves privadas. No escribir una curva criptográfica propia. Usar bytes exactos y prefijo de propósito. Hash de idempotencia sobre payload, nunca sobre firma.

**Pruebas:** ejecutar `node pruebas/verificar_vectores.mjs`; luego verificar esos mismos fixtures desde tests JVM/Android. Un bit cambiado, otra clave, otro propósito, otra pareja o esquema inválido se rechazan por la capa pertinente. Una firma válida **no** significa que la política esté autorizada: faltan sesión, nonce, boot y secuencia.

**Cierre:** interoperabilidad documentada. La verificación de fixtures incluida en este paquete valida ejemplos, no sustituye pruebas del APK.

### Convenciones de trabajo

Una tarea por conversación de IA. No generar toda la app de una vez. Crear un commit después de prueba verificable y actualizar `ESTADO.md`. Si el cambio necesita más de cuatro archivos complejos, dividirlo manteniendo el mismo contrato. El límite de contexto de la IA no justifica borrar tests ni sustituir seguridad por un mock de producción.

---

<a id="parte-16"></a>

## Parte 16 · Trazabilidad de requisitos

Archivo modular: `TRAZABILIDAD.md`.

## Trazabilidad del objetivo a implementación y evidencia

| Requisito del propietario | Especificación principal | Decisión | Evidencia mínima |
|---|---|---|---|
| Control remoto desde su móvil | SPEC-P P3/P5; SPEC-R R3/R4; CP/1 §6–7 | ADR-P-003/004 | P-ACK-01/02/03, R-AUTH-01, H-ENF-02 |
| Bloquear tras reiniciar | SPEC-H H6/H8; CP/1 §2.2 | ADR-H-002/005 | H-BOOT-01/02/03 |
| Bloquear sin internet | SPEC-H H7; CP/1 §8 | ADR-H-002/003 | H-NET-01/02/03/04 |
| Duración de bloqueo elegida por el padre | SPEC-P P3/P5; SPEC-H H6 | ADR-H-005 | H-TIME-01/02/03, C-SEQ-01 |
| Restringir desinstalación y salidas | SPEC-H H4/H5 | ADR-H-001 | H-SEC-01/02/03 |
| Retirada solo autorizada | SPEC-H H10/H11; CP/1 §9/9.A | ADR-P-005 | H-REC-01/02/03/04, H-RET-01 |
| Emergencias disponibles | SPEC-H H9; INSTALACION_A13 | ADR-H-004 | H-EMG-01/02/03/04, puerta G0 |
| Datos y clave en móviles | SPEC-P P4; SPEC-H H8 | ADR-P-002, ADR-H-005 | P-KEY-01, P-STORE-01, H-STATE-01/02 |
| Render sin base de datos ni historial | SPEC-R R1/R2/R4 | ADR-P-003 | R-CONFIG-01, R-ROUTE-01, R-FAIL-01 |
| Mínimo consumo de infraestructura | SPEC-R R1/R5; SPEC-H H7 | ADR-P-003, ADR-H-003 | R-LIMIT-01/02, H-POWER-02 |
| Resistencia a órdenes falsas/repetidas | CP/1 §3–7 | ADR-P-002/004, ADR-H-005 | C-CRYPTO-01, C-SCHEMA-01, C-SEQ-01/02/03/04 |
| Primer objetivo A13; portabilidad | Fuentes/compatibilidad; FICHA_DISPOSITIVO | ADR-H-006 | G0–G4 en el modelo exacto, sin certificación extrapolada |
| Navegador en el futuro | SPEC-P P8 y contrato versionado | ADR-P-001 | Fuera de v1; requiere ADR de delegación de autoridad |
| Fácil de dividir para IA pequeña | Inicio técnico, AGENTS y planes | Organización documental | Una tarea, tests reales y HANDOFF por sesión |

Los sufijos compactos `H-EMG-01/02/...` se refieren a los IDs completos que aparecen en `pruebas/MATRIZ_ACEPTACION.md`. No son pruebas ejecutadas.

---

<a id="parte-17"></a>

## Parte 17 · Matriz de aceptación

Archivo modular: `pruebas/MATRIZ_ACEPTACION.md`.

## Matriz de aceptación — CP/1

Todos los casos del producto se entregan **PENDIENTES**. Completar evidencia con versión, entorno, resultado observado y archivo/commit. La verificación de fixtures adjunta es documental/criptográfica; no aprueba estos casos de la aplicación.

### Puertas

G0 = administración/quiosco y emergencia en A13. G1 = identidad, protocolo y recuperación. G2 = persistencia, tiempo, arranque y red. G3 = integración padre/relay/hijo. G4 = proceso muerto, energía, escapes y actualización en release. No omitir un caso crítico porque el emulador no lo soporte. Una llamada real de emergencia no se realiza sin coordinación formal.

### Contrato y seguridad criptográfica

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| C-CRYPTO-01 | **Firma válida y verificación cruzada.** Firmar/verificar fixtures P-256 DER en JVM, Android y Node. | Se aceptan bytes originales y se rechazan cambios/clave/propósito incorrectos. | Automática | PENDIENTE |
| C-SCHEMA-01 | **Parser estricto.** Probar campos extra, UTF-8/base64 inválidos, duplicados, números y tamaños fuera de límites. | Todos se rechazan antes de aplicar políticas. | Automática | PENDIENTE |
| C-PAIR-01 | **Vinculación física.** QR vencido, clave del hijo diferente, otra pareja, cancelación y segunda vinculación sin permiso. | No cambian claves; solo oferta física vigente y confirmada crea vínculo. | Automática + A13 | PENDIENTE |
| C-SEQ-01 | **Repetición exacta.** Reenviar último comando en la misma sesión tras perder ACK. | Devuelve resultado, conserva inicio y duración; no vuelve a liberar. | Automática | PENDIENTE |
| C-SEQ-02 | **Secuencia conflictiva.** Mismo seq con otro payload/id; después un seq menor. | SEQUENCE_CONFLICT o STALE_SEQUENCE, sin cambiar estado aceptado. | Automática | PENDIENTE |
| C-SEQ-03 | **Orden concurrente.** Procesar seq7 y seq8 con demoras de enforcement. | No termina aplicando seq7 después de seq8. Un solo escritor. | Automática | PENDIENTE |
| C-SEQ-04 | **Sesión/arranque/desafío viejos.** Entregar ALLOW de sesión anterior, otro boot o nonce vencido. | Rechazo; pedir estado actual y nueva confirmación al padre. | Automática + integración | PENDIENTE |

### Padre

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| P-KEY-01 | **Autenticación y pérdida de clave.** Cancelar credencial, expirar ventana, invalidar clave de prueba. | No firma ni crea una autoridad de reemplazo silenciosa. | Android padre | PENDIENTE |
| P-PAIR-01 | **Exportación y datos mínimos.** Inspeccionar QR/archivo público/env y logs de pruebas. | No contienen privadas ni códigos en claro; solo públicas/hashes/configuración. | Automática + manual | PENDIENTE |
| P-STORE-01 | **Persistencia ante cierre.** Fallar después de reservar seq y antes de enviar. | No reutiliza secuencia para otra intención; archivo sigue consistente. | Automática | PENDIENTE |
| P-ACK-01 | **ACK impostor.** Enviar ACK falso/otro hijo/otro hash o solo PEER_STATUS. | No muestra aplicado. | Automática | PENDIENTE |
| P-ACK-02 | **ACK perdido.** Aceptar en hijo y descartar ACK; sincronizar nuevamente. | Reconciliar id/seq/hash sin reiniciar temporizador. | Integración | PENDIENTE |
| P-ACK-03 | **Enforcement pendiente/fallido.** Hijo devuelve pending o failed; ALLOW aceptado pero red no sana. | UI muestra causa/pendiente/error, no permiso efectivo falso. | Automática + UI | PENDIENTE |
| P-NET-01 | **Padre cerrado.** Preparar intención sin hijo; cerrar y abrir después. | No entrega en background; pide autenticación/sincronización/confirmación. | Android padre | PENDIENTE |
| P-UI-01 | **Acciones y duraciones.** Probar cuatro acciones, 59,60,604800,604801s y doble pulsación. | Límites correctos, una orden en curso, explicación de efectos. | UI automática | PENDIENTE |
| P-UI-02 | **Estado antiguo.** Cortar conexión después de STATE válido. | Muestra último estado y antigüedad, no estado actual inventado. | UI automática | PENDIENTE |

### Relay Render

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| R-CONFIG-01 | **Config inválida.** Quitar claves/env, usar curva incorrecta o claves iguales. | Falla cerrado; health operativo solo con configuración válida. | Automática | PENDIENTE |
| R-AUTH-01 | **Suplantación de rol.** Autenticar como padre con privada del hijo o pública propuesta por atacante. | Rechaza; no incorpora socket al mapa. | Automática | PENDIENTE |
| R-AUTH-02 | **Reto viejo o reutilizado.** Reutilizar AUTH en otro socket o después de 10s. | Rechaza y consume/limpia correctamente. | Automática | PENDIENTE |
| R-AUTH-03 | **Expulsión de cliente.** Abrir impostor y luego segundo socket autenticado. | Impostor no expulsa; el nuevo auténtico reemplaza limpiamente. | Automática | PENDIENTE |
| R-ROUTE-01 | **Receptor ausente.** Enviar SET_POLICY cuando no existe hijo registrado. | PEER_UNAVAILABLE; no se guarda ni entrega más tarde. | Automática | PENDIENTE |
| R-ROUTE-02 | **Rol y bytes.** Intentar SET_POLICY desde hijo; reenviar uno permitido. | Rechaza rol incorrecto; bytes de contenedor válido no se alteran. | Automática | PENDIENTE |
| R-FAIL-01 | **Reinicio/despliegue.** Reiniciar servicio con móviles conectados. | Sockets reconectan, políticas locales se conservan, sin colas recuperadas. | Render + móviles | PENDIENTE |
| R-LIMIT-01 | **Abuso de frames.** Binarios, >16KiB, compresión, campos desconocidos, inundación. | Cierre/rate limit, sin caída del proceso ni memoria sin límite. | Automática | PENDIENTE |
| R-LIMIT-02 | **Socket lento y memoria.** Receptor deja de leer; muchas reconexiones. | Backpressure y limpieza; mapa no borra el socket nuevo por cierre del viejo. | Automática | PENDIENTE |

### Hijo: administración y emergencia

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-OWNER-01 | **Sin privilegio de propietario.** Instalar como app normal y comparar con DPC aprovisionado. | Primera situación aparece NO CONFIGURADO; segunda verifica Device Owner. | A13 | PENDIENTE |
| H-ENF-01 | **Quiosco real.** Pulsar Home, Recientes, Atrás y gestos desde bloqueado. | Sigue bajo launcher propio y LOCK_TASK_MODE_LOCKED, no PINNED. | A13 | PENDIENTE |
| H-ENF-02 | **Cambio permitido/bloqueado.** Abrir una app aprobada y recibir LOCK. | Deja de ser utilizable; tiempo local recepción-aplicación medido, objetivo ≤1s activo. | A13 | PENDIENTE |
| H-ENF-03 | **Política no aplicada.** Inyectar error de API/resultado y recuperar. | No ACK APPLIED hasta evidencia; estado seguro y error visible. | Automática + A13 | PENDIENTE |
| H-SEC-01 | **Elevación de rol.** Buscar cambio a padre en UI, intents o actividad exportada. | No existe vía operativa para elevar rol. | A13 + revisión | PENDIENTE |
| H-SEC-02 | **Controles normales sobre app.** Intentar desinstalar, detener, borrar datos, modo seguro y reset desde Ajustes. | Restricciones impiden la vía normal; no se afirma protección contra recovery. | A13 release | PENDIENTE |
| H-SEC-03 | **Salidas laterales.** Probar notificaciones, PiP, compartir, enlaces, instalador, otro launcher y usuarios. | No abren apps/componentes no autorizados ni ajustes libres. | A13 release | PENDIENTE |
| H-EMG-01 | **Keyguard y arranque.** Arranque antes del PIN Android, con/sin PIN SIM. Abrir marcador sin llamar. | Ruta de emergencia reconocible sin clave parental; documentar cada variante. | A13 físico | PENDIENTE |
| H-EMG-02 | **Sin internet.** Desconectar datos/Wi-Fi con radio telefónica disponible; abrir marcador sin llamar. | Emergencia no depende del relay ni de credenciales parentales. | A13 físico | PENDIENTE |
| H-EMG-03 | **Quiosco estricto y regreso.** Abrir marcador desde bloqueo endurecido; salir sin llamar. | Acceso funciona y vuelve a estado restringido sin escape a apps. | A13 físico | PENDIENTE |
| H-EMG-04 | **Llamada en curso y cambios.** Simulación autorizada de llamada de emergencia y devolución; cambiar política/red. | No se finaliza llamada ni ocultan controles críticos. No llamadas reales no coordinadas. | Entorno telefónico de pruebas autorizado | PENDIENTE |

### Hijo: estado, red y ejecución

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-BOOT-01 | **Reinicio desde permitido.** ALLOW, reiniciar y esperar que vuelva internet. | Permanece bloqueado hasta orden fresca para nuevo bootId. | A13 | PENDIENTE |
| H-BOOT-02 | **Dos broadcasts de un arranque.** Procesar LOCKED_BOOT_COMPLETED y BOOT_COMPLETED del mismo boot. | Un solo bootId, sin reset de seq ni acceso prematuro. | Automática + A13 | PENDIENTE |
| H-BOOT-03 | **Direct Boot.** Reiniciar sin introducir credencial; observar servicio/pantalla. | No lee Credential Encrypted ni exporta privadas; bloqueo y emergencia se conservan. | A13 | PENDIENTE |
| H-TIME-01 | **Cuatro políticas.** Recorrer LOCK, LOCK_FOR, ALLOW y ALLOW_FOR con reloj inyectado. | Prioridades/expiraciones exactas del contrato; política nueva sustituye anterior. | Automática | PENDIENTE |
| H-TIME-02 | **Cambio de hora y zona.** Cambiar reloj ±24h en laboratorio y repetir en release restringido. | No adelanta autorización ni evita vencimiento; pruebas no dependen de fecha. | Automática + A13 | PENDIENTE |
| H-TIME-03 | **Expiración sin red.** Vence LOCK_FOR con canal vencido; luego volver a conectar. | Sigue bloqueado offline; reevalúa al recuperar si no hay otro bloqueo. | Automática + A13 | PENDIENTE |
| H-NET-01 | **Red perdida.** Quitar Wi-Fi/datos desde un escenario autorizado de laboratorio. | Bloqueo local tras 3s, sin orden remota. Medir tiempo real. | A13 | PENDIENTE |
| H-NET-02 | **Handover de red.** Cambiar Wi-Fi/datos con un corte inferior al margen y después superior. | Evita falso positivo breve y bloquea si supera margen; un socket/callback. | A13 | PENDIENTE |
| H-NET-03 | **Solo falla Render.** Mantener internet pero cortar acceso al relay o sus PONG. | Bloqueo al superar 45s desde último latido válido. | Integración + A13 | PENDIENTE |
| H-NET-04 | **Portal, DNS, TLS y latido falso.** Wi-Fi sin salida, portal, TLS inválido, PONG de nonce viejo. | Ninguno autoriza canal; no deshabilita validación TLS. | Automática + A13 | PENDIENTE |
| H-STATE-01 | **Crash entre aceptar/aplicar.** Matar proceso en cada fase del último comando. | Estado se reconcilia; no duplica duración ni concede permiso por error. | Automática + Android | PENDIENTE |
| H-STATE-02 | **Archivo corrupto/truncado.** Corromper copia de laboratorio antes de cargar estado. | ERROR_SEGURO; no reabre enrolamiento ni pone ALLOWED. | Automática + Android | PENDIENTE |
| H-KEY-01 | **Keystore indisponible.** Simular error y probar antes de primer desbloqueo. | No autentica ni inventa ACK; conserva bloqueo y emergencia. | Android | PENDIENTE |
| H-PROC-01 | **Proceso muerto con otra app abierta.** Mientras se usa app aprobada, matar/simular muerte del controlador en laboratorio. | No hay ventana de uso que viole el requisito; si existe, falla la certificación hasta corregir. | A13 físico | PENDIENTE |
| H-POWER-01 | **Doze y despertar.** Doze forzado y reposo natural; expirar tiempo/red mientras duerme. | Al despertar no se habilita interacción fuera de política; observar otras apps, no solo launcher. | A13 físico | PENDIENTE |
| H-POWER-02 | **Consumo en reposo.** Comparar dos periodos equivalentes de 24h, con misma señal/apps/carga inicial. | Registrar energía, CPU, bytes, reconexiones; objetivo inicial delta ≤5 puntos de batería, ajustar con evidencia sin debilitar bloqueo. | A13 físico | PENDIENTE |

### Hijo: recuperación, actualización y retirada

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-REC-01 | **Recuperación offline.** Sin red, usar código/token MAINTENANCE. | Solo red dentro de UI propia por 5min; no habilita apps normales. | A13 | PENDIENTE |
| H-REC-02 | **Replay de recuperación.** Repetir token, usarlo para otro propósito o reiniciar. | Rechaza; códigos consumidos no vuelven y tokens efímeros expiran. | Automática + A13 | PENDIENTE |
| H-REC-03 | **Adivinación y reinicio.** Fallar códigos 5/10 veces y reiniciar durante espera. | Mantiene/reimpone espera; emergencia sigue accesible. | Automática + A13 | PENDIENTE |
| H-REC-04 | **Padre perdido.** Usar RECOVER_PARENT, revincular y actualizar env pública. | Clave vieja ya no autoriza; nueva funciona después de configuración, siempre con bloqueo durante transición. | A13 + Render | PENDIENTE |
| H-UPDATE-01 | **Actualización firmada.** Actualizar APK propio durante mantenimiento. | Conserva vínculo, claves, seq y temporizador; no requiere ADB permanente. | A13 release | PENDIENTE |
| H-UPDATE-02 | **Firma/migración incorrecta.** APK de otra firma o migración incompatible de estado. | No instala/acepta como legítimo; no pierde restricciones ni permite por defecto. | Automática + A13 | PENDIENTE |
| H-RET-01 | **Retirada local.** Código/token RETIRE, cancelar una vez; luego doble confirmación en equipo de prueba. | Cancelar no borra. Confirmar usa API correcta; jamás se ejecuta desde SET_POLICY remoto. | Destructiva, solo equipo autorizado | PENDIENTE |

---

<a id="parte-18"></a>

## Parte 18 · Ficha del dispositivo

Archivo modular: `pruebas/FICHA_DISPOSITIVO.md`.

## Ficha de validación física

Equipo declarado por el propietario: A13.
Fabricante / código SM completo: NO COMPROBADO
Versión Android / API / One UI / parche: NO COMPROBADO
Operador / SIM / PIN de SIM: REGISTRAR LOCALMENTE SIN NÚMEROS PERSONALES
APK / versionCode / huella del certificado de firma: NO CONSTRUIDO
Estado de Device Owner / bloqueo de arranque: NO COMPROBADO
Fecha / responsable autorizado: POR COMPLETAR

### Puertas

G0: PENDIENTE
G1: PENDIENTE
G2: PENDIENTE
G3: PENDIENTE
G4: PENDIENTE

### Evidencias

| Caso | Procedimiento realmente ejecutado | Resultado observado | Evidencia y entorno |
|---|---|---|---|
| POR COMPLETAR | | | |

### Emergencia

Ruta exacta desde pantalla bloqueada y desde boot:
Prueba de abrir/salir del marcador sin llamada:
Prueba de llamada completa en entorno autorizado, si existe:
Aspectos NO verificados:

### Ejecución y consumo

Tiempo recepción → enforcement con pantalla activa:
Tiempo sin red → bloqueo:
Tiempo sin PONG → bloqueo:
Comportamiento con otra app abierta y proceso controlador muerto:
Comportamiento tras despertar:
Comparación de batería y condiciones medidas:

### Veredicto

NO CERTIFICADO. Cambiarlo solo con evidencia de las puertas aplicables y autorización del responsable. Un modelo/firmware distinto requiere una nueva ficha.

---

<a id="parte-19"></a>

## Parte 19 · Plantilla de estado

Archivo modular: `plantillas/ESTADO.md`.

## Estado de implementación

Última actualización: POR COMPLETAR
Commit actual: POR COMPLETAR
Tarea actual: C00
Estado: PENDIENTE

### Entorno observado

Android Studio/JDK/Gradle/AGP/Kotlin: NO REGISTRADO
Node / sistema operativo: NO REGISTRADO
SM / Android / One UI / parche del hijo: NO COMPROBADO

### Terminado y demostrado

Ninguna tarea del producto. La documentación y fixtures no son una implementación.

### Archivos modificados en la última tarea

POR COMPLETAR

### Pruebas ejecutadas y resultado

POR COMPLETAR con comando, entorno y resultado real. No incluir pruebas imaginadas.

### Decisiones locales sin cambiar ADR

POR COMPLETAR

### Bloqueos y riesgos abiertos

G0–G4 pendientes. No hay validación física del A13.

### Próxima tarea y lectura mínima

C00 — compartido/03_INICIO_TECNICO.md

### Seguridad

No almacenar claves, códigos, QR sensibles, contraseñas ni datos del menor aquí.

---

<a id="parte-20"></a>

## Parte 20 · Plantilla de traspaso de contexto

Archivo modular: `plantillas/HANDOFF.md`.

## Relevo entre conversaciones o desarrolladores

Tarea terminada:
Commit:
Resultado realmente comprobado:
Archivos creados/modificados:
Interfaces públicas añadidas:
Pruebas y comandos ejecutados:
Pruebas que NO se ejecutaron:
Fallo conocido y forma de reproducir:
Decisión pendiente que requiere ADR:
Próxima tarea concreta:
Secciones mínimas que debe leer la siguiente IA:

No repetir toda la conversación. Máximo orientativo: 300–500 palabras. No incluir secretos ni marcar una API como verificada si solo se leyó su nombre.

---

<a id="parte-21"></a>

## Parte 21 · Resultados de validación del paquete

Archivo modular: `pruebas/VALIDACION_DEL_PAQUETE.md`.

## Validación documental realizada

Fecha: 8 de octubre de 2026. Alcance: archivos de documentación, esquema y fixtures de este paquete.

### Comprobaciones ejecutadas

- JSON Schema 2020-12 estructuralmente válido.
- 28 archivos JSON de ejemplos aceptados por el esquema; 13 contraejemplos estructurales rechazados.
- 10 vectores ECDSA P-256/SHA-256/DER: 78 comprobaciones de firmas, bytes, propósito, clave incorrecta, modificación y variantes de JSON no canónicas.
- JSON del paquete legible; bloques de código Markdown cerrados; 59 identificadores de pruebas de aceptación sin duplicados.
- No se incluyeron claves privadas en archivos. Los fixtures contienen claves públicas y firmas ficticias, no material de una familia real.

Entorno efectivamente usado: **Node v22.16.0**, Python 3.13.5, jsonschema 4.26.0. El relay del proyecto fija Node 24 LTS: **este entorno no ejecutó Node 24**. Repetir el verificador y todos los tests del relay en ese runtime al implementar; no confundir una prueba en Node 22 con certificación en Node 24.

Salida del verificador:

```text
OK: 10 vectores firmados; 78 comprobaciones documentales. Runtime: v22.16.0.
Esto no prueba Device Owner, Android Keystore, temporizadores, red real, emergencias ni el A13.
OK: esquema válido; 28 ejemplos aceptados y 13 contraejemplos rechazados.
Es validación estructural: no comprueba claves, firmas, nonce fresco ni permisos reales.
```

### Repetir estas comprobaciones

Desde la raíz del paquete:

```powershell
node pruebas/verificar_vectores.mjs
# Opcional: verificación del esquema en Python.
python -m pip install jsonschema
python pruebas/verificar_esquema.py
```

El primer comando no tiene dependencias adicionales a Node. El segundo flujo requiere Python y jsonschema. Los scripts no tocan un teléfono ni hacen conexiones de red; únicamente la instalación de la dependencia requiere acceso a su repositorio.

### No probado y no entregado

No se compiló una app Android ni un relay real; no hay APK. No se ejecutaron Device Owner, Lock Task, Android Keystore, arranque, restricciones, batería, latencia, mantenimiento, actualización ni retirada en el A13. Tampoco se hizo una llamada de emergencia ni una simulación telefónica. No se desplegó ningún servicio en Render.

Todos los **59 casos de aceptación del producto siguen PENDIENTES**. Los resultados anteriores no cambian su estado. La revisión de documentación reduce ambigüedades, pero no sustituye implementar, probar, revisar seguridad y validar físicamente el dispositivo.
