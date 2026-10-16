package dev.controlparental.child.lock

import java.util.concurrent.atomic.AtomicBoolean

/** Un resultado por intento. Una orden aceptada no es bloqueo aplicado ni permiso nuevo. */
internal class ConfirmacionBloqueoLocal(private val vigente: () -> Boolean,
    private val modo: () -> ModoLocal?, private val recuperar: () -> Unit,
    private val terminar: (Boolean) -> Unit) {
    private val terminada = AtomicBoolean()
    fun iniciar(solicitar: ((Boolean) -> Unit) -> Boolean) {
        if (!solicitar(::resultado)) resultado(false)
    }
    fun resultado(aplicado: Boolean) {
        if (!terminada.compareAndSet(false, true)) return
        val actual = vigente()
        val confirmado = actual && aplicado && modo() == ModoLocal.LOCKED
        if (actual && !confirmado) recuperar()
        terminar(confirmado)
    }
}
