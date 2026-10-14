package dev.controlparental.child.network

internal data class CondicionesObservador(val apiAdmitida: Boolean, val deviceOwner: Boolean,
    val notificaciones: Boolean, val canalVisible: Boolean) {
    val validas get() = apiAdmitida && deviceOwner && notificaciones && canalVisible
}

/** Dos confirmaciones distintas: startForeground retornó y callback se registró. */
internal class ObservadorVisible {
    @Volatile private var foreground = false
    @Volatile private var callback = false
    fun confirmarForeground() { foreground = true }
    fun confirmarCallback() { callback = true }
    fun invalidar() { foreground = false; callback = false }
    fun vigente(c: CondicionesObservador) = foreground && callback && c.validas
    fun autorizar(m: UsoRedLocal, tipo: UsoLocal, c: CondicionesObservador, minutos: MinutosUso? = null,
        autoridad: () -> Boolean): Boolean {
        if (!vigente(c)) {
            invalidar(); m.referencia?.let(m::falloEnforcement)
            return false
        }
        return m.autorizar(tipo, minutos, autoridad)
    }
}
