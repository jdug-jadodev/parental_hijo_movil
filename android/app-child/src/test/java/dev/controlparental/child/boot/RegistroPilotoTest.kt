package dev.controlparental.child.boot

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import org.junit.Assert.*
import org.junit.Test

class RegistroPilotoTest {
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null; var ignorar = false
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { if (!ignorar) this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private val marca = ArchivoPrueba(); private val registro = ArchivoPrueba()
    private var boot: Int? = 5; private var tiempo = 100L
    private val s = AlmacenPiloto(marca, registro, FuenteArranque { ObservacionArranque(boot, tiempo) })
    private fun falla(accion: () -> Unit) { try { accion(); fail() } catch (_: Exception) {} }
    @Test fun `virgen no se enrola ni bloquea por observar instalar`() {
        assertNull(s.observar()); assertNull(marca.bytes); assertNull(registro.bytes)
    }
    @Test fun `armar requiere adulto antes persistencia`() {
        falla { s.armar { false } }; assertNull(s.leer())
        val r = s.armar { true }; assertTrue(r.armado); assertTrue(s.vigente(r))
    }
    @Test fun `mismo boot mantiene generacion no permiso ni sesion en registro`() {
        val r = s.armar { true }; tiempo++
        val n = s.observar()!!; assertEquals(r.generacion, n.generacion); assertTrue(s.vigente(r))
    }
    @Test fun `nuevo boot invalida callbacks previos y conserva bloqueo sin permiso`() {
        val r = s.armar { true }; boot = 6; tiempo = 1
        val n = s.observar()!!; assertTrue(n.armado); assertTrue(n.generacion > r.generacion)
        assertFalse(s.vigente(r)); assertTrue(s.vigente(n))
    }
    @Test fun `contexto incierto reloj regresivo conserva armado renueva generacion`() {
        val r = s.armar { true }; boot = null; tiempo = 0
        val n = s.observar()!!; assertTrue(n.armado); assertNull(n.boot); assertFalse(s.vigente(r))
        assertTrue(s.vigente(n)); tiempo = -1; assertFalse(s.vigente(n)); falla { s.observar() }
    }
    @Test fun `desarme invalida callbackviejo y nuevo reboot no rearma`() {
        val r = s.armar { true }; s.desarmar { true }; boot = 8; tiempo = 1
        assertFalse(s.vigente(r)); assertFalse(s.observar()!!.armado)
        falla { s.finalizar(false) }; assertNotNull(s.leer())
        s.finalizar(true); assertNull(s.leer())
    }
    @Test fun `cancelar autoridad no desarma ni permite nuevo enrolamiento`() {
        val r = s.armar { true }; falla { s.desarmar { false } }
        assertEquals(r, s.leer()); falla { s.armar { true } }
    }
    @Test fun `perdida parcial corrupcion falla cerrado nunca alta`() {
        s.armar { true }; val b = registro.bytes!!.copyOf()
        registro.bytes = null; falla { s.leer() }; falla { s.armar { true } }
        registro.bytes = b; marca.bytes = null; falla { s.observar() }
    }
    @Test fun `checksum alterado version excess size son error`() {
        val b = CodecPiloto.codificar(s.armar { true })
        for (v in listOf(b.copyOf().also { it[0] = 9 }, ByteArray(129), b.copyOf(10))) falla { CodecPiloto.decodificar(v) }
    }
    @Test fun `fallo readback de registro no crea alta libre ni borra marca`() {
        registro.ignorar = true; falla { s.armar { true } }
        assertNotNull(marca.bytes); falla { s.leer() }
    }
}
