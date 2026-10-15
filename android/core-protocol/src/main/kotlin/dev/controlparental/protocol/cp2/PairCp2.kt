package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import java.net.URI
import java.security.MessageDigest
import java.security.Signature

/** Solo hashes declarados del PAIR; no códigos ni autoridad de recuperación. */
data class HashesPairCp2(val MAINTENANCE: String, val RECOVER_PARENT: String, val RETIRE: String)
data class AceptacionPairCp2(val pairId: String, val enrollId: String, val enrollNonce: String,
    val parentPublicKeyB64: String, val childPublicKeyB64: String, val relayOrigin: String,
    val recoveryHashes: HashesPairCp2)
data class SobrePairCp2(val payloadB64: String, val sigB64: String)

/** Integridad con una candidata explícita, NO confianza humana, frescura ni vínculo. */
class IntegridadPairCp2 internal constructor(val aceptacion: AceptacionPairCp2,
    val payloadSha256B64: String, payload: ByteArray, frame: ByteArray) {
    private val originales = payload.copyOf()
    private val sobreOriginal = frame.copyOf()
    fun bytesPayload(): ByteArray = originales.copyOf()
    fun bytesFrame(): ByteArray = sobreOriginal.copyOf()
}

/** Borde separado v2; reutiliza primitivas técnicas, nunca Codec/CriptografiaCp1. */
object CodecPairCp2 {
    const val MAX_PAYLOAD = 8192
    const val MAX_FRAME = 16384
    const val MAX_PROFUNDIDAD = 6
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val campos = setOf("v", "kind", "protocol", "pairId", "enrollId", "enrollNonce",
        "parentPublicKeyB64", "childPublicKeyB64", "relayOrigin", "recoveryHashes")
    // El objeto aceptación tiene diez campos: la lista exacta del schema suministrado.
    private val camposSobre = setOf("v", "kind", "purpose", "payloadB64", "sigB64")

