package dev.controlparental.child.security

import android.content.Context
import android.os.UserManager
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.ClavePublicaCp1
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ErrorProtocolo
import dev.controlparental.protocol.FirmanteCp1
import dev.controlparental.protocol.JsonCp1
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.security.Signature

/**
 * Firma mediante una identidad YA EXISTENTE en Android Keystore.
 * No genera, importa, reemplaza ni exporta privadas; la identidad se gestionará en H08.
 */
class FirmanteAndroidKeystore(contexto: Context, private val alias: String) : FirmanteCp1 {
    private val contextoApp = contexto.applicationContext

    init {
        require(alias.isNotBlank()) { "Se requiere el alias de la identidad del hijo." }
    }

    override val clavePublica: ClavePublicaCp1
        get() = conEntrada { entrada ->
            ClavePublicaCp1.leer(Base64Cp1.codificar(entrada.certificate.publicKey.encoded))
        }

    override fun firmarBytes(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty() || bytes.size > JsonCp1.MAX_PAYLOAD_BYTES + 16) {
            throw ErrorProtocolo(mensaje = "Tamaño inválido para la operación de firma.")
        }
        return conEntrada { entrada ->
            // Se valida la pública y se entrega únicamente el handle privado a JCA.
            ClavePublicaCp1.leer(Base64Cp1.codificar(entrada.certificate.publicKey.encoded))
            Signature.getInstance("SHA256withECDSA").run {
                initSign(entrada.privateKey)
                update(bytes.copyOf())
                sign()
            }
        }
    }

    private fun <T> conEntrada(operacion: (KeyStore.PrivateKeyEntry) -> T): T {
        try {
            val usuario = contextoApp.getSystemService(UserManager::class.java)
            if (usuario?.isUserUnlocked != true) {
                throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "La identidad requiere el primer desbloqueo de Android.")
            }
            val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entrada = almacen.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "La identidad de Keystore no está disponible.")
            return operacion(entrada)
        } catch (_: GeneralSecurityException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "Android no pudo utilizar la identidad de Keystore.")
        } catch (_: ProviderException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "El proveedor Android Keystore no está disponible.")
        } catch (_: IOException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "No se pudo abrir Android Keystore.")
        } catch (_: SecurityException) {
            throw ErrorProtocolo(CodigoCp1.KEY_UNAVAILABLE, "Android no autorizó el acceso a la identidad.")
        }
    }
}
