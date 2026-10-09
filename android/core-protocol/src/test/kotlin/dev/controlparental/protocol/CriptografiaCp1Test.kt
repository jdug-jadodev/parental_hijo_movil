package dev.controlparental.protocol

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Identidad efímera únicamente en memoria de pruebas, sin claves fijas ni archivos privados. */
internal class FirmanteEfimero(curva: String = "secp256r1") : FirmanteCp1 {
    private val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(curva)) }.generateKeyPair()
    val publicaCodificada: String = Base64Cp1.codificar(par.public.encoded)
    override val clavePublica: ClavePublicaCp1 get() = ClavePublicaCp1.leer(publicaCodificada)
    override fun firmarBytes(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(par.private)
        update(bytes)
        sign()
    }
}

class CriptografiaCp1Test {
    private fun orden(): OrdenPolitica = CodecCp1.leerDocumentoSinAutorizar(
        EjemplosCp1.bytes("04_lock_for.payload.json")) as OrdenPolitica

    @Test
    fun `firma y verifica una orden nueva con identidad efimera`() {
        val firmante = FirmanteEfimero()
        val sobre = CriptografiaCp1.firmar(orden(), Proposito.MESSAGE, Rol.PARENT, firmante)
        val resultado = CriptografiaCp1.verificar(sobre, firmante.clavePublica, Rol.PARENT, Proposito.MESSAGE)
        assertEquals(orden(), resultado.mensaje)
        assertEquals(Base64Cp1.codificar(MessageDigest.getInstance("SHA-256")
            .digest(CodecCp1.serializar(orden()))), resultado.payloadSha256B64)
    }

    @Test
    fun `una firma valida no hace canonico JSON ambiguo ni autoriza campos extra`() {
        val firmante = FirmanteEfimero()
        val original = CodecCp1.serializar(orden()).decodeToString()
        val invalidos = listOf(original.replace("\"v\":1", "\"v\":1,\"v\":1"), " $original", "$original\n",
            original.replace("1800", "1800.0"), original.replace("1800", "18e2"),
            original.replace("\"v\":1", "\"v\":1,\"extra\":null"),
            original.replace("\"LOCK_FOR\"", "\"\\u004cOCK_FOR\""))
        invalidos.forEach { texto ->
            val bytes = texto.toByteArray()
            val firma = firmante.firmarBytes(CriptografiaCp1.bytesFirmados(Proposito.MESSAGE, bytes))
            val sobre = SobreSinVerificar(Proposito.MESSAGE, Base64Cp1.codificar(bytes), Base64Cp1.codificar(firma))
            val error = assertThrows(ErrorProtocolo::class.java) {
                CriptografiaCp1.verificar(sobre, firmante.clavePublica, Rol.PARENT, Proposito.MESSAGE)
            }
            assertEquals(CodigoCp1.BAD_SCHEMA, error.codigo)
        }
    }

    @Test
    fun `rechaza SPKI truncado otra curva otra clase de clave y base64 ambiguo`() {
        val valida = VectoresCripto.clave(Rol.PARENT).base64
        val bytes = Base64Cp1.decodificar(valida, 150)
        listOf(valida + "=", Base64Cp1.codificar(bytes.copyOf(bytes.size - 1)),
            Base64Cp1.codificar(bytes + byteArrayOf(0)), FirmanteEfimero("secp384r1").publicaCodificada,
            Base64Cp1.codificar(KeyPairGenerator.getInstance("RSA").apply { initialize(512) }.generateKeyPair().public.encoded),
            Base64Cp1.codificar(ByteArray(91))).forEach {
            assertThrows(ErrorProtocolo::class.java) { ClavePublicaCp1.leer(it) }
        }
    }

    @Test
    fun `DER rechaza JOSE negativos cero padding redundante y longitudes ambiguas`() {
        val minimo = byteArrayOf(0x30, 6, 2, 1, 1, 2, 1, 1)
        DerEcdsaCp1.validar(minimo)
        listOf(ByteArray(64), minimo + byteArrayOf(0), minimo.copyOf(7),
            minimo.copyOf().apply { this[4] = 0 }, minimo.copyOf().apply { this[4] = 0x80.toByte() },
            byteArrayOf(0x30, 7, 2, 2, 0, 1, 2, 1, 1),
            byteArrayOf(0x30, 0x81.toByte(), 6, 2, 1, 1, 2, 1, 1)).forEach {
            assertThrows(ErrorProtocolo::class.java) { DerEcdsaCp1.validar(it) }
        }
        assertThrows(ErrorProtocolo::class.java) {
            DerEcdsaCp1.validar(der(BigInteger.ONE, ClavePublicaCp1.parametrosP256.order))
        }
    }

    @Test
    fun `acepta representaciones S complementarias sin cambiar el hash del payload`() {
        val sobre = VectoresCripto.sobre("04_lock_for")
        val firma = Base64Cp1.decodificar(sobre.sigB64, 72)
        val longitudR = firma[3].toInt()
        val r = BigInteger(firma.copyOfRange(4, 4 + longitudR))
        val s = BigInteger(firma.copyOfRange(6 + longitudR, firma.size))
        val alternativa = der(r, ClavePublicaCp1.parametrosP256.order - s)
        val verificado = CriptografiaCp1.verificar(sobre.copy(sigB64 = Base64Cp1.codificar(alternativa)),
            VectoresCripto.clave(Rol.PARENT), Rol.PARENT, Proposito.MESSAGE)
        assertEquals(VectoresCripto.hash("04_lock_for"), verificado.payloadSha256B64)
        assertTrue(!firma.contentEquals(alternativa))
    }

    @Test
    fun `rechaza firmar con otro rol o proposito y no acepta firma falsa del adaptador`() {
        val firmante = FirmanteEfimero()
        assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.firmar(orden(), Proposito.AUTH, Rol.PARENT, firmante) }
        assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.firmar(orden(), Proposito.MESSAGE, Rol.CHILD, firmante) }
        val falso = object : FirmanteCp1 {
            override val clavePublica = firmante.clavePublica
            override fun firmarBytes(bytes: ByteArray) = byteArrayOf(0x30, 6, 2, 1, 1, 2, 1, 1)
        }
        assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.firmar(orden(), Proposito.MESSAGE, Rol.PARENT, falso) }
    }

    private fun der(r: BigInteger, s: BigInteger): ByteArray {
        val rb = r.toByteArray()
        val sb = s.toByteArray()
        return byteArrayOf(0x30, (4 + rb.size + sb.size).toByte(), 2, rb.size.toByte()) +
            rb + byteArrayOf(2, sb.size.toByte()) + sb
    }

    @Test
    fun `vinculo no permite sustituir la publica del firmante ni incorporar otra curva`() {
        val mensaje = CodecCp1.leerDocumentoSinAutorizar(EjemplosCp1.bytes("07_pair_accept.payload.json")) as AceptacionVinculo
        val firmante = FirmanteEfimero()
        assertThrows(ErrorProtocolo::class.java) {
            CriptografiaCp1.firmar(mensaje, Proposito.PAIR, Rol.PARENT, firmante)
        }
        assertThrows(ErrorProtocolo::class.java) {
            CriptografiaCp1.firmar(mensaje.copy(parentPublicKeyB64 = firmante.clavePublica.base64,
                childPublicKeyB64 = FirmanteEfimero("secp384r1").publicaCodificada), Proposito.PAIR, Rol.PARENT, firmante)
        }
    }
}
