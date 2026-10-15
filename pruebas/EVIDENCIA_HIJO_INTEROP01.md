# HIJO-INTEROP01 — PAIR público Node → JVM y vuelta JVM preparada

15 de octubre de 2026. Base `3f12aa6`. **NODE_PAIR_ACEPTADO_JVM / VUELTA_PUBLICA_JVM_GENERADA_Y_REVALIDADA / NODE_VUELTA_PENDIENTE**. Unidad exclusivamente JVM/recursos/documentación; sin cambios de producción ni pruebas físicas.

## Fuente y coordinación

- CP2-I01 íntegro suministrado en conversación previa: Node24/Render, claves privadas efímeras sólo RAM del proceso terminado según propietario; sin identidad/pair/nonce de teléfonos. No se leyó otro repositorio ni se verificó procedencia mediante acceso a Render.
- Copia exacta UTF8/LF final `android/app-child/src/test/resources/cp2/cp2-i01-publico.json`, SHA256 **`967ae05aecd28c6b7f90b8ae90e8421b408dd70acbf22fd30911085c69a964ec`**, comprobado antes de esta unidad y fijado por nueva prueba. Recurso JVM, no `src/main`/assetsAndroid/fixturesCP1. Ocho campos fuente: seis públicos compartidos y dos strings AUTH adicionales.
- Fuente normativa CP2-V01/schema exacto intacto: PAIR10/sobre5/oferta6; conteo documental padre11 no altera fuente. No cambiosschema/CP1/fixtures congelados ni reinterpretación del protocolo.
- PadrePAIR02 `2511353` MAINfirma/persistencia pública CP2pendiente implementados/noinstalado, y RenderCP2T01authsocketsNode143correctos son **reportados**, no ejecutados/verificados hijo. PAIR03 aceptada por revisión no es instalación ni ceremonia acreditada.
- **AUTH_NO_EVALUADO_HIJO**: campos authParentFrameUtf8/authChildFrameUtf8 se conservan íntegros como información adicional. No decodificar/verificar sus frames, implementar AUTHhijo, pasar a CP1/enforcer o añadir canal. Rechazar propósito AUTH en codec PAIR no significa evaluar AUTH del fixture.

## Prueba/generador sólo JVM

`android/app-child/src/test/java/dev/controlparental/child/pairing/InteropCp2Test.kt`, cinco casos distintos:

1. Hash exacto del recurso/LF final/campos externos. OfertaNode con **CodecOfertaCp2** real; PAIRNode con **CodecPairCp2/CriptografiaPairCp2** reales y pública PADRE esperada EXTERNA del fixture, no extraída como autoridad del sobre. Comprueba canonicalidad/roundtripbytesUTF8 de oferta/sobre/payload, conjuntos exactos6/5/10, hashpayload fijado, ambas públicas, nonce32/enrollId compartidos con oferta, pairId/enrollId de fuente y origen exacto `https://relay.example:443`. No basta JCA.verify directo: verificación criptográfica a través del borde CP2.
2. FuenteNode rechaza públicaesperada distinta, payloadmutado/origen distinto con firma original, campos requeridos ausentes y extras en oferta/sobre/PAIR, whitespace añadido. Cada rechazo pasa por codec/cripto productivos, sin cambiar producción.
3. Firmas reales efímeras JVM con prefijoCPv1/PAIR+LF, CPv2/PAIR+`\\n` literal o CPv2/AUTH+LF no verifican como PAIRv2; propósitoAUTH en sobre no pasa CodecPair. Caso positivo generado también revalidado por ambos codecs/cripto antes de mutaciones. Ninguna privada de Node necesaria ni disponible.
4. Genera oferta/PAIR con JCA **sólo test**, padre/hijo P256 efímeros únicos por generación, UUIDv4, nonce32 y tres hashes recovery32 independientes aleatorios MAINTENANCE/RECOVER_PARENT/RETIRE. SHA256withECDSA DERr,s por PADRE sobre UTF8(`CPv2/PAIR\n`) con **LF real** + payloadoriginal; CodecPairCp2 valida DER/rango/domain/signature al revalidar, no nuevo firmante MAIN. Origenlaboratorioexacto arriba. Oferta/payload/sobre se serializan mediante codecs productivos. Contenedor externo plano de exactamente6campos permitidos, sin AUTH/privadas/PEM/código/Keystore/datos teléfonos.
5. Relee la salida publicada ya existente y comprueba exactamente6campos/canonicalidad/LF y mismos codecs/cripto/hash/bindings, sin regenerarla ni sobrescribirla.

