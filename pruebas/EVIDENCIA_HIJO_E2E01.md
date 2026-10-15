# HIJO-E2E01 — implementación vertical piloto, evidencia y límites

15 de octubre de 2026. Base `9ddce88`; primer bloque `d507df8`.
**CERRADA IMPLEMENTACIÓN VERTICAL PILOTO / COMPILADA Y PROBADA JVM / SIN FÍSICA.**
APK preparado en código para un ensayo posteriormente autorizado; no producto final,
pareja/canal reales, enforcement Android ni recuperación acreditados.

## Actualización de coordinación posterior al cierre de código — misma E2E01

Propietario comunica STATE piloto Render `d2fddc4`, SHA256UTF8/LFfinal
`bf6c04c3614010932233b2ca052d5a5eb2605361f9b35e764000975198027809`.
`Get-FileHash` local volvió a comprobar ese SHA exacto. No se leyó ni verificó
el commit/repo remoto. Schema local sin cambios; no hacen falta más fuentes.

**Resultados externos REPORTADOS, no ejecutados hijo**: routing local con169tests
correctos; CP1esquema28/13 y vectores10/78; dos sockets Node auténticos con claves
efímeras recorrieron SYNC→STATE→LOCK→ACKpending→ACKapplied conservando bytes.
El hijo de ese ensayo es **SIMULADO**: no prueba bloqueo/readback Android,
Keystore/CE reales, emergencia, pareja real ni canal público. Peer ausente usa
PEER_UNAVAILABLE/noACKrelay/no presencia espontánea. CLI normal mantiene perfil
previo y endpoint público aún pendiente, según el reporte.

La rama MAIN del hijo ya existe en `a02e9d6`: UI→PilotoE2eAndroid→WSS/AUTH→
ControlRedAndroid/guardia/writerCE→UsoRed/Bloqueo→STATE/ACK firmados. Inspección de
rutas confirmó llamadas productivas de UI/recepción/emisión y ausencia de
ControlOrdenNoDisponible en MAIN; no equivale a prueba nueva de ejecución.
No se añadió un modelo/puerto sustituto ni se modificó código por este reporte.

**Impedimentos concretos del siguiente ensayo real**: endpoint TLS público aún
pendiente y falta aprobación específica para teléfono/instalación/pareja real.
No interpretar el ensayo Node ni esta actualización como tales permisos; no
probar el perfil CLI previo ni contactar un endpoint supuesto. No despliegue,
push, dispositivos, otros repos ni nueva unidad. Sólo documentación/procedencia:
sin nuevo build/test; los resultados y hashesAPK del cierre de código se mantienen.

## Cierre con ACKRenderK01 — estado final de la misma E2E01

FuenteACKRenderK01 suministrada íntegra,7testsRender REPORTADOS/noejecutadoshijo.
Copia exacta `contrato-cp2/ack.schema.json`, UTF8/LFfinal/SHA256
`3aeae8504de36f5f05462a79f60972889c6d0eb45d356d031eec361412e512e4`.
No otros repos/HEADs remotos leídos. STATE y ORD copias exactas intactas.

- `core.../cp2/AckCp2.kt`: DTO/codec/criptoACKCHILD14campos/sobre5, dominioUTF8
  CPv2/MESSAGE+LFreal+bytescanónicos, P256SPKI/DERr,s/base64roundtrip/profundidad6/
  payload8192-frame16384/camposcerrados/duplicados/tipos/countersC-C+hastaLongMAX.
  acceptedRevisionpositivo<=evaluatedRevision; variantes exactas APPLIED/OK/
  accepted+elapsed, PENDING/OK/accepted+elapsednull, FAILED/ENFORCEMENT_ERROR/
  accepted+elapsed, REJECTEDsinaccepted-elapsed/motivo!=OK-UNKNOWN_COMMAND y
  UNKNOWN_COMMANDconmotivoexacto/sinaccepted-elapsed. No ACKrelay ni permiso.
- `orders/ProyeccionAckCp2.kt`: respuesta correlacionada sólo a comando verificado
  y estado consolidado; resultado retenido preserva boot/sesión/id/seq/hash/bytes/
  acceptedRevision/resultElapsed originales, evaluatedRevisionactual. No comparar
  relojes de otros boots. Duplicados no reaplican ni renuevan nonce/plazo/seq;
  misma secuencia/id no retenidoexacto=SEQUENCE_CONFLICT, secuencia anterior no
  retenida=UNKNOWN_COMMANDsinaccepted-elapsed, CAS/contexto/nonce rechazados sin
  aceptar. COUNTER_EXHAUSTED se rechaza antes de incrementar/IO. Sólo último
  resultado retenido, sin ledger/historia ilimitados ni exactlyonce general.
