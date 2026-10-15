# C8/1 — transporte de encuentro sin cámara

Versión de implementación compartida R1, 2026-10-15. Resuelve las revisiones
PADRE/HIJO-C8-R0; cada rol confirma compatibilidad en su entrega y comunica cualquier
objeción concreta antes de desviarse. No habilita ni despliega un servicio público.
CP1 y CP2 no cambian. C8 sólo transporta CHILD_OFFER y SIGNED/PAIR originales.

## Canal y confianza

WSS del mismo origen HTTPS elegido, ruta exacta `/pair`, subprotocolo único
`c8.v1`, sin query/Origin/compresión/binarios. HTTP sigue sólo health/keepalive.
Separado de `/ws`/`cp.v2`: ninguna operación de control por C8. Perfil público de
enrolamiento aún desactivado; requiere autorización específica posterior.

Posesión provisional de una pública P256 NO identifica familia/persona/adulto.
Antes de firmar padre, comparar la huella completa de la candidata con la huella
de la identidad LOCAL del hijo mostrado en ese teléfono conocido. Antes de validar
hijo, comparar la huella del padre con la identidad LOCAL mostrada por el padre,
independientemente de PAIR/C8. Ambas UI deben mostrar su propia huella local antes
de recibir candidata. Confirmación presencial explícita, no autollenar expectativas
con la pública recibida. Si la mancha impide leer/comparar texto, detener.
Origen se confirma presencialmente desde configuración local, no desde PAIR.

Conservar autoridad adulta, tickets/generaciones/guardas de legado y snapshots,
identidad existente, custodia offline de recuperación, writer/readback y estados
pendientes. Código adulto5..10 dígitos no cambia. C8 no es código adulto/recuperación.

## Codificación común

JSON canónico: claves ordenadas lexicográficamente, sin espacios, ASCII imprimible,
sin duplicados/campos adicionales; enteros seguros no negativos, profundidad≤6,
UTF8 estricto sin BOM. Mismas primitivas canónicas CP2, no cambiar sus mensajes.
Frames≤16384 bytes. Base64url canónico sin padding; públicas SPKI DER P256
canónicas≤150 bytes; firmas ECDSA SHA256 DER estrictas P256≤72 bytes, r/s>0 y<n.
nonce32 bytes; connectionId UUIDv4 minúscula. No emitir logs de cuerpos/códigos.

Cada lista siguiente es el conjunto EXACTO de campos; todas incluyen `v:1`.
No añadir campos ni otros mensajes. `kind` es literal mostrado. Dirección S=servidor.

| Dirección | kind | Campos además de v/kind |
|---|---|---|
| S→ambos | CHALLENGE | connectionId, nonceB64, relayOrigin |
| ambos→S | AUTH | payloadB64, sigB64 |
| payload AUTH | PROOF | connectionId, nonceB64, publicKeyB64, relayOrigin, role |
| S→ambos | READY | ninguno |
| CHILD→S | PUBLISH | offerB64, ttlMs |
| S→CHILD | CODE | code, remainingMs |
| PARENT→S | CLAIM | code |
| S→PARENT | OFFER | offerB64, remainingMs |
| S→CHILD | CLAIMED | parentPublicKeyB64, remainingMs |
| PARENT→S | SEND_PAIR | pairB64 |
| S→CHILD | PAIR | pairB64 |
| S→PARENT | SENT | ninguno |
| ambos→S | CANCEL | ninguno |
| S→ambos | UNAVAILABLE | ninguno |

PROOF es payload, nunca frame directo. role sólo CHILD/PARENT. AUTH firma
`UTF8("C8v1/PROOF\n") || bytesOriginalesPayload`; payload≤8192 bytes.
Coincidencia exacta con reto/socket/origen, reto≤10s, consumido ANTES de verificar:
no reintento de AUTH, replay, cambio de rol/clave o autenticación familiar por esto.
READY sólo permite operaciones C8. Sin AUTH válido ningún dato de encuentro.

offerB64 son bytes originales CHILD_OFFER CP2; pairB64 son bytes originales del
sobre SIGNED/PAIR CP2, NO sólo payload. Cada documento decodificado≤8192 bytes
en C8 (restricción de transporte adicional, no cambio del máximo general CP2).
Receptor verifica también CP2 completo. Servidor comprueba oferta/públicaCHILD;
PAIR firmaPARENT y públicasCHILD/PARENT, enrollId/enrollNonce y relayOrigin exactos
con contexto original. No reserializar ni volver a firmar documentos interiores.

## Máquina y plazos

1. Tras READY, CHILD puede publicar UNA oferta. PARENT puede reclamar un código,
   no publicar. No sustituir una oferta, reautenticar o renovar con PUBLISH.
