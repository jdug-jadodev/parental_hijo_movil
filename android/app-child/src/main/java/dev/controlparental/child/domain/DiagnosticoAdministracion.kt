package dev.controlparental.child.domain

/** Datos de diagnóstico, nunca autorización de uso ni evidencia de quiosco. */
data class DiagnosticoAdministracion(
    val esDeviceOwner: Boolean? = null,
    val administradorActivo: Boolean? = null,
) {
    val estadoPreparacion: EstadoPreparacion
        get() = when (esDeviceOwner) {
            null -> EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE
            false -> EstadoPreparacion.NO_CONFIGURADO
            true -> if (administradorActivo == true) EstadoPreparacion.BLOQUEADO_EN_PREPARACION
                else EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE
        }
}

enum class EstadoPreparacion {
    NO_CONFIGURADO,
    DIAGNOSTICO_NO_DISPONIBLE,
    BLOQUEADO_EN_PREPARACION,
}