- MAIN `ControlRedAndroid` y `PilotoE2eAndroid`: ACKpendiente firmado/enfilado en
  MISMAgeneración antes de invocar sideeffect; ACKfinal desde resultado durable
  de ESAorden/ESArevisión tras callback/readback; snapshot antes/despuéscrypto/
  revisióncomparada; callbacks obsoletos no confirman nuevaorden. Enfilar no es
  entregar ni prueba bloqueo. Si no puede firmar/enfilar respuesta, falla cerrado
  y no amplía uso. Publica STATE además con última consulta SYNC real, sin fabricar
  syncNonce. UI ya indica STATE/ACK disponibles y pendiente no implica aplicación.
- Rechazo de tramas no autenticadas/malformadas o contextoCONTROLincierto puede
  ser silencioso: no fabricar hash/correlación/evaluatedRevision0 para contestar.
  Una pareja distinta nunca recibe firma como si fuera la actual. No fallback
  ACKCP1. PEER_UNAVAILABLE sóloinformativo/noACK/noefecto, no presencia necesaria.

**Recorrido MAIN completo callable** de la unidad: sesiónadulta/códigofresco y
marcooriginal120s → WSS/TLSnormal/AUTHCHILDconKeystoreexistente → SYNC/SET_POLICY
PARENT verificadas → guardia → writerCONTROLdurableCE/reserva-readback → actor
existente UsoRed/Bloqueo reversible → STATE/ACKCHILDfirmados en mismo canal.
No ControlOrdenNoDisponible ni writerRAM alternativo en MAIN. NORMALAndroid10s/
reconectarnoabre, EXCEPCION_OFFLINEsólolocalexplícita, boot/procesonoreanudanRAM.
Sólo acceso adulto foreground/sesiónoriginal; salir/fondo/caducarcierran canal,
no reintento ni grant autónomo. No prometer background/appmuerta/serviciocanal.
No nuevos recibos de pareja ni CP1activado/migración/credencial/claves nuevas.

ACKnuevas pruebas5core+3proyecciónapp, verticales existentes ampliadas para verificar
ACKreales CHILD/hash/commandId/accepted-evaluated/PENDINGantesefectoFAKE/APPLIEDsólo
callbackFAKE. Totales E2E01 nuevos **12core+30app distintos**, no duplicarvariantes.
Disco/reloj/socket/enforcement y glue del ensayo **FALSOS**, componentes Kotlin/
firmas PADRE-HIJO reales. NO UIAndroid/DeviceOwner/Keystorefísico/TLShandshake real.

Pasada ACKJVM completa SUCCESS41s/15ejecutadas37UPTODATE. **FINAL** comando completo
offline--rerun indicado abajo **SUCCESS1m49s/131tareas131ejecutadas/0UPTODATE**,
XML **205core/490debug/435release0fallos/errores**, lintReleaseMAIN-test-reporte
**0errores12avisos** (versionesprevias+OkHttpautorizado4.12.0). APKAndroidTest sólo
compilado/empaquetado, NOinstrumentado/instalado. Falla QRsintética histórica y
pasadas anteriores preservadas abajo; no esconderlas por este final exitoso.

InspecciónAPKdirigida final python-B/ZipFile: MAINauth/control/PilotoE2eAndroid/
STATE-ACKcodecs-cripto-proyecciónpresentes, ControlOrdenNoDisponible/tests/vectores
fuente-vueltas/schemasausentes, rollbacksoloDEBUG, ignoreFGSúnico/mismonodo;
hashesACK-STATE-ORD exactos. No auditoríaexhaustiva ni lintDebug/verificadorFake
históricos resueltos. Revisión de cambios y gitdiffcheck antes de commit.

