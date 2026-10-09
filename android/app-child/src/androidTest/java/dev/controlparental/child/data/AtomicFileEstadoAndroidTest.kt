package dev.controlparental.child.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.boot.BootReceiver
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Directorio efímero aislado en el UID instrumentado, nunca control-state.json. */
@RunWith(AndroidJUnit4::class)
class AtomicFileEstadoAndroidTest {
    // La instrumentación ejecuta con el UID del destino, no con el del APK test.
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun conArchivo(tarea: (File) -> Unit) {
        val protegido = contexto.createDeviceProtectedStorageContext()
        assertTrue(protegido.isDeviceProtectedStorage)
        assertNotEquals(contexto.filesDir.canonicalPath, protegido.filesDir.canonicalPath)
        val directorio = File(protegido.filesDir, "h04-prueba-${UUID.randomUUID()}")
        assertTrue("Crear únicamente el directorio efímero del test", directorio.mkdir())
        val archivo = File(directorio, "estado-prueba.json")
        try {
            tarea(archivo)
        } finally {
            // Solo los tres nombres únicos creados por este test.
            listOf(archivo, File(archivo.path + ".bak"), File(archivo.path + ".new")).forEach {
                assertTrue("Retirar únicamente el archivo efímero del test", !it.exists() || it.delete())
            }
            assertTrue("Retirar únicamente el directorio efímero vacío", directorio.delete())
        }
    }

    @Test
    fun `AtomicFile persiste un registro en Device Protected y permite recrear el lector`() = conArchivo { ruta ->
        val inicial = EstadoControl(recoveryFailureCount = 2, esperaRecuperacionPendiente = true)
        ChildStateStore(ArchivoAtomicAndroid(ruta)).actualizar { inicial }
        assertEquals(CargaEstado.Disponible(inicial), ChildStateStore(ArchivoAtomicAndroid(ruta)).cargar())
    }

    @Test
    fun `failWrite conserva el registro anterior y no confirma los bytes parciales`() = conArchivo { ruta ->
        val inicial = EstadoControl()
        val store = ChildStateStore(ArchivoAtomicAndroid(ruta))
        store.actualizar { inicial }
        val atomico = AtomicFile(ruta)
        val escritura = atomico.startWrite()
        try {
            escritura.write("interrumpido".toByteArray())
        } finally {
            atomico.failWrite(escritura)
        }
        assertEquals(CargaEstado.Disponible(inicial), store.cargar())
    }

    @Test
    fun `corrupcion permanece intacta y no se inicializa de nuevo`() = conArchivo { ruta ->
        ruta.writeText("corrupto")
        val store = ChildStateStore(ArchivoAtomicAndroid(ruta))
        assertEquals(CargaEstado.ErrorSeguro, store.cargar())
        assertThrows(ErrorEstadoLocal::class.java) { store.actualizar { EstadoControl() } }
        assertEquals("corrupto", ruta.readText())
    }

    @Test
    fun `archivo nuevo parcial sin anterior no equivale a estado ausente`() = conArchivo { ruta ->
        File(ruta.path + ".new").writeText("interrumpido")
        assertEquals(CargaEstado.ErrorSeguro, ChildStateStore(ArchivoAtomicAndroid(ruta)).cargar())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `receiver de boot esta protegido de exportacion y declarado para Direct Boot`() {
        val destino = InstrumentationRegistry.getInstrumentation().targetContext
        val info = destino.packageManager.getReceiverInfo(ComponentName(destino, BootReceiver::class.java), 0)
        assertFalse(info.exported)
        assertTrue(info.directBootAware)
        assertEquals(PackageManager.PERMISSION_GRANTED, destino.checkSelfPermission("android.permission.RECEIVE_BOOT_COMPLETED"))
    }
}
