# HIJO-AUTH01 — AUTH CP/2 CHILD, sin socket ni física

15 de octubre de 2026. Base `275f072`; bloque1 `f3a2900` (~440líneas propias Kotlin/UI/pruebas, schema suministrado excluido del conteo); cierre en commit que incorpora esta evidencia. **AUTH_CHILD_COMPILADA_Y_PROBADA_JVM_SIN_CANAL_NI_ANDROID_REAL**. Ningún ADB/instalación/dispositivo/red/despliegue/push/subagente.

## Fuente y coordinación

- Schema CP2-A01 `contrato-cp2/autenticacion.schema.json` recibido íntegro del propietario como Render`d99ddac`, copiado separado sin CP1. LF/saltofinal SHA256local **`db96b66ec81a379836751491244bb498a3b45ad75428593d9ea2c8755e0dd17a`**. No lectura/verificación de HEAD remoto. Requisitos de la asignación y draft`75eb56e`§4 como referencia declarada; no nueva interpretación de esquema/campos. PAIRschemaSHA7290e6cb…22239 intacto.
- Propietario reporta Render aceptó vueltaPAIRhijo `fa04fc86…b36bf6` y padreoferta/PAIR/2AUTH `fb830c79…65d4b`, cruce local aprobado, no ceremonia/identidadhumana. No reejecutado aquí ni acceso a repositorios ajenos. Actualiza el pendiente de INTEROP01 por **resultado externo reportado**, no ejecución hijo.
- FuenteNode pública íntegra de INTEROP01 en recursoJVM, SHA256 `967ae05aecd28c6b7f90b8ae90e8421b408dd70acbf22fd30911085c69a964ec`. AUTH_NO_EVALUADO_HIJO era alcance HISTÓRICO de INTEROP01; **esta unidad sí evalúa ambosAUTH** mediante codec/criptoAUTH reales. No lee otro fixture externo ni modifica fuente/PAIR previo.
- Rendercodecórdenes y padreSYNC/SET_POLICY trabajan en paralelo según propietario: sólo reportado/contexto, sin integrar esos mensajes, recibos o STATE aquí. No CP2completo/migración/recuperación/canal autorizado.

## Código productivo separado

`android/core-protocol/src/main/kotlin/dev/controlparental/protocol/cp2/AuthCp2.kt`:

- DTOs CHALLENGE5(connectionIdUUIDv4lower/nonce32/timeoutSecconst10), AUTH_PROOF7(v2/kind/pairId/role/connectionId/nonce/protocolcp.v2), AUTH_OK4, PING/PONG3, SIGNED/AUTH5. Enum de roles/latidos cerrado, ningún alg/clave en mensaje. Sin fallbackv1 ni paso de DTO al enforcer.
- Preflight técnicoASCII/UTF8 imprimible/depth6 antes de parser; límites8192payload/16384frame, claves duplicadas/desconocidas/tipos/versiones/constantes/escapes no canónicos rechazados, Base64urlroundtrip/sinpadding, UUIDv4bits/lower, nonce32. Reutiliza sólo primitivas técnicas JSON/Base64/SPKI/DER/preflight existentes, no CodecCp1/CriptografiaCp1.
- SHA256withECDSA/P256/DERestricto8..72/r,s1..n−1, bytesoriginalespayload con dominio UTF8**CPv2/AUTH + LFreal**, no literalbackslash-n/CRLF/PAIR/v1. PúblicaESPERADA y rolESPERADO externos, nunca desde frame. Resultado incluye DTO/hashpayload/copiabytesprivada/gettercopiadefensiva.
- Emisión MAIN `firmarHijo` sóloCHILD, valida propia pública igual esperada antes/después del puerto y autoverifica firma/sobre. Verificador de ambos roles para interop no agrega firmantepadreMAIN. Primitiva privada sólo handle mediante puerto `FirmanteAuthCp2`; sin crear/importar/exportar/rotar claves.