**APKs finales E2E01 compilados, NOINSTALADOS**:
- Debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk`, SHA256
  `fc7a5bd82e6bfdcb0ae0434ce2d88be03eac2cc1390d73a339c9eb52c83108ed`.
- Releaseunsigned `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`,
  SHA256 `17c0808bdca086ddfb0dea0a4e57cda7a5dbe24b92d14944cabe1f4daa94102f`.
- Últimoinstalado acreditado sigue PAIR01histórico, sin consultaactual. SinADB/
  A13/A56/install/QRreal/vínculo/altas/Keystorefísico/Owner/PIN/CA/redapp-router/
  protecciones/LOCAL07-LOCAL08/push/deploy/otrosrepos/subagentes/prompts.
- Bloques d507df8/897cfe7/647d138/61eccca y cierre en commit que incorpora esta
  evidencia. Rutas de cierre: schemaACKsuministrado/coreACK+test/appProyeccionACK+
  test/ControlRed-PilotoE2e-UI/verticalesampliados/READMEcp2/ESTADO/evidencia/relevo.
  CP1/fixtures/schemaSTATE-ORD-AUTH-PAIR/deps/manifest de esta última parte intactos;
  dependenciaOkHttp+INTERNET ya autorizados e incorporados en bloquesanteriores.
- **Entregar y detener.** Ensayo endpointTLS/pareja/Keystore/CE/readback/enforcement/
  emergencia/red-timer-boot físicos requiere aprobación concreta posterior. No
  generar QR/vínculo/instalar ni heredar permisos por este cierre. Sin otra tarea.

Lo siguiente preserva los avances anteriores: ACKpendiente allí YA RESUELTO;
hashes parciales previos no son los APKs finales de este cierre.

## Resultado actual — STATE vertical conectado, ACK exacto pendiente

Bloques `d507df8`, `897cfe7`, `647d138` y commit que incorpora este avance.
**Existe recorrido MAIN callable real**: código adulto fresco/sesión original120s
→ WSS/TLSnormal/AUTHCHILD → SYNC/SET_POLICY PARENT verificados con clave esperada
→ guardia/controlador durable CE → actor UsoRed/Bloqueo reversible → STATE firmado
por la identidad CHILD existente y en la misma generación. No sustitución por
ControlOrdenNoDisponible ni fábrica de autoridad RAM. Fuente STATE ya completa,
sin esperar otro schema STATE. **Lo que falta es la definición ACK§7/9 exacta**:
no aparece en la fuente STATE/PEER_UNAVAILABLE suministrada; no emitir un ACK
inventado ni reutilizar CP1. UI declara ACK no emitido. E2E01 NO se marca cerrada.

Pruebas nuevas totales contra ORD01: **7core+27app distintos** (writer11,
UsoIntegrado3, canal6, proyección3, sesión1, verticalSTATE3). Las verticales usan
claves PADRE/HIJO efímeras diferentes, firmas reales, ClienteAuthHijo/ClienteCanalCp2/
GuardiaOrdenCp2/EscritorControlCp2/UsoRedLocal/ProyeccionStateCp2/codecs reales y
glue del ensayo con disco/reloj/socket/enforcement **FALSOS**. Recorren AUTH-SYNC-
LOCK/ALLOW/ALLOW_FOR, pending firmado antes del efecto falso, resultado APPLIED
sólo callback falso correlacionado, replay retenido sin reaplicar/renovar, pérdida
de red10s/STATE NETWORK_LOST sin reapertura al reconectar, IO ambiguo sin permiso.
Esto NO ejecuta Activity/DeviceOwner/KeystoreAndroid/TLS/relay real ni acredita
observación física. Revisar esos bordes con autorización posterior específica.

Endurecimiento de código limitado al canal y coherencia del piloto: cabecera de
subprotocolo debe ser única y exactamentecp.v2; timerfixedDelay no acumula ráfagas;
expiración de marco original120s comprobada en primer plano aunque otro ticket
local cambie el límite de sesión; no reintento/autogrant. Si no se puede firmar y
enfilar STATEpendiente en la MISMA generación, writer confirma fallo/bloquea en vez
de ampliar uso. **Enfilar no equivale a entregar**, ACK relay ni enforcement.

Último full con comando offline--rerun de abajo: **SUCCESS2m40s/131tareas131ejecutadas/
0UPTODATE**, XML **200core/487debug/432release0fallos/errores**; lintRelease
**0errores12avisos**. AndroidTest compilado/empaquetado, NOinstrumentado.
Intento inmediatamente anterior **FAILED2m31s/106tareas106ejecutadas** por
`QrOfertaCp2Test.matrizConMargenEscaneableConservaBytesOriginales` preexistente con
datos aleatorios (`NotFoundException`). **No se alteró la prueba**: repetir el
conjunto pasó. Queda anotada la inestabilidad sintética, no resuelta ni cámara
ensayada. Primer pase vertical **FAILED21s9exec41UPTODATE** por recursión accidental
en el puerto falso de consolidación, corregido a `this@Lab.consolidar`;
posterior **SUCCESS21s4exec48UPTODATE** (corecached/appambasejecutadas), y después
el fullforzado ya indicado. No sumar pruebas del pase fallido como aprobadas.

Inspección dirigida python-B/ZipFile: clasesMAINwriter/canal/PilotoE2eAndroid/
CodecStateCp2/CriptografiaStateCp2 presentes; ControlOrdenNoDisponible/tests nuevos/
vectoresfuente-vueltas/schemasausentes; rollbacksoloDEBUG/ignoreFGSúnicoigual;
hashesORD ySTATEexactos. No auditoríaAPKexhaustiva ni verificadoreshistóricos resueltos.

APKs compilados actuales **NOINSTALADOS y NO listos como unidad E2E completa**:
- Debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk`, SHA256
  `e3807734deae91d5e349aecb8ca8b7b03bbed12e1eea6960a439a17dfa88bc7a`.
