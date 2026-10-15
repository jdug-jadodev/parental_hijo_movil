package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import java.security.Signature

enum class ResultadoAckCp2 { APPLIED, ACCEPTED_PENDING_ENFORCEMENT, ENFORCEMENT_FAILED, REJECTED, UNKNOWN_COMMAND }
enum class MotivoAckCp2 { OK, BAD_SCHEMA, BAD_SIGNATURE, WRONG_PAIR, WRONG_ROLE, UNKNOWN_VERSION, PROTOCOL_MISMATCH,
    STALE_BOOT, STALE_SESSION, STALE_NONCE, STALE_REVISION, STALE_SEQUENCE, SEQUENCE_CONFLICT, UNKNOWN_COMMAND,
    NOT_DEVICE_OWNER, NOT_PROVISIONED, BEFORE_USER_UNLOCK, STATE_CORRUPT, KEY_UNAVAILABLE, ENFORCEMENT_ERROR,
    COUNTER_EXHAUSTED, PEER_OFFLINE, RATE_LIMITED }
data class AckCp2(val pairId: String, val bootId: String, val childSessionId: String, val commandId: String,
    val seq: Long, val commandPayloadSha256B64: String, val acceptedRevision: Long?, val evaluatedRevision: Long,
    val result: ResultadoAckCp2, val reason: MotivoAckCp2, val resultElapsedMs: Long?)

/** ACKRenderK01: 14 campos; no es ACK relay ni permiso de uso. */
object CodecAckCp2 {
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val contador = Regex("^(0|[1-9][0-9]{0,18})$")
    fun validar(a: AckCp2) {
        for (id in listOf(a.pairId, a.bootId, a.childSessionId, a.commandId)) require(uuid.matches(id))
        CodecPairCp2.hash32(a.commandPayloadSha256B64)
        require(a.seq > 0 && a.evaluatedRevision >= 0)
        require(a.acceptedRevision == null || a.acceptedRevision in 1..a.evaluatedRevision)
        require(a.resultElapsedMs == null || a.resultElapsedMs >= 0)
        when (a.result) {
            ResultadoAckCp2.APPLIED -> require(a.reason == MotivoAckCp2.OK && a.acceptedRevision != null && a.resultElapsedMs != null)
            ResultadoAckCp2.ACCEPTED_PENDING_ENFORCEMENT -> require(a.reason == MotivoAckCp2.OK && a.acceptedRevision != null && a.resultElapsedMs == null)
            ResultadoAckCp2.ENFORCEMENT_FAILED -> require(a.reason == MotivoAckCp2.ENFORCEMENT_ERROR && a.acceptedRevision != null && a.resultElapsedMs != null)
            ResultadoAckCp2.REJECTED -> require(a.reason !in setOf(MotivoAckCp2.OK, MotivoAckCp2.UNKNOWN_COMMAND) && a.acceptedRevision == null && a.resultElapsedMs == null)
            ResultadoAckCp2.UNKNOWN_COMMAND -> require(a.reason == MotivoAckCp2.UNKNOWN_COMMAND && a.acceptedRevision == null && a.resultElapsedMs == null)
        }
    }
    private fun c(n: Long?): ValorJson = n?.let { TextoJson(it.toString()) } ?: NuloJson
    fun escribir(a: AckCp2): ByteArray {
        validar(a)
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to TextoJson("ACK"), "sender" to TextoJson("CHILD"),
            "pairId" to TextoJson(a.pairId), "bootId" to TextoJson(a.bootId), "childSessionId" to TextoJson(a.childSessionId),
            "commandId" to TextoJson(a.commandId), "seq" to c(a.seq), "commandPayloadSha256B64" to TextoJson(a.commandPayloadSha256B64),
            "acceptedRevision" to c(a.acceptedRevision), "evaluatedRevision" to c(a.evaluatedRevision), "result" to TextoJson(a.result.name),
            "reason" to TextoJson(a.reason.name), "resultElapsedMs" to c(a.resultElapsedMs)))).also { require(it.size <= 8192) }
    }
    fun leer(bytes: ByteArray): AckCp2 {
        CodecPairCp2.preflight(bytes, 8192)
        val o = JsonCp1.leer(bytes, 8192) as? ObjetoJson ?: error("ACK requiere objeto")
        require(JsonCp1.serializar(o).contentEquals(bytes))
        require(o.campos.keys == setOf("v", "kind", "sender", "pairId", "bootId", "childSessionId", "commandId", "seq", "commandPayloadSha256B64",
            "acceptedRevision", "evaluatedRevision", "result", "reason", "resultElapsedMs"))
        fun t(k: String) = (o.campos[k] as? TextoJson)?.texto ?: error("Texto ACK requerido")
        fun n(k: String): Long? = if (o.campos[k] === NuloJson) null else t(k).let { require(contador.matches(it)); it.toLongOrNull() ?: error("Contador ACK fuera de Long") }
        require(o.campos["v"] == EnteroJson(2) && t("kind") == "ACK" && t("sender") == "CHILD")
        return AckCp2(t("pairId"), t("bootId"), t("childSessionId"), t("commandId"), n("seq")!!, t("commandPayloadSha256B64"), n("acceptedRevision"),
            n("evaluatedRevision")!!, ResultadoAckCp2.valueOf(t("result")), MotivoAckCp2.valueOf(t("reason")), n("resultElapsedMs")).also(::validar)
    }
}
object CriptografiaAckCp2 {
    fun firmar(a: AckCp2, publicaEsperada: String, firmante: FirmanteAuthCp2): ByteArray {
        require(firmante.publica == publicaEsperada)
        val p = CodecAckCp2.escribir(a); val firma = firmante.firmar("CPv2/MESSAGE\n".toByteArray(Charsets.UTF_8) + p)
        require(firmante.publica == publicaEsperada)
        return JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to TextoJson("SIGNED"), "purpose" to TextoJson("MESSAGE"),
            "payloadB64" to TextoJson(Base64Cp1.codificar(p)), "sigB64" to TextoJson(Base64Cp1.codificar(firma))))).also { verificar(it, publicaEsperada) }
    }
    fun verificar(frame: ByteArray, publicaHijoEsperada: String): AckCp2 {
        SobreMessageCp2.leer(frame)
        val o = JsonCp1.leer(frame, 16384) as ObjetoJson
        val p = Base64Cp1.decodificar((o.campos["payloadB64"] as TextoJson).texto, 8192)
        val firma = Base64Cp1.decodificar((o.campos["sigB64"] as TextoJson).texto, 72)
        require(Signature.getInstance("SHA256withECDSA").run { initVerify(ClavePublicaCp1.leer(publicaHijoEsperada).clave)
            update("CPv2/MESSAGE\n".toByteArray(Charsets.UTF_8)); update(p); verify(firma) })
        return CodecAckCp2.leer(p)
    }
}
