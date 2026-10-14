package dev.controlparental.child.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.controlparental.child.network.ControlRedAndroid
import dev.controlparental.child.network.UsoLocal

/** SOLO DEBUG/DUMP shell, cancelar observer/gracia/referencia antes de restore propio. */
class RollbackRedDebug : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val p = goAsync()
        val entregar = {
            p.resultCode = if (ControlRedAndroid.uso == UsoLocal.ERROR) 0 else 1
            p.resultData = "uso=${ControlRedAndroid.uso};activo=${ControlRedAndroid.activo};listo=${ControlRedAndroid.listo};deadline=${ControlRedAndroid.deadline}"
            p.finish()
        }
        ControlRedAndroid.consultar(c) {
            when (i.action) {
                "dev.controlparental.child.LOCAL05_COMPROBAR" -> entregar()
                "dev.controlparental.child.LOCAL05_FINALIZAR" -> ControlRedAndroid.finalizarTecnico(c,
                    { c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }, entregar)
                else -> p.finish()
            }
        }
    }
}
