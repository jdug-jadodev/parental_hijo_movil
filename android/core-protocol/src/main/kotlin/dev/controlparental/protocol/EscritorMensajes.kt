package dev.controlparental.protocol

/** Mantiene los nombres de campos y los null explícitos definidos por el contrato. */
internal object EscritorMensajes {
    fun construir(m: MensajeCp1): ObjetoJson {
        val campos = linkedMapOf<String, ValorJson>("v" to EnteroJson(1), "kind" to TextoJson(m.kind))
        val datos: List<Pair<String, Any?>> = when (m) {
            is SobreSinVerificar -> listOf("purpose" to m.purpose, "payloadB64" to m.payloadB64, "sigB64" to m.sigB64)
            is DesafioConexion -> listOf("connectionId" to m.connectionId, "nonce" to m.nonce, "timeoutSec" to 10L)
            is PruebaAutenticacion -> listOf("pairId" to m.pairId, "role" to m.role,
                "connectionId" to m.connectionId, "nonce" to m.nonce)
            is AutenticacionAceptada -> listOf("connectionId" to m.connectionId)
            is Ping -> listOf("nonce" to m.nonce)
            is Pong -> listOf("nonce" to m.nonce)
            is Presencia -> listOf("peerRole" to m.peerRole, "online" to m.online)
            is DestinatarioAusente -> listOf("reason" to "PEER_OFFLINE", "refCommandId" to m.refCommandId)
            is ErrorRelay -> listOf("code" to m.code)
            is SolicitudSincronizacion -> listOf("pairId" to m.pairId, "sender" to "PARENT", "syncNonce" to m.syncNonce)
            is OrdenPolitica -> listOf("pairId" to m.pairId, "sender" to "PARENT", "bootId" to m.bootId,
                "childSessionId" to m.childSessionId, "commandId" to m.commandId, "commandNonce" to m.commandNonce,
                "seq" to m.seq.toString(), "action" to m.action, "durationSec" to m.durationSec)
            is ConfirmacionOrden -> listOf("pairId" to m.pairId, "sender" to "CHILD", "bootId" to m.bootId,
                "childSessionId" to m.childSessionId, "commandId" to m.commandId, "seq" to m.seq.toString(),
                "commandPayloadSha256B64" to m.commandPayloadSha256B64, "result" to m.result,
                "mode" to m.mode, "reason" to m.reason, "remainingSec" to m.remainingSec)
            is EstadoHijo -> listOf("pairId" to m.pairId, "sender" to "CHILD", "syncNonce" to m.syncNonce,
                "bootId" to m.bootId, "childSessionId" to m.childSessionId, "stateSeq" to m.stateSeq.toString(),
                "lastAcceptedSeq" to m.lastAcceptedSeq.toString(), "lastCommandId" to m.lastCommandId,
                "lastCommandPayloadSha256B64" to m.lastCommandPayloadSha256B64, "lastResult" to m.lastResult,
                "mode" to m.mode, "reason" to m.reason, "remainingSec" to m.remainingSec,
                "networkValidated" to m.networkValidated, "channelHealthy" to m.channelHealthy,
                "isDeviceOwner" to m.isDeviceOwner, "lockTaskActive" to m.lockTaskActive, "commandNonce" to m.commandNonce)
            is OfertaHijo -> listOf("childPublicKeyB64" to m.childPublicKeyB64,
                "enrollId" to m.enrollId, "enrollNonce" to m.enrollNonce)
            is AceptacionVinculo -> listOf("pairId" to m.pairId, "enrollId" to m.enrollId,
                "enrollNonce" to m.enrollNonce, "parentPublicKeyB64" to m.parentPublicKeyB64,
                "childPublicKeyB64" to m.childPublicKeyB64, "relayOrigin" to m.relayOrigin,
                "recoveryHashes" to ObjetoJson(mapOf("MAINTENANCE" to TextoJson(m.recoveryHashes.MAINTENANCE),
                    "RECOVER_PARENT" to TextoJson(m.recoveryHashes.RECOVER_PARENT), "RETIRE" to TextoJson(m.recoveryHashes.RETIRE))))
            is MensajeOffline -> listOf("pairId" to m.pairId, "bootId" to m.bootId,
                "sender" to if (m.esRespuesta) "PARENT" else "CHILD", "action" to m.action, "nonce" to m.nonce)
        }
        datos.forEach { (nombre, dato) -> campos[nombre] = valor(dato) }
        return ObjetoJson(campos.toMap())
    }

    private fun valor(dato: Any?): ValorJson = when (dato) {
        null -> NuloJson
        is String -> TextoJson(dato)
        is Long -> EnteroJson(dato)
        is Boolean -> BooleanoJson(dato)
        is Enum<*> -> TextoJson(dato.name)
        is ObjetoJson -> dato
        else -> throw ErrorProtocolo(mensaje = "Tipo local no serializable en CP/1.")
    }
}
