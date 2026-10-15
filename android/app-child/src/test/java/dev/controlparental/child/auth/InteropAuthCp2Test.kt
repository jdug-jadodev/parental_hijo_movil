package dev.controlparental.child.auth

import dev.controlparental.child.pairing.*
import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/** Fixture8 público, privadas exclusivamente RAM/JCA de pruebas. No autentica socket ni teléfono. */
class InteropAuthCp2Test {
    private val campos = setOf("fixture", "parentPublicKeyB64", "childPublicKeyB64", "childOfferUtf8", "pairFrameUtf8",
        "pairPayloadSha256B64", "authParentFrameUtf8", "authChildFrameUtf8")
    private fun objeto(bytes: ByteArray) = JsonCp1.leer(bytes) as ObjetoJson
    private fun ObjetoJson.t(k: String) = (campos.getValue(k) as TextoJson).texto
    private fun b(o: ObjetoJson, k: String) = o.t(k).toByteArray(Charsets.UTF_8)
    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b)
    private fun raiz() = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "00_EMPIEZA_AQUI.md").isFile }
    private fun clave() = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private fun publica(k: KeyPair) = Base64Cp1.codificar(k.public.encoded)
    private fun azar() = Base64Cp1.codificar(ByteArray(32).also(SecureRandom()::nextBytes))
    private fun firma(k: KeyPair, bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update(bytes); sign() }
    private fun verificar(o: ObjetoJson) {
        assertEquals(campos, o.campos.keys); assertEquals("CP2_PUBLICO_EFIMERO_SIN_AUTORIDAD", o.t("fixture"))
        val oferta = CodecOfertaCp2.leer(b(o, "childOfferUtf8"))
        val pair = CriptografiaPairCp2.verificarIntegridad(b(o, "pairFrameUtf8"), o.t("parentPublicKeyB64"))
        assertEquals(o.t("pairPayloadSha256B64"), pair.payloadSha256B64)
        assertEquals(oferta.childPublicKeyB64, pair.aceptacion.childPublicKeyB64)
        assertEquals(oferta.enrollId, pair.aceptacion.enrollId); assertEquals(oferta.enrollNonce, pair.aceptacion.enrollNonce)
        for ((rol, campo, clave) in listOf(Triple(RolAuthCp2.PARENT, "authParentFrameUtf8", "parentPublicKeyB64"),
            Triple(RolAuthCp2.CHILD, "authChildFrameUtf8", "childPublicKeyB64"))) {
            val frame = b(o, campo)
            val sobre = CodecAuthCp2.leer(frame) as SobreAuthCp2
            assertArrayEquals(frame, CodecAuthCp2.escribir(sobre))
            val v = CriptografiaAuthCp2.verificar(frame, o.t(clave), rol)
            assertEquals(rol, v.prueba.role); assertEquals(pair.aceptacion.pairId, v.prueba.pairId)
            assertArrayEquals(Base64Cp1.decodificar(sobre.payloadB64, 8192), v.bytesPayload())
            assertArrayEquals(v.bytesPayload(), CodecAuthCp2.escribirPrueba(v.prueba))
            assertThrows(Exception::class.java) { CriptografiaAuthCp2.verificar(frame, o.t(clave), if (rol == RolAuthCp2.CHILD) RolAuthCp2.PARENT else RolAuthCp2.CHILD) }
            assertThrows(Exception::class.java) { CriptografiaPairCp2.verificarIntegridad(frame, o.t(clave)) }
        }
    }
    @Test fun dosAuthNodeSeEvalúanAhoraPorCodecCriptoConPublicasExternas() {
        val bytes = javaClass.getResourceAsStream("/cp2/cp2-i01-publico.json")!!.use { it.readBytes() }
        assertEquals("967ae05aecd28c6b7f90b8ae90e8421b408dd70acbf22fd30911085c69a964ec", sha(bytes).joinToString("") { "%02x".format(it) })
        verificar(objeto(bytes))
    }
    private fun generar(): ObjetoJson {
        val padre = clave(); val hijo = clave()
        val oferta = OfertaCp2(publica(hijo), UUID.randomUUID().toString(), azar())
        val a = AceptacionPairCp2(UUID.randomUUID().toString(), oferta.enrollId, oferta.enrollNonce, publica(padre), publica(hijo),
            "https://relay.example:443", HashesPairCp2(azar(), azar(), azar()))
        val p = CodecPairCp2.escribirPayload(a)
        // Padre PAIR/AUTH sólo helper JVM, jamás firmante padre MAIN.
        val pair = CodecPairCp2.escribirSobre(SobrePairCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(firma(padre, "CPv2/PAIR\n".toByteArray() + p))))
        val pruebaPadre = CodecAuthCp2.escribirPrueba(PruebaAuthCp2(a.pairId, RolAuthCp2.PARENT, UUID.randomUUID().toString(), azar()))
        val authPadre = CodecAuthCp2.escribir(SobreAuthCp2(Base64Cp1.codificar(pruebaPadre), Base64Cp1.codificar(firma(padre, "CPv2/AUTH\n".toByteArray() + pruebaPadre))))
        val authHijo = CriptografiaAuthCp2.firmarHijo(PruebaAuthCp2(a.pairId, RolAuthCp2.CHILD, UUID.randomUUID().toString(), azar()), publica(hijo),
            object : FirmanteAuthCp2 {
                override val publica = publica(hijo)
                override fun firmar(bytes: ByteArray) = firma(hijo, bytes)
            })
        fun t(s: String) = TextoJson(s)
        return ObjetoJson(mapOf("fixture" to t("CP2_PUBLICO_EFIMERO_SIN_AUTORIDAD"), "parentPublicKeyB64" to t(publica(padre)),
            "childPublicKeyB64" to t(publica(hijo)), "childOfferUtf8" to t(CodecOfertaCp2.escribir(oferta).toString(Charsets.UTF_8)),
            "pairFrameUtf8" to t(pair.toString(Charsets.UTF_8)), "pairPayloadSha256B64" to t(Base64Cp1.codificar(sha(p))),
            "authParentFrameUtf8" to t(authPadre.toString(Charsets.UTF_8)), "authChildFrameUtf8" to t(authHijo.toString(Charsets.UTF_8))))
    }
    @Test fun exportarUnaVueltaPublicaNuevaOchoCamposTrasAutoverificacion() {
        val o = generar(); verificar(o)
        if (System.getenv("HIJO_AUTH01_EXPORTAR") == "SI") {
            val archivo = File(raiz(), "pruebas/interop_auth_cp2_jvm_publica.json")
            val bytes = JsonCp1.serializar(o) + byteArrayOf(10)
            verificar(objeto(bytes)); check(archivo.createNewFile()) { "No sobrescribir vector existente" }
            archivo.writeBytes(bytes); assertArrayEquals(bytes, archivo.readBytes()); verificar(objeto(archivo.readBytes()))
        }
    }
    @Test fun relecturaVectorAuthNoAlteraVectorPairAnterior() {
        val archivo = File(raiz(), "pruebas/interop_auth_cp2_jvm_publica.json")
        val bytes = archivo.readBytes(); verificar(objeto(bytes)); assertEquals(10.toByte(), bytes.last())
        assertEquals("fa04fc863d7bace67b5036751e04865e2e6d90047d333252e4a70d21f5b36bf6",
            sha(File(raiz(), "pruebas/interop_cp2_jvm_publica.json").readBytes()).joinToString("") { "%02x".format(it) })
    }
}
