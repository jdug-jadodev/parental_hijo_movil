# Relevo de coordinación — padre, hijo y WebSocket CP/1

Fecha: 9 de octubre de 2026. Base de coordinación: commit hijo `524ad8b`.

## Petición para la sesión de Render

Coordinar la integración del padre y del hijo con el relay, primero en laboratorio.
Leer este archivo y comprobar el estado actual de los tres proyectos antes de
proponer una prueba. Este relevo no autoriza despliegue, uso de teléfonos o cambios
en otros repositorios: acordarlos con el propietario. No relajar TLS para conectar.

## Responsabilidades y rutas

| Proyecto | Ruta | Responsabilidad |
|---|---|---|
| Niño | `C:\Users\Usuario\Documents\parental_hijo` | Identidad Keystore, QR inicial, aceptación, cliente WSS, estado firmado y bloqueo local |
| Padre | `C:\Users\Usuario\Documents\parental_padre` | Leer oferta, autoridad parental, aceptación firmada, códigos y órdenes |
| Relay | `C:\Users\Usuario\Documents\parental_render` | Autenticar roles y retransmitir bytes; TLS, límites y despliegue coordinado |

Render NO genera CHILD_OFFER, claves privadas ni códigos; no instala APK, aplica
bloqueo o modifica las apps. Niño autorizado físicamente: A13 SM-A135M; desde la
sesión del hijo el A56 no se toca. Las pruebas del padre deben coordinarse en su
propio proyecto. Nada de pruebas conectadas indiscriminadas.

## Flujo de vinculación, antes de WebSocket

1. Niño genera identidad no exportable y muestra JSON `CHILD_OFFER`: `v`, `kind`,
   `childPublicKeyB64`, `enrollId` UUID v4, `enrollNonce` base64url de 32 bytes.
   Oferta RAM visible cinco minutos; sin timestamp añadido ni secretos.
2. Padre escanea, prepara `pairId`, origen HTTPS y tres códigos independientes.
   Adulto guarda códigos fuera del teléfono y compara huellas de ambas públicas.
3. Padre muestra sobre `SIGNED`, propósito `PAIR`, con `PAIR_ACCEPT`. Niño necesita
   escanearlo, verificar firma/oferta vigente y confirmación física antes del commit.
4. Exportación parental pública manual: `ACTIVE_PAIR_ID`, `PARENT_PUBLIC_KEY_B64`,
   `CHILD_PUBLIC_KEY_B64`, `PUBLIC_ORIGIN`. Relay usa exactamente esa pareja/claves.

Leer el primer QR no conecta automáticamente. `OFFLINE_CHALLENGE` es recuperación,
no oferta inicial. Vinculación completa exige estado firmado y recuperación probada.

## Contrato WebSocket congelado

- `wss://<servicio>/ws`, subprotocolo `cp.v1`; confianza TLS y hostname normales.
- Relay envía `CHALLENGE`; cliente firma `AUTH_PROOF` con propósito `AUTH`, rol,
  pareja, connectionId y nonce exactos. `AUTH_OK` no prueba bloqueo ni canal sano.
- PING/PONG CP/1 correlacionados; distinguir latidos de transporte y presencia.
  Consultar tiempos exactos en contrato; no inventar cadencias.
- Padre envía `SYNC_REQUEST`/`SET_POLICY`; niño responde `STATE`/`ACK`, propósito
  `MESSAGE`, firmas sobre bytes originales y claves vinculadas. No cola del relay.
- ACK del relay/presencia/envío no equivalen a APPLIED. Niño decide aplicación.

## Estado y próximos pasos verificables

Niño: 329 resultados JVM y 23 Android previos correctos; recuperación parcial,
consumo UI cerrado. CHILD_OFFER/PAIR_ACCEPT, cliente WSS y enforcement no implementados.
H05 abierta; H08 depende de ella y H06 sigue bloqueada. No saltar esa puerta para
aparentar integración lista. Próximo trabajo hijo: delimitar identidad/oferta inicial.

Padre: lector CHILD_OFFER y emisión PAIR_ACCEPT existentes; PREPARADO no habilita
órdenes. Relay registra cruce local WSS padre JVM/niño Node simulado; no APK hijo.
Loopback, health 503 y producción bloqueada: despliegue público pendiente.

Sesión Render: leer `documentacion/INTEGRACION_LOCAL_PADRE.md`, estados actuales y
CP/1 §5/6/9; proponer entorno TLS alcanzable por ambos teléfonos, sin trustAll.
`127.0.0.1` del teléfono NO es el PC. Coordinar después pruebas por etapas:
QR ida/vuelta → configuración pública → AUTH/PONG → SYNC/STATE → orden/ACK →
reconexión. Hasta existir hijo real, etiquetar simulación y no afirmar bloqueo.

Contrato/schema comparados en los tres repositorios, iguales (lectura, no tests):
`deee863f7587736d3e209f5a11a6e972a93004d0700c47eb3a2d136ad8f16a3c` /
`eccf964fbb41869c47d86ddb0bfda6585136430bab297568d3c20b56607372bb`.
Revalidar antes del cruce. No guardar privadas, códigos, series o credenciales.
Registrar resultados y devolver pendientes por proyecto al propietario; cualquier
cambio CP/1 requiere acuerdo/versionado, no adaptación unilateral del relay.
