package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import java.security.MessageDigest
import java.security.Signature

enum class AccionOrdenCp2 { LOCK, ALLOW, ALLOW_FOR }
sealed interface MensajeOrdenCp2 { val pairId: String }
data class SyncOrdenCp2(override val pairId: String, val syncNonce: String, val requestedAction: AccionOrdenCp2?) : MensajeOrdenCp2
data class PoliticaOrdenCp2(override val pairId: String, val bootId: String, val childSessionId: String,
    val commandId: String, val seq: Long, val expectedRevision: Long, val commandNonce: String,
    val action: AccionOrdenCp2, val durationSec: Long?) : MensajeOrdenCp2
data class SobreOrdenCp2(val payloadB64: String, val sigB64: String)
class OrdenVerificadaCp2 internal constructor(val mensaje: MensajeOrdenCp2, payload: ByteArray, frame: ByteArray) {
    private val originales = payload.copyOf()
    private val sobre = frame.copyOf()
    val payloadSha256B64 = Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(originales))
    fun bytesPayload() = originales.copyOf()
    fun bytesFrame() = sobre.copyOf()
}

/** PARENT/MESSAGE separado de CP1/AUTH/PAIR. Sólo validación técnica, nunca permiso ni aplicación. */
object CodecOrdenCp2 {
    const val MAX_PAYLOAD = 8192
    const val MAX_FRAME = 16384
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val contador = Regex("^(0|[1-9][0-9]{0,18})$")
    private fun id(s: String) { require(uuid.matches(s)) }
    private fun objeto(bytes: ByteArray, max: Int): ObjetoJson {
        CodecPairCp2.preflight(bytes, max)
        val o = JsonCp1.leer(bytes, max) as? ObjetoJson ?: error("MESSAGE requiere objeto")
        require(bytes.contentEquals(JsonCp1.serializar(o))); return o
    }
    private fun ObjetoJson.t(k: String) = (campos[k] as? TextoJson)?.texto ?: error("Tipo MESSAGE inválido")
    private fun ObjetoJson.cerrado(vararg claves: String) { require(campos.keys == claves.toSet()); require(campos["v"] == EnteroJson(2)) }
    private fun t(s: String) = TextoJson(s)
    private fun ObjetoJson.numero(k: String, positivo: Boolean = false): Long {
        val s = t(k); require(contador.matches(s)); val n = s.toLongOrNull() ?: error("Contador fuera de Long")
        require(!positivo || n > 0); return n
    }
    fun escribirPayload(m: MensajeOrdenCp2): ByteArray {
        id(m.pairId)
        val campos = mutableMapOf<String, ValorJson>("v" to EnteroJson(2), "sender" to t("PARENT"), "pairId" to t(m.pairId))
        when (m) {
            is SyncOrdenCp2 -> {
                CodecPairCp2.hash32(m.syncNonce)
                campos += mapOf("kind" to t("SYNC_REQUEST"), "syncNonce" to t(m.syncNonce), "requestedAction" to (m.requestedAction?.let { t(it.name) } ?: NuloJson))
            }
            is PoliticaOrdenCp2 -> {
                id(m.bootId); id(m.childSessionId); id(m.commandId); CodecPairCp2.hash32(m.commandNonce)
                require(m.seq > 0 && m.expectedRevision >= 0)
                require(if (m.action == AccionOrdenCp2.ALLOW_FOR) m.durationSec != null && m.durationSec in 60..86400 && m.durationSec % 60 == 0L else m.durationSec == null)
                campos += mapOf("kind" to t("SET_POLICY"), "bootId" to t(m.bootId), "childSessionId" to t(m.childSessionId),
                    "commandId" to t(m.commandId), "seq" to t(m.seq.toString()), "expectedRevision" to t(m.expectedRevision.toString()),
                    "commandNonce" to t(m.commandNonce), "action" to t(m.action.name), "durationSec" to (m.durationSec?.let(::EnteroJson) ?: NuloJson))
            }
        }
        return JsonCp1.serializar(ObjetoJson(campos)).also { require(it.size <= MAX_PAYLOAD) }
    }
    fun leerPayload(bytes: ByteArray): MensajeOrdenCp2 {
        val o = objeto(bytes, MAX_PAYLOAD); require(o.t("sender") == "PARENT")
        val m = when (o.t("kind")) {
            "SYNC_REQUEST" -> {
                o.cerrado("v", "kind", "pairId", "sender", "syncNonce", "requestedAction")
                SyncOrdenCp2(o.t("pairId"), o.t("syncNonce"), if (o.campos["requestedAction"] === NuloJson) null else AccionOrdenCp2.valueOf(o.t("requestedAction")))
            }
            "SET_POLICY" -> {
                o.cerrado("v", "kind", "sender", "pairId", "bootId", "childSessionId", "commandId", "seq", "expectedRevision", "commandNonce", "action", "durationSec")
                val duracion = when (val d = o.campos["durationSec"]) { NuloJson -> null; is EnteroJson -> d.numero; else -> error("Duración sin coerción") }
                PoliticaOrdenCp2(o.t("pairId"), o.t("bootId"), o.t("childSessionId"), o.t("commandId"), o.numero("seq", true),
                    o.numero("expectedRevision"), o.t("commandNonce"), AccionOrdenCp2.valueOf(o.t("action")), duracion)
            }
            else -> error("Tipo MESSAGE no previsto")
        }
        escribirPayload(m); return m
    }
    fun escribirSobre(s: SobreOrdenCp2): ByteArray {
        require(s.payloadB64.length in 1..10923 && s.sigB64.length in 11..96)
        leerPayload(Base64Cp1.decodificar(s.payloadB64, MAX_PAYLOAD)); DerEcdsaCp1.validar(Base64Cp1.decodificar(s.sigB64, 72))
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to t("SIGNED"), "purpose" to t("MESSAGE"),
            "payloadB64" to t(s.payloadB64), "sigB64" to t(s.sigB64)))).also { require(it.size <= MAX_FRAME) }
    }
    fun leerSobre(bytes: ByteArray): SobreOrdenCp2 {
        val o = objeto(bytes, MAX_FRAME); o.cerrado("v", "kind", "purpose", "payloadB64", "sigB64")
        require(o.t("kind") == "SIGNED" && o.t("purpose") == "MESSAGE")
        return SobreOrdenCp2(o.t("payloadB64"), o.t("sigB64")).also { escribirSobre(it) }
    }
}
object CriptografiaOrdenCp2 {
    fun verificar(bytes: ByteArray, publicaPadreEsperada: String): OrdenVerificadaCp2 {
        require(bytes.size in 1..CodecOrdenCp2.MAX_FRAME)
        val frame = bytes.copyOf(); val s = CodecOrdenCp2.leerSobre(frame)
        val payload = Base64Cp1.decodificar(s.payloadB64, CodecOrdenCp2.MAX_PAYLOAD)
        val valida = Signature.getInstance("SHA256withECDSA").run {
            initVerify(ClavePublicaCp1.leer(publicaPadreEsperada).clave)
            update("CPv2/MESSAGE\n".toByteArray(Charsets.UTF_8)); update(payload)
            verify(Base64Cp1.decodificar(s.sigB64, 72))
        }
        require(valida) { "Firma PARENT/MESSAGE inválida" }
        return OrdenVerificadaCp2(CodecOrdenCp2.leerPayload(payload), payload, frame)
    }
}