2. Código criptográfico uniforme:8 símbolos de
   `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (32 símbolos,40 bits). UI acepta minúsculas y
   las convierte a mayúsculas ANTES de enviar; wire exactamente8mayúsculas de ese
   alfabeto, sin espacios/separadores/sustituciones0-O/1-I. Sin portapapeles/autofill.
3. Hijo recibe CHALLENGE ANTES de medir ttlMs. Inmediatamente antes de PUBLISH,
   calcula entero positivo restante hasta `min(marcoOriginal,refOferta,ahora+120s)`.
   Límite Android inmutable; fondo/cancelar/caducar/contexto cambiado cierran C8.
   Node fija `deadline = instanteMonotonicoCHALLENGE + ttlMs` (NO ahora+ttlMs),
   ttlMs1..120000. Descontar todo el tiempo desde CHALLENGE es conservador: éste
   precede la medición del presupuesto hijo; no sumar tránsito ni comparar relojes.
   Si ya venció, rechazar. READY/CLAIM/tickets/pings nunca renuevan deadline.
4. PARENT expira≤120s desde su CHALLENGE; al recibir OFFER fija también tope local
   `ahora+remainingMs`, no renueva preparador/ticket. CHILD mantiene verdad final
   del marco original, incluso si una entrega llega tarde. No prometer commit a tiempo.
5. CLAIM válido consume atómicamente y liga a UN socket/identidadPARENT distinta
   de CHILD. Notifica OFFER y CLAIMED con tiempo restante. Código consumido nunca
   vuelve disponible por cancelación/error/callback viejo. Un PARENT no reclama
   otro tras éxito. Sin listar/resolver huellas/códigos ni reemplazar reclamante.
6. Padre coteja ancla humana antes de consentimiento/firma. Selección LOCAL QR/C8
   ligada al ticket/generación; C8 omite render/capacidadQR, NO guardas comunes.
   SEND_PAIR sólo después de persistir y readback reales. UNA entrega por encuentro.
7. PAIR al hijo entra por candidata→confirmación humana independiente→VALIDAR_PAIR
   →GUARDAR_PAIR con códigos frescos y writer/readback existentes. No exige CAMERA.
   SENT sólo significa salida de transporte aceptada, NO guardado/aplicación/ACTIVO.
   Mantener canal vivo hasta fin/cancelar/caducar. Tras recibir PAIR no hay segunda
   entrega; cierre inesperado invalida candidata no guardada. Si ya se guardó,
   no revertir; comunicar incertidumbre según fase. Sin reconexión/reentrega en R1.
8. CANCEL/cierre/expiración invalida ambos sockets/encuentro; purgar documentos y
   claves de RAM. Conservar tombstone del código hasta su deadline para impedir
   reutilizarlo en esa ventana. Reinicio pierde encuentros, NO datos móviles.
   Tras persistir padre y fallo de transporte, conservar PENDIENTE; no regenerar,
   borrar ni refirmar para reintentar. Reconciliación futura fuera de R1.

## Cupos y errores

Máximo18 sockets y16 encuentros incluyendo tombstones; autenticación10s, socket
máximo120s desde CHALLENGE. Frames10/s ráfaga20 y controles10/s ráfaga20,
salida64KiB, fragmentos64/chunks128. Upgrade global10/s ráfaga40 y por dirección
real2/s ráfaga20, tabla128/TTL60s; nunca confiar en X-Forwarded-For.
CLAIM: máximo5 fallidos por socket, además cupo global de40 intentos en ventana
fija60s desde primer intento (éxitos incluidos). No confiar sólo en cupo por IP
detrás del proxy. Saturación se rechaza sin expulsar encuentros existentes.
Inexistente/consumido/expirado/ocupado devuelve el mismo UNAVAILABLE sin detalles;
errores de protocolo/firma cierran1008, límite1008/upgrade429; cierre controlado1012.
No códigos ni datos en motivos de cierre. Aplicar limpieza por deadline aunque
cliente no avise. Ningún ping de C8 renueva adulto ni evita suspensión de Render.

## Entrega y autorización

Unidad C8-I01 por rol: implementar y probar MAIN/transporte/codec/estado reales
localmente con contrato idéntico. Servidor sin habilitación pública nueva ni cambio
de bootstrap/cp.v2. Entregar hashes de contrato y pruebas; objeción concreta se
coordina antes de alterar formato. Sin ADB/instalación/códigos reales/push/deploy.
CP2 requiere después exportación pública de pareja y configuración autenticada
existente. STATE CHILD fresco firmado confirma piloto, nunca C8/QR/READY/SENT.
