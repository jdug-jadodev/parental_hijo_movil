# HIJO-PAIR02 — validar PAIR_ACCEPT CP/2 antes del commit

2026-10-14. Base hijo `0697bf5`; primer bloque `af22188` (~487 líneas Kotlin propias, sin documentación). **VALIDACION_RAM_PROBADA_SIN_COMMIT_NI_FISICA**. Cierre en commit que incorpora esta evidencia. Unidad pura MAIN/core; recepción UI permanece deshabilitada.

## Fuente y límites

- Fuente íntegra CP2-V01 Render `4ac4962`: schema/README suministrados en dos entradas antes de PAIR01, copia local/procedencia conservadas. Base draft `75eb56e` §§2–3/10.1 y gate C08 `3cc206e`, no protocolo CP/2 completo congelado ni autorización de migración. No lectura de repositorios externos ni interop servidor/Padre ejecutada.
- Coordinador reporta Render125testsCP2A01 y PadrePAIR01 `82734c7` codec/lectorMAIN/casoRAM probados, **firma MAIN no conectada por snapshot incompatible y no instalado**. Solo reportado, no reejecutado ni verificado desde hijo; no ceremonia lista.
- Discrepancia de conteo señalada: el aviso dice11 campos aceptación, pero el schema exacto trae **10**: v/kind/protocol/pairId/enrollId/enrollNonce/parentPublicKeyB64/childPublicKeyB64/relayOrigin/recoveryHashes. Se preserva la lista normativa cerrada sin inventar campo11. Sobre5, oferta6, hashes3.
- Schema intacto SHA256 `7290e6cb419e910756d3848586be24358f063455b1c65c47f24e75b5cef22239`. CP/1/fixtures/planes/ADR/deps/manifest/identidad/OfertasRam/QR/UI sin cambios. Se reutilizan primitivas técnicas JSON/base64/SPKI/DER internas existentes sin cambiar codec/dominio CP1.
- **Ningún ADB, dispositivo, instalación, ofertaQRreal, nueva identidad, políticas o permiso físico**. PAIR01 identidad/último baseline físico conservados sin consultarlos ahora. LOCAL07cancelada/LOCAL08cerradaaceptación no reabiertas; sin DNSVPN/boot/red/Owner/PIN/rotación/migración.

## Borde criptográfico separado

`android/core-protocol/src/main/kotlin/dev/controlparental/protocol/cp2/PairCp2.kt`:

- DTOs cerrados e inmutables: AceptacionPairCp2/HashesPairCp2/SobrePairCp2. Parser propio CP/2, no CodecCp1 ni tránsito de DTO v2 al enforcer. JSON ASCII imprimible/UTF8 exacto, objetos cerrados/tipos/constantes, claves duplicadas y forma no canónica rechazadas; igualdad byte a byte con serialización técnica canónica ASCII ordenada, también en sobre.
- Límites payload8192/frame16384 y profundidad máxima6 (raíz contenedor1) ANTES de parse anidado; límite de frame también antes de copia defensiva en verificador. Base64url sin padding/roundtrip, nonce/hashes32, UUIDv4lowercase, SPKI DER canónico/curvaP256 exacta, origenHTTPS patrón cerrado/URI sintáctica exacta sin trim/lowercase ni retirar puerto/punto final. No lookup DNS ni conexión; conserva texto de origen confirmado, `:443` no se equipara silenciosamente a ausencia de puerto.
- DER8..72 estricta/dosINTEGERmínimos/r,s en1..n−1/P256, noJOSE/lowS impuesto. Firma JCA real SHA256withECDSA sobre **UTF8("CPv2/PAIR\n") con LF real + bytes payload originales**; no reconstruir payload antes de verificar. Candidata pública esperada aportada independientemente por llamador y comparada con la declarada; nunca se extrae del frame como autoridad.
- `IntegridadPairCp2` distingue integridad con candidata de confianza/frescura/vínculo. Pública valida no prueba identidad humana. Resultado guarda DTO/hashSHA256 de payload original y copias privadas de payload/frame; getters devuelven copias. No escritor de firma/privadas en MAIN; firmas de pruebas viven solo JVM efímero.

