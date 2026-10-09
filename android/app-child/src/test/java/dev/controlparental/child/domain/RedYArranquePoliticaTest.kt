package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Test

class RedYArranquePoliticaTest {
    @Test
    fun `perdida de red bloquea al cumplirse tres segundos sin reiniciar margen`() {
        val entrada = entradaLista().copy(red = RedObservada(false, 100000))
        listOf(0L, 2999L, 3000L, 3001L).forEach { delta ->
            val resultado = PolicyEngine(RelojSimulado(100000 + delta)).evaluar(entrada)
            assertEquals(if (delta < 3000) ModoAcceso.ALLOWED else ModoAcceso.LOCKED, resultado.modoSolicitado)
        }
    }

    @Test
    fun `canal caduca a los cuarenta y cinco segundos aunque haya internet`() {
        listOf(44999L, 45000L, 45001L).forEach { delta ->
            val resultado = PolicyEngine(RelojSimulado(100000 + delta)).evaluar(entradaLista())
            assertEquals(if (delta < 45000) ModoAcceso.ALLOWED else ModoAcceso.LOCKED, resultado.modoSolicitado)
        }
    }

    @Test
    fun `reinicio no recupera permiso por volver internet ni reinterpreta tiempo anterior`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR).copy(bootId = BOOT_NUEVO,
            canal = CanalObservado(true, 0))
        val resultado = PolicyEngine(RelojSimulado(0)).evaluar(entrada)
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, resultado.motivo)
        assertEquals(null, resultado.politicaRestanteSegundos)
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, PolicyEngine(RelojSimulado(0))
            .evaluar(entrada.copy(authorizedBootId = BOOT_NUEVO)).motivo)
    }

    @Test
    fun `LOCK_FOR vencido sigue bloqueado sin red o sin canal`() {
        val entrada = entradaLista(AccionPolitica.LOCK_FOR)
        val motor = PolicyEngine(RelojSimulado(160000))
        assertEquals(CodigoCp1.NETWORK_LOST, motor.evaluar(entrada.copy(red = RedObservada())).motivo)
        assertEquals(CodigoCp1.CHANNEL_EXPIRED, motor.evaluar(entrada).motivo)
        assertEquals(ModoAcceso.ALLOWED, motor.evaluar(entrada.copy(canal = CanalObservado(true, 160000))).modoSolicitado)
    }

    @Test
    fun `reconexion reevalua la politica sin reiniciar su duracion`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR)
        val reloj = RelojSimulado(120000)
        val motor = PolicyEngine(reloj)
        assertEquals(CodigoCp1.CHANNEL_EXPIRED, motor.evaluar(entrada.copy(canal = CanalObservado())).motivo)
        reloj.monotonicMs = 160000
        val resultado = motor.evaluar(entrada.copy(canal = CanalObservado(true, 160000)))
        assertEquals(CodigoCp1.TIMER_EXPIRED, resultado.motivo)
        assertEquals(0L, resultado.politicaRestanteSegundos)
    }

    @Test
    fun `la siguiente evaluacion usa el plazo mas cercano y nunca un bucle de cero`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR).copy(red = RedObservada(false, 100000))
        assertEquals(3000L, PolicyEngine(RelojSimulado()).evaluar(entrada).reevaluarEnMs)
        val vencida = entradaLista(AccionPolitica.ALLOW_FOR, ahora = 160000)
        assertEquals(45000L, PolicyEngine(RelojSimulado(160000)).evaluar(vencida).reevaluarEnMs)
    }
}
