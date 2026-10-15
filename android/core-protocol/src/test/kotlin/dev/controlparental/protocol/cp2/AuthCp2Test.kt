package dev.controlparental.protocol.cp2

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class AuthCp2Test {
    private val clave = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private val publica = Base64Cp1.codificar(clave.public.encoded)
    private val id = "12345678-1234-4234-8234-123456789abc"
    private val nonce = Base64Cp1.codificar(ByteArray(32) { 1 })
    private fun prueba(rol: RolAuthCp2 = RolAuthCp2.CHILD) = PruebaAuthCp2(id, rol, id, nonce)
    private val firmante = object : FirmanteAuthCp2 {
        override val publica get() = this@AuthCp2Test.publica
        override fun firmar(bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update(bytes); sign() }
    }
    private fun rechaza(a: () -> Unit) { assertThrows(Exception::class.java) { a() } }
    private fun obj(b: ByteArray) = JsonCp1.leer(b) as ObjetoJson
    @Test fun framesCerradosCincoCuatroTresYSobreCincoPruebaSiete() {
        val firmado = CriptografiaAuthCp2.firmarHijo(prueba(), publica, firmante)
        val frames = listOf(RetoAuthCp2(id, nonce) to 5, OkAuthCp2(id) to 4,
            LatidoAuthCp2(TipoLatidoCp2.PING, nonce) to 3, LatidoAuthCp2(TipoLatidoCp2.PONG, nonce) to 3,
            CodecAuthCp2.leer(firmado) to 5)
        for ((f, cantidad) in frames) {
            val b = CodecAuthCp2.escribir(f); assertEquals(f, CodecAuthCp2.leer(b)); val o = obj(b)
            assertEquals(cantidad, o.campos.size)
            for (k in o.campos.keys) rechaza { CodecAuthCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos - k))) }
            rechaza { CodecAuthCp2.leer(JsonCp1.serializar(ObjetoJson(o.campos + ("alg" to TextoJson("ES256"))))) }
        }
        val p = obj(CodecAuthCp2.escribirPrueba(prueba())); assertEquals(7, p.campos.size)
        for (k in p.campos.keys) rechaza { CodecAuthCp2.leerPrueba(JsonCp1.serializar(ObjetoJson(p.campos - k))) }
        rechaza { CodecAuthCp2.leerPrueba(JsonCp1.serializar(ObjetoJson(p.campos + ("publicKey" to TextoJson(publica))))) }
    }
    @Test fun firmaHijoRealNoPadreRolClaveBytesYDominiosCruzados() {
        val f = CriptografiaAuthCp2.firmarHijo(prueba(), publica, firmante)
        val v = CriptografiaAuthCp2.verificar(f, publica, RolAuthCp2.CHILD)
        assertEquals(prueba(), v.prueba); assertArrayEquals(CodecAuthCp2.escribirPrueba(prueba()), v.bytesPayload())
        val copia = v.bytesPayload(); copia.fill(0); assertNotEquals(0.toByte(), v.bytesPayload()[0])
        rechaza { CriptografiaAuthCp2.verificar(f, publica, RolAuthCp2.PARENT) }
        rechaza { CriptografiaAuthCp2.firmarHijo(prueba(RolAuthCp2.PARENT), publica, firmante) }
        val otra = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        rechaza { CriptografiaAuthCp2.verificar(f, Base64Cp1.codificar(otra.public.encoded), RolAuthCp2.CHILD) }
        for (dominio in listOf("CPv1/AUTH\n", "CPv2/PAIR\n", "CPv2/AUTH\\n", "CPv2/AUTH\r\n")) {
            val p = CodecAuthCp2.escribirPrueba(prueba()); val sig = firmante.firmar(dominio.toByteArray() + p)
            rechaza { CriptografiaAuthCp2.verificar(CodecAuthCp2.escribir(SobreAuthCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(sig))), publica, RolAuthCp2.CHILD) }
        }
        rechaza { CodecPairCp2.leerSobre(f) }
    }
    @Test fun canonicalAsciiUtf8DuplicadosVersionTimeoutProtocolYTipos() {
        val b = CodecAuthCp2.escribir(RetoAuthCp2(id, nonce)); val texto = b.toString(Charsets.UTF_8)
        for (s in listOf(" $texto", texto.replace(",", ", "), texto.replace("{", "{\"v\":2,"), texto.replace("CHALLENGE", "CH\\u0041LLENGE"),
            texto.replace("\"v\":2", "\"v\":1"), texto.replace("\"timeoutSec\":10", "\"timeoutSec\":11"),
            texto.replace("\"timeoutSec\":10", "\"timeoutSec\":\"10\""), "é", "\n$texto")) rechaza { CodecAuthCp2.leer(s.toByteArray()) }
        val p = CodecAuthCp2.escribirPrueba(prueba()).toString(Charsets.UTF_8)
        for (s in listOf(p.replace("cp.v2", "cp.v1"), p.replace("CHILD", "child"), p.replace("\"role\":\"CHILD\"", "\"role\":null")))
            rechaza { CodecAuthCp2.leerPrueba(s.toByteArray()) }
    }
    @Test fun uuidNonceB64ProfundidadYLimites() {
        for (malo in listOf(id.uppercase(), id.replace("-4234-", "-3234-"), id.replace("-8234-", "-7234-")))
            rechaza { CodecAuthCp2.escribir(RetoAuthCp2(malo, nonce)) }
        for (malo in listOf(nonce + "=", nonce.dropLast(1) + "B", Base64Cp1.codificar(ByteArray(31))))
            rechaza { CodecAuthCp2.escribir(RetoAuthCp2(id, malo)) }
        for (b in listOf(ByteArray(16385), byteArrayOf(0xc3.toByte(), 0x28), ("[".repeat(7) + "0" + "]".repeat(7)).toByteArray())) rechaza { CodecAuthCp2.leer(b) }
        rechaza { CodecAuthCp2.leerPrueba(ByteArray(8193)) }
        val s = CodecAuthCp2.leer(CriptografiaAuthCp2.firmarHijo(prueba(), publica, firmante)) as SobreAuthCp2
        rechaza { CodecAuthCp2.escribir(s.copy(payloadB64 = s.payloadB64 + "=")) }
        rechaza { CodecAuthCp2.escribir(s.copy(sigB64 = s.sigB64 + "=")) }
    }
    @Test fun derRangoYFirmaMutadaRechazados() {
        val s = CodecAuthCp2.leer(CriptografiaAuthCp2.firmarHijo(prueba(), publica, firmante)) as SobreAuthCp2
        val sig = Base64Cp1.decodificar(s.sigB64, 72).also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        rechaza { CriptografiaAuthCp2.verificar(CodecAuthCp2.escribir(s.copy(sigB64 = Base64Cp1.codificar(sig))), publica, RolAuthCp2.CHILD) }
        val n = BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16).toByteArray()
        for (firma in listOf(ByteArray(64), byteArrayOf(0x30, 6, 2, 1, 0, 2, 1, 1), byteArrayOf(0x30, (n.size + 5).toByte(), 2, n.size.toByte()) + n + byteArrayOf(2, 1, 1)))
            rechaza { CodecAuthCp2.escribir(s.copy(sigB64 = Base64Cp1.codificar(firma))) }
    }
}
