package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import java.security.Signature

enum class ModoStateCp2 { LOCKED, ALLOWED, MAINTENANCE, SETUP, UNKNOWN }
enum class MotivoStateCp2 { OK, STATE_CORRUPT, ENFORCEMENT_ERROR, KEY_UNAVAILABLE, NOT_DEVICE_OWNER, NOT_PROVISIONED,
    BEFORE_USER_UNLOCK, BOOT_AUTH_REQUIRED, SYSTEM_KEYGUARD, MAINTENANCE_ACTIVE, LOCAL_LOCK, PARENT_LOCK,
    TIMER_EXPIRED, CONTROL_UNKNOWN, NETWORK_LOST }
enum class FuenteStateCp2 { NONE, LOCAL, REMOTE }
enum class FaseStateCp2 { BLOCKED, PERMITTED, EXPIRED, NONE }
enum class EfectoStateCp2 { PENDING, VERIFIED, FAILED, UNKNOWN }
enum class ResultadoStateCp2 { APPLIED, ACCEPTED_PENDING_ENFORCEMENT, ENFORCEMENT_FAILED }
enum class MotivoResultadoCp2 { OK, ENFORCEMENT_ERROR }
data class ChallengeStateCp2(val nonce: String, val action: AccionOrdenCp2, val revision: Long,
    val issuedElapsedMs: Long, val expiresElapsedMs: Long)
data class EfectivoStateCp2(val mode: ModoStateCp2, val reason: MotivoStateCp2, val source: FuenteStateCp2,
    val policyRevision: Long?, val action: AccionOrdenCp2?, val phase: FaseStateCp2, val acceptedElapsedMs: Long?,
    val phaseStartedElapsedMs: Long?, val deadlineElapsedMs: Long?, val remainingSec: Long?, val enforcement: EfectoStateCp2)
data class RemotoStateCp2(val commandId: String, val seq: Long, val payloadSha256B64: String, val bootId: String,
    val childSessionId: String, val acceptedRevision: Long, val result: ResultadoStateCp2,
    val resultReason: MotivoResultadoCp2, val resultElapsedMs: Long?)
data class StateCp2(val pairId: String, val bootId: String, val childSessionId: String, val syncNonce: String,
    val stateSeq: Long, val sampledElapsedMs: Long, val controlRevision: Long, val lastAcceptedSeq: Long,
    val commandChallenge: ChallengeStateCp2?, val effective: EfectivoStateCp2, val lastRemote: RemotoStateCp2?,
    val networkValidated: Boolean?, val channelHealthy: Boolean?, val isDeviceOwner: Boolean?, val lockTaskActive: Boolean?)

