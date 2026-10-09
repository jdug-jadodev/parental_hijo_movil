package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso

/** Decisión local pura. No acepta comandos, escribe estado ni usa APIs Android. */
class PolicyEngine(private val reloj: RelojMonotonico) {
    fun evaluar(entrada: EntradaEvaluacion): DecisionAcceso {
        if (!entrada.esDeviceOwner) return DecisionAcceso(ModoAcceso.SETUP, CodigoCp1.NOT_DEVICE_OWNER)
        if (!entrada.aprovisionado || !entrada.vinculado) {
            return DecisionAcceso(ModoAcceso.SETUP, CodigoCp1.NOT_PROVISIONED)
        }
        if (!entrada.integridadValida) return errorSeguro(CodigoCp1.STATE_CORRUPT)
        if (!entrada.restriccionesVerificadas) return errorSeguro(CodigoCp1.ENFORCEMENT_ERROR)
        val ahora = try {
            reloj.ahoraMs()
        } catch (_: RuntimeException) {
            return errorSeguro(CodigoCp1.STATE_CORRUPT)
        }
        if (ahora < 0) return errorSeguro(CodigoCp1.STATE_CORRUPT)

        val politica = entrada.politica
        if (politica != null && !politicaValida(politica)) return errorSeguro(CodigoCp1.STATE_CORRUPT)
        val mismoArranque = politica != null && politica.bootId == entrada.bootId
        if (mismoArranque && politica!!.aceptadaEnMs > ahora) return errorSeguro(CodigoCp1.STATE_CORRUPT)
        val restanteMs = if (mismoArranque && politica!!.duracionSegundos != null) {
            maxOf(0L, politica.duracionSegundos * 1000L - (ahora - politica.aceptadaEnMs))
        } else null
        val transiciones = mutableListOf<Long>()
        restanteMs?.takeIf { it > 0 }?.let(transiciones::add)

        fun decidir(modo: ModoAcceso, motivo: CodigoCp1, mantenimientoMs: Long? = null): DecisionAcceso =
            DecisionAcceso(modo, motivo, segundos(restanteMs), segundos(mantenimientoMs), transiciones.minOrNull())

        if (!entrada.usuarioDesbloqueado) return decidir(ModoAcceso.LOCKED, CodigoCp1.BEFORE_USER_UNLOCK)
        val bootValido = entrada.bootId?.let(UUID_V4::matches) == true
        val mantenimiento = entrada.mantenimiento
        if (bootValido && mantenimiento != null && mantenimiento.bootId == entrada.bootId) {
            if (mantenimiento.iniciadaEnMs < 0 || mantenimiento.iniciadaEnMs > ahora) {
                return errorSeguro(CodigoCp1.STATE_CORRUPT)
            }
            val margen = MANTENIMIENTO_MS - (ahora - mantenimiento.iniciadaEnMs)
            if (margen > 0) {
                transiciones += margen
                return decidir(ModoAcceso.MAINTENANCE, CodigoCp1.MAINTENANCE_ACTIVE, margen)
            }
        }
        if (!bootValido || entrada.authorizedBootId != entrada.bootId || !mismoArranque) {
            return decidir(ModoAcceso.LOCKED, CodigoCp1.BOOT_AUTH_REQUIRED)
        }

        if (!entrada.red.validada) {
            val perdida = entrada.red.perdidaEnMs
                ?: return decidir(ModoAcceso.LOCKED, CodigoCp1.NETWORK_LOST)
            if (perdida < 0 || perdida > ahora) return errorSeguro(CodigoCp1.STATE_CORRUPT)
            val margen = MARGEN_RED_MS - (ahora - perdida)
            if (margen <= 0) return decidir(ModoAcceso.LOCKED, CodigoCp1.NETWORK_LOST)
            transiciones += margen
        }
        val pong = entrada.canal.ultimoPongEnMs
        if (!entrada.canal.autenticado || pong == null) return decidir(ModoAcceso.LOCKED, CodigoCp1.CHANNEL_EXPIRED)
        if (pong < 0 || pong > ahora) return errorSeguro(CodigoCp1.STATE_CORRUPT)
        val canalRestante = CADUCIDAD_CANAL_MS - (ahora - pong)
        if (canalRestante <= 0) return decidir(ModoAcceso.LOCKED, CodigoCp1.CHANNEL_EXPIRED)
        transiciones += canalRestante

        return when (politica!!.accion) {
            AccionPolitica.LOCK -> decidir(ModoAcceso.LOCKED, CodigoCp1.PARENT_LOCK)
            AccionPolitica.LOCK_FOR -> if (restanteMs!! > 0) decidir(ModoAcceso.LOCKED, CodigoCp1.PARENT_LOCK)
                else decidir(ModoAcceso.ALLOWED, CodigoCp1.OK)
            AccionPolitica.ALLOW -> decidir(ModoAcceso.ALLOWED, CodigoCp1.OK)
            AccionPolitica.ALLOW_FOR -> if (restanteMs!! > 0) decidir(ModoAcceso.ALLOWED, CodigoCp1.OK)
                else decidir(ModoAcceso.LOCKED, CodigoCp1.TIMER_EXPIRED)
        }
    }

    private fun politicaValida(politica: PoliticaAceptada): Boolean {
        if (politica.aceptadaEnMs < 0 || !UUID_V4.matches(politica.bootId)) return false
        return when (politica.accion) {
            AccionPolitica.LOCK, AccionPolitica.ALLOW -> politica.duracionSegundos == null
            AccionPolitica.LOCK_FOR, AccionPolitica.ALLOW_FOR -> politica.duracionSegundos?.let { it in 60..604800 } == true
        }
    }

    private fun segundos(ms: Long?): Long? = ms?.let { (it + 999L) / 1000L }
    private fun errorSeguro(motivo: CodigoCp1) = DecisionAcceso(ModoAcceso.LOCKED, motivo)

    companion object {
        const val MARGEN_RED_MS = 3000L
        const val CADUCIDAD_CANAL_MS = 45000L
        const val MANTENIMIENTO_MS = 300000L
        private val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
