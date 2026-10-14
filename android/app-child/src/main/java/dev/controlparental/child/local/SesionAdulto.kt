package dev.controlparental.child.local

internal enum class FinAdulto { ALTA, ENTRAR, RECUPERAR, CAMBIAR, BLOQUEAR, SALIR_ENSAYO }
internal class TicketAdulto(val epoca: Long, val fin: FinAdulto, val boot: Int, val limite: Long)
internal enum class AutoridadAdulto { CERRADA, ADMINISTRAR, CAMBIAR_CODIGO }

/** Autoridad solo RAM; no escribe ni concede permiso de uso y no conoce CP/1. */
internal class SesionAdulto(private val ahora: () -> Long, private val boot: () -> Int) {
    private var epoca = 0L
    private var primerPlano = false
    private var ticket: TicketAdulto? = null
    private var accionAcreditada: TicketAdulto? = null
    private var autoridad = AutoridadAdulto.CERRADA
    private var revision = 0L
    private var limite = 0L
    private var arranque = -1
    private var ultimo = 0L

    @Synchronized fun reanudar() { cerrar(); primerPlano = true }
    @Synchronized fun cerrar() {
        epoca++; primerPlano = false; ticket = null; accionAcreditada = null; autoridad = AutoridadAdulto.CERRADA
        revision = 0; limite = 0; arranque = -1
    }
    @Synchronized fun estado(): AutoridadAdulto {
        val t = ahora()
        if (!primerPlano || t < ultimo || t >= limite || boot() != arranque) autoridad = AutoridadAdulto.CERRADA
        ultimo = t
        return autoridad
    }
    @Synchronized fun solicitar(fin: FinAdulto): TicketAdulto? {
        if (!primerPlano || boot() < 0 || ahora() < ultimo) return null
        if (fin == FinAdulto.CAMBIAR && estado() != AutoridadAdulto.CAMBIAR_CODIGO) return null
        if (fin in setOf(FinAdulto.BLOQUEAR, FinAdulto.SALIR_ENSAYO) && estado() != AutoridadAdulto.ADMINISTRAR) return null
        accionAcreditada = null
        val t = ahora(); ultimo = t
        val vencimiento = if (fin == FinAdulto.CAMBIAR) minOf(limite, Math.addExact(t, 120_000))
            else Math.addExact(t, 120_000)
        return TicketAdulto(++epoca, fin, boot(), vencimiento).also { ticket = it }
    }
    @Synchronized fun vigente(t: TicketAdulto): Boolean {
        val n = ahora(); val valida = primerPlano && ticket === t && t.epoca == epoca &&
            boot() == t.boot && n >= ultimo && n < t.limite
        if (n < ultimo || boot() != t.boot || n >= t.limite) {
            autoridad = AutoridadAdulto.CERRADA
            if (ticket === t) ticket = null
        }
        ultimo = n
        return valida
    }
    @Synchronized fun completar(t: TicketAdulto, r: ResultadoAdulto): Boolean {
        if (!vigente(t)) return false
        ticket = null
        autoridad = when {
            t.fin == FinAdulto.ENTRAR && r.tipo == TipoAdulto.AUTENTICADO -> AutoridadAdulto.ADMINISTRAR
            t.fin == FinAdulto.RECUPERAR && r.tipo == TipoAdulto.RESPUESTA_VALIDA -> AutoridadAdulto.CAMBIAR_CODIGO
            else -> AutoridadAdulto.CERRADA
        }
        revision = r.revision; limite = t.limite; arranque = t.boot
        return true
    }
    @Synchronized fun revisionRecuperacion(): Long? =
        if (estado() == AutoridadAdulto.CAMBIAR_CODIGO) revision else null
    /** Exige otro código válido para cada operación, ligado a revisión y propósito. */
    @Synchronized fun autorizarAccion(t: TicketAdulto, r: ResultadoAdulto): Boolean {
        if (!vigente(t) || t.fin !in setOf(FinAdulto.BLOQUEAR, FinAdulto.SALIR_ENSAYO) ||
            r.tipo != TipoAdulto.AUTENTICADO || r.revision != revision) return false
        accionAcreditada = t; limite = t.limite; arranque = t.boot
        return true
    }
    @Synchronized fun accionVigente(t: TicketAdulto) = accionAcreditada === t && vigente(t)
}
