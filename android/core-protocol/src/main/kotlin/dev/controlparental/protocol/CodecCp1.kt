package dev.controlparental.protocol

/** Valida estructura, no firmas ni autorización. No enviar estos DTO al enforcer. */
object CodecCp1 {
    private val PAYLOADS = setOf("AUTH_PROOF", "SYNC_REQUEST", "SET_POLICY", "ACK", "STATE",
        "PAIR_ACCEPT", "OFFLINE_CHALLENGE", "OFFLINE_RESPONSE")
    private val FRAMES = setOf("SIGNED", "CHALLENGE", "AUTH_OK", "PING", "PONG",
        "PEER_STATUS", "PEER_UNAVAILABLE", "ERROR")

    fun leerFrame(bytes: ByteArray): MensajeCp1 {
        val mensaje = leerDocumentoSinAutorizar(bytes)
        exigir(mensaje.kind in FRAMES, "El transporte requiere un frame o un sobre firmado.")
        return mensaje
    }

    /** Para QR/fixtures/inspección; un DTO correcto sigue sin ser una orden autorizada. */
    fun leerDocumentoSinAutorizar(bytes: ByteArray): MensajeCp1 = decodificar(JsonCp1.leer(bytes))

    /** C02 verificará la firma sobre bytes originales ANTES de llamar a esta función. */
    fun leerPayloadSinVerificar(bytes: ByteArray, purpose: Proposito): MensajeCp1 {
        val mensaje = decodificar(JsonCp1.leerCanonico(bytes))
        val esperado = when (mensaje.kind) {
            "AUTH_PROOF" -> Proposito.AUTH
            "SYNC_REQUEST", "SET_POLICY", "ACK", "STATE" -> Proposito.MESSAGE
            "PAIR_ACCEPT" -> Proposito.PAIR
            "OFFLINE_CHALLENGE", "OFFLINE_RESPONSE" -> Proposito.OFFLINE
            else -> throw ErrorProtocolo(mensaje = "Este tipo no es un payload firmado.")
        }
        exigir(purpose == esperado, "El propósito no corresponde al payload.")
        return mensaje
    }

    fun serializar(mensaje: MensajeCp1): ByteArray {
        val objeto = EscritorMensajes.construir(mensaje)
        decodificar(objeto) // Los DTO construidos localmente también cumplen el esquema.
        val bytes = JsonCp1.serializar(objeto)
        val limite = if (mensaje.kind in PAYLOADS) JsonCp1.MAX_PAYLOAD_BYTES else JsonCp1.MAX_FRAME_BYTES
        exigir(bytes.size <= limite, "Mensaje serializado demasiado grande.")
        return bytes
    }

    private fun decodificar(valor: ValorJson): MensajeCp1 {
        val objeto = valor as? ObjetoJson ?: throw ErrorProtocolo(mensaje = "El mensaje debe ser un objeto.")
        val c = LectorCampos(objeto)
        if (c.entero("v") != 1L) throw ErrorProtocolo(CodigoCp1.UNKNOWN_VERSION, "Versión CP desconocida.")
        val kind = c.texto("kind")
        val mensaje = when (kind) {
            "SIGNED" -> {
                val purpose = c.enumerado<Proposito>("purpose")
                val payload = c.texto("payloadB64")
                Base64Cp1.decodificar(payload, JsonCp1.MAX_PAYLOAD_BYTES)
                val firma = c.texto("sigB64")
                exigir(firma.length in 8..100, "Tamaño de firma inválido.")
                Base64Cp1.decodificar(firma, 75)
                SobreSinVerificar(purpose, payload, firma)
            }
            "CHALLENGE" -> DesafioConexion(c.uuid("connectionId"), c.bytes32("nonce")).also {
                exigir(c.entero("timeoutSec") == 10L, "Timeout de autenticación incorrecto.")
            }
            "AUTH_PROOF" -> PruebaAutenticacion(c.uuid("pairId"), c.enumerado("role"),
                c.uuid("connectionId"), c.bytes32("nonce"))
            "AUTH_OK" -> AutenticacionAceptada(c.uuid("connectionId"))
            "PING" -> Ping(c.bytes32("nonce"))
            "PONG" -> Pong(c.bytes32("nonce"))
            "PEER_STATUS" -> Presencia(c.enumerado("peerRole"), c.booleano("online"))
            "PEER_UNAVAILABLE" -> DestinatarioAusente(c.uuidOpcional("refCommandId")).also {
                c.literal("reason", "PEER_OFFLINE")
            }
            "ERROR" -> ErrorRelay(c.enumerado("code"))
            "SYNC_REQUEST" -> SolicitudSincronizacion(c.uuid("pairId"), c.bytes32("syncNonce")).also {
                c.literal("sender", "PARENT")
            }
            "SET_POLICY" -> orden(c)
            "ACK" -> confirmacion(c)
            "STATE" -> estado(c)
            "CHILD_OFFER" -> OfertaHijo(c.clavePublica("childPublicKeyB64"),
                c.uuid("enrollId"), c.bytes32("enrollNonce"))
            "PAIR_ACCEPT" -> vinculo(c)
            "OFFLINE_CHALLENGE", "OFFLINE_RESPONSE" -> MensajeOffline(c.uuid("pairId"),
                c.uuid("bootId"), c.enumerado("action"), c.bytes32("nonce"), kind == "OFFLINE_RESPONSE").also {
                c.literal("sender", if (it.esRespuesta) "PARENT" else "CHILD")
            }
            else -> throw ErrorProtocolo(mensaje = "Tipo de mensaje desconocido.")
        }
        c.terminar()
        return mensaje
    }