`android/app-child/src/main/java/dev/controlparental/child/auth/ClienteAuthHijo.kt`:

- `iniciar` explícito crea referenciaRAM de generación nueva contra snapshot actual, no socket. `ContextoAuthHijo` liga pairId/ambaspúblicas/origen/revisiónhashregistro/boot. En cada operación/consulta revalida contexto completo y monotonía; referencia obsoleta no cambia cliente nuevo. Tokens después de puertos impiden reentrancia de fuente que sobrescriba generación nueva.
- Primer reto válido ancla deadline10.000msmonotónico, guarda connectionId/nonce exactos y firma CHILD con pairId/pública del snapshot. Un reto por generación; replayconnectionId/nonce retenidosRAM entrecierres hasta64, sin evicción/crecimiento ilimitado (capacidadagotada rechaza). No persistencia de nonce/cuerpos/logs. Recrear proceso no reconstruye referencia/sesión/auth; no promesa de anti-replay durable entre procesos, responsabilidad de reto único del futuro servidor/canal.
- Firma fuera de monitor; después de proveedor revalida generación/contexto/reto/deadline. Cierre/expiración/cambioidentidad-revisión/erroresdescartan resultado. TicketEnvio por identidad de objeto/bytescopiados, fasePENDIENTE_ENVIO hasta confirmación explícita de éxito de ESE ticket por transporte. Copiar bytes/ticket no equivale a envío. AUTH_OK sólo en ESPERANDO_OK, mismoconnectionId, dentro del deadline y misma generación/contexto; prematuro/ajeno/duplicado cierra. No callbackviejo, timeout o PONG acredita auth.
- FaseAUTENTICADO_TRANSPORTE no es STATEAPPLIED/ALLOW/recuperación/padreoperativo/permiso NORMAL-OFFLINE; no referencias al enforcement. PING/PONG únicamente después de AUTH, respuestaPONG conserva nonce de PING, recepciónPONG exige nonce de pingpendiente/una vez. Latidos no renuevan reto ni permisos. Sin temporizador/servicio nuevo: deadline se consolida en cada operación/consulta. No se acredita liveness/watchdog/red física mediante este borde.

`android/app-child/src/main/java/dev/controlparental/child/auth/AuthAndroid.kt`:

- Borde real **callable** `AuthAndroid.crear(contexto)`, usado sólo si futuro transporte lo solicita explícitamente. Puertos reales `PairAndroid.cargar` verificado y `IdentidadOfertaAndroid.leer`: snapshot pendiente canal/recuperación puede abastecer AUTH, no promoverse a activo. Revisiónhash del registroLOCAL completo/consentimiento estable + igualdad públicaHIJO/boot. Incierto/sinregistro/identidadincoherente -> NO_DISPONIBLE sin mutar ni fabricar credencial.
- Adaptador `FirmanteAndroidKeystore` únicamente alias **privateSigning EXISTENTE**; misma pública antes/después, registroCE y snapshot revalidados. No inicialización/rotación/exportación/credencialdummy/maestro/pregunta/código/estadoDP/obligaciónenforcement. No ejecutar KeystoreAndroid en pruebasJVM.
- Una acción UI **deshabilitada** en AccesoAdultoActivity indica pendiente transporteTLS/canalcompatible. Ninguna llamada automática al boot/panel/crear/abrirsocket ni permiso nuevo/SDK/servicio/push/probe. AUTH_OK no firmado se aceptaría sólo desde la referencia del futuro canalTLS correcto; **TLS/servidorauténtico/canal real NO implementados ni acreditados por el método recibirOk**. No enlazar con enforcer/órdenes ni afirmar canal operativo.

## Pruebas/interop y salida pública nueva

