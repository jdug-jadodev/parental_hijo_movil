package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PoliticasTablaTest(private val accion: AccionPolitica, private val transcurridoMs: Long) {
    @Test
    fun `la accion y su vencimiento producen la decision exacta`() {
        val ahora = 100000L + transcurridoMs
        val entrada = entradaLista(accion, ahora = ahora)
        val resultado = PolicyEngine(RelojSimulado(ahora)).evaluar(entrada)
        val vencida = transcurridoMs >= 60000L
        val permite = accion == AccionPolitica.ALLOW ||
            accion == AccionPolitica.ALLOW_FOR && !vencida || accion == AccionPolitica.LOCK_FOR && vencida
        val motivo = if (permite) CodigoCp1.OK else if (accion == AccionPolitica.ALLOW_FOR)
            CodigoCp1.TIMER_EXPIRED else CodigoCp1.PARENT_LOCK
        val restante = if (accion == AccionPolitica.LOCK_FOR || accion == AccionPolitica.ALLOW_FOR)
            (maxOf(0L, 60000L - transcurridoMs) + 999L) / 1000L else null
        assertEquals(if (permite) ModoAcceso.ALLOWED else ModoAcceso.LOCKED, resultado.modoSolicitado)
        assertEquals(motivo, resultado.motivo)
        assertEquals(restante, resultado.politicaRestanteSegundos)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, transcurrido={1}ms")
        fun casos(): List<Array<Any>> = AccionPolitica.entries.flatMap { accion ->
            listOf(0L, 1L, 59999L, 60000L, 60001L, 604800000L).map { arrayOf<Any>(accion, it) }
        }
    }
}
