package dev.controlparental.child.network

import dev.controlparental.child.lock.*
import org.junit.Assert.*
import org.junit.Test

/** Modelo y transición reales, reloj/disco/Keyguard/LockTask simulados. No acredita broadcasts Android. */
class ReentradaDespertarTest {
    private class Archivo : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null
        override fun leer() = bytes?.copyOf()
        override fun escribir(bytes: ByteArray) { this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private var tiempo = 100L
    private val marca = Archivo(); private val archivo = Archivo()
    private val m = UsoRedLocal(marca, archivo, { tiempo }, { 5 })
    private var keyguard = true; private var modo = ModoLocal.NONE; private var solicitudes = 0
    private val puerto = object : PuertoLockLocal {
        override fun preparar() { if (keyguard) throw FalloLocal(CausaLocal.KEYGUARD) }
        override fun aplicar() {}
        override fun solicitarInicio() { solicitudes++; modo = ModoLocal.LOCKED }
        override fun modo() = modo
        override fun restaurar() { fail("Despertar no concede uso ni restaura políticas") }
        override fun restaurado() = false
    }
    private fun permitir(tipo: UsoLocal = UsoLocal.NORMAL) {
        assertTrue(m.preparar { true }); m.iniciarObservador(1)
        m.observar(ValidacionRed.VALIDADA, 1, 1)
        assertTrue(m.autorizar(tipo, MinutosUso.desdeTexto("1")) { true })
    }
    private fun despertar(cerrando: Boolean = false): DiagnosticoLocal? {
        if (!requiereReentradaAlDespertar(m, cerrando)) return null
        val t = TransicionLocal(puerto, { tiempo }, { !m.permiteUso }, true)
        val g = t.entrar(); t.observar(g); return t.diagnostico
    }
    @Test fun `vencio dormido y despertar reintenta aunque permiso ya estaba revocado`() {
        permitir(); tiempo = 60100; m.comprobarDeadline(m.referencia!!)
        assertFalse(m.permiteUso); val obligacion = archivo.leer(); val ref = m.referencia
        assertEquals(CausaLocal.KEYGUARD, despertar()!!.causa)
        assertEquals(0, solicitudes); assertEquals(ModoLocal.NONE, modo)
        keyguard = false; tiempo += 30000
        assertEquals(FaseLocal.LOCKED, despertar()!!.fase)
        assertEquals(ModoLocal.LOCKED, modo); assertEquals(1, solicitudes)
        assertEquals(ref, m.referencia); assertArrayEquals(obligacion, archivo.leer())
        assertNull(m.vencimientoUso)
    }
    @Test fun `alarma diferida se consolida al despertar normal u offline sin extender plazo`() {
        for (tipo in listOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE)) {
            marca.borrar(); archivo.borrar()
            val modelo = UsoRedLocal(marca, archivo, { tiempo }, { 5 })
            assertTrue(modelo.preparar { true }); modelo.iniciarObservador(1)
            modelo.observar(ValidacionRed.VALIDADA, 1, 1)
            assertTrue(modelo.autorizar(tipo, MinutosUso.desdeTexto("1")) { true })
            tiempo += 120000 // Sin entregar la alarma durante el sueño.
            assertTrue(requiereReentradaAlDespertar(modelo, false))
            assertFalse(modelo.permiteUso)
            assertEquals(MotivoRed.TIEMPO, modelo.motivoRevocacion)
            val ref = modelo.referencia
            repeat(3) { assertTrue(requiereReentradaAlDespertar(modelo, false)) }
            assertEquals(ref, modelo.referencia)
        }
    }
    @Test fun `despertar antes de vencer conserva exactamente el permiso aceptado`() {
        permitir(); val ref = m.referencia; val bytes = archivo.leer(); tiempo = 60099
        keyguard = false
        assertNull(despertar()); assertTrue(m.permiteUso)
        assertEquals(60100L, m.vencimientoUso); assertEquals(ref, m.referencia)
        assertArrayEquals(bytes, archivo.leer()); assertEquals(0, solicitudes)
    }
    @Test fun `senal tardia no aplica una obligacion anterior a nuevo permiso adulto`() {
        permitir(); tiempo = 60100; m.comprobarDeadline(m.referencia!!)
        assertTrue(m.autorizar(UsoLocal.EXCEPCION_OFFLINE, MinutosUso.desdeTexto("2")) { true })
        val ref = m.referencia; keyguard = false; tiempo += 1000
        assertNull(despertar()); assertEquals(ref, m.referencia)
        assertEquals(180100L, m.vencimientoUso); assertEquals(0, solicitudes)
    }
    @Test fun `cierre voluntario prevalece sobre despertar pendiente`() {
        permitir(); assertTrue(m.cerrar { true }); val bytes = archivo.leer(); tiempo += 120000
        assertNull(despertar()); assertArrayEquals(bytes, archivo.leer()); assertEquals(0, solicitudes)
        assertFalse(requiereReentradaAlDespertar(null, true))
    }
    @Test fun `error conserva obligacion de cierre sin recapturar archivos`() {
        permitir(); archivo.bytes = byteArrayOf(0); tiempo = 60100
        assertTrue(requiereReentradaAlDespertar(m, false))
        assertEquals(UsoLocal.ERROR, m.uso); assertFalse(m.permiteUso)
        assertArrayEquals(byteArrayOf(0), archivo.leer())
    }
}