- Core`AuthCp2Test`:5casosdistintos, camposcerrados5/7/4/3/5/todosrequired/alg-keyextra, tipos/constantes/protocol/v1/timeout10, duplicate/escapes/whitespace/ASCII/UTF8/depth-límites, UUIDbits/nonce32/Base64roundtrip, firmaCHILDreal/copia/hash/rolpadreemisiónrechazada/claveincorrecta/rolpayloadalteradoconfirmaoriginal/prefijosv1-PAIR-LFliteral-CRLF/DERrango-JOSE-firmamutada. Puertos de firma JCA sólo efímerosRAM.
- App`ClienteAuthHijoTest`:9casosdistintos, emisión CHILD real por borde, ticketenvío antesOK/copiaticketfallida/errorenvío/connectionajeno,10s exactos y9999ms/rollbackclock, cambiosboot/públicaHIJO-PADRE/pairId/origen/revisión/snapshotincierto, cierres/reentranciafirmante/fuente/generación/callbackobsoleto, replayreto, proveedorfirmafallido/procesonuevo, latidos sin AUTH/noALLOWeffect/PONGreplay. Contexto/clock/proveedorIO son fakesRAM, no almacenamientoAndroid/Keystore/teléfonos.
- App`InteropAuthCp2Test`:3casosdistintos. AmbosAUTH del Node967ae05a…a964ec verifican con públicasESPERADAS del fixture y rolesPARENT/CHILD reales; oferta/PAIR codec/cripto reales, pairIdAUTH ligado al PAIR y canonicidad/bytesoriginalesautenticados. Rechaza rolcontrario y tratarAUTHcomoPAIR. Genera vectorNUEVO8campos/autoverificaoferta-PAIR-ambosAUTH antes publicar/relee después; no sobrescribe el PAIR anterior. HelperspadrePAIR/AUTH **sóloJVMtest**, emisiónCHILD utiliza firmarHijo productivo con puertoJCAfake efímero. No cuerpo/privada/código real impreso.
- Fuente8 y salida8 contienen `fixture=CP2_PUBLICO_EFIMERO_SIN_AUTORIDAD,parentPublicKeyB64,childPublicKeyB64,childOfferUtf8,pairFrameUtf8,pairPayloadSha256B64,authParentFrameUtf8,authChildFrameUtf8`. SalidaUTF8canónicaexterna+LFfinal; origenhttps://relay.example:443/UUIDv4/nonce32/hashesrecoveryaleatorios3propósitos/públicasP256/DER. PrivadasJVM únicamente RAM del proceso de pruebas, sinPEM/export/Keystore/teléfonos. Exportflag explícito y createNewFile impiden sobrescritura.

**Salida** `pruebas/interop_auth_cp2_jvm_publica.json`, SHA256UTF8archivo **`14ad8921072e628e700cd4404a3a4d0506c07ce434215c13f288efe32860c289`**. JVMacepta antespublicar y relecturafinal por ambasvariantes. **Aceptación Render de ESTA nueva vueltaAUTH pendiente**, no verificador externo ejecutado aquí. VectorPAIRprevio `pruebas/interop_cp2_jvm_publica.json` conserva SHAfa04fc863d7bace67b5036751e04865e2e6d90047d333252e4a70d21f5b36bf6. Ni cruce local ni firma prueban identidadhumana/persistenciaAndroidreal/ceremonia/recuperación/canal/permiso.

## Comandos y ejecución real

Desde `android/`, conjunto completo:

