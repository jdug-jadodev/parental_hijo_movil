package dev.controlparental.child.protection

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import java.security.MessageDigest

internal enum class FaseProteccion { SIN_SNAPSHOT, PENDIENTE, ACTIVA, RESTAURADA, ERROR }
internal enum class CausaProteccion { NINGUNA, AUTORIDAD, BASELINE, SNAPSHOT, AJENA, READBACK, ANDROID_IO }
internal data class LecturaProteccion(val uninstallEfectivo: Boolean, val controlPropio: Boolean,
    val controlGlobalPropio: Boolean, val controlEfectivo: Boolean)
internal data class EstadoProteccion(val fase: FaseProteccion, val causa: CausaProteccion = CausaProteccion.NINGUNA,
    val lectura: LecturaProteccion? = null, val snapshot: Boolean = false)
internal class FalloProteccion(val causa: CausaProteccion) : RuntimeException()
internal fun exigirProteccion(ok: Boolean, causa: CausaProteccion) { if (!ok) throw FalloProteccion(causa) }

/** API34: uninstall es RESUELTO, no getter de contribución propia. Baseline true se rechaza. */
internal interface PuertoProteccion {
    fun validar()
    fun leer(): LecturaProteccion
    fun uninstall(bloquear: Boolean)
    fun control(restringir: Boolean)
}
internal object CodecProteccion {
    fun codificar(s: LecturaProteccion): ByteArray {
        val b = byteArrayOf(1, if (s.uninstallEfectivo) 1 else 0, if (s.controlPropio) 1 else 0,
            if (s.controlGlobalPropio) 1 else 0, if (s.controlEfectivo) 1 else 0)
        return b + MessageDigest.getInstance("SHA-256").digest(b)
    }
    fun decodificar(b: ByteArray): LecturaProteccion {
        exigirProteccion(b.size == 37 && b[0] == 1.toByte() && b.sliceArray(1..4).all { it == 0.toByte() || it == 1.toByte() }, CausaProteccion.SNAPSHOT)
        exigirProteccion(MessageDigest.isEqual(b.copyOfRange(5, 37),
            MessageDigest.getInstance("SHA-256").digest(b.copyOfRange(0, 5))), CausaProteccion.SNAPSHOT)
        val s = LecturaProteccion(b[1] == 1.toByte(), b[2] == 1.toByte(), b[3] == 1.toByte(), b[4] == 1.toByte())
        exigirProteccion(!s.uninstallEfectivo && s.controlEfectivo == (s.controlPropio || s.controlGlobalPropio), CausaProteccion.SNAPSHOT)
        return s
    }
}

/** Obligación independiente de Lock Task. Cierre/fondo no llama restore; fallo conserva snapshot. */
internal class ProteccionLocal(private val archivo: ArchivoSnapshotLocal, private val puerto: PuertoProteccion,
    private val autoridad: () -> Boolean) {
    private fun autorizar() = exigirProteccion(autoridad(), CausaProteccion.AUTORIDAD)
    private fun snapshot() = archivo.leer()?.let(CodecProteccion::decodificar) ?: throw FalloProteccion(CausaProteccion.SNAPSHOT)
    fun consultar(): EstadoProteccion = intentar {
        puerto.validar(); val l = puerto.leer(); val b = archivo.leer()
        if (b == null) EstadoProteccion(FaseProteccion.SIN_SNAPSHOT, lectura = l)
        else {
            val s = CodecProteccion.decodificar(b); comparar(s, l)
            EstadoProteccion(if (l.uninstallEfectivo && l.controlPropio && l.controlEfectivo) FaseProteccion.ACTIVA else FaseProteccion.PENDIENTE,
                lectura = l, snapshot = true)
        }
    }
    /** Compatibilidad de lectura; preparar Lock Task jamás restaura ni altera protección. */
    fun permitePrepararControl(): Boolean {
        val e = consultar()
        if (e.causa != CausaProteccion.NINGUNA) return false
        return when (e.fase) {
            FaseProteccion.SIN_SNAPSHOT -> !e.snapshot && e.lectura == LecturaProteccion(false, false, false, false)
            FaseProteccion.ACTIVA -> e.snapshot && e.lectura?.let {
                it.uninstallEfectivo && it.controlPropio && it.controlEfectivo
            } == true // consultar ya verificó snapshot y contribuciones propias/globales.
            else -> false // Parcial, corrupta o incierta: no se interpreta como desactivada.
        }
    }
    fun aplicar(): EstadoProteccion = intentar {
        autorizar(); puerto.validar()
        exigirProteccion(archivo.leer() == null, CausaProteccion.BASELINE)
        val s = puerto.leer()
        exigirProteccion(!s.uninstallEfectivo && s.controlEfectivo == (s.controlPropio || s.controlGlobalPropio), CausaProteccion.BASELINE)
        archivo.escribir(CodecProteccion.codificar(s))
        exigirProteccion(snapshot() == s, CausaProteccion.SNAPSHOT)
        exigirProteccion(puerto.leer() == s, CausaProteccion.AJENA)
        autorizar(); puerto.uninstall(true)
        comparar(s, puerto.leer())
        autorizar(); puerto.control(true)
        val l = puerto.leer(); comparar(s, l)
        exigirProteccion(l.uninstallEfectivo && l.controlPropio && l.controlEfectivo, CausaProteccion.READBACK)
        EstadoProteccion(FaseProteccion.ACTIVA, lectura = l, snapshot = true)
    }
    fun restaurar(): EstadoProteccion = intentar {
        autorizar(); puerto.validar(); val s = snapshot(); comparar(s, puerto.leer())
        // Solo retirar nuestra contribución; nunca clear global ni restricciones de otro admin.
        autorizar(); puerto.uninstall(false)
        comparar(s, puerto.leer())
        autorizar(); puerto.control(s.controlPropio)
        val l = puerto.leer()
        exigirProteccion(l == s, CausaProteccion.READBACK)
        archivo.borrar(); exigirProteccion(archivo.leer() == null, CausaProteccion.SNAPSHOT)
        EstadoProteccion(FaseProteccion.RESTAURADA, lectura = l)
    }
    private fun comparar(s: LecturaProteccion, l: LecturaProteccion) {
        exigirProteccion(l.controlGlobalPropio == s.controlGlobalPropio &&
            (l.controlPropio == s.controlPropio || l.controlPropio) &&
            l.controlEfectivo == (l.controlPropio || l.controlGlobalPropio), CausaProteccion.AJENA)
    }
    private fun intentar(accion: () -> EstadoProteccion): EstadoProteccion = try { accion() }
        catch (e: Exception) {
            EstadoProteccion(FaseProteccion.ERROR, (e as? FalloProteccion)?.causa ?: CausaProteccion.ANDROID_IO,
                try { puerto.leer() } catch (_: Exception) { null },
                try { archivo.leer() != null } catch (_: Exception) { true })
        }
}
