package dev.controlparental.child.orders

import dev.controlparental.protocol.cp2.*
import java.io.*
import java.security.MessageDigest
import java.util.UUID

internal enum class EfectoControl { PENDING, VERIFIED, FAILED, UNKNOWN }
internal enum class ResultadoControl { ACCEPTED_PENDING_ENFORCEMENT, APPLIED, ENFORCEMENT_FAILED }
internal data class DecisionControl(val revision: Long, val action: AccionOrdenCp2, val local: Boolean,
    val offline: Boolean, val accepted: Long, val deadline: Long?, val efecto: EfectoControl)
internal data class RemotoControl(val orden: PoliticaOrdenCp2, val hash: String, val frame: ByteArray,
    val acceptedRevision: Long, val resultado: ResultadoControl, val resultElapsed: Long?)
internal data class RegistroControl(val pareja: String, val boot: Int, val bootId: String, val sesion: String,
    val revision: Long, val seq: Long, val decision: DecisionControl?, val remoto: RemotoControl?)
internal interface ArchivosControl {
    fun leer(): ByteArray?
    fun reserva(): ByteArray?
    fun iniciar(bytes: ByteArray)
    fun reservar(bytes: ByteArray)
    fun escribir(bytes: ByteArray)
    fun terminarReserva()
}

/** Registro CE propio: no permiso restaurable. Frames firmados retenidos sólo para comparar duplicados exactos. */
internal object CodecControlDurable {
    const val MAX = 32768
    fun escribir(r: RegistroControl): ByteArray {
        require(r.revision > 0 && r.seq >= 0 && r.boot >= 0)
        val salida = ByteArrayOutputStream()
        DataOutputStream(salida).use { o ->
            o.writeInt(1); o.writeUTF(r.pareja); o.writeInt(r.boot); o.writeUTF(r.bootId); o.writeUTF(r.sesion)
            o.writeLong(r.revision); o.writeLong(r.seq); o.writeBoolean(r.decision != null)
            r.decision?.let { d ->
                require(d.revision in 1..r.revision && d.accepted >= 0 && (!d.offline || d.local))
                require(if (d.action == AccionOrdenCp2.ALLOW_FOR) d.deadline != null && d.deadline > d.accepted else d.deadline == null)
                o.writeLong(d.revision); o.writeUTF(d.action.name); o.writeBoolean(d.local); o.writeBoolean(d.offline)
                o.writeLong(d.accepted); o.writeLong(d.deadline ?: -1); o.writeUTF(d.efecto.name)
            }
            o.writeBoolean(r.remoto != null)
            r.remoto?.let { m ->
                require(m.orden.seq == r.seq && m.acceptedRevision in 1..r.revision)
                require((m.resultado == ResultadoControl.ACCEPTED_PENDING_ENFORCEMENT) == (m.resultElapsed == null))
                require(m.frame.size in 1..16384)
                o.writeUTF(m.hash); o.writeInt(m.frame.size); o.write(m.frame); o.writeLong(m.acceptedRevision)
                o.writeUTF(m.resultado.name); o.writeLong(m.resultElapsed ?: -1)
            }
            require(r.seq == 0L || r.remoto != null)
        }
        val bytes = salida.toByteArray(); require(bytes.size + 32 <= MAX)
        return bytes + MessageDigest.getInstance("SHA-256").digest(bytes)
    }
    fun leer(bytes: ByteArray): RegistroControl {
        require(bytes.size in 33..MAX)
        val cuerpo = bytes.copyOfRange(0, bytes.size - 32)
        require(MessageDigest.isEqual(bytes.copyOfRange(bytes.size - 32, bytes.size), MessageDigest.getInstance("SHA-256").digest(cuerpo)))
        return DataInputStream(ByteArrayInputStream(cuerpo)).use { i ->
            require(i.readInt() == 1)
            val pareja = i.readUTF(); val boot = i.readInt(); val id = i.readUTF(); val sesion = i.readUTF()
            val revision = i.readLong(); val seq = i.readLong()
            val decision = if (!i.readBoolean()) null else DecisionControl(i.readLong(), AccionOrdenCp2.valueOf(i.readUTF()),
                i.readBoolean(), i.readBoolean(), i.readLong(), i.readLong().takeIf { it != -1L }, EfectoControl.valueOf(i.readUTF()))
            val remoto = if (!i.readBoolean()) null else {
                val hash = i.readUTF(); val n = i.readInt(); require(n in 1..16384)
                val frame = ByteArray(n); i.readFully(frame)
                val sobre = CodecOrdenCp2.leerSobre(frame)
                val orden = CodecOrdenCp2.leerPayload(dev.controlparental.protocol.Base64Cp1.decodificar(sobre.payloadB64, 8192)) as PoliticaOrdenCp2
                RemotoControl(orden, hash, frame, i.readLong(), ResultadoControl.valueOf(i.readUTF()), i.readLong().takeIf { it != -1L })
            }
            require(i.available() == 0)
            RegistroControl(pareja, boot, id, sesion, revision, seq, decision, remoto).also { require(escribir(it).contentEquals(bytes)) }
        }
    }
}

