package dev.controlparental.child.prototype

import org.junit.Assert.*
import org.junit.Test

class PermisoTemporalTest {
    private var reloj = 100L
    private val plan = PermisoTemporal { reloj }
    private fun confirmar(minutos: Int = 1): ReferenciaTemporal = plan.preparar(minutos, 3).also(plan::confirmar)
    @Test fun `sin preseleccion solo minutos enteros elegidos`() {
        for (texto in listOf("", "0", "1441", "-1", "1.0", " 1", "１", "9999999999"))
            assertNull(PermisoTemporal.minutos(texto))
        assertEquals(1, PermisoTemporal.minutos("1")); assertEquals(1440, PermisoTemporal.minutos("1440"))
    }
    @Test fun `intervalos un minuto y un dia capturan deadline original`() {
        assertEquals(60_100L, confirmar().deadlineMs)
        assertEquals(86_400_100L, confirmar(1440).deadlineMs)
    }
    @Test fun `preparacion no concede permiso antes de confirmacion local`() {
        val ref = plan.preparar(1, 3)
        assertNull(plan.actual); assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(ref, 3))
        plan.confirmar(ref); assertSame(ref, plan.actual)
    }
    @Test fun `overflow y rango invalido no reemplazan permiso valido`() {
        val actual = confirmar()
        for (m in listOf(0, 1441)) {
            assertThrows(IllegalArgumentException::class.java) { plan.preparar(m, 3) }
        }
        reloj = Long.MAX_VALUE - 1
        assertThrows(ArithmeticException::class.java) { plan.preparar(1, 3) }
        assertSame(actual, plan.actual)
    }
    @Test fun `alarma temprana no bloquea ni renueva y vencida se reclama una vez`() {
        val ref = confirmar()
        reloj += 59_999
        assertEquals(EntregaTemporal.TEMPRANA, plan.entregar(ref, 3))
        reloj++
        assertEquals(EntregaTemporal.VENCIDA, plan.entregar(ref, 3))
        assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(ref, 3))
        assertEquals(60_100L, ref.deadlineMs)
    }
    @Test fun `entrega atrasada incluye tiempo apagado sin hora civil ni actividad`() {
        val ref = confirmar()
        reloj += 120_000
        assertEquals(EntregaTemporal.VENCIDA, plan.entregar(ref, 3))
        assertEquals(60_100L, ref.deadlineMs)
    }
    @Test fun `sustitucion invalida callback anterior y conserva deadline elegido nuevo`() {
        val anterior = confirmar()
        reloj += 10_000
        val nuevo = confirmar(2)
        reloj = anterior.deadlineMs
        assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(anterior, 3))
        assertEquals(EntregaTemporal.TEMPRANA, plan.entregar(nuevo, 3))
        assertEquals(130_100L, nuevo.deadlineMs)
    }
    @Test fun `cancelacion vieja por referencia no tiene autoridad sobre permiso nuevo`() {
        val anterior = confirmar(); val nuevo = confirmar()
        assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(anterior, 3))
        assertSame(nuevo, plan.actual)
        val copia = ReferenciaTemporal(nuevo.generacion, nuevo.boot, nuevo.aceptadoMs, nuevo.deadlineMs)
        assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(copia, 3))
    }
    @Test fun `salida de prueba invalida alarma y propuesta pendiente sin relock`() {
        val ref = confirmar(); val propuesta = plan.preparar(2, 3)
        plan.cancelar(); reloj += 120_000
        assertEquals(EntregaTemporal.OBSOLETA, plan.entregar(ref, 3))
        assertThrows(IllegalArgumentException::class.java) { plan.confirmar(propuesta) }
        assertNull(plan.actual)
    }
    @Test fun `reloj regresivo boot distinto y confirmacion demasiado tarde no conceden bloqueo`() {
        val ref = confirmar()
        reloj--
        assertEquals(EntregaTemporal.RELOJ_INVALIDO, plan.entregar(ref, 3))
        assertEquals(EntregaTemporal.BOOT_DISTINTO, plan.entregar(ref, 4))
        assertThrows(IllegalArgumentException::class.java) { plan.preparar(1, 3) }
        reloj = ref.deadlineMs
        val otro = plan.preparar(1, 3); reloj = otro.deadlineMs
        assertThrows(IllegalArgumentException::class.java) { plan.confirmar(otro) }
    }
    @Test fun `fondo o cancelacion admin no revocan permiso comprometido ni autorizan otro`() {
        val tickets = TicketsPrototipo { reloj }
        tickets.reanudar()
        val auth = tickets.solicitar(FinAutenticacion.PERMITIR)!!
        assertEquals(FinAutenticacion.PERMITIR, tickets.completar(auth, true))
        val ref = confirmar()
        tickets.fondo(); tickets.reanudar(); tickets.cancelar()
        assertNull(tickets.completar(auth, true))
        reloj = ref.deadlineMs
        assertEquals(EntregaTemporal.VENCIDA, plan.entregar(ref, 3))
    }
    @Test fun `recreacion sin plan RAM no inventa permiso o expiracion anterior`() {
        val ref = confirmar()
        val nuevo = PermisoTemporal { reloj }
        assertEquals(EntregaTemporal.OBSOLETA, nuevo.entregar(ref, 3))
        assertNull(nuevo.actual)
    }
}
