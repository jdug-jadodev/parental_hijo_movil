package dev.controlparental.child.orders

import android.content.Context
import android.os.SystemClock
import dev.controlparental.child.pairing.*

/** Sin escritor CONTROL CP2 durable todavía: no usar revisión adulta/CP1 ni autoridad RAM alternativa. */
internal object ControlOrdenNoDisponible : EscritorControlOrdenCp2 {
    override fun consolidar(): ControlOrdenCp2? = null
    override fun consultarRetenida(orden: dev.controlparental.protocol.cp2.OrdenVerificadaCp2) { error("NO_DISPONIBLE: falta transacción durable de duplicados") }
    override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) { error("NO_DISPONIBLE: falta escritor CONTROL CP2 durable") }
}
internal object OrdenesAndroid {
    fun crear(contexto: Context): GuardiaOrdenCp2 {
        val app = contexto.applicationContext
        return GuardiaOrdenCp2(SystemClock::elapsedRealtime, {
            val s = (PairAndroid.cargar(app) as? CargaPairCp2.Guardado)?.snapshot
            val identidad = IdentidadOfertaAndroid(app).leer().contexto
            if (s == null || identidad == null || identidad.identidad != s.publicaHijo) null else
                ParejaOrdenCp2(s.pairId, s.publicaPadreEsperada, s.publicaHijo,
                    CodecRegistroPair.sha(CodecRegistroPair.escribir(s.integridad, s.consentimiento)), identidad.boot)
        }, ControlOrdenNoDisponible)
    }
}
