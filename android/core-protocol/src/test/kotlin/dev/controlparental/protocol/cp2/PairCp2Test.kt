package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class PairCp2Test {
    private fun clave(curva: String = "secp256r1") = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curva)); generateKeyPair()
    }
    private val padre = clave(); private val hijo = clave()
    private fun publica(k: KeyPair) = Base64Cp1.codificar(k.public.encoded)
    private val hash = Base64Cp1.codificar(ByteArray(32) { 7 })
    private fun aceptacion() = AceptacionPairCp2("12345678-1234-4234-8234-123456789abc",
        "abcdefab-1234-4234-8234-abcdefabcdef", hash, publica(padre), publica(hijo), "https://relay.invalid:443",
        HashesPairCp2(hash, hash, hash))
    private fun firma(payload: ByteArray, prefijo: String = "CPv2/PAIR\n", firmante: KeyPair = padre): ByteArray =
        Signature.getInstance("SHA256withECDSA").run { initSign(firmante.private); update(prefijo.toByteArray()); update(payload); sign() }
    private fun frame(payload: ByteArray = CodecPairCp2.escribirPayload(aceptacion()),
        sig: ByteArray = firma(payload), proposito: String = "PAIR"): ByteArray = JsonCp1.serializar(ObjetoJson(mapOf(
        "kind" to TextoJson("SIGNED"), "payloadB64" to TextoJson(Base64Cp1.codificar(payload)),
        "purpose" to TextoJson(proposito), "sigB64" to TextoJson(Base64Cp1.codificar(sig)), "v" to EnteroJson(2))))
    private fun rechaza(accion: () -> Unit) = assertThrows(Exception::class.java) { accion() }
    @Test fun firmaRealBytesOriginalesHashYCopiasDefensivas() {
        val p = CodecPairCp2.escribirPayload(aceptacion()); val f = frame(p)
        val v = CriptografiaPairCp2.verificarIntegridad(f, publica(padre))
        assertEquals(aceptacion(), v.aceptacion); assertArrayEquals(p, v.bytesPayload()); assertArrayEquals(f, v.bytesFrame())
        assertEquals(Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(p)), v.payloadSha256B64)
        val sobre = CodecPairCp2.leerSobre(f); assertArrayEquals(f, CodecPairCp2.escribirSobre(sobre))
        f.fill(0); p.fill(0); v.bytesPayload().fill(0); v.bytesFrame().fill(0)
        assertEquals(aceptacion(), CodecPairCp2.leerPayload(v.bytesPayload()))
        assertNotEquals(0.toByte(), v.bytesFrame()[0])
    }
    @Test fun camposExactosDiezCincoTresYTodosObligatorios() {
        val p = CodecPairCp2.escribirPayload(aceptacion()); val o = JsonCp1.leer(p) as ObjetoJson
        assertEquals(10, o.campos.size)
        for (k in o.campos.keys) rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(o.campos - k))) }
        rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(o.campos + ("extra" to NuloJson)))) }
        val h = o.campos["recoveryHashes"] as ObjetoJson; assertEquals(3, h.campos.size)
        for (k in h.campos.keys) rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(o.campos +
            ("recoveryHashes" to ObjetoJson(h.campos - k))))) }
        rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(o.campos +
            ("recoveryHashes" to ObjetoJson(h.campos + ("ALLOW" to TextoJson(hash))))))) }
        val s = JsonCp1.leer(frame()) as ObjetoJson; assertEquals(5, s.campos.size)
        for (k in s.campos.keys) rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(s.campos - k))) }
        rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(s.campos + ("protocol" to TextoJson("cp.v2"))))) }
    }
    @Test fun tiposConstantesYNulosNoSeConvierten() {
        val o = JsonCp1.leer(CodecPairCp2.escribirPayload(aceptacion())) as ObjetoJson
        for ((k, v) in listOf("v" to EnteroJson(1), "v" to TextoJson("2"), "kind" to TextoJson("CHILD_OFFER"),
            "protocol" to TextoJson("cp.v1"), "pairId" to NuloJson, "recoveryHashes" to ListaJson(emptyList())))
            rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(o.campos + (k to v)))) }
    }
    @Test fun firmaCruzadaV1OtroPropositoLfLiteralYCandidataDistinta() {
        val p = CodecPairCp2.escribirPayload(aceptacion())
        for (dominio in listOf("CPv1/PAIR\n", "CPv2/OFFLINE\n", "CPv2/MESSAGE\n", "CPv2/PAIR\\n", "CPv2/PAIR\r\n"))
            rechaza { CriptografiaPairCp2.verificarIntegridad(frame(p, firma(p, dominio)), publica(padre)) }
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame(p, firma(p, firmante = clave())), publica(padre)) }
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame(), publica(clave())) }
        rechaza { CodecPairCp2.leerSobre(frame(proposito = "OFFLINE")) }
        rechaza { CodecCp1.leerFrame(frame()) }
    }
    @Test fun padreDeclaradoDebeCoincidirConCandidataEsperada() {
        val p = CodecPairCp2.escribirPayload(aceptacion().copy(parentPublicKeyB64 = publica(clave())))
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame(p), publica(padre)) }
    }
    @Test fun canonicoNoWhitespaceOrdenEscapesNiDuplicadosAunqueFirmados() {
        val s = CodecPairCp2.escribirPayload(aceptacion()).toString(Charsets.US_ASCII)
        for (alterado in listOf(" $s", "$s ", s.replace(",", ", "), s.replace("{", "{\"v\":2,"),
            s.replace("PAIR_ACCEPT", "PAIR_\\u0041CCEPT"), s.replace("\"v\":2", "\"v\":2.0"))) {
            val p = alterado.toByteArray(); rechaza { CriptografiaPairCp2.verificarIntegridad(frame(p), publica(padre)) }
        }
        val o = JsonCp1.leer(s.toByteArray()) as ObjetoJson
        val inverso = o.campos.entries.reversed().joinToString(",", "{", "}") { "\"${it.key}\":" + JsonCp1.serializar(it.value).toString(Charsets.UTF_8) }
        rechaza { CodecPairCp2.leerPayload(inverso.toByteArray()) }
        rechaza { CodecPairCp2.leerSobre(" ".toByteArray() + frame()) }
    }
    @Test fun limitesPayloadFrameYProfundidadAntesDeParse() {
        CodecPairCp2.preflight(ByteArray(8192) { 32 }, 8192); CodecPairCp2.preflight(ByteArray(16384) { 32 }, 16384)
        rechaza { CodecPairCp2.preflight(ByteArray(8193) { 32 }, 8192) }
        rechaza { CodecPairCp2.preflight(ByteArray(16385) { 32 }, 16384) }
        CodecPairCp2.preflight(("[".repeat(6) + "0" + "]".repeat(6)).toByteArray(), 8192)
        rechaza { CodecPairCp2.preflight(("[".repeat(7) + "0" + "]".repeat(7)).toByteArray(), 8192) }
        for (b in listOf(byteArrayOf(), byteArrayOf(0xc3.toByte(), 0x28), "é".toByteArray(), byteArrayOf(10)))
            rechaza { CodecPairCp2.leerPayload(b) }
    }
    @Test fun hashesNonce32CanonicalidadB64YUuidBits() {
        val a = aceptacion()
        for (h in listOf(hash + "=", Base64Cp1.codificar(ByteArray(31)), hash.dropLast(1) + "B")) {
            rechaza { CodecPairCp2.escribirPayload(a.copy(enrollNonce = h)) }
            rechaza { CodecPairCp2.escribirPayload(a.copy(recoveryHashes = a.recoveryHashes.copy(RETIRE = h))) }
        }
        for (id in listOf(a.enrollId.uppercase(), a.enrollId.replace("-4234-", "-3234-"), a.enrollId.replace("-8234-", "-7234-"))) {
            rechaza { CodecPairCp2.escribirPayload(a.copy(enrollId = id)) }
            rechaza { CodecPairCp2.escribirPayload(a.copy(pairId = id)) }
        }
        val o = JsonCp1.leer(frame()) as ObjetoJson
        for (k in listOf("payloadB64", "sigB64")) {
            val t = (o.campos[k] as TextoJson).texto
            rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(o.campos + (k to TextoJson(t + "="))))) }
        }
    }
    @Test fun p256SpkiExactaYHuellaDeBytesDerNoDeTexto() {
        val a = aceptacion()
        for (p in listOf(publica(clave("secp384r1")), a.parentPublicKeyB64 + "=",
            Base64Cp1.codificar(padre.public.encoded + byteArrayOf(0)))) rechaza { CodecPairCp2.escribirPayload(a.copy(parentPublicKeyB64 = p)) }
        assertEquals(Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(padre.public.encoded)), CodecPairCp2.huellaPublica(publica(padre)))
    }
    @Test fun origenHttpsExactoSinNormalizarNiAccesoExterno() {
        for (origen in listOf("http://relay.invalid", "https://Relay.invalid", "https://relay.invalid/", "https://user@relay.invalid",
            "https://relay.invalid?q=x", "https://relay.invalid#x", "https://relay.invalid:65536", " https://relay.invalid"))
            rechaza { CodecPairCp2.escribirPayload(aceptacion().copy(relayOrigin = origen)) }
        val a = aceptacion(); assertEquals(a.relayOrigin, CodecPairCp2.leerPayload(CodecPairCp2.escribirPayload(a)).relayOrigin)
    }
    private fun der(r: BigInteger, s: BigInteger): ByteArray {
        val rb = r.toByteArray(); val sb = s.toByteArray()
        return byteArrayOf(0x30, (4 + rb.size + sb.size).toByte(), 2, rb.size.toByte()) + rb + byteArrayOf(2, sb.size.toByte()) + sb
    }
    @Test fun derDosEnterosMinimosYAmbosEnRangoNoJose() {
        val n = BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16)
        val p = CodecPairCp2.escribirPayload(aceptacion())
        CodecPairCp2.leerSobre(frame(p, der(BigInteger.ONE, n - BigInteger.ONE))) // Estructura válida, no firma acreditada.
        for (x in listOf(BigInteger.ZERO, n, -BigInteger.ONE)) {
            rechaza { CodecPairCp2.leerSobre(frame(p, der(x, BigInteger.ONE))) }
            rechaza { CodecPairCp2.leerSobre(frame(p, der(BigInteger.ONE, x))) }
        }
        for (s in listOf(ByteArray(64), ByteArray(7), ByteArray(73), der(BigInteger.ONE, BigInteger.ONE) + byteArrayOf(0),
            byteArrayOf(0x30, 7, 2, 2, 0, 1, 2, 1, 1), byteArrayOf(0x30, 0x81.toByte(), 6, 2, 1, 1, 2, 1, 1)))
            rechaza { CodecPairCp2.leerSobre(frame(p, s)) }
    }
    @Test fun firmaMutadaOCambioEnPayloadNoSeAutorizaPorHash() {
        val p = CodecPairCp2.escribirPayload(aceptacion()); val sig = firma(p)
        val mutada = sig.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame(p, mutada), publica(padre)) }
        val otroPayload = CodecPairCp2.escribirPayload(aceptacion().copy(pairId = "abcdefab-1234-4234-8234-abcdefabcdef"))
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame(otroPayload, sig), publica(padre)) }
    }
    @Test fun limitesEnVerificadorAntesDeCopiarYFrameCanonicalidad() {
        for (bytes in listOf(byteArrayOf(), ByteArray(16385))) rechaza { CriptografiaPairCp2.verificarIntegridad(bytes, publica(padre)) }
        val f = frame().toString(Charsets.US_ASCII)
        for (alterado in listOf(f.replace(",", ", "), f.replace("{", "{\"v\":2,"), f.replace("SIGNED", "S\\u0049GNED")))
            rechaza { CriptografiaPairCp2.verificarIntegridad(alterado.toByteArray(), publica(padre)) }
    }
}
