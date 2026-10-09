package dev.controlparental.child.boot

import android.content.Context
import dev.controlparental.child.data.CargaEstado

/** Release no consulta ni registra evidencia de arranques de laboratorio. */
@Suppress("UNUSED_PARAMETER")
internal object TrazaArranque {
    fun leerDesbloqueo(contexto: Context): Boolean? = null
    fun registrar(contexto: Context, accion: String?, alRecibir: Boolean?, carga: CargaEstado) = Unit
}
