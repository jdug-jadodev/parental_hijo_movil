package dev.controlparental.child.prototype

internal class ReferenciaTemporal(val generacion: Long, val boot: Int,
    val aceptadoMs: Long, val deadlineMs: Long)
internal enum class EntregaTemporal { OBSOLETA, TEMPRANA, VENCIDA, RELOJ_INVALIDO, BOOT_DISTINTO }

/** Laboratorio RAM: tiempo transcurrido, sin CP/1, persistencia, historial o reinicio. */
internal class PermisoTemporal(private val ahora: () -> Long) {
    var actual: ReferenciaTemporal? = null; private set
    private var generacion = 0L
    private var entregado = false
    private var ultimo = 0L

    fun preparar(minutos: Int, boot: Int): ReferenciaTemporal {
        require(minutos in 1..1440 && boot >= 0)
        val t = ahora()
        require(t >= ultimo && t >= 0)
        return ReferenciaTemporal(Math.incrementExact(generacion), boot, t,
            Math.addExact(t, Math.multiplyExact(minutos.toLong(), 60_000L)))
    }
    fun confirmar(ref: ReferenciaTemporal) {
        val t = ahora()
        require(ref.generacion == Math.incrementExact(generacion) &&
            t >= ref.aceptadoMs && t < ref.deadlineMs && ref.aceptadoMs >= ultimo)
        generacion = ref.generacion
        actual = ref; entregado = false; ultimo = t
    }
    fun entregar(ref: ReferenciaTemporal, boot: Int): EntregaTemporal {
        if (actual !== ref || entregado) return EntregaTemporal.OBSOLETA
        if (boot != ref.boot) return EntregaTemporal.BOOT_DISTINTO
        val t = ahora()
        if (t < ultimo) return EntregaTemporal.RELOJ_INVALIDO
        ultimo = t
        if (t < ref.deadlineMs) return EntregaTemporal.TEMPRANA
        entregado = true
        return EntregaTemporal.VENCIDA
    }
    fun cancelar() { actual = null; entregado = true; generacion = Math.incrementExact(generacion) }

    companion object {
        fun minutos(texto: String): Int? = texto.takeIf { it.matches(Regex("[0-9]{1,4}")) }
            ?.toIntOrNull()?.takeIf { it in 1..1440 }
    }
}
