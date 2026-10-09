package dev.controlparental.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecCp1Test {
    private val orden = "04_lock_for.payload.json"

    private fun alterar(nombre: String, vararg cambios: Pair<String, ValorJson>): ByteArray =
        JsonCp1.serializar(ObjetoJson(EjemplosCp1.objeto(nombre).campos + cambios.toMap()))

    private fun rechazar(nombre: String, campo: String, valores: List<ValorJson>) {
        valores.forEach { valor ->
            assertThrows("$nombre: $campo", ErrorProtocolo::class.java) {
                CodecCp1.leerDocumentoSinAutorizar(alterar(nombre, campo to valor))
            }
        }
    }

    @Test
    fun `acepta maximo Long y rechaza secuencias fuera de rango o con otro tipo`() {
        val maximo = TextoJson(Long.MAX_VALUE.toString())
        assertEquals(Long.MAX_VALUE, (CodecCp1.leerDocumentoSinAutorizar(alterar(orden, "seq" to maximo)) as OrdenPolitica).seq)
        rechazar(orden, "seq", listOf("0", "01", "-1", "+1", " 1", "9223372036854775808",
            "99999999999999999999").map(::TextoJson) + listOf(EnteroJson(1), NuloJson))
        rechazar("06_state.payload.json", "stateSeq", listOf(TextoJson("0"), TextoJson("9223372036854775808")))
        rechazar("06_state.payload.json", "lastAcceptedSeq", listOf(TextoJson("9223372036854775808")))
    }

    @Test
    fun `duraciones temporales aceptan solo los extremos y enteros del intervalo`() {
        listOf(60L, 604800L).forEach {
            val dto = CodecCp1.leerDocumentoSinAutorizar(alterar(orden, "durationSec" to EnteroJson(it))) as OrdenPolitica
            assertEquals(it, dto.durationSec)
        }
        rechazar(orden, "durationSec", listOf(EnteroJson(59), EnteroJson(604801), NuloJson, TextoJson("60")))
    }

    @Test
    fun `las cuatro acciones conservan sus reglas de duracion`() {
        AccionPolitica.entries.forEach { accion ->
            val temporal = accion == AccionPolitica.LOCK_FOR || accion == AccionPolitica.ALLOW_FOR
            val bytes = alterar(orden, "action" to TextoJson(accion.name),
                "durationSec" to if (temporal) EnteroJson(60) else NuloJson)
            assertEquals(accion, (CodecCp1.leerDocumentoSinAutorizar(bytes) as OrdenPolitica).action)
            assertThrows(ErrorProtocolo::class.java) {
                CodecCp1.leerDocumentoSinAutorizar(alterar(orden, "action" to TextoJson(accion.name),
                    "durationSec" to if (temporal) NuloJson else EnteroJson(60)))
            }
        }
    }

    @Test
    fun `UUID nonce remitente y enumeraciones no se coercionan`() {
        rechazar(orden, "bootId", listOf(TextoJson("no-uuid"), TextoJson("FFFFFFFF-FFFF-4FFF-8FFF-FFFFFFFFFFFF"), NuloJson))
        rechazar(orden, "commandNonce", listOf(TextoJson("ABC"), TextoJson("A".repeat(42) + "B")))
        rechazar(orden, "sender", listOf(TextoJson("CHILD"), TextoJson("parent")))
        rechazar(orden, "action", listOf(TextoJson("RETIRE"), TextoJson("allow")))
        rechazar("06_state.payload.json", "networkValidated", listOf(TextoJson("true"), EnteroJson(1)))
        rechazar("18_error.frame.json", "code", listOf(TextoJson("ERROR_INVENTADO")))
        rechazar("05_ack_applied.payload.json", "reason", listOf(TextoJson("ERROR_INVENTADO")))
        rechazar("12_challenge.frame.json", "timeoutSec", listOf(EnteroJson(9), TextoJson("10")))
        rechazar("17_peer_unavailable.frame.json", "reason", listOf(TextoJson("OK")))
    }

    @Test
    fun `version desconocida produce codigo explicito y campos adicionales no aportan autoridad`() {
        val error = assertThrows(ErrorProtocolo::class.java) {
            CodecCp1.leerDocumentoSinAutorizar(alterar(orden, "v" to EnteroJson(2)))
        }
        assertEquals(CodigoCp1.UNKNOWN_VERSION, error.codigo)
        assertThrows(ErrorProtocolo::class.java) {
            CodecCp1.leerDocumentoSinAutorizar(alterar(orden, "parentPublicKeyB64" to TextoJson("clave-no-vinculada")))
        }
    }

    @Test
    fun `STATE conserva relacion entre contador identificador hash y resultado`() {
        val inicial = "10_state_inicial.payload.json"
        rechazar(inicial, "lastResult", listOf(TextoJson("APPLIED"), TextoJson("REJECTED")))
        rechazar(inicial, "lastAcceptedSeq", listOf(TextoJson("1")))
        rechazar("06_state.payload.json", "lastCommandId", listOf(NuloJson))
        rechazar("06_state.payload.json", "lastCommandPayloadSha256B64", listOf(NuloJson, TextoJson("ABC")))
        rechazar("06_state.payload.json", "lastResult", listOf(TextoJson("NONE"), TextoJson("REJECTED")))
        rechazar("06_state.payload.json", "remainingSec", listOf(EnteroJson(604801), TextoJson("0")))
    }

    @Test
    fun `vinculo valida HTTPS claves base64 y hashes anidados sin aceptar secretos`() {
        val nombre = "07_pair_accept.payload.json"
        rechazar(nombre, "relayOrigin", listOf(TextoJson("http://ejemplo.onrender.com"),
            TextoJson("https://ejemplo.onrender.com/ruta"), TextoJson("https://usuario@ejemplo.com")))
        rechazar(nombre, "parentPublicKeyB64", listOf(TextoJson("no-es-clave")))
        val hashes = EjemplosCp1.objeto(nombre).campos.getValue("recoveryHashes") as ObjetoJson
        listOf(ObjetoJson(hashes.campos + ("codigoReal" to TextoJson("prohibido"))),
            ObjetoJson(hashes.campos - "RETIRE"), ObjetoJson(hashes.campos + ("RETIRE" to TextoJson("ABC")))).forEach {
            assertThrows(ErrorProtocolo::class.java) { CodecCp1.leerDocumentoSinAutorizar(alterar(nombre, "recoveryHashes" to it)) }
        }
    }

    @Test
    fun `el canal no acepta payloads sin sobre firmado ni ofertas de QR`() {
        listOf(orden, "05_ack_applied.payload.json", "11_child_offer.frame.json").forEach {
            assertThrows(ErrorProtocolo::class.java) { CodecCp1.leerFrame(EjemplosCp1.bytes(it)) }
        }
        assertTrue(CodecCp1.leerFrame(EjemplosCp1.bytes("04_lock_for.signed.json")) is SobreSinVerificar)
        assertTrue(CodecCp1.leerFrame(EjemplosCp1.bytes("15_pong.frame.json")) is Pong)
    }

    @Test
    fun `payload requiere proposito correcto y ningun frame puede ser una orden firmada`() {
        assertThrows(ErrorProtocolo::class.java) {
            CodecCp1.leerPayloadSinVerificar(JsonCp1.serializar(EjemplosCp1.objeto(orden)), Proposito.OFFLINE)
        }
        assertThrows(ErrorProtocolo::class.java) {
            CodecCp1.leerPayloadSinVerificar(JsonCp1.serializar(EjemplosCp1.objeto("15_pong.frame.json")), Proposito.MESSAGE)
        }
        rechazar("09_offline_response.payload.json", "action", listOf(TextoJson("ALLOW")))
        rechazar("09_offline_response.payload.json", "sender", listOf(TextoJson("CHILD")))
    }

    @Test
    fun `sobre limita bytes decodificados y rechaza firmas con formato base64 incorrecto`() {
        val nombre = "04_lock_for.signed.json"
        rechazar(nombre, "payloadB64", listOf(TextoJson(Base64Cp1.codificar(ByteArray(8193))), TextoJson("YQ==")))
        rechazar(nombre, "sigB64", listOf(TextoJson("AAAA"), TextoJson("A".repeat(101)), TextoJson("no+firma")))
        assertThrows(ErrorProtocolo::class.java) { CodecCp1.leerFrame(ByteArray(16385)) }
    }

    @Test
    fun `serializador no permite fabricar un DTO contrario al esquema`() {
        val dto = CodecCp1.leerDocumentoSinAutorizar(EjemplosCp1.bytes(orden)) as OrdenPolitica
        assertThrows(ErrorProtocolo::class.java) { CodecCp1.serializar(dto.copy(seq = 0)) }
        assertThrows(ErrorProtocolo::class.java) { CodecCp1.serializar(dto.copy(durationSec = null)) }
    }
}
