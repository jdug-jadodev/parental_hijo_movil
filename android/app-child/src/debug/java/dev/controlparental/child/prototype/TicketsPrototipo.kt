package dev.controlparental.child.prototype

internal enum class FinAutenticacion { COMPROBAR, SALIR }
internal class TicketPrototipo(val id: Long, val fin: FinAutenticacion, val inicio: Long)

/** Solo debug, autoridad RAM de laboratorio; nunca guarda PIN o resultados en Bundle. */
internal class TicketsPrototipo(private val ahora: () -> Long) {
    private var siguiente = 0L
    private var pendiente: TicketPrototipo? = null
    private var comprobado: Long? = null
    private var primerPlano = false

    fun reanudar() { primerPlano = true }
    fun cancelar() { pendiente = null; comprobado = null }
    fun fondo() { primerPlano = false; cancelar() }
    fun solicitar(fin: FinAutenticacion): TicketPrototipo? {
        val t = ahora()
        if (!primerPlano || t < 0 || siguiente == Long.MAX_VALUE) return null
        cancelar()
        return TicketPrototipo(++siguiente, fin, t).also { pendiente = it }
    }
    fun completar(ticket: TicketPrototipo, credencialSistema: Boolean): FinAutenticacion? {
        if (pendiente !== ticket) return null
        val t = ahora()
        val valido = primerPlano && pendiente == ticket && credencialSistema &&
            t >= ticket.inicio && t - ticket.inicio < 120_000
        pendiente = null
        if (!valido) { comprobado = null; return null }
        if (ticket.fin == FinAutenticacion.COMPROBAR) comprobado = t
        return ticket.fin
    }
    fun consumirComprobacion(): Boolean {
        val inicio = comprobado
        comprobado = null
        val t = ahora()
        return primerPlano && inicio != null && t >= inicio && t - inicio < 120_000
    }
}
