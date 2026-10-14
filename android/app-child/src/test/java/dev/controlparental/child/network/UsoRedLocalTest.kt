package dev.controlparental.child.network

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import org.junit.Assert.*
import org.junit.Test

class UsoRedLocalTest {
    @Test fun `corrupcion durante uso no se repara al autenticar otro permiso`() {
        normal(); f.bytes = byteArrayOf(0)
        assertFalse(m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true }); assertFalse(m.permiteUso)
        assertArrayEquals(byteArrayOf(0), f.bytes)
    }
    @Test fun `fondo adulto o propuesta obsoleta no cancela uso ya aceptado`() {
        normal(); val ref = m.referencia
        assertFalse(m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { false })
        assertFalse(m.bloquear { false }); assertFalse(m.cerrar { false })
        assertTrue(m.permiteUso); assertEquals(ref, m.referencia)
    }
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null; var falla = false
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { if (!falla) this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private var t = 100L; private var boot = 5; private var revision = 0L
    private val marca = ArchivoPrueba(); private val f = ArchivoPrueba()
    private val m = UsoRedLocal(marca, f, { t }, { boot })
    private fun iniciar() { assertTrue(m.preparar { true }); m.iniciarObservador(1) }
    private fun red(v: ValidacionRed) = m.observar(v, 1, ++revision)
    private fun normal() { iniciar(); red(ValidacionRed.VALIDADA); assertTrue(m.autorizar(UsoLocal.NORMAL) { true }) }
    @Test fun `startup unknown no normal ni default abierto`() {
        iniciar(); assertFalse(m.autorizar(UsoLocal.NORMAL) { true }); assertFalse(m.permiteUso)
    }
    @Test fun `frontera 9999 permite 10000 revoca durable`() {
        normal(); red(ValidacionRed.NO_VALIDADA); val ref = m.referencia!!; val d = m.deadline!!
        t = d - 1; m.comprobarDeadline(ref); assertTrue(m.permiteUso)
        t++; m.comprobarDeadline(ref); assertEquals(UsoLocal.BLOQUEADO, m.uso)
        assertEquals(MotivoRed.RED_10S, CodecObligacionRed.decodificar(f.bytes!!).motivo)
    }
    @Test fun `reconecta antes cancela gracia sin renovar referencia`() {
        normal(); val ref = m.referencia; red(ValidacionRed.NO_VALIDADA); t += 9999
        red(ValidacionRed.VALIDADA); assertNull(m.deadline); assertEquals(ref, m.referencia)
        t += 50_000; m.comprobarDeadline(ref!!); assertTrue(m.permiteUso)
    }
    @Test fun `reconecta exactamente10s no abre ni borra latch`() {
        normal(); red(ValidacionRed.NO_VALIDADA); t += 10000; red(ValidacionRed.VALIDADA)
        assertFalse(m.permiteUso); assertEquals(UsoLocal.BLOQUEADO, m.uso)
        red(ValidacionRed.VALIDADA); assertFalse(m.permiteUso)
        assertTrue(m.autorizar(UsoLocal.NORMAL) { true }); assertTrue(m.permiteUso)
    }
    @Test fun `duplicados negativos no reinician deadline original`() {
        normal(); red(ValidacionRed.NO_VALIDADA); val d = m.deadline
        repeat(9) { t += 1000; red(ValidacionRed.NO_VALIDADA); assertEquals(d, m.deadline) }
        t += 1000; red(ValidacionRed.DESCONOCIDA); assertFalse(m.permiteUso)
    }
    @Test fun `callback con revision vieja no valida red ni cancela grace`() {
        normal(); red(ValidacionRed.NO_VALIDADA); val d = m.deadline
        m.observar(ValidacionRed.VALIDADA, 1, revision - 1); assertEquals(d, m.deadline)
    }
    @Test fun `callback observador anterior tras cerrar no concede uso`() {
        normal(); m.cerrar { true }; m.observar(ValidacionRed.VALIDADA, 1, ++revision)
        assertFalse(m.permiteUso); assertNull(m.deadline)
    }
    @Test fun `defaultswap negativo conserva grace y actual validada cancela antes`() {
        normal(); red(ValidacionRed.NO_VALIDADA); t += 5000
        red(ValidacionRed.DESCONOCIDA); assertEquals(10100L, m.deadline)
        red(ValidacionRed.VALIDADA); assertNull(m.deadline); assertTrue(m.permiteUso)
    }
    @Test fun `excepcion requiere codigo pero no requiere red ni expireautomatico`() {
        iniciar(); assertTrue(m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true })
        red(ValidacionRed.NO_VALIDADA); t += 1_000_000; m.comprobarDeadline(m.referencia!!)
        assertEquals(UsoLocal.EXCEPCION_OFFLINE, m.uso); assertNull(m.deadline)
        red(ValidacionRed.VALIDADA); assertEquals(UsoLocal.EXCEPCION_OFFLINE, m.uso)
    }
    @Test fun `pregunta y autoridad cancelada no conceden excepcion`() {
        iniciar(); assertFalse(m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { false }); assertFalse(m.permiteUso)
    }
    @Test fun `bloquear adulto revoca normal y excepcion`() {
        normal(); assertTrue(m.bloquear { true }); assertFalse(m.permiteUso)
        m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true }; m.bloquear { true }; assertFalse(m.permiteUso)
    }
    @Test fun `reloj regresivo y bootnuevo invalidan ambas`() {
        normal(); t = 0; red(ValidacionRed.VALIDADA); assertFalse(m.permiteUso)
        t = 1000; m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true }; boot++
        m.comprobarDeadline(m.referencia!!); assertFalse(m.permiteUso)
    }
    @Test fun `reiniciar proceso no restaura permiso normal ni offline`() {
        normal(); val nuevo = UsoRedLocal(marca, f, { t }, { boot })
        assertTrue(nuevo.cargar()); assertFalse(nuevo.permiteUso)
        m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true }
        assertTrue(nuevo.cargar()); assertFalse(nuevo.permiteUso)
    }
    @Test fun `fallo readback no mantiene permitido ni permite recuperar por corrupcion`() {
        normal(); f.falla = true; assertFalse(m.bloquear { true }); assertFalse(m.permiteUso)
        f.bytes = byteArrayOf(0); assertFalse(m.cargar()); assertEquals(UsoLocal.ERROR, m.uso)
        assertFalse(m.preparar { true })
    }
    @Test fun `callback readback viejo no revoca nueva autorizacion`() {
        normal(); val viejo = m.referencia!!; m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true }
        m.falloEnforcement(viejo); assertEquals(UsoLocal.EXCEPCION_OFFLINE, m.uso)
    }
    @Test fun `finalizar autenticado invalida ref deadline y elimina solo tras restore`() {
        normal(); red(ValidacionRed.NO_VALIDADA); val ref = m.referencia!!
        m.cerrar { true }; t += 20000; m.comprobarDeadline(ref); assertFalse(m.permiteUso)
        assertTrue(m.finalizar(true)); assertNull(f.bytes); assertNull(marca.bytes)
    }
    @Test fun `codec estricto version checksum limites`() {
        normal(); val b = f.bytes!!; assertEquals(b.size, 52)
        for (v in listOf(b.copyOf().also { it[0] = 7 }, b.copyOf(51), ByteArray(100))) {
            try { CodecObligacionRed.decodificar(v); fail() } catch (_: Exception) {}
        }
    }
}
