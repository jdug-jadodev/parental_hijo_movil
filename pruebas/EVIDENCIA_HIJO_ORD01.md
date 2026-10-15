# HIJO-ORD01 — validación MESSAGE y retos CP/2, sin escritor durable

15 de octubre de 2026. Base `141ed73`; bloque1 `71b21f7` (~438líneas propias Kotlin/UI/pruebas, schema suministrado excluido); cierre en commit que incorpora esta evidencia. **CODEC_Y_GUARDIA_PROBADOS_JVM / BORDE_MAIN_NO_DISPONIBLE_SIN_CONTROL_DURABLE**. No sockets, enforcement, persistencia de órdenes ni teléfonos.

## Fuente y coordinación

- CP2-O01 Render `4481c92`, `contrato-cp2/ordenes.schema.json` íntegro transmitido y copiado separado, UTF8/LFfinal. SHA256 **`e5acd941ea0739ad875dcd493ea6685a2a3ec9d07a440f1c89a578fc064e8906`** intacto. Listas exactas **SYNC6 / SET_POLICY12 / sobre5**, no campoextra `protocol` en estos payloads. CP1/schemaPAIR/schemaAUTH/fixtures congelados intactos; sin leer otro repositorio ni HEAD remoto.
- Propietario reporta nueva vueltaAUTHhijo14ad8921…60c289 aceptada por Node (oferta/PAIR/ambosAUTH); padreORD01 codec/firmanteSYNC-SET probados; RenderO01schema/signatures153tests y socketNodeAUTH-onlylocal. **Reportado, NO reejecutado hijo** y no acredita canal/Androidreal/identidadhumana/ceremonia. El pendiente de aceptación externa AUTH01 queda actualizado por este reporte, no por ejecución aquí.
- Push previo de los tres repos reportado; **ningún push nuevo** desde esta sesión ni acceso a otros repos. RenderACKschema/verificador y padreACK/correlación en paralelo sólo contexto; no implementar ACK/STATE/enforcement aquí.

## Core productivo separado

`android/core-protocol/src/main/kotlin/dev/controlparental/protocol/cp2/OrdenesCp2.kt`:

- DTO SyncOrdenCp2/PoliticaOrdenCp2/SobreOrdenCp2, enum cerrado LOCK/ALLOW/ALLOW_FOR, sin LOCK_FOR/deferredAllow/WAITING_ALLOW/REMOTE_OFFLINE. PARENT obligatorio, pública padre **esperada externa del snapshot**, nunca incluida en payload. Ningún firmantepadreMAIN/alg dinámico/fallbackCP1.
- Parser de SYNC6/SET_POLICY12/SIGNEDMESSAGE5 cerrados/required/duplicados/tipos/v2/purposeMESSAGE. ASCIIcanónico/UTF8 exacto, reserialización técnica para comparar canonicidad, preflight profundidad6 antesparse y límites payload8192/frame16384. Primitivas JSON/Base64/SPKI/DER/preflight reutilizadas sin cambiar CP1/AUTH/PAIR.
- `seq` string decimal C+ y `expectedRevision` C0..Long.MAX_VALUE, no signo/padding/espacios/exponente/decimal/coerciónnumérica/overflow. UUIDv4lower/bits/nonce32/base64urlroundtrip. ALLOW_FOR duraciónentera60..86400 múltiplo60; LOCK/ALLOW sólo null.
- P256SPKIcanónica, DER8..72/r,s1..n−1, SHA256ECDSA sobre **UTF8(CPv2/MESSAGE + LF real) + payload original**. Resultado inmutable con DTO/hashSHA256/copiabytespayload-frame; getters copian, nunca reconstituyen bytes para firmaverificada. Firma/integridad no es autoridad de control/aplicación.

## Guardia y puerto de autoridad NO_DISPONIBLE

`android/app-child/src/main/java/dev/controlparental/child/orders/GuardiaOrdenCp2.kt`:

