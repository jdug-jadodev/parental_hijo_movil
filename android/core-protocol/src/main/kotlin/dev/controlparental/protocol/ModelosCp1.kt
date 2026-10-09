package dev.controlparental.protocol

enum class Proposito { AUTH, MESSAGE, PAIR, OFFLINE }
enum class Rol { PARENT, CHILD }
enum class AccionPolitica { LOCK, LOCK_FOR, ALLOW, ALLOW_FOR }
enum class ModoAcceso { LOCKED, ALLOWED, MAINTENANCE, SETUP }
enum class ResultadoOrden { APPLIED, ACCEPTED_PENDING_ENFORCEMENT, ENFORCEMENT_FAILED, REJECTED }
enum class UltimoResultado { NONE, APPLIED, ACCEPTED_PENDING_ENFORCEMENT, ENFORCEMENT_FAILED }
enum class AccionOffline { MAINTENANCE, RETIRE }

/** DTO estructurales. Ninguno acredita firma válida, autorización o política aplicada. */
sealed interface MensajeCp1 {
    val kind: String
}

data class SobreSinVerificar(
    val purpose: Proposito,
    val payloadB64: String,
    val sigB64: String,
) : MensajeCp1 {
    override val kind = "SIGNED"
}

data class DesafioConexion(val connectionId: String, val nonce: String) : MensajeCp1 {
    override val kind = "CHALLENGE"
}

data class PruebaAutenticacion(
    val pairId: String,
    val role: Rol,
    val connectionId: String,
    val nonce: String,
) : MensajeCp1 {
    override val kind = "AUTH_PROOF"
}

data class AutenticacionAceptada(val connectionId: String) : MensajeCp1 {
    override val kind = "AUTH_OK"
}

data class Ping(val nonce: String) : MensajeCp1 { override val kind = "PING" }
data class Pong(val nonce: String) : MensajeCp1 { override val kind = "PONG" }

data class Presencia(val peerRole: Rol, val online: Boolean) : MensajeCp1 {
    override val kind = "PEER_STATUS"
}

data class DestinatarioAusente(val refCommandId: String?) : MensajeCp1 {
    override val kind = "PEER_UNAVAILABLE"
}

data class ErrorRelay(val code: CodigoCp1) : MensajeCp1 { override val kind = "ERROR" }

data class SolicitudSincronizacion(val pairId: String, val syncNonce: String) : MensajeCp1 {
    override val kind = "SYNC_REQUEST"
}

data class OrdenPolitica(
    val pairId: String,
    val bootId: String,
    val childSessionId: String,
    val commandId: String,
    val commandNonce: String,
    val seq: Long,
    val action: AccionPolitica,
    val durationSec: Long?,
) : MensajeCp1 {
    override val kind = "SET_POLICY"
}

data class ConfirmacionOrden(
    val pairId: String,
    val bootId: String,
    val childSessionId: String,
    val commandId: String,
    val seq: Long,
    val commandPayloadSha256B64: String,
    val result: ResultadoOrden,
    val mode: ModoAcceso,
    val reason: CodigoCp1,
    val remainingSec: Long?,
) : MensajeCp1 {
    override val kind = "ACK"
}

data class EstadoHijo(
    val pairId: String,
    val syncNonce: String,
    val bootId: String,
    val childSessionId: String,
    val stateSeq: Long,
    val lastAcceptedSeq: Long,
    val lastCommandId: String?,
    val lastCommandPayloadSha256B64: String?,
    val lastResult: UltimoResultado,
    val mode: ModoAcceso,
    val reason: CodigoCp1,
    val remainingSec: Long?,
    val networkValidated: Boolean,
    val channelHealthy: Boolean,
    val isDeviceOwner: Boolean,
    val lockTaskActive: Boolean,
    val commandNonce: String,
) : MensajeCp1 {
    override val kind = "STATE"
}

data class OfertaHijo(
    val childPublicKeyB64: String,
    val enrollId: String,
    val enrollNonce: String,
) : MensajeCp1 {
    override val kind = "CHILD_OFFER"
}

data class HashesRecuperacion(val MAINTENANCE: String, val RECOVER_PARENT: String, val RETIRE: String)

data class AceptacionVinculo(
    val pairId: String,
    val enrollId: String,
    val enrollNonce: String,
    val parentPublicKeyB64: String,
    val childPublicKeyB64: String,
    val relayOrigin: String,
    val recoveryHashes: HashesRecuperacion,
) : MensajeCp1 {
    override val kind = "PAIR_ACCEPT"
}

data class MensajeOffline(
    val pairId: String,
    val bootId: String,
    val action: AccionOffline,
    val nonce: String,
    val esRespuesta: Boolean,
) : MensajeCp1 {
    override val kind = if (esRespuesta) "OFFLINE_RESPONSE" else "OFFLINE_CHALLENGE"
}
