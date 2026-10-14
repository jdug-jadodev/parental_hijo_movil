package dev.controlparental.child.lock

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

internal data class SnapshotLockLocal(val paquetes: Set<String>, val features: Int)
internal interface ArchivoSnapshotLocal {
    fun leer(): ByteArray?
    fun escribir(bytes: ByteArray)
    fun borrar()
}
/** Checksum detecta corrupción accidental, no autenticación frente a root/rollback. */
internal object CodecSnapshotLocal {
    const val MAX = 40_000
    fun codificar(s: SnapshotLockLocal): ByteArray {
        validar(s)
        val contenido = ByteArrayOutputStream().also { b -> DataOutputStream(b).use { d ->
            d.writeInt(1); d.writeInt(s.features); d.writeInt(s.paquetes.size)
            s.paquetes.sorted().forEach { d.writeUTF(it) }
        } }.toByteArray()
        return contenido + MessageDigest.getInstance("SHA-256").digest(contenido)
    }
    fun decodificar(bytes: ByteArray): SnapshotLockLocal {
        exigirLocal(bytes.size in 44..MAX, CausaLocal.SNAPSHOT)
        val contenido = bytes.copyOfRange(0, bytes.size - 32)
        exigirLocal(MessageDigest.isEqual(bytes.copyOfRange(bytes.size - 32, bytes.size),
            MessageDigest.getInstance("SHA-256").digest(contenido)), CausaLocal.SNAPSHOT)
        return try { DataInputStream(ByteArrayInputStream(contenido)).use { d ->
            exigirLocal(d.readInt() == 1, CausaLocal.SNAPSHOT)
            val features = d.readInt(); val n = d.readInt(); exigirLocal(n in 0..128, CausaLocal.SNAPSHOT)
            val lista = List(n) { d.readUTF() }; exigirLocal(lista.toSet().size == n && d.available() == 0, CausaLocal.SNAPSHOT)
            SnapshotLockLocal(lista.toSet(), features).also(::validar)
        } } catch (_: Exception) { throw FalloLocal(CausaLocal.SNAPSHOT) }
    }
    private fun validar(s: SnapshotLockLocal) {
        exigirLocal(s.paquetes.size <= 128 && s.paquetes.all {
            it.length in 1..255 && it.all { c -> c.isLetterOrDigit() || c in "._" }
        }, CausaLocal.SNAPSHOT)
    }
}

internal interface PoliticasLockLocal {
    val paquete: String
    val featuresPropios: Int
    fun validar()
    fun leerPaquetes(): Set<String>
    fun leerFeatures(): Int
    fun paquetes(valor: Set<String>)
    fun features(valor: Int)
    fun modo(): ModoLocal
    fun modoRestauracion(): ModoLocal = modo()
    fun iniciar()
}
/** Persistencia antes de writes y compare actual contra baseline/propio, nunca defaults. */
internal class AdaptadorLockLocal(private val archivo: ArchivoSnapshotLocal,
    private val politicas: PoliticasLockLocal) : PuertoLockLocal {
    override fun preparar() {
        politicas.validar()
        exigirLocal(archivo.leer() == null && politicas.modo() == ModoLocal.NONE, CausaLocal.BASELINE)
        val s = SnapshotLockLocal(politicas.leerPaquetes(), politicas.leerFeatures())
        exigirLocal(politicas.paquete !in s.paquetes, CausaLocal.BASELINE)
        archivo.escribir(CodecSnapshotLocal.codificar(s))
        exigirLocal(snapshot() == s, CausaLocal.SNAPSHOT)
    }
    override fun aplicar() {
        val s = snapshot(); politicas.validar()
        exigirLocal(politicas.leerPaquetes() == s.paquetes && politicas.leerFeatures() == s.features, CausaLocal.POLITICA_AJENA)
        politicas.paquetes(setOf(politicas.paquete)); politicas.features(politicas.featuresPropios)
        exigirLocal(politicas.leerPaquetes() == setOf(politicas.paquete) &&
            politicas.leerFeatures() == politicas.featuresPropios, CausaLocal.IO_O_ANDROID)
    }
    override fun solicitarInicio() { politicas.validar(); politicas.iniciar() }
    override fun modo() = politicas.modo()
    override fun modoProteccion() = politicas.modoRestauracion()
    fun comprobarEntradaPreparada() {
        val s = snapshot(); politicas.validar()
        exigirLocal(politicas.paquete !in s.paquetes && politicas.modo() == ModoLocal.NONE &&
            politicas.leerPaquetes() == s.paquetes && politicas.leerFeatures() == s.features, CausaLocal.BASELINE)
    }
    /** Reboot no elimina la obligación CE ni inventa baseline: solo políticas propias exactas. */
    fun comprobarReentrada() {
        val s = snapshot(); politicas.validar()
        exigirLocal(politicas.paquete !in s.paquetes, CausaLocal.SNAPSHOT)
        exigirLocal(politicas.leerPaquetes() == setOf(politicas.paquete) &&
            politicas.leerFeatures() == politicas.featuresPropios && politicas.modo() != ModoLocal.PINNED, CausaLocal.POLITICA_AJENA)
    }
    /** LOCAL05 alterna solo entre baseline guardado y nuestras dos políticas, sin recapturar. */
    fun comprobarAlternancia() {
        val s = snapshot(); politicas.validar()
        exigirLocal(politicas.paquete !in s.paquetes && politicas.modo() != ModoLocal.PINNED, CausaLocal.BASELINE)
        val p = politicas.leerPaquetes(); val f = politicas.leerFeatures()
        exigirLocal((p == s.paquetes && f == s.features) ||
            (p == setOf(politicas.paquete) && f == politicas.featuresPropios), CausaLocal.POLITICA_AJENA)
    }
    fun aplicarGuardado() {
        comprobarAlternancia()
        if (politicas.leerPaquetes() == setOf(politicas.paquete) && politicas.leerFeatures() == politicas.featuresPropios) return
        aplicar()
    }
    fun baselineConfirmado(): Boolean {
        val s = snapshot()
        return politicas.leerPaquetes() == s.paquetes && politicas.leerFeatures() == s.features &&
            politicas.modoRestauracion() == ModoLocal.NONE
    }
    override fun restaurar() {
        val s = snapshot()
        exigirLocal(politicas.paquete !in s.paquetes, CausaLocal.SNAPSHOT)
        val p = politicas.leerPaquetes(); val f = politicas.leerFeatures()
        exigirLocal((p == s.paquetes || p == setOf(politicas.paquete)) &&
            (f == s.features || f == politicas.featuresPropios), CausaLocal.POLITICA_AJENA)
        politicas.paquetes(s.paquetes); politicas.features(s.features)
    }
    override fun restaurado(): Boolean {
        if (!baselineConfirmado()) return false
        archivo.borrar(); exigirLocal(archivo.leer() == null, CausaLocal.SNAPSHOT); return true
    }
    private fun snapshot() = archivo.leer()?.let(CodecSnapshotLocal::decodificar) ?: throw FalloLocal(CausaLocal.SNAPSHOT)
}