/** STATE01-PILOTO: historia/avisos siempre null (no disponibles); no son cero ni autorización. */
object CodecStateCp2 {
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
    private val contador = Regex("^(0|[1-9][0-9]{0,18})$")
    private fun id(s: String) { require(uuid.matches(s)) }
    private fun t(s: String) = TextoJson(s)
    private fun c(n: Long?): ValorJson = n?.let { require(it >= 0); t(it.toString()) } ?: NuloJson
    private fun b(v: Boolean?): ValorJson = v?.let(::BooleanoJson) ?: NuloJson
    private fun o(vararg p: Pair<String, ValorJson>) = ObjetoJson(mapOf(*p))
    fun validar(s: StateCp2) {
        id(s.pairId); id(s.bootId); id(s.childSessionId); CodecPairCp2.hash32(s.syncNonce)
        require(s.stateSeq > 0 && s.sampledElapsedMs >= 0 && s.controlRevision >= 0 && s.lastAcceptedSeq >= 0)
        s.commandChallenge?.let { r ->
            CodecPairCp2.hash32(r.nonce)
            require(r.revision == s.controlRevision && r.issuedElapsedMs >= 0)
            require(r.expiresElapsedMs == Math.addExact(r.issuedElapsedMs, 30000) && r.issuedElapsedMs <= s.sampledElapsedMs && s.sampledElapsedMs < r.expiresElapsedMs)
        }
        val e = s.effective
        if (e.source == FuenteStateCp2.NONE) {
            require(e.phase == FaseStateCp2.NONE && e.policyRevision == null && e.action == null && e.acceptedElapsedMs == null &&
                e.phaseStartedElapsedMs == null && e.deadlineElapsedMs == null && e.remainingSec == null)
        } else {
            require(e.policyRevision != null && e.policyRevision in 0..s.controlRevision && e.action != null && e.acceptedElapsedMs != null &&
                e.acceptedElapsedMs in 0..s.sampledElapsedMs && e.phaseStartedElapsedMs == e.acceptedElapsedMs)
            when (e.action) {
                AccionOrdenCp2.LOCK -> require(e.phase == FaseStateCp2.BLOCKED && e.deadlineElapsedMs == null && e.remainingSec == null)
                AccionOrdenCp2.ALLOW -> require(e.phase == FaseStateCp2.PERMITTED && e.deadlineElapsedMs == null && e.remainingSec == null)
                AccionOrdenCp2.ALLOW_FOR -> {
                    val fin = e.deadlineElapsedMs ?: error("Plazo requerido")
                    val duracion = Math.subtractExact(fin, e.acceptedElapsedMs)
                    require(duracion in 60000..86400000 && duracion % 60000 == 0L)
                    require(e.phase == if (fin > s.sampledElapsedMs) FaseStateCp2.PERMITTED else FaseStateCp2.EXPIRED)
                    val resto = if (fin <= s.sampledElapsedMs) 0 else (fin - s.sampledElapsedMs - 1) / 1000 + 1
                    require(e.remainingSec == resto)
                }
                null -> error("Decisión incompleta")
            }
        }
        if (e.mode == ModoStateCp2.ALLOWED) require(e.enforcement == EfectoStateCp2.VERIFIED && e.phase == FaseStateCp2.PERMITTED)
        if (e.mode == ModoStateCp2.LOCKED) require(e.enforcement == EfectoStateCp2.VERIFIED && e.phase != FaseStateCp2.PERMITTED)
        s.lastRemote?.let { m ->
            id(m.commandId); id(m.bootId); id(m.childSessionId); CodecPairCp2.hash32(m.payloadSha256B64)
            require(m.seq > 0 && m.seq == s.lastAcceptedSeq && m.acceptedRevision in 1..s.controlRevision)
            require(m.resultElapsedMs == null || m.resultElapsedMs >= 0) // Otro boot/sesión puede tener otro reloj.
            when (m.result) {
                ResultadoStateCp2.ACCEPTED_PENDING_ENFORCEMENT -> require(m.resultElapsedMs == null && m.resultReason == MotivoResultadoCp2.OK)
                ResultadoStateCp2.APPLIED -> require(m.resultElapsedMs != null && m.resultReason == MotivoResultadoCp2.OK)
                ResultadoStateCp2.ENFORCEMENT_FAILED -> require(m.resultElapsedMs != null && m.resultReason == MotivoResultadoCp2.ENFORCEMENT_ERROR)
            }
        }
    }
    fun escribir(s: StateCp2): ByteArray {
        validar(s)
        val r = s.commandChallenge?.let { o("nonce" to t(it.nonce), "action" to t(it.action.name), "revision" to c(it.revision),
            "issuedElapsedMs" to c(it.issuedElapsedMs), "expiresElapsedMs" to c(it.expiresElapsedMs)) } ?: NuloJson
        val e = s.effective
        val efectivo = o("mode" to t(e.mode.name), "reason" to t(e.reason.name), "source" to t(e.source.name), "policyRevision" to c(e.policyRevision),
            "action" to (e.action?.let { t(it.name) } ?: NuloJson), "phase" to t(e.phase.name), "acceptedElapsedMs" to c(e.acceptedElapsedMs),
            "phaseStartedElapsedMs" to c(e.phaseStartedElapsedMs), "deadlineElapsedMs" to c(e.deadlineElapsedMs),
            "remainingSec" to (e.remainingSec?.let(::EnteroJson) ?: NuloJson), "enforcement" to t(e.enforcement.name))
        val remoto = s.lastRemote?.let { o("commandId" to t(it.commandId), "seq" to c(it.seq), "payloadSha256B64" to t(it.payloadSha256B64),
            "bootId" to t(it.bootId), "childSessionId" to t(it.childSessionId), "acceptedRevision" to c(it.acceptedRevision), "result" to t(it.result.name),
            "resultReason" to t(it.resultReason.name), "resultElapsedMs" to c(it.resultElapsedMs)) } ?: NuloJson
        return JsonCp1.serializar(o("v" to EnteroJson(2), "kind" to t("STATE"), "sender" to t("CHILD"), "pairId" to t(s.pairId), "bootId" to t(s.bootId),
            "childSessionId" to t(s.childSessionId), "syncNonce" to t(s.syncNonce), "stateSeq" to c(s.stateSeq), "sampledElapsedMs" to c(s.sampledElapsedMs),
            "controlRevision" to c(s.controlRevision), "lastAcceptedSeq" to c(s.lastAcceptedSeq), "commandChallenge" to r, "effective" to efectivo,
            "lastRemote" to remoto, "permittedTime" to NuloJson, "lastExpiry" to NuloJson, "networkValidated" to b(s.networkValidated),
            "channelHealthy" to b(s.channelHealthy), "isDeviceOwner" to b(s.isDeviceOwner), "lockTaskActive" to b(s.lockTaskActive)))
            .also { require(it.size <= 8192) }
    }
    private fun ObjetoJson.cerrado(vararg claves: String) { require(campos.keys == claves.toSet()) }
    private fun ObjetoJson.t(k: String) = (campos[k] as? TextoJson)?.texto ?: error("Texto STATE requerido")
    private fun ObjetoJson.n(k: String): Long? = when (val v = campos[k]) {
        NuloJson -> null
        is TextoJson -> { require(contador.matches(v.texto)); v.texto.toLongOrNull() ?: error("Contador fuera de Long") }
        else -> error("Contador STATE sin coerción")
    }
    private fun ObjetoJson.bn(k: String): Boolean? = when (val v = campos[k]) { NuloJson -> null; is BooleanoJson -> v.valor; else -> error("Booleano STATE") }
    fun leer(bytes: ByteArray): StateCp2 {
        CodecPairCp2.preflight(bytes, 8192)
        val p = JsonCp1.leer(bytes, 8192) as? ObjetoJson ?: error("STATE requiere objeto")
        require(JsonCp1.serializar(p).contentEquals(bytes))
        p.cerrado("v", "kind", "sender", "pairId", "bootId", "childSessionId", "syncNonce", "stateSeq", "sampledElapsedMs", "controlRevision", "lastAcceptedSeq",
            "commandChallenge", "effective", "lastRemote", "permittedTime", "lastExpiry", "networkValidated", "channelHealthy", "isDeviceOwner", "lockTaskActive")
        require(p.campos["v"] == EnteroJson(2) && p.t("kind") == "STATE" && p.t("sender") == "CHILD" && p.campos["permittedTime"] === NuloJson && p.campos["lastExpiry"] === NuloJson)
        val reto = when (val v = p.campos["commandChallenge"]) {
            NuloJson -> null
            is ObjetoJson -> { v.cerrado("nonce", "action", "revision", "issuedElapsedMs", "expiresElapsedMs")
                ChallengeStateCp2(v.t("nonce"), AccionOrdenCp2.valueOf(v.t("action")), v.n("revision")!!, v.n("issuedElapsedMs")!!, v.n("expiresElapsedMs")!!) }
            else -> error("Reto STATE")
        }
        val e = p.campos["effective"] as? ObjetoJson ?: error("Efectivo STATE requerido")
        e.cerrado("mode", "reason", "source", "policyRevision", "action", "phase", "acceptedElapsedMs", "phaseStartedElapsedMs", "deadlineElapsedMs", "remainingSec", "enforcement")
        val efectivo = EfectivoStateCp2(ModoStateCp2.valueOf(e.t("mode")), MotivoStateCp2.valueOf(e.t("reason")), FuenteStateCp2.valueOf(e.t("source")), e.n("policyRevision"),
            if (e.campos["action"] === NuloJson) null else AccionOrdenCp2.valueOf(e.t("action")), FaseStateCp2.valueOf(e.t("phase")), e.n("acceptedElapsedMs"),
            e.n("phaseStartedElapsedMs"), e.n("deadlineElapsedMs"), when (val v = e.campos["remainingSec"]) { NuloJson -> null; is EnteroJson -> v.numero; else -> error("Restante entero requerido") },
            EfectoStateCp2.valueOf(e.t("enforcement")))
        val remoto = when (val v = p.campos["lastRemote"]) {
            NuloJson -> null
            is ObjetoJson -> { v.cerrado("commandId", "seq", "payloadSha256B64", "bootId", "childSessionId", "acceptedRevision", "result", "resultReason", "resultElapsedMs")
                RemotoStateCp2(v.t("commandId"), v.n("seq")!!, v.t("payloadSha256B64"), v.t("bootId"), v.t("childSessionId"), v.n("acceptedRevision")!!,
                    ResultadoStateCp2.valueOf(v.t("result")), MotivoResultadoCp2.valueOf(v.t("resultReason")), v.n("resultElapsedMs")) }
            else -> error("Remoto STATE")
        }
        return StateCp2(p.t("pairId"), p.t("bootId"), p.t("childSessionId"), p.t("syncNonce"), p.n("stateSeq")!!, p.n("sampledElapsedMs")!!,
            p.n("controlRevision")!!, p.n("lastAcceptedSeq")!!, reto, efectivo, remoto, p.bn("networkValidated"), p.bn("channelHealthy"), p.bn("isDeviceOwner"), p.bn("lockTaskActive"))
            .also { validar(it) }
    }
}

