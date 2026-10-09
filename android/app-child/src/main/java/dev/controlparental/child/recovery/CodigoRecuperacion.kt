package dev.controlparental.child.recovery

import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.protocol.Base64Cp1
import java.security.MessageDigest

/** CP/1 §9: sin código maestro, coerciones Unicode ni registro del texto recibido. */
object CodigoRecuperacion {
    fun normalizar(texto: String): String? {
        if (texto.length !in 32..128) return null
        val resultado = StringBuilder(32)
        for (caracter in texto) {
            when (caracter) {
                '-', ' ' -> Unit
                in 'a'..'z' -> resultado.append(caracter.uppercaseChar())
                in 'A'..'Z', in '2'..'7' -> resultado.append(caracter)
                else -> return null
            }
            if (resultado.length > 32) return null
        }
        return resultado.toString().takeIf { it.length == 32 && it.all { c -> c in 'A'..'Z' || c in '2'..'7' } }
    }

    fun hash(pairId: String, proposito: PropositoRecuperacionLocal, codigoNormalizado: String): String {
        require(UUID_V4.matches(pairId))
        require(codigoNormalizado.length == 32 && normalizar(codigoNormalizado) == codigoNormalizado)
        val bytes = "CPv1/RECOVERY\n$pairId\n${proposito.name}\n$codigoNormalizado".toByteArray(Charsets.UTF_8)
        return Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(bytes))
    }

    fun coincide(pairId: String, proposito: PropositoRecuperacionLocal, texto: String, esperado: String): Boolean {
        val codigo = normalizar(texto) ?: return false
        val calculado = Base64Cp1.decodificar(hash(pairId, proposito, codigo), 32)
        val confiable = Base64Cp1.decodificar(esperado, 32)
        require(confiable.size == 32)
        return MessageDigest.isEqual(calculado, confiable)
    }

    private val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
}
