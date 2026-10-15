package dev.controlparental.child.pairing

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

/** Laboratorio JVM, sin Android/Keystore ni autoridad. AUTH del vector NO se evalúa. */
class InteropCp2Test {
    private val etiqueta = "CP2_PUBLICO_EFIMERO_SIN_AUTORIDAD"
    private val clavesFixture = setOf("fixture", "parentPublicKeyB64", "childPublicKeyB64", "childOfferUtf8", "pairFrameUtf8", "pairPayloadSha256B64")
    private val camposOferta = setOf("v", "kind", "protocol", "childPublicKeyB64", "enrollId", "enrollNonce")
    private val camposPair = setOf("v", "kind", "protocol", "pairId", "enrollId", "enrollNonce", "parentPublicKeyB64", "childPublicKeyB64", "relayOrigin", "recoveryHashes")
    private val camposSobre = setOf("v", "kind", "purpose", "payloadB64", "sigB64")
    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b)
    private fun objeto(b: ByteArray) = JsonCp1.leer(b) as ObjetoJson
    private fun ObjetoJson.texto(k: String) = (campos.getValue(k) as TextoJson).texto
    private fun textoBytes(o: ObjetoJson, k: String) = o.texto(k).toByteArray(Charsets.UTF_8)
    private fun fuente(): ByteArray = javaClass.getResourceAsStream("/cp2/cp2-i01-publico.json")!!.use { it.readBytes() }
    private fun clave() = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private fun publica(k: KeyPair) = Base64Cp1.codificar(k.public.encoded)
    private fun azar32() = Base64Cp1.codificar(ByteArray(32).also(SecureRandom()::nextBytes))
    private fun firmar(p: ByteArray, padre: KeyPair, dominio: String = "CPv2/PAIR\n") =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(padre.private); update(dominio.toByteArray(Charsets.UTF_8)); update(p); sign()
        }
    private fun sobre(p: ByteArray, firma: ByteArray) = CodecPairCp2.escribirSobre(SobrePairCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(firma)))
    private fun rechaza(accion: () -> Unit) { assertThrows(Exception::class.java) { accion() } }

    /** Pasa SIEMPRE por ambos codecs CP2 y CriptografiaPairCp2, nunca sólo JCA.verify. */
    private fun verificar(f: ObjetoJson): IntegridadPairCp2 {
        assertEquals(etiqueta, f.texto("fixture"))
        val oBytes = textoBytes(f, "childOfferUtf8"); val frame = textoBytes(f, "pairFrameUtf8")
        val oferta = CodecOfertaCp2.leer(oBytes)
        assertArrayEquals(oBytes, CodecOfertaCp2.escribir(oferta))
        assertEquals(camposOferta, objeto(oBytes).campos.keys)
        assertEquals(camposSobre, objeto(frame).campos.keys)
        val s = CodecPairCp2.leerSobre(frame)
        assertArrayEquals(frame, CodecPairCp2.escribirSobre(s))
        val p = Base64Cp1.decodificar(s.payloadB64, CodecPairCp2.MAX_PAYLOAD)
        assertEquals(camposPair, objeto(p).campos.keys)
        val a = CodecPairCp2.leerPayload(p)
        assertArrayEquals(p, CodecPairCp2.escribirPayload(a))
        val v = CriptografiaPairCp2.verificarIntegridad(frame, f.texto("parentPublicKeyB64"))
        assertArrayEquals(frame, v.bytesFrame()); assertArrayEquals(p, v.bytesPayload())
        assertEquals(f.texto("pairPayloadSha256B64"), v.payloadSha256B64)
        assertEquals(Base64Cp1.codificar(sha(p)), v.payloadSha256B64)
        assertEquals(a, v.aceptacion)
        assertEquals(f.texto("childPublicKeyB64"), oferta.childPublicKeyB64)
        assertEquals(oferta.childPublicKeyB64, a.childPublicKeyB64)
        assertEquals(f.texto("parentPublicKeyB64"), a.parentPublicKeyB64)
        assertEquals(oferta.enrollId, a.enrollId); assertEquals(oferta.enrollNonce, a.enrollNonce)
        assertEquals(32, Base64Cp1.decodificar(a.enrollNonce, 32).size)
        assertEquals("https://relay.example:443", a.relayOrigin)
        return v
    }
    @Test fun fuenteNodeExactaYPairCompartidoValidoConCodecReal() {
        val bytes = fuente()
        assertEquals("967ae05aecd28c6b7f90b8ae90e8421b408dd70acbf22fd30911085c69a964ec", sha(bytes).joinToString("") { "%02x".format(it) })
        assertEquals(10.toByte(), bytes.last())
        val f = objeto(bytes)
        assertEquals(clavesFixture + setOf("authParentFrameUtf8", "authChildFrameUtf8"), f.campos.keys)
        val v = verificar(f)
        assertEquals("6442f9e0-52d5-4020-abe0-ddccfecdb3d4", v.aceptacion.enrollId)
        assertEquals("0eb84428-63f1-4d1e-9787-eac194d06359", v.aceptacion.pairId)
        // AUTH_NO_EVALUADO_HIJO: sólo se comprueba presencia de campos adicionales, no sus frames.
    }
    @Test fun nodeRechazaClaveExternaDistintaBytesMutadosYCamposAjenos() {
        val f = objeto(fuente()); val frame = textoBytes(f, "pairFrameUtf8")
        rechaza { CriptografiaPairCp2.verificarIntegridad(frame, publica(clave())) }
        val s = CodecPairCp2.leerSobre(frame); val p = Base64Cp1.decodificar(s.payloadB64, 8192)
        val a = CodecPairCp2.leerPayload(p)
        val cambiado = CodecPairCp2.escribirPayload(a.copy(relayOrigin = "https://otro.example:443"))
        rechaza { CriptografiaPairCp2.verificarIntegridad(CodecPairCp2.escribirSobre(s.copy(payloadB64 = Base64Cp1.codificar(cambiado))), f.texto("parentPublicKeyB64")) }
        for (k in camposPair) rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(objeto(p).campos - k))) }
        rechaza { CodecPairCp2.leerPayload(JsonCp1.serializar(ObjetoJson(objeto(p).campos + ("extra" to TextoJson("no"))))) }
        for (k in camposSobre) rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(objeto(frame).campos - k))) }
        rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(objeto(frame).campos + ("extra" to TextoJson("no"))))) }
        val oferta = textoBytes(f, "childOfferUtf8")
        for (k in camposOferta) rechaza { CodecOfertaCp2.leer(JsonCp1.serializar(ObjetoJson(objeto(oferta).campos - k))) }
        rechaza { CodecOfertaCp2.leer(JsonCp1.serializar(ObjetoJson(objeto(oferta).campos + ("extra" to TextoJson("no"))))) }
        rechaza { CodecOfertaCp2.leer(oferta + byteArrayOf(32)) }
        rechaza { CodecPairCp2.leerSobre(frame + byteArrayOf(32)) }
    }
    @Test fun dominiosV1LfLiteralYOtroPropositoNoPasanCodecCriptoV2() {
        val padre = clave(); val f = generar(padre); val v = verificar(f)
        for (prefijo in listOf("CPv1/PAIR\n", "CPv2/PAIR\\n", "CPv2/AUTH\n"))
            rechaza { CriptografiaPairCp2.verificarIntegridad(sobre(v.bytesPayload(), firmar(v.bytesPayload(), padre, prefijo)), publica(padre)) }
        val frame = objeto(v.bytesFrame())
        rechaza { CodecPairCp2.leerSobre(JsonCp1.serializar(ObjetoJson(frame.campos + ("purpose" to TextoJson("AUTH"))))) }
    }
    private fun generar(padre: KeyPair): ObjetoJson {
        val hijo = clave()
        val o = OfertaCp2(publica(hijo), UUID.randomUUID().toString(), azar32())
        val a = AceptacionPairCp2(UUID.randomUUID().toString(), o.enrollId, o.enrollNonce, publica(padre), o.childPublicKeyB64,
            "https://relay.example:443", HashesPairCp2(azar32(), azar32(), azar32()))
        val p = CodecPairCp2.escribirPayload(a)
        val frame = sobre(p, firmar(p, padre))
        return ObjetoJson(mapOf("fixture" to TextoJson(etiqueta), "parentPublicKeyB64" to TextoJson(publica(padre)),
            "childPublicKeyB64" to TextoJson(o.childPublicKeyB64), "childOfferUtf8" to TextoJson(CodecOfertaCp2.escribir(o).toString(Charsets.UTF_8)),
            "pairFrameUtf8" to TextoJson(frame.toString(Charsets.UTF_8)), "pairPayloadSha256B64" to TextoJson(Base64Cp1.codificar(sha(p)))))
    }
    @Test fun vueltaJvmSoloSePublicaTrasAceptarMismosCodecsCripto() {
        val f = generar(clave()); verificar(f)
        assertEquals(clavesFixture, f.campos.keys)
        val salida = JsonCp1.serializar(f) + byteArrayOf(10)
        verificar(objeto(salida))
        if (System.getenv("HIJO_INTEROP01_EXPORTAR") == "SI") {
            val archivo = File(raiz(), "pruebas/interop_cp2_jvm_publica.json")
            check(archivo.createNewFile()) { "Salida ya existente: no regenerar ni sobrescribir." }
            archivo.writeBytes(salida)
            assertArrayEquals(salida, archivo.readBytes()); verificar(objeto(archivo.readBytes()))
        }
    }
    private fun raiz() = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "00_EMPIEZA_AQUI.md").isFile }
    @Test fun salidaPublicadaPermanecePublicaYRevalidaSinRegenerarla() {
        val bytes = File(raiz(), "pruebas/interop_cp2_jvm_publica.json").readBytes()
        assertEquals(10.toByte(), bytes.last())
        val f = objeto(bytes); assertEquals(clavesFixture, f.campos.keys)
        verificar(f)
        assertArrayEquals(bytes, JsonCp1.serializar(f) + byteArrayOf(10))
    }
}