La exportación se habilita únicamente con `HIJO_INTEROP01_EXPORTAR=SI` y requiere archivo inexistente (`createNewFile`): nunca sustituye un vector ya entregado. Las ejecuciones normales generan sólo datosRAM para los tests y releen el público entregado; no escriben otro vector. Privadas JVM jamás serializadas/impresas/exportadas y sólo referencias locales del proceso de pruebas terminado, no Keystore. Identidades efímeras del laboratorio no se incorporan a MAIN. Las comparaciones de autoridad aquí son técnicas de laboratorio, no identidad humana ni consentimiento de un vínculo real.

## Ejecución exacta y resultados

Desde `android/`, exportación UNA vez:

```powershell
$env:HIJO_INTEROP01_EXPORTAR='SI'
.\gradlew.bat :app-child:testDebugUnitTest --tests dev.controlparental.child.pairing.InteropCp2Test --console=plain
Remove-Item Env:HIJO_INTEROP01_EXPORTAR
```

**SUCCESS4s/27tareas:3ejecutadas24UPTODATE**. Se ejecutaron los cuatro casos iniciales Interop; compilaciónKotlinJVM y recursoJVM procesados, produccióncached. Vector creado y reaceptado antes/después de escribir. Aviso compilador de nulabilidad de `System.getProperty("user.dir")` corregido con requireNotNull (sólo test); luego se añadieron comprobaciones extra y quinto caso de relectura, sin regenerar el archivo público.

Pasada final, SIN variableexportación:

```text
.\gradlew.bat :app-child:testDebugUnitTest :app-child:testReleaseUnitTest --console=plain
```

**SUCCESS12s/50tareas:7ejecutadas43UPTODATE**. XML: **433debug/378release,0fallos/errores**; InteropCp2Test **5porvariante** (mismos cinco casos, no diez distintos). Ambas tareas JVM y compilaciones de pruebas ejecutadas; core/productivoUPTODATE no contados como nuevas pruebascore. Sin assemble/buildAPK/lint/AndroidTest/instrumentación/ADB/Nodeverificador externo. Los143Node reportados no se suman a los resultados hijo.

Inspección dirigida con `python -B -`/json/ZipFile/hashlib y Get-FileHash: fuentehash exacto/LF y ocho campos; salidaLF y seis campos; en APKappdebug/release EXISTENTES de PAIR03 no aparecen nombres de recursos/salida/InteropCp2Test, etiqueta de fixture, bytes completos de archivos, públicas ni strings de oferta/PAIR de ambos vectores en contenidos ZIP descomprimidos. Se comprobó hash APKsin cambios: debug `257ea98f0f36d723cca6f5a5f3265350de31e92f4f89354ef63973c0e2dc08cf`, unsigned `55b88e9390a16add6004b2590a6b3a4c9a3e9f0cde78435b5e9c16f7a18bf807`. **No APK reconstruido en esta unidad**; exclusión futura se apoya además en rutasourceJVMtest/defaultGradle sin cambiossourceSets. No afirmar empaquetado nuevo ni inspección exhaustiva de seguridad. `git diff --check` y revisión de rutas antes del commit.

## Salida pública para Node y límites

- Archivo **`pruebas/interop_cp2_jvm_publica.json`**, exactamente6claves `fixture,parentPublicKeyB64,childPublicKeyB64,childOfferUtf8,pairFrameUtf8,pairPayloadSha256B64`. UTF8canónicoexterno + LFfinal; strings internos canónicos de codecs. No autoridad/credencial real.
- **SHA256UTF8archivo `fa04fc863d7bace67b5036751e04865e2e6d90047d333252e4a70d21f5b36bf6`**. Aceptado por codecs/criptoJVM antes de publicarlo y reaceptado por ambas variantes tras publicar. **Aceptación Node/relay de esta vuelta PENDIENTE**, no ejecutada desde hijo ni serverinterop completo acreditado.
- Rutas modificadas únicamente InteropCp2Test, recursoJVMNode, salida pública autorizada, esta evidencia, ESTADO y relevo. Sin código productivo/config/deps/planes/ADR/schema/CP1fixtures/repos ajenos. Sin teléfonos/instalación/cámara/QRreal/nuevaidentidadAndroid/credenciales/alta/permisos/boot/red/protecciones/DNSVPNLOCAL07/LOCAL08/migración/push/deploy/subagentes.
- **Unidad entregada y detener**. No identidadhumana/persistenciareal/ceremonia/recuperación/canal/permiso por interoperar criptografía. PAIR03 MAIN sólo compilada, sin instalación. No otra tarea automática ni herencia de permisos físicos; esperar nueva asignación.
