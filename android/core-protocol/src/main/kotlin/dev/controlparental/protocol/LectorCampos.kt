package dev.controlparental.protocol

/** Lee cada campo con su tipo exacto y comprueba que no quedaron propiedades extra. */
internal class LectorCampos(private val objeto: ObjetoJson) {
    private val leidos = mutableSetOf<String>()

    fun valor(nombre: String): ValorJson {
        leidos += nombre
        return objeto.campos[nombre] ?: throw ErrorProtocolo(mensaje = "Falta un campo obligatorio.")
    }

    fun texto(nombre: String): String = (valor(nombre) as? TextoJson)?.texto
        ?: throw ErrorProtocolo(mensaje = "Se esperaba un campo de texto.")

    fun entero(nombre: String): Long = (valor(nombre) as? EnteroJson)?.numero
        ?: throw ErrorProtocolo(mensaje = "Se esperaba un entero.")

    fun booleano(nombre: String): Boolean = (valor(nombre) as? BooleanoJson)?.valor
        ?: throw ErrorProtocolo(mensaje = "Se esperaba un booleano.")

    fun literal(nombre: String, esperado: String) {
        exigir(texto(nombre) == esperado, "Valor fijo del mensaje incorrecto.")
    }

    inline fun <reified E : Enum<E>> enumerado(nombre: String): E {
        val texto = texto(nombre)
        return enumValues<E>().firstOrNull { it.name == texto }
            ?: throw ErrorProtocolo(mensaje = "Valor enumerado desconocido.")
    }

    fun uuid(nombre: String): String = validarUuid(texto(nombre))

    fun uuidOpcional(nombre: String): String? = when (val valor = valor(nombre)) {
        NuloJson -> null
        is TextoJson -> validarUuid(valor.texto)
        else -> throw ErrorProtocolo(mensaje = "Identificador opcional inválido.")
    }

    fun bytes32(nombre: String): String = validarBytes32(texto(nombre))

    fun bytes32Opcionales(nombre: String): String? = when (val valor = valor(nombre)) {
        NuloJson -> null
        is TextoJson -> validarBytes32(valor.texto)
        else -> throw ErrorProtocolo(mensaje = "Hash opcional inválido.")
    }

    fun secuencia(nombre: String, permiteCero: Boolean = false): Long {
        val texto = texto(nombre)
        val formato = if (permiteCero) Regex("0|[1-9][0-9]{0,18}") else Regex("[1-9][0-9]{0,18}")
        exigir(formato.matches(texto), "Formato de secuencia inválido.")
        return texto.toLongOrNull() ?: throw ErrorProtocolo(mensaje = "Secuencia fuera de 64 bits.")
    }

    fun segundosOpcionales(nombre: String): Long? = when (val valor = valor(nombre)) {
        NuloJson -> null
        is EnteroJson -> valor.numero.also { exigir(it in 0..604800, "Segundos fuera de rango.") }
        else -> throw ErrorProtocolo(mensaje = "Duración opcional inválida.")
    }

    fun clavePublica(nombre: String): String = texto(nombre).also {
        exigir(it.length in 100..200, "Tamaño de clave pública inválido.")
        Base64Cp1.decodificar(it, 150)
        // SPKI DER y curva P-256 se comprobarán con APIs criptográficas en C02.
    }

    fun terminar() {
        exigir(leidos == objeto.campos.keys, "El mensaje contiene campos desconocidos.")
    }

    companion object {
        private val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

        private fun validarUuid(texto: String): String = texto.also {
            exigir(UUID_V4.matches(it), "Se requiere un UUID v4 en minúsculas.")
        }

        private fun validarBytes32(texto: String): String = texto.also {
            exigir(it.length == 43 && Base64Cp1.decodificar(it, 32).size == 32,
                "Se requieren exactamente 32 bytes en base64url canónico.")
        }
    }
}
