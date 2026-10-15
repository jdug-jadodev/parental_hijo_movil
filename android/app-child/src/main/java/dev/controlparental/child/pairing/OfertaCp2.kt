package dev.controlparental.child.pairing

import dev.controlparental.protocol.*
import java.security.SecureRandom
import java.util.UUID

/** Documento óptico sin firma ni autoridad; separado de CodecCp1. */
internal data class OfertaCp2(val childPublicKeyB64: String, val enrollId: String, val enrollNonce: String)

internal object CodecOfertaCp2 {
    const val MAX_PAYLOAD = 8192
    const val MAX_FRAME = 16384
    const val MAX_PROFUNDIDAD = 6
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val campos = setOf("childPublicKeyB64", "enrollId", "enrollNonce", "kind", "protocol", "v")

    fun validar(o: OfertaCp2) {
        ClavePublicaCp1.leer(o.childPublicKeyB64) // Primitiva SPKI exacta, no perfil/firma CP/1.
        require(uuid.matches(o.enrollId))
        require(o.enrollNonce.length == 43 && Base64Cp1.decodificar(o.enrollNonce, 32).size == 32)
    }

    fun escribir(o: OfertaCp2): ByteArray {
        validar(o)
        return JsonCp1.serializar(ObjetoJson(mapOf(
            "childPublicKeyB64" to TextoJson(o.childPublicKeyB64), "enrollId" to TextoJson(o.enrollId),
            "enrollNonce" to TextoJson(o.enrollNonce), "kind" to TextoJson("CHILD_OFFER"),
            "protocol" to TextoJson("cp.v2"), "v" to EnteroJson(2)
        ))).also { require(it.size <= MAX_PAYLOAD && it.size <= MAX_FRAME) }
    }

    fun leer(bytes: ByteArray): OfertaCp2 {
        require(bytes.size in 1..MAX_FRAME && bytes.size <= MAX_PAYLOAD)
        // ASCII imprimible es subconjunto UTF-8 estricto; rechaza antes de decodificar.
        require(bytes.all { it.toInt() in 32..126 })
        var nivel = 0; var cadena = false; var escape = false
        bytes.forEach {
            val c = it.toInt().toChar()
            if (cadena) {
                if (escape) escape = false else if (c == '\\') escape = true else if (c == '"') cadena = false
            } else when (c) {
                '"' -> cadena = true
                '{', '[' -> { nivel++; require(nivel <= MAX_PROFUNDIDAD) }
                '}', ']' -> { nivel--; require(nivel >= 0) }
            }
        }
        require(nivel == 0 && !cadena)
        val raiz = JsonCp1.leer(bytes, MAX_PAYLOAD) as? ObjetoJson ?: error("Oferta no es objeto")
        require(raiz.campos.keys == campos)
        fun texto(nombre: String) = (raiz.campos[nombre] as? TextoJson)?.texto ?: error("Tipo inválido")
        require(raiz.campos["v"] == EnteroJson(2) && texto("kind") == "CHILD_OFFER" && texto("protocol") == "cp.v2")
        return OfertaCp2(texto("childPublicKeyB64"), texto("enrollId"), texto("enrollNonce")).also {
            require(bytes.contentEquals(escribir(it))) // Sin trim/normalización/escapes alternativos.
        }
    }
}

internal fun interface EntropiaOferta { fun bytes(cantidad: Int): ByteArray }
internal class EntropiaSegura : EntropiaOferta {
    private val random = SecureRandom()
    override fun bytes(cantidad: Int) = ByteArray(cantidad).also(random::nextBytes)
}

internal data class ContextoOferta(val boot: Int, val identidad: String)
internal class ReferenciaOferta(val generacion: Long, val contexto: ContextoOferta, val inicio: Long, val limite: Long,
    val oferta: OfertaCp2)

/** Una sola referencia RAM; el puerto de autoridad nunca es un permiso parental. */
internal class OfertasRam(private val ahora: () -> Long, private val contexto: () -> ContextoOferta?,
    private val autorizada: () -> Boolean, private val entropia: EntropiaOferta = EntropiaSegura(),
    private val vidaSesion: () -> Boolean = autorizada) {
    private var generacion = 0L
    private var actual: ReferenciaOferta? = null
    private var ultimo = -1L
    @Synchronized fun cancelar() { generacion++; actual = null }
    @Synchronized fun generar(): ReferenciaOferta {
        cancelar()
        val solicitada = generacion
        check(autorizada())
        val c = contexto() ?: error("Identidad/contexto no disponible")
        check(c.boot >= 0)
        val n = ahora(); check(n >= 0 && n >= ultimo); ultimo = n
        val b = entropia.bytes(16); require(b.size == 16)
        b[6] = ((b[6].toInt() and 15) or 64).toByte(); b[8] = ((b[8].toInt() and 63) or 128).toByte()
        fun mitad(inicio: Int): Long = (inicio until inicio + 8).fold(0L) { a, i -> (a shl 8) or (b[i].toLong() and 255) }
        val nonce = entropia.bytes(32); require(nonce.size == 32)
        val o = OfertaCp2(c.identidad, UUID(mitad(0), mitad(8)).toString(), Base64Cp1.codificar(nonce))
        CodecOfertaCp2.validar(o)
        val limite = Math.addExact(n, 300_000)
        check(autorizada() && contexto() == c && generacion == solicitada)
        val fin = ahora(); check(fin >= n && fin < limite); ultimo = fin
        return ReferenciaOferta(solicitada, c, n, limite, o).also { actual = it }
    }
    @Synchronized fun vigente(ref: ReferenciaOferta): Boolean {
        if (actual !== ref || ref.generacion != generacion) return false
        val n = ahora()
        val contextoActual = try { contexto() } catch (_: Exception) { null }
        val valida = actual === ref && ref.generacion == generacion && n >= ultimo && n >= ref.inicio &&
            n < ref.limite && vidaSesion() && contextoActual == ref.contexto
        ultimo = n
        if (!valida && actual === ref) cancelar()
        return valida
    }
    /** Un callback viejo no cancela la referencia nueva. */
    @Synchronized fun restante(ref: ReferenciaOferta): Long? = if (vigente(ref)) maxOf(0, ref.limite - ahora()) else null
}
