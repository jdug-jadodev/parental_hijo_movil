package dev.controlparental.protocol

/** Errores con códigos de CP/1; los mensajes no incluyen el contenido recibido. */
class ErrorProtocolo(
    val codigo: CodigoCp1 = CodigoCp1.BAD_SCHEMA,
    mensaje: String,
) : IllegalArgumentException(mensaje)

enum class CodigoCp1 {
    BAD_SCHEMA, BAD_SIGNATURE, WRONG_PAIR, WRONG_ROLE, STALE_BOOT, STALE_SESSION,
    STALE_NONCE, STALE_SEQUENCE, SEQUENCE_CONFLICT, NOT_DEVICE_OWNER,
    NOT_PROVISIONED, BEFORE_USER_UNLOCK, BOOT_AUTH_REQUIRED, NETWORK_LOST,
    CHANNEL_EXPIRED, PARENT_LOCK, TIMER_EXPIRED, OK, MAINTENANCE_ACTIVE,
    STATE_CORRUPT, ENFORCEMENT_ERROR, KEY_UNAVAILABLE, PEER_OFFLINE,
    RATE_LIMITED, UNKNOWN_VERSION, SYSTEM_KEYGUARD, CALL_IN_PROGRESS,
}

internal fun exigir(condicion: Boolean, mensaje: String) {
    if (!condicion) throw ErrorProtocolo(mensaje = mensaje)
}
