package dev.controlparental.child.pairing

import android.content.Context
import android.os.UserManager
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dev.controlparental.child.data.*
import dev.controlparental.child.security.FirmanteAndroidKeystore
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.ClavePublicaCp1
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec

internal enum class EstadoIdentidadOferta { DISPONIBLE, NO_DISPONIBLE, INCIERTA }
internal data class LecturaIdentidadOferta(val estado: EstadoIdentidadOferta, val contexto: ContextoOferta? = null)

/** Ausencia de clave con artefacto de creación/pública nunca permite inicializar otra. */
internal fun clasificarIdentidadOferta(clave: Boolean, registro: Boolean, coherente: Boolean): EstadoIdentidadOferta = when {
    !clave && !registro -> EstadoIdentidadOferta.NO_DISPONIBLE
    !clave || !coherente -> EstadoIdentidadOferta.INCIERTA
    else -> EstadoIdentidadOferta.DISPONIBLE
}

/** No promueve PREPARACION, no modifica datos, consumos ni vínculo CP/1. */
internal fun estadoCompatibleOferta(carga: CargaEstado, boot: Int, ahora: Long): Boolean {
    val e = (carga as? CargaEstado.Disponible)?.estado ?: return false
    val b = e.bootContext ?: return false
    return boot >= 0 && b.systemBootCount == boot && ahora >= b.ultimoElapsedObservado &&
        e.vinculo == null && e.provisioningStage in setOf(EtapaAprovisionamiento.NO_CONFIGURADO, EtapaAprovisionamiento.PREPARACION) &&
        e.policy == null && e.lastCommand == null && e.recoveryConsumed.isEmpty() &&
        e.recoveryFailureCount == 0 && !e.esperaRecuperacionPendiente
}

/** Registro CE solo de pública/intención; pérdida o interrupción nunca se interpreta como virginidad. */
internal class IdentidadOfertaAndroid(contexto: Context) {
    private val app = contexto.applicationContext
    private val ruta = File(app.noBackupFilesDir, "identidad-oferta-v1")
    private val registro = AtomicFile(ruta)
    private fun k() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun restos() = listOf(ruta, File(ruta.path + ".bak"), File(ruta.path + ".new")).any { it.exists() }
    private fun compatible(): Boolean = app.getSystemService(UserManager::class.java)?.isUserUnlocked == true &&
        estadoCompatibleOferta(EstadoAndroid.obtener(app).cargar(),
            Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1), android.os.SystemClock.elapsedRealtime())

    fun leer(): LecturaIdentidadOferta = synchronized(monitor) { try {
        if (!compatible()) LecturaIdentidadOferta(EstadoIdentidadOferta.INCIERTA)
        else {
            val almacen = k()
            if (!almacen.containsAlias(ALIAS)) LecturaIdentidadOferta(clasificarIdentidadOferta(false, restos(), false))
            else {
                val publica = FirmanteAndroidKeystore(app, ALIAS).clavePublica.base64
                // Identidad existente sin este registro se conserva; jamás reemplazarla/importarla.
                val estado = clasificarIdentidadOferta(true, restos(), !restos() ||
                    leerRegistro().contentEquals(publica.toByteArray(Charsets.US_ASCII)))
                LecturaIdentidadOferta(estado, if (estado == EstadoIdentidadOferta.DISPONIBLE) ContextoOferta(
                    Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1), publica) else null)
            }
        }
    } catch (_: Exception) { LecturaIdentidadOferta(EstadoIdentidadOferta.INCIERTA) } }

    /** Acción inicial SEPARADA de generar oferta; exige código fresco y confirmación presencial. */
    fun inicializar(autorizada: () -> Boolean): Boolean = synchronized(monitor) { try {
        check(autorizada() && compatible() && leer().estado == EstadoIdentidadOferta.NO_DISPONIBLE)
        check(!restos() && !k().containsAlias(ALIAS))
        escribirRegistro("CREACION_INICIADA".toByteArray(Charsets.US_ASCII)) // Antes de mutar Keystore.
        check(autorizada() && compatible() && !k().containsAlias(ALIAS))
        val opciones = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256).build()
        val par = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").run {
            initialize(opciones); generateKeyPair()
        }
        check(par.private.encoded == null)
        val publica = ClavePublicaCp1.leer(Base64Cp1.codificar(par.public.encoded)).base64
        escribirRegistro(publica.toByteArray(Charsets.US_ASCII))
        check(autorizada() && leer().contexto?.identidad == publica)
        true
    } catch (_: Exception) { false } } // No borrar marca/clave ni reintentar ante fallo parcial.

    private fun leerRegistro(): ByteArray = registro.openRead().use {
        val salida = java.io.ByteArrayOutputStream()
        var siguiente = it.read()
        while (siguiente != -1) {
            check(salida.size() < 200); salida.write(siguiente); siguiente = it.read()
        }
        check(salida.size() > 0); salida.toByteArray()
    }
    private fun escribirRegistro(bytes: ByteArray) {
        val salida = registro.startWrite()
        try { salida.write(bytes); registro.finishWrite(salida) }
        catch (e: Exception) { registro.failWrite(salida); throw e }
        check(leerRegistro().contentEquals(bytes))
    }
    companion object {
        const val ALIAS = "privateSigning"
        private val monitor = Any() // Todas las Activities comparten exclusión; nunca sobrescribir por carrera.
    }
}
