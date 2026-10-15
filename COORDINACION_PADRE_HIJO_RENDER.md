# Relevo de coordinación — padre, hijo y WebSocket CP/1

## HIJO-LOCAL05 — entrega bloqueada sin física, 14 de octubre de 2026

- Base6ab6c1c; bloque1 `552d4b9`; cierre en commit que incorpora este relevo. MAIN uso libre NORMAL LOCAL/red default INTERNET+VALIDATED/gracia10.000ms y EXCEPCIÓN OFFLINE LOCAL con código fresco/casilla explícita; autoridad/snapshot/boot separados de CP/1. Reconexión antes deadline cancela gracia; a partir de10s revoca y NO abre de nuevo. Boot/proceso recreado no reconstruyen permisosRAM. No duración nueva/CP2/remoteoffline.
- **ENTREGADA_BLOQUEADA_LINT_RELEASE_SIN_FISICA**:342debug/287release0fallos/errores; APKdebug/releaseunsigned/AndroidTestcompilados.3testsAndroid DP/fakesNOejecutados; core-protocol:testUP-TO-DATE. lintRelease1error11avisos: ForegroundServicePermission exige alarmas paraFGSsystemExempted aunque docs admitenDeviceOwner. Sin permisosalarmas/suppress/baseline/deps/bypass. Evidencia/rutas/hashes: `pruebas/EVIDENCIA_HIJO_LOCAL05.md`.
- NingúnADB/instalación/notificaciones/armado/corteconectividad/LOCAL03/reboot ni cambio en otros repos/push. A13 último estado conocidoLOCAL04 desarmado/NONE/restaurado, no reconsultado. No bloqueo físico por red/latencia/crash/zeroescape/productfinal acreditados.
- Resolver gate únicamente por revisión/asignación concreta; luego nueva autorización específica de instalación/notificación/armado y cortes manuales. No PREPARADO_FISICO mientras gate falla. Detenido: no siguiente tarea ni ensayo automático. Render no dispositivos.

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

## Resultado HIJO-C03 — control local normal del adulto

Fecha 2026-10-10, base `cb62a73`. Requisitos nuevos confirmados según asignación;
unidad solo documental. Diseño en `hijo/DISENO_CONTROL_LOCAL_ADULTO.md`.
Fuentes en lectura: propuesta de control local/coordinación central Render y
diseño de acreditación padre; CP/1 §5/9/9.A y ADR-H-002/005/006 actuales.

Matriz separa sesión administrativa RAM, permiso normal por boot y recuperación
limitada. Emergencia/Keyguard independientes. Propuesta de credencial aleatoria
reutilizable por instalación (no universal), verificador protegido/KDF por calibrar,
espera persistida independiente, rotación atómica y recuperación específica sin
reciclar MAINTENANCE o códigos CP/1. Nada de claros/logs/QR/WSS del secreto.

Alternativas y recomendaciones pendientes: duración explícita, sesión administrativa
RAM cinco minutos cerrada al fondo, permiso LOCAL sobrevive offline/proceso del
mismo boot pero nunca reboot; última acción válida con contexto/revisión fresca
serializada gana, no vieja orden tras reconexión. Timer/ACK de orden anterior no
certifican modo vigente local. STATE debe distinguir origen/revisión efectiva y
última orden remota; solo propuestas de conceptos, ningún campo/schema añadido.

Bloqueo: cambia ALLOW offline y ADR-H-002, exige nueva versión acordada de tres
roles antes de código. CP/2 provisional; detalles de firma/subprotocolo/estado,
precedencia/duración/olvido/migración/puertas requieren decisión. H05-L01 no valida
uso normal. H05 abierta, H08 pendiente, H06 y puertas no se activan. Diseño anterior
padre requiere revisar suposiciones, no promoción automática o recibos exactos.

Evidencia: lectura y revisión documental con git diff --check, sin pruebas nuevas
o builds. Rutas modificadas: diseño nuevo, este relevo y ESTADO.md. Ningún cambio
externo, contrato/fixtures/PLAN_HIJO, UI/Keystore/WSS, ADB/dispositivos/certificados/
LAN/push/despliegue. Commit de cierre devuelto en respuesta y consultable en git log.
Siguiente unidad propuesta: revisión padre y decisión explícita de versión/ADR,
credencial/precedencia/migración y puertas por propietario/coordinador, no código
o ejecución automática por este cierre. No enviar prompts de vuelta.

## Resultado HIJO-C04 — consolidación reanudada el 2026-10-12

Misma unidad sobre `9ae6c99`, no una nueva asignación. Primera ejecución quedó
interrumpida: tres lecturas externas sin completar, sin causa adicional inferida.
Al reanudar, árbol propio limpio; se usa resumen íntegro del coordinador sin
ampliar acceso/permisos, nuevas sesiones o subagentes. PADRE-C03 `984693c` se toma
como contexto comunicado, no como lectura externa nueva acreditada.

`hijo/DISENO_CONTROL_LOCAL_ADULTO.md` consolida decisiones expresas que sustituyen
recomendaciones anteriores: código elegido de 5–10 dígitos cadena/ceros iniciales,
pregunta personalizada local offline para establecer código nuevo (no revelar
anterior), duración LOCAL elegida cada vez sin default, todas apps normales,
permisos LOCAL/REMOTO independientes de desconexión hasta plazo/bloqueo nuevo/reboot.
Administración RAM separada, cierre salir/fondo/proceso y emergencia/Keyguard intactos.

Matriz y propuestas precisan revisión global distinta seq remoto/escritor único,
invalidación de referencias/timers incluso apertura LOCK_FOR, segunda acción
conflictiva con confirmación nueva. Resultado de orden remota distinto de modo
efectivo; ACK viejo no reemplaza STATE posterior ni reconexión renueva permiso.
Código/respuesta/salts/verificadores no salen a padre/relay/QR/logs/exportación/STATE.
Pregunta visible localmente no prueba identidad; cinco dígitos y respuesta deducible
son riesgos, sin promesa de entropía o antirollback físico.

Reset propuesto solo capacidad RAM de cambiar código, con readback/commit atómico,
revocación de autoridad/revisión/timers/permiso previo por acordar; no apps/admin
automáticos ni reset de consumos/esperas CP/1. Normalización/límites, espera agregada
código+respuesta resistente a alternancia/boot, KDF/protección versionados sin
medición inventada, alta/migración presencial y olvido doble documentados. No código
extra obligatorio. Todas apps sigue requisito; límites oficiales/escapes Android
deben validarse, no volver a lista parental para fingir cumplimiento.

Pendientes imprescindibles: rango/granularidad/LOCK_FOR, caducidad admin/reset y
revocación tras cambio, normalización/cupos, salida ante olvido doble, versión/ADR/
migración/contextos y puertas/orden. CP/2 provisional; H05 abierta/H08 pendiente,
H06 no activada. Defecto QR padre `a8e4ffc` comunicado reproducido NO corregido,
QR02 no asignado; no se repara ni acredita ceremonia óptica aquí.

Evidencia: solo lectura local/resumen suministrado, revisión documental y
git diff --check antes de commit. Sin tests/builds/KDF medido/Android/código/APK,
dispositivos/ADB/CA/LAN/WSS/enforcement/push/despliegue o cambios externos. Rutas:
diseño consolidado, este relevo y ESTADO.md. Contrato/fixtures/PLAN_HIJO/ADR aceptados
intactos. Hash de commit de cierre en respuesta y git log de estas rutas.

Siguiente subunidad mínima **propuesta, no asignada**: especificación conjunta exacta
de revisión/ticket/timers LOCK_FOR y espera/ventana de reset, para revisión padre y
acuerdo del propietario de detalles/versión/puertas antes de modelos/tests/código.
No otra tarea automática, prompts de vuelta o cambio de permisos por esta devolución.

## Resultado HIJO-C05 — viabilidad Android de todas las apps normales

2026-10-12, base C04 `4636d30`; única unidad documental asignada. Informe técnico
y resumen apto propietario: `hijo/VIABILIDAD_ANDROID_TODAS_APPS.md`. Consultas
oficiales Android actuales con fecha/URLs [R1–R12], todas HTTP 200; configuración,
manifest/adaptadores propios solo leídos. No repos ajenos ni dispositivos consultados.

Conclusión: **viabilidad condicionada, no certificación**. Candidato preferido para
investigar es Lock Task persistente/HOME propio con catálogo técnico dinámico de
TODAS las apps normales, no lista parental. Retirar paquetes finaliza tareas
bloqueadas, no garantiza parar servicios. Suspensión excluye admins/launcher activo/
instaladores esenciales/marcador/permission controller; lockNow no es bloqueo
parental continuo. Ajustes/launchers con administración dentro del mismo paquete,
intents/intra-task, PiP/ventanas/notificaciones y perfiles tienen límites propios.

No hay lease de política con fecha aplicado autónomamente por Android: alarma/FGS
requieren ejecución, resultado y relectura; no garantía oficial de cierre instantáneo
ante crash/Doze ni ausencia de ventana al primer unlock tras reboot. Guía BAL actual
no acredita excepción DO universal; ejemplo Lock Task no prueba retorno desde fondo.
Direct Boot aporta almacenamiento/receiver, no convierte lista previa en bloqueo
automático. Emergencia/Keyguard se conservan, no PIN parental constante ni acceso
amplio a Settings como arreglo. Root/recovery/firmware/antirollback fuera de garantías.

Tres elecciones imprescindibles antes de prototipo: alcance de superficies de
administración y HOME propio; instalación libre con carreras frente a mantenimiento
acotado (todas las instaladas siguen incluidas); aceptar investigar límites medidos
temporales/boot o exigir garantía estricta que aquí queda sin solución demostrada.
No se cambian requisitos confirmados ni se impone alternativa. Rangos/LOCK_FOR/
caducidades/normalización/intentos/olvido/versión quedan a coordinación separada.

Informe distingue minSdk31/targetSdk36/compileSdk36 del A13 API34 registrado;
detalles Samsung no comprobados por esta lectura. Mapa futuro mínimo y negativas
Q1–Q9 por escapes/paquetes/proceso/timer/boot/emergencia, sin ejecución ni permiso:
recuperación previa, reversibilidad y autorización física específica del A13/selector
único independientes. H05 abierta/H06 no autorizada, CP/2 provisional, CP/1 intacto.
QR padre reproducido no corregido, QR02 no asignado ni trabajado aquí.

Rutas de cierre: informe nuevo, este relevo y ESTADO.md. Solo revisión documental
y git diff --check; sin código/config/contratos/fixtures/planes/ADR modificados,
tests/builds/APK/ADB/dispositivos/KDF/Keystore/WSS/certificados/LAN/push/despliegue.
Commit de cierre se entrega en respuesta/git log; siguiente paso solo revisión,
no nueva especificación/prototipo/implementación automática ni prompts de vuelta.

## Resultado HIJO-C06 — arquitectura e indicadores mínimos

2026-10-12, bases C04 `4636d30`/C05 `94b1bbe`. Una unidad exclusivamente documental;
nuevo `hijo/ARQUITECTURA_BLOQUEO_Y_SENALES_MINIMAS.md`, con advertencia incremental
en C05. Solo contexto local/mensaje íntegro del coordinador, sin repos ajenos.

**Corrección expresa:** HOME propio aceptado, instalar/actualizar libre durante uso
obligatorio, no mantenimiento/lista parental. «Evaluar» NO implica aceptar ventanas/
fallos como entrega final; propietario exige máxima resistencia y corregirlos
iterativamente. Eso no garantiza lo imposible ni autoriza acceso físico/H06.
Arquitectura candidata Lock Task/catálogo dinámico/escritor único/deadline/readback;
carreras de instalación/actualización/vencimiento, admin inseparable en Settings,
PiP/intents/boot/Doze/crash requieren negativas y corrección antes de certificar.

Avisos mínimos diseñados, no monitorización por app: isInteractive/Keyguard son
proxy, USER_INTERACTION evento con acceso especial, no detector humano continuo ni
tiempo humano; PONG/foreground/permiso no significan usando. Propuesta de indicador
uso posible o interacción reciente/desconocido a elegir; acceso de uso no concedido,
API34 entrega amplia que necesita minimización/aprobación (filtros API35 no aplican).
Agregado diario estimado por intervalos monotónicos/cursor atómico y calidad, sin
rellenar proceso muerto/corrupción con duración autorizada/cero. Día/zona/ancla y
huecos explícitos, snapshot absoluto fresco firmado, sin sumar deltas duplicados.

Recomendación pendiente: snapshot vigente/agregado del día y un único marcador del
último vencimiento del día, no cola/historial. Deadline agotado separado de bloqueo
aplicado/modo actual, detección tardía visible. Internet validado Android separado
de WSS/presencia. Padre sin reporte muestra causa desconocida/último reporte, no
deduce hijo sin internet. Notificación requiere permisos/canal/lifecycle revisión
padre: no socket/push inmediato garantizado con app muerta/red ausente/relay caído,
sin añadir FCM/cron/autoping o almacenamiento de uso en relay.

Tres decisiones §7: semántica de indicador/total y acceso especial; día/calidad/
retención marcador e incertidumbre temporal; resolver flujos admin inseparables y
avisos con padre cerrado sin imponer restricciones funcionales nuevas. Mínimos por
rol requieren acuerdo de versión/compatibilidad, no schema aprobado. CP/1 intacto,
CP/2 provisional, H05 abierta/H06 no autorizada. QR padre no corregido, QR02 intacto.

