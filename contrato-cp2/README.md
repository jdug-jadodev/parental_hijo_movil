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
