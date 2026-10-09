package dev.controlparental.child.recovery

import dev.controlparental.child.data.*
import dev.controlparental.child.domain.EntradaEvaluacion
import dev.controlparental.child.domain.PolicyEngine
import dev.controlparental.protocol.HashesRecuperacion
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RecoveryControllerTest {
    private class Laboratorio {
        val archivo = ArchivoSimulado()
        val almacen = ChildStateStore(archivo)
        var ahora = 100000L
        var desbloqueado = true
        var bootId = estadoConOrden().bootContext!!.bootId
        // Códigos ficticios solo de JVM. El APK no contiene identidades/códigos inyectados.
        val codigos = PropositoRecuperacionLocal.entries.associateWith { ('A' + it.ordinal).toString().repeat(32) }
        init {
            val original = estadoConOrden()
            val par = original.vinculo!!.pairId
            val hashes = codigos.mapValues { (p, c) -> CodigoRecuperacion.hash(par, p, c) }
            almacen.actualizar { original.copy(vinculo = original.vinculo.copy(recoveryHashes = HashesRecuperacion(
                hashes.getValue(PropositoRecuperacionLocal.MAINTENANCE), hashes.getValue(PropositoRecuperacionLocal.RECOVER_PARENT),
                hashes.getValue(PropositoRecuperacionLocal.RETIRE)))) }
        }
        fun controlador() = RecoveryController(almacen, { ahora }, { ContextoRecuperacion(desbloqueado, bootId) })
        fun estado() = (almacen.cargar() as CargaEstado.Disponible).estado
        fun valido(c: RecoveryController, p: PropositoRecuperacionLocal = PropositoRecuperacionLocal.MAINTENANCE) = c.usarCodigo(p, codigos.getValue(p))
        fun invalido(c: RecoveryController) = c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido")
    }

    @Test
    fun `consumo durable precede sesion y conserva politica contador y autorizacion de boot`() {
        val l = Laboratorio()
        val anterior = l.estado()
        val c = l.controlador()
        assertEquals(ResultadoRecuperacion.AUTORIZADA, l.valido(c).resultado)
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in l.estado().recoveryConsumed)
        assertEquals(300000L, c.accesoActual()!!.restanteMs)
        assertEquals(anterior.policy, l.estado().policy)
        assertEquals(anterior.lastCommand, l.estado().lastCommand)
        assertEquals(anterior.bootContext, l.estado().bootContext)
        assertNotNull(c.mantenimientoActual())
    }

    @Test
    fun `repeticion y nuevo proceso no devuelven codigo ni sesion`() {
        val l = Laboratorio()
        val c = l.controlador()
        l.valido(c)
        val nuevo = l.controlador()
        assertNull(nuevo.accesoActual())
        assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, l.valido(nuevo).resultado)
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in l.estado().recoveryConsumed)
    }

    @Test
    fun `cada proposito solo concede su funcion y recuperacion no revoca padre anticipadamente`() {
        for (p in PropositoRecuperacionLocal.entries) {
            val l = Laboratorio()
            val anterior = l.estado()
            val c = l.controlador()
            assertEquals(ResultadoRecuperacion.AUTORIZADA, l.valido(c, p).resultado)
            assertEquals(p, c.accesoActual()!!.proposito)
            assertEquals(anterior.vinculo, l.estado().vinculo)
            if (p != PropositoRecuperacionLocal.MAINTENANCE) assertNull(c.mantenimientoActual())
            if (p == PropositoRecuperacionLocal.RECOVER_PARENT) assertEquals(EtapaAprovisionamiento.RECUPERACION_PADRE_PENDIENTE, l.estado().provisioningStage)
            if (p == PropositoRecuperacionLocal.RETIRE) assertEquals(EtapaAprovisionamiento.RETIRADA_PENDIENTE, l.estado().provisioningStage)
        }
    }

    @Test
    fun `sesion vence exactamente a cinco minutos y cancelar no restaura el codigo`() {
        val l = Laboratorio()
        val c = l.controlador()
        l.valido(c)
        l.ahora += 299999
        assertEquals(1L, c.accesoActual()!!.restanteMs)
        l.ahora++
        assertNull(c.accesoActual())
        assertNull(c.mantenimientoActual())
        c.cancelar()
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in l.estado().recoveryConsumed)
    }

    @Test
    fun `bloqueo de Android boot diferente y reloj atras invalidan acceso RAM`() {
        for (caso in 0..2) {
            val l = Laboratorio()
            val c = l.controlador()
            l.valido(c)
            when (caso) {
                0 -> l.desbloqueado = false
                1 -> l.bootId = "00000000-0000-4000-8000-000000000010"
                2 -> l.ahora--
            }
            assertNull(c.accesoActual())
            assertNull(c.mantenimientoActual())
        }
    }

    @Test
    fun `antes de desbloqueo o con boot distinto no acepta ni cuenta intentos`() {
        val l = Laboratorio()
        val c = l.controlador()
        l.desbloqueado = false
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, l.valido(c).resultado)
        l.desbloqueado = true
        l.bootId = "00000000-0000-4000-8000-000000000010"
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, l.valido(c).resultado)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
        assertEquals(0, l.estado().recoveryFailureCount)
    }

    @Test
    fun `cinco fallos imponen sesenta segundos sin gastar codigo durante espera`() {
        val l = Laboratorio()
        val c = l.controlador()
        repeat(4) { assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, l.invalido(c).resultado) }
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 60000), l.invalido(c))
        l.ahora += 59999
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 1), l.valido(c))
        assertEquals(5, l.estado().recoveryFailureCount)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
        l.ahora++
        assertEquals(ResultadoRecuperacion.AUTORIZADA, l.valido(c).resultado)
        assertFalse(l.estado().esperaRecuperacionPendiente)
    }

    @Test
    fun `nuevo proceso reimpone espera completa conservando intentos`() {
        val l = Laboratorio()
        val c = l.controlador()
        repeat(5) { l.invalido(c) }
        l.ahora += 30000
        val nuevo = l.controlador()
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 60000), l.valido(nuevo))
        assertEquals(5, l.estado().recoveryFailureCount)
        assertNull(nuevo.accesoActual())
    }

    @Test
    fun `diez fallos imponen cinco minutos y no se reinicia conteo en recreacion`() {
        val l = Laboratorio()
        val c = l.controlador()
        repeat(10) { numero ->
            val respuesta = l.invalido(c)
            if (numero >= 4) {
                assertEquals(if (numero >= 9) 300000L else 60000L, respuesta.esperaRestanteMs)
                l.ahora += respuesta.esperaRestanteMs
            }
        }
        assertEquals(10, l.estado().recoveryFailureCount)
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 300000), l.valido(l.controlador()))
    }

    @Test
    fun `reloj retrocedido reimpone espera o produce error seguro nunca acceso`() {
        val l = Laboratorio()
        val c = l.controlador()
        repeat(5) { l.invalido(c) }
        l.ahora--
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.valido(c).resultado)
        assertNull(c.accesoActual())
        assertTrue(l.estado().esperaRecuperacionPendiente)
    }

    @Test
    fun `fallo escritura o commit perdido no concede sesion`() {
        for (silencioso in listOf(false, true)) {
            val l = Laboratorio()
            val c = l.controlador()
            if (silencioso) l.archivo.omiteCommit = true else l.archivo.falloEscritura = true
            assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.valido(c).resultado)
            assertNull(c.accesoActual())
            assertTrue(l.estado().recoveryConsumed.isEmpty())
        }
    }

    @Test
    fun `archivo corrupto no se reemplaza ni habilita sesion o nuevo enrolamiento`() {
        val l = Laboratorio()
        l.archivo.bytes = "corrupto".toByteArray()
        val c = l.controlador()
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.valido(c).resultado)
        assertNull(c.accesoActual())
        assertArrayEquals("corrupto".toByteArray(), l.archivo.bytes)
    }

    @Test
    fun `pendiente de revinculacion o retirada no reabre acceso tras recrear controlador`() {
        for (p in listOf(PropositoRecuperacionLocal.RECOVER_PARENT, PropositoRecuperacionLocal.RETIRE)) {
            val l = Laboratorio()
            l.valido(l.controlador(), p)
            assertNull(l.controlador().accesoActual())
            assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, l.valido(l.controlador(), p).resultado)
            // RETIRE conserva su vía independiente al perder la sesión RECOVER_PARENT.
            if (p == PropositoRecuperacionLocal.RECOVER_PARENT) assertEquals(ResultadoRecuperacion.AUTORIZADA,
                l.valido(l.controlador(), PropositoRecuperacionLocal.RETIRE).resultado)
        }
    }

    @Test
    fun `dos controladores concurrentes no consumen el mismo codigo dos veces`() {
        val l = Laboratorio()
        val a = l.controlador()
        val b = l.controlador()
        val ejecutor = Executors.newFixedThreadPool(2)
        try {
            val respuestas = listOf(a, b).map { c -> ejecutor.submit<ResultadoRecuperacion> { l.valido(c).resultado } }
                .map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, respuestas.count { it == ResultadoRecuperacion.AUTORIZADA })
            assertEquals(1, respuestas.count { it == ResultadoRecuperacion.CODIGO_INVALIDO })
        } finally { ejecutor.shutdownNow() }
    }

    @Test
    fun `fallo de readback tras consumo confirmado no concede sesion ni devuelve el codigo`() {
        val l = Laboratorio()
        val puerto = object : ArchivoEstadoAtomico {
            override fun leerAcotado(maximoBytes: Int) = l.archivo.leerAcotado(maximoBytes)
            override fun escribirAtomico(bytes: ByteArray) {
                l.archivo.escribirAtomico(bytes)
                l.archivo.falloLectura = true
            }
        }
        val c = RecoveryController(ChildStateStore(puerto), { l.ahora }, { ContextoRecuperacion(true, l.bootId) })
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.valido(c).resultado)
        assertNull(c.accesoActual())
        l.archivo.falloLectura = false
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in l.estado().recoveryConsumed)
        assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, l.valido(l.controlador()).resultado)
    }

    @Test
    fun `espera persistida incoherente no se convierte en permiso`() {
        val l = Laboratorio()
        l.almacen.actualizar { it!!.copy(esperaRecuperacionPendiente = true, recoveryFailureCount = 0) }
        val c = l.controlador()
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.valido(c).resultado)
        assertNull(c.accesoActual())
        assertTrue(l.estado().esperaRecuperacionPendiente)
    }

    @Test
    fun `sesion se pierde si cambian hashes del vinculo o falla la lectura`() {
        for (fallo in listOf(false, true)) {
            val l = Laboratorio()
            val c = l.controlador()
            l.valido(c)
            if (fallo) l.archivo.falloLectura = true else {
                l.almacen.actualizar { anterior -> anterior!!.copy(vinculo = anterior.vinculo!!.copy(
                    recoveryHashes = anterior.vinculo.recoveryHashes.copy(RETIRE =
                        CodigoRecuperacion.hash(anterior.vinculo.pairId, PropositoRecuperacionLocal.RETIRE, "D".repeat(32))))) }
            }
            assertNull(c.accesoActual())
        }
    }

    @Test
    fun `reboot conserva consumo y reimpone espera completa sin liberar politica`() {
        val l = Laboratorio()
        val c = l.controlador()
        l.valido(c, PropositoRecuperacionLocal.RECOVER_PARENT)
        repeat(5) { l.invalido(c) }
        val politica = l.estado().policy
        l.bootId = "00000000-0000-4000-8000-000000000010"
        l.ahora = 0
        l.almacen.actualizar { it!!.copy(bootContext = ContextoArranque(13, l.bootId, null, 0)) }
        val nuevo = l.controlador()
        assertNull(nuevo.accesoActual())
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 60000), l.valido(nuevo))
        assertTrue(PropositoRecuperacionLocal.RECOVER_PARENT in l.estado().recoveryConsumed)
        assertEquals(politica, l.estado().policy)
        assertNull(l.estado().bootContext!!.authorizedBootId)
    }

    @Test
    fun `mantenimiento offline con boot no autorizado no concede ALLOWED ni autoriza arranque`() {
        val l = Laboratorio()
        l.almacen.actualizar { it!!.copy(bootContext = it.bootContext!!.copy(authorizedBootId = null)) }
        val c = l.controlador()
        assertEquals(ResultadoRecuperacion.AUTORIZADA, l.valido(c).resultado)
        val entrada = EntradaEvaluacion(true, true, true, true, true, true, l.bootId, null, l.estado().policy,
            mantenimiento = c.mantenimientoActual())
        assertEquals(ModoAcceso.MAINTENANCE, PolicyEngine { l.ahora }.evaluar(entrada).modoSolicitado)
        assertNull(l.estado().bootContext!!.authorizedBootId)
        l.ahora += 300000
        assertNull(c.mantenimientoActual())
        assertEquals(ModoAcceso.LOCKED, PolicyEngine { l.ahora }.evaluar(entrada.copy(mantenimiento = null)).modoSolicitado)
    }

    @Test
    fun `consultar espera actual no gasta intentos ni elimina obligacion persistida`() {
        val l = Laboratorio()
        val c = l.controlador()
        assertEquals(0L, c.esperaCodigoActual())
        repeat(5) { l.invalido(c) }
        l.ahora += 59000
        assertEquals(1000L, c.esperaCodigoActual())
        assertEquals(5, l.estado().recoveryFailureCount)
        l.ahora += 1000
        assertEquals(0L, c.esperaCodigoActual())
        assertTrue(l.estado().esperaRecuperacionPendiente)
        assertEquals(60000L, l.controlador().esperaCodigoActual())
    }

    @Test
    fun `consulta de espera con contexto o archivo invalido no finge estar disponible`() {
        val l = Laboratorio()
        val c = l.controlador()
        l.desbloqueado = false
        assertNull(c.esperaCodigoActual())
        l.desbloqueado = true
        l.archivo.falloLectura = true
        assertNull(c.esperaCodigoActual())
    }
}
