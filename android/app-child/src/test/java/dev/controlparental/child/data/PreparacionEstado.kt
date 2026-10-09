package dev.controlparental.child.data

import dev.controlparental.child.domain.PoliticaAceptada
import dev.controlparental.protocol.*
import java.io.IOException
import java.security.MessageDigest

internal fun estadoConOrden(): EstadoControl {
    fun recurso(nombre: String) = EstadoControl::class.java.classLoader!!.getResourceAsStream(nombre)!!.use { it.readBytes() }
    val par = CodecCp1.leerDocumentoSinAutorizar(recurso("07_pair_accept.payload.json")) as AceptacionVinculo
    val sobre = CodecCp1.leerFrame(recurso("04_lock_for.signed.json")) as SobreSinVerificar
    val verificado = CriptografiaCp1.verificar(sobre, ClavePublicaCp1.leer(par.parentPublicKeyB64), Rol.PARENT, Proposito.MESSAGE)
    val orden = verificado.mensaje as OrdenPolitica
    return EstadoControl(vinculo = VinculoPublico(par.pairId, par.parentPublicKeyB64,
        par.childPublicKeyB64, par.relayOrigin, par.recoveryHashes),
        bootContext = ContextoArranque(12, orden.bootId, orden.bootId, 100000),
        policy = PoliticaAceptada(orden.action, orden.durationSec, 100000, orden.bootId),
        lastCommand = UltimaOrdenDuradera(orden.seq, orden.commandId, verificado.payloadSha256B64,
            sobre, EtapaAplicacion.ACCEPTED, UltimoResultado.ACCEPTED_PENDING_ENFORCEMENT),
        approvedPackages = listOf("dev.controlparental.child"), provisioningStage = EtapaAprovisionamiento.VINCULADO)
}

internal fun payloadLocal(estado: EstadoControl): ObjetoJson {
    val contenedor = JsonCp1.leer(CodecEstado.codificar(estado)) as ObjetoJson
    val bytes = Base64Cp1.decodificar((contenedor.campos.getValue("payloadB64") as TextoJson).texto, CodecEstado.MAX_REGISTRO_BYTES)
    return JsonCp1.leer(bytes) as ObjetoJson
}

internal fun envolverPrueba(payload: ObjetoJson): ByteArray {
    val bytes = JsonCp1.serializar(payload)
    return JsonCp1.serializar(ObjetoJson(mapOf("formatVersion" to EnteroJson(1),
        "payloadB64" to TextoJson(Base64Cp1.codificar(bytes)),
        "sha256B64" to TextoJson(Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(bytes))))))
}

internal class ArchivoSimulado : ArchivoEstadoAtomico {
    var bytes: ByteArray? = null
    var falloLectura = false
    var falloEscritura = false
    var omiteCommit = false
    var escrituras = 0
    override fun leerAcotado(maximoBytes: Int): ByteArray? {
        if (falloLectura || (bytes?.size ?: 0) > maximoBytes) throw IOException("Lectura fallida de prueba")
        return bytes?.copyOf()
    }
    override fun escribirAtomico(bytes: ByteArray) {
        escrituras++
        if (falloEscritura) throw IOException("Escritura interrumpida de prueba")
        if (!omiteCommit) this.bytes = bytes.copyOf()
    }
}
