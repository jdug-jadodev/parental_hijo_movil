package dev.controlparental.protocol

import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.X509EncodedKeySpec

/** Pública SPKI DER P-256 validada con APIs estándar; nunca contiene una privada. */
class ClavePublicaCp1 private constructor(internal val clave: ECPublicKey, val base64: String) {
    companion object {
        internal val parametrosP256: ECParameterSpec by lazy {
            AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec("secp256r1")) }
                .getParameterSpec(ECParameterSpec::class.java)
        }

        fun leer(base64: String): ClavePublicaCp1 {
            exigir(base64.length in 100..200, "Tamaño de clave pública CP/1 inválido.")
            val bytes = Base64Cp1.decodificar(base64, 150)
            val clave = try {
                KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes)) as? ECPublicKey
                    ?: throw ErrorProtocolo(mensaje = "La clave pública debe ser EC.")
            } catch (_: GeneralSecurityException) {
                throw ErrorProtocolo(mensaje = "Clave pública SPKI inválida.")
            }
            val p = clave.params
            val esperado = parametrosP256
            exigir(p.curve == esperado.curve && p.generator == esperado.generator &&
                p.order == esperado.order && p.cofactor == esperado.cofactor,
                "La clave pública debe usar la curva P-256 exacta.")
            exigir(bytes.contentEquals(clave.encoded), "La clave pública no usa SPKI DER canónico.")
            return ClavePublicaCp1(clave, base64)
        }
    }
}
