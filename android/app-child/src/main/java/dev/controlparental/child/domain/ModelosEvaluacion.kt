package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso

fun interface RelojMonotonico {
    /** Milisegundos desde el arranque, incluyendo suspensión; nunca fecha civil. */
    fun ahoraMs(): Long
}

/** Última política aceptada duraderamente; H04/H09 conservarán este inicio original. */
data class PoliticaAceptada(
    val accion: AccionPolitica,
    val duracionSegundos: Long?,
    val aceptadaEnMs: Long,
    val bootId: String,
)

data class RedObservada(val validada: Boolean = false, val perdidaEnMs: Long? = null)

/** Solo cuenta el PONG correlacionado de la conexión autenticada actual; H10 lo valida. */
data class CanalObservado(val autenticado: Boolean = false, val ultimoPongEnMs: Long? = null)

/** Sesión ya autorizada por H05, mantenida en RAM; no es un permiso persistido libre. */
data class SesionMantenimiento(val bootId: String, val iniciadaEnMs: Long)

data class EntradaEvaluacion(
    val esDeviceOwner: Boolean = false,
    val aprovisionado: Boolean = false,
    val vinculado: Boolean = false,
    val integridadValida: Boolean = false,
    val restriccionesVerificadas: Boolean = false,
    val usuarioDesbloqueado: Boolean = false,
    val bootId: String? = null,
    val authorizedBootId: String? = null,
    val politica: PoliticaAceptada? = null,
    val mantenimiento: SesionMantenimiento? = null,
    val red: RedObservada = RedObservada(),
    val canal: CanalObservado = CanalObservado(),
)

/** Intención calculada, NO evidencia de aplicación Android ni ACK APPLIED. */
data class DecisionAcceso(
    val modoSolicitado: ModoAcceso,
    val motivo: CodigoCp1,
    val politicaRestanteSegundos: Long? = null,
    val mantenimientoRestanteSegundos: Long? = null,
    val reevaluarEnMs: Long? = null,
)