Fuentes oficiales HTTP200 con URL/fecha 2026-10-12; min31/target36 versus A13 API34
diferenciados. Sin datos reales/código/tests/builds/APK/Keystore/KDF/WSS/dispositivos/
ADB/políticas/permisos reales/CA/LAN/push/despliegue. Revisión documental y diff
únicamente. Rutas: informe nuevo, nota C05, este relevo y ESTADO.md. Hash de cierre
en respuesta/git log. Siguiente unidad mínima propuesta para revisión: modelo puro
aislado de acumulador/snapshot/calidad/marcador con fakes, sin iniciar implementación;
prototipo Android/pruebas negativas posteriores necesitan recuperación previa y
autorizaciones independientes. Sin sesiones/subagentes/prompts u otra tarea automática.

## Resultado HIJO-L02 — acumulador puro solo FICTICIAS_JVM

2026-10-12, base `ae1c7eb`, unidad aislada asignada. Modelo/test exclusivamente
`android/app-child/src/test/java/dev/controlparental/child/laboratorio/`:
AgregadoFicticio.kt y AgregadoFicticioTest.kt. Sin configuración/dependencias nuevas,
main/androidTest/productivo o protocolo. Evidencia/política/comandos detallados en
`pruebas/EVIDENCIA_HIJO_L02.md`; este relevo y ESTADO.md actualizados.

Entradas sintéticas clasificadas y límites de ventana/boot/elapsed/época explícitos,
sin permiso como observación. Registro mínimo subtotal/cobertura/cursor/calidad y
snapshot absoluto diagnóstico; CAS/readback fake, reentrada explícita, O(1).
Duplicado/prefijo solapado no se suma; atrasados no reparan huecos sin historial,
calidad parcial/no disponible, null en vez de cero sin evidencia. Ventanas 23/25h
inyectadas y cruces recortados, no día/zona elegidos para producto. Sin detector,
fuente Android/UsageStats/firma/DTO CP1, marcador o retención de vencimientos.

Pruebas reales: focalizado offline primero FAILED (19/1 fallo de expectativa de
test al perder registro conocido: RECHAZADO vs INCIERTO); corrección explícita de
expectativa, test de overflow ejercitado y tres casos añadidos. Regresión offline
después SUCCESSFUL: hijo debug 199 ejecutados, 22 L02+177 previos, cero fallos/
errores/omitidos; núcleo 169 UP-TO-DATE, no ejecutados de nuevo. No ocultar primera
ejecución ni presentar 368 resultados como Android/nueva ejecución completa.

Exclusión: 12 clases laboratorio debugUnitTest, cero en directorio kotlin debug
y ambos JAR principales debug compile/runtime; no referencias al componente en
main/androidTest. Solo inspección/config/clases, no APK ensamblado nuevo. 467 líneas
Kotlin propias; commit de cierre entregado en respuesta/git log.

Pendientes del propietario intactos: significado uso/total y acceso especial,
retención/marcador/zona y avisos con padre cerrado, versión/contrato/puertas. Toda
app/instalación libre/código/pregunta/offline/reboot confirmados, no aceptación de
fallos finales. H05 abierta/H06 no autorizada, CP2 no aprobado, QR padre no corregido/
QR02 no asignado. Sin Android/persistencia real/Keystore/KDF/enforcement/WSS/avisos,
dispositivos/ADB/permisos/CA/LAN/push/despliegue, repos ajenos o subagentes/sesiones/
prompts de vuelta. Esperar revisión; no siguiente unidad/prototipo automático.

## Resultado HIJO-C07 — revisión PADRE-C04 con decisiones del 13 de octubre

2026-10-13, base hijo `c6e8d5d`; misma unidad reanudada, no nueva ejecución C08.
Lecturas externas padre/Render interrumpidas inicialmente, sin causa inferida ni
reintento/ampliación permisos. Fuente **texto suministrado íntegro** PADRE-C04
`1582e66` §§1–13 en cinco partes y §17 central al reanudar; no lectura externa
acreditada ni equivalencia verificada con archivos actuales. Árbol limpio al retomar.

`hijo/REVISION_PADRE_C04_DELTAS_CP2.md`: tabla compatible/corregir/pendiente, deltas
exactos de acciones/mensajes/combinaciones/trazas y revisión contra boot/writer/
crypto propios. Retirar LOCK_FOR/deferredAllowSec/WAITING_ALLOW/apertura y trazas
de dos temporizadores, mantener expiración ALLOW_FOR; LOCK y ALLOW libre requeridos. Rango1min–7d
NO aprobado, alcance ALLOW local/remoto pendiente. No botones/codec/fixtures CP1
editados. Sin aceptar APPLIED por aceptación/callback de revisión sustituida/ACK.

Contador autorizado desde aceptación durable, apagado/Keyguard incluidos,
sin humano/UsageStats/duration futura ni promoción L02. Unión/cursor/durabilidad,
reconstrucción mismo boot condicionada a evidencia de continuidad, nuevo boot
parcial sin inventar gap. Guardas/recuperación/emergencia intactas, revisión
exhaustiva escapes pospuesta sin garantía inviolable ni permiso físico.

Propuesta semanal exacta: permittedTime sustituye day único por days array<=8
fechas (domingo21→domingo21 toca dos domingos parciales), epoch de retención,
retentionSeq/historyRevision/cutoff/calidad/snapshots absolutos. Borrar buckets/
total retenido/aviso expiración anterior sin cancelar permiso activo/deadline/seq/
credenciales; mantener cursor mínimo para no reconstruir prefijo purgado. Cleanup
local/catch-up hijo/padre antes de lectura/publicación, tombstone padre contra
sync viejo/caché/restauración/push tardío. No total vitalicio fuera de limpieza,
no histórico apps/intervalos ni almacenamiento de uso en relay. DST/cambio hora/
zona y pérdida conservadora necesitan ratificación; documento no promete borrado
físico exacto a21 con proceso muerto. Fallo purge no publica datos antiguos.

Push sí a evaluar, solo hint mínimo autenticado/verificado por STATE fresco;
no órdenes desde push, totales/secretos/proveedor/SDK/cuenta/token/servicio autorizado
ni endpoints en relay. Ausencia de comunicación sigue causa desconocida. PAIR/
OFFLINE/migración pendientes por acuerdo seguro de tres roles, no bypass/downgrade/
generación libre/corte de recuperación ni aprobación de puertas/H05/H06.

Decisiones breves pendientes: rango/alcance libre local; zona/DST/purga incierta y
límite físico de eliminación; lastExpiry y evaluación push/consentimiento aparte.
KDF/cupos/normalización/admin/reset/olvido doble no elegidos por este contrato.
Siguiente unidad mínima propuesta: revisar/ajustar draft cliente/relay antes de
versionado; eventual modelo puro nuevo de autorización/retención solo tras asignación.

Rutas: documento incremental nuevo, este relevo, ESTADO.md. Verificación solo
lecturas locales/texto/Git/diff; sin código/tests/builds/L02 productivo/Android/UI/
Keystore/WSS/CP1/schema/fixtures/planes/ADR aceptados, dispositivos/ADB/CA/LAN/push/
despliegue/repos ajenos/QR02. QR padre reproducido no corregido. Sin subagentes/
sesiones/prompts de vuelta o siguiente tarea automática. Commit propio en respuesta/
git log; detenerse para revisión conjunta. CP/2 sigue provisional.

## Resultado HIJO-C08 — gate final del draft integrado R1

2026-10-13, base hijo `8b25c24`. **APTO_PARA_GENERAR_CONTRATO_SEPARADO**.
Nota final: `hijo/COMPROBACION_FINAL_CP2_R1.md`. Fuente PADRE-C04-R1 `75eb56e`
§§1–14 íntegros recibidos en siete partes del coordinador, no lectura directa
externa acreditada; no reintentos/ampliación permisos. Git limpio antes de tarea.

No error material de especificación encontrado que requiera corrección de campo/
regla bloqueante. Confirmados en R1 libre LOCAL/REMOTO, ALLOW_FOR60..86400 múltiplo60
sin preselección/LOCK sin apertura, contador autorizado apagado/Keyguard/offline,
retención domingo21 local/array8/catch-up y purga UNKNOWN independientes de permiso.
Revisión/seq/contextos/ACK histórico/STATE/frescura/calidad/tombstones coherentes.
DST primera ocurrencia/primer instante posterior ratificado técnicamente para
especificación; sin imposición de zona UTC o pérdida de guardas/emergencia.

§10: formatos locales cerrados CHILD_OFFER/PAIR_ACCEPT CPv2PAIR y OFFLINEv2
boot/revisión/acción/nonce ratificados para contrato separado. Hash CPv2RECOVERY
nuevo y hashes CPv1RECOVERY legados con esquema/pair/purpose/consumos/esperas
preservados, no rehash claro/fallback WSS/recuperación que abre normal. Consumo
OFFLINE no consume código impreso por implicación; privado v2 y revalidación/commit
se requieren antes de función RAM. RECOVER_PARENT presencial conserva vínculo
hasta nuevo commit; transición sin herramientas/readback/ruta segura queda bloqueada.
No alta/migración/PAIR físico o H05/H06/puertas aprobadas por esta ratificación.

Recuento y cálculo PowerShell local §14 coincide: STATE91/84/15, payload7116/
frame9788; límite8192=>frame11224; PAIR1355/frame2108, oferta524,
OFFLINE731/frame1276. Solo aritmética/cota condicional del inventario, sin firma/
codec/script de fronteras/fixture/test/build; ejercicio relay2966/4122 no reejecutado.
No aumentar límites. Lecturas propias codec/crypto/boot/repositorio/recovery
identifican trabajo privado v2 separado, no implementación existente acreditada.

Dueño autorizó preparar versión/contrato/schema/validadores/tests locales NUEVOS
separados tras gate; coordinador iniciará asignación, no esta sesión automáticamente.
CP1/fixtures/planes/ADR aceptados intactos. Parámetros privados/ancla/capacidades/
productos Android y emisor/proveedor push siguen unidades posteriores, no cuentas/
SDK/token/endpoint/servicio aprobado ni operación pública.

Rutas: nota final, este relevo, ESTADO.md. Sin código/tests/builds/Android/UI/Keystore/
KDF/WSS/APK/dispositivos/ADB/políticas/CA/LAN/push/despliegue/repos ajenos/QR02,
subagentes/sesiones/prompts de vuelta. QR padre reproducido no corregido; L02 no
promovida. Git diff --check y commit propio de cierre; hash en respuesta/git log.
Detenerse; etiqueta APTO solo gate de especificación/formato, no prueba operacional.

## HIJO-PROT01 — prototipo reversible debug, PARCIAL

2026-10-13. Nueva autorización explícita del dueño: instalación/actualización debug
y prueba reversible solo A13 SM-A135M. A56 excluido aun estando conectado.
Evidencia: `pruebas/EVIDENCIA_HIJO_PROT01.md`.

Implementado en sourceSets debug/testDebug/androidTestDebug: actividad independiente,
tickets RAM con reloj inyectado, BiometricPrompt DEVICE_CREDENTIAL, adaptador DPM
limitado a whitelist/features, snapshot previo y rollback solo debug protegido por
DUMP para shell. No HOME/restricciones/código parental/CP1/temporizador/red.
PIN Android autoridad temporal de laboratorio, no credencial parental final.

Builds APK debug e instrumentado correctos; 205 JVM debug/0 fallos (6 nuevos),
núcleo UP-TO-DATE. Compile release UP-TO-DATE; exclusión del paquete/componentes
prototype verificada en clases/manifest release, sin APK release instalado.
Instalaciones A13 Success; prueba instrumentada focalizada 1 test OK antes y después.
Selección modelo y toda operación con `adb -s` al mismo A13, sin serie en repo.

Adulto confirmó presencialmente baseline de credencial y acceso Emergencia en
Keyguard sin llamar. Avisado antes de aplicar. Intento de entrada: informó «no hizo
nada, todo siguio igual»; no LOCKED acreditado ni causa demostrada. Se detuvo la
prueba sin repetir/endurecer. Rollback ADB result=1, modo NONE/sin snapshot
verificados; adulto confirma uso normal. No demostrar restauración desde LOCKED
si no se alcanzó ni afirmar políticas cambiadas. Inicio/Atrás/Recientes, cancelación,
emergencia/salida autenticada/reentrada bajo LOCKED no realizados/acreditados.

Bloqueo concreto: entrada sin cambio observado; diagnóstico genérico no distingue
precondición/caducidad/Android/readback. Próxima asignación, si coordinador decide:
diagnosticar y coordinar nueva prueba, no tarea automática. H05/H06/puertas/producto
no cerrados; C08 APTO contrato separado sigue sin implementar. Sin otros repos/
CP1/CP2 compartido/planes/ADR/credenciales/servicio/push/QR02/gitpush/despliegue.
APK/ruta/hash local en evidencia, no subida. Commit propio de cierre en respuesta.

## HIJO-PROT01-R1 — bloqueo debug real reversible logrado