## Caso RAM y autoridad adulta

`android/app-child/src/main/java/dev/controlparental/child/pairing/ValidacionPairRam.kt`, propósito nuevo `FinAdulto.VALIDAR_PAIR` en SesionAdulto:

- `confirmar` recibe explícitamente pública padre esperada y las **dos huellas SHA256 de bytes SPKI DER + origen** provenientes de confirmación humana independiente, antes de resultado. Comprueba ambas huellas y la identidad de la oferta, valida origen y crea referencia interna de confirmación ligada al mismo objeto ReferenciaOferta/ticketfresh. No UI humana implementada ni autoconfirmación desde el frame: el puerto de entrada es un contrato del llamador futuro.
- Exige SesionAdulto real/ticketVALIDAR_PAIR con otro código acreditado/revisión vigente/boot/plazo; respuesta de recuperación/ENTRAR/GENERAR_OFERTA/sólo solicitar ticket no bastan. Además exige `OfertasRam.vigente(ref)` actual (identidad de objeto, boot/pública, generación, plazo/monotonía/contexto). Oferta5min NO amplía sesión120s; código fresco tardío no renueva la vida original de oferta.
- Verificador fuera de monitor, copia entrada y bloquea reentrancia duplicada. Revalida autoridad y contexto después de cripto; comparación de padre/hijo/enrollId/enrollNonce/origen. Token interno tras consultar puertos evita publicar callback obsoleto o borrar un resultado nuevo desde consulta vieja reentrante. Cancelar/fondo/contexto viejo/reloj/boot/nueva oferta/sesión invalidan consultas/resultados; recreación no reconstruye confirmación.
- Marca RAM de una sola validación por referencia/nonce, antes de publicar snapshot. Cancelar no borra esa marca; firma inválida/contexto ajeno no consume oferta válida. No protección replay durable ni commit/storepair. Resultado inmutable conserva bytes/hash y boot/generación como evidencia, **NO PREPARADO/activo/permiso/APPLIED**. Conservar un snapshot anterior no mantiene su vigencia: resultadoActual revalida; cualquier commit futuro requeriría otra unidad y guardas propias.
- RecoveryHashes son exactamente MAINTENANCE/RECOVER_PARENT/RETIRE de ese PAIR local; no códigos maestros/pregunta ni funciones de recuperación/ALLOW. No escrituraDP/CE/claves/estado/vínculo; no scanner/cámara/WSS/STATE/órdenes.

### Límite explícito de integración

Caso puro usa OfertasRam/SesionAdulto MAIN reales, con fuentes/reloj/identidad/contexto de pruebas JVM controlados. La UI PAIR01 no se cambia: sigue autorizando vidaQR con ticket GENERAR_OFERTA y pedir VALIDAR_PAIR invalidaría ese ticket; **no se esquiva esa guarda para conservar QR ni se conecta receptor**. Laboratorio de RAM separa vida de la sesión original (anclada120s) de ticketfreshVALIDAR. Futuro puente de ciclo de vida/confirmaciones UI necesita unidad explícita, además de snapshot/persistencia y firma padre compatibles. Esta entrega no afirma validación operativa en el APK instalado/ceremonia MAIN.

## Pruebas/comandos efectivamente ejecutados

Desde `android/`:

```text
gradlew :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --console=plain
```

