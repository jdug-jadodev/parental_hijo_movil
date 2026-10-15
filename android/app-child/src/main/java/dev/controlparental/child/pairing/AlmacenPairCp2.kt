package dev.controlparental.child.pairing

import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.*
import java.security.MessageDigest

internal interface ArchivosPairCp2 {
    fun leerReserva(): ByteArray?
    fun leerRegistro(): ByteArray?
    fun reservar(bytes: ByteArray)
    fun escribirRegistro(bytes: ByteArray)
}
internal data class ConsentimientoPair(val datos: DatosConfirmadosPair, val boot: Int, val generacion: Long,
    val revisionAdulta: Long, val elapsed: Long)
internal class SnapshotPairCp2 internal constructor(val integridad: IntegridadPairCp2,
    val consentimiento: ConsentimientoPair, val nonceConsumidoSha256: String) {
    val pairId get() = integridad.aceptacion.pairId
    val publicaPadreEsperada get() = consentimiento.datos.publicaPadreEsperada
    val publicaHijo get() = integridad.aceptacion.childPublicKeyB64
    val origen get() = integridad.aceptacion.relayOrigin
    val recoveryHashes get() = integridad.aceptacion.recoveryHashes
    val estadoLocal get() = "VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION"
}
internal sealed interface CargaPairCp2 {
    data object Ausente : CargaPairCp2
    data object Incierta : CargaPairCp2
    class Guardado(val snapshot: SnapshotPairCp2) : CargaPairCp2
}
internal enum class ResultadoGuardarPair { GUARDADO, GUARDADO_SIN_SESION, RECHAZADO, INCIERTO }

/** Registro local v1/perfil cp.v2, NO nuevo frame CP/2. Reserva durable impide virginidad tras crash. */
internal object CodecRegistroPair {
    const val MAX_REGISTRO = 16384
    fun sha(bytes: ByteArray) = Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(bytes))
    private fun obj(vararg campos: Pair<String, ValorJson>) = ObjetoJson(mapOf(*campos))
    private fun t(s: String) = TextoJson(s)
    fun escribir(v: IntegridadPairCp2, c: ConsentimientoPair): ByteArray {
        val a = v.aceptacion
        val nonce = sha(Base64Cp1.decodificar(a.enrollNonce, 32))
        return JsonCp1.serializar(obj("formatVersion" to EnteroJson(1), "profile" to t("cp.v2"),
            "state" to t("VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION"),
            "pairFrameB64" to t(Base64Cp1.codificar(v.bytesFrame())), "payloadSha256B64" to t(v.payloadSha256B64),
            "consumedNonceSha256B64" to t(nonce), "consent" to obj("version" to EnteroJson(1),
                "parentExpected" to t(c.datos.publicaPadreEsperada), "parentFingerprint" to t(c.datos.huellaPadre),
                "childFingerprint" to t(c.datos.huellaHijo), "origin" to t(c.datos.origen),
                "boot" to EnteroJson(c.boot.toLong()), "generation" to EnteroJson(c.generacion),
                "adultRevision" to EnteroJson(c.revisionAdulta), "elapsed" to EnteroJson(c.elapsed))))
            .also { require(it.size <= MAX_REGISTRO); leer(it) }
    }
    fun leer(bytes: ByteArray): SnapshotPairCp2 {
        val o = JsonCp1.leer(bytes, MAX_REGISTRO) as? ObjetoJson ?: error("Registro CP2 inválido")
        require(bytes.contentEquals(JsonCp1.serializar(o)))
        require(o.campos.keys == setOf("formatVersion", "profile", "state", "pairFrameB64", "payloadSha256B64", "consumedNonceSha256B64", "consent"))
        fun ObjetoJson.texto(k: String) = (campos[k] as? TextoJson)?.texto ?: error("Tipo registro inválido")
        fun ObjetoJson.numero(k: String) = (campos[k] as? EnteroJson)?.numero ?: error("Tipo registro inválido")
        require(o.numero("formatVersion") == 1L && o.texto("profile") == "cp.v2" &&
            o.texto("state") == "VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION")
        val c = o.campos["consent"] as? ObjetoJson ?: error("Consentimiento ausente")
        require(c.campos.keys == setOf("version", "parentExpected", "parentFingerprint", "childFingerprint", "origin", "boot", "generation", "adultRevision", "elapsed"))
        require(c.numero("version") == 1L && c.numero("boot") in 0..Int.MAX_VALUE.toLong() &&
            c.numero("generation") > 0 && c.numero("adultRevision") > 0 && c.numero("elapsed") >= 0)
        val datos = DatosConfirmadosPair(c.texto("parentExpected"), c.texto("parentFingerprint"), c.texto("childFingerprint"), c.texto("origin"))
        val v = CriptografiaPairCp2.verificarIntegridad(Base64Cp1.decodificar(o.texto("pairFrameB64"), CodecPairCp2.MAX_FRAME), datos.publicaPadreEsperada)
        val a = v.aceptacion
        val nonce = sha(Base64Cp1.decodificar(a.enrollNonce, 32))
        require(o.texto("payloadSha256B64") == v.payloadSha256B64 && o.texto("consumedNonceSha256B64") == nonce &&
            datos.huellaPadre == CodecPairCp2.huellaPublica(a.parentPublicKeyB64) &&
            datos.huellaHijo == CodecPairCp2.huellaPublica(a.childPublicKeyB64) && datos.origen == a.relayOrigin)
        return SnapshotPairCp2(v, ConsentimientoPair(datos, c.numero("boot").toInt(), c.numero("generation"),
            c.numero("adultRevision"), c.numero("elapsed")), nonce)
    }
}

