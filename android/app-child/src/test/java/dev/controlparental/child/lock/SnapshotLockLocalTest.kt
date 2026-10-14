package dev.controlparental.child.lock

import org.junit.Assert.*
import org.junit.Test

class SnapshotLockLocalTest {
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private class PoliticasPrueba : PoliticasLockLocal {
        override val paquete = "dev.hijo"; override val featuresPropios = 48
        var lista = setOf("dev.previo"); var flags = 0; var estado = ModoLocal.NONE; var writes = 0
        override fun validar() {}
        override fun leerPaquetes() = lista
        override fun leerFeatures() = flags
        override fun paquetes(valor: Set<String>) { lista = valor; writes++ }
        override fun features(valor: Int) { flags = valor; writes++ }
        override fun modo() = estado
        override fun iniciar() { estado = ModoLocal.LOCKED }
    }
    private val a = ArchivoPrueba(); private val p = PoliticasPrueba(); private val adaptador = AdaptadorLockLocal(a, p)
    @Test fun `snapshot previo aplica solo propio restore y borra tras readbackNONE`() {
        adaptador.preparar(); assertEquals(0, p.writes)
        adaptador.aplicar(); adaptador.solicitarInicio(); adaptador.restaurar()
        assertFalse(adaptador.restaurado()); assertNotNull(a.bytes)
        p.estado = ModoLocal.NONE; assertTrue(adaptador.restaurado()); assertNull(a.bytes)
        assertEquals(setOf("dev.previo"), p.lista); assertEquals(0, p.flags)
    }
    @Test fun `baseline propio incompatible no sobrescribe politicas`() {
        p.lista = setOf(p.paquete)
        try { adaptador.preparar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.BASELINE, e.causa) }
        assertNull(a.bytes); assertEquals(0, p.writes)
    }
    @Test fun `snapshot corrupto ausente o version desconocida no defaults ni writes`() {
        for (bytes in listOf<ByteArray?>(null, byteArrayOf(1), ByteArray(44))) {
            a.bytes = bytes
            try { adaptador.restaurar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.SNAPSHOT, e.causa) }
        }
        assertEquals(0, p.writes)
    }
    @Test fun `politicas ajenas no se pisan al aplicar o restaurar`() {
        adaptador.preparar(); p.lista = setOf("dev.ajeno")
        try { adaptador.aplicar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        try { adaptador.restaurar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        assertEquals(0, p.writes); assertNotNull(a.bytes)
    }
    @Test fun `features ajenos no restauran ni borran snapshot`() {
        adaptador.preparar(); adaptador.aplicar(); p.flags = 12345; val antes = p.writes
        try { adaptador.restaurar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        assertEquals(antes, p.writes); assertNotNull(a.bytes)
    }
    @Test fun `codec acepta baseline vacio y detecta checksum cambio y exceso`() {
        val s = SnapshotLockLocal(emptySet(), 0); val bytes = CodecSnapshotLocal.codificar(s)
        assertEquals(s, CodecSnapshotLocal.decodificar(bytes))
        bytes[0] = 9
        for (b in listOf(bytes, ByteArray(40001))) {
            try { CodecSnapshotLocal.decodificar(b); fail() } catch (_: FalloLocal) { }
        }
    }
}
