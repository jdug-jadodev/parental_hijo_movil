package dev.controlparental.protocol.c8

import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class C8Test {
    private val k = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
    private val publica = Base64Cp1.codificar(k.public.encoded)
    private val prueba = PruebaC8("abcdefab-1234-4234-8234-abcdefabcdef", Base64Cp1.codificar(ByteArray(32)), publica, "https://relay.invalid", "CHILD")
    private val firmante = object : FirmanteAuthCp2 {
        override val publica get() = this@C8Test.publica
        override fun firmar(bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update(bytes); sign() }
    }
    private fun falla(a: () -> Unit) { assertThrows(Exception::class.java) { a() } }
    @Test fun todosLosFramesCamposExactosYCanonicosSinProofDirecto() {
        val muestras = listOf(CodecC8.crear("CHALLENGE", mapOf("connectionId" to prueba.connectionId, "nonceB64" to prueba.nonceB64, "relayOrigin" to prueba.relayOrigin)),
            CriptografiaC8.firmarHijo(prueba, firmante), CodecC8.crear("PUBLISH", mapOf("offerB64" to "e30"), mapOf("ttlMs" to 1)),
            CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 1)), CodecC8.crear("CLAIM", mapOf("code" to "ABCDEFGH")),
            CodecC8.crear("OFFER", mapOf("offerB64" to "e30"), mapOf("remainingMs" to 0)),
            CodecC8.crear("CLAIMED", mapOf("parentPublicKeyB64" to publica), mapOf("remainingMs" to 120000)),
            CodecC8.crear("SEND_PAIR", mapOf("pairB64" to "e30")), CodecC8.crear("PAIR", mapOf("pairB64" to "e30"))) +
            listOf("READY", "SENT", "CANCEL", "UNAVAILABLE").map { CodecC8.crear(it) }
        assertEquals(13, muestras.size)
        for (m in muestras) {
            val b = CodecC8.escribir(m); assertArrayEquals(b, CodecC8.escribir(CodecC8.leer(b)))
            val o = JsonCp1.leer(b) as ObjetoJson
            for (c in o.campos.keys) falla { CodecC8.leer(JsonCp1.serializar(ObjetoJson(o.campos - c))) }
            falla { CodecC8.leer(JsonCp1.serializar(ObjetoJson(o.campos + ("extra" to NuloJson)))) }
        }
        falla { CodecC8.leer(CodecC8.escribirPrueba(prueba)) }
    }
    @Test fun rechazaTiposDuplicadosUtf8EspaciosDepthYVersion() {
        for (s in listOf("{\"kind\":\"READY\",\"v\":1,\"v\":1}", " {\"kind\":\"READY\",\"v\":1}",
            "{\"kind\":\"READY\",\"v\":2}", "{\"v\":1,\"kind\":\"READY\"}", "{\"kind\":\"RÉADY\",\"v\":1}",
            "{\"kind\":\"PUBLISH\",\"offerB64\":\"e30\",\"ttlMs\":\"1\",\"v\":1}", "[[[[[[[0]]]]]]]")) falla { CodecC8.leer(s.toByteArray()) }
        falla { CodecC8.leer(ByteArray(16385) { 32 }) }
        falla { CodecC8.leer(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + CodecC8.escribir(CodecC8.crear("READY"))) }
    }
    @Test fun limitesBase64CodigosEnterosSegurosDocumentosOriginales() {
        for (c in listOf("abcdefgh", "ABCDEFG0", "ABCDEFGO", "ABCDEFGI", "ABCDEFG1", "ABCDEFG", "ABCD EFG", "ABCDEFGH "))
            falla { CodecC8.crear("CLAIM", mapOf("code" to c)) }
        for (n in listOf(-1L, 0L, 120001L, Long.MAX_VALUE)) falla { CodecC8.crear("PUBLISH", mapOf("offerB64" to "e30"), mapOf("ttlMs" to n)) }
        falla { CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 9007199254740992)) }
        for (b in listOf("e30=", "e31", "", Base64Cp1.codificar(ByteArray(8193)))) falla { CodecC8.crear("PAIR", mapOf("pairB64" to b)) }
        val original = ByteArray(8192) { (it % 127).toByte() }
        val f = CodecC8.crear("PAIR", mapOf("pairB64" to Base64Cp1.codificar(original)))
        assertArrayEquals(original, CodecC8.documento(CodecC8.leer(CodecC8.escribir(f)).texto("pairB64")))
    }
    @Test fun proofChildFirmaRealDominioRolClaveOrigenYSocketExactos() {
        val f = CriptografiaC8.firmarHijo(prueba, firmante); CriptografiaC8.verificar(f, prueba)
        for (p in listOf(prueba.copy(connectionId = "12345678-1234-4234-8234-123456789abc"), prueba.copy(nonceB64 = Base64Cp1.codificar(ByteArray(32) { 1 })),
            prueba.copy(role = "PARENT"), prueba.copy(relayOrigin = "https://otro.invalid"))) falla { CriptografiaC8.verificar(f, p) }
        val p = CodecC8.escribirPrueba(prueba)
        for (dominio in listOf("CPv2/AUTH\n", "C8v1/PROOF\\n", "C8v1/PROOF\r\n")) {
            val falso = CodecC8.crear("AUTH", mapOf("payloadB64" to Base64Cp1.codificar(p), "sigB64" to Base64Cp1.codificar(firmante.firmar(dominio.toByteArray() + p))))
            falla { CriptografiaC8.verificar(falso, prueba) }
        }
        falla { CriptografiaC8.firmarHijo(prueba.copy(role = "PARENT"), firmante) }
        falla { CodecC8.crear("AUTH", mapOf("payloadB64" to Base64Cp1.codificar(p), "sigB64" to "AAAAAAAAAAA")) }
    }
    @Test fun proofRechazaClaveDerNoCanonicaUuidNonceOrigenYRol() {
        for (p in listOf(prueba.copy(connectionId = prueba.connectionId.uppercase()), prueba.copy(nonceB64 = prueba.nonceB64 + "="),
            prueba.copy(publicKeyB64 = publica + "="), prueba.copy(role = "ADULT"), prueba.copy(relayOrigin = "http://relay.invalid"),
            prueba.copy(relayOrigin = "https://relay.invalid/pair"))) falla { CodecC8.escribirPrueba(p) }
        val b = CodecC8.escribirPrueba(prueba); assertEquals(prueba, CodecC8.leerPrueba(b))
        falla { CodecC8.leerPrueba(b + byteArrayOf(32)) }
    }
    @Test fun c8v2NoAmpliaC8v1NiAceptaFramesOProofCruzados() {
        val v2 = CodecC8.crear("PUBLISH", mapOf("offerB64" to "e30"), mapOf("ttlMs" to 600000), version = 2)
        assertEquals(2, CodecC8.leer(CodecC8.escribir(v2), 2).version)
        falla { CodecC8.leer(CodecC8.escribir(v2)) }
        falla { CodecC8.leer(CodecC8.escribir(CodecC8.crear("READY")), 2) }
        falla { CodecC8.crear("PUBLISH", mapOf("offerB64" to "e30"), mapOf("ttlMs" to 600001), 2) }
        falla { CodecC8.crear("PUBLISH", mapOf("offerB64" to "e30"), mapOf("ttlMs" to 600000)) }
        falla { CodecC8.crear("READY", version = 3) }
        val p = prueba.copy(version = 2); val b = CodecC8.escribirPrueba(p)
        assertEquals(p, CodecC8.leerPrueba(b, 2)); falla { CodecC8.leerPrueba(b) }
        assertEquals("c8.v2", CodecC8.protocolo(2)); assertEquals(120000L, CodecC8.duracion(1))
    }
    @Test fun dominioC8v2TieneLfRealYRechazaFirmaV1AunquePayloadSeaV2() {
        val p = prueba.copy(version = 2); val f = CriptografiaC8.firmarHijo(p, firmante)
        CriptografiaC8.verificar(f, p)
        falla { CriptografiaC8.verificar(f, prueba) }
        for (dominio in listOf("C8v1/PROOF\n", "C8v2/PROOF\\n", "C8v2/PROOF\r\n")) {
            val b = CodecC8.escribirPrueba(p)
            val incorrecta = CodecC8.crear("AUTH", mapOf("payloadB64" to Base64Cp1.codificar(b),
                "sigB64" to Base64Cp1.codificar(firmante.firmar(dominio.toByteArray() + b))), version = 2)
            falla { CriptografiaC8.verificar(incorrecta, p) }
        }
    }
}