2026-10-13, base `d38d3ad`. Evidencia: `pruebas/EVIDENCIA_HIJO_PROT01_R1.md`.
**BLOQUEO_REAL_DEBUG_LOGRADO, RESTAURADO**. Nuevas autorizaciones concretas en
sesión para instalar y, separadamente, un intento de Lock Task. Solo A13 SM-A135M,
selector ADB único explícito en toda operación; A56 excluido, sin serie en repo.

Modelo puro debug/PuertoEntrada: fases/causas cerradas, readbacks y una solicitud
por generación; NONE inicial espera hasta3s, fallo restaura con readback hasta2s.
Sin reescrituras/poll infinito/relajar tickets/lifecycle/Keyguard/snapshot/política ajena.
UI y log sanitizados, gate ADB comprobado antes de consumir credencial previa,
rollback goAsync acotado. Solo paquetes/features propios, sin main/manifest release.

221 JVM debug/0 fallos (16 nuevos), fallo intermedio de secuencia del test corregido,
no guarda relajada; núcleo y compileReleaseKotlin UP-TO-DATE. APKs construidos,
instrumentación focalizada 1 test OK antes/después; exclusión clases/manifest release
comprobada, no APK release. No declarar UP-TO-DATE como pruebas ejecutadas.

Un intento: primer readback ESPERAR_LOCKED,113ms después LOCKED_VERIFICADO;
ADB corroboró LOCKED. Confirma que espera era necesaria en recorrido actual,
no prueba causa histórica de «no hizo nada». Adulto confirmó Inicio/Atrás/Recientes
contenidos, cancelación sin salida, Emergencia/Keyguard sin llamadas y vuelta aún
LOCKED. Salida autenticada del sistema: RESTAURADO/NINGUNA, readback de políticas
previas; ADB NONE, test sin snapshot pendiente y uso normal confirmado. Rollback
externo verificado sin snapshot al inicio/final, no ejercitado desde LOCKED.

Estado final restaurado, APK debug permanece instalado. PIN solo autoridad temporal
laboratorio; no H05/H06/puertas/tiempo/remoto/reboot/proceso muerto/producto aprobados.
No segunda entrada, auditoría exhaustiva, planes/ADR/CP1/CP2/proveedor/servidor/push/
QR02/repos ajenos/gitpush/despliegue ni siguiente tarea automática. APK/hash local
en evidencia; commit propio de cierre en respuesta. Esperar revisión.

## HIJO-PROT02 — permiso debug1min y bloqueo automático desde otra app

2026-10-13, base `4faea1f`. **LOGRADO en ensayo interactivo/proceso vivo,
RESTAURADO**. Evidencia `pruebas/EVIDENCIA_HIJO_PROT02.md`. Solo A13 SM-A135M/API34,
selector ADB fijo, A56 excluido. Nuevas confirmaciones antes de install/notificación/
FGS, separada de ensayo único1min y adicional para guarda final sin ensayo.

Servicio debug no exportado `systemExempted` por Device Owner, visible; listener
AlarmManager.setExact ELAPSED_REALTIME_WAKEUP ligado a servicio vivo (API34+),
sin SCHEDULE_EXACT_ALARM/special-access por ADB/wakelock/deps nuevas. Modelo puro
de propuesta/confirmación/referencia/generación/boot/deadline original; minutos
enteros1..1440 sin preselección, autenticación Android temporal, no masterPIN.
Baseline/rollback/salida/emergencia preparados antes; vencimiento no pide PIN.
Snapshot privado/readbacks/política reducida y relanzamiento oficial LockTask;
servicio fuera Activity para volver desde otra app. Salida/rollback cancela plan.

233 JVM debug/0 fallos (12 nuevos), builds correctos; núcleo/compileReleaseKotlin
UP-TO-DATE, no ejecutados nuevamente. Exclusión clases/manifest release comprobada,
no APK release. Instrumentación 1 test OK tres veces; pequeño error de comando
shell install sin argumentos código1 y stderr no conservado, instalación corregida
Success, registrado sin inventar causa. CP1/CP2/main/planes/ADR/fixtures intactos.

Manual: eligió1min/callback adulto, abrió otra app y volvió solo al prototipo; ADB
LOCKED. Entrega alarma+17ms/solicitud+60ms/LOCKED+1308ms relativo al deadline;
no segundo exacto ni timestamp civil. Cancelación/emergencia bajo LOCKED y salida
autenticada confirmadas. FinalNONE/sin snapshot, readback políticas restauradas,
PLAN_CANCELADO/servicio terminado, uso normal/sin notificación. Grant/canal debug
notificación y APK permanecen; no nuevo plan tras actualización final.

Revisión detectó borde no ejercitado «bloquear ahora yaLOCKED»: guarda añadida
para conservar bloqueo sin writes; APK final instalado autorizado, sin repetir
minuto. Evidencia distingue hashes del APK ensayado y final. No permitir desde
LOCKED/reemplazo/bloquearahora físicos acreditados ni persistencia/crash/reboot/
Doze/apagado, no H05/H06/puertas/producto/remoto/contador/historial aprobados.
Sin servidor/repos ajenos/push/QR02/gitpush/despliegue/subagentes u otra unidad
automática. Esperar revisión tras commit de cierre.

## HIJO-PROT03 — recorrido desde LOCKED probado y restaurado

2026-10-13, base `5dbfa3f`. **RECORRIDO_LOGRADO_Y_RESTAURADO**, uno solo.
Evidencia `pruebas/EVIDENCIA_HIJO_PROT03.md`. A13 único SM-A135M/API34, todos
comandos adb -s fijos; A56 excluido, sin serie en repo. APK instalado previo
finalPROT02 hash22b896… comprobado. Presencia/autorización específica del adulto,
baseline credencial/emergencia/notificación/Owner/rollback antes de restringir.

Conflicto fuente: entrada R1 consume gate ADB y PERMITIR lo requiere; COMPROBAR
no renovaba con snapshot pendiente. Explicado y autorizado ajuste mínimo debug,
sin ensayo fallido del APK previo ni inferir causa histórica PROT01. Función pura
gate y lecturas Owner/snapshot/whitelist/features permiten renovar exclusivamente
LOCKED propio íntegro; no writes ni salida, DUMP/caducidad/auth intactos. Timer,
main/permisos/deps/CP1/CP2/planes/ADR no cambiados.

Único recorrido: entrada R1/ADB LOCKED, renovación ROLLBACK_LOCKED_PROPIO/NINGUNA
con LOCKED antes/después, adulto eligió1 y autenticó PERMITIR; servicio restauró
baseline/readback y confirmó permiso con deadline60.000ms original, ADB NONE.
Adulto usó otra app sin inventario/contenido y volvió solo, sin nuevo PIN al vencer;
ADB LOCKED. Alarma+4ms/solicitud+12ms/LOCKED+429ms, no segundo exacto. Cancelación/
emergencia bajo quiosco correctas manualmente, salida adulta/readback/finalNONE/
snapshot borrado/plan cancelado/servicio detenido/uso normal confirmados.

238 JVM debug correctos (5 nuevos), build correcto; núcleo/compileReleaseKotlin/
assembleAndroidTest UP-TO-DATE, no ejecución nueva de núcleo. Instrumentación1 OK
tres veces, sin adulto simulado. Release excluido por clases/manifest, no APK release.
APK ensayado=final local/instalado hash2d6f6468… íntegro en evidencia; sin código
posterior al ensayo, no fallos de comandos/builds/ensayo ni repetición automática.

No rollback externo desdeLOCKED/reemplazo/guarda yaLOCKED/24h/crash/reboot/Doze/
apagado/producto H05/H06/remoto/historial acreditados. PIN temporal laboratorio,
API34 interactivo/FGS vivo, sin masterPIN definitivo. Notificación grant/canal/APK
permanecen, no nuevas concesiones ni políticas amplias/Owner/PIN/ADB/HOME alterados.
Sin repos ajenos/push/QR02/contrato nuevo/gitpush/despliegue/subagentes/sesiones/
prompts de vuelta/otra tarea automática. Commit propio de cierre; esperar revisión.

## HIJO-PROT04 — preparado, impedimento físico y restauración

2026-10-13, base `478f97d`. **PREPARADO / BLOQUEADO_FISICO**.
Evidencia `pruebas/EVIDENCIA_HIJO_PROT04.md`. Solo debug/testDebug: intención RAM
VENCIDO_PENDIENTE separada de LOCKED, ref/boot/generación/deadline originales,
receptor dinámico SCREEN_ON/USER_PRESENT en servicio existente, revalidación de
interactividad/Keyguard antes de único intento acotado; no renovación ni bypass.
Salida/rollback invalida intención/callbacks/listener antes de restore. Sin manifest/
permisos nuevos/main/CP1/CP2/fixtures/planes/ADR/deps/repos ajenos.

247 JVM correctos (9 nuevos) y dos builds exitosos, núcleo/release/testAPK tareas
UP-TO-DATE no contadas nuevas; exclusión release clases/manifest inspeccionada.
Autorización concreta obtenida antes de install y luego antes del ensayo previsto.
Único A13 SM-A135M/API34, adb-s fijo, A56 excluido; baseline Owner/NONE/test1 OK,
install Success, servicio TIMER_LISTO visible, gateBASELINE/NINGUNA.

No recorrido apagado acreditado: confirmación inicial manual «aceptado; apagada»
corregida por adulto: botón de apagado/encendido averiado, no podía apagar.
Logs solo TIMER_LISTO/etapas previas, sin PERMISO_CONFIRMADO/entrega/LOCKED.
No inventar fallo de Android ni causa de falta de aceptación. Se detuvo sin
repetición/forzar pantalla/permisos. Rollback result1 RESTAURADO/NINGUNA y final
NONE/sin servicio/snapshot (instrumentación1 OK), adulto confirma uso normal.
No salida adulta desdeLOCKED/emergencia/cancelación físicas nuevas en esta unidad.

APK preparado/instalado/final hash5eff03e5… completo en evidencia; NO APK ensayado
en apagado/Keyguard. Error real de shell final pidof vacío/.Trim null exit1: hash
sí comprobado antes, último logcat no ejecutado. No matarproceso/crash acreditado.

No cero ventana tras desbloquear garantizada ni supervivencia proceso/Doze/reboot/
producto H05/H06/remoto/masterPIN/historial aprobados. Cualquier alternativa para
botón averiado/ensayo posterior requiere nueva coordinación/autorización física.
Commit propio de cierre y esperar revisión, no nueva tarea/prompts/subagentes/
sesiones/push/QR02/gitpush/despliegue ni operación Render de dispositivo.

## HIJO-PROT04-R1 — vencido bajo Keyguard acreditado, retorno parcial

2026-10-13, base08ecd81, solo evidencia/ESTADO/relevo. Dueño pidió retomar;
autorización específica1min con botón existente Emergencia/Keyguard (lockNow)
tras confirmar presencia/credencial/cargador para despertar. Sin ADB para forzar
pantalla, cambios de «No apagar conectado», grants/reinstalación/build/código.
Único A13 SM-A135M/API34, adb-s fijo, A56 excluido; Owner/NONE/sin snapshot,
hash5eff03e5… igual APK preparado/ensayado/instalado/final. Test1 baseline OK.

**PARCIAL**. Primer permiso1min explícito aceptado, lockNow manual apagó pantalla;
entrega vencida+6ms, pendiente+22ms con interactivo=false/keyguard=true/usuario
bloqueado=true; FGS vivo/ADB NONE. SCREEN_ON+120157ms aúnKeyguard. Adulto despertó
con huella y desbloqueó PIN Android, pero luego confirmó haber pulsado PERMITIR
otra vez. Log segundo permiso acredita referencia/deadline nuevos y segundo
vencimiento+5ms/solicitud+18ms/LOCKED+397ms/ADB LOCKED. **No contar dos permisos
como único minuto correcto ni segundoLOCKED como retorno del deadline original.**
No señalUSER_PRESENT/primerLOCKED observados; causa ausente no determinada,
no inferir renovación espontánea ni fallo de Android, ventana otra app no medida.

Se detuvo al detectar segundo permiso, salida adulta autenticada, cancelación/
servicio terminado/restauración por logs propios; finalNONE/sin servicio/snapshot,
test1 final OK, rollback result1, uso normal confirmado. Sin repetir cancelación/
emergencia bajoLOCKED al priorizar restore. No seguimiento otro deadline físico.
Instrumentación1 dos veces, no JVM/release/build nuevos;
pidof ausente tras salida manejado (código individual no registrado), logs
históricos por PID observado recuperados. Resto sin errores reportados.

Detalle `pruebas/EVIDENCIA_HIJO_PROT04_R1.md`. Primer vencimiento apagadoKeyguard
validado solo API34/FGS vivo/lockNow; primer retorno al desbloquear sin PERMITIR
nuevo queda pendiente. Sin producto/H05/H06/cero ventana/Doze/crash/reboot/remoto/
masterPIN/Push/CP2. Cierre commit documental y esperar revisión, no nueva prueba/
tarea/prompts/subagentes/sesiones/repos ajenos/gitpush/despliegue.

## HIJO-LOCAL01 — puerta adulta MAIN implementada y validada localmente

