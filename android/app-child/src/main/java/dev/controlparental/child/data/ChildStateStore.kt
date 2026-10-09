package dev.controlparental.child.data

import java.io.IOException

/** La implementación Android usa AtomicFile; el fake solo se usa en tests. */
interface ArchivoEstadoAtomico {
    fun leerAcotado(maximoBytes: Int): ByteArray?
    fun escribirAtomico(bytes: ByteArray)
}

/** Un escritor serializado por instancia; Android debe compartirla por archivo/proceso. */
class ChildStateStore(private val archivo: ArchivoEstadoAtomico) {
    private val monitor = Any()

    fun cargar(): CargaEstado = synchronized(monitor) { cargarDentro() }

    /** Sin aceptar comandos: H09 autorizará antes de esta transacción local. */
    fun actualizar(transformar: (EstadoControl?) -> EstadoControl): EstadoControl = synchronized(monitor) {
        val anterior = when (val carga = cargarDentro()) {
            CargaEstado.Ausente -> null
            CargaEstado.ErrorSeguro -> throw ErrorEstadoLocal()
            is CargaEstado.Disponible -> carga.estado
        }
        val nuevo = transformar(anterior)
        if (anterior != null) {
            require(nuevo.lastAcceptedSeq >= anterior.lastAcceptedSeq)
            if (nuevo.vinculo == anterior.vinculo) require(nuevo.recoveryConsumed.containsAll(anterior.recoveryConsumed))
            if (anterior.lastCommand != null && nuevo.lastAcceptedSeq == anterior.lastAcceptedSeq) {
                require(nuevo.lastCommand?.commandId == anterior.lastCommand.commandId)
                require(nuevo.lastCommand?.payloadSha256B64 == anterior.lastCommand.payloadSha256B64)
                require(nuevo.policy == anterior.policy) // Repetición no reinicia startElapsed.
            }
        }
        val bytes = CodecEstado.codificar(nuevo)
        try {
            archivo.escribirAtomico(bytes)
            // AtomicFile no da un ACK de durabilidad: detectar también fallos silenciosos.
            val leido = archivo.leerAcotado(CodecEstado.MAX_ARCHIVO_BYTES) ?: throw ErrorEstadoLocal()
            if (!bytes.contentEquals(leido)) throw ErrorEstadoLocal()
            CodecEstado.decodificar(leido)
        } catch (_: IOException) {
            throw ErrorEstadoLocal()
        }
    }

    private fun cargarDentro(): CargaEstado = try {
        val bytes = archivo.leerAcotado(CodecEstado.MAX_ARCHIVO_BYTES)
        if (bytes == null) CargaEstado.Ausente else CargaEstado.Disponible(CodecEstado.decodificar(bytes))
    } catch (_: IOException) {
        CargaEstado.ErrorSeguro
    } catch (_: RuntimeException) {
        CargaEstado.ErrorSeguro
    }
}
