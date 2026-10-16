package dev.controlparental.child.protection

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import org.junit.Assert.*
import org.junit.Test

class ProteccionLocalTest {
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null; var negarWrite = false; var negarDelete = false
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { if (!negarWrite) this.bytes = bytes.copyOf() }
        override fun borrar() { if (!negarDelete) bytes = null }
    }
    private class PoliticasPrueba : PuertoProteccion {
        var u = false; var propio = false; var global = false; var ajeno = false; var uninstallAjeno = false
        var writes = 0; var ignorar = false; var incompatible = false; var despuesUninstall: () -> Unit = {}
        override fun validar() { exigirProteccion(!incompatible, CausaProteccion.BASELINE) }
        override fun leer() = LecturaProteccion(u || uninstallAjeno, propio, global, propio || global || ajeno)
        override fun uninstall(bloquear: Boolean) { writes++; if (!ignorar) u = bloquear; despuesUninstall() }
        override fun control(restringir: Boolean) { writes++; if (!ignorar) propio = restringir }
    }
    private val f = ArchivoPrueba(); private val p = PoliticasPrueba(); private var vigente = true
    private val m = ProteccionLocal(f, p) { vigente }
    @Test fun `aplica dos readbacks guarda antes y restore confirmado borra`() {
        p.despuesUninstall = { assertNotNull(f.bytes) }
        assertEquals(FaseProteccion.ACTIVA, m.aplicar().fase)
        assertEquals(LecturaProteccion(true, true, false, true), m.consultar().lectura)
        assertEquals(FaseProteccion.RESTAURADA, m.restaurar().fase)
        assertNull(f.bytes); assertFalse(p.u); assertFalse(p.propio)
    }
    @Test fun `baseline propio preexistente y global propio se conservan`() {
        p.propio = true; p.global = true
        m.aplicar(); val r = m.restaurar()
        assertEquals(FaseProteccion.RESTAURADA, r.fase)
        assertTrue(p.propio); assertTrue(p.global); assertFalse(p.u)
    }
    @Test fun `uninstall efectivo previo no se toma como propio`() {
        p.uninstallAjeno = true
        assertEquals(CausaProteccion.BASELINE, m.aplicar().causa)
        assertEquals(0, p.writes); assertNull(f.bytes)
    }
    @Test fun `control efectivo ajeno no se captura como baseline propio`() {
        p.ajeno = true
        assertEquals(CausaProteccion.BASELINE, m.aplicar().causa)
        assertEquals(0, p.writes); assertNull(f.bytes)
    }
    @Test fun `snapshot ausente corrupto no permite restore defaults`() {
        for (b in listOf<ByteArray?>(null, byteArrayOf(0), ByteArray(37))) {
            f.bytes = b; assertEquals(CausaProteccion.SNAPSHOT, m.restaurar().causa)
        }
        assertEquals(0, p.writes)
    }
    @Test fun `snapshot persistido impide otro aplicar hasta restauracion`() {
        m.aplicar(); val n = p.writes
        assertEquals(CausaProteccion.BASELINE, m.aplicar().causa); assertEquals(n, p.writes)
    }
    @Test fun `readback escritura fallida no escribe DPM`() {
        f.negarWrite = true
        assertEquals(CausaProteccion.SNAPSHOT, m.aplicar().causa); assertEquals(0, p.writes)
    }
    @Test fun `readback DPM fallido no es ACTIVA ni borra obligacion`() {
        p.ignorar = true
        assertEquals(CausaProteccion.READBACK, m.aplicar().causa); assertNotNull(f.bytes)
    }
    @Test fun `autoridad cancelada antes no inicia`() {
        vigente = false
        assertEquals(CausaProteccion.AUTORIDAD, m.aplicar().causa); assertNull(f.bytes)
        assertEquals(0, p.writes)
    }
    @Test fun `fondo entre writes conserva parcial hasta adulto fresco no autoquita`() {
        p.despuesUninstall = { vigente = false }
        assertEquals(CausaProteccion.AUTORIDAD, m.aplicar().causa)
        assertTrue(p.u); assertFalse(p.propio); assertNotNull(f.bytes)
        assertEquals(CausaProteccion.AUTORIDAD, m.restaurar().causa)
        vigente = true; p.despuesUninstall = {}
        assertEquals(FaseProteccion.RESTAURADA, m.restaurar().fase)
    }
    @Test fun `ACTIVA no desaparece al cerrar sesion o consultar`() {
        m.aplicar(); vigente = false; val n = p.writes
        repeat(3) { assertEquals(FaseProteccion.ACTIVA, m.consultar().fase) }
        assertEquals(CausaProteccion.AUTORIDAD, m.restaurar().causa)
        assertEquals(n, p.writes); assertTrue(p.u && p.propio)
    }
    @Test fun `cambio global propio ajeno no se pisa al restaurar`() {
        m.aplicar(); p.global = true; val n = p.writes
        assertEquals(CausaProteccion.AJENA, m.restaurar().causa)
        assertEquals(n, p.writes); assertTrue(p.global); assertNotNull(f.bytes)
    }
    @Test fun `otro admin superpuesto no se elimina y falla readback baseline`() {
        m.aplicar(); p.ajeno = true; p.uninstallAjeno = true
        assertEquals(CausaProteccion.READBACK, m.restaurar().causa)
        assertTrue(p.ajeno && p.uninstallAjeno); assertNotNull(f.bytes)
        assertFalse(p.u || p.propio)
    }
    @Test fun `baseline propio true quitado por tercero no se restaura encima`() {
        p.propio = true; m.aplicar(); p.propio = false; val n = p.writes
        assertEquals(CausaProteccion.AJENA, m.restaurar().causa); assertEquals(n, p.writes)
    }
    @Test fun `fallo delete mantiene snapshot y reporta error`() {
        m.aplicar(); f.negarDelete = true
        assertEquals(CausaProteccion.SNAPSHOT, m.restaurar().causa); assertNotNull(f.bytes)
    }
    @Test fun `capacidad incompatible falla antes de escribir`() {
        p.incompatible = true
        assertEquals(CausaProteccion.BASELINE, m.aplicar().causa); assertEquals(0, p.writes)
    }
    @Test fun `codec exacto version checksum booleanos y limite`() {
        val b = CodecProteccion.codificar(p.leer()); assertEquals(p.leer(), CodecProteccion.decodificar(b))
        for (v in listOf(b.copyOf().also { it[0] = 2 }, b.copyOf().also { it[1] = 2 }, b + 0, ByteArray(100))) {
            try { CodecProteccion.decodificar(v); fail() } catch (e: FalloProteccion) { assertEquals(CausaProteccion.SNAPSHOT, e.causa) }
        }
    }
    @Test fun `IO excepcion no permite escrituras ni alta libre`() {
        val roto = object : ArchivoSnapshotLocal {
            override fun leer(): ByteArray? = throw java.io.IOException()
            override fun escribir(bytes: ByteArray) = throw java.io.IOException()
            override fun borrar() = throw java.io.IOException()
        }
        val modelo = ProteccionLocal(roto, p) { true }
        assertEquals(CausaProteccion.ANDROID_IO, modelo.aplicar().causa)
        assertTrue(modelo.consultar().snapshot); assertEquals(0, p.writes)
    }
    @Test fun `cambio global entre writes detiene restauracion antes de control`() {
        m.aplicar(); val n = p.writes; p.despuesUninstall = { p.global = true }
        assertEquals(CausaProteccion.AJENA, m.restaurar().causa)
        assertEquals(n + 1, p.writes); assertTrue(p.propio && p.global); assertNotNull(f.bytes)
    }
    @Test fun `preparar control acepta proteccion activa propia y conserva bytes y politicas`() {
        assertEquals(FaseProteccion.ACTIVA, m.aplicar().fase)
        val bytes = f.bytes!!.copyOf(); val n = p.writes; val lectura = p.leer()
        vigente = false // La comprobación NO concede autoridad ni la necesita para leer.
        val recreado = ProteccionLocal(f, p) { false }
        repeat(3) { assertTrue(recreado.permitePrepararControl()) }
        assertArrayEquals(bytes, f.bytes); assertEquals(n, p.writes); assertEquals(lectura, p.leer())
        assertEquals(CausaProteccion.AUTORIDAD, recreado.restaurar().causa)
    }
    @Test fun `preparar control conserva baseline sin proteccion y rechaza restricciones sin snapshot`() {
        assertTrue(m.permitePrepararControl()); assertNull(f.bytes); assertEquals(0, p.writes)
        p.u = true; assertFalse(m.permitePrepararControl())
        p.u = false; p.propio = true; assertFalse(m.permitePrepararControl())
        p.propio = false; p.global = true; assertFalse(m.permitePrepararControl())
        p.global = false; p.ajeno = true; assertFalse(m.permitePrepararControl())
        assertNull(f.bytes); assertEquals(0, p.writes)
    }
    @Test fun `preparar control no acepta parcial ni snapshot corrupto o perdido`() {
        m.aplicar(); val bytes = f.bytes!!.copyOf(); val n = p.writes
        p.propio = false; assertFalse(m.permitePrepararControl())
        p.propio = true; p.u = false; assertFalse(m.permitePrepararControl())
        p.u = true
        for (b in listOf<ByteArray?>(null, byteArrayOf(0), bytes.copyOf().also { it[5] = (it[5].toInt() xor 1).toByte() })) {
            f.bytes = b; assertFalse(m.permitePrepararControl()); assertArrayEquals(b, f.bytes)
        }
        assertEquals(n, p.writes)
    }
    @Test fun `preparar control comprueba de nuevo cambios ajenos capacidad e IO`() {
        m.aplicar(); val n = p.writes; val bytes = f.bytes!!.copyOf()
        assertTrue(m.permitePrepararControl())
        p.global = true; assertFalse(m.permitePrepararControl())
        p.global = false; p.incompatible = true; assertFalse(m.permitePrepararControl())
        val roto = object : ArchivoSnapshotLocal {
            override fun leer(): ByteArray? = throw java.io.IOException()
            override fun escribir(bytes: ByteArray) { fail("No debe escribir") }
            override fun borrar() { fail("No debe borrar") }
        }
        p.incompatible = false
        assertFalse(ProteccionLocal(roto, p) { true }.permitePrepararControl())
        assertEquals(n, p.writes); assertArrayEquals(bytes, f.bytes)
    }
}
