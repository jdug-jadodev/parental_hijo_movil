package dev.controlparental.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

internal object EjemplosCp1 {
    val nombres: List<String> = listOf("01_auth_padre", "02_auth_hijo", "03_sync_request",
        "04_lock_for", "05_ack_applied", "06_state", "07_pair_accept", "08_offline_challenge",
        "09_offline_response", "10_state_inicial").flatMap {
        listOf("$it.payload.json", "$it.signed.json")
    } + listOf("11_child_offer", "12_challenge", "13_auth_ok", "14_ping", "15_pong",
        "16_peer_status", "17_peer_unavailable", "18_error").map { "$it.frame.json" }

    fun bytes(nombre: String): ByteArray = javaClass.classLoader.getResourceAsStream(nombre)!!.use { it.readBytes() }
    fun objeto(nombre: String): ObjetoJson = JsonCp1.leer(bytes(nombre)) as ObjetoJson
}

@RunWith(Parameterized::class)
class EjemplosCp1Test(private val nombre: String) {
    @Test
    fun `cada ejemplo produce un DTO y conserva sus bytes canonicos al serializar`() {
        val original = EjemplosCp1.bytes(nombre)
        val dto = CodecCp1.leerDocumentoSinAutorizar(original)
        val serializado = CodecCp1.serializar(dto)
        assertTrue(JsonCp1.serializar(JsonCp1.leer(original)).contentEquals(serializado))
        assertEquals(dto, CodecCp1.leerDocumentoSinAutorizar(serializado))
        if (dto is SobreSinVerificar) {
            val payload = Base64Cp1.decodificar(dto.payloadB64, 8192)
            assertTrue(payload.contentEquals(CodecCp1.serializar(CodecCp1.leerPayloadSinVerificar(payload, dto.purpose))))
        }
    }

    @Test
    fun `cada ejemplo rechaza propiedades desconocidas`() {
        val original = EjemplosCp1.objeto(nombre)
        val alterado = ObjetoJson(original.campos + ("campoInventado" to NuloJson))
        assertThrows(ErrorProtocolo::class.java) { CodecCp1.leerDocumentoSinAutorizar(JsonCp1.serializar(alterado)) }
    }

    @Test
    fun `todos los campos del ejemplo son obligatorios incluso los null`() {
        val original = EjemplosCp1.objeto(nombre)
        original.campos.keys.forEach { eliminado ->
            val alterado = ObjetoJson(original.campos - eliminado)
            assertThrows("$nombre sin $eliminado", ErrorProtocolo::class.java) {
                CodecCp1.leerDocumentoSinAutorizar(JsonCp1.serializar(alterado))
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun ejemplos(): List<Array<String>> = EjemplosCp1.nombres.map { arrayOf(it) }
    }
}
