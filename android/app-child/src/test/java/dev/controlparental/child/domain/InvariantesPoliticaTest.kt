package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvariantesPoliticaTest {
    @Test
    fun `1024 combinaciones solo permiten cuando todas las condiciones obligatorias se cumplen`() {
        val base = entradaLista()
        val motor = PolicyEngine(RelojSimulado())
        repeat(1024) { mascara ->
            fun habilitado(bit: Int) = (mascara and (1 shl bit)) != 0
            val entrada = base.copy(esDeviceOwner = habilitado(0), aprovisionado = habilitado(1),
                vinculado = habilitado(2), integridadValida = habilitado(3), restriccionesVerificadas = habilitado(4),
                usuarioDesbloqueado = habilitado(5), authorizedBootId = if (habilitado(6)) BOOT_PRUEBA else null,
                politica = if (habilitado(7)) base.politica else null, red = RedObservada(habilitado(8)),
                canal = if (habilitado(9)) base.canal else CanalObservado())
            assertEquals("Combinación $mascara", mascara == 1023,
                motor.evaluar(entrada).modoSolicitado == ModoAcceso.ALLOWED)
        }
    }

    @Test
    fun `ninguna politica permite en otro arranque aun con permiso antiguo y PONG fresco`() {
        AccionPolitica.entries.forEach { accion ->
            val entrada = entradaLista(accion).copy(bootId = BOOT_NUEVO, authorizedBootId = BOOT_NUEVO)
            assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, PolicyEngine(RelojSimulado()).evaluar(entrada).motivo)
        }
    }

    @Test
    fun `la proxima transicion nunca es cero negativa ni mayor que el canal sano`() {
        AccionPolitica.entries.forEach { accion ->
            listOf(0L, 1L, 44000L, 44999L, 45000L, 59999L, 60000L).forEach { delta ->
                val resultado = PolicyEngine(RelojSimulado(100000 + delta)).evaluar(entradaLista(accion))
                resultado.reevaluarEnMs?.let {
                    assertTrue(it > 0 && it <= 604800000L)
                    if (delta < 45000L) assertTrue(it <= 45000L - delta)
                }
            }
        }
    }

    @Test
    fun `sin boot valido ni politica no se deduce un permiso de campos coincidentes`() {
        val entrada = entradaLista().copy(bootId = null, authorizedBootId = null, politica = null)
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, PolicyEngine(RelojSimulado()).evaluar(entrada).motivo)
    }
}