2026-10-13, basef9eeeea, bloque1 `6d5bbf9`. **IMPLEMENTADO/VALIDACION_LOCAL_LOGRADA**,
instalación/alta/pruebas solo tras autorización física específica. MAIN abre Activity
noexportada código/pregunta. Alta DeviceOwner/DEVICE_CREDENTIAL/ticketRAM/almacén
virgen coherente, no default/bypass ni reemplazo. Código ASCII5..10 cadena/ceros;
respuesta exacta v1 sin trim/NFC/case (explícito en UI), pregunta personalizada.
Recuperación offline solo establece código nuevo y cierra, nunca revela anterior/
abre administración/uso. Sesión RAM120s cierra fondo/salida/reentrada/proceso, no
concede/cancela permiso normal. Controles sensibles futuros todos deshabilitados.

PBKDF2HMACSHA256210000/saltsindependientes, AES256GCM/Keystore/CE/noBackupAtomicFile/
readback, marcador/versionado privado v1; artefactos perdidos/corrupción/versión
desconocida fallan cerrado, no alta libre ni regeneración clave. Espera agregada
reserva durableantesKDF,5fallos60s10fallos300s/bootregresivo reimponeespera,
respuesta/cambio no resetean cupo. No contador antirollback hardware/promesa de
pregunta fuerte; calibraciónlatencia y variantes de cancelación/BootstrapOEM
pendientes en A13; almacén Keystore aislado ya probado en cinco tests.
Permiso normalUSE_BIOMETRIC MAIN para alta, sin grantespecial ni políticas nuevas.

266debug/211release JVM correctos,19 locales nuevos compartidos, release repite;
coreUP-TO-DATE. APKdebug/test/releaseunsigned construidos, lintRelease0errores11avisos.
lintDebug fallóerrorFGS manifestdebug previo; no supresión/permisosalarmes nuevos.
Verificador APK genérico falló subcadenaFake AndroidX/Kotlinmetadata; inspección
tablasDEX propia confirmó claseslocales producto/ausencia tests locales y prototype
ausente release, manifestadulta noexportada. Cinco testsAndroid nuevos preparados
soloalmacén/aliasUUID efímeros, ejecutadosOK72.075s, nunca estadoadultoreal usado.

Rutas en `pruebas/EVIDENCIA_HIJO_LOCAL01.md`; APKdebug preparado41fdde66… y
releaseunsignedd97da195… hashesíntegros allí. APKdebugensayado=finalinstalado,
releaseunsigned no físico. Autorizaciónpresencial concreta obtenida antes de
instalar/alta/pruebas, dueño presente, únicoA13 SM-A135M/adb-s, A56 excluido;
app/test Success y capacidades1 OK antes/final. Sesión Render no opera dispositivos.

Dueño confirmó configuración tras mensajeocupado/resuelto con Actualizar una vez,
entrada por código/panelprotegido y regreso de fondo exige nuevo código, pregunta
propia→solo código nuevo→nuevo código autentica, cierre explícito/emergencia visible/
uso normal. Sin repetiralta ni leer secretos. Finalregistroadultoconfigurado/código
nuevo conservado/sesióncerrada/ADB NONE/sin snapshot/servicio. No políticas nuevas.
Cancelación/caducidad120s/esperas/reboot/desconexiónfísica/KDFindividual pendientes;
sin red deshabilitada: recuperación no usa red por implementación, no ensayooffline.

Nueva regla posteriorC08 registrada: pérdida conexión requerida bloquea aunque
tiempo, reconexión sin nueva autorización no restaura; supersede continuidadoffline
documental anterior. Señal/plazo/uso estandooffline pendientes/no implementaciónred.
Puertaadmin/recuperaciónoffline no concede uso. Reboot/ajustes/desinstalación/
enforcement/QR/remoto/Push fueraunidad; salida física nueva antes de endurecer.
CP1/CP2/fixtures/planes/ADR/deps/RecoveryController/prototipos/reposajenos intactos.
No H05/H06/producto acreditados. Cierrecommit y detener, sin otra tarea/push.

## HIJO-LOCAL02 — bloqueo MAIN y salida adulta real logrados

2026-10-13, base8533ae4, bloque1 `602586e`. **RECORRIDO_MAIN_LOGRADO_Y_RESTAURADO**,
un bloqueo real autorizado A13 SM-A135M; selecciónúnico/adb-s fijo/A56 excluido.
Modelo/puerto/actor/snapshotAtomic privados MAIN, no debugimports/CP1 reinterpretado.
BLOQUEARAHORA y SALIDAENSAYO requieren panel autenticado y otro código fresco por
operación/ticket/revisión/boot/plazo. Sesión se cierra sin retirarLOCKED; recuperación
solo código nuevo y luego nueva autenticación, no salida directa por respuesta.
Whitelistsolo propio/featuresKEYGUARD|GLOBAL_ACTIONS, sin HOME/settings/suspensión/
desinstalación/red/permiso de uso normal. Snapshot antes de writes, compare contra
baseline/propio, borrado solo readback paq/features/NONE; error/ajena no defaults.

284debug/229release correctos,18 nuevos compartidos/release repite, coreUP-TO-DATE;
APKdebug/test/releaseunsigned construidos/lintRelease0errores11avisos. lintDebugFGS
previo y verificadorFake genérico no reejecutados/ocultados/reparados. DEX/clases/
manifest separan rollbackDUMP/debug/tests del producto, MAINLock presente release.
Dos AndroidAtomicFile/políticasFAKE OK0.161s, capacidades1 antes/final, ninguna
simulaciónadulto/DPMwrites desde tests. Instalación app/test Success tras nueva
autorizaciónpresencial; baselineNONE/sinsnapshots/servicio y rollback shell ruta
probada result1 antes de restringir; no rollback externo físico desdeLOCKED probado.

Confirmaciónadicional UNrecorridoMAIN. Dueño autentica panel+acción bloqueo, reporta
LOCKED/NINGUNA; ADBLOCKED. Inicio/Recientes/cancelarpregunta sin cambio código y
cancelar salida no abrieron, emergencia/Keyguard sin llamadas funcionó; ADB seguía
LOCKED. Salida MAIN con código adulto fresco (NO PINAndroid) → uso normal/ADB NONE,
COMPROBAR_LOCAL baseline1/snapshot local ausente por readback y sin servicio/snapshot
prot. No shellrestore después de salida ni nuevaentrada/cambio credenciales reales.

Evidencia `pruebas/EVIDENCIA_HIJO_LOCAL02.md`; APKensayado=finalinstalado717a2cce…,
releaseunsigned1d5a4920… no físico, hashesíntegros allí. Registroadulto conservado,
sesióncerrada/finalNONE. No H05/H06/productfinal/boot/proceso muerto/ceroescapes/
rollbackexternoLOCKED/caducidad durante transición física acreditados. Recuperación
real LOCAL01 antecedente, enLOCAL02 pregunta solo cancelada bajo quiosco. Red nueva
vigente pero señal/plazo/usooffline pendientes; no implementación/red probada.
Planes/ADR/CP1/CP2/fixtures/deps/reposajenos/Push/QR02/servidor intactos. Cierrecommit
y detener sin otra tarea/ensayo/push/despliegue/subagentes/sesiones/promptsdevuelta.

## HIJO-LOCAL03 — protección MAIN, parcial por caché y restaurada

14-10-2026, base4431593/implementación `c7fb85e`. **PARCIAL_RESTAURADO**.
Dos políticas nuevas: setUninstallBlocked solo hijo y DISALLOW_APPS_CONTROL para
gestión de TODASapps del usuario. Alternativa granular userControlDisabledPackages
solo documenta datos/force-stop y no deshabilitar; no usada. UI/aviso global,
autorización física concreta temporal distinta de autorización install/test.
Owner/API34/puertoDI/modelo/actorIO/código adulto fresco por operación/revisión/
boot/generación/ticket vigentes. Cerrar/fondo/pregunta/LockTasksalida no retiran
protección. SnapshotAtomic privado independiente de credencial/LockTask, guardar
anteswrites/readback/compare propio local-global vs efectivo. API34 uninstallgetter
es RESUELTO, no propio: baseline true incompatible anteswrites, no falsa atribución.
Restore solo contribuciones propias, nunca clear global/otroadmin ni defaults.

306debug/251release correctos,22nuevos compartidos (release repite), núcleoUP-TO-DATE.
APKdebug/test/unsigned construidos; primer lintRelease falló NewApi guardindirecto,
corregido guardSDK directo sin suppress/minSDK/deps; último BUILD SUCCESSFUL/
lintRelease0errores11avisos. lintDebugFGS/verificadorFake previos no reejecutados/
corregidos. InspecciónDEX/manifest MAINpresente/tests ausentes/rollback solo DEBUG.
3AndroidAtomic/políticasFAKE OK0.217s tras instalación app/test autorizada Success,
capacidad1OKantes-final. Ninguna escritura DPM desde instrumentación/simulaciónadulto.

A13únicoSM-A135M/todo adb-s/A56excluido. BaselineNONE/sinsnapshots/servicio,
uninstall/controlpropio/globalpropio/efectivo todosfalse. Ruta rollbackshellDUMP
responde: sin snapshot ERROR/SNAPSHOT sin writes, no prueba de rollback desdeaplicado.
Dueño pulsó Proteger antes autorizaciónglobal, pero readbacksin snapshot/todosfalse
confirmó no aplicación (faltaba formulario/código fresco). No atribuir desinstalar/
force-stop ya inhibidos entonces a LOCAL03. Luego autorización explícita global
y UNAaplicación adulta→ACTIVA/readbackuninstalltrue/controlpropiotrue/globalfalse/
efectivotrue. Ajustes propio sin intentar acciones destructivas: desinstalar/
deshabilitar/borrardatos/force-stop inhibidos, **borrarcaché disponible**. Contradice
alcance documentado en fabricante, causa no determinada; no borrar para comprobar.
Detener sin ampliar/repetir, restauración MAIN con código fresco→readbacks todosfalse/
snapshot ausente/LOCAL02baseline1/NONE/uso normal/sesióncerrada. Confirmaciónagrupada
de entrada/recuperación/emergencia disponibles, no nueva llamada o LockTaskensayo/
Keyguarddetallado. No código/pregunta cambiados ni recoveryrealnuevo.

Evidencia `pruebas/EVIDENCIA_HIJO_LOCAL03.md`; APKensayado=finalinstalado40891e21…,
unsignedef1dd1ba… no físico (hashesíntegros allí). Credenciales/datos conservados,
no políticas nuevas activas permanente. No H05/H06/productfinal/boot/red/remoto
acreditados. Caché/rollbackexternodesdeaplicado/multiadmin/actualizaciónnormal/
persistencia físicos pendientes, DPMpersistente noLockTaskpersistente. Rednueva
vigente pero señal/plazo/usooffline pendientes, no implementada. CP1/CP2/fixtures/
planes/ADR/deps/reposajenos/padre/Render/Push/QR02 intactos. Cierrecommit/detener,
sin otra tarea/ensayo/push/despliegue/subagentes/sesiones/promptsdevuelta.

## HIJO-LOCAL04 — autobloqueo tras boot con latencia y piloto finalizado

14-10-2026, base644dbef/bloque1 `9cfe675`. **AUTOBOOT_LOCKED_LOGRADO_Y_RESTAURADO**.
Piloto MAIN código adulto fresco ARMAR_BOOT/FINALIZAR_BOOT, intenciónDPversionada
marca-registro/enrolamiento local/boot/monotónico/generación; distinto H04/CP1/snapshot
restauración. Virgensinarmado no bloquea, corrupción/perdida parcial no alta libre;
no defensa checksum contra root/pérdida total. BeforeUserUnlocked soloDP, conserva
Keyguard/emergencia, ceroCE/códigostore. Capturar snapshotbaseline ANTES DP-armar y
DPMwrites; reentrada exige snapshot/políticaspropias exactas sin recrear baseline,
erroresconservan obligación, PINNED no éxito. Lanzamiento oficial DeviceOwner,
Activitylifecycle/readbackLOCKED, una referencia/gen/intento y timeoutActivity5s/
LockTask3s; sin FGS/HOMEpersistente/suspensiones/permisos nuevos/red/ALLOW/ACK.
Finalizar adulto desarma/invalida watchers primero, restore/readbacks/NONE y borrar
DP temporal después; cerrar/fondo/recuperación pregunta no sale ni desarma.

320debug/265release correctos,14nuevoscompartidos (release repite), coreUP-TO-DATE.
Primerglobal timeout120s enlint; lintseparado correcto12avisos/flagInlinedApi nuevo,
guardSDKdirecto>=33 sin suppress y buildglobalcorrecto; final39s APKdebug/test/
releaseunsigned/lintRelease0errores11avisos (último reportUP-TO-DATE). TresAndroid
DPAtomic/fuenteFAKE OK0.258s tras installapp/test autorizadoSuccess, capacidad1antes/
final, ningunapolíticareal/credencial simulada comoevidencia. DEXbootMAINpresentes/
testausentes/rollbacksoloDEBUG. lintDebugFGS/verificadorFake previos no reejecutados/
resueltos. Plans/ADR/CP1/CP2/fixtures/deps/reposajenos intactos; H04bootcontext
seactualiza naturalmente4→5, no migración unilateral ni falsas seq/permisos.