- `EscritorControlOrdenCp2.consolidar` debe consolidar boot, expiraciones y revocaciones antes de devolver contextoCONTROL; null/incierto/fallo cierra disponibilidad. ControlOrdenCp2 recibe del escritor boot/bootId/childSessionId/revisionControl/lastAcceptedSeq, **sin defaults/revisióncero fabricada**, sin leer revisiónadulta ni bootrecordCP1 como CONTROL. Observación previa sólo comprueba monotonía de revisión/seq, no los incrementa ni crea política.
- `ParejaOrdenCp2` liga pairId/públicasPADRE-HIJO/revisiónhashregistro/boot de snapshot real. Consolidación lee pareja antes/después y exige boot consistente e IDs canónicos. GeneracionOrden referenciaRAM por identidad con número monotónico; cada SYNC tiene etiqueta creciente distinta de revisiónCONTROL. Cambio snapshot/identidad/bootId/childSession/relojretrogreso o contadoresCONTROL regresivos cierra generación. Cambio CONTROL observado invalida reto/preparación sin resucitarlos si vuelve un dato viejo.
- Antes y después de cripto **y** antes de emisión/entrega consulta consolidación. Tokens capturados viajan hasta la publicación final, no sólo durante verificación: reentrancia/callbackobsoleto no puede retirar reto nuevo ni publicar con revisión vieja. Verificador productivo usa clavePADRE del snapshot; tests inyectan wrapper con verificaciónreal y efectos simulados después.
- SYNC_REQUEST válido con requestedActionnull consulta/anula reto anterior sin emitir uno; acción concreta sustituye siempre, incluso misma revisión. SecureRandom32, máximo128noncesretenidosRAM sin evicción (fallo/repetición/capacidadagotada rechaza), vida estricta30.000ms desde issuedElapsedMs del hijo; expires por addExact/guardia relojmonotónico. Entropía reentrante/contextocambiante no publica reto viejo. No serialización STATE: objeto interno RetoOrdenCp2 sólo nonce/acción/revisión/issued/expires/generación/contexto para futura integración.
- SET_POLICY compara pareja, rol, bootId, childSessionId, expectedRevisionCONTROL, nonce, acción, seq>lastAcceptedSeq persistido y plazo **n<expires**, con snapshot/control estable antes/después. PreparacionOrdenCp2 inmutable queda ligada a ESE reto/guardia y hash/bytes originales. Preparar no entrega al writer ni autoriza APPLIED/ALLOW.
- `entregar` vuelve a consolidar y comprobar referencia/preparación/contexto/CAS/plazo, consume reto/preparación **ANTES** de llamar al puerto de decisión. Errores/cancelar/reentrancia/callbackviejo no restauran consumo RAM ni regeneran nonce. Retorno ENTREGADA_AL_PUERTO_NO_APPLIED sólo dice que se invocó un puerto, no persistencia/autoridad/readback/aplicación. El escritor futuro deberá revalidar CAS/boot/snapshot/generación y autoridad durable en su punto de decisión; no conservar preparación como permiso tras cierre. No se acredita atomicidad de una transacción que todavía no existe.
- Seq<=lastAcceptedSeq no produce preparación: delega **sólo consulta** de bytes/hash ya verificados a `consultarRetenida`, con contexto firmado/boot/sesión vigentes. No determina duplicadoexacto/conflicto/no-retenido en RAM, no reaplica, no nuevo reto/seqreset. Futura transacción durable debe comprobar IDs/seq/hash/bytes retenidos y resultado. No exactly-once durable ni historial/persistencia en esta guardia.

`OrdenesAndroid.kt` conecta lectura real `PairAndroid.cargar`/identidadCE/revisiónhashregistro pero usa explícitamente **ControlOrdenNoDisponible**, cuyo consolidar devuelve null: **borde MAIN callable falla cerrado incluso si hay pareja válida**. No otro modelo de autoridad productivo ni fuenteCONTROLfake en MAIN; fakes sólo en JVM. Botón UI órdenes deshabilitado, hasta writer/canalcompatible. No boot/panel/socket/recepciónautomáticos.

No uso/enforcement/credencial/DP/almacénlegacy modificado. NORMAL sigue revocación10s desde AndroidNEGATIVA y reconexión no abre; EXCEPCION_OFFLINE sólo local explícita; WSS/PONG/AUTH no sustituyen Android ni autorizan REMOTE_OFFLINE. Boot/proceso invalidan permisosRAM. No reenforcement/protección nueva, LOCAL07cancelada/LOCAL08closedaccept intactos.

## Pruebas ejecutadas

- Core`OrdenesCp2Test` **6casosdistintos**: firmasrealesP256/bytes/hash/copias; listas6-12-5/todosrequired/extras/alg-key-protocol; C+/C hastaLongMAX/overflow/coerciones; límitesdurationnull/60/86400/múltiplos/tipos; canonical/ASCII-UTF8/duplicados/UUID/base64nonce32/DERlongitud-JOSE-entero0/límites8192-16384/depth7rechazada; clave/dominiov1-AUTH-PAIR-LFliteral-CRLF/payloadmutado rechazados. DERrangoestricto adicional sigue cubierto por regresión AUTH/PAIR primitivas técnicas. No nueva dependencia ni fixture privado.
- App`GuardiaOrdenCp2Test` **15casosdistintos**: consolidación previa/generación/30s/29999vs30000/rollback/cancelación; preparaciónsinaplicación/consumoantespuerto/reentrancia-error/sinreplay; SYNCnuevoigualrevisión/nullanula; nonceacciónCONTROLrev/boot-session-pair-seq; revocación-expiración-cambio snapshot/control antes/despuéscrypto/entrega; callbacks viejos misma/nuevageneración/puertoreentrante/proceso; límitesLongMAX sincontadorinventado; consulta retenida sin reaplicar ni cambiarreto; entropíareentrante/repetida; escritorNO_DISPONIBLE no crea autoridadMAIN. FirmasJCA reales/clavesefímeras sóloRAM, fuenteCONTROL/clock/escritor **FAKES** sin políticas reales ni transacción durable.

