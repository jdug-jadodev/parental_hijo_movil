package dev.controlparental.child.boot

import dev.controlparental.child.data.CargaEstado
import java.security.MessageDigest

/** Solo debug: proyección mínima del resultado duradero, nunca el snapshot completo. */
internal object FormatoTrazaArranque {
    fun crear(accion: String?, desbloqueadoAlRecibir: Boolean?, desbloqueadoAlTerminar: Boolean?, carga: CargaEstado): String? {
        val evento = when (accion) {
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> "LOCKED_BOOT_COMPLETED"
            "android.intent.action.BOOT_COMPLETED" -> "BOOT_COMPLETED"
            "android.intent.action.MY_PACKAGE_REPLACED" -> "MY_PACKAGE_REPLACED"
            else -> return null // No registrar texto arbitrario de intents.
        }
        val prefijo = "evento=$evento desbloqueadoAlRecibir=${bandera(desbloqueadoAlRecibir)} " +
            "desbloqueadoAlTerminar=${bandera(desbloqueadoAlTerminar)}"
        val boot = (carga as? CargaEstado.Disponible)?.estado?.bootContext
        if (boot == null || !UUID_V4.matches(boot.bootId) || boot.systemBootCount?.let { it < 0 } == true) {
            return "$prefijo resultado=ERROR_SEGURO"
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(boot.bootId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "$prefijo resultado=CONTEXTO_PERSISTIDO bootCount=${boot.systemBootCount ?: "DESCONOCIDO"} bootHash=$hash"
    }

    private fun bandera(valor: Boolean?) = when (valor) {
        true -> "SI"
        false -> "NO"
        null -> "DESCONOCIDO"
    }

    private val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
}
