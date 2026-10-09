package dev.controlparental.child.recovery

enum class DisponibilidadRecuperacion { COMPROBANDO, DISPONIBLE, SIN_VINCULO, NO_DISPONIBLE, ERROR_SEGURO }

/** Proyección de UI sin códigos, hashes, claves o sobres firmados. */
data class EstadoPantallaRecuperacion(
    val disponibilidad: DisponibilidadRecuperacion = DisponibilidadRecuperacion.COMPROBANDO,
    val ocupado: Boolean = false,
    val respuesta: RespuestaRecuperacion? = null,
    val acceso: AccesoRecuperacionLimitado? = null,
    val consumoHabilitado: Boolean = false,
) {
    val permiteEnviar: Boolean get() = consumoHabilitado && disponibilidad == DisponibilidadRecuperacion.DISPONIBLE && !ocupado && acceso == null &&
        (respuesta?.esperaRestanteMs ?: 0) == 0L
}
