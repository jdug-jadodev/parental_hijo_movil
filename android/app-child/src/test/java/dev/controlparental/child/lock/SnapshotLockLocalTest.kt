package dev.controlparental.child.lock

import org.junit.Assert.*
import org.junit.Test

class SnapshotLockLocalTest {
    @Test fun `lista vacia serializada por Android14 permite recuperar sin reescribir snapshot`() {
        p.lista = emptySet(); p.flags = 16
        adaptador.preparar(); val bytes = a.bytes!!.copyOf()
        // Mismas operaciones join/split Java de LockTaskPolicySerializer, no un paquete válido.
        p.lista = java.util.regex.Pattern.compile(";").split(p.lista.joinToString(";")).toSet()
        assertEquals(setOf(""), p.lista)
        assertNotEquals(CodecSnapshotLocal.decodificar(bytes).paquetes, p.lista)
        adaptador.comprobarAlternancia(); adaptador.aplicarGuardado(); adaptador.solicitarInicio()
        assertEquals(ModoLocal.LOCKED, p.estado)
        assertEquals(setOf(p.paquete), p.lista); assertEquals(48, p.flags)
        assertArrayEquals(bytes, a.bytes)
        adaptador.restaurar(); p.estado = ModoLocal.NONE
        assertTrue(adaptador.baselineConfirmado()); assertEquals(16, p.flags)
        assertArrayEquals(bytes, a.bytes)
    }
    @Test fun `compatibilidad de vacio no oculta paquete ajeno espacios mezcla ni flags distintos`() {
        p.lista = emptySet(); p.flags = 16; adaptador.preparar(); val bytes = a.bytes!!.copyOf()
        for (lista in listOf(setOf("dev.ajeno"), setOf(" "), setOf("", "dev.ajeno"), setOf("", p.paquete))) {
            p.lista = lista
            try { adaptador.aplicarGuardado(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        }
        p.lista = setOf(""); p.flags = 12345
        try { adaptador.aplicarGuardado(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        assertEquals(0, p.writes); assertArrayEquals(bytes, a.bytes)
    }
    @Test fun `uso libre conserva snapshot y permite bloquear despues sin pisar baseline`() {
        adaptador.preparar(); val bytes = a.bytes!!.copyOf()
        adaptador.aplicar(); adaptador.solicitarInicio(); adaptador.restaurar(); p.estado = ModoLocal.NONE
        assertTrue(adaptador.baselineConfirmado()); assertArrayEquals(bytes, a.bytes)
        adaptador.comprobarAlternancia(); adaptador.aplicarGuardado(); adaptador.solicitarInicio()
        assertEquals(ModoLocal.LOCKED, p.estado); assertArrayEquals(bytes, a.bytes)
        p.lista = setOf("dev.ajeno")
        try { adaptador.aplicarGuardado(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
    }
    @Test fun `snapshot propio reaprovechado tras reboot no captura baseline falso`() {
        adaptador.preparar(); val b = a.bytes!!.copyOf(); adaptador.aplicar()
        p.estado = ModoLocal.NONE; val writes = p.writes
        adaptador.comprobarReentrada(); assertArrayEquals(b, a.bytes); assertEquals(writes, p.writes)
        p.lista = setOf("dev.ajeno")
        try { adaptador.comprobarReentrada(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.POLITICA_AJENA, e.causa) }
        assertArrayEquals(b, a.bytes)
    }
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
        override fun leerPaquetes() = paquetesLockLeidos(lista.toTypedArray())
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
