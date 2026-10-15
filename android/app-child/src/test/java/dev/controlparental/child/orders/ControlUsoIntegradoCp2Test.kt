package dev.controlparental.child.orders

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import dev.controlparental.child.network.*
import dev.controlparental.protocol.cp2.AccionOrdenCp2
import org.junit.Assert.*
import org.junit.Test

/** Componentes productivos reales, reloj/archivos falsos. No simula ni acredita Device Owner Android. */
class ControlUsoIntegradoCp2Test {
    private class Archivo : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null
        override fun leer() = bytes?.copyOf()
        override fun escribir(bytes: ByteArray) { this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private class Disco : ArchivosControl {
        var registro: ByteArray? = null; var pendiente: ByteArray? = null
        override fun leer() = registro?.copyOf()
        override fun reserva() = pendiente?.copyOf()
        override fun iniciar(bytes: ByteArray) { registro = bytes.copyOf() }
        override fun reservar(bytes: ByteArray) { pendiente = bytes.copyOf() }
        override fun escribir(bytes: ByteArray) { registro = bytes.copyOf() }
        override fun terminarReserva() { pendiente = null }
    }
    @Test fun normalRevocaDiezSegundosYReconectarNoReabreControlNiUso() {
        var t = 0L
        val m = UsoRedLocal(Archivo(), Archivo(), { t }, { 1 })
        assertTrue(m.preparar { true }); m.iniciarObservador(1); m.observar(ValidacionRed.VALIDADA, 1, 1)
        val w = EscritorControlCp2(Disco(), { t }, { ParejaOrdenCp2("p", "a", "b", "r", 1) }, { fail("IO inesperado") })
        assertTrue(w.abrir { true }); val d = w.local(AccionOrdenCp2.ALLOW, null, false)!!
        assertTrue(m.autorizar(UsoLocal.NORMAL) { w.esRevision(d.revision) })
        t = 100; m.observar(ValidacionRed.NO_VALIDADA, 1, 2)
        t = 10099; m.comprobarDeadline(m.referencia!!); assertTrue(m.permiteUso)
        t = 10100; m.observar(ValidacionRed.VALIDADA, 1, 3)
        assertFalse(m.permiteUso); assertTrue(w.revocar()); assertNull(w.actual().decision)
        val rev = w.actual().revision
        m.observar(ValidacionRed.VALIDADA, 1, 4); assertFalse(m.permiteUso); assertEquals(rev, w.actual().revision)
    }
    @Test fun plazoDurableNoSeEstiraPorLatenciaYOfflineSoloDecisionLocal() {
        var t = 100L
        val m = UsoRedLocal(Archivo(), Archivo(), { t }, { 1 })
        m.preparar { true }; m.iniciarObservador(1)
        val w = EscritorControlCp2(Disco(), { t }, { ParejaOrdenCp2("p", "a", "b", "r", 1) }, { fail("IO inesperado") })
        assertTrue(w.abrir { true }); val d = w.local(AccionOrdenCp2.ALLOW_FOR, 60, true)!!
        t = 50100
        assertTrue(m.autorizar(UsoLocal.EXCEPCION_OFFLINE, MinutosUso.desdeTexto("1"), d.deadline) { w.esRevision(d.revision) })
        assertEquals(60100L, m.vencimientoUso)
        m.observar(ValidacionRed.NO_VALIDADA, 1, 1); assertNull(m.deadline)
        t = 60100; m.comprobarDeadline(m.referencia!!); assertFalse(m.permiteUso)
        w.revocar(); assertNull(w.actual().decision)
    }
    @Test fun limiteYaVencidoNoAutorizaAunqueDuracionRelativaSeaNueva() {
        var t = 100L
        val m = UsoRedLocal(Archivo(), Archivo(), { t }, { 1 }); m.preparar { true }; m.iniciarObservador(1)
        t = 60100
        assertFalse(m.autorizar(UsoLocal.EXCEPCION_OFFLINE, MinutosUso.desdeTexto("1"), 60100) { true })
        assertFalse(m.permiteUso)
    }
}
