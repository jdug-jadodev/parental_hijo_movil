package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import java.security.MessageDigest
import java.security.Signature

enum class RolAuthCp2 { PARENT, CHILD }
sealed interface FrameAuthCp2
data class RetoAuthCp2(val connectionId: String, val nonce: String) : FrameAuthCp2
data class OkAuthCp2(val connectionId: String) : FrameAuthCp2
enum class TipoLatidoCp2 { PING, PONG }
data class LatidoAuthCp2(val kind: TipoLatidoCp2, val nonce: String) : FrameAuthCp2
data class PruebaAuthCp2(val pairId: String, val role: RolAuthCp2, val connectionId: String, val nonce: String)
data class SobreAuthCp2(val payloadB64: String, val sigB64: String) : FrameAuthCp2
interface FirmanteAuthCp2 {
    val publica: String
    fun firmar(bytes: ByteArray): ByteArray
}
class AuthVerificadaCp2 internal constructor(val prueba: PruebaAuthCp2, bytes: ByteArray) {
    private val originales = bytes.copyOf()
    val payloadSha256B64 = Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(originales))
    fun bytesPayload() = originales.copyOf()
}

/** Perfil AUTH aislado: ni CodecCp1 ni selección alg/clave desde el mensaje. */
object CodecAuthCp2 {
    const val MAX_PAYLOAD = 8192
    const val MAX_FRAME = 16384
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private fun id(s: String) { require(uuid.matches(s)) }
    private fun objeto(bytes: ByteArray, maximo: Int): ObjetoJson {
        CodecPairCp2.preflight(bytes, maximo) // Primitiva técnica ASCII/depth6 antes del parser.
        val o = JsonCp1.leer(bytes, maximo) as? ObjetoJson ?: error("AUTH requiere objeto")
        require(bytes.contentEquals(JsonCp1.serializar(o)))
        return o
    }
    private fun ObjetoJson.t(k: String) = (campos[k] as? TextoJson)?.texto ?: error("Tipo AUTH inválido")
    private fun ObjetoJson.cerrado(vararg claves: String) { require(campos.keys == claves.toSet()); require(campos["v"] == EnteroJson(2)) }
    private fun t(s: String) = TextoJson(s)
    private fun o(vararg campos: Pair<String, ValorJson>) = ObjetoJson(mapOf("v" to EnteroJson(2), *campos))
    fun escribirPrueba(p: PruebaAuthCp2): ByteArray {
        id(p.pairId); id(p.connectionId); CodecPairCp2.hash32(p.nonce)
        return JsonCp1.serializar(o("kind" to t("AUTH_PROOF"), "protocol" to t("cp.v2"),
            "pairId" to t(p.pairId), "role" to t(p.role.name), "connectionId" to t(p.connectionId), "nonce" to t(p.nonce)))
            .also { require(it.size <= MAX_PAYLOAD) }
    }
    fun leerPrueba(bytes: ByteArray): PruebaAuthCp2 {
        val o = objeto(bytes, MAX_PAYLOAD)
        o.cerrado("v", "kind", "pairId", "role", "connectionId", "nonce", "protocol")
        require(o.t("kind") == "AUTH_PROOF" && o.t("protocol") == "cp.v2")
        return PruebaAuthCp2(o.t("pairId"), RolAuthCp2.valueOf(o.t("role")), o.t("connectionId"), o.t("nonce"))
            .also { escribirPrueba(it) }
    }
    fun escribir(frame: FrameAuthCp2): ByteArray {
        val o = when (frame) {
            is RetoAuthCp2 -> {
                id(frame.connectionId); CodecPairCp2.hash32(frame.nonce)
                o("kind" to t("CHALLENGE"), "connectionId" to t(frame.connectionId), "nonce" to t(frame.nonce), "timeoutSec" to EnteroJson(10))
            }
            is OkAuthCp2 -> { id(frame.connectionId); o("kind" to t("AUTH_OK"), "connectionId" to t(frame.connectionId), "protocol" to t("cp.v2")) }
            is LatidoAuthCp2 -> { CodecPairCp2.hash32(frame.nonce); o("kind" to t(frame.kind.name), "nonce" to t(frame.nonce)) }
            is SobreAuthCp2 -> {
                require(frame.payloadB64.length in 1..10923 && frame.sigB64.length in 11..96)
                leerPrueba(Base64Cp1.decodificar(frame.payloadB64, MAX_PAYLOAD))
                DerEcdsaCp1.validar(Base64Cp1.decodificar(frame.sigB64, 72))
                o("kind" to t("SIGNED"), "purpose" to t("AUTH"), "payloadB64" to t(frame.payloadB64), "sigB64" to t(frame.sigB64))
            }
        }
        return JsonCp1.serializar(o).also { require(it.size <= MAX_FRAME) }
    }
    fun leer(bytes: ByteArray): FrameAuthCp2 {
        val o = objeto(bytes, MAX_FRAME)
        val f = when (o.t("kind")) {
            "CHALLENGE" -> { o.cerrado("v", "kind", "connectionId", "nonce", "timeoutSec"); require(o.campos["timeoutSec"] == EnteroJson(10)); RetoAuthCp2(o.t("connectionId"), o.t("nonce")) }
            "AUTH_OK" -> { o.cerrado("v", "kind", "connectionId", "protocol"); require(o.t("protocol") == "cp.v2"); OkAuthCp2(o.t("connectionId")) }
            "PING", "PONG" -> { o.cerrado("v", "kind", "nonce"); LatidoAuthCp2(TipoLatidoCp2.valueOf(o.t("kind")), o.t("nonce")) }
            "SIGNED" -> { o.cerrado("v", "kind", "purpose", "payloadB64", "sigB64"); require(o.t("purpose") == "AUTH"); SobreAuthCp2(o.t("payloadB64"), o.t("sigB64")) }
            else -> error("Frame AUTH no previsto")
        }
        escribir(f)
        return f
    }
}

