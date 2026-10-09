package dev.controlparental.child.recovery

import android.os.SystemClock
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.admin.DeviceCapabilities
import dev.controlparental.child.boot.FuenteArranqueAndroid
import dev.controlparental.child.data.*
import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Solo archivos efímeros H05; sin estado real, códigos reales, claves privadas ni wipe. */
@RunWith(AndroidJUnit4::class)
class RecuperacionArchivoAndroidTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun laboratorio(tarea: (ChildStateStore, String, String, Long) -> Unit) {
        val protegido = contexto.createDeviceProtectedStorageContext()
        val directorio = File(protegido.filesDir, "h05-prueba-${UUID.randomUUID()}")
        assertTrue(directorio.mkdir())
        val archivo = File(directorio, "estado-prueba.json")
        try {
            val estado = ChildStateStore(ArchivoAtomicAndroid(archivo))
            val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("07_pair_accept.payload.json").use { it.readBytes() }
            val par = CodecCp1.leerDocumentoSinAutorizar(bytes) as AceptacionVinculo
            val codigoFicticio = "A".repeat(32)
            val bootId = UUID.randomUUID().toString()
            val observacion = FuenteArranqueAndroid(contexto).observar()
            estado.actualizar { EstadoControl(vinculo = VinculoPublico(par.pairId, par.parentPublicKeyB64,
                par.childPublicKeyB64, par.relayOrigin, par.recoveryHashes.copy(MAINTENANCE =
                    CodigoRecuperacion.hash(par.pairId, PropositoRecuperacionLocal.MAINTENANCE, codigoFicticio))),
                bootContext = ContextoArranque(observacion.systemBootCount, bootId, null, observacion.elapsedRealtimeMs),
                provisioningStage = EtapaAprovisionamiento.VINCULADO) }
            tarea(estado, bootId, codigoFicticio, observacion.elapsedRealtimeMs)
        } finally {
            listOf(archivo, File(archivo.path + ".bak"), File(archivo.path + ".new")).forEach {
                assertTrue(!it.exists() || it.delete())
            }
            assertTrue(directorio.delete())
        }
    }

    @Test
    fun `fuente Android compara Owner desbloqueo y contador real sin escribir estado`() = laboratorio { almacen, bootId, _, _ ->
        val antes = (almacen.cargar() as CargaEstado.Disponible).estado
        val owner = DeviceCapabilities(contexto).leerAdministracion().esDeviceOwner == true
        val desbloqueado = contexto.getSystemService(UserManager::class.java).isUserUnlocked
        val contador = FuenteArranqueAndroid(contexto).observar().systemBootCount
        val fuente = FuenteRecuperacionAndroid(contexto, almacen)
        assertEquals(owner && desbloqueado && contador != null, fuente.leer().usuarioDesbloqueado)
        assertEquals(if (contador != null) bootId else null, fuente.leer().bootId)
        assertEquals(antes, (almacen.cargar() as CargaEstado.Disponible).estado)
        almacen.actualizar { it!!.copy(bootContext = it.bootContext!!.copy(systemBootCount = null)) }
        assertFalse(fuente.leer().usuarioDesbloqueado)
        assertNull(fuente.leer().bootId)
    }

    @Test
    fun `consumo AtomicFile precede sesion y no vuelve al recrear controlador`() = laboratorio { almacen, bootId, codigo, _ ->
        val fuente = FuenteContextoRecuperacion { ContextoRecuperacion(true, bootId) }
        val c = RecoveryController(almacen, { SystemClock.elapsedRealtime() }, fuente)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, codigo).resultado)
        assertNotNull(c.accesoActual())
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in (almacen.cargar() as CargaEstado.Disponible).estado.recoveryConsumed)
        val nuevo = RecoveryController(almacen, { SystemClock.elapsedRealtime() }, fuente)
        assertNull(nuevo.accesoActual())
        assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, nuevo.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, codigo).resultado)
        assertNull((almacen.cargar() as CargaEstado.Disponible).estado.bootContext!!.authorizedBootId)
    }

    @Test
    fun `espera en archivo Android se reimpone completa al recrear RAM`() = laboratorio { almacen, bootId, codigo, inicial ->
        var ahora = inicial
        val fuente = FuenteContextoRecuperacion { ContextoRecuperacion(true, bootId) }
        val c = RecoveryController(almacen, { ahora }, fuente)
        repeat(5) { c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido") }
        ahora += 59000 // Reloj inyectado, no cambiar reloj Android ni esperar realmente 60 segundos.
        assertEquals(1000L, c.esperaCodigoActual())
        val nuevo = RecoveryController(almacen, { ahora }, fuente)
        assertEquals(RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 60000), nuevo.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, codigo))
        val estado = (almacen.cargar() as CargaEstado.Disponible).estado
        assertEquals(5, estado.recoveryFailureCount)
        assertTrue(estado.esperaRecuperacionPendiente)
        assertTrue(estado.recoveryConsumed.isEmpty())
    }
}
