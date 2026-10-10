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

## Resultado H05-L01 — coordinador OFFLINE aislado

Asignación recibida del coordinador con autorización comunicada del propietario;
una sola unidad local, sin ADB. Base propia `d350bf6`. Coordinación central leída
de nuevo solo en lectura. No se aprueba dividir puertas ni cambiar dependencias.
Commit de esta unidad: consultar `git log -1 -- COORDINACION_PADRE_HIJO_RENDER.md`
después del cierre; hash también devuelto en respuesta, sin mensajes entre agentes.

### Implementación y archivos

- `android/app-child/src/main/java/dev/controlparental/child/recovery/CoordinadorOffline.kt`:
  propietario RAM con RecoveryController interno; almacén, firmante, reloj/contexto
  y ejecutor serial inyectados. Proyección de desafío/matriz/tiempo, respuesta y
  acceso limitado, sin textos de códigos, públicas, hashes o sobres. No singleton.
- `.../recovery/RecoveryController.kt`: consulta desafioRestanteMs, que compara
  estado íntegro, vínculo, boot/contexto y tiempo local; retira desafío inválido
  sin firma, consumo o escritura. No timestamp nuevo ni cambios CP/1.
- `android/app-child/src/test/java/dev/controlparental/child/recovery/CoordinadorOfflineTest.kt`:
  17 tests nuevos. `OfflineRecoveryTest.kt`: reutilizar su firmante JCA efímero
  mediante visibilidad internal, sin duplicar algoritmos o pruebas existentes.
- Documentación propia: ESTADO.md y este archivo. No repos externos modificados.

### Comportamiento comprobado en JVM

Generar retira desafío/sesión previos. Ticket por identidad de objeto y generación
interna invalidan respuestas/trabajos viejos; una cola serial debe ejecutar una
sola vez/en orden/sin solapamientos. Cancelar/pausar limpia la proyección de inmediato
sin esperar firma/IO; limpieza del núcleo al finalizar/por cola impide permiso tardío.
Si se rechaza un trabajo de limpieza, la siguiente tarea aceptada limpia primero;
no reconstruye autorización antigua. No restaura código ya consumido.

QrOffline solo devuelve bytes sin autorizar; RecoveryController verifica firma
con pública vinculada y contexto antes de conceder sesión limitada. Espera impresa
no intercepta OFFLINE válido ni se elimina por él. Código conserva transacción
duradera anterior a sesión/publicación. Error de escritura no concede acceso.
Proyección no es permiso de enforcement: no incluye ALLOWED/APPLIED/wipe/Ajustes.

El dueño debe pedir actualizar periódicamente: sin timers/hilos ocultos ni wiring
de lifecycle Android. Consulta no rejuvenece duración. Frontera exacta 300000 ms,
reloj anterior al inicio/observación durable y boot/contexto distintos invalidan
desafío/sesión según el núcleo existente. Una proyección leída sin actualizar no
es autoridad vigente; respuesta siempre se vuelve a comprobar en el controlador.
Emergencia Android no se modifica ni depende de esta proyección; no se añade
una afirmación de nueva validación física.

### Pruebas y comandos realmente ejecutados

