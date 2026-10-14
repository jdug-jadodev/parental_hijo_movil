package dev.controlparental.child.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.controlparental.child.lock.BloqueoLocalAndroid
import dev.controlparental.child.lock.FaseLocal
import dev.controlparental.child.prototype.ServicioTemporal

/** SOLO debug/DUMP shell; baseline y rollback limitado al snapshot LOCAL02, nunca credenciales. */
class RollbackLocalDebug : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val pendiente = goAsync()
        when (intent.action) {
            "dev.controlparental.child.LOCAL02_COMPROBAR" -> {
                val limpio = !context.getSharedPreferences("prot01_politicas", Context.MODE_PRIVATE).getBoolean("pendiente", false) &&
                    ServicioTemporal.instancia == null
                BloqueoLocalAndroid.comprobar(context) { ok ->
                    pendiente.resultCode = if (ok && limpio) 1 else 0
                    pendiente.resultData = if (ok && limpio) "BASELINE_LOCAL/NINGUNA" else "BASELINE_LOCAL/ERROR"
                    pendiente.finish()
                }
            }
            "dev.controlparental.child.LOCAL02_RESTAURAR" -> BloqueoLocalAndroid.restaurarPropias(context,
                { context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }) { d ->
                    pendiente.resultCode = if (d.fase == FaseLocal.RESTAURADO) 1 else 0
                    pendiente.resultData = "${d.fase}/${d.causa}"; pendiente.finish()
                }
            else -> pendiente.finish()
        }
    }
}
