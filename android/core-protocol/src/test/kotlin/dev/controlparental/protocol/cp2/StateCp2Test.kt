package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class StateCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val hash = Base64Cp1.codificar(ByteArray(32))
    private fun efectivo() = EfectivoStateCp2(ModoStateCp2.UNKNOWN, MotivoStateCp2.CONTROL_UNKNOWN, FuenteStateCp2.NONE,
        null, null, FaseStateCp2.NONE, null, null, null, null, EfectoStateCp2.UNKNOWN)
    private fun state() = StateCp2(id, id, id, hash, 1, 100, 7, 0, null, efectivo(), null, null, null, null, null)
    private fun temporal() = efectivo().copy(source = FuenteStateCp2.REMOTE, action = AccionOrdenCp2.ALLOW_FOR, policyRevision = 7,
        acceptedElapsedMs = 100, phaseStartedElapsedMs = 100, deadlineElapsedMs = 60100, remainingSec = 60, phase = FaseStateCp2.PERMITTED)
    private fun falla(a: () -> Unit) { assertThrows(Exception::class.java) { a() } }
    private fun objeto(b: ByteArray) = JsonCp1.leer(b) as ObjetoJson
    @Test fun listasExactasVeinteOnceCincoNueveHistorialNullNoCero() {
        val s = state().copy(effective = temporal(), commandChallenge = ChallengeStateCp2(hash, AccionOrdenCp2.ALLOW_FOR, 7, 100, 30100))
        val b = CodecStateCp2.escribir(s); assertEquals(s, CodecStateCp2.leer(b))
        val o = objeto(b); assertEquals(20, o.campos.size)
        assertEquals(11, (o.campos["effective"] as ObjetoJson).campos.size)
        assertEquals(5, (o.campos["commandChallenge"] as ObjetoJson).campos.size)
        assertSame(NuloJson, o.campos["permittedTime"]); assertSame(NuloJson, o.campos["lastExpiry"])
        for (k in o.campos.keys) falla { CodecStateCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos - k))) }
        for (k in listOf("protocol", "publicKey", "alg")) falla { CodecStateCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos + (k to TextoJson("x"))))) }
        for (k in listOf("permittedTime", "lastExpiry")) falla { CodecStateCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos + (k to EnteroJson(0))))) }
    }
    @Test fun retosRevisionTiempoEstrictoYOverflow() {
        val r = ChallengeStateCp2(hash, AccionOrdenCp2.LOCK, 7, 100, 30100)
        CodecStateCp2.escribir(state().copy(commandChallenge = r, sampledElapsedMs = 30099))
        for (reto in listOf(r.copy(revision = 6), r.copy(expiresElapsedMs = 30099), r.copy(issuedElapsedMs = 101),
            r.copy(issuedElapsedMs = Long.MAX_VALUE, expiresElapsedMs = Long.MAX_VALUE))) falla { CodecStateCp2.escribir(state().copy(commandChallenge = reto)) }
        falla { CodecStateCp2.escribir(state().copy(commandChallenge = r, sampledElapsedMs = 30100)) }
    }
    @Test fun temporalDuracionExactaRemainingCeilYExpirada() {
        val e = temporal(); CodecStateCp2.escribir(state().copy(effective = e))
        CodecStateCp2.escribir(state().copy(sampledElapsedMs = 101, effective = e.copy(remainingSec = 60)))
        CodecStateCp2.escribir(state().copy(sampledElapsedMs = 60100, effective = e.copy(phase = FaseStateCp2.EXPIRED, remainingSec = 0)))
        CodecStateCp2.escribir(state().copy(sampledElapsedMs = 60099, effective = e.copy(remainingSec = 1)))
        for (e2 in listOf(e.copy(remainingSec = 59), e.copy(deadlineElapsedMs = 60101), e.copy(deadlineElapsedMs = 60099),
            e.copy(deadlineElapsedMs = 86460100), e.copy(phaseStartedElapsedMs = 101), e.copy(policyRevision = 8), e.copy(phase = FaseStateCp2.EXPIRED)))
            falla { CodecStateCp2.escribir(state().copy(effective = e2)) }
    }
    @Test fun sinDecisionSoloNoneNullYModosExigenVerifiedFaseCompatible() {
        val e = efectivo()
        for (e2 in listOf(e.copy(action = AccionOrdenCp2.LOCK), e.copy(remainingSec = 0), e.copy(phase = FaseStateCp2.BLOCKED),
            e.copy(mode = ModoStateCp2.LOCKED), e.copy(mode = ModoStateCp2.ALLOWED))) falla { CodecStateCp2.escribir(state().copy(effective = e2)) }
        val permitir = temporal().copy(mode = ModoStateCp2.ALLOWED, enforcement = EfectoStateCp2.VERIFIED)
        CodecStateCp2.escribir(state().copy(effective = permitir))
        falla { CodecStateCp2.escribir(state().copy(effective = permitir.copy(enforcement = EfectoStateCp2.PENDING))) }
        falla { CodecStateCp2.escribir(state().copy(effective = permitir.copy(mode = ModoStateCp2.LOCKED))) }
        val lock = permitir.copy(action = AccionOrdenCp2.LOCK, phase = FaseStateCp2.BLOCKED, mode = ModoStateCp2.LOCKED, deadlineElapsedMs = null, remainingSec = null)
        CodecStateCp2.escribir(state().copy(effective = lock))
        falla { CodecStateCp2.escribir(state().copy(effective = lock.copy(deadlineElapsedMs = 1000))) }
    }
    @Test fun lastRemoteCoherenteSinCompararRelojDeOtroBootYNullNoReseteaSeq() {
        val m = RemotoStateCp2(id, 1, hash, id, id, 7, ResultadoStateCp2.APPLIED, MotivoResultadoCp2.OK, Long.MAX_VALUE)
        val s = state().copy(lastAcceptedSeq = 1, lastRemote = m)
        assertEquals(s, CodecStateCp2.leer(CodecStateCp2.escribir(s)))
        assertEquals(9, (objeto(CodecStateCp2.escribir(s)).campos["lastRemote"] as ObjetoJson).campos.size)
        CodecStateCp2.escribir(s.copy(lastRemote = null)) // Desconocido histórico, no reiniciar secuencia.
        for (m2 in listOf(m.copy(seq = 2), m.copy(acceptedRevision = 0), m.copy(acceptedRevision = 8), m.copy(resultElapsedMs = null),
            m.copy(result = ResultadoStateCp2.ENFORCEMENT_FAILED), m.copy(result = ResultadoStateCp2.ACCEPTED_PENDING_ENFORCEMENT)))
            falla { CodecStateCp2.escribir(s.copy(lastRemote = m2)) }
        CodecStateCp2.escribir(s.copy(lastRemote = m.copy(result = ResultadoStateCp2.ACCEPTED_PENDING_ENFORCEMENT, resultElapsedMs = null)))
    }
    @Test fun contadoresLongMaxSinCoercionEnumsCerradosCanonicalidadLimites() {
        CodecStateCp2.escribir(state().copy(controlRevision = Long.MAX_VALUE, stateSeq = Long.MAX_VALUE, sampledElapsedMs = Long.MAX_VALUE))
        val s = CodecStateCp2.escribir(state()).toString(Charsets.UTF_8)
        for (c in listOf("01", "+1", "-1", "9223372036854775808")) falla { CodecStateCp2.leer(s.replace("\"stateSeq\":\"1\"", "\"stateSeq\":\"$c\"").toByteArray()) }
        for (mal in listOf(s.replace("\"stateSeq\":\"1\"", "\"stateSeq\":1"), " $s", s.replace("{", "{\"v\":2,"),
            s.replace("CONTROL_UNKNOWN", "BAD_NONCE"), s.replace("UNKNOWN", "WAITING_ALLOW"), s.replace("CHILD", "PARENT"))) falla { CodecStateCp2.leer(mal.toByteArray()) }
        falla { CodecStateCp2.leer(ByteArray(8193)) }
    }
    @Test fun firmaChildRealClaveExternaDominioBytesYSobreCerrado() {
        val clave = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val publica = Base64Cp1.codificar(clave.public.encoded)
        val esperada = publica
        val firmante = object : FirmanteAuthCp2 {
            override val publica get() = esperada
            override fun firmar(bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update(bytes); sign() }
        }
        val b = CriptografiaStateCp2.firmar(state(), publica, firmante)
        assertEquals(state(), CriptografiaStateCp2.verificar(b, publica))
        assertEquals(5, objeto(b).campos.size)
        val otra = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        falla { CriptografiaStateCp2.verificar(b, Base64Cp1.codificar(otra.public.encoded)) }
        val o = objeto(b); val p = CodecStateCp2.escribir(state().copy(stateSeq = 2))
        falla { CriptografiaStateCp2.verificar(JsonCp1.serializar(ObjetoJson(o.campos + ("payloadB64" to TextoJson(Base64Cp1.codificar(p))))), publica) }
        val firmaAuth = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update("CPv2/AUTH\n".toByteArray()); update(CodecStateCp2.escribir(state())); sign() }
        falla { CriptografiaStateCp2.verificar(JsonCp1.serializar(ObjetoJson(o.campos + ("sigB64" to TextoJson(Base64Cp1.codificar(firmaAuth))))), publica) }
    }
}