    private fun orden(c: LectorCampos): OrdenPolitica {
        c.literal("sender", "PARENT")
        return OrdenPolitica(c.uuid("pairId"), c.uuid("bootId"), c.uuid("childSessionId"),
            c.uuid("commandId"), c.bytes32("commandNonce"), c.secuencia("seq"),
            c.enumerado("action"), c.segundosOpcionales("durationSec")).also {
            val temporal = it.action == AccionPolitica.LOCK_FOR || it.action == AccionPolitica.ALLOW_FOR
            exigir(if (temporal) it.durationSec != null && it.durationSec in 60..604800 else it.durationSec == null,
                "La duración no corresponde a la acción de política.")
        }
    }

    private fun confirmacion(c: LectorCampos): ConfirmacionOrden {
        c.literal("sender", "CHILD")
        return ConfirmacionOrden(c.uuid("pairId"), c.uuid("bootId"), c.uuid("childSessionId"),
            c.uuid("commandId"), c.secuencia("seq"), c.bytes32("commandPayloadSha256B64"),
            c.enumerado("result"), c.enumerado("mode"), c.enumerado("reason"), c.segundosOpcionales("remainingSec"))
    }

    private fun estado(c: LectorCampos): EstadoHijo {
        c.literal("sender", "CHILD")
        return EstadoHijo(c.uuid("pairId"), c.bytes32("syncNonce"), c.uuid("bootId"),
            c.uuid("childSessionId"), c.secuencia("stateSeq"), c.secuencia("lastAcceptedSeq", true),
            c.uuidOpcional("lastCommandId"), c.bytes32Opcionales("lastCommandPayloadSha256B64"),
            c.enumerado("lastResult"), c.enumerado("mode"), c.enumerado("reason"), c.segundosOpcionales("remainingSec"),
            c.booleano("networkValidated"), c.booleano("channelHealthy"), c.booleano("isDeviceOwner"),
            c.booleano("lockTaskActive"), c.bytes32("commandNonce")).also {
            val sinOrden = it.lastAcceptedSeq == 0L
            exigir(if (sinOrden) it.lastCommandId == null && it.lastCommandPayloadSha256B64 == null &&
                it.lastResult == UltimoResultado.NONE else it.lastCommandId != null &&
                it.lastCommandPayloadSha256B64 != null && it.lastResult != UltimoResultado.NONE,
                "El estado no es consistente con la última aceptación.")
        }
    }

    private fun vinculo(c: LectorCampos): AceptacionVinculo {
        val pairId = c.uuid("pairId")
        val enrollId = c.uuid("enrollId")
        val enrollNonce = c.bytes32("enrollNonce")
        val parentKey = c.clavePublica("parentPublicKeyB64")
        val childKey = c.clavePublica("childPublicKeyB64")
        val origen = c.texto("relayOrigin")
        exigir(origen.length <= 200 && Regex("https://[a-z0-9][a-z0-9.-]*(?::[0-9]{1,5})?").matches(origen),
            "El origen del relay debe cumplir el formato HTTPS de CP/1.")
        val hashes = c.valor("recoveryHashes") as? ObjetoJson
            ?: throw ErrorProtocolo(mensaje = "Hashes de recuperación inválidos.")
        val h = LectorCampos(hashes)
        val recuperacion = HashesRecuperacion(h.bytes32("MAINTENANCE"), h.bytes32("RECOVER_PARENT"), h.bytes32("RETIRE"))
        h.terminar()
        return AceptacionVinculo(pairId, enrollId, enrollNonce, parentKey, childKey, origen, recuperacion)
    }
}
