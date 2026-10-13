package dev.controlparental.child.prototype

internal enum class EstadoVencimiento { INACTIVO, VENCIDO_PENDIENTE, SOLICITADO, LOCKED, ERROR }

/** Intención vencida RAM; una transición pertinente puede reclamar un único intento. */
internal class VencimientoKeyguard(private val ahora: () -> Long) {
    var actual: ReferenciaTemporal? = null; private set
    var estado = EstadoVencimiento.INACTIVO; private set
    private var ultimo = 0L

    fun armar(ref: ReferenciaTemporal, boot: Int) {
        actual = ref
        ultimo = ahora()
        estado = if (boot == ref.boot && ultimo >= ref.deadlineMs)
            EstadoVencimiento.VENCIDO_PENDIENTE else EstadoVencimiento.ERROR
    }
    fun intentar(ref: ReferenciaTemporal, boot: Int, impedido: Boolean): Boolean {
        if (actual !== ref || estado != EstadoVencimiento.VENCIDO_PENDIENTE) return false
        val t = ahora()
        if (boot != ref.boot || t < ultimo || t < ref.deadlineMs) {
            estado = EstadoVencimiento.ERROR; return false
        }
        ultimo = t
        if (impedido) return false
        estado = EstadoVencimiento.SOLICITADO
        return true
    }
    fun terminar(ref: ReferenciaTemporal, locked: Boolean) {
        if (actual === ref && estado == EstadoVencimiento.SOLICITADO)
            estado = if (locked) EstadoVencimiento.LOCKED else EstadoVencimiento.ERROR
    }
    fun cancelar() { actual = null; estado = EstadoVencimiento.INACTIVO }
    fun fallar(ref: ReferenciaTemporal) { if (actual === ref) estado = EstadoVencimiento.ERROR }
}
