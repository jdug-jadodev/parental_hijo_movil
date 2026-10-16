package dev.controlparental.child.network

/** Decisión de reanudar el observador, nunca de restaurar un permiso de uso. */
internal enum class RecuperacionControlLocal { INACTIVO, REANUDAR, ERROR_CERRADO }
internal fun recuperacionControlLocal(existe: Boolean, uso: UsoLocal?, motivo: MotivoRed?,
    armado: Boolean?, cerrando: Boolean): RecuperacionControlLocal = when {
    cerrando || !existe || armado == false || motivo == MotivoRed.CIERRE || uso == UsoLocal.FINALIZANDO -> RecuperacionControlLocal.INACTIVO
    armado == null || uso == null || uso == UsoLocal.ERROR -> RecuperacionControlLocal.ERROR_CERRADO
    else -> RecuperacionControlLocal.REANUDAR
}
