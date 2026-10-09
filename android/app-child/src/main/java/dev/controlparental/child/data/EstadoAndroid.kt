package dev.controlparental.child.data

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executors

/** Sin lectura Credential Encrypted ni Keystore; un almacén por ruta y proceso. */
object EstadoAndroid {
    private val almacenes = mutableMapOf<String, ChildStateStore>()

    @Synchronized
    fun obtener(contexto: Context): ChildStateStore {
        val protegido = contexto.applicationContext.createDeviceProtectedStorageContext()
        check(protegido.isDeviceProtectedStorage)
        val ruta = File(protegido.filesDir, "control-state.json").canonicalFile
        return almacenes.getOrPut(ruta.path) { ChildStateStore(ArchivoAtomicAndroid(ruta)) }
    }
}

/** Misma cola de IO para launcher y broadcasts; las transacciones también se serializan. */
object EjecutorEstado {
    private val ejecutor = Executors.newSingleThreadExecutor { tarea -> Thread(tarea, "estado-local") }
    fun ejecutar(tarea: () -> Unit) { ejecutor.execute(tarea) }
}

/** Solo se usa dentro de ChildStateStore. AtomicFile NO aporta exclusión entre hilos. */
internal class ArchivoAtomicAndroid(private val ruta: File) : ArchivoEstadoAtomico {
    private val atomico = AtomicFile(ruta)

    override fun leerAcotado(maximoBytes: Int): ByteArray? {
        require(maximoBytes in 1..CodecEstado.MAX_ARCHIVO_BYTES)
        val restosPrevios = listOf(ruta, File(ruta.path + ".bak"), File(ruta.path + ".new")).any { it.exists() }
        val entrada = try {
            atomico.openRead()
        } catch (error: FileNotFoundException) {
            // Un archivo parcial o inaccesible no es una instalación nueva.
            if (restosPrevios || !ruta.parentFile!!.isDirectory || !ruta.parentFile!!.canRead()) throw error
            return null
        }
        return entrada.use { flujo ->
            val salida = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (true) {
                val leidos = flujo.read(buffer, 0, minOf(buffer.size, maximoBytes + 1 - salida.size()))
                if (leidos == -1) break
                salida.write(buffer, 0, leidos)
                if (salida.size() > maximoBytes) throw IOException("Registro local excede el límite")
            }
            salida.toByteArray()
        }
    }

    override fun escribirAtomico(bytes: ByteArray) {
        require(bytes.size in 1..CodecEstado.MAX_ARCHIVO_BYTES)
        val flujo = atomico.startWrite()
        var terminado = false
        try {
            flujo.write(bytes)
            atomico.finishWrite(flujo)
            terminado = true
        } finally {
            if (!terminado) atomico.failWrite(flujo)
        }
    }
}