- Releaseunsigned `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`,
  SHA256 `9c1ee3385fa7e9314dbab2e103650573b9c2a8c45472901132c812a257249f4b`.
- Sin ADB/A13/A56/install/QRreales/altas/pareja/credencial/Owner/PIN/protecciones/
  redapp-router/CA/push/deploy/otrosrepos/subagentes/prompts. Últimoinstalado sigue
  PAIR01histórico sin consultar. Historia/avisos NO DISPONIBLE/null, no cero.
- Siguiente trabajo **MISMA E2E01**, no nueva tarea: ACK exacto y correlación/tests/
  fullfinal/entrega cuando exista fuente. No suponer campos/reasons/resultados.

El resto del archivo conserva el historial de avances y hashes anteriores.

## Avance STATE01 posterior — misma E2E01, fuente recibida

FuenteSTATE01JSON exacta y enumreason recibidos después de la consulta al usuario,
copiados `contrato-cp2/state-piloto.schema.json`, SHA256UTF8LFfinal
`bf6c04c3614010932233b2ca052d5a5eb2605361f9b35e764000975198027809`.
Render5tests/routingbytespreservados REPORTADOS, no ejecutados hijo. No esperar
otrafuenteSTATE. El schema no define ACK§7/9: ese emisor aún sin fuenteexacta.

CoreStateCp2 completo: listas20/11/5/9/5/enums exactos/C-C+LongMAX/retoigualrevisión/
issued<=sampled<expires=issued+30000sinoverflow/ALLOW_FOR60000..86400000múltiplo60000/
remainingceil/fase-modo-efecto/remotohistóricorelojindependiente/nullconseq>0noreset/
historianull(no0)/CHILDdomMESSAGE-LFreal/P256DER/claveesperadaexterna. AuthAndroid
comparte fuente/firmanteKeystoreexistente sincrear/importar/exportar.

ProyeccionStateCp2 no es autoridad: modos sóloVERIFIEDconcorrrelacióncallbackdeESA
revisión y observaciónactualcompatible; PINNEDnoLOCKED/Keyguardnofake. Revocación
NORMALred10s/timer escribe decisiónLOCKLOCALnueva antes de reentrada, con motivo
NETWORK_LOST/TIMER_EXPIRED; remotoanteriorretienehistoria. Rootreadback enlaza
ticketderevisiónexacto, callbackviejo no confirma otraorden. channelHealthy=null
sin mediciónliveness/WSS-AUTH-PONGnoAndroid.

PilotoE2eAndroid **MAINcallable**, no puerto vacío: UIcódigofrescoCONECTAR_CP2
marcooriginal120s→ControlRedwriterreal→WSS-AUTH→PARENTMESSAGEguardia→UsoRed-Bloqueo→
STATEfirmadoMISMAgeneración/últimoSyncNonceválido. Cambios writer publican STATE
pendiente/final. Sin nonce inicial fabricado. Fondo/salir/caducar/desconectarcierran,
no reintento/reanudación de permiso; sin promesabackground. **ACKNOemitido/UIloindica**,
no ACKCP1. PEER_UNAVAILABLE4camposv2/kind/reasonPEER_OFFLINE/refCommandId(UUID|null)
sóloinformativo, no ACK/fracaso/aplicación; no PEER_STATUS necesario paraAUTH/SYNC.