    fun origenExacto(origen: String) {
        require(origen.length in 9..200 && Regex("^https://[a-z0-9][a-z0-9.-]*(?::[0-9]{1,5})?$").matches(origen))
        val uri = URI(origen)
        require(uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null &&
            uri.rawPath.isEmpty() && uri.rawQuery == null && uri.rawFragment == null &&
            uri.port in -1..65535 && uri.toASCIIString() == origen)
        // Se conserva el texto exacto, no lowerCase/trim ni quitar puerto/punto final.
    }
    fun hash32(texto: String) {
        require(texto.length == 43 && Base64Cp1.decodificar(texto, 32).size == 32)
    }
    fun huellaPublica(publica: String): String {
        ClavePublicaCp1.leer(publica)
        return Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(Base64Cp1.decodificar(publica, 150)))
    }
    private fun validar(a: AceptacionPairCp2) {
        require(uuid.matches(a.pairId) && uuid.matches(a.enrollId)); hash32(a.enrollNonce)
        ClavePublicaCp1.leer(a.parentPublicKeyB64); ClavePublicaCp1.leer(a.childPublicKeyB64)
        origenExacto(a.relayOrigin)
        hash32(a.recoveryHashes.MAINTENANCE); hash32(a.recoveryHashes.RECOVER_PARENT); hash32(a.recoveryHashes.RETIRE)
    }
    fun escribirPayload(a: AceptacionPairCp2): ByteArray {
        validar(a)
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to TextoJson("PAIR_ACCEPT"),
            "protocol" to TextoJson("cp.v2"), "pairId" to TextoJson(a.pairId), "enrollId" to TextoJson(a.enrollId),
            "enrollNonce" to TextoJson(a.enrollNonce), "parentPublicKeyB64" to TextoJson(a.parentPublicKeyB64),
            "childPublicKeyB64" to TextoJson(a.childPublicKeyB64), "relayOrigin" to TextoJson(a.relayOrigin),
            "recoveryHashes" to ObjetoJson(mapOf("MAINTENANCE" to TextoJson(a.recoveryHashes.MAINTENANCE),
                "RECOVER_PARENT" to TextoJson(a.recoveryHashes.RECOVER_PARENT), "RETIRE" to TextoJson(a.recoveryHashes.RETIRE)))
        ))).also { require(it.size <= MAX_PAYLOAD) }
    }
    /** Acotación previa al parser. Raíz objeto/array=1, solo contenedores aumentan nivel. */
    internal fun preflight(bytes: ByteArray, limite: Int) {
        require(limite in 1..MAX_FRAME && bytes.size in 1..limite && bytes.all { it.toInt() in 32..126 })
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
    }
    private fun objeto(bytes: ByteArray, limite: Int): ObjetoJson {
        preflight(bytes, limite)
        val o = JsonCp1.leer(bytes, limite) as? ObjetoJson ?: error("Se requiere objeto CP/2")
        require(bytes.contentEquals(JsonCp1.serializar(o)))
        return o
    }
    private fun ObjetoJson.texto(nombre: String) = (campos[nombre] as? TextoJson)?.texto ?: error("Tipo CP/2 inválido")
    fun leerPayload(bytes: ByteArray): AceptacionPairCp2 {
        val o = objeto(bytes, MAX_PAYLOAD); require(o.campos.keys == campos)
        require(o.campos["v"] == EnteroJson(2) && o.texto("kind") == "PAIR_ACCEPT" && o.texto("protocol") == "cp.v2")
        val h = o.campos["recoveryHashes"] as? ObjetoJson ?: error("Hashes PAIR inválidos")
        require(h.campos.keys == setOf("MAINTENANCE", "RECOVER_PARENT", "RETIRE"))
        return AceptacionPairCp2(o.texto("pairId"), o.texto("enrollId"), o.texto("enrollNonce"),
            o.texto("parentPublicKeyB64"), o.texto("childPublicKeyB64"), o.texto("relayOrigin"),
            HashesPairCp2(h.texto("MAINTENANCE"), h.texto("RECOVER_PARENT"), h.texto("RETIRE"))).also(::validar)
    }
    fun escribirSobre(s: SobrePairCp2): ByteArray {
        validarSobre(s)
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to TextoJson("SIGNED"),
            "purpose" to TextoJson("PAIR"), "payloadB64" to TextoJson(s.payloadB64), "sigB64" to TextoJson(s.sigB64))))
            .also { require(it.size <= MAX_FRAME) }
    }
    private fun validarSobre(s: SobrePairCp2) {
        require(s.payloadB64.length in 1..10923 && s.sigB64.length in 11..96)
        leerPayload(Base64Cp1.decodificar(s.payloadB64, MAX_PAYLOAD))
        DerEcdsaCp1.validar(Base64Cp1.decodificar(s.sigB64, 72)) // DER/rango P256 técnico, sin dominio v1.
    }
    fun leerSobre(bytes: ByteArray): SobrePairCp2 {
        val o = objeto(bytes, MAX_FRAME); require(o.campos.keys == camposSobre)
        require(o.campos["v"] == EnteroJson(2) && o.texto("kind") == "SIGNED" && o.texto("purpose") == "PAIR")
        return SobrePairCp2(o.texto("payloadB64"), o.texto("sigB64")).also(::validarSobre)
    }
}

object CriptografiaPairCp2 {
    /** El llamador aporta la candidata esperada INDEPENDIENTE del frame; aún no confianza. */
    fun verificarIntegridad(bytes: ByteArray, publicaPadreEsperada: String): IntegridadPairCp2 {
        require(bytes.size in 1..CodecPairCp2.MAX_FRAME) // Antes incluso de la copia defensiva.
        val frame = bytes.copyOf()
        val clave = ClavePublicaCp1.leer(publicaPadreEsperada)
        val sobre = CodecPairCp2.leerSobre(frame)
        val payload = Base64Cp1.decodificar(sobre.payloadB64, CodecPairCp2.MAX_PAYLOAD)
        val aceptacion = CodecPairCp2.leerPayload(payload)
        require(aceptacion.parentPublicKeyB64 == publicaPadreEsperada)
        val firma = Base64Cp1.decodificar(sobre.sigB64, 72)
        val valida = Signature.getInstance("SHA256withECDSA").run {
            initVerify(clave.clave)
            update("CPv2/PAIR\n".toByteArray(Charsets.UTF_8)); update(payload) // LF real y bytes originales.
            verify(firma)
        }
        require(valida) { "Firma PAIR CP/2 inválida" }
        return IntegridadPairCp2(aceptacion, Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(payload)), payload, frame)
    }
}