Desde `android/`, conjunto completo:

```text
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

1. **FAILED54s/7ejecutadas81UPTODATE** por smartcast requestedAction de DTO entre módulos en MAIN; corregido mediante val local. No builds completos acreditados en ese pase. No inflar con pruebascore que hubiesen finalizado antes del error.
2. Mismo comando **SUCCESS1m15s/32ejecutadas99UPTODATE**: XML193core/454debug/399release0fallos; core:testcached en esta pasada/no nueva ejecucióncore, appJVM y builds ejecutados;9casosguardia iniciales. LintanálisisMAIN-test ejecutado/reporteUPTODATE0errores11avisos; AndroidTestcompilado/empaquetado, no ejecución. Commitbloque `71b21f7`.
3. **FINAL** comando completo añadiendo `--rerun-tasks --console=plain`: **SUCCESS1m59s/131tareas131ejecutadas/0UPTODATE**, XML **193core/460debug/405release,0fallos/errores**. Nuevos **6core+15appdistintos**, mismos15app en ambasvariantes, no duplicarlos como30. LintReleaseMAIN/test/reporte ejecutados **0errores11avisos previos de versiones**, ignoreFGSúnico/mismo nodo/manifestintactos. APKAndroidTest sólo compilado/empaquetado, **NOinstrumentado/NOinstalado**. No lintDebug/verificadorFake histórico resueltos por esta inspección.

Inspección dirigida `python -B -`/ZipFile/json/hashlib/Get-FileHash: CodecOrdenCp2/CriptografiaOrdenCp2/GuardiaOrdenCp2/OrdenesAndroid/ControlOrdenNoDisponible presentes en APKdebug/release; OrdenesCp2Test/GuardiaOrdenCp2Test/InteropAuthCp2Test/InteropCp2Test ausentes; fuente/vueltas públicas previas y sus campos/etiqueta/stringsQR completos ausentes, fixturesJSONcongelados y schemaORD no empaquetados; RollbackRedDebug sóloDEBUG; schemaORD hash exacto/ignoreFGSúnico. No inspección exhaustivaAPK ni seguridadcertificada. Ningún fixture nuevo de órdenes publicado: no solicitado, sólo firmas/nonce/claves de pruebas en RAM. `git diff --check` y revisión de rutas antes de commits.

## Artefactos y entrega

- APK **buildfinal NOinstalado/NOfísico** debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk`, SHA256 **`a800a9698018bbdaabe59a38911cda1e55b11efa3abf2555bef144ba4a445a4b`**.
- Releaseunsigned `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`, SHA256 **`6f62f2a7f296ea848b485390fc8dd7ee88e52af26fbe8e7bf1d5c68e07cf8218`**, sin física.
- ÚltimoinstaladoACREDITADO sigue históricoPAIR01 `0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9`, **sin consulta actual**. IdentidadprivateSigning/registroCE/credencial/baselineNONE-pilotosdesarmados/snapshotslimpios preservados sin leerlos ni modificarlos en esta sesión.
- Rutas nuevas: schemaORDsuministrado, corecp2/OrdenesCp2+test, apporders/GuardiaOrdenCp2/OrdenesAndroid+GuardiaOrdenCp2Test; UI sólo botónórdenesdeshabilitado, READMEcp2/ESTADO/estaevidencia/relevo. No CP1/schemaPAIR-AUTH/fixtures/planes/ADR/deps/manifest/Keystore/credencial/UsoRed/enforcer/DP/otrosrepos.
- **Unidad entregada y detener**. Falta escritorCONTROLdurable/CAS/retenciónresultados/enforcement/canalTLS/STATE/ACK: no resolverlos con RAM productiva ni usar firmas como autorización. Sin ADB/A13/A56/install/QRreal/altas/migración/Owner/PIN/radios/red/router/protecciones/LOCAL07-LOCAL08/subagentes/prompts/push/deploy/otra tarea. No permisos físicos heredados. Cualquier continuación requiere asignación nueva explícita; no producto listo/ceremonia/recuperación/canal/permiso acreditados por JVM.