A13únicoSM-A135M/adb-sfijo/A56excluido. BaselineNONE/sinsnapshots/LOCAL03false/
pilotoDESARMADO y rutas rollbackshellbaseline comprobadas (noaplicadoprobado).
Confirmaciónespecífica UNarmado/UNrebootmanual; armarcódigo→ADB LOCKED/boot4/gen1,
+165ms desde armado. Menúmanual inaccesible bajoquiosco/botónroto, NOrebootmanual;
dueño pidió alternativa, se obtuvo confirmación **ADICIONAL explícita adb-s reboot
UNA vez**, comandoexit0. No salir ni desarmar para reiniciar, ninguna repetición.
Dueño confirma Keyguard/Emergencia prePIN sin llamadas; ADBlecturasprevioPINno
disponibles (no habilitar/grants/rebootextra). Luego Androidunlocksinabrirmanualmain/
sinBloquear/códigoparental→pantalla hijoBLOQUEADO automática confirmada.
LogsDP boot5/gen2 LOCKED_BOOT_COMPLETED/PENDIENTE_KEYGUARD/USER_UNLOCKED y ADBLOCKED:
4068ms USER_UNLOCKED→readback (+2586ms solicitud). No medir instantePIN/ventanaefectiva
ni certificarceroescapes; receptorboot/intención/Keyguard NO éxito por sí mismos.
BOOT_COMPLETEDposterior nosegundointento/permiso. Adulto códigoMAINnuevasesión/
recuperacióndisponibles y FINALIZARENSAYO codecódigofresco→readbacksDESARMADO/
enroladofalse/sinsnapshotDP-Lock-LOCAL03/NONE/uso normal. Sinrollbackshellaplicado.

Evidencia `pruebas/EVIDENCIA_HIJO_LOCAL04.md`; APKensayado=finalinstalado3c283303…,
unsigned6491d8a3… no físico (hashesíntegros allí), código no cambiado despuésinstall.
Registroadulto/código/pregunta/datos conservados; ningún armado persistente deja
sorpresa al siguiente reboot. Crash/processkill/rollbackexternoaplicado/corrupción
física/rebootmanualseguro/otrofirmware/red/uso productivo pendientes; noH05/H06/
productfinal/remoto/zeroescape certificados. Nueva red perderconexiónbloquea/
reconectarNOabre sin autorizaciónnueva vigente sigue sin señal/plazo/usooffline
resueltos, no implementar aquí; cachéLOCAL03 no perseguida. Render no dispositivos.
Cierrecommit/detener sin otra tarea/ensayo/reboot/push/despliegue/subagentes/sesiones/
promptsdevuelta; siguiente física exige asignación/confirmación específica nueva.

## HIJO-LOCAL05 / LOCAL05-R1 — devolución posterior a LOCAL04

14-10-2026. LOCAL05 original cerró en `b2f59f8`, bloque1 `552d4b9`:342debug/287release0fallos,22nuevoscompartidos; bloqueo gateFGS/ninguna física. La entrega original quedó arriba de este archivo; esta devolución posteriorLOCAL04 elimina ambigüedad cronológica. Evidencia original `pruebas/EVIDENCIA_HIJO_LOCAL05.md` se conserva histórica.

R1 baseb2f59f8: **PREPARADO_FISICO_PENDIENTE_AUTORIZACION**. Excepción expresamente autorizada tools:ignore SOLO ForegroundServicePermission en service MAIN ControlRedService; documentación oficial HTTP200 exige almenosUNO (DeviceOwner O alarmas), diagnósticoAGP8.11.1 exigía conjunción no aplicable a rutaDO. GuardSDK>=34/Owner/notificaciones/canal conservados; puerta confirmastartForeground+callback y fallosfailclosed explícitos, sin alarmas/suppressglobal/deps/roles/prototype. Runtime todavía NO probado.

348debug/293release0fallos/errores,6nuevoscompartidos; lintRelease0errores11avisos; APKdebug/test/releaseunsignedcompilados, core-protocol:testUP-TO-DATE y3DPAndroidNOejecutados. Fuente/razón/riesgo/scopesignore/hashes/rutas en `pruebas/EVIDENCIA_HIJO_LOCAL05_R1.md`. No ADB/instalación/grants/armado/conectividad/reboot/push. A13últimoestadoLOCAL04 sin nueva consulta; A56excluido.

Esperar nueva aprobación instalación/notificación/testsaislados y armado/corte manual concreto antes del ensayoNORMAL; OFFLINE requiere fase aparte. No afirmar LOCKEDred/latencias/FGSsurvivecrash/ceroescape/productfinal/H05/H06/remoto/DNS ni usar ACK como enforcement. No otra tarea automática. Render no dispositivos.

### LOCAL05-R1 — cierre físico posterior, NORMAL logrado y restaurado

Correccióncommit `6d3e085`; cierre en commit de esta devolución. El estado PREPARADO_FISICO anterior quedó superado por autorizaciones concretas en sesiónhijo. **NORMAL_LOCKED_RED_RECONEXION_NO_ABRE_Y_RESTAURADO**; OFFLINEfísica pendiente/no autorizada, no aprobación permanente.

A13únicoSM-A135M/todoadb-s/A56excluido. Instalaciónapp-r rechazotestonly; reintento-t autorizadoexit1sin detalle, hashbaseAPKconfirmaappnueva; adultoindicócablemalconectado y autorizaciónadicionaltestsolo→installtestSuccess/3DPAndroidOK0.322s. Sin reinstalaciónautomática. APKdebugensayado=final `d6de016608a22245843def8d32062004943e0de6d3aafec2ab2d4692221dfc84`; releaseunsigned `13c529467439eee57046c4b901b965ae4568f365f67145051a8aef3bf7ba4120` no físico.

Confirmacionesseparadasnotificación/UNarmado MAIN codecódigofresco→FGSrealforeground=true/systemExempted/listo=true/LOCKED. Antes de corte se acordó nueva excepción explícita al límiterouter: propietario manualmente bloquea/restaura SOLOA13 desde su appmódem, sin sesiones consultando/controlando router/redidentificadores/credenciales/otrodispositivo. UNNORMALautoridadfresca→NONE; UNcorte→señalNO_VALIDADA4772255/deadline4782255/revocación4782281/solicitud4782507/LOCKED4783220: **10965msseñal→LOCKED**,713mssolicitud→LOCKED. Corte físico exacto no medido; ni DNS/relay/deteccióninstantánea garantizados.

Restauraciónmanualred→VALIDADA4877235 mantieneBLOQUEADO/LOCKED/gen3, no nuevaautorización/autoapertura. FINALIZARRED MAIN codecódigofresco desactivaobserver5016642 antesdesarmeboot5016707/restore5016885/finalred5016893. Finalreadbacks: NONE/REDactivo-listofalse/deadlinenull/bootDESARMADO-enrolfalse/LOCAL02baseline/LOCAL03snapshotfalseytodosfalse/servicioausente/cuatroDPpilotoausentes/uso normal. Credencial adulta conservada; sin shellrollbackaplicado.

348debug/293release0fallos/lintRelease0errores11avisos/6JVMnuevos; coreUP-TO-DATE;3AndroidrealesahoraOK. LímitesFGScrash/Keyguard/bootinvalidaambos/latenciaotrasfirmwares/OFFLINEreal pendientes, noH05/H06/productfinal/remoto/24h/ceroescape. Evidenciaíntegra `pruebas/EVIDENCIA_HIJO_LOCAL05_R1.md`/ESTADOactualizado. No reboot/kills/Doze/grantsADB/LOCAL03/endurecimiento/reposajenos/push. **Entregado y detenido**, sin otra tarea ni ensayo automático. Render no dispositivos.

## HIJO-LOCAL06 — permitir por tiempo MAIN, preparación

14-10-2026, base2582dea. **PREPARADO_FISICO_PENDIENTE_AUTORIZACION**. Campo vacío/elegir1..1440min enteros/NORMAL-OFFLINEtemporales/códigofresco-propósitos-distintos/checkboxOFFLINEfalse/refpreviaKDFcapturada. Inicio-deadlineRAMelapsed/addExact-multiplyExact aceptados con preparaciónlista; ninguna duración por defecto. Últimaacciónválida reemplaza/timersviejosinvalidados; fondo/otraapp/pantallaapagada no pausa; boot/proceso nunca reconstruyenuso. Red10srevocaNORMALaunquequedetiempo/reconexiónNOabre; OFFLINE tambiénvence. ObligaciónDP/readbackantesenforcement, Keyguardpendiente, errornofallbackALLOWED.

SchedulerMAIN únicoPlanPlazo/puertoinyectable/AlarmManagersetExactELAPSED_REALTIME_WAKEUP OnAlarmListenerAPI34 dentroFGSvivo, sinpermisoalarma/cron/poll/wakelock/cambiosmanifest. IgnoreFGSR1único se mantiene. Doze/lifecycle/crash/latencia no garantizados.377debug/322release0fallos29nuevoscompartidos/lintRelease0errores11avisos/APKdebug-test-releaseunsignedconstruidos; coreUPTODATE.4DPAndroidcompiladosNOejecutados. NingúnADB/install/armado/notif/corte; últimaexcepciónmódemR1NOheredada.

Evidencia/hashes/rutas/fuentes/tests/límites: `pruebas/EVIDENCIA_HIJO_LOCAL06.md`. Solicitar NUEVA autorización instalación/test y armado/UNNORMAL1min→otraapp→LOCKEDauto→cierreMAIN-cancelación antesdesarme/restauración. OFFLINEtemporal/cortes requieren aprobación separada; ninguna nueva tarea, contratos/planes/ADR/deps/reposajenos/push. NoH05/H06/productfinal/24hfísico/rebootuso/crash/ceroescape/remoto. Render no dispositivos.

### HIJO-LOCAL06 — cierre físico posterior: temporal MAIN NORMAL logrado/restaurado

Código/preparación `6ef3413`, cierre en commit de esta devolución. **TEMPORAL_NORMAL_1MIN_LOCKED_AUTOMATICO_Y_RESTAURADO**. PREPARADO_FISICO anterior superado por NUEVASconfirmaciones específicas instalaciónapp-test/4DP y notificación/UNarmado/UNNORMAL1min/otraapp/cierre. A13únicoSM-A135M/todoadb-s/A56excluido; install-r-tapp-testSuccess/4AndroidDP-contextoFAKE-OK0.339s (sinpolíticas/credencialreal). No cortes/redmodem ni herencia excepciónrouterR1/OFFLINE/reboot/kills/Doze/pantallaapagadaintencional.

BaselineNONE/desarmado/snapshotlimpio/LOCAL03todosfalse antesarmado. Panel/códigofresco UNARMARRED→LOCKED/FGSforeground=true/systemExempted/listo=true/redVALIDADA. Adulto confirma campominutosvacío/elige1min/códigofresco/usaotraappsinvolver/pulsarBloquear/nuevo códigoalvencer. Traza inicio6811673/deadline6871673; USO_APLICADO_NONE6811866; eventosredno renovarondeadline; entregaListener6871690(+17ms)/obligaciónBLOQUEADOgen3 6871714/solicitud6871986/readbackLOCKED6872782(**+1109msdeadline/+796mssolicitud/+61109msinicio**). AutomatismoMAINproceso vivo; no segundoexacto/zeroescape/Doze/crashcertificado.

FINALIZARREDadultocódigofresco: cancelaciónref-plazos-observer7172282 antesdesarmeboot7172359/restore7172548/finalred7172558. Final NONE/REDactivo-listofalse/deadline-inicio-vencimientonull/bootDESARMADO-enrolfalse/snapshotbaselineLOCAL02limpio/LOCAL03todosfalse/servicioausente/cuatroDPpilotoausentes/panelcerrado-usonormal. Credencialconservada; no shellrollbackaplicado. APKdebugensayado=finalinstaladoSHA256 `48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092`; releaseunsigned `c410bd84857c3c5051616952254e1ed10f3dd08ef525fbbc4d0303c896b5e9b2` sinfísica; código no cambiado trasinstalar.

377debug/322release0fallos/29JVMnuevoscompartidos/lintRelease0errores11avisos/coreUPTODATE; cuatroDPahora ejecutados0.339s. Evidencia `pruebas/EVIDENCIA_HIJO_LOCAL06.md`/ESTADOactualizado. OFFLINEtemporalreal/24hfísico/pantallaapagada/Doze/bootinvalidauso/crash/Keyguarddeadline/otrofirmware/rollbackexternoaplicado pendientes; noH05/H06/productfinal/remoto/CP2/ceroescape/supervivencia/DNS garantizados. Sin contratos/fixtures/planes/ADR/deps/reposajenos/push. **Entregado y detenido**, no otra tarea ni ensayo automático. Render no dispositivos.

## HIJO-LOCAL08 — entrega parcial, OFFLINE vence/restaurado, Keyguard pendiente no acreditado

**Cierre posterior por aceptación del propietario:** tras entrega `aeae31a`, indicó «igual funciono, entonces cierra eso». LOCAL08 queda CERRADA_POR_ACEPTACION_DEL_PROPIETARIO por vencimiento/bloqueo OFFLINE y restauración observados. La evidencia física parcial y límites siguientes se conservan: apagado antes del deadline/primer reenforcement tras Keyguard no acreditados; segundo permiso separado. Sin repetir ensayo, nuevos comandos físicos, instalación, código ni pruebas; actualización solo documental y detener, sin siguiente tarea automática. LOCAL07 cancelada intacta.