- Primera pasada SUCCESS1m25s/131tareas33ejecutadas98UPTODATE: core180 (11nuevos), app405debug/350release (4nuevos). JVM de esos módulos ejecutados; APKappdebug/release empaquetados, KotlinAndroidTest recompilado/paqueteUPTODATE, instrumentación NO ejecutada. LintRelease0errores11avisos/reporteUPTODATE.
- Segunda pasada mismo comando SUCCESS17s/7ejecutadas124UPTODATE: app407/352 (6nuevos), core/APKapp/AndroidTestUPTODATE; no sumar core/builds como nuevas ejecuciones. Tercera pasada sólo appJVM/lintSUCCESS17s/7ejecutadas50UPTODATE:409/354 (8nuevos), mismo lint/reporteUPTODATE, antes del commitaf22188.
- Última pasada completa tras límite previo a copia/callbackviejo y pruebas adicionales: **SUCCESS29s/31ejecutadas100UPTODATE; core182/416debug/361release, cero fallos/errores**. Nuevos casos distintos: **13core +15app** (14ValidacionPairRamTest/1SesionAdultoTest); variantes app comparten esos15, no sumar30. Casos preexistentes de OfertasRam5min/profundidad/schemahash/QR/CP1 también ejecutados como regresión. Lint análisis MAIN/test ejecutado, reporteUPTODATE0errores11avisos previos, ignoreFGS único/mismo nodo intacto. AndroidTest Kotlin/paqueteUPTODATE en esta última pasada, no físicos.

Cobertura nueva: camposcerrados10/5/3/required/desconocidos/tipos/versiones, exactcanonical/UTF8/ASCII/duplicados/orden/escapes, base64/UUIDbits/nonce/hash32/SPKIotra curva/DERextra, HTTPSorigen exacto, límites8192/16384 inclusivos/preflightdepth6 vs7, DERrango/canonicalidad/JOSE/mutación/firma real/Pv1-Pv2-propósitos-LF cruzados, clave candidata distinta/declarada distinta, hashes/copias. RAM confirmeambashuellas/origen/ticketfresh/revisión/purpose/recuperación/replay/fondo/sesiónboot-identidad-ofertanueva/nonce-enroll-hijo-origenincorrectos/reloj/cancelación-reentrancia/callbackviejo/consultaobsoleta/entradamutada/vidaoriginal120s/proceso. Claves P256 efímeras y firmas reales SOLO RAM JVM; fuentes/tiempos/puertos falsos de laboratorio no equivalen a Keystore/teléfono/humano/óptica física. Sin privadas/frames/nonces/códigos reales en repo.

Inspección dirigida `python -B -`/ZipFile/cadenasDEX: CodecPairCp2/CriptografiaPairCp2/IntegridadPairCp2/ValidacionPairRam presentes en APKappdebug/release; PairCp2Test/ValidacionPairRamTest/SesionAdultoTest ausentes, JSONfixtures no empaquetados, rollbackred sóloDEBUG. No certificación exhaustiva APK. Schema hash y manifest únicoignore comprobados. No reejecutar/verdar lintDebugprototype/verificadorFake histórico por ello. `git diff --check` antes de commits y revisión de rutas.

## Artefactos, estado y entrega

- **Build final PAIR02 NO instalado/NO ensayado físicamente**: debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk`, SHA256 `a198093ea7acb5cb4c193003eb711b3c46caa32551634f6fc5789cdcbf296328`; releaseunsigned `982f1f07669be7a8c4b9eda0957604921e511bf437cdca200ad86a9f53d82e7a`.
- Último APK instalado acreditado sigue histórico PAIR01 `0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9`. **No comprobado de nuevo en esta unidad**. Último baseline físico histórico NONE/pilotosdesarmados/snapshotslimpios/servicioausente/panelcerrado e identidadinicialautorizada se preserva sin leerlo ni cambiarlo.
- Rutas nuevas core `protocol/cp2/PairCp2.kt` y test `cp2/PairCp2Test.kt`; app `pairing/ValidacionPairRam.kt` y testhomónimo; cambios local/SesionAdulto y test (solo propósito fresco), READMEcp2 propio/estaevidencia/ESTADO/relevo. Schema/CP1/fixtures/OfertasRam/UI/Keystore/manifest/planes/ADR/deps/otrosrepos intactos; sinpush/subagentes/sesiones externas.
- **Unidad entregada y detener**. Recepción/ceremonia/commitpair/snapshot-persistencia/firma padre/WSS/migración CP2 quedan separados, no iniciar otra tarea ni tests físicos automáticamente. No H05/H06/productofinal/recuperaciónoperativa/ceroescape/internet/Doze/crash/reboot garantizados. Render no dispositivos.
