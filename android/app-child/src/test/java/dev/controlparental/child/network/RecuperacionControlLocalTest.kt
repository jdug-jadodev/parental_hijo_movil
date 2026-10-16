package dev.controlparental.child.network

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import org.junit.Assert.*
import org.junit.Test

class RecuperacionControlLocalTest {
    private class Archivo : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null
        override fun leer() = bytes?.copyOf()
        override fun escribir(bytes: ByteArray) { this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private val marca = Archivo(); private val registro = Archivo()
    private var boot = 5; private var tiempo = 100L
    private fun modelo() = UsoRedLocal(marca, registro, { tiempo }, { boot })
    private fun decision(m: UsoRedLocal, armado: Boolean? = true) =
        recuperacionControlLocal(true, m.uso, m.motivoRevocacion, armado, false)

    @Test fun `recreacion reanuda observador sin restaurar permiso libre ni minutos normales u offline`() {
        for (tipo in listOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE)) {
            for (minutos in listOf(null, MinutosUso.desdeTexto("2"))) {
                marca.borrar(); registro.borrar()
                val anterior = modelo(); assertTrue(anterior.preparar { true })
                anterior.iniciarObservador(1); anterior.observar(ValidacionRed.VALIDADA, 1, 1)
                assertTrue(anterior.autorizar(tipo, minutos) { true })
                val bytes = registro.leer(); tiempo++
                val nuevo = modelo(); assertTrue(nuevo.cargar())
                assertEquals(RecuperacionControlLocal.REANUDAR, decision(nuevo))
                nuevo.iniciarObservador(2); nuevo.observar(ValidacionRed.VALIDADA, 2, 1)
                assertFalse(nuevo.permiteUso); assertNull(nuevo.vencimientoUso)
                assertArrayEquals(bytes, registro.leer())
            }
        }
    }
    @Test fun `cierre interrumpido no se reactiva al recrear ni al cambiar boot`() {
        val m = modelo(); assertTrue(m.preparar { true }); assertTrue(m.cerrar { true })
        val cierre = registro.leer()
        for (b in listOf(5, 6)) {
            boot = b
            val nuevo = modelo(); assertTrue(nuevo.cargar())
            assertEquals(UsoLocal.FINALIZANDO, nuevo.uso)
            assertEquals(RecuperacionControlLocal.INACTIVO, decision(nuevo))
            assertFalse(nuevo.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true })
            assertArrayEquals(cierre, registro.leer())
        }
    }
    @Test fun `sin preparacion o con desarme no crea observador ni registros`() {
        assertEquals(RecuperacionControlLocal.INACTIVO, recuperacionControlLocal(false, null, null, false, false))
        assertNull(marca.leer()); assertNull(registro.leer())
        val m = modelo(); assertTrue(m.preparar { true })
        assertEquals(RecuperacionControlLocal.INACTIVO, decision(m, false))
    }
    @Test fun `corrupcion y armado incierto solicitan cierre sin reparar archivos`() {
        val m = modelo(); assertTrue(m.preparar { true })
        assertEquals(RecuperacionControlLocal.ERROR_CERRADO, decision(m, null))
        registro.bytes = byteArrayOf(0)
        val nuevo = modelo(); assertFalse(nuevo.cargar())
        assertEquals(RecuperacionControlLocal.ERROR_CERRADO, decision(nuevo))
        assertFalse(nuevo.permiteUso); assertArrayEquals(byteArrayOf(0), registro.leer())
    }
}
