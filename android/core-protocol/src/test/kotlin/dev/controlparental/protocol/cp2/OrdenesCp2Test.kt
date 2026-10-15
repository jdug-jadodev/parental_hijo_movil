package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class OrdenesCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val nonce = Base64Cp1.codificar(ByteArray(32) { 2 })
    private val padre = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private val publica = Base64Cp1.codificar(padre.public.encoded)
    private fun politica() = PoliticaOrdenCp2(id, id, id, id, 1, 7, nonce, AccionOrdenCp2.ALLOW_FOR, 60)
    private fun firma(p: ByteArray, dominio: String = "CPv2/MESSAGE\n") = Signature.getInstance("SHA256withECDSA").run {
        initSign(padre.private); update(dominio.toByteArray(Charsets.UTF_8)); update(p); sign()
    }
    private fun frame(m: MensajeOrdenCp2, dominio: String = "CPv2/MESSAGE\n"): ByteArray {
        val p = CodecOrdenCp2.escribirPayload(m)
        return CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(firma(p, dominio))))
    }
    private fun o(b: ByteArray) = JsonCp1.leer(b) as ObjetoJson
    private fun rechaza(a: () -> Unit) { assertThrows(Exception::class.java) { a() } }
    @Test fun firmaPadreMessageBytesOriginalesHashYCopias() {
        for (m in listOf(SyncOrdenCp2(id, nonce, null), SyncOrdenCp2(id, nonce, AccionOrdenCp2.LOCK), politica())) {
            val f = frame(m); val v = CriptografiaOrdenCp2.verificar(f, publica)
            assertEquals(m, v.mensaje); assertArrayEquals(f, v.bytesFrame())
            assertArrayEquals(CodecOrdenCp2.escribirPayload(m), v.bytesPayload())
            assertEquals(Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(v.bytesPayload())), v.payloadSha256B64)
            f.fill(0); v.bytesPayload().fill(0); v.bytesFrame().fill(0); assertNotEquals(0.toByte(), v.bytesFrame()[0])
        }
    }
    @Test fun listasExactasSeisDoceCincoYNingunCampoAusenteOExtra() {
        for ((m, n) in listOf(SyncOrdenCp2(id, nonce, null) to 6, politica() to 12)) {
            val p = o(CodecOrdenCp2.escribirPayload(m)); assertEquals(n, p.campos.size)
            for (k in p.campos.keys) rechaza { CodecOrdenCp2.leerPayload(JsonCp1.serializar(ObjetoJson(p.campos - k))) }
            for (k in listOf("alg", "publicKey", "protocol", "deferredAllow")) rechaza { CodecOrdenCp2.leerPayload(JsonCp1.serializar(ObjetoJson(p.campos + (k to NuloJson)))) }
        }
        val s = o(frame(politica())); assertEquals(5, s.campos.size)
        for (k in s.campos.keys) rechaza { CodecOrdenCp2.leerSobre(JsonCp1.serializar(ObjetoJson(s.campos - k))) }
        rechaza { CodecOrdenCp2.leerSobre(JsonCp1.serializar(ObjetoJson(s.campos + ("alg" to TextoJson("ES256"))))) }
    }
    @Test fun contadoresCanonicosHastaLongMaxSinCoercionNiOverflow() {
        val original = o(CodecOrdenCp2.escribirPayload(politica()))
        fun poner(k: String, v: ValorJson) = JsonCp1.serializar(ObjetoJson(original.campos + (k to v)))
        assertEquals(Long.MAX_VALUE, (CodecOrdenCp2.leerPayload(poner("seq", TextoJson(Long.MAX_VALUE.toString()))) as PoliticaOrdenCp2).seq)
        assertEquals(0L, (CodecOrdenCp2.leerPayload(poner("expectedRevision", TextoJson("0"))) as PoliticaOrdenCp2).expectedRevision)
        assertEquals(Long.MAX_VALUE, (CodecOrdenCp2.leerPayload(poner("expectedRevision", TextoJson(Long.MAX_VALUE.toString()))) as PoliticaOrdenCp2).expectedRevision)
        for (k in listOf("seq", "expectedRevision")) for (s in listOf("00", "01", "+1", "-1", " 1", "1.0", "1e1", "9223372036854775808", "99999999999999999999"))
            rechaza { CodecOrdenCp2.leerPayload(poner(k, TextoJson(s))) }
        rechaza { CodecOrdenCp2.leerPayload(poner("seq", TextoJson("0"))) }
        rechaza { CodecOrdenCp2.leerPayload(poner("seq", EnteroJson(1))) }
    }
    @Test fun duracionAccionSinLockForNiWaitingAllow() {
        for (a in listOf(AccionOrdenCp2.LOCK, AccionOrdenCp2.ALLOW)) {
            CodecOrdenCp2.escribirPayload(politica().copy(action = a, durationSec = null))
            rechaza { CodecOrdenCp2.escribirPayload(politica().copy(action = a)) }
        }
        CodecOrdenCp2.escribirPayload(politica().copy(durationSec = 86400))
        for (d in listOf(null, 0L, 59L, 61L, 86460L)) rechaza { CodecOrdenCp2.escribirPayload(politica().copy(durationSec = d)) }
        val p = CodecOrdenCp2.escribirPayload(politica()).toString(Charsets.UTF_8)
        for (s in listOf("LOCK_FOR", "WAITING_ALLOW", "REMOTE_OFFLINE")) rechaza { CodecOrdenCp2.leerPayload(p.replace("ALLOW_FOR", s).toByteArray()) }
        rechaza { CodecOrdenCp2.leerPayload(p.replace("\"durationSec\":60", "\"durationSec\":\"60\"").toByteArray()) }
    }
    @Test fun tiposCanonicalidadUuidNonceLimitesYProfundidad() {
        val p = CodecOrdenCp2.escribirPayload(politica()).toString(Charsets.UTF_8)
        for (s in listOf(" $p", p.replace(",", ", "), p.replace("{", "{\"v\":2,"), p.replace("PARENT", "CHILD"), p.replace("PARENT", "P\\u0041RENT"),
            p.replace("\"v\":2", "\"v\":1"), p.replace(id, id.uppercase()), p.replace(id, id.replace("-4234-", "-3234-")), p.replace(nonce, nonce + "="))) rechaza { CodecOrdenCp2.leerPayload(s.toByteArray()) }
        for (b in listOf(ByteArray(8193), "é".toByteArray(), ("[".repeat(7) + "0" + "]".repeat(7)).toByteArray())) rechaza { CodecOrdenCp2.leerPayload(b) }
        rechaza { CodecOrdenCp2.leerSobre(ByteArray(16385)) }
        val s = CodecOrdenCp2.leerSobre(frame(politica()))
        rechaza { CodecOrdenCp2.escribirSobre(s.copy(sigB64 = s.sigB64 + "=")) }
        rechaza { CodecOrdenCp2.escribirSobre(s.copy(sigB64 = Base64Cp1.codificar(ByteArray(64)))) }
        for (firma in listOf(ByteArray(7), ByteArray(73), byteArrayOf(0x30, 6, 2, 1, 0, 2, 1, 1)))
            rechaza { CodecOrdenCp2.escribirSobre(s.copy(sigB64 = Base64Cp1.codificar(firma))) }
        for (n in listOf(nonce.dropLast(1) + "B", Base64Cp1.codificar(ByteArray(31))))
            rechaza { CodecOrdenCp2.escribirPayload(politica().copy(commandNonce = n)) }
    }
    @Test fun claveDominioFirmaYBytesAlteradosNoVerifican() {
        val otra = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        rechaza { CriptografiaOrdenCp2.verificar(frame(politica()), Base64Cp1.codificar(otra.public.encoded)) }
        for (dominio in listOf("CPv1/MESSAGE\n", "CPv2/AUTH\n", "CPv2/PAIR\n", "CPv2/MESSAGE\\n", "CPv2/MESSAGE\r\n"))
            rechaza { CriptografiaOrdenCp2.verificar(frame(politica(), dominio), publica) }
        val s = CodecOrdenCp2.leerSobre(frame(politica()))
        val cambiado = CodecOrdenCp2.escribirPayload(politica().copy(expectedRevision = 8))
        rechaza { CriptografiaOrdenCp2.verificar(CodecOrdenCp2.escribirSobre(s.copy(payloadB64 = Base64Cp1.codificar(cambiado))), publica) }
        rechaza { CodecAuthCp2.leer(frame(politica())) }
    }
}
