# Cómo usar el esquema JSON

`protocolo.schema.json` valida un frame o un payload **ya decodificado**. No descodifica automáticamente `payloadB64`, no verifica firmas ni demuestra que una orden esté autorizada. Se valida primero el contenedor y después la definición correspondiente al `kind` de los bytes internos.

| Purpose | Payloads válidos | Clave confiable |
|---|---|---|
| AUTH | AUTH_PROOF | Pública del rol fijada en la configuración del relay |
| MESSAGE | SYNC_REQUEST, SET_POLICY | Pública del padre ya vinculada |
| MESSAGE | STATE, ACK | Pública del hijo ya vinculada |
| PAIR | PAIR_ACCEPT | Nueva pública del padre solo en enrolamiento físico vigente, ligada a la oferta del hijo |
| OFFLINE | OFFLINE_CHALLENGE | Pública del hijo vinculado |
| OFFLINE | OFFLINE_RESPONSE | Pública del padre vinculado |

CHILD_OFFER es un QR local sin firma de autoridad previa. CHALLENGE, AUTH_OK, PING, PONG, PEER_STATUS, PEER_UNAVAILABLE y ERROR son frames de transporte; jamás sustituyen una orden o confirmación firmada. CHALLENGE/AUTH_OK vienen del relay sobre WSS; los latidos solo se aceptan después de autenticar. PAIR/OFFLINE no se aceptan en el relay.

Validaciones adicionales obligatorias: tamaño en bytes, UTF-8 estricto, ASCII del subconjunto, base64url canónico sin padding, SPKI de curva P-256, DER de firma, prefijo de propósito, serialización determinista, máximo real de seq (64 bits), relación entre estado/campos, identidad, boot, sesión, nonce y secuencia. El esquema no aplica por sí mismo esas reglas.

`STATE` describe la última orden **aceptada**. Rechazar una orden posterior no cambia lastAcceptedSeq ni sobrescribe el resultado de esa aceptación. `ACK(REJECTED)` puede describir un comando nuevo inválido en contexto sin modificar ese registro. Un mensaje malformado que no permite construir un ACK seguro se descarta o produce ERROR; no se inventan campos del comando.

`OFFLINE_CHALLENGE` del hijo y `OFFLINE_RESPONSE` del padre están firmados con propósito OFFLINE; ambos contienen pairId, bootId, nonce, action y sender según el esquema. El padre valida primero la firma/pareja del desafío. El hijo conserva el desafío y su vencimiento monotónico en RAM, y lo consume antes de habilitar mantenimiento/retirada. Una foto antigua no sirve después de consumo, vencimiento o reinicio.

`AUTH_OK` tiene connectionId. `PEER_STATUS` lleva peerRole y online, solo informativos. `PEER_UNAVAILABLE` usa reason PEER_OFFLINE y refCommandId (null en una sincronización); no es un ACK del hijo. Los payloads firmados no pueden cambiar estas definiciones unilateralmente.

Los ejemplos bajo `compartido/ejemplos` son objetos válidos de prueba. Sus identidades, nonces y claves no se usan en producción. Los ejemplos se firman y verifican en `pruebas/vectores_crypto.json`.
