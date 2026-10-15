package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class AckCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val hash = Base64Cp1.codificar(ByteArray(32))
    private fun ack() = AckCp2(id, id, id, id, 1, hash, 2, 3, ResultadoAckCp2.APPLIED, MotivoAckCp2.OK, 100)
    private fun falla(a: () -> Unit) { assertThrows(Exception::class.java) { a() } }
    @Test fun variantesExactasYSinCoercionesDeResultado() {
        val a = ack()
        val validas = listOf(a, a.copy(result = ResultadoAckCp2.ACCEPTED_PENDING_ENFORCEMENT, resultElapsedMs = null),
            a.copy(result = ResultadoAckCp2.ENFORCEMENT_FAILED, reason = MotivoAckCp2.ENFORCEMENT_ERROR),
            a.copy(result = ResultadoAckCp2.REJECTED, reason = MotivoAckCp2.STALE_NONCE, acceptedRevision = null, resultElapsedMs = null),
            a.copy(result = ResultadoAckCp2.UNKNOWN_COMMAND, reason = MotivoAckCp2.UNKNOWN_COMMAND, acceptedRevision = null, resultElapsedMs = null))
        for (v in validas) assertEquals(v, CodecAckCp2.leer(CodecAckCp2.escribir(v)))
        for (v in validas) {
            if (v.acceptedRevision != null) falla { CodecAckCp2.escribir(v.copy(acceptedRevision = null)) }
            else falla { CodecAckCp2.escribir(v.copy(acceptedRevision = 1)) }
            if (v.resultElapsedMs != null) falla { CodecAckCp2.escribir(v.copy(resultElapsedMs = null)) }
            else falla { CodecAckCp2.escribir(v.copy(resultElapsedMs = 1)) }
        }
        falla { CodecAckCp2.escribir(validas[3].copy(reason = MotivoAckCp2.OK)) }
        falla { CodecAckCp2.escribir(validas[3].copy(reason = MotivoAckCp2.UNKNOWN_COMMAND)) }
        falla { CodecAckCp2.escribir(a.copy(reason = MotivoAckCp2.PEER_OFFLINE)) }
    }
    @Test fun catorceRequiredCerradosDuplicadosTiposYVersion() {
        val b = CodecAckCp2.escribir(ack()); val o = JsonCp1.leer(b) as ObjetoJson
        assertEquals(14, o.campos.size)
        for (k in o.campos.keys) falla { CodecAckCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos - k))) }
        for (k in listOf("protocol", "alg", "publicKey", "mode")) falla { CodecAckCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos + (k to NuloJson)))) }
        val s = b.toString(Charsets.UTF_8)
        for (mal in listOf(" $s", s.replace("{", "{\"v\":2,"), s.replace("CHILD", "PARENT"), s.replace("\"v\":2", "\"v\":1"),
            s.replace("\"seq\":\"1\"", "\"seq\":1"), s.replace("APPLIED", "DELIVERED"))) falla { CodecAckCp2.leer(mal.toByteArray()) }
        falla { CodecAckCp2.leer(ByteArray(8193)) }
    }
    @Test fun contadoresLongMaxAcceptedMenorIgualEvaluatedSinPaddingSignosNiOverflow() {
        CodecAckCp2.escribir(ack().copy(seq = Long.MAX_VALUE, acceptedRevision = Long.MAX_VALUE, evaluatedRevision = Long.MAX_VALUE, resultElapsedMs = Long.MAX_VALUE))
        falla { CodecAckCp2.escribir(ack().copy(acceptedRevision = 4)) }
        falla { CodecAckCp2.escribir(ack().copy(acceptedRevision = 0)) }
        falla { CodecAckCp2.escribir(ack().copy(seq = 0)) }
        falla { CodecAckCp2.escribir(ack().copy(resultElapsedMs = -1)) }
        val s = CodecAckCp2.escribir(ack()).toString(Charsets.UTF_8)
        for (c in listOf("01", "+1", "-1", "1.0", "1e0", "9223372036854775808"))
            falla { CodecAckCp2.leer(s.replace("\"seq\":\"1\"", "\"seq\":\"$c\"").toByteArray()) }
    }
    @Test fun hashRoundtripUuidYNulosNoFabricanAplicacion() {
        for (h in listOf(hash + "=", hash.dropLast(1) + "B", Base64Cp1.codificar(ByteArray(31)))) falla { CodecAckCp2.escribir(ack().copy(commandPayloadSha256B64 = h)) }
        falla { CodecAckCp2.escribir(ack().copy(commandId = id.uppercase())) }
        falla { CodecAckCp2.escribir(ack().copy(bootId = id.replace("-4234-", "-1234-"))) }
        val unknown = ack().copy(result = ResultadoAckCp2.UNKNOWN_COMMAND, reason = MotivoAckCp2.UNKNOWN_COMMAND, acceptedRevision = null, resultElapsedMs = null)
        assertNull(CodecAckCp2.leer(CodecAckCp2.escribir(unknown)).acceptedRevision)
    }
    @Test fun firmaChildRealSobreCincoCamposClaveYDominioExternosNoRelay() {
        val k = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val publica = Base64Cp1.codificar(k.public.encoded)
        fun firma(b: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update(b); sign() }
        val firmante = object : FirmanteAuthCp2 {
            override val publica = Base64Cp1.codificar(k.public.encoded)
            override fun firmar(bytes: ByteArray) = firma(bytes)
        }
        val frame = CriptografiaAckCp2.firmar(ack(), publica, firmante)
        assertEquals(ack(), CriptografiaAckCp2.verificar(frame, publica))
        val o = JsonCp1.leer(frame) as ObjetoJson; assertEquals(5, o.campos.size)
        val otro = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        falla { CriptografiaAckCp2.verificar(frame, Base64Cp1.codificar(otro.public.encoded)) }
        for (dominio in listOf("CPv2/AUTH\n", "CPv1/MESSAGE\n", "CPv2/MESSAGE\\n")) {
            val s = Base64Cp1.codificar(firma(dominio.toByteArray() + CodecAckCp2.escribir(ack())))
            falla { CriptografiaAckCp2.verificar(JsonCp1.serializar(ObjetoJson(o.campos + ("sigB64" to TextoJson(s)))), publica) }
        }
        falla { CriptografiaAckCp2.verificar(JsonCp1.serializar(ObjetoJson(o.campos + ("payloadB64" to TextoJson(Base64Cp1.codificar(CodecAckCp2.escribir(ack().copy(seq = 2))))))), publica) }
        falla { CriptografiaStateCp2.verificar(frame, publica) }
    }
}
