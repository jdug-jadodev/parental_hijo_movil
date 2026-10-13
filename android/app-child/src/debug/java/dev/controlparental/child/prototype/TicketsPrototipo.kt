package dev.controlparental.child.prototype

internal enum class FinAutenticacion { COMPROBAR, SALIR, PERMITIR }
internal class TicketPrototipo(val id: Long, val fin: FinAutenticacion, val inicio: Long)

/** Solo debug, autoridad RAM de laboratorio; nunca guarda PIN o resultados en Bundle. */
internal class TicketsPrototipo(private val ahora: () -> Long) {
    private var siguiente = 0L
    private var pendiente: TicketPrototipo? = null
    private var comprobado: Long? = null
    private var primerPlano = false
    var causa = Causa.TICKET_FALTA; private set

    fun reanudar() { primerPlano = true }
    fun cancelar() { pendiente = null; comprobado = null; causa = Causa.CANCELADO }
    fun fondo() { primerPlano = false; cancelar(); causa = Causa.FONDO }
    fun solicitar(fin: FinAutenticacion): TicketPrototipo? {
        val t = ahora()
        if (!primerPlano || t < 0 || siguiente == Long.MAX_VALUE) {
            causa = if (!primerPlano) Causa.FONDO else Causa.RELOJ_INVALIDO
            return null
        }
        cancelar()
        causa = Causa.NINGUNA
        return TicketPrototipo(++siguiente, fin, t).also { pendiente = it }
    }
    fun completar(ticket: TicketPrototipo, credencialSistema: Boolean): FinAutenticacion? {
        if (pendiente !== ticket) return null
        val t = ahora()
        val valido = primerPlano && pendiente == ticket && credencialSistema &&
            t >= ticket.inicio && t - ticket.inicio < 120_000
        pendiente = null
        if (!valido) {
            causa = when {
                !primerPlano -> Causa.FONDO
                !credencialSistema -> Causa.CREDENCIAL_RECHAZADA
                t < ticket.inicio -> Causa.RELOJ_INVALIDO
                else -> Causa.TICKET_CADUCADO
            }
            comprobado = null; return null
        }
        causa = Causa.NINGUNA
        if (ticket.fin == FinAutenticacion.COMPROBAR) comprobado = t
        return ticket.fin
    }
    fun consumirComprobacion(): Boolean {
        if (comprobar() != Causa.NINGUNA) return false
        comprobado = null
        causa = Causa.TICKET_FALTA
        return true
    }
    fun comprobar(): Causa {
        val inicio = comprobado
        val t = ahora()
        return when {
            !primerPlano -> Causa.FONDO
            inicio == null -> if (causa == Causa.NINGUNA) Causa.TICKET_FALTA else causa
            t < inicio -> Causa.RELOJ_INVALIDO
            t - inicio >= 120_000 -> Causa.TICKET_CADUCADO
            else -> Causa.NINGUNA
        }
    }
}
