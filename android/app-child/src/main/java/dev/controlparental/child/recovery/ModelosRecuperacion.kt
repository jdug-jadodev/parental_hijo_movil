package dev.controlparental.child.recovery

import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.protocol.SobreSinVerificar

data class ContextoRecuperacion(val usuarioDesbloqueado: Boolean, val bootId: String?)
fun interface FuenteContextoRecuperacion { fun leer(): ContextoRecuperacion }

enum class ResultadoRecuperacion {
    AUTORIZADA, DESAFIO_CREADO, CODIGO_INVALIDO, RESPUESTA_INVALIDA, DESAFIO_VENCIDO, ESPERA, NO_DISPONIBLE, ERROR_SEGURO,
}

/** Autoriza solo una función local; jamás ALLOWED, APPLIED o una operación destructiva. */
data class RespuestaRecuperacion(
    val resultado: ResultadoRecuperacion,
    val esperaRestanteMs: Long = 0,
)

data class AccesoRecuperacionLimitado(
    val proposito: PropositoRecuperacionLocal,
    val bootId: String,
    val restanteMs: Long,
)

data class RespuestaDesafioOffline(
    val resultado: ResultadoRecuperacion,
    val sobre: SobreSinVerificar? = null,
)
