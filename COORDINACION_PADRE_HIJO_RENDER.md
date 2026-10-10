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

## Resultado HIJO-C01 — diseño, no implementación

Unidad ejecutada: lectura y diseño solicitados por el coordinador. Base propia
`3f160c4`; relay consultado declara implementación `8385410` y documentación
`e26aebb`. Esta sección y ESTADO.md constituyen la devolución; el commit de cierre
se obtiene con `git log -1 -- COORDINACION_PADRE_HIJO_RENDER.md` después del registro.
No existe otra implementación en curso que se haya descartado.

### Evidencia y límites

Leídos en Render exclusivamente COORDINACION_PROYECTOS.md y
documentacion/INTEGRACION_LOCAL_PADRE.md; aquí instrucciones, estado, relevo y
dependencias H05–H11. Relay reporta 104 tests y seis variantes JVM, incluidas
muerte de proceso relay y pérdida de ACK emitidos en proxy WSS. Son resultados
ajenos registrados, NO reejecutados por esta sesión. Almacenamiento parental
cifrado RAM y niño Node simulado no acreditan Android, Device Owner o APPLIED.
No nuevas pruebas, builds, sockets, ADB, certificados o cambios externos.

### Bloqueo de dependencias y decisión requerida

El grafo explícito del plan no tiene ciclo: H05 depende C02/H04; H06 y H08 exigen
H05; H11 exige H05/H06/H08. Sin embargo, cerrar H05 como recuperación **operativa
de producción** exige vínculo/identidad H08 y herramientas H11. Ese significado
de cierre introduce un ciclo funcional. Además, CP/1 §5 exige recuperación y
estado firmado para dar vinculación por terminada, no solamente persistir públicas.

**Detenida la vía de identidad/oferta productiva H08.** No cambiar dependencias,
habilitar envío ni marcar H05 cerrada para resolverlo. Propuesta pendiente de
acuerdo del propietario/coordinador: distinguir una puerta H05 de autorización
local probada aisladamente (previa a identidad/vinculación) de la validación
integral H05+H08+H11 de recuperación operativa. Documentar expresamente en el plan
qué habilita cada puerta antes de ejecutar H08; no basta renombrar el estado.
No modifica CP/1; si el acuerdo requiere semántica nueva, proponer versión/ADR
antes de implementarla. H06 sigue bloqueada por recuperación y emergencia reales.

### Diseño acotado de identidad y oferta inicial (pendiente)

- ChildKeyStore será un puerto Android separado de FirmanteAndroidKeystore:
  crear identidad solo por acción presencial inicial autorizada y estado íntegro
  sin vínculo; P-256 no exportable. Nunca generar en boot, callback de red o lector
  de QR. Antes del primer desbloqueo devolver indisponible. Si existe vínculo y
  falta la clave, error seguro: no reemplazarla, importar privadas o abrir enrolamiento.
- Alias/identidad deben sobrevivir al proceso; oferta NO. Definir registro local
  del alias y recuperación de creación interrumpida antes de implementar, sin
  incorporarlo como campo CP/1 ni sobrescribir estado desconocido/corrupto.
- PairingController mantendrá una sola OfertaInicial en RAM: pública, enrollId,
  enrollNonce y referencia interna irreutilizable. UUID v4 y nonce32 de entropía
  segura; reloj elapsed y bootId se guardan solo en contexto local. Documento
  CHILD_OFFER contiene exclusivamente sus cinco campos CP/1, sin firma, códigos,
  timestamp, dirección de servidor o indicador de autorización inventados.
- Vigencia 300000 ms desde creación, no desde dibujar/leer QR; límite exacto
  inválido. Cancelar/salir/fondo, nuevo desafío, muerte de proceso, boot diferente
  o reloj discontinuo retiran oferta y futuras confirmaciones. Reevaluar no amplía
  tiempo. Emerger de nuevo no restaura un QR anterior desde Bundle/archivo.
- Separar puertos QR de oferta, PAIR_ACCEPT y OFFLINE. Mostrar CHILD_OFFER no
  gasta códigos, persiste vínculo ni conecta WSS. No aceptar datos de un comando
  para sustituir pública ya vinculada. Capturar aceptación es otra unidad.
- Futuro commit de aceptación: verificar PAIR con pública propuesta únicamente
  en enrolamiento inicial, correspondencia exacta de oferta/clave/nonce/id y
  confirmación física de huellas; revalidar contexto/vigencia tras firma y justo
  antes del commit. Consumo de oferta y exclusión de segundo vínculo serializados;
  éxito durable impide reapertura incluso tras crash. Fallo no concede permisos.
  No llamar VINCULADO operativo a esta sola transacción ni iniciar WSS en ella.

Puertos propuestos (no añadidos): IdentidadHijoInicial, FuenteContextoLocal,
RelojMonotonico, EntropiaOferta, EstadoEnrolamiento y RenderQrOferta. Escritura
serializada con ChildStateStore; futuras sesiones limitadas pasan por
RecoveryController, no por un booleano UI. Errores nunca ocultan emergencia.

### Siguiente implementación local permitida, propuesta para asignar

**H05-L01: coordinador de presentación OFFLINE aislado**, con identidad, estado,
reloj/contexto y ejecutor inyectados en tests. Integrar QrOffline/RecoveryController
en una proyección RAM; generación, cancelación, respuesta tardía y expiración
descartan desafío. Sin alias/vínculo reales, cámara Android, herramientas, WSS o
activación UI de producción. No ejecutar esta propuesta por el presente handoff:
esperar revisión/asignación de una sola unidad. No es comenzar H08 encubiertamente.

Aceptación H05-L01: QR decodificado solo entrega bytes al controlador; sesión
limitada únicamente tras firma/contexto correctos; cancelar invalida trabajos y
permisos, no restaura códigos gastados. Consumo impreso duradero antes de sesión,
espera persistida intacta y respuesta OFFLINE válida independiente de esa espera.
Sin ALLOWED, APPLIED, wipe, acceso libre a Ajustes o bloqueo de emergencia.
Para cerrar recuperación operativa faltarán integración Android/cámara, herramientas
y pruebas físicas coordinadas según la decisión de puertas anterior.

### Pruebas a implementar, todavía NO ejecutadas

H05-L01: pausa/cancelación durante firma/IO, respuesta tardía, nuevo desafío frente
a respuesta vieja, RAM recreada, boot cambiado, 299999/300000 ms, reloj regresivo,
espera impresa con OFFLINE válido, escritura fallida sin sesión y cancelación tras
consumo sin devolver código. Contrastar con tests existentes sin duplicar resultados.

Oferta futura: cinco campos exactos y lectura con codec CP/1; entropía nueva al
reabrir; vigencia exacta sin timestamp; rotación/redibujado no rejuvenecen; cancelación,
fondo, proceso/boot y doble operación invalidan referencia. Rechazar oferta consumida,
aceptación vieja/cruzada, clave distinta, firma inválida y vínculo ya existente;
fallo antes/después de commit sin revinculación libre. Identidad perdida o usuario
bloqueado nunca crean reemplazo. Pruebas Keystore físicas requieren permiso aparte;
comparación óptica con padre tampoco queda acreditada por JVM.

**Devolución:** HIJO-C01 diseño terminado; identidad/oferta productiva bloqueada por
decisión de puertas H05/H08/H11. Propuesta H05-L01 y criterios listos para revisión.
Sin alterar plan/contrato o permisos; resultado en documentos, sin mensajes de vuelta
entre agentes ni bucles de coordinación.