object CriptografiaStateCp2 {
    fun firmar(s: StateCp2, publicaEsperada: String, firmante: FirmanteAuthCp2): ByteArray {
        require(firmante.publica == publicaEsperada)
        val p = CodecStateCp2.escribir(s); val firma = firmante.firmar("CPv2/MESSAGE\n".toByteArray(Charsets.UTF_8) + p)
        require(firmante.publica == publicaEsperada)
        val frame = JsonCp1.serializar(ObjetoJson(mapOf("v" to EnteroJson(2), "kind" to TextoJson("SIGNED"), "purpose" to TextoJson("MESSAGE"),
            "payloadB64" to TextoJson(Base64Cp1.codificar(p)), "sigB64" to TextoJson(Base64Cp1.codificar(firma)))))
        verificar(frame, publicaEsperada); return frame
    }
    fun verificar(frame: ByteArray, publicaHijoEsperada: String): StateCp2 {
        SobreMessageCp2.leer(frame)
        val o = JsonCp1.leer(frame, 16384) as ObjetoJson
        val p = Base64Cp1.decodificar((o.campos["payloadB64"] as TextoJson).texto, 8192)
        val firma = Base64Cp1.decodificar((o.campos["sigB64"] as TextoJson).texto, 72)
        require(Signature.getInstance("SHA256withECDSA").run { initVerify(ClavePublicaCp1.leer(publicaHijoEsperada).clave)
            update("CPv2/MESSAGE\n".toByteArray(Charsets.UTF_8)); update(p); verify(firma) })
        return CodecStateCp2.leer(p)
    }
}
