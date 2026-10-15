package dev.controlparental.child.pairing

import android.content.Context
import android.os.SystemClock
import android.os.UserManager
import android.util.AtomicFile
import dev.controlparental.child.local.*
import java.io.File
import java.io.FileNotFoundException

/** Singleton por ruta CE; noBackup/AtomicFile, sin tocar almacenes legado/DP/credencial/Keystore. */
internal object PairAndroid {
    private val almacenes = mutableMapOf<String, AlmacenPairCp2>()
    @Synchronized fun almacen(contexto: Context): AlmacenPairCp2 {
        val app = contexto.applicationContext
        check(!app.isDeviceProtectedStorage)
        check(app.getSystemService(UserManager::class.java)?.isUserUnlocked == true)
        val base = File(app.noBackupFilesDir, "vinculo-cp2-v1").canonicalFile
        return almacenes.getOrPut(base.path) { AlmacenPairCp2(ArchivosPairAndroid(base)) }
    }
    fun cargar(contexto: Context): CargaPairCp2 = try {
        val carga = almacen(contexto).cargar()
        if (carga is CargaPairCp2.Guardado &&
            IdentidadOfertaAndroid(contexto).leer().contexto?.identidad != carga.snapshot.publicaHijo) CargaPairCp2.Incierta
        else carga
    } catch (_: Exception) { CargaPairCp2.Incierta }

    fun guardar(contexto: Context, caso: ValidacionPairRam, v: ValidacionPair, sesion: SesionAdulto,
        ticket: TicketAdulto): ResultadoGuardarPair {
        fun vigente() = ticket.fin == FinAdulto.GUARDAR_PAIR && sesion.accionVigente(ticket) &&
            sesion.marcoVigente(v.marco) && caso.resultadoActual() === v &&
            IdentidadOfertaAndroid(contexto).leer().contexto == v.referencia.contexto
        if (!vigente()) return ResultadoGuardarPair.RECHAZADO
        return almacen(contexto).guardar(v, ConsentimientoPair(v.consentimiento, v.boot, v.generacionOferta,
            v.marco.revision, SystemClock.elapsedRealtime()), ::vigente)
    }
}

internal class ArchivosPairAndroid(private val base: File) : ArchivosPairCp2 {
    private fun leer(ruta: File): ByteArray? {
        val restos = listOf(ruta, File(ruta.path + ".bak"), File(ruta.path + ".new")).any { it.exists() }
        // openRead puede retirar .new: conservarlo si es el único resto para no fabricar virginidad.
        check(!restos || ruta.exists() || File(ruta.path + ".bak").exists())
        val entrada = try { AtomicFile(ruta).openRead() } catch (e: FileNotFoundException) {
            if (restos || !ruta.parentFile!!.isDirectory || !ruta.parentFile!!.canRead()) throw e
            return null
        }
        return entrada.use {
            val salida = java.io.ByteArrayOutputStream(); val buffer = ByteArray(1024)
            while (true) {
                val n = it.read(buffer); if (n < 0) break
                check(salida.size() + n <= CodecRegistroPair.MAX_REGISTRO); salida.write(buffer, 0, n)
            }
            salida.toByteArray()
        }
    }
    private fun escribir(ruta: File, bytes: ByteArray) {
        require(bytes.size in 1..CodecRegistroPair.MAX_REGISTRO)
        val a = AtomicFile(ruta); val flujo = a.startWrite()
        try { flujo.write(bytes); a.finishWrite(flujo) } catch (e: Exception) { a.failWrite(flujo); throw e }
    }
    override fun leerReserva(): ByteArray? {
        val reserva = leer(File(base.path + ".reserva"))
        check(reserva != null || !File(base.path + ".intento").exists())
        return reserva
    }
    override fun leerRegistro() = leer(base)
    override fun reservar(bytes: ByteArray) {
        check(leerReserva() == null && leerRegistro() == null)
        val intento = File(base.path + ".intento")
        check(intento.createNewFile()) // Incluso vacío es resto: nunca se interpreta como ausencia coherente.
        java.io.FileOutputStream(intento).use { it.write(1); it.fd.sync() }
        escribir(File(base.path + ".reserva"), bytes)
    }
    override fun escribirRegistro(bytes: ByteArray) { check(leerRegistro() == null); escribir(base, bytes) }
}
