package dev.controlparental.child.boot

import android.content.Context
import android.os.UserManager
import android.util.Log
import dev.controlparental.child.data.CargaEstado

/** Evidencia técnica en el buffer acotado del sistema; sin archivo ni permiso de uso. */
internal object TrazaArranque {
    fun leerDesbloqueo(contexto: Context): Boolean? = try {
        contexto.getSystemService(UserManager::class.java)?.isUserUnlocked
    } catch (_: RuntimeException) {
        null
    }

    fun registrar(contexto: Context, accion: String?, alRecibir: Boolean?, carga: CargaEstado) {
        try {
            val texto = FormatoTrazaArranque.crear(accion, alRecibir, leerDesbloqueo(contexto), carga) ?: return
            Log.i("ArranqueH04", texto)
        } catch (_: RuntimeException) {
            // Diagnóstico opcional: su fallo jamás cambia el resultado ni el estado local.
        }
    }
}
