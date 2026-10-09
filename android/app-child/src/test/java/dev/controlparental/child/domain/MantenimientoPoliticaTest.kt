package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Test

class MantenimientoPoliticaTest {
    @Test
    fun `mantenimiento termina a los cinco minutos sin liberar aplicaciones`() {
        val entrada = entradaLista(AccionPolitica.LOCK, inicio = 0).copy(authorizedBootId = null,
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 0))
        listOf(0L, 299999L, 300000L, 300001L).forEach { ahora ->
            val resultado = PolicyEngine(RelojSimulado(ahora)).evaluar(entrada)
            assertEquals(if (ahora < 300000) ModoAcceso.MAINTENANCE else ModoAcceso.LOCKED, resultado.modoSolicitado)
            if (ahora >= 300000) assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, resultado.motivo)
        }
    }

    @Test
    fun `cuenta parental continua durante mantenimiento y nunca se mezcla con sus cinco minutos`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR, inicio = 0).copy(
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 0), canal = CanalObservado(true, 60000))
        val resultado = PolicyEngine(RelojSimulado(60000)).evaluar(entrada)
        assertEquals(ModoAcceso.MAINTENANCE, resultado.modoSolicitado)
        assertEquals(0L, resultado.politicaRestanteSegundos)
        assertEquals(240L, resultado.mantenimientoRestanteSegundos)
        assertEquals(240000L, resultado.reevaluarEnMs)
        val terminado = PolicyEngine(RelojSimulado(300000)).evaluar(entrada.copy(canal = CanalObservado(true, 300000)))
        assertEquals(CodigoCp1.TIMER_EXPIRED, terminado.motivo)
    }

    @Test
    fun `reinicio cancela mantenimiento aunque el reloj monotono vuelva a cero`() {
        val entrada = entradaLista().copy(bootId = BOOT_NUEVO,
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 100000))
        val resultado = PolicyEngine(RelojSimulado(0)).evaluar(entrada)
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, resultado.motivo)
        assertEquals(null, resultado.mantenimientoRestanteSegundos)
    }

    @Test
    fun `mantenimiento con inicio imposible queda en error seguro`() {
        listOf(-1L, 100001L).forEach { inicio ->
            assertEquals(CodigoCp1.STATE_CORRUPT, PolicyEngine(RelojSimulado()).evaluar(entradaLista().copy(
                mantenimiento = SesionMantenimiento(BOOT_PRUEBA, inicio))).motivo)
        }
    }

    @Test
    fun `LOCK sin autorizacion previa no concede autorizacion de arranque`() {
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, PolicyEngine(RelojSimulado()).evaluar(
            entradaLista(AccionPolitica.LOCK).copy(authorizedBootId = null)).motivo)
    }

    @Test
    fun `volver la red o terminar mantenimiento no elimina un LOCK del padre`() {
        val entrada = entradaLista(AccionPolitica.LOCK, inicio = 0, ahora = 300000)
            .copy(mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 0))
        assertEquals(CodigoCp1.PARENT_LOCK, PolicyEngine(RelojSimulado(300000)).evaluar(entrada).motivo)
    }

    @Test
    fun `margen de red no aplaza el vencimiento de ALLOW_FOR`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR, ahora = 160000).copy(red = RedObservada(false, 159999))
        assertEquals(CodigoCp1.TIMER_EXPIRED, PolicyEngine(RelojSimulado(160000)).evaluar(entrada).motivo)
    }
}