/** Se usa EXCLUSIVAMENTE en el actor ControlRedAndroid. Toda intención durable precede al efecto Android. */
internal class EscritorControlCp2(private val archivos: ArchivosControl, private val ahora: () -> Long,
    private val pareja: () -> ParejaOrdenCp2?, private val falloCerrado: () -> Unit,
    private val id: () -> String = { UUID.randomUUID().toString() }) {
    private var registro: RegistroControl? = null
    private var bytesGuardados: ByteArray? = null
    private var incierto = false
    private var ultimo = -1L
    private var enPuerto = false
    val disponible get() = registro != null && !incierto
    fun actual(): RegistroControl { comprobar(); return registro!! }
    private fun huella(p: ParejaOrdenCp2) = p.pairId + ":" + p.revisionRegistro + ":" + p.publicaHijo + ":" + p.publicaPadre
    fun abrir(autoridadInicial: () -> Boolean): Boolean = seguro {
        check(registro == null && !incierto)
        val p = pareja() ?: error("Pareja no disponible")
        check(p.boot >= 0); val n = ahora(); check(n >= 0)
        check(archivos.reserva() == null)
        val bytes = archivos.leer()
        if (bytes == null) {
            check(autoridadInicial() && pareja() == p)
            val r = RegistroControl(huella(p), p.boot, id(), id(), 1, 0, null, null)
            val b = CodecControlDurable.escribir(r); archivos.iniciar(b)
            check(archivos.leer()?.contentEquals(b) == true && archivos.reserva() == null)
            registro = r; bytesGuardados = b
        } else {
            val previo = CodecControlDurable.leer(bytes)
            check(previo.pareja == huella(p))
            previo.remoto?.let {
                val v = CriptografiaOrdenCp2.verificar(it.frame, p.publicaPadre)
                check(v.payloadSha256B64 == it.hash && v.mensaje == it.orden && it.orden.pairId == p.pairId)
            }
            registro = previo; bytesGuardados = bytes
            // Incluso mismo boot: nueva sesión de proceso, sin restaurar decisión/permiso RAM.
            guardar(previo.copy(boot = p.boot, bootId = if (previo.boot == p.boot) previo.bootId else id(),
                sesion = id(), revision = Math.addExact(previo.revision, 1), decision = null))
        }
        ultimo = n
    }
    private fun comprobar() {
        check(disponible && !enPuerto)
        val r = registro!!; val p = pareja() ?: error("Snapshot incierto")
        check(r.pareja == huella(p) && r.boot == p.boot && ahora() >= ultimo)
        check(archivos.reserva() == null && archivos.leer()?.contentEquals(bytesGuardados!!) == true)
        ultimo = ahora()
    }
    private fun guardar(nuevo: RegistroControl) {
        val b = CodecControlDurable.escribir(nuevo)
        enPuerto = true
        try {
            archivos.reservar(b); check(archivos.reserva()?.contentEquals(b) == true)
            archivos.escribir(b); check(archivos.leer()?.contentEquals(b) == true)
            archivos.terminarReserva(); check(archivos.reserva() == null)
            registro = nuevo; bytesGuardados = b
        } finally { enPuerto = false }
    }
    fun contexto(): ControlOrdenCp2? = try {
        comprobar(); val r = registro!!
        ControlOrdenCp2(r.boot, r.bootId, r.sesion, r.revision, r.seq)
    } catch (_: Exception) { invalidar(); null }
    fun local(action: AccionOrdenCp2, duracionSec: Long?, offline: Boolean): DecisionControl? = decidir(action, duracionSec, true, offline, null)
    fun remoto(p: PreparacionOrdenCp2): DecisionControl? {
        val r = registro ?: return null; val o = p.orden
        if (contexto() != p.reto.control || p.reto.referencia.pareja != pareja() || ahora() >= p.reto.expiresElapsedMs ||
            o.bootId != r.bootId || o.childSessionId != r.sesion || o.seq <= r.seq || o.expectedRevision != r.revision ||
            o.commandNonce != p.reto.nonce || o.action != p.reto.action) return null
        return decidir(o.action, o.durationSec, false, false, p.verificada)
    }
    private fun decidir(action: AccionOrdenCp2, duracion: Long?, local: Boolean, offline: Boolean,
        v: OrdenVerificadaCp2?): DecisionControl? {
        var d: DecisionControl? = null
        if (!seguro {
            comprobar(); val r = registro!!; val n = ahora()
            require(if (action == AccionOrdenCp2.ALLOW_FOR) duracion != null && duracion in 60..86400 && duracion % 60 == 0L else duracion == null)
            require(!offline || local)
            d = DecisionControl(Math.addExact(r.revision, 1), action, local, offline, n,
                duracion?.let { Math.addExact(n, Math.multiplyExact(it, 1000)) }, EfectoControl.PENDING)
            val remoto = v?.let { RemotoControl(it.mensaje as PoliticaOrdenCp2, it.payloadSha256B64, it.bytesFrame(), d!!.revision,
                ResultadoControl.ACCEPTED_PENDING_ENFORCEMENT, null) } ?: r.remoto
            guardar(r.copy(revision = d!!.revision, seq = remoto?.orden?.seq ?: r.seq, decision = d, remoto = remoto))
        }) return null
        return d
    }
    /** Revocación observada en UsoRed: queda durable antes de solicitar reentrada de bloqueo. */
    fun revocar(): Boolean = seguro {
        comprobar(); val r = registro!!
        if (r.decision?.action != null && r.decision?.action != AccionOrdenCp2.LOCK) {
            guardar(r.copy(revision = Math.addExact(r.revision, 1), decision = null))
        }
    }
    fun confirmar(revision: Long, exito: Boolean): Boolean = seguro {
        comprobar(); val r = registro!!; val d = r.decision
        if (d == null || d.revision != revision || r.revision != revision || d.efecto != EfectoControl.PENDING) return@seguro
        val m = r.remoto?.takeIf { !d.local && it.acceptedRevision == revision }
        guardar(r.copy(decision = d.copy(efecto = if (exito) EfectoControl.VERIFIED else EfectoControl.FAILED),
            remoto = if (m == null) r.remoto else m.copy(resultado = if (exito) ResultadoControl.APPLIED else ResultadoControl.ENFORCEMENT_FAILED, resultElapsed = ahora())))
    }
    fun retenida(v: OrdenVerificadaCp2): RemotoControl? {
        if (contexto() == null) return null
        val m = registro!!.remoto ?: return null
        return m.takeIf { it.orden == v.mensaje && it.hash == v.payloadSha256B64 && it.frame.contentEquals(v.bytesFrame()) }
    }
    private fun invalidar() { incierto = true; falloCerrado() }
    private fun seguro(a: () -> Unit): Boolean = try { check(!incierto && !enPuerto); a(); true }
        catch (_: Exception) { invalidar(); false }
}