Nuevas StateCp2Test7/ProyeccionStateCp2Test3 (firmasreales/campos/contadores/retos/
duraciones/remaining/dominioclave/mutaciones/remotohistórico/efecto), JVMoffline
SUCCESS48s15exec37UPTODATE y1m13s15exec37UPTODATE, **200core/482debug/427release0fallos**.
Fullbuild/lintoffline SUCCESS1m31s19exec112UPTODATE/JVMcached,0errores13avisos
(SetTextI18n nuevo corregido arecurso después/pendiente nueva pasada). Bloques
previos d507df8/897cfe7; seguir mismaunidadpruebasverticales/revisión/ACKexacto/final.
Secciones siguientes conservan evidencia del avance anterior, no estadoactual.

Antes del commit STATE/UI: fulloffline SUCCESS1m7s39exec92UPTODATE/corecached/
appJVMambasejecutadas200core-482debug-427release0fallos/lint0errores12avisos;
recurso corrige SetTextI18n sin suppress. Buildsdebug-test-release compilados,
sin instalación/instrumentación. Continúa mismaunidad, ACKexacto/pruebasverticales.

## Borde productivo compilado

- `orders/EscritorControlCp2.kt` y `ArchivosControlAndroid.kt`: writer CONTROL real CE/noBackup/AtomicFile, marca exclusiva/reserva por transacción/readback. Revisión propia, intención/seq/commandId/contexto/hash/payload del último remoto durables antes de efecto; pendiente no es APPLIED. Reapertura conserva secuencia/resultado, cambia sesión y elimina decisión recuperable: sin restaurar permiso RAM, incluso mismo boot. Corrupción/interrupción/IO ambiguo/reentrancia cierran sin reset ni virginidad fabricada. Callback sólo confirma revisión vigente. Duplicados exigen contexto/id/seq/hash/bytespayload idénticos, distinta firma válida de los mismos bytes no reaplica. Sólo último resultado, no ledger ilimitado.
- `network/ControlRedAndroid.kt`: puente callable en el **actor local existente**, no writer paralelo. Con pareja validada las acciones adultas locales inicializan/cargan el escritor real; acciones locales/remotas pasan por él. Consolidación red/plazos antes de guardia/CAS/consulta; revocación durable antes de pedir reentrada. Reutiliza UsoRedLocal/ObservadorVisible/BloqueoLocalAndroid reversibles. Confirmación compara revisión y callback de transición/readback, no firma ni Keyguard. Token de canal antes de aceptar/ampliar. **Todavía sin conexión UI-transporte-writer.**
- `UsoRedLocal.kt`/`ObservadorVisible.kt`: límite absoluto adicional no estira ALLOW_FOR por latencia/cola. NORMAL mantiene INTERNET+VALIDATED/gracia10s desde negativaAndroid y reconexiónnoabre; OFFLINE sólo LOCAL explícita. Boot/proceso no restauran uso.
- `GuardiaOrdenCp2.kt`: consultar duplicado retenido admite contexto histórico boot/sesión, comparado exactamente por writer sin reaplicar ni comparar relojes entre boots. Nueva orden exige contexto/reto actuales. `OrdenesAndroid.kt` mantiene fuente real validada; retirados factory/objeto ControlOrdenNoDisponible de MAIN, fake sólo tests.
- `channel/ClienteCanalCp2.kt`/`CanalWssAndroid.kt`: WSS callable, AuthAndroid/Keystoreexistente/snapshotreales; origenHTTPSexacto→WSS`/ws`, cp.v2 único, TLS/hostname normal/plataforma, sin CA/trustAll/queries/reintento. Rechazo destinos literales LAN/loopback y DNSno público. Límiteframe16384/cola64, sin logsbody/excepciones; callbacks antiguos no entregan/cierran generación nueva. AUTHCHILD completo: reto/firma/envíoaceptado/OK; PING-PONG no otorgan uso. Vida explícita objeto/close/relojfixedDelay, **sin lifecycle UI aún**; no prometer background/procesomuerto. Única dependencia nueva autorizada OkHttp4.12.0 en catálogo, INTERNET y cleartext=false.
- `core.../cp2/SobreMessageCp2.kt`: helper de estructura cerrada/canónica/profundidad6/ASCII-UTF8/Base64/DER del sobre MESSAGE; NO verificación firma/rol/clave ni codec STATE/ACK.

