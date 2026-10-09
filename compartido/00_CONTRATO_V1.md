# Contrato compartido de control parental — CP/1

Versión de protocolo: 1.0. Fecha: 2026-10-08. Estado: especificación normativa por implementar.

Este archivo es la única fuente de verdad del protocolo. Padre, hijo y relay deben usar los mismos nombres, unidades y validaciones. «DEBE» es obligatorio. Los intervalos y límites de este documento son decisiones del proyecto. Referencias externas: [S09–S13, S20, S23–S26] en `02_FUENTES_Y_COMPATIBILIDAD.md`.

## 1. Alcance y arquitectura mínima

Una familia, un padre y un hijo en v1. Dos APK Kotlin comparten una biblioteca `core-protocol`. El padre conserva su clave privada, configuración y una única intención pendiente. El hijo conserva su clave privada de identidad, la clave pública del padre, restricciones y estado actual. Un proceso Node.js + TypeScript + `ws` en Render conecta ambas partes mediante WSS.

No hay Firebase, base de datos, Redis, colas persistentes, historial, analítica, login con correo ni panel web en v1. Docker no es necesario. El relay conserva **configuración pública estable** en variables de entorno y conexiones temporales en RAM. Eso no es una base de datos ni contiene la clave privada del padre. Sin esta raíz de confianza no se podría autenticar a los clientes tras reiniciar.

El relay usa exactamente una instancia y un proceso; no hay clúster ni escalado horizontal. Puede desconectarse por despliegue o fallo. Los móviles reconectan. El padre necesita la aplicación abierta y autenticada para enviar una orden; el hijo no necesita que el padre esté conectado para ejecutar una política ya aceptada.

## 2. Semántica del producto

### 2.1 Órdenes

| Acción | Estado solicitado al aceptar | Qué ocurre después |
|---|---|---|
| `LOCK` | Bloqueado | Permanece así hasta nueva autorización |
| `LOCK_FOR` | Bloqueado | Al vencer, permite uso solo con red/canal válidos y en el mismo arranque autorizado |
| `ALLOW` | Permitido | Uso permitido mientras las condiciones locales lo permitan |
| `ALLOW_FOR` | Permitido | Al vencer, vuelve a bloqueado |

`LOCK_FOR` y `ALLOW_FOR` requieren `durationSec` entero entre 60 y 604800 (1 minuto a 7 días). `LOCK` y `ALLOW` requieren `durationSec: null`. Una nueva orden sustituye la anterior: no se acumulan temporizadores. Las duraciones comienzan en la primera aceptación duradera por el hijo, no al pulsar el padre ni al recibir el relay. El ACK devuelve el tiempo restante real.

El bloqueo significa quiosco con pantalla del hijo y acceso de emergencia del sistema; no apagar telefonía, internet ni el servicio controlador. Permitir significa acceso a las aplicaciones expresamente aprobadas durante configuración; **no devolver acceso libre a Ajustes ni retirar Device Owner**. La selección local inicial de aplicaciones autorizadas es parte necesaria del quiosco, no un sistema de seguimiento de aplicaciones.

### 2.2 Condiciones con prioridad

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

### 2.3 Valores iniciales

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

## 3. Identidades y datos

- `pairId`, `bootId`, `childSessionId`, `connectionId`, `commandId`: UUID v4 en minúsculas.
- `nonce`, `syncNonce`, `commandNonce`, `enrollNonce`: 32 bytes aleatorios, base64url sin `=`.
- `seq`: entero positivo de 64 bits representado como **cadena decimal**. Rango 1..9223372036854775807; sin signo, espacios ni ceros iniciales. Estado sin órdenes usa `lastAcceptedSeq: "0"`.
- `stateSeq`: contador de estado de la sesión del hijo, también cadena decimal. Se reinicia solo al cambiar `childSessionId`.
- Tiempos de duración: segundos enteros. Reloj de medición: `elapsedRealtime()` en hijo, reloj monotónico en relay. No se usan fechas de pared para desbloquear.
- Nombres visibles y datos del menor son locales. El protocolo lleva identificadores aleatorios, modos y códigos, no nombre, edad, teléfono ni ubicación.

El hijo genera un `bootId` persistido por arranque, no por cada callback. `LOCKED_BOOT_COMPLETED` y `BOOT_COMPLETED` pertenecen al mismo arranque y no deben crear dos identidades. Se contrasta el contador de arranque del sistema y el estado guardado; si no se puede probar continuidad, se adopta un nuevo contexto bloqueado. Un simple reinicio del proceso no reinicia el contador de órdenes ni el temporizador de la política.

El hijo crea un `childSessionId` nuevo cada vez que autentica una conexión WSS. Las órdenes nuevas se dirigen a ese identificador. Reconectar no borra la política existente, pero invalida órdenes nuevas dirigidas a la sesión anterior.

## 4. Firma y serialización interoperable

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

## 5. Vinculación física inicial, sin servidor de cuentas