Desde android/ con JAVA_HOME de BUILD_ENV:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest --console=plain
```

Ejecutado dos veces: BUILD SUCCESSFUL. Primera con 14 tests nuevos; última con
17 tras añadir carrera entre hilos, QR óptico sintético y rechazo de cola. Últimos
informes: núcleo 169 UP-TO-DATE; hijo debug 177 EJECUTADOS; total núcleo+debug
**346 resultados/0 fallos/0 errores/0 omitidos**. Release 170 EJECUTADOS (repite
casos), también correctos. No sumar release como pruebas independientes nuevas.

Casos nuevos: generación serial/firma impostora; cancelación antes de ejecutar y
durante firma; generación durante firma; referencia/nonce viejos; recreación RAM;
299999/300000 ms en desafío y sesión; respuesta vencida; boot/reloj inválidos;
espera impresa con OFFLINE válido; cancelación durante IO de respuesta y código;
consumo confirmado antes de proyección; fallo de escritura; cancelación real en
otro hilo mientras firma detenida con latch; QR sintético decodificado; cola que
rechaza limpieza sin recuperar permiso. Crypto CP/1 y QrOffline reales; archivo,
contexto y tiempo simulados. IO cancelada mediante hook determinista en commit.

No assemble/APK, lint, instrumentación, cámara, Android Keystore o dispositivos
en esta unidad. No alias/vínculo reales, herramientas, WSS/TLS/LAN ni certificados.
Las 23 pruebas Android anteriores no validan este coordinador. git diff --check
antes del commit; sin privadas, credenciales, códigos reales o series registrados.

### Devolución y siguiente unidad propuesta

H05-L01 terminada al alcance aislado. H05 sigue abierta; no se inicia H06/H08/H10/
H11 y no se considera resuelta la decisión de puertas funcionales HIJO-C01.
Propuesta para revisión: fijar el límite de cierre H05 y su evidencia requerida
antes de asignar integración Android/UI o identidad inicial; todavía faltan
cámara/lifecycle reales y herramientas/recuperación operativa. No ejecutar otra
unidad por este cierre ni cablear consumo de producción como atajo.

## Resultado HIJO-C02 — propuesta de puertas y resumen público

Unidad exclusivamente documental sobre base `c7fb842`, reutilizando HIJO-C01 y
H05-L01. Entregable: `hijo/DISENO_PUERTAS_Y_RESUMEN_PUBLICO.md`.
Leídos CP/1 §5/9/9.A, PLAN_HIJO H05–H11, ModelosEstado.kt y, solo en lectura,
padre/DISENO_ACREDITACION_VINCULO.md y coordinación central del relay.

Matriz: garantías aisladas A, puerta local propuesta B, recuperación real C1 antes
de endurecer y acreditación integral C2 con WSS/STATE/ceremonia. Separar C1 de C2
evita exigir H10 para comprobar la salida local anterior a H06. Ninguna etiqueta
parcial/JVM habilita H06. B tampoco habilita H08 con dependencias actuales.
Definidas unidades mínimas de lifecycle/capacidades/Android aislado y evidencia
física necesarias para presentar revisión del orden H05/H08/H11 al propietario.
No se altera PLAN_HIJO ni se declara H05 cerrada; ciclo funcional sigue bloqueante.

Resumen post-commit H08 propuesto: pairId, SHA-256 SPKI DER de ambas públicas
(hexadecimal mayúsculo completo agrupado de cuatro), origen HTTPS exacto desde
un snapshot persistido íntegro, nunca desde QR/formulario previo. Oferta, captura,
commit/readback y acreditación se distinguen; reapertura no revincula. Error/crash/
corrupción/identidad perdida no generan claves o datos confirmados ficticios.
Sin privadas/códigos/hashes de recuperación/historial ni recibos/frames nuevos.
EstadoControl actual no acredita H08; procedencia/migración futura por diseñar.

Acuerdo propuesto para padre: comparación presencial de esos cuatro datos,
declaración de commit observado y mantenimiento real separadas de criptografía.
STATE no acredita PAIR_ACCEPT exacto ni consumo OFFLINE. Acreditación automática
remota exacta exigiría propuesta CP/2; esa vía queda detenida, no implementada.
PADRE-C02 espera revisión/asignación independiente; ni promoción ni órdenes aquí.

Evidencia propia: lectura y revisión documental; git diff --check antes del commit.
NO tests/builds/ADB/código/UI/cámara/Keystore/WSS/certificados/LAN/despliegue;
resultados H05-L01/Android/relay son históricos, no reejecutados. Rutas modificadas:
este relevo, ESTADO.md y el nuevo diseño. Commit de cierre devuelto en respuesta
y consultable mediante git log de estas rutas. No editar repos externos/enviar prompts.

Siguiente paso: coordinador traslada propuesta al padre y solicita decisión del
propietario sobre puertas/orden y evidencia antes de asignar otra unidad. No
implementar ninguna puerta/ceremonia ni reducir pruebas físicas por este documento.
