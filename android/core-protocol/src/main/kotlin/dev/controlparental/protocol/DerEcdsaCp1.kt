package dev.controlparental.protocol

import java.math.BigInteger

/** Comprueba el contenedor DER; el cálculo y verificación ECDSA pertenecen a JCA. */
internal object DerEcdsaCp1 {
    fun validar(bytes: ByteArray) {
        fun comprobar(condicion: Boolean) {
            if (!condicion) throw ErrorProtocolo(CodigoCp1.BAD_SIGNATURE, "Firma ECDSA DER inválida.")
        }
        comprobar(bytes.size in 8..72)
        comprobar(bytes[0] == 0x30.toByte() && bytes[1].toInt() == bytes.size - 2)
        var posicion = 2
        repeat(2) {
            comprobar(posicion + 2 <= bytes.size && bytes[posicion++] == 0x02.toByte())
            val longitud = bytes[posicion++].toInt() and 0xff
            comprobar(longitud in 1..33 && posicion + longitud <= bytes.size)
            val entero = bytes.copyOfRange(posicion, posicion + longitud)
            comprobar((entero[0].toInt() and 0x80) == 0)
            comprobar(longitud == 1 || entero[0] != 0.toByte() || (entero[1].toInt() and 0x80) != 0)
            val numero = BigInteger(entero)
            comprobar(numero.signum() > 0 && numero < ClavePublicaCp1.parametrosP256.order)
            posicion += longitud
        }
        comprobar(posicion == bytes.size)
        // CP/1 no impone low-S: no rechazar otra representación DER matemáticamente válida.
    }
}
