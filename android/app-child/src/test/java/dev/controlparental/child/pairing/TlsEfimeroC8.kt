package dev.controlparental.child.pairing

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/** Certificado autofirmado de laboratorio, efímero y sólo RAM; sin APIs JDK internas/dependencias nuevas. */
internal class TlsEfimeroC8 {
    private val par = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
    val certificado: X509Certificate
    val gestor: X509TrustManager
    val contexto: SSLContext
    init {
        val algoritmo = secuencia(oid(1, 2, 840, 10045, 4, 3, 2))
        val nombre = X500Principal("CN=Laboratorio C8 efimero").encoded
        val actual = Instant.now()
        val tiempo = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC)
        val san = secuencia(oid(2, 5, 29, 17), octetos(secuencia(der(0x87, byteArrayOf(127, 0, 0, 1)))))
        val ca = secuencia(oid(2, 5, 29, 19), der(1, byteArrayOf(-1)), octetos(secuencia(der(1, byteArrayOf(-1)))))
        val usos = secuencia(oid(2, 5, 29, 15), der(1, byteArrayOf(-1)), octetos(der(3, byteArrayOf(2, 0x84.toByte()))))
        val tbs = secuencia(der(0xa0, der(2, byteArrayOf(2))),
            der(2, BigInteger(128, SecureRandom()).add(BigInteger.ONE).toByteArray()), algoritmo, nombre,
            secuencia(der(0x17, tiempo.format(actual.minusSeconds(60)).toByteArray(Charsets.US_ASCII)),
                der(0x17, tiempo.format(actual.plusSeconds(600)).toByteArray(Charsets.US_ASCII))),
            nombre, par.public.encoded, der(0xa3, secuencia(san, ca, usos)))
        val firma = Signature.getInstance("SHA256withECDSA").run { initSign(par.private); update(tbs); sign() }
        certificado = CertificateFactory.getInstance("X.509").generateCertificate(
            secuencia(tbs, algoritmo, der(3, byteArrayOf(0) + firma)).inputStream()) as X509Certificate
        certificado.checkValidity(); certificado.verify(par.public)
        val almacen = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null); setCertificateEntry("efimero", certificado) }
        gestor = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(almacen); trustManagers.filterIsInstance<X509TrustManager>().single()
        }
        contexto = SSLContext.getInstance("TLS").apply { init(null, arrayOf(gestor), SecureRandom()) }
    }
    fun certPem() = pem("CERTIFICATE", certificado.encoded)
    fun keyPem() = pem("PRIVATE KEY", par.private.encoded)
    private fun pem(tipo: String, bytes: ByteArray) = "-----BEGIN $tipo-----\n" +
        Base64.getMimeEncoder(64, byteArrayOf(10)).encodeToString(bytes) + "\n-----END $tipo-----\n"
    private fun der(tipo: Int, bytes: ByteArray): ByteArray {
        val largo = if (bytes.size < 128) byteArrayOf(bytes.size.toByte())
            else if (bytes.size < 256) byteArrayOf(0x81.toByte(), bytes.size.toByte())
            else byteArrayOf(0x82.toByte(), (bytes.size shr 8).toByte(), bytes.size.toByte())
        return byteArrayOf(tipo.toByte()) + largo + bytes
    }
    private fun secuencia(vararg partes: ByteArray) = der(0x30, partes.fold(byteArrayOf()) { a, b -> a + b })
    private fun octetos(bytes: ByteArray) = der(4, bytes)
    private fun oid(vararg partes: Int): ByteArray {
        val b = mutableListOf((partes[0] * 40 + partes[1]).toByte())
        for (n in partes.drop(2)) {
            var v = n; val grupo = mutableListOf((v and 127).toByte()); v = v ushr 7
            while (v > 0) { grupo.add(0, ((v and 127) or 128).toByte()); v = v ushr 7 }
            b.addAll(grupo)
        }
        return der(6, b.toByteArray())
    }
}
