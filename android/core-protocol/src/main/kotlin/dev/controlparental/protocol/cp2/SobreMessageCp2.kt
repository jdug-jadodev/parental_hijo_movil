package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*

/** Sólo estructura del sobre compartido. NO autentica rol, pareja, firma ni autoridad CONTROL. */
object SobreMessageCp2 {
    fun leerPeerUnavailable(bytes: ByteArray): String? {
        require(tipoFrame(bytes) == "PEER_UNAVAILABLE")
        val o = JsonCp1.leer(bytes, 16384) as ObjetoJson
        require(o.campos.keys == setOf("v", "kind", "reason", "refCommandId") && o.campos["v"] == EnteroJson(2) && o.campos["reason"] == TextoJson("PEER_OFFLINE"))
        return when (val v = o.campos["refCommandId"]) {
            NuloJson -> null
            is TextoJson -> v.texto.also { require(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(it)) }
            else -> error("Referencia PEER_UNAVAILABLE inválida")
        }
    }
    fun tipoFrame(bytes: ByteArray): String {
        CodecPairCp2.preflight(bytes, 16384)
        val o = JsonCp1.leer(bytes, 16384) as? ObjetoJson ?: error("Frame requiere objeto")
        require(JsonCp1.serializar(o).contentEquals(bytes))
        return (o.campos["kind"] as? TextoJson)?.texto ?: error("kind requerido")
    }
    fun leer(bytes: ByteArray): ObjetoJson {
        CodecPairCp2.preflight(bytes, 16384)
        val o = JsonCp1.leer(bytes, 16384) as? ObjetoJson ?: error("MESSAGE requiere objeto")
        require(JsonCp1.serializar(o).contentEquals(bytes))
        require(o.campos.keys == setOf("v", "kind", "purpose", "payloadB64", "sigB64"))
        require(o.campos["v"] == EnteroJson(2) && o.campos["kind"] == TextoJson("SIGNED") && o.campos["purpose"] == TextoJson("MESSAGE"))
        val p = (o.campos["payloadB64"] as? TextoJson)?.texto ?: error("Payload requerido")
        val s = (o.campos["sigB64"] as? TextoJson)?.texto ?: error("Firma requerida")
        require(p.length in 1..10923 && s.length in 11..96)
        val payload = Base64Cp1.decodificar(p, 8192)
        CodecPairCp2.preflight(payload, 8192)
        val objeto = JsonCp1.leer(payload, 8192) as? ObjetoJson ?: error("Payload requiere objeto")
        require(JsonCp1.serializar(objeto).contentEquals(payload))
        DerEcdsaCp1.validar(Base64Cp1.decodificar(s, 72))
        return objeto
    }
}
