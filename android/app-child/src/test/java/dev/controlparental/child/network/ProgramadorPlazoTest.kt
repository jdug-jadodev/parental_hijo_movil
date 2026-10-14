package dev.controlparental.child.network

import org.junit.Assert.*
import org.junit.Test

class ProgramadorPlazoTest {
    private class PuertoPrueba : PuertoPlazo {
        val entregas = mutableListOf<() -> Unit>(); val deadlines = mutableListOf<Long>()
        var cancelaciones = 0; var falla = false
        override fun programar(deadline: Long, entregar: () -> Unit) {
            if (falla) error("fallo simulado")
            deadlines += deadline; entregas += entregar
        }
        override fun cancelar() { cancelaciones++ }
    }
    private val puerto = PuertoPrueba(); private val vencidos = mutableListOf<PlanPlazo>()
    private val p = ProgramadorPlazo(puerto) { vencidos += it }
    private fun plan(g: Long = 1, d: Long = 60100) = PlanPlazo(ReferenciaRed(g, 5), d)
    @Test fun `duplicados no rearman listener infinitamente`() {
        repeat(100) { p.actualizar(plan()) }; assertEquals(listOf(60100L), puerto.deadlines)
    }
    @Test fun `callback viejo no afecta autorizacion nueva`() {
        p.actualizar(plan()); p.actualizar(plan(2, 90100)); puerto.entregas[0]()
        assertTrue(vencidos.isEmpty()); assertEquals(plan(2, 90100), p.plan)
        puerto.entregas[1](); assertEquals(listOf(plan(2, 90100)), vencidos)
    }
    @Test fun `nuevo plazo de gracia cancela listener original sin cambiar ref`() {
        p.actualizar(plan()); p.actualizar(plan(d = 10100)); puerto.entregas[0]()
        assertTrue(vencidos.isEmpty()); puerto.entregas[1](); assertEquals(listOf(plan(d = 10100)), vencidos)
    }
    @Test fun `reconexion elimina gracia y recupera deadline original de tiempo`() {
        p.actualizar(plan()); p.actualizar(plan(d = 10100)); p.actualizar(plan())
        puerto.entregas[1](); assertTrue(vencidos.isEmpty()); assertEquals(plan(), p.plan)
        puerto.entregas[2](); assertEquals(listOf(plan()), vencidos)
    }
    @Test fun `libre bloquear y finalizar cancelan listener y callbacks entregados`() {
        p.actualizar(plan()); p.actualizar(null); puerto.entregas[0](); assertTrue(vencidos.isEmpty())
        p.actualizar(plan(2)); p.cancelar(); puerto.entregas[1](); assertNull(p.plan); assertTrue(vencidos.isEmpty())
    }
    @Test fun `entrega duplicada se procesa una sola vez`() {
        p.actualizar(plan()); repeat(10) { puerto.entregas[0]() }; assertEquals(1, vencidos.size)
    }
    @Test fun `fallo de scheduler invalida plan y callback viejo`() {
        p.actualizar(plan()); puerto.falla = true
        try { p.actualizar(plan(2)); fail() } catch (_: IllegalStateException) {}
        assertNull(p.plan); puerto.entregas[0](); assertTrue(vencidos.isEmpty())
    }
    @Test fun `entrega temprana puede reevaluar y reprogramar solo deadlineoriginal`() {
        var n = 100L; var bloqueado = false
        lateinit var programador: ProgramadorPlazo
        programador = ProgramadorPlazo(puerto) { plan ->
            if (n >= plan.deadline) bloqueado = true else programador.actualizar(plan)
        }
        programador.actualizar(plan()); n = 60100 - 1; puerto.entregas[0]()
        assertFalse(bloqueado); assertEquals(listOf(60100L, 60100L), puerto.deadlines)
        n = 100000; puerto.entregas[1](); assertTrue(bloqueado); assertNull(programador.plan)
    }
}
