package dev.controlparental.child.orders

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import java.security.SecureRandom

internal data class ParejaOrdenCp2(val pairId: String, val publicaPadre: String, val publicaHijo: String,
    val revisionRegistro: String, val boot: Int)
/** Exclusivamente del escritor CONTROL CP2; NO revisión adulta/registro boot CP1 ni valores por defecto. */
internal data class ControlOrdenCp2(val boot: Int, val bootId: String, val childSessionId: String,
    val revisionControl: Long, val lastAcceptedSeq: Long)
internal interface EscritorControlOrdenCp2 {
    /** Consolida boot, expiración y revocación ANTES de devolver contexto. null = NO_DISPONIBLE. */
    fun consolidar(): ControlOrdenCp2?
    /** Sólo consulta de bytes/hash retenidos en futura transacción durable, NUNCA reaplicación. */
    fun consultarRetenida(orden: OrdenVerificadaCp2)
    /** Punto futuro de CAS/transacción durable: esta guardia NO autoriza APPLIED ni implementa dicho escritor. */
    fun entregarParaDecision(preparacion: PreparacionOrdenCp2)
}
internal class GeneracionOrdenCp2 internal constructor(val numero: Long, internal val pareja: ParejaOrdenCp2,
    internal val bootId: String, internal val childSessionId: String)
internal class RetoOrdenCp2 internal constructor(internal val referencia: GeneracionOrdenCp2,
    internal val control: ControlOrdenCp2, val generacion: Long, val nonce: String, val action: AccionOrdenCp2,
    val revision: Long, val issuedElapsedMs: Long, val expiresElapsedMs: Long)
internal class SyncConsolidadoCp2 internal constructor(val reto: RetoOrdenCp2?)
internal class PreparacionOrdenCp2 internal constructor(internal val reto: RetoOrdenCp2, val verificada: OrdenVerificadaCp2) {
    val orden get() = verificada.mensaje as PoliticaOrdenCp2
    val payloadSha256B64 get() = verificada.payloadSha256B64
}
internal enum class EntregaOrdenCp2 { ENTREGADA_AL_PUERTO_NO_APPLIED, RECHAZADA, ERROR_PUERTO }
internal fun interface VerificadorOrdenCp2 { fun verificar(bytes: ByteArray, publica: String): OrdenVerificadaCp2 }

