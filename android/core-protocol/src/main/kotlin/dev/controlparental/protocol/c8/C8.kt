package dev.controlparental.protocol.c8

import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.CodecPairCp2
import dev.controlparental.protocol.cp2.FirmanteAuthCp2
import java.security.Signature

/** Sólo transporte C8/1 R1. Ningún frame implica autoridad adulta o pareja. */
class FrameC8 internal constructor(val kind: String, internal val objeto: ObjetoJson) {
    fun texto(nombre: String) = (objeto.campos[nombre] as TextoJson).texto
    fun numero(nombre: String) = (objeto.campos[nombre] as EnteroJson).numero
}
data class PruebaC8(val connectionId: String, val nonceB64: String, val publicKeyB64: String,
    val relayOrigin: String, val role: String)

object CodecC8 {
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val codigo = Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$")
    private val listas = mapOf("CHALLENGE" to setOf("connectionId", "nonceB64", "relayOrigin"),
        "AUTH" to setOf("payloadB64", "sigB64"), "READY" to emptySet(),
        "PUBLISH" to setOf("offerB64", "ttlMs"), "CODE" to setOf("code", "remainingMs"),
        "CLAIM" to setOf("code"), "OFFER" to setOf("offerB64", "remainingMs"),
        "CLAIMED" to setOf("parentPublicKeyB64", "remainingMs"), "SEND_PAIR" to setOf("pairB64"),
        "PAIR" to setOf("pairB64"), "SENT" to emptySet(), "CANCEL" to emptySet(), "UNAVAILABLE" to emptySet())
    private fun objeto(bytes: ByteArray, maximo: Int): ObjetoJson {
        CodecPairCp2.preflight(bytes, maximo)
        val o = JsonCp1.leer(bytes, maximo) as? ObjetoJson ?: error("C8 requiere objeto")
        require(JsonCp1.serializar(o).contentEquals(bytes))
        return o
    }
    fun crear(kind: String, textos: Map<String, String> = emptyMap(), numeros: Map<String, Long> = emptyMap()): FrameC8 {
        require(textos.keys.intersect(numeros.keys).isEmpty())
        val campos = mapOf("v" to EnteroJson(1), "kind" to TextoJson(kind)) +
            textos.mapValues { TextoJson(it.value) } + numeros.mapValues { EnteroJson(it.value) }
        return leer(JsonCp1.serializar(ObjetoJson(campos)))
    }
    fun escribir(frame: FrameC8): ByteArray = JsonCp1.serializar(frame.objeto)
    fun leer(bytes: ByteArray): FrameC8 {
        val o = objeto(bytes, 16384)
        val kind = (o.campos["kind"] as? TextoJson)?.texto ?: error("Falta kind C8")
        val campos = listas[kind] ?: error("Tipo C8 no previsto")
        require(o.campos.keys == campos + setOf("v", "kind") && o.campos["v"] == EnteroJson(1))
        val f = FrameC8(kind, o)
        for (k in campos) when (k) {
            "ttlMs" -> require((o.campos[k] as? EnteroJson)?.numero in 1L..120000L)
            "remainingMs" -> require((o.campos[k] as? EnteroJson)?.numero in 0L..9007199254740991L)
            "connectionId" -> require(uuid.matches(f.texto(k)))
            "nonceB64" -> CodecPairCp2.hash32(f.texto(k))
            "relayOrigin" -> CodecPairCp2.origenExacto(f.texto(k))
            "code" -> require(codigo.matches(f.texto(k)))
            "parentPublicKeyB64" -> ClavePublicaCp1.leer(f.texto(k))
            "offerB64", "pairB64" -> documento(f.texto(k))
            "payloadB64" -> leerPrueba(documento(f.texto(k)))
            "sigB64" -> DerEcdsaCp1.validar(Base64Cp1.decodificar(f.texto(k), 72))
        }
        return f
    }
    fun documento(b64: String): ByteArray = Base64Cp1.decodificar(b64, 8192).also { require(it.isNotEmpty()) }
    fun escribirPrueba(p: PruebaC8): ByteArray {
        require(uuid.matches(p.connectionId) && p.role in setOf("CHILD", "PARENT"))
        CodecPairCp2.hash32(p.nonceB64); ClavePublicaCp1.leer(p.publicKeyB64); CodecPairCp2.origenExacto(p.relayOrigin)
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(1), "kind" to TextoJson("PROOF"),
            "connectionId" to TextoJson(p.connectionId), "nonceB64" to TextoJson(p.nonceB64),
            "publicKeyB64" to TextoJson(p.publicKeyB64), "relayOrigin" to TextoJson(p.relayOrigin), "role" to TextoJson(p.role))))
    }
    fun leerPrueba(bytes: ByteArray): PruebaC8 {
        val o = objeto(bytes, 8192)
        require(o.campos.keys == setOf("v", "kind", "connectionId", "nonceB64", "publicKeyB64", "relayOrigin", "role"))
        fun t(k: String) = (o.campos[k] as? TextoJson)?.texto ?: error("Texto PROOF requerido")
        require(o.campos["v"] == EnteroJson(1) && t("kind") == "PROOF")
        return PruebaC8(t("connectionId"), t("nonceB64"), t("publicKeyB64"), t("relayOrigin"), t("role")).also { escribirPrueba(it) }
    }
}

object CriptografiaC8 {
    private fun dominio() = "C8v1/PROOF\n".toByteArray(Charsets.UTF_8)
    fun firmarHijo(p: PruebaC8, firmante: FirmanteAuthCp2): FrameC8 {
        require(p.role == "CHILD" && firmante.publica == p.publicKeyB64)
        val bytes = CodecC8.escribirPrueba(p)
        val firma = firmante.firmar(dominio() + bytes)
        require(firmante.publica == p.publicKeyB64)
        return CodecC8.crear("AUTH", mapOf("payloadB64" to Base64Cp1.codificar(bytes), "sigB64" to Base64Cp1.codificar(firma)))
            .also { verificar(it, p) }
    }
    /** El servidor debe consumir reto ANTES de llamar; esto sólo verifica bytes/clave/contexto. */
    fun verificar(f: FrameC8, esperada: PruebaC8) {
        require(f.kind == "AUTH")
        val bytes = CodecC8.documento(f.texto("payloadB64"))
        require(CodecC8.leerPrueba(bytes) == esperada)
        require(Signature.getInstance("SHA256withECDSA").run {
            initVerify(ClavePublicaCp1.leer(esperada.publicKeyB64).clave)
            update(dominio()); update(bytes); verify(Base64Cp1.decodificar(f.texto("sigB64"), 72))
        })
    }
}
