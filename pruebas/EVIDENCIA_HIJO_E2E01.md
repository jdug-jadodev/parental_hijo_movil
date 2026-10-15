# HIJO-E2E01 — avance, NO cierre

15 de octubre de 2026. Base `9ddce88`; primer bloque `d507df8`.
**EN CURSO. No hay recorrido funcional E2E entregable ni APK listo para ensayo.**

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
