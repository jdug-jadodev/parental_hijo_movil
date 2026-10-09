package dev.controlparental.child.data

import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test

class CodecEstadoTest {
    @Test
    fun `registro inicial y orden firmada sobreviven ida y vuelta sin reiniciar duracion`() {
        listOf(EstadoControl(), estadoConOrden()).forEach { estado ->
            val bytes = CodecEstado.codificar(estado)
            assertEquals(estado, CodecEstado.decodificar(bytes))
            assertArrayEquals(bytes, CodecEstado.codificar(CodecEstado.decodificar(bytes)))
        }
    }

    @Test
    fun `campos desconocidos o faltantes se rechazan incluso con checksum recalculado`() {
        val payload = payloadLocal(estadoConOrden())
        payload.campos.keys.forEach { campo ->
            assertThrows(RuntimeException::class.java) { CodecEstado.decodificar(envolverPrueba(ObjetoJson(payload.campos - campo))) }
        }
        assertThrows(RuntimeException::class.java) { CodecEstado.decodificar(envolverPrueba(ObjetoJson(payload.campos + ("extra" to NuloJson)))) }
    }

    @Test
    fun `checksum y version del contenedor no pueden omitirse ni cambiarse`() {
        val original = JsonCp1.leer(CodecEstado.codificar(estadoConOrden())) as ObjetoJson
        val alterados = listOf(original.campos - "sha256B64", original.campos + ("formatVersion" to EnteroJson(2)),
            original.campos + ("sha256B64" to TextoJson(Base64Cp1.codificar(ByteArray(32)))),
            original.campos + ("otro" to NuloJson))
        alterados.forEach { assertThrows(RuntimeException::class.java) { CodecEstado.decodificar(JsonCp1.serializar(ObjetoJson(it))) } }
    }

    @Test
    fun `JSON ambiguo truncado vacio y excesivo no se recupera por defecto`() {
        listOf(byteArrayOf(), "{".toByteArray(), "{\"formatVersion\":1,\"formatVersion\":1}".toByteArray(),
            ByteArray(CodecEstado.MAX_ARCHIVO_BYTES + 1)).forEach {
            assertThrows(RuntimeException::class.java) { CodecEstado.decodificar(it) }
        }
    }

    @Test
    fun `metadatos derivados de la ultima orden deben coincidir con su firma`() {
        val estado = estadoConOrden()
        val ultima = estado.lastCommand!!
        listOf(estado.copy(lastCommand = ultima.copy(seq = ultima.seq + 1)),
            estado.copy(lastCommand = ultima.copy(payloadSha256B64 = Base64Cp1.codificar(ByteArray(32)))),
            estado.copy(policy = estado.policy!!.copy(duracionSegundos = 60)),
            estado.copy(lastCommand = ultima.copy(signedEnvelope = ultima.signedEnvelope.copy(purpose = Proposito.AUTH))),
            estado.copy(vinculo = estado.vinculo!!.copy(parentPublicKeyB64 = estado.vinculo.childPublicKeyB64)))
            .forEach { assertThrows(RuntimeException::class.java) { CodecEstado.codificar(it) } }
    }

    @Test
    fun `etapas y resultados incoherentes y estados de preparacion vinculados fallan`() {
        val estado = estadoConOrden()
        listOf(estado.copy(lastCommand = estado.lastCommand!!.copy(result = UltimoResultado.APPLIED)),
            estado.copy(provisioningStage = EtapaAprovisionamiento.NO_CONFIGURADO),
            estado.copy(policy = null), estado.copy(lastCommand = null), estado.copy(bootContext = null),
            estado.copy(recoveryFailureCount = -1), estado.copy(approvedPackages = listOf("paquete/invalido")))
            .forEach { assertThrows(RuntimeException::class.java) { CodecEstado.codificar(it) } }
    }

    @Test
    fun `boot autorizado distinto e inicio futuro en el mismo boot son incoherentes`() {
        val estado = estadoConOrden()
        assertThrows(RuntimeException::class.java) { CodecEstado.codificar(estado.copy(bootContext = estado.bootContext!!.copy(
            authorizedBootId = "00000000-0000-4000-8000-000000000010"))) }
        assertThrows(RuntimeException::class.java) { CodecEstado.codificar(estado.copy(policy = estado.policy!!.copy(aceptadaEnMs = 100001))) }
        val nuevoBoot = estado.copy(bootContext = ContextoArranque(13, "00000000-0000-4000-8000-000000000010", null, 0))
        assertEquals(nuevoBoot, CodecEstado.decodificar(CodecEstado.codificar(nuevoBoot)))
    }

    @Test
    fun `colecciones cargadas no permiten mutar el snapshot recibido`() {
        val estado = CodecEstado.decodificar(CodecEstado.codificar(estadoConOrden()))
        assertThrows(UnsupportedOperationException::class.java) { (estado.approvedPackages as MutableList).clear() }
        assertThrows(UnsupportedOperationException::class.java) { (estado.recoveryConsumed as MutableSet).add(PropositoRecuperacionLocal.RETIRE) }
    }
}
