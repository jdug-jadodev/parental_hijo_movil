package dev.controlparental.child.orders

import android.content.Context
import android.os.UserManager
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/** Reserva por transacción, sin borrar restos ambiguos ni interpretar un .new huérfano como virginidad. */
internal class ArchivosControlAndroid(c: Context) : ArchivosControl {
    private val app = c.applicationContext
    private val base = File(app.noBackupFilesDir, "control-cp2-v1")
    private val ancla = File(base.path + ".inicial")
    private val pendiente = File(base.path + ".reserva")
    private fun ce() { check(!app.isDeviceProtectedStorage && app.getSystemService(UserManager::class.java).isUserUnlocked) }
    private fun leer(f: File): ByteArray? {
        ce()
        val restos = listOf(f, File(f.path + ".bak"), File(f.path + ".new")).any(File::exists)
        check(!restos || f.exists() || File(f.path + ".bak").exists())
        val flujo = try { AtomicFile(f).openRead() } catch (e: FileNotFoundException) {
            check(!restos && f.parentFile!!.isDirectory && f.parentFile!!.canRead()); return null
        }
        return flujo.use { entrada ->
            val salida = java.io.ByteArrayOutputStream(); val bloque = ByteArray(1024)
            while (true) {
                val n = entrada.read(bloque); if (n < 0) break
                check(salida.size() + n <= CodecControlDurable.MAX); salida.write(bloque, 0, n)
            }
            salida.toByteArray().also { check(it.isNotEmpty()) }
        }
    }
    private fun escribir(f: File, b: ByteArray) {
        ce(); require(b.size in 1..CodecControlDurable.MAX)
        val a = AtomicFile(f); val out = a.startWrite()
        try { out.write(b); a.finishWrite(out) } catch (e: Exception) { a.failWrite(out); throw e }
    }
    override fun leer(): ByteArray? {
        val b = leer(base)
        check((b != null) == ancla.exists())
        if (b != null) check(ancla.inputStream().use { it.read() == 1 && it.read() == -1 })
        return b
    }
    override fun reserva() = leer(pendiente)
    override fun iniciar(bytes: ByteArray) {
        ce(); check(leer() == null && reserva() == null && ancla.createNewFile())
        FileOutputStream(ancla).use { it.write(1); it.fd.sync() }
        escribir(base, bytes)
    }
    override fun reservar(bytes: ByteArray) { check(reserva() == null); escribir(pendiente, bytes) }
    override fun escribir(bytes: ByteArray) { check(reserva()?.contentEquals(bytes) == true); escribir(base, bytes) }
    override fun terminarReserva() {
        check(pendiente.delete()); check(!File(pendiente.path + ".bak").exists() && !File(pendiente.path + ".new").exists())
    }
}
