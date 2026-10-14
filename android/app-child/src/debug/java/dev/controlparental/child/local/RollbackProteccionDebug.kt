package dev.controlparental.child.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.controlparental.child.protection.FaseProteccion
import dev.controlparental.child.protection.ProteccionAndroid

/** Solo DEBUG/DUMP shell: nueva obligación LOCAL03, no Lock Task ni credenciales. */
class RollbackProteccionDebug : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val p = goAsync()
        fun entregar(r: dev.controlparental.child.protection.EstadoProteccion) {
            p.resultCode = if (r.fase != FaseProteccion.ERROR) 1 else 0
            p.resultData = "${r.fase}/${r.causa};snapshot=${r.snapshot};${r.lectura}"
            p.finish()
        }
        when (i.action) {
            "dev.controlparental.child.LOCAL03_COMPROBAR" -> ProteccionAndroid.consultar(c, ::entregar)
            "dev.controlparental.child.LOCAL03_RESTAURAR" -> {
                val ok = ProteccionAndroid.ejecutar(c, true,
                    { c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }, ::entregar)
                if (!ok) { p.resultCode = 0; p.resultData = "OCUPADA"; p.finish() }
            }
            else -> p.finish()
        }
    }
}
