# CP/2 parcial — fuente local suministrada

HIJO-PAIR01, 2026-10-14. Copia del texto íntegro de
`contrato-cp2/vinculacion.schema.json`, suministrado por el propietario como
FUENTE CP2-V01 Render `4ac4962`; README de esa fuente también suministrado íntegro
en la entrada siguiente. No se leyó el repositorio externo ni se verificó su commit.
117 pruebas/13 nuevas Render son resultado reportado, no ejecutado aquí.
La copia conserva el texto del schema con LF y salto final; su hash local no
certifica el hash del archivo remoto ni sus terminadores de línea.

Base normativa: draft PADRE-C04-R1 `75eb56e` §§2–3/10.1, gate local C08
`3cc206e` APTO y autorización explícita HIJO-PAIR01. No congelación CP/2 completa.
CP/1 y fixtures en compartido/ siguen intactos. Se reutilizan únicamente primitivas
técnicas JSON/base64/SPKI ya existentes sin cambiar sus dominios ni codecs CP/1.
El borde CP/2 impone profundidad6 ANTES del parser y canonicalidad exacta.

Entrega PAIR01: implementación Kotlin limitada a CHILD_OFFER no firmado, seis campos cerrados,
v2/protocol cp.v2. No PAIR_ACCEPT, verificación PAIR, commitpair, códigos/hashes de
recuperación, WSS, órdenes/STATE, migración, perfil público ni ceremonia operativa.
Oferta no demuestra identidad humana, frescura al receptor, bloqueo ni permiso.
El QR es texto original CHILD_OFFER, no aprovisionamiento Android Device Owner.

Oferta RAM300000ms ligada a generación/boot/identidad/autoridad adulta fresca.
La sesión adulta actual vence antes (120000ms desde solicitar código fresco):
no se alarga para mostrar un QR cinco minutos. Fondo/salir/cancelar/caducar sesión,
boot/reloj discontinuo/identidad o contexto no disponible retiran oferta y QR.
No autooferta, persistencia de nonce, Bundle, log, clipboard, compartir ni red.
FLAG_SECURE bloquea capturas del dispositivo, no cámara externa presencial futura.
Recepción de aceptación explícitamente deshabilitada.

## PAIR02 — validación previa al commit, solo RAM

El borde nuevo `protocol/cp2/PairCp2.kt` valida PAIR_ACCEPT, sobre y firma
SHA256withECDSA sobre `UTF8("CPv2/PAIR\n") || payload` original (LF real).
Schema intacto: aceptación **10 campos** (no 11 del aviso de coordinación),
sobre5/oferta6 y recoveryHashes3. Candidata padre esperada independiente del
frame; integridad con ella no acredita confianza humana ni oferta viva.
El caso `ValidacionPairRam` exige ambas huellas/origen confirmados, oferta RAM
actual y ticket VALIDAR_PAIR acreditado por otro código fresco, mismo contexto
boot/generación/identidad/enrollId/nonce/plazos. Resultado y bytes/hash inmutables,
sin storepair, persistencia de sobre/nonce, promoción de vínculo, recuperación
operativa o permiso. Una validación por oferta solo en RAM; no replay durable.

La UI receptora sigue deshabilitada. No se conecta scanner, snapshot/persistencia
ni firma MAIN padre. El puerto de vida de oferta y el código fresco de validación
no deben confundirse: la UI PAIR01 aún liga su oferta al ticket GENERAR_OFERTA,
que solicitar otro propósito invalida. No se elude esa guarda ni se amplía su
sesión; el futuro puente de ciclo de vida UI requiere una unidad explícita.
PAIR02 prueba el caso puro con OfertasRam/SesionAdulto reales y puertos JVM
controlados, acotando la vida de oferta a la sesión original120s además del
límite modelo300000ms. No ceremonia real/CP2completo/migración por esta entrega.

## PAIR03 — integración MAIN, compilada sin ceremonia física

El límite de UI anterior queda resuelto en código: MarcoSesionAdulto mantiene
la sesión ORIGINAL120s por identidad de objeto/revisión/boot; GENERAR_OFERTA,
VALIDAR_PAIR y GUARDAR_PAIR tienen tickets propios/código fresco, acotados al
mismo marco. OfertasRam separa autorización de generar y vida de sesión sin
ampliar su plazo5min ni recrearla al volver del fondo. ValidacionPairRam sigue
siendo el único caso de validación de contexto; el snapshot RAM no es permiso.

