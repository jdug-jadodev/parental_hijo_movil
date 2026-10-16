package dev.controlparental.child.network

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

internal enum class ValidacionRed { DESCONOCIDA, NO_VALIDADA, VALIDADA }
internal enum class UsoLocal { BLOQUEADO, NORMAL, EXCEPCION_OFFLINE, FINALIZANDO, ERROR }
internal enum class MotivoRed { INICIO, CODIGO_ADULTO, RED_10S, BLOQUEAR_ADULTO, BOOT_O_RELOJ, ENFORCEMENT, CIERRE, TIEMPO }
internal data class ObligacionRed(val generacion: Long, val boot: Int, val motivo: MotivoRed)
internal data class ReferenciaRed(val generacion: Long, val boot: Int)
internal object CodecObligacionRed {
    fun codificar(r: ObligacionRed): ByteArray {
        require(r.generacion > 0 && r.boot >= 0)
        val b = ByteArrayOutputStream().also { out -> DataOutputStream(out).use {
            it.writeInt(1); it.writeLong(r.generacion); it.writeInt(r.boot); it.writeInt(r.motivo.ordinal)
        } }.toByteArray()
        return b + MessageDigest.getInstance("SHA-256").digest(b)
    }
    fun decodificar(b: ByteArray): ObligacionRed {
        require(b.size == 52)
        require(MessageDigest.isEqual(b.copyOfRange(20, 52), MessageDigest.getInstance("SHA-256").digest(b.copyOfRange(0, 20))))
        return DataInputStream(ByteArrayInputStream(b.copyOfRange(0, 20))).use {
            require(it.readInt() == 1); val g = it.readLong(); val boot = it.readInt(); val motivo = it.readInt()
            require(motivo in MotivoRed.entries.indices)
            ObligacionRed(g, boot, MotivoRed.entries[motivo]).also { r -> require(codificar(r).contentEquals(b)) }
        }
    }
}

