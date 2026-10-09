package dev.controlparental.protocol

import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.ProviderException
import java.security.Signature

/** Puerto de firma: Android conserva el identificador/handle de Keystore, no exporta la privada. */
interface FirmanteCp1 {
    val clavePublica: ClavePublicaCp1
    fun firmarBytes(bytes: ByteArray): ByteArray
}

/** Firma válida no equivale a autorización de uso, contexto fresco ni restricciones aplicadas. */
class PayloadVerificado internal constructor(
    val mensaje: MensajeCp1,
    val rolFirmante: Rol,
    val proposito: Proposito,
    val payloadSha256B64: String,
    bytes: ByteArray,
) {
    private val originales = bytes.copyOf()
    fun bytesOriginales(): ByteArray = originales.copyOf()
}

object CriptografiaCp1 {
    fun verificarFrame(
        bytes: ByteArray,
        claveConfiable: ClavePublicaCp1,
        rolEsperado: Rol,
        propositoEsperado: Proposito,
    ): PayloadVerificado {
        val sobre = CodecCp1.leerFrame(bytes) as? SobreSinVerificar
            ?: throw ErrorProtocolo(mensaje = "Se requiere un sobre firmado CP/1.")
        return verificar(sobre, claveConfiable, rolEsperado, propositoEsperado)
    }

    fun verificar(
        sobre: SobreSinVerificar,
        claveConfiable: ClavePublicaCp1,
        rolEsperado: Rol,
        propositoEsperado: Proposito,
    ): PayloadVerificado {
        CodecCp1.serializar(sobre) // Validar también sobres construidos localmente.
        if (sobre.purpose != propositoEsperado) {
            throw ErrorProtocolo(CodigoCp1.BAD_SIGNATURE, "Propósito inesperado para esta operación.")
        }
        val payload = Base64Cp1.decodificar(sobre.payloadB64, JsonCp1.MAX_PAYLOAD_BYTES)
        val firma = Base64Cp1.decodificar(sobre.sigB64, 72)
        DerEcdsaCp1.validar(firma)
        val valida = try {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(claveConfiable.clave)
                update(bytesFirmados(sobre.purpose, payload))
                verify(firma)
            }
        } catch (_: GeneralSecurityException) {
            false
        } catch (_: ProviderException) {
            false
        }
        if (!valida) throw ErrorProtocolo(CodigoCp1.BAD_SIGNATURE, "La firma no corresponde a la clave confiable.")

        // Nunca reconstruir JSON antes de verificar: se autentican los bytes originales.
        val mensaje = CodecCp1.leerPayloadSinVerificar(payload, sobre.purpose)
        comprobarRol(mensaje, rolEsperado)
        comprobarClavesVinculo(mensaje, claveConfiable)
        return PayloadVerificado(mensaje, rolEsperado, sobre.purpose,
            Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(payload)), payload)
    }

    fun firmar(
        mensaje: MensajeCp1,
        proposito: Proposito,
        rol: Rol,
        firmante: FirmanteCp1,
    ): SobreSinVerificar {
        val payload = CodecCp1.serializar(mensaje)
        CodecCp1.leerPayloadSinVerificar(payload, proposito)
        comprobarRol(mensaje, rol)
        val clave = firmante.clavePublica
        comprobarClavesVinculo(mensaje, clave)
        val firma = try {
            firmante.firmarBytes(bytesFirmados(proposito, payload))
        } catch (_: GeneralSecurityException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "No se pudo usar la identidad de firma.")
        } catch (_: ProviderException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "El proveedor de firma no está disponible.")
        }
        val sobre = SobreSinVerificar(proposito, Base64Cp1.codificar(payload), Base64Cp1.codificar(firma))
        verificar(sobre, clave, rol, proposito) // No emitir una firma inválida del adaptador.
        return sobre
    }

    internal fun bytesFirmados(proposito: Proposito, payload: ByteArray): ByteArray =
        "CPv1/${proposito.name}\n".toByteArray(Charsets.US_ASCII) + payload

    private fun comprobarClavesVinculo(mensaje: MensajeCp1, firmante: ClavePublicaCp1) {
        if (mensaje !is AceptacionVinculo) return
        val padre = ClavePublicaCp1.leer(mensaje.parentPublicKeyB64)
        ClavePublicaCp1.leer(mensaje.childPublicKeyB64)
        if (padre.base64 != firmante.base64) {
            throw ErrorProtocolo(CodigoCp1.BAD_SIGNATURE, "La pública del padre no corresponde al firmante del vínculo.")
        }
        // H08 debe comprobar además la oferta del hijo y la confirmación física.
    }

    private fun comprobarRol(mensaje: MensajeCp1, esperado: Rol) {
        val rol = when (mensaje) {
            is PruebaAutenticacion -> mensaje.role
            is SolicitudSincronizacion, is OrdenPolitica, is AceptacionVinculo -> Rol.PARENT
            is ConfirmacionOrden, is EstadoHijo -> Rol.CHILD
            is MensajeOffline -> if (mensaje.esRespuesta) Rol.PARENT else Rol.CHILD
            else -> throw ErrorProtocolo(mensaje = "Tipo de payload no firmable.")
        }
        if (rol != esperado) throw ErrorProtocolo(CodigoCp1.WRONG_ROLE, "El rol no corresponde al tipo firmado.")
    }
}