1. El hijo, todavía en configuración presencial, genera su clave de identidad no exportable. Muestra un QR `CHILD_OFFER` con `childPublicKeyB64`, `enrollNonce` y `enrollId` aleatorios. El QR no contiene secretos.
2. El padre genera su clave de autoridad en Keystore, protegida por autenticación del dueño del teléfono. Escanea la oferta y crea un `pairId` nuevo. La aplicación solicita el origen del relay (`https://nombre.onrender.com`); nunca acepta `http` en release.
3. El padre genera tres códigos de recuperación diferentes de 160 bits y prepara sus hashes con propósito. Los muestra para guardarlos fuera del teléfono. No los transmite; solo se vinculan sus hashes.
4. El padre presenta un QR `PAIR_ACCEPT` firmado con propósito `PAIR`: `pairId`, `enrollId`, `enrollNonce`, ambas claves públicas, `relayOrigin` y los tres hashes. El hijo lo escanea, verifica coincidencia con su oferta vigente, firma y confirmación física del adulto en ambos equipos. Cada pantalla muestra el mismo resumen de huellas de claves para comparar. Una vez vinculado, esta pantalla no puede cambiar claves sin recuperación autorizada.
5. El padre exporta únicamente configuración **pública** para Render: `ACTIVE_PAIR_ID`, `PARENT_PUBLIC_KEY_B64`, `CHILD_PUBLIC_KEY_B64`, `PUBLIC_ORIGIN`. El desarrollador la coloca en el servicio. Las claves privadas y códigos no se exportan.
6. Padre e hijo prueban autenticación con el relay. La vinculación no se da por terminada hasta recibir estado firmado del hijo y comprobar una recuperación local en entorno de prueba.

Este paso manual evita un sistema de registro, usuarios y base de datos. No se entrega una credencial de administración de Render a ninguna app. El QR de aprovisionamiento Android para instalar un DPC es **otro QR distinto** del QR que vincula padre e hijo. Si se utiliza ese aprovisionamiento se implementan sus handlers oficiales [S18, S19].

## 6. Conexión y autenticación del relay

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

## 7. Sincronización, emisión y confirmación

### 7.1 Obtener estado fresco

El padre crea `syncNonce` nuevo y envía `SYNC_REQUEST` firmado con `pairId`, `sender:"PARENT"`, `v:1`, `syncNonce`. No consume `seq`.

El hijo responde `STATE` firmado con la misma pareja y ese `syncNonce`, su `bootId`, `childSessionId`, `stateSeq`, `lastAcceptedSeq`, `lastCommandId`, `lastCommandPayloadSha256B64`, `lastResult`, `mode`, `reason`, `remainingSec`, `networkValidated`, `channelHealthy`, `isDeviceOwner`, `lockTaskActive` y un **nuevo `commandNonce` válido 30 segundos en su reloj monotónico**. El hijo conserva ese desafío en RAM para esa sesión. Cada sincronización reemplaza el anterior. Si no hubo orden previa, id/hash son null y `lastResult:"NONE"`.

El padre acepta el estado solo con firma de la clave del hijo y `syncNonce` de su sesión actual; en actualizaciones posteriores exige `stateSeq` creciente y la misma sesión del hijo. Al reconectar solicita nueva sincronización. La presencia indicada por el relay no reemplaza este proceso.

### 7.2 Enviar una política

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

### 7.3 ACK real

`ACK` está firmado por el hijo y contiene `pairId`, `sender:"CHILD"`, `bootId`, `childSessionId`, `commandId`, `seq`, `commandPayloadSha256B64`, `result`, `mode`, `reason`, `remainingSec` y `v:1`.

Valores de `result`:

- `APPLIED`: la intención está persistida y el controlador comprobó las restricciones efectivas; `mode` puede seguir bloqueado por red o por el PIN inicial de Android. La interfaz explica la causa.
- `ACCEPTED_PENDING_ENFORCEMENT`: existe aceptación duradera pero todavía no prueba suficiente de aplicación. No mostrar «Aplicado».
- `ENFORCEMENT_FAILED`: no se pudo comprobar la aplicación. El hijo intenta mantener bloqueo y el padre muestra error.
- `REJECTED`: no se aceptó ni aplicó esa orden. El campo `reason` explica el código permitido.

`STATE.lastResult` describe solo la última orden aceptada: `APPLIED`, `ACCEPTED_PENDING_ENFORCEMENT`, `ENFORCEMENT_FAILED` o `NONE` si no existe aceptación. Un rechazo posterior no sobrescribe ese registro. `mode` es `LOCKED`, `ALLOWED`, `MAINTENANCE` o `SETUP`. `reason` usa los códigos del esquema. `remainingSec` es null si no existe temporizador, o entero ≥0. No se usa un mensaje de Render para marcar `APPLIED`.

Si el ACK se pierde, el padre retransmite exactamente el mismo comando como máximo dos veces mientras la sesión siga siendo la misma. Luego solicita estado fresco. Solo reconoce la aplicación previa si el estado firmado incluye id, seq y hash coincidentes. No crea una nueva orden con nueva duración por el simple hecho de perder el ACK.

### 7.4 Órdenes pendientes y cierre del padre

