package dev.controlparental.child.network

internal data class PlanPlazo(val referencia: ReferenciaRed, val deadline: Long)
internal interface PuertoPlazo {
    fun programar(deadline: Long, entregar: () -> Unit)
    fun cancelar()
}

/** Un único siguiente plazo, serializado por el actor. Callback cancelado no toca un plan nuevo. */
internal class ProgramadorPlazo(private val puerto: PuertoPlazo, private val vencer: (PlanPlazo) -> Unit) {
    var plan: PlanPlazo? = null; private set
    private var generacion = 0L
    fun actualizar(nuevo: PlanPlazo?) {
        if (nuevo == plan) return
        val g = ++generacion
        plan = null; puerto.cancelar()
        if (nuevo == null) return
        plan = nuevo
        try {
            puerto.programar(nuevo.deadline) {
                if (g == generacion && plan == nuevo) {
                    plan = null; vencer(nuevo)
                }
            }
        } catch (e: RuntimeException) {
            plan = null; generacion++; puerto.cancelar(); throw e
        }
    }
    fun cancelar() { generacion++; plan = null; puerto.cancelar() }
}
