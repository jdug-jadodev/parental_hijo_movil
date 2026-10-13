package dev.controlparental.child.prototype

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/** Receptor SOLO debug, protegido por android.permission.DUMP (shell), no app normal. */
class RollbackPrototipo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val politicas = PoliticasPrototipo(context)
            when (intent.action) {
                COMPROBAR -> {
                    check(politicas.capacidad() && !politicas.pendiente())
                    comprobadoEn = SystemClock.elapsedRealtime()
                    resultCode = 1
                }
                RESTAURAR -> { resultCode = if (politicas.restaurar()) 1 else 0 }
                else -> resultCode = 0
            }
        } catch (_: RuntimeException) { resultCode = 0 }
    }
    companion object {
        const val COMPROBAR = "dev.controlparental.child.PROT01_COMPROBAR_ADB"
        const val RESTAURAR = "dev.controlparental.child.PROT01_RESTAURAR"
        private var comprobadoEn: Long? = null
        internal fun consumirComprobacion(): Boolean {
            val t = comprobadoEn
            comprobadoEn = null
            val ahora = SystemClock.elapsedRealtime()
            return t != null && ahora >= t && ahora - t < 300_000
        }
    }
}