La aplicación guarda una única **intención pendiente**, no una cola histórica. Si no hay hijo conectado no puede firmar un comando nuevo con sesión y desafío válidos. Muestra «Pendiente local; abre esta app cuando el hijo tenga conexión».

Después de reconectar o reiniciar el hijo, no reproduce un desbloqueo firmado antiguo. Sincroniza, reconcilia el último resultado y pide confirmación al padre para emitir otra intención con nuevos identificadores de sesión, desafío y secuencia. Esto también evita desbloqueos diferidos sorpresivos. Ninguna orden tiene garantía de entrega tras cerrar el padre.

## 8. Heartbeats y disponibilidad

Después de `AUTH_OK`, cada cliente envía `PING` de aplicación con nonce aleatorio; el relay devuelve `PONG` con el mismo nonce. El hijo usa solo respuesta a su desafío vigente, en la conexión autenticada actual, como latido. Las tramas de control ping/pong WebSocket pueden usarse para limpiar sockets, pero no reemplazan ese estado de aplicación.

Red validada y canal sano son condiciones independientes. `NetworkCallback` actualiza la primera [S09]. PONG actualiza la segunda. Una respuesta HTTP 200, DNS resuelto, socket abierto o icono Wi-Fi no demuestran un canal autenticado. Captive portal, DNS roto o TLS inválido no permiten uso normal. Si Android pausa el proceso, al reanudarse se calcula el tiempo transcurrido antes de permitir interacción; no se reinicia el margen desde cero.

El relay emite `PEER_UNAVAILABLE` cuando no hay receptor; no almacena el mensaje. Los errores del relay son informativos y nunca autorizan al hijo a liberar el teléfono.

## 9. Recuperación y retirada

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

## 10. Persistencia sin base de datos

Padre: archivo privado cifrado con AES-GCM y clave local de Keystore para metadatos, sin backups automáticos; alias de clave de firma, vínculo, contador y una intención pendiente. La clave privada de firma nunca se serializa. Guardar no equivale a sincronizar.

Hijo: un archivo `control-state.json` mediante `AtomicFile` en almacenamiento protegido del dispositivo y un único escritor serializado. Contiene vínculo público, hashes de recuperación, última aceptación, etapa de aplicación, temporizador y contexto de arranque. No contiene clave privada del padre ni códigos. Su clave de identidad está en Keystore; se usa después del primer desbloqueo. Si antes no puede accederse a Keystore, **se mantiene bloqueado sin intentar fingir conectividad** [S07, S10, S21].

Separar archivo de política y contador sin transacción produciría fallos de repetición; no hacerlo. No guardar duración solo en memoria. Al leer datos desconocidos o corruptos, usar `ERROR_SEGURO`, no reabrir el enrolamiento sin autorización.

## 11. Errores y evolución

Códigos mínimos: `BAD_SCHEMA`, `BAD_SIGNATURE`, `WRONG_PAIR`, `WRONG_ROLE`, `STALE_BOOT`, `STALE_SESSION`, `STALE_NONCE`, `STALE_SEQUENCE`, `SEQUENCE_CONFLICT`, `NOT_DEVICE_OWNER`, `NOT_PROVISIONED`, `BEFORE_USER_UNLOCK`, `BOOT_AUTH_REQUIRED`, `NETWORK_LOST`, `CHANNEL_EXPIRED`, `PARENT_LOCK`, `TIMER_EXPIRED`, `OK`, `MAINTENANCE_ACTIVE`, `STATE_CORRUPT`, `ENFORCEMENT_ERROR`, `KEY_UNAVAILABLE`, `PEER_OFFLINE`, `RATE_LIMITED`, `UNKNOWN_VERSION`, `SYSTEM_KEYGUARD`, `CALL_IN_PROGRESS`.

Un comando con versión desconocida se rechaza. No se agrega «compatibilidad» aceptando campos sin validación. Cualquier cambio incompatible exige CP/2, otro ADR, nuevos fixtures y cambio coordinado de ambas aplicaciones. El futuro navegador necesita su propia delegación de clave/autorización: **no puede usar automáticamente una clave privada no exportable del Android del padre**.

### 9.A. Recuperación interrumpida y alcance del mantenimiento

El mantenimiento autorizado no concede ALLOWED ni elimina el bloqueo del arranque: puede accederse a sus herramientas limitadas aun con ese bloqueo, después de desbloquear el Android ordinario.

RECOVER_PARENT habilita una ventana presencial de 5 minutos ligada al proceso/arranque. El código se consume al abrirla. La clave vieja se revoca y el nuevo vínculo sustituye al anterior **solo en el commit atómico de la nueva vinculación**. Antes del commit, un fallo/cancelación conserva el vínculo anterior pero cierra la ventana; no permite retomarla sin nueva autorización. Si el código único ya fue consumido y se perdió el padre, se usa la vía RETIRE todavía disponible para restablecer y configurar de nuevo. Después del commit, nunca se restaura silenciosamente la clave anterior. No persistir un booleano que reabra enrolamiento libre después de reiniciar.
