package dev.controlparental.child.pairing

import dev.controlparental.protocol.*
import dev.controlparental.child.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.UUID

class OfertaCp2Test {
    private fun publica(curva: String = "secp256r1") = Base64Cp1.codificar(KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curva)); generateKeyPair().public.encoded
    }) // Privadas efímeras solo RAM de JVM; nunca inyectadas al APK.
    private fun oferta() = OfertaCp2(publica(), "12345678-1234-4234-8234-123456789abc", Base64Cp1.codificar(ByteArray(32)))
    private fun rechaza(accion: () -> Unit) {
        try { accion(); fail("Debía rechazar") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {}
    }
    @Test fun canonicoExactoYUtf8Roundtrip() {
        val o = oferta(); val b = CodecOfertaCp2.escribir(o)
        assertEquals("{\"childPublicKeyB64\":\"${o.childPublicKeyB64}\",\"enrollId\":\"${o.enrollId}\",\"enrollNonce\":\"${o.enrollNonce}\",\"kind\":\"CHILD_OFFER\",\"protocol\":\"cp.v2\",\"v\":2}", b.toString(Charsets.UTF_8))
        assertEquals(o, CodecOfertaCp2.leer(b)); assertArrayEquals(b, b.toString(Charsets.UTF_8).toByteArray(Charsets.UTF_8))
        rechaza { CodecCp1.leerDocumentoSinAutorizar(b) }
    }
    @Test fun obligatoriosCerradosYTipos() {
        val b = CodecOfertaCp2.escribir(oferta()); val raiz = JsonCp1.leer(b) as ObjetoJson
        for (campo in raiz.campos.keys) rechaza { CodecOfertaCp2.leer(JsonCp1.serializar(ObjetoJson(raiz.campos - campo))) }
        rechaza { CodecOfertaCp2.leer(JsonCp1.serializar(ObjetoJson(raiz.campos + ("extra" to NuloJson)))) }
        for ((campo, valor) in listOf("v" to EnteroJson(1), "protocol" to TextoJson("cp.v1"), "kind" to TextoJson("PAIR_ACCEPT"), "enrollId" to EnteroJson(7)))
            rechaza { CodecOfertaCp2.leer(JsonCp1.serializar(ObjetoJson(raiz.campos + (campo to valor)))) }
    }
    @Test fun noWhitespaceDuplicadosEscapesUOrdenAlternativo() {
        val s = CodecOfertaCp2.escribir(oferta()).toString(Charsets.US_ASCII)
        for (alterado in listOf(" $s", "$s ", s.replace(",", ", "), s.replace("{", "{\"v\":2,"),
            s.replace("CHILD_OFFER", "CHILD_\\u004fFFER"), s.replace("\"v\":2", "\"v\":2.0")))
            rechaza { CodecOfertaCp2.leer(alterado.toByteArray()) }
        val raiz = JsonCp1.leer(s.toByteArray()) as ObjetoJson
        val inverso = raiz.campos.entries.reversed().joinToString(",", "{", "}") { "\"${it.key}\":" + JsonCp1.serializar(it.value).toString(Charsets.UTF_8) }
        rechaza { CodecOfertaCp2.leer(inverso.toByteArray()) }
    }
    @Test fun uuidVersionVarianteYLowercase() {
        val o = oferta()
        for (id in listOf(o.enrollId.uppercase(), o.enrollId.replace("-4234-", "-1234-"), o.enrollId.replace("-8234-", "-7234-"), "", UUID.randomUUID().toString() + " "))
            rechaza { CodecOfertaCp2.escribir(o.copy(enrollId = id)) }
    }
    @Test fun nonce32BytesRoundtripYBitsResiduales() {
        val o = oferta()
        for (n in listOf(o.enrollNonce + "=", Base64Cp1.codificar(ByteArray(31)), Base64Cp1.codificar(ByteArray(33)),
            o.enrollNonce.dropLast(1) + "B", o.enrollNonce.dropLast(1) + "+")) rechaza { CodecOfertaCp2.escribir(o.copy(enrollNonce = n)) }
    }
    @Test fun publicaP256ExactaNoOtraCurvaNiDerExtra() {
        val o = oferta()
        for (p in listOf(publica("secp384r1"), o.childPublicKeyB64 + "=", Base64Cp1.codificar(Base64Cp1.decodificar(o.childPublicKeyB64, 150) + byteArrayOf(0)), "A".repeat(100)))
            rechaza { CodecOfertaCp2.escribir(o.copy(childPublicKeyB64 = p)) }
    }
    @Test fun limitesAntesDeParseUtf8AsciiYProfundidad() {
        for (n in listOf(0, 8192, 8193, 16384, 16385)) rechaza { CodecOfertaCp2.leer(ByteArray(n) { 32 }) }
        for (b in listOf(byteArrayOf(0xc3.toByte(), 0x28), "é".toByteArray(), byteArrayOf(10))) rechaza { CodecOfertaCp2.leer(b) }
        for (n in listOf(6, 7, 100)) rechaza { CodecOfertaCp2.leer(("[".repeat(n) + "0" + "]".repeat(n)).toByteArray()) }
        assertEquals(8192, CodecOfertaCp2.MAX_PAYLOAD); assertEquals(16384, CodecOfertaCp2.MAX_FRAME); assertEquals(6, CodecOfertaCp2.MAX_PROFUNDIDAD)
    }
    @Test fun copiaSchemaSuministradoNoFuenteExterna() {
        val archivo = listOf(File("../../contrato-cp2/vinculacion.schema.json"), File("contrato-cp2/vinculacion.schema.json")).first { it.exists() }
        val hash = MessageDigest.getInstance("SHA-256").digest(archivo.readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals("7290e6cb419e910756d3848586be24358f063455b1c65c47f24e75b5cef22239", hash)
    }
    private inner class Laboratorio {
        var tiempo = 100L; var autorizado = true; var c: ContextoOferta? = ContextoOferta(5, publica()); var contador = 0
        fun nuevo() = OfertasRam({ tiempo }, { c }, { autorizado }, EntropiaOferta { n -> ByteArray(n) { (++contador).toByte() } })
        val ram = nuevo()
    }
    @Test fun generaUuidBitsNonce32YNoAutoOferta() {
        val l = Laboratorio(); val r = l.ram.generar(); val u = UUID.fromString(r.oferta.enrollId)
        assertEquals(4, u.version()); assertEquals(2, u.variant()); assertEquals(32, Base64Cp1.decodificar(r.oferta.enrollNonce, 32).size)
        assertFalse(l.nuevo().vigente(r)) // Recreación/proceso nuevo no reconstruye oferta.
    }
    @Test fun cincoMinutosAbsolutosSinRenovar() {
        val l = Laboratorio(); val r = l.ram.generar(); l.tiempo += 299999
        assertEquals(1L, l.ram.restante(r)); assertTrue(l.ram.vigente(r)); l.tiempo++
        assertFalse(l.ram.vigente(r)); l.tiempo++; assertNull(l.ram.restante(r))
    }
    @Test fun fondoCancelSesionCaducaRetiraReferencia() {
        val l = Laboratorio(); val r = l.ram.generar(); l.autorizado = false
        assertFalse(l.ram.vigente(r)); l.autorizado = true; assertFalse(l.ram.vigente(r))
        val siguiente = l.ram.generar(); l.ram.cancelar(); assertFalse(l.ram.vigente(siguiente))
    }
    @Test fun bootIdentidadYContextoErrorInvalidanSinAutoRestaurar() {
        for (tipo in 0..2) {
            val l = Laboratorio(); val r = l.ram.generar()
            l.c = when (tipo) { 0 -> l.c!!.copy(boot = 6); 1 -> l.c!!.copy(identidad = publica()); else -> null }
            assertFalse(l.ram.vigente(r)); l.c = r.contexto; assertFalse(l.ram.vigente(r))
        }
    }
    @Test fun relojRetrocedeInvalida() {
        val l = Laboratorio(); val r = l.ram.generar(); l.tiempo--; assertFalse(l.ram.vigente(r))
        l.tiempo = r.inicio; assertFalse(l.ram.vigente(r))
    }
    @Test fun nuevaOfertaSustituyeNonceYCallbackViejoNoCancelaNueva() {
        val l = Laboratorio(); val vieja = l.ram.generar(); val nueva = l.ram.generar()
        assertNotEquals(vieja.oferta.enrollNonce, nueva.oferta.enrollNonce); assertNotEquals(vieja.oferta.enrollId, nueva.oferta.enrollId)
        assertNull(l.ram.restante(vieja)); assertTrue(l.ram.vigente(nueva))
    }
    @Test fun sinAutoridadIdentidadOContextoNoGenera() {
        val l = Laboratorio(); l.autorizado = false; rechaza { l.ram.generar() }
        l.autorizado = true; l.c = null; rechaza { l.ram.generar() }
        l.c = ContextoOferta(-1, publica()); rechaza { l.ram.generar() }
        l.c = ContextoOferta(5, "invalida"); rechaza { l.ram.generar() }
    }
    @Test fun preparacionIntegraNoSePromueveNiReabreEstadoIncierto() {
        val e = EstadoControl(provisioningStage = EtapaAprovisionamiento.PREPARACION,
            bootContext = ContextoArranque(5, UUID.randomUUID().toString(), ultimoElapsedObservado = 10))
        assertTrue(estadoCompatibleOferta(CargaEstado.Disponible(e), 5, 10))
        assertFalse(estadoCompatibleOferta(CargaEstado.Ausente, 5, 10))
        assertFalse(estadoCompatibleOferta(CargaEstado.ErrorSeguro, 5, 10))
        assertFalse(estadoCompatibleOferta(CargaEstado.Disponible(e), 6, 10))
        assertFalse(estadoCompatibleOferta(CargaEstado.Disponible(e), 5, 9))
        for (etapa in listOf(EtapaAprovisionamiento.VINCULADO, EtapaAprovisionamiento.RECUPERACION_PADRE_PENDIENTE, EtapaAprovisionamiento.RETIRADA_PENDIENTE))
            assertFalse(estadoCompatibleOferta(CargaEstado.Disponible(e.copy(provisioningStage = etapa)), 5, 10))
        assertFalse(estadoCompatibleOferta(CargaEstado.Disponible(e.copy(recoveryConsumed = setOf(PropositoRecuperacionLocal.RETIRE))), 5, 10))
    }
}
