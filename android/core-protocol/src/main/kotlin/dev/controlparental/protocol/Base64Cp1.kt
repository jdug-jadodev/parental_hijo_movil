package dev.controlparental.protocol

import java.util.Base64

/** Base64url canónico sin padding; valida límites antes de reservar el resultado. */
object Base64Cp1 {
    fun codificar(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decodificar(texto: String, maximoBytes: Int): ByteArray {
        exigir(maximoBytes in 1..JsonCp1.MAX_FRAME_BYTES, "Límite base64 inválido.")
        val maximoCaracteres = (maximoBytes * 4 + 2) / 3
        exigir(texto.isNotEmpty() && texto.length <= maximoCaracteres, "Tamaño base64 inválido.")
        exigir(texto.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' },
            "Base64url contiene caracteres no permitidos.")
        val bytes = try {
            Base64.getUrlDecoder().decode(texto)
        } catch (_: IllegalArgumentException) {
            throw ErrorProtocolo(mensaje = "Base64url inválido.")
        }
        exigir(bytes.size <= maximoBytes && codificar(bytes) == texto, "Base64url no canónico.")
        return bytes
    }
}
