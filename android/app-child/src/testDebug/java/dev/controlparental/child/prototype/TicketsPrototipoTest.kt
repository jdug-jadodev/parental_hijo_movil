package dev.controlparental.child.prototype

import org.junit.Assert.*
import org.junit.Test

class TicketsPrototipoTest {
    @Test fun `diagnosticos distinguen falta caducidad fondo y cancelacion`() {
        tickets.reanudar()
        assertEquals(Causa.TICKET_FALTA, tickets.comprobar())
        val t = tickets.solicitar(FinAutenticacion.COMPROBAR)!!
        tickets.completar(t, true)
        assertEquals(Causa.NINGUNA, tickets.comprobar())
        ahora += 120_000
        assertEquals(Causa.TICKET_CADUCADO, tickets.comprobar())
        tickets.fondo(); tickets.reanudar()
        assertEquals(Causa.FONDO, tickets.comprobar())
        tickets.cancelar()
        assertEquals(Causa.CANCELADO, tickets.comprobar())
    }
    @Test fun `validar gates sin consumir preserva credencial si ADB falta`() {
        tickets.reanudar()
        tickets.completar(tickets.solicitar(FinAutenticacion.COMPROBAR)!!, true)
        assertEquals(Causa.NINGUNA, tickets.comprobar())
        assertEquals(Causa.NINGUNA, tickets.comprobar())
        assertTrue(tickets.consumirComprobacion())
        assertEquals(Causa.TICKET_FALTA, tickets.comprobar())
    }
    private var ahora = 100L
    private val tickets = TicketsPrototipo { ahora }
    @Test fun `sin primer plano ni credencial no autoriza`() {
        assertNull(tickets.solicitar(FinAutenticacion.SALIR))
        tickets.reanudar()
        assertNull(tickets.completar(tickets.solicitar(FinAutenticacion.SALIR)!!, false))
    }
    @Test fun `comprobacion se consume una sola vez`() {
        tickets.reanudar()
        val t = tickets.solicitar(FinAutenticacion.COMPROBAR)!!
        assertEquals(FinAutenticacion.COMPROBAR, tickets.completar(t, true))
        assertTrue(tickets.consumirComprobacion())
        assertFalse(tickets.consumirComprobacion())
    }
    @Test fun `salida autenticada requiere ticket actual de salida`() {
        tickets.reanudar()
        val t = tickets.solicitar(FinAutenticacion.SALIR)!!
        assertEquals(FinAutenticacion.SALIR, tickets.completar(t, true))
        assertNull(tickets.completar(t, true))
    }
    @Test fun `fondo cancelacion reentrada y recreacion no restauran ticket`() {
        tickets.reanudar()
        val t = tickets.solicitar(FinAutenticacion.SALIR)!!
        tickets.fondo(); tickets.reanudar()
        assertNull(tickets.completar(t, true))
        val otro = tickets.solicitar(FinAutenticacion.SALIR)!!
        tickets.cancelar()
        assertNull(tickets.completar(otro, true))
        val nuevo = TicketsPrototipo { ahora }
        nuevo.reanudar()
        assertNull(nuevo.completar(t, true))
    }
    @Test fun `caducado regresivo y negativo se rechazan`() {
        tickets.reanudar()
        val t = tickets.solicitar(FinAutenticacion.SALIR)!!
        ahora += 120_000
        assertNull(tickets.completar(t, true))
        val otro = tickets.solicitar(FinAutenticacion.SALIR)!!
        ahora--
        assertNull(tickets.completar(otro, true))
        ahora = -1
        assertNull(tickets.solicitar(FinAutenticacion.SALIR))
    }
    @Test fun `callback viejo no consume ticket nuevo ni permite suplantarlo`() {
        tickets.reanudar()
        val viejo = tickets.solicitar(FinAutenticacion.SALIR)!!
        val actual = tickets.solicitar(FinAutenticacion.SALIR)!!
        assertNull(tickets.completar(viejo, true))
        assertNull(tickets.completar(TicketPrototipo(actual.id, actual.fin, actual.inicio), true))
        assertEquals(FinAutenticacion.SALIR, tickets.completar(actual, true))
    }
}