14-10-2026, base45476e6; commit de cierre incorpora este relevo. **PARCIAL_OFFLINE_1MIN_VENCE_Y_RESTAURADO**. No código/install/builds/tests nuevos. DiffpendienteESTADO inicial era registro del propietarioLOCAL07cancelada pedido en conversación, procedencia revisada/preservada íntegra e incorporada documentalmente; no implementar/probarDNSVPN. ReglaNORMAL10s/reconexiónNOabre intacta.

NUEVASaprobacionesselecciónA13/lecturas/hash/launcher y UNarmadoFGSvisible/UNcorte-restauraciónmanualSOLOA13appmódem(excepciónnuevaexplicitanoheredadaR1)/UNOFFLINE1mincheckbox-códigofresco/UNlockNowexistente/AndroidunlocksinotroPERMITIR/cierreMAIN. BaselineNONE/boot-REDdesarmados/snapshotslimpios/LOCAL03false/servicioausente; únicoSM-A135M/todoadb-s/A56excluido/sinseriesrepo. AndroidNO_VALIDADA11417850; inicioOFFLINEgen2 11606645/deadline11666645/USO_APLICADO_NONE11606869/entrega11666657(+12ms)/obligaciónBLOQUEADOgen3 11666701/solicitud11666785/LOCKED11666982(**+337msdeadline/+197mssolicitud**), sinrevocaciónRED10s ni códigoalvencer.

LecturaposteriorpantallaOFF-Keyguardtrue **después** deLOCKED, noPENDIENTE_KEYGUARDenprimerdeadline. Adultoconfirmaquesebloqueóantesdelapagado; Androidunlockposterior mantuvoLOCKEDpreexistente, NOpruebaprimerreenforceKeyguardpendiente. Agente no solicitósegundoPERMITIR; alrevertircorte se detectógen5/otrareferencia. Adulto confirmó acciónautónoma adicional: appmódemfalló, concedióOFFLINE**2min** y cambió a otraWi-Fi. Gen4/inicio14581667/deadline14701667/redVALIDADA; segundoLOCKED14702629/+962msSEGUNDOdeadline **NO valida el original**. Nada de routertools/SSIDIPMAC/credenciales/otrodispositivo operado por esta sesión. No looppara verde ni autorizaciónextraautomática.

FINALIZARREDMAINcodecódigofresco y panelcerrado: cancelrefs/plazos/observer15775028 antesdesarme15775098/restore15775272/finalred15775278; finalNONE/REDinactivo-listofalse/deadline-inicio-vencimientonull/bootDESARMADO-enrolfalse/LOCAL02baseline/LOCAL03todosfalse/servicioausente/cuatroDPpilotoausentes/uso normal; adulto confirma conexiónpreviarestablecida, sinconsultarIDs/redcontinuidadgarantizada. APKinstalado=ensayado=finalLOCAL06SHA256 `48ddcdda31c997c3b7fc801f6602af36c5ba770e5bb0a1c370aa5d65c9d3a092`; unsigned `c410bd84857c3c5051616952254e1ed10f3dd08ef525fbbc4d0303c896b5e9b2` existente/sinfísica. No shellrollbackaplicado/cambioscredencial/Owner/PIN/ADB/grants/LOCAL03/reboot/kills/Dozeinducido.

GradleJVM/lintSUCCESS2s/57tareas1ejecutada56UPTODATE, JVM/análisis-reporteUPTODATE/no nuevocasoejecutado, resultadoprevio377debug/322release/0fallos/lintRelease0errores11avisos.4AndroidLOCAL06NOreejecutados; lintDebugprototype/verificadorFakeprevios pendientes. Evidenciaíntegra `pruebas/EVIDENCIA_HIJO_LOCAL08.md`. NoH05/H06/productfinal/remoto/bootuso/crash/ceroescape/24hfísico/Doze garantizados. **Entregado/restaurado/detenido**, primerdeadline bajoKeyguard pendiente sigue NOacreditado; cualquierreensayo precisa asignación/confirmaciónespecífica nueva. Sin CP1/CP2/fixtures/planes/ADR/deps/reposajenos/push/otra tarea. Render no dispositivos.

## HIJO-PAIR01 — oferta CP/2 real protegida, sin vínculo ni ceremonia

2026-10-14, base45f7a44/bloque71e4e2a/cierre en commit de esta evidencia. **OFERTA_REAL_PRESENTADA_Y_RETIRADA_SIN_VINCULO**. Fuente CP2-V01 Render4ac4962 schema/README íntegros suministrados por propietario antes de asignación, no lectura/verificación externa. Render117/13 solo reportado. Draft75eb56e§§2–3/10.1/gateC08 3cc206e autorizan esta unidad separada, NO CP2completo/migración. Copia textual schemaLF SHA2567290e6cb419e910756d3848586be24358f063455b1c65c47f24e75b5cef22239/hashlocalnoacreditaremoto; READMEpropio procedencia/alcance.

MAINmodelo/codecCHILD_OFFER6campos canónicosASCII-v2/cp.v2/SPKIP256DER/base64url/UUIDv4/nonce32SecureRandom, sinfirma/códigos/permisos/paircommit. Preflightprofundidad6/8192payload/16384frame antes parser, primitivas técnicas CP1 intactas. OfertaRAM5min generación/boot/identidad/ticketadulto; sesión120s NOalargada, cancelar/fondo/expiración/contextoincierto/callbackviejo/nuevaoferta/proceso retiran/noautoQR. UIadultoMAINFLAG_SECURE/codecódigo fresco por GENERAR_OFERTA, QrZXing existente/margen4/M/textooriginal sincopiar/share/log/persistencia; recepciónPAIR_ACCEPTdeshabilitada.

No alta productiva previa en código C02: fuenteleeKeystoreexistente. Alta separada INICIALIZAR_IDENTIDAD añadida solo ausencia inicialcoherente/estadoPREPARACIONsinvínculo; intenciónCEantesKeyStore/privateSigningP256noexportable/públicavalidada/readback y exclusiónglobal. Clave/registrodiscrepantes o creacióninterrumpidaNOsonvirgen, no auto-reemplazo/rotación/borrado. PREPARACION/consumos/credenciallegados conservados; no migración.