## Pendiente normativo y funcional de la MISMA unidad

Fuente ACKschema§7/9 prometida para siguiente mensaje **no recibida**, y falta enum exacto `effective.reason` §9. STATE01-PILOTO sí suministró los otros campos/listas y delta `permittedTime=null`/`lastExpiry=null` = **NO DISPONIBLE, no cero**. No inventar campos/motivos/historia/epoch/zona. Solicitadas ambas fuentes; usuario respondió «no entiendo qué es lo que necesitas» y se explicó que basta copiar el mensaje pendiente de Render, no credenciales/datos del teléfono.

Completar codec/emisor STATE/ACK firmados por misma identidad CHILD, correlación snapshot/revisión/orden, UI adulta explícita y lifecycle foreground con canal/guardia/writer, pruebas verticales de emisión/callbacks, revisión concurrencia/errores y builds/lint/inspección final nuevos. No cerrar esta unidad por componentes compilados ni iniciar otra tarea. Sin las fuentes no afirmar interoperabilidad de respuestas ni habilitar botones como si hubiera recorrido completo.

## Pruebas ejecutadas

Nuevos **19 casos app distintos**: EscritorControlCp2Test11 (firmasP256reales/IO-reserva/corrupción/boot-proceso/CAS/local-remoto/callbackantiguo/reentrancia/duplicados-firmanueva/falloenforcement), ControlUsoIntegradoCp2Test3 (writer+UsoRedreales con archivos/relojFAKE: NORMAL10s/reconexiónnoabre/OFFLINElocal/plazoabsoluto), ClienteCanalCp2Test5 (socketFAKE/Authfirmareal/subprotocolo/destino/timeout/generación). NOhandshakeTLSreal/servidor/KeystoreAndroidreal/DeviceOwnerinstrumentado.

Desde `android/`:

```text
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --offline --rerun-tasks --console=plain
```

Último **SUCCESS4m11s/131tareas131ejecutadas/0UPTODATE**. XML **193core/479debug/424release,0fallos/errores**. Mismos19app en ambasvariantes, no contarlos38. lintRelease **0errores12avisos** (11previos de versiones y actualizaciónOkHttp, no cambiar versión autorizada). AndroidTest sólo compilado/empaquetado, NOinstrumentado/instalado.

Historial: writerJVM SUCCESS28s10exec40UPTODATE; integración FAILED35s14exec71UPTODATE por preflightinternal entre módulos, corregido con helperestructural público; full FAILED1m39s43exec88UPTODATE por readNBytesAPI33/min31, corregido con lecturabufferacotada; full SUCCESS1m55s34exec97UPTODATE; forzado SUCCESS2m131exec0UPTODATE/14avisos (fixedRate/catalogocorregidosdespués). Últimointentoforzado online TIMEOUT herramienta240s sin resultado completo acreditado; se repitió todo offline y ése es el pasefinal usado. No inferir éxito deltimeout/UPTODATE.

Inspección dirigida python-B/ZipFile: clasesMAINwriter/almacén/canal presentes, ControlOrdenNoDisponible/tests nuevos/vectores fuente-vueltas previas/schemasausentes, rollback sóloDEBUG, hashORD exacto/ignoreFGSúnico/mismonodo. No auditoríaexhaustiva ni resolución de verificadores históricos. `git diff --check`/revisión de rutas antes de commits.

## Artefactos PARCIALES, NO instalados ni listos E2E

- Debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk`, SHA256 `ae8b7661a2ed52ff796ff919b6e29312de33efcb743d62144008cfa602b822e1`.
- Releaseunsigned `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`, SHA256 `87bae7db9fa754ce3b11639f96c09433f83fce32572abfb9b2c3c199d7414c39`.
- Últimoinstalado acreditado sigue PAIR01histórico, sin consultaactual. NingúnADB/A13/A56/install/QRreal/pareja/credencial/Owner/PIN/radios/router/redreal/protecciones/CA/push/deploy/otrosrepos/subagentes/prompts. CP1/fixturescongelados/schemaORD-AUTH-PAIR intactos. Permisos físicos no heredados. Continuar sólo E2E01 con fuentes pendientes.