/** Un escritor por ruta en Android. No reemplazo/reintento tras reserva, aunque el frame sea idéntico. */
internal class AlmacenPairCp2(private val archivos: ArchivosPairCp2) {
    private var escribiendo = false
    private var falloIncierto = false
    @Synchronized fun cargar(publicaPadreEsperada: String? = null): CargaPairCp2 =
        if (escribiendo || falloIncierto) CargaPairCp2.Incierta else cargarInterno(publicaPadreEsperada)
    private fun cargarInterno(publicaPadreEsperada: String? = null): CargaPairCp2 = try {
        val reserva = archivos.leerReserva(); val registro = archivos.leerRegistro()
        when {
            reserva == null && registro == null -> CargaPairCp2.Ausente
            reserva == null || registro == null -> CargaPairCp2.Incierta
            !reserva.contentEquals(CodecRegistroPair.sha(registro).toByteArray(Charsets.US_ASCII)) -> CargaPairCp2.Incierta
            else -> {
                val s = CodecRegistroPair.leer(registro)
                if (publicaPadreEsperada != null && s.publicaPadreEsperada != publicaPadreEsperada) CargaPairCp2.Incierta
                else CargaPairCp2.Guardado(s)
            }
        }
    } catch (_: Exception) { CargaPairCp2.Incierta }

    @Synchronized fun guardar(v: ValidacionPair, consentimiento: ConsentimientoPair,
        vigente: () -> Boolean): ResultadoGuardarPair {
        if (escribiendo || falloIncierto) return ResultadoGuardarPair.RECHAZADO
        escribiendo = true
        try {
            if (cargarInterno() !is CargaPairCp2.Ausente || !vigente()) return ResultadoGuardarPair.RECHAZADO
            require(consentimiento.datos == v.consentimiento && consentimiento.boot == v.boot &&
                consentimiento.generacion == v.generacionOferta && consentimiento.revisionAdulta == v.marco.revision)
            val bytes = CodecRegistroPair.escribir(v.integridad, consentimiento)
            val reserva = CodecRegistroPair.sha(bytes).toByteArray(Charsets.US_ASCII)
            if (!vigente()) return ResultadoGuardarPair.RECHAZADO
            archivos.reservar(reserva)
            check(archivos.leerReserva()?.contentEquals(reserva) == true)
            // Si caduca aquí, la reserva queda cerrada. No borrar ni fabricar otra alta.
            if (!vigente()) return ResultadoGuardarPair.INCIERTO
            archivos.escribirRegistro(bytes) // Punto irreversible: autoridad comprobada justo antes de IO.
            check(archivos.leerRegistro()?.contentEquals(bytes) == true)
            val leido = cargarInterno(v.aceptacion.parentPublicKeyB64) as? CargaPairCp2.Guardado ?: error("Readback CP2 falló")
            check(leido.snapshot.integridad.payloadSha256B64 == v.payloadSha256B64)
            return if (vigente()) ResultadoGuardarPair.GUARDADO else ResultadoGuardarPair.GUARDADO_SIN_SESION
        } catch (_: Exception) { falloIncierto = true; return ResultadoGuardarPair.INCIERTO }
        finally { escribiendo = false }
    }
}
