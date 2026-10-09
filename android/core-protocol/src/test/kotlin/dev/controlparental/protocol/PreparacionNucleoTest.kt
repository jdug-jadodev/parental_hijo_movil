package dev.controlparental.protocol

import dev.controlparental.protocol.pruebas.SimuladorDeMensajes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparacionNucleoTest {
    @Test
    fun `el nucleo identifica el contrato congelado`() {
        assertEquals("CP/1", InformacionNucleo.CONTRATO)
    }

    @Test
    fun `el simulador entrega el ejemplo original sin modificarlo`() {
        val simulador = SimuladorDeMensajes()
        simulador.encolarEjemplo("04_lock_for.signed.json")
        val original = javaClass.classLoader
            .getResourceAsStream("04_lock_for.signed.json")!!.use { it.readBytes() }

        assertTrue(original.contentEquals(simulador.recibir()))
        assertNull(simulador.recibir())
    }

    @Test
    fun `el simulador respeta el orden de entrega`() {
        val simulador = SimuladorDeMensajes()
        simulador.encolarEjemplo("12_challenge.frame.json")
        simulador.encolarEjemplo("15_pong.frame.json")

        assertTrue(simulador.recibir()!!.decodeToString().contains("CHALLENGE"))
        assertTrue(simulador.recibir()!!.decodeToString().contains("PONG"))
    }

    @Test
    fun `la desconexion no conserva una cola para reconectar`() {
        val simulador = SimuladorDeMensajes()
        simulador.encolarEjemplo("04_lock_for.signed.json")
        simulador.desconectar()

        assertNull(simulador.recibir())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `el simulador rechaza rutas fuera de los ejemplos`() {
        SimuladorDeMensajes().encolarEjemplo("../secreto.json")
    }

    @Test(expected = IllegalStateException::class)
    fun `un ejemplo inexistente produce un error visible`() {
        SimuladorDeMensajes().encolarEjemplo("inexistente.json")
    }
}