/** Una guardia RAM previa al escritor durable. Nunca concede uso, persiste orden ni fabrica autoridad. */
internal class GuardiaOrdenCp2(private val ahora: () -> Long, private val pareja: () -> ParejaOrdenCp2?,
    private val escritor: EscritorControlOrdenCp2,
    private val entropia: () -> ByteArray = { ByteArray(32).also(SecureRandom()::nextBytes) },
    private val cripto: VerificadorOrdenCp2 = VerificadorOrdenCp2(CriptografiaOrdenCp2::verificar)) {
    private var generacion = 0L
    private var generacionSync = 0L // Etiqueta RAM, nunca revisión CONTROL ni secuencia aceptada.
    private var referencia: GeneracionOrdenCp2? = null
    private var epoca = Any()
    private var reto: RetoOrdenCp2? = null
    private var preparada: PreparacionOrdenCp2? = null
    private var observado: ControlOrdenCp2? = null
    private var ultimo = -1L
    private val emitidos = mutableSetOf<String>()
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    @Synchronized fun cerrar() { epoca = Any(); referencia = null; reto = null; preparada = null; observado = null }
    @Synchronized fun retoActual(ref: GeneracionOrdenCp2): RetoOrdenCp2? {
        vigente(ref) ?: return null
        return reto
    }
    private fun consolidado(): Pair<ParejaOrdenCp2, ControlOrdenCp2>? = try {
        val p = pareja(); val c = escritor.consolidar()
        if (p == null || c == null || pareja() != p || c.boot != p.boot || c.boot < 0 || c.revisionControl < 0 || c.lastAcceptedSeq < 0 ||
            !uuid.matches(c.bootId) || !uuid.matches(c.childSessionId)) null
        else p to c
    } catch (_: Exception) { null }
    @Synchronized fun iniciar(): GeneracionOrdenCp2? {
        cerrar(); val token = epoca; val contexto = consolidado(); val n = ahora()
        if (epoca !== token) return null
        if (contexto == null || n < 0 || n < ultimo || generacion == Long.MAX_VALUE) return null
        ultimo = n
        observado = contexto.second
        return GeneracionOrdenCp2(++generacion, contexto.first, contexto.second.bootId, contexto.second.childSessionId).also { referencia = it }
    }
    private fun vigente(ref: GeneracionOrdenCp2): ControlOrdenCp2? {
        if (referencia !== ref) return null
        val token = epoca; val c = consolidado(); val n = ahora()
        if (referencia !== ref || epoca !== token) return null
        if (c == null || c.first != ref.pareja || c.second.bootId != ref.bootId || c.second.childSessionId != ref.childSessionId || n < ultimo) {
            cerrar(); return null
        }
        val previo = observado ?: return null
        if (c.second.revisionControl < previo.revisionControl || c.second.lastAcceptedSeq < previo.lastAcceptedSeq) { cerrar(); return null }
        if (c.second != previo) { epoca = Any(); reto = null; preparada = null; observado = c.second }
        if (reto?.let { n >= it.expiresElapsedMs } == true) { epoca = Any(); reto = null; preparada = null }
        ultimo = n; return c.second
    }
    private fun verificar(ref: GeneracionOrdenCp2, bytes: ByteArray): Triple<OrdenVerificadaCp2, ControlOrdenCp2, Any>? {
        if (bytes.size !in 1..CodecOrdenCp2.MAX_FRAME) return null
        val copia = bytes.copyOf()
        val captura = synchronized(this) { val c = vigente(ref) ?: return null; epoca to c }
        val v = try { cripto.verificar(copia, ref.pareja.publicaPadre) } catch (_: Exception) { return null }
        return synchronized(this) {
            if (referencia !== ref || epoca !== captura.first) return@synchronized null
            val c = vigente(ref)
            if (epoca !== captura.first || c != captura.second || v.mensaje.pairId != ref.pareja.pairId) null else Triple(v, c, captura.first)
        }
    }
    fun recibirSync(ref: GeneracionOrdenCp2, bytes: ByteArray): SyncConsolidadoCp2? {
        val (v, control, sello) = verificar(ref, bytes) ?: return null
        val sync = v.mensaje as? SyncOrdenCp2 ?: return null
        return synchronized(this) {
            if (referencia !== ref || epoca !== sello || vigente(ref) != control || epoca !== sello) return@synchronized null
            epoca = Any(); reto = null; preparada = null // TODO SYNC válido sustituye el reto anterior, incluido null.
            if (generacionSync == Long.MAX_VALUE) return@synchronized null
            val etiqueta = ++generacionSync
            val action = sync.requestedAction ?: return@synchronized SyncConsolidadoCp2(null)
            val token = epoca; val inicio = ultimo
            val nonce = try { val b = entropia(); require(b.size == 32); Base64Cp1.codificar(b) } catch (_: Exception) { return@synchronized null }
            val fin = try { Math.addExact(inicio, 30_000) } catch (_: Exception) { return@synchronized null }
            if (epoca !== token || referencia !== ref || vigente(ref) != control || epoca !== token || ultimo >= fin || nonce in emitidos || emitidos.size >= 128) return@synchronized null
            emitidos.add(nonce)
            RetoOrdenCp2(ref, control, etiqueta, nonce, action, control.revisionControl, inicio, fin)
                .also { reto = it }.let(::SyncConsolidadoCp2)
        }
    }
    fun prepararPolitica(ref: GeneracionOrdenCp2, bytes: ByteArray): PreparacionOrdenCp2? {
        val (v, control, sello) = verificar(ref, bytes) ?: return null
        val orden = v.mensaje as? PoliticaOrdenCp2 ?: return null
        if (orden.seq <= control.lastAcceptedSeq) {
            val consultar = synchronized(this) {
                referencia === ref && epoca === sello && vigente(ref) == control && epoca === sello
            }
            if (consultar) try { escritor.consultarRetenida(v) } catch (_: Exception) { /* Sin resultado ni reto nuevo. */ }
            return null // La guardia no puede decidir si es duplicado exacto, conflicto o ya descartado.
        }
        return synchronized(this) {
            val r = reto ?: return@synchronized null
            if (referencia !== ref || epoca !== sello || preparada != null || vigente(ref) != control || epoca !== sello || reto !== r ||
                control != r.control || ultimo >= r.expiresElapsedMs || orden.bootId != ref.bootId || orden.childSessionId != ref.childSessionId ||
                orden.expectedRevision != r.revision || orden.commandNonce != r.nonce || orden.action != r.action || orden.seq <= control.lastAcceptedSeq) return@synchronized null
            PreparacionOrdenCp2(r, v).also { preparada = it }
        }
    }
    fun entregar(ref: GeneracionOrdenCp2, p: PreparacionOrdenCp2): EntregaOrdenCp2 {
        synchronized(this) {
            val token = epoca
            if (referencia !== ref || preparada !== p || reto !== p.reto || vigente(ref) != p.reto.control || epoca !== token || ultimo >= p.reto.expiresElapsedMs)
                return EntregaOrdenCp2.RECHAZADA
            epoca = Any(); reto = null; preparada = null // Consumo ANTES del puerto de decisión, incluso si falla/reentra.
        }
        return try { escritor.entregarParaDecision(p); EntregaOrdenCp2.ENTREGADA_AL_PUERTO_NO_APPLIED }
            catch (_: Exception) { EntregaOrdenCp2.ERROR_PUERTO }
    }
}
