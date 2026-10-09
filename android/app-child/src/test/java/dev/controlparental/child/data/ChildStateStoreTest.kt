package dev.controlparental.child.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChildStateStoreTest {
    @Test
    fun `solo ausencia real permite inicializar y corrupcion nunca dispara el transformador`() {
        val archivo = ArchivoSimulado()
        val store = ChildStateStore(archivo)
        assertEquals(CargaEstado.Ausente, store.cargar())
        store.actualizar { EstadoControl() }
        archivo.bytes = "corrupto".toByteArray()
        var ejecutado = false
        assertEquals(CargaEstado.ErrorSeguro, store.cargar())
        assertThrows(ErrorEstadoLocal::class.java) { store.actualizar { ejecutado = true; EstadoControl() } }
        assertFalse(ejecutado)
        assertArrayEquals("corrupto".toByteArray(), archivo.bytes)
    }

    @Test
    fun `recrear almacen conserva politica inicio secuencia y etapa`() {
        val archivo = ArchivoSimulado()
        val original = estadoConOrden()
        ChildStateStore(archivo).actualizar { original }
        assertEquals(CargaEstado.Disponible(original), ChildStateStore(archivo).cargar())
    }

    @Test
    fun `fallo de lectura o archivo excesivo mantienen error seguro`() {
        val archivo = ArchivoSimulado().apply { falloLectura = true }
        val store = ChildStateStore(archivo)
        assertEquals(CargaEstado.ErrorSeguro, store.cargar())
        archivo.falloLectura = false
        archivo.bytes = ByteArray(CodecEstado.MAX_ARCHIVO_BYTES + 1)
        assertEquals(CargaEstado.ErrorSeguro, store.cargar())
    }

    @Test
    fun `escritura interrumpida o commit silenciosamente perdido no confirma almacenamiento`() {
        val archivo = ArchivoSimulado()
        val store = ChildStateStore(archivo)
        store.actualizar { EstadoControl() }
        archivo.falloEscritura = true
        assertThrows(ErrorEstadoLocal::class.java) { store.actualizar { it!!.copy(recoveryFailureCount = 1) } }
        assertEquals(EstadoControl(), (store.cargar() as CargaEstado.Disponible).estado)
        archivo.falloEscritura = false
        archivo.omiteCommit = true
        assertThrows(ErrorEstadoLocal::class.java) { store.actualizar { it!!.copy(recoveryFailureCount = 1) } }
    }

    @Test
    fun `no se pierde contador ni se reinicia tiempo ante repeticion exacta`() {
        val archivo = ArchivoSimulado()
        val store = ChildStateStore(archivo)
        val estado = estadoConOrden()
        store.actualizar { estado }
        assertEquals(estado, store.actualizar { it!! })
        assertThrows(IllegalArgumentException::class.java) { store.actualizar { EstadoControl() } }
        assertThrows(IllegalArgumentException::class.java) { store.actualizar { it!!.copy(policy = it.policy!!.copy(aceptadaEnMs = 99999)) } }
        assertEquals(estado, (store.cargar() as CargaEstado.Disponible).estado)
    }

    @Test
    fun `consumo de recuperacion no retrocede dentro del mismo vinculo`() {
        val store = ChildStateStore(ArchivoSimulado())
        store.actualizar { estadoConOrden().copy(recoveryConsumed = setOf(PropositoRecuperacionLocal.MAINTENANCE)) }
        assertThrows(IllegalArgumentException::class.java) { store.actualizar { it!!.copy(recoveryConsumed = emptySet()) } }
    }

    @Test
    fun `cien escritores concurrentes no pierden actualizaciones`() {
        val store = ChildStateStore(ArchivoSimulado())
        store.actualizar { EstadoControl() }
        val ejecutor = Executors.newFixedThreadPool(4)
        try {
            val tareas = List(100) { ejecutor.submit { store.actualizar { anterior -> anterior!!.copy(recoveryFailureCount = anterior.recoveryFailureCount + 1) } } }
            tareas.forEach { it.get(10, TimeUnit.SECONDS) }
            assertEquals(100, (store.cargar() as CargaEstado.Disponible).estado.recoveryFailureCount)
        } finally {
            ejecutor.shutdownNow()
        }
    }
}
