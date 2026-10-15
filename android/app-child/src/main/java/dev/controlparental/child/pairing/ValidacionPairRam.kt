package dev.controlparental.child.pairing

import dev.controlparental.child.local.*
import dev.controlparental.protocol.cp2.*

/** Datos aportados por confirmación humana independiente, nunca extraídos automáticamente del sobre. */
internal data class DatosConfirmadosPair(val publicaPadreEsperada: String, val huellaPadre: String,
    val huellaHijo: String, val origen: String)
internal class ConfirmacionPair internal constructor(internal val referencia: ReferenciaOferta,
    internal val ticket: TicketAdulto, internal val datos: DatosConfirmadosPair, internal val marco: MarcoSesionAdulto)

/** Snapshot inmutable de validación; NO PREPARADO/activo, permiso, escritura ni commit de pareja. */
internal class ValidacionPair internal constructor(val integridad: IntegridadPairCp2,
    val boot: Int, val generacionOferta: Long, internal val referencia: ReferenciaOferta,
    internal val consentimiento: DatosConfirmadosPair, internal val marco: MarcoSesionAdulto) {
    val aceptacion get() = integridad.aceptacion
    val payloadSha256B64 get() = integridad.payloadSha256B64
    fun bytesPayload() = integridad.bytesPayload()
    fun bytesFrame() = integridad.bytesFrame()
}

internal fun interface VerificadorIntegridadPair {
    fun verificar(frame: ByteArray, publicaEsperada: String): IntegridadPairCp2
}

/** Caso puro sin UI/IO. Vigencia de oferta y código fresco son dos guardas obligatorias independientes. */
internal class ValidacionPairRam(private val ofertas: OfertasRam, private val sesion: SesionAdulto,
    private val verificador: VerificadorIntegridadPair = VerificadorIntegridadPair(CriptografiaPairCp2::verificarIntegridad)) {
    private var epoca = Any()
    private var confirmacion: ConfirmacionPair? = null
    private var ocupado = false
    private var consumida: ReferenciaOferta? = null
    private var resultado: ValidacionPair? = null

    /** Fondo/cancelación: invalida de inmediato incluso con proveedor criptográfico en curso. */
    @Synchronized fun cancelar() {
        epoca = Any(); confirmacion = null; resultado = null; ocupado = false
        // Conserva la marca de consumo RAM: cancelar no permite validar otra vez el mismo nonce.
    }
    private fun vigente(c: ConfirmacionPair, soloSesion: Boolean = false): Boolean = c.ticket.fin == FinAdulto.VALIDAR_PAIR &&
        c.ticket.boot == c.referencia.contexto.boot && sesion.marcoVigente(c.marco) &&
        (soloSesion || sesion.accionVigente(c.ticket)) && ofertas.vigente(c.referencia)

    /** El llamador aún debe ofrecer una UI humana segura; no hay recepción operativa cableada. */
    @Synchronized fun confirmar(ref: ReferenciaOferta, ticket: TicketAdulto, datos: DatosConfirmadosPair): ConfirmacionPair? {
        cancelar()
        val token = epoca
        val marco = sesion.marcoAcreditado(ticket) ?: return null
        val nueva = ConfirmacionPair(ref, ticket, datos, marco)
        val valida = try {
            CodecPairCp2.origenExacto(datos.origen)
            datos.huellaPadre == CodecPairCp2.huellaPublica(datos.publicaPadreEsperada) &&
                datos.huellaHijo == CodecPairCp2.huellaPublica(ref.contexto.identidad) &&
                ref.oferta.childPublicKeyB64 == ref.contexto.identidad && ref !== consumida && vigente(nueva)
        } catch (_: Exception) { false }
        if (!valida || epoca !== token) return null
        confirmacion = nueva
        return nueva
    }

    fun validar(c: ConfirmacionPair, frame: ByteArray): ValidacionPair? {
        // Capturar antes de llamar al proveedor; ninguna mutación del llamador cambia los bytes examinados.
        if (frame.size !in 1..CodecPairCp2.MAX_FRAME) return null
        val copia = frame.copyOf()
        val token = synchronized(this) {
            val actual = epoca
            if (confirmacion !== c || ocupado || consumida === c.referencia || !vigente(c) || epoca !== actual) return null
            ocupado = true; actual
        }
        val integridad = try { verificador.verificar(copia, c.datos.publicaPadreEsperada) } catch (_: Exception) { null }
        return synchronized(this) {
            val a = integridad?.aceptacion
            val valida = a != null && a.parentPublicKeyB64 == c.datos.publicaPadreEsperada &&
                a.childPublicKeyB64 == c.referencia.contexto.identidad && a.enrollId == c.referencia.oferta.enrollId &&
                a.enrollNonce == c.referencia.oferta.enrollNonce && a.relayOrigin == c.datos.origen && vigente(c)
            // Revalidar token DESPUÉS de puertos: pueden reentrar/cancelar mientras verificamos.
            if (epoca !== token || confirmacion !== c) return@synchronized null
            ocupado = false
            if (!valida) { resultado = null; return@synchronized null }
            consumida = c.referencia // Una sola validación RAM por oferta; no consumo durable ni commitpair.
            ValidacionPair(integridad!!, c.referencia.contexto.boot, c.referencia.generacion,
                c.referencia, c.datos, c.marco).also { resultado = it }
        }
    }
    /** Nunca reconstruye un resultado caducado ni transforma el snapshot en autoridad durable. */
    @Synchronized fun resultadoActual(): ValidacionPair? {
        val c = confirmacion ?: return null
        val token = epoca
        val valida = vigente(c, soloSesion = true)
        if (epoca !== token || confirmacion !== c) return null // Un callback viejo no borra resultado nuevo.
        if (!valida) { resultado = null; return null }
        return resultado
    }
}