object CriptografiaAuthCp2 {
    private fun prefijo() = "CPv2/AUTH\n".toByteArray(Charsets.UTF_8)
    fun verificar(bytes: ByteArray, publicaEsperada: String, rolEsperado: RolAuthCp2): AuthVerificadaCp2 {
        val s = CodecAuthCp2.leer(bytes) as? SobreAuthCp2 ?: error("Se requiere firma AUTH")
        val p = Base64Cp1.decodificar(s.payloadB64, CodecAuthCp2.MAX_PAYLOAD)
        val prueba = CodecAuthCp2.leerPrueba(p)
        require(prueba.role == rolEsperado)
        val firma = Base64Cp1.decodificar(s.sigB64, 72)
        val valida = Signature.getInstance("SHA256withECDSA").run {
            initVerify(ClavePublicaCp1.leer(publicaEsperada).clave); update(prefijo()); update(p); verify(firma)
        }
        require(valida) { "Firma AUTH inválida" }
        return AuthVerificadaCp2(prueba, p)
    }
    /** Emisión MAIN exclusivamente CHILD. El puerto nunca exporta privada. */
    fun firmarHijo(p: PruebaAuthCp2, publicaEsperada: String, firmante: FirmanteAuthCp2): ByteArray {
        require(p.role == RolAuthCp2.CHILD && firmante.publica == publicaEsperada)
        ClavePublicaCp1.leer(publicaEsperada)
        val bytes = CodecAuthCp2.escribirPrueba(p)
        val firma = firmante.firmar(prefijo() + bytes)
        require(firmante.publica == publicaEsperada)
        return CodecAuthCp2.escribir(SobreAuthCp2(Base64Cp1.codificar(bytes), Base64Cp1.codificar(firma))).also {
            verificar(it, publicaEsperada, RolAuthCp2.CHILD)
        }
    }
}