JVMfinal401debug/346release0fallos/**24casosnuevosporvariante** (20Oferta/2QR/2Sesión), codec/UUIDbits/b64bits/keycurve/limites/schemahash/RAM/contexto/callback/reloj/generaciónlenta/autoridad2min/QRópticosintético bytesexactos. Primerbuild2m18s32ejecutadas97UPTODATE394/339/lint17avisos; segundo2m6s39ejecutadas90UPTODATE401/346/lint11avisos; último48s25ejecutadas104UPTODATEtrasmonitorKeystoreJVMexecutados401/346/lintanálisis ejecutado/reporteUPTODATE0errores11avisos. Buildsdebug/test/releaseunsigned pertinentes; AndroidTestpaqueteUPTODATE/NOinstrumentación. InspecciónDEXdirigida MAINpresente/nuevostestsausentes/fixturesnoempaquetados/rollbacksoloDEBUG/ignoreFGSúnicomismonodo; no verificadorFake/lintDebugprototype resueltos.

NUEVASaprobacionesconcretas: preparación-instalarSOLOappdebugA13-r-t/UNAidentidadcondicionada/3ofertasrealescodigofresco(cancelar-fondo-caducidadsesión2min). ÚnicoSM-A135Mcomprobado/todoadb-s/A56excluido, baselineNONE/pilotosdesarmados/snapshotslimpios/servicioausente. UNAinstallSuccess; primerhashposteriorfallóporrutaAPKvieja, pm pathactual/hashcorrecto sinreinstalar. Adultoconfirmaidentidadinicializadaconreadback, QRvisible-retiradocancel/fondosinautoreaparición/caducidadadministrativaypanelcerrado. Confirmaciónpresencial, sincapturas/logframes/UI dumps/payloads/códigos/nonce/serialesrepo. No escaneoreal conpadre/A56/cámara ni5minUI acreditados.

FinallecturasNONE/REDinactivo-listofalse-deadlinenull/bootDESARMADO-enrolfalse/LOCAL02baseline/LOCAL03sinSnapshotfalse/servicioausente/cuatroDPpilotoausentes; identidadCEpresentesoloexistencia/Keystorecreadoautorizado, QRretirado/sesióncerradaconfirmadosadulto. Sinpolíticas/boot/red/radios/router/reset/grants/Owner/PIN/LOCAL03/DNSVPN/Doze/reboot/kills. LOCAL07cancelada/LOCAL08closedacceptparcial intactos. APKdebuginstalado=ensayado=finalSHA256 `0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9`; unsigned `74a43135dc7f4cf577610b4b5d14774d9ea0c2fcccd786440dbb06a41ea32060` sinfísica. Código no cambió tras física. Evidencia/rutas/limitaciones `pruebas/EVIDENCIA_HIJO_PAIR01.md`, ESTADOactualizado. **Entregado y detenido**: sinPAIR_ACCEPT/commitpair/ceremonia/remoto/migración/CP2freeze/H05-H06-productfinal/ceroescapesgarantizados. Sin CP1/fixtures/planes/ADR/deps/reposajenos/push/subagentes/otra tarea. Render no dispositivos.

## HIJO-PAIR02 — validación PAIR_ACCEPT CP/2 solo RAM, sin commit ni física

2026-10-14, base0697bf5/bloqueaf22188(~487líneasKotlin)/cierre en commit que incorpora este relevo. **VALIDACION_RAM_PROBADA_SIN_COMMIT_NI_FISICA**. FuenteCP2-V01 Render4ac4962 schema/README suministrados previamente y copia/procedenciaPAIR01 conservadas; no lecturas externas. Padre82734c7 codec/lectorMAIN/casoRAMprobados pero firmaMAINnoconectada por snapshotincompatible/noinstalado y Render125CP2A01 son SOLO reportados, no reejecutados/verificados hijo. No ceremonia lista/CP2freeze/migración. Discrepanciadeconteo avisada: esquema exactoaceptación10campos, no11; sobre5/oferta6/recoveryHashes3, schema intactoSHA7290e6cb…22239.

Nuevo coreprotocol/cp2/PairCp2 DTO/codec/sobre/verificador: JSONASCII/UTF8/canonicalidadexacta/closedfields/duplicados/tipos/versiones/8192payload-16384frame-depth6antesparse/Base64roundtrip/UUIDv4/P256SPKIcanónico/origenHTTPSexactosin normalización/DER8..72r,s1..n−1 sinJOSE. SHA256ECDSAsobreUTF8CPv2/PAIR+LFreal+payloadoriginal, públicaesperadaINDEPENDIENTEdelmensaje/candidataigualdeclarada. Integridadcon candidataNOconfianzahumana/ofertaviva. ResultadoinmutableDTO/bytesoriginales/hashpayload/copiadefensiva, sinfirmanteMAINprivado ni cambiosCryptoCP1.

AppValidacionPairRam exige confirmaciónexplícitaambashuellasSHA256SPKI+origen y ticketVALIDAR_PAIRcodecódigofresco/revisión-boot-plazo vigentes +OfertasRam referenciaACTUAL/identidad-generación-nonce-enrollId. Guardsantes/despuéscripto/cancelación/reentrancia/callbacks/consultaobsoleta no borraresultado nuevo. Una validaciónRAMporoferta, cancelarno rehabilitaconsumo; no replaydurable/storepair/promociónPREPARADO-activo/DP/CE/claves/permiso/recuperación. GetterresultadoActualrevalida, snapshotprevionoesautoridad/commit. NoBodylogs/persistenciasobrenonce.

UI receptoraseguedesHABILITADA, sin scanner ni permisos nuevos. UI PAIR01 ligavidaQRaticketGENERAR_OFERTA y nuevopropósitoinvalidaese ticket; no se elude guarda para integrarVALIDAR. Caso puro usaOfertasRam/SesionAdultoreales conpuertosJVMcontrolados, vidaoferta a sesiónoriginal120s ademásmodelo5min: codigoFRESCOtardíoNOamplíaoferta. PuenteUI/lifecycleconfirmación/snapshot-persistencia/firma padre aún unidades posteriores explícitas, no integración real acreditada.

Pruebasfinales **182core/416debug/361release0fallos,13core+15appnuevosdistintos** (14ValidacionRAM+1Sesion, no duplicarvariantes). Firmasreales/clavesefímerasJVM/contexto-reloj-fuentesfake; canon/bytes/hash/crossdominio-v1-v2-otropropósito-LF/DERrango/clave/noncecontexto/confirmaciones/plazos/revisión/replay/callbackviejo/fondo/boot/proceso/reentrancia. Comandocore:test/appJVM/builddebug-test-releaseunsigned/lintReleaseSUCCESS1m25s33ejecutadas98UPTODATE(inicial180/405/350); intermedias17s7ejecutadas124UPTODATE407/352 y17s7ejecutadas50UPTODATE409/354; final29s31ejecutadas100UPTODATE182/416/361. LintMAIN-testanálisis ejecutado/reporteUPTODATE0errores11avisos; AndroidTestúltimoUPTODATE/noinstrumentación. InspecciónDEXdirigida nuevosMAINpresentes/testsausentes/fixturesnoempaquetados/rollbackDEBUG/ignoreFGSúnico; no verificadorFake/lintDebugprototype resueltos.

APKbuildfinal **NOinstalado/NOfísico**, debugSHA `a198093ea7acb5cb4c193003eb711b3c46caa32551634f6fc5789cdcbf296328`, unsigned `982f1f07669be7a8c4b9eda0957604921e511bf437cdca200ad86a9f53d82e7a`. ÚltimoinstaladohistóricoPAIR01 `0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9` sinconsultaactual. NingúnADB/A13/A56/ofertaQRreal/instalación/Keystore/políticas/boot/red/DNSVPN/LOCAL07-LOCAL08/remoto. IdentidadinicialPAIR01/credenciales/baselinefísicohistóricoNONE-desarmados se conservan sin nueva lectura ni cambios. Evidencia `pruebas/EVIDENCIA_HIJO_PAIR02.md`, READMElocal/ESTADO/relevoactualizados. CP1/schema/fixtures/OfertasRam/UI/manifest/planes/ADR/deps/reposajenos intactos; sinpush/despliegue/subagentes/sesiones/promptsdevuelta/otra tarea. **Entregar y detener**, no commitpair/ceremonia/migración/H05-H06-productfinal/recuperaciónoperativa/ceroescapesgarantizados. Render no dispositivos.

## HIJO-PAIR03 — recepción/confirmación/commit CP2 integrados MAIN, sin alta real

2026-10-15, base7291dde/bloque1 `1c6e1d3` (~450líneasKotlin-config-pruebas)/cierre en commit que incorpora este relevo. **INTEGRACION_MAIN_COMPILADA_SIN_VINCULO_REAL**. PAIR01/PAIR02 y fuenteCP2-V01 suministrada sin leer otrosrepos; schemaSHA7290e6cb…22239intacto/10PAIR-5sobre-6oferta. PadreF02lecturaoferta-huella-cierre/no vinculó/código0b010d7instalado y Render133contextoAUTH/transporteCP2local-nopúblico son SOLO REPORTADOS, no ejecutados hijo. Firma/persistenciaCP2padre próximaPAIR02 pendiente; no ceremonia compatible lista.

PuenteMAIN: MarcoSesionAdulto referenciaoriginal/revisión/boot/deadline120s separado de ticketGENERAR/VALIDAR/GUARDAR, cada operación otro código fresco limitadoalmarco. OfertasRam autorizaciónGENERAR distinta de vidasesión, misma referencia/identidad/boot/generación/nonce/enrollId/límite5min, sin renovar por propósito o autoridadfresh. QRretiradoalrecibir/fondo/cancelar/caducidad; ninguna restauración/ofertaautogenerada.

AccesoAdultoActivity conecta cámaraPROPIACamera2/ImageReaderYUV/TextureView/ZXingcoreexistente, sinSDKnuevo/Intentexterno/archivos/capturas/logbody/clipboard. CAMERA peligroso/runtime con diálogo explícito/deny-safe: pedir/negar retira oferta-sesión, no autoescanea tras permiso. Integridadcon candidata del QR NOidentidadhumana; UIpública/ambashuellas/origen, huellaPADREyorigenesperados introducidos desde pantallaindependiente/sin defaults + casillapresencial. Luego códigoVALIDAR y ValidacionPairRam real (no bypass); otro códigoGUARDAR irreversible con misma referencia/resultado/marco/contextofresco. Commit mantiene escritorAdultoAndroidocupado/revisiónrealestable; no cambia código/pregunta/maestro/privadas ni permisos.

AlmacenPairCp2/PairAndroid/ArchivosPairAndroid: registroLOCALversion1/perfilcp.v2 CE/noBackupvinculo-cp2-v1 separado deCP1/adulto/boot/consumos/DP. AltasolousenciacoherenteCP2+legadoPREPARACIONnooperativo;ACTIVO/corrupto/incierto no migra/sobrescribe. Intentoexclusivo+syncantesreservaAtomicFile/hashregistro; registroAtomicFileoriginalPAIRbytes/hashpayload/públicasesperadas/consentimientolocalversión-huellas-origen-revisión-boot-generación/noncehashconsumidodurable/recoveryHashesexactosderivadosPAIR. Unescritor/reentranciacerrada, replay/segundaguarda rechazados; restoparcial/crashreserva exigeincierto/no nuevaalta. Readbackexacto+firma+hashes+consentimiento antespublicar. Si sesión se pierde DURANTEIOirreversible autorizado, conservarhechodurablesinautoridad/GUARDADO_SIN_SESION; no inventarrollback. Getterduranteescritura incierto; restorevalida con candidataESPERADAguardada y mismaidentidadlegadocompatible, no confíaenpublicanueva. Sin defensa root/borradototal/IOhardware garantizada. No recuperaciónCP1activada/ALLOW/canal/orden/STATE/ACK/recibonuevo.

EstadoUIhonesto **VINCULO_CP2_GUARDADO / PENDIENTE_CANAL_Y_RECUPERACION**, noAPPLIED/uso/H05cerrado/listo. Getterlocalsnapshotpara transportefuturo, no conectarloautomáticamente. No vínculoREALguardado en esta sesión.

Final **182core/428debug/373releaseJVM0fallos/errores**,12appcasosnuevosdistintos(11AlmacenPair+1Sesion), firmasreales/clavesefímeras/fakeIO-crash-reloj/contexto, regresiónOferta20/Validacion14/core182. Puentevida/ticketfresh/revisión/fondo/reentrancia/replay/corrupción/readback/restore/nonceconsumido/candidataesperada/crashsimulado/ventanasIO. Comando full core:test/appJVMdebug-release/assembleDebug-AndroidTest-release/lintRelease **--rerun-tasks --console=plain FINAL SUCCESS1m2s/131tareas131ejecutadas/0UPTODATE**, lintMAIN-test-reporte0errores11avisosprevios/ignoreFGSúnicointacto. AndroidTestArchivosPairaisladoaleatorio sólo compilar/empaquetar, NOinstrumentación. Pasadasprevias: bloque1full55s34exec97UPTODATE/corecached; UI1FAILED1m18s typo test `.datos` corregido (no pruebas acreditadas); UI2SUCCESS2m13s31exec100UPTODATE425/370/corecached/lint12avisos, SetTextI18n corregido recurso; rerunprevio1m13s131exec182/428/373antesrevisión.new/guardCE. `.new`solitario conservado antesopenRead para no ocultarrestos, pruebaAndroidsolo compilada. Evidencia exacta `pruebas/EVIDENCIA_HIJO_PAIR03.md`.

InspecciónDEXfinalrepetidadirigida nuevosMAINpresentes/testsausentes/fixturesfuera/rollbacksoloDEBUG/schemahashintacto/manifestCAMERAsinaudio; noAPKexhaustivo ni lintDebug/verificadorFake resueltos. Buildfinal **NOinstalado/NOfísico** debugSHA `257ea98f0f36d723cca6f5a5f3265350de31e92f4f89354ef63973c0e2dc08cf`, releaseunsigned `55b88e9390a16add6004b2590a6b3a4c9a3e9f0cde78435b5e9c16f7a18bf807`. ÚltimoinstaladoacreditadoPAIR01histórico0d363886…4344e9/baselineNONE-desarmados/identidadprivateSigning-registroCE/credencialpreservados, sinconsultarlosahora. NingúnADB/A13/A56/install/QRreal/escaneo/permisoCAMERA/alta/identidadnueva/política/boot/radios/router/red/Owner/PIN/reset/Doze/DNSVPN/LOCAL07-LOCAL08. Archivos modificadosMAINpairing-local-UI/manifestCAMERA-strings/JVM/androidTestaislado/READMEcp2/ESTADO/evidencia/relevo; sin core/CP1/schema/fixtures/planes/ADR/deps/reposajenos/subagentes/sesiones/promptsdevuelta/push/deploy. **Entregar y detener**: nueva asignación y aprobaciónconcreta antes de cualquier ensayo/alta en teléfonos; no heredar lecturaF02 ni permisosanteriores. Canal/recuperaciónCP2/firmapersistenciapadre/ceremoniareal/H05-H06/productofinal pendientes. Render no dispositivos.

## HIJO-INTEROP01 — PAIR público Node aceptado JVM; vuelta pública entregada

2026-10-15, base `3f12aa6`, cierre en commit que incorpora este relevo. **NODE_PAIR_ACEPTADO_JVM / VUELTA_PUBLICA_JVM_GENERADA_Y_REVALIDADA / NODE_VUELTA_PENDIENTE**. PadrePAIR02 `2511353` MAINfirma/persistenciapúblicaCP2pendienteimplementados/noinstalado y RenderCP2T01authsocketsNode143correctos son SOLOreportados, no ejecutados hijo. PAIR03 aceptada documentalmente/noinstalada no implica ceremonia ni producto listo.

FuenteCP2-I01Node24 íntegra transmitida/copiada sólo recursoJVM `android/app-child/src/test/resources/cp2/cp2-i01-publico.json`, hashUTF8LFfinal **967ae05aecd28c6b7f90b8ae90e8421b408dd70acbf22fd30911085c69a964ec** exacto fijado por prueba. VectorpúblicoefímeroSINautoridad, NodeprivadasRAMproceso terminado según propietario; no identidad/pair/nonce teléfonos. Oferta/PAIR aceptados por CodecOfertaCp2+CodecPairCp2/CriptografiaPairCp2 reales/públicaPADREexterna esperada/hashpayload/nonce-enroll-ambasclaves-origenexacto/UTF8canonical/bytesoriginales/LFreal/conjuntoscerrados6-5-10; no sóloJCAverify. Negativos claveexterna/payloadmutado/camposfaltantes-extras/whitespace y prefijoCPv1-LFliteral-otropropósito con firmasJVMreales efímeras rechazados por borde real. **AUTH_NO_EVALUADO_HIJO**, dosstringsAUTHfuente conservados informativos, sincodecAUTH/enforcer/CP1/canal implementados.

ÚNICA salida públicaparaNode `pruebas/interop_cp2_jvm_publica.json`, exactamente6clavesfixture-plano, SHA256UTF8LFfinal **fa04fc863d7bace67b5036751e04865e2e6d90047d333252e4a70d21f5b36bf6**. Padre/hijoP256JVMefímeros/UUIDv4/nonce32/hashesrecovery32aleatorios3propósitos/origenhttps://relay.example:443, PAIR firmadoJCAenTESTSHA256ECDSADER/domCPv2PAIRLFreal/serializadorescodecs. PrivadassóloRAM/noPEM/noexport/noKeystore/nocredencialesreales. Verificacióncodec/cripto antespublicar y relecturadespués; flagexportexplícito requiereausenciaarchivo/no sobrescribe. **Nodeverificadordevuelta NOejecutado/PENDIENTE**, no declarar interopservidor completo/identidadhumana/persistenciareal/ceremonia/recuperación/canal/permiso por ello.

JVMtestnuevo `pairing/InteropCp2Test.kt`:5casosdistintos. ExportUNAvez con envHIJO_INTEROP01_EXPORTAR=SI y `:app-child:testDebugUnitTest --tests dev.controlparental.child.pairing.InteropCp2Test --console=plain` SUCCESS4s3exec24UPTODATE/4casosiniciales; flagretirado/avisouserdirnullablecorregido sólotest/añadido5ºrelectura/sinregenerar vector. FINAL `:app-child:testDebugUnitTest :app-child:testReleaseUnitTest --console=plain` **SUCCESS12s7ejecutadas43UPTODATE/433debug378release0fallos-errores**,5casosnuevosporvariante(mismos5distintos), core/productivocached NOcontados como pruebas ejecutadas. Sin rebuildAPK/lint/AndroidTest/instrumentación/Nodeverificadorexterno.

InspecciónZIPdirigida APKsEXISTENTESPAIR03 sin nombres/bytesvectores/etiqueta/test/públicas/stringsQR de ambos vectores, hashesdebug257ea98f…dc08cf/unsigned55b88e93…8bf807intactos; defaultsourceJVMtest/configsin cambios excluye recursos de MAIN, NOAPKnuevo reconstruido. Evidencia `pruebas/EVIDENCIA_HIJO_INTEROP01.md`/ESTADO/relevoactualizados; rutasúnicamente tests/recursos/salidapública/docs, sin producción/core/schema/CP1fixtures/planes/ADR/deps/reposajenos/subagentes/push/deploy. NingúnADB/A13/A56/install/QRreal/cámara/credenciales/alta/Keystore/boot/red/protecciones/DNSVPN/LOCAL07-LOCAL08/migración. **Entregar y detener**, esperar nueva asignación, sin permisos físicos heredados ni otra tarea automática. Render no dispositivos.

## HIJO-AUTH01 — cliente CHILD callable, sin socket/TLS/Android físico

2026-10-15, base275f072/bloque `f3a2900` (~440líneasKotlinconfigpruebas sin schema suministrado)/cierre en commit que incorpora este relevo. **AUTH_CHILD_COMPILADA_Y_PROBADA_JVM_SIN_CANAL_NI_ANDROID_REAL**. Propietario reporta Render aceptó vueltaPAIRhijoSHAfa04fc86…b36bf6 y padreoferta/PAIR/2AUTHfb830c79…65d4b: cruce local aprobado, no ejecutado/verificado aquí ni identidadhumana/ceremonia. Actualiza pendienteINTEROP01 por resultadoexternoREPORTADO. Rendercodecórdenes/padreSYNC-SET_POLICY paralelos son contexto, no implementación hijo en esta unidad.

Fuente íntegra schemaCP2-A01 Renderd99ddac copiada separada `contrato-cp2/autenticacion.schema.json`, SHAUTF8LFfinal **db96b66ec81a379836751491244bb498a3b45ad75428593d9ea2c8755e0dd17a**, sin cambiar CP1/schemaPAIR10campos/sobre5/oferta6. Corecp2/AuthCp2 DTOcodec CHALLENGE5/AUTH_PROOF7/AUTH_OK4/PING-PONG3/SIGNED-AUTH5, cerrado/canónicoASCII-UTF8/duplicados/8192payload16384frame/depth6/Base64roundtrip/UUIDv4lower/nonce32/P256SPKI-DERr,s1..n−1/prefijoUTF8CPv2AUTH+LFreal+payloadoriginal/sin alg-key-incluida/fallbackv1. Verificacióncon pública-yrolesperados externos; emisiónMAINsóloCHILD/puerto privadanoexportable/autoverificación, no firmantepadreMAIN.

Appauth/ClienteAuthHijo+AuthAndroid reales: iniciar explícito por futurotransporte, snapshotPairAndroid.cargarvalidado/revisiónhashregistro/propiaidentidadpública-boot/ambaspúblicas-pairId-origen estables. AliasprivateSigning EXISTENTE/registroCEpreservado/FirmanteAndroidKeystore sincrear-importar-rotar-exportar/credencialdummy. Incierto/incoherenteno disponible/sinmutación. RefsRAMgeneración/reto10smonotónico/connectionId-nonceexactos, replayretenidoRAM64retos sin evicción, firmarfuera monitor/revalidarantespublicar. EnvioAuthticketidentidad/copiabytes no esenvío; confirmar éxito ESEticket antesAUTH_OK/mismoID/vigencia/fase. Contexto-cierre-boot-revisión-reloj-fallossignan/callbackviejo-reentrancia-proceso nuevo no reabrenauth ni tocan generaciónnueva. PING/PONGnoautentican/norenuevanreto/permiso, PONGexactopendienteunavez. Tiempo se verifica poroperación/consulta, no nuevo scheduler. FaseAUTENTICADO_TRANSPORTE NOALLOW/APPLIED/padreoperativo/recuperación/liveness/canalTLSacreditado. AUTH_OKsinfirmasólo desde futurocanalTLS correcto: estaunidadNOcrea/verificaTLS/socket. AcciónUIconexiónDESHABILITADA hastacanalcompatible, sin authautomática boot/panel ni enforcer/red/permiso nuevo/SDK/servicio/push.

AUTHNode2frames del recurso público967ae05a…a964ec ahora evaluados por codec/criptoAUTHreales/PARENT-yCHILDcon clavesexternas; estadoAUTH_NO_EVALUADO_INTEROP01 queda HISTÓRICO. Vuelta públicaNUEVA8campos `pruebas/interop_auth_cp2_jvm_publica.json` SHAUTF8LFfinal **14ad8921072e628e700cd4404a3a4d0506c07ce434215c13f288efe32860c289**, oferta/PAIR/ambosAUTH autoverificados antespublicar/relecturaambasvariantes. P256efímeroJVMprivadassóloRAM/sinPEM/códigos/teléfonos; helperPADRE sóloTEST/CHILDemisiónproductiva. VectorPAIRprevioSHAfa04fc86…b36bf6 intacto/sin sobrescribir. **Aceptación Render de NUEVAvueltaAUTH PENDIENTE**, ningún verificadorNodeexterno ejecutado hijo.

Pruebasfinales **187core/445debug/390releaseJVM0fallos/errores**,5core+12appnuevosdistintos(9borde+3InteropAUTH, no duplicarvariantes). Reglasclosed/canonical/limits/DER/domainv1-PAIR-LFliteral/rol-públicaesperada-mutaciones/ticketcopia/10sfrontera9999-10000/bootidentidadrevisión/snapshot/cierre/replay/procesorecreado/firmaerror/callbackstale/reentrancia/latidosnoALLOW. CorefirmasrealesJCA efímeras/fuentes-IOrelojRAMfakes, NOAndroidKeystorereal. Fullcomando core:test/appJVMdebug-release/assembleDebug-AndroidTest-release/lintRelease **--rerun-tasks --console=plain SUCCESS3m29s131tareas131ejecutadas0UPTODATE**; lintMAIN/test/reporte0errores11avisosprevios/ignoreFGSúnicoigual/manifestintacto. APKtestcompilado/empaquetadoNOinstrumentado. Previosfull1m29s35exec96UPTODATE187/433/378/lintreporteUPTODATE; bordeFAILED8s porparéntesisextratestcorregido/sinJVMnuevoacreditado/reintento21s6exec46UPTODATE441/386corecached; exportUNAvez2métodosInteropAUTHconenvHIJO_AUTH01_EXPORTAR=SIretirado SUCCESS26s2exec25UPTODATE. Evidencia completa `pruebas/EVIDENCIA_HIJO_AUTH01.md`.

InspecciónAPKdirigidaMAINAUTHpresentes/tests-vectoresfuente-vueltaspublicas-fixtures-schemaAUTHausentes/rollbacksoloDEBUG/schemaPAIR-vectorPAIRhashintactos/ignoreFGSúnico; noAPKexhaustivo ni lintDebug/verificadorFake resueltos. APKbuildfinal **NOinstalado/NOfísico** debugSHA **60cf3399ec867308797696c0346fd6552d0d03bce3a35a59e97b231dbe39ee0f**, unsigned **a4d946adbbad1468231154620381fa07c94af57615ba947b132b307a6ebb77d4**. ÚltimoinstaladoACREDITADOPAIR01histórico0d363886…4344e9/baselineNONE-desarmados/identidadregistroCE-credencial conservados sin consultaactual. Rutascorecp2Auth/appauth-UIbotóndesHABILITADO/schemaAUTHsuministrado/tests/vueltaAUTHpública/READMEcp2/ESTADO/evidencia/relevo. Sin CP1/schemaPAIR/fixtures/planes/ADR/deps/manifest/enforcer/Keystorecódigo/DP/otrosrepos. NingúnADB/A13/A56/install/QRreal/cámara/ceremonia/alta/migración/Owner/PIN/boot/red/router/protecciones/DNSVPN/LOCAL07-LOCAL08/subagentes/sesiones/prompts/push/deploy. **Entregar y detener**, canalTLS/recuperación/órdenes-STATE/SYNC/enforcer fueraalcance, no otra tareaautomática ni permisosheredados. Render no dispositivos.

## HIJO-ORD01 — MESSAGE validado, guardia30s, MAIN NO_DISPONIBLE sin CONTROL durable

2026-10-15, base141ed73/bloque `71b21f7` (~438líneasKotlin-UI-pruebas sin schema suministrado)/cierre en commit que incorpora este relevo. **CODEC_Y_GUARDIA_PROBADOS_JVM / BORDE_MAIN_NO_DISPONIBLE_SIN_CONTROL_DURABLE**. Propietario reporta Node aceptó nueva vueltaAUTH14ad8921…60c289(ofertaPAIR2AUTH), padreORD01codec-firmanteSYNC-SETprobados/RenderO01schema-signatures153tests/NodeAUTH-onlysocketlocal: resultados REPORTADOS/no ejecución hijo/no canalAndroidreal/identidadhumana/ceremonia. Pushprevio3reposreportado, no nuevo push. RenderACKschema/verificador y padreACKcorrelación sólo contexto, no integrados aquí.

FuenteCP2-O01Render4481c92 íntegra, `contrato-cp2/ordenes.schema.json` SHAUTF8LFfinal **e5acd941ea0739ad875dcd493ea6685a2a3ec9d07a440f1c89a578fc064e8906** exacto/listasSYNC6-SET_POLICY12-sobre5. Corecp2OrdenesCp2 DTO/codec/criptoPARENTMESSAGE/públicaPADREESPERADAsnapshot-nodelpayload/dominioCPv2MESSAGELFreal/bytesoriginales-hash/copias/ASCIIcanónicoUTF8closed-duplicados/Base64nonce32/UUIDv4/P256DERr,s/depth6/8192-16384/C+seq-CexpectedRevisionstringhastaLongMAX. ALLOW_FOR60..86400múltiplo60/LOCK-ALLOWdurationnull, sinLOCK_FOR/deferredAllow/WAITING_ALLOW/fallbackCP1/alg-key-incluida.

GuardiaOrdenCp2 pide puertoESCRITOR consolidarboot-expiración-revocación antesemitir/validar/entregar; snapshotpareja-públicas-revisiónregistro/bootId-childSession/revisiónCONTROL-lastSeq exclusivos de writer, NOrevisióncredencial/default0/bootrecordCP1/autoridadRAMproductiva. GeneracionesRAM porreferencia y contadorSYNC diferentesdeCONTROL, nuevoSYNCsustituye inclusoigualrev/nullconsultaanula; nonceSecureRandom32/30sestrictos/issued-expiresinternos/max128nonceRAMsin evicción/noSTATEparcial. Capturatokens ycontextobeforeaftercrypto-final-publicación; cambio CONTROLobservado revoca reto, regresióncontador/reloj-boot-snapshot-cierre/proceso invalidan. Preparacióninmutable con hash/bytes NOAPPLIED/permiso; consumo ANTESpuertodecisión, fallos/reentranciacallbackviejo no recuperannonce. Seqretenidasóloconsulta bytes/hash a futurodurable, sin clasificar exactduplicadoRAM/reaplicar/retonuevo/seqreset/exactlyonce. Futurowriterrequiere CAS/autoridadcontextodurable propio, aún NOimplementado.

OrdenesAndroidcallable leepareja/identidadCE reales pero **ControlOrdenNoDisponible.consolidar=null** y métodos decisión/retenciónno disponibles: iniciar falla cerrado, UIórdenesdeshabilitada, sin socket ni fuenteCONTROLfalsaMAIN. No persistirorden/nonce/historial/permit, no UsoRed/enforcer/credencial/CP1/DP. NORMAL10sdesdeAndroidNEGATIVA/reconectarnoabre/OFFLINEsólolocalexplícito; WSS/PONG/AUTHnoAndroidniREMOTE_OFFLINE, bootprocesoinvalidanRAM. Sinreenforcement/protecciones/LOCAL07-LOCAL08 nuevas.

Final **193core/460debug/405release0fallos/errores**,6core+15appnuevosdistintos(no duplicarvariantes), firmasJCAreales/efímerasRAM/control-clock-escritorFAKEsóloJVM. CierresCAS-bootrevision/nonce-action/session/30sfrontera29999-30000/Syncnull/reentranciaentropía-consolidación-crypto-decision/oldcallback/errorpuerto/LongMAX/consultaRetenidanoefecto/NO_DISPONIBLE. Fullcomando core:test/appJVMdebug-release/assembleDebug-AndroidTest-release/lintRelease **--rerun-tasks --console=plain SUCCESS1m59s131tareas131ejecutadas0UPTODATE**; lintMAIN-test-reporte0errores11avisosprevios/ignoreFGSúnicoigual/mismonodo/manifestintacto. AndroidTestcompilado-empaquetadoNOinstrumentado. PrimerfullFAILED54s7exec81UPTODATE por smartcastMAINentre módulos corregido a vallocal/sinbuildcompletoacreditado; segundofull1m15s32exec99UPTODATE193/454/399/corecached/lintreporteUPTODATE. Evidencia completa `pruebas/EVIDENCIA_HIJO_ORD01.md`.

InspecciónAPKdirigidaMAINORD/ControlNoDisponiblepresentes/tests-vectores-fixtures-schemaORDausentes/rollbacksoloDEBUG/hashschemaORDexacto/ignoreFGSúnico; noAPKexhaustivo ni lintDebug/verificadorFake resueltos. APKbuildfinal **NOinstalado/NOfísico** debugSHA **a800a9698018bbdaabe59a38911cda1e55b11efa3abf2555bef144ba4a445a4b**, unsigned **6f62f2a7f296ea848b485390fc8dd7ee88e52af26fbe8e7bf1d5c68e07cf8218**. ÚltimoinstaladoACREDITADOPAIR01histórico0d363886…4344e9/identidadregistroCE-credencial/baselineNONE-desarmados conservados sinconsultaactual. Rutascorecp2Ordenes/apporders-UIbotóndesHABILITADO/schemaORDsuministrado/tests/READMEcp2/ESTADO/evidencia/relevo; sinCP1/schemaPAIR-AUTH/fixtures/planes/ADR/deps/manifest/UsoRed/enforcer/Keystore/DP/otrosrepos. NingúnADB/A13/A56/install/QRreal/altas/permisosfísicos/migración/Owner/PIN/red/router/protecciones/LOCAL07-LOCAL08/subagentes/prompts/push/deploy. **Entregar y detener**, escritorCONTROLdurable/CAS/retenciónderesultados/STATE/ACK/TLS/enforcement pendientes, no alternativaRAM ni otra tareaautomática/permisosheredados. Render no dispositivos.
