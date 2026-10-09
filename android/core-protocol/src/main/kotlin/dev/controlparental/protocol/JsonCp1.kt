package dev.controlparental.protocol

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

sealed interface ValorJson
data class ObjetoJson(val campos: Map<String, ValorJson>) : ValorJson
data class ListaJson(val elementos: List<ValorJson>) : ValorJson
data class TextoJson(val texto: String) : ValorJson
data class EnteroJson(val numero: Long) : ValorJson
data class BooleanoJson(val valor: Boolean) : ValorJson
data object NuloJson : ValorJson

/** Subconjunto JSON de CP/1, sin coerciones, duplicados ni reemplazo de UTF-8. */
object JsonCp1 {
    const val MAX_FRAME_BYTES = 16384
    const val MAX_PAYLOAD_BYTES = 8192
    const val MAX_ENTERO_SEGURO = 9007199254740991L

    fun leer(bytes: ByteArray, limite: Int = MAX_FRAME_BYTES): ValorJson {
        exigir(limite in 1..MAX_FRAME_BYTES, "Límite de lectura inválido.")
        exigir(bytes.isNotEmpty() && bytes.size <= limite, "Tamaño de JSON inválido.")
        val texto = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            throw ErrorProtocolo(mensaje = "El mensaje no contiene UTF-8 válido.")
        }
        return Lector(texto).leer()
    }

    fun leerCanonico(bytes: ByteArray): ValorJson {
        val valor = leer(bytes, MAX_PAYLOAD_BYTES)
        exigir(bytes.contentEquals(serializar(valor)), "El payload no usa los bytes canónicos CP/1.")
        return valor
    }

    fun serializar(valor: ValorJson): ByteArray = escribir(valor, 0).toByteArray(Charsets.UTF_8)

    private fun escribir(valor: ValorJson, profundidad: Int): String {
        exigir(profundidad <= 32, "JSON demasiado anidado.")
        return when (valor) {
            is ObjetoJson -> valor.campos.toSortedMap().entries.joinToString(",", "{", "}") {
                cadena(it.key) + ":" + escribir(it.value, profundidad + 1)
            }
            is ListaJson -> valor.elementos.joinToString(",", "[", "]") {
                escribir(it, profundidad + 1)
            }
            is TextoJson -> cadena(valor.texto)
            is EnteroJson -> {
                exigir(valor.numero in 0..MAX_ENTERO_SEGURO, "Entero fuera del subconjunto CP/1.")
                valor.numero.toString()
            }
            is BooleanoJson -> valor.valor.toString()
            NuloJson -> "null"
        }
    }

    private fun cadena(texto: String): String {
        exigir(texto.all { it.code in 0x20..0x7e }, "La cadena debe ser ASCII imprimible.")
        return "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private class Lector(private val texto: String) {
        private var posicion = 0

        fun leer(): ValorJson {
            val valor = valor(0)
            espacios()
            exigir(posicion == texto.length, "Contenido adicional después del JSON.")
            return valor
        }

        private fun valor(profundidad: Int): ValorJson {
            exigir(profundidad <= 32, "JSON demasiado anidado.")
            espacios()
            return when (actual()) {
                '{' -> objeto(profundidad)
                '[' -> lista(profundidad)
                '"' -> TextoJson(cadena())
                't' -> literal("true", BooleanoJson(true))
                'f' -> literal("false", BooleanoJson(false))
                'n' -> literal("null", NuloJson)
                in '0'..'9' -> numero()
                else -> throw ErrorProtocolo(mensaje = "Valor JSON inválido.")
            }
        }

        private fun objeto(profundidad: Int): ObjetoJson {
            consumir('{')
            val campos = linkedMapOf<String, ValorJson>()
            espacios()
            if (tomar('}')) return ObjetoJson(campos.toMap())
            do {
                espacios()
                exigir(actual() == '"', "Se esperaba una propiedad JSON.")
                val clave = cadena()
                exigir(!campos.containsKey(clave), "No se permiten propiedades duplicadas.")
                espacios()
                consumir(':')
                campos[clave] = valor(profundidad + 1)
                espacios()
            } while (tomar(','))
            consumir('}')
            return ObjetoJson(campos.toMap())
        }

        private fun lista(profundidad: Int): ListaJson {
            consumir('[')
            val elementos = mutableListOf<ValorJson>()
            espacios()
            if (tomar(']')) return ListaJson(elementos.toList())
            do {
                elementos += valor(profundidad + 1)
                espacios()
            } while (tomar(','))
            consumir(']')
            return ListaJson(elementos.toList())
        }

        private fun cadena(): String {
            consumir('"')
            val salida = StringBuilder()
            while (actual() != '"') {
                exigir(posicion < texto.length, "Cadena JSON sin cierre.")
                var caracter = texto[posicion++]
                if (caracter == '\\') {
                    exigir(posicion < texto.length, "Escape JSON incompleto.")
                    caracter = when (texto[posicion++]) {
                        '"' -> '"'
                        '\\' -> '\\'
                        '/' -> '/'
                        'u' -> unicode()
                        else -> throw ErrorProtocolo(mensaje = "Escape fuera del subconjunto CP/1.")
                    }
                }
                exigir(caracter.code in 0x20..0x7e, "La cadena debe ser ASCII imprimible.")
                salida.append(caracter)
            }
            consumir('"')
            return salida.toString()
        }

        private fun unicode(): Char {
            exigir(posicion + 4 <= texto.length, "Escape Unicode incompleto.")
            val digitos = texto.substring(posicion, posicion + 4)
            exigir(digitos.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }, "Escape Unicode inválido.")
            posicion += 4
            return digitos.toInt(16).toChar()
        }

        private fun numero(): EnteroJson {
            val inicio = posicion
            while (actual() in '0'..'9') posicion++
            val digitos = texto.substring(inicio, posicion)
            exigir(digitos == "0" || !digitos.startsWith('0'), "Entero con cero inicial.")
            val numero = digitos.toLongOrNull()
            exigir(numero != null && numero in 0..MAX_ENTERO_SEGURO, "Entero fuera de rango.")
            return EnteroJson(numero!!)
        }

        private fun literal(palabra: String, valor: ValorJson): ValorJson {
            exigir(texto.startsWith(palabra, posicion), "Literal JSON inválido.")
            posicion += palabra.length
            return valor
        }

        private fun espacios() {
            while (actual() in listOf(' ', '\t', '\r', '\n')) posicion++
        }

        private fun actual(): Char = texto.getOrNull(posicion) ?: '\u0000'

        private fun tomar(caracter: Char): Boolean {
            if (actual() != caracter) return false
            posicion++
            return true
        }

        private fun consumir(caracter: Char) {
            exigir(tomar(caracter), "Estructura JSON incompleta o inválida.")
        }
    }
}