/** Solo obligación de bloqueo durable. Uso NORMAL/OFFLINE vive en RAM y NUNCA se recupera del archivo. */
internal class UsoRedLocal(private val marca: ArchivoSnapshotLocal, private val archivo: ArchivoSnapshotLocal,
    private val ahora: () -> Long, private val boot: () -> Int) {
    var uso = UsoLocal.BLOQUEADO; private set
    var red = ValidacionRed.DESCONOCIDA; private set
    var deadline: Long? = null; private set
    var inicioUso: Long? = null; private set
    var vencimientoUso: Long? = null; private set
    var referencia: ReferenciaRed? = null; private set
    private var ultimo = 0L
    private var revisionRed = -1L
    private var generacionObservador = -1L
    private var obligacion: ObligacionRed? = null
    val motivoRevocacion get() = obligacion?.motivo
    val permiteUso get() = uso == UsoLocal.NORMAL || uso == UsoLocal.EXCEPCION_OFFLINE
    val proximoPlazo get() = listOfNotNull(deadline, vencimientoUso).minOrNull()
    fun preparar(autoridad: () -> Boolean) = seguro {
        require(autoridad() && marca.leer() == null && archivo.leer() == null)
        val r = ObligacionRed(1, boot(), MotivoRed.INICIO); require(ahora() >= 0)
        val b = CodecObligacionRed.codificar(r); marca.escribir(b); archivo.escribir(b)
        require(leer() == r); obligacion = r; referencia = ReferenciaRed(r.generacion, r.boot)
        ultimo = ahora(); uso = UsoLocal.BLOQUEADO
    }
    fun cargar(): Boolean = seguro {
        val r = leer(); obligacion = r; referencia = ReferenciaRed(r.generacion, r.boot)
        uso = if (r.motivo == MotivoRed.CIERRE) UsoLocal.FINALIZANDO else UsoLocal.BLOQUEADO
        limpiarPlazos(); red = ValidacionRed.DESCONOCIDA
        ultimo = ahora(); require(ultimo >= 0)
        if (uso != UsoLocal.FINALIZANDO && boot() != r.boot) revocar(MotivoRed.BOOT_O_RELOJ)
    }
    private fun leer(): ObligacionRed {
        val m = marca.leer() ?: error("Marca ausente")
        val r = archivo.leer() ?: error("Registro ausente")
        val ancla = CodecObligacionRed.decodificar(m)
        require(ancla.generacion == 1L && ancla.motivo == MotivoRed.INICIO)
        return CodecObligacionRed.decodificar(r)
    }
    private fun contexto(): Boolean {
        val n = ahora(); val r = obligacion ?: return false
        if (n < ultimo || boot() < 0 || boot() != r.boot) {
            revocar(MotivoRed.BOOT_O_RELOJ); ultimo = n; return false
        }
        ultimo = n; return true
    }
    fun observar(validacionActual: ValidacionRed, observador: Long, revision: Long): Boolean = seguro {
        if (observador != generacionObservador || revision <= revisionRed) return@seguro
        revisionRed = revision
        if (!contexto()) return@seguro
        vencerSiCorresponde() // A las10s reconectar ya no cancela el latch.
        red = validacionActual
        if (uso == UsoLocal.NORMAL) {
            if (red == ValidacionRed.VALIDADA) deadline = null
            else if (deadline == null) deadline = Math.addExact(ahora(), 10_000)
        }
    }
    fun iniciarObservador(g: Long) { generacionObservador = g; revisionRed = -1; red = ValidacionRed.DESCONOCIDA }
    fun autorizar(tipo: UsoLocal, minutos: MinutosUso? = null, limiteAbsoluto: Long? = null, autoridad: () -> Boolean): Boolean {
        if (!autoridad() || tipo !in setOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE) ||
            uso in setOf(UsoLocal.ERROR, UsoLocal.FINALIZANDO) ||
            (tipo == UsoLocal.NORMAL && red != ValidacionRed.VALIDADA)) return false
        return seguro {
            require(contexto())
            val inicio = ahora(); val fin = listOfNotNull(minutos?.deadlineDesde(inicio), limiteAbsoluto).minOrNull()
            require(fin == null || fin > inicio)
            // Guardar obligación antes de abrir; no persistir inicio/plazo/permiso RAM.
            guardar(MotivoRed.CODIGO_ADULTO); uso = tipo; deadline = null
            inicioUso = inicio; vencimientoUso = fin
        }
    }
    fun comprobarDeadline(ref: ReferenciaRed): Boolean = seguro {
        if (referencia != ref || uso == UsoLocal.FINALIZANDO) return@seguro
        if (contexto()) vencerSiCorresponde()
    }
    private fun vencerSiCorresponde() {
        if (!permiteUso) return
        val n = ahora()
        val fin = vencimientoUso
        val gracia = if (uso == UsoLocal.NORMAL) deadline else null
        // Si ambos llegaron, la primera obligación cronológica gana; empate: tiempo.
        if (fin != null && n >= fin && (gracia == null || fin <= gracia)) revocar(MotivoRed.TIEMPO)
        else if (gracia != null && n >= gracia) revocar(MotivoRed.RED_10S)
    }
    fun bloquear(autoridad: () -> Boolean): Boolean = if (!autoridad()) false else seguro { revocar(MotivoRed.BLOQUEAR_ADULTO) }
    fun falloEnforcement(ref: ReferenciaRed) = seguro { if (referencia == ref) revocar(MotivoRed.ENFORCEMENT) }
    private fun revocar(motivo: MotivoRed) {
        uso = UsoLocal.BLOQUEADO; limpiarPlazos() // Ni IO fallido conserva permiso RAM.
        guardar(motivo)
    }
    private fun guardar(motivo: MotivoRed) {
        val previo = obligacion ?: error("Sin obligación")
        require(leer() == previo) // No sobrescribir corrupción/cambio ajeno con un nuevo permiso.
        val r = ObligacionRed(Math.addExact(previo.generacion, 1), boot().takeIf { it >= 0 } ?: previo.boot, motivo)
        archivo.escribir(CodecObligacionRed.codificar(r)); require(leer() == r)
        obligacion = r; referencia = ReferenciaRed(r.generacion, r.boot)
    }
    fun cerrar(autoridad: () -> Boolean): Boolean = if (!autoridad()) false else seguro {
        revocar(MotivoRed.CIERRE); uso = UsoLocal.FINALIZANDO
        generacionObservador = -1; revisionRed = -1; red = ValidacionRed.DESCONOCIDA
    }
    fun finalizar(restaurado: Boolean): Boolean = seguro {
        require(restaurado && uso == UsoLocal.FINALIZANDO)
        archivo.borrar(); marca.borrar(); require(archivo.leer() == null && marca.leer() == null)
        obligacion = null; referencia = null; uso = UsoLocal.BLOQUEADO
    }
    private fun limpiarPlazos() { deadline = null; inicioUso = null; vencimientoUso = null }
    private fun seguro(accion: () -> Unit): Boolean = try { accion(); true } catch (_: Exception) {
        uso = UsoLocal.ERROR; limpiarPlazos(); false
    }
}