AccesoAdultoActivity conecta Camera2/ImageReader y ZXing existente, sin intentos
externos, archivos de cámara, clipboard o SDK nuevo. CAMERA es runtime/peligroso:
diálogo explícito, pedir/negar retira oferta, nunca autoescanea tras el permiso.
Dos pasos RAM: integridad de candidata (NO confianza), luego comparación humana
de pública/ambas huellas/origen con el padre conocido, huella padre y origen
esperados introducidos independientemente/sin predeterminar, casilla y otro
código para validar. Otro código GUARDAR_PAIR consiente el guardado irreversible.

Registro LOCAL versionado1/perfilcp.v2 en CE/noBackup `vinculo-cp2-v1`, separado
de CP1/adulto/boot/consumos. `.intento` exclusivo+sync precede reserva AtomicFile;
reserva SHA256 del registro y registro AtomicFile con PAIR original/hash,
consentimiento versionado/pública esperada/huellas/origen/revisión/boot/generación,
nonceconsumidoSHA256. Datos de pareja/recoveryHashes exactos se derivan únicamente
de ese PAIR verificado. Readback exacto y verificación criptográfica antes de
publicar. Resto/parcial/incoherencia nunca autoriza reemplazo; sin reparación,
rotación, migración o reintento automático. No segunda alta si ya está guardado.

Estado honesto `VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION`; getter
PairAndroid.cargar para transporte futuro revalida firma con candidata ESPERADA
del consentimiento guardado y compatibilidad de identidad/legado, no con una
clave nueva de un mensaje. No órdenes/STATE/ACK/recibo nuevo/canal ni activación
de recuperación CP1 por estos hashes. Sin escrituraDP/permisos/credenciales.
JVM y builds/lint comprobados; cámara/diálogo/ceremonia/IO Android/commit real
NO ejecutados en teléfono. PadrePAIR02/firma-persistencia nueva sigue pendiente
según coordinación reportada, no integración externa acreditada.

## AUTH01 — borde CHILD compilado sin red

Fuente separada `autenticacion.schema.json`: texto íntegro CP2-A01 Render
`d99ddac` suministrado por el propietario, LF/saltofinal; hashlocal
`db96b66ec81a379836751491244bb498a3b45ad75428593d9ea2c8755e0dd17a`.
No acceso al repositorio externo ni modificación de CP1/schemaPAIR.
Codec/DTO/criptoAUTH en corecp2: CHALLENGE5/AUTH_PROOF7/AUTH_OK4/PING-PONG3/
SIGNED-AUTH5, dominio `UTF8("CPv2/AUTH\n")` LF real y payloadoriginal,
canonicalidadASCII/UTF8/camposcerrados/profundidad6/Base64/UUIDv4/nonce32/
P256DERr,s/8192payload-16384frame, sin alg/key del mensaje ni fallbackv1.
Emisión productiva sólo CHILD; verificación de ambos roles con pública esperada.

`AuthAndroid.crear` construye cliente callable con puertos reales PairAndroid/
identidadCE/privateSigning existente; no genera/importa/rota/exporta clave ni
modifica registros. `iniciar` es intención explícita del futuro transporte,
NO abre socket. Revalida snapshot completo/revisiónhash/boot/identidad, reto10s
monotónico y referenciasRAM por generación; ticket de bytes no es envío hasta
confirmar ESTE objeto y éxito, AUTH_OK sólo después/mismoconnectionId/vigencia.
Callbacks viejos/cambio snapshot/errores/replay cierran sin afectar generación
nueva. Hasta64retos retenidos RAM, sin evicción; capacidad agotada rechaza.
PING/PONG no autentican ni conceden uso; PONG exige nonce pendiente.

La acción de conexión MAIN permanece DESHABILITADA hasta transporteTLS/canal
compatible; no boot/panel/socket automático. AUTH_OK sin firma sólo sería
aceptable del canal TLS correcto futuro, que esta unidad NO crea ni acredita.
Estado AUTENTICADO_TRANSPORTE no acredita permiso, estado de enforcement,
recuperación operativa, padre activo ni liveness/canal compatible. No órdenes,
STATE/ACK/SYNC, recuperación, ALLOW ni cambio de snapshot pendiente por AUTH.
Caducidad se comprueba en cada operación/consulta, sin scheduler/socket nuevo.

INTEROP01 queda histórico AUTH_NO_EVALUADO; AUTH01 ahora verifica los dos AUTH
del recursoNode967ae05a…a964ec por codec/cripto reales. Vuelta NUEVA autorizada
de8campos en `pruebas/interop_auth_cp2_jvm_publica.json`, pública/efímeraJVM,
sin sobrescribir vectorPAIR6campos. Verificación Render de esta nueva vuelta
pendiente; no Androidreal/Keystorefísico/canal/ceremonia por pruebasJVM.
