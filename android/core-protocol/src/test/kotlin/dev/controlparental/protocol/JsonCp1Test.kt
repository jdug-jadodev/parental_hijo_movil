package dev.controlparental.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonCp1Test {
    @Test
    fun `ordena propiedades y conserva el orden de listas`() {
        val valor = JsonCp1.leer("{\"z\":[2,1,null],\"a\":true}".toByteArray())
        assertEquals("{\"a\":true,\"z\":[2,1,null]}", JsonCp1.serializar(valor).decodeToString())
    }

    @Test
    fun `escapa comillas y barras invertidas sin cambiar otros caracteres ASCII`() {
        val texto = TextoJson("ruta \\ y \"comillas\" /~")
        assertEquals(texto, JsonCp1.leerCanonico(JsonCp1.serializar(texto)))
    }

    @Test
    fun `rechaza numeros ambiguos o fuera del entero seguro`() {
        listOf("-0", "-1", "1.0", "1e0", "01", "+1", "9007199254740992", "99999999999999999999").forEach {
            assertThrows(it, ErrorProtocolo::class.java) { JsonCp1.leer(it.toByteArray()) }
        }
        assertEquals(EnteroJson(9007199254740991), JsonCp1.leer("9007199254740991".toByteArray()))
    }

    @Test
    fun `rechaza propiedades duplicadas aunque usen escapes distintos`() {
        listOf("{\"v\":1,\"v\":2}", "{\"v\":1,\"\\u0076\":1}").forEach {
            assertThrows(ErrorProtocolo::class.java) { JsonCp1.leer(it.toByteArray()) }
        }
    }

    @Test
    fun `payload exige mismos bytes despues de canonizar`() {
        listOf("{ \"v\":1}", "{\"v\":1}\n", "{\"z\":1,\"a\":2}", "{\"s\":\"\\u0041\"}", "{\"s\":\"\\/\"}").forEach {
            assertThrows(it, ErrorProtocolo::class.java) { JsonCp1.leerCanonico(it.toByteArray()) }
        }
    }

    @Test
    fun `rechaza UTF8 invalido BOM y caracteres fuera de ASCII imprimible`() {
        val invalidos = listOf(
            byteArrayOf(0x22, 0xc0.toByte(), 0xaf.toByte(), 0x22),
            byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte(), 0x31),
            "\"niño\"".toByteArray(), "\"\\u0000\"".toByteArray(), "\"\\n\"".toByteArray(),
            "\"\\ud800\"".toByteArray(), "\"\u007f\"".toByteArray(),
        )
        invalidos.forEach { assertThrows(ErrorProtocolo::class.java) { JsonCp1.leer(it) } }
    }

    @Test
    fun `rechaza sintaxis incompleta contenido sobrante y anidacion excesiva`() {
        listOf("", "{", "[1,]", "{\"a\":1,}", "\"sin cierre", "true false", "nul", "\"\\u00GG\"",
            "[".repeat(40) + "0" + "]".repeat(40)).forEach {
            assertThrows(it, ErrorProtocolo::class.java) { JsonCp1.leer(it.toByteArray()) }
        }
    }

    @Test
    fun `impone limite de bytes y permite exactamente el maximo`() {
        val permitido = ("\"" + "a".repeat(8190) + "\"").toByteArray()
        assertEquals(8192, permitido.size)
        assertTrue(JsonCp1.leerCanonico(permitido) is TextoJson)
        assertThrows(ErrorProtocolo::class.java) { JsonCp1.leerCanonico(permitido + byteArrayOf(32)) }
        assertThrows(ErrorProtocolo::class.java) { JsonCp1.leer(ByteArray(16385)) }
    }

    @Test
    fun `serializador no genera caracteres o numeros prohibidos`() {
        listOf<ValorJson>(TextoJson("ñ"), EnteroJson(-1), EnteroJson(Long.MAX_VALUE),
            ObjetoJson(mapOf("\n" to NuloJson))).forEach {
            assertThrows(ErrorProtocolo::class.java) { JsonCp1.serializar(it) }
        }
    }

    @Test
    fun `base64url rechaza padding bits sobrantes y tamaños excesivos`() {
        listOf("YQ==", "YR", "Y", "a+b", "a/b", "", " YQ").forEach {
            assertThrows(it, ErrorProtocolo::class.java) { Base64Cp1.decodificar(it, 32) }
        }
        assertEquals("a", Base64Cp1.decodificar("YQ", 1).decodeToString())
        assertThrows(ErrorProtocolo::class.java) { Base64Cp1.decodificar("YWE", 1) }
    }
}