```text
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

1. Bloque1completo **SUCCESS1m29s/35ejecutadas96UPTODATE**:187core/433debug/378release0fallos;5core nuevos ejecutados, appregresión ejecutada sin testsborde aún. BuildsAPKdebug-test-release, lintanálisis ejecutado/reporteUPTODATE0errores11avisos, no instrumentación.
2. Pasecore:test/appJVM: **FAILED8s/2ejecutadas46UPTODATE** por paréntesisextra en testborde, corregido; no atribuirle pruebasJVM nuevas. Reintento mismo conjunto sinbuilds **SUCCESS21s/6ejecutadas46UPTODATE**, corecached/app441debug386release0fallos (8testsbordeiniciales), antes commitbloque1f3a2900.
3. Exportación UNAvez (dos métodos seleccionados; no relectura antes de que exista salida):

```powershell
$env:HIJO_AUTH01_EXPORTAR='SI'
.\gradlew.bat :app-child:testDebugUnitTest --tests '*.InteropAuthCp2Test.dosAuth*' --tests '*.InteropAuthCp2Test.exportar*' --console=plain
Remove-Item Env:HIJO_AUTH01_EXPORTAR
```

**SUCCESS26s/2ejecutadas25UPTODATE**,2métodosInteropAUTH ejecutados, producción/corecached, no sumarlos como todas las pruebasapp. Nuevos tests/coremutación y9ºborde compilados, pero ejecución de éstos acreditada sólo en final.

4. **FINAL** comando completo arriba añadiendo `--rerun-tasks --console=plain`: **SUCCESS3m29s/131tareas131ejecutadas/0UPTODATE**. XML **187core/445debug/390release,0fallos/errores**, **5core+12appcasosnuevosdistintos** (9borde+3InteropAUTH, no duplicar por variantes). LintReleaseMAIN/test/reporte ejecutados **0errores11avisos previos de versiones**, ignoreFGSúnico/mismonodo/manifestintactos. APKAndroidTestcompilado/empaquetado, **NOinstrumentado/NOinstalado**. Sin lintDebug/verificadorAPKFake histórico resueltos por ello.

Inspección dirigida `python -B -`/ZipFile/json/hashlib/Get-FileHash: CodecAuthCp2/CriptografiaAuthCp2/AuthAndroid/ClienteAuthHijo presentes en APKappdebug/release; AuthCp2Test/ClienteAuthHijoTest/InteropAuthCp2Test/InteropCp2Test ausentes, vectores fuente/vueltaPAIR/vueltaAUTH enteros y campospúblicos/stringsQR/AUTH/etiqueta ausentes, nombres/schemaAUTH/fixturesJSONcongelados no empaquetados; rollback sóloDEBUG; schemaPAIR y vectorPAIRprevios hashesintactos/ignoreFGSúnico. No inspección exhaustivaAPK/seguridadhardware. `git diff --check` y revisión de rutas antes de commits. No logsbody/privadas/noncesreales/seriesdatosred/capturas añadidos.

## Artefactos y cierre

- APKbuildfinal **NOinstalado/NOfísico**, debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk` SHA256 **`60cf3399ec867308797696c0346fd6552d0d03bce3a35a59e97b231dbe39ee0f`**.
- Releaseunsigned `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk` SHA256 **`a4d946adbbad1468231154620381fa07c94af57615ba947b132b307a6ebb77d4`**, sólo build/no teléfono.
- ÚltimoinstaladoACREDITADO sigue históricoPAIR01 `0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9`, **sin consulta actual**. IdentidadprivateSigning/registroCE/credencial/baselineNONE-pilotosdesarmados/snapshotslimpios conservados sin acceder en esta sesión. No vínculo ni AUTHreal/canal/estadoenforcementreal modificado.
- Rutas: schemaAUTHseparado suministrado, corecp2/AuthCp2+test, appauth/ClienteAuthHijo/AuthAndroid+testsCliente/Interop, UI sólo botónAUTHdeshabilitado; vueltaAUTHpública nueva, READMEcp2/ESTADO/estaevidencia/relevo. No CP1/schemaPAIR/fixturescongelados/planes/ADR/deps/manifest/Keystorecódigo/almacénlegado/enforcer/DP/otrosrepos.
- **Unidad entregada y detener**. No ADB/A13/A56/install/QRreal/cámara/ceremonia/alta/migración/Owner/PIN/boot/radios/router/red/protecciones/DNSVPN/LOCAL07/LOCAL08cerrada/subagentes/sesiones/prompts/push/deploy. Futuro transporteTLS/canalcompatible/recuperación/órdenes/STATE fuera de esta unidad; cualquier ensayo físico requiere asignación y aprobación concreta nuevas. No siguiente tarea automática.
