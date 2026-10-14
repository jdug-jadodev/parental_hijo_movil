package dev.controlparental.child.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.controlparental.child.boot.ArranquePilotoAndroid
import dev.controlparental.child.boot.FasePiloto

/** Solo DEBUG/DUMP shell; desarme primero y restore de obligación propia, no CP1/LOCAL03. */
class RollbackArranqueDebug : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val p = goAsync()
        val entregar = {
            p.resultCode = if (ArranquePilotoAndroid.fase == FasePiloto.ERROR) 0 else 1
            p.resultData = "${ArranquePilotoAndroid.fase};enrolado=${ArranquePilotoAndroid.enrolado}"
            p.finish()
        }
        when (i.action) {
            "dev.controlparental.child.LOCAL04_COMPROBAR" -> ArranquePilotoAndroid.consultar(c, entregar)
            "dev.controlparental.child.LOCAL04_FINALIZAR" -> ArranquePilotoAndroid.finalizarTecnico(c,
                { c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }, entregar)
            else -> p.finish()
        }
    }
}
