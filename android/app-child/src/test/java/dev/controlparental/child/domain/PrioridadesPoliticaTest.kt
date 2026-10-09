package dev.controlparental.child.domain

import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Test

class PrioridadesPoliticaTest {
    private val reloj = RelojSimulado()
    private val motor = PolicyEngine(reloj)

    @Test
    fun `los valores iniciales nunca permiten aplicaciones`() {
        assertEquals(ModoAcceso.SETUP, motor.evaluar(EntradaEvaluacion()).modoSolicitado)
    }

    @Test
    fun `configuracion precede a errores y mantenimiento`() {
        val entrada = entradaLista().copy(esDeviceOwner = false, integridadValida = false,
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 100000))
        assertEquals(CodigoCp1.NOT_DEVICE_OWNER, motor.evaluar(entrada).motivo)
        assertEquals(CodigoCp1.NOT_PROVISIONED, motor.evaluar(entrada.copy(esDeviceOwner = true, vinculado = false)).motivo)
    }

    @Test
    fun `integridad y enforcement fallidos preceden a PIN y mantenimiento`() {
        val entrada = entradaLista().copy(usuarioDesbloqueado = false, integridadValida = false,
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 100000))
        assertEquals(CodigoCp1.STATE_CORRUPT, motor.evaluar(entrada).motivo)
        assertEquals(CodigoCp1.ENFORCEMENT_ERROR,
            motor.evaluar(entrada.copy(integridadValida = true, restriccionesVerificadas = false)).motivo)
    }

    @Test
    fun `PIN de Android precede a mantenimiento y autorizacion parental`() {
        val entrada = entradaLista().copy(usuarioDesbloqueado = false,
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 100000))
        assertEquals(CodigoCp1.BEFORE_USER_UNLOCK, motor.evaluar(entrada).motivo)
    }

    @Test
    fun `mantenimiento permite solo herramientas incluso sin autorizacion de arranque ni red`() {
        val entrada = entradaLista().copy(authorizedBootId = null, red = RedObservada(), canal = CanalObservado(),
            mantenimiento = SesionMantenimiento(BOOT_PRUEBA, 100000))
        val resultado = motor.evaluar(entrada)
        assertEquals(ModoAcceso.MAINTENANCE, resultado.modoSolicitado)
        assertEquals(CodigoCp1.MAINTENANCE_ACTIVE, resultado.motivo)
        assertEquals(300L, resultado.mantenimientoRestanteSegundos)
        assertEquals(null, resultado.politicaRestanteSegundos)
    }

    @Test
    fun `autorizacion de arranque precede a red canal y politica`() {
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, motor.evaluar(entradaLista().copy(
            authorizedBootId = null, red = RedObservada(), canal = CanalObservado())).motivo)
    }

    @Test
    fun `red precede a canal y un icono WiFi no es validacion`() {
        assertEquals(CodigoCp1.NETWORK_LOST, motor.evaluar(entradaLista().copy(
            red = RedObservada(), canal = CanalObservado())).motivo)
    }

    @Test
    fun `socket sin autenticacion o sin PONG no permite uso`() {
        listOf(CanalObservado(), CanalObservado(true, null), CanalObservado(false, 100000)).forEach {
            assertEquals(CodigoCp1.CHANNEL_EXPIRED, motor.evaluar(entradaLista().copy(canal = it)).motivo)
        }
    }

    @Test
    fun `leer reloj una vez mantiene una instantanea coherente`() {
        motor.evaluar(entradaLista())
        assertEquals(1, reloj.lecturas)
    }
}
